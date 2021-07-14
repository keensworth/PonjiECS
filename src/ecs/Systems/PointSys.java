package ecs.Systems;

import ecs.Components.Health;
import ecs.Components.Points;
import ecs.Entity;
import ecs.System;
import util.ComponentMask;
import util.ETree.EntNode;

public class PointSys extends System{
    private Health health;
    private Points points;

    public PointSys() {
        super(Health.class);
    }

    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask componentMask, boolean entityChange) {
        java.lang.System.out.println("Updating PointSys");

        Entity[] entitiesKilled = ecs.entityRemove.toArray(new Entity[0]);

        health = (Health) componentMask.getComponent(Health.class);
        points = (Points) componentMask.getComponent(Points.class);

        for (int i = 0; i < entitiesKilled.length; i++){
            Entity entity = entitiesKilled[i];
            points.incPoints(health.getHealth(health.getEntityIndex(entity.getEntityId())));
            java.lang.System.out.println(points.getPoints());
        }

        //return MovementSys.class;
        return null;
    }

    @Override
    public void exit() {

    }
}
