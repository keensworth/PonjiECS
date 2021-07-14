package ecs.Components;

import ecs.Component;
import ecs.Entity;
import graphic.Mesh;
import util.Container;

public class World extends Component {
    private Container[] world;

    public World(){
        world = new Container[5];
        for (int i = 0; i < 5; i++){
            world[i] = new Container();
        }
    }

    public Container getChunk(int index){
        return world[index];
    }

    public Container[] getWorld(){
        return world;
    }

    public void setChunk(int index, Container chunk){
        world[index] = chunk;
    }

    public void setWorld(Container[] world){
        this.world = world;
    }

    public World add(){
        super.setLastWriteIndex(0);
        return this;
    }
}
