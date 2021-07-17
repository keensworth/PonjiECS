package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

public class Health extends Component {
    private Container health;

    public Health() {
        setContainer(new Container<>());
    }

    public Health add(int health){
        super.add(health);
        return this;
    }
}
