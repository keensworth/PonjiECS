package util.nodes;

import ecs.Entity;
import util.Container;

public class EntNode extends ENode {
    //--------Node Data----------//
    //---(data in each node)-----//
    private int entQuantity;

    //--------Branching Array--------//
    //--(array to hold more ?Nodes)--//
    private EntNode[] branch;


    //----------Leaf Array-----------//
    //(array to hold contiguous data)//
    private Container<Entity>[] leaf;


    /**
     * Initialize a new EntNode. If the order is:
     *         1+ - create a branch node
     *         0  - create a data node
     *
     * @param order the level of the node
     */
    public EntNode(int order){
        this.order = (byte) order;
        if (order>0) {
            this.branch = new EntNode[8];
        } else {
            leaf = new Container[8];
            for (int i = 0; i<8; i++){
                leaf[i] = new Container(Entity.class);
            }
        }
    }


    public void buildBranch(int index, int order){
        this.branch[index] = new EntNode(order-1);
    }

    public EntNode getBranch(int index){ return this.branch[index]; }

    public void setBranch(int index, EntNode node){
        this.branch[index] = node;
        this.branch[index].setOrder((byte)(this.order-1));
    }

    public void addEntity(Entity entity){
        this.changeItem(entity,true);
    }
    
    public void removeEntity(Entity entity){
        this.changeItem(entity,false);
    }

    /**
     * Add or remove an entity, and indexes it by its components.
     * Used to store all entities in the ECS
     *
     * @param entity entity to be added/removed
     * @param add boolean determining addition or removal
     */
    public void changeItem(Entity entity, boolean add){
        int componentIndex;
        int componentMask = entity.getComponents();
        EntNode tempNode = this;
        for (int order = this.order; order > 0; order--){
            componentIndex = subIndex(componentMask,order);
            
            if (tempNode.getBranch(componentIndex)==null){
                tempNode.buildBranch(componentIndex, order);
                tempNode.setBit((componentIndex),1);
            }
            
            tempNode = tempNode.getBranch(componentIndex);
        }
        componentIndex = subIndex(componentMask, tempNode.getOrder());
        if (add){
            tempNode.addLeafItem(componentIndex, entity);
        } else {
            tempNode.removeLeafItem(componentIndex, entity);
        }
    }

    /**
     * Gets the entities from the tree matching the bitMask's components.
     * Used to retrieve entities have particular components
     *
     * @param bitMask integer bitmask of components to filter
     * @return Container of entities matching bitmask's components
     */
    public Container<Entity> getEntities(int bitMask){
        int componentIndex = subIndex(bitMask,order);
        Container<Entity> container = new Container(Entity.class);

        for (int index = 0; index < 8; index++) {
            if (order>0){
                if ((componentIndex & index) == componentIndex && this.getBranch(index) != null) {
                    container.add(this.getBranch(index).getEntities(bitMask));
                }
            } else {
                if ((componentIndex&index)==componentIndex && this.getLeafData(index).getSize()!=0){
                    container.add(this.getLeafData(index));
                }
            }
        }
        return container;
    }

    public int getData(){ return this.entQuantity; }

    public void setData(int data){ this.entQuantity = data; }

    public void incData(){ this.entQuantity++;}

    public void decData(){ this.entQuantity--;}

    public Container<Entity> getLeafData(int index){
        return leaf[index];
    }

    int getLeafID(int leafIndex, int itemIndex){
        return leaf[leafIndex].get(itemIndex).getEntityId();
    }
    int getLeafComponents(int leafIndex, int itemIndex) { return leaf[leafIndex].get(itemIndex).getComponents(); }

    void setLeafData(int leafIndex, Container container){
        leaf[leafIndex] = container;
    }

    void addLeafItem(int leafIndex, Entity item){
        leaf[leafIndex].add(item);
    }

    void removeLeafItem(int leafIndex, Entity item){
        leaf[leafIndex].remove(item);
    }

    boolean leafContains(int leafIndex, Entity item){
        return leaf[leafIndex].contains(item);
    }

    public static byte subIndex(int number, int order){
        return (byte)((number>>>(order*3))&0b111);
    }
}
