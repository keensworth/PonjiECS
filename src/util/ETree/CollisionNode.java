package util.ETree;

import ecs.Entity;
import util.Container;

public class CollisionNode extends ENode {
    //--------Node Data----------//
    //---(data in each node)-----//

    //private ? data;
    //public ? getData(return this.data);
    //public void setData(? data);

    //--------Branching Array--------//
    //--(array to hold more ?Nodes)--//

    private CollisionNode[] branch;
    public CollisionNode getBranch(int index){
        return branch[index];
    }
    public void setBranch(int index, CollisionNode node){
        this.branch[index] = node;
    }


    //----------Leaf Array-----------//
    //(array to hold contiguous data)//

    private Container<Entity>[] leaf;
    public Container<Entity> getLeafData(int index){
        return leaf[index];
    }
    public void setLeafData(int index, Container entities){
        this.leaf[index] = entities;
    }
    public void addLeafData(int index, Entity entity){
        leaf[index].add(entity);
    }

    public CollisionNode(int worldWidth, boolean init, int order){
        if (init){
            this.order = (byte) Math.floor ((Math.log(worldWidth) / Math.log(8)) + 1);
        } else {
            this.order = (byte) (order-1);
        }

        if (this.order>0) {
            branch = new CollisionNode[8];
        } else {
            leaf = new Container[8];
            for (int i = 0; i<8; i++){
                leaf[i] = new Container(Entity.class);
            }
        }
    }

    public void buildBranch(int index){
        branch[index] = new CollisionNode(0,false, order);
    }

    public void addEntity(Entity entity, float xBound){
        int addIndex;
        int intXBound = (int) xBound;
        CollisionNode tempNode = this;

        for (int order = this.order; order > 0; order--){
            addIndex = subIndex(intXBound,order);
            if (tempNode.getBit(addIndex)==0){
                tempNode.buildBranch(addIndex);
            }
            tempNode.setBit(addIndex, 1);
            tempNode = tempNode.getBranch(addIndex);
        }

        addIndex = subIndex(intXBound,0);
        tempNode.setBit(addIndex, 1);
        tempNode.addLeafData(addIndex,entity);
    }

    public static byte subIndex(int number, int order){
        return (byte)((number>>>(order*3))&0b111);
    }

}
