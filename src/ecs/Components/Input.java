package ecs.Components;

import ecs.Component;
import ecs.Entity;

public class Input extends Component {
    private Entity controllable;

    private int[] mousePos;
    private int[] clickPos;
    private boolean clicked = false;

    public Input(){
        mousePos = new int[2];
        clickPos = new int[2];
    }

    public Input setControllable(){
        //System.out.println("Input added");
        super.setLastWriteIndex(0);
        return this;
    }

    public void setControllable(Entity entity){
        this.controllable = entity;
    }

    public Entity getControllable(){
        return this.controllable;
    }

    public int[] getClickPos(){
        return clickPos;
    }

    public int[] getMousePos(){
        return mousePos;
    }

    public boolean isClicked(){
        return clicked;
    }

    public void setClickPos(int[] clickPos){
        this.clickPos = clickPos;
    }

    public void setMousePos(int[] move){
        mousePos = move;
    }

    public void setClicked(){
        clicked = true;
    }

    public void reset(){
        clicked = false;
    }
}
