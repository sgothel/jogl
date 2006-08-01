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

package com.sun.opengl.impl;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.nio.*;
import javax.media.opengl.*;
import com.sun.gluegen.runtime.*;

/** Extends GLDrawableFactory with a few methods for handling
    typically software-accelerated offscreen rendering (Device
    Independent Bitmaps on Windows, pixmaps on X11). Direct access to
    these GLDrawables is not supplied directly to end users, though
    they may be instantiated by the GLJPanel implementation. */
public abstract class GLDrawableFactoryImpl extends GLDrawableFactory implements DynamicLookupHelper {
  /** Creates a (typically software-accelerated) offscreen GLDrawable
      used to implement the fallback rendering path of the
      GLJPanel. */
  public abstract GLDrawableImpl createOffscreenDrawable(GLCapabilities capabilities,
                                                         GLCapabilitiesChooser chooser);

  /** Dynamically looks up the given function. */
  public abstract long dynamicLookupFunction(String glFuncName);

  /** Locks the AWT for the purposes of Java2D/JOGL integration. This
   * is not necessary on some platforms.
   */
  public abstract void lockAWTForJava2D();

  /** Unlocks the AWT for the purposes of Java2D/JOGL integration.
   * This is not necessary on some platforms.
   */
  public abstract void unlockAWTForJava2D();

  public static GLDrawableFactoryImpl getFactoryImpl() {
    return (GLDrawableFactoryImpl) getFactory();
  }

  // Helper function for more lazily loading the GLU library;
  // apparently can't use System.loadLibrary on UNIX because it uses
  // RTLD_LOCAL and we need to call dlsym(RTLD_DEFAULT)
  public abstract void loadGLULibrary();

  //---------------------------------------------------------------------------
  // Support for Java2D/JOGL bridge on Mac OS X; the external
  // GLDrawable mechanism in the public API is sufficienit to
  // implement this functionality on all other platforms
  //

  public abstract boolean canCreateContextOnJava2DSurface();

  public abstract GLContext createContextOnJava2DSurface(Graphics g, GLContext shareWith)
    throws GLException;

  //----------------------------------------------------------------------
  // Gamma adjustment support
  // Thanks to the LWJGL team for illustrating how to make these
  // adjustments on various OSs.

  /*
   * Portions Copyright (c) 2002-2004 LWJGL Project
   * All rights reserved.
   *
   * Redistribution and use in source and binary forms, with or without
   * modification, are permitted provided that the following conditions are
   * met:
   *
   * * Redistributions of source code must retain the above copyright
   *   notice, this list of conditions and the following disclaimer.
   *
   * * Redistributions in binary form must reproduce the above copyright
   *   notice, this list of conditions and the following disclaimer in the
   *   documentation and/or other materials provided with the distribution.
   *
   * * Neither the name of 'LWJGL' nor the names of
   *   its contributors may be used to endorse or promote products derived
   *   from this software without specific prior written permission.
   *
   * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
   * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
   * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
   * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
   */

  /**
   * Sets the gamma, brightness, and contrast of the current main
   * display. Returns true if the settings were changed, false if
   * not. If this method returns true, the display settings will
   * automatically be reset upon JVM exit (assuming the JVM does not
   * crash); if the user wishes to change the display settings back to
   * normal ahead of time, use resetDisplayGamma(). Throws
   * IllegalArgumentException if any of the parameters were
   * out-of-bounds.
   * 
   * @param gamma The gamma value, typically > 1.0 (default value is
   *   1.0)
   * @param brightness The brightness value between -1.0 and 1.0,
   *   inclusive (default value is 0)
   * @param contrast The contrast, greater than 0.0 (default value is 1)
   * @throws IllegalArgumentException if any of the parameters were
   *   out-of-bounds
   */
  public boolean setDisplayGamma(float gamma, float brightness, float contrast) throws IllegalArgumentException {
    if ((brightness < -1.0f) || (brightness > 1.0f)) {
      throw new IllegalArgumentException("Brightness must be between -1.0 and 1.0");
    }
    if (contrast < 0) {
      throw new IllegalArgumentException("Contrast must be greater than 0.0");
    }
    // FIXME: ensure gamma is > 1.0? Are smaller / negative values legal?
    int rampLength = getGammaRampLength();
    if (rampLength == 0) {
      return false;
    }
    float[] gammaRamp = new float[rampLength];
    for (int i = 0; i < rampLength; i++) {
      float intensity = (float) i / (float) (rampLength - 1);
      // apply gamma
      float rampEntry = (float) java.lang.Math.pow(intensity, gamma);
      // apply brightness
      rampEntry += brightness;
      // apply contrast
      rampEntry = (rampEntry - 0.5f) * contrast + 0.5f;
      // Clamp entry to [0, 1]
      if (rampEntry > 1.0f)
        rampEntry = 1.0f;
      else if (rampEntry < 0.0f)
        rampEntry = 0.0f;
      gammaRamp[i] = rampEntry;
    }
    registerGammaShutdownHook();
    return setGammaRamp(gammaRamp);
  }

  public synchronized void resetDisplayGamma() {
    if (gammaShutdownHook == null) {
      throw new IllegalArgumentException("Should not call this unless setDisplayGamma called first");
    }
    resetGammaRamp(originalGammaRamp);
    unregisterGammeShutdownHook();
  }

  //------------------------------------------------------
  // Gamma-related methods to be implemented by subclasses
  //

  /** Returns the length of the computed gamma ramp for this OS and
      hardware. Returns 0 if gamma changes are not supported. */
  protected int getGammaRampLength() {
    return 0;
  }

  /** Sets the gamma ramp for the main screen. Returns false if gamma
      ramp changes were not supported. */
  protected boolean setGammaRamp(float[] ramp) {
    return false;
  }

  /** Gets the current gamma ramp. This is basically an opaque value
      used only on some platforms to reset the gamma ramp to its
      original settings. */
  protected Buffer getGammaRamp() {
    return null;
  }

  /** Resets the gamma ramp, potentially using the specified Buffer as
      data to restore the original values. */
  protected void resetGammaRamp(Buffer originalGammaRamp) {
  }

  // Shutdown hook mechanism for resetting gamma
  private boolean gammaShutdownHookRegistered;
  private Thread  gammaShutdownHook;
  private Buffer  originalGammaRamp;
  private synchronized void registerGammaShutdownHook() {
    if (gammaShutdownHookRegistered)
      return;
    if (gammaShutdownHook == null) {
      gammaShutdownHook = new Thread(new Runnable() {
          public void run() {
            synchronized (GLDrawableFactoryImpl.this) {
              resetGammaRamp(originalGammaRamp);
            }
          }
        });
      originalGammaRamp = getGammaRamp();
    }
    Runtime.getRuntime().addShutdownHook(gammaShutdownHook);
    gammaShutdownHookRegistered = true;
  }

  private synchronized void unregisterGammeShutdownHook() {
    if (!gammaShutdownHookRegistered)
      return;
    if (gammaShutdownHook == null) {
      throw new InternalError("Error in gamma shutdown hook logic");
    }
    Runtime.getRuntime().removeShutdownHook(gammaShutdownHook);
    gammaShutdownHookRegistered = false;
    // Leave the original gamma ramp data alone
  }
}
