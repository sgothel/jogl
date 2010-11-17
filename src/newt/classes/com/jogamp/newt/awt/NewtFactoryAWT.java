/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 

package com.jogamp.newt.awt;


import javax.media.nativewindow.*;
import javax.media.nativewindow.awt.*;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.impl.Debug;

public class NewtFactoryAWT extends NewtFactory {
  public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");

  /**
   * Wraps an AWT component into a {@link javax.media.nativewindow.NativeWindow} utilizing the {@link javax.media.nativewindow.NativeWindowFactory},<br>
   * using a configuration agnostic dummy {@link javax.media.nativewindow.DefaultGraphicsConfiguration}.<br>
   * <p>
   * The actual wrapping implementation is {@link com.jogamp.nativewindow.impl.jawt.JAWTWindow}.<br></p>
   * <p>
   * Purpose of this wrapping is to access the AWT window handle,<br>
   * not to actually render into it.<br>
   * Hence the dummy configuration only.</p>
   *
   * @param awtCompObject must be of type java.awt.Component
   */
  public static NativeWindow getNativeWindow(Object awtCompObject, CapabilitiesImmutable capsRequested) {
      if(null==awtCompObject) {
        throw new NativeWindowException("Null AWT Component");
      }
      if( ! (awtCompObject instanceof java.awt.Component) ) {
        throw new NativeWindowException("AWT Component not a java.awt.Component");
      }
      return getNativeWindow( (java.awt.Component) awtCompObject, capsRequested );
  }

  public static NativeWindow getNativeWindow(java.awt.Component awtComp, CapabilitiesImmutable capsRequested) {
      DefaultGraphicsConfiguration config = AWTGraphicsConfiguration.create(awtComp, capsRequested, capsRequested);
      NativeWindow awtNative = NativeWindowFactory.getNativeWindow(awtComp, config); // a JAWTWindow
      if(DEBUG_IMPLEMENTATION) {
        System.err.println("NewtFactoryAWT.getNativeWindow: "+awtComp+" -> "+awtNative);
      }
      return awtNative;
  }
}

