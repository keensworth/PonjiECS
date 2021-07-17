package ecs.Systems;

import ecs.Entity;
import ecs.System;
import ecs.Components.*;
import graphic.Mesh;
import org.joml.Vector3f;
import org.joml.sampling.Callback2d;
import org.joml.sampling.PoissonSampling;
import util.*;
import util.ETree.EntNode;

public class ProceduralSys extends System {
    private OpenSimplexNoise noise = new OpenSimplexNoise();
    private PoissonSampling disk;
    private Position position;
    private Rotation rotation;
    private Scale scale;
    private ModelData modelData;
    private MeshData meshData;
    private World world;
    private Entity camera;
    private int currentChunk;
    private boolean initialized;
    
    private static final int CHUNK_SIZE = 512;
    private static float Z_SHIFT;
    private static final int X_SHIFT = -256;

    private static final int FLORA_GENERATE = 0;
    private static final int OBJECT_GENERATE = 1;
    private static final int TREE_GENERATE = 2;

    private static final int GRASS_FEATURE = 25;
    private static final int MUSH_FEATURE = 500;
    private static final int ROCK_FEATURE = 1000;
    private static final int STUMP_FEATURE = 1000;
    private static final int LOG_FEATURE = 1000;
    private static final int TREE_FEATURE = 500;
    
    private static final int GRASS_EDGE_DIST = 75;
    private static final int MUSH_EDGE_DIST = 100;
    private static final int ROCK_EDGE_DIST = 200;
    private static final int STUMP_EDGE_DIST = 200;
    private static final int LOG_EDGE_DIST = 100;
    private static final int TREE_EDGE_DIST = 100;
    
    private static final int[][] NATURE_GROUPS = new int[][]{
            new int[]{337, 338, 339, 340, 341, 342},                //grass
            new int[]{343, 344, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358}, //mush
            new int[]{360, 361, 362, 363, 364, 365, 366, 367, 368},           //rock
            new int[]{369, 370},                                              //stump
            new int[]{345, 346, 347},                                         //log
            new int[]{                                                        //tree
                    371,372,373,374,385,386,387,388,397,398,399,400,401},
            new int[]{379,380,381,382,383,384}                                //dead tree
    };
    
