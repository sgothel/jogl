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

import java.nio.ByteBuffer;
import java.util.Map;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;

import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;

public abstract class EGLContext extends GLContextImpl {
    private boolean eglQueryStringInitialized;
    private boolean eglQueryStringAvailable;
    private EGLExt _eglExt;
    // Table that holds the addresses of the native C-language entry points for
    // EGL extension functions.
    private EGLExtProcAddressTable eglExtProcAddressTable;

    EGLContext(GLDrawableImpl drawable,
               GLContext shareWith) {
        super(drawable, shareWith);
    }

    @Override
    protected void resetStates() {
        eglQueryStringInitialized = false;
        eglQueryStringAvailable = false;
        // no inner state _eglExt = null;
        super.resetStates();
    }
    
    public Object getPlatformGLExtensions() {
      return getEGLExt();
    }

    public EGLExt getEGLExt() {
      if (_eglExt == null) {
        _eglExt = new EGLExtImpl(this);
      }
      return _eglExt;
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

    protected void makeCurrentImpl() throws GLException {
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

    protected boolean createImpl(GLContextImpl shareWith) throws GLException {
        long eglDisplay = ((EGLDrawable)drawable).getDisplay();
        EGLGraphicsConfiguration config = ((EGLDrawable)drawable).getGraphicsConfiguration();
        GLProfile glProfile = drawable.getGLProfile();
        long eglConfig = config.getNativeConfig();
        long shareWithHandle = EGL.EGL_NO_CONTEXT;

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

        if (shareWith != null) {
            shareWithHandle = shareWith.getHandle();
            if (shareWithHandle == 0) {
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
        contextHandle = EGL.eglCreateContext(eglDisplay, eglConfig, shareWithHandle, contextAttrs, 0);
        if (contextHandle == 0) {
            throw new GLException("Error creating OpenGL context: eglDisplay "+toHexString(eglDisplay)+
                                  ", eglConfig "+config+", "+glProfile+", shareWith "+toHexString(shareWithHandle)+", error "+toHexString(EGL.eglGetError()));
        }
        if (DEBUG) {
            System.err.println(getThreadName() + ": !!! Created OpenGL context 0x" +
                               Long.toHexString(contextHandle) + 
                               ",\n\twrite surface 0x" + Long.toHexString(drawable.getHandle()) +
                               ",\n\tread  surface 0x" + Long.toHexString(drawableRead.getHandle())+
                               ",\n\t"+this+
                               ",\n\tsharing with 0x" + Long.toHexString(shareWithHandle));
        }
        if (!EGL.eglMakeCurrent(((EGLDrawable)drawable).getDisplay(),
                                drawable.getHandle(),
                                drawableRead.getHandle(),
                                contextHandle)) {
            throw new GLException("Error making context 0x" +
                                  Long.toHexString(contextHandle) + " current: error code " + EGL.eglGetError());
        }
        int ctp = CTX_PROFILE_ES|CTX_OPTION_ANY;
        int major;
        if(glProfile.usesNativeGLES2()) {
            ctp |= CTX_PROFILE_ES2_COMPAT;
            major = 2;
        } else {            
            major = 1;
        }
        setGLFunctionAvailability(true, major, 0, ctp);
        return true;
    }

    protected final void updateGLXProcAddressTable() {
        final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration();
        final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
        final String key = "EGL-"+adevice.getUniqueID();
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
  
    protected final StringBuffer getPlatformExtensionsStringImpl() {
        StringBuffer sb = new StringBuffer();        
        if (!eglQueryStringInitialized) {
          eglQueryStringAvailable =
            getDrawableImpl().getGLDynamicLookupHelper().dynamicLookupFunction("eglQueryString") != 0;
          eglQueryStringInitialized = true;
        }
        if (eglQueryStringAvailable) {
            final String ret = EGL.eglQueryString(((EGLDrawable)drawable).getDisplay(), 
                                                  EGL.EGL_EXTENSIONS);
            if (DEBUG) {
              System.err.println("!!! EGL extensions: " + ret);
            }
            sb.append(ret);
        }
        return sb;
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
