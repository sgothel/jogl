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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLArrayData;
import javax.media.opengl.GLException;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.media.opengl.fixedfunc.GLPointerFunc;
import javax.media.opengl.fixedfunc.GLPointerFuncUtil;

import jogamp.opengl.Debug;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.IntIntHashMap;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.glsl.fixedfunc.ShaderSelectionMode;

/**
 * 
 * <p>
 * Note: Certain GL FFP state values (e.g.: alphaTestFunc and cullFace) 
 *       are mapped to a lower number range so they can be stored in low precision storage, 
 *       i.e. in a 'lowp int' (GL ES2).
 * </p>
 */
public class FixedFuncPipeline {
    protected static final boolean DEBUG = Debug.isPropertyDefined("jogl.debug.FixedFuncPipeline", true);
    public static final int MAX_TEXTURE_UNITS = 8;
    public static final int MAX_LIGHTS        = 8;
    
    public FixedFuncPipeline(GL2ES2 gl, ShaderSelectionMode mode, PMVMatrix pmvMatrix) {
        init(gl, mode, pmvMatrix, FixedFuncPipeline.class, shaderSrcRootDef, 
             shaderBinRootDef, vertexColorFileDef, vertexColorLightFileDef, fragmentColorFileDef, fragmentColorTextureFileDef);
    }
    public FixedFuncPipeline(GL2ES2 gl, ShaderSelectionMode mode, PMVMatrix pmvMatrix, Class<?> shaderRootClass, String shaderSrcRoot, 
                       String shaderBinRoot,
                       String vertexColorFile,
                       String vertexColorLightFile,
                       String fragmentColorFile, String fragmentColorTextureFile) {
        init(gl, mode, pmvMatrix, shaderRootClass, shaderSrcRoot, 
             shaderBinRoot, vertexColorFile, vertexColorLightFile, fragmentColorFile, fragmentColorTextureFile);
    }
    
    public ShaderSelectionMode getShaderSelectionMode() { return shaderSelectionMode; }
    public void setShaderSelectionMode(ShaderSelectionMode mode) { shaderSelectionMode=mode; }

    public boolean verbose() { return verbose; }

    public void setVerbose(boolean v) { verbose = DEBUG || v; }

    public boolean isValid() {
        return shaderState.linked();
    }

    public ShaderState getShaderState() {
        return shaderState;
    }

    public int getActiveTextureUnit() {
        return activeTextureUnit;
    }

    public void destroy(GL2ES2 gl) {
        shaderProgramColor.release(gl, true);
        shaderProgramColorLight.release(gl, true);
        shaderProgramColorTexture.release(gl, true);
        shaderProgramColorTextureLight.release(gl, true);
        shaderState.destroy(gl);
    }

    //
    // Simple Globals
    //
    
    public void glColor4fv(GL2ES2 gl, FloatBuffer data ) {
        shaderState.useProgram(gl, true);
        GLUniformData ud = shaderState.getUniform(mgl_ColorStatic);
        if(null!=ud) {
            ud.setData(data);
            shaderState.uniform(gl, ud);
        }
    }

    //
    // Arrays / States
    //
    
    public void glEnableClientState(GL2ES2 gl, int glArrayIndex) {
        glToggleClientState(gl, glArrayIndex, true);
    }

    public void glDisableClientState(GL2ES2 gl, int glArrayIndex) {
        glToggleClientState(gl, glArrayIndex, false);
    }

