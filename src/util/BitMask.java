package util;

import ecs.Component;

public class BitMask {
    private int bitMask;
    private Container<Component> components;

    BitMask(){
        this.components = new Container(Component.class);
        this.update();
    }

    /**
     * Initialize component container and add components to it and update bitmask
     *
     * @param components component(s) to be represented
     */
    public BitMask(Component... components){
        this.components = new Container(Component.class);
        for (Component component : components){
            this.components.add(component);
        }
        this.update();
    }

    /**
     * Add component(s) to component container and update the bitmask
     *
     * @param components component(s) to be added
     */
    public void addComponent(Component... components){
        for (Component component : components){
            if (!this.components.contains(component)){
                this.components.add(component);
            }
        }
        this.update();
    }

    /**
     * Remove component(s) from component container and update the bitmask
     *
     * @param components component(s) to be removed
     */
    public void removeComponent(Component... components){
        for (Component component : components){
            this.components.remove(component);
        }
        this.update();
    }

    /**
     * Update the integer bitmask
     *
     * @return updated integer bitmask
     */
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

    /**
     * Get the bitmask, with only the bits representing the passed in component(s)
     *
     * @param components component(s) to be represented in the integer bitmask
     * @return integer bitmask of components
     */
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

    /**
     * Get the Component represented by the integer componentMask
     *
     * @param componentMask integer with single activated bit
     * @return Component represented by componentMask
     */
    public Component getComponent(int componentMask){
        for (int index = 0; index<32; index++){
            if (((componentMask>>>index)&1)==1){
                return components.get(index);
            }
        }
        return null;
    }

    /**
     * Get the Component represented by the Component's class
     *
     * @param component Component Class
     * @return Component represented by class
     */
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


    /**
     * Get an integer bitmask from class(es)
     *
     * @param classes class(es) used to build bitmask
     * @return integer bitmask of existing components represented by the class(es)
     */
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

}
