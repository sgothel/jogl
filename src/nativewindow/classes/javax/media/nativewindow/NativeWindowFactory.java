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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.NativeWindowFactoryImpl;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.ReflectionUtil;

/** Provides a pluggable mechanism for arbitrary window toolkits to
    adapt their components to the {@link NativeWindow} interface,
    which provides a platform-independent mechanism of accessing the
    information required to perform operations like
    hardware-accelerated rendering using the OpenGL API. */

public abstract class NativeWindowFactory {
    protected static final boolean DEBUG;

    /** OpenKODE/EGL type, as retrieved with {@link #getNativeWindowType(boolean)}*/
    public static final String TYPE_EGL = "EGL";

    /** Microsoft Windows type, as retrieved with {@link #getNativeWindowType(boolean)} */
    public static final String TYPE_WINDOWS = "Windows";

    /** X11 type, as retrieved with {@link #getNativeWindowType(boolean)} */
    public static final String TYPE_X11 = "X11";

    /** Android/EGL type, as retrieved with {@link #getNativeWindowType(boolean)}*/
    public static final String TYPE_ANDROID = "ANDROID";

    /** Mac OS X type, as retrieved with {@link #getNativeWindowType(boolean)} */
    public static final String TYPE_MACOSX = "MacOSX";

    /** Generic AWT type, as retrieved with {@link #getNativeWindowType(boolean)} */
    public static final String TYPE_AWT = "AWT";

    /** Generic DEFAULT type, where platform implementation don't care, as retrieved with {@link #getNativeWindowType(boolean)} */
    public static final String TYPE_DEFAULT = "default";

    private static NativeWindowFactory defaultFactory;
    private static Map<Class<?>, NativeWindowFactory> registeredFactories;
    
    private static Class<?> nativeWindowClass;
    private static String nativeWindowingTypePure;
    private static String nativeWindowingTypeCustom;
    private static boolean isAWTAvailable;
    
    private static final String JAWTUtilClassName = "jogamp.nativewindow.jawt.JAWTUtil" ;
    private static final String X11UtilClassName = "jogamp.nativewindow.x11.X11Util";
    private static final String OSXUtilClassName = "jogamp.nativewindow.macosx.OSXUtil";
    private static final String GDIClassName = "jogamp.nativewindow.windows.GDIUtil";
    
    private static Class<?>  jawtUtilClass;
    private static Method jawtUtilGetJAWTToolkitMethod;
    private static Method jawtUtilInitMethod;
    
    public static final String AWTComponentClassName = "java.awt.Component" ;
    public static final String X11JAWTToolkitLockClassName = "jogamp.nativewindow.jawt.x11.X11JAWTToolkitLock" ;
    public static final String X11ToolkitLockClassName = "jogamp.nativewindow.x11.X11ToolkitLock" ;
    
    private static Class<?>  x11JAWTToolkitLockClass;
    private static Constructor<?> x11JAWTToolkitLockConstructor;
    private static Class<?>  x11ToolkitLockClass;
    private static Constructor<?> x11ToolkitLockConstructor;
    private static boolean isFirstUIActionOnProcess;
    private static boolean requiresToolkitLock;

    /** Creates a new NativeWindowFactory instance. End users do not
        need to call this method. */
    protected NativeWindowFactory() {
    }

    private static String _getNativeWindowingType() {
        switch(Platform.OS_TYPE) {
            case ANDROID:
              return TYPE_ANDROID;
            case MACOS:
              return TYPE_MACOSX;
            case WINDOWS:
              return TYPE_WINDOWS;                
            case OPENKODE:
              return TYPE_EGL;
                
            case LINUX:
            case FREEBSD:
            case SUNOS:
            case HPUX:
            default:
              return TYPE_X11;
        }
    }

    static {
        Platform.initSingleton();
        DEBUG = Debug.debug("NativeWindow");
        if(DEBUG) {
            Throwable td = new Throwable(Thread.currentThread().getName()+" - Info: NativeWindowFactory.<init>");
            td.printStackTrace();
        }
    }

    static boolean initialized = false;

