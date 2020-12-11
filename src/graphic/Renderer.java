package graphic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import android.graphics.Color;
import android.graphics.Shader;
import ecs.Components.Health;
import ecs.Components.Position;
import ecs.Components.Radius;
import ecs.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.vulkan.VkExportFenceCreateInfo;
import util.Geometry;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;

public class Renderer {

    /**
     * Field of View in Radians
     */
    private static final float FOV = (float) Math.toRadians(60.0f);

    private static final float Z_NEAR = 0.01f;

    private static final float Z_FAR = 1000.f;

    private static final int MAX_POINT_LIGHTS = 5;

    private static final int MAX_SPOT_LIGHTS = 5;

    private Window window;

    private ShaderProgram basicShaderProgram;
    private ShaderProgram bloomShaderProgram;
    private ShaderProgram blendShaderProgram;
    private ShaderProgram ballShaderProgram;
    private ShaderProgram testShaderProgram;

    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;

    private float aspectRatio;

    int original;
    int origTex;
    int horizontalFBO, verticalFBO;
    int horizontalTex, verticalTex;
    Mesh mesh;
    Mesh mesh2;
    Mesh quad;

    float[] cameraPos;
    float[] cameraRot;

    float[] unitCircleVertices;
    int[] unitCircleIndices;

    public Renderer(){
    }

    public void init(Window window) {
        this.window = window;
        glClearColor(0.0f,0.0f,0.0f,0.0f);
        aspectRatio = (float)window.getWidth() / window.getHeight();
        projectionMatrix = new Matrix4f().perspective(FOV, aspectRatio, Z_NEAR, Z_FAR);
        System.out.println(projectionMatrix.toString());

        setupObjects();

        setupBasicShader();
        setupBallShader();
        setupBloomShader();
        setupBlendShader();
        //setupTestShader();
    }

