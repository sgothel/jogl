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
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAWTCardLayoutAnimatorStartStopBug532 extends UITestCase {
    static final String LABEL = "Label"; 
    static final String CANVAS = "GLCanvas";
    
    public enum AnimatorControlBehavior {
        StartStop, PauseResume, Continue;
    }
    
    static long durationPerTest = 200*4; // ms    
    static boolean manual = false;
    static volatile boolean shouldStop = false;
    
    private String selected = LABEL;
    
    @Test
    public void testFPSAnimatorStartStop() throws InterruptedException, InvocationTargetException {
        testImpl(AnimatorControlBehavior.StartStop, true);
    }
    
    @Test
    public void testFPSAnimatorResumePause() throws InterruptedException, InvocationTargetException {
        testImpl(AnimatorControlBehavior.PauseResume, true);
    }
    
    @Test
    public void testFPSAnimatorContinue() throws InterruptedException, InvocationTargetException {
        testImpl(AnimatorControlBehavior.Continue, true);
    }
    
    @Test
    public void testAnimatorStartStop() throws InterruptedException, InvocationTargetException {
        testImpl(AnimatorControlBehavior.StartStop, false);
    }
    
    @Test
    public void testAnimatorResumePause() throws InterruptedException, InvocationTargetException {
        testImpl(AnimatorControlBehavior.PauseResume, false);
    }
    
    @Test
    public void testAnimatorContinue() throws InterruptedException, InvocationTargetException {
        testImpl(AnimatorControlBehavior.Continue, false);
    }
    
    void testImpl(final AnimatorControlBehavior animCtrl, boolean useFPSAnimator) throws InterruptedException, InvocationTargetException {
      final GLProfile glp = GLProfile.get(GLProfile.GL2); 
      final GLCapabilities caps = new GLCapabilities(glp); 
      final GLCanvas canvas = new GLCanvas(caps); 
      canvas.setPreferredSize(new Dimension(640, 480));
      
      final GLAnimatorControl animatorCtrl = useFPSAnimator ? new FPSAnimator(canvas, 60) : new Animator(canvas);
      animatorCtrl.setUpdateFPSFrames(60, null);// System.err);
      switch (animCtrl) {
          case PauseResume:
              animatorCtrl.start();
              animatorCtrl.pause();
              break;
          case Continue:
              animatorCtrl.start();
              break;
          default:
      }

      canvas.addGLEventListener(new GearsES2(1));
      /* if(Platform.OS_TYPE == Platform.OSType.WINDOWS) {
          canvas.addGLEventListener(new GLEventListener() {
            public void init(GLAutoDrawable drawable) { } 
            public void dispose(GLAutoDrawable drawable) { }
            public void display(GLAutoDrawable drawable) {
                final NativeWindow win = (NativeWindow) drawable.getNativeSurface();
                long hdc = win.getSurfaceHandle();
                long hdw = win.getWindowHandle();
                long hdw_hdc = GDI.WindowFromDC(hdc);
                System.err.println("*** hdc 0x"+Long.toHexString(hdc)+", hdw(hdc) 0x"+Long.toHexString(hdw_hdc)+", hdw 0x"+Long.toHexString(hdw) + " - " + Thread.currentThread().getName() + ", " + animatorCtrl);
                // System.err.println(drawable.getNativeSurface().toString());
            }
            public void reshape(GLAutoDrawable drawable, int x, int y, int width,
                    int height) { }
          });
      } */

      final JFrame frame = new JFrame();
      frame.setTitle(getSimpleTestName(" - "));
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
        public void itemStateChanged(final ItemEvent evt) {
            final CardLayout cl = (CardLayout)(cards.getLayout());
            final String newSelection = (String)evt.getItem();
            if(!newSelection.equals(selected)) {
                final String oldSelected = selected;
                if(newSelection.equals(CANVAS)) {
                    cl.show(cards, CANVAS); 
                    switch (animCtrl) {
                       case StartStop:
                           animatorCtrl.start();
                           break;
                       case PauseResume:
                           animatorCtrl.resume();
                           break;
                       default:
                    }
                    selected = CANVAS;
                } else if(newSelection.equals(LABEL)) {
                    switch (animCtrl) {
                       case StartStop:
                           animatorCtrl.stop();
                           break;
                       case PauseResume:
                           animatorCtrl.pause();
                           break;
                       default:
                    }
                    cl.show(cards, LABEL); 
                    selected = LABEL;
                } else {
                    throw new RuntimeException("oops .. unexpected item: "+evt);
                }
                System.err.println("Item Change: "+oldSelected+" -> "+selected+", "+animatorCtrl);                
            } else {
                System.err.println("Item Stays: "+selected+", "+animatorCtrl);
            }
        }
      });
      comboBoxPanel.add(comboBox);            

      cards.add(new JLabel("A label to cover the canvas"), LABEL); 
      cards.add(canvas, CANVAS);
      
      frame.add(comboBoxPanel, BorderLayout.PAGE_START);
      frame.add(cards, BorderLayout.CENTER);
            
      javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
            frame.pack(); 
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
