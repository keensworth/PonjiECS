package ecs.Components;

import ecs.Component;
import graphic.Mesh;
import util.Container;

public class MeshData extends Component {

    public MeshData(){
        setContainer(new Container<>(Mesh.class));
    }

    public MeshData add(Mesh mesh){
        super.add(mesh);
        return this;
    }
}