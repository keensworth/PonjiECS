package ecs.Systems;

import ecs.Entity;
import ecs.System;
import ecs.Components.*;
import graphic.Mesh;
import util.ComponentMask;
import util.ETree.EntNode;
import util.Geometry;
import util.OpenSimplexNoise;

public class ProceduralSys extends System {
    private OpenSimplexNoise noise = new OpenSimplexNoise();
    private Position position;
    private World world;
    private Entity camera;
    private int currentChunk;
    private boolean initialized;

    public ProceduralSys() {
        super(Camera.class);
        currentChunk = 0;
        initialized = false;
    }

    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask componentMask, boolean entityChange) {
        java.lang.System.out.println("Updating ProceduralSys");
        updateValues(entityTree, componentMask);

        if (currentChunk == 0 && !initialized)
            initWorld();

        float[] cameraPos = position.getPosition(position.getEntityIndex(camera.getEntityId()));
        float cameraYPos = cameraPos[1];
        updateWorld(cameraYPos);

        return null;
    }

    private void updateWorld(float cameraYPos){
        int chunk = (int) (cameraYPos / 510);
        java.lang.System.out.println("-----------------------------CHUNK: " + chunk);

        if (chunk > currentChunk){
            currentChunk = chunk;
            loadNextChunk();
        } else if (chunk < currentChunk){
            currentChunk = chunk;
            loadPreviousChunk();
        }
    }

    private void loadNextChunk(){
        Mesh nextChunk = generateWorldMesh(currentChunk+1);

        Mesh[] worldMeshes = world.getWorld();
        Mesh chunkToUnload = worldMeshes[0];

        //Swap mesh locations (shift forward)
        worldMeshes[0] = worldMeshes[1];
        worldMeshes[1] = worldMeshes[2];
        worldMeshes[2] = nextChunk;

        world.setWorld(worldMeshes);

        chunkToUnload.cleanUp();
    }

    private void loadPreviousChunk() {
        Mesh nextChunk = generateWorldMesh(currentChunk-1);

        Mesh[] worldMeshes = world.getWorld();
        Mesh chunkToUnload = worldMeshes[2];

        //Swap mesh locations (shift backwards)
        worldMeshes[2] = worldMeshes[1];
        worldMeshes[1] = worldMeshes[0];
        worldMeshes[0] = nextChunk;
        world.setWorld(worldMeshes);

        chunkToUnload.cleanUp();
    }

    private Mesh generateWorldMesh(int chunk){
        float[][] heightMap = generateHeightMap(chunk);

        float[] vertices = new float[256*256*3*2];
        float[] normals = new float[256*256*3*2];
        int[] indices = new int[255*255*6];
        int height = 90;

        //Fill vertices and normals
        for (int y = 0; y < 256; y++){
            for (int x = 0; x < 256; x++){
                vertices[y*256*3 + x*3]     = x;
                vertices[y*256*3 + x*3 + 1] = y;
                vertices[y*256*3 + x*3 + 2] = heightMap[x][y]*height;
                if (x==0){
                    normals[y*256*3 + x*3]     = 0;
                    normals[y*256*3 + x*3 + 1] = 0;
                    normals[y*256*3 + x*3 + 2] = 0;
                    normals[y*256*3 + x*3 + 256*256*3]     = 0;
                    normals[y*256*3 + x*3 + 1 + 256*256*3] = 0;
                    normals[y*256*3 + x*3 + 2 + 256*256*3] = 0;
                } else {
                    if (y==0){
                        float[] normal = Geometry.normal(
                                new float[]{x-1,y,heightMap[x-1][y]*height},
                                new float[]{x-1,y+1,heightMap[x-1][y+1]*height},
                                new float[]{x,y,heightMap[x][y]*height});

                        normals[y*256*3 + x*3]     = normal[0];
                        normals[y*256*3 + x*3 + 1] = normal[1];
                        normals[y*256*3 + x*3 + 2] = normal[2];
                        normals[y*256*3 + x*3 + 256*256*3]     = normal[0];
                        normals[y*256*3 + x*3 + 1 + 256*256*3] = normal[1];
                        normals[y*256*3 + x*3 + 2 + 256*256*3] = normal[2];

                    } else if (y==255){
                        float[] normal = Geometry.normal(
                                new float[]{x,y-1,heightMap[x][y-1]*height},
                                new float[]{x-1,y,heightMap[x-1][y]*height},
                                new float[]{x,y,heightMap[x][y]*height}
                        );

                        normals[y*256*3 + x*3]     = normal[0];
                        normals[y*256*3 + x*3 + 1] = normal[1];
                        normals[y*256*3 + x*3 + 2] = normal[2];
                        normals[y*256*3 + x*3 + 256*256*3]     = normal[0];
                        normals[y*256*3 + x*3 + 1 + 256*256*3] = normal[1];
                        normals[y*256*3 + x*3 + 2 + 256*256*3] = normal[2];

                    } else {
                        float[] topNormal = Geometry.normal(
                                new float[]{x-1, y-1, heightMap[x-1][y-1]*height},
                                new float[]{x-1, y, heightMap[x-1][y]*height},
                                new float[]{x, y, heightMap[x][y]*height}
                        );

                        float[] bottomNormal = Geometry.normal(
                                new float[]{x,y-1,heightMap[x][y-1]*height},
                                new float[]{x-1,y,heightMap[x-1][y]*height},
                                new float[]{x,y,heightMap[x][y]*height}
                        );

                        for (int i = 0; i < 3; i++){
                            if (x%2==y%2){
                                normals[y*256*3 + x*3 + i] = bottomNormal[i];
                                normals[y*256*3 + x*3 + i + 256*256*3] = topNormal[i];
                            } else {
                                normals[y*256*3 + x*3 + i] = topNormal[i];
                                normals[y*256*3 + x*3 + i + 256*256*3] = bottomNormal[i];
                            }
                        }
                    }
                }
            }
        }

        //Copy vertices twice
        java.lang.System.arraycopy(vertices,0,vertices,vertices.length/2,vertices.length/2);

        //Set indices
        for (int y = 0; y < 255; y++){
            int shift = (y)%2;
            for (int x = shift; x < 255; x += 2) {
                indices[y * 255 * 3 + x * 3    ] = (y+1)*256 + x;
                indices[y * 255 * 3 + x * 3 + 1] = y*256 + x;
                indices[y * 255 * 3 + x * 3 + 2] = y*256 + x+1;
                indices[y * 255 * 3 + x * 3 + 3] = (y+1)*256 + x;
                indices[y * 255 * 3 + x * 3 + 4] = y*256 + x+1;
                indices[y * 255 * 3 + x * 3 + 5] = (y+1)*256 + x+1;
            }
        }



        for (int y = 0; y < 255; y++){
            int shift = (y+1)%2;
            for (int x = shift; x < 255; x+=2){
                indices[y*255*3 + x*3 + 255*255*3] = (y+1)*256 + x + 256*256;
                indices[y*255*3 + x*3 + 1 + 255*255*3] = y*256 + x + 256*256;
                indices[y*255*3 + x*3 + 2 + 255*255*3] = y*256 + x+1 + 256*256;
                indices[y*255*3 + x*3 + 3 + 255*255*3] = (y+1)*256 + x + 256*256;
                indices[y*255*3 + x*3 + 4 + 255*255*3] = y*256 + x+1 + 256*256;
                indices[y*255*3 + x*3 + 5 + 255*255*3] = (y+1)*256 + x+1 + 256*256;
            }
        }

        Mesh generatedChunk = new Mesh(vertices, indices,null, normals);
        return generatedChunk;
    }

    private float[][] generateHeightMap(int chunk){
        final int WIDTH = 512/2;
        final int HEIGHT = 512/2;
        final double FEATURE_SIZE = 128;
        final double RIVER_SIZE = 200f/2.25f;
        final double RIVER_FEATURE = 175;

        float[][] heightMap = new float[256][256];
        float[] weights = new float[]{0.227027f, 0.1945946f, 0.1216216f, 0.054054f, 0.016216f};

        //Create 1D noise representing river
        double[] mainRiverValues = new double[256+8];
        for (int i = 0; i < 256+8; i++){
            mainRiverValues[i] = noise.eval(1,(double)(256*chunk + i - chunk)/RIVER_FEATURE);
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

    private double riverSigmoid(int origin, int x, double normalizer){
        float adjusted = x - origin;
        adjusted /= normalizer;
        double value = 1.5 / (1 + Math.pow(2.718,(4-Math.abs(8*adjusted)))) - 1;
        value += 0.33*adjusted*adjusted;
        return value;
    }

    private double riverSigmoid2(int origin, int x, double normalizer){
        float adjusted = x - origin;
        adjusted /= normalizer;
        double value = -3.3f/(2.44f + 10*adjusted*adjusted)+0.4;
        value += 0.25*adjusted*adjusted;
        return value;
    }

    private double exp(double val) {
        final long tmp = (long) (1512775 * val + 1072632447);
        return Double.longBitsToDouble(tmp << 32);
    }


    private void initWorld(){
        Mesh lowerChunk = generateWorldMesh(-1);
        Mesh middleChunk = generateWorldMesh(0);
        Mesh upperChunk = generateWorldMesh(1);

        world.setWorld(0, lowerChunk);
        world.setWorld(1, middleChunk);
        world.setWorld(2, upperChunk);

        initialized = true;
    }

    private void updateValues(EntNode entityTree, ComponentMask componentMask){
        Entity[] entities = getEntities(entityTree);
        camera = entities[0];

        position = (Position) componentMask.getComponent(Position.class);
        world = (World) componentMask.getComponent(World.class);

    }

    @Override
    public void exit() {

    }
}
