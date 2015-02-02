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

package com.jogamp.opengl.test.junit.jogl.demos.es1;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.opengl.util.ImmModeSink;

public class MultisampleDemoES1 implements GLEventListener {

    boolean multisample;
    ImmModeSink immModeSink;

    public MultisampleDemoES1(final boolean multisample) {
        this.multisample = multisample;
    }

    public void init(final GLAutoDrawable drawable) {
        System.err.println();
        System.err.println("Requested: " + drawable.getNativeSurface().getGraphicsConfiguration().getRequestedCapabilities());
        System.err.println();
        System.err.println("Chosen   : " + drawable.getChosenGLCapabilities());
        System.err.println();
        final GL2ES1 gl = drawable.getGL().getGL2ES1();
        if (multisample) {
            gl.glEnable(GL.GL_MULTISAMPLE);
        }
        gl.glClearColor(0, 0, 0, 0);
        //      gl.glEnable(GL.GL_DEPTH_TEST);
        //      gl.glDepthFunc(GL.GL_LESS);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrtho(-1, 1, -1, 1, -1, 1);
        if (multisample) {
            gl.glDisable(GL.GL_MULTISAMPLE);
        }
        immModeSink = ImmModeSink.createFixed(40,
                                              3, GL.GL_FLOAT, // vertex
                                              0, GL.GL_FLOAT, // color
                                              0, GL.GL_FLOAT, // normal
                                              0, GL.GL_FLOAT, // texCoords
                                              GL.GL_STATIC_DRAW);
        final int numSteps = 20;
        final double increment = Math.PI / numSteps;
        final double radius = 1;
        immModeSink.glBegin(GL.GL_LINES);
        for (int i = numSteps - 1; i >= 0; i--) {
            immModeSink.glVertex3f((float) (radius * Math.cos(i * increment)),
                                   (float) (radius * Math.sin(i * increment)),
                                   0f);
            immModeSink.glVertex3f((float) (-1.0 * radius * Math.cos(i * increment)),
                                   (float) (-1.0 * radius * Math.sin(i * increment)),
                                   0f);
        }
        immModeSink.glEnd(gl, false);
    }

    public void dispose(final GLAutoDrawable drawable) {
        immModeSink.destroy(drawable.getGL());
        immModeSink = null;
    }

    public void display(final GLAutoDrawable drawable) {
        final GL2ES1 gl = drawable.getGL().getGL2ES1();
        if (multisample) {
            gl.glEnable(GL.GL_MULTISAMPLE);
        }
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        immModeSink.draw(gl, true);
        if (multisample) {
            gl.glDisable(GL.GL_MULTISAMPLE);
        }
    }

    // Unused routines
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
    }

    public void displayChanged(final GLAutoDrawable drawable, final boolean modeChanged, final boolean deviceChanged) {
    }
}
