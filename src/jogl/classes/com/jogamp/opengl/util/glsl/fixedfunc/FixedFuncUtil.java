/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
 */

package com.jogamp.opengl.util.glsl.fixedfunc;

import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.*;

import jogamp.opengl.util.glsl.fixedfunc.*;

/**
 * Tool to pipeline GL2ES2 into a fixed function emulation implementing GL2ES1.
 */
public class FixedFuncUtil {
    /**
     * @return If gl is a GL2ES1 and force is false, return the type cast object,
     *         otherwise create a fixed function emulation pipeline using the given GL2ES2 impl
     *         and hook it to the GLContext via {@link GLContext#setGL(GL)}.
     * @throws GLException if the GL object is neither GL2ES1 nor GL2ES2
     */
    public static final GL2ES1 wrapFixedFuncEmul(GL gl, boolean force) {
        if(gl.isGL2ES2() && ( !gl.isGL2ES1() || force ) ) {
            GL2ES2 es2 = gl.getGL2ES2();
            FixedFuncHook hook = new FixedFuncHook(es2);
            FixedFuncImpl impl = new FixedFuncImpl(es2, hook);
            gl.getContext().setGL(impl);
            return impl;
        } else if(gl.isGL2ES1()) {
            return gl.getGL2ES1();
        }
        throw new GLException("GL Object is neither GL2ES1 nor GL2ES2: "+gl.getContext());
    }

    /**
     * @return If gl is a GL2ES1, return the type cast object,
     *         otherwise create a fixed function emulation pipeline using the GL2ES2 impl.
     *         and hook it to the GLContext via {@link GLContext#setGL(GL)}.
     * @throws GLException if the GL object is neither GL2ES1 nor GL2ES2
     */
    public static final GL2ES1 wrapFixedFuncEmul(GL gl) {
        return wrapFixedFuncEmul(gl, false);
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
