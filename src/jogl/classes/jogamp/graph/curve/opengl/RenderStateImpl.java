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

import java.nio.FloatBuffer;

import javax.media.opengl.GLUniformData;

import jogamp.graph.curve.opengl.shader.UniformNames;

import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public class RenderStateImpl extends RenderState {    
    /**
     * weight is equivalent to the 
     * global off-curve vertex weight.
     * TODO: change to per vertex
     */
    private final GLUniformData gcu_Weight;
    private final GLUniformData gcu_Alpha;
    private final GLUniformData gcu_ColorStatic;

    public RenderStateImpl(ShaderState st, Vertex.Factory<? extends Vertex> pointFactory, PMVMatrix pmvMatrix) {
        super(st, pointFactory, pmvMatrix);
        
        gcu_Weight = new GLUniformData(UniformNames.gcu_Weight, 1.0f);
        st.ownUniform(gcu_PMVMatrix);
        gcu_Alpha = new GLUniformData(UniformNames.gcu_Alpha, 1.0f);
        st.ownUniform(gcu_Alpha);
        gcu_ColorStatic = new GLUniformData(UniformNames.gcu_ColorStatic, 3, FloatBuffer.allocate(3));
        st.ownUniform(gcu_ColorStatic);
//        gcu_Strength = new GLUniformData(UniformNames.gcu_Strength, 3.0f);
//        st.ownUniform(gcu_Strength);
    }
    
    public RenderStateImpl(ShaderState st, Vertex.Factory<? extends Vertex> pointFactory) {
        this(st, pointFactory, new PMVMatrix());
    }
    
    public final GLUniformData getWeight() { return gcu_Weight; }
    public final GLUniformData getAlpha() { return gcu_Alpha; }
    public final GLUniformData getColorStatic() { return gcu_ColorStatic; }
    //public final GLUniformData getStrength() { return gcu_Strength; }
    
    
}
