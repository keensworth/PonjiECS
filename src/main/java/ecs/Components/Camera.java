package ecs.Components;

import ecs.Component;
import util.Container;

public class Camera extends Component {
    private Container camera;
    private int lastID;

    public Camera(){
        camera = new Container();
        lastID = 1;
    }

    public int getCameraID(int index){
        return (int)camera.get(index);
    }

    public void setCameraID(int index, int cameraID){
        this.camera.set(index, cameraID);
    }

    public Camera add(){
        //System.out.println("Radius added: " + radius);
        super.setLastWriteIndex(this.camera.add(lastID));
        lastID++;
        return this;
    }
}
