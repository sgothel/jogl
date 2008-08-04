
package com.sun.opengl.impl.es2;

import javax.media.opengl.*;
import javax.media.opengl.util.*;
import javax.media.opengl.util.glsl.*;
import java.nio.*;

public class FixedFuncPipeline {
    public static final int MAX_TEXTURE_UNITS = 8;
    public static final int MAX_LIGHTS        = 8;

    public FixedFuncPipeline(GL2ES2 gl, PMVMatrix pmvMatrix) {
        init(gl, pmvMatrix);
    }

    public boolean verbose() { return verbose; }

    public void setVerbose(boolean v) { verbose=v; }

    public boolean isValid() {
        return shaderState.linked();
    }

    public ShaderState getShaderState() {
        return shaderState;
    }

    public void enableLighting(boolean enable) {
        lightingEnabled=enable;
    }

    public boolean lightingEnabled() {
        return lightingEnabled;
    }

    public int getActiveTextureUnit() {
        return activeTextureUnit;
    }

    public String getArrayIndexName(int glArrayIndex) {
      String name = GLContext.getPredefinedArrayIndexName(glArrayIndex); 
      switch(glArrayIndex) {
          case GL.GL_VERTEX_ARRAY:
          case GL.GL_NORMAL_ARRAY:
          case GL.GL_COLOR_ARRAY:
              break;
          case GL.GL_TEXTURE_COORD_ARRAY:
              name = name + activeTextureUnit;
      }
      return name;
    }

    public void release(GL2ES2 gl) {
        shaderState.release(gl);
        shaderProgramColor.release(gl, true);
        shaderProgramColorLight.release(gl, true);
        shaderProgramColorTexture.release(gl, true);
        shaderProgramColorTextureLight.release(gl, true);
    }

    public void glEnableClientState(GL2ES2 gl, int glArrayIndex) {
        shaderState.glUseProgram(gl, true);

        shaderState.glEnableVertexAttribArray(gl, getArrayIndexName(glArrayIndex));
        textureCoordsEnabled |=  (1 << activeTextureUnit);
    }

    public void glDisableClientState(GL2ES2 gl, int glArrayIndex) {
        shaderState.glUseProgram(gl, true);

        shaderState.glDisableVertexAttribArray(gl, getArrayIndexName(glArrayIndex));
        textureCoordsEnabled &= ~(1 << activeTextureUnit);
    }

    public void glVertexPointer(GL2ES2 gl, GLArrayData data) {
        shaderState.glUseProgram(gl, true);
        shaderState.glVertexAttribPointer(gl, data);
    }

    public void glColorPointer(GL2ES2 gl, GLArrayData data) {
        shaderState.glUseProgram(gl, true);
        shaderState.glVertexAttribPointer(gl, data);
    }

    public void glColor4fv(GL2ES2 gl, FloatBuffer data ) {
        shaderState.glUseProgram(gl, true);
        GLUniformData ud = shaderState.getUniform(mgl_ColorStatic);
        if(null!=ud) {
            ud.setData(data);
            shaderState.glUniform(gl, ud);
        }
    }

    public void glNormalPointer(GL2ES2 gl, GLArrayData data) {
        shaderState.glUseProgram(gl, true);
        shaderState.glVertexAttribPointer(gl, data);
    }

    public void glTexCoordPointer(GL2ES2 gl, GLArrayData data) {
        shaderState.glUseProgram(gl, true);
        data.setName( getArrayIndexName(data.getIndex()) );
        shaderState.glVertexAttribPointer(gl, data);
    }

