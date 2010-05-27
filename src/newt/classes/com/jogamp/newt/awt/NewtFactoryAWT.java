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
import com.jogamp.newt.Display;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.NewtFactory;
import com.jogamp.common.util.ReflectionUtil;

public class AWTNewtFactory extends NewtFactory {

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
  public static NativeWindow getNativeWindow(Object awtCompObject, Capabilities capsRequested) {
      if(null==awtCompObject) {
        throw new NativeWindowException("Null AWT Component");
      }
      if( ! (awtCompObject instanceof java.awt.Component) ) {
        throw new NativeWindowException("AWT Component not a java.awt.Component");
      }
      return getNativeWindow( (java.awt.Component) awtCompObject, capsRequested );
  }

  public static NativeWindow getNativeWindow(java.awt.Component awtComp, Capabilities capsRequested) {
      DefaultGraphicsConfiguration config = 
          AWTGraphicsConfiguration.create(awtComp, (Capabilities) capsRequested.clone(), capsRequested);
      NativeWindow awtNative = NativeWindowFactory.getNativeWindow(awtComp, config); // a JAWTWindow
      return awtNative;
  }

  /**
   * Creates a native NEWT child window to a AWT parent window.<br>
   * <p>
   * First we create a {@link javax.media.nativewindow.NativeWindow} presentation of the given {@link java.awt.Component},
   * utilizing {@link #getNativeWindow(java.awt.Component)}.<br>
   * The actual wrapping implementation is {@link com.jogamp.nativewindow.impl.jawt.JAWTWindow}.<br></p>
   * <p>
   * Second we create a child {@link com.jogamp.newt.Window}, 
   * utilizing {@link com.jogamp.newt.NewtFactory#createWindowImpl(java.lang.String, javax.media.nativewindow.NativeWindow, com.jogamp.newt.Screen, javax.media.nativewindow.Capabilities, boolean)},
   * passing the created {@link javax.media.nativewindow.NativeWindow}.<br></p>

   * <p>
   * Third we attach a {@link com.jogamp.newt.event.awt.AWTParentWindowAdapter} to the given AWT component.<br>
   * The adapter passes window related events to our new child window, look at the implementation<br></p>
   *
   * <p>
   * Forth we pass the parents visibility to the new Window<br></p>
   *
   * @param awtParentObject must be of type java.awt.Component
   * @param undecorated only impacts if the window is in top-level state, while attached to a parent window it's rendered undecorated always
   * @return The successful created child window, or null if the AWT parent is not ready yet (no valid peers)
   */ 
  public static Window createNativeChildWindow(Object awtParentObject, Capabilities newtCaps, boolean undecorated) {
      if( null == awtParentObject ) {
        throw new NativeWindowException("Null AWT Parent Component");
      }
      if( ! (awtParentObject instanceof java.awt.Component) ) {
        throw new NativeWindowException("AWT Parent Component not a java.awt.Component");
      }
      java.awt.Component awtParent = (java.awt.Component) awtParentObject;

      // Generate a complete JAWT NativeWindow from the AWT Component
      NativeWindow parent = getNativeWindow(awtParent, newtCaps);
      if(null==parent) {
        throw new NativeWindowException("Null NativeWindow from parent: "+awtParent);
      }

      // Get parent's NativeWindow details
      AWTGraphicsConfiguration parentConfig = (AWTGraphicsConfiguration) parent.getGraphicsConfiguration();
      AWTGraphicsScreen parentScreen = (AWTGraphicsScreen) parentConfig.getScreen();
      AWTGraphicsDevice parentDevice = (AWTGraphicsDevice) parentScreen.getDevice();

      // Prep NEWT's Display and Screen according to the parent
      final String type = NativeWindowFactory.getNativeWindowType(true);
      Display display = NewtFactory.wrapDisplay(type, parentDevice.getHandle());
      Screen screen  = NewtFactory.createScreen(type, display, parentScreen.getIndex());

      // NEWT Window creation and add event handler for proper propagation AWT -> NEWT
      // and copy size/visible state
      Window window = NewtFactory.createWindowImpl(type, parent, screen, newtCaps, undecorated);
      new AWTParentWindowAdapter(window).addTo(awtParent);
      window.setSize(awtParent.getWidth(), awtParent.getHeight());
      window.setVisible(awtParent.isVisible());

      return window;
  }
}

