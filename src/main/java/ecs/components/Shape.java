package ecs.components;

import ecs.Component;
import util.Container;

public class Shape extends Component {
    public Shape() {
        setContainer(new Container<>(util.Polygon.class));
    }

    public Shape add(util.Polygon shape){
        super.add(shape);
        return this;
    }
}