    private static void initSingletonNativeImpl(final boolean firstUIActionOnProcess, final ClassLoader cl) {
        isFirstUIActionOnProcess = firstUIActionOnProcess;
        
        final String clazzName;
        if( TYPE_X11.equals(nativeWindowingTypePure) ) {
            clazzName = X11UtilClassName;
        } else if( TYPE_WINDOWS.equals(nativeWindowingTypePure) ) {
            clazzName = GDIClassName;
        } else if( TYPE_MACOSX.equals(nativeWindowingTypePure) ) {
            clazzName = OSXUtilClassName;
        } else {
            clazzName = null;
        }
        if( null != clazzName ) {
            ReflectionUtil.callStaticMethod(clazzName, "initSingleton",
                                            new Class[] { boolean.class },
                                            new Object[] { new Boolean(firstUIActionOnProcess) }, cl );
            
            final Boolean res = (Boolean) ReflectionUtil.callStaticMethod(clazzName, "requiresToolkitLock", null, null, cl);
            requiresToolkitLock = res.booleanValue();             
        } else {            
            requiresToolkitLock = false;
        }        
    }

    /**
     * Static one time initialization of this factory.<br>
     * This initialization method <b>must be called</b> once by the program or utilizing modules!
     * <p>
     * The parameter <code>firstUIActionOnProcess</code> has an impact on concurrent locking:
     * <ul>
     *   <li> {@link #getDefaultToolkitLock() getDefaultToolkitLock() }</li>
     *   <li> {@link #getDefaultToolkitLock(java.lang.String) getDefaultToolkitLock(type) }</li>
     *   <li> {@link #createDefaultToolkitLock(java.lang.String, long) createDefaultToolkitLock(type, dpyHandle) }</li>
     *   <li> {@link #createDefaultToolkitLockNoAWT(java.lang.String, long) createDefaultToolkitLockNoAWT(type, dpyHandle) }</li>
     * </ul>
     * </p>
     * @param firstUIActionOnProcess Should be <code>true</code> if called before the first UI action of the running program,
     * otherwise <code>false</code>.
     */
    public static synchronized void initSingleton(final boolean firstUIActionOnProcess) {
        if(!initialized) {
            initialized = true;

            if(DEBUG) {
                System.err.println(Thread.currentThread().getName()+" - NativeWindowFactory.initSingleton("+firstUIActionOnProcess+")");
            }

            final ClassLoader cl = NativeWindowFactory.class.getClassLoader();

            // Gather the windowing OS first
            AccessControlContext acc = AccessController.getContext();
            nativeWindowingTypePure = _getNativeWindowingType();
            String tmp = Debug.getProperty("nativewindow.ws.name", true, acc);
            if(null==tmp || tmp.length()==0) {
                nativeWindowingTypeCustom = nativeWindowingTypePure;
            } else {
                nativeWindowingTypeCustom = tmp;
            }

            if(firstUIActionOnProcess) {
                // X11 initialization before possible AWT initialization
                initSingletonNativeImpl(true, cl);
            }            
            isAWTAvailable = false; // may be set to true below

            if( !Debug.getBooleanProperty("java.awt.headless", true, acc) &&
                ReflectionUtil.isClassAvailable(AWTComponentClassName, cl) &&
                ReflectionUtil.isClassAvailable("javax.media.nativewindow.awt.AWTGraphicsDevice", cl) ) {

                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        try {
                            jawtUtilClass = Class.forName(JAWTUtilClassName, true, NativeWindowFactory.class.getClassLoader());
                            jawtUtilInitMethod = jawtUtilClass.getDeclaredMethod("initSingleton", (Class[])null);
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
                    ReflectionUtil.callMethod(null, jawtUtilInitMethod);

                    Object resO = ReflectionUtil.callStaticMethod(JAWTUtilClassName, "isHeadlessMode", null, null, cl );
                    if(resO instanceof Boolean) {
                        // AWT is only available in case all above classes are available
                        // and AWT is not int headless mode
                        isAWTAvailable = ((Boolean)resO).equals(Boolean.FALSE);
                    }
                }
            }
            if(!firstUIActionOnProcess) {
                // X11 initialization after possible AWT initialization
                initSingletonNativeImpl(false, cl);
            }
            registeredFactories = Collections.synchronizedMap(new HashMap<Class<?>, NativeWindowFactory>());

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
                System.err.println("NativeWindowFactory requiresToolkitLock "+requiresToolkitLock);
                System.err.println("NativeWindowFactory isAWTAvailable "+isAWTAvailable+", defaultFactory "+factory);
            }
        }
    }

    public static synchronized void shutdown() {
        if(initialized) {
            initialized = false;
            if(DEBUG) {
                System.err.println(Thread.currentThread().getName()+" - NativeWindowFactory.shutdown() START");                
            }
            registeredFactories.clear();
            registeredFactories = null;
            // X11Util.shutdown(..) already called via GLDrawableFactory.shutdown() ..
            if(DEBUG) {
                System.err.println(Thread.currentThread().getName()+" - NativeWindowFactory.shutdown() END");                
            }
        }
    }
    
    /** @return true if initialized with <b>{@link #initSingleton(boolean) initSingleton(firstUIActionOnProcess==true)}</b>,
        otherwise false. */
    public static boolean isFirstUIActionOnProcess() {
        return isFirstUIActionOnProcess;
    }

    /** @return true if the underlying toolkit requires locking, otherwise false. */
    public static boolean requiresToolkitLock() {
        return requiresToolkitLock;
    }    
    
    /** @return true if not headless, AWT Component and NativeWindow's AWT part available */
    public static boolean isAWTAvailable() { return isAWTAvailable; }

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
     * @see #getDefaultToolkitLock(java.lang.String)
     */
    public static ToolkitLock getDefaultToolkitLock() {
        return getDefaultToolkitLock(getNativeWindowType(false));
    }

    /**
     * Provides the default {@link ToolkitLock} for <code>type</code>, a singleton instance.
     * <br>
     * <ul>
     *   <li> If {@link #initSingleton(boolean) initSingleton( <b>firstUIActionOnProcess := false</b> )} </li>
     *   <ul>
     *     <li>If <b>AWT-type</b> and <b>native-X11-type</b> and <b>AWT-available</b></li>
     *       <ul>
     *         <li> return {@link jogamp.nativewindow.jawt.JAWTToolkitLock} </li>
     *       </ul>
     *   </ul>
     *   <li> Otherwise return {@link jogamp.nativewindow.NullToolkitLock} </li>
     * </ul>
     */
    public static ToolkitLock getDefaultToolkitLock(String type) {
        if( requiresToolkitLock() ) {
            if( TYPE_AWT == type && TYPE_X11 == getNativeWindowType(false) && isAWTAvailable() ) {
                return getAWTToolkitLock();
            }
        }
        return NativeWindowFactoryImpl.getNullToolkitLock();
    }

    private static ToolkitLock getAWTToolkitLock() {
        Object resO = ReflectionUtil.callMethod(null, jawtUtilGetJAWTToolkitMethod);

        if(DEBUG) {
            System.err.println("NativeWindowFactory.getAWTToolkitLock()");
            Thread.dumpStack();
        }            
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
     * <ul>
     *   <li> If {@link #initSingleton(boolean) initSingleton( <b>firstUIActionOnProcess := false</b> )} </li>
     *   <ul>
     *     <li>If <b>X11 type</b> </li>
     *     <ul>
     *       <li> return {@link jogamp.nativewindow.x11.X11ToolkitLock} </li>
     *     </ul>
     *   </ul>
     *   <li> Otherwise return {@link jogamp.nativewindow.NullToolkitLock} </li>
     * </ul>
     */
    public static ToolkitLock createDefaultToolkitLock(String type, long deviceHandle) {
        if( requiresToolkitLock() ) {
            if( TYPE_X11 == type ) {
                if( 0== deviceHandle ) {
                    throw new RuntimeException("JAWTUtil.createDefaultToolkitLock() called with NULL device but on X11");
                }
                return createX11ToolkitLock(deviceHandle);
            }
        }
        return NativeWindowFactoryImpl.getNullToolkitLock();
    }
    
    /**
     * Creates the default {@link ToolkitLock} for <code>type</code> and <code>deviceHandle</code>.
     * <br>
     * <ul>
     *   <li> If {@link #initSingleton(boolean) initSingleton( <b>firstUIActionOnProcess := false</b> )} </li>
     *   <ul>
     *     <li>If <b>X11 type</b> </li>
     *     <ul>
     *       <li> If <b>shared-AWT-type</b> and <b>AWT available</b> </li>
     *       <ul>
     *         <li> return {@link jogamp.nativewindow.jawt.x11.X11JAWTToolkitLock} </li>
     *       </ul>
     *       <li> else return {@link jogamp.nativewindow.x11.X11ToolkitLock} </li>
     *     </ul>
     *   </ul>
     *   <li> Otherwise return {@link jogamp.nativewindow.NullToolkitLock} </li>
     * </ul>
     */
    public static ToolkitLock createDefaultToolkitLock(String type, String sharedType, long deviceHandle) {
        if( requiresToolkitLock() ) {
            if( TYPE_X11 == type ) {
                if( 0== deviceHandle ) {
                    throw new RuntimeException("JAWTUtil.createDefaultToolkitLock() called with NULL device but on X11");
                }
                if( TYPE_AWT == sharedType && isAWTAvailable() ) {
                    return createX11AWTToolkitLock(deviceHandle);
                }
                return createX11ToolkitLock(deviceHandle);
            }
        }
        return NativeWindowFactoryImpl.getNullToolkitLock();
    }

    protected static ToolkitLock createX11AWTToolkitLock(long deviceHandle) {
        try {
            if(DEBUG) {
                System.err.println("NativeWindowFactory.createX11AWTToolkitLock(0x"+Long.toHexString(deviceHandle)+")");
                Thread.dumpStack();
            }            
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
    public static NativeWindowFactory getFactory(Class<?> windowClass) throws IllegalArgumentException {
        if (nativeWindowClass.isAssignableFrom(windowClass)) {
            return registeredFactories.get(nativeWindowClass);
        }
        Class<?> clazz = windowClass;
        while (clazz != null) {
            NativeWindowFactory factory = registeredFactories.get(clazz);
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
    protected static void registerFactory(Class<?> windowClass, NativeWindowFactory factory) {
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
    
    /**
     * Returns the {@link OffscreenLayerSurface} instance of this {@link NativeSurface}.
     * <p>
     * In case this surface is a {@link NativeWindow}, we traverse from the given surface 
     * up to root until an implementation of {@link OffscreenLayerSurface} is found.
     * In case <code>ifEnabled</code> is true, the surface must also implement {@link OffscreenLayerOption}
     * where {@link OffscreenLayerOption#isOffscreenLayerSurfaceEnabled()} is <code>true</code>.  
     * </p>
     * 
     * @param surface The surface to query.
     * @param ifEnabled If true, only return the enabled {@link OffscreenLayerSurface}, see {@link OffscreenLayerOption#isOffscreenLayerSurfaceEnabled()}. 
     * @return
     */
    public static OffscreenLayerSurface getOffscreenLayerSurface(NativeSurface surface, boolean ifEnabled) {
        if(surface instanceof OffscreenLayerSurface && 
           ( !ifEnabled || surface instanceof OffscreenLayerOption ) ) {
            final OffscreenLayerSurface ols = (OffscreenLayerSurface) surface;
            return ( !ifEnabled || ((OffscreenLayerOption)ols).isOffscreenLayerSurfaceEnabled() ) ? ols : null;
        }
        if(surface instanceof NativeWindow) {
            NativeWindow nw = ((NativeWindow) surface).getParent();
            while(null != nw) {
                if(nw instanceof OffscreenLayerSurface &&
                   ( !ifEnabled || nw instanceof OffscreenLayerOption ) ) {
                    final OffscreenLayerSurface ols = (OffscreenLayerSurface) nw;
                    return ( !ifEnabled || ((OffscreenLayerOption)ols).isOffscreenLayerSurfaceEnabled() ) ? ols : null;
                }
                nw = nw.getParent();                
            }
        }
        return null;            
    }
}
