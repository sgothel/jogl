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

package com.sun.opengl.util;

import com.sun.opengl.impl.*;

/** Provides control over the primary display's gamma, brightness and
    contrast controls via the hardware gamma ramp tables. Not
    supported on all platforms or graphics hardware. <P>

    Thanks to the LWJGL project for illustrating how to access gamma
    control on the various platforms.
*/

public class Gamma {
  private Gamma() {}

  /**
   * Sets the gamma, brightness, and contrast of the current main
   * display. This functionality is not available on all platforms and
   * graphics hardware. Returns true if the settings were successfully
   * changed, false if not. This method may return false for some
   * values of the incoming arguments even on hardware which does
   * support the underlying functionality. <P>
   *
   * If this method returns true, the display settings will
   * automatically be reset to their original values upon JVM exit
   * (assuming the JVM does not crash); if the user wishes to change
   * the display settings back to normal ahead of time, use {@link
   * #resetDisplayGamma resetDisplayGamma}(). It is recommended to
   * call {@link #resetDisplayGamma resetDisplayGamma} before calling
   * e.g. <code>System.exit()</code> from the application rather than
   * rely on the shutdown hook functionality due to inevitable race
   * conditions and unspecified behavior during JVM teardown. <P>
   *
   * This method may be called multiple times during the application's
   * execution, but calling {@link #resetDisplayGamma
   * resetDisplayGamma} will only reset the settings to the values
   * before the first call to this method. <P>
   *
   * @param gamma The gamma value, typically > 1.0 (default values
   *   vary, but typically roughly 1.0)
   * @param brightness The brightness value between -1.0 and 1.0,
   *   inclusive (default values vary, but typically 0)
   * @param contrast The contrast, greater than 0.0 (default values
   *   vary, but typically 1)
   * @return true if gamma settings were successfully changed, false
   *   if not
   * @throws IllegalArgumentException if any of the parameters were
   *   out-of-bounds
   */
  public static boolean setDisplayGamma(float gamma, float brightness, float contrast) throws IllegalArgumentException {
    return GLDrawableFactoryImpl.getFactoryImpl().setDisplayGamma(gamma, brightness, contrast);
  }

  /**
   * Resets the gamma, brightness and contrast values for the primary
   * display to their original values before {@link #setDisplayGamma
   * setDisplayGamma} was called the first time. {@link
   * #setDisplayGamma setDisplayGamma} must be called before calling
   * this method or an unspecified exception will be thrown. While it
   * is not explicitly required that this method be called before
   * exiting, calling it is recommended because of the inevitable
   * unspecified behavior during JVM teardown.
   */
  public static void resetDisplayGamma() {
    GLDrawableFactoryImpl.getFactoryImpl().resetDisplayGamma();
  }
}
