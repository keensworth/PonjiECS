package ecs.Events;

public class Event {
    private int eventId;
    private String eventName;


    public Event(int eventId, String eventName){
        this.eventId = eventId;
        this.eventName = eventName;
    }

    public Event(String eventName){
        this.eventId = 0;
        this.eventName = eventName;
    }

    public int getEventId(){
        return eventId;
    }

    public String getEventName(){
        return eventName;
    }
}
