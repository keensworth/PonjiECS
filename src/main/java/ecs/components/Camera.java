package ecs.components;

import ecs.Component;
import util.Container;

public class Camera extends Component {
    private int lastID;

    public Camera(){
        setContainer(new Container<>());
        lastID = 1;
    }

    public Camera add(){
        super.add(lastID);
        lastID++;
        return this;
    }
}
