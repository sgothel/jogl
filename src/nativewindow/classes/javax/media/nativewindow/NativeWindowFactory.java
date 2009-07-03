/*
 * Copyright (c) 2008-2009 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package javax.media.nativewindow;

import java.lang.reflect.*;
import java.security.*;
import java.util.*;

import com.sun.nativewindow.impl.*;
import com.sun.nativewindow.impl.jvm.JVMUtil;

/** Provides a pluggable mechanism for arbitrary window toolkits to
    adapt their components to the {@link NativeWindow} interface,
    which provides a platform-independent mechanism of accessing the
    information required to perform operations like
    hardware-accelerated rendering using the OpenGL API. */

public abstract class NativeWindowFactory {
    protected static final boolean DEBUG = Debug.debug("NativeWindow");

    /** OpenKODE/EGL type */
    public static final String TYPE_EGL = "EGL";

    /** Microsoft Windows type */
    public static final String TYPE_WINDOWS = "Windows";

    /** X11 type */
    public static final String TYPE_X11 = "X11";

    /** Mac OS X type */
    public static final String TYPE_MACOSX = "MacOSX";

    /** Generic AWT type */
    public static final String TYPE_AWT = "AWT";

    /** Generic DEFAULT type, where platform implementation don't care */
    public static final String TYPE_DEFAULT = "default";

    private static NativeWindowFactory defaultFactory;
    private static Map/*<Class, NativeWindowFactory>*/ registeredFactories;
    private static Class nativeWindowClass;
    private static String nativeWindowingTypePure;
    private static String nativeOSNamePure;
    private static String nativeWindowingTypeCustom;
    private static String nativeOSNameCustom;

    /** Creates a new NativeWindowFactory instance. End users do not
        need to call this method. */
    protected NativeWindowFactory() {
    }

    private static String _getNativeWindowingType(String osNameLowerCase) {
        if (osNameLowerCase.startsWith("kd")) {
              return TYPE_EGL;
        } else if (osNameLowerCase.startsWith("wind")) {
              return TYPE_WINDOWS;
        } else if (osNameLowerCase.startsWith("mac os x") ||
                     osNameLowerCase.startsWith("darwin")) {
              return TYPE_MACOSX;
        } else if (osNameLowerCase.equals("awt")) {
              return TYPE_AWT;
        } else {
              return TYPE_X11;
        }
    }

