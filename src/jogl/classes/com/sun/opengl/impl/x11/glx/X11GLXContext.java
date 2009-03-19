/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.impl.x11.glx;

import java.nio.*;
import java.util.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.opengl.impl.x11.glx.*;
import com.sun.nativewindow.impl.x11.*;
import com.sun.gluegen.runtime.ProcAddressTable;

public abstract class X11GLXContext extends GLContextImpl {
  protected X11GLXDrawable drawable;
  protected long context;
  private boolean glXQueryExtensionsStringInitialized;
  private boolean glXQueryExtensionsStringAvailable;
  private static final Map/*<String, String>*/ functionNameMap;
  private GLXExt glXExt;
  // Table that holds the addresses of the native C-language entry points for
  // GLX extension functions.
  private GLXExtProcAddressTable glXExtProcAddressTable;

  static {
    functionNameMap = new HashMap();
    functionNameMap.put("glAllocateMemoryNV", "glXAllocateMemoryNV");
    functionNameMap.put("glFreeMemoryNV", "glXFreeMemoryNV");
  }

  public X11GLXContext(X11GLXDrawable drawable,
                      GLContext shareWith) {
    super(shareWith);
    this.drawable = drawable;
  }
  
  public final ProcAddressTable getPlatformExtProcAddressTable() {
    return getGLXExtProcAddressTable();
  }

  public final GLXExtProcAddressTable getGLXExtProcAddressTable() {
    return glXExtProcAddressTable;
  }

  public Object getPlatformGLExtensions() {
    return getGLXExt();
  }

  public GLXExt getGLXExt() {
    if (glXExt == null) {
      glXExt = new GLXExtImpl(this);
    }
    return glXExt;
  }

  public GLDrawable getGLDrawable() {
    return drawable;
  }

  protected String mapToRealGLFunctionName(String glFunctionName) {
    String lookup = (String) functionNameMap.get(glFunctionName);
    if (lookup != null) {
      return lookup;
    }
    return glFunctionName;
  }

  protected String mapToRealGLExtensionName(String glExtensionName) {
    return glExtensionName;
  }

  /** Helper routine which usually just turns around and calls
   * createContext (except for pbuffers, which use a different context
   * creation mechanism). Should only be called by {@link
   * makeCurrentImpl()}.
   */
  protected abstract void create();

  /**
   * Creates and initializes an appropriate OpenGL context. Should only be
   * called by {@link create()}.
   */
  protected void createContext(boolean onscreen) {
    XVisualInfo vis = drawable.chooseVisual(onscreen);
    X11GLXContext other = (X11GLXContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      share = other.getContext();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
    }
    context = GLX.glXCreateContext(drawable.getNativeWindow().getDisplayHandle(), vis, share, onscreen);
    if (context == 0) {
      throw new GLException("Unable to create OpenGL context");
    }
    GLContextShareSet.contextCreated(this);
  }

  protected int makeCurrentImpl() throws GLException {
    if (drawable.getNativeWindow().getSurfaceHandle() == 0) {
        if (DEBUG) {
          System.err.println("drawable not properly initialized");
        }
        return CONTEXT_NOT_CURRENT;
    }
    boolean created = false;
    if (context == 0) {
      create();
      if (DEBUG) {
        System.err.println(getThreadName() + ": !!! Created GL context for " + getClass().getName());
      }
      created = true;
    }

    if (GLX.glXGetCurrentContext() != context) {
        if (!GLX.glXMakeContextCurrent(drawable.getNativeWindow().getDisplayHandle(), 
                                       drawable.getNativeWindow().getSurfaceHandle(), 
                                       drawable.getNativeWindow().getSurfaceHandle(), 
                                       context)) {
          throw new GLException("Error making context current");
        } else {
          if (DEBUG && (VERBOSE || created)) {
            System.err.println(getThreadName() + ": glXMakeCurrent(display " + 
                               toHexString(drawable.getNativeWindow().getDisplayHandle()) +
                               ", drawable " + toHexString(drawable.getNativeWindow().getSurfaceHandle()) +
                               ", context " + toHexString(context) + ") succeeded");
          }
        }
    }

    if (created) {
      resetGLFunctionAvailability();
      return CONTEXT_CURRENT_NEW;
    }
    return CONTEXT_CURRENT;
  }

  protected void releaseImpl() throws GLException {
    getDrawableImpl().getFactoryImpl().lockToolkit();
    try {
      if (!GLX.glXMakeContextCurrent(drawable.getNativeWindow().getDisplayHandle(), 0, 0, 0)) {
        throw new GLException("Error freeing OpenGL context");
      }
    } finally {
      getDrawableImpl().getFactoryImpl().unlockToolkit();
    }
  }

