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
import com.jogamp.newt.util.EDTUtil;
import com.jogamp.newt.impl.Debug;
import com.jogamp.common.util.ReflectionUtil;

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
      if(DEBUG_IMPLEMENTATION) {
        System.out.println("NewtFactoryAWT.getNativeWindow: "+awtComp+" -> "+awtNative);
      }
      return awtNative;
  }

  public static Screen createCompatibleScreen(NativeWindow parent) {
    return createCompatibleScreen(parent, null);
  }

  public static Screen createCompatibleScreen(NativeWindow parent, Screen childScreen) {
      // Get parent's NativeWindow details
      AWTGraphicsConfiguration parentConfig = (AWTGraphicsConfiguration) parent.getGraphicsConfiguration();
      AWTGraphicsScreen parentScreen = (AWTGraphicsScreen) parentConfig.getScreen();
      AWTGraphicsDevice parentDevice = (AWTGraphicsDevice) parentScreen.getDevice();

      final String type = NativeWindowFactory.getNativeWindowType(true);

      if(null != childScreen) {
        // check if child Display/Screen is compatible already
        Display childDisplay = childScreen.getDisplay();
        String parentDisplayName = childDisplay.validateDisplayName(null, parentDevice.getHandle());
        String childDisplayName = childDisplay.getName();
        boolean displayEqual = parentDisplayName.equals( childDisplayName );
        boolean screenEqual = parentScreen.getIndex() == childScreen.getIndex();
        if(DEBUG_IMPLEMENTATION) {
            System.out.println("NewtFactoryAWT.createCompatibleScreen: Display: "+
                parentDisplayName+" =? "+childDisplayName+" : "+displayEqual+"; Screen: "+
                parentScreen.getIndex()+" =? "+childScreen.getIndex()+" : "+screenEqual);
        }
        if( displayEqual && screenEqual ) {
            // match: display/screen
            return childScreen;
        }
      }

      // Prep NEWT's Display and Screen according to the parent
      Display display = NewtFactory.createDisplay(type, parentDevice.getHandle());
      return NewtFactory.createScreen(type, display, parentScreen.getIndex());
  }

  public static boolean isScreenCompatible(NativeWindow parent, Screen childScreen) {
      // Get parent's NativeWindow details
      AWTGraphicsConfiguration parentConfig = (AWTGraphicsConfiguration) parent.getGraphicsConfiguration();
      AWTGraphicsScreen parentScreen = (AWTGraphicsScreen) parentConfig.getScreen();
      AWTGraphicsDevice parentDevice = (AWTGraphicsDevice) parentScreen.getDevice();

      Display childDisplay = childScreen.getDisplay();
      String parentDisplayName = childDisplay.validateDisplayName(null, parentDevice.getHandle());
      String childDisplayName = childDisplay.getName();
      if( ! parentDisplayName.equals( childDisplayName ) ) {
        return false;
      }

      if( parentScreen.getIndex() != childScreen.getIndex() ) {
        return false;
      }
      return true;
  }
}

