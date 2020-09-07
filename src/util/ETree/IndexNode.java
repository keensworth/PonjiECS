package util.ETree;

public class IndexNode extends ENode{
    //--------Node Data----------//
    //---(data in each node)-----//
    //private int itemQuantity;

    //--------Branching Array--------//
    //--(array to hold more ?Nodes)--//
    private IndexNode[] branch;


    //----------Leaf Array-----------//
    //(array to hold contiguous data)//
    private int[] leaf;


    public IndexNode(int order){
        this.order = (byte)order;
        if (order>0){
            branch = new IndexNode[8];
        } else {
            this.leaf = new int[8];
        }
    }

    public void addItem(int index, int value){
        index--;
        while (Math.pow(8,this.order+1) < index){
            resizeTree();
        }

        IndexNode tempNode = this;

        for (int order = this.order; order>0; order--){
            int currIndex = subIndex(index,order);

            if (tempNode.getBranch(currIndex)==null){
                tempNode.buildBranch(currIndex, order);
            }

            tempNode.setBit((currIndex),1);
            tempNode = tempNode.getBranch(currIndex);
        }

        tempNode.setBit(subIndex(index, 0),1);
        tempNode.setLeafData(subIndex(index,0), value);
    }
    
    /*
    public void addItemAux(int index, int value){
        if (order>1){
            int currIndex = subIndex(index, order);

            if (this.getBranch(currIndex)==null){
                this.buildBranch(currIndex);
                this.setBit((currIndex),1);
            }

            this.getBranch(currIndex).addItemAux(index, value);
        }
        else {
            this.setBit(subIndex(index,1),1);
            this.setLeafData(subIndex(item,1),subIndex(item,0),1);
        }
    }
    
     */

    public void removeItem(int index){
        index--;
        IndexNode tempNode = this;

        for (int order = this.order; order>1; order--) {
            int currIndex = subIndex(index,order);

            tempNode = tempNode.getBranch(currIndex);
        }

        if (countBits(tempNode.getMask())==1){
            tempNode = null;
        } else {
            int orderZeroIndex = subIndex(index, 0);
            tempNode.setBit(orderZeroIndex, 0);
            tempNode.setLeafData(orderZeroIndex, 0);
        }
    }

    public int getIndex(int index){
        index--;
        IndexNode tempNode = this;

        for (int order = this.order; order > 0; order--) {
            int pathIndex = subIndex(index, order);

            tempNode = tempNode.getBranch(pathIndex);
        }
        //System.out.println(index + " [getIndex] " + tempNode.getLeafData(subIndex(index,0)));
        return tempNode.getLeafData(subIndex(index, 0));
    }


    public void buildBranch(int index, int currOrder){
        this.branch[index] = new IndexNode(currOrder-1);
    }

    public IndexNode getBranch(int index) throws NullPointerException { return this.branch[index]; }

    public IndexNode[] getBranches(){return this.branch; }

    public void setBranch(int index, IndexNode node){
        this.branch[index] = node;
        this.branch[index].setOrder((byte)(this.order-1));
    }

    public void setBranches(IndexNode[] branchArray){this.branch = branchArray;}

    public int getLeafData(int index) {
        try {
            return leaf[index];
        } catch (NullPointerException e){
            System.out.println("Index does not exist!");
            return -1;
        }
    }

    public void setLeafData(int index, int data){
        this.leaf[index] = data;
    }

    public static byte subIndex(int number, int order){
        return (byte)((number>>>(order*3))&0b111);
    }

    int countBits(byte currByte){
        int count = 0;
        for (int i = 0; i<8; i++){
            if (((currByte>>>i)&1) == 1){
                count++;
            }
        }
        return count;
    }

    int countBits(byte currByte, int upToIndex){
        int count = 0;
        for (int i = 0; i<upToIndex; i++){
            if (((currByte>>>i)&1) == 1){
                count++;
            }
        }
        return count;
    }

    void resizeTree(){
        if (order>1) {
            IndexNode tempNode = new IndexNode(this.order + 1);
            tempNode.setBranch(0, this);

            this.setOrder(tempNode.getOrder());
            this.setBranches(tempNode.getBranches());
        }

        //System.out.println("IndexNode resized " + this.order);
    }
}
