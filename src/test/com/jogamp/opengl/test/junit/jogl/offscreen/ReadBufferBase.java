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
 
package com.jogamp.opengl.test.junit.jogl.offscreen;

import javax.media.opengl.*;

public class ReadBufferBase implements GLEventListener {
    public boolean glDebug = false ;
    public boolean glTrace = false ;

    protected GLDrawable externalRead;

    ReadBufferUtil readBufferUtil = new ReadBufferUtil();

    public ReadBufferBase (GLDrawable externalRead) {
        this.externalRead = externalRead ;
    }

    public void init(GLAutoDrawable drawable) {
        GL _gl = drawable.getGL();

        _gl.glGetError(); // flush error ..

        if(glDebug) {
            try {
                _gl = _gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Debug", null, _gl, null) );
            } catch (Exception e) {
                throw new RuntimeException("can not set debug pipeline", e);
            }
        }

        if(glTrace) {
            try {
                _gl = _gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Trace", null, _gl, new Object[] { System.err } ) );
            } catch (Exception e) {
                throw new RuntimeException("can not set trace pipeline", e);
            }
        }

        System.out.println(_gl);

        _gl.getContext().setGLReadDrawable(externalRead);
        if(_gl.isGL2GL3()) {
            _gl.getGL2GL3().glReadBuffer(GL2GL3.GL_FRONT);
        }
        System.out.println("---------------------------");
        System.out.println(_gl.getContext());
        System.out.println("---------------------------");
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    public void dispose(GLAutoDrawable drawable) {
        readBufferUtil.dispose(drawable.getGL());
    }

    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        readBufferUtil.fetchOffscreenTexture(drawable, gl);
    }

}

