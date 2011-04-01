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

package com.jogamp.opengl.test.junit.graph.demos;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.opengl.SVertex;

/** Demonstrate the rendering of multiple OutlineShapes
 *  into one region
 *
 */
public class GPURegionGLListener02 extends GPURegionRendererListenerBase01 {
    OutlineShape[] outlineShapes = new OutlineShape[2];

    public GPURegionGLListener02 (int numpass, int fbosize, boolean debug, boolean trace) {
        super(SVertex.factory(), numpass, debug, trace);
        setMatrix(-20, 00, 0f, -50, fbosize);
    }
    	
    private void createTestOutline(){
        float offset = 0;
        outlineShapes[0] = new OutlineShape(SVertex.factory());
        outlineShapes[0].addVertex(0.0f,-10.0f,true);
        outlineShapes[0].addVertex(15.0f,-10.0f, true);
        outlineShapes[0].addVertex(10.0f,5.0f, false);
        outlineShapes[0].addVertex(15.0f,10.0f, true);
        outlineShapes[0].addVertex(6.0f,15.0f, false);
        outlineShapes[0].addVertex(5.0f,8.0f, false);
        outlineShapes[0].addVertex(0.0f,10.0f,true);
        outlineShapes[0].closeLastOutline();
        outlineShapes[0].addEmptyOutline();
        outlineShapes[0].addVertex(5.0f,-5.0f,true);
        outlineShapes[0].addVertex(10.0f,-5.0f, false);
        outlineShapes[0].addVertex(10.0f,0.0f, true);
        outlineShapes[0].addVertex(5.0f,0.0f, false);
        outlineShapes[0].closeLastOutline();
        
        /** Same shape as above but without any off-curve vertices */
        outlineShapes[1] = new OutlineShape(SVertex.factory());
        offset = 30;
        outlineShapes[1].addVertex(offset+0.0f,-10.0f, true);
        outlineShapes[1].addVertex(offset+17.0f,-10.0f, true);
        outlineShapes[1].addVertex(offset+11.0f,5.0f, true);
        outlineShapes[1].addVertex(offset+16.0f,10.0f, true);
        outlineShapes[1].addVertex(offset+7.0f,15.0f, true);
        outlineShapes[1].addVertex(offset+6.0f,8.0f, true);
        outlineShapes[1].addVertex(offset+0.0f,10.0f, true);
        outlineShapes[1].closeLastOutline();
        outlineShapes[1].addEmptyOutline();
        outlineShapes[1].addVertex(offset+5.0f,0.0f, true);
        outlineShapes[1].addVertex(offset+5.0f,-5.0f, true);
        outlineShapes[1].addVertex(offset+10.0f,-5.0f, true);
        outlineShapes[1].addVertex(offset+10.0f,0.0f, true);
        outlineShapes[1].closeLastOutline();
    }

	public void init(GLAutoDrawable drawable) {
		super.init(drawable);
		
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        final RegionRenderer regionRenderer = (RegionRenderer) getRenderer();

		gl.setSwapInterval(1);
		gl.glEnable(GL2ES2.GL_DEPTH_TEST);
		regionRenderer.init(gl);
        regionRenderer.setAlpha(gl, 1.0f);
        regionRenderer.setColor(gl, 0.0f, 0.0f, 0.0f);
        MSAATool.dump(drawable);
        
		createTestOutline();
	}

	public void display(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final RegionRenderer regionRenderer = (RegionRenderer) getRenderer();
        
        regionRenderer.resetModelview(null);
        regionRenderer.translate(null, getXTran(), getYTran(), getZoom());
        regionRenderer.rotate(gl, getAngle(), 0, 1, 0);

        regionRenderer.renderOutlineShapes(gl, outlineShapes, getPosition(), getTexSize());            
		
	}		
}
