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

package com.jogamp.opengl.test.junit.graph.demos.ui;

import java.io.IOException;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.opengl.test.junit.graph.demos.MSAATool;
import com.jogamp.opengl.util.PMVMatrix;

public class UIGLListener01 extends UIListenerBase01 {

    public UIGLListener01 (final int renderModes, final RenderState rs, final boolean debug, final boolean trace) {
        super(renderModes, RegionRenderer.create(rs, RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable), debug, trace);
        setMatrix(-20, 00, 0f, -50);
        try {
            final Font font = FontFactory.get(FontFactory.UBUNTU).getDefault();
            button = new LabelButton(SVertex.factory(), 0, font, "Click me!", 4f, 3f);
            button.translate(2,1,0);
            /** Button defaults !
                button.setLabelColor(1.0f,1.0f,1.0f);
                button.setButtonColor(0.6f,0.6f,0.6f);
                button.setCorner(1.0f);
                button.setSpacing(2.0f);
             */
            System.err.println(button);
        } catch (final IOException ex) {
            System.err.println("Caught: "+ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        super.init(drawable);

        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);

        MSAATool.dump(drawable);
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final int[] sampleCount = { 4 };
        final float[] translate = button.getTranslate();

        final RegionRenderer regionRenderer = getRegionRenderer();
        final PMVMatrix pmv = regionRenderer.getMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
        pmv.glTranslatef(getXTran(), getYTran(), getZoom());
        pmv.glRotatef(getAngle(), 0, 1, 0);
        pmv.glTranslatef(translate[0], translate[1], 0);
        button.drawShape(gl, regionRenderer, sampleCount);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        button.destroy(gl, getRegionRenderer());
        super.dispose(drawable);
    }
}
