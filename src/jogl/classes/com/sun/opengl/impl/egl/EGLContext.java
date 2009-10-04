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

import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.gluegen.runtime.ProcAddressTable;
import java.nio.*;
import java.util.*;

public abstract class EGLContext extends GLContextImpl {
    private long eglContext;
    private boolean eglQueryStringInitialized;
    private boolean eglQueryStringAvailable;
    private EGLExt eglExt;
    // Table that holds the addresses of the native C-language entry points for
    // EGL extension functions.
    private EGLExtProcAddressTable eglExtProcAddressTable;

    public EGLContext(GLDrawableImpl drawable, GLDrawableImpl drawableRead,
                      GLContext shareWith) {
        super(drawable, drawableRead, shareWith);
    }

    public EGLContext(GLDrawableImpl drawable,
                      GLContext shareWith) {
        this(drawable, null, shareWith);
    }

    public Object getPlatformGLExtensions() {
      return getEGLExt();
    }

    public EGLExt getEGLExt() {
      if (eglExt == null) {
        eglExt = new EGLExtImpl(this);
      }
      return eglExt;
    }

    public final ProcAddressTable getPlatformExtProcAddressTable() {
        return eglExtProcAddressTable;
    }

    public final EGLExtProcAddressTable getEGLExtProcAddressTable() {
        return eglExtProcAddressTable;
    }

    protected String mapToRealGLFunctionName(String glFunctionName) {
        return glFunctionName;
    }

    protected String mapToRealGLExtensionName(String glExtensionName) {
        return glExtensionName;
    }

    public long getContext() {
        return eglContext;
    }

    protected int makeCurrentImpl() throws GLException {
        if(EGL.EGL_NO_DISPLAY==((EGLDrawable)drawable).getDisplay() ) {
            System.err.println("drawable not properly initialized");
            return CONTEXT_NOT_CURRENT;
        }
        boolean created = false;
        if (eglContext == 0) {
            create();
            if (DEBUG) {
                System.err.println(getThreadName() + ": !!! Created GL context 0x" +
                                   Long.toHexString(eglContext) + " for " + getClass().getName());
            }
            created = true;
        }
        if (EGL.eglGetCurrentContext() != eglContext) {
            if (!EGL.eglMakeCurrent(((EGLDrawable)drawable).getDisplay(),
                                    ((EGLDrawable)drawable).getSurface(),
                                    ((EGLDrawable)drawableRead).getSurface(),
                                    eglContext)) {
                throw new GLException("Error making context 0x" +
                                      Long.toHexString(eglContext) + " current: error code " + EGL.eglGetError());
            }
        }

        if (created) {
            setGLFunctionAvailability(false);
            return CONTEXT_CURRENT_NEW;
        }
        return CONTEXT_CURRENT;
    }

    protected void releaseImpl() throws GLException {
      getDrawableImpl().getFactoryImpl().lockToolkit();
      try {
          if (!EGL.eglMakeCurrent(((EGLDrawable)drawable).getDisplay(),
                                  EGL.EGL_NO_SURFACE,
                                  EGL.EGL_NO_SURFACE,
                                  EGL.EGL_NO_CONTEXT)) {
                throw new GLException("Error freeing OpenGL context 0x" +
                                      Long.toHexString(eglContext) + ": error code " + EGL.eglGetError());
          }
      } finally {
          getDrawableImpl().getFactoryImpl().unlockToolkit();
      }
    }

    protected void destroyImpl() throws GLException {
      getDrawableImpl().getFactoryImpl().lockToolkit();
      try {
          if (eglContext != 0) {
              if (!EGL.eglDestroyContext(((EGLDrawable)drawable).getDisplay(), eglContext)) {
                  throw new GLException("Error destroying OpenGL context 0x" +
                                        Long.toHexString(eglContext) + ": error code " + EGL.eglGetError());
              }
              eglContext = 0;
              GLContextShareSet.contextDestroyed(this);
          }
      } finally {
          getDrawableImpl().getFactoryImpl().unlockToolkit();
      }
    }

