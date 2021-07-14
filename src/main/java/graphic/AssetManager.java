package graphic;

import de.javagl.obj.*;
import util.Container;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AssetManager {
    private Container<Model> models;
    private Container<AssetGroup> assetGroups;

    public AssetManager(int count){
        models = new Container<>(Model.class, count);
        assetGroups = new Container<>(AssetGroup.class,4);
    }

    public void importAssets(String directory){
        AtomicInteger assetGroupID = new AtomicInteger(-1);
        AtomicInteger itemID = new AtomicInteger();
        AtomicInteger textureID = new AtomicInteger(0);
        try {
            Path dir = Paths.get(directory);
            Files.walk(dir).forEach(path -> {
                File file = path.toFile();
                String name = file.getName();
                if (file.isDirectory()){
                    if (name.startsWith("g")){
                        assetGroupID.getAndIncrement();
                        assetGroups.add(new AssetGroup(assetGroupID.get()));
                    }
                } else {
                    if (name.endsWith("obj")){
                        Model model;
                        Obj obj = ResourceLoader.loadObj(path.toString());
                        Container<Mtl> materials = getMaterials(obj, file);
                        if (materials!=null){
                            model = createMaterialModel(obj, assetGroupID.get(), itemID.get(), materials);
                        } else {
                            model = createTextureModel(obj, assetGroupID.get(), itemID.get(), textureID.get());
                        }
                        try {
                            System.out.println("Model " + itemID.get() + " : " + file.getName());
                        } catch (Exception e){
                            //
                        }
                        models.add(model);
                        itemID.getAndIncrement();
                    } else if (name.endsWith("png") || name.endsWith("jpg")){
                        int id = ResourceLoader.loadTexture(ResourceLoader.loadImage(file));
                        textureID.set(id);
                        System.out.println("ADDED TEXTURE: " + file.getName());
                        assetGroups.get(assetGroupID.get()).setTextureID(textureID.get());
                    }
                }
            });
        } catch (Exception e){
            System.out.println("Error importing assets!");
        }
    }

    private Container<Mtl> getMaterials(Obj obj, File file){
        try {
            List<Mtl> allMtls = new ArrayList<>();
            if (!obj.getMtlFileNames().isEmpty()){
                for (String mtlFileName : obj.getMtlFileNames()) {
                    File upOne = file.getParentFile();
                    InputStream mtlInputStream = new FileInputStream(upOne.getAbsolutePath() + "\\" + mtlFileName);
                    List<Mtl> mtls = MtlReader.read(mtlInputStream);
                    allMtls.addAll(mtls);
                }
                Container<Mtl> materials = new Container<>(Mtl.class);
                for (Mtl mtl : allMtls){
                    materials.add(mtl);
                }
                return materials;
            }
        } catch (Exception e){
            System.out.println("Failed to get obj materials!");
        }
        return null;
    }

    private Model createTextureModel(Obj obj, int groupID, int itemID, int textureID){
        Mesh modelMesh = createTextureMesh(obj);
        return new Model(itemID, textureID, groupID, modelMesh);
    }

    private Mesh createTextureMesh(Obj obj){
        float[] positions = ObjData.getVerticesArray(obj);
        int[] indices = ObjData.getFaceNormalIndicesArray(obj);
        float[] normals = ObjData.getNormalsArray(obj);
        float[] texCoords = ObjData.getTexCoordsArray(obj,2);
        return new Mesh(positions, indices, null, normals, texCoords);
    }

    //TODO: might break???
    private Model createMaterialModel(Obj obj, int groupId, int itemID, Container<Mtl> allMaterials){
        Map<String, Obj> materialGroups = ObjSplitting.splitByMaterialGroups(obj);
        Container<Mesh> meshes = new Container<>(Mesh.class);
        Container<Mtl> materials = new Container<>(Mtl.class);

        for (Map.Entry<String, Obj> entry : materialGroups.entrySet()) {
            String materialName = entry.getKey();
            Obj materialGroup = entry.getValue();

            // Find the MTL that defines the material with the current name
            Mtl mtl = findMtlForName(allMaterials, materialName);
            if (mtl != null) {
                // Render the current material group with this material:
                meshes.add(createMaterialMesh(materialGroup));
                materials.add(mtl);
            }
        }
        Mesh[] meshArray = meshes.toArray();
        Mtl[] materialArray = materials.toArray();

        return new Model(itemID, groupId, meshArray, materialArray);
    }

    private Mesh createMaterialMesh(Obj obj){
        float[] positions = ObjData.getVerticesArray(obj);
        int[] indices = ObjData.getFaceNormalIndicesArray(obj);
        float[] normals = ObjData.getNormalsArray(obj);
        return new Mesh(positions, indices, null, normals, null);
    }

    private Mtl findMtlForName(Container<Mtl> mtls, String name)
    {
        for (int i = 0; i < mtls.getSize(); i++){
            Mtl mtl = mtls.get(i);
            if (mtl.getName().equals(name))
            {
                return mtl;
            }
        }
        return null;
    }

    public Model getModel(int modelID){
        return models.get(modelID);
    }

    public Container<Model> getModels(){
        return models;
    }

    public Container<AssetGroup> getAssetGroups() {
        return assetGroups;
    }

    public void addModel(Model model){
        models.add(model);
    }

    public void setModel(int index, Model model){
        models.set(index, model);
    }

    public void removeModel(int modelID){
        models.remove(models.get(modelID));
    }
}