    static {
        JVMUtil.initSingleton();

        // Gather the windowing OS first
        AccessControlContext acc = AccessController.getContext();
        nativeOSNamePure = Debug.getProperty("os.name", false, acc);
        nativeOSNameCustom = Debug.getProperty("nativewindow.ws.name", true, acc);
        if(null==nativeOSNameCustom||nativeOSNameCustom.length()==0) {
              nativeOSNameCustom = nativeOSNamePure;
        }
        nativeWindowingTypePure = _getNativeWindowingType(nativeOSNamePure.toLowerCase());
        nativeWindowingTypeCustom = _getNativeWindowingType(nativeOSNameCustom.toLowerCase());

        registeredFactories = Collections.synchronizedMap(new HashMap());

        String factoryClassName = null;

        // register our default factory -> NativeWindow
        NativeWindowFactory factory = new NativeWindowFactoryImpl();
        nativeWindowClass = javax.media.nativewindow.NativeWindow.class;
        registerFactory(nativeWindowClass, factory);
        defaultFactory = factory;
    
        // We break compile-time dependencies on the AWT here to
        // make it easier to run this code on mobile devices

        Class componentClass = null;
        if ( NWReflection.isClassAvailable("java.awt.Component") &&
             NWReflection.isClassAvailable("javax.media.nativewindow.awt.AWTGraphicsDevice") ) {
            try {
                componentClass = NWReflection.getClass("java.awt.Component", false);
            } catch (Exception e) { }
        }

        boolean toolkitLockForced   = Debug.getBooleanProperty("nativewindow.locking", true, acc);
        boolean awtToolkitLockDisabled = Debug.getBooleanProperty("java.awt.headless", false, acc);

        NativeWindowFactory _factory = null;
        
        if( !awtToolkitLockDisabled && null!=componentClass && TYPE_X11.equals(nativeWindowingTypeCustom) ) {
            // There are certain operations that may be done by
            // user-level native code which must share the display
            // connection with the underlying window toolkit. In JOGL,
            // for example, the AWT GLCanvas makes GLX and OpenGL
            // calls against an X Drawable that was created by the
            // AWT. In this case, the AWT Native Interface ("JAWT") is
            // used to lock and unlock this surface, which grabs and
            // releases a lock which is also used internally to the
            // AWT implementation. This is required because the AWT
            // makes X calls from multiple threads: for example, the
            // AWT Toolkit thread and one or more Event Dispatch
            // Threads.
            //
            // There are cases where synchronization with the
            // toolkit is required in case of a shared resource pattern,
            // e.g. with AWT.  From recollection, visual selection is 
            // performed outside of the cover of the
            // toolkit's lock, and the toolkit's display connection is
            // used for this operation, so for correctness the toolkit
            // must be locked during glXChooseFBConfig /
            // glXChooseVisual. Synchronization with the toolkit is
            // definitely needed for support of external GLDrawables,
            // where JOGL creates additional OpenGL contexts on a
            // surface that was created by a third party. External
            // GLDrawables are the foundation of the Java 2D / JOGL
            // bridge. While this bridge may be historical at this
            // point, support for external GLDrawables on platforms
            // that can support them (namely, WGL and X11 platforms;
            // Mac OS X does not currently have the required
            // primitives in its OpenGL window system binding) makes
            // the JOGL library more powerful.
            // 
            // (FIXME: from code examination, it looks like there are
            // regressions in the support for external GLDrawables in
            // JOGL 2 compared to JOGL 1.1.1. Note that the "default"
            // X display connection from X11Util is being used during
            // construction of the X11ExternalGLXDrawable instead of
            // the result of glXGetCurrentDisplay().)
            //
            // The X11AWTNativeWindowFactory provides a locking
            // mechanism compatible with the AWT. It may be desirable
            // to replace this window factory when using third-party
            // toolkits like Newt even when running on Java SE when
            // the AWT is available.

            try {
                Constructor factoryConstructor =
                    NWReflection.getConstructor("com.sun.nativewindow.impl.x11.awt.X11AWTNativeWindowFactory", new Class[] {});
                _factory = (NativeWindowFactory) factoryConstructor.newInstance(null);
            } catch (Exception e) { }
        }

        if (toolkitLockForced && null==_factory) {
            try {
                Constructor factoryConstructor =
                    NWReflection.getConstructor("com.sun.nativewindow.impl.LockingNativeWindowFactory", new Class[] {});
                _factory = (NativeWindowFactory) factoryConstructor.newInstance(null);
            } catch (Exception e) { }
        }

        if (null !=_factory) {
            factory = _factory;
        }

        if(null!=componentClass) {
            // register either our default factory or (if exist) the X11/AWT one -> AWT Component
            registerFactory(componentClass, factory);
        }
        defaultFactory = factory;

        if(DEBUG) {
            System.err.println("NativeWindowFactory toolkitLockForced "+toolkitLockForced+
                               ", awtToolkitLockDisabled "+awtToolkitLockDisabled+", defaultFactory "+factory);
        }
    }

    public static String getNativeOSName(boolean useCustom) {
        return useCustom?nativeOSNameCustom:nativeOSNamePure;
    }

    public static String getNativeWindowType(boolean useCustom) {
        return useCustom?nativeWindowingTypeCustom:nativeWindowingTypePure;
    }

    /** Sets the default NativeWindowFactory. Certain operations on
        X11 platforms require synchronization, and the implementation
        of this synchronization may be specific to the window toolkit
        in use. It is impractical to require that all of the APIs that
        might require synchronization receive a {@link ToolkitLock
        ToolkitLock} as argument. For this reason the concept of a
        default NativeWindowFactory is introduced. The toolkit lock
        provided via {@link #getToolkitLock getToolkitLock} from this
        default NativeWindowFactory will be used for synchronization
        within the Java binding to OpenGL. By default, if the AWT is
        available, the default toolkit will support the AWT. */
    public static void setDefaultFactory(NativeWindowFactory factory) {
        defaultFactory = factory;
    }

