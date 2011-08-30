/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.curve.opengl;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLUniformData;

import jogamp.graph.curve.opengl.RenderStateImpl;
import jogamp.graph.curve.opengl.shader.UniformNames;

import com.jogamp.common.os.Platform;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderState;

public abstract class RenderState {
    private static final String thisKey = "jogamp.graph.curve.RenderState" ;
    
    public static RenderState createRenderState(ShaderState st, Vertex.Factory<? extends Vertex> pointFactory) {
        return new RenderStateImpl(st, pointFactory);
    }

    public static RenderState createRenderState(ShaderState st, Vertex.Factory<? extends Vertex> pointFactory, PMVMatrix pmvMatrix) {
        return new RenderStateImpl(st, pointFactory, pmvMatrix);
    }
    
    public static final RenderState getRenderState(GL2ES2 gl) {
        return (RenderState) gl.getContext().getAttachedObject(thisKey);
    }
    
    protected final ShaderState st;
    protected final Vertex.Factory<? extends Vertex> vertexFactory;
    protected final PMVMatrix pmvMatrix;
    protected final GLUniformData gcu_PMVMatrix;                    
        
    protected RenderState(ShaderState st, Vertex.Factory<? extends Vertex> vertexFactory, PMVMatrix pmvMatrix) {
        this.st = st;
        this.vertexFactory = vertexFactory;
        this.pmvMatrix = pmvMatrix;        
        this.gcu_PMVMatrix = new GLUniformData(UniformNames.gcu_PMVMatrix, 4, 4, pmvMatrix.glGetPMvMatrixf());
        st.ownUniform(gcu_PMVMatrix);        
    }
        
    public final ShaderState getShaderState() { return st; }
    public final Vertex.Factory<? extends Vertex> getVertexFactory() { return vertexFactory; }
    public final PMVMatrix pmvMatrix() { return pmvMatrix; }
    public final GLUniformData getPMVMatrix() { return gcu_PMVMatrix; }
    
    public void destroy(GL2ES2 gl) {
        st.destroy(gl);
    }
    
    public abstract GLUniformData getWeight();
    public abstract GLUniformData getAlpha();
    public abstract GLUniformData getColorStatic();
    // public abstract GLUniformData getStrength();
    
    public final RenderState attachTo(GL2ES2 gl) {
        return (RenderState) gl.getContext().attachObject(thisKey, this);
    }
    
    public final boolean detachFrom(GL2ES2 gl) {
        RenderState _rs = (RenderState) gl.getContext().getAttachedObject(thisKey);
        if(_rs == this) {
            gl.getContext().detachObject(thisKey);
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
