package org.event;

import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;

public class EventRunner implements NativeKeyListener {

    private List<EventList> eventList;

    public EventRunner(List<EventList> eventList) {
        setEventList(eventList);
        run();
    }

    private void run(){
        System.out.println("Running..."+ this.getEventList().size());
        try{
            Robot robot = new Robot();

            for (EventList evt : eventList) {
                    switch (evt.getEventType()){
                        case "Mouse Movement":
                            String[] coordenadas = evt.getKeyData().split(", ");

                            int x = Integer.parseInt(coordenadas[0]);
                            int y = Integer.parseInt(coordenadas[1]);

                            robot.mouseMove(x,y);
                            break;
                        case "Mouse Wheel":
                            if(evt.getKeyData().equals("UP")){
                                //FAZER CONDIÇÃO PRO CLIQUE DA MOUSE WHEEL
                            } else {
                                //FAZER CONDIÇÃO PRO CLIQUE DA MOUSE WHEEL
                            }
                            break;
                        case "Mouse Pressed LEFT":
                            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                            break;
                        case "Mouse Released LEFT":
                            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                            break;
                        case "Mouse Pressed RIGHT":
                            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                            break;
                        case "Mouse Released RIGHT":
                            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                            break;
                        case "Key Pressed":
                            robot.keyPress(evt.getKeyCode());
                            break;
                        case "Key Released":
                            robot.keyRelease(evt.getKeyCode());
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + evt.getEventType());
                    }

                robot.delay(Math.toIntExact(evt.getDelay()));
            }
        }catch (AWTException e){
            e.printStackTrace();
        }
    }

    public List<EventList> getEventList() {
        return eventList;
    }

    public void setEventList(List<EventList> eventList) {
        this.eventList = eventList;
    }

}
