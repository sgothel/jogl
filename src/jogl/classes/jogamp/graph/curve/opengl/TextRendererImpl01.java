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
import jogamp.graph.curve.text.GlyphString;

import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

public class TextRendererImpl01 extends TextRenderer {    
    public TextRendererImpl01(RenderState rs, int type) {
        super(rs, type);
        // rs.getSharpness().setData(0.5f);
        // rs.getAlpha().setData(1.0f);
        // rs.getStrength().setData(3.0f);                        
        rs.getStrength().setData(1.9f);                        
    }
    
    @Override
    protected boolean initShaderProgram(GL2ES2 gl){
        final ShaderState st = rs.getShaderState();
        
        ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, TextRendererImpl01.class,
                "shader", "shader/bin", "curverenderer01");
        ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, TextRendererImpl01.class,
                "shader", "shader/bin", "curverenderer01");
        
        ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);
        
        sp.init(gl);
        st.attachShaderProgram(gl, sp);        
        st.bindAttribLocation(gl, AttributeNames.VERTEX_ATTR_IDX, AttributeNames.VERTEX_ATTR_NAME);
        st.bindAttribLocation(gl, AttributeNames.TEXCOORD_ATTR_IDX, AttributeNames.TEXCOORD_ATTR_NAME);        
        
        if(!sp.link(gl, System.err)) {
            throw new GLException("TextRendererImpl01: Couldn't link program: "+sp);
        }
        st.useProgram(gl, true);

        if(DEBUG) {
            System.err.println("TextRendererImpl01 initialized: " + Thread.currentThread()+" "+st);
        }        
        return true;
    }
    
    @Override
    protected void disposeImpl(GL2ES2 gl) {
        super.disposeImpl(gl);
    }
    
    @Override
    public void renderString3D(GL2ES2 gl, Font font, String str, float[] position, int fontSize, int texSize) {
        if(!isInitialized()){
            throw new GLException("TextRendererImpl01: not initialized!");
        }
        GlyphString glyphString = getCachedGlyphString(font, str, fontSize);
        if(null == glyphString) {
            glyphString = createString(gl, font, fontSize, str);
            addCachedGlyphString(gl, font, str, fontSize, glyphString);
        }
        
        glyphString.renderString3D(gl, rs, vp_width, vp_height, texSize);
    }
    
}
