package ecs;

import util.BitMask;
import util.Container;
import util.ETree.EntNode;
import util.ETree.IndexNode;

import static java.lang.Integer.toBinaryString;

public abstract class System {
    public ECS ecs;
    private int bitMask;
    private Class[] components;

    public System(Class... components){
        this.components = components;
    }


    public abstract Class update(float dt, EntNode entityTree, BitMask componentMask, boolean entityChange);
    public abstract void exit();

    /**
     * Gets entities containing this system's required components
     *
     * @param entityTree EntNode tree that stores entities by components
     * @return Entity array of entities in tree that match required components
     */
    public Entity[] getEntities(EntNode entityTree){
        boolean empty = true;
        Container<Entity> entities =  entityTree.getEntities(bitMask);
        //java.lang.System.out.println("--------System.class" + toBinaryString(bitMask));

        int buildIndex = 0;
        Entity[] entityArray = new Entity[entities.getSparseSize()];
        //java.lang.System.out.println(entityArray.length + " System.class/ entityArray.length");

        for (int index = 0; index< entityArray.length; index++){
            Entity entity = entities.get(index);

            if (entity!=null){
                entityArray[buildIndex] = entity;
                buildIndex++;
                empty = false;
            }
        }

        if (empty){
            return new Entity[0];
        } else {
            return entityArray;
        }
    }

    /**
     * Gets entities containing desired components
     *
     * @param entityTree EntNode tree that stores entities by components
     * @param components Class array of desired components
     * @return Entity array of entities in tree that match required components
     */
    public Entity[] getEntities(EntNode entityTree, Class[] components){
        boolean empty = true;
        Container<Entity> entities =  entityTree.getEntities(ecs.getComponentPool().getFromClasses(components));

        int buildIndex = 0;
        Entity[] entityArray = new Entity[entities.getSparseSize()];

        for (int index = 0; index< entityArray.length; index++){
            Entity entity = entities.get(index);

            if (entity!=null){
                entityArray[buildIndex] = entity;
                buildIndex++;
                empty = false;
            }
        }

        if (empty){
            return new Entity[0];
        } else {
            return entityArray;
        }
    }

    /**
     * Retrieves the indices of entities in a certain component
     *
     * @param componentClass Class of desired component
     * @param entities array of entities to get the indices of
     * @param componentMask BitMask to check if entity contains particular component
     * @return array of indices of entities that contain the component
     */
    public int[] getComponentIndices(Class componentClass, Entity[] entities, BitMask componentMask){
        Component component = componentMask.getComponent(componentClass);
        int[] indices = new int[entities.length];

        for (int scan = 0; scan < entities.length; scan++){
            if (entities[scan]!=null && entities[scan].contains(componentMask.getFromClasses(componentClass))) {
                try {
                    indices[scan] = component.getEntityIndex(entities[scan].getEntityId());
                } catch (NullPointerException e){}
            }
        }

        return indices;
    }


    public void setMask(BitMask componentPool){
        bitMask = componentPool.getFromClasses(components);
    }

    public void updateMask(int mask){ bitMask = mask; }


    public int getMask(){ return bitMask; }


    void setECS(ECS ecs){
        this.ecs = ecs;
    }

    public ECS getECS(){
        return ecs;
    }

}
