package ecs.Components;

import ecs.Component;
import util.Container;

public class Scale extends Component {
    public Scale(){
        setContainer(new Container<>(Float.class));
    }

    public Scale add(float scale){
        super.add(scale);
        return this;
    }
}