    private void glToggleClientState(GL2ES2 gl, int glArrayIndex, boolean enable) {
        final String arrayName = GLPointerFuncUtil.getPredefinedArrayIndexName(glArrayIndex, clientActiveTextureUnit);
        if(null == arrayName) {
            throw new GLException("arrayIndex "+toHexString(glArrayIndex)+" unknown");
        }
        shaderState.useProgram(gl, true);
        if(enable) {
            shaderState.enableVertexAttribArray(gl, arrayName );
        } else {
            shaderState.disableVertexAttribArray(gl, arrayName );
        }
        switch( glArrayIndex ) {
            case GLPointerFunc.GL_TEXTURE_COORD_ARRAY:
                final int enableV = enable ? 1 : 0;
                // enable-bitwise:  textureCoordsEnabled |=  (1 << clientActiveTextureUnit);
                // disable-bitwise: textureCoordsEnabled &= ~(1 << clientActiveTextureUnit);
                if ( textureCoordEnabled.get(clientActiveTextureUnit) != enableV) {
                    textureCoordEnabled.put(clientActiveTextureUnit, enableV);
                    textureCoordEnabledDirty = true;
                }
                break;
            case GLPointerFunc.GL_COLOR_ARRAY:
                colorVAEnabledDirty = true;
                break;
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

    public void glNormalPointer(GL2ES2 gl, GLArrayData data) {
        shaderState.useProgram(gl, true);
        shaderState.vertexAttribPointer(gl, data);
    }
    
    //
    // MULTI-TEXTURE
    //

    public void glClientActiveTexture(int textureUnit) {
        textureUnit -= GL.GL_TEXTURE0;
        if(0 <= textureUnit && textureUnit<MAX_TEXTURE_UNITS) {
            clientActiveTextureUnit = textureUnit;
        } else {
            throw new GLException("glClientActiveTexture textureUnit not within GL_TEXTURE0 + [0.."+MAX_TEXTURE_UNITS+"]: "+textureUnit);
        }
    }
    
    public void glActiveTexture(int textureUnit) {
        textureUnit -= GL.GL_TEXTURE0;
        if(0 <= textureUnit && textureUnit<MAX_TEXTURE_UNITS) {
            activeTextureUnit = textureUnit;
        } else {
            throw new GLException("glActivateTexture textureUnit not within GL_TEXTURE0 + [0.."+MAX_TEXTURE_UNITS+"]: "+textureUnit);
        }
    }

    public void glTexCoordPointer(GL2ES2 gl, GLArrayData data) {
        if( GLPointerFunc.GL_TEXTURE_COORD_ARRAY != data.getIndex() ) {
            throw new GLException("Invalid GLArrayData Index "+toHexString(data.getIndex())+", "+data);
        }
        shaderState.useProgram(gl, true);
        data.setName( GLPointerFuncUtil.getPredefinedArrayIndexName(data.getIndex(), clientActiveTextureUnit) ) ;
        shaderState.vertexAttribPointer(gl, data);
    }
    
    public void glBindTexture(int target, int texture) {
        if(GL.GL_TEXTURE_2D == target) {
            if( texture != boundTextureObject[activeTextureUnit] ) {
                boundTextureObject[activeTextureUnit] = texture;
                textureFormatDirty = true;
            }
        } else {
            System.err.println("FixedFuncPipeline: Unimplemented glBindTexture for target "+toHexString(target)+". Texture name "+toHexString(texture));            
        }
    }
    
    public void glTexImage2D(int target, /* int level, */ int internalformat, /*, int width, int height, int border, */
                             int format /*, int type,  Buffer pixels */) {
        final int ifmt;
        if(GL.GL_TEXTURE_2D == target) {
            switch(internalformat) {            
            case 3:
            case GL.GL_RGB:
            case GL.GL_RGB565:
            case GL.GL_RGB8:
            case GL.GL_RGB10:
                ifmt = 3;
                break;
            case 4:
            case GL.GL_RGBA:
            case GL.GL_RGB5_A1:
            case GL.GL_RGBA4:
            case GL.GL_RGBA8:
            case GL.GL_RGB10_A2:
                ifmt = 4;
                break;
            default:
                System.err.println("FixedFuncPipeline: glTexImage2D TEXTURE_2D: Unimplemented internalformat "+toHexString(internalformat));
                ifmt = 4;
                break;
            }
            if( ifmt != texID2Format.put(boundTextureObject[activeTextureUnit], ifmt) ) {
                textureFormatDirty = true;
                // System.err.println("glTexImage2D TEXTURE_2D: internalformat ifmt "+toHexString(internalformat)+" fmt "+toHexString(format)+" -> "+toHexString(ifmt));
            }
        } else {
            System.err.println("FixedFuncPipeline: Unimplemented glTexImage2D: target "+toHexString(target)+", internalformat "+toHexString(internalformat));            
        }
    }
    /*
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                             int format, int type,  long pixels_buffer_offset) {        
        textureFormat.put(activeTextureUnit, internalformat);
        textureFormatDirty = true;
    }*/
         
    public void glTexEnvi(int target, int pname, int value) {
        if(GL2ES1.GL_TEXTURE_ENV == target && GL2ES1.GL_TEXTURE_ENV_MODE == pname) {
            final int mode;
            switch( value ) {
                case GL2ES1.GL_ADD:
                    mode = 1;
                    break;
                case GL2ES1.GL_MODULATE:
                    mode = 2;
                    break;
                case GL2ES1.GL_DECAL:
                    mode = 3;
                    break;
                case GL2ES1.GL_BLEND:
                    mode = 4;
                    break;
                case GL2ES1.GL_REPLACE:
                    mode = 5;
                    break;
                case GL2ES1.GL_COMBINE:
                    mode = 2; // FIXME
                    System.err.println("FixedFuncPipeline: glTexEnv GL_TEXTURE_ENV_MODE: unimplemented mode: "+toHexString(value));
                    break;
                default:
                    throw new GLException("glTexEnv GL_TEXTURE_ENV_MODE: invalid mode: "+toHexString(value));
            }
            setTextureEnvMode(mode);
        } else if(verbose) {
            System.err.println("FixedFuncPipeline: Unimplemented TexEnv: target "+toHexString(target)+", pname "+toHexString(pname)+", mode: "+toHexString(value));
        }
    }
    private void setTextureEnvMode(int value) {
        if( value != textureEnvMode.get(activeTextureUnit) ) {
            textureEnvMode.put(activeTextureUnit, value);
            textureEnvModeDirty = true;
        }        
    }
    public void glGetTexEnviv(int target, int pname,  IntBuffer params) { // FIXME
        System.err.println("FixedFuncPipeline: Unimplemented glGetTexEnviv: target "+toHexString(target)+", pname "+toHexString(pname));
    }
    public void glGetTexEnviv(int target, int pname,  int[] params, int params_offset) { // FIXME
        System.err.println("FixedFuncPipeline: Unimplemented glGetTexEnviv: target "+toHexString(target)+", pname "+toHexString(pname));
    }
    
    //
    // Lighting
    // 

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
                    throw new GLException("glLightfv invalid pname: "+toHexString(pname));
            }
            if(null!=ud) {
                ud.setData(params);
                shaderState.uniform(gl, ud);
            }
        } else {
            throw new GLException("glLightfv light not within [0.."+MAX_LIGHTS+"]: "+light);
        }
    }

    public void glMaterialfv(GL2ES2 gl, int face, int pname, java.nio.FloatBuffer params) {
        shaderState.useProgram(gl, true);

        switch (face) {
            case GL.GL_FRONT:
            case GL.GL_FRONT_AND_BACK:
                break;
            case GL.GL_BACK:
                System.err.println("FixedFuncPipeline: Unimplemented glMaterialfv GL_BACK face");
                return;
            default:
        }

        GLUniformData ud = null;
        switch(pname) {
            case  GLLightingFunc.GL_AMBIENT:
                ud = shaderState.getUniform(mgl_FrontMaterial+".ambient");
                break;
            case  GLLightingFunc.GL_AMBIENT_AND_DIFFUSE:
                {
                    ud = shaderState.getUniform(mgl_FrontMaterial+".ambient");
                    if(null!=ud) {
                        ud.setData(params);
                        shaderState.uniform(gl, ud);
                    }
                }
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
                throw new GLException("glMaterialfv invalid pname: "+toHexString(pname));
        }
        if(null!=ud) {
            ud.setData(params);
            shaderState.uniform(gl, ud);
        } else if(verbose) {
            
        }
    }

    //
    // Misc States
    //
    
    public void glShadeModel(GL2ES2 gl, int mode) {
        shaderState.useProgram(gl, true);
        GLUniformData ud = shaderState.getUniform(mgl_ShadeModel);
        if(null!=ud) {
            ud.setData(mode);
            shaderState.uniform(gl, ud);
        }
    }

    public void glCullFace(int faceName) {
        int _cullFace;
        switch(faceName) {
            case GL.GL_FRONT:
                _cullFace = 1;
                break;
            case GL.GL_BACK:
                _cullFace = 2;
                break;
            case GL.GL_FRONT_AND_BACK:
                _cullFace = 3;
                break;
            default:
                throw new GLException("glCullFace invalid faceName: "+toHexString(faceName));
        }
        if(0 < _cullFace) {
            if(0>cullFace) {
                _cullFace *= -1;
            }
            if(cullFace != _cullFace) {
                cullFace = _cullFace;
                cullFaceDirty=true;
            }
        }
    }

    public  void glAlphaFunc(int func, float ref) {
        int _func;
        switch(func) {
            case GL.GL_NEVER:
                _func = 1;
                break;
            case GL.GL_LESS:
                _func = 2;
                break;
            case GL.GL_EQUAL:
                _func = 3;
                break;
            case GL.GL_LEQUAL:
                _func = 4;
                break;
            case GL.GL_GREATER:
                _func = 5;
                break;
            case GL.GL_NOTEQUAL:
                _func = 6;
                break;
            case GL.GL_GEQUAL:
                _func = 7;
                break;
            case GL.GL_ALWAYS:
                _func = 8;
                break;
            default:
                throw new GLException("glAlphaFunc invalid func: "+toHexString(func));                    
        }
        if(0 < _func) {
            if(0>alphaTestFunc) {
                _func *= -1;
            }
            if( alphaTestFunc != _func || alphaTestRef != ref ) {
                alphaTestFunc = _func;
                alphaTestRef = ref;
                alphaTestDirty=true;
            }
        }
    }
    
    /**
     * @return false if digested in regard to GL2ES2 spec, 
     *         eg this call must not be passed to an underlying ES2 implementation.
     *         true if this call shall be passed to an underlying GL2ES2/ES2 implementation as well.
     */
    public boolean glEnable(int cap, boolean enable) {
        switch(cap) {
            case GL.GL_BLEND:
            case GL.GL_DEPTH_TEST:
            case GL.GL_DITHER:   
            case GL.GL_POLYGON_OFFSET_FILL:
            case GL.GL_SAMPLE_ALPHA_TO_COVERAGE:
            case GL.GL_SAMPLE_COVERAGE:
            case GL.GL_SCISSOR_TEST:
            case GL.GL_STENCIL_TEST:
                return true;
                
            case GL.GL_CULL_FACE:
                final int _cullFace;
                if(0>cullFace && enable || 0<cullFace && !enable) {
                    _cullFace = cullFace * -1;
                } else {
                    _cullFace = cullFace;
                }
                if(_cullFace != cullFace) {
                    cullFaceDirty=true;
                    cullFace=_cullFace;
                }
                return true;
                
            case GL.GL_TEXTURE_2D:
                final boolean isEnabled = 0 != ( textureEnabledBits & ( 1 << activeTextureUnit ) );  
                if( isEnabled != enable ) {
                    if(enable) {
                        textureEnabledBits |=  ( 1 << activeTextureUnit );
                        textureEnabled.put(activeTextureUnit, 1);
                    } else {
                        textureEnabledBits &= ~( 1 << activeTextureUnit );
                        textureEnabled.put(activeTextureUnit, 0);
                    }
                    textureEnabledDirty=true;
                }
                return false;
                
            case GLLightingFunc.GL_LIGHTING:
                lightingEnabled=enable;
                return false;
                
            case GL2ES1.GL_ALPHA_TEST:
                final int _alphaTestFunc;
                if(0>alphaTestFunc && enable || 0<alphaTestFunc && !enable) {
                    _alphaTestFunc = alphaTestFunc * -1;
                } else {
                    _alphaTestFunc = alphaTestFunc;
                }
                if(_alphaTestFunc != alphaTestFunc) {
                    alphaTestDirty=true;
                    alphaTestFunc=_alphaTestFunc;
                }
                return false;
        }

        int light = cap - GLLightingFunc.GL_LIGHT0;
        if(0 <= light && light < MAX_LIGHTS) {
            if ( (lightsEnabled.get(light)==1) != enable ) {
                lightsEnabled.put(light, enable?1:0);
                lightsEnabledDirty = true;
                return false;
            }
        }
        System.err.println("FixedFunctionPipeline: "+(enable ? "glEnable" : "glDisable")+" "+toHexString(cap)+" not handled in emulation and not supported in ES2");
        return false; // ignore!
    }

    public void validate(GL2ES2 gl) {
        if( ShaderSelectionMode.AUTO == shaderSelectionMode) {
            final ShaderSelectionMode newMode;
            
            // pre-validate shader switch
            if( 0 != textureEnabledBits ) {
                if(lightingEnabled) {
                    newMode = ShaderSelectionMode.COLOR_TEXTURE_LIGHT_PER_VERTEX;
                } else {
                    newMode = ShaderSelectionMode.COLOR_TEXTURE;
                }
            } else {
                if(lightingEnabled) {
                    newMode = ShaderSelectionMode.COLOR_LIGHT_PER_VERTEX;
                } else {
                    newMode = ShaderSelectionMode.COLOR;
                }
            }
            shaderState.attachShaderProgram(gl, selectShaderProgram(gl, newMode), true); // enables shader-program implicit
        } else {
            shaderState.useProgram(gl, true);
        }
        
        GLUniformData ud;
        if( pmvMatrix.update() ) {
            ud = shaderState.getUniform(mgl_PMVMatrix);
            if(null!=ud) {
                // same data object ..
                shaderState.uniform(gl, ud);
            } else {
                throw new GLException("Failed to update: mgl_PMVMatrix");
            }
        }
        if(colorVAEnabledDirty) { 
            ud = shaderState.getUniform(mgl_ColorEnabled);
            if(null!=ud) {
                int ca = (shaderState.isVertexAttribArrayEnabled(GLPointerFuncUtil.mgl_Color)==true)?1:0;
                if(ca!=ud.intValue()) {
                    ud.setData(ca);
                    shaderState.uniform(gl, ud);
                }
            }
            colorVAEnabledDirty = false;
        }
        if(cullFaceDirty) {
            ud = shaderState.getUniform(mgl_CullFace);
            if(null!=ud) {
                ud.setData(cullFace);
                shaderState.uniform(gl, ud);
            }
            cullFaceDirty = false;
        }

        if(alphaTestDirty) {
            ud = shaderState.getUniform(mgl_AlphaTestFunc);
            if(null!=ud) {
                ud.setData(alphaTestFunc);
                shaderState.uniform(gl, ud);
            }
            ud = shaderState.getUniform(mgl_AlphaTestRef);
            if(null!=ud) {
                ud.setData(alphaTestRef);
                shaderState.uniform(gl, ud);
            }
            alphaTestDirty = false;
        }
        if(lightsEnabledDirty) {
            ud = shaderState.getUniform(mgl_LightsEnabled);
            if(null!=ud) {
                // same data object 
                shaderState.uniform(gl, ud);
            }
            lightsEnabledDirty=false;
        }

        if(textureCoordEnabledDirty) {
            ud = shaderState.getUniform(mgl_TexCoordEnabled);
            if(null!=ud) {
                // same data object 
                shaderState.uniform(gl, ud);
            }
            textureCoordEnabledDirty=false;
        }        

        if(textureEnvModeDirty) {
            ud = shaderState.getUniform(mgl_TexEnvMode);
            if(null!=ud) {
                // same data object 
                shaderState.uniform(gl, ud);
            }
            textureEnvModeDirty = false;
        }
        
        if(textureFormatDirty) {
            for(int i = 0; i<MAX_TEXTURE_UNITS; i++) {
                textureFormat.put(i, texID2Format.get(boundTextureObject[i]));
            }
            ud = shaderState.getUniform(mgl_TexFormat);
            if(null!=ud) {
                // same data object 
                shaderState.uniform(gl, ud);
            }            
            textureFormatDirty = false;
        }            
        if(textureEnabledDirty) {
            ud = shaderState.getUniform(mgl_TextureEnabled);
            if(null!=ud) {
                // same data object 
                shaderState.uniform(gl, ud);
            }
            textureEnabledDirty=false;
        }
        
        if(verbose) {
            System.err.println("validate: "+toString(null, DEBUG).toString());
        }
    }

    public StringBuilder toString(StringBuilder sb, boolean alsoUnlocated) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        sb.append("FixedFuncPipeline[");
        sb.append(", textureEnabled: "+toHexString(textureEnabledBits)+", "); Buffers.toString(sb, null, textureEnabled);
        sb.append("\n\t, textureCoordEnabled: "); Buffers.toString(sb, null, textureCoordEnabled);
        sb.append("\n\t lightingEnabled: "+lightingEnabled);
        sb.append(", lightsEnabled: "); Buffers.toString(sb, null, lightsEnabled);
        sb.append("\n\t, shaderProgramColor: "+shaderProgramColor);
        sb.append("\n\t, shaderProgramColorTexture: "+shaderProgramColorTexture);
        sb.append("\n\t, shaderProgramColorLight: "+shaderProgramColorLight);
        sb.append("\n\t, shaderProgramColorTextureLight: "+shaderProgramColorTextureLight);
        sb.append("\n\t, ShaderState: ");
        shaderState.toString(sb, alsoUnlocated);
        sb.append("]");
        return sb;        
    }
    public String toString() {
        return toString(null, DEBUG).toString();
    }

    private ShaderProgram selectShaderProgram(GL2ES2 gl, ShaderSelectionMode mode) {
        final ShaderProgram sp;
        switch(mode) {
            case COLOR_LIGHT_PER_VERTEX:
                sp = shaderProgramColorLight;
                break;
            case COLOR_TEXTURE:
                sp = shaderProgramColorTexture;
                break;
            case COLOR_TEXTURE_LIGHT_PER_VERTEX:
                sp = shaderProgramColorTextureLight;
                break;
            case AUTO:
            case COLOR:
            default:
                sp = shaderProgramColor;
        }
        return sp;
    }
    
    private void init(GL2ES2 gl, ShaderSelectionMode mode, PMVMatrix pmvMatrix, Class<?> shaderRootClass, String shaderSrcRoot, 
                      String shaderBinRoot,
                      String vertexColorFile,
                      String vertexColorLightFile,
                      String fragmentColorFile, String fragmentColorTextureFile) 
   {
        if(null==pmvMatrix) {
            throw new GLException("PMVMatrix is null");
        }
        this.pmvMatrix=pmvMatrix;
        this.shaderSelectionMode = mode;
        this.shaderState=new ShaderState();
        this.shaderState.setVerbose(verbose);
        ShaderCode vertexColor, vertexColorLight, fragmentColor, fragmentColorTexture;

        vertexColor = ShaderCode.create( gl, GL2ES2.GL_VERTEX_SHADER, shaderRootClass, shaderSrcRoot,
                                         shaderBinRoot, vertexColorFile, false);

        vertexColorLight = ShaderCode.create( gl, GL2ES2.GL_VERTEX_SHADER, shaderRootClass, shaderSrcRoot,
                                           shaderBinRoot, vertexColorLightFile, false);

        fragmentColor = ShaderCode.create( gl, GL2ES2.GL_FRAGMENT_SHADER, shaderRootClass, shaderSrcRoot,
                                           shaderBinRoot, fragmentColorFile, false);

        fragmentColorTexture = ShaderCode.create( gl, GL2ES2.GL_FRAGMENT_SHADER, shaderRootClass, shaderSrcRoot,
                                                  shaderBinRoot, fragmentColorTextureFile, false);

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

        shaderState.attachShaderProgram(gl, selectShaderProgram(gl, shaderSelectionMode), true);

        // mandatory ..
        if(!shaderState.uniform(gl, new GLUniformData(mgl_PMVMatrix, 4, 4, pmvMatrix.glGetPMvMvitMatrixf()))) {
            throw new GLException("Error setting PMVMatrix in shader: "+this);
        }

        shaderState.uniform(gl, new GLUniformData(mgl_ColorEnabled,  0));
        shaderState.uniform(gl, new GLUniformData(mgl_ColorStatic, 4, one4f));
        
        texID2Format.setKeyNotFoundValue(0);        
        shaderState.uniform(gl, new GLUniformData(mgl_TexCoordEnabled,  1, textureCoordEnabled));
        shaderState.uniform(gl, new GLUniformData(mgl_TexEnvMode, 1, textureEnvMode));
        shaderState.uniform(gl, new GLUniformData(mgl_TexFormat, 1, textureFormat));        
        shaderState.uniform(gl, new GLUniformData(mgl_TextureEnabled, 1, textureEnabled));
        for(int i=0; i<MAX_TEXTURE_UNITS; i++) {
            shaderState.uniform(gl, new GLUniformData(mgl_Texture+i, i));
        }
        shaderState.uniform(gl, new GLUniformData(mgl_ShadeModel, 0));
        shaderState.uniform(gl, new GLUniformData(mgl_CullFace, cullFace));
        shaderState.uniform(gl, new GLUniformData(mgl_AlphaTestFunc, alphaTestFunc));
        shaderState.uniform(gl, new GLUniformData(mgl_AlphaTestRef, alphaTestRef));              
        for(int i=0; i<MAX_LIGHTS; i++) {
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].ambient", 4, defAmbient));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].diffuse", 4, 0==i ? one4f : defDiffuseN));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].specular", 4, 0==i ? one4f : defSpecularN));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].position", 4, defPosition));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].spotDirection", 3, defSpotDir));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].spotExponent", defSpotExponent));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].spotCutoff", defSpotCutoff));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].constantAttenuation", defConstantAtten));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].linearAttenuation", defLinearAtten));
            shaderState.uniform(gl, new GLUniformData(mgl_LightSource+"["+i+"].quadraticAttenuation", defQuadraticAtten));
        }        
        shaderState.uniform(gl, new GLUniformData(mgl_LightModel+".ambient", 4, defLightModelAmbient));
        shaderState.uniform(gl, new GLUniformData(mgl_LightsEnabled,  1, lightsEnabled));
        shaderState.uniform(gl, new GLUniformData(mgl_FrontMaterial+".ambient", 4, defMatAmbient));
        shaderState.uniform(gl, new GLUniformData(mgl_FrontMaterial+".diffuse", 4, defMatDiffuse));
        shaderState.uniform(gl, new GLUniformData(mgl_FrontMaterial+".specular", 4, defMatSpecular));
        shaderState.uniform(gl, new GLUniformData(mgl_FrontMaterial+".emission", 4, defMatEmission));
        shaderState.uniform(gl, new GLUniformData(mgl_FrontMaterial+".shininess", defMatShininess));

        shaderState.useProgram(gl, false);
        if(verbose) {
            System.err.println("init: "+toString(null, DEBUG).toString());
        }
    }

    private String toHexString(int i) {
        return "0x"+Integer.toHexString(i);
    }
    
    protected boolean verbose = DEBUG;

    private int activeTextureUnit=0;
    private int clientActiveTextureUnit=0;
    private final IntIntHashMap texID2Format = new IntIntHashMap();
    private final int[] boundTextureObject = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 }; // per unit
    private int textureEnabledBits = 0;
    private final IntBuffer textureEnabled = Buffers.newDirectIntBuffer(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 }); // per unit
    private boolean textureEnabledDirty = false;
    private final IntBuffer textureCoordEnabled = Buffers.newDirectIntBuffer(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 }); // per unit
    private boolean textureCoordEnabledDirty = false;    
    // textureEnvMode: 1 GL_ADD, 2 GL_MODULATE (default), 3 GL_DECAL, 4 GL_BLEND, 5 GL_REPLACE, 6 GL_COMBINE
    private final IntBuffer textureEnvMode = Buffers.newDirectIntBuffer(new int[] { 2, 2, 2, 2, 2, 2, 2, 2 });    
    private boolean textureEnvModeDirty = false;
    private final IntBuffer textureFormat = Buffers.newDirectIntBuffer(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 }); // per unit
    private boolean textureFormatDirty = false;

    private int cullFace=-2; // <=0 disabled, 1 GL_FRONT, 2 GL_BACK (default) and 3 GL_FRONT_AND_BACK
    private boolean cullFaceDirty = false;

    private boolean colorVAEnabledDirty = false;
    private boolean lightingEnabled=false;
    private final IntBuffer lightsEnabled = Buffers.newDirectIntBuffer(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });
    private boolean   lightsEnabledDirty = false;

    private boolean alphaTestDirty=false;
    private int alphaTestFunc=-8; // <=0 disabled; 1 GL_NEVER, 2 GL_LESS, 3 GL_EQUAL, 4 GL_LEQUAL, 5 GL_GREATER, 6 GL_NOTEQUAL, 7 GL_GEQUAL, and 8 GL_ALWAYS (default)
    private float alphaTestRef=0f;
        
    private PMVMatrix pmvMatrix;
    private ShaderState shaderState;
    private ShaderProgram shaderProgramColor;
    private ShaderProgram shaderProgramColorTexture;
    private ShaderProgram shaderProgramColorLight;
    private ShaderProgram shaderProgramColorTextureLight;
    
    private ShaderSelectionMode shaderSelectionMode = ShaderSelectionMode.AUTO;

    // uniforms ..
    private static final String mgl_PMVMatrix        = "mgl_PMVMatrix";       // m4fv[4] - P, Mv, Mvi and Mvit
    private static final String mgl_ColorEnabled     = "mgl_ColorEnabled";    //  1i
    private static final String mgl_ColorStatic      = "mgl_ColorStatic";     //  4fv

    private static final String mgl_LightModel       = "mgl_LightModel";      //  struct mgl_LightModelParameters
    private static final String mgl_LightSource      = "mgl_LightSource";     //  struct mgl_LightSourceParameters[MAX_LIGHTS]
    private static final String mgl_FrontMaterial    = "mgl_FrontMaterial";   //  struct mgl_MaterialParameters
    private static final String mgl_LightsEnabled    = "mgl_LightsEnabled";   //  int mgl_LightsEnabled[MAX_LIGHTS];

    private static final String mgl_CullFace         = "mgl_CullFace";        //  1i (lowp int)
    private static final String mgl_AlphaTestFunc    = "mgl_AlphaTestFunc";   //  1i (lowp int)
    private static final String mgl_AlphaTestRef     = "mgl_AlphaTestRef";    //  1f    
    private static final String mgl_ShadeModel       = "mgl_ShadeModel";      //  1i

    private static final String mgl_TextureEnabled   = "mgl_TextureEnabled";  //  int mgl_TextureEnabled[MAX_TEXTURE_UNITS];
    private static final String mgl_Texture          = "mgl_Texture";         //  sampler2D mgl_Texture<0..7>
    private static final String mgl_TexCoordEnabled  = "mgl_TexCoordEnabled"; //  int mgl_TexCoordEnabled[MAX_TEXTURE_UNITS];
    private static final String mgl_TexEnvMode       = "mgl_TexEnvMode";      //  int mgl_TexEnvMode[MAX_TEXTURE_UNITS];
    private static final String mgl_TexFormat        = "mgl_TexFormat";       //  int mgl_TexFormat[MAX_TEXTURE_UNITS];

    // private static final FloatBuffer zero4f = Buffers.newDirectFloatBuffer(new float[] { 0.0f, 0.0f, 0.0f, 0.0f });
    private static final FloatBuffer neut4f = Buffers.newDirectFloatBuffer(new float[] { 0.0f, 0.0f, 0.0f, 1.0f });
    private static final FloatBuffer one4f  = Buffers.newDirectFloatBuffer(new float[] { 1.0f, 1.0f, 1.0f, 1.0f });    

    public static final FloatBuffer defAmbient   = neut4f;
    public static final FloatBuffer defDiffuseN  = neut4f;
    public static final FloatBuffer defSpecularN = neut4f;
    public static final FloatBuffer defPosition  = Buffers.newDirectFloatBuffer(new float[] { 0f, 0f, 1f, 0f });
    public static final FloatBuffer defSpotDir   = Buffers.newDirectFloatBuffer(new float[] { 0f, 0f, -1f });
    public static final float defSpotExponent    = 0f;
    public static final float defSpotCutoff      = 180f;
    public static final float defConstantAtten   = 1f;
    public static final float defLinearAtten     = 0f;
    public static final float defQuadraticAtten  = 0f;

    public static final FloatBuffer defLightModelAmbient = Buffers.newDirectFloatBuffer(new float[] { 0.2f, 0.2f, 0.2f, 1.0f });
    
    public static final FloatBuffer defMatAmbient   = Buffers.newDirectFloatBuffer(new float[] { 0.2f, 0.2f, 0.2f, 1.0f });
    public static final FloatBuffer defMatDiffuse   = Buffers.newDirectFloatBuffer(new float[] { 0.8f, 0.8f, 0.8f, 1.0f });
    public static final FloatBuffer defMatSpecular  = neut4f;
    public static final FloatBuffer defMatEmission  = neut4f;
    public static final float       defMatShininess = 0f;

    private static final String vertexColorFileDef          = "FixedFuncColor";
    private static final String vertexColorLightFileDef     = "FixedFuncColorLight";
    private static final String fragmentColorFileDef        = "FixedFuncColor";
    private static final String fragmentColorTextureFileDef = "FixedFuncColorTexture";
    private static final String shaderSrcRootDef            = "shaders" ;
    private static final String shaderBinRootDef            = "shaders/bin" ;
}

