package ecs.Components;

import ecs.Component;
import graphic.Mesh;

public class World extends Component {
    private Mesh[] world;

    public World(){
        world = new Mesh[3];
    }

    public Mesh getWorld(int index){
        return world[index];
    }

    public Mesh[] getWorld(){
        return world;
    }

    public void setWorld(int index, Mesh mesh){
        world[index] = mesh;
    }

    public void setWorld(Mesh[] world){
        this.world = world;
    }

    public World add(Mesh mesh){
        super.setLastWriteIndex(0);
        return this;
    }
}
