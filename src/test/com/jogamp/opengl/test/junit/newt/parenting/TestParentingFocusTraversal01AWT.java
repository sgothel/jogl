/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 
package com.jogamp.opengl.test.junit.newt.parenting;

import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.AWTException;
import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Robot;

import javax.media.opengl.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;

import java.io.IOException;

import jogamp.newt.driver.DriverClearFocus;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

public class TestParentingFocusTraversal01AWT extends UITestCase {
    static Dimension glSize, fSize;
    static int numFocus = 8;
    static long durationPerTest = numFocus * 100;
    static GLCapabilities glCaps;
    static boolean manual = false;

    @BeforeClass
    public static void initClass() {
        glSize = new Dimension(200,200);
        fSize = new Dimension(300,300);
        glCaps = new GLCapabilities(null);
    }

    @Test
    public void testWindowParentingAWTFocusTraversal01Onscreen() throws InterruptedException, InvocationTargetException, AWTException {
        testWindowParentingAWTFocusTraversal(true);
    }

    @Test
    public void testWindowParentingAWTFocusTraversal02Offscreen() throws InterruptedException, InvocationTargetException, AWTException {
        testWindowParentingAWTFocusTraversal(false);
    }

    public void testWindowParentingAWTFocusTraversal(boolean onscreen) throws InterruptedException, InvocationTargetException, AWTException {
        Robot robot = new Robot();
        
        // Bug 4908075 - http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4908075
        // Bug 6463168 - http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6463168
        {
            final KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            final Set<AWTKeyStroke> bwdKeys = kfm.getDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);         
            final AWTKeyStroke newBack = AWTKeyStroke.getAWTKeyStroke(java.awt.event.KeyEvent.VK_BACK_SPACE, 0, false);
            Assert.assertNotNull(newBack);
            final Set<AWTKeyStroke> bwdKeys2 = new HashSet<AWTKeyStroke>(bwdKeys); 
            bwdKeys2.add(newBack);
            kfm.setDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, bwdKeys2);
        }
        {
            final KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            final Set<AWTKeyStroke> fwdKeys = kfm.getDefaultFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);             
            final Set<AWTKeyStroke> bwdKeys = kfm.getDefaultFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);         
            Iterator<AWTKeyStroke> iter;
            for(iter = fwdKeys.iterator(); iter.hasNext(); ) {
                System.err.println("FTKL.fwd-keys: "+iter.next());
            }
            for(iter = bwdKeys.iterator(); iter.hasNext(); ) {
                System.err.println("FTKL.bwd-keys: "+iter.next());
            }
        }
        
        final Frame frame1 = new Frame("AWT Parent Frame");
        final Button cWest = new Button("WEST");
        final Button cEast = new Button("EAST");
        final GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setUpdateFPSFrames(1, null);
        final NewtCanvasAWT newtCanvasAWT1 = new NewtCanvasAWT(glWindow1);
        newtCanvasAWT1.setPreferredSize(glSize);
        newtCanvasAWT1.setShallUseOffscreenLayer(!onscreen);
        newtCanvasAWT1.setFocusable(true);

        // Test FocusAdapter
        NEWTFocusAdapter glWindow1FA = new NEWTFocusAdapter("GLWindow1");
        glWindow1.addWindowListener(glWindow1FA);
        AWTFocusAdapter bWestFA = new AWTFocusAdapter("WEST");
        cWest.addFocusListener(bWestFA);
        AWTFocusAdapter bEastFA = new AWTFocusAdapter("EAST");
        cEast.addFocusListener(bEastFA);
        
        // Test KeyAdapter
        NEWTKeyAdapter glWindow1KA = new NEWTKeyAdapter("GLWindow1");
        glWindow1.addKeyListener(glWindow1KA);
        AWTKeyAdapter bWestKA = new AWTKeyAdapter("West");
        cWest.addKeyListener(bWestKA);
        AWTKeyAdapter bEastKA = new AWTKeyAdapter("East");
        cEast.addKeyListener(bEastKA);
        
        // demo ..
        GLEventListener demo1 = new GearsES2(1);
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        glWindow1.addKeyListener(new NewtAWTReparentingKeyAdapter(frame1, newtCanvasAWT1, glWindow1));
        glWindow1.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar()=='c') {                    
                    System.err.println("Focus Clear");
                    if(glWindow1.getDelegatedWindow() instanceof DriverClearFocus) {
                         ((DriverClearFocus)glWindow1.getDelegatedWindow()).clearFocus();
                    }
                } else if(e.getKeyChar()=='e') {                    
                    System.err.println("Focus East");
                    try {
                        java.awt.EventQueue.invokeLater(new Runnable() {
                           public void run() {
                               cEast.requestFocusInWindow();
                           }                         
                        });
                    } catch (Exception ex) { ex.printStackTrace(); }
                } else if(e.getKeyChar()=='w') {                    
                    System.err.println("Focus West");
                    try {
                        java.awt.EventQueue.invokeLater(new Runnable() {
                           public void run() {
                               cWest.requestFocusInWindow();
                           }                         
                        });
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }            
        });
        GLAnimatorControl animator1 = new Animator(glWindow1);
        animator1.start();

        // make frame
        frame1.setLayout(new BorderLayout());
        frame1.setLayout(new BorderLayout());
        frame1.add(cWest, BorderLayout.WEST);
        frame1.add(newtCanvasAWT1, BorderLayout.CENTER);
        frame1.add(cEast, BorderLayout.EAST);

        frame1.setLocation(0, 0);
        frame1.setSize(fSize);
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame1.validate();                
                frame1.setVisible(true);
            }});
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(glWindow1, true));
        Assert.assertEquals(newtCanvasAWT1.getNativeWindow(),glWindow1.getParent());
                        
        Assert.assertEquals(true, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertNotNull(animator1.getThread());
          
        if(manual) {
            Thread.sleep(durationPerTest);            
        } else {
            //
            // initial focus on bWest
            //        
            AWTRobotUtil.assertRequestFocusAndWait(robot, cWest, cWest, bWestFA, null);
            Assert.assertEquals(true,  bWestFA.focusGained());
            Thread.sleep(durationPerTest/numFocus);
                    
            //
            // forth
            //
            
            // bWest -> glWin
            AWTRobotUtil.keyType(0, robot, java.awt.event.KeyEvent.VK_TAB, cWest, null);
            Assert.assertTrue("Did not gain focus", AWTRobotUtil.waitForFocus(glWindow1, glWindow1FA, bWestFA)); 
            Assert.assertEquals(true,  glWindow1FA.focusGained());
            Assert.assertEquals(true,  bWestFA.focusLost());
            Thread.sleep(durationPerTest/numFocus);
            
            // glWin -> bEast
            AWTRobotUtil.keyType(0, robot, java.awt.event.KeyEvent.VK_TAB, glWindow1, null);
            Assert.assertTrue("Did not gain focus", AWTRobotUtil.waitForFocus(cEast, bEastFA, glWindow1FA)); 
            Assert.assertEquals(true,  bEastFA.focusGained());
            Assert.assertEquals(true,  glWindow1FA.focusLost());
            Thread.sleep(durationPerTest/numFocus);
    
            //
            // back (using custom back traversal key 'backspace')
            //
            // bEast -> glWin
            AWTRobotUtil.keyType(0, robot, java.awt.event.KeyEvent.VK_BACK_SPACE, cEast, null);
            Assert.assertTrue("Did not gain focus", AWTRobotUtil.waitForFocus(glWindow1, glWindow1FA, bEastFA)); 
            Assert.assertEquals(true,  glWindow1FA.focusGained());
            Assert.assertEquals(true,  bEastFA.focusLost());
            Thread.sleep(durationPerTest/numFocus);
    
            AWTRobotUtil.keyType(0, robot, java.awt.event.KeyEvent.VK_BACK_SPACE, glWindow1, null);
            Assert.assertTrue("Did not gain focus", AWTRobotUtil.waitForFocus(cWest, bWestFA, glWindow1FA)); 
            Assert.assertEquals(true,  bWestFA.focusGained());
            Assert.assertEquals(true,  glWindow1FA.focusLost());
            Thread.sleep(durationPerTest/numFocus);  
            
            // direct AWT request focus
            try {
                java.awt.EventQueue.invokeAndWait(new Runnable() {
                   public void run() {
                       newtCanvasAWT1.requestFocus();
                   }                         
                });
            } catch (Exception ex) { ex.printStackTrace(); }            
            Assert.assertTrue("Did not gain focus", AWTRobotUtil.waitForFocus(glWindow1, glWindow1FA, bWestFA)); 
            Assert.assertEquals(true,  glWindow1FA.focusGained());
            Assert.assertEquals(true,  bWestFA.focusLost());
            Thread.sleep(durationPerTest/numFocus);
            
            // direct AWT request focus
            try {
                java.awt.EventQueue.invokeAndWait(new Runnable() {
                   public void run() {
                       cWest.requestFocus();
                   }                         
                });
            } catch (Exception ex) { ex.printStackTrace(); }            
            Assert.assertTrue("Did not gain focus", AWTRobotUtil.waitForFocus(cWest, bWestFA, glWindow1FA)); 
            Assert.assertEquals(true,  bWestFA.focusGained());
            Assert.assertEquals(true,  glWindow1FA.focusLost());
            Thread.sleep(durationPerTest/numFocus);
            
            // direct NEWT request focus
            glWindow1.requestFocus();
            Assert.assertTrue("Did not gain focus", AWTRobotUtil.waitForFocus(glWindow1, glWindow1FA, bWestFA)); 
            Assert.assertEquals(true,  glWindow1FA.focusGained());
            Assert.assertEquals(true,  bWestFA.focusLost());
            Thread.sleep(durationPerTest/numFocus);            
        }
        
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertEquals(null, animator1.getThread());

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame1.dispose();
            } } );
        glWindow1.destroy();
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow, boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        Window window = glWindow.getDelegatedWindow();
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", window)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            } else if(args[i].equals("-manual")) {
                manual = true;
            }
        }
        String tstname = TestParentingFocusTraversal01AWT.class.getName();
        /*
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } ); */
        org.junit.runner.JUnitCore.main(tstname);
    }

}
