/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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

package com.jogamp.opengl.util;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;

import com.jogamp.common.util.locks.RecursiveLock;

/**
 * Provides convenient wrapper for {@link GLDrawableFactory} control over
 * individual display's gamma, brightness and contrast values
 * via the hardware gamma ramp tables.
 * <p>
 * Not supported on all platforms or graphics hardware.
 * </p>
 * <p>
 * Thanks to the LWJGL project for illustrating how to access gamma
 * control on the various platforms.
 * </p>
 */
public class Gamma {
  private Gamma() {}

  /**
   * Convenient wrapper for {@link GLDrawableFactory#setDisplayGamma(com.jogamp.nativewindow.NativeSurface, float, float, float)}.
   * <p>
   * Use {@link #setDisplayGamma(GLAutoDrawable, float, float, float)} in case of using an {#link GLAutoDrawable}.
   * </p>
   */
  public static boolean setDisplayGamma(final GLDrawable drawable, final float gamma, final float brightness, final float contrast) throws IllegalArgumentException {
    return GLDrawableFactory.getFactory(drawable.getGLProfile()).setDisplayGamma(drawable.getNativeSurface(), gamma, brightness, contrast);
  }

  /**
   * Convenient wrapper for {@link GLDrawableFactory#setDisplayGamma(com.jogamp.nativewindow.NativeSurface, float, float, float)}
   * locking {@link GLAutoDrawable#getUpstreamLock()} to ensure proper atomic operation.
   */
  public static boolean setDisplayGamma(final GLAutoDrawable drawable, final float gamma, final float brightness, final float contrast) throws IllegalArgumentException {
    final RecursiveLock lock = drawable.getUpstreamLock();
    lock.lock();
    try {
        return GLDrawableFactory.getFactory(drawable.getGLProfile()).setDisplayGamma(drawable.getNativeSurface(), gamma, brightness, contrast);
    } finally {
        lock.unlock();
    }
  }

  /**
   * Convenient wrapper for {@link GLDrawableFactory#resetDisplayGamma(com.jogamp.nativewindow.NativeSurface)}.
   * <p>
   * Use {@link #resetDisplayGamma(GLAutoDrawable)} in case of using an {#link GLAutoDrawable}.
   * </p>
   */
  public static void resetDisplayGamma(final GLDrawable drawable) {
    GLDrawableFactory.getFactory(drawable.getGLProfile()).resetDisplayGamma(drawable.getNativeSurface());
  }

  /**
   * Convenient wrapper for {@link GLDrawableFactory#resetDisplayGamma(com.jogamp.nativewindow.NativeSurface)}
   * locking {@link GLAutoDrawable#getUpstreamLock()} to ensure proper atomic operation.
   */
  public static void resetDisplayGamma(final GLAutoDrawable drawable) {
    final RecursiveLock lock = drawable.getUpstreamLock();
    lock.lock();
    try {
        GLDrawableFactory.getFactory(drawable.getGLProfile()).resetDisplayGamma(drawable.getNativeSurface());
    } finally {
        lock.unlock();
    }
  }

  /**
   * Convenient wrapper for {@link GLDrawableFactory#resetAllDisplayGamma()}.
   */
  public static void resetAllDisplayGamma(final GLDrawable drawable) {
    GLDrawableFactory.getFactory(drawable.getGLProfile()).resetAllDisplayGamma();
  }
}
