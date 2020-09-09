package ecs.Components;

import ecs.Component;
import util.Container;

public class Position extends Component {
    private Container<Float>[] position = new Container[2];

    public Position(){
        position[0] = new Container(Float.class);
        position[1] = new Container(Float.class);
    }

    public float[] getPosition(int index){
        float[] position = new float[2];

        position[0] = this.position[0].get(index);
        position[1] = this.position[1].get(index);

        return position;
    }

    public float getXPos(int index){
        return this.position[0].get(index);
    }

    public float getYPos(int index){
        return this.position[1].get(index);
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

    public void setPosition(float[] position, int index){
        this.position[0].set(index, position[0]);
        this.position[1].set(index, position[1]);
    }

    public Position add(float[] position){
        //System.out.println("Position added: (" + position[0] + ", " + position[1] + ")");
        int index = this.position[0].add(position[0]);
        this.position[1].add(position[1]);
        //System.out.println("----------------------------" + index);
        super.setLastWriteIndex(index);
        return this;
    }

    public void setXPos(int index, float xPos){
        position[0].set(index,xPos);
    }

    public void setYPos(int index, float yPos){
        position[1].set(index,yPos);
    }
}
