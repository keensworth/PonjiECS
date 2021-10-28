package ecs.components;

import ecs.Component;
import util.Container;

public class Radius extends Component {
    public Radius(){
        setContainer(new Container<>());
    }

    public Radius add(int radius){
        super.add(radius);
        return this;
    }
}
