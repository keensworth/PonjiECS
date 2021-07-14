package graphic;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.Scanner;

import de.javagl.obj.*;
import ecs.Components.*;
import ecs.Entity;
import org.joml.*;
import util.ComponentMask;
import util.Container;
import util.ETree.EntNode;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL32.*;


public class Renderer {

    private int HEIGHT;
    private int WIDTH;

    //Frustrum
    private final float FOV = (float) Math.toRadians(60.0f);
    private final float Z_NEAR = 1f;
    private final float Z_FAR = 1500.f;
    private float aspectRatio;

    //Lights
    private static final int MAX_POINT_LIGHTS = 15;
    private static final int MAX_SPOT_LIGHTS = 15;

    //Asset manager
    private AssetManager assetManager;

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


    public Renderer(AssetManager assetManager){
        this.assetManager = assetManager;
    }

    public void init(Window window) {
        this.window = window;
        glClearColor(0.0f,0.0f,0.0f,0.0f);

        WIDTH = window.getWidth();
        HEIGHT = window.getHeight();

        aspectRatio = (float) WIDTH / HEIGHT;
        projectionMatrix = new Matrix4f().perspective(FOV, aspectRatio, Z_NEAR, Z_FAR);

        setupObjects();
        setupSceneShader();
        setupBallShader();
        setupBlendShader();
        setupBloomShader();
        setupTestShader();
    }

    public void renderScene(EntNode entities, ComponentMask components){
        glProvokingVertex(GL_LAST_VERTEX_CONVENTION);
        glEnable(GL_DEPTH_TEST);
        
        sceneShaderProgram.bind();
        sceneShaderProgram.setUniform("cameraPos", new Vector3f(cameraPos));
        sceneShaderProgram.setUniform("projectionMatrix", projectionMatrix);
        sceneShaderProgram.setUniform("viewMatrix", viewMatrix);
        sceneShaderProgram.setUniform("textured",0);
        setLightUniforms(entities, components);

        Input input = (Input) components.getComponent(Input.class);
        //render light over cursor
        int[] mousePos = input.getMousePos();
        sceneShaderProgram.setUniform("mousePos", new Vector3f(mousePos[0]-225+cameraPos[0],mousePos[1]-450+cameraPos[1],300));

        //render chunks
        Position position = (Position) components.getComponent(Position.class);
        Rotation rotation = (Rotation) components.getComponent(Rotation.class);
        Scale scale = (Scale) components.getComponent(Scale.class);
        ModelData modelData = (ModelData) components.getComponent(ModelData.class);
        MeshData meshData = (MeshData) components.getComponent(MeshData.class);
        World world = (World) components.getComponent(World.class);

        Container[] chunks = world.getWorld();
        for (int i = 0; i < chunks.length; i++){
            sceneShaderProgram.setUniform("diffuseColor", new Vector3f(0,0,0));
            sceneShaderProgram.setUniform("specularColor", new Vector3f(0,0,0));
            Container<Entity> chunk = chunks[i];
            for (int j = 0; j < chunk.getSize(); j++){
                sceneShaderProgram.setUniform("inColor", new Vector3f(0, 0, 0));
                Entity entity = chunk.get(j);
                int entityID = entity.getEntityId();

                Mesh mesh = null;
                float itemScale = 1;

                float[] pos = position.getPosition(position.getEntityIndex(entityID));
                float[] rot = rotation.getRotation(rotation.getEntityIndex(entityID));
                if (entity.contains(components.getFromClasses(Scale.class))){
                    itemScale = scale.getScale(scale.getEntityIndex(entityID));
                }

                int itemID = modelData.getModel(modelData.getEntityIndex(entityID));
                if (itemID == -1) {
                    mesh = meshData.getMesh(meshData.getEntityIndex(entityID));
                }

                renderItem(itemID, pos, rot, itemScale, mesh);

            }
        }

        sceneShaderProgram.unbind();
    }