  protected void destroyImpl() throws GLException {
    getDrawableImpl().getFactoryImpl().lockToolkit();
    try {
      if (context != 0) {
        if (DEBUG) {
          System.err.println("glXDestroyContext(0x" +
                             Long.toHexString(drawable.getNativeWindow().getDisplayHandle()) +
                             ", 0x" +
                             Long.toHexString(context) + ")");
        }
        GLX.glXDestroyContext(drawable.getNativeWindow().getDisplayHandle(), context);
        if (DEBUG) {
          System.err.println("!!! Destroyed OpenGL context " + context);
        }
        context = 0;
        GLContextShareSet.contextDestroyed(this);
      }
    } finally {
      getDrawableImpl().getFactoryImpl().unlockToolkit();
    }
  }

  public boolean isCreated() {
    return (context != 0);
  }

  public void copy(GLContext source, int mask) throws GLException {
    long dst = getContext();
    long src = ((X11GLXContext) source).getContext();
    if (src == 0) {
      throw new GLException("Source OpenGL context has not been created");
    }
    if (dst == 0) {
      throw new GLException("Destination OpenGL context has not been created");
    }
    if (drawable.getNativeWindow().getDisplayHandle() == 0) {
      throw new GLException("Connection to X display not yet set up");
    }
    getDrawableImpl().getFactoryImpl().lockToolkit();
    try {
      GLX.glXCopyContext(drawable.getNativeWindow().getDisplayHandle(), src, dst, mask);
      // Should check for X errors and raise GLException
    } finally {
      getDrawableImpl().getFactoryImpl().unlockToolkit();
    }
  }

  protected void resetGLFunctionAvailability() {
    super.resetGLFunctionAvailability();
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Initializing GLX extension address table");
    }
    if (glXExtProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      glXExtProcAddressTable = new GLXExtProcAddressTable();
    }          
    resetProcAddressTable(getGLXExtProcAddressTable());
  }
  
  public synchronized String getPlatformExtensionsString() {
    if (!glXQueryExtensionsStringInitialized) {
      glXQueryExtensionsStringAvailable =
        getDrawableImpl().getFactoryImpl().dynamicLookupFunction("glXQueryExtensionsString") != 0;
      glXQueryExtensionsStringInitialized = true;
    }
    if (glXQueryExtensionsStringAvailable) {
      GLDrawableFactoryImpl factory = getDrawableImpl().getFactoryImpl();
      factory.lockToolkit();
      try {
        String ret = GLX.glXQueryExtensionsString(drawable.getNativeWindow().getDisplayHandle(), 
                                                  drawable.getNativeWindow().getScreenIndex());
        if (DEBUG) {
          System.err.println("!!! GLX extensions: " + ret);
        }
        return ret;
      } finally {
        factory.unlockToolkit();
      }
    } else {
      return "";
    }
  }

  /**
   * using dynamic ProcAddressTable verification always
   *
  public boolean isFunctionAvailable(String glFunctionName)
  {
    boolean available = super.isFunctionAvailable(glFunctionName);
    
    // Sanity check for implementations that use proc addresses for run-time
    // linking: if the function IS available, then make sure there's a proc
    // address for it if it's an extension or not part of the OpenGL 1.1 core
    // (post GL 1.1 functions are run-time linked on windows).
    assert(!available ||
           (getGLProcAddressTable().getAddressFor(mapToRealGLFunctionName(glFunctionName)) != 0 ||
            FunctionAvailabilityCache.isPartOfGLCore("1.1", mapToRealGLFunctionName(glFunctionName)))
           );

    return available;
  }*/
  
  public boolean isExtensionAvailable(String glExtensionName) {
    if (glExtensionName.equals("GL_ARB_pbuffer") ||
        glExtensionName.equals("GL_ARB_pixel_format")) {
      return getGLDrawable().getFactory().canCreateGLPbuffer();
    }
    return super.isExtensionAvailable(glExtensionName);
  }


  public void setSwapInterval(int interval) {
    getDrawableImpl().getFactoryImpl().lockToolkit();
    try {
      // FIXME: make the context current first? Currently assumes that
      // will not be necessary. Make the caller do this?
      GLXExt glXExt = getGLXExt();
      if (glXExt.isExtensionAvailable("GLX_SGI_swap_control")) {
        glXExt.glXSwapIntervalSGI(interval);
      }
    } finally {
      getDrawableImpl().getFactoryImpl().unlockToolkit();
    }
  }

  public ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
    return getGLXExt().glXAllocateMemoryNV(arg0, arg1, arg2, arg3);
  }

  public int getOffscreenContextPixelDataType() {
    throw new GLException("Should not call this");
  }

  public int getOffscreenContextReadBuffer() {
    throw new GLException("Should not call this");
  }

  public boolean offscreenImageNeedsVerticalFlip() {
    throw new GLException("Should not call this");
  }

  public void bindPbufferToTexture() {
    throw new GLException("Should not call this");
  }

  public void releasePbufferFromTexture() {
    throw new GLException("Should not call this");
  }

  public boolean isOptimizable() {
    return (super.isOptimizable() &&
            !GLXUtil.isVendorATI());
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  public long getContext() {
    return context;
  }

}
