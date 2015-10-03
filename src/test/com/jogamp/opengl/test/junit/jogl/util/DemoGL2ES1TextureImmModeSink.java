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

package com.jogamp.opengl.test.junit.jogl.util;

import java.io.InputStream;
import java.net.URLConnection;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.test.junit.jogl.demos.TextureDraw01Accessor;
import com.jogamp.opengl.test.junit.jogl.util.texture.PNGTstFiles;
import com.jogamp.opengl.util.ImmModeSink;
import com.jogamp.opengl.util.glsl.fixedfunc.FixedFuncUtil;
import com.jogamp.opengl.util.glsl.fixedfunc.ShaderSelectionMode;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;

public class DemoGL2ES1TextureImmModeSink implements GLEventListener, TextureDraw01Accessor {
    private boolean debugFFPEmu = false;
    private boolean verboseFFPEmu = false;
    private boolean traceFFPEmu = false;
    private boolean forceFFPEmu = false;
    private final ImmModeSink ims;
    private final GLU      glu = new GLU();
    private TextureData textureData;
    private Texture  texture;
    boolean keepTextureBound;

    public DemoGL2ES1TextureImmModeSink() {
        this.ims = ImmModeSink.createFixed(32, 3, GL.GL_FLOAT, 4, GL.GL_FLOAT, 0, GL.GL_FLOAT, 2, GL.GL_FLOAT, GL.GL_STATIC_DRAW);
        this.keepTextureBound = false;
    }

    public void setForceFFPEmu(final boolean forceFFPEmu, final boolean verboseFFPEmu, final boolean debugFFPEmu, final boolean traceFFPEmu) {
        this.forceFFPEmu = forceFFPEmu;
        this.verboseFFPEmu = verboseFFPEmu;
        this.debugFFPEmu = debugFFPEmu;
        this.traceFFPEmu = traceFFPEmu;
    }


    @Override
    public void setKeepTextureBound(final boolean v) {
        this.keepTextureBound = v;
    }
    @Override
    public Texture getTexture( ) {
        return this.texture;
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        GL _gl = drawable.getGL();
        if(debugFFPEmu) {
            // Debug ..
            _gl = _gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", GL2ES2.class, _gl, null) );
        }
        if(traceFFPEmu) {
            // Trace ..
            _gl = _gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", GL2ES2.class, _gl, new Object[] { System.err } ) );
        }
        final GL2ES1 gl = FixedFuncUtil.wrapFixedFuncEmul(_gl, ShaderSelectionMode.AUTO, null, forceFFPEmu, verboseFFPEmu);

        final URLConnection testTextureUrlConn = IOUtil.getResource("test-ntscP_3-01-160x90.png", PNGTstFiles.class.getClassLoader(), PNGTstFiles.class);
        try {
            final InputStream  testTextureStream = testTextureUrlConn.getInputStream();
            textureData = TextureIO.newTextureData(gl.getGLProfile(), testTextureStream , false /* mipmap */, TextureIO.PNG);
            texture = TextureIO.newTexture(gl, textureData);
            if( keepTextureBound && null != texture ) {
                texture.enable(gl);
                texture.bind(gl);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2ES1 gl = drawable.getGL().getGL2ES1();
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, 1, 0, 1);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES1 gl = drawable.getGL().getGL2ES1();
        if(null!=texture) {
            texture.disable(gl);
            texture.destroy(gl);
        }
        if(null!=textureData) {
            textureData.destroy();
        }
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES1 gl = drawable.getGL().getGL2ES1();

        // draw one quad with the texture
        if(null!=texture) {
            if( !keepTextureBound ) {
                texture.enable(gl);
                texture.bind(gl);
            }
            // gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
            final TextureCoords coords = texture.getImageTexCoords();
            ims.glBegin(ImmModeSink.GL_QUADS);
            ims.glTexCoord2f(coords.left(), coords.bottom());
            ims.glVertex3f(0, 0, 0);
            ims.glTexCoord2f(coords.right(), coords.bottom());
            ims.glVertex3f(1, 0, 0);
            ims.glTexCoord2f(coords.right(), coords.top());
            ims.glVertex3f(1, 1, 0);
            ims.glTexCoord2f(coords.left(), coords.top());
            ims.glVertex3f(0, 1, 0);
            ims.glEnd(gl);
            if( !keepTextureBound ) {
                texture.disable(gl);
            }
        }
    }
}

