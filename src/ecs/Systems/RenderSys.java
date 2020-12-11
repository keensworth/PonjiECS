package ecs.Systems;

import ecs.Components.*;
import ecs.Entity;
import ecs.System;
import graphic.Renderer;
import graphic.Window;
import util.ComponentMask;
import util.ETree.EntNode;

public class RenderSys extends System {

    private Input input;
    private Position position;
    private Rotation rotation;
    private Health health;
    private Radius radius;

    private int[] ballHealthIndices, ballPositionIndices, ballRadiusIndices;

    private Entity[] camera;
    private Entity[] balls;

    private Window window;
    private Renderer renderer;

    public RenderSys(int width, int height){
        window = new Window("Ponji", width, height, true);
        window.init();

        renderer = new Renderer();
        renderer.init(window);
    }

    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask componentMask, boolean entityChange) {
        java.lang.System.out.println("Updating RenderSys");

        updateValues(dt, entityTree, componentMask, entityChange);

        float[] cameraPos = position.getPosition(position.getEntityIndex(camera[0].getEntityId()));
        float[] cameraRot = rotation.getRotation(rotation.getEntityIndex(camera[0].getEntityId()));

        renderer.prepare(window, cameraPos, cameraRot);
        //renderer.defaultRender();
        //renderer.renderExample((float) 0);
        //renderScene();
        renderBalls();
        //renderHUD();

        window.update();
        return null;
    }


    private void renderBalls(){
            renderer.renderCircles(balls, position, health, radius, ballPositionIndices, ballRadiusIndices, ballHealthIndices);
    }

    private void updateValues(float dt, EntNode entityTree, ComponentMask componentMask, boolean entityChange){
        balls = getEntities(entityTree, new Class[]{Radius.class});
        camera = getEntities(entityTree, new Class[]{Camera.class});

        health = (Health) componentMask.getComponent(Health.class);
        position = (Position) componentMask.getComponent(Position.class);
        radius = (Radius) componentMask.getComponent(Radius.class);
        rotation = (Rotation) componentMask.getComponent(Rotation.class);
        input = (Input) componentMask.getComponent(Input.class);

        ballHealthIndices = getComponentIndices(Health.class, balls, componentMask);
        ballPositionIndices = getComponentIndices(Position.class, balls, componentMask);
        ballRadiusIndices = getComponentIndices(Radius.class, balls, componentMask);
    }

    public Window getWindow(){
        return window;
    }

    @Override
    public void exit() {

    }
}

