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

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;

import jogamp.graph.curve.text.GlyphString;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRenderer;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.opengl.test.junit.graph.demos.MSAATool;

public class UIGLListener01 extends UIListenerBase01 {
    
    public UIGLListener01 (RenderState rs, boolean debug, boolean trace) {
        super(RegionRenderer.create(rs, Region.SINGLE_PASS),
              TextRenderer.create(rs, Region.SINGLE_PASS), debug, trace);
        setMatrix(-20, 00, 0f, -50);
        button = new RIButton(SVertex.factory(), "Click me!");
        /** Button defaults !
            button.setLabelColor(1.0f,1.0f,1.0f);
            button.setButtonColor(0.6f,0.6f,0.6f);
            button.setCorner(1.0f);
            button.setSpacing(2.0f);
         */
        System.err.println(button);
    }
    
    private GlyphString glyphString;
    public void init(GLAutoDrawable drawable) {
        super.init(drawable);
        
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        final RegionRenderer regionRenderer = getRegionRenderer();
        final TextRenderer textRenderer = getTextRenderer();
        
        gl.setSwapInterval(1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        gl.glEnable(GL2ES2.GL_POLYGON_OFFSET_FILL);
        
        regionRenderer.init(gl);
        // default is 1.0f .. regionRenderer.setAlpha(gl, 1.0f);
        // default is 0.5f .. regionRenderer.setSharpness(gl, 0.5f); 
        // FIXME: What if Sharpness diff for text/outline-only ?!
        
        glyphString = textRenderer.createString(gl, button.getFont(), 10, button.getLabel());
        glyphString.generateRegion(gl, regionRenderer.getRenderState(), regionRenderer.getRenderType());
        
        button.generate(glyphString.getBounds());
        MSAATool.dump(drawable);
    }

    public void display(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final RegionRenderer regionRenderer = getRegionRenderer();
        
        regionRenderer.resetModelview(null);
        
        regionRenderer.translate(null, getXTran(), getYTran(), getZoom());
        regionRenderer.rotate(gl, getAngle(), 0, 1, 0);
        float[] bColor = button.getButtonColor(); 
        regionRenderer.setColorStatic(gl, bColor[0], bColor[1], bColor[2]);
        regionRenderer.renderOutlineShape(gl, button.getShape(glyphString.getBounds()), getPosition(), 0);
        float[] lColor = button.getLabelColor(); 
        regionRenderer.setColorStatic(gl, lColor[0], lColor[1], lColor[2]);
        glyphString.renderString3D(gl);
    }        
    
    public void dispose(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        glyphString.destroy(gl, getRegionRenderer().getRenderState());
        super.dispose(drawable);
    }
}
