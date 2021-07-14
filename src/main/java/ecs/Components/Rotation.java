package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

public class Rotation extends Component {
    private Container<Float>[] rotation = new Container[3];

    public Rotation(){
        rotation[0] = new Container(Float.class);
        rotation[1] = new Container(Float.class);
        rotation[2] = new Container(Float.class);
    }

    public float[] getRotation(int index){
        float[] rotation = new float[3];

        rotation[0] = this.rotation[0].get(index);
        rotation[1] = this.rotation[1].get(index);
        rotation[2] = this.rotation[2].get(index);

        return rotation;
    }

    public float[] getRotation(Entity entity){
        return getRotation(this.getEntityIndex(entity.getEntityId()));
    }

    public float getXRot(int index){
        return this.rotation[0].get(index);
    }

    public float getYRot(int index){
        return this.rotation[1].get(index);
    }

    public float getZRot(int index){
        return this.rotation[2].get(index);
    }

    public Container[] getRotations(){
        return rotation;
    }

    public Container getXRot(){
        return rotation[0];
    }

    public Container getYRot(){
        return rotation[1];
    }

    public Container getZRot(){
        return rotation[2];
    }

    public void setRotation(int index, float[] rotation){
        this.rotation[0].set(index, rotation[0]);
        this.rotation[1].set(index, rotation[1]);
        this.rotation[2].set(index, rotation[2]);
    }

    public void setRotation(Entity entity, float[] rotation){
        setRotation(this.getEntityIndex(entity.getEntityId()), rotation);
    }

    public Rotation add(float[] rotation){
        //System.out.println("Rotation added: (" + rotation[0] + ", " + rotation[1] + ")");
        int index = this.rotation[0].add(rotation[0]);
        this.rotation[1].add(rotation[1]);
        this.rotation[2].add(rotation[2]);
        //System.out.println("----------------------------" + index);
        super.setLastWriteIndex(index);
        return this;
    }

    public void setXRot(int index, float xRot){
        rotation[0].set(index,xRot);
    }

    public void setYRot(int index, float yRot){
        rotation[1].set(index,yRot);
    }

    public void setZYRot(int index, float zRot){
        rotation[2].set(index,zRot);
    }
}
