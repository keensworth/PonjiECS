package ecs;

import ecs.Events.EventManager;
import graphic.Window;
import util.ComponentMask;
import util.ETree.EntNode;

import java.util.LinkedList;
import java.util.List;

public class ECS {

    private ComponentMask componentPool;
    private EntNode entityTree;

    private int itEntityId = 1;

    private List<System> systems = new LinkedList<>();
    private System renderer;
    public Window window;

    private Class requireSystem = null;

    public EventManager eventManager;

    public List<Entity> entityRemove = new LinkedList<>();
    private List<Entity> entityAdd = new LinkedList<>();

    public int width;
    public int height;

    /**
     * ECS constructor
     *
     * @param width width of game area
     * @param height height of game area
     */
    public ECS(int width, int height){
        entityTree = new EntNode(4);
        componentPool = new ComponentMask();
        this.width = width;
        this.height = height;
        window = null;
    }

    /**
     * Central ECS pipeline that adds or removes entities and updates and updates systems
     *
     * @param delta time step in milliseconds
     */
    public void update(final float delta) {
        //adds/removes entities to entityTree
        if (!entityAdd.isEmpty() || !entityRemove.isEmpty()) {
            for (Entity entity : entityAdd) {
                entityTree.addEntity(entity);
            }
            for (Entity entity : entityRemove)
                entityTree.removeEntity(entity);
        }

        entityAdd.clear();
        entityRemove.clear();

        //updates all systems
        for (System system : systems) {
            if (requireSystem == null || system.getClass() == requireSystem) {
                requireSystem = system.update(delta, entityTree, componentPool);
            }
        }
        renderer.update(delta, entityTree, componentPool);
        eventManager.update();
    }


    /**
     * Add system(s) to the ECS
     *
     * @param systems System(s) to add
     */
    public void addSystem(System... systems) {
        for (System system : systems) {
            system.setECS(this);
            system.setMask(componentPool);

            this.systems.add(system);
        }
    }

    /**
     * Add a renderer to the ECS
     *
     * @param renderer designated rendering system
     */
    public void addRenderer(System renderer){
        this.renderer = renderer;
        renderer.setECS(this);
        renderer.setMask(componentPool);
    }

    public void addEventManager(EventManager eventManager){
        this.eventManager = eventManager;
    }

    /**
     * Add component(s) to the ECS
     *
     * @param components Component(s) to add
     */
    public void addComponent(Component... components) {
        componentPool.addComponent(components);
    }

    /**
     * Creates an entity with set components to the ECS
     *
     * @param components Component(s) given to the new entity
     * @return newly created entity
     */
    public Entity createEntity(Component... components) {
        int entityComponents = componentPool.get(components);
        //java.lang.System.out.println(Integer.toBinaryString(entityComponents));
        java.lang.System.out.println("> > ECS.class - Entity E"+itEntityId+" added");
        Entity entity = new Entity(itEntityId++, entityComponents);

        for (Component component : components){
            component.addEntity(entity.getEntityId(), component.getLastWriteIndex());
        }

        entityAdd.add(entity);
        return entity;
    }

    /**
     * Removes an entity from the ECS
     *
     * @param entity entity to be removed
     * @return removed entity
     */
    public Entity destroyEntity(Entity entity)  {
        Component[] entityComponents = componentPool.getComponents(entity.getComponents());

        for (Component component : entityComponents)
            component.removeEntity(entity.getEntityId());

        java.lang.System.out.println("> > ECS.class - Entity E"+entity.getEntityId()+" destroyed");
        entityRemove.add(entity);

        return entity;
    }

    /**
     * Update an entity's components
     *
     * @param entity entity to be updated
     * @param components Component(s) to add to the entity
     */
    public void addEntityComponent(Entity entity, Component... components){
        entityTree.removeEntity(entity);
        for (Component component : components){
            component.addEntity(entity.getEntityId(), component.getLastWriteIndex());
            entity.addComponent(componentPool.get(components));
        }
        entityTree.addEntity(entity);
    }

    /**
     * Update an entity's components
     *
     * @param entity entity to be updated
     * @param components Component(s) to be removed from the entity
     */
    public void removeEntityComponent(Entity entity, Component... components){
        entityTree.removeEntity(entity);
        for (Component component : components){
            component.removeEntity(entity.getEntityId());
            entity.removeComponent(componentPool.get(components));
        }
        entityTree.addEntity(entity);
    }

    public ComponentMask getComponentPool(){
        return componentPool;
    }

    public void exit() {
        for (System system : systems)
            system.exit();
        renderer.exit();
    }

    public void setWindow(Window window){
        this.window = window;
    }

    public Window getWindow(){
        return window;
    }

}
