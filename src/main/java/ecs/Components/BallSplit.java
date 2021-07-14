package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

public class BallSplit extends Component {
    private Container ballSplit;

    public BallSplit(){
        ballSplit = new Container();
    }

    public int getBallSplit(int index){
        return (int)ballSplit.get(index);
    }
    
    public int getBallSplit(Entity entity){
        return getBallSplit(this.getEntityIndex(entity.getEntityId()));
    }

    public void setBallSplit(int index, int ballSplit){
        this.ballSplit.set(index, ballSplit);
    }

    public void setBallSplit(Entity entity, int ballSplit){
        setBallSplit(this.getEntityIndex(entity.getEntityId()), ballSplit);
    }

    public BallSplit add(int ballSplit){
        //System.out.println("BallSplit added: " + ballSplit);
        super.setLastWriteIndex(this.ballSplit.add(ballSplit));
        return this;
    }
}
