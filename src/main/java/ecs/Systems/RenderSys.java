package ecs.Systems;

import ecs.Components.*;
import ecs.Entity;
import ecs.System;
import graphic.AssetManager;
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
    private World world;

    private Entity[] camera;
    private Entity[] balls;

    private Window window;
    AssetManager assetManager;
    private Renderer renderer;

    public RenderSys(int width, int height, AssetManager assetManager){
        window = new Window("Ponji", width, height, true);
        window.init();
        renderer = new Renderer(assetManager);
        renderer.init(window);
    }

    @Override
    public Class update(float dt, EntNode entityTree, ComponentMask components) {
        java.lang.System.out.println("Updating RenderSys");

        updateValues(dt, entityTree, components);

        float[] cameraPos = position.getPosition(position.getEntityIndex(camera[0].getEntityId()));
        float[] cameraRot = rotation.getRotation(rotation.getEntityIndex(camera[0].getEntityId()));

        renderer.prepare(window, cameraPos, cameraRot);
        renderScene(entityTree, components);
        renderBalls(entityTree, components);
        //renderParticles();
        //renderHUD();

        window.update();
        return null;
    }


    private void renderBalls(EntNode entities, ComponentMask components){
            renderer.renderCircles(entities, components);
    }

    private void renderScene(EntNode entities, ComponentMask components){
        renderer.renderScene(entities, components);
    }

    private void updateValues(float dt, EntNode entityTree, ComponentMask components){
        balls = getEntities(entityTree, new Class[]{Radius.class});
        camera = getEntities(entityTree, new Class[]{Camera.class});

        health = (Health) components.getComponent(Health.class);
        position = (Position) components.getComponent(Position.class);
        radius = (Radius) components.getComponent(Radius.class);
        rotation = (Rotation) components.getComponent(Rotation.class);
        input = (Input) components.getComponent(Input.class);
        world = (World)components.getComponent(World.class);
    }

    public Window getWindow(){
        return window;
    }

    @Override
    public void exit() {

    }
}

