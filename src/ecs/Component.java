package ecs;

import util.BitMask;
import util.ETree.IndexNode;

import java.lang.reflect.Field;

public class Component {
    private IndexNode indexTree;
    private int lastWriteIndex;


    public Component(){
        indexTree = new IndexNode(3);
    }


    public void addEntity(int entity, int index){
        indexTree.addItem(entity, index);
    }

    public void addEntity(int entity){
        indexTree.addItem(entity,0);
    }

    public void removeEntity(int entity) {
        indexTree.removeItem(entity);
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
