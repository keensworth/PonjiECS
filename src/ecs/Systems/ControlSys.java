package ecs.Systems;

import ecs.Component;
import ecs.Components.*;
import ecs.ECS;
import ecs.Entity;
import ecs.System;
import util.BitMask;
import util.ETree.EntNode;

public class ControlSys extends System {
    Position position;
    Velocity velocity;
    Health health;
    Radius radius;
    Input input;

    public ControlSys() {
        super(Input.class, Velocity.class, Position.class);
    }

    @Override
    public Class update(float dt, EntNode entityTree, BitMask componentMask, boolean entityChange) {
        java.lang.System.out.println("Updating ControlSys");

        position = (Position) componentMask.getComponent(Position.class);
        velocity = (Velocity) componentMask.getComponent(Velocity.class);
        health = (Health) componentMask.getComponent(Health.class);
        radius = (Radius) componentMask.getComponent(Radius.class);
        input = (Input) componentMask.getComponent(Input.class);

        Entity[] entities = new Entity[]{input.getControllable()};

        int[] velocityIndices = getComponentIndices(Velocity.class, entities, componentMask);
        //java.lang.System.out.println(velocityIndices[0]);
        
        if (entities[0]!=null){
            if (input.isClicked()){//launch entity
                int[] click = input.getClick();
                int i = click[0] - ecs.width/2;
                int j = click[1] - ecs.height + 30;
                float norm = (float) Math.sqrt(Math.pow(i,2) + Math.pow(j,2));
                float xNorm = i/norm;
                float yNorm = j/norm;

                velocity.setVelocity(new float[]{xNorm*200,yNorm*200}, velocityIndices[0]);
                java.lang.System.out.println("Launcher launched");
                input.setControllable(null);
                input.reset();
                return null;
            }
        } else {//create new entity
            Entity controllable = ecs.createEntity(
                    position.add(new float[]{(float)this.getECS().width/2,(float)this.getECS().height-30}),
                    velocity.add(new float[]{0,0}),
                    health.add(5),
                    radius.add(20),
                    input.setControllable()
            );
            input.setControllable(controllable);
        }

        return ControlSys.class;
    }

    @Override
    public void exit() {

    }
}
