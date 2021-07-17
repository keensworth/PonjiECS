package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

public class NoCollide extends Component {
    private int instance = 0;

    public NoCollide(){
        setContainer(new Container<>());
    }

    public int increaseInstance(){
        instance++;
        return instance;
    }

    public NoCollide add(int instance){
        super.add(instance);
        return this;
    }
}
