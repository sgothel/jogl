/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
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

package jogamp.opengl.util.glsl.fixedfunc;

import com.jogamp.common.nio.Buffers;
import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.glsl.*;
import java.nio.*;

public class FixedFuncPipeline {
    public static final int MAX_TEXTURE_UNITS = 8;
    public static final int MAX_LIGHTS        = 8;

    public FixedFuncPipeline(GL2ES2 gl, PMVMatrix pmvMatrix) {
        init(gl, pmvMatrix, FixedFuncPipeline.class, shaderSrcRootDef, shaderBinRootDef, 
             vertexColorFileDef, vertexColorLightFileDef, fragmentColorFileDef, fragmentColorTextureFileDef);
    }
    public FixedFuncPipeline(GL2ES2 gl, PMVMatrix pmvMatrix, Class shaderRootClass, String shaderSrcRoot, String shaderBinRoot, 
                       String vertexColorFile,
                       String vertexColorLightFile,
                       String fragmentColorFile,
                       String fragmentColorTextureFile) {
        init(gl, pmvMatrix, shaderRootClass, shaderSrcRoot, shaderBinRoot, 
             vertexColorFile, vertexColorLightFile, fragmentColorFile, fragmentColorTextureFile);
    }

    public boolean verbose() { return verbose; }

    public void setVerbose(boolean v) { verbose=v; }

    public boolean isValid() {
        return shaderState.linked();
    }

    public ShaderState getShaderState() {
        return shaderState;
    }

    public int getActiveTextureUnit() {
        return activeTextureUnit;
    }

    public String getArrayIndexName(int glArrayIndex) {
      String name = GLPointerFuncUtil.getPredefinedArrayIndexName(glArrayIndex); 
      switch(glArrayIndex) {
          case GLPointerFunc.GL_VERTEX_ARRAY:
          case GLPointerFunc.GL_NORMAL_ARRAY:
          case GLPointerFunc.GL_COLOR_ARRAY:
              break;
          case GLPointerFunc.GL_TEXTURE_COORD_ARRAY:
              name = name + activeTextureUnit;
      }
      return name;
    }

    public void destroy(GL2ES2 gl) {
        shaderProgramColor.release(gl, true);
        shaderProgramColorLight.release(gl, true);
        shaderProgramColorTexture.release(gl, true);
        shaderProgramColorTextureLight.release(gl, true);
        shaderState.destroy(gl);
    }

    public void glEnableClientState(GL2ES2 gl, int glArrayIndex) {
        shaderState.useProgram(gl, true);

        shaderState.enableVertexAttribArray(gl, getArrayIndexName(glArrayIndex));
        // textureCoordsEnabled |=  (1 << activeTextureUnit);
        if ( textureCoordsEnabled.get(activeTextureUnit) != 1 ) {
            textureCoordsEnabled.put(activeTextureUnit, 1);
            textureCoordsEnabledDirty = true;
        }
    }

    public void glDisableClientState(GL2ES2 gl, int glArrayIndex) {
        shaderState.useProgram(gl, true);

        shaderState.disableVertexAttribArray(gl, getArrayIndexName(glArrayIndex));
        // textureCoordsEnabled &= ~(1 << activeTextureUnit);
        if ( textureCoordsEnabled.get(activeTextureUnit) != 0 ) {
            textureCoordsEnabled.put(activeTextureUnit, 0);
            textureCoordsEnabledDirty = true;
        }
    }

    public void glVertexPointer(GL2ES2 gl, GLArrayData data) {
        shaderState.useProgram(gl, true);
        shaderState.vertexAttribPointer(gl, data);
    }

    public void glColorPointer(GL2ES2 gl, GLArrayData data) {
        shaderState.useProgram(gl, true);
        shaderState.vertexAttribPointer(gl, data);
    }

    public void glColor4fv(GL2ES2 gl, FloatBuffer data ) {
        shaderState.useProgram(gl, true);
        GLUniformData ud = shaderState.getUniform(mgl_ColorStatic);
        if(null!=ud) {
            ud.setData(data);
            shaderState.uniform(gl, ud);
        }
    }

    public void glNormalPointer(GL2ES2 gl, GLArrayData data) {
        shaderState.useProgram(gl, true);
        shaderState.vertexAttribPointer(gl, data);
    }

    public void glTexCoordPointer(GL2ES2 gl, GLArrayData data) {
        shaderState.useProgram(gl, true);
        data.setName( getArrayIndexName(data.getIndex()) );
        shaderState.vertexAttribPointer(gl, data);
    }

