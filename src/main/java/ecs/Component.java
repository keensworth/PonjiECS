package ecs;

import util.Container;
import util.nodes.IndexNode;

import java.lang.reflect.Field;

public class Component {
    private IndexNode indexTree;
    private int lastWriteIndex;
    private Container data;

    public Component(){
        indexTree = new IndexNode(3);
    }

    public<T> void setContainer(Container<T> container){
        this.data = container;
    }

    public<T> T get(Entity entity){
        return (T) data.get(getEntityIndex(entity.getEntityId()));
    }

    public<T> void set(Entity entity, T data){
        this.data.set(getEntityIndex(entity.getEntityId()), data);
    }

    public<T> void add(T data){
        setLastWriteIndex(this.data.add(data));
    }

    /**
     * Adds an entity and its respective component index to the index tree
     *
     * @param entityID entity integer ID reference Entity object
     * @param index index of entity in the component
     */
    public void addEntity(int entityID, int index){
        indexTree.addItem(entityID, index);
    }

    /**
     * Removes an entity from the component's index tree
     *
     * @param entityID ID of entity to be removed
     */
    void removeEntity(int entityID) {
        indexTree.removeItem(entityID);
    }

    public int getEntityIndex(int entity){
        return indexTree.getIndex(entity);
    }

    public int getLastWriteIndex(){
        return lastWriteIndex;
    }

    public void setLastWriteIndex(int index){
        lastWriteIndex = index;
    }

    public void resetTree(){
        indexTree = new IndexNode(2);
    }



    @Override
    public String toString(){
        Field[] fields = this.getClass().getDeclaredFields();

        StringBuilder desc = new StringBuilder(super.toString() + ": {\n");
        for (Field field : fields) {
            desc.append("    ").append(field.getName()).append(": ");
            try {
                desc.append(field.get(this));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                desc.append("IAException");
            }
            desc.append("\n");
        }
        desc.append("}");

        return desc.toString();
    }

    public String getName(){
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        return getClass() == obj.getClass();
    }
}
