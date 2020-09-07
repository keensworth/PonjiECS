package util.ETree;

import ecs.Entity;
import util.Container;

import static java.lang.Integer.toBinaryString;

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

    public void changeItem(Entity entity, boolean add){
        int componentIndex;
        int componentMask = entity.getComponents();
        //System.out.println(toBinaryString(componentMask) + " entity mask");
        EntNode tempNode = this;
        for (int order = this.order; order > 0; order--){
            componentIndex = subIndex(componentMask,order);
            
            if (tempNode.getBranch(componentIndex)==null){
                tempNode.buildBranch(componentIndex, order);
                tempNode.setBit((componentIndex),1);
            }
            
            tempNode = tempNode.getBranch(componentIndex);
        }
        componentIndex = subIndex(componentMask,tempNode.getOrder());
        //System.out.println("------------------------------" + toBinaryString(componentIndex));
        if (add){
            tempNode.addLeafItem(componentIndex,entity);
        } else {
            tempNode.removeLeafItem(componentIndex, entity);
        }
    }

    public Container<Entity> getEntities(int bitMask){
        int componentIndex = subIndex(bitMask,order);
        Container<Entity> container = new Container(Entity.class);

        for (int index = 0; index < 8; index++) {
            if (order>0){
                //System.out.println(toBinaryString(componentIndex) + " " + toBinaryString(index) + " B " + order + " " + ((componentIndex&index)==componentIndex));
                if ((componentIndex & index) == componentIndex && this.getBranch(index) != null) {
                    container.add(this.getBranch(index).getEntities(bitMask));
                }
            } else {
                //System.out.println(toBinaryString(componentIndex) + " " + toBinaryString(index) + " N " + order + " " + ((componentIndex&index)==componentIndex) + this.getLeafData(index).getSize());
                if ((componentIndex&index)==componentIndex && this.getLeafData(index).getSize()!=0){
                    container.add(this.getLeafData(index));
                }
            }
        }
        //System.out.println("-----" + container.getSize());
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
        //System.out.println("ENTITY ADDED " + item.getEntityId() + " " + leafIndex + " " + order);
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
