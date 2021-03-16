package ecs.Systems;

import ecs.Entity;
import ecs.System;
import ecs.Components.*;
import util.ComponentMask;
import util.Container;
import util.ETree.EntNode;

public class MovementSys extends System {
    private Position position;
    private Velocity velocity;

    public MovementSys() {
        super(Position.class, Velocity.class);
    }

    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask componentMask, boolean entityChange) {
        java.lang.System.out.println("Updating MovementSys");
        Entity[] entities = getEntities(entityTree);
        Entity[] camera = getEntities(entityTree, new Class[]{Camera.class});

        int[] positionIndices = getComponentIndices(Position.class, entities, componentMask);
        int[] velocityIndices = getComponentIndices(Velocity.class, entities, componentMask);

        position = (Position) componentMask.getComponent(Position.class);
        velocity = (Velocity) componentMask.getComponent(Velocity.class);

        float highestBallYPos = 0;

        if (velocityIndices.length==0){
            //return ControlSys.class;
            return null;
        }
        else { //update positions of entities
            for (int index = 0; index < entities.length; index++) {
                int positionIndex = positionIndices[index];
                int velocityIndex = velocityIndices[index];

                float[] velocityVec = velocity.getVelocity(velocityIndex);
                float[] positionVec = position.getPosition(positionIndex);

                float xVelocity = velocityVec[0];
                float yVelocity = velocityVec[1];

                float xPosition = positionVec[0] + xVelocity*dt;
                float yPosition = positionVec[1] + yVelocity*dt;

                if (yPosition > highestBallYPos && yPosition > 0)
                    highestBallYPos = yPosition;

                position.setXPos(positionIndex, xPosition);
                position.setYPos(positionIndex, yPosition);
            }

            position.setYPos(position.getEntityIndex(camera[0].getEntityId()),highestBallYPos);

            return null;
        }


    }

    @Override
    public void exit() {

    }
}
