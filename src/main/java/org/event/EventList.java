package org.event;

public class EventList {

    private Long delay;
    private String keyData; // x,y ou keyCode
    private String eventType;
    private int keyCode;

    public EventList (String keyData, String eventType, Long delay) {
        this.setDelay(delay);
        this.setKeyData(keyData);
        this.setEventType(eventType);
    }

    public EventList (int keyCode, String eventType, Long delay){
        this.setDelay(delay);
        this.setEventType(eventType);
        this.setKeyCode(keyCode);
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getDelay() {
        return delay;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public String getKeyData() {
        return keyData;
    }

    public void setKeyData(String keyData) {
        this.keyData = keyData;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int nativeKeyEvent) {
        this.keyCode = nativeKeyEvent;
    }
}
