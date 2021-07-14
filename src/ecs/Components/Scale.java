package ecs.Components;

import ecs.Component;
import util.Container;

public class Scale extends Component {
    private Container<Float> scale;

    public Scale(){
        scale = new Container<>(Float.class);
    }

    public float getScale(int index){
        return scale.get(index);
    }

    public void setScale(int index, float scale){
        this.scale.set(index, scale);
    }

    public Scale add(float scale){
        //System.out.println("Scale added: " + scale);
        super.setLastWriteIndex(this.scale.add(scale));
        return this;
    }
}
