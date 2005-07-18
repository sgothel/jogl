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

package net.java.games.jogl.impl.x11;

import java.awt.Component;
import java.security.*;
import java.util.*;
import net.java.games.gluegen.runtime.*; // for PROCADDRESS_VAR_PREFIX
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public abstract class X11GLContext extends GLContextImpl {
  protected X11GLDrawable drawable;
  protected long context;
  private boolean glXQueryExtensionsStringInitialized;
  private boolean glXQueryExtensionsStringAvailable;
  private static final Map/*<String, String>*/ functionNameMap;
  private boolean isGLX13;
  // Table that holds the addresses of the native C-language entry points for
  // OpenGL functions.
  private GLProcAddressTable glProcAddressTable;
  private static boolean haveResetGLXProcAddressTable;
  // Cache the most recent value of the "display" variable (which we
  // only guarantee to be valid in between makeCurrent / free pairs)
  // so that we can implement displayImpl() (which must be done when
  // the context is not current)
  protected long mostRecentDisplay;
  // There is currently a bug on Linux/AMD64 distributions in glXGetProcAddressARB
  protected static boolean isLinuxAMD64;

  static {
    functionNameMap = new HashMap();
    functionNameMap.put("glAllocateMemoryNV", "glXAllocateMemoryNV");
    functionNameMap.put("glFreeMemoryNV", "glXFreeMemoryNV");

    AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          String os   = System.getProperty("os.name").toLowerCase();
          String arch = System.getProperty("os.arch").toLowerCase();
          if (os.startsWith("linux") && arch.equals("amd64")) {
            isLinuxAMD64 = true;
          }
          return null;
        }
      });
  }

  public X11GLContext(X11GLDrawable drawable,
                      GLContext shareWith) {
    super(shareWith);
    this.drawable = drawable;
  }
  
  protected GL createGL()
  {
    return new X11GLImpl(this);
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
    X11GLContext other = (X11GLContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      share = other.getContext();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
    }
    context = GLX.glXCreateContext(drawable.getDisplay(), vis, share, onscreen);
    if (context == 0) {
      throw new GLException("Unable to create OpenGL context");
    }
    GLContextShareSet.contextCreated(this);
  }

  protected int makeCurrentImpl() throws GLException {
    boolean created = false;
    if (context == 0) {
      create();
      if (DEBUG) {
        System.err.println(getThreadName() + ": !!! Created GL context for " + getClass().getName());
      }
      created = true;
    }

    if (!GLX.glXMakeCurrent(drawable.getDisplay(), drawable.getDrawable(), context)) {
      throw new GLException("Error making context current");
    } else {
      mostRecentDisplay = drawable.getDisplay();
      if (DEBUG && VERBOSE) {
        System.err.println(getThreadName() + ": glXMakeCurrent(display " + toHexString(drawable.getDisplay()) +
                           ", drawable " + toHexString(drawable.getDrawable()) +
                           ", context " + toHexString(context) + ") succeeded");
      }
    }

    if (created) {
      resetGLFunctionAvailability();
      return CONTEXT_CURRENT_NEW;
    }
    return CONTEXT_CURRENT;
  }

  protected void releaseImpl() throws GLException {
    if (!GLX.glXMakeCurrent(drawable.getDisplay(), 0, 0)) {
      throw new GLException("Error freeing OpenGL context");
    }
  }

  protected void destroyImpl() throws GLException {
    lockAWT();
    if (context != 0) {
      GLX.glXDestroyContext(mostRecentDisplay, context);
      if (DEBUG) {
        System.err.println("!!! Destroyed OpenGL context " + context);
      }
      context = 0;
      mostRecentDisplay = 0;
      GLContextShareSet.contextDestroyed(this);
    }
    unlockAWT();
  }

  protected long dynamicLookupFunction(String glFuncName) {
    long res = 0;
    if (!isLinuxAMD64) {
      res = GLX.glXGetProcAddressARB(glFuncName);
    }
    if (res == 0) {
      // GLU routines aren't known to the OpenGL function lookup
      res = GLX.dlsym(glFuncName);
    }
    return res;
  }

  public boolean isCreated() {
    return (context != 0);
  }

  protected void resetGLFunctionAvailability() {
    super.resetGLFunctionAvailability();
    if (DEBUG) {
      System.err.println("!!! Initializing OpenGL extension address table");
    }
    resetProcAddressTable(getGLProcAddressTable());

    if (!haveResetGLXProcAddressTable) {
      resetProcAddressTable(GLX.getGLXProcAddressTable());
    }

    // Figure out whether we are running GLX version 1.3 or above and
    // therefore have pbuffer support
    if (drawable.getDisplay() == 0) {
      throw new GLException("Expected non-null DISPLAY for querying GLX version");
    }
    int[] major = new int[1];
    int[] minor = new int[1];
    if (!GLX.glXQueryVersion(drawable.getDisplay(), major, 0, minor, 0)) {
      throw new GLException("glXQueryVersion failed");
    }
    if (DEBUG) {
      System.err.println("!!! GLX version: major " + major[0] +
                         ", minor " + minor[0]);
    }

    // Work around bugs in ATI's Linux drivers where they report they
    // only implement GLX version 1.2 but actually do support pbuffers
    if (major[0] == 1 && minor[0] == 2) {
      GL gl = getGL();
      String str = gl.glGetString(GL.GL_VENDOR);
      if (str != null && str.indexOf("ATI") >= 0) {
        isGLX13 = true;
        return;
      }
    }

    isGLX13 = ((major[0] > 1) || (minor[0] > 2));
  }
  
  public GLProcAddressTable getGLProcAddressTable() {
    if (glProcAddressTable == null) {
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
      glProcAddressTable = new GLProcAddressTable();
    }          
    return glProcAddressTable;
  }
  
  public synchronized String getPlatformExtensionsString() {
    if (drawable.getDisplay() == 0) {
      throw new GLException("Context not current");
    }
    if (!glXQueryExtensionsStringInitialized) {
      glXQueryExtensionsStringAvailable = (dynamicLookupFunction("glXQueryExtensionsString") != 0);
      glXQueryExtensionsStringInitialized = true;
    }
    if (glXQueryExtensionsStringAvailable) {
      lockAWT();
      try {
        String ret = GLX.glXQueryExtensionsString(drawable.getDisplay(), GLX.DefaultScreen(drawable.getDisplay()));
        if (DEBUG) {
          System.err.println("!!! GLX extensions: " + ret);
        }
        return ret;
      } finally {
        unlockAWT();
      }
    } else {
      return "";
    }
  }

  protected boolean isFunctionAvailable(String glFunctionName)
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
  }
  
  public boolean isExtensionAvailable(String glExtensionName) {
    if (glExtensionName.equals("GL_ARB_pbuffer") ||
        glExtensionName.equals("GL_ARB_pixel_format")) {
      return isGLX13;
    }
    return super.isExtensionAvailable(glExtensionName);
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

  public boolean canCreatePbufferContext() {
    return false;
  }

  public GLDrawableImpl createPbufferDrawable(GLCapabilities capabilities,
                                              int initialWidth,
                                              int initialHeight) {
    throw new GLException("Not supported");
  }

  public void bindPbufferToTexture() {
    throw new GLException("Should not call this");
  }

  public void releasePbufferFromTexture() {
    throw new GLException("Should not call this");
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  protected long getContext() {
    return context;
  }

  // These synchronization primitives prevent the AWT from making
  // requests from the X server asynchronously to this code.
  protected void lockAWT() {
    X11GLContextFactory.lockAWT();
  }

  protected void unlockAWT() {
    X11GLContextFactory.unlockAWT();
  }
}