    protected void create() throws GLException {
        long eglDisplay = ((EGLDrawable)drawable).getDisplay();
        EGLGraphicsConfiguration config = ((EGLDrawable)drawable).getGraphicsConfiguration();
        GLProfile glProfile = drawable.getGLProfile();
        _EGLConfig eglConfig = config.getNativeConfig();
        long shareWith = EGL.EGL_NO_CONTEXT;

        if (eglDisplay == 0) {
            throw new GLException("Error: attempted to create an OpenGL context without a display connection");
        }
        if (eglConfig == null) {
            throw new GLException("Error: attempted to create an OpenGL context without a graphics configuration");
        }

        try {
            // might be unavailable on EGL < 1.2
            if(!EGL.eglBindAPI(EGL.EGL_OPENGL_ES_API)) {
                throw new GLException("eglBindAPI to ES failed , error 0x"+Integer.toHexString(EGL.eglGetError()));
            }
        } catch (GLException glex) {
            if (DEBUG) {
                glex.printStackTrace();
            }
        }

        EGLContext other = (EGLContext) GLContextShareSet.getShareContext(this);
        if (other != null) {
            shareWith = other.getContext();
            if (shareWith == 0) {
                throw new GLException("GLContextShareSet returned an invalid OpenGL context");
            }
        }

        int[] contextAttrs = new int[] {
                EGL.EGL_CONTEXT_CLIENT_VERSION, -1,
                EGL.EGL_NONE
        };
        if (glProfile.usesNativeGLES2()) {
            contextAttrs[1] = 2;
        } else if (glProfile.usesNativeGLES1()) {
            contextAttrs[1] = 1;
        } else {
            throw new GLException("Error creating OpenGL context - invalid GLProfile: "+glProfile);
        }
        eglContext = EGL.eglCreateContext(eglDisplay, eglConfig, shareWith, contextAttrs, 0);
        if (eglContext == 0) {
            throw new GLException("Error creating OpenGL context: eglDisplay 0x"+Long.toHexString(eglDisplay)+
                                  ", "+glProfile+", error 0x"+Integer.toHexString(EGL.eglGetError()));
        }
        GLContextShareSet.contextCreated(this);
        if (DEBUG) {
            System.err.println(getThreadName() + ": !!! Created OpenGL context 0x" +
                               Long.toHexString(eglContext) + 
                               ",\n\twrite surface 0x" + Long.toHexString(((EGLDrawable)drawable).getSurface()) +
                               ",\n\tread  surface 0x" + Long.toHexString(((EGLDrawable)drawableRead).getSurface())+
                               ",\n\t"+this+
                               ",\n\tsharing with 0x" + Long.toHexString(shareWith));
        }
        if (!EGL.eglMakeCurrent(((EGLDrawable)drawable).getDisplay(),
                                ((EGLDrawable)drawable).getSurface(),
                                ((EGLDrawable)drawableRead).getSurface(),
                                eglContext)) {
            throw new GLException("Error making context 0x" +
                                  Long.toHexString(eglContext) + " current: error code " + EGL.eglGetError());
        }
        setGLFunctionAvailability(true);
    }

    public boolean isCreated() {
        return (eglContext != 0);
    }

    protected void updateGLProcAddressTable() {
        if (DEBUG) {
          System.err.println(getThreadName() + ": !!! Initializing EGL extension address table");
        }
        eglQueryStringInitialized = false;
        eglQueryStringAvailable = false;

        if (eglExtProcAddressTable == null) {
          // FIXME: cache ProcAddressTables by capability bits so we can
          // share them among contexts with the same capabilities
          eglExtProcAddressTable = new EGLExtProcAddressTable();
        }          
        resetProcAddressTable(getEGLExtProcAddressTable());
        super.updateGLProcAddressTable();
    }
  
    public synchronized String getPlatformExtensionsString() {
        if (!eglQueryStringInitialized) {
          eglQueryStringAvailable =
            getDrawableImpl().getDynamicLookupHelper().dynamicLookupFunction("eglQueryString") != 0;
          eglQueryStringInitialized = true;
        }
        if (eglQueryStringAvailable) {
          GLDrawableFactoryImpl factory = getDrawableImpl().getFactoryImpl();
          factory.lockToolkit();
          try {
            String ret = EGL.eglQueryString(((EGLDrawable)drawable).getDisplay(), 
                                            EGL.EGL_EXTENSIONS);
            if (DEBUG) {
              System.err.println("!!! EGL extensions: " + ret);
            }
            return ret;
          } finally {
            factory.unlockToolkit();
          }
        } else {
          return "";
        }
    }

    protected void setSwapIntervalImpl(int interval) {
        if (EGL.eglSwapInterval(((EGLDrawable)drawable).getDisplay(), interval)) {
            currentSwapInterval = interval ;
        }
    }

    public abstract void bindPbufferToTexture();

    public abstract void releasePbufferFromTexture();

    //----------------------------------------------------------------------
    // Currently unimplemented stuff
    //

    public void copy(GLContext source, int mask) throws GLException {
        throw new GLException("Not yet implemented");
    }


    public ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
        throw new GLException("Should not call this");
    }

    public boolean offscreenImageNeedsVerticalFlip() {
        throw new GLException("Should not call this");
    }

    public int getOffscreenContextPixelDataType() {
        throw new GLException("Should not call this");
    }
}
