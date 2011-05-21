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

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public class RegionRendererImpl01 extends RegionRenderer {
    public RegionRendererImpl01(RenderState rs, int renderModes) {
        super(rs, renderModes);
        
    }
    
    @Override
    protected String getFragmentShaderName(GL2ES2 gl) {
        if(Region.usesVariableCurveWeight(renderModes)){
            return "curverenderer02" + getShaderGLVersionSuffix(gl);
        }
        return "curverenderer01" + getShaderGLVersionSuffix(gl);
    }
    
    protected boolean initShaderProgram(GL2ES2 gl) {
        final ShaderState st = rs.getShaderState();
        
        ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, RegionRendererImpl01.class,
                "shader", "shader/bin", getVertexShaderName(gl));
        ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RegionRendererImpl01.class,
                "shader", "shader/bin", getFragmentShaderName(gl));
    
        ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);

        sp.init(gl);
        st.attachShaderProgram(gl, sp);        
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
    protected void destroyImpl(GL2ES2 gl) {
        super.destroyImpl(gl);
    }

    @Override
    protected void drawImpl(GL2ES2 gl, Region region, float[] position, int texSize) {
        ((GLRegion)region).draw(gl, rs, vp_width, vp_height, texSize);
    }    
}
