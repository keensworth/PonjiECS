package graphic;

import de.javagl.obj.Mtl;
import de.javagl.obj.Obj;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.function.DoubleToIntFunction;

public class Model {
    private int itemID;
    private int textureID;
    private int groupID;
    private Mesh[] meshes;
    private Mtl[] materials;

    public Model(int itemID, int groupID, Mesh[] meshes, Mtl[] materials){
        this.itemID = itemID;
        this.textureID = -1;
        this.groupID = groupID;
        this.meshes = meshes;
        this.materials = materials;
    }

    public Model(int itemID, int groupID, Mesh mesh, Mtl material){
        this.itemID = itemID;
        this.textureID = -1;
        this.groupID = groupID;
        this.meshes = new Mesh[]{mesh};
        this.materials = new Mtl[]{material};
    }

    public Model(int itemID, int textureID, int groupID, Mesh mesh){
        this.itemID = itemID;
        this.textureID = textureID;
        this.groupID = groupID;
        this.meshes = new Mesh[]{mesh};
    }

    public Model(int itemID, int groupID, Mesh mesh){
        this.itemID = itemID;
        this.textureID = -1;
        this.groupID = groupID;
        this.meshes = new Mesh[]{mesh};
    }

    public int getItemID(){
        return itemID;
    }

    public int getTextureID(){
        return textureID;
    }

    public int getGroupID(){
        return groupID;
    }

    public Mesh[] getMeshes(){
        return meshes;
    }

    public Mtl[] getMaterials(){
        if (materials != null) {
            return materials;
        } else {
            System.out.println("Model " + itemID + " has no materials!");
            return null;
        }
    }

    public Mesh getMesh(){
        return meshes[0];
    }

    public Mtl getMaterial(){
        if (materials[0] != null) {
            return materials[0];
        } else {
            System.out.println("Model " + itemID + " has no material!");
            return null;
        }
    }

    public void clean(){
        meshes = null;
        materials = null;
    }
}
