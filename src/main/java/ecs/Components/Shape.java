package ecs.Components;

import ecs.Component;
import util.Container;

public class Shape extends Component {
    private Container<util.Polygon> shape;

    public Shape() {
        shape = new Container(util.Polygon.class);
    }

    public util.Polygon getShape(int index){
        return shape.get(index);
    }

    public void setShape(int index, util.Polygon shape){
        this.shape.set(index,shape);
    }

    public Shape add(util.Polygon shape){
        //System.out.println("Shape added: " + polygon.toString());
        super.setLastWriteIndex(this.shape.add(shape));
        return this;
    }
}
