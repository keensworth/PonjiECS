package ecs.Components;

import ecs.Component;
import util.Container;

public class BallSplit extends Component {
    private Container ballSplit;

    public BallSplit(){
        ballSplit = new Container();
    }

    public int getBallSplit(int index){
        return (int)ballSplit.get(index);
    }

    public void setBallSplit(int index, int ballSplit){
        this.ballSplit.set(index, ballSplit);
    }

    public BallSplit add(int ballSplit){
        System.out.println("BallSplit added: " + ballSplit);
        super.setLastWriteIndex(this.ballSplit.add(ballSplit));
        return this;
    }
}
