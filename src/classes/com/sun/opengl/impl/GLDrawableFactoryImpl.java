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
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import com.sun.gluegen.runtime.*;
import javax.media.opengl.*;

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
}
