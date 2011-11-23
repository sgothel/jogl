/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.opengl.test.junit.jogl.caps;

import jogamp.opengl.x11.glx.GLX;
import jogamp.opengl.x11.glx.X11GLXGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPipelineFactory;

class MultisampleDemo01 implements GLEventListener {

    static boolean glDebug = false;
    static boolean glTrace = false;

    boolean multisample;

    public MultisampleDemo01(boolean multisample) {
        this.multisample = multisample;
    }

    public void init(GLAutoDrawable drawable) {
        AbstractGraphicsConfiguration config = drawable.getNativeSurface().getGraphicsConfiguration();
        System.err.println();
        System.err.println("Info: " + config);
        System.err.println();
        if (NativeWindowFactory.TYPE_X11.equals(NativeWindowFactory.getNativeWindowType(false))) {
            X11GLXGraphicsConfiguration x11config = (X11GLXGraphicsConfiguration) config;
            long display = drawable.getNativeSurface().getDisplayHandle();
            int[] foo = new int[1];
            GLX.glXGetFBConfigAttrib(display, x11config.getFBConfig(), GLX.GLX_SAMPLES, foo, 0);
            System.out.println("GLX_SAMPLES " + foo[0]);
            GLX.glXGetFBConfigAttrib(display, x11config.getFBConfig(), GLX.GLX_SAMPLE_BUFFERS, foo, 0);
            System.out.println("GLX_SAMPLE_BUFFERS " + foo[0]);
        }
        GL _gl = drawable.getGL();
        if (glDebug) {
            try {
                // Debug ..
                _gl = _gl.getContext().setGL(GLPipelineFactory.create("javax.media.opengl.Debug", GL2.class, _gl, null));
                if (glTrace) {
                    // Trace ..
                    _gl = _gl.getContext().setGL(GLPipelineFactory.create("javax.media.opengl.Trace", GL2.class, _gl, new Object[]{System.err}));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        GL2 gl = _gl.getGL2();
        if (multisample) {
            gl.glEnable(GL.GL_MULTISAMPLE);
        }
        gl.glClearColor(0, 0, 0, 0);
        //      gl.glEnable(GL.GL_DEPTH_TEST);
        //      gl.glDepthFunc(GL.GL_LESS);
        gl.glMatrixMode(GL2ES1.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2ES1.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(-1, 1, -1, 1, -1, 1);
        if (multisample) {
            gl.glDisable(GL.GL_MULTISAMPLE);
        }
    }

    public void dispose(GLAutoDrawable drawable) {
    }

    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        if (multisample) {
            gl.glEnable(GL.GL_MULTISAMPLE);
        }
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        int numSteps = 20;
        double increment = Math.PI / numSteps;
        double radius = 1;
        gl.glBegin(GL.GL_LINES);
        for (int i = numSteps - 1; i >= 0; i--) {
            gl.glVertex3d(radius * Math.cos(i * increment), radius * Math.sin(i * increment), 0);
            gl.glVertex3d(-1.0 * radius * Math.cos(i * increment), -1.0 * radius * Math.sin(i * increment), 0);
        }
        gl.glEnd();
        if (multisample) {
            gl.glDisable(GL.GL_MULTISAMPLE);
        }
    }

    // Unused routines
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }
}
