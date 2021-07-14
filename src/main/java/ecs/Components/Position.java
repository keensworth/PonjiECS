package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

public class Position extends Component {
    private Container<Float>[] position = new Container[3];

    public Position(){
        position[0] = new Container(Float.class);
        position[1] = new Container(Float.class);
        position[2] = new Container(Float.class);
    }

    public float[] getPosition(int index){
        float[] position = new float[3];

        position[0] = this.position[0].get(index);
        position[1] = this.position[1].get(index);
        position[2] = this.position[2].get(index);

        return position;
    }

    public float[] getPosition(Entity entity){
        return getPosition(this.getEntityIndex(entity.getEntityId()));
    }

    public float getXPos(int index){
        return this.position[0].get(index);
    }

    public float getYPos(int index){
        return this.position[1].get(index);
    }

    public float getZPos(int index){
        return this.position[2].get(index);
    }

    public Container[] getPositions(){
        return position;
    }

    public Container getXPos(){
        return position[0];
    }

    public Container getYPos(){
        return position[1];
    }

    public Container getZPos(){
        return position[2];
    }

    public void setPosition(int index, float[] position){
        this.position[0].set(index, position[0]);
        this.position[1].set(index, position[1]);
        this.position[2].set(index, position[2]);
    }

    public void setPosition(Entity entity, float[] position){
        setPosition(this.getEntityIndex(entity.getEntityId()), position);
    }

    public void setXPos(int index, float xPos){
        position[0].set(index,xPos);
    }

    public void setXPos(Entity entity, float xPos){
        setXPos(this.getEntityIndex(entity.getEntityId()), xPos);
    }

    public void setYPos(int index, float yPos){
        position[1].set(index,yPos);
    }

    public void setYPos(Entity entity, float yPos){
        setYPos(this.getEntityIndex(entity.getEntityId()), yPos);
    }

    public void setZPos(int index, float zPos){
        position[2].set(index,zPos);
    }

    public void setZPos(Entity entity, float zPos){
        setZPos(this.getEntityIndex(entity.getEntityId()), zPos);
    }

    public Position add(float[] position){
        //System.out.println("Position added: (" + position[0] + ", " + position[1] + ")");
        int index = this.position[0].add(position[0]);
        this.position[1].add(position[1]);
        this.position[2].add(position[2]);
        super.setLastWriteIndex(index);
        return this;
    }
}
