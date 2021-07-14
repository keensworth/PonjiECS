package ecs.Components;

import ecs.Component;
import util.Container;

public class Velocity extends Component {
    private Container<Float>[] velocity = new Container[2];

    public Velocity(){
        velocity[0] = new Container(Float.class);
        velocity[1] = new Container(Float.class);
    }

    public float[] getVelocity(int index){
        float[] velocity = new float[2];

        velocity[0] = this.velocity[0].get(index);
        velocity[1] = this.velocity[1].get(index);

        return velocity;
    }

    public float getXVel(int index){
        return this.velocity[0].get(index);
    }

    public float getYVel(int index){
        return this.velocity[1].get(index);
    }

    public Container[] getVelocities(){
        return velocity;
    }

    public Container getXVels(){
        return velocity[0];
    }

    public Container getYVels(){
        return velocity[1];
    }

    public void setVelocity(float[] velocity, int index){
        this.velocity[0].set(index, velocity[0]);
        this.velocity[1].set(index, velocity[1]);
    }

    public Velocity add(float[] velocity){
        //System.out.println("Velocity added: (" + velocity[0] + ", " + velocity[1] + ")");
        int index = this.velocity[0].add(velocity[0]);
        this.velocity[1].add(velocity[1]);
        super.setLastWriteIndex(index);
        return this;
    }

    public void setXVel(int index, float xVel){
        velocity[0].set(index,xVel);
    }

    public void setYVel(int index, float yVel){
        velocity[1].set(index,yVel);
    }
}
