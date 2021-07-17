package ecs.Components;

import ecs.Component;
import org.joml.Vector3f;
import util.Container;

public class Rotation extends Component {

    public Rotation(){
        setContainer(new Container<>(Vector3f.class));
    }

    public Rotation add(Vector3f rotation){
        super.add(rotation);
        return this;
    }
}
