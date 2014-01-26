/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
 */

package com.jogamp.opengl.util.glsl.fixedfunc;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.fixedfunc.GLPointerFuncUtil;

import jogamp.opengl.util.glsl.fixedfunc.FixedFuncHook;
import jogamp.opengl.util.glsl.fixedfunc.FixedFuncImpl;
import jogamp.opengl.util.glsl.fixedfunc.FixedFuncPipeline;

import com.jogamp.opengl.util.PMVMatrix;


/**
 * Tool to pipeline GL2ES2 into a fixed function emulation implementing GL2ES1.
 */
public class FixedFuncUtil {
    /**
     * @param gl
     * @param mode one of the {@link ShaderSelectionMode}s
     * @param pmvMatrix optional pass through PMVMatrix for the {@link FixedFuncHook} and {@link FixedFuncPipeline}
     * @return If gl is a GL2ES1 and force is false, return the type cast object,
     *         otherwise create a fixed function emulation pipeline using the given GL2ES2 impl
     *         and hook it to the GLContext via {@link GLContext#setGL(GL)}.
     * @throws GLException if the GL object is neither GL2ES1 nor GL2ES2
     *
     * @see ShaderSelectionMode#AUTO
     * @see ShaderSelectionMode#COLOR
     * @see ShaderSelectionMode#COLOR_LIGHT_PER_VERTEX
     * @see ShaderSelectionMode#COLOR_TEXTURE
     * @see ShaderSelectionMode#COLOR_TEXTURE_LIGHT_PER_VERTEX
     */
    public static final GL2ES1 wrapFixedFuncEmul(GL gl, ShaderSelectionMode mode, PMVMatrix pmvMatrix, boolean force, boolean verbose) {
        if(gl.isGL2ES2() && ( !gl.isGL2ES1() || force ) ) {
            final GL2ES2 es2 = gl.getGL2ES2();
            final FixedFuncHook hook = new FixedFuncHook(es2, mode, pmvMatrix);
            hook.setVerbose(verbose);
            final FixedFuncImpl impl = new FixedFuncImpl(es2, hook);
            gl.getContext().setGL(impl);
            return impl;
        } else if(gl.isGL2ES1()) {
            return gl.getGL2ES1();
        }
        throw new GLException("GL Object is neither GL2ES1 nor GL2ES2: "+gl.getContext());
    }

    /**
     * @param gl
     * @param mode one of the {@link ShaderSelectionMode}s
     * @param pmvMatrix optional pass through PMVMatrix for the {@link FixedFuncHook} and {@link FixedFuncPipeline}
     * @return If gl is a GL2ES1, return the type cast object,
     *         otherwise create a fixed function emulation pipeline using the GL2ES2 impl.
     *         and hook it to the GLContext via {@link GLContext#setGL(GL)}.
     * @throws GLException if the GL object is neither GL2ES1 nor GL2ES2
     *
     * @see ShaderSelectionMode#AUTO
     * @see ShaderSelectionMode#COLOR
     * @see ShaderSelectionMode#COLOR_LIGHT_PER_VERTEX
     * @see ShaderSelectionMode#COLOR_TEXTURE
     * @see ShaderSelectionMode#COLOR_TEXTURE_LIGHT_PER_VERTEX
     */
    public static final GL2ES1 wrapFixedFuncEmul(GL gl, ShaderSelectionMode mode, PMVMatrix pmvMatrix) {
        return wrapFixedFuncEmul(gl, mode, null, false, false);
    }

    /**
     * Mapping fixed function (client) array indices to
     * GLSL array attribute names.
     *
     * Useful for uniq mapping of canonical array index names as listed.
     *
     * @see #mgl_Vertex
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#GL_VERTEX_ARRAY
     * @see #mgl_Normal
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#GL_NORMAL_ARRAY
     * @see #mgl_Color
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#GL_COLOR_ARRAY
     * @see #mgl_MultiTexCoord
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#GL_TEXTURE_COORD_ARRAY
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#glEnableClientState
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#glVertexPointer
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#glColorPointer
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#glNormalPointer
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#glTexCoordPointer
     */
    public static String getPredefinedArrayIndexName(int glArrayIndex) {
        return GLPointerFuncUtil.getPredefinedArrayIndexName(glArrayIndex);
    }

    /**
     * String name for
     * @see javax.media.opengl.GL2#GL_VERTEX_ARRAY
     */
    public static final String mgl_Vertex = GLPointerFuncUtil.mgl_Vertex;

    /**
     * String name for
     * @see javax.media.opengl.GL2#GL_NORMAL_ARRAY
     */
    public static final String mgl_Normal = GLPointerFuncUtil.mgl_Normal;

    /**
     * String name for
     * @see javax.media.opengl.GL2#GL_COLOR_ARRAY
     */
    public static final String mgl_Color = GLPointerFuncUtil.mgl_Color;

    /**
     * String name for
     * @see javax.media.opengl.GL2#GL_TEXTURE_COORD_ARRAY
     */
    public static final String mgl_MultiTexCoord = GLPointerFuncUtil.mgl_MultiTexCoord;
}
