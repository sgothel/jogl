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
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Bug 816: OSX CALayer Positioning Bug.
 * <p>
 * Diff. OSX CALayer positioning w/ java6, [7uxx..7u40[, and >= 7u40
 * </p>
 * <p>
 * Test simply positions a GLCanvas via setBounds(..) within it's Applet.
 * </p>
 */
@SuppressWarnings("serial")
public class Bug816AppletGLCanvas01 extends Applet implements GLEventListener {

    public Bug816AppletGLCanvas01() {
    }

    public static JFrame frame;
    public static JPanel appletHolder;
    public static boolean isApplet = true;

    static public void main(final String args[]) {
        Applet myApplet = null;
        isApplet = false;

        myApplet = new Bug816AppletGLCanvas01();
        appletStarter(myApplet, "Bug861AppletGLCanvasTest01", 800, 600);
    }

    static public void appletStarter(final Applet des, final String frameName, final int width, final int height) {
        appletHolder = new JPanel();
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        frame = new JFrame(frameName);
        frame.setVisible(false);
        frame.getContentPane().add(appletHolder);

        appletHolder.setLayout(null);
        des.setBounds(0, 0, width, height);
        appletHolder.add(des);

        frame.setVisible(true);
        final int frameBorderSize = appletHolder.getLocationOnScreen().x - frame.getLocationOnScreen().x;
        final int titleBarHeight = appletHolder.getLocationOnScreen().y - frame.getLocationOnScreen().y;
        final int frameWidth = width + 2 * frameBorderSize;
        final int frameHeight = height + titleBarHeight + frameBorderSize;
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(frameWidth, frameHeight);
        frame.setVisible(true);
        des.init();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(final WindowEvent e) {
                System.exit(0);
            }
        });
    }

    public void init() {
        initOpenGLAWT();
    }

    public void initOpenGLAWT() {
        setBackground(Color.gray);
        setLayout(null);

        final GLProfile glp = GLProfile.getDefault();
        final GLCapabilities caps = new GLCapabilities(glp);
        final GLCanvas canvas = new GLCanvas(caps);
        canvas.setBounds(50, 50, 200, 450);
        canvas.addGLEventListener(this);
        add(canvas);
    }

    public void init(final GLAutoDrawable gLAutoDrawable) {
        final GL gl = gLAutoDrawable.getGL();
        gl.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gLAutoDrawable.swapBuffers();
    }

    public void dispose(final GLAutoDrawable glad) {
    }

    public void display(final GLAutoDrawable glad) {
    }

    public void reshape(final GLAutoDrawable glad, final int i, final int i1, final int i2, final int i3) {
    }

}
