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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowFactory;

import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.PropertyAccess;

import jogamp.newt.Debug;
import jogamp.newt.DisplayImpl;
import jogamp.newt.ScreenImpl;
import jogamp.newt.WindowImpl;

public class NewtFactory {
    public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");

    public static final String DRIVER_DEFAULT_ROOT_PACKAGE = "jogamp.newt.driver";

    private static IOUtil.ClassResources defaultWindowIcons;
    private static String sysPaths = "newt/data/jogamp-16x16.png newt/data/jogamp-32x32.png";

    static {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                NativeWindowFactory.initSingleton(); // last resort ..
                {
                    /** See API Doc in {@link Window} ! */
                    final String[] paths = PropertyAccess.getProperty("newt.window.icons", true, sysPaths).split("[\\s,]");
                    if( paths.length < 2 ) {
                        throw new IllegalArgumentException("Property 'newt.window.icons' did not specify at least two PNG icons, but "+Arrays.toString(paths));
                    }
                    defaultWindowIcons = new IOUtil.ClassResources(paths, NewtFactory.class.getClassLoader(), null);
                }
                return null;
            } } );
    }

    /**
     * Returns the application window icon resources to be used.
     * <p>
     * Property <code>newt.window.icons</code> may define a list of PNG icons separated by one whitespace or one comma character.
     * Shall reference at least two PNG icons, from lower (16x16) to higher (>= 32x32) resolution.
     * </p>
     * <p>
     * Users may also specify application window icons using {@link #setWindowIcons(com.jogamp.common.util.IOUtil.ClassResources)}.
     * </p>
     */
    public static IOUtil.ClassResources getWindowIcons() { return defaultWindowIcons; }

    /**
     * Allow user to set custom window icons, only applicable at application start before creating any NEWT instance.
     * <p>
     * Shall reference at least two PNG icons, from lower (16x16) to higher (>= 32x32) resolution.
     * </p>
     */
    public static void setWindowIcons(final IOUtil.ClassResources cres) { defaultWindowIcons = cres; }

    public static Class<?> getCustomClass(final String packageName, final String classBaseName) {
        Class<?> clazz = null;
        if(packageName!=null && classBaseName!=null) {
            final String clazzName;
            if( packageName.startsWith(".") ) {
                clazzName = DRIVER_DEFAULT_ROOT_PACKAGE + packageName + "." + classBaseName ;
            } else {
                clazzName = packageName + "." + classBaseName ;
            }
            try {
                clazz = Class.forName(clazzName);
            } catch (final Throwable t) {
                if(DEBUG_IMPLEMENTATION) {
                    System.err.println("Warning: Failed to find class <"+clazzName+">: "+t.getMessage());
                    t.printStackTrace();
                }
            }
        }
        return clazz;
    }

    private static boolean useEDT = true;

    /**
     * Toggles the usage of an EventDispatchThread while creating a Display.<br>
     * The default is enabled.<br>
     * The EventDispatchThread is thread local to the Display instance.<br>
     */
    public static synchronized void setUseEDT(final boolean onoff) {
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
    public static Display createDisplay(final String name) {
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
    public static Display createDisplay(final String name, final boolean reuse) {
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
    public static Display createDisplay(final String type, final String name) {
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
    public static Display createDisplay(final String type, final String name, final boolean reuse) {
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
    public static Screen createScreen(final Display display, final int index) {
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
    public static Window createWindow(final CapabilitiesImmutable caps) {
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
    public static Window createWindow(final Screen screen, final CapabilitiesImmutable caps) {
        return WindowImpl.create(null, 0, screen, caps);
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
     * In case <code>parentWindowObject</code> is a different {@link com.jogamp.nativewindow.NativeWindow} implementation,<br>
     * you have to handle all events appropriate.<br></p>
     * <p>
     * <p>
     * The lifecycle of this Window's Screen and Display is handled via {@link Screen#addReference()}
     * and {@link Screen#removeReference()}.
     * </p>
     *
     * @param parentWindowObject either a NativeWindow instance
     */
    public static Window createWindow(final NativeWindow parentWindow, final CapabilitiesImmutable caps) {
        final String type = NativeWindowFactory.getNativeWindowType(true);
        if( null == parentWindow ) {
            return createWindowImpl(type, caps);
        }
        Screen screen  = null;
        Window newtParentWindow = null;

        if ( parentWindow instanceof Window ) {
            // use parent NEWT Windows Display/Screen
            newtParentWindow = (Window) parentWindow ;
            screen = newtParentWindow.getScreen();
        } else {
            // create a Display/Screen compatible to the NativeWindow
            final AbstractGraphicsConfiguration parentConfig = parentWindow.getGraphicsConfiguration();
            if(null!=parentConfig) {
                final AbstractGraphicsScreen parentScreen = parentConfig.getScreen();
                final AbstractGraphicsDevice parentDevice = parentScreen.getDevice();
                final Display display = NewtFactory.createDisplay(type, parentDevice.getHandle(), true);
                screen  = NewtFactory.createScreen(display, parentScreen.getIndex());
            } else {
                final Display display = NewtFactory.createDisplay(type, null, true); // local display
                screen  = NewtFactory.createScreen(display, 0); // screen 0
            }
        }
        final Window win = WindowImpl.create(parentWindow, 0, screen, caps);

        win.setSize(parentWindow.getWidth(), parentWindow.getHeight());
        if ( null != newtParentWindow ) {
            newtParentWindow.addChild(win);
            win.setVisible(newtParentWindow.isVisible());
        }
        return win;
    }

    private static Window createWindowImpl(final String type, final CapabilitiesImmutable caps) {
        final Display display = NewtFactory.createDisplay(type, null, true); // local display
        final Screen screen  = NewtFactory.createScreen(display, 0); // screen 0
        return WindowImpl.create(null, 0, screen, caps);
    }

    /**
     * Create a child Window entity attached to the given parent, incl native creation<br>
     *
     * @param displayConnection the parent window's display connection
     * @param screenIdx the desired screen index
     * @param parentWindowHandle the native parent window handle
     * @param caps the desired capabilities
     * @return
     */
    public static Window createWindow(final String displayConnection, final int screenIdx, final long parentWindowHandle, final CapabilitiesImmutable caps) {
        final String type = NativeWindowFactory.getNativeWindowType(true);
        final Display display = NewtFactory.createDisplay(type, displayConnection, true);
        final Screen screen  = NewtFactory.createScreen(display, screenIdx);
        return WindowImpl.create(null, parentWindowHandle, screen, caps);
    }

    /**
     * Ability to try a Window type with a constructor argument, if supported ..<p>
     * Currently only valid is <code> AWTWindow(Frame frame) </code>,
     * to support an external created AWT Frame, ie the browsers embedded frame.
     *
     * @param undecorated only impacts if the window is in top-level state, while attached to a parent window it's rendered undecorated always
     */
    public static Window createWindow(final Object[] cstrArguments, final Screen screen, final CapabilitiesImmutable caps) {
        return WindowImpl.create(cstrArguments, screen, caps);
    }

    /**
     * Instantiate a Display entity using the native handle.
     */
    public static Display createDisplay(final String type, final long handle, final boolean reuse) {
      return DisplayImpl.create(type, null, handle, reuse);
    }

    public static boolean isScreenCompatible(final NativeWindow parent, final Screen childScreen) {
      // Get parent's NativeWindow details
      final AbstractGraphicsConfiguration parentConfig = parent.getGraphicsConfiguration();
      final AbstractGraphicsScreen parentScreen = parentConfig.getScreen();
      final AbstractGraphicsDevice parentDevice = parentScreen.getDevice();

      final DisplayImpl childDisplay = (DisplayImpl) childScreen.getDisplay();
      final String parentDisplayName = childDisplay.validateDisplayName(null, parentDevice.getHandle());
      final String childDisplayName = childDisplay.getName();
      if( ! parentDisplayName.equals( childDisplayName ) ) {
        return false;
      }

      if( parentScreen.getIndex() != childScreen.getIndex() ) {
        return false;
      }
      return true;
    }

    public static Screen createCompatibleScreen(final NativeWindow parent) {
      return createCompatibleScreen(parent, null);
    }

    public static Screen createCompatibleScreen(final NativeWindow parent, final Screen childScreen) {
      // Get parent's NativeWindow details
      final AbstractGraphicsConfiguration parentConfig = parent.getGraphicsConfiguration();
      final AbstractGraphicsScreen parentScreen = parentConfig.getScreen();
      final AbstractGraphicsDevice parentDevice = parentScreen.getDevice();

      if(null != childScreen) {
        // check if child Display/Screen is compatible already
        final DisplayImpl childDisplay = (DisplayImpl) childScreen.getDisplay();
        final String parentDisplayName = childDisplay.validateDisplayName(null, parentDevice.getHandle());
        final String childDisplayName = childDisplay.getName();
        final boolean displayEqual = parentDisplayName.equals( childDisplayName );
        final boolean screenEqual = parentScreen.getIndex() == childScreen.getIndex();
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
      final Display display = NewtFactory.createDisplay(type, parentDevice.getHandle(), true);
      return NewtFactory.createScreen(display, parentScreen.getIndex());
    }
}

