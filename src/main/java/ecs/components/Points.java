package ecs.components;

import ecs.Component;

public class Points extends Component {
    private int points;

    public Points() {
        points = 0;
    }

    public void incPoints(){
        points+=1;
    }

    public void incPoints(int inc){
        points+=inc;
    }

    public int getPoints(){
        return points;
    }
}