    /** Gets the default NativeWindowFactory. Certain operations on
        X11 platforms require synchronization, and the implementation
        of this synchronization may be specific to the window toolkit
        in use. It is impractical to require that all of the APIs that
        might require synchronization receive a {@link ToolkitLock
        ToolkitLock} as argument. For this reason the concept of a
        default NativeWindowFactory is introduced. The toolkit lock
        provided via {@link #getToolkitLock getToolkitLock} from this
        default NativeWindowFactory will be used for synchronization
        within the Java binding to OpenGL. By default, if the AWT is
        available, the default toolkit will support the AWT. */
    public static NativeWindowFactory getDefaultFactory() {
        return defaultFactory;
    }

    /** Returns the appropriate NativeWindowFactory to handle window
        objects of the given type. The windowClass might be {@link
        NativeWindow NativeWindow}, in which case the client has
        already assumed the responsibility of creating a compatible
        NativeWindow implementation, or it might be that of a toolkit
        class like {@link java.awt.Component Component}. */
    public static NativeWindowFactory getFactory(Class windowClass) throws IllegalArgumentException {
        if (nativeWindowClass.isAssignableFrom(windowClass)) {
            return (NativeWindowFactory) registeredFactories.get(nativeWindowClass);
        }
        Class clazz = windowClass;
        while (clazz != null) {
            NativeWindowFactory factory = (NativeWindowFactory) registeredFactories.get(clazz);
            if (factory != null) {
                return factory;
            }
            clazz = clazz.getSuperclass();
        }
        throw new IllegalArgumentException("No registered NativeWindowFactory for class " + windowClass.getName());
    }

    /** Registers a NativeWindowFactory handling window objects of the
        given class. This does not need to be called by end users,
        only implementors of new NativeWindowFactory subclasses. */
    protected static void registerFactory(Class windowClass, NativeWindowFactory factory) {
        if(DEBUG) {
            System.err.println("NativeWindowFactory.registerFactory() "+windowClass+" -> "+factory);
        }
        registeredFactories.put(windowClass, factory);
    }

    /** Converts the given window object and it's
        {@link AbstractGraphicsConfiguration AbstractGraphicsConfiguration} into a 
        {@link NativeWindow NativeWindow} which can be operated upon by a custom
        toolkit, e.g. {@link javax.media.opengl.GLDrawableFactory javax.media.opengl.GLDrawableFactory}.<br>
        The object may be a component for a particular window toolkit, such as an AWT
        Canvas.  It may also be a NativeWindow object itself.<br>
        You shall utilize {@link javax.media.nativewindow.GraphicsConfigurationFactory GraphicsConfigurationFactory}
        to construct a proper {@link AbstractGraphicsConfiguration AbstractGraphicsConfiguration}.<br>
        The particular implementation of the
        NativeWindowFactory is responsible for handling objects from a
        particular window toolkit. The built-in NativeWindowFactory
        handles NativeWindow instances as well as AWT Components.<br>
    
        @throws IllegalArgumentException if the given window object
        could not be handled by any of the registered
        NativeWindowFactory instances

        @see javax.media.nativewindow.GraphicsConfigurationFactory#chooseGraphicsConfiguration(Capabilities, CapabilitiesChooser, AbstractGraphicsScreen)
    */
    public static NativeWindow getNativeWindow(Object winObj, AbstractGraphicsConfiguration config) throws IllegalArgumentException, NativeWindowException {
        if (winObj == null) {
            throw new IllegalArgumentException("Null window object");
        }

        return getFactory(winObj.getClass()).getNativeWindowImpl(winObj, config);
    }

    /** Performs the conversion from a toolkit's window object to a
        NativeWindow. Implementors of concrete NativeWindowFactory
        subclasses should override this method. */
    protected abstract NativeWindow getNativeWindowImpl(Object winObj, AbstractGraphicsConfiguration config) throws IllegalArgumentException;

    /** Returns the object which provides support for synchronizing
        with the underlying window toolkit.<br>
        @see ToolkitLock
      */
    public abstract ToolkitLock getToolkitLock();
}
