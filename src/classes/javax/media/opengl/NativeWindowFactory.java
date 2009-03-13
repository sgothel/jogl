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
 */

package javax.media.opengl;

import javax.media.nwi.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;

import com.sun.opengl.impl.*;

/** Provides the link between the window toolkit and the Java binding
    to the OpenGL API. The NativeWindowFactory, and NativeWindow
    instances it creates, encompass all of the toolkit-specific
    functionality, leaving the GLDrawableFactory independent of any
    particular toolkit. */

public abstract class NativeWindowFactory {
    private static NativeWindowFactory defaultFactory;
    private static HashMap/*<Class, NativeWindowFactory>*/ registeredFactories =
        new HashMap();
    private static Class nativeWindowClass;

    /** Creates a new NativeWindowFactory instance. End users do not
        need to call this method. */
    protected NativeWindowFactory() {
    }

    static {
        initialize();
    }

    private static void initialize() {
        String osName = System.getProperty("os.name");
        String osNameLowerCase = osName.toLowerCase();
        String factoryClassName = null;

        // We break compile-time dependencies on the AWT here to
        // make it easier to run this code on mobile devices

        NativeWindowFactory factory = new NativeWindowFactoryImpl();
        nativeWindowClass = javax.media.nwi.NativeWindow.class;
        registerFactory(nativeWindowClass, factory);
        defaultFactory = factory;
        
        Class componentClass = null;
        try {
            componentClass = Class.forName("java.awt.Component");
        } catch (Exception e) {
        }
        if (componentClass != null) {
            if (!osNameLowerCase.startsWith("wind") &&
                !osNameLowerCase.startsWith("mac os x")) {
                // Assume X11 platform -- should probably test for these explicitly
                try {
                    Constructor factoryConstructor =
                        GLReflection.getConstructor("com.sun.opengl.impl.x11.glx.awt.X11AWTGLXNativeWindowFactory", new Class[] {});
                    factory = (NativeWindowFactory) factoryConstructor.newInstance(null);
                } catch (Exception e) { }
            }
            registerFactory(componentClass, factory);
            defaultFactory = factory;
        }
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
        only implementors of new NativeWindowFactory subclasses.. */
    protected static void registerFactory(Class windowClass, NativeWindowFactory factory) {
        registeredFactories.put(windowClass, factory);
    }

    /** Converts the given window object into a {@link NativeWindow
        NativeWindow} which can be operated upon by the {@link
        GLDrawableFactory GLDrawableFactory}. The object may be a
        component for a particular window toolkit, such as an AWT
        Canvas. It may also be a NativeWindow object, in which no
        conversion is necessary. The particular implementation of the
        NativeWindowFactory is responsible for handling objects from a
        particular window toolkit. The built-in NativeWindowFactory
        handles NativeWindow instances as well as AWT Components.
    
        @throws IllegalArgumentException if the given window object
        could not be handled by any of the registered
        NativeWindowFactory instances
    */
    public static NativeWindow getNativeWindow(Object winObj) throws IllegalArgumentException, NativeWindowException {
        if (winObj == null) {
            throw new IllegalArgumentException("Null window object");
        }

        return getFactory(winObj.getClass()).getNativeWindowImpl(winObj);
    }

    /**
     * <P> Selects a graphics configuration on the specified graphics
     * device compatible with the supplied NWCapabilities. This method
     * is intended to be used by applications which do not use the
     * supplied GLCanvas class but instead wrap their own Canvas or
     * other window toolkit-specific object with a GLDrawable. Some
     * platforms (specifically X11) require the graphics configuration
     * to be specified when the window toolkit object is created. This
     * method may return null on platforms on which the OpenGL pixel
     * format selection process is performed later. </P>
     *
     * <P> The concrete data type of the passed graphics device and
     * returned graphics configuration must be specified in the
     * documentation binding this particular API to the underlying
     * window toolkit. The Reference Implementation accepts {@link
     * AWTGraphicsDevice AWTGraphicsDevice} objects and returns {@link
     * AWTGraphicsConfiguration AWTGraphicsConfiguration} objects. </P>
     *
     * @see java.awt.Canvas#Canvas(java.awt.GraphicsConfiguration)
     *
     * @throws IllegalArgumentException if the data type of the passed
     *         AbstractGraphicsDevice is not supported by this
     *         NativeWindowFactory.
     * @throws GLException if any window system-specific errors caused
     *         the selection of the graphics configuration to fail.
     */
    public abstract AbstractGraphicsConfiguration
        chooseGraphicsConfiguration(NWCapabilities capabilities,
                                    NWCapabilitiesChooser chooser,
                                    AbstractGraphicsDevice device)
        throws IllegalArgumentException, GLException;

    /** Performs the conversion from a toolkit's window object to a
        NativeWindow. Implementors of concrete NativeWindowFactory
        subclasses should override this method. */
    protected abstract NativeWindow getNativeWindowImpl(Object winObj) throws IllegalArgumentException;

    /** Returns the object which provides support for synchronizing
        with the underlying window toolkit. On most platforms the
        returned object does nothing; currently it only has effects on
        X11 platforms. */
    public abstract ToolkitLock getToolkitLock();
}
