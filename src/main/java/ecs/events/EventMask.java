package ecs.events;

import util.Container;

public class EventMask {
    private int bitMask;
    private Container<Event> events;

    EventMask(){
        this.events = new Container(Event.class);
        this.update();
    }

    /**
     * Initialize event container and add events to it and update bitmask
     *
     * @param events event(s) to be represented
     */
    public EventMask(Event... events){
        this.events = new Container(Event.class);
        for (Event event : events){
            this.events.add(event);
        }
        this.update();
    }

    /**
     * Add event(s) to event container and update the bitmask
     *
     * @param events event(s) to be added
     */
    public void addEvent(Event... events){
        for (Event event : events){
            if (!this.events.contains(event)){
                this.events.add(event);
            }
        }
        this.update();
    }

    /**
     * Remove event(s) from event container and update the bitmask
     *
     * @param events event(s) to be removed
     */
    public void removeEvent(Event... events){
        for (Event event : events){
            this.events.remove(event);
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
        for (int index = 0; index < events.getSparseSize(); index++){
            if (events.get(index)!=null){
                tempMask |= (setter<<index);
            }
        }

        this.bitMask = tempMask;

        return bitMask;
    }

    /**
     * Get the bitmask, with only the bits representing the passed in event(s)
     *
     * @param events event(s) to be represented in the integer bitmask
     * @return integer bitmask of events
     */
    public int get(Event... events){
        int tempMask = 0;
        int setter = 1;

        for (Event event : events){
            if(this.events.contains(event)){
                tempMask |= (setter<<(this.events.getIndex(event)));
            }
        }

        return tempMask;
    }

    /**
     * Get the Event represented by the integer eventMask
     *
     * @param eventMask integer with single activated bit
     * @return Event represented by eventMask
     */
    public Event getEvent(int eventMask){
        for (int index = 0; index<32; index++){
            if (((eventMask>>>index)&1)==1){
                return events.get(index);
            }
        }
        return null;
    }

    /**
     * Get the Event represented by the Event's class
     *
     * @param event Event Class
     * @return Event represented by class
     */
    public Event getEvent(Class event){
        return getEvent(getFromClasses(event));
    }

    public int get(){
        return bitMask;
    }

    public Event[] getEvents(){
        return events.toArray();
    }

    public Event[] getEvents(int eventMask){
        Container<Event> events = new Container(Event.class);
        eventMask &= bitMask;

        for (int event = 0; event < events.getSize(); event++){
            if (((eventMask>>>event)&1)==1){
                events.add(this.events.get(event));
            }
        }
        return events.toArray();
    }


    /**
     * Get an integer bitmask from class(es)
     *
     * @param classes class(es) used to build bitmask
     * @return integer bitmask of existing events represented by the class(es)
     */
    public int getFromClasses(Class... classes){
        int eventMask = 0;
        int setter = 1;
        for (int i = 0; i < events.getSize(); i ++){
            if (containsClass(classes, events.get(i).getClass())){
                eventMask |= (setter<<i);
            }
        }
        return eventMask;
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
