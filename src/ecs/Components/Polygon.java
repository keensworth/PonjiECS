package ecs.Components;

import ecs.Component;
import util.Container;

public class Polygon extends Component {
    private Container<java.awt.Polygon> polygon;

    public Polygon() {
        polygon = new Container(java.awt.Polygon.class);
    }

    public java.awt.Polygon getPolygon(int index){
        return polygon.get(index);
    }

    public void setPolygon(int index, java.awt.Polygon polygon){
        this.polygon.set(index,polygon);
    }

    public Polygon add(java.awt.Polygon polygon){
        //System.out.println("Polygon added: " + polygon.toString());
        super.setLastWriteIndex(this.polygon.add(polygon));
        return this;
    }
}
