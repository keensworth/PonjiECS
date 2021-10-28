package util;

import util.nodes.IndexNode;

public class NodeTest {
    private static int[] components = new int[]{44564563,23452345,53453453,222333111};
    private static int[] count = new int[4];
    private static long start, end;
    public static void main(String[] args){
        /*
        start = System.nanoTime();
        EntNode entityNode = new EntNode(2);
        for (int i = 0; i < 1000000; i++){
            int index = (int) Math.floor(Math.random()*4);
            entityNode.addEntity(new Entity(i,components[index]));
            count[index]++;
        }
        end = System.nanoTime();
        System.out.println((end-start)/1000000f + " ms to addEntity()");

        start = System.nanoTime();
        Container<Entity> e1 = entityNode.getEntities(components[0]);
        Container<Entity> e2 = entityNode.getEntities(components[1]);
        Container<Entity> e3 = entityNode.getEntities(components[2]);
        Container<Entity> e4 = entityNode.getEntities(components[3]);
        end = System.nanoTime();
        System.out.println((end-start)/1000000f + " ms to getEntities()");

        System.out.println(count[0] + " actual, " + e1.getSize() + " found");
        System.out.println(count[1] + " actual, " + e2.getSize() + " found");
        System.out.println(count[2] + " actual, " + e3.getSize() + " found");
        System.out.println(count[3] + " actual, " + e4.getSize() + " found");

         */
        test();
    }


    public static void test(){
        start = System.nanoTime();
        IndexNode indexNode = new IndexNode(3);
        for (int i = 0; i < 100000; i++){
            int index = (int) Math.floor(Math.random()*4);
            System.out.println("Adding at index: " + i);
            indexNode.addItem(i,i*2);
            count[index]++;
        }
        end = System.nanoTime();
        System.out.println((end-start)/1000000f + " ms to addEntity()");

        start = System.nanoTime();
        int e1 = indexNode.getIndex(1);
        int e2 = indexNode.getIndex(4);
        int e3 = indexNode.getIndex(8);
        int e4 = indexNode.getIndex(126);
        end = System.nanoTime();
        System.out.println((end-start)/1000000f + " ms to getEntities()");

        System.out.println(1 + " actual, " + e1 + " found");
        System.out.println(2 + " actual, " + e2 + " found");
        System.out.println(3 + " actual, " + e3 + " found");
        System.out.println(63 + " actual, " + e4+ " found");
    }
}
