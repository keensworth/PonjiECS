package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

public class Light extends Component {

    public static final int POINT_LIGHT = 0;
    public static final int SPOT_LIGHT = 1;

    private Container lightType;

    public Light(){
        lightType = new Container();
    }

    public int getLight(int index){
        return (int) lightType.get(index);
    }
    
    public int getLight(Entity entity){
        return getLight(this.getEntityIndex(entity.getEntityId()));
    }

    public Light add(int lightType){
        super.setLastWriteIndex(this.lightType.add(lightType));
        return this;
    }
}