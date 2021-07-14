package ecs;

public class Entity {
    private int entityId;
    private int componentMask;


    public Entity(int entityId, int components){
        this.entityId = entityId;

        this.componentMask = components;
    }


    public int getEntityId(){
        return entityId;
    }


    public int getComponents(){
        return componentMask;
    }


    public boolean contains(int compareMask){
        if (compareMask == 0)
            return false;
        return (componentMask & compareMask) == compareMask;
    }


    void addComponent(int addedComponents){
        this.componentMask |= addedComponents;
    }


    void removeComponent(int removedComponents){
        this.componentMask &= (~removedComponents);
    }

}
