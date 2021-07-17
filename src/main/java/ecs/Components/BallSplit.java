package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

public class BallSplit extends Component {
    public BallSplit(){
        setContainer(new Container<>());
    }

    public BallSplit add(int ballSplit){
        super.add(ballSplit);
        return this;
    }
}
