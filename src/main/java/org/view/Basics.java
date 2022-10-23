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

    // variáveis manipuláveis
    private long start;
    private long delay;
    private long lastEvent;
    private String[] row;
    private Boolean isRecording;
    private String lastKeyPressed;
    private String lastKeyReleased;
    private Boolean lastKeyIsReleased;
    private Boolean hasKeyPressed;
    private HashMap<String, Integer> pressedKeys;

    //recursos
    private Robot robot;
    private GeneralPath gp;
    private Timer timer;
    private Point lastPoint;

    public Basics () {
        initComponents();
    }

    private void mouseMoveOnScreen () throws NativeHookException {
        registerListeners();

        gp = new GeneralPath();
        Point p = MouseInfo.getPointerInfo().getLocation();
        gp.moveTo(p.x, p.y);

        ActionListener al = new ActionListener() {
            Point lastPoint;

            @Override
            public void actionPerformed(ActionEvent e) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                if (!p.equals(lastPoint)) {
                    start = System.currentTimeMillis();
                    gp.lineTo(p.x, p.y);
                    updateTable(p.x+ ", " +p.y, "Mouse Movement", delay + "ms");
                }
                lastEvent  = System.currentTimeMillis();

                delay = lastEvent - start;
                lastPoint = p;
            }
        };
        if(isRecording){
            timer = new Timer(0, al);
            timer.start();
        } else {
            timer.stop();
        }
    }

    private void updateTable (String input, String event, String delayMs) {
        row[0] = input;
        row[1] = event;
        row[2] = delayMs;
        tableModel.addRow(row);
        tabela.scrollRectToVisible(tabela.getCellRect(tabela.getRowCount()-1, 0, true));
    }

    private void changeStatus() throws NativeHookException {
        if (isRecording) {
            lbStatus.setText("RECORDING");
            lbStatus.setForeground(new Color(60,179,113));
            recordButton.setText("Stop");
        }   else {
            lbStatus.setText("IDLE");
            lbStatus.setForeground(new Color(0,0,0));
            recordButton.setText("Record");
            removeListeners();
        }
    }

    private void recordAction (ActionEvent e) throws AWTException, NativeHookException {
        isRecording = !isRecording;
        lastKeyPressed = "";
        changeStatus();
        mouseMoveOnScreen();

        if (!isRecording) {
            removeListeners();
        }
    }

    public void registerListeners() throws NativeHookException {
        GlobalScreen.setEventDispatcher(new SwingDispatchService());
        GlobalScreen.isNativeHookRegistered();
        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(this);
        GlobalScreen.addNativeMouseListener(this);
        GlobalScreen.addNativeMouseMotionListener(this);
        GlobalScreen.addNativeMouseWheelListener(this);
    }

    public void removeListeners() throws NativeHookException {
        GlobalScreen.isNativeHookRegistered();
        GlobalScreen.unregisterNativeHook();
        GlobalScreen.removeNativeKeyListener(this);
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

        if(!pressedKeys.containsKey(keyChar)){
            start = System.currentTimeMillis();
            pressedKeys.put(keyChar, e.getKeyCode());
            lastKeyPressed = keyChar;
            lastKeyIsReleased = false;
            lastEvent = System.currentTimeMillis();
            updateTable(keyChar, "Key Pressed "+ e.getKeyCode(), delay + "ms");
            delay = start - lastEvent;
        }
    }

    /**
     * @see NativeKeyListener#nativeKeyReleased(NativeKeyEvent)
     */
    public void nativeKeyReleased(NativeKeyEvent e) {
        String keyChar = e.paramString().substring(e.paramString().lastIndexOf("keyText="));
        keyChar = keyChar.substring(8,keyChar.indexOf(","));

        if (!lastKeyPressed.equals("")){
            start = System.currentTimeMillis();
            lastKeyReleased = keyChar;
            lastKeyPressed = "FREE";
            lastKeyIsReleased = true;
            pressedKeys.remove(keyChar,e.getKeyCode());
            lastEvent = System.currentTimeMillis();
            updateTable(keyChar, "Key Released "+ e.getKeyCode(), delay + "ms");
            delay = start - lastEvent;
        }
    }

    /**
     * @see NativeKeyListener#nativeKeyTyped(NativeKeyEvent)
     */
    public void nativeKeyTyped(NativeKeyEvent e) {

    }

    /**
     * @see NativeMouseListener#nativeMousePressed(NativeMouseEvent)
     */
    public void nativeMousePressed(NativeMouseEvent e) {
        start = System.currentTimeMillis();
        Point p = MouseInfo.getPointerInfo().getLocation();
        lastEvent = System.currentTimeMillis();
        updateTable(p.x+ ", " +p.y, "Mouse Pressed "+ (e.getButton() == 1 ? "LEFT" : "RIGHT"), delay + "ms");
        delay = start - lastEvent;
    }

    /**
     * @see NativeMouseListener#nativeMouseReleased(NativeMouseEvent)
     */
    public void nativeMouseReleased(NativeMouseEvent e) {
        start = System.currentTimeMillis();
        Point p = MouseInfo.getPointerInfo().getLocation();
        lastEvent = System.currentTimeMillis();
        updateTable(p.x+ ", " +p.y, "Mouse Released "+ (e.getButton() == 1 ? "LEFT" : "RIGHT"), delay + "ms");
        delay = start - lastEvent;
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
        start = System.currentTimeMillis();
        Point p = MouseInfo.getPointerInfo().getLocation();
        lastEvent = System.currentTimeMillis();
        updateTable((e.getWheelRotation() == 1 ? "DOWN" : "UP"), "Mouse Wheel", delay + "ms");
        delay = start - lastEvent;
    }

    private void initComponents(){
        frame = new JFrame("Basics");

        //inicialização de variaveis
        row = new String[3];
        start = 0;
        delay = 1;
        lastEvent = 0;
        isRecording = false;
        lastKeyIsReleased = true;
        lastKeyPressed = "";
        lastKeyReleased = "";
        lastPoint = null;
        hasKeyPressed = false;
        pressedKeys = new HashMap<>();

        timesTo.setModel(new SpinnerNumberModel(0,0,null,1));
        JFormattedTextField numericTimesTo = ((JSpinner.NumberEditor) timesTo.getEditor()).getTextField();
        ((NumberFormatter) numericTimesTo.getFormatter()).setAllowsInvalid(false);

        tabela = new JTable();
        tabela.setModel(new DefaultTableModel(
                new Object[][] { },
                new String[] {
                        "Input", "Event", "Delay"
                }
        ));
        tabela.getTableHeader().setReorderingAllowed(false);
        scrollPn.setViewportView(tabela);

        tableModel = (DefaultTableModel) this.tabela.getModel();

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