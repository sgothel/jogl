/*
 * Copyright (c) 2008-2009 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package javax.media.nativewindow;

import java.security.*;
import java.util.*;

import com.jogamp.common.util.*;
import com.jogamp.common.jvm.JVMUtil;
import com.jogamp.nativewindow.impl.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Provides a pluggable mechanism for arbitrary window toolkits to
    adapt their components to the {@link NativeWindow} interface,
    which provides a platform-independent mechanism of accessing the
    information required to perform operations like
    hardware-accelerated rendering using the OpenGL API. */

public abstract class NativeWindowFactory {
    protected static final boolean DEBUG = Debug.debug("NativeWindow");

    /** OpenKODE/EGL type, as retrieved with {@link #getNativeWindowType(boolean)}*/
    public static final String TYPE_EGL = "EGL";

    /** Microsoft Windows type, as retrieved with {@link #getNativeWindowType(boolean)} */
    public static final String TYPE_WINDOWS = "Windows";

    /** X11 type, as retrieved with {@link #getNativeWindowType(boolean)} */
    public static final String TYPE_X11 = "X11";

    /** Mac OS X type, as retrieved with {@link #getNativeWindowType(boolean)} */
    public static final String TYPE_MACOSX = "MacOSX";

    /** Generic AWT type, as retrieved with {@link #getNativeWindowType(boolean)} */
    public static final String TYPE_AWT = "AWT";

    /** Generic DEFAULT type, where platform implementation don't care, as retrieved with {@link #getNativeWindowType(boolean)} */
    public static final String TYPE_DEFAULT = "default";

    private static NativeWindowFactory defaultFactory;
    private static Map/*<Class, NativeWindowFactory>*/ registeredFactories;
    private static Class nativeWindowClass;
    private static String nativeWindowingTypePure;
    private static String nativeOSNamePure;
    private static String nativeWindowingTypeCustom;
    private static String nativeOSNameCustom;
    private static boolean isAWTAvailable;
    public static final String AWTComponentClassName = "java.awt.Component" ;
    public static final String JAWTUtilClassName = "com.jogamp.nativewindow.impl.jawt.JAWTUtil" ;
    public static final String X11UtilClassName = "com.jogamp.nativewindow.impl.x11.X11Util";
    public static final String X11JAWTToolkitLockClassName = "com.jogamp.nativewindow.impl.jawt.x11.X11JAWTToolkitLock" ;
    public static final String X11ToolkitLockClassName = "com.jogamp.nativewindow.impl.x11.X11ToolkitLock" ;
    private static Class  jawtUtilClass;
    private static Method jawtUtilGetJAWTToolkitMethod;
    private static Method jawtUtilInitMethod;
    private static Class  x11JAWTToolkitLockClass;
    private static Constructor x11JAWTToolkitLockConstructor;
    private static Class  x11ToolkitLockClass;
    private static Constructor x11ToolkitLockConstructor;
    private static boolean isFirstUIActionOnProcess;

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
    }

    static boolean initialized = false;

    /**
     * Static one time initialization of this factory.<br>
     * This initialization method <b>must be called</b> once by the program or utilizing modules!<br>
     * @param firstUIActionOnProcess Should be <code>true</code> if called before the first UI action of the running program,
     * otherwise <code>false</code>.
     */
    public static synchronized void initSingleton(final boolean firstUIActionOnProcess) {
        if(!initialized) {
            initialized = true;

            if(DEBUG) {
                Throwable td = new Throwable("Info: NativeWindowFactory.initSingleton("+firstUIActionOnProcess+")");
                td.printStackTrace();
            }

            // Gather the windowing OS first
            AccessControlContext acc = AccessController.getContext();
            nativeOSNamePure = Debug.getProperty("os.name", false, acc);
            nativeWindowingTypePure = _getNativeWindowingType(nativeOSNamePure.toLowerCase());
            nativeOSNameCustom = Debug.getProperty("nativewindow.ws.name", true, acc);
            if(null==nativeOSNameCustom||nativeOSNameCustom.length()==0) {
                nativeOSNameCustom = nativeOSNamePure;
                nativeWindowingTypeCustom = nativeWindowingTypePure;
            } else {
                nativeWindowingTypeCustom = nativeOSNameCustom;
            }

            ClassLoader cl = NativeWindowFactory.class.getClassLoader();

            if( TYPE_X11.equals(nativeWindowingTypePure) ) {
                // explicit initialization of X11Util
                ReflectionUtil.callStaticMethod(X11UtilClassName, "initSingleton", 
                                                new Class[] { boolean.class }, 
                                                new Object[] { new Boolean(firstUIActionOnProcess) }, cl );
            }
            isFirstUIActionOnProcess = firstUIActionOnProcess;

            if( !Debug.getBooleanProperty("java.awt.headless", true, acc) &&
                ReflectionUtil.isClassAvailable(AWTComponentClassName, cl) &&
                ReflectionUtil.isClassAvailable("javax.media.nativewindow.awt.AWTGraphicsDevice", cl) ) {

                AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        try {
                            jawtUtilClass = Class.forName(JAWTUtilClassName, false, NativeWindowFactory.class.getClassLoader());
                            jawtUtilInitMethod = jawtUtilClass.getDeclaredMethod("initSingleton", null);
                            jawtUtilInitMethod.setAccessible(true);
                            jawtUtilGetJAWTToolkitMethod = jawtUtilClass.getDeclaredMethod("getJAWTToolkitLock", new Class[]{});
                            jawtUtilGetJAWTToolkitMethod.setAccessible(true);
                        } catch (Exception e) {
                            // Either not a Sun JDK or the interfaces have changed since 1.4.2 / 1.5
                        }
                        return null;
                    }
                });
                if(null != jawtUtilClass && null != jawtUtilGetJAWTToolkitMethod && null != jawtUtilInitMethod) {
                    ReflectionUtil.callMethod(null, jawtUtilInitMethod, null);

                    Object resO = ReflectionUtil.callStaticMethod(JAWTUtilClassName, "isHeadlessMode", null, null, cl );
                    if(resO instanceof Boolean) {
                        // AWT is only available in case all above classes are available
                        // and AWT is not int headless mode
                        isAWTAvailable = ((Boolean)resO).equals(Boolean.FALSE);
                    } else {
                        isAWTAvailable = false;
                    }
                } else {
                    isAWTAvailable = false;
                }
            } else {
                isAWTAvailable = false;
            }

            registeredFactories = Collections.synchronizedMap(new HashMap());

            // register our default factory -> NativeWindow
            NativeWindowFactory factory = new NativeWindowFactoryImpl();
            nativeWindowClass = javax.media.nativewindow.NativeWindow.class;
            registerFactory(nativeWindowClass, factory);
            defaultFactory = factory;
        
            if ( isAWTAvailable ) {
                // register either our default factory or (if exist) the X11/AWT one -> AWT Component
                registerFactory(ReflectionUtil.getClass(AWTComponentClassName, false, cl), factory);
            }

            if( TYPE_X11 == nativeWindowingTypePure ) {
                // passing through RuntimeException if not exists intended
                x11ToolkitLockClass = ReflectionUtil.getClass(X11ToolkitLockClassName, false, cl);
                x11ToolkitLockConstructor = ReflectionUtil.getConstructor(x11ToolkitLockClass, new Class[] { long.class } );
                if( isAWTAvailable() ) {
                    x11JAWTToolkitLockClass = ReflectionUtil.getClass(X11JAWTToolkitLockClassName, false, cl);
                    x11JAWTToolkitLockConstructor = ReflectionUtil.getConstructor(x11JAWTToolkitLockClass, new Class[] { long.class } );
                }
            }
 
            if(DEBUG) {
                System.err.println("NativeWindowFactory firstUIActionOnProcess "+firstUIActionOnProcess);
                System.err.println("NativeWindowFactory isAWTAvailable "+isAWTAvailable+", defaultFactory "+factory);
            }
        }
    }

    /** @return true if initialized with <b>{@link #initSingleton(boolean) initSingleton(firstUIActionOnProcess==true)}</b>,
        otherwise false. */
    public static boolean isFirstUIActionOnProcess() {
        return isFirstUIActionOnProcess;
    }

    /** @return true if not headless, AWT Component and NativeWindow's AWT part available */
    public static boolean isAWTAvailable() { return isAWTAvailable; }

    /**
     * @param useCustom if false return the native value, if true return a custom value if set, otherwise fallback to the native value.
     * @return the native OS name
     */
    public static String getNativeOSName(boolean useCustom) {
        return useCustom?nativeOSNameCustom:nativeOSNamePure;
    }

    /**
     * @param useCustom if false return the native value, if true return a custom value if set, otherwise fallback to the native value.
     * @return a define native window type, like {@link #TYPE_X11}, ..
     */
    public static String getNativeWindowType(boolean useCustom) {
        return useCustom?nativeWindowingTypeCustom:nativeWindowingTypePure;
    }

    /** Don't know if we shall add this factory here .. 
    public static AbstractGraphicsDevice createGraphicsDevice(String type, String connection, int unitID, long handle, ToolkitLock locker) {
        if(type.equals(TYPE_EGL)) {
            return new
        } else if(type.equals(TYPE_X11)) {
        } else if(type.equals(TYPE_WINDOWS)) {
        } else if(type.equals(TYPE_MACOSX)) {
        } else if(type.equals(TYPE_AWT)) {
        } else if(type.equals(TYPE_DEFAULT)) {
        }
    } */

    /** Sets the default NativeWindowFactory. */
    public static void setDefaultFactory(NativeWindowFactory factory) {
        defaultFactory = factory;
    }

    /** Gets the default NativeWindowFactory. */
    public static NativeWindowFactory getDefaultFactory() {
        return defaultFactory;
    }

    /**
     * Provides the system default {@link ToolkitLock}, a singleton instance.
     * <br>
     * This is a {@link com.jogamp.nativewindow.impl.jawt.JAWTToolkitLock}
     * in case of a <b>X11 system</b> <em>and</em> <b>AWT availability</b> and if
     * this factory has been initialized with <b>{@link #initSingleton(boolean) initSingleton(firstUIActionOnProcess==true)}</b>, <br>
     * otherwise {@link com.jogamp.nativewindow.impl.NullToolkitLock} is returned.
     */
    public static ToolkitLock getDefaultToolkitLock() {
        return getDefaultToolkitLock(getNativeWindowType(false));
    }

    /**
     * Provides the default {@link ToolkitLock} for <code>type</code>, a singleton instance.
     * <br>
     * This is a {@link com.jogamp.nativewindow.impl.jawt.JAWTToolkitLock}
     * in case of a <b>X11 type</b> or <b>AWT type / X11 system</b> <em>and</em> <b>AWT availability</b> and if
     * this factory has been initialized with <b>{@link #initSingleton(boolean) initSingleton(firstUIActionOnProcess==true)}</b>, <br>
     * otherwise {@link com.jogamp.nativewindow.impl.NullToolkitLock} is returned.
     */
    public static ToolkitLock getDefaultToolkitLock(String type) {
        if( isAWTAvailable() && !isFirstUIActionOnProcess() &&
            ( TYPE_X11 == type || TYPE_AWT == type && TYPE_X11 == getNativeWindowType(false) ) ) {
            return getAWTToolkitLock();
        }
        return NativeWindowFactoryImpl.getNullToolkitLock();
    }

    protected static ToolkitLock getAWTToolkitLock() {
        Object resO = ReflectionUtil.callMethod(null, jawtUtilGetJAWTToolkitMethod, null);

        if(resO instanceof ToolkitLock) {
            return (ToolkitLock) resO;
        } else {
            throw new RuntimeException("JAWTUtil.getJAWTToolkitLock() didn't return a ToolkitLock");
        }
    }

    public static ToolkitLock getNullToolkitLock() {
        return NativeWindowFactoryImpl.getNullToolkitLock();
    }
    /**
     * Creates the default {@link ToolkitLock} for <code>type</code> and <code>deviceHandle</code>.
     * <br>
     * This is a {@link com.jogamp.nativewindow.impl.jawt.x11.X11JAWTToolkitLock}
     * in case of a <b>X11 type</b> <em>and</em> <b>AWT availability</b> and if
     * this factory has been initialized with <b>{@link #initSingleton(boolean) initSingleton(firstUIActionOnProcess==true)}</b>, <br>
     * or a {@link com.jogamp.nativewindow.impl.x11.X11ToolkitLock}
     * in case of a <b>X11 type</b> <em>and</em> <b>no AWT availability</b> and if
     * this factory has been initialized with <b>{@link #initSingleton(boolean) initSingleton(firstUIActionOnProcess==true)}</b>, <br>
     * otherwise {@link com.jogamp.nativewindow.impl.NullToolkitLock} is returned.
     */
    public static ToolkitLock createDefaultToolkitLock(String type, long deviceHandle) {
        if( TYPE_X11 == type ) {
            if( 0== deviceHandle ) {
                throw new RuntimeException("JAWTUtil.createDefaultToolkitLock() called with NULL device but on X11");
            }
            if( !isFirstUIActionOnProcess() ) {
                if( isAWTAvailable() ) {
                    return createX11AWTToolkitLock(deviceHandle);
                } else {
                    return createX11ToolkitLock(deviceHandle);
                }
            }
        }
        return NativeWindowFactoryImpl.getNullToolkitLock();
    }

    public static ToolkitLock createDefaultToolkitLockNoAWT(String type, long deviceHandle) {
        if( TYPE_X11 == type ) {
            if( 0== deviceHandle ) {
                throw new RuntimeException("JAWTUtil.createDefaultToolkitLockNoAWT() called with NULL device but on X11");
            }
            if( !isFirstUIActionOnProcess() ) {
                return createX11ToolkitLock(deviceHandle);
            }
        }
        return NativeWindowFactoryImpl.getNullToolkitLock();
    }

    protected static ToolkitLock createX11AWTToolkitLock(long deviceHandle) {
        try {
            return (ToolkitLock) x11JAWTToolkitLockConstructor.newInstance(new Object[]{new Long(deviceHandle)});
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected static ToolkitLock createX11ToolkitLock(long deviceHandle) {
        try {
            return (ToolkitLock) x11ToolkitLockConstructor.newInstance(new Object[]{new Long(deviceHandle)});
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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
}
