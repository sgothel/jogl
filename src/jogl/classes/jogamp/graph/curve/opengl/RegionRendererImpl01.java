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
package jogamp.graph.curve.opengl;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLException;

import jogamp.graph.curve.opengl.shader.AttributeNames;
import jogamp.opengl.Debug;

import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public class RegionRendererImpl01 extends RegionRenderer {
    private static final String CUSTOM_FP, CUSTOM_VP;
    static {
        Debug.initSingleton();
        CUSTOM_VP = Debug.getProperty("jogl.debug.graph.curve.vp", false);
        CUSTOM_FP = Debug.getProperty("jogl.debug.graph.curve.fp", false);
    }

    public RegionRendererImpl01(final RenderState rs, final int renderModes, final GLCallback enableCallback, final GLCallback disableCallback) {
        super(rs, renderModes, enableCallback, disableCallback);
    }

    @Override
    protected final boolean initImpl(GL2ES2 gl) {
        final ShaderState st = getShaderState();
        final ShaderCode rsVp, rsFp;
        if( null != CUSTOM_VP ) {
            rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, RegionRendererImpl01.class, null, null, CUSTOM_VP, true);
        } else {
            rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, RegionRendererImpl01.class, "shader", "shader/bin", getVertexShaderName(), true);
        }
        if( null != CUSTOM_FP ) {
            rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, RegionRendererImpl01.class, null, null, CUSTOM_FP, true);
        } else {
            rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, RegionRendererImpl01.class, "shader", "shader/bin", getFragmentShaderName(), true);
        }
        rsVp.defaultShaderCustomization(gl, true, true);
        // rsFp.defaultShaderCustomization(gl, true, true);
        int pos = rsFp.addGLSLVersion(gl);
        if( gl.isGLES2() && ! gl.isGLES3() ) {
            pos = rsFp.insertShaderSource(0, pos, ShaderCode.createExtensionDirective(GLExtensions.OES_standard_derivatives, ShaderCode.ENABLE));
        }
        final String rsFpDefPrecision =  getFragmentShaderPrecision(gl);
        if( null != rsFpDefPrecision ) {
            rsFp.insertShaderSource(0, pos, rsFpDefPrecision);
        }

        final ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);

        if( !sp.init(gl) ) {
            throw new GLException("RegionRenderer: Couldn't init program: "+sp);
        }
        st.attachShaderProgram(gl, sp, false);
        st.bindAttribLocation(gl, AttributeNames.VERTEX_ATTR_IDX, AttributeNames.VERTEX_ATTR_NAME);
        st.bindAttribLocation(gl, AttributeNames.TEXCOORD_ATTR_IDX, AttributeNames.TEXCOORD_ATTR_NAME);

        if(!sp.link(gl, System.err)) {
            throw new GLException("RegionRenderer: Couldn't link program: "+sp);
        }
        st.useProgram(gl, true);

        if(DEBUG) {
            System.err.println("RegionRendererImpl01 initialized: " + Thread.currentThread()+" "+st);
        }
        return true;
    }

    @Override
    protected final void destroyImpl(GL2ES2 gl) {
        // NOP .. all will be destroyed via RenderState
    }
}
