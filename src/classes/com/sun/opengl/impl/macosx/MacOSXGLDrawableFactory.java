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

package com.sun.opengl.impl.macosx;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.lang.reflect.InvocationTargetException;
import java.nio.*;
import java.util.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class MacOSXGLDrawableFactory extends GLDrawableFactoryImpl {
  static {
    NativeLibLoader.loadCore();
  }

  public AbstractGraphicsConfiguration chooseGraphicsConfiguration(GLCapabilities capabilities,
                                                                   GLCapabilitiesChooser chooser,
                                                                   AbstractGraphicsDevice device) {
    return null;
  }

  public GLDrawable getGLDrawable(Object target,
                                  GLCapabilities capabilities,
                                  GLCapabilitiesChooser chooser) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    if (!(target instanceof Component)) {
      throw new IllegalArgumentException("GLDrawables not supported for objects of type " +
                                         target.getClass().getName() + " (only Components are supported in this implementation)");
    }
    if (capabilities == null) {
      capabilities = new GLCapabilities();
    }
    if (chooser == null) {
      chooser = new DefaultGLCapabilitiesChooser();
    }
    return new MacOSXOnscreenGLDrawable((Component) target, capabilities, chooser);
  }

  public GLDrawableImpl createOffscreenDrawable(GLCapabilities capabilities,
                                                GLCapabilitiesChooser chooser) {
    return new MacOSXOffscreenGLDrawable(capabilities);
  }

  public boolean canCreateGLPbuffer() {
    return true;
  }

  public GLPbuffer createGLPbuffer(final GLCapabilities capabilities,
                                   final GLCapabilitiesChooser chooser,
                                   final int initialWidth,
                                   final int initialHeight,
                                   final GLContext shareWith) {
    final List returnList = new ArrayList();
    Runnable r = new Runnable() {
        public void run() {
          MacOSXPbufferGLDrawable pbufferDrawable = new MacOSXPbufferGLDrawable(capabilities,
										initialWidth,
										initialHeight);
          GLPbufferImpl pbuffer = new GLPbufferImpl(pbufferDrawable, shareWith);
          returnList.add(pbuffer);
        }
      };
    maybeDoSingleThreadedWorkaround(r);
    return (GLPbuffer) returnList.get(0);
  }

  public GLContext createExternalGLContext() {
    return new MacOSXExternalGLContext();
  }

  public boolean canCreateExternalGLDrawable() {
    return false;
  }

  public GLDrawable createExternalGLDrawable() {
    // FIXME
    throw new GLException("Not yet implemented");
  }

  public void loadGLULibrary() {
    // Nothing to do; already loaded by native code; not much point in
    // making it lazier on this platform
  }

  public long dynamicLookupFunction(String glFuncName) {
    return CGL.getProcAddress(glFuncName);
  }

  private void maybeDoSingleThreadedWorkaround(Runnable action) {
    if (Threading.isSingleThreaded() &&
        !Threading.isOpenGLThread()) {
      Threading.invokeOnOpenGLThread(action);
    } else {
      action.run();
    }
  }

  public void lockAWTForJava2D() {
  }

  public void unlockAWTForJava2D() {
  }

  public boolean canCreateContextOnJava2DSurface() {
    return true;
  }

  public GLContext createContextOnJava2DSurface(Graphics g, GLContext shareWith)
    throws GLException {
    return new MacOSXJava2DGLContext(shareWith);
  }
  

  //------------------------------------------------------
  // Gamma-related functionality
  //

  private static final int GAMMA_RAMP_LENGTH = 256;

  /** Returns the length of the computed gamma ramp for this OS and
      hardware. Returns 0 if gamma changes are not supported. */
  protected int getGammaRampLength() {
    return GAMMA_RAMP_LENGTH;
  }

  protected boolean setGammaRamp(float[] ramp) {
    return CGL.setGammaRamp(ramp.length,
                            ramp, 0,
                            ramp, 0,
                            ramp, 0);
  }

  protected Buffer getGammaRamp() {
    return null;
  }

  protected void resetGammaRamp(Buffer originalGammaRamp) {
    CGL.resetGammaRamp();
  }
}
