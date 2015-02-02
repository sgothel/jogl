/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.jogl.demos.es2.awt;

import java.applet.Applet;
import java.awt.BorderLayout;

import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.util.Animator;

/**
 * Bug 816: OSX CALayer Positioning Bug - AWT Applet w/ 1 JRootPane and 2 JSplitPanes
 * <p>
 * Diff. OSX CALayer positioning w/ java6, [7uxx..7u40[, and >= 7u40
 * </p>
 * <p>
 * See also {@link com.jogamp.opengl.test.junit.jogl.awt.TestBug816OSXCALayerPos02AWT}
 * </p>
 * Bug persists in browser (Firefox, Safari) and not in appletviewer!
 */
@SuppressWarnings("serial")
public class Bug816AppletOSXCALayerPos03a extends Applet {
    GLAnimatorControl animator;
    boolean added = false;

    @Override
    public void init() {
        System.err.println("GearsApplet: init() - begin [visible "+isVisible()+", displayable "+isDisplayable()+"] - "+currentThreadName());
        final GLProfile glp = GLProfile.getDefault();
        final GLCapabilities caps = new GLCapabilities(glp);
        final GLCanvas glCanvas1 = new GLCanvas(caps);
        glCanvas1.addGLEventListener(new GearsES2(1));

        animator = new Animator();
        animator.add(glCanvas1);
        setLayout(new BorderLayout());

        // Build a GUI where the canvas 3D is located at top right of the frame
        // and can be resized with split panes dividers
        final JSplitPane verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            true, new JScrollPane(), glCanvas1);
        verticalSplitPane.setResizeWeight(0.5);
        final JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            true, new JScrollPane(), verticalSplitPane);
        horizontalSplitPane.setResizeWeight(0.5);
        final JRootPane intermediateRootPane = new JRootPane();
        intermediateRootPane.setContentPane(horizontalSplitPane);
        add(intermediateRootPane, BorderLayout.CENTER);
        System.err.println("GearsApplet: init() - end [visible "+isVisible()+", displayable "+isDisplayable()+"] - "+currentThreadName());
    }

    String currentThreadName() {
        return Thread.currentThread().getName();
    }

    @Override
    public void start() {
        System.err.println("GearsApplet: start() - begin [visible "+isVisible()+", displayable "+isDisplayable()+"] - "+currentThreadName());
        animator.start();
        animator.setUpdateFPSFrames(60, System.err);
        System.err.println("GearsApplet: start() - end [visible "+isVisible()+", displayable "+isDisplayable()+"] - "+currentThreadName());
    }

    @Override
    public void stop() {
        System.err.println("GearsApplet: stop() - [visible "+isVisible()+", displayable "+isDisplayable()+"] - "+currentThreadName());
        animator.stop();
    }
}
