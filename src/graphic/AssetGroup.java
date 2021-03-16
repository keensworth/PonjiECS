package graphic;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public class AssetGroup {
    private float angle;
    private Vector3f axis;
    private float baseScale;
    private int groupID;
    private int textureID;

    public AssetGroup(int id){
        groupID = id;
        angle = 0;
        axis = new Vector3f(1,0,0);
        baseScale = 1;
    }

    public void setRotation(float theta, Vector3f axis){
        this.angle = theta;
        this.axis = axis;
    }

    public void setScale(float scale){
        baseScale = scale;
    }

    public void setTextureID(int textureID){
        this.textureID = textureID;
    }

    public int getTextureID(){
        return textureID;
    }

    public float getRotation() {
        return angle;
    }

    public Vector3f getAxis(){
        return axis;
    }

    public void setAxis(Vector3f axis){
        this.axis = axis;
    }

    public Matrix4f getRotationMatrix(){
        return new Matrix4f().identity().rotate(angle,axis);
    }

    public float getScale(){
        return baseScale;
    }

}
