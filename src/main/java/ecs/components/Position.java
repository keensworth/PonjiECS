package ecs.components;

import ecs.Component;
import org.joml.Vector3f;
import util.Container;

public class Position extends Component {

    public Position(){
        setContainer(new Container<>(Vector3f.class));
    }

    public Position add(Vector3f position){
        super.add(position);
        return this;
    }
}