    public void glLightfv(GL2ES2 gl, int light, int pname, java.nio.FloatBuffer params) {
        shaderState.useProgram(gl, true);
        light -=GLLightingFunc.GL_LIGHT0;
        if(0 <= light && light < MAX_LIGHTS) {
            GLUniformData ud = null;
            switch(pname) {
                case  GLLightingFunc.GL_AMBIENT:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].ambient");
                    break;
                case  GLLightingFunc.GL_DIFFUSE:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].diffuse");
                    break;
                case  GLLightingFunc.GL_SPECULAR:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].specular");
                    break;
                case GLLightingFunc.GL_POSITION:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].position");
                    break;
                case GLLightingFunc.GL_SPOT_DIRECTION:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].spotDirection");
                    break;
                case GLLightingFunc.GL_SPOT_EXPONENT:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].spotExponent");
                    break;
                case GLLightingFunc.GL_SPOT_CUTOFF:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].spotCutoff");
                    break;
                case GLLightingFunc.GL_CONSTANT_ATTENUATION:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].constantAttenuation");
                    break;
                case GLLightingFunc.GL_LINEAR_ATTENUATION:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].linearAttenuation");
                    break;
                case GLLightingFunc.GL_QUADRATIC_ATTENUATION:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].quadraticAttenuation");
                    break;
                default:
                    if(verbose) {
                        System.err.println("glLightfv pname not within [GL_AMBIENT GL_DIFFUSE GL_SPECULAR GL_POSITION GL_SPOT_DIRECTION]: "+pname);
                    }
                    return;
            }
            if(null!=ud) {
                ud.setData(params);
                shaderState.uniform(gl, ud);
            }
        } else if(verbose) {
            System.err.println("glLightfv light not within [0.."+MAX_LIGHTS+"]: "+light);
        }
    }

    public void glMaterialfv(GL2ES2 gl, int face, int pname, java.nio.FloatBuffer params) {
        shaderState.useProgram(gl, true);

        switch (face) {
            case GL.GL_FRONT:
            case GL.GL_FRONT_AND_BACK:
                break;
            case GL.GL_BACK:
                if(verbose) {
                    System.err.println("glMaterialfv face GL_BACK currently not supported");
                }
                break;
            default:
        }

        GLUniformData ud = null;
        switch(pname) {
            case  GLLightingFunc.GL_AMBIENT:
                ud = shaderState.getUniform(mgl_FrontMaterial+".ambient");
                break;
            case  GLLightingFunc.GL_AMBIENT_AND_DIFFUSE:
                glMaterialfv(gl, face, GLLightingFunc.GL_AMBIENT, params);
                // fall through intended ..
            case  GLLightingFunc.GL_DIFFUSE:
                ud = shaderState.getUniform(mgl_FrontMaterial+".diffuse");
                break;
            case  GLLightingFunc.GL_SPECULAR:
                ud = shaderState.getUniform(mgl_FrontMaterial+".specular");
                break;
            case  GLLightingFunc.GL_EMISSION:
                ud = shaderState.getUniform(mgl_FrontMaterial+".emission");
                break;
            case  GLLightingFunc.GL_SHININESS:
                ud = shaderState.getUniform(mgl_FrontMaterial+".shininess");
                break;
            default:
                if(verbose) {
                    System.err.println("glMaterialfv pname not within [GL_AMBIENT GL_DIFFUSE GL_SPECULAR GL_EMISSION GL_SHININESS]: "+pname);
                }
                return;
        }
        if(null!=ud) {
            ud.setData(params);
            shaderState.uniform(gl, ud);
        }
    }

    public void glShadeModel(GL2ES2 gl, int mode) {
        shaderState.useProgram(gl, true);
        GLUniformData ud = shaderState.getUniform(mgl_ShadeModel);
        if(null!=ud) {
            ud.setData(mode);
            shaderState.uniform(gl, ud);
        }
    }

    public void glActiveTexture(GL2ES2 gl, int textureUnit) {
        textureUnit -= GL.GL_TEXTURE0;
        if(0 <= textureUnit && textureUnit<MAX_TEXTURE_UNITS) {
            shaderState.useProgram(gl, true);
            GLUniformData ud;
            ud = shaderState.getUniform(mgl_ActiveTexture);
            if(null!=ud) {
                ud.setData(textureUnit);
                shaderState.uniform(gl, ud);
            }
            ud = shaderState.getUniform(mgl_ActiveTextureIdx);
            if(null!=ud) {
                ud.setData(textureUnit);
                shaderState.uniform(gl, ud);
            }
            activeTextureUnit = textureUnit;
        } else {
            throw new GLException("glActivateTexture textureUnit not within GL_TEXTURE0 + [0.."+MAX_TEXTURE_UNITS+"]: "+textureUnit);
        }
    }

    /**
     * @return false if digested in regard to GL2ES2 spec, 
     *         eg this call must not be passed to an underlying ES2 implementation.
     *         true if this call shall be passed to an underlying GL2ES2/ES2 implementation as well.
     */
    public boolean glEnable(GL2ES2 gl, int cap, boolean enable) {
        switch(cap) {
            case GL.GL_TEXTURE_2D:
                textureEnabled=enable;
                return true;
            case GLLightingFunc.GL_LIGHTING:
                lightingEnabled=enable;
                return false;
            case GL.GL_CULL_FACE:
                cullFace=Math.abs(cullFace);
                if(!enable) {
                    cullFace*=-1;
                }
                return true;
        }

        int light = cap - GLLightingFunc.GL_LIGHT0;
        if(0 <= light && light < MAX_LIGHTS) {
            if ( (lightsEnabled.get(light)==1) != enable ) {
                lightsEnabled.put(light, enable?1:0);
                lightsEnabledDirty = true;
                return false;
            }
        }
        return true; // pass it on ..
    }

    public void glCullFace(GL2ES2 gl, int faceName) {
        switch(faceName) {
            case GL.GL_FRONT:
                faceName = 1; break;
            case GL.GL_BACK:
                faceName = 2; break;
            case GL.GL_FRONT_AND_BACK:
                faceName = 3; break;
        }
        if(0>cullFace) {
            faceName *= -1;
        }
        cullFace = faceName;
    }

    public void validate(GL2ES2 gl) {
        shaderState.useProgram(gl, true);
        GLUniformData ud;
        if(pmvMatrix.update()) {
            ud = shaderState.getUniform(mgl_PMVMatrix);
            if(null!=ud) {
                // same data object ..
                shaderState.uniform(gl, ud);
            } else {
                throw new GLException("Failed to update: mgl_PMVMatrix");
            }
        }
        ud = shaderState.getUniform(mgl_ColorEnabled);
        if(null!=ud) {
            int ca = (shaderState.isVertexAttribArrayEnabled(GLPointerFuncUtil.mgl_Color)==true)?1:0;
            if(ca!=ud.intValue()) {
                ud.setData(ca);
                shaderState.uniform(gl, ud);
            }
        }
        ud = shaderState.getUniform(mgl_CullFace);
        if(null!=ud) {
            if(cullFace!=ud.intValue()) {
                ud.setData(cullFace);
                shaderState.uniform(gl, ud);
            }
        }

        if(lightsEnabledDirty) {
            ud = shaderState.getUniform(mgl_LightsEnabled);
            if(null!=ud) {
                // same data object 
                shaderState.uniform(gl, ud);
            }
            lightsEnabledDirty=false;
        }

        if(textureCoordsEnabledDirty) {
            ud = shaderState.getUniform(mgl_TexCoordEnabled);
            if(null!=ud) {
                // same data object 
                shaderState.uniform(gl, ud);
            }
            textureCoordsEnabledDirty=false;
        }

        if(textureEnabled) {
            if(lightingEnabled) {
                shaderState.attachShaderProgram(gl, shaderProgramColorTextureLight);
            } else {
                shaderState.attachShaderProgram(gl, shaderProgramColorTexture);
            }
        } else {
            if(lightingEnabled) {
                shaderState.attachShaderProgram(gl, shaderProgramColorLight);
            } else {
                shaderState.attachShaderProgram(gl, shaderProgramColor);
            }
        }
        if(DEBUG) {
            System.err.println("validate: "+this);
        }
    }

    public String toString() {
        return "FixedFuncPipeline[pmv: "+pmvMatrix+
               ", textureEnabled: "+textureEnabled+
               ", textureCoordsEnabled: "+textureCoordsEnabled+
               ", lightingEnabled: "+lightingEnabled+
               ", lightsEnabled: "+lightsEnabled+
               "\n\t, shaderProgramColor: "+shaderProgramColor+
               "\n\t, shaderProgramColorTexture: "+shaderProgramColorTexture+
               "\n\t, shaderProgramColorLight: "+shaderProgramColorLight+
               "\n\t, shaderProgramColorTextureLight: "+shaderProgramColorTextureLight+
               "\n\t, ShaderState: "+shaderState+
               "]";
    }

    protected void init(GL2ES2 gl, PMVMatrix pmvMatrix, Class shaderRootClass, String shaderSrcRoot, String shaderBinRoot, 
                       String vertexColorFile,
                       String vertexColorLightFile,
                       String fragmentColorFile,
                       String fragmentColorTextureFile) 
   {
        if(null==pmvMatrix) {
            throw new GLException("PMVMatrix is null");
        }
        this.pmvMatrix=pmvMatrix;
        this.shaderState=new ShaderState();
        this.shaderState.setVerbose(verbose);
        ShaderCode vertexColor, vertexColorLight, fragmentColor, fragmentColorTexture;

        vertexColor = ShaderCode.create( gl, gl.GL_VERTEX_SHADER, 1, shaderRootClass,
                                         shaderSrcRoot, shaderBinRoot, vertexColorFile);

        vertexColorLight = ShaderCode.create( gl, gl.GL_VERTEX_SHADER, 1, shaderRootClass,
                                           shaderSrcRoot, shaderBinRoot, vertexColorLightFile);

        fragmentColor = ShaderCode.create( gl, gl.GL_FRAGMENT_SHADER, 1, shaderRootClass,
                                           shaderSrcRoot, shaderBinRoot, fragmentColorFile);

        fragmentColorTexture = ShaderCode.create( gl, gl.GL_FRAGMENT_SHADER, 1, shaderRootClass,
                                                  shaderSrcRoot, shaderBinRoot, fragmentColorTextureFile);

        shaderProgramColor = new ShaderProgram();
        shaderProgramColor.add(vertexColor);
        shaderProgramColor.add(fragmentColor);
        if(!shaderProgramColor.link(gl, System.err)) {
            throw new GLException("Couldn't link VertexColor program: "+shaderProgramColor);
        }

        shaderProgramColorTexture = new ShaderProgram();
        shaderProgramColorTexture.add(vertexColor);
        shaderProgramColorTexture.add(fragmentColorTexture);
        if(!shaderProgramColorTexture.link(gl, System.err)) {
            throw new GLException("Couldn't link VertexColorTexture program: "+shaderProgramColorTexture);
        }

        shaderProgramColorLight = new ShaderProgram();
        shaderProgramColorLight.add(vertexColorLight);
        shaderProgramColorLight.add(fragmentColor);
        if(!shaderProgramColorLight.link(gl, System.err)) {
            throw new GLException("Couldn't link VertexColorLight program: "+shaderProgramColorLight);
        }

        shaderProgramColorTextureLight = new ShaderProgram();
        shaderProgramColorTextureLight.add(vertexColorLight);
        shaderProgramColorTextureLight.add(fragmentColorTexture);
        if(!shaderProgramColorTextureLight.link(gl, System.err)) {
            throw new GLException("Couldn't link VertexColorLight program: "+shaderProgramColorTextureLight);
        }

        shaderState.attachShaderProgram(gl, shaderProgramColor);
        shaderState.useProgram(gl, true);

        // mandatory ..
        if(!shaderState.uniform(gl, new GLUniformData(mgl_PMVMatrix, 4, 4, pmvMatrix.glGetPMvMvitMatrixf()))) {
            throw new GLException("Error setting PMVMatrix in shader: "+this);
        }

        shaderState.uniform(gl, new GLUniformData(mgl_ColorEnabled,  0));
        shaderState.uniform(gl, new GLUniformData(mgl_ColorStatic, 4, zero4f));
        shaderState.uniform(gl, new GLUniformData(mgl_TexCoordEnabled,  1, textureCoordsEnabled));
        shaderState.uniform(gl, new GLUniformData(mgl_ActiveTexture, activeTextureUnit));
        shaderState.uniform(gl, new GLUniformData(mgl_ActiveTextureIdx, activeTextureUnit));
        shaderState.uniform(gl, new GLUniformData(mgl_ShadeModel, 0));
        shaderState.uniform(gl, new GLUniformData(mgl_CullFace, cullFace));
        for(int i=0; i<MAX_LIGHTS; i++) {
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].ambient", 4, defAmbient));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].diffuse", 4, defDiffuse));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].specular", 4, defSpecular));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].position", 4, defPosition));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].spotDirection", 3, defSpotDir));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].spotExponent", defSpotExponent));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].spotCutoff", defSpotCutoff));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].constantAttenuation", defConstantAtten));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].linearAttenuation", defLinearAtten));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].quadraticAttenuation", defQuadraticAtten));
        }
        shaderState.uniform(gl, new GLUniformData(mgl_LightsEnabled,  1, lightsEnabled));
        shaderState.uniform(gl, new GLUniformData(mgl_FrontMaterial+".ambient", 4, defMatAmbient));
        shaderState.uniform(gl, new GLUniformData(mgl_FrontMaterial+".diffuse", 4, defMatDiffuse));
        shaderState.uniform(gl, new GLUniformData(mgl_FrontMaterial+".specular", 4, defMatSpecular));
        shaderState.uniform(gl, new GLUniformData(mgl_FrontMaterial+".emission", 4, defMatEmission));
        shaderState.uniform(gl, new GLUniformData(mgl_FrontMaterial+".shininess", defMatShininess));

        shaderState.useProgram(gl, false);
    }

    protected static final boolean DEBUG=false;
    protected boolean verbose=false;

    protected boolean textureEnabled=false;
    protected IntBuffer textureCoordsEnabled = Buffers.newDirectIntBuffer(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });
    protected boolean textureCoordsEnabledDirty = false;
    protected int     activeTextureUnit=0;

    protected int cullFace=-2; // <=0 disabled, 1: front, 2: back (default, but disabled), 3: front & back

    protected boolean lightingEnabled=false;
    protected IntBuffer lightsEnabled = Buffers.newDirectIntBuffer(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });
    protected boolean   lightsEnabledDirty = false;

    protected PMVMatrix pmvMatrix;
    protected ShaderState shaderState;
    protected ShaderProgram shaderProgramColor;
    protected ShaderProgram shaderProgramColorTexture;
    protected ShaderProgram shaderProgramColorLight;
    protected ShaderProgram shaderProgramColorTextureLight;

    // uniforms ..
    protected static final String mgl_PMVMatrix        = "mgl_PMVMatrix";       // m4fv[4] - P, Mv, Mvi and Mvit
    protected static final String mgl_ColorEnabled     = "mgl_ColorEnabled";    //  1i
    protected static final String mgl_ColorStatic      = "mgl_ColorStatic";     //  4fv

    protected static final String mgl_LightSource      = "mgl_LightSource";     //  struct mgl_LightSourceParameters[MAX_LIGHTS]
    protected static final String mgl_FrontMaterial    = "mgl_FrontMaterial";   //  struct mgl_MaterialParameters
    protected static final String mgl_LightsEnabled    = "mgl_LightsEnabled";   //  int mgl_LightsEnabled[MAX_LIGHTS];

    protected static final String mgl_ShadeModel       = "mgl_ShadeModel";      //  1i

    protected static final String mgl_TexCoordEnabled  = "mgl_TexCoordEnabled"; //  int mgl_TexCoordEnabled[MAX_TEXTURE_UNITS];
    protected static final String mgl_ActiveTexture    = "mgl_ActiveTexture";   //  1i
    protected static final String mgl_ActiveTextureIdx = "mgl_ActiveTextureIdx";//  1i

    protected static final String mgl_CullFace         = "mgl_CullFace";   //  1i

    protected static final FloatBuffer zero4f     = Buffers.newDirectFloatBuffer(new float[] { 0.0f, 0.0f, 0.0f, 0.0f });

    public static final FloatBuffer defAmbient = Buffers.newDirectFloatBuffer(new float[] { 0f, 0f, 0f, 1f });
    public static final FloatBuffer defDiffuse = zero4f;
    public static final FloatBuffer defSpecular= zero4f;
    public static final FloatBuffer defPosition= Buffers.newDirectFloatBuffer(new float[] { 0f, 0f, 1f, 0f });
    public static final FloatBuffer defSpotDir = Buffers.newDirectFloatBuffer(new float[] { 0f, 0f, -1f });
    public static final float defSpotExponent  = 0f;
    public static final float defSpotCutoff    = 180f;
    public static final float defConstantAtten = 1f;
    public static final float defLinearAtten   = 0f;
    public static final float defQuadraticAtten= 0f;

    public static final FloatBuffer defMatAmbient = Buffers.newDirectFloatBuffer(new float[] { 0.2f, 0.2f, 0.2f, 1.0f });
    public static final FloatBuffer defMatDiffuse = Buffers.newDirectFloatBuffer(new float[] { 0.8f, 0.8f, 0.8f, 1.0f });
    public static final FloatBuffer defMatSpecular= Buffers.newDirectFloatBuffer(new float[] { 0f, 0f, 0f, 1f});
    public static final FloatBuffer defMatEmission= Buffers.newDirectFloatBuffer(new float[] { 0f, 0f, 0f, 1f});
    public static final float       defMatShininess = 0f;

    protected static final String vertexColorFileDef          = "FixedFuncColor";
    protected static final String vertexColorLightFileDef    = "FixedFuncColorLight";
    protected static final String fragmentColorFileDef        = "FixedFuncColor";
    protected static final String fragmentColorTextureFileDef = "FixedFuncColorTexture";
    protected static final String shaderSrcRootDef = "shaders" ;
    protected static final String shaderBinRootDef = "shaders/bin" ;
}

