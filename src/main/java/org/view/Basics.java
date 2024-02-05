package org.view;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.dispatcher.SwingDispatchService;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.*;
import org.event.EventList;
import org.event.EventRunner;
import org.event.SpecialKeyAdapter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Basics implements NativeKeyListener, NativeMouseInputListener, NativeMouseWheelListener{
    //componentes
    private JFrame frame;
    private JPanel pn;
    private JSpinner timesTo;
    private JLabel keyToggleRecording;
    private JLabel keyTogglePlay;
    private JLabel keyToggleDiscardCurrent;
    private JCheckBox randomizeClicks; //adicionar caixa de seleção desenhável na tela opcional para randomizar
    private JCheckBox randomizeActions; //se o tick for ativado o valor aleatório deve ser mínimo
    private JCheckBox tickClick;
    private JLabel lbStatus;
    private JButton saveButton;
    private JButton loadButton;
    private JButton recordButton;
    private JButton playButton;
    private JTable tabela;
    private DefaultTableModel tableModel;
    private JScrollPane scrollPn;
    private DefaultTableModel modelTabela;

    // variáveis manipuláveis
    private long start;
    private long delay;
    private long lastEvent;
    private String[] row;
    private Boolean isRecording;
    private String lastKeyPressed;
    private Boolean hasKeyPressed;
    private HashMap<String, Integer> pressedKeys;
    private int tickBase;
    private Boolean ctrlPressed;
    private Boolean isTickable;
    private int totalTickDelay;
    private Boolean showDialog;
    private Boolean isRegistredEvents;

    //recursos
    private GeneralPath gp;
    private Timer timer;
    private Point lastPoint;
    private List<EventList> eventList;

    public Basics () throws NativeHookException {
        initComponents();
        registerListeners();
    }

    private void mouseMoveOnScreen () {
        gp = new GeneralPath();
        Point p = MouseInfo.getPointerInfo().getLocation();
        gp.moveTo(p.x, p.y);

        ActionListener al = new ActionListener() {
            Point lastPoint;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!getRecording() && timer.isRunning()) {
                    timer.stop();
                } else {
                    Point p = MouseInfo.getPointerInfo().getLocation();
                    if (!p.equals(lastPoint)) {
                        setStart(System.currentTimeMillis());
                        gp.lineTo(p.x, p.y);
                        updateTable(p.x+ ", " +p.y, "Mouse Movement", (getDelay() == 0 ? 1 : getDelay()) + "ms",-1);
                        delaySum();
                    }
                    setLastEvent(System.currentTimeMillis());
                    setDelay(delayCalc(lastEvent, start, false));
                    lastPoint = p;
                }

            }
        };
        timer = new Timer(0, al);
        timer.start();
    }

    private void updateTable (String input, String event, String delayMs, int nativeKeyEvent) {
        row[0] = input;
        row[1] = event;
        row[2] = delayMs;
        tableModel.addRow(row);
        tabela.scrollRectToVisible(tabela.getCellRect(tabela.getRowCount()-1, 0, true));

        if (nativeKeyEvent == -1) {
            eventList.add(new EventList(input, event, getDelay() == 0 ? 1 : getDelay()));
        } else {
            eventList.add(new EventList(nativeKeyEvent, event, getDelay() == 0 ? 1 : getDelay()));
        }

    }

    private void changeStatus (Boolean isRec) throws NativeHookException {
        isRecording = isRec;
        if (isRec) {
            lbStatus.setText("RECORDING");
            lbStatus.setForeground(new Color(60,179,113));
            recordButton.setText("Stop");
            frame.setState(JFrame.ICONIFIED);
            registerListeners();
            mouseMoveOnScreen();
        } else {
            lbStatus.setText("IDLE");
            lbStatus.setForeground(new Color(0,0,0));
            recordButton.setText("Record");
            removeListeners();
            initVars();
            frame.setState(JFrame.NORMAL);
        }
    }

    private void recordAction () throws AWTException, NativeHookException {
        setLastKeyPressed("");
        changeStatus(!getRecording());
    }

    public void registerListeners () throws NativeHookException {
        if (!getRegistredEvents()) {
            GlobalScreen.setEventDispatcher(new SwingDispatchService());
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeMouseListener(this);
            GlobalScreen.addNativeMouseMotionListener(this);
            GlobalScreen.addNativeMouseWheelListener(this);
            setRegistredEvents(true);
        }
    }

    public void removeListeners () {
        if (getRegistredEvents()) {
            GlobalScreen.isNativeHookRegistered();
            GlobalScreen.removeNativeMouseListener(this);
            GlobalScreen.removeNativeMouseMotionListener(this);
            GlobalScreen.removeNativeMouseWheelListener(this);
            setRegistredEvents(false);
        }
    }

    /**
     * @see NativeKeyListener#nativeKeyPressed(NativeKeyEvent)
     */
    public void nativeKeyPressed (NativeKeyEvent e) {
        String keyChar = e.paramString().substring(e.paramString().lastIndexOf("keyText="));
        keyChar = keyChar.substring(8,keyChar.indexOf(","));

        if(keyChar.equals("F5")) {
            confirmDiscard();
        }

        if (keyChar.equals("F2")) {
            play();
        }

        if (keyChar.equals("F1")) {
            try {
                changeStatus(!getRecording());
            } catch (NativeHookException ex) {
                throw new RuntimeException(ex);
            }
        }else if (!pressedKeys.containsKey(keyChar)
                && getRecording()
                && !keyChar.equals("F2")) {

            setStart(System.currentTimeMillis());
            pressedKeys.put(keyChar, e.getKeyCode());
            setLastKeyPressed(keyChar);
            setCtrlPressed(keyChar.equals("Ctrl"));
            if (getTickable() && getCtrlPressed()) {
                pressedKeys.put(keyChar, e.getKeyCode());
            }else {
                setLastEvent(System.currentTimeMillis());
                System.out.println(new SpecialKeyAdapter().isSpecialKey(e.getRawCode()));
                updateTable(keyChar, "Key Pressed", getDelay() + "ms", new SpecialKeyAdapter().isSpecialKey(e.getRawCode()));
                setDelay(delayCalc(lastEvent, start, false));
                delaySum();
            }
        }
    }

    /**
     * @see NativeKeyListener#nativeKeyReleased(NativeKeyEvent)
     */
    public void nativeKeyReleased (NativeKeyEvent e) {
        String keyChar = e.paramString().substring(e.paramString().lastIndexOf("keyText="));
        keyChar = keyChar.substring(8,keyChar.indexOf(","));

        if (!getLastKeyPressed().equals("") && getRecording()) {
            setStart(System.currentTimeMillis());
            setLastKeyPressed("FREE");
            pressedKeys.remove(keyChar,e.getKeyCode());
            setCtrlPressed(pressedKeys.containsKey("Ctrl"));
            setLastEvent(System.currentTimeMillis());

            if (!getTickable() || (!keyChar.equals("Ctrl")
                    && !keyChar.equals("F1")
                    && !keyChar.equals("F2")
                    && !keyChar.equals("F5"))) {
                updateTable(keyChar, "Key Released", getDelay() + "ms", new SpecialKeyAdapter().isSpecialKey(e.getRawCode()));
                setDelay(delayCalc(lastEvent, start, false));
                delaySum();
            }
            //soltou o ctrl
            if (getTickable() && !getCtrlPressed()) {
                pressedKeys.remove("Ctrl");
                setTotalTickDelay(0);
            }
        }
    }

    /**
     * @see NativeKeyListener#nativeKeyTyped(NativeKeyEvent)
     */
    public void nativeKeyTyped (NativeKeyEvent e) { }

    /**
     * @see NativeMouseListener#nativeMousePressed(NativeMouseEvent)
     */
    public void nativeMousePressed (NativeMouseEvent e) {
        if (isRecording) {
            setStart(System.currentTimeMillis());
            Point p = MouseInfo.getPointerInfo().getLocation();
            setLastEvent(System.currentTimeMillis());
            updateTable(p.x+ ", " +p.y, "Mouse Pressed "+ (e.getButton() == 1 ? "LEFT" : "RIGHT"),
                    ((e.getButton() == 1 && ctrlPressed)? delayCalc(start, lastEvent, true) : getDelay()) + "ms", -1);
            setDelay(delayCalc(lastEvent, start, false));
        }
    }

    /**
     * @see NativeMouseListener#nativeMouseReleased(NativeMouseEvent)
     */
    public void nativeMouseReleased (NativeMouseEvent e) {
        if (isRecording) {
            setStart(System.currentTimeMillis());
            Point p = MouseInfo.getPointerInfo().getLocation();
            setTotalTickDelay(0);
            setLastEvent(System.currentTimeMillis());
            updateTable(p.x+ ", " +p.y, "Mouse Released "+ (e.getButton() == 1 ? "LEFT" : "RIGHT"), getDelay() + "ms", -1);
            setDelay(delayCalc(lastEvent, start, false));
            delaySum();
        }
    }

    /**
     * @see NativeMouseListener#nativeMouseClicked(NativeMouseEvent)
     */
    public void nativeMouseClicked (NativeMouseEvent e) {

    }

    /**
     * @see NativeMouseMotionListener#nativeMouseMoved(NativeMouseEvent)
     */
    public void nativeMouseMoved (NativeMouseEvent e) { }

    /**
     * @see NativeMouseMotionListener#nativeMouseDragged(NativeMouseEvent)
     */
    public void nativeMouseDragged (NativeMouseEvent e) { }

    /**
     * @see NativeMouseWheelListener#nativeMouseWheelMoved(NativeMouseWheelEvent)
     */
    public void nativeMouseWheelMoved (NativeMouseWheelEvent e) {
        if (isRecording) {
            setStart(System.currentTimeMillis());
            setLastEvent(System.currentTimeMillis());
            updateTable((e.getWheelRotation() == 1 ? "DOWN" : "UP"), "Mouse Wheel", getDelay() + "ms", -1);
            setDelay(delayCalc(lastEvent, start, false));
            delaySum();
        }
    }

    private void confirmDiscard () {
            if (modelTabela.getRowCount() > 0 && !getShowDialog()){
                try {
                    changeStatus(false);
                } catch (NativeHookException ex) {
                    throw new RuntimeException(ex);
                }

                changeDialogState();

                int resp = JOptionPane.showConfirmDialog(null,"Do you really want to discard the " +
                        "current recording?","Warning!",JOptionPane.YES_NO_OPTION);

                if(resp == 0){
                    modelTabela.setRowCount(0);
                    eventList = new ArrayList<>();
                }
                changeDialogState();
            }
    }

    private void changeDialogState(){
        setShowDialog(!getShowDialog());
    }

    private void delaySum () {
        if (getTickable()) {
            String currentRow = tabela.getModel().getValueAt(tabela.getRowCount() -1, 2).toString();
            setTotalTickDelay(getTotalTickDelay() + Integer.parseInt(currentRow.substring(0, currentRow.indexOf("m"))));

            if (getTotalTickDelay() >= 600) {
                setTickBase(1);
            } else {
                setTickBase(600 - getTotalTickDelay());
            }
        }
    }

    private long delayCalc (long end, long start, Boolean tickable) {
        return tickable ? (getTickBase() == 0) ? getDelay() : getTickBase() : end - start;
    }

    private void play () {
        new EventRunner(eventList);
    }

    private void initVars () {
        //inicialização de variaveis
        pressedKeys = new HashMap<>();

        if (!pressedKeys.isEmpty()) {
            pressedKeys.clear();
        }

        //String[]
        row = new String[3];

        //map
        if (eventList == null) {
            eventList = new ArrayList<>();
        }

        //boolean
        setTickable(tickClick.isSelected());
        setRegistredEvents(false);
        setHasKeyPressed(false);
        setCtrlPressed(false);
        setShowDialog(false);
        setRecording(false);

        //int e long
        setTotalTickDelay(0);
        setLastEvent(0);
        setTickBase(0);
        setStart(0);
        setDelay(0);

        //String
        setLastKeyPressed("");

        //point
        setLastPoint(null);
    }

    private void initComponents () {
        GlobalScreen.addNativeKeyListener(this);

        frame = new JFrame("Basics");

        initVars();

        timesTo.setModel(new SpinnerNumberModel(0,0,null,1));
        JFormattedTextField numericTimesTo = ((JSpinner.NumberEditor) timesTo.getEditor()).getTextField();
        ((NumberFormatter) numericTimesTo.getFormatter()).setAllowsInvalid(false);

        tabela = new JTable();
        modelTabela = new DefaultTableModel(
                new Object[][] { },
                new String[] {
                        "Input",
                        "Event",
                        "Delay"
                });
        tabela.setModel(modelTabela);
        tabela.getTableHeader().setReorderingAllowed(false);
        scrollPn.setViewportView(tabela);

        tableModel = (DefaultTableModel) this.tabela.getModel();

        tickClick.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setTickBase(tickClick.isSelected() ? 600 : 0);
                setTickable(!isTickable);
            }
        });

        ImageIcon icon = new ImageIcon("src/main/java/org/icons/robot-excited-outline.png");

        //actionPerformed dos botões
        recordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    recordAction();
                } catch (AWTException | NativeHookException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        //componentes do JFrame
        frame.setIconImage(icon.getImage());
        frame.setTitle("Bald's MyRec");
        frame.setContentPane(pn);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public long getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(long lastEvent) {
        this.lastEvent = lastEvent;
    }

    public Boolean getRecording() {
        return isRecording;
    }

    public void setRecording(Boolean recording) {
        isRecording = recording;
    }

    public String getLastKeyPressed() {
        return lastKeyPressed;
    }

    public void setLastKeyPressed(String lastKeyPressed) {
        this.lastKeyPressed = lastKeyPressed;
    }

    public Boolean getHasKeyPressed() {
        return hasKeyPressed;
    }

    public void setHasKeyPressed(Boolean hasKeyPressed) {
        this.hasKeyPressed = hasKeyPressed;
    }

    public int getTickBase() {
        return tickBase;
    }

    public void setTickBase(int tickBase) {
        this.tickBase = tickBase;
    }

    public Boolean getCtrlPressed() {
        return ctrlPressed;
    }

    public void setCtrlPressed(Boolean ctrlPressed) {
        this.ctrlPressed = ctrlPressed;
    }

    public Boolean getTickable() {
        return isTickable;
    }

    public void setTickable(Boolean tickable) {
        isTickable = tickable;
    }

    public int getTotalTickDelay() {
        return totalTickDelay;
    }

    public void setTotalTickDelay(int totalTickDelay) {
        this.totalTickDelay = totalTickDelay;
    }

    public Boolean getShowDialog() {
        return showDialog;
    }

    public void setShowDialog(Boolean showDialog) {
        this.showDialog = showDialog;
    }

    public Point getLastPoint() {
        return lastPoint;
    }

    public void setLastPoint(Point lastPoint) {
        this.lastPoint = lastPoint;
    }

    public Boolean getRegistredEvents() {
        return isRegistredEvents;
    }

    public void setRegistredEvents(Boolean registredEvents) {
        isRegistredEvents = registredEvents;
    }
}