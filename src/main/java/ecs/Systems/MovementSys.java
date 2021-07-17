package ecs.Systems;

import ecs.Entity;
import ecs.System;
import ecs.Components.*;
import org.joml.Vector3f;
import util.*;
import util.ETree.EntNode;

public class MovementSys extends System {
    private Position position;
    private Velocity velocity;

    public MovementSys() {
        super(Position.class, Velocity.class);
    }

    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask components) {
        java.lang.System.out.println("Updating MovementSys");
        Entity[] entities = getEntities(entityTree);
        Entity[] camera = getEntities(entityTree, new Class[]{Camera.class});

        position = (Position) components.getComponent(Position.class);
        velocity = (Velocity) components.getComponent(Velocity.class);

        float highestBallYPos = 0;

        if (entities.length==0){
            return null;
        }
        else { //update positions of entities
            for (Entity entity : entities) {
                Vector3f velocityVec = velocity.get(entity);
                Vector3f positionVec = position.get(entity);

                positionVec = new Vector3f(positionVec.x + velocityVec.x*dt, positionVec.y + velocityVec.y*dt,positionVec.z + velocityVec.z*dt);

                if (positionVec.y > highestBallYPos && positionVec.y > 0)
                    highestBallYPos = positionVec.y;

                position.set(entity, positionVec);
            }

            Vector3f camPos = position.get(camera[0]);
            position.set(camera[0], new Vector3f(camPos.x, highestBallYPos, camPos.z));

            return null;
        }


    }

    @Override
    public void exit() {

    }
}
