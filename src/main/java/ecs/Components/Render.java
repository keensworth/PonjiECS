package ecs.Components;

import ecs.Component;
import util.Container;

public class Render extends Component {
    private Container color;

    public Render() {
        color = new Container();
    }

    public int getColor(int index){
        return (int)color.get(index);
    }

    public void setColor(int index, int color){
        this.color.set(index, color);
    }

    public Render add(int red, int green, int blue){
        int color = ((red<<16)&0xff) | ((green<<8)&0xff) | (blue&0xff);
        super.setLastWriteIndex(this.color.add(color));
        return this;
    }


}
