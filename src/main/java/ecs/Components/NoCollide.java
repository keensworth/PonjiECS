package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

public class NoCollide extends Component {
    private Container noCollide;
    private int instance = 0;

    public NoCollide(){
        noCollide = new Container();
    }

    public int increaseInstance(){
        instance++;
        return instance;
    }

    public int getNoCollide(int index){
        return (int) noCollide.get(index);
    }

    public void setNoCollide(int index, int instance){
        //System.out.println("NoCollide instance set: " + instance);
        this.noCollide.set(index, instance);
    }

    public NoCollide add(int instance){
        //System.out.println("NoCollide instance added: " + instance);
        super.setLastWriteIndex(this.noCollide.add(instance));
        return this;
    }

    public int getSize(){
        return noCollide.getSize();
    }
}
