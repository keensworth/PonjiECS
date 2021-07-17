package ecs.Components;

import ecs.Component;
import util.Container;

public class Light extends Component {

    public static final int POINT_LIGHT = 0;
    public static final int SPOT_LIGHT = 1;

    public Light(){
        setContainer(new Container<>());
    }

    public Light add(int lightType){
        super.add(lightType);
        return this;
    }
}