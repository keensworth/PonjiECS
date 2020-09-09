package ecs.Systems;

import ecs.Components.*;
import ecs.Entity;
import ecs.System;
import util.BitMask;
import util.Container;
import util.ETree.CollisionNode;
import util.ETree.EntNode;
import util.Geometry;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

public class CollisionSys extends System {
    private Entity[] entities;

    private BitMask components;
    private Position position;
    private Velocity velocity;
    private Radius radius;
    private Health health;
    private NoCollide noCollide;
    private Polygon polygon;

    private int[] entityPositionIndices;
    private int[] ballVelocityIndices;
    private int[] ballRadiusIndices;

    
    public CollisionSys() {
        super(Position.class, Radius.class);
    }

    @Override
    public Class update(float dt, EntNode entityTree, BitMask componentMask, boolean indexChange) {
        java.lang.System.out.println("Updating CollisionSys");
        components = componentMask;
        int worldWidth = this.getECS().width;
        int worldHeight = this.getECS().height;

        //entities = getEntities(entityTree);
        entities = getEntities(entityTree, new Class[]{Position.class});

        entityPositionIndices = getComponentIndices(Position.class, entities, componentMask);
        ballVelocityIndices = getComponentIndices(Velocity.class, entities, componentMask);
        ballRadiusIndices = getComponentIndices(Radius.class, entities, componentMask);

        position = (Position) componentMask.getComponent(Position.class);
        health = (Health) componentMask.getComponent(Health.class);
        velocity = (Velocity) componentMask.getComponent(Velocity.class);
        radius = (Radius) componentMask.getComponent(Radius.class);
        noCollide = (NoCollide) componentMask.getComponent(NoCollide.class);
        polygon = (Polygon) componentMask.getComponent(Polygon.class);

        //traverse entities, build collision tree
        CollisionNode collisionTree = buildCollisionTree(componentMask, worldWidth, worldHeight);

        //traverse collision tree, collect values
        Container<Entity> concatenated = concatenate(collisionTree);

        //build refined collision container
        Container<Entity>[] possibleCollisions = buildCollisionGroups(concatenated);

        //Broad Phase analysis of collision container
        Container<Entity>[] broadPhase = refineCollisions(false, possibleCollisions);

        //Narrow Phase analysis of collision container
        Container<Entity>[] narrowPhase = refineCollisions(true, broadPhase);
        if (narrowPhase.length==0) {
            return MovementSys.class;
        }

        return null;
    }

    private CollisionNode buildCollisionTree(BitMask componentMask, int worldWidth, int worldHeight){
        CollisionNode tempCollisionTree = new CollisionNode(worldWidth, true, -1);

        //Build collision tree w/ entities
        for (int index = 0; index < entities.length; index++){
            Entity entity = entities[index];

            float minX, maxX, minY, maxY;

            if (entity.contains(componentMask.get(polygon))) {
                java.awt.Polygon objectPoly = polygon.getPolygon(polygon.getEntityIndex(entity.getEntityId()));

                Rectangle2D boundingBox = objectPoly.getBounds2D();

                minX = (float) boundingBox.getMinX();
                maxX = (float) boundingBox.getMaxX();
                //java.lang.System.out.print("Polygon: ");

            } else {
                float[] entityPos = position.getPosition(entityPositionIndices[index]);
                int entityRadius = radius.getRadius(ballRadiusIndices[index]);

                minX = entityPos[0] - entityRadius;
                maxX = entityPos[0] + entityRadius;
                minY = entityPos[1] - entityRadius;
                maxY = entityPos[1] + entityRadius;

                entityPos = checkWorldEdges(index, entityPos, minX, maxX, minY, maxY, worldHeight, worldWidth);
                minX = entityPos[0] - entityRadius;
                maxX = entityPos[0] + entityRadius;
                //java.lang.System.out.print("Circle " + entity.getEntityId() + ": ");
            }
            //java.lang.System.out.print(minX + " " + maxX + "\n");

            tempCollisionTree.addEntity(entity, minX);
            tempCollisionTree.addEntity(entity, maxX);
        }

        return tempCollisionTree;
    }

    private Container<Entity> concatenate(CollisionNode node){
        int order = node.getOrder();
        Container<Entity> accumulate = new Container(Entity.class);

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
        Container<Entity> seenEntities = new Container(Entity.class);

        Container<Entity>[] possibleCollisions = new Container[entities.length];
        for (int i = 0; i < entities.length; i++){
            possibleCollisions[i] = new Container(Entity.class);
        }

        for (int i = 0; i < concatenated.getSparseSize(); i++){
            Entity entity = (Entity) concatenated.get(i);
            if (!(seenEntities.contains(entity))){
                possibleCollisions[getIndex(entities, entity)].add(seenEntities);
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
            Container<Entity> collisionsToCheck = new Container(Entity.class);

            Entity entity = entities[index];
            //Scan entity's possible collisions and check:
            //     1.) if they are worth checking more precisely (broad phase)
            //     2.) overlapping (narrow phase)
            for (int i = 0; i < possibleCollisions[index].getSize(); i++){
                Entity otherEntity = possibleCollisions[index].get(i);
                //java.lang.System.out.println("Checking collision between " + entity.getEntityId() + " & " + otherEntity.getEntityId() + " | " + narrowPhase);

                //Check if the entities are no-collided with each other
                //java.lang.System.out.println(narrowPhase + ":");
                if (narrowPhase && entitiesNoCollided(entity, otherEntity)){
                    noCollideOccurred = true;
                    continue;
                }

                //Check for a collision between the entities (broad or narrow)
                if (collisionOccurred(narrowPhase, entity, otherEntity, index)){
                    if (narrowPhase){
                        resolveCollisions(entity,otherEntity);
                    } else {
                        collisionsToCheck.add(otherEntity);
                        collisions++;
                    }
                }
            }
            possibleCollisions[index] = collisionsToCheck;
        }
        try {
            if (!noCollideOccurred && narrowPhase) {
                for (int i = 0; i < entities.length; i++) {
                    Entity entity = entities[i];
                    if (entity.contains(components.getFromClasses(NoCollide.class))) {
                        noCollide.setNoCollide(noCollide.getEntityIndex(entity.getEntityId()), -1);
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
        float[] e1Pos = {0,0}, e2Pos = {0,0};
        int e1Radius = 0, e2Radius = 0;
        java.awt.Polygon poly = null;

        //Check type of e1
        if (e1.contains(components.getFromClasses(Polygon.class))) {
            e1IsPolygon = true;
            poly = polygon.getPolygon(polygon.getEntityIndex(e1.getEntityId()));
            e1MinY = (float) poly.getBounds2D().getMinY();
            e1MaxY = (float) poly.getBounds2D().getMaxY();
        } else {
            e1Pos = position.getPosition(entityPositionIndices[e1Index]);
            e1Radius = radius.getRadius(ballRadiusIndices[e1Index]);
            e1MinY = e1Pos[1] - e1Radius;
            e1MaxY = e1Pos[1] + e1Radius;
        }
        //java.lang.System.out.println("E1 - MinY: " + e1MinY + ", MaxY: " + e1MaxY);

        //Check type of e2
        if (e2.contains(components.getFromClasses(Polygon.class))) {
            e2IsPolygon = true;
            poly = polygon.getPolygon(polygon.getEntityIndex(e2.getEntityId()));
            e2MinY = (float) poly.getBounds2D().getMinY();
            e2MaxY = (float) poly.getBounds2D().getMaxY();
        } else {
            e2Pos = position.getPosition(position.getEntityIndex(e2.getEntityId()));
            e2Radius = radius.getRadius(ballRadiusIndices[e1Index]);
            e2MinY = e2Pos[1] - e2Radius;
            e2MaxY = e2Pos[1] + e2Radius;
        }
        //java.lang.System.out.println("E2 - MinY: " + e2MinY + ", MaxY: " + e2MaxY);

        //Broad phase, only use bounding boxes
        if (!narrowPhase && (!e1IsPolygon || !e2IsPolygon)){
            return (!((e2MinY>e1MaxY)||(e2MaxY<e1MinY)));
        }
        //Both entities are circles,
        //see if they overlap
        if (narrowPhase && !e1IsPolygon && !e2IsPolygon){
            return circleIsInCircle(e1Radius, e2Radius, e1Pos, e2Pos);
        }
        //One entity is a circle, the other a polygon,
        //see if they overlap
        if (narrowPhase && (e1IsPolygon ^ e2IsPolygon)){
            int radius;
            float[] pos;

            if (!e1IsPolygon){
                radius = e1Radius;
                pos = e1Pos;
            } else {
                radius = e2Radius;
                pos = e2Pos;
            }

            return circleIsInPolygon(poly, radius, pos);
        }

        return false;
    }

    private boolean circleIsInCircle(int e1Radius, int e2Radius, float[] e1Pos, float[] e2Pos){
        return (Math.pow(e1Radius + e2Radius,2) > Math.pow(e1Pos[0] - e2Pos[0],2) + Math.pow(e1Pos[1] - e2Pos[1],2));
    }

    private boolean circleIsInPolygon(java.awt.Polygon poly, int radius, float[] pos){
        //java.lang.System.out.println("--Checking circle-polygon");
        int[] xPoints = poly.xpoints;
        int[] yPoints = poly.ypoints;
        for (int side = 0; side < poly.npoints; side++){
            int x2, y2;
            int x1 = xPoints[side];
            int y1 = yPoints[side];

            // get side endpoints
            if (side==poly.npoints-1){
                x2 = xPoints[0];
                y2 = yPoints[0];
            } else {
                x2 = xPoints[side+1];
                y2 = yPoints[side+1];
            }

            // get length of the line
            float sideLength = Geometry.distanceFromPointToPoint(x1,y1,x2,y2);

            // get dot product of the line and circle
            float dot = (float) ( (Geometry.dotProduct(x1,y1,x2,y2,pos[0],pos[1])) / Math.pow(sideLength,2));
            //float dot = (float) (( ((pos[0]-x1)*(x2-x1)) + ((pos[1]-y1)*(y2-y1)) ) /  Math.pow(sideLength,2));

            // find the closest point on the line
            float projectionX = x1 - (dot * (x1-x2));
            float projectionY = y1 - (dot * (y1-y2));

            // is this point actually on the line segment?
            // if so keep going, but if not, return false
            boolean onSegment = linePoint(x1,y1,x2,y2, projectionX,projectionY,sideLength, pos);
            if (onSegment){
                // distance from circle to segment projection
                float distance = Geometry.distanceFromPointToPoint(projectionX, projectionY, pos[0], pos[1]);
                if (distance < radius){
                    //java.lang.System.out.println("---segment collision");
                    return true;
                }
            }

            // check endpoints
            boolean inside1 = Geometry.distanceFromPointToPoint(x1,y1,pos[0],pos[1]) < radius;
            boolean inside2 = Geometry.distanceFromPointToPoint(x2,y2,pos[0],pos[1]) < radius;
            if (inside1 || inside2) {
                //java.lang.System.out.println("---endpoint collision");
                return true;
            }
        }

        return false;
    }

    private boolean entitiesNoCollided(Entity e1, Entity e2){
        if (!e1.contains(components.getFromClasses(Polygon.class)) && !e2.contains(components.getFromClasses(Polygon.class))) {
            if (e1.contains(components.getFromClasses(NoCollide.class)) && e2.contains(components.getFromClasses(NoCollide.class))) {
                int e1NoCollide = noCollide.getNoCollide(noCollide.getEntityIndex(e1.getEntityId()));
                int e2NoCollide = noCollide.getNoCollide(noCollide.getEntityIndex(e2.getEntityId()));
                //java.lang.System.out.println("NoCollide check [" + e1.getEntityId() + ", " + e2.getEntityId() + "]  " + e1NoCollide + " " + e2NoCollide);
                if (e1NoCollide >= 0 && e2NoCollide >= 0)
                    return (e1NoCollide == e2NoCollide);
            }
        }
        return false;
    }

    private void resolveCollisions(Entity entity, Entity otherEntity){
        //java.lang.System.out.println("resolving collision between " + entity.getEntityId() + " & " + otherEntity.getEntityId());

        if (entity==otherEntity)
            return;

        boolean entityIsBall = entity.contains(components.getFromClasses(Radius.class));
        boolean otherEntityIsBall = otherEntity.contains(components.getFromClasses(Radius.class));
        //java.lang.System.out.println("---In resolution: " + entityIsBall + " " + otherEntityIsBall);

        if (entityIsBall && otherEntityIsBall)
            resolveBallOnBall(entity, otherEntity);
        else
            resolveBallOnPoly(entity, otherEntity);
    }

    private void resolveBallOnBall(Entity e1, Entity e2){
        //java.lang.System.out.println("--Resolving ball on ball");
        int e1Health = health.getHealth(health.getEntityIndex(e1.getEntityId()));
        int e2Health = health.getHealth(health.getEntityIndex(e2.getEntityId()));


        int healthDifference = Math.abs(e1Health - e2Health);

        if (destroyBothEntities(e1Health, e2Health, healthDifference)){
            this.ecs.destroyEntity(e1);
            this.ecs.destroyEntity(e2);
            playDeathSound();
            return;
        }

        boolean e1Healthier = e1Health > e2Health;
        boolean e1Moving = e1.contains(components.getFromClasses(Velocity.class));
        boolean e2Moving = e2.contains(components.getFromClasses(Velocity.class));
        boolean e1NoCollided = e1.contains(components.getFromClasses(NoCollide.class)) && (noCollide.getNoCollide(noCollide.getEntityIndex(e1.getEntityId()))>=0);
        boolean e2NoCollided = e2.contains(components.getFromClasses(NoCollide.class)) && (noCollide.getNoCollide(noCollide.getEntityIndex(e2.getEntityId()))>=0);

        //No Collision edge case
        if ((e1Moving && e2Moving && e1NoCollided && e2NoCollided) || (!e1NoCollided && e2NoCollided && e2Moving) || (e1NoCollided && !e2NoCollided & e1Moving)){
            if (e1Healthier){
                health.setHealth(health.getEntityIndex(e1.getEntityId()), healthDifference);
                this.ecs.destroyEntity(e2);
                playDeathSound();
            } else {
                health.setHealth(health.getEntityIndex(e2.getEntityId()), healthDifference);
                this.ecs.destroyEntity(e1);
                playDeathSound();
            }
            return;
        }

        //Normal case
        if (e1Healthier && e2Moving) {
            ballSplit(e1, e2, healthDifference, 2);
            this.ecs.destroyEntity(e1);
            this.ecs.destroyEntity(e2);
            playDeathSound();
        }
        else if (!e1Healthier && e1Moving) {
            ballSplit(e2, e1, healthDifference, 2);
            this.ecs.destroyEntity(e1);
            this.ecs.destroyEntity(e2);
            playDeathSound();
        }
        else {
            if (e1Moving) {
                health.setHealth(health.getEntityIndex(e1.getEntityId()), healthDifference);
                this.ecs.destroyEntity(e2);
                playDeathSound();
            }
            else if (e2Moving) {
                health.setHealth(health.getEntityIndex(e2.getEntityId()), healthDifference);
                this.ecs.destroyEntity(e1);
                playDeathSound();
            }
        }
    }

    private void resolveBallOnPoly(Entity e1, Entity e2){
        //java.lang.System.out.println("--Resolving ball on polygon");
        Entity ePoly;
        Entity eBall;

        if (e1.contains(components.getFromClasses(Polygon.class))){
            //java.lang.System.out.println(e2.getEntityId());
            ePoly = e1;
            eBall = e2;
            //java.lang.System.out.println(eBall.getEntityId());
        } else {
            ePoly = e2;
            eBall = e1;
        }

        java.awt.Polygon poly = polygon.getPolygon(polygon.getEntityIndex(ePoly.getEntityId()));
        int ballRadius = radius.getRadius(radius.getEntityIndex(eBall.getEntityId()));
        float[] ballPos = position.getPosition(position.getEntityIndex(eBall.getEntityId()));
        float[] ballVel = velocity.getVelocity(velocity.getEntityIndex(eBall.getEntityId()));

        int[] segmentX = new int[2];
        int[] segmentY = new int[2];
        float[] endpoint = new float[2];

        float projectionX;
        float projectionY;
        float distance = 0;
        for (int side = 0; side < poly.npoints; side++){
            int x2, y2;
            int x1 = poly.xpoints[side];
            int y1 = poly.ypoints[side];

            if (side==poly.npoints-1){
                x2 = poly.xpoints[0];
                y2 = poly.ypoints[0];
            } else {
                x2 = poly.xpoints[side+1];
                y2 = poly.ypoints[side+1];
            }

            // get length of the line
            float sideLength = Geometry.distanceFromPointToPoint(x1,y1,x2,y2);

            // get dot product of the line and circle
            float dot = (float) (Geometry.dotProduct(x1,y1,x2,y2,ballPos[0],ballPos[1]) / Math.pow(sideLength,2));

            // find the closest point on the line
            projectionX = x1 - (dot * (x1-x2));
            projectionY = y1 - (dot * (y1-y2));

            // is this point actually on the line segment?
            // if so keep going, but if not, return false
            boolean onSegment = linePoint(x1,y1,x2,y2, projectionX,projectionY,sideLength, ballPos);
            if (onSegment){
                distance = Geometry.distanceFromPointToPoint(projectionX, projectionY, ballPos[0], ballPos[1]);
                if (distance < ballRadius) {
                    segmentX = new int[]{x1, x2};
                    segmentY = new int[]{y1, y2};
                    break;
                }
            }

            // check endpoints, break if found
            boolean inside1 = (Geometry.distanceFromPointToPoint(x1,y1,ballPos[0],ballPos[1]) < ballRadius);
            boolean inside2 = (Geometry.distanceFromPointToPoint(x2,y2,ballPos[0],ballPos[1]) < ballRadius);
            if (inside1 || inside2) {
                if (inside1){
                    endpoint[0] = x1;
                    endpoint[1] = y1;
                } else {
                    endpoint[0] = x2;
                    endpoint[1] = y2;
                }
                segmentX = new int[]{x1,x2};
                segmentY = new int[]{y1,y2};
                break;
            }
        }

        float[] normal = new float[2];
        if(endpoint[0]!=0 && endpoint[1]!=0){
            //java.lang.System.out.println("~endpoint");
            projectionX = endpoint[0];
            projectionY = endpoint[1];
            distance = Geometry.distanceFromPointToPoint(projectionX, projectionY, ballPos[0], ballPos[1]);
            float magnitude = Geometry.magnitude(ballPos[0],ballPos[1],endpoint[0],endpoint[1]);
            normal[0] = (ballPos[0] - endpoint[0])/magnitude;
            normal[1] = (ballPos[1] - endpoint[1])/magnitude;
        } else {
            //java.lang.System.out.println("~segment");
            normal = Geometry.norm(segmentX[0],segmentY[0],segmentX[1],segmentY[1]);
            float temp = normal[0];
            normal[0] = -normal[1];
            normal[1] = temp;
        }


        float newXPos = (float) (ballPos[0] + Math.abs(ballRadius - distance + 0.5)*normal[0]);
        float newYPos = (float) (ballPos[1] + Math.abs(ballRadius - distance + 0.5)*normal[1]);

        float constant = 2*Geometry.dotProduct(ballVel[0],ballVel[1],normal[0],normal[1]);
        float newXVel = ballVel[0] - constant*normal[0];
        float newYVel = ballVel[1] - constant*normal[1];

        //java.lang.System.out.println(distance + " -- " + projectionX + ", " + projectionY);
        //java.lang.System.out.println(ballVel[0] + " " + ballVel[1] + ", " + newXVel + "  " + newYVel + " | " + normal[0] + " " + normal[1]);
        //java.lang.System.out.println(ballPos[0] + " " + ballPos[1] + ", " + newXPos + "  " + newYPos);
        position.setPosition(new float[]{newXPos,newYPos}, position.getEntityIndex(eBall.getEntityId()));
        velocity.setVelocity(new float[]{newXVel,newYVel}, velocity.getEntityIndex(eBall.getEntityId()));
    }

    private void ballSplit(Entity eSplit, Entity eReference, int totalHealth, int splitQuantity){
        //java.lang.System.out.println("--Splitting ball");
        float[] splitPos = position.getPosition(position.getEntityIndex(eSplit.getEntityId()));
        float[] referencePos = position.getPosition(position.getEntityIndex(eReference.getEntityId()));
        int eRadius = radius.getRadius(radius.getEntityIndex(eSplit.getEntityId()));

        float currentAngle = 0;
        float splitAngle = (float) ((2*3.14159265) / splitQuantity);
        if (splitQuantity%2==0){
            currentAngle += (splitAngle/2);
        }

        float[] velComp = getVelocityComponents(eSplit, eReference, splitPos, referencePos);
        int newHealth = totalHealth / splitQuantity;

        if (newHealth==0)
            return;

        int noCollideInstance = noCollide.increaseInstance();
        for (int k = 0; k < splitQuantity; k++){
            currentAngle += (splitAngle);

            //Calculate new velocity components for current split
            float newXVel = (float) (velComp[0]*Math.cos(currentAngle) - velComp[1]*Math.sin(currentAngle));
            float newYVel = (float) (velComp[0]*Math.sin(currentAngle) + velComp[1]*Math.cos(currentAngle));


            //Create new entity
            this.ecs.createEntity(
                    position.add(splitPos),
                    velocity.add(new float[]{newXVel, newYVel}),
                    radius.add(eRadius),
                    health.add(newHealth),
                    noCollide.add(noCollideInstance)
            );
        }
    }

    private boolean linePoint(float x1, float y1, float x2, float y2, float projX, float projY, float length, float[] pos){
        /*
        float distance1 = Geometry.distanceFromPointToPoint(x1,y1,projX,projY);
        float distance2 = Geometry.distanceFromPointToPoint(x2,y2,projX,projY);

        float sum = distance1+distance2;
        float error = (float) 0.1;
        java.lang.System.out.println(sum + " -- " + length);

        return sum < length + error && sum > length - error;

         */

        float error = (float) 0.05;
        float xProjSum = Math.abs(x2-projX) + Math.abs(x1-projX);
        float yProjSum = Math.abs(y2-projY) + Math.abs(y1-projY);
        float xSum = Math.abs(x2-x1);
        float ySum = Math.abs(y2-y1);

        boolean withinXBounds = (xProjSum < xSum + error) && (xProjSum > xSum - error);
        boolean withinYBounds = (yProjSum < ySum + error) && (yProjSum > ySum - error);

        //java.lang.System.out.println(xSum+error + " -- " + xProjSum + " | " + ySum + " -- " + yProjSum + " | " + "(" + x1 + "," + y1 + ") (" + x2 + "," + y2 + "), ball: (" + pos[0] + "," + pos[1] + ")");
        //java.lang.System.out.println(withinXBounds + " " + withinYBounds);
        return (withinXBounds && withinYBounds);
    }

    private boolean destroyBothEntities(int e1Health, int e2Health, int healthDifference){
        return ((healthDifference) / (float)(e1Health + e2Health)) < 0.05;
    }

    private float[] checkWorldEdges(int index, float[] entityPos, float minX, float maxX, float minY, float maxY, int worldHeight, int worldWidth){
        float[] indexVel;
        float[] newPos = entityPos;

        if (minY <= 0 || maxY >= worldHeight) {
            indexVel = velocity.getVelocity(ballVelocityIndices[index]);
            velocity.setVelocity(new float[]{indexVel[0], -indexVel[1]}, ballVelocityIndices[index]);
            if (minY <= 0) {
                newPos[1] = entityPos[1] - minY;
                position.setYPos(entityPositionIndices[index], newPos[1]);
            } else {
                newPos[1] = entityPos[1] + (worldHeight - maxY);
                position.setYPos(entityPositionIndices[index], newPos[1]);
            }

            //this.getECS().destroyEntity(entity);
        } else {
            if (minX <= 0 || maxX >= worldWidth) {
                indexVel = velocity.getVelocity(ballVelocityIndices[index]);
                velocity.setVelocity(new float[]{-indexVel[0], indexVel[1]}, ballVelocityIndices[index]);
                if (minX <= 0) {
                    newPos[0] = entityPos[0] - minX;
                    position.setXPos(entityPositionIndices[index], newPos[0]);
                } else {
                    newPos[0] = entityPos[0] + (worldWidth - maxX);
                    position.setXPos(entityPositionIndices[index], newPos[0]);
                }

                //this.getECS().destroyEntity(entity);
            }
        }

        return newPos;
    }

    private float[] getVelocityComponents(Entity eSplit, Entity eReference, float[] splitPos, float[] referencePos){
        float[] splitVel;
        if (eSplit.contains(components.getFromClasses(Velocity.class))) {
            splitVel = velocity.getVelocity(velocity.getEntityIndex(eSplit.getEntityId()));
        } else {
            splitVel = velocity.getVelocity(velocity.getEntityIndex(eReference.getEntityId()));
        }

        //Calculate scalar velocity from x and y components
        float splitVelocity = (float) Math.sqrt((splitVel[0] * splitVel[0]) + (splitVel[1] * splitVel[1]));

        //Set the new velocity components
        float[] velComp = new float[2];
        velComp[0] = splitPos[0] - referencePos[0];
        velComp[1] = splitPos[1] - referencePos[1];

        //Get norm
        float norm = (float) Math.sqrt((velComp[0] * velComp[0]) + (velComp[1] * velComp[1]));

        //Scale normalized vectors
        velComp[0] /= norm;
        velComp[1] /= norm;
        velComp[0] *= splitVelocity;
        velComp[1] *= splitVelocity;

        return velComp;
    }

    private int getIndex(Entity[] arr, Entity entity) {
        for (int i = 0; i < arr.length; i++)
            if (arr[i] == entity)
                return i;
        return -1;
    }

    private void playDeathSound(){
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("C:/Users/Sargy/IdeaProjects/PonjiECS/assets/pop2.wav").getCanonicalFile());
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
        }
    }

    @Override
    public void exit() {

    }
}
