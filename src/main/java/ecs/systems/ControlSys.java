package ecs.systems;

import ecs.components.*;
import ecs.Entity;
import ecs.System;
import org.joml.Vector3f;
import util.ComponentMask;
import util.nodes.EntNode;

public class ControlSys extends System {
    Position position;
    Velocity velocity;
    Health health;
    Radius radius;
    Input input;
    Light light;

    public ControlSys() {
        super(Input.class, Velocity.class, Position.class);
    }

    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask components) {
        java.lang.System.out.println("Updating ControlSys");

        position = (Position) components.getComponent(Position.class);
        velocity = (Velocity) components.getComponent(Velocity.class);
        health = (Health) components.getComponent(Health.class);
        radius = (Radius) components.getComponent(Radius.class);
        input = (Input) components.getComponent(Input.class);
        light = (Light) components.getComponent(Light.class);

        Entity[] inputEntity = new Entity[]{input.getControllable()};
        Entity[] movingEntities = getEntities(entityTree, new Class[]{Velocity.class});
        int[] velocityIndices = getComponentIndices(Velocity.class, inputEntity, components);
        //java.lang.System.out.println(velocityIndices[0]);
        if (inputEntity[0]!=null){
            if (input.isClicked()){ //launch entity
                int[] click = input.getMousePos();

                int i = click[0] - ecs.width/2;
                int j = click[1] - 50;

                float norm = (float) Math.sqrt(Math.pow(i,2) + Math.pow(j,2));
                float xNorm = i/norm;
                float yNorm = j/norm;

                velocity.set(inputEntity[0], new Vector3f(xNorm*200,yNorm*200,0));

                java.lang.System.out.println("Launcher launched");

                input.setControllable(null);
                input.reset();
                return null;
            }
        } else if (movingEntities.length == 0) { //create new entity
            Entity controllable = ecs.createEntity(
                    position.add(new Vector3f(0,(float)-this.getECS().height/2 + 50, 0)),
                    velocity.add(new Vector3f(0,0,0)),
                    health.add(5),
                    radius.add(25),
                    input.setControllable(),
                    light.add(Light.SPOT_LIGHT)
            );
            input.setControllable(controllable);
        }

        return null;
    }

    @Override
    public void exit() {

    }
}
