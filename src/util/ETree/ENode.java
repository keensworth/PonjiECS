package util.ETree;

import java.lang.reflect.Array;

public abstract class ENode {
    //--------Node Data----------//
    //---(data in each node)-----//

    //private ? data;
    //public ? getData(return this.data);
    //public void setData(? data);

    //--------Branching Array--------//
    //--(array to hold more ?Nodes)--//

    //private ?Node[] branch;
    //public ?Node getBranch(int index);
    //public void setBranch(int index, ?Node node);


    //----------Leaf Array-----------//
    //(array to hold contiguous data)//

    //private ?[] leaf;
    //public ? getLeafData(int index);
    //public void setLeafData(int index, ? data);

    //Default data
    protected byte mask;
    protected byte order;

    ENode(){}

    public byte getMask(){
        return mask;
    }

    public byte getOrder() {
        return order;
    }

    public byte getBit(int bit){
        return (byte)((mask>>>bit)&0b1);
    }

    public void setMask(byte newMask){
        mask = newMask;
    }

    public void setOrder(byte newOrder){
        order = newOrder;
    }

    public void setBit(int bit, int setter){
        byte setterMask = (byte)((0b00000001)<<bit);
        if (setter==0){
            mask &= (setterMask^0xff);
        }
        else {
            mask |= setterMask;
        }
    }
}
