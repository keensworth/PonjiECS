package ecs.events;

import util.Container;

public class EventManager {
    private Container<Event> currentEvents; // current frame's events to process
    private Container<Event> nextEvents;    // events to process next frame
    private final EventMask eventsMask;     // mask of all events (static)
    private EventMask currentEventsMask;    // mask of current events to process

    public EventManager(Event... events){
        currentEvents = new Container<>(Event.class);
        nextEvents = new Container<>(Event.class);
        eventsMask = new EventMask(events);
        currentEventsMask = new EventMask();
    }

    public Container<Event> getCurrentEvents(){
        return currentEvents;
    }

    public int getCurrentEventsMask(){
        return currentEventsMask.get();
    }

    public void publishEvent(Event... events){
        nextEvents.add(events);
    }

    public int subscribeToEvents(Event... events){
        return eventsMask.get(events);
    }

    public void update(){
        currentEvents = nextEvents;
        currentEventsMask = new EventMask(nextEvents.toArray());
        nextEvents = new Container<>(Event.class);
    }
}
