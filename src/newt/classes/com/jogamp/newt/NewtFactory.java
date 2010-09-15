/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.jogamp.newt;

import com.jogamp.common.util.ReflectionUtil;
import javax.media.nativewindow.*;
import java.util.ArrayList;
import java.util.Iterator;
import com.jogamp.common.jvm.JVMUtil;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.impl.Debug;

public abstract class NewtFactory {
    public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");

    // Work-around for initialization order problems on Mac OS X
    // between native Newt and (apparently) Fmod
    static {
        JVMUtil.initSingleton();
        Window.init(NativeWindowFactory.getNativeWindowType(true));
    }

    static Class getCustomClass(String packageName, String classBaseName) {
        Class clazz = null;
        if(packageName!=null || classBaseName!=null) {
            String clazzName = packageName + "." + classBaseName ;
            try {
                clazz = Class.forName(clazzName);
            } catch (Throwable t) {}
        }
        return clazz;
    }

    private static boolean useEDT = true;

    /** 
     * Toggles the usage of an EventDispatchThread while creating a Display.<br>
     * The default is enabled.<br>
     * The EventDispatchThread is thread local to the Display instance.<br>
     */
    public static synchronized void setUseEDT(boolean onoff) {
        useEDT = onoff;
    }

    /** @see #setUseEDT(boolean) */
    public static boolean useEDT() { return useEDT; }

    /**
     * Create a Display entity, incl native creation
     */
    public static Display createDisplay(String name) {
      return Display.create(NativeWindowFactory.getNativeWindowType(true), name, 0);
    }

    /**
     * Create a Display entity using the given implementation type, incl native creation
     */
    public static Display createDisplay(String type, String name) {
      return Display.create(type, name, 0);
    }

    /**
     * Create a Screen entity, incl native creation
     */
    public static Screen createScreen(Display display, int index) {
      return Screen.create(NativeWindowFactory.getNativeWindowType(true), display, index);
    }

    /**
     * Create a Screen entity using the given implementation type, incl native creation
     */
    public static Screen createScreen(String type, Display display, int index) {
      return Screen.create(type, display, index);
    }

    /**
     * Create a top level Window entity, incl native creation
     */
    public static Window createWindow(Screen screen, Capabilities caps) {
        return createWindowImpl(NativeWindowFactory.getNativeWindowType(true), screen, caps, false);
    }

    /**
     * Create a top level Window entity, incl native creation
     */
    public static Window createWindow(Screen screen, Capabilities caps, boolean undecorated) {
        return createWindowImpl(NativeWindowFactory.getNativeWindowType(true), screen, caps, undecorated);
    }

    /**
     * Create a child Window entity attached to the given parent, incl native creation.<br>
     * The Screen and Display information is regenerated utilizing the parents information.<br>
     * <p>
     * In case <code>parentWindowObject</code> is a {@link com.jogamp.newt.Window} instance,<br>
     * the new window is added to it's list of children.<br>
     * This assures proper handling of visibility, creation and destruction.<br>
     * {@link com.jogamp.newt.event.WindowEvent#EVENT_WINDOW_RESIZED} is not propagated to the child window for layout<br>,
     * you have to add an appropriate {@link com.jogamp.newt.event.WindowListener} for this use case.<br>
     * The parents visibility is passed to the new Window<br></p>
     * <p>
     * In case <code>parentWindowObject</code> is a different {@link javax.media.nativewindow.NativeWindow} implementation,<br>
     * you have to handle all events appropriatly.<br></p>
     * <p>
     *
     * @param parentWindowObject either a NativeWindow instance
     * @param undecorated only impacts if the window is in top-level state, while attached to a parent window it's rendered undecorated always
     */
    public static Window createWindow(NativeWindow nParentWindow, Capabilities caps, boolean undecorated) {
        final String type = NativeWindowFactory.getNativeWindowType(true);
        if(null==nParentWindow) {
            return createWindowImpl(type, caps, undecorated);
        }

        Screen screen  = null;
        Window parentWindow = null;

        if ( nParentWindow instanceof Window ) {
            // use parent NEWT Windows Display/Screen
            parentWindow = (Window) nParentWindow ;
            screen = parentWindow.getScreen();
        } else {
            // create a Display/Screen compatible to the NativeWindow
            AbstractGraphicsConfiguration nParentConfig = nParentWindow.getGraphicsConfiguration();
            if(null!=nParentConfig) {
                AbstractGraphicsScreen nParentScreen = nParentConfig.getScreen();
                AbstractGraphicsDevice nParentDevice = nParentScreen.getDevice();
                Display display = NewtFactory.createDisplay(type, nParentDevice.getHandle());
                screen  = NewtFactory.createScreen(type, display, nParentScreen.getIndex());
            } else {
                Display display = NewtFactory.createDisplay(type, null); // local display
                screen  = NewtFactory.createScreen(type, display, 0); // screen 0
            }
            screen.setDestroyWhenUnused(true);
        }
        final Window win = createWindowImpl(type, nParentWindow, screen, caps, undecorated);

        win.setSize(nParentWindow.getWidth(), nParentWindow.getHeight());
        if ( null != parentWindow ) {
            parentWindow.getInnerWindow().addChild(win);
            win.setVisible(parentWindow.isVisible());
        }
        return win;
    }

    protected static Window createWindowImpl(String type, NativeWindow parentNativeWindow, Screen screen, Capabilities caps, boolean undecorated) {
        return Window.create(type, parentNativeWindow, 0, screen, caps, undecorated);
    }

    protected static Window createWindowImpl(String type, long parentWindowHandle, Screen screen, Capabilities caps, boolean undecorated) {
        return Window.create(type, null, parentWindowHandle, screen, caps, undecorated);
    }

    protected static Window createWindowImpl(String type, Screen screen, Capabilities caps, boolean undecorated) {
        return Window.create(type, null, 0, screen, caps, undecorated);
    }

    protected static Window createWindowImpl(String type, Capabilities caps, boolean undecorated) {
        Display display = NewtFactory.createDisplay(type, null); // local display
        Screen screen  = NewtFactory.createScreen(type, display, 0); // screen 0
        screen.setDestroyWhenUnused(true);
        return Window.create(type, null, 0, screen, caps, undecorated);
    }

    /**
     * Create a child Window entity attached to the given parent, incl native creation<br>
     *
     * @param parentWindowObject the native parent window handle
     * @param undecorated only impacts if the window is in top-level state, while attached to a parent window it's rendered undecorated always
     */
    public static Window createWindow(long parentWindowHandle, Screen screen, Capabilities caps, boolean undecorated) {
        return createWindowImpl(NativeWindowFactory.getNativeWindowType(true), parentWindowHandle, screen, caps, undecorated);
    }

    /**
     * Ability to try a Window type with a construnctor argument, if supported ..<p>
     * Currently only valid is <code> AWTWindow(Frame frame) </code>,
     * to support an external created AWT Frame, ie the browsers embedded frame.
     *
     * @param undecorated only impacts if the window is in top-level state, while attached to a parent window it's rendered undecorated always
     */
    public static Window createWindow(Object[] cstrArguments, Screen screen, Capabilities caps, boolean undecorated) {
        return Window.create(NativeWindowFactory.getNativeWindowType(true), cstrArguments, screen, caps, undecorated);
    }

    /**
     * Create a Window entity using the given implementation type, incl native creation
     *
     * @param undecorated only impacts if the window is in top-level state, while attached to a parent window it's rendered undecorated always
     */
    public static Window createWindow(String type, Screen screen, Capabilities caps, boolean undecorated) {
        return createWindowImpl(type, null, screen, caps, undecorated);
    }

    public static Window createWindow(String type, Object[] cstrArguments, Screen screen, Capabilities caps, boolean undecorated) {
        return Window.create(type, cstrArguments, screen, caps, undecorated);
    }

    /**
     * Instantiate a Display entity using the native handle.
     */
    public static Display createDisplay(String type, long handle) {
      return Display.create(type, null, handle);
    }

    private static final boolean instanceOf(Object obj, String clazzName) {
        Class clazz = obj.getClass();
        do {
            if(clazz.getName().equals(clazzName)) {
                return true;
            }
            clazz = clazz.getSuperclass();
        } while (clazz!=null);
        return false;
    }

    public static boolean isScreenCompatible(NativeWindow parent, Screen childScreen) {
      // Get parent's NativeWindow details
      AbstractGraphicsConfiguration parentConfig = (AbstractGraphicsConfiguration) parent.getGraphicsConfiguration();
      AbstractGraphicsScreen parentScreen = (AbstractGraphicsScreen) parentConfig.getScreen();
      AbstractGraphicsDevice parentDevice = (AbstractGraphicsDevice) parentScreen.getDevice();

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

    public static Screen createCompatibleScreen(NativeWindow parent) {
      return createCompatibleScreen(parent, null);
    }

    public static Screen createCompatibleScreen(NativeWindow parent, Screen childScreen) {
      // Get parent's NativeWindow details
      AbstractGraphicsConfiguration parentConfig = (AbstractGraphicsConfiguration) parent.getGraphicsConfiguration();
      AbstractGraphicsScreen parentScreen = (AbstractGraphicsScreen) parentConfig.getScreen();
      AbstractGraphicsDevice parentDevice = (AbstractGraphicsDevice) parentScreen.getDevice();

      if(null != childScreen) {
        // check if child Display/Screen is compatible already
        Display childDisplay = childScreen.getDisplay();
        String parentDisplayName = childDisplay.validateDisplayName(null, parentDevice.getHandle());
        String childDisplayName = childDisplay.getName();
        boolean displayEqual = parentDisplayName.equals( childDisplayName );
        boolean screenEqual = parentScreen.getIndex() == childScreen.getIndex();
        if(DEBUG_IMPLEMENTATION) {
            System.err.println("NewtFactory.createCompatibleScreen: Display: "+
                parentDisplayName+" =? "+childDisplayName+" : "+displayEqual+"; Screen: "+
                parentScreen.getIndex()+" =? "+childScreen.getIndex()+" : "+screenEqual);
        }
        if( displayEqual && screenEqual ) {
            // match: display/screen
            return childScreen;
        }
      }

      // Prep NEWT's Display and Screen according to the parent
      final String type = NativeWindowFactory.getNativeWindowType(true);
      Display display = NewtFactory.createDisplay(type, parentDevice.getHandle());
      return NewtFactory.createScreen(type, display, parentScreen.getIndex());
    }
}

