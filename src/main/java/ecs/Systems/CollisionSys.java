package ecs.Systems;

import ecs.Components.*;
import ecs.Entity;
import ecs.System;
import org.joml.Vector3f;
import util.ComponentMask;
import util.Container;
import util.ETree.CollisionNode;
import util.ETree.EntNode;
import util.Geometry;
import util.Polygon;

public class CollisionSys extends System {
    private Entity[] balls;

    private ComponentMask components;
    private Position position;
    private Velocity velocity;
    private Radius radius;
    private Health health;
    private NoCollide noCollide;
    private Shape shape;
    private Light light;

    public CollisionSys() {
        super(Position.class, Radius.class);
    }

    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask components) {
        java.lang.System.out.println("Updating CollisionSys");

        updateValues(dt, entityTree, components);

        //traverse entities, build collision tree
        CollisionNode collisionTree = buildCollisionTree(components, this.ecs.width, this.ecs.height);

        //traverse collision tree, collect values
        Container<Entity> concatenated = concatenate(collisionTree);

        //build refined collision container
        Container<Entity>[] possibleCollisions = buildCollisionGroups(concatenated);

        //Broad Phase analysis of collision container
        Container<Entity>[] broadPhase = refineCollisions(false, possibleCollisions);

        //Narrow Phase analysis of collision container
        Container<Entity>[] narrowPhase = refineCollisions(true, broadPhase);
        if (narrowPhase.length==0){
            //return MovementSys.class;
            return null;
        }

        return null;
    }

    private void updateValues(float dt, EntNode entityTree, ComponentMask components){
        components = components;
        int worldWidth = this.getECS().width;
        int worldHeight = this.getECS().height;

        //entities = getEntities(entityTree);
        balls = getEntities(entityTree, new Class[]{Radius.class});

        position = (Position) components.getComponent(Position.class);
        health = (Health) components.getComponent(Health.class);
        velocity = (Velocity) components.getComponent(Velocity.class);
        radius = (Radius) components.getComponent(Radius.class);
        noCollide = (NoCollide) components.getComponent(NoCollide.class);
        shape = (Shape) components.getComponent(Shape.class);
        light = (Light) components.getComponent(Light.class);
    }

    private CollisionNode buildCollisionTree(ComponentMask components, int worldWidth, int worldHeight){
        CollisionNode tempCollisionTree = new CollisionNode(worldWidth, true, -1);

        //Build collision tree w/ entities
        for (int index = 0; index < balls.length; index++){
            Entity entity = balls[index];

            float minX, maxX, minY, maxY;

            if (entity.contains(components.get(shape))) {
                util.Polygon objectPoly = shape.get(entity);

                minX = objectPoly.minX;
                maxX = objectPoly.maxX;

            } else {
                Vector3f entityPos = position.get(entity);
                int entityRadius = radius.get(entity);

                minX = entityPos.x - entityRadius;
                maxX = entityPos.x + entityRadius;
                minY = entityPos.y - entityRadius;
                maxY = entityPos.y + entityRadius;

                entityPos = checkWorldEdges(entity, entityPos, minX, maxX, minY, maxY, worldHeight, worldWidth);
                minX = entityPos.x - entityRadius;
                maxX = entityPos.x + entityRadius;
            }

            tempCollisionTree.addEntity(entity, minX);
            tempCollisionTree.addEntity(entity, maxX);
        }

        return tempCollisionTree;
    }

    private Container<Entity> concatenate(CollisionNode node){
        int order = node.getOrder();
        Container<Entity> accumulate = new Container<>(Entity.class);

        for (int i = 0; i < 8; i++){
            if (node.getBit(i)==1){
                if (order > 0){
                    accumulate.add(concatenate(node.getBranch(i)));
                } else {
                    accumulate.add(node.getLeafData(i));
                }
            }
        }
        return accumulate;
    }

    private Container<Entity>[] buildCollisionGroups(Container concatenated){
        Container<Entity> seenEntities = new Container<>(Entity.class);

        Container<Entity>[] possibleCollisions = new Container[balls.length];
        for (int i = 0; i < balls.length; i++){
            possibleCollisions[i] = new Container<>(Entity.class);
        }

        for (int i = 0; i < concatenated.getSparseSize(); i++){
            Entity entity = (Entity) concatenated.get(i);
            if (!(seenEntities.contains(entity))){
                possibleCollisions[getIndex(balls, entity)].add(seenEntities);
                seenEntities.add(entity);
            } else {
                seenEntities.remove(entity);
            }
        }
        return possibleCollisions;
    }

    private Container<Entity>[] refineCollisions(boolean narrowPhase, Container<Entity>[] possibleCollisions){
        int collisions = 0;
        boolean noCollideOccurred = false;
        //Scan over entities to refine possible collisions (or check verified ones)
        for (int index = 0; index < possibleCollisions.length; index++){
            Container<Entity> collisionsToCheck = new Container<>(Entity.class);

            Entity entity = balls[index];
            //Scan entity's possible collisions and check:
            //     1.) if they are worth checking more precisely (broad phase)
            //     2.) overlapping (narrow phase)
            for (int i = 0; i < possibleCollisions[index].getSize(); i++){
                Entity otherEntity = possibleCollisions[index].get(i);

                //Check if the entities are no-collided with each other
                if (narrowPhase && entitiesNoCollided(entity, otherEntity)){
                    noCollideOccurred = true;
                    continue;
                }

                //Check for a collision between the entities (broad or narrow)
                if (collisionOccurred(narrowPhase, entity, otherEntity, index)){
                    if (narrowPhase){
                        resolveCollisions(entity,otherEntity); //TODO:
                    }
                    collisionsToCheck.add(otherEntity);
                    collisions++;
                }
            }
            possibleCollisions[index] = collisionsToCheck;
        }
        try {
            if (!noCollideOccurred && narrowPhase) {
                for (int i = 0; i < balls.length; i++) {
                    Entity entity = balls[i];
                    if (entity.contains(components.getFromClasses(NoCollide.class))) {
                        noCollide.set(entity, -1);
                    }
                }
            }
        } catch (NullPointerException e ){
            //do nothing
        }

        //Return collisions if any occurred, else return empty
        if (collisions>0){
            return possibleCollisions;
        } else {
            return new Container[0];
        }
    }

    private boolean collisionOccurred(boolean narrowPhase, Entity e1, Entity e2, int e1Index){
        boolean e1IsPolygon = false, e2IsPolygon = false;
        float e1MaxY = 0, e1MinY = 0;
        float e2MaxY = 0, e2MinY = 0;
        Vector3f e1Pos = new Vector3f(0,0,0);
        Vector3f e2Pos = new Vector3f(0,0,0);
        int e1Radius = 0, e2Radius = 0;
        util.Polygon poly = null;

        //Check type of e1
        if (e1.contains(components.getFromClasses(Polygon.class))) {
            e1IsPolygon = true;
            poly = shape.get(e1);
            e1MinY = poly.minY;
            e1MaxY = poly.maxY;
        } else {
            e1Pos = position.get(e1);
            e1Radius = radius.get(e1);
            e1MinY = e1Pos.y - e1Radius;
            e1MaxY = e1Pos.y + e1Radius;
        }

        //Check type of e2
        if (e2.contains(components.getFromClasses(Polygon.class))) {
            e2IsPolygon = true;
            poly = shape.get(e2);
            e2MinY = poly.minY;
            e2MaxY = poly.maxY;
        } else {
            e2Pos = position.get(e2);
            e2Radius = radius.get(e2);
            e2MinY = e2Pos.y - e2Radius;
            e2MaxY = e2Pos.y + e2Radius;
        }

        //Broad phase, only use bounding boxes
        if (!narrowPhase && (!e1IsPolygon || !e2IsPolygon)){
            return (!((e2MinY>e1MaxY)||(e2MaxY<e1MinY)));
        }
        //Both entities are circles,
        //see if they overlap
        if (narrowPhase && !e1IsPolygon && !e2IsPolygon){
            return circleIsInCircle(e1Radius, e2Radius, e1Pos, e2Pos);
        }

        return false;
    }

    private Vector3f checkWorldEdges(Entity entity, Vector3f entityPos, float minX, float maxX, float minY, float maxY, int worldHeight, int worldWidth){
        Vector3f indexVel;
        Vector3f newPos = entityPos;

        float leftEdge = (float)-worldWidth/2;
        float rightEdge = (float)worldWidth/2;
        float topEdge = (float)worldHeight/2;
        float bottomEdge = (float)-worldHeight/2;

        if (minY <= bottomEdge) {
            indexVel = velocity.get(entity);
            velocity.set(entity, new Vector3f(indexVel.x, -indexVel.y, indexVel.z));
            if (minY <= bottomEdge) {
                newPos.y = entityPos.y + (bottomEdge - minY);
                position.set(entity, new Vector3f(newPos.x, newPos.y, newPos.z));
            } else {
                newPos.y = entityPos.y - (maxY - topEdge);
                position.set(entity, new Vector3f(newPos.x, newPos.y, newPos.z));
            }

        } else {
            if (minX <= leftEdge || maxX >= rightEdge) {
                indexVel = velocity.get(entity);
                velocity.set(entity, new Vector3f(-indexVel.x, indexVel.y, indexVel.z));
                if (minX <= leftEdge) {
                    newPos.x = entityPos.x + (leftEdge - minX);
                    position.set(entity, new Vector3f(newPos.x, newPos.y, newPos.z));
                } else {
                    newPos.x = entityPos.x - (maxX - rightEdge);
                    position.set(entity, new Vector3f(newPos.x, newPos.y, newPos.z));
                }
            }
        }

        return newPos;
    }

    private boolean circleIsInCircle(int e1Radius, int e2Radius, Vector3f e1Pos, Vector3f e2Pos){
        return (Math.pow(e1Radius + e2Radius,2) > Math.pow(e1Pos.x - e2Pos.x,2) + Math.pow(e1Pos.y - e2Pos.y,2));
    }

    private boolean entitiesNoCollided(Entity e1, Entity e2){
        if (!e1.contains(components.getFromClasses(Polygon.class)) && !e2.contains(components.getFromClasses(Polygon.class))) {
            if (e1.contains(components.getFromClasses(NoCollide.class)) && e2.contains(components.getFromClasses(NoCollide.class))) {
                int e1NoCollide = noCollide.get(e1);
                int e2NoCollide = noCollide.get(e2);
                if (e1NoCollide >= 0 && e2NoCollide >= 0)
                    return (e1NoCollide == e2NoCollide);
            }
        }
        return false;
    }

    private void resolveCollisions(Entity entity, Entity otherEntity){
        if (entity==otherEntity)
            return;

        boolean entityIsBall = entity.contains(components.getFromClasses(Radius.class));
        boolean otherEntityIsBall = otherEntity.contains(components.getFromClasses(Radius.class));

       // if (entityIsBall && otherEntityIsBall)
            //resolveBallOnBall(entity, otherEntity);
        //else
            //resolveBallOnPoly(entity, otherEntity);
    }


    private int getIndex(Entity[] arr, Entity entity) {
        for (int i = 0; i < arr.length; i++)
            if (arr[i] == entity)
                return i;
        return -1;
    }

    @Override
    public void exit() {

    }
}