    public void renderCircles(EntNode entities, ComponentMask components){
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

        Position position = (Position) components.getComponent(Position.class);
        Health health = (Health) components.getComponent(Health.class);
        Radius radius = (Radius) components.getComponent(Radius.class);

        Container<Entity> balls = entities.getEntities(components.getFromClasses(Radius.class));

        for (int i = 0; i < balls.getSize(); i++) {
            int ball = balls.get(i).getEntityId();

            float[] ballPosition = position.getPosition(position.getEntityIndex(ball));
            int ballRadius = radius.getRadius(radius.getEntityIndex(ball));
            int ballHealth = health.getHealth(health.getEntityIndex(ball));
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

    private void renderItem(int itemID, float[] position, float[] rotation, float scale, Mesh mesh){
        if (scale <= 0)
            scale *= -1;

        //set uniforms
        Matrix4f modelMatrix = new Matrix4f().identity().translate(new Vector3f(position))
                .rotate(rotation[2], new Vector3f(0,0,1)).scale(scale);
        Matrix4f rotationMatrix = new Matrix4f().identity()
                .rotate(rotation[2], new Vector3f(0,0,1));

        if (mesh != null){
            sceneShaderProgram.setUniform("modelMatrix", modelMatrix);
            sceneShaderProgram.setUniform("rotationMatrix", rotationMatrix);
            sceneShaderProgram.setUniform("inColor", new Vector3f(36f/255, 117f/255, 58f/255));
            mesh.render();
        } else {
            Model model = assetManager.getModel(itemID);
            AssetGroup assetGroup = assetManager.getAssetGroups().get(model.getGroupID());

            modelMatrix.scale(assetGroup.getScale()).rotate(assetGroup.getRotation(),assetGroup.getAxis());
            rotationMatrix.rotate(assetGroup.getRotation(), assetGroup.getAxis());

            sceneShaderProgram.setUniform("modelMatrix", modelMatrix);
            sceneShaderProgram.setUniform("rotationMatrix", rotationMatrix.rotate(rotation[2], new Vector3f(0,0,1)));

            //render the item
            if (model.getTextureID() < 0) {         //materials
                renderMaterialItem(model);
            } else if (model.getTextureID() > 0) { //texture
                renderTextureItem(model);
            }
        }
    }

    private void renderMaterialItem(Model model){
        Mesh[] meshes = model.getMeshes();
        Mtl[] materials = model.getMaterials();

        sceneShaderProgram.setUniform("textured",0);

        for(int i = 0; i < materials.length; i++){
            Mesh mesh = meshes[i];
            Mtl material = materials[i];
            if (material!= null) {
                FloatTuple diffuse = material.getKd();
                FloatTuple specular = material.getKs();

                sceneShaderProgram.setUniform("diffuseColor", diffuse);
                sceneShaderProgram.setUniform("specularColor", specular);

                mesh.render();
            }
        }
    }

    private void renderTextureItem(Model model){
        glDisable(GL_BLEND);
        AssetGroup assetGroup = assetManager.getAssetGroups().get(model.getGroupID());
        Mesh mesh = model.getMesh();
        int textureID = assetGroup.getTextureID();

        sceneShaderProgram.setUniform("textured",1);

        glEnable(GL_TEXTURE_2D);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);
        sceneShaderProgram.setUniform("texture", 0);

        mesh.render();

        glBindTexture(GL_TEXTURE_2D, 0);
        glEnable(GL_BLEND);
    }

    private void setLightUniforms(EntNode entities, ComponentMask components){
        Container<Entity> lights = entities.getEntities(components.getFromClasses(Light.class));
        Container<Entity> tacos = entities.getEntities(components.getFromClasses(Camera.class));
        //System.out.println(Integer.toBinaryString(components.getFromClasses(Light.class)));
        System.out.println(tacos.getSize() + ", " + tacos.getSparseSize() + " (renderer)");
        java.lang.System.out.println(Integer.toBinaryString(components.getFromClasses(Light.class)));
        java.lang.System.out.println(Integer.toBinaryString(components.getFromClasses(MeshData.class)));
        Position position = (Position) components.getComponent(Position.class);
        Light light = (Light) components.getComponent(Light.class);
        
        //set light uniforms
        int pointLightIndex = 0;
        int spotLightIndex = 0;
        for (int i = 0; i < lights.getSize(); i++){
            int entity = lights.get(i).getEntityId();
            
            float[] pos = position.getPosition(position.getEntityIndex(entity));
            int lightType = light.getLight(light.getEntityIndex(entity));

            if (lightType == 0){
                sceneShaderProgram.setUniform("pointLights["+pointLightIndex+"]", new Vector3f(pos));
                pointLightIndex++;
            } else {
                sceneShaderProgram.setUniform("spotLights["+spotLightIndex+"]", new Vector3f(pos));
                spotLightIndex++;
            }
        }

        //reset the rest of the light uniforms
        for (int i = pointLightIndex; i < MAX_POINT_LIGHTS; i++){
            sceneShaderProgram.setUniform("pointLights["+i+"]", new Vector3f(0,0,0));
        }
        for (int i = spotLightIndex; i < MAX_SPOT_LIGHTS; i++){
            sceneShaderProgram.setUniform("spotLights["+i+"]", new Vector3f(0,0,0));
        }

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
        sceneShaderProgram.createVertexShader(loadResource("resources/shaders/scene_vertex.glsl"));
        sceneShaderProgram.createFragmentShader(loadResource("resources/shaders/scene_fragment.glsl"));
        sceneShaderProgram.link();

        sceneShaderProgram.createUniform("cameraPos");
        sceneShaderProgram.createUniform("projectionMatrix");
        sceneShaderProgram.createUniform("modelMatrix");
        sceneShaderProgram.createUniform("rotationMatrix");
        sceneShaderProgram.createUniform("viewMatrix");
        sceneShaderProgram.createUniform("inColor");
        sceneShaderProgram.createUniform("spotLights", MAX_SPOT_LIGHTS);
        sceneShaderProgram.createUniform("pointLights", MAX_POINT_LIGHTS);
        sceneShaderProgram.createUniform("mousePos");
        sceneShaderProgram.createUniform("diffuseColor");
        sceneShaderProgram.createUniform("specularColor");
        sceneShaderProgram.createUniform("texture");
        sceneShaderProgram.createUniform("textured");
    }

    private void setupTestShader(){
        textureShaderProgram = new ShaderProgram();
        textureShaderProgram.createVertexShader(loadResource("resources/shaders/texture_vertex.glsl"));
        textureShaderProgram.createFragmentShader(loadResource("resources/shaders/texture_fragment.glsl"));
        textureShaderProgram.link();

        textureShaderProgram.createUniform("scene");
    }

    private void setupBloomShader() {
        bloomShaderProgram = new ShaderProgram();
        bloomShaderProgram.createVertexShader(loadResource("resources/shaders/bloom_vertex.glsl"));
        bloomShaderProgram.createFragmentShader(loadResource("resources/shaders/bloom_fragment.glsl"));
        bloomShaderProgram.link();

        bloomShaderProgram.createUniform("image");
        bloomShaderProgram.createUniform("horizontal");
    }
    
    private void setupBallShader() {
        ballShaderProgram = new ShaderProgram();
        ballShaderProgram.createVertexShader(loadResource("resources/shaders/ball_vertex.glsl"));
        ballShaderProgram.createFragmentShader(loadResource("resources/shaders/ball_fragment.glsl"));
        ballShaderProgram.link();

        ballShaderProgram.createUniform("projectionMatrix");
        ballShaderProgram.createUniform("viewMatrix");
        ballShaderProgram.createUniform("modelMatrix");
        ballShaderProgram.createUniform("inColor");
    }

    private void setupBlendShader() {
        blendShaderProgram = new ShaderProgram();
        blendShaderProgram.createVertexShader(loadResource("resources/shaders/blend_vertex.glsl"));
        blendShaderProgram.createFragmentShader(loadResource("resources/shaders/blend_fragment.glsl"));
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