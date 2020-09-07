package util;

import java.lang.reflect.Array;
import java.util.Vector;

public class Container2D<E> {
    private E[][] container;

    private int write=0;
    private int size = 0;
    private int sparseDepth = 0;

    Container2D(){
        new Container2D(int.class);
    }

    Container2D(int size){
        new Container2D(int.class,size);
    }

    Container2D(Class c){
        this.container = (E[][]) Array.newInstance(c, 2,10);
    }

    Container2D(Class c, int size) {
        this.container = (E[][]) Array.newInstance(c, 2, size);
    }

    //TODO: Done??
    void add(E data1, E data2){
        if (sparseDepth==0){
            if (write>=container[0].length){
                container = doubleSize(container);
            }
            container[0][write] = data1;
            container[1][write] = data2;
            write++;
            size++;
        }
        else {
            for (int index = 0; index<container[0].length; index++){
                if (container[0][index]==null){
                    container[0][write] = data1;
                    container[1][write] = data2;
                    sparseDepth--;
                    size++;
                    break;
                }
            }
        }
    }

    public void add(E[][] data){
        for (int index = 0; index < data[0].length; index++){
            if (data[0][index]!=null){
                this.add(data[0][index],data[1][index]);
            }
        }
    }

    public void set(int index, E data1, E data2){
        if (index>write){
            index = write;
            write++;
        }
        if (index >= container[0].length){
            container = doubleSize(container);
        }
        if (container[0][index]==null){
            size++;
        }
        container[0][write] = data1;
        container[1][write] = data2;
    }

    public E get(byte elemIndex){
        return container[elemIndex][size - 1 + sparseDepth];
    }

    public E get(int index, byte elemIndex){
        if (index >= container[0].length){
            return null;
        }
        else{
            return container[elemIndex][index];
        }
    }

    public Vector<E> get(){
        Vector<E> tempVec = new Vector(2);
        tempVec.add(container[0][size - 1 + sparseDepth]);
        tempVec.add(container[1][size - 1 + sparseDepth]);
        return tempVec;
    }

    Vector<E> get(int index){
        if (index >= container[0].length){
            return null;
        }
        else{
            Vector<E> tempVec = new Vector(2);
            tempVec.add(container[0][index]);
            tempVec.add(container[1][index]);
            return tempVec;
        }
    }

    void remove(E data){
        for (int index = 0; index < size+sparseDepth; index++){
            for (int elemIndex = 0; elemIndex<2; elemIndex++){
                if (container[elemIndex][index] == data){
                    container[0][index] = null;
                    container[1][index] = null;
                    size--;
                    sparseDepth++;
                }
            }
        }
    }

    boolean contains(E data){
        for (int index = 0; index < size+sparseDepth; index++){
            for (int elemIndex = 0; elemIndex<2; elemIndex++){
                if (container[elemIndex][index] == data){
                    return true;
                }
            }
        }
        return false;
    }

    int getIndex(E data){
        for (int index = 0; index < size+sparseDepth; index++){
            for (int elemIndex = 0; elemIndex<2; elemIndex++){
                if (container[elemIndex][index] == data){
                    return index;
                }
            }
        }
        return -1;
    }

    int getSize(){
        return size;
    }
    int getSparseSize(){ return (size+sparseDepth); }

    public E[][] doubleSize(E[][] array){
        @SuppressWarnings("unchecked")
        E[][] newArray = (E[][]) Array.newInstance(this.getClass(), 2, array.length*2);

        System.arraycopy(array[0],0,newArray[0],0,array[0].length);
        System.arraycopy(array[1],0,newArray[1],0,array[0].length);
        return newArray;
    }

    public E[][] toArray(){
        @SuppressWarnings("unchecked")
        E[][] newArray = (E[][]) Array.newInstance(this.getClass(), 2, size*2);

        System.arraycopy(container[0],0,newArray[0],0,size+sparseDepth);
        System.arraycopy(container[1],0,newArray[1],0,size+sparseDepth);
        return newArray;
    }
}
