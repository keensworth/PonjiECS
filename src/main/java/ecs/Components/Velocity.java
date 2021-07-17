package ecs.Components;

import ecs.Component;
import org.joml.Vector3f;
import util.Container;

public class Velocity extends Component {
    public Velocity(){
        setContainer(new Container<>(Vector3f.class));
    }

    public Velocity add(Vector3f velocity){
        super.add(velocity);
        return this;
    }
}
