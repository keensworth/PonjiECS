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

    public int getNoCollide(Entity entity){
        return getNoCollide(this.getEntityIndex(entity.getEntityId()));
    }

    public void setNoCollide(int index, int instance){
        this.noCollide.set(index, instance);
    }

    public void setNoCollide(Entity entity, int instance){
        setNoCollide(this.getEntityIndex(entity.getEntityId()), instance);
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
