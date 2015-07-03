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
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.BoxLayout;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.util.Animator;

/**
 * Bug 848: Applet on OSX w/ CALayer and 2 or more GLCanvas may 'crash'.
 * <p>
 * Test uses 2x3 GLCanvas in a box layout within the Applet.
 * </p>
 */
@SuppressWarnings("serial")
public class Bug848AppletGLCanvas01 extends Applet {
    private final List<GLAnimatorControl> animators = new ArrayList<GLAnimatorControl>(2);

    @Override
    public void init() {
        System.err.println("GearsApplet: init() - begin [visible "+isVisible()+", displayable "+isDisplayable()+"] - "+currentThreadName());
        new BoxLayout(this, BoxLayout.X_AXIS);
        setSize(1024, 664);
        add(createCanvas(0));
        add(createCanvas(0));
        add(createCanvas(0));
        add(createCanvas(0));
        add(createCanvas(0));
        add(createCanvas(1));
        System.err.println("GearsApplet: init() - end [visible "+isVisible()+", displayable "+isDisplayable()+"] - "+currentThreadName());
    }

    private GLCanvas createCanvas(final int vsyncI) {
        final GLCanvas canvas = new GLCanvas();
        canvas.addGLEventListener(new GearsES2(vsyncI));
        canvas.setSize(300, 300);
        animators.add(new Animator(canvas));
        return canvas;
    }

    String currentThreadName() {
        return Thread.currentThread().getName();
    }

    @Override
    public void start() {
        System.err.println("GearsApplet: start() - begin [visible "+isVisible()+", displayable "+isDisplayable()+"] - "+currentThreadName());
        for (final GLAnimatorControl control : animators) {
            control.start();
            control.setUpdateFPSFrames(60, System.err);
        }
        System.err.println("GearsApplet: start() - end [visible "+isVisible()+", displayable "+isDisplayable()+"] - "+currentThreadName());
    }

    @Override
    public void stop() {
        System.err.println("GearsApplet: stop() - [visible "+isVisible()+", displayable "+isDisplayable()+"] - "+currentThreadName());
        for (final GLAnimatorControl control : animators) {
            control.stop();
        }
    }
}
