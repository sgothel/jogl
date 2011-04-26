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

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import jogamp.graph.curve.opengl.shader.UniformNames;

import com.jogamp.common.os.Platform;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public class RenderStateImpl implements RenderState {
    
    private final ShaderState st;
    private final Vertex.Factory<? extends Vertex> pointFactory;    
    private final PMVMatrix pmvMatrix;
    private final GLUniformData gcu_PMVMatrix;                    
        
    /**
     * Sharpness is equivalent to the texture-coord component <i>t</i>
     * on the off-curve vertex. Higher values of sharpness will 
     * result in higher curvature.
     */
    private final GLUniformData gcu_Sharpness;
    private final GLUniformData gcu_Alpha;
    private final GLUniformData gcu_ColorStatic;
    private final GLUniformData gcu_Strength;

    public static final RenderState getRenderState(GL2ES2 gl) {
        return (RenderState) gl.getContext().getAttachedObject(RenderState.class.getName());
    }
    
    public RenderStateImpl(ShaderState st, Vertex.Factory<? extends Vertex> pointFactory, PMVMatrix pmvMatrix) {
        this.st = st;
        this.pointFactory = pointFactory;
        this.pmvMatrix = pmvMatrix;        
        this.gcu_PMVMatrix = new GLUniformData(UniformNames.gcu_PMVMatrix, 4, 4, pmvMatrix.glGetPMvMatrixf());
        st.ownUniform(gcu_PMVMatrix);
        
        gcu_Sharpness = new GLUniformData(UniformNames.gcu_P1Y, 0.5f);
        st.ownUniform(gcu_PMVMatrix);
        gcu_Alpha = new GLUniformData(UniformNames.gcu_Alpha, 1.0f);
        st.ownUniform(gcu_Alpha);
        gcu_ColorStatic = new GLUniformData(UniformNames.gcu_ColorStatic, 3, FloatBuffer.allocate(3));
        st.ownUniform(gcu_ColorStatic);
        gcu_Strength = new GLUniformData(UniformNames.gcu_Strength, 3.0f);
        st.ownUniform(gcu_Strength);
    }
    
    public RenderStateImpl(ShaderState st, Vertex.Factory<? extends Vertex> pointFactory) {
        this(st, pointFactory, new PMVMatrix());
        
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();        
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();        
    }
    
    public final ShaderState getShaderState() { return st; }
    public final Vertex.Factory<? extends Vertex> getPointFactory () { return pointFactory; }
    public final PMVMatrix pmvMatrix() { return pmvMatrix; }
    public final GLUniformData getPMVMatrix() { return gcu_PMVMatrix; }
    public final GLUniformData getSharpness() { return gcu_Sharpness; }
    public final GLUniformData getAlpha() { return gcu_Alpha; }
    public final GLUniformData getColorStatic() { return gcu_ColorStatic; }
    public final GLUniformData getStrength() { return gcu_Strength; }
    
    public void destroy(GL2ES2 gl) {
        st.destroy(gl);
    }
    
    public final RenderState attachTo(GL2ES2 gl) {
        return (RenderState) gl.getContext().attachObject(RenderState.class.getName(), this);
    }
    public final boolean detachFrom(GL2ES2 gl) {
        RenderState _rs = (RenderState) gl.getContext().getAttachedObject(RenderState.class.getName());
        if(_rs == this) {
            gl.getContext().detachObject(RenderState.class.getName());
            return true;
        }
        return false;
    }    
    
    public StringBuilder toString(StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }

        sb.append("RenderState[");
        st.toString(sb).append(Platform.getNewline());
        sb.append("]");

        return sb;
    }
    
    public String toString() {
        return toString(null).toString();
    }    
}
