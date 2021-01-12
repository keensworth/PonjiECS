package graphic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Scanner;

import ecs.Components.Health;
import ecs.Components.Position;
import ecs.Components.Radius;
import ecs.Entity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL32.*;


public class Renderer {

    //Frustrum
    private static final float FOV = (float) Math.toRadians(60.0f);
    private static final float Z_NEAR = 0.01f;
    private static final float Z_FAR = 1000.f;
    private float aspectRatio;

    //GLFW window
    private Window window;

    //Shaders
    private ShaderProgram sceneShaderProgram;
    private ShaderProgram bloomShaderProgram;
    private ShaderProgram blendShaderProgram;
    private ShaderProgram ballShaderProgram;
    private ShaderProgram textureShaderProgram;

    //Transform matrices
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;

    //Framebuffer and texture IDs
    private int sceneBuffer;
    private int sceneTexture;
    private int circleBuffer;
    private int circleTexture;
    private int horizontalFBO, verticalFBO;
    private int horizontalTex, verticalTex;

    //Reusable meshes
    private Mesh quad;
    private Mesh circle;

    //Camera
    private float[] cameraPos;
    private float[] cameraRot;


    public Renderer(){
    }

    public void init(Window window) {
        this.window = window;
        glClearColor(0.0f,0.0f,0.0f,0.0f);
        
        aspectRatio = (float)window.getWidth() / window.getHeight();
        projectionMatrix = new Matrix4f().perspective(FOV, aspectRatio, Z_NEAR, Z_FAR);

        setupObjects();
        setupSceneShader();
        setupBallShader();
        setupBlendShader();
        setupBloomShader();
        setupTestShader();
    }

    public void renderScene(Mesh[] scene){
        glProvokingVertex(GL_LAST_VERTEX_CONVENTION);
        glEnable(GL_DEPTH_TEST);
        
        sceneShaderProgram.bind();
        sceneShaderProgram.setUniform("cameraPos", new Vector3f(cameraPos));
        sceneShaderProgram.setUniform("projectionMatrix", projectionMatrix);
        sceneShaderProgram.setUniform("viewMatrix", viewMatrix);
        sceneShaderProgram.setUniform("inColor", new Vector3f(152f/255, 168f/255, 171f/255));

        //Render each mesh at the appropriate location
        int shift = -1;
        for (Mesh mesh : scene) {
            int chunk = (int) (cameraPos[1] / 510);
            Matrix4f modelMatrix = new Matrix4f().identity().translate(-255, chunk * 255 * 2 + 255 * 2 * shift, -50).scale(2f);
            sceneShaderProgram.setUniform("modelMatrix", modelMatrix);
            mesh.render();
            shift++;
        }
        sceneShaderProgram.unbind();
    }
    
    public void renderCircles(Entity[] balls, Position position, Health health, Radius radius, int[] posIndices, int[] radiusIndices, int[] healthIndices){
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_MULTISAMPLE);
        glEnable(GL_BLEND);
        
