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

/** Provides a pluggable mechanism for arbitrary window toolkits to
    adapt their components to the {@link NativeWindow} interface,
    which provides a platform-independent mechanism of accessing the
    information required to perform operations like
    hardware-accelerated rendering using the OpenGL API. */

public abstract class NativeWindowFactory {
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
    private static Map/*<Class, NativeWindowFactory>*/ registeredFactories =
        Collections.synchronizedMap(new HashMap());
    private static Class nativeWindowClass;
    private static String nativeWindowingType=null;

    /** Creates a new NativeWindowFactory instance. End users do not
        need to call this method. */
    protected NativeWindowFactory() {
    }

    private static boolean initialized = false;
    private static void initialize() {
        synchronized (NativeWindowFactory.class) {
            if (initialized) {
                return;
            }
            initialized = true;

            String factoryClassName = null;

            // We break compile-time dependencies on the AWT here to
            // make it easier to run this code on mobile devices

            NativeWindowFactory factory = new NativeWindowFactoryImpl();
            nativeWindowClass = javax.media.nativewindow.NativeWindow.class;
            registerFactory(nativeWindowClass, factory);
            defaultFactory = factory;
        
            Class componentClass = null;
            try {
                componentClass = Class.forName("java.awt.Component");
            } catch (Exception e) { }

            if(TYPE_X11.equals(getNativeWindowType(true))) {
                // Assume X11 platform -- should probably test for these explicitly
                NativeWindowFactory _factory = null;
                if (componentClass != null) {
                    try {
                        Constructor factoryConstructor =
                            NWReflection.getConstructor("com.sun.nativewindow.impl.x11.awt.X11AWTNativeWindowFactory", new Class[] {});
                        _factory = (NativeWindowFactory) factoryConstructor.newInstance(null);
                    } catch (Exception e) { }
                }
                if (null ==_factory) {
                    // Try the non-AWT X11 native window factory
                    try {
                        Constructor factoryConstructor =
                            NWReflection.getConstructor("com.sun.nativewindow.impl.x11.X11NativeWindowFactory", new Class[] {});
                        _factory = (NativeWindowFactory) factoryConstructor.newInstance(null);
                    } catch (Exception e) { }
                }
                if (null !=_factory) {
                    factory = _factory;
                }
            }
            registerFactory(componentClass, factory);
            defaultFactory = factory;
        }
    }

    public static String getNativeWindowType(boolean useCustom) {
      if(null==nativeWindowingType) {
          String osName = useCustom?System.getProperty("nativewindow.ws.name"):null;
          if(null==osName||osName.length()==0) {
              osName = System.getProperty("os.name");
          }
          String osNameLowerCase = osName.toLowerCase();
          String windowType;
          if (osNameLowerCase.startsWith("kd")) {
              nativeWindowingType = NativeWindowFactory.TYPE_EGL;
          } else if (osNameLowerCase.startsWith("wind")) {
              nativeWindowingType = NativeWindowFactory.TYPE_WINDOWS;
          } else if (osNameLowerCase.startsWith("mac os x") ||
                     osNameLowerCase.startsWith("darwin")) {
              nativeWindowingType = NativeWindowFactory.TYPE_MACOSX;
          } else if (useCustom && osNameLowerCase.equals("awt")) {
              nativeWindowingType = NativeWindowFactory.TYPE_AWT;
          } else {
              nativeWindowingType = NativeWindowFactory.TYPE_X11;
          }
      }
      return nativeWindowingType;
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
        initialize();
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
        initialize();
        return defaultFactory;
    }

    /** Returns the appropriate NativeWindowFactory to handle window
        objects of the given type. The windowClass might be {@link
        NativeWindow NativeWindow}, in which case the client has
        already assumed the responsibility of creating a compatible
        NativeWindow implementation, or it might be that of a toolkit
        class like {@link java.awt.Component Component}. */
    public static NativeWindowFactory getFactory(Class windowClass) throws IllegalArgumentException {
        initialize();
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
        initialize();
        registeredFactories.put(windowClass, factory);
    }

    /** Converts the given window object and it's optional
        {@link AbstractGraphicsConfiguration AbstractGraphicsConfiguration} into a 
        {@link NativeWindow NativeWindow} which can be operated upon by the 
        {@link GLDrawableFactory GLDrawableFactory}. The object may be a
        component for a particular window toolkit, such as an AWT
        Canvas. In this case {@link AbstractGraphicsConfiguration AbstractGraphicsConfiguration}
        must be valid. It may also be a NativeWindow object, in which no
        conversion is necessary and {@link AbstractGraphicsConfiguration AbstractGraphicsConfiguration}
        will be ignored. The particular implementation of the
        NativeWindowFactory is responsible for handling objects from a
        particular window toolkit. The built-in NativeWindowFactory
        handles NativeWindow instances as well as AWT Components.
    
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
        with the underlying window toolkit. On most platforms the
        returned object does nothing; currently it only has effects on
        X11 platforms. */
    public abstract ToolkitLock getToolkitLock();
}