    public ProceduralSys() {
        super(Camera.class);
        currentChunk = 0;
        initialized = false;
    }

    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask components) {
        java.lang.System.out.println("Updating ProceduralSys");
        updateValues(entityTree, components);

        if (currentChunk == 0 && !initialized) {
            int windowHeight = ecs.getWindow().getHeight();
            int windowWidth = ecs.getWindow().getWidth();
            float camHeight = (float) ((windowHeight/2) * Math.sqrt(3));
            Z_SHIFT = camHeight * (1 - ((float)CHUNK_SIZE / windowWidth));

            initWorld();
        }


        Vector3f cameraPos = position.get(camera);
        float cameraYPos = cameraPos.y;
        updateWorld(cameraYPos);

        return null;
    }

    private void updateWorld(float cameraYPos){
        int chunk = (int) (cameraYPos / CHUNK_SIZE);
        //java.lang.System.out.println("-----------------------------CHUNK: " + chunk);

        if (chunk > currentChunk){
            currentChunk = chunk;
            loadNextChunk();
        } else if (chunk < currentChunk){
            currentChunk = chunk;
            loadPreviousChunk();
        }
    }

    private void loadNextChunk(){
        Container nextChunk = generateChunk(currentChunk+1);

        Container[] worldMeshes = world.getWorld();

        //Swap mesh locations (shift forward)
        worldMeshes[0] = worldMeshes[1];
        worldMeshes[1] = worldMeshes[2];
        worldMeshes[2] = worldMeshes[3];
        worldMeshes[3] = worldMeshes[4];
        worldMeshes[4] = nextChunk;

        world.setWorld(worldMeshes);
    }

    private void loadPreviousChunk() {
        Container previousChunk = generateChunk(currentChunk-1);

        Container[] worldMeshes = world.getWorld();

        //Swap mesh locations (shift forward)
        worldMeshes[4] = worldMeshes[3];
        worldMeshes[3] = worldMeshes[2];
        worldMeshes[2] = worldMeshes[1];
        worldMeshes[1] = worldMeshes[0];
        worldMeshes[0] = previousChunk;

        world.setWorld(worldMeshes);
    }

    private Container generateChunk(int chunk){
        Container<Entity> currentChunk = new Container<>(Entity.class);

        float[][] heightMap = generateHeightMap(chunk);

        //add world floor
        Mesh worldMesh = generateWorldMesh(heightMap, 0);
        Entity entity = ecs.createEntity(
                position.add(new Vector3f(X_SHIFT, chunk*CHUNK_SIZE, Z_SHIFT)),
                rotation.add(new Vector3f(0,0,0)),
                scale.add((float)CHUNK_SIZE/31),
                modelData.add(-1),
                meshData.add(worldMesh)
        );
        currentChunk.add(entity);

        float[][] treeNoise = generateNoise(128, 16, true, chunk);
        float[][] natureObjectsNoise = generateNoise(128, 8, false, chunk);
        
        //create and add nature objects
        Container flora   = generateEntities(chunk, natureObjectsNoise, FLORA_GENERATE);
        Container objects = generateEntities(chunk, natureObjectsNoise, OBJECT_GENERATE);
        Container trees   = generateEntities(chunk, treeNoise, TREE_GENERATE);
        currentChunk.add((Entity[]) flora.toArray());
        currentChunk.add((Entity[]) objects.toArray());
        currentChunk.add((Entity[]) trees.toArray());
        
        return currentChunk;
    }

    private Container generateEntities(int chunk, float[][] noise, int objectTag){
        float adjust = CHUNK_SIZE/(float)noise.length;
        Container<Entity> entities = new Container<>(Entity.class);

        if (objectTag == FLORA_GENERATE){
            PoissonGrid.Grid disk = new PoissonGrid.Grid(chunk, noise.length/2, 10, 30, noise, new Callback2d() {
                int tempMax = (int)(Math.random()*4);
                int tempCount = 0;
                int modelGroup = 0;
                @Override
                public void onNewSample(float v, float v1) {
                    if (Math.random() < 0.05){
                        modelGroup = 1;
                    }
                    if (tempCount == tempMax && modelGroup==1){
                        tempCount = 0;
                        tempMax = (int)(Math.random()*8);
                        modelGroup = 0;
                    }
                    int modelIndex = (int)(Math.random()*NATURE_GROUPS[0].length);
                    int modelID = NATURE_GROUPS[modelGroup][modelIndex];
                    Entity entity = ecs.createEntity(
                            position.add(new Vector3f(v*adjust, chunk*CHUNK_SIZE + v1*adjust + 256, Z_SHIFT)),
                            rotation.add(new Vector3f(0,0,v1%3.141f)),
                            scale.add(0.75f),
                            modelData.add(modelID)
                    );
                    entities.add(entity);

                    if (modelGroup==1)
                        tempCount++;
                }
            });

        } else if (objectTag == OBJECT_GENERATE){
            int[][] present = new int[noise.length][noise.length];
            for (int y = 0; y < noise.length; y++){
                for (int x = 0; x < noise.length; x++){
                    if (noise[y][x] < -0.9 && y > 10 && !objectBelow(x, y, present, 10)){
                        present[y][x] = 1;
                    }
                }
            }

            for (int y = 0; y < present.length; y++){
                for (int x = 0; x < present.length; x++){
                    if (present[y][x] == 1){
                        float object = (float) Math.random();
                        int modelGroup = object <= 0.5 ? 2 : object <= 0.8 ? 3 : 4;
                        int modelIndex = (int)(Math.random()*NATURE_GROUPS[modelGroup].length);
                        int modelID = NATURE_GROUPS[modelGroup][modelIndex];
                        Entity entity = ecs.createEntity(
                                position.add(new Vector3f(X_SHIFT + x*adjust, chunk*CHUNK_SIZE + y*adjust, Z_SHIFT)),
                                rotation.add(new Vector3f(0, 0, y%3.141f)),
                                scale.add(1f),
                                modelData.add(modelID)
                        );
                        entities.add(entity);
                    }
                }
            }
        } else {
            int[][] present = new int[noise.length][noise.length];
            for (int y = 0; y < noise.length; y++){
                for (int x = 0; x < noise.length; x++){
                    if (noise[y][x] > 0.1 && y > 20 && !objectBelow(x, y, present, 20)){
                        present[y][x] = 1;
                    }
                }
            }

            for (int y = 0; y < present.length; y++){
                for (int x = 0; x < present.length; x++){
                    if (present[y][x] == 1){
                        float object = (float) Math.random();
                        int modelGroup = object <= 0.9 ? 5 : 6;
                        int modelIndex = (int)(Math.random()*NATURE_GROUPS[modelGroup].length);
                        int modelID = NATURE_GROUPS[modelGroup][modelIndex];
                        Entity entity = ecs.createEntity(
                                position.add(new Vector3f(X_SHIFT + x*adjust, chunk*CHUNK_SIZE + y*adjust, Z_SHIFT)),
                                rotation.add(new Vector3f(0,0, y%3.141f)),
                                scale.add(0.7f),
                                modelData.add(modelID)
                        );
                        entities.add(entity);
                    }
                }
            }
        }
        return entities;
    }

    private boolean objectBelow(int x, int y, int[][] presenceArray, int distance){
        for (int j = y; j > y-distance; j--){
            for (int i = x+distance; i > x-distance; i--){
                if (i>=0 && j>=0 && i < presenceArray.length && j < presenceArray.length){
                    if (presenceArray[j][i] == 1){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private float[][] generateNoise(int size, int freq, boolean treeNoise, int chunk){
        float[][] noiseArray = new float[size][size];
        for (int y = 0; y < size; y++) {
            double mainRiverValue, mainRiverValueP1, mainRiverValueP2, mainRiverValueN1, mainRiverValueN2;
            int riverCoord = 0, riverCoordP1 = 0, riverCoordP2 = 0, riverCoordN1 = 0, riverCoordN2 = 0;

            if (treeNoise) {
                mainRiverValue = noise.eval(1, (double) (y) / 75);
                mainRiverValueP1 = noise.eval(1, (double) (y + 1.38461) / 75);
                mainRiverValueP2 = noise.eval(1, (double) (y + 3.230769) / 75);
                mainRiverValueN1 = noise.eval(1, (double) (y - 1.38461) / 75);
                mainRiverValueN2 = noise.eval(1, (double) (y - 3.230769) / 75);
                riverCoord = (int) (size / 2 + mainRiverValue * 32);
                riverCoordP1 = (int) (size / 2 + mainRiverValueP1 * 32);
                riverCoordP2 = (int) (size / 2 + mainRiverValueP2 * 32);
                riverCoordN1 = (int) (size / 2 + mainRiverValueN1 * 32);
                riverCoordN2 = (int) (size / 2 + mainRiverValueN2 * 32);
            }
            for (int x = 0; x < size; x++) {

                double riverValue = 0;
                double value = noise.eval((x) / freq, (y+chunk*size) / freq);

                if (treeNoise) {
                    riverValue = riverSigmoid(riverCoord, x, 32, true, size) * 0.22702;
                    riverValue += riverSigmoid(riverCoordP1, x, 32, true, size) * 0.31621 + riverSigmoid(riverCoordP2, x, 32, true, size) * 0.07027 + riverSigmoid(riverCoordN1, x, 32, true, size) * 0.31621 + riverSigmoid(riverCoordN2, x, 32, true, size) * 0.07027;
                }

                double average;
                if (treeNoise){
                    average  = (riverValue*1 + value*1)/2;
                } else {
                    average = value;
                }

                noiseArray[y][x] = (float) average;
            }
        }
        return noiseArray;
    }


    private Mesh generateWorldMesh(float[][] heightMap, int height){
        float[] vertices = new float[32*32*3*2];
        float[] normals = new float[32*32*3*2];
        int[] indices = new int[31*31*6];

        //Fill vertices and normals
        for (int y = 0; y < 32; y++){
            for (int x = 0; x < 32; x++){
                vertices[y*32*3 + x*3]     = x;
                vertices[y*32*3 + x*3 + 1] = y;
                vertices[y*32*3 + x*3 + 2] = heightMap[x][y]*height;
                if (x==0){
                    normals[y*32*3 + x*3]     = 0;
                    normals[y*32*3 + x*3 + 1] = 0;
                    normals[y*32*3 + x*3 + 2] = 0;
                    normals[y*32*3 + x*3 + 32*32*3]     = 0;
                    normals[y*32*3 + x*3 + 1 + 32*32*3] = 0;
                    normals[y*32*3 + x*3 + 2 + 32*32*3] = 0;
                } else {
                    if (y==0){
                        float[] normal = Geometry.normal(
                                new float[]{x-1,y+1,heightMap[x-1][y+1]*height},
                                new float[]{x-1,y,heightMap[x-1][y]*height},
                                new float[]{x,y,heightMap[x][y]*height});

                        normals[y*32*3 + x*3]     = normal[0];
                        normals[y*32*3 + x*3 + 1] = normal[1];
                        normals[y*32*3 + x*3 + 2] = normal[2];
                        normals[y*32*3 + x*3 + 32*32*3]     = normal[0];
                        normals[y*32*3 + x*3 + 1 + 32*32*3] = normal[1];
                        normals[y*32*3 + x*3 + 2 + 32*32*3] = normal[2];

                    } else if (y==31){
                        float[] normal = Geometry.normal(
                                new float[]{x-1,y,heightMap[x-1][y]*height},
                                new float[]{x,y-1,heightMap[x][y-1]*height},
                                new float[]{x,y,heightMap[x][y]*height}
                        );

                        normals[y*32*3 + x*3]     = normal[0];
                        normals[y*32*3 + x*3 + 1] = normal[1];
                        normals[y*32*3 + x*3 + 2] = normal[2];
                        normals[y*32*3 + x*3 + 32*32*3]     = normal[0];
                        normals[y*32*3 + x*3 + 1 + 32*32*3] = normal[1];
                        normals[y*32*3 + x*3 + 2 + 32*32*3] = normal[2];

                    } else {
                        float[] topNormal = Geometry.normal(
                                new float[]{x-1, y, heightMap[x-1][y]*height},
                                new float[]{x-1, y-1, heightMap[x-1][y-1]*height},
                                new float[]{x, y, heightMap[x][y]*height}
                        );

                        float[] bottomNormal = Geometry.normal(
                                new float[]{x-1,y,heightMap[x-1][y]*height},
                                new float[]{x,y-1,heightMap[x][y-1]*height},
                                new float[]{x,y,heightMap[x][y]*height}
                        );
                        //java.lang.System.out.println("NORMAL: " + bottomNormal[0] + ", "+ bottomNormal[1] + ", "+ bottomNormal[2]);

                        for (int i = 0; i < 3; i++){
                            if (x%2==y%2){
                                normals[y*32*3 + x*3 + i] = bottomNormal[i];
                                normals[y*32*3 + x*3 + i + 32*32*3] = topNormal[i];
                            } else {
                                normals[y*32*3 + x*3 + i] = topNormal[i];
                                normals[y*32*3 + x*3 + i + 32*32*3] = bottomNormal[i];
                            }
                        }
                    }
                }
            }
        }

        //Copy vertices twice
        java.lang.System.arraycopy(vertices,0,vertices,vertices.length/2,vertices.length/2);

        //Set indices
        for (int y = 0; y < 31; y++){
            int shift = (y)%2;
            for (int x = shift; x < 31; x += 2) {
                indices[y * 31 * 3 + x * 3    ] = (y+1)*32 + x;
                indices[y * 31 * 3 + x * 3 + 1] = y*32 + x;
                indices[y * 31 * 3 + x * 3 + 2] = y*32 + x+1;
                indices[y * 31 * 3 + x * 3 + 3] = (y+1)*32 + x;
                indices[y * 31 * 3 + x * 3 + 4] = y*32 + x+1;
                indices[y * 31 * 3 + x * 3 + 5] = (y+1)*32 + x+1;
            }
        }

        for (int y = 0; y < 31; y++){
            int shift = (y+1)%2;
            for (int x = shift; x < 31; x+=2){
                indices[y*31*3 + x*3 + 31*31*3] = (y+1)*32 + x + 32*32;
                indices[y*31*3 + x*3 + 1 + 31*31*3] = y*32 + x + 32*32;
                indices[y*31*3 + x*3 + 2 + 31*31*3] = y*32 + x+1 + 32*32;
                indices[y*31*3 + x*3 + 3 + 31*31*3] = (y+1)*32 + x + 32*32;
                indices[y*31*3 + x*3 + 4 + 31*31*3] = y*32 + x+1 + 32*32;
                indices[y*31*3 + x*3 + 5 + 31*31*3] = (y+1)*32 + x+1 + 32*32;
            }
        }

        Mesh generatedChunk = new Mesh(vertices, indices,null, normals);
        return generatedChunk;
    }

    private float[][] generateHeightMap(int chunk){
        final int WIDTH = 32;
        final int HEIGHT = WIDTH;
        final double FEATURE_SIZE = 24;
        final double RIVER_SIZE = 200f/2.25f/8f;
        final double RIVER_FEATURE = 30;

        float[][] heightMap = new float[HEIGHT][HEIGHT];
        float[] weights = new float[]{0.227027f, 0.1945946f, 0.1216216f, 0.054054f, 0.016216f};

        //Create 1D noise representing river
        double[] mainRiverValues = new double[HEIGHT+8];
        for (int i = 0; i < HEIGHT+8; i++){
            mainRiverValues[i] = noise.eval(1,(double)(HEIGHT*chunk + i - chunk)/RIVER_FEATURE);
        }

        //Create height map
        for (int y = 0; y < HEIGHT; y++) {

            int riverCoord = (int) (WIDTH/2 + mainRiverValues[y+4]*RIVER_SIZE);
            int[] riverCoordP = new int[]{
                    (int) (WIDTH/2 + mainRiverValues[y+4+1]*RIVER_SIZE),
                    (int) (WIDTH/2 + mainRiverValues[y+4+2]*RIVER_SIZE),
                    (int) (WIDTH/2 + mainRiverValues[y+4+3]*RIVER_SIZE),
                    (int) (WIDTH/2 + mainRiverValues[y+4+4]*RIVER_SIZE)
            };
            int[] riverCoordN = new int[]{
                    (int) (WIDTH/2 + mainRiverValues[y+4-1]*RIVER_SIZE),
                    (int) (WIDTH/2 + mainRiverValues[y+4-2]*RIVER_SIZE),
                    (int) (WIDTH/2 + mainRiverValues[y+4-3]*RIVER_SIZE),
                    (int) (WIDTH/2 + mainRiverValues[y+4-4]*RIVER_SIZE)
            };

            for (int x = 0; x < WIDTH; x++) {
                double freq = FEATURE_SIZE;
                double scale = 0.5;
                double currentScale = 0.5;
                double value=0;

                //compose varying frequency noise maps
                while (freq >= 1) {
                    value += noise.eval((x) / freq,   (y + chunk * HEIGHT - chunk) / freq) * currentScale * 1.125;
                    currentScale *= scale;
                    freq /= 2;
                }

                //Stretch out 1D noise to create river height map
                double riverValue = riverSigmoid2(riverCoord, x, RIVER_SIZE)*weights[0];
                for (int i = 1; i < 5; i++){
                    riverValue += riverSigmoid2(riverCoordP[i-1], x, RIVER_SIZE)*weights[i] + riverSigmoid2(riverCoordN[i-1], x, RIVER_SIZE)*weights[i];
                }

                //Combine terrain and river height maps
                double composite = (riverValue * 0.6 + value * 0.4);
                heightMap[x][y] = (float) composite;
            }
        }

        return heightMap;
    }

    private double riverSigmoid(int origin, int x, double normalizer, boolean tree, int size){
        if (tree){
            origin = size/2;
        }
        float adjusted = x - origin;
        adjusted /= normalizer;
        //System.out.println(value);
        double value = (1 / (1 + Math.pow(2.718,(4-Math.abs(4*adjusted)))) - 1);
        value += (1 / (1 + Math.pow(2.718,(3-Math.abs(12*(0.4*adjusted+1))))) - 1);
        value += (1 / (1 + Math.pow(2.718,(3-Math.abs(12*(0.4*adjusted-1))))) - 1);
        value += 0.225;
        return value;
    }

    private double riverSigmoid2(int origin, int x, double normalizer){
        /*
        float adjusted = x - origin;
        adjusted /= normalizer;
        double value = -3.3f/(2.44f + 18*adjusted*adjusted)+0.35;
        //value += 0.25*adjusted*adjusted;
        return value;

         */
        return 0;
    }

    private double exp(double val) {
        final long tmp = (long) (1512775 * val + 1072632447);
        return Double.longBitsToDouble(tmp << 32);
    }


    private void initWorld(){
        Container lowerChunk = generateChunk(-2);
        Container lowerMiddleChunk = generateChunk(-1);
        Container middleChunk = generateChunk(0);
        Container upperMiddleChunk = generateChunk(1);
        Container upperChunk = generateChunk(2);

        world.setChunk(0, lowerChunk);
        world.setChunk(1, lowerMiddleChunk);
        world.setChunk(2, middleChunk);
        world.setChunk(3, upperMiddleChunk);
        world.setChunk(4, upperChunk);

        initialized = true;
    }

    private void updateValues(EntNode entityTree, ComponentMask components){
        Entity[] entities = getEntities(entityTree);
        camera = entities[0];

        position = (Position) components.getComponent(Position.class);
        rotation = (Rotation) components.getComponent(Rotation.class);
        scale = (Scale) components.getComponent(Scale.class);
        modelData = (ModelData) components.getComponent(ModelData.class);
        meshData = (MeshData) components.getComponent(MeshData.class);
        world = (World) components.getComponent(World.class);

    }

    @Override
    public void exit() {

    }
}
