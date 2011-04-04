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
import javax.media.opengl.GLException;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;


public class RegionRendererImpl01 extends RegionRenderer {
	/**Sharpness is equivalent to the value of t value of texture coord
	 * on the off-curve vertex. The high value of sharpness will 
	 * result in high curvature.
	 */
    private GLUniformData mgl_sharpness = new GLUniformData("p1y", 0.5f);
    GLUniformData mgl_alpha = new GLUniformData("g_alpha", 1.0f);
    private GLUniformData mgl_color = new GLUniformData("g_color", 3, FloatBuffer.allocate(3));
    private GLUniformData mgl_strength = new GLUniformData("a_strength", 3.0f);
    
    public RegionRendererImpl01(Vertex.Factory<? extends Vertex> factory, int type) {
        super(factory, type);
    }
    
    protected boolean initImpl(GL2ES2 gl) {
        boolean VBOsupported = gl.isFunctionAvailable("glGenBuffers") &&
            gl.isFunctionAvailable("glBindBuffer") &&
            gl.isFunctionAvailable("glBufferData") &&
            gl.isFunctionAvailable("glDrawElements") &&
            gl.isFunctionAvailable("glVertexAttribPointer") &&
            gl.isFunctionAvailable("glDeleteBuffers");
        
        if(DEBUG) {
            System.err.println("RegionRenderer: VBO Supported = " + VBOsupported);
        }
        
        if(!VBOsupported){
            return false;
        }
        
        gl.glEnable(GL2ES2.GL_BLEND);
        gl.glBlendFunc(GL2ES2.GL_SRC_ALPHA, GL2ES2.GL_ONE_MINUS_SRC_ALPHA);
        
        ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, RegionRendererImpl01.class,
                "shader", "shader/bin", "curverenderer01");
        ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RegionRendererImpl01.class,
                "shader", "shader/bin", "curverenderer01");
    
        ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);

        sp.init(gl);
        gl.glBindAttribLocation(sp.program(), Region.VERTEX_ATTR_IDX, "v_position");
        gl.glBindAttribLocation(sp.program(), Region.TEXCOORD_ATTR_IDX, "texCoord");
        
        if(!sp.link(gl, System.err)) {
            throw new GLException("RegionRenderer: Couldn't link program: "+sp);
        }
    
        st = new ShaderState();
        st.attachShaderProgram(gl, sp);
        
        st.glUseProgram(gl, true);
    
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        resetModelview(null);
    
        mgl_PMVMatrix = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());                  
        if(!st.glUniform(gl, mgl_PMVMatrix)) {
            if(DEBUG){
                System.err.println("Error setting PMVMatrix in shader: "+st);
            }
            return false;
        }
        
        if(!st.glUniform(gl, mgl_sharpness)) {
            if(DEBUG){
                System.err.println("Error setting sharpness in shader: "+st);
            }
            return false;
        }
                
        if(!st.glUniform(gl, mgl_alpha)) {
            if(DEBUG){
                System.err.println("Error setting global alpha in shader: "+st);
            }
            return false;
        }       
        
        if(!st.glUniform(gl, mgl_color)) {
            if(DEBUG){
                System.err.println("Error setting global color in shader: "+st);
            }
            return false;
        }       
        
        if(!st.glUniform(gl, mgl_strength)) {
            System.err.println("Error setting antialias strength in shader: "+st);
        }
        
        if(DEBUG) {
            System.err.println("RegionRendererImpl01 initialized: " + Thread.currentThread()+" "+st);
        }
        return true;
    }

    @Override
    protected void disposeImpl(GL2ES2 gl) {
        super.disposeImpl(gl);
    }
        
    @Override
    public float getAlpha() {
        return mgl_alpha.floatValue();
    }

    @Override
    public void setAlpha(GL2ES2 gl, float alpha_t) {
        mgl_alpha.setData(alpha_t);
        if(null != gl && st.inUse()) {
            st.glUniform(gl, mgl_alpha);
        }
    }
    
    @Override
    public void setColor(GL2ES2 gl, float r, float g, float b){
        FloatBuffer fb = (FloatBuffer) mgl_color.getBuffer();
        fb.put(0, r);
        fb.put(1, r);
        fb.put(2, r);
        if(null != gl && st.inUse()) {
            st.glUniform(gl, mgl_color);
        }
    }
    
	
	@Override
    public void renderOutlineShape(GL2ES2 gl, OutlineShape outlineShape, float[] position, int texSize) {
		if(!isInitialized()){
			throw new GLException("RegionRendererImpl01: not initialized!");
		}
		int hashCode = getHashCode(outlineShape);
		Region region = regions.get(hashCode);
		
		if(null == region) {
			region = createRegion(gl, outlineShape, mgl_sharpness.floatValue());
			regions.put(hashCode, region);
		}		
		region.render(pmvMatrix, vp_width, vp_height, texSize);
	}
	
	@Override
    public void renderOutlineShapes(GL2ES2 gl, OutlineShape[] outlineShapes, float[] position, int texSize) {
        if(!isInitialized()){
            throw new GLException("RegionRendererImpl01: not initialized!");
        }
		
		int hashCode = getHashCode(outlineShapes);
		Region region = regions.get(hashCode);
		
		if(null == region) {
            region = createRegion(gl, outlineShapes, mgl_sharpness.floatValue());
			regions.put(hashCode, region);
		}		
        region.render(pmvMatrix, vp_width, vp_height, texSize);
	}	
}
