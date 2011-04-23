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

package jogamp.opengl.egl;

import javax.media.opengl.*;
import jogamp.opengl.*;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;
import java.nio.*;
import java.util.*;
import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;

public abstract class EGLContext extends GLContextImpl {
    private boolean eglQueryStringInitialized;
    private boolean eglQueryStringAvailable;
    private EGLExt eglExt;
    // Table that holds the addresses of the native C-language entry points for
    // EGL extension functions.
    private EGLExtProcAddressTable eglExtProcAddressTable;

    EGLContext(GLDrawableImpl drawable,
               GLContext shareWith) {
        super(drawable, shareWith);
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

    protected Map<String, String> getFunctionNameMap() { return null; }

    protected Map<String, String> getExtensionNameMap() { return null; }

    public final boolean isGLReadDrawableAvailable() {
        return true;
    }

    protected void makeCurrentImpl(boolean newCreated) throws GLException {
        if(EGL.EGL_NO_DISPLAY==((EGLDrawable)drawable).getDisplay() ) {
            throw new GLException("drawable not properly initialized, NO DISPLAY: "+drawable);
        }
        if (EGL.eglGetCurrentContext() != contextHandle) {
            if (!EGL.eglMakeCurrent(((EGLDrawable)drawable).getDisplay(),
                                    drawable.getHandle(),
                                    drawableRead.getHandle(),
                                    contextHandle)) {
                throw new GLException("Error making context 0x" +
                                      Long.toHexString(contextHandle) + " current: error code " + EGL.eglGetError());
            }
        }
    }

    protected void releaseImpl() throws GLException {
      if (!EGL.eglMakeCurrent(((EGLDrawable)drawable).getDisplay(),
                              EGL.EGL_NO_SURFACE,
                              EGL.EGL_NO_SURFACE,
                              EGL.EGL_NO_CONTEXT)) {
            throw new GLException("Error freeing OpenGL context 0x" +
                                  Long.toHexString(contextHandle) + ": error code " + EGL.eglGetError());
      }
    }

    protected void destroyImpl() throws GLException {
      if (!EGL.eglDestroyContext(((EGLDrawable)drawable).getDisplay(), contextHandle)) {
          throw new GLException("Error destroying OpenGL context 0x" +
                                Long.toHexString(contextHandle) + ": error code " + EGL.eglGetError());
      }
    }

    protected long createContextARBImpl(long share, boolean direct, int ctp, int major, int minor) {
        return 0; // FIXME
    }

    protected void destroyContextARBImpl(long _context) {
        // FIXME
    }

    protected boolean createImpl() throws GLException {
        long eglDisplay = ((EGLDrawable)drawable).getDisplay();
        EGLGraphicsConfiguration config = ((EGLDrawable)drawable).getGraphicsConfiguration();
        GLProfile glProfile = drawable.getGLProfile();
        long eglConfig = config.getNativeConfig();
        long shareWith = EGL.EGL_NO_CONTEXT;

        if (eglDisplay == 0) {
            throw new GLException("Error: attempted to create an OpenGL context without a display connection");
        }
        if (eglConfig == 0) {
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
            shareWith = other.getHandle();
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
        contextHandle = EGL.eglCreateContext(eglDisplay, eglConfig, shareWith, contextAttrs, 0);
        if (contextHandle == 0) {
            throw new GLException("Error creating OpenGL context: eglDisplay 0x"+Long.toHexString(eglDisplay)+
                                  ", "+glProfile+", error 0x"+Integer.toHexString(EGL.eglGetError()));
        }
        GLContextShareSet.contextCreated(this);
        if (DEBUG) {
            System.err.println(getThreadName() + ": !!! Created OpenGL context 0x" +
                               Long.toHexString(contextHandle) + 
                               ",\n\twrite surface 0x" + Long.toHexString(drawable.getHandle()) +
                               ",\n\tread  surface 0x" + Long.toHexString(drawableRead.getHandle())+
                               ",\n\t"+this+
                               ",\n\tsharing with 0x" + Long.toHexString(shareWith));
        }
        if (!EGL.eglMakeCurrent(((EGLDrawable)drawable).getDisplay(),
                                drawable.getHandle(),
                                drawableRead.getHandle(),
                                contextHandle)) {
            throw new GLException("Error making context 0x" +
                                  Long.toHexString(contextHandle) + " current: error code " + EGL.eglGetError());
        }
        setGLFunctionAvailability(true, glProfile.usesNativeGLES2()?2:1, 0, CTX_PROFILE_ES|CTX_OPTION_ANY);
        return true;
    }

    protected final void updateGLXProcAddressTable() {
        AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
        AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
        String key = adevice.getUniqueID();
        if (DEBUG) {
          System.err.println(getThreadName() + ": !!! Initializing EGLextension address table: "+key);
        }
        eglQueryStringInitialized = false;
        eglQueryStringAvailable = false;

        ProcAddressTable table = null;
        synchronized(mappedContextTypeObjectLock) {
            table = mappedGLXProcAddress.get( key );
        }
        if(null != table) {
            eglExtProcAddressTable = (EGLExtProcAddressTable) table;
            if(DEBUG) {
                System.err.println(getThreadName() + ": !!! GLContext EGL ProcAddressTable reusing key("+key+") -> "+table.hashCode());
            }
        } else {
            if (eglExtProcAddressTable == null) {
              // FIXME: cache ProcAddressTables by capability bits so we can
              // share them among contexts with the same capabilities
              eglExtProcAddressTable = new EGLExtProcAddressTable(new GLProcAddressResolver());
            }
            resetProcAddressTable(getEGLExtProcAddressTable());
            synchronized(mappedContextTypeObjectLock) {
                mappedGLXProcAddress.put(key, getEGLExtProcAddressTable());
                if(DEBUG) {
                    System.err.println(getThreadName() + ": !!! GLContext EGL ProcAddressTable mapping key("+key+") -> "+getEGLExtProcAddressTable().hashCode());
                }
            }
        }
    }
  
    public synchronized String getPlatformExtensionsString() {
        if (!eglQueryStringInitialized) {
          eglQueryStringAvailable =
            getDrawableImpl().getGLDynamicLookupHelper().dynamicLookupFunction("eglQueryString") != 0;
          eglQueryStringInitialized = true;
        }
        if (eglQueryStringAvailable) {
            String ret = EGL.eglQueryString(((EGLDrawable)drawable).getDisplay(), 
                                            EGL.EGL_EXTENSIONS);
            if (DEBUG) {
              System.err.println("!!! EGL extensions: " + ret);
            }
            return ret;
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

    protected void copyImpl(GLContext source, int mask) throws GLException {
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