    public void glLightfv(GL2ES2 gl, int light, int pname, java.nio.FloatBuffer params) {
        shaderState.glUseProgram(gl, true);
        light -=GL.GL_LIGHT0;
        if(0 <= light && light < MAX_LIGHTS) {
            GLUniformData ud = null;
            switch(pname) {
                case  GL.GL_AMBIENT:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].ambient");
                    break;
                case  GL.GL_DIFFUSE:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].diffuse");
                    break;
                case  GL.GL_SPECULAR:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].specular");
                    break;
                case GL.GL_POSITION:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].position");
                    break;
                case GL.GL_SPOT_DIRECTION:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].spotDirection");
                    break;
                case GL.GL_SPOT_EXPONENT:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].spotExponent");
                    break;
                case GL.GL_SPOT_CUTOFF:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].spotCutoff");
                    break;
                case GL.GL_CONSTANT_ATTENUATION:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].constantAttenuation");
                    break;
                case GL.GL_LINEAR_ATTENUATION:
                    ud = shaderState.getUniform(mgl_LightSource+"["+light+"].linearAttenuation");
                    break;
                case GL.GL_QUADRATIC_ATTENUATION:
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
                shaderState.glUniform(gl, ud);
            }
        } else if(verbose) {
            System.err.println("glLightfv light not within [0.."+MAX_LIGHTS+"]: "+light);
        }
    }

    public void glMaterialfv(GL2ES2 gl, int face, int pname, java.nio.FloatBuffer params) {
        shaderState.glUseProgram(gl, true);

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
            case  GL.GL_AMBIENT:
                ud = shaderState.getUniform(mgl_FrontMaterial+".ambient");
                break;
            case  GL.GL_AMBIENT_AND_DIFFUSE:
                glMaterialfv(gl, face, GL.GL_AMBIENT, params);
                // fall through intended ..
            case  GL.GL_DIFFUSE:
                ud = shaderState.getUniform(mgl_FrontMaterial+".diffuse");
                break;
            case  GL.GL_SPECULAR:
                ud = shaderState.getUniform(mgl_FrontMaterial+".specular");
                break;
            case  GL.GL_EMISSION:
                ud = shaderState.getUniform(mgl_FrontMaterial+".emission");
                break;
            case  GL.GL_SHININESS:
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
            shaderState.glUniform(gl, ud);
        }
    }

    public void glShadeModel(GL2ES2 gl, int mode) {
        shaderState.glUseProgram(gl, true);
        GLUniformData ud = shaderState.getUniform(mgl_ShadeModel);
        if(null!=ud) {
            ud.setData(mode);
            shaderState.glUniform(gl, ud);
        }
    }

    public void glActiveTexture(GL2ES2 gl, int textureUnit) {
        textureUnit -= GL.GL_TEXTURE0;
        if(0 <= textureUnit && textureUnit<MAX_TEXTURE_UNITS) {
            shaderState.glUseProgram(gl, true);
            GLUniformData ud;
            ud = shaderState.getUniform(mgl_ActiveTexture);
            if(null!=ud) {
                ud.setData(textureUnit);
                shaderState.glUniform(gl, ud);
            }
            ud = shaderState.getUniform(mgl_ActiveTextureIdx);
            if(null!=ud) {
                ud.setData(textureUnit);
                shaderState.glUniform(gl, ud);
            }
            activeTextureUnit = textureUnit;
        } else {
            throw new GLException("glActivateTexture textureUnit not within GL_TEXTURE0 + [0.."+MAX_TEXTURE_UNITS+"]: "+textureUnit);
        }
    }

    public void glEnable(GL2ES2 gl, int cap, boolean enable) {
        shaderState.glUseProgram(gl, true);

        switch(cap) {
            case GL.GL_TEXTURE_2D:
                textureEnabled=enable;
                return;
            case GL.GL_LIGHTING:
                lightingEnabled=enable;
                return;
        }

        int light = cap - GL.GL_LIGHT0;
        if(0 <= light && light < MAX_LIGHTS) {
            if(enable) {
                lightsEnabled |=  (1 << light);
            } else {
                lightsEnabled &= ~(1 << light);
            }
            return;
        }

    }

    public void validate(GL2ES2 gl) {
        shaderState.glUseProgram(gl, true);
        GLUniformData ud;
        if(pmvMatrix.update()) {
            ud = shaderState.getUniform(mgl_PMVMatrix);
            if(null!=ud) {
                // same data object ..
                shaderState.glUniform(gl, ud);
            } else {
                throw new GLException("Failed to update: mgl_PMVMatrix");
            }
            ud = shaderState.getUniform(mgl_NormalMatrix);
            if(null!=ud) {
                // same data object ..
                shaderState.glUniform(gl, ud);
            }
        }
        ud = shaderState.getUniform(mgl_ColorEnabled);
        if(null!=ud) {
            int ca = (shaderState.isVertexAttribArrayEnabled(GLContext.mgl_Color)==true)?1:0;
            if(ca!=ud.intValue()) {
                ud.setData(ca);
                shaderState.glUniform(gl, ud);
            }
        }

        ud = shaderState.getUniform(mgl_TexCoordEnabled);
        if(null!=ud) {
            if(textureCoordsEnabled!=ud.intValue()) {
                ud.setData(textureCoordsEnabled);
                shaderState.glUniform(gl, ud);
            }
        }

        ud = shaderState.getUniform(mgl_LightsEnabled);
        if(null!=ud) {
            if(lightsEnabled!=ud.intValue()) {
                ud.setData(lightsEnabled);
                shaderState.glUniform(gl, ud);
            }
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
            System.out.println("validate: "+this);
        }
    }

    public String toString() {
        return "FixedFuncPipeline[pmv: "+pmvMatrix+
               ", textureEnabled: "+textureEnabled+
               ", textureCoordsEnabled: "+textureCoordsEnabled+
               ", lightingEnabled: "+lightingEnabled+
               ", lightsEnabled: "+lightsEnabled+
               ", ShaderState: "+shaderState+
               "]";
    }

    protected void init(GL2ES2 gl, PMVMatrix pmvMatrix) {
        if(null==pmvMatrix) {
            throw new GLException("PMVMatrix is null");
        }
        this.pmvMatrix=pmvMatrix;
        this.shaderState=new ShaderState();
        this.shaderState.setVerbose(verbose);

        ShaderCode vertexColor = new ShaderCode( gl.GL_VERTEX_SHADER, 1, -1, null,
                                 FixedFuncShaderVertexColor.vertShaderSource);

        ShaderCode vertexColorLight = new ShaderCode( gl.GL_VERTEX_SHADER, 1, -1, null,
                                      FixedFuncShaderVertexColorLight.vertShaderSource);

        ShaderCode fragmentColor = new ShaderCode( gl.GL_FRAGMENT_SHADER, 1, -1, null,
                                    FixedFuncShaderFragmentColor.fragShaderSource);

        ShaderCode fragmentColorTexture = new ShaderCode( gl.GL_FRAGMENT_SHADER, 1, -1, null,
                                    FixedFuncShaderFragmentColorTexture.fragShaderSource);

        shaderProgramColor = new ShaderProgram();
        shaderProgramColor.add(vertexColor);
        shaderProgramColor.add(fragmentColor);
        if(!shaderProgramColor.link(gl, System.err)) {
            throw new GLException("Couldn't link VertexColor program");
        }

        shaderProgramColorTexture = new ShaderProgram();
        shaderProgramColorTexture.add(vertexColor);
        shaderProgramColorTexture.add(fragmentColorTexture);
        if(!shaderProgramColorTexture.link(gl, System.err)) {
            throw new GLException("Couldn't link VertexColorTexture program");
        }

        shaderProgramColorLight = new ShaderProgram();
        shaderProgramColorLight.add(vertexColorLight);
        shaderProgramColorLight.add(fragmentColor);
        if(!shaderProgramColorLight.link(gl, System.err)) {
            throw new GLException("Couldn't link VertexColorLight program");
        }

        shaderProgramColorTextureLight = new ShaderProgram();
        shaderProgramColorTextureLight.add(vertexColorLight);
        shaderProgramColorTextureLight.add(fragmentColorTexture);
        if(!shaderProgramColorTextureLight.link(gl, System.err)) {
            throw new GLException("Couldn't link VertexColorLight program");
        }

        shaderState.attachShaderProgram(gl, shaderProgramColor);
        shaderState.glUseProgram(gl, true);

        // mandatory ..
        if(!shaderState.glUniform(gl, new GLUniformData(mgl_PMVMatrix, 4, 4, pmvMatrix.glGetPMvMviMatrixf()))) {
            throw new GLException("Error setting PMVMatrix in shader: "+this);
        }

        // optional parameter ..
        shaderState.glUniform(gl, new GLUniformData(mgl_NormalMatrix, 3, 3, pmvMatrix.glGetNormalMatrixf()));

        shaderState.glUniform(gl, new GLUniformData(mgl_ColorEnabled,  0));
        shaderState.glUniform(gl, new GLUniformData(mgl_ColorStatic, 4, zero4f));
        shaderState.glUniform(gl, new GLUniformData(mgl_TexCoordEnabled,  textureCoordsEnabled));
        shaderState.glUniform(gl, new GLUniformData(mgl_ActiveTexture, activeTextureUnit));
        shaderState.glUniform(gl, new GLUniformData(mgl_ActiveTextureIdx, activeTextureUnit));
        shaderState.glUniform(gl, new GLUniformData(mgl_ShadeModel, 0));
        for(int i=0; i<MAX_LIGHTS; i++) {
            shaderState.glUniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].ambient", 4, defAmbient));
            shaderState.glUniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].diffuse", 4, defDiffuse));
            shaderState.glUniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].specular", 4, defSpecular));
            shaderState.glUniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].position", 4, defPosition));
            shaderState.glUniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].spotDirection", 3, defSpotDir));
            shaderState.glUniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].spotExponent", defSpotExponent));
            shaderState.glUniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].spotCutoff", defSpotCutoff));
            shaderState.glUniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].constantAttenuation", defConstantAtten));
            shaderState.glUniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].linearAttenuation", defLinearAtten));
            shaderState.glUniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].quadraticAttenuation", defQuadraticAtten));
        }
        shaderState.glUniform(gl, new GLUniformData(mgl_LightsEnabled,  lightsEnabled));
        shaderState.glUniform(gl, new GLUniformData(mgl_FrontMaterial+".ambient", 4, defMatAmbient));
        shaderState.glUniform(gl, new GLUniformData(mgl_FrontMaterial+".diffuse", 4, defMatDiffuse));
        shaderState.glUniform(gl, new GLUniformData(mgl_FrontMaterial+".specular", 4, defMatSpecular));
        shaderState.glUniform(gl, new GLUniformData(mgl_FrontMaterial+".emission", 4, defMatEmission));
        shaderState.glUniform(gl, new GLUniformData(mgl_FrontMaterial+".shininess", defMatShininess));

        shaderState.glUseProgram(gl, false);
    }

    protected static final boolean DEBUG=false;

    protected boolean verbose=false;

    protected boolean textureEnabled=false;
    protected int     textureCoordsEnabled=0;
    protected int     activeTextureUnit=0;

    protected boolean lightingEnabled=false;
    protected int     lightsEnabled=0;

    protected PMVMatrix pmvMatrix;
    protected ShaderState shaderState;
    protected ShaderProgram shaderProgramColor;
    protected ShaderProgram shaderProgramColorTexture;
    protected ShaderProgram shaderProgramColorLight;
    protected ShaderProgram shaderProgramColorTextureLight;

    // uniforms ..
    protected static final String mgl_PMVMatrix        = "mgl_PMVMatrix";       // m4fv[3]
    protected static final String mgl_NormalMatrix     = "mgl_NormalMatrix";    // m4fv
    protected static final String mgl_ColorEnabled     = "mgl_ColorEnabled";    //  1i
    protected static final String mgl_ColorStatic      = "mgl_ColorStatic";     //  4fv

    protected static final String mgl_LightSource      = "mgl_LightSource";     //  struct mgl_LightSourceParameters[MAX_LIGHTS]
    protected static final String mgl_FrontMaterial    = "mgl_FrontMaterial";   //  struct mgl_MaterialParameters
    protected static final String mgl_LightsEnabled    = "mgl_LightsEnabled";   //  1i bitfield

    protected static final String mgl_ShadeModel       = "mgl_ShadeModel";      //  1i

    protected static final String mgl_TexCoordEnabled  = "mgl_TexCoordEnabled"; //  1i bitfield
    protected static final String mgl_ActiveTexture    = "mgl_ActiveTexture";   //  1i
    protected static final String mgl_ActiveTextureIdx = "mgl_ActiveTextureIdx";//  1i

    protected static final FloatBuffer zero4f     = BufferUtil.newFloatBuffer(new float[] { 0.0f, 0.0f, 0.0f, 0.0f });

    public static final FloatBuffer defAmbient = BufferUtil.newFloatBuffer(new float[] { 0f, 0f, 0f, 1f });
    public static final FloatBuffer defDiffuse = zero4f;
    public static final FloatBuffer defSpecular= zero4f;
    public static final FloatBuffer defPosition= BufferUtil.newFloatBuffer(new float[] { 0f, 0f, 1f, 0f });
    public static final FloatBuffer defSpotDir = BufferUtil.newFloatBuffer(new float[] { 0f, 0f, -1f });
    public static final float defSpotExponent  = 0f;
    public static final float defSpotCutoff    = 180f;
    public static final float defConstantAtten = 1f;
    public static final float defLinearAtten   = 0f;
    public static final float defQuadraticAtten= 0f;

    public static final FloatBuffer defMatAmbient = BufferUtil.newFloatBuffer(new float[] { 0.2f, 0.2f, 0.2f, 1.0f });
    public static final FloatBuffer defMatDiffuse = BufferUtil.newFloatBuffer(new float[] { 0.8f, 0.8f, 0.8f, 1.0f });
    public static final FloatBuffer defMatSpecular= BufferUtil.newFloatBuffer(new float[] { 0f, 0f, 0f, 1f});
    public static final FloatBuffer defMatEmission= BufferUtil.newFloatBuffer(new float[] { 0f, 0f, 0f, 1f});
    public static final float       defMatShininess = 0f;
}

