/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util.texture;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLException;

/**
 * Preserves a [ texture-unit, texture-target ] state.
 * <p>
 * The states keys are the retrieved active texture-unit and the given texture-target
 * for which the following states are being queried:
 * <pre>
 *   - texture-object
 *   - GL.GL_TEXTURE_MAG_FILTER
 *   - GL.GL_TEXTURE_MIN_FILTER
 *   - GL.GL_TEXTURE_WRAP_S
 *   - GL.GL_TEXTURE_WRAP_T
 * </pre>
 */
public class TextureState {
    /**
     * Returns the <code>pname</code> to query the <code>textureTarget</code> currently bound to the active texture-unit.
     * <p>
     * Returns <code>0</code> is <code>textureTarget</code> is not supported.
     * </p>
     */
    public static final int getTextureTargetQueryName(final int textureTarget) {
        final int texBindQName;
        switch(textureTarget) {
            case GL.GL_TEXTURE_2D: texBindQName = GL.GL_TEXTURE_BINDING_2D; break;
            case GL.GL_TEXTURE_CUBE_MAP: texBindQName = GL.GL_TEXTURE_BINDING_CUBE_MAP; break;
            case GL2ES2.GL_TEXTURE_3D: texBindQName = GL2ES2.GL_TEXTURE_BINDING_3D; break;
            case GL2GL3.GL_TEXTURE_1D: texBindQName = GL2GL3.GL_TEXTURE_BINDING_1D; break;
            case GL2GL3.GL_TEXTURE_1D_ARRAY: texBindQName = GL2GL3.GL_TEXTURE_BINDING_1D_ARRAY; break;
            case GL2ES3.GL_TEXTURE_2D_ARRAY: texBindQName = GL2ES3.GL_TEXTURE_BINDING_2D_ARRAY; break;
            case GL2GL3.GL_TEXTURE_RECTANGLE: texBindQName = GL2GL3.GL_TEXTURE_BINDING_RECTANGLE; break;
            case GL2GL3.GL_TEXTURE_BUFFER: texBindQName = GL2GL3.GL_TEXTURE_BINDING_BUFFER; break;
            case GL2ES2.GL_TEXTURE_2D_MULTISAMPLE: texBindQName = GL2ES2.GL_TEXTURE_BINDING_2D_MULTISAMPLE; break;
            case GL2ES2.GL_TEXTURE_2D_MULTISAMPLE_ARRAY: texBindQName = GL2ES2.GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY; break;
            default: texBindQName = 0;
        }
        return texBindQName;
    }

    private final int target;
    /**
     * <pre>
     *   0 - unit
     *   1 - texture object
     *   2 - GL.GL_TEXTURE_MAG_FILTER
     *   3 - GL.GL_TEXTURE_MIN_FILTER
     *   4 - GL.GL_TEXTURE_WRAP_S
     *   5 - GL.GL_TEXTURE_WRAP_T
     * </pre>
     */
    private final int[] state = new int[] { 0, 0, 0, 0, 0, 0 };

    private static final String toHexString(final int i) { return "0x"+Integer.toHexString(i); }

    private static final int activeTexture(final GL gl) {
        final int[] vi = { 0 };
        gl.glGetIntegerv(GL.GL_ACTIVE_TEXTURE, vi, 0);
        return vi[0];
    }

    /**
     * Creates a texture state for the retrieved active texture-unit and the given texture-target.
     * See {@link TextureState}.
     * @param gl current GL context's GL object
     * @param textureTarget
     * @throws GLException if textureTarget is not supported
     */
    public TextureState(final GL gl, final int textureTarget) throws GLException {
        this(gl, activeTexture(gl), textureTarget);
    }

    /**
     * Creates a texture state for the given active texture-unit and the given texture-target.
     * See {@link TextureState}.
     * @param gl current GL context's GL object
     * @param textureUnit  of range [ {@link GL#GL_TEXTURE0}.. ]
     * @param textureTarget
     * @throws GLException if textureTarget is not supported
     */
    public TextureState(final GL gl, final int textureUnit, final int textureTarget) throws GLException {
        target = textureTarget;
        state[0] = textureUnit;
        final int texBindQName = getTextureTargetQueryName(textureTarget);
        if( 0 == texBindQName ) {
            throw new GLException("Unsupported textureTarget "+toHexString(textureTarget));
        }
        gl.glGetIntegerv(texBindQName, state, 1);
        gl.glGetTexParameteriv(target, GL.GL_TEXTURE_MAG_FILTER, state, 2);
        gl.glGetTexParameteriv(target, GL.GL_TEXTURE_MIN_FILTER, state, 3);
        gl.glGetTexParameteriv(target, GL.GL_TEXTURE_WRAP_S, state, 4);
        gl.glGetTexParameteriv(target, GL.GL_TEXTURE_WRAP_T, state, 5);
    }

    /**
     * Restores the texture-unit's texture-target state.
     * <p>
     * First the texture-unit is activated, then all states are restored.
     * </p>
     * @param gl current GL context's GL object
     */
    public final void restore(final GL gl) {
        gl.glActiveTexture(state[0]);
        gl.glBindTexture(target, state[1]);
        gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, state[2]);
        gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, state[3]);
        gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_S, state[4]);
        gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_T, state[5]);
    }

    /** Returns the texture-unit of this state, key value. Unit is of range [ {@link GL#GL_TEXTURE0}.. ]. */
    public final int getUnit() { return state[0]; }
    /** Returns the texture-target of this state, key value. */
    public final int getTarget() { return target; }

    /** Returns the state's texture-object. */
    public final int getObject() { return state[1]; }
    /** Returns the state's mag-filter param. */
    public final int getMagFilter() { return state[2]; }
    /** Returns the state's min-filter param. */
    public final int getMinFilter() { return state[3]; }
    /** Returns the state's wrap-s param. */
    public final int getWrapS() { return state[4]; }
    /** Returns the state's wrap-t param. */
    public final int getWrapT() { return state[5]; }


    @Override
    public final String toString() {
        return "TextureState[unit "+(state[0] - GL.GL_TEXTURE0)+", target "+toHexString(target)+
                ": obj "+toHexString(state[1])+
                ", filter[mag "+toHexString(state[2])+", min "+toHexString(state[3])+"], "+
                ": wrap[s "+toHexString(state[4])+", t "+toHexString(state[5])+"]]";
    }
}