    public void renderCircles(Entity[] balls, Position position, Health health, Radius radius, int[] posIndices, int[] radiusIndices, int[] healthIndices){
        //clearBuffers(0, original);

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_MULTISAMPLE);
        glDisable(GL_BLEND);
        // Render to Texture
        glBindFramebuffer(GL_FRAMEBUFFER, original);
        clear();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, origTex);

        ballShaderProgram.bind();

        ballShaderProgram.setUniform("projectionMatrix", projectionMatrix);
        ballShaderProgram.setUniform("viewMatrix", viewMatrix);
        Vector3f bC1 = new Vector3f(0.839f, 0.070f, 0.870f);
        Vector3f bC2 = new Vector3f(0.956f, 0.501f, 0.082f);
        //Vector3f nC = new Vector3f(1f - bC.x, 1f - bC.y, 1f - bC.z);

        for (int i = 0; i < balls.length; i++) {
            float[] ballPosition = position.getPosition(posIndices[i]);
            int ballRadius = radius.getRadius(radiusIndices[i]);
            int ballHealth = health.getHealth(healthIndices[i]);
            float adjHealth = (float) Math.pow((float)(100 - ballHealth)/100, 1);

            Matrix4f modelMatrix = new Matrix4f().identity().translate(ballPosition[0], ballPosition[1], ballPosition[2]).scale(ballRadius);
            Vector3f color = new Vector3f(bC1.x + adjHealth * (bC2.x-bC1.x), bC1.y + adjHealth * (bC2.y-bC1.y), bC1.z + adjHealth * (bC2.z-bC1.z));

            ballShaderProgram.setUniform("modelMatrix", modelMatrix);
            ballShaderProgram.setUniform("inColor", color);

            Mesh circle = new Mesh(
                    unitCircleVertices,
                    unitCircleIndices,
                    null
            );

            circle.render();
        }

        ballShaderProgram.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // Blur original Texture
        glEnable(GL_BLEND);
        glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA );
        bloomShaderProgram.setUniform("image",0);
        int[] pingpongFBO = new int[]{horizontalFBO,verticalFBO};
        int[] pingpongTex = new int[]{horizontalTex,verticalTex};
        boolean first_iteration = true;
        int amount = 10;
        bloomShaderProgram.bind();
        for (int i = 0; i < amount; i++)
        {
            glBindFramebuffer(GL_FRAMEBUFFER, pingpongFBO[(i%2)]);
            bloomShaderProgram.setUniform("horizontal", i%2);
            glBindTexture(GL_TEXTURE_2D, first_iteration ? origTex : pingpongTex[((i+1)%2)]);
            quad.render();
            //System.out.println(pingpongFBO[(i)%2] + " " + pingpongTex[(i+1)%2]);
            if (first_iteration)
                first_iteration = false;
        }
        bloomShaderProgram.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        clear();


        //glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA );
        blendShaderProgram.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, origTex);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, pingpongTex[1]);
        blendShaderProgram.setUniform("scene", 0); //origTex
        blendShaderProgram.setUniform("bloomBlur", 1); //pingpongTex[1]
        //blendShaderProgram.setUniform("exposure",2);
        quad.render();
        blendShaderProgram.unbind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_MULTISAMPLE);
        glDisable(GL_BLEND);
    }

    public void renderExample(float shift) {
        Matrix4f modelMatrix = new Matrix4f().identity().translate(0, 0, -25).scale(10);
        basicShaderProgram.bind();
        basicShaderProgram.setUniform("projectionMatrix", projectionMatrix);
        basicShaderProgram.setUniform("viewMatrix", viewMatrix);
        basicShaderProgram.setUniform("modelMatrix", modelMatrix);
        glDepthMask(false);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        Mesh mesh = new Mesh(
                new float[]{
                        -0.5f, (0.5f + shift), 0,
                        -0.5f,(-0.5f + shift), 0,
                         0.5f,(-0.5f + shift), 0,
                         0.5f,(0.5f + shift), 0
                },
                new int[]{0,1,2,2,0,3},
                new float[]{
                        1.0f,0.5f,1.0f,
                        0.5f,0.5f,1.0f,
                        1.0f,0.5f,1.0f,
                        1.0f,1.0f,0.5f
                }
        );
        mesh.render();

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(true);

        basicShaderProgram.unbind();
    }

    private void renderBloom(){
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        // Render to Texture
        glBindFramebuffer(GL_FRAMEBUFFER, original);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, origTex);
        basicShaderProgram.bind();
        basicShaderProgram.setUniform("projectionMatrix", projectionMatrix);
        mesh.render();
        basicShaderProgram.unbind();


        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        //clear();

        // Blur original Texture
        glEnable(GL_BLEND);
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
            glBindTexture(GL_TEXTURE_2D, first_iteration ? origTex : pingpongTex[((i+1)%2)]);
            mesh.render();
            System.out.println(pingpongFBO[(i)%2] + " " + pingpongTex[(i+1)%2]);
            if (first_iteration)
                first_iteration = false;
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        bloomShaderProgram.unbind();
        //clear();

        glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA );
        blendShaderProgram.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, origTex);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, pingpongTex[1]);
        blendShaderProgram.setUniform("scene", 0); //origTex
        blendShaderProgram.setUniform("bloomBlur", 1); //pingpongTex[1]
        //blendShaderProgram.setUniform("exposure",2);
        mesh.render();
        blendShaderProgram.unbind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void renderTest(){
        //FIRST
        glEnable(GL_DEPTH_TEST);
        glBindFramebuffer(GL_FRAMEBUFFER, original);
        glBindTexture(GL_TEXTURE_2D, origTex);
        basicShaderProgram.bind();
        basicShaderProgram.setUniform("projectionMatrix", projectionMatrix);
        mesh.render();
        basicShaderProgram.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);

        //SECOND
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, origTex);

        testShaderProgram.bind();
        testShaderProgram.setUniform("scene", 0); //origTex
        testShaderProgram.setUniform("projectionMatrix", projectionMatrix);
        mesh.render();
        testShaderProgram.unbind();

        glBindTexture(GL_TEXTURE_2D, 0);


    }

    public void defaultRender(){
        basicShaderProgram.bind();
        basicShaderProgram.setUniform("projectionMatrix", projectionMatrix);
        mesh2.render();
        basicShaderProgram.unbind();
    }

    private void setupObjects(){
        glDisable(GL_DEPTH_TEST);

        glEnable(GL_TEXTURE_2D);
        glActiveTexture(GL_TEXTURE0);
        original = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, original);

        origTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, origTex);
        glTexImage2D(GL_TEXTURE_2D, 0,GL_RGBA8, window.getWidth(), window.getHeight(), 0,GL_RGBA, GL_UNSIGNED_BYTE,(ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, origTex,0);


        horizontalFBO = glGenFramebuffers();
        verticalFBO = glGenFramebuffers();
        horizontalTex = glGenTextures();
        verticalTex = glGenTextures();
        glBindFramebuffer(GL_FRAMEBUFFER, horizontalFBO);
        glBindTexture(GL_TEXTURE_2D, horizontalTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, window.getWidth() , window.getHeight(),0, GL_RGBA, GL_UNSIGNED_BYTE,(ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, horizontalTex, 0);

        glBindFramebuffer(GL_FRAMEBUFFER, verticalFBO);
        glBindTexture(GL_TEXTURE_2D, verticalTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, window.getWidth() , window.getHeight(),0, GL_RGBA, GL_UNSIGNED_BYTE,(ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, verticalTex, 0);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        
        double pi = 3.14159265;
        int edges = 64;
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

        mesh = new Mesh(
                new float[]{
                        -0.5f, 0.5f, -2.0f,
                        -0.5f,-0.5f, -2.0f,
                        0.5f,-0.5f, -5.0f,
                        0.5f, 0.5f, -5.0f
                },
                new int[]{0,1,2,2,0,3},
                new float[]{
                        0.1f,0.8f,0.6f,
                        0.1f,0.8f,0.6f,
                        0.1f,0.8f,1.0f,
                        0.1f,0.8f,0.6f,
                }
        );

        mesh2 = new Mesh(
                new float[]{
                        -0.48f, 0.48f+0.5f, -10f,
                        -0.48f,-0.48f+0.5f, -10f,
                        0.48f,-0.48f+0.5f, -10f,
                        0.48f, 0.48f+0.5f, -10f
                },
                new int[]{0,1,2,2,0,3},
                new float[]{
                        0.9f,0.4f,0.6f,
                        0.9f,0.4f,0.6f,
                        0.9f,0.4f,0.6f,
                        0.9f,0.4f,0.6f,
                }
        );

        quad = new Mesh(
                new float[]{
                        -1, 1, 0,
                        -1,-1, 0,
                         1,-1, 0,
                         1, 1, 0
                },
                new int[]{0,1,2,2,0,3},
                new float[]{
                        0, 0, 0, 0,
                        0, 0, 0, 0,
                        0, 0, 0, 0,
                        0, 0, 0, 0
                }
        );

        cameraPos = new float[3];
        cameraRot = new float[3];
    }

    private void setupTestShader(){
        testShaderProgram = new ShaderProgram();
        testShaderProgram.createVertexShader(loadResource("resources/test_vertex.glsl"));
        testShaderProgram.createFragmentShader(loadResource("resources/test_fragment.glsl"));
        testShaderProgram.link();

        testShaderProgram.createUniform("scene");
        testShaderProgram.createUniform("projectionMatrix");
    }

    private void setupBasicShader() {
        basicShaderProgram = new ShaderProgram();
        basicShaderProgram.createVertexShader(loadResource("resources/basic_vertex.glsl"));
        basicShaderProgram.createFragmentShader(loadResource("resources/basic_fragment.glsl"));
        basicShaderProgram.link();

        basicShaderProgram.createUniform("projectionMatrix");
        basicShaderProgram.createUniform("viewMatrix");
        basicShaderProgram.createUniform("modelMatrix");
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

    private void setupBloomShader(){
        bloomShaderProgram = new ShaderProgram();
        bloomShaderProgram.createVertexShader(loadResource("resources/bloom_vertex.glsl"));
        bloomShaderProgram.createFragmentShader(loadResource("resources/bloom_fragment.glsl"));
        bloomShaderProgram.link();

        bloomShaderProgram.createUniform("horizontal");
        bloomShaderProgram.createUniform("image");

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
        if (basicShaderProgram != null) {
            basicShaderProgram.cleanup();
        }
    }
}