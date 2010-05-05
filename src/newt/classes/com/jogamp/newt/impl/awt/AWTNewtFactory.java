/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
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
 * Neither the name Sven Gothel or the names of
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
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.newt.impl.awt;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Component;
import java.awt.Canvas;

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.awt.*;

import com.jogamp.newt.event.awt.AWTParentWindowAdapter;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.NewtFactory;

public class AWTNewtFactory {

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
  public static NativeWindow getNativeWindow(Object awtCompObject) {
      if(null==awtCompObject) {
        throw new NativeWindowException("Null AWT Component");
      }
      if( ! (awtCompObject instanceof java.awt.Component) ) {
        throw new NativeWindowException("AWT Component not a java.awt.Component");
      }
      java.awt.Component awtComp = (java.awt.Component) awtCompObject;
      DefaultGraphicsDevice dummyDevice = new DefaultGraphicsDevice("AWTNewtBridge");
      DefaultGraphicsScreen dummyScreen = new DefaultGraphicsScreen(dummyDevice, 0);
      Capabilities dummyCaps = new Capabilities();
      DefaultGraphicsConfiguration dummyConfig = new DefaultGraphicsConfiguration(dummyScreen, dummyCaps, dummyCaps);
      NativeWindow awtNative = NativeWindowFactory.getNativeWindow(awtComp, dummyConfig);
      return awtNative;
  }

  /**
   * Creates a native NEWT child window to a AWT parent window.<br>
   * <p>
   * First we create a {@link javax.media.nativewindow.NativeWindow} presentation of the given {@link java.awt.Component},
   * utilizing {@link #getNativeWindow(java.awt.Component)}.<br>
   * The actual wrapping implementation is {@link com.jogamp.nativewindow.impl.jawt.JAWTWindow}.<br></p>
   * <p>
   * Second we create a child {@link com.jogamp.newt.Window}, utilizing {@link com.jogamp.newt.NewtFactory#createWindow(long, com.jogamp.newt.Screen, com.jogamp.newt.Capabilities, boolean)}, passing the AWT parent's native window handle retrieved via {@link com.jogamp.nativewindow.impl.jawt.JAWTWindow#getWindowHandle()}.<br></p>
   * <p>
   * Third we attach a {@link com.jogamp.newt.event.awt.AWTParentWindowAdapter} to the given AWT component.<br>
   * The adapter passes window related events to our new child window, look at the implementation<br></p>
   *
   * @param awtParentObject must be of type java.awt.Component
   * @param undecorated only impacts if the window is in top-level state, while attached to a parent window it's rendered undecorated always
   */ 
  public static Window createNativeChildWindow(Object awtParentObject, Screen newtScreen, Capabilities newtCaps, boolean undecorated) {
      NativeWindow parent = getNativeWindow(awtParentObject); // also checks java.awt.Component type
      java.awt.Component awtParent = (java.awt.Component) awtParentObject;
      if(null==parent) {
        throw new NativeWindowException("Null NativeWindow from parent: "+awtParent);
      }
      parent.lockSurface();
      long windowHandle = parent.getWindowHandle();
      parent.unlockSurface();
      if(0==windowHandle) {
        throw new NativeWindowException("Null window handle: "+parent);
      }
      Window window = NewtFactory.createWindow(windowHandle, newtScreen, newtCaps, undecorated);
      new AWTParentWindowAdapter(window).addTo(awtParent);
      return window;
  }
}

