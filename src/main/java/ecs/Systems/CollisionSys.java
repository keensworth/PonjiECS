package ecs.Systems;

import ecs.Components.*;
import ecs.Entity;
import ecs.System;
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

    private int[] entityPositionIndices;
    private int[] ballVelocityIndices;
    private int[] ballRadiusIndices;


    public CollisionSys() {
        super(Position.class, Radius.class);
    }

    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask componentMask, boolean indexChange) {
        java.lang.System.out.println("Updating CollisionSys");

        updateValues(dt, entityTree, componentMask, indexChange);

        //traverse entities, build collision tree
        CollisionNode collisionTree = buildCollisionTree(componentMask, this.ecs.width, this.ecs.height);

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

    private void updateValues(float dt, EntNode entityTree, ComponentMask componentMask, boolean indexChange){
        components = componentMask;
        int worldWidth = this.getECS().width;
        int worldHeight = this.getECS().height;

        balls = getEntities(entityTree, new Class[]{Radius.class});

        entityPositionIndices = getComponentIndices(Position.class, balls, componentMask);
        ballVelocityIndices = getComponentIndices(Velocity.class, balls, componentMask);
        ballRadiusIndices = getComponentIndices(Radius.class, balls, componentMask);

        position = (Position) componentMask.getComponent(Position.class);
        health = (Health) componentMask.getComponent(Health.class);
        velocity = (Velocity) componentMask.getComponent(Velocity.class);
        radius = (Radius) componentMask.getComponent(Radius.class);
        noCollide = (NoCollide) componentMask.getComponent(NoCollide.class);
        shape = (Shape) componentMask.getComponent(Shape.class);
        light = (Light) componentMask.getComponent(Light.class);
    }

    private CollisionNode buildCollisionTree(ComponentMask componentMask, int worldWidth, int worldHeight){
        CollisionNode tempCollisionTree = new CollisionNode(worldWidth, true, -1);

        //Build collision tree w/ entities
        for (int index = 0; index < balls.length; index++){
            Entity entity = balls[index];

            float minX, maxX, minY, maxY;

            if (entity.contains(componentMask.get(shape))) {
                util.Polygon objectPoly = shape.getShape(entity);

                minX = objectPoly.minX;
                maxX = objectPoly.maxX;

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
            }

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

        Container<Entity>[] possibleCollisions = new Container[balls.length];
        for (int i = 0; i < balls.length; i++){
            possibleCollisions[i] = new Container(Entity.class);
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
            Container<Entity> collisionsToCheck = new Container(Entity.class);

            Entity entity = balls[index];
            //Scan entity's possible collisions and check:
            //     1.) if they are worth checking more precisely (broad phase)
            //     2.) overlapping (narrow phase)
            for (int i = 0; i < possibleCollisions[index].getSize(); i++){
                Entity otherEntity = possibleCollisions[index].get(i);

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
                        noCollide.setNoCollide(entity, -1);
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
        util.Polygon poly = null;

        //Check type of e1
        if (e1.contains(components.getFromClasses(Polygon.class))) {
            e1IsPolygon = true;
            poly = shape.getShape(e1);
            e1MinY = poly.minY;
            e1MaxY = poly.maxY;
        } else {
            e1Pos = position.getPosition(entityPositionIndices[e1Index]);
            e1Radius = radius.getRadius(ballRadiusIndices[e1Index]);
            e1MinY = e1Pos[1] - e1Radius;
            e1MaxY = e1Pos[1] + e1Radius;
        }

        //Check type of e2
        if (e2.contains(components.getFromClasses(Polygon.class))) {
            e2IsPolygon = true;
            poly = shape.getShape(e2);
            e2MinY = poly.minY;
            e2MaxY = poly.maxY;
        } else {
            e2Pos = position.getPosition(e2);
            e2Radius = radius.getRadius(ballRadiusIndices[e1Index]);
            e2MinY = e2Pos[1] - e2Radius;
            e2MaxY = e2Pos[1] + e2Radius;
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

    private boolean circleIsInPolygon(Polygon poly, int radius, float[] pos){
        float[] xPoints = poly.xPoints;
        float[] yPoints = poly.yPoints;
        for (int side = 0; side < poly.sides; side++){
            float x2, y2;
            float x1 = xPoints[side];
            float y1 = yPoints[side];

            // get side endpoints
            if (side==poly.sides-1){
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
                    return true;
                }
            }

            // check endpoints
            boolean inside1 = Geometry.distanceFromPointToPoint(x1,y1,pos[0],pos[1]) < radius;
            boolean inside2 = Geometry.distanceFromPointToPoint(x2,y2,pos[0],pos[1]) < radius;
            if (inside1 || inside2) {
                return true;
            }
        }

        return false;
    }

    private boolean entitiesNoCollided(Entity e1, Entity e2){
        if (!e1.contains(components.getFromClasses(Polygon.class)) && !e2.contains(components.getFromClasses(Polygon.class))) {
            if (e1.contains(components.getFromClasses(NoCollide.class)) && e2.contains(components.getFromClasses(NoCollide.class))) {
                int e1NoCollide = noCollide.getNoCollide(e1);
                int e2NoCollide = noCollide.getNoCollide(e2);
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

        if (entityIsBall && otherEntityIsBall)
            resolveBallOnBall(entity, otherEntity);
        else
            resolveBallOnPoly(entity, otherEntity);
    }

    private void resolveBallOnBall(Entity e1, Entity e2){
        int e1Health = health.getHealth(e1);
        int e2Health = health.getHealth(e2);


        int healthDifference = Math.abs(e1Health - e2Health);

        if (destroyBothEntities(e1Health, e2Health, healthDifference)){
            this.ecs.destroyEntity(e1);
            this.ecs.destroyEntity(e2);
            return;
        }

        boolean e1Healthier = e1Health > e2Health;
        boolean e1Moving = e1.contains(components.getFromClasses(Velocity.class));
        boolean e2Moving = e2.contains(components.getFromClasses(Velocity.class));
        boolean e1NoCollided = e1.contains(components.getFromClasses(NoCollide.class)) && (noCollide.getNoCollide(e1)>=0);
        boolean e2NoCollided = e2.contains(components.getFromClasses(NoCollide.class)) && (noCollide.getNoCollide(e2)>=0);

        //No Collision edge case
        if ((e1Moving && e2Moving && e1NoCollided && e2NoCollided) || (!e1NoCollided && e2NoCollided && e2Moving) || (e1NoCollided && !e2NoCollided & e1Moving)){
            if (e1Healthier){
                health.setHealth(e1, healthDifference);
                this.ecs.destroyEntity(e2);
            } else {
                health.setHealth(e2, healthDifference);
                this.ecs.destroyEntity(e1);
            }
            return;
        }

        //Normal case
        if (e1Healthier && e2Moving) {
            ballSplit(e1, e2, healthDifference, 2);
            this.ecs.destroyEntity(e1);
            this.ecs.destroyEntity(e2);
        }
        else if (!e1Healthier && e1Moving) {
            ballSplit(e2, e1, healthDifference, 2);
            this.ecs.destroyEntity(e1);
            this.ecs.destroyEntity(e2);
        }
        else {
            if (e1Moving) {
                health.setHealth(e1, healthDifference);
                this.ecs.destroyEntity(e2);
            }
            else if (e2Moving) {
                health.setHealth(e2, healthDifference);
                this.ecs.destroyEntity(e1);
            }
        }
    }

    private void resolveBallOnPoly(Entity e1, Entity e2){
        Entity ePoly;
        Entity eBall;

        if (e1.contains(components.getFromClasses(Polygon.class))){
            ePoly = e1;
            eBall = e2;
        } else {
            ePoly = e2;
            eBall = e1;
        }

        Polygon poly = shape.getShape(ePoly);
        int ballRadius = radius.getRadius(eBall);
        float[] ballPos = position.getPosition(eBall);
        float[] ballVel = velocity.getVelocity(eBall);

        float[] segmentX = new float[2];
        float[] segmentY = new float[2];
        float[] endpoint = new float[2];

        float projectionX;
        float projectionY;
        float distance = 0;
        for (int side = 0; side < poly.sides; side++){
            float x2, y2;
            float x1 = poly.xPoints[side];
            float y1 = poly.yPoints[side];

            if (side==poly.sides-1){
                x2 = poly.xPoints[0];
                y2 = poly.yPoints[0];
            } else {
                x2 = poly.xPoints[side+1];
                y2 = poly.yPoints[side+1];
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
                    segmentX = new float[]{x1, x2};
                    segmentY = new float[]{y1, y2};
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
                segmentX = new float[]{x1,x2};
                segmentY = new float[]{y1,y2};
                break;
            }
        }

        float[] normal = new float[2];
        if(endpoint[0]!=0 && endpoint[1]!=0){
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

        position.setPosition(eBall, new float[]{newXPos,newYPos});
        velocity.setVelocity(eBall, new float[]{newXVel,newYVel});
    }

    private void ballSplit(Entity eSplit, Entity eReference, int totalHealth, int splitQuantity){
        //java.lang.System.out.println("--Splitting ball");
        float[] splitPos = position.getPosition(eSplit);
        float[] referencePos = position.getPosition(eReference);
        int eRadius = radius.getRadius(eSplit);

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
                    noCollide.add(noCollideInstance),
                    light.add(Light.POINT_LIGHT)
            );
        }
    }

    private boolean linePoint(float x1, float y1, float x2, float y2, float projX, float projY, float length, float[] pos){
        float error = (float) 0.05;
        float xProjSum = Math.abs(x2-projX) + Math.abs(x1-projX);
        float yProjSum = Math.abs(y2-projY) + Math.abs(y1-projY);
        float xSum = Math.abs(x2-x1);
        float ySum = Math.abs(y2-y1);

        boolean withinXBounds = (xProjSum < xSum + error) && (xProjSum > xSum - error);
        boolean withinYBounds = (yProjSum < ySum + error) && (yProjSum > ySum - error);

        return (withinXBounds && withinYBounds);
    }

    private boolean destroyBothEntities(int e1Health, int e2Health, int healthDifference){
        return ((healthDifference) / (float)(e1Health + e2Health)) < 0.05;
    }

    private float[] checkWorldEdges(int index, float[] entityPos, float minX, float maxX, float minY, float maxY, int worldHeight, int worldWidth){
        float[] indexVel;
        float[] newPos = entityPos;

        float leftEdge = (float)-worldWidth/2;
        float rightEdge = (float)worldWidth/2;
        float topEdge = (float)worldHeight/2;
        float bottomEdge = (float)-worldHeight/2;


        if (minY <= bottomEdge) {
            indexVel = velocity.getVelocity(ballVelocityIndices[index]);
            velocity.setVelocity(ballVelocityIndices[index], new float[]{indexVel[0], -indexVel[1]});
            if (minY <= bottomEdge) {
                newPos[1] = entityPos[1] + (bottomEdge - minY);
                position.setYPos(entityPositionIndices[index], newPos[1]);
            } else {
                newPos[1] = entityPos[1] - (maxY - topEdge);
                position.setYPos(entityPositionIndices[index], newPos[1]);
            }

        } else {
            if (minX <= leftEdge || maxX >= rightEdge) {
                indexVel = velocity.getVelocity(ballVelocityIndices[index]);
                velocity.setVelocity(ballVelocityIndices[index], new float[]{-indexVel[0], indexVel[1]});
                if (minX <= leftEdge) {
                    newPos[0] = entityPos[0] + (leftEdge - minX);
                    position.setXPos(entityPositionIndices[index], newPos[0]);
                } else {
                    newPos[0] = entityPos[0] - (maxX - rightEdge);
                    position.setXPos(entityPositionIndices[index], newPos[0]);
                }

            }
        }

        return newPos;
    }

    private float[] getVelocityComponents(Entity eSplit, Entity eReference, float[] splitPos, float[] referencePos){
        float[] splitVel;
        if (eSplit.contains(components.getFromClasses(Velocity.class))) {
            splitVel = velocity.getVelocity(eSplit);
        } else {
            splitVel = velocity.getVelocity(eReference);
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

    @Override
    public void exit() {

    }
}