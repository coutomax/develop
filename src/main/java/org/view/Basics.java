package org.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.dispatcher.SwingDispatchService;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseMotionListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener;
import com.sun.org.apache.xpath.internal.operations.Bool;

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
    private ImageIcon icon;
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

    //recursos
    private Robot robot;
    private GeneralPath gp;
    private Timer timer;
    private Point lastPoint;

    public Basics () throws NativeHookException {
        initComponents();
        registerListeners();
    }

    private void mouseMoveOnScreen () throws NativeHookException {
        gp = new GeneralPath();
        Point p = MouseInfo.getPointerInfo().getLocation();
        gp.moveTo(p.x, p.y);

        ActionListener al = new ActionListener() {
            Point lastPoint;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isRecording && timer.isRunning()) {
                    timer.stop();
                } else {
                    Point p = MouseInfo.getPointerInfo().getLocation();
                    if (!p.equals(lastPoint)) {
                        start = System.currentTimeMillis();
                        gp.lineTo(p.x, p.y);
                        updateTable(p.x+ ", " +p.y, "Mouse Movement", delay + "ms");
                    }
                    lastEvent  = System.currentTimeMillis();

                    delay = delayCalc(lastEvent, start, false);
                    lastPoint = p;
                }

            }
        };
        timer = new Timer(0, al);
        timer.start();
    }

    private void updateTable (String input, String event, String delayMs) {
        row[0] = input;
        row[1] = event;
        row[2] = delayMs;
        tableModel.addRow(row);
        tabela.scrollRectToVisible(tabela.getCellRect(tabela.getRowCount()-1, 0, true));
    }

    private void changeStatus(Boolean isRec) throws NativeHookException {
        isRecording = isRec;
        if (isRec) {
            lbStatus.setText("RECORDING");
            lbStatus.setForeground(new Color(60,179,113));
            recordButton.setText("Stop");
            frame.setState(JFrame.ICONIFIED);
            mouseMoveOnScreen();
        }   else {
            lbStatus.setText("IDLE");
            lbStatus.setForeground(new Color(0,0,0));
            recordButton.setText("Record");
            removeListeners();
            frame.setState(JFrame.NORMAL);
        }
    }

    private void recordAction (ActionEvent e) throws AWTException, NativeHookException {
        lastKeyPressed = "";
        changeStatus(!isRecording);
    }

    public void registerListeners() throws NativeHookException {
        GlobalScreen.setEventDispatcher(new SwingDispatchService());
        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(this);
        GlobalScreen.addNativeMouseListener(this);
        GlobalScreen.addNativeMouseMotionListener(this);
        GlobalScreen.addNativeMouseWheelListener(this);
    }

    public void removeListeners() throws NativeHookException {
        GlobalScreen.isNativeHookRegistered();
        GlobalScreen.removeNativeMouseListener(this);
        GlobalScreen.removeNativeMouseMotionListener(this);
        GlobalScreen.removeNativeMouseWheelListener(this);
    }

    /**
     * @see NativeKeyListener#nativeKeyPressed(NativeKeyEvent)
     */
    public void nativeKeyPressed(NativeKeyEvent e) {
        String keyChar = e.paramString().substring(e.paramString().lastIndexOf("keyText="));
        keyChar = keyChar.substring(8,keyChar.indexOf(","));

        if(keyChar.equals("F5")){
            confirmDiscard();
        }

        if (keyChar.equals("F2")){
            //stop if running
        }

        if (keyChar.equals("F1")){
            try {
                changeStatus(!isRecording);
            } catch (NativeHookException ex) {
                throw new RuntimeException(ex);
            }
        }else if (!pressedKeys.containsKey(keyChar) && isRecording && !keyChar.equals("F2")) {
            start = System.currentTimeMillis();
            pressedKeys.put(keyChar, e.getKeyCode());
            lastKeyPressed = keyChar;
            ctrlPressed = keyChar.equals("Ctrl");
            if (tickClick.isSelected() && ctrlPressed) {
                pressedKeys.put(keyChar, e.getKeyCode());
            }else {
                lastEvent = System.currentTimeMillis();
                updateTable(keyChar, "Key Pressed "+ e.getKeyCode(), delay + "ms");
                delay = delayCalc(start, lastEvent, false);
            }
        }
    }

    /**
     * @see NativeKeyListener#nativeKeyReleased(NativeKeyEvent)
     */
    public void nativeKeyReleased(NativeKeyEvent e) {
        String keyChar = e.paramString().substring(e.paramString().lastIndexOf("keyText="));
        keyChar = keyChar.substring(8,keyChar.indexOf(","));

        if (!lastKeyPressed.equals("") && isRecording && (!keyChar.equals("F1") || !keyChar.equals("F2"))) {
            start = System.currentTimeMillis();
            lastKeyPressed = "FREE";
            pressedKeys.remove(keyChar,e.getKeyCode());
            lastEvent = System.currentTimeMillis();
            ctrlPressed = pressedKeys.containsKey("Ctrl");
            if (tickClick.isSelected() && !ctrlPressed){
                ctrlPressed = false;
            } else {
                updateTable(keyChar, "Key Released "+ e.getKeyCode(), delay + "ms");
                delay = delayCalc(start, lastEvent, false);
            }
        }
    }

    /**
     * @see NativeKeyListener#nativeKeyTyped(NativeKeyEvent)
     */
    public void nativeKeyTyped(NativeKeyEvent e) { }

    /**
     * @see NativeMouseListener#nativeMousePressed(NativeMouseEvent)
     */
    public void nativeMousePressed(NativeMouseEvent e) {
        if (isRecording) {
            start = System.currentTimeMillis();
            Point p = MouseInfo.getPointerInfo().getLocation();
            lastEvent = System.currentTimeMillis();
            updateTable(p.x+ ", " +p.y, "Mouse Pressed "+ (e.getButton() == 1 ? "LEFT" : "RIGHT"),
                    ((e.getButton() == 1 && ctrlPressed)? delayCalc(start, lastEvent, true) : delay) + "ms");
            delay = delayCalc(start, lastEvent, false);
        }
    }

    /**
     * @see NativeMouseListener#nativeMouseReleased(NativeMouseEvent)
     */
    public void nativeMouseReleased(NativeMouseEvent e) {
        if (isRecording) {
            start = System.currentTimeMillis();
            Point p = MouseInfo.getPointerInfo().getLocation();
            lastEvent = System.currentTimeMillis();
            updateTable(p.x+ ", " +p.y, "Mouse Released "+ (e.getButton() == 1 ? "LEFT" : "RIGHT"), delay + "ms");
            delay = delayCalc(lastEvent, start, false);
        }
    }

    /**
     * @see NativeMouseListener#nativeMouseClicked(NativeMouseEvent)
     */
    public void nativeMouseClicked(NativeMouseEvent e) {

    }

    /**
     * @see NativeMouseMotionListener#nativeMouseMoved(NativeMouseEvent)
     */
    public void nativeMouseMoved(NativeMouseEvent e) { }

    /**
     * @see NativeMouseMotionListener#nativeMouseDragged(NativeMouseEvent)
     */
    public void nativeMouseDragged(NativeMouseEvent e) { }

    /**
     * @see NativeMouseWheelListener#nativeMouseWheelMoved(NativeMouseWheelEvent)
     */
    public void nativeMouseWheelMoved(NativeMouseWheelEvent e) {
        if (isRecording) {
            start = System.currentTimeMillis();
            Point p = MouseInfo.getPointerInfo().getLocation();
            lastEvent = System.currentTimeMillis();
            updateTable((e.getWheelRotation() == 1 ? "DOWN" : "UP"), "Mouse Wheel", delay + "ms");
            delay = delayCalc(start , lastEvent, false);
        }
    }

    private void confirmDiscard (){
        try {
            changeStatus(false);
        } catch (NativeHookException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (modelTabela.getRowCount() > 0){
                int resp = JOptionPane.showConfirmDialog(null,"Do you really want to discard the " +
                        "current recording?","Warning!",JOptionPane.YES_NO_OPTION);

                if(resp == 0){
                    modelTabela.setRowCount(0);
                }
            }
        }

    }

    private long delayCalc (long end, long start, Boolean tickable) {
        return tickable ? (tickBase == 0) ? delay : tickBase : end - start;
    }

    private void initComponents(){
        frame = new JFrame("Basics");

        //inicialização de variaveis
        pressedKeys = new HashMap<>();

        hasKeyPressed = false;
        isRecording = false;
        ctrlPressed = false;

        row = new String[3];
        lastKeyPressed = "";

        lastEvent = 0;
        tickBase = 0;
        start = 0;
        delay = 1;

        lastPoint = null;

        timesTo.setModel(new SpinnerNumberModel(0,0,null,1));
        JFormattedTextField numericTimesTo = ((JSpinner.NumberEditor) timesTo.getEditor()).getTextField();
        ((NumberFormatter) numericTimesTo.getFormatter()).setAllowsInvalid(false);

        tabela = new JTable();
        modelTabela = new DefaultTableModel(
                new Object[][] { },
                new String[] {
                        "Input", "Event", "Delay"
                });
        tabela.setModel(modelTabela);
        tabela.getTableHeader().setReorderingAllowed(false);
        scrollPn.setViewportView(tabela);

        tableModel = (DefaultTableModel) this.tabela.getModel();

        tickClick.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tickBase = tickClick.isSelected() ? 600 : 0; //0.6s
            }
        });

        icon = new ImageIcon("src/main/java/org/icons/robot-excited-outline.png");

        //actionPerformed dos botões
        recordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    recordAction(e);
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
}