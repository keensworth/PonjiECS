package ecs.Components;

import ecs.Component;
import graphic.Mesh;
import util.Container;

public class MeshData extends Component {
    private Container<Mesh> meshes;

    public MeshData(){
        meshes = new Container<>(Mesh.class);
    }

    public Mesh getMesh(int index){
        return  meshes.get(index);
    }

    public void setMesh(int index, Mesh mesh){
        this.meshes.set(index, mesh);
    }

    public MeshData add(Mesh mesh){
        super.setLastWriteIndex(this.meshes.add(mesh));
        return this;
    }
}