package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

public class Collision extends Component {
    private Container<Entity> collisions;

    public Collision(){
        collisions = new Container(Entity.class);
    }

    public Entity getCollision(int index){
        return collisions.get(index);
    }

    public void setCollision(int index, Entity collided){
        this.collisions.set(index, collided);
    }

    public void setCollisions(Container<Entity> collisions){
        this.collisions = collisions;
    }

    public Collision addCollision(Entity collidedEntity){
        //System.out.println("Collision added: " + collidedEntity.getEntityId());
        super.setLastWriteIndex(this.collisions.add(collidedEntity));
        return this;
    }
}
