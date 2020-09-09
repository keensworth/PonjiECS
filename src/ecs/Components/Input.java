package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.LinkedList;

public class Input extends Component {
    private Entity controllable;

    private int[] movedCoords;
    private int[] clickedCoords;
    private boolean clicked = false;

    public Input(){
        movedCoords = new int[2];
        clickedCoords = new int[2];
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

    public int[] getClick(){
        return clickedCoords;
    }

    public int[] getMove(){
        return movedCoords;
    }

    public boolean isClicked(){
        return clicked;
    }

    public void setClick(int[] click){
        clickedCoords = click;
    }

    public void setMove(int[] move){
        movedCoords = move;
    }

    public void setClicked(){
        clicked = true;
    }

    public void reset(){
        clicked = false;
    }
}
