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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLArrayData;
import com.jogamp.opengl.GLES2;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLRunnable2;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;
import com.jogamp.opengl.fixedfunc.GLPointerFuncUtil;

import jogamp.opengl.Debug;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.IntIntHashMap;
import com.jogamp.common.util.PropertyAccess;
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
    protected static final boolean DEBUG;

    static {
        Debug.initSingleton();
        DEBUG = PropertyAccess.isPropertyDefined("jogl.debug.FixedFuncPipeline", true);
    }

    /** The maximum texture units which could be used, depending on {@link ShaderSelectionMode}. */
    public static final int MAX_TEXTURE_UNITS = 8;
    public static final int MAX_LIGHTS        = 8;

    public FixedFuncPipeline(final GL2ES2 gl, final ShaderSelectionMode mode, final PMVMatrix pmvMatrix) {
        shaderRootClass = FixedFuncPipeline.class;
        shaderSrcRoot = shaderSrcRootDef;
        shaderBinRoot = shaderBinRootDef;
        vertexColorFile = vertexColorFileDef;
        vertexColorLightFile = vertexColorLightFileDef;
        fragmentColorFile = fragmentColorFileDef;
        fragmentColorTextureFile = fragmentColorTextureFileDef;
        init(gl, mode, pmvMatrix);
    }
    public FixedFuncPipeline(final GL2ES2 gl, final ShaderSelectionMode mode, final PMVMatrix pmvMatrix,
                             final Class<?> shaderRootClass, final String shaderSrcRoot,
                             final String shaderBinRoot,
                             final String vertexColorFile, final String vertexColorLightFile,
                             final String fragmentColorFile, final String fragmentColorTextureFile) {
        this.shaderRootClass = shaderRootClass;
        this.shaderSrcRoot = shaderSrcRoot;
        this.shaderBinRoot = shaderBinRoot;
        this.vertexColorFile = vertexColorFile;
        this.vertexColorLightFile = vertexColorLightFile;
        this.fragmentColorFile = fragmentColorFile;
        this.fragmentColorTextureFile = fragmentColorTextureFile;
        init(gl, mode, pmvMatrix);
    }

    public ShaderSelectionMode getShaderSelectionMode() { return requestedShaderSelectionMode; }
    public void setShaderSelectionMode(final ShaderSelectionMode mode) { requestedShaderSelectionMode=mode; }
    public ShaderSelectionMode getCurrentShaderSelectionMode() { return currentShaderSelectionMode; }

    public boolean verbose() { return verbose; }

    public void setVerbose(final boolean v) { verbose = DEBUG || v; }

    public boolean isValid() {
        return shaderState.linked();
    }

    public ShaderState getShaderState() {
        return shaderState;
    }

    public int getActiveTextureUnit() {
        return activeTextureUnit;
    }

    public void destroy(final GL2ES2 gl) {
        if(null != shaderProgramColor) {
            shaderProgramColor.release(gl, true);
        }
        if(null != shaderProgramColorLight) {
            shaderProgramColorLight.release(gl, true);
        }
        if(null != shaderProgramColorTexture2) {
            shaderProgramColorTexture2.release(gl, true);
        }
        if(null != shaderProgramColorTexture4) {
            shaderProgramColorTexture4.release(gl, true);
        }
        if(null != shaderProgramColorTexture4) {
            shaderProgramColorTexture4.release(gl, true);
        }
        if(null != shaderProgramColorTexture8Light) {
            shaderProgramColorTexture8Light.release(gl, true);
        }
        shaderState.destroy(gl);
    }

    //
    // Simple Globals
    //
    public void glColor4f(final GL2ES2 gl, final float red, final float green, final float blue, final float alpha) {
        colorStatic.put(0, red);
        colorStatic.put(1, green);
        colorStatic.put(2, blue);
        colorStatic.put(3, alpha);

        shaderState.useProgram(gl, true);
        final GLUniformData ud = shaderState.getUniform(mgl_ColorStatic);
        if(null!=ud) {
            // same data object ..
            shaderState.uniform(gl, ud);
        } else {
            throw new GLException("Failed to update: mgl_ColorStatic");
        }
    }

    //
    // Arrays / States
    //

    public void glEnableClientState(final GL2ES2 gl, final int glArrayIndex) {
        glToggleClientState(gl, glArrayIndex, true);
    }

    public void glDisableClientState(final GL2ES2 gl, final int glArrayIndex) {
        glToggleClientState(gl, glArrayIndex, false);
    }

    private void glToggleClientState(final GL2ES2 gl, final int glArrayIndex, final boolean enable) {
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

    public void glVertexPointer(final GL2ES2 gl, final GLArrayData data) {
        shaderState.useProgram(gl, true);
        shaderState.vertexAttribPointer(gl, data);
    }

    public void glColorPointer(final GL2ES2 gl, final GLArrayData data) {
        shaderState.useProgram(gl, true);
        shaderState.vertexAttribPointer(gl, data);
    }

    public void glNormalPointer(final GL2ES2 gl, final GLArrayData data) {
        shaderState.useProgram(gl, true);
        shaderState.vertexAttribPointer(gl, data);
    }

    //
    // MULTI-TEXTURE
    //

    /** Enables/Disables the named texture unit (if changed), returns previous state */
    private boolean glEnableTexture(final boolean enable, final int unit) {
        final boolean isEnabled = 0 != ( textureEnabledBits & ( 1 << activeTextureUnit ) );
        if( isEnabled != enable ) {
            if(enable) {
                textureEnabledBits |=  ( 1 << unit );
                textureEnabled.put(unit, 1);
            } else {
                textureEnabledBits &= ~( 1 << unit );
                textureEnabled.put(unit, 0);
            }
            textureEnabledDirty=true;
        }
        return isEnabled;
    }

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

    public void glTexCoordPointer(final GL2ES2 gl, final GLArrayData data) {
        if( GLPointerFunc.GL_TEXTURE_COORD_ARRAY != data.getIndex() ) {
            throw new GLException("Invalid GLArrayData Index "+toHexString(data.getIndex())+", "+data);
        }
        shaderState.useProgram(gl, true);
        data.setName( GLPointerFuncUtil.getPredefinedArrayIndexName(data.getIndex(), clientActiveTextureUnit) ) ;
        shaderState.vertexAttribPointer(gl, data);
    }

    public void glBindTexture(final int target, final int texture) {
        if(GL.GL_TEXTURE_2D == target) {
            if( texture != boundTextureObject[activeTextureUnit] ) {
                boundTextureObject[activeTextureUnit] = texture;
                textureFormatDirty = true;
            }
        } else {
            System.err.println("FixedFuncPipeline: Unimplemented glBindTexture for target "+toHexString(target)+". Texture name "+toHexString(texture));
        }
    }

    public void glTexImage2D(final int target, /* int level, */ final int internalformat, /*, int width, int height, int border, */
                             final int format /*, int type,  Buffer pixels */) {
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

    public void glTexEnvi(final int target, final int pname, final int value) {
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
                case GL.GL_BLEND:
                    mode = 4;
                    break;
                case GL.GL_REPLACE:
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
    private void setTextureEnvMode(final int value) {
        if( value != textureEnvMode.get(activeTextureUnit) ) {
            textureEnvMode.put(activeTextureUnit, value);
            textureEnvModeDirty = true;
        }
    }
    public void glGetTexEnviv(final int target, final int pname,  final IntBuffer params) { // FIXME
        System.err.println("FixedFuncPipeline: Unimplemented glGetTexEnviv: target "+toHexString(target)+", pname "+toHexString(pname));
    }
    public void glGetTexEnviv(final int target, final int pname,  final int[] params, final int params_offset) { // FIXME
        System.err.println("FixedFuncPipeline: Unimplemented glGetTexEnviv: target "+toHexString(target)+", pname "+toHexString(pname));
    }

    //
    // Point Sprites
    //
    public void glPointSize(final float size) {
        pointParams.put(0, size);
        pointParamsDirty = true;
    }
    public  void glPointParameterf(final int pname, final float param) {
        switch(pname) {
            case GL2ES1.GL_POINT_SIZE_MIN:
                pointParams.put(2, param);
                break;
            case GL2ES1.GL_POINT_SIZE_MAX:
                pointParams.put(3, param);
                break;
            case GL.GL_POINT_FADE_THRESHOLD_SIZE:
                pointParams.put(4+3, param);
                break;
        }
        pointParamsDirty = true;
    }
    public  void glPointParameterfv(final int pname, final float[] params, final int params_offset) {
        switch(pname) {
            case GL2ES1.GL_POINT_DISTANCE_ATTENUATION:
                pointParams.put(4+0, params[params_offset + 0]);
                pointParams.put(4+1, params[params_offset + 1]);
                pointParams.put(4+2, params[params_offset + 2]);
                break;
        }
        pointParamsDirty = true;
    }
    public  void glPointParameterfv(final int pname, final java.nio.FloatBuffer params) {
        final int o = params.position();
        switch(pname) {
            case GL2ES1.GL_POINT_DISTANCE_ATTENUATION:
                pointParams.put(4+0, params.get(o + 0));
                pointParams.put(4+1, params.get(o + 1));
                pointParams.put(4+2, params.get(o + 2));
                break;
        }
        pointParamsDirty = true;
    }

    // private int[] pointTexObj = new int[] { 0 };

    private void glDrawPoints(final GL2ES2 gl, final GLRunnable2<Object,Object> glDrawAction, final Object args) {
        if(gl.isGL2GL3()) {
            gl.glEnable(GL2GL3.GL_VERTEX_PROGRAM_POINT_SIZE);
        }
        if(gl.isGL2ES1()) {
            gl.glEnable(GL2ES1.GL_POINT_SPRITE);
        }
        loadShaderPoints(gl);
        shaderState.attachShaderProgram(gl, shaderProgramPoints, true);
        validate(gl, false); // sync uniforms

        glDrawAction.run(gl, args);

        if(gl.isGL2ES1()) {
            gl.glDisable(GL2ES1.GL_POINT_SPRITE);
        }
        if(gl.isGL2GL3()) {
            gl.glDisable(GL2GL3.GL_VERTEX_PROGRAM_POINT_SIZE);
        }
        shaderState.attachShaderProgram(gl, selectShaderProgram(gl, currentShaderSelectionMode), true);
    }
    private static final GLRunnable2<Object, Object> glDrawArraysAction = new GLRunnable2<Object,Object>() {
        @Override
        public Object run(final GL gl, final Object args) {
            final int[] _args = (int[])args;
            gl.glDrawArrays(GL.GL_POINTS, _args[0], _args[1]);
            return null;
        }
    };
    private final void glDrawPointArrays(final GL2ES2 gl, final int first, final int count) {
        glDrawPoints(gl, glDrawArraysAction, new int[] { first, count });
    }

    //
    // Lighting
    //

    public void glLightfv(final GL2ES2 gl, int light, final int pname, final java.nio.FloatBuffer params) {
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

    public void glMaterialfv(final GL2ES2 gl, final int face, final int pname, final java.nio.FloatBuffer params) {
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

    public void glShadeModel(final GL2ES2 gl, final int mode) {
        shaderState.useProgram(gl, true);
        final GLUniformData ud = shaderState.getUniform(mgl_ShadeModel);
        if(null!=ud) {
            ud.setData(mode);
            shaderState.uniform(gl, ud);
        }
    }

    /** ES2 supports CullFace implicit
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
    } */

    public  void glAlphaFunc(final int func, final float ref) {
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
    public boolean glEnable(final int cap, final boolean enable) {
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
                /** ES2 supports CullFace implicit
                final int _cullFace;
                if(0>cullFace && enable || 0<cullFace && !enable) {
                    _cullFace = cullFace * -1;
                } else {
                    _cullFace = cullFace;
                }
                if(_cullFace != cullFace) {
                    cullFaceDirty=true;
                    cullFace=_cullFace;
                } */
                return true;

            case GL.GL_TEXTURE_2D:
                glEnableTexture(enable, activeTextureUnit);
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

            case GL2ES1.GL_POINT_SMOOTH:
                pointParams.put(1, enable ? 1.0f : 0.0f);
                pointParamsDirty = true;
                return false;

            case GL2ES1.GL_POINT_SPRITE:
                // gl_PointCoord always enabled
                return false;
        }

        final int light = cap - GLLightingFunc.GL_LIGHT0;
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

    //
    // Draw
    //

    public void glDrawArrays(final GL2ES2 gl, int mode, final int first, final int count) {
        switch(mode) {
            case GL2.GL_QUAD_STRIP:
                mode=GL.GL_TRIANGLE_STRIP;
                break;
            case GL2.GL_POLYGON:
                mode=GL.GL_TRIANGLE_FAN;
                break;
            case GL.GL_POINTS:
                glDrawPointArrays(gl, first, count);
                return;
        }
        validate(gl, true);
        if ( GL2GL3.GL_QUADS == mode && !gl.isGL2() ) {
            for (int j = first; j < count - 3; j += 4) {
                gl.glDrawArrays(GL.GL_TRIANGLE_FAN, j, 4);
            }
        } else {
            gl.glDrawArrays(mode, first, count);
        }
    }
    public void glDrawElements(final GL2ES2 gl, final int mode, final int count, final int type, final java.nio.Buffer indices) {
        validate(gl, true);
        if ( GL2GL3.GL_QUADS == mode && !gl.isGL2() ) {
            final int idx0 = indices.position();

            if( GL.GL_UNSIGNED_BYTE == type ) {
                final ByteBuffer b = (ByteBuffer) indices;
                for (int j = 0; j < count; j++) {
                    gl.glDrawArrays(GL.GL_TRIANGLE_FAN, 0x000000ff & b.get(idx0+j), 4);
                }
            } else if( GL.GL_UNSIGNED_SHORT == type ){
                final ShortBuffer b = (ShortBuffer) indices;
                for (int j = 0; j < count; j++) {
                    gl.glDrawArrays(GL.GL_TRIANGLE_FAN, 0x0000ffff & b.get(idx0+j), 4);
                }
            } else {
                final IntBuffer b = (IntBuffer) indices;
                for (int j = 0; j < count; j++) {
                    gl.glDrawArrays(GL.GL_TRIANGLE_FAN, 0xffffffff & b.get(idx0+j), 4);
                }
            }
        } else {
            // FIXME: Impl. VBO usage .. or unroll (see above)!
            if( !gl.getContext().isCPUDataSourcingAvail() ) {
                throw new GLException("CPU data sourcing n/a w/ "+gl.getContext());
            }
            // if( GL.GL_POINTS != mode ) {
                ((GLES2)gl).glDrawElements(mode, count, type, indices);
            /* } else {
                // FIXME GL_POINTS !
                ((GLES2)gl).glDrawElements(mode, count, type, indices);
            } */
        }
    }
    public void glDrawElements(final GL2ES2 gl, final int mode, final int count, final int type, final long indices_buffer_offset) {
        validate(gl, true);
        if ( GL2GL3.GL_QUADS == mode && !gl.isGL2() ) {
            throw new GLException("Cannot handle indexed QUADS on !GL2 w/ VBO due to lack of CPU index access");
        } else /* if( GL.GL_POINTS != mode ) */ {
            gl.glDrawElements(mode, count, type, indices_buffer_offset);
        } /* else {
            // FIXME GL_POINTS !
            gl.glDrawElements(mode, count, type, indices_buffer_offset);
        } */
    }

    private final int textureEnabledCount() {
        int n=0;
        for(int i=MAX_TEXTURE_UNITS-1; i>=0; i--) {
            if( 0 != textureEnabled.get(i) ) {
                n++;
            }
        }
        return n;
    }

    public void validate(final GL2ES2 gl, final boolean selectShader) {
        if( selectShader ) {
            if( ShaderSelectionMode.AUTO == requestedShaderSelectionMode) {
                final ShaderSelectionMode newMode;

                // pre-validate shader switch
                if( 0 != textureEnabledBits ) {
                    if(lightingEnabled) {
                        newMode = ShaderSelectionMode.COLOR_TEXTURE8_LIGHT_PER_VERTEX;
                    } else {
                        final int n = textureEnabledCount();
                        if( 4 < n ) {
                            newMode = ShaderSelectionMode.COLOR_TEXTURE8;
                        } else if ( 2 < n ) {
                            newMode = ShaderSelectionMode.COLOR_TEXTURE4;
                        } else {
                            newMode = ShaderSelectionMode.COLOR_TEXTURE2;
                        }
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
        }

        GLUniformData ud;
        if( pmvMatrix.update() ) {
            ud = shaderState.getUniform(mgl_PMVMatrix);
            if(null!=ud) {
                final FloatBuffer m;
                if(ShaderSelectionMode.COLOR_TEXTURE8_LIGHT_PER_VERTEX == currentShaderSelectionMode ||
                   ShaderSelectionMode.COLOR_LIGHT_PER_VERTEX== currentShaderSelectionMode ) {
                    m = pmvMatrix.glGetPMvMvitMatrixf();
                } else {
                    m = pmvMatrix.glGetPMvMatrixf();
                }
                if(m != ud.getBuffer()) {
                    ud.setData(m);
                }
                // same data object ..
                shaderState.uniform(gl, ud);
            } else {
                throw new GLException("Failed to update: mgl_PMVMatrix");
            }
        }
        if(colorVAEnabledDirty) {
            ud = shaderState.getUniform(mgl_ColorEnabled);
            if(null!=ud) {
                final int ca = true == shaderState.isVertexAttribArrayEnabled(GLPointerFuncUtil.mgl_Color) ? 1 : 0 ;
                if(ca!=ud.intValue()) {
                    ud.setData(ca);
                    shaderState.uniform(gl, ud);
                }
            } else {
                throw new GLException("Failed to update: mgl_ColorEnabled");
            }
            colorVAEnabledDirty = false;
        }
        /** ES2 supports CullFace implicit
        if(cullFaceDirty) {
            ud = shaderState.getUniform(mgl_CullFace);
            if(null!=ud) {
                ud.setData(cullFace);
                shaderState.uniform(gl, ud);
            }
            cullFaceDirty = false;
        } */

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
        if(pointParamsDirty) {
            ud = shaderState.getUniform(mgl_PointParams);
            if(null!=ud) {
                // same data object
                shaderState.uniform(gl, ud);
            }
            pointParamsDirty = false;
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

    public StringBuilder toString(StringBuilder sb, final boolean alsoUnlocated) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        sb.append("FixedFuncPipeline[");
        sb.append(", textureEnabled: "+toHexString(textureEnabledBits)+", "); Buffers.toString(sb, null, textureEnabled);
        sb.append("\n\t, textureCoordEnabled: "); Buffers.toString(sb, null, textureCoordEnabled);
        sb.append("\n\t lightingEnabled: "+lightingEnabled);
        sb.append(", lightsEnabled: "); Buffers.toString(sb, null, lightsEnabled);
        sb.append("\n\t, shaderProgramColor: "+shaderProgramColor);
        sb.append("\n\t, shaderProgramColorTexture2: "+shaderProgramColorTexture2);
        sb.append("\n\t, shaderProgramColorTexture4: "+shaderProgramColorTexture4);
        sb.append("\n\t, shaderProgramColorTexture8: "+shaderProgramColorTexture8);
        sb.append("\n\t, shaderProgramColorLight: "+shaderProgramColorLight);
        sb.append("\n\t, shaderProgramColorTexture8Light: "+shaderProgramColorTexture8Light);
        sb.append("\n\t, ShaderState: ");
        shaderState.toString(sb, alsoUnlocated);
        sb.append("]");
        return sb;
    }
    @Override
    public String toString() {
        return toString(null, DEBUG).toString();
    }

    private static final String constMaxTextures0 = "#define MAX_TEXTURE_UNITS 0\n";
    private static final String constMaxTextures2 = "#define MAX_TEXTURE_UNITS 2\n";
    private static final String constMaxTextures4 = "#define MAX_TEXTURE_UNITS 4\n";
    private static final String constMaxTextures8 = "#define MAX_TEXTURE_UNITS 8\n";

    private final void customizeShader(final GL2ES2 gl, final ShaderCode vp, final ShaderCode fp, final String maxTextureDefine) {
        final int rsVpPos = vp.defaultShaderCustomization(gl, true, true);
        final int rsFpPos = fp.defaultShaderCustomization(gl, true, true);
        vp.insertShaderSource(0, rsVpPos, maxTextureDefine);
        fp.insertShaderSource(0, rsFpPos, maxTextureDefine);
    }

    private final void loadShaderPoints(final GL2ES2 gl) {
        if( null != shaderProgramPoints ) {
            return;
        }

        final ShaderCode vp = ShaderCode.create( gl, GL2ES2.GL_VERTEX_SHADER, shaderRootClass, shaderSrcRoot,
                                         shaderBinRoot, shaderPointFileDef, true);
        final ShaderCode fp = ShaderCode.create( gl, GL2ES2.GL_FRAGMENT_SHADER, shaderRootClass, shaderSrcRoot,
                                           shaderBinRoot, shaderPointFileDef, true);
        customizeShader(gl, vp, fp, constMaxTextures2);
        shaderProgramPoints = new ShaderProgram();
        shaderProgramPoints.add(vp);
        shaderProgramPoints.add(fp);
        if(!shaderProgramPoints.link(gl, System.err)) {
            throw new GLException("Couldn't link VertexColor program: "+shaderProgramPoints);
        }
    }

    private final void loadShader(final GL2ES2 gl, final ShaderSelectionMode mode) {
        final boolean loadColor = ShaderSelectionMode.COLOR == mode;
        final boolean loadColorTexture2 = ShaderSelectionMode.COLOR_TEXTURE2 == mode;
        final boolean loadColorTexture4 = ShaderSelectionMode.COLOR_TEXTURE4 == mode;
        final boolean loadColorTexture8 = ShaderSelectionMode.COLOR_TEXTURE8 == mode;
        final boolean loadColorTexture = loadColorTexture2 || loadColorTexture4 || loadColorTexture8 ;
        final boolean loadColorLightPerVertex = ShaderSelectionMode.COLOR_LIGHT_PER_VERTEX == mode;
        final boolean loadColorTexture8LightPerVertex = ShaderSelectionMode.COLOR_TEXTURE8_LIGHT_PER_VERTEX == mode;

        if( null != shaderProgramColor && loadColor ||
            null != shaderProgramColorTexture2 && loadColorTexture2 ||
            null != shaderProgramColorTexture4 && loadColorTexture4 ||
            null != shaderProgramColorTexture8 && loadColorTexture8 ||
            null != shaderProgramColorLight && loadColorLightPerVertex ||
            null != shaderProgramColorTexture8Light && loadColorTexture8LightPerVertex ) {
            return;
        }

        if( loadColor ) {
            final ShaderCode vp = ShaderCode.create( gl, GL2ES2.GL_VERTEX_SHADER, shaderRootClass, shaderSrcRoot,
                                             shaderBinRoot, vertexColorFile, true);
            final ShaderCode fp = ShaderCode.create( gl, GL2ES2.GL_FRAGMENT_SHADER, shaderRootClass, shaderSrcRoot,
                                               shaderBinRoot, fragmentColorFile, true);
            customizeShader(gl, vp, fp, constMaxTextures0);
            shaderProgramColor = new ShaderProgram();
            shaderProgramColor.add(vp);
            shaderProgramColor.add(fp);
            if(!shaderProgramColor.link(gl, System.err)) {
                throw new GLException("Couldn't link VertexColor program: "+shaderProgramColor);
            }
        } else  if( loadColorTexture ) {
            final ShaderCode vp = ShaderCode.create( gl, GL2ES2.GL_VERTEX_SHADER, shaderRootClass, shaderSrcRoot, shaderBinRoot, vertexColorFile, true);
            final ShaderCode fp = ShaderCode.create( gl, GL2ES2.GL_FRAGMENT_SHADER, shaderRootClass, shaderSrcRoot,
                                                     shaderBinRoot, fragmentColorTextureFile, true);

            if( loadColorTexture2 ) {
                customizeShader(gl, vp, fp, constMaxTextures2);
                shaderProgramColorTexture2 = new ShaderProgram();
                shaderProgramColorTexture2.add(vp);
                shaderProgramColorTexture2.add(fp);
                if(!shaderProgramColorTexture2.link(gl, System.err)) {
                    throw new GLException("Couldn't link VertexColorTexture2 program: "+shaderProgramColorTexture2);
                }
            } else if( loadColorTexture4 ) {
                customizeShader(gl, vp, fp, constMaxTextures4);
                shaderProgramColorTexture4 = new ShaderProgram();
                shaderProgramColorTexture4.add(vp);
                shaderProgramColorTexture4.add(fp);
                if(!shaderProgramColorTexture4.link(gl, System.err)) {
                    throw new GLException("Couldn't link VertexColorTexture4 program: "+shaderProgramColorTexture4);
                }
            } else if( loadColorTexture8 ) {
                customizeShader(gl, vp, fp, constMaxTextures8);
                shaderProgramColorTexture8 = new ShaderProgram();
                shaderProgramColorTexture8.add(vp);
                shaderProgramColorTexture8.add(fp);
                if(!shaderProgramColorTexture8.link(gl, System.err)) {
                    throw new GLException("Couldn't link VertexColorTexture8 program: "+shaderProgramColorTexture8);
                }
            }
        } else if( loadColorLightPerVertex ) {
            final ShaderCode vp = ShaderCode.create( gl, GL2ES2.GL_VERTEX_SHADER, shaderRootClass, shaderSrcRoot,
                                               shaderBinRoot, vertexColorLightFile, true);
            final ShaderCode fp = ShaderCode.create( gl, GL2ES2.GL_FRAGMENT_SHADER, shaderRootClass, shaderSrcRoot,
                                               shaderBinRoot, fragmentColorFile, true);
            customizeShader(gl, vp, fp, constMaxTextures0);
            shaderProgramColorLight = new ShaderProgram();
            shaderProgramColorLight.add(vp);
            shaderProgramColorLight.add(fp);
            if(!shaderProgramColorLight.link(gl, System.err)) {
                throw new GLException("Couldn't link VertexColorLight program: "+shaderProgramColorLight);
            }
        }  else if( loadColorTexture8LightPerVertex ) {
            final ShaderCode vp = ShaderCode.create( gl, GL2ES2.GL_VERTEX_SHADER, shaderRootClass, shaderSrcRoot,
                                               shaderBinRoot, vertexColorLightFile, true);
            final ShaderCode fp = ShaderCode.create( gl, GL2ES2.GL_FRAGMENT_SHADER, shaderRootClass, shaderSrcRoot,
                                                     shaderBinRoot, fragmentColorTextureFile, true);
            customizeShader(gl, vp, fp, constMaxTextures8);
            shaderProgramColorTexture8Light = new ShaderProgram();
            shaderProgramColorTexture8Light.add(vp);
            shaderProgramColorTexture8Light.add(fp);
            if(!shaderProgramColorTexture8Light.link(gl, System.err)) {
                throw new GLException("Couldn't link VertexColorLight program: "+shaderProgramColorTexture8Light);
            }
        }
    }

    private ShaderProgram selectShaderProgram(final GL2ES2 gl, ShaderSelectionMode newMode) {
        if(ShaderSelectionMode.AUTO == newMode) {
            newMode = ShaderSelectionMode.COLOR;
        }
        loadShader(gl, newMode);
        final ShaderProgram sp;
        switch(newMode) {
            case COLOR_LIGHT_PER_VERTEX:
                sp = shaderProgramColorLight;
                break;
            case COLOR_TEXTURE2:
                sp = shaderProgramColorTexture2;
                break;
            case COLOR_TEXTURE4:
                sp = shaderProgramColorTexture4;
                break;
            case COLOR_TEXTURE8:
                sp = shaderProgramColorTexture8;
                break;
            case COLOR_TEXTURE8_LIGHT_PER_VERTEX:
                sp = shaderProgramColorTexture8Light;
                break;
            case COLOR:
            default:
                sp = shaderProgramColor;
        }
        currentShaderSelectionMode = newMode;
        return sp;
    }

    private void init(final GL2ES2 gl, final ShaderSelectionMode mode, final PMVMatrix pmvMatrix) {
        if(null==pmvMatrix) {
            throw new GLException("PMVMatrix is null");
        }
        this.pmvMatrix=pmvMatrix;
        this.requestedShaderSelectionMode = mode;
        this.shaderState=new ShaderState();
        this.shaderState.setVerbose(verbose);

        shaderState.attachShaderProgram(gl, selectShaderProgram(gl, requestedShaderSelectionMode), true);

        // mandatory ..
        if(!shaderState.uniform(gl, new GLUniformData(mgl_PMVMatrix, 4, 4, pmvMatrix.glGetPMvMvitMatrixf()))) {
            throw new GLException("Error setting PMVMatrix in shader: "+this);
        }

        shaderState.uniform(gl, new GLUniformData(mgl_ColorEnabled,  0));
        shaderState.uniform(gl, new GLUniformData(mgl_ColorStatic, 4, colorStatic));

        texID2Format.setKeyNotFoundValue(0);
        shaderState.uniform(gl, new GLUniformData(mgl_TexCoordEnabled,  1, textureCoordEnabled));
        shaderState.uniform(gl, new GLUniformData(mgl_TexEnvMode, 1, textureEnvMode));
        shaderState.uniform(gl, new GLUniformData(mgl_TexFormat, 1, textureFormat));
        shaderState.uniform(gl, new GLUniformData(mgl_TextureEnabled, 1, textureEnabled));
        for(int i=0; i<MAX_TEXTURE_UNITS; i++) {
            shaderState.uniform(gl, new GLUniformData(mgl_Texture+i, i));
        }
        shaderState.uniform(gl, new GLUniformData(mgl_ShadeModel, 0));
        /** ES2 supports CullFace implicit
        shaderState.uniform(gl, new GLUniformData(mgl_CullFace, cullFace)); */
        shaderState.uniform(gl, new GLUniformData(mgl_AlphaTestFunc, alphaTestFunc));
        shaderState.uniform(gl, new GLUniformData(mgl_AlphaTestRef, alphaTestRef));
        shaderState.uniform(gl, new GLUniformData(mgl_PointParams, 4, pointParams));
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

    private String toHexString(final int i) {
        return "0x"+Integer.toHexString(i);
    }

    protected boolean verbose = DEBUG;

    private final FloatBuffer colorStatic = Buffers.copyFloatBuffer(one4f);

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

    /** ES2 supports CullFace implicit
    private int cullFace=-2; // <=0 disabled, 1 GL_FRONT, 2 GL_BACK (default) and 3 GL_FRONT_AND_BACK
    private boolean cullFaceDirty = false;
    private static final String mgl_CullFace         = "mgl_CullFace";        //  1i (lowp int) */

    private boolean colorVAEnabledDirty = false;
    private boolean lightingEnabled=false;
    private final IntBuffer lightsEnabled = Buffers.newDirectIntBuffer(new int[] { 0, 0, 0, 0, 0, 0, 0, 0 });
    private boolean   lightsEnabledDirty = false;

    private boolean alphaTestDirty=false;
    private int alphaTestFunc=-8; // <=0 disabled; 1 GL_NEVER, 2 GL_LESS, 3 GL_EQUAL, 4 GL_LEQUAL, 5 GL_GREATER, 6 GL_NOTEQUAL, 7 GL_GEQUAL, and 8 GL_ALWAYS (default)
    private float alphaTestRef=0f;

    private boolean pointParamsDirty = false;
    /** ( pointSize, pointSmooth, attn. pointMinSize, attn. pointMaxSize ) , ( attenuation coefficients 1f 0f 0f, attenuation fade theshold 1f )   */
    private final FloatBuffer pointParams = Buffers.newDirectFloatBuffer(new float[] {  1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f });

    private PMVMatrix pmvMatrix;
    private ShaderState shaderState;
    private ShaderProgram shaderProgramColor;
    private ShaderProgram shaderProgramColorTexture2, shaderProgramColorTexture4, shaderProgramColorTexture8;
    private ShaderProgram shaderProgramColorLight;
    private ShaderProgram shaderProgramColorTexture8Light;
    private ShaderProgram shaderProgramPoints;

    private ShaderSelectionMode requestedShaderSelectionMode = ShaderSelectionMode.AUTO;
    private ShaderSelectionMode currentShaderSelectionMode = requestedShaderSelectionMode;

    // uniforms ..
    private static final String mgl_PMVMatrix        = "mgl_PMVMatrix";       // m4fv[4] - P, Mv, Mvi and Mvit
    private static final String mgl_ColorEnabled     = "mgl_ColorEnabled";    //  1i
    private static final String mgl_ColorStatic      = "mgl_ColorStatic";     //  4fv

    private static final String mgl_LightModel       = "mgl_LightModel";      //  struct mgl_LightModelParameters
    private static final String mgl_LightSource      = "mgl_LightSource";     //  struct mgl_LightSourceParameters[MAX_LIGHTS]
    private static final String mgl_FrontMaterial    = "mgl_FrontMaterial";   //  struct mgl_MaterialParameters
    private static final String mgl_LightsEnabled    = "mgl_LightsEnabled";   //  int mgl_LightsEnabled[MAX_LIGHTS];

    private static final String mgl_AlphaTestFunc    = "mgl_AlphaTestFunc";   //  1i (lowp int)
    private static final String mgl_AlphaTestRef     = "mgl_AlphaTestRef";    //  1f
    private static final String mgl_ShadeModel       = "mgl_ShadeModel";      //  1i
    private static final String mgl_PointParams      = "mgl_PointParams";     //  vec4[2]: { (sz, smooth, attnMinSz, attnMaxSz), (attnCoeff(3), attnFadeTs) }

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

    private static final String vertexColorFileDef           = "FixedFuncColor";
    private static final String vertexColorLightFileDef      = "FixedFuncColorLight";
    private static final String fragmentColorFileDef         = "FixedFuncColor";
    private static final String fragmentColorTextureFileDef  = "FixedFuncColorTexture";
    private static final String shaderPointFileDef           = "FixedFuncPoints";
    private static final String shaderSrcRootDef             = "shaders" ;
    private static final String shaderBinRootDef             = "shaders/bin" ;

    private final Class<?> shaderRootClass;
    private final String shaderSrcRoot;
    private final String shaderBinRoot;
    private final String vertexColorFile;
    private final String vertexColorLightFile;
    private final String fragmentColorFile;
    private final String fragmentColorTextureFile;
}

