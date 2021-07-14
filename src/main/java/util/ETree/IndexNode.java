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


    /**
     * Initialize the IndexNode. If the order is:
     *         1+ - create a branch node
     *         0  - create a data node
     *
     * @param order level of the node
     */
    public IndexNode(int order){
        this.order = (byte)order;
        if (order>0){
            branch = new IndexNode[8];
        } else {
            this.leaf = new int[8];
        }
    }

    public IndexNode(IndexNode node){
        this.setOrder(node.getOrder());
        this.setBranches(node.getBranches());
        this.setMask(node.getMask());
    }

    /**
     * Add an integer value to the tree at index.
     * Used to store the indices of entities' component data
     *
     * @param index index to add the value
     * @param value data to be added to the tree
     */
    public void addItem(int index, int value){
        index--;
        while (Math.pow(8,this.order+1) < index+1){
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

    /**
     * Remove data at a particular index
     *
     * @param index index of data to be removed
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
        return tempNode.getLeafData(subIndex(index, 0));
    }


    public void buildBranch(int index, int currOrder){
        this.branch[index] = new IndexNode(currOrder-1);
    }

    public IndexNode getBranch(int index) {
        try {
            return this.branch[index];
        } catch (Exception e) {
            System.out.println("Failed to get branch");
            return null;
        }
    }


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

    //BROKEN
    void resizeTree(){
        System.out.println("Resizing IndexNode");
        if (order>1) {
            IndexNode tempNode = new IndexNode(this.order + 1);
            tempNode.setBranch(0, new IndexNode(this));
            System.out.println(tempNode.getBranch(0));

            this.setBranches(tempNode.getBranches());
            this.setOrder(tempNode.getOrder());
            this.setMask((byte) 0b00000001);
        }
    }
}
