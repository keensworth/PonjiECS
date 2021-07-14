package ecs.Components;

import ecs.Component;
import ecs.Entity;
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
    
    public Mesh getMesh(Entity entity){
        return  getMesh(this.getEntityIndex(entity.getEntityId()));
    }

    public void setMesh(int index, Mesh mesh){
        this.meshes.set(index, mesh);
    }

    public void setMesh(Entity entity, Mesh mesh){
        setMesh(this.getEntityIndex(entity.getEntityId()), mesh);
    }

    public MeshData add(Mesh mesh){
        super.setLastWriteIndex(this.meshes.add(mesh));
        return this;
    }
}