/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.impl.egl;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import java.nio.*;

public class EGLContext extends GLContextImpl {
    private EGLDrawable drawable;
    private long context;

    public EGLContext(EGLDrawable drawable, GLContext shareWith) {
        super(shareWith);
        this.drawable = drawable;
    }

    public GLDrawable getGLDrawable() {
        return drawable;
    }

    public long getContext() {
        return context;
    }

    protected int makeCurrentImpl() throws GLException {
        boolean created = false;
        if (context == 0) {
            create();
            if (DEBUG) {
                System.err.println(getThreadName() + ": !!! Created GL context 0x" +
                                   Long.toHexString(context) + " for " + getClass().getName());
            }
            created = true;
        }

        if (!EGL.eglMakeCurrent(drawable.getDisplay(),
                                drawable.getSurface(),
                                drawable.getSurface(),
                                context)) {
            throw new GLException("Error making context 0x" +
                                  Long.toHexString(context) + " current: error code " + EGL.eglGetError());
        }

        if (created) {
            resetGLFunctionAvailability();
            return CONTEXT_CURRENT_NEW;
        }
        return CONTEXT_CURRENT;
    }

    protected void releaseImpl() throws GLException {
        if (!EGL.eglMakeCurrent(drawable.getDisplay(),
                                EGL.EGL_NO_SURFACE,
                                EGL.EGL_NO_SURFACE,
                                EGL.EGL_NO_CONTEXT)) {
            throw new GLException("Error freeing OpenGL context 0x" +
                                  Long.toHexString(context) + ": error code " + EGL.eglGetError());
        }
    }

    protected void destroyImpl() throws GLException {
        if (context != 0) {
            if (!EGL.eglDestroyContext(drawable.getDisplay(), context)) {
                throw new GLException("Error destroying OpenGL context 0x" +
                                      Long.toHexString(context) + ": error code " + EGL.eglGetError());
            }
            context = 0;
            GLContextShareSet.contextDestroyed(this);
        }
    }

    protected void create() throws GLException {
        long display = drawable.getDisplay();
        _EGLConfig config = drawable.getConfig();
        long shareWith = 0;

        if (display == 0) {
            throw new GLException("Error: attempted to create an OpenGL context without a display connection");
        }
        if (config == null) {
            throw new GLException("Error: attempted to create an OpenGL context without a graphics configuration");
        }
        EGLContext other = (EGLContext) GLContextShareSet.getShareContext(this);
        if (other != null) {
            shareWith = other.getContext();
            if (shareWith == 0) {
                throw new GLException("GLContextShareSet returned an invalid OpenGL context");
            }
        }

        EGLDrawableFactory factory = (EGLDrawableFactory) GLDrawableFactory.getFactory();
        boolean isGLES2 = EGLDrawableFactory.PROFILE_GLES2.equals(factory.getProfile());
        int[] contextAttrs = null;
        // FIXME: need to determine whether to specify the context
        // attributes based on the EGL version
        if (isGLES2) {
            contextAttrs = new int[] {
                EGL.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL.EGL_NONE
            };
        }
        context = EGL.eglCreateContext(display, config, shareWith, contextAttrs, 0);
        if (context == 0) {
            throw new GLException("Error creating OpenGL context");
        }
        GLContextShareSet.contextCreated(this);
        if (DEBUG) {
            System.err.println(getThreadName() + ": !!! Created OpenGL context 0x" +
                               Long.toHexString(context) + " for " + this +
                               ", surface 0x" + Long.toHexString(drawable.getSurface()) +
                               ", sharing with 0x" + Long.toHexString(shareWith));
        }
    }

    public boolean isCreated() {
        return (context != 0);
    }

    //----------------------------------------------------------------------
    // Currently unimplemented stuff
    //

    public Object getPlatformGLExtensions() {
        return null;
    }

    public void copy(GLContext source, int mask) throws GLException {
        throw new GLException("Not yet implemented");
    }

    public void bindPbufferToTexture() {
        throw new GLException("Should not call this");
    }

    public void releasePbufferFromTexture() {
        throw new GLException("Should not call this");
    }

    public ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
        throw new GLException("Should not call this");
    }

    protected String mapToRealGLFunctionName(String glFunctionName) {
        return glFunctionName;
    }

    protected String mapToRealGLExtensionName(String glExtensionName) {
        return glExtensionName;
    }

    public String getPlatformExtensionsString() {
        return "";
    }

    public boolean offscreenImageNeedsVerticalFlip() {
        throw new GLException("Should not call this");
    }

    public int getOffscreenContextPixelDataType() {
        throw new GLException("Should not call this");
    }
}
