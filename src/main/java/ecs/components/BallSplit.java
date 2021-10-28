package ecs.components;

import ecs.Component;
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
