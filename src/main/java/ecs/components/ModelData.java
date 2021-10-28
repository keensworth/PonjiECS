package ecs.components;

import ecs.Component;
import util.Container;

public class ModelData extends Component {
    public ModelData(){
        setContainer(new Container<>());
    }

    public ModelData add(int modelId){
        super.add(modelId);
        return this;
    }
}