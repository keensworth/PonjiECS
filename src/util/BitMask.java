package util;

import ecs.Component;

public class BitMask {
    private int bitMask;
    private Container<Component> components;

    BitMask(){
        this.components = new Container(Component.class);
        this.update();
    }

    public BitMask(Component... components){
        this.components = new Container(Component.class);
        for (Component component : components){
            this.components.add(component);
        }
        this.update();
    }

    public void addComponent(Component... components){
        for (Component component : components){
            if (!this.components.contains(component)){
                this.components.add(component);
            }
        }
        this.update();
    }

    public void removeComponent(Component... components){
        for (Component component : components){
            this.components.remove(component);
        }
        this.update();
    }

    public int update(){
        int tempMask = 0;
        int setter = 1;
        for (int index = 0; index < components.getSparseSize(); index++){
            if (components.get(index)!=null){
                tempMask |= (setter<<index);
            }
        }

        this.bitMask = tempMask;

        return bitMask;
    }

    public int get(Component... components){
        int tempMask = 0;
        int setter = 1;

        for (Component component : components){
            if(this.components.contains(component)){
                tempMask |= (setter<<(this.components.getIndex(component)));
            }
        }

        return tempMask;
    }

    public Component getComponent(int componentMask){
        for (int index = 0; index<32; index++){
            if (((componentMask>>>index)&1)==1){
                return components.get(index);
            }
        }
        return null;
    }

    public Component getComponent(Class component){
        return getComponent(getFromClasses(component));
    }

    public int get(){
        return bitMask;
    }

    public Component[] getComponents(){
        return components.toArray();
    }

    public Component[] getComponents(int componentMask){
        Container<Component> components = new Container(Component.class);
        componentMask &= bitMask;

        for (int component = 0; component < components.getSize(); component++){
            if (((componentMask>>>component)&1)==1){
                components.add(this.components.get(component));
            }
        }
        return components.toArray();
    }


    public int getFromClasses(Class... classes){
        int componentMask = 0;
        int setter = 1;
        for (int i = 0; i < components.getSize(); i ++){
            if (containsClass(classes, components.get(i).getClass())){
                componentMask |= (setter<<i);
            }
        }
        return componentMask;
    }

    private boolean containsClass(Class[] classes, Class classCheck){
        for (Class scanClass : classes) {
            if (scanClass == classCheck) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param matchMask entity's component mask
     * @param classToCheck class to check for in entity's mask
     * @return
     */
    public boolean containsComponent(int matchMask, Class classToCheck){
        return ((matchMask & getFromClasses(classToCheck)) == getFromClasses(classToCheck));
    }

}
