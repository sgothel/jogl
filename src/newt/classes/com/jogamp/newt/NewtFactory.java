/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.NativeWindowFactory;

import jogamp.newt.Debug;
import jogamp.newt.DisplayImpl;
import jogamp.newt.ScreenImpl;
import jogamp.newt.WindowImpl;

import com.jogamp.common.os.Platform;

public class NewtFactory {
    public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");

    // Work-around for initialization order problems on Mac OS X
    // between native Newt and (apparently) Fmod
    static {
        NativeWindowFactory.initSingleton(false); // last resort ..
        WindowImpl.init(NativeWindowFactory.getNativeWindowType(true));
    }

    public static Class<?> getCustomClass(String packageName, String classBaseName) {
        Class<?> clazz = null;
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
     * Create a Display entity.
     * <p>
     * Native creation is lazily done at usage, ie. {@link Display#addReference()}.
     * </p>
     * <p>
     * An already existing display connection of the same <code>name</code> will be reused.  
     * </p>
     * @param name the display connection name which is a technical platform specific detail,
     *        see {@link AbstractGraphicsDevice#getConnection()}. Use <code>null</code> for default.
     * @return the new or reused Display instance
     */
    public static Display createDisplay(String name) {
        return createDisplay(name, true);
    }

    /**
     * Create a Display entity.
     * <p>
     * Native creation is lazily done at usage, ie. {@link Display#addReference()}.
     * </p>
     * <p>
     * An already existing display connection of the same <code>name</code> will be reused
     * <b>if</b> <code>reuse</code> is <code>true</code>, otherwise a new instance is being created.
     * </p>
     * @param name the display connection name which is a technical platform specific detail,
     *        see {@link AbstractGraphicsDevice#getConnection()}. Use <code>null</code> for default.
     * @param reuse attempt to reuse an existing Display with same <code>name</code> if set true, otherwise create a new instance.
     * @return the new or reused Display instance
     */
    public static Display createDisplay(String name, boolean reuse) {
      return DisplayImpl.create(NativeWindowFactory.getNativeWindowType(true), name, 0, reuse);
    }

    /**
     * Create a Display entity.
     * <p>
     * Native creation is lazily done at usage, ie. {@link Display#addReference()}.
     * </p>
     * <p>
     * An already existing display connection of the same <code>name</code> will be reused.
     * </p>
     * @param type explicit NativeWindow type eg. {@link NativeWindowFactory#TYPE_AWT}
     * @param name the display connection name which is a technical platform specific detail,
     *        see {@link AbstractGraphicsDevice#getConnection()}. Use <code>null</code> for default.
     * @return the new or reused Display instance
     */
    public static Display createDisplay(String type, String name) {
        return createDisplay(type, name, true);
    }

    /**
     * Create a Display entity.
     * <p>
     * Native creation is lazily done at usage, ie. {@link Display#addReference()}.
     * </p>
     * <p>
     * An already existing display connection of the same <code>name</code> will be reused
     * <b>if</b> <code>reuse</code> is <code>true</code>, otherwise a new instance is being created.
     * </p>
     * @param type explicit NativeWindow type eg. {@link NativeWindowFactory#TYPE_AWT}
     * @param name the display connection name which is a technical platform specific detail,
     *        see {@link AbstractGraphicsDevice#getConnection()}. Use <code>null</code> for default.
     * @param reuse attempt to reuse an existing Display with same <code>name</code> if set true, otherwise create a new instance.
     * @return the new or reused Display instance
     */
    public static Display createDisplay(String type, String name, boolean reuse) {
      return DisplayImpl.create(type, name, 0, reuse);
    }

    /**
     * Create a Screen entity.
     * <p>
     * Native creation is lazily done at usage, ie. {@link Screen#addReference()}.
     * </p>
     * <p>
     * The lifecycle of this Screen's Display is handled via {@link Display#addReference()}
     * and {@link Display#removeReference()}.
     * </p>
     */
    public static Screen createScreen(Display display, int index) {
      return ScreenImpl.create(display, index);
    }

    /**
     * Create a top level Window entity on the default Display and default Screen.
     * <p>
     * Native creation is lazily done at usage, ie. {@link Window#setVisible(boolean)}.
     * </p>
     * <p>
     * An already existing default Display will be reused.
     * </p>
     * <p>
     * The lifecycle of this Window's Screen and Display is handled via {@link Screen#addReference()}
     * and {@link Screen#removeReference()}.
     * </p>
     */
    public static Window createWindow(CapabilitiesImmutable caps) {
        return createWindowImpl(NativeWindowFactory.getNativeWindowType(true), caps);
    }

    /**
     * Create a top level Window entity.
     * <p>
     * Native creation is lazily done at usage, ie. {@link Window#setVisible(boolean)}.
     * </p>
     * <p>
     * The lifecycle of this Window's Screen and Display is handled via {@link Screen#addReference()}
     * and {@link Screen#removeReference()}.
     * </p>
     */
    public static Window createWindow(Screen screen, CapabilitiesImmutable caps) {
        return createWindowImpl(screen, caps);
    }

    /**
     * Create a child Window entity attached to the given parent.<br>
     * The Screen and Display information is regenerated utilizing the parents information,
     * while reusing an existing Display.<br>
     * <p>
     * In case <code>parentWindowObject</code> is a {@link com.jogamp.newt.Window} instance,<br>
     * the new window is added to it's list of children.<br>
     * This assures proper handling of visibility, creation and destruction.<br>
     * {@link com.jogamp.newt.event.WindowEvent#EVENT_WINDOW_RESIZED} is not propagated to the child window for layout<br>,
     * you have to add an appropriate {@link com.jogamp.newt.event.WindowListener} for this use case.<br>
     * The parents visibility is passed to the new Window<br></p>
     * <p>
     * In case <code>parentWindowObject</code> is a different {@link javax.media.nativewindow.NativeWindow} implementation,<br>
     * you have to handle all events appropriate.<br></p>
     * <p>
     * <p>
     * The lifecycle of this Window's Screen and Display is handled via {@link Screen#addReference()}
     * and {@link Screen#removeReference()}.
     * </p>
     *
     * @param parentWindowObject either a NativeWindow instance
     */
    public static Window createWindow(NativeWindow parentWindow, CapabilitiesImmutable caps) {
        final String type = NativeWindowFactory.getNativeWindowType(true);
        Screen screen  = null;
        Window newtParentWindow = null;

        if ( parentWindow instanceof Window ) {
            // use parent NEWT Windows Display/Screen
            newtParentWindow = (Window) parentWindow ;
            screen = newtParentWindow.getScreen();
        } else {
            // create a Display/Screen compatible to the NativeWindow
            AbstractGraphicsConfiguration parentConfig = parentWindow.getGraphicsConfiguration();
            if(null!=parentConfig) {
                AbstractGraphicsScreen parentScreen = parentConfig.getScreen();
                AbstractGraphicsDevice parentDevice = parentScreen.getDevice();
                Display display = NewtFactory.createDisplay(type, parentDevice.getHandle(), true);
                screen  = NewtFactory.createScreen(display, parentScreen.getIndex());
            } else {
                Display display = NewtFactory.createDisplay(type, null, true); // local display
                screen  = NewtFactory.createScreen(display, 0); // screen 0
            }
        }
        final Window win = createWindowImpl(parentWindow, screen, caps);

        win.setSize(parentWindow.getWidth(), parentWindow.getHeight());
        if ( null != newtParentWindow ) {
            newtParentWindow.addChild(win);
            win.setVisible(newtParentWindow.isVisible());
        }
        return win;
    }

    protected static Window createWindowImpl(NativeWindow parentNativeWindow, Screen screen, CapabilitiesImmutable caps) {
        return WindowImpl.create(parentNativeWindow, 0, screen, caps);
    }

    protected static Window createWindowImpl(long parentWindowHandle, Screen screen, CapabilitiesImmutable caps) {
        return WindowImpl.create(null, parentWindowHandle, screen, caps);
    }

    protected static Window createWindowImpl(Screen screen, CapabilitiesImmutable caps) {
        return WindowImpl.create(null, 0, screen, caps);
    }

    protected static Window createWindowImpl(String type, CapabilitiesImmutable caps) {
        Display display = NewtFactory.createDisplay(type, null, true); // local display
        Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        return WindowImpl.create(null, 0, screen, caps);
    }

    /**
     * Create a child Window entity attached to the given parent, incl native creation<br>
     *
     * @param parentWindowObject the native parent window handle
     * @param undecorated only impacts if the window is in top-level state, while attached to a parent window it's rendered undecorated always
     */
    public static Window createWindow(long parentWindowHandle, Screen screen, CapabilitiesImmutable caps) {
        return createWindowImpl(parentWindowHandle, screen, caps);
    }

    /**
     * Ability to try a Window type with a constructor argument, if supported ..<p>
     * Currently only valid is <code> AWTWindow(Frame frame) </code>,
     * to support an external created AWT Frame, ie the browsers embedded frame.
     *
     * @param undecorated only impacts if the window is in top-level state, while attached to a parent window it's rendered undecorated always
     */
    public static Window createWindow(Object[] cstrArguments, Screen screen, CapabilitiesImmutable caps) {
        return WindowImpl.create(cstrArguments, screen, caps);
    }

    /**
     * Instantiate a Display entity using the native handle.
     */
    public static Display createDisplay(String type, long handle, boolean reuse) {
      return DisplayImpl.create(type, null, handle, false);
    }

    public static boolean isScreenCompatible(NativeWindow parent, Screen childScreen) {
      // Get parent's NativeWindow details
      AbstractGraphicsConfiguration parentConfig = (AbstractGraphicsConfiguration) parent.getGraphicsConfiguration();
      AbstractGraphicsScreen parentScreen = (AbstractGraphicsScreen) parentConfig.getScreen();
      AbstractGraphicsDevice parentDevice = (AbstractGraphicsDevice) parentScreen.getDevice();

      DisplayImpl childDisplay = (DisplayImpl) childScreen.getDisplay();
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
        DisplayImpl childDisplay = (DisplayImpl) childScreen.getDisplay();
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
      Display display = NewtFactory.createDisplay(type, parentDevice.getHandle(), true);
      return NewtFactory.createScreen(display, parentScreen.getIndex());
    }
}

