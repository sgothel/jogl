package com.jogamp.opengl.test.junit.jogl.awt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;

import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.Test;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;

public class TestAWTCardLayoutAnimatorStartStopBug532 extends UITestCase {
    static final String LABEL = "Label"; 
    static final String CANVAS = "GLCanvas";
    
    static long durationPerTest = 200*4; // ms    
    static boolean manual = false;
    static volatile boolean shouldStop = false;
    
    private String selected = LABEL;
    
    @Test
    public void testFPSAnimatorStartStop() throws InterruptedException, InvocationTargetException {
        testImpl(true, true);
    }
    
    @Test
    public void testFPSAnimatorResumePause() throws InterruptedException, InvocationTargetException {
        testImpl(false, true);
    }
    
    @Test
    public void testAnimatorStartStop() throws InterruptedException, InvocationTargetException {
        testImpl(true, false);
    }
    
    @Test
    public void testAnimatorResumePause() throws InterruptedException, InvocationTargetException {
        testImpl(false, false);
    }
    
    void testImpl(final boolean useAnimStartStop, boolean useFPSAnimator) throws InterruptedException, InvocationTargetException {
      final GLProfile glp = GLProfile.get(GLProfile.GL2); 
      final GLCapabilities caps = new GLCapabilities(glp); 
      final GLCanvas canvas = new GLCanvas(caps); 
      canvas.setPreferredSize(new Dimension(640, 480));
      canvas.addGLEventListener(new GearsES2(1));
      
      final GLAnimatorControl animatorCtrl = useFPSAnimator ? new FPSAnimator(canvas, 60) : new Animator(canvas);
      animatorCtrl.setUpdateFPSFrames(60, System.err);
      if(!useAnimStartStop) {
          animatorCtrl.start();
          animatorCtrl.pause();
      }
      final JFrame frame = new JFrame();
      frame.setTitle(getSimpleTestName());
      frame.addWindowListener(new WindowAdapter() { 
         public void windowClosing(WindowEvent e) {
            animatorCtrl.stop();
            shouldStop = true;
         } 
      });
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      
      final JPanel cards = new JPanel(new CardLayout());      
      final JPanel comboBoxPanel = new JPanel(); // nicer look ..
      final JComboBox comboBox = new JComboBox(new String[] { LABEL, CANVAS });
      comboBox.setEditable(false);
      comboBox.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent evt) {
            final String os = selected;
            CardLayout cl = (CardLayout)(cards.getLayout());
            String s = (String)evt.getItem();
            if(s.equals(CANVAS)) {
                if(useAnimStartStop) {
                    animatorCtrl.start();
                } else {
                    animatorCtrl.resume();
                }
                cl.show(cards, CANVAS); 
                selected = CANVAS;
            } else if(s.equals(LABEL)) {
                if(useAnimStartStop) {
                    animatorCtrl.stop(); 
                } else {
                    animatorCtrl.pause();
                }
                cl.show(cards, LABEL); 
                selected = LABEL;
            } else {
                throw new RuntimeException("oops .. unexpected item: "+evt);
            }
            System.err.println("Item Change "+os+" -> "+selected+", "+animatorCtrl);                
        }
      });
      comboBoxPanel.add(comboBox);            

      cards.add(new JLabel("A label to cover the canvas"), LABEL); 
      cards.add(canvas, CANVAS);
      
      frame.add(comboBoxPanel, BorderLayout.PAGE_START);
      frame.add(cards, BorderLayout.CENTER);
      
      frame.pack(); 
      
      javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
            frame.setVisible(true);
        }});
    
      if(manual) {
          for(long w=durationPerTest; !shouldStop && w>0; w-=100) {
              Thread.sleep(100);
          }
      } else {
          javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                comboBox.setSelectedItem(LABEL);
            }});
          Thread.sleep(durationPerTest/4);
          
          javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                comboBox.setSelectedItem(CANVAS);
            }});
          Thread.sleep(durationPerTest/4);
          
          javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                comboBox.setSelectedItem(LABEL);
            }});
          Thread.sleep(durationPerTest/4);
          
          javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                comboBox.setSelectedItem(CANVAS);
            }});
          Thread.sleep(durationPerTest/4);
      }
      
      javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
            frame.setVisible(false);
            frame.dispose();
        }});
      
    }
    
    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            } else if(args[i].equals("-manual")) {
                manual = true;
            }
        }
        org.junit.runner.JUnitCore.main(TestAWTCardLayoutAnimatorStartStopBug532.class.getName());
    }
}
