package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

public class Radius extends Component {
    private Container radius;

    public Radius(){
        radius = new Container();
    }

    public int getRadius(int index){
        return (int)radius.get(index);
    }

    public int getRadius(Entity entity){
        return getRadius(this.getEntityIndex(entity.getEntityId()));
    }

    public void setRadius(int index, int radius){
        this.radius.set(index, radius);
    }

    public void setRadius(Entity entity, int radius){
        setRadius(this.getEntityIndex(entity.getEntityId()), radius);
    }

    public Radius add(int radius){
        //System.out.println("Radius added: " + radius);
        super.setLastWriteIndex(this.radius.add(radius));
        return this;
    }
}
