package org.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.GeneralPath;
import java.util.logging.Logger;

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
    private JComboBox keyToggleRecording;
    private JComboBox keyTogglePlay;
    private JComboBox keyToggleDiscardCurrent;
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
    private long start = 0;
    private long delay = 1;
    private long lastEvent = 0;
    private String[] row;
    private Boolean isRecording = false;
    private String lastKeyPressed = "";
    private Boolean lastKeyIsReleased = true;

    //recursos
    private Robot robot;
    private GeneralPath gp;
    private Timer timer;
    private Point lastPoint = null;

    private static final Logger log = Logger.getLogger(GlobalScreen.class.getPackage().getName());

    public Basics () {
        initComponents();
    }

    private void mouseMoveOnScreen () throws NativeHookException {
        GlobalScreen.setEventDispatcher(new SwingDispatchService());
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
                    row[0] = "" +p.x+ ", " +p.y;
                    row[1] = "Mouse Movement";
                    row[2] = ""+ delay + "ms";
                    updateTable(row);
                }
                delay  = System.currentTimeMillis() - start;
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

    private void updateTable (String[] row) {
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
        changeStatus();
        mouseMoveOnScreen();

        if (!isRecording) {
            removeListeners();
        }
    }

    public void registerListeners() throws NativeHookException {
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

        if(!keyChar.equals(lastKeyPressed) && lastKeyIsReleased){
            start = System.currentTimeMillis();

            Point p = MouseInfo.getPointerInfo().getLocation();
            row[0] = ""+ keyChar;
            row[1] = "Key Pressed "+ e.getKeyCode();
            row[2] = ""+ delay + "ms";
            delay = start - lastEvent;
            updateTable(row);

            lastEvent = System.currentTimeMillis();

            lastKeyPressed = keyChar;
            lastKeyIsReleased = false;
        }
    }

    /**
     * @see NativeKeyListener#nativeKeyReleased(NativeKeyEvent)
     */
    public void nativeKeyReleased(NativeKeyEvent e) {
        String keyChar = e.paramString().substring(e.paramString().lastIndexOf("keyText="));
        keyChar = keyChar.substring(8,keyChar.indexOf(","));

        start = System.currentTimeMillis();

        Point p = MouseInfo.getPointerInfo().getLocation();
        row[0] = ""+ keyChar;
        row[1] = "Key Released "+ e.getKeyCode();
        row[2] = ""+ delay + "ms";
        delay = start - lastEvent;
        updateTable(row);

        lastEvent = System.currentTimeMillis();

        lastKeyPressed = "";
        lastKeyIsReleased = true;
    }

    /**
     * @see NativeKeyListener#nativeKeyTyped(NativeKeyEvent)
     */
    public void nativeKeyTyped(NativeKeyEvent e) { }

    /**
     * @see NativeMouseListener#nativeMousePressed(NativeMouseEvent)
     */
    public void nativeMousePressed(NativeMouseEvent e) {
        start = System.currentTimeMillis();
        Point p = MouseInfo.getPointerInfo().getLocation();
        row[0] = "" +p.x+ ", " +p.y;
        row[1] = "Mouse Pressed "+ (e.getButton() == 1 ? "LEFT" : "RIGHT");
        row[2] = ""+ delay + "ms";
        delay = start - lastEvent;
        updateTable(row);
        lastEvent = System.currentTimeMillis();
    }

    /**
     * @see NativeMouseListener#nativeMouseReleased(NativeMouseEvent)
     */
    public void nativeMouseReleased(NativeMouseEvent e) {
        start = System.currentTimeMillis();
        Point p = MouseInfo.getPointerInfo().getLocation();
        row[0] = "" +p.x+ ", " +p.y;
        row[1] = "Mouse Released "+ (e.getButton() == 1 ? "LEFT" : "RIGHT");
        row[2] = ""+ delay + "ms";
        delay = start - lastEvent;
        updateTable(row);
        lastEvent = System.currentTimeMillis();
    }

    /**
     * @see NativeMouseListener#nativeMouseClicked(NativeMouseEvent)
     */
    public void nativeMouseClicked(NativeMouseEvent e) { }

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
        row[0] = ""+ (e.getWheelRotation() == 1 ? "DOWN" : "UP"); //default amount: 5
        row[1] = "Mouse Wheel ";
        row[2] = ""+ delay + "ms";
        delay = start - lastEvent;
        updateTable(row);
        lastEvent = System.currentTimeMillis();
    }

    private void initComponents(){
        frame = new JFrame("Basics");

        //definição de variaveis
        row = new String[3];

        timesTo.setModel(new SpinnerNumberModel(0,0,null,1));
        JFormattedTextField numericTimesTo = ((JSpinner.NumberEditor) timesTo.getEditor()).getTextField();
        ((NumberFormatter) numericTimesTo.getFormatter()).setAllowsInvalid(false);

        //fazer condição para evitar a mesma key nas duas (exceto NONE)
        keyToggleRecording.setModel(new DefaultComboBoxModel(
                new String[] {"NONE", "F1", "F2", "F3", "F4", "F5", "F6"}));
        keyTogglePlay.setModel(new DefaultComboBoxModel(
                new String[] {"NONE", "F1", "F2", "F3", "F4", "F5", "F6"}));
        keyToggleDiscardCurrent.setModel(new DefaultComboBoxModel(
                new String[] {"NONE", "F1", "F2", "F3", "F4", "F5", "F6"}));

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
