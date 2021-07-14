package ecs.Components;

import ecs.Component;
import ecs.Entity;
import util.Container;

public class ModelData extends Component {
    private Container models;

    public ModelData(){
        models = new Container();
    }

    public int getModel(int index){
        return (int) models.get(index);
    }

    public int getModel(Entity entity){
        return getModel(this.getEntityIndex(entity.getEntityId()));
    }

    public void setModel(int index, int modelId){
        this.models.set(index, modelId);
    }

    public void setModel(Entity entity, int modelId){
        setModel(this.getEntityIndex(entity.getEntityId()), modelId);
    }

    public ModelData add(int modelId){
        super.setLastWriteIndex(this.models.add(modelId));
        return this;
    }
}