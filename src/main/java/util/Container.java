package util;

import java.lang.reflect.Array;

public class Container<E> {
    private E[] container;
    private Class containerClass;

    private int write = 0;
    private int size = 0;
    private int sparseDepth = 0;
    private int lastWrite = 0;

    public Container(){
        this.container = (E[]) java.lang.reflect.Array.newInstance(Integer.class, 10);
        containerClass = Integer.class;
    }

    public Container(int size){
        this.container = (E[]) java.lang.reflect.Array.newInstance(Integer.class, 10);
        containerClass = Integer.class;
    }

    public Container(Class<E> c){
        this.container = (E[]) java.lang.reflect.Array.newInstance(c, 10);
        containerClass = c;
    }

    public Container(Class<E> c, int size) {
        this.container = (E[]) java.lang.reflect.Array.newInstance(c, size);
        containerClass = c;
    }

    /**
     * Add data to container array
     *
     * @param data object to be stored in array
     * @return index of data write
     */
    public int add(E data){
        if (sparseDepth==0){
            if (write>=container.length){
                container = doubleSize(container);
            }
            container[write] = data;
            lastWrite = write;
            write++;
            size++;
        }
        else {
            for (int index = 0; index<container.length; index++){
                if (container[index]==null) {
                    container[index] = data;
                    sparseDepth--;
                    size++;
                    lastWrite = index;
                    break;
                }
            }
        }
        return lastWrite;
    }

    public int add(Container<E> container){
        return this.add(container.toArray());
    }

    /**
     * Add data to furthest write location (ignoring empty middle locations)
     *
     * @param data object to be stored in array
     * @return index of data write
     */
    public int append(E data){
        if (write>=container.length){
            container = doubleSize(container);
        }
        container[write] = data;
        write++;
        size++;
        return write;
    }

    /**
     * Add array to container array
     *
     * @param data array of objects to add to array
     * @return 32 bit integer, <31:16> array size, <15:0> index of first write
     */
    public int add(E[] data){
        boolean first = true;
        int firstIndex = 0;
        int count = 0;
        for (E item : data) {
            if (item!=null) {
                if (first){
                    firstIndex = this.append(item);
                    first = false;
                } else {
                    this.append(item);
                }
                count++;
            }
        }
        return ((count<<16)|(firstIndex&0xffff));
    }

    /**
     * Set data in array at a specific index
     *
     * @param index index of data write
     * @param data object to be stored in array
     * @return index of data write
     */
    public int set(int index, E data){
        if (index>write){
            index = write;
            write++;
        }
        while (index >= container.length){
            container = doubleSize(container);
        }
        if (container[index]==null){
            size++;
        }
        container[index] = data;
        return index;
    }

    public E get(){
        return container[size - 1];
    }

    public E get(int index){
        if (index >= container.length){
            return null;
        }
        else{
            return container[index];
        }
    }

    /**
     * Remove first instance of data from array
     *
     * @param data object to be removed
     */
    public void remove(E data){
        for (int index = 0; index < size+sparseDepth; index++){
            if (container[index] == data){
                container[index] = null;
                size--;
                sparseDepth++;
                break;
            }
        }
    }

    public boolean contains(E data){
        for (E item : container) {
            if (item == data) {
                return true;
            }
        }
        return false;
    }

    public int getIndex(E data){
        int tempIndex = 0;
        for (E item : container){
            if (item==data){
                return tempIndex;
            }
            tempIndex++;
        }
        return -1;
    }

    public int getLastWrite(){
        return lastWrite;
    }

    public int getSize(){
        return size;
    }
    public int getArraySize() { return container.length;}
    public int getSparseSize(){ return (size+sparseDepth); }

    public E[] doubleSize(E[] array){
        @SuppressWarnings("unchecked")
        E[] newArray = (E[]) Array.newInstance(containerClass, array.length*2);

        System.arraycopy(array,0,newArray,0,array.length);
        return newArray;
    }

    public E[] toArray(){
        @SuppressWarnings("unchecked")
        E[] newArray = (E[]) Array.newInstance(containerClass, (size + sparseDepth)*2);

        if (size!=0) {
            System.arraycopy(container, 0, newArray, 0, size + sparseDepth );
        }
        return newArray;
    }

    public String toString(){
        for (int i = 0; i < size+sparseDepth; i++){
            System.out.println(this.get(i));
        }
        return null;
    }
}