        // Render to Texture
        glBindFramebuffer(GL_FRAMEBUFFER, circleBuffer);
        clearBuffers(circleBuffer);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, circleTexture);

        ballShaderProgram.bind();
        ballShaderProgram.setUniform("projectionMatrix", projectionMatrix);
        ballShaderProgram.setUniform("viewMatrix", viewMatrix);
        Vector4f bC1 = new Vector4f(0.839f, 0.070f, 0.870f, 1f);
        Vector4f bC2 = new Vector4f(0.956f, 0.501f, 0.082f, 1f);

        for (int i = 0; i < balls.length; i++) {
            float[] ballPosition = position.getPosition(posIndices[i]);
            int ballRadius = radius.getRadius(radiusIndices[i]);
            int ballHealth = health.getHealth(healthIndices[i]);
            float adjHealth = (float) Math.pow((float)(100 - ballHealth)/100, 1);

            Matrix4f modelMatrix = new Matrix4f().identity().translate(ballPosition[0], ballPosition[1], ballPosition[2]).scale(ballRadius);
            Vector4f color = new Vector4f(bC1.x + adjHealth * (bC2.x-bC1.x), bC1.y + adjHealth * (bC2.y-bC1.y), bC1.z + adjHealth * (bC2.z-bC1.z),1f);

            ballShaderProgram.setUniform("modelMatrix", modelMatrix);
            ballShaderProgram.setUniform("inColor", color);

            circle.render();
        }
        ballShaderProgram.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDisable(GL_BLEND);
        
        
        // Blur circleBuffer Texture
        bloomShaderProgram.setUniform("image",0);
        int[] pingpongFBO = new int[]{horizontalFBO,verticalFBO};
        int[] pingpongTex = new int[]{horizontalTex,verticalTex};
        boolean first_iteration = true;
        int amount = 20;
        bloomShaderProgram.bind();
        for (int i = 0; i < amount; i++)
        {
            glBindFramebuffer(GL_FRAMEBUFFER, pingpongFBO[(i%2)]);
            bloomShaderProgram.setUniform("horizontal", i%2);
            glBindTexture(GL_TEXTURE_2D, first_iteration ? circleTexture : pingpongTex[((i+1)%2)]);
            quad.render();
            if (first_iteration)
                first_iteration = false;
        }
        bloomShaderProgram.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, circleBuffer);

        
        //Blend blur and circle textures
        blendShaderProgram.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, circleTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, pingpongTex[1]);
        blendShaderProgram.setUniform("scene", 0); //circleTexture
        blendShaderProgram.setUniform("bloomBlur", 1); //pingpongTex[1]
        quad.render();
        blendShaderProgram.unbind();

        
        //Render blended texture
        glEnable(GL_BLEND);
        glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA );
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, circleTexture);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        textureShaderProgram.bind();
        textureShaderProgram.setUniform("scene",0);
        quad.render();
        textureShaderProgram.unbind();

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_MULTISAMPLE);
        glDisable(GL_BLEND);
        clearBuffers(pingpongFBO[0],pingpongFBO[1]);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void setupObjects(){
        glDisable(GL_DEPTH_TEST);

        glEnable(GL_TEXTURE_2D);
        glActiveTexture(GL_TEXTURE0);

        //Currently unused
        sceneBuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, sceneBuffer);
        sceneTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, sceneTexture);
        glTexImage2D(GL_TEXTURE_2D, 0,GL_RGBA8, window.getWidth(), window.getHeight(), 0,GL_RGBA, GL_UNSIGNED_BYTE,(ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, sceneTexture,0);

        //Create framebuffer and texture for circles
        circleBuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, circleBuffer);
        circleTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, circleTexture);
        clear();
        glTexImage2D(GL_TEXTURE_2D, 0,GL_RGBA8, window.getWidth(), window.getHeight(), 0,GL_RGBA, GL_UNSIGNED_BYTE,(ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, circleTexture,0);

        //Create framebuffer and texture to perform gaussian blur
        horizontalFBO = glGenFramebuffers();
        verticalFBO = glGenFramebuffers();
        horizontalTex = glGenTextures();
        verticalTex = glGenTextures();
        glBindFramebuffer(GL_FRAMEBUFFER, horizontalFBO);
        glBindTexture(GL_TEXTURE_2D, horizontalTex);
        clear();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, window.getWidth() , window.getHeight(),0, GL_RGBA, GL_UNSIGNED_BYTE,(ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, horizontalTex, 0);

        glBindFramebuffer(GL_FRAMEBUFFER, verticalFBO);
        glBindTexture(GL_TEXTURE_2D, verticalTex);
        clear();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, window.getWidth() , window.getHeight(),0, GL_RGBA, GL_UNSIGNED_BYTE,(ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, verticalTex, 0);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);


        float[] unitCircleVertices;
        int[] unitCircleIndices;
        //Unit circle mesh
        double pi = 3.14159265;
        int edges = 100;
        unitCircleVertices = new float[edges*3+3];
        unitCircleIndices = new int[edges*3+3];
        for (int i = 0; i < edges+1; i++){
            unitCircleVertices[i*3] = (float) Math.cos(((double)i/edges)*2*pi);
            unitCircleVertices[i*3+1] = (float) Math.sin(((double)i/edges)*2*pi);
            unitCircleVertices[i*3+2] = 0;

            unitCircleIndices[i*3] = 0;
            unitCircleIndices[i*3+1] = i+1;
            if (i==edges){
                unitCircleIndices[i*3+2] = 1;
            } else {
                unitCircleIndices[i*3+2] = i+2;
            }
        }

        circle = new Mesh(
                unitCircleVertices,
                unitCircleIndices,
                null
        );

        //Screen mesh, to render textures
        quad = new Mesh(
                new float[]{
                        -1, 1, 0,
                        -1,-1, 0,
                         1,-1, 0,
                         1, 1, 0
                },
                new int[]{0,1,2,2,0,3},
                new float[]{
                        1, 1, 1, 0,
                        1, 1, 1, 0,
                        1, 1, 1, 0,
                        1, 1, 1, 0
                }
        );
        
        cameraPos = new float[3];
        cameraRot = new float[3];
    }
    
    private void setupSceneShader(){
        sceneShaderProgram = new ShaderProgram();
        sceneShaderProgram.createVertexShader(loadResource("resources/scene_vertex.glsl"));
        sceneShaderProgram.createFragmentShader(loadResource("resources/scene_fragment.glsl"));
        sceneShaderProgram.link();

        sceneShaderProgram.createUniform("cameraPos");
        sceneShaderProgram.createUniform("projectionMatrix");
        sceneShaderProgram.createUniform("modelMatrix");
        sceneShaderProgram.createUniform("viewMatrix");
        sceneShaderProgram.createUniform("inColor");
    }

    private void setupTestShader(){
        textureShaderProgram = new ShaderProgram();
        textureShaderProgram.createVertexShader(loadResource("resources/texture_vertex.glsl"));
        textureShaderProgram.createFragmentShader(loadResource("resources/texture_fragment.glsl"));
        textureShaderProgram.link();

        textureShaderProgram.createUniform("scene");
    }

    private void setupBloomShader() {
        bloomShaderProgram = new ShaderProgram();
        bloomShaderProgram.createVertexShader(loadResource("resources/bloom_vertex.glsl"));
        bloomShaderProgram.createFragmentShader(loadResource("resources/bloom_fragment.glsl"));
        bloomShaderProgram.link();

        bloomShaderProgram.createUniform("image");
        bloomShaderProgram.createUniform("horizontal");
    }
    
    private void setupBallShader() {
        ballShaderProgram = new ShaderProgram();
        ballShaderProgram.createVertexShader(loadResource("resources/ball_vertex.glsl"));
        ballShaderProgram.createFragmentShader(loadResource("resources/ball_fragment.glsl"));
        ballShaderProgram.link();

        ballShaderProgram.createUniform("projectionMatrix");
        ballShaderProgram.createUniform("viewMatrix");
        ballShaderProgram.createUniform("modelMatrix");
        ballShaderProgram.createUniform("inColor");
    }

    private void setupBlendShader() {
        blendShaderProgram = new ShaderProgram();
        blendShaderProgram.createVertexShader(loadResource("resources/blend_vertex.glsl"));
        blendShaderProgram.createFragmentShader(loadResource("resources/blend_fragment.glsl"));
        blendShaderProgram.link();

        //createUniforms
        blendShaderProgram.createUniform("scene");
        blendShaderProgram.createUniform("bloomBlur");
        //blendShaderProgram.createUniform("exposure");
    }

    private void updateViewMatrix(float[] cameraPos, float[] cameraRot){
        viewMatrix = new Matrix4f().identity();
        viewMatrix.rotate(cameraRot[0], new Vector3f(1, 0, 0))
                  .rotate(cameraRot[1], new Vector3f(0, 1, 0))
                  .rotate(cameraRot[2], new Vector3f(0, 0, 1));
        viewMatrix.translate(-cameraPos[0], -cameraPos[1], -cameraPos[2]);
    }

    public void prepare(Window window, float[] cameraPos, float[] cameraRot) {
        this.cameraPos = cameraPos;
        this.cameraRot = cameraRot;

        updateViewMatrix(cameraPos, cameraRot);

        clear();

        glViewport(0, 0, window.getWidth(), window.getHeight());
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    private void clearBuffers(int... buffers){
        for (int buffer : buffers){
            glBindFramebuffer(GL_FRAMEBUFFER, buffer);
            clear();
        }
    }

    public static String loadResource(String fileName) {
        String result = "";
        try (InputStream in = Renderer.class.getResourceAsStream(fileName);
             Scanner scanner = new Scanner(in, java.nio.charset.StandardCharsets.UTF_8.name())) {
            result = scanner.useDelimiter("\\A").next();
        } catch (IOException e){
            System.out.println("Could not load resource: " + fileName);
        }
        return result;
    }

    public void cleanup() {
        
    }
}