/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.acore;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.DumpGLInfo;
import com.jogamp.opengl.test.junit.util.GLClearColor;
import com.jogamp.opengl.test.junit.util.GLEventListenerCounter;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug1146GLContextDialogToolTipAWT extends UITestCase {
    static final int NB_TEST = 5;
    static final int ACTION_DELAY = 500;
    static final int MOVE_DELAY = 2;
    static final int MOVE_ITER = 100;
    static final int TOOLTIP_WAIT = 3*1000; // 5s

    static AbstractButton findButton(final int depth, final Container c, final String buttonText) {
        AbstractButton res = null;
        final int cc = c.getComponentCount();
        for(int i=0; null==res && i<cc; i++) {
            final Component e = c.getComponent(i);
            // System.err.println("["+depth+"]["+i+"]: "+e.getClass().getSimpleName()+": "+e);
            if( e instanceof AbstractButton ) {
                final AbstractButton b = (AbstractButton) e;
                final String bT = b.getText();
                if( buttonText.equals(bT) ) {
                    res = b;
                }
            } else if( e instanceof Container ) {
                res = findButton(depth+1, (Container)e, buttonText);
            }
        }
        return res;
    }

    private void oneTest(final GLCapabilitiesImmutable caps) {
        // base dialog
        final JDialog dialog = new JDialog((Window) null);
        dialog.setMinimumSize(new Dimension(500, 300));
        dialog.setPreferredSize(new Dimension(500, 300));

        dialog.setModal(false);

        // build accessory
        final GLEventListenerCounter glelc1 = new GLEventListenerCounter();
        final GLCanvas canvas1 = new GLCanvas(caps);
        canvas1.addGLEventListener(new DumpGLInfo(Platform.getNewline()+Platform.getNewline()+"Pre-ToolTip", false, false, false));
        canvas1.addGLEventListener(new GLClearColor(1f, 0f, 0f, 1f));
        canvas1.addGLEventListener(glelc1);
        final JPanel panel1 = new JPanel(new BorderLayout());
        panel1.add(canvas1, BorderLayout.CENTER);
        panel1.setPreferredSize(new Dimension(300, 300));

        final GLEventListenerCounter glelc2 = new GLEventListenerCounter();
        final GLCanvas canvas2 = new GLCanvas(caps);
        canvas2.addGLEventListener(new DumpGLInfo(Platform.getNewline()+Platform.getNewline()+"Post-ToolTip", false, false, false));
        canvas2.addGLEventListener(new GLClearColor(0f, 0f, 1f, 1f));
        canvas2.addGLEventListener(glelc2);
        final JPanel panel2 = new JPanel(new BorderLayout());
        panel2.add(canvas2, BorderLayout.CENTER);
        panel2.setPreferredSize(new Dimension(300, 300));

        // create file chooser with accessory
        final JFileChooser fileChooser = new JFileChooser();
        final String approveButtonText = "Approved";
        fileChooser.setApproveButtonText(approveButtonText);
        fileChooser.setApproveButtonToolTipText("Tool-Tip for Approved");
        fileChooser.setAccessory(panel1);

        final Locale l = fileChooser.getLocale();
        final String cancelButtonText = UIManager.getString("FileChooser.cancelButtonText",l);

        // launch robot action ..
        new InterruptSource.Thread()
        {
            public void run()
            {
                try {
                    Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(fileChooser, true));
                    Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(canvas1, true));

                    final Point approveButtonPos;
                    final AbstractButton approveButton = findButton(0, fileChooser, approveButtonText);
                    if( null != approveButton ) {
                        approveButtonPos = approveButton.getLocationOnScreen();
                        final Dimension approveButtonSize = approveButton.getSize();
                        approveButtonPos.translate(approveButtonSize.width/2, approveButtonSize.height/2);
                        System.err.println("OK Button: "+approveButton.getClass().getSimpleName()+"; "+approveButton+", "+approveButtonPos);
                    } else {
                        System.err.println("OK Button: NULL");
                        approveButtonPos = null;
                    }
                    final Point cancelButtonPos;
                    final AbstractButton cancelButton = findButton(0, fileChooser, cancelButtonText);
                    if( null != approveButton ) {
                        cancelButtonPos = cancelButton.getLocationOnScreen();
                        final Dimension cancelButtonSize = cancelButton.getSize();
                        cancelButtonPos.translate(cancelButtonSize.width/2, cancelButtonSize.height/2);
                        System.err.println("CANCEL Button: "+cancelButton.getClass().getSimpleName()+"; "+cancelButton+", "+cancelButtonPos);
                    } else {
                        cancelButtonPos = null;
                        System.err.println("CANCEL Button: NULL");
                    }
                    final Robot robot = new Robot();
                    // hover to 'approve' -> tool tip
                    if( null != approveButtonPos ) {
                        AWTRobotUtil.mouseMove(robot, approveButtonPos, MOVE_ITER, MOVE_DELAY);
                        java.lang.Thread.sleep(TOOLTIP_WAIT);
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {
                                public void run() {
                                    fileChooser.setAccessory(panel2);
                                    fileChooser.validate();
                                } } ) ;
                        } catch (final InterruptedException e) {
                            e.printStackTrace();
                        } catch (final InvocationTargetException e) {
                            e.printStackTrace();
                        }
                        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(canvas2, true));
                    }
                    if( null != cancelButtonPos ) {
                        AWTRobotUtil.mouseClick(robot, cancelButtonPos, MOVE_ITER, MOVE_DELAY, ACTION_DELAY);
                    } else {
                        // oops ..
                        fileChooser.cancelSelection();
                    }
                } catch (final AWTException e1) {
                    e1.printStackTrace();
                } catch (final InterruptedException e2) {
                    e2.printStackTrace();
                }
            }
        }.start();

        // show file chooser dialog
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    dialog.setVisible(true);
                    fileChooser.showOpenDialog(dialog);
                } } ) ;
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            e.printStackTrace();
        }

        // dispose of resources
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    canvas1.destroy();
                    canvas2.destroy();
                    dialog.setVisible(false);
                    dialog.dispose();
                } } ) ;
        } catch (final InterruptedException e) {
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(1, glelc1.initCount);
        Assert.assertEquals(1, glelc2.initCount);
    }


    @Test(timeout=180000) // TO 3 min
    public void test01() {
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        for (int i = 0; i < NB_TEST; i++) {
           System.out.println("Iteration  " + i + " / " + NB_TEST);
           oneTest(caps);
        }
    }

    public static void main(final String[] pArgs)
    {
        org.junit.runner.JUnitCore.main(TestBug1146GLContextDialogToolTipAWT.class.getName());
    }
}