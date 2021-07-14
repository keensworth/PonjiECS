package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

public class Health extends Component {
    private Container health;

    public Health() {
        health = new Container();
    }

    public int getHealth(int index){
        return (int)health.get(index);
    }

    public int getHealth(Entity entity){
        return getHealth(this.getEntityIndex(entity.getEntityId()));
    }

    public void setHealth(int index, int health){
        this.health.set(index,health);
    }

    public void setHealth(Entity entity, int health){
        setHealth(this.getEntityIndex(entity.getEntityId()), health);
    }

    public Health add(int health){
        //System.out.println("Health added: " + health);
        super.setLastWriteIndex(this.health.add(health));
        return this;
    }
}
