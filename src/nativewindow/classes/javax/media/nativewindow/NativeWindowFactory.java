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

import java.io.File;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.NativeWindowFactoryImpl;
import jogamp.nativewindow.ToolkitProperties;
import jogamp.nativewindow.ResourceToolkitLock;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.awt.AWTGraphicsDevice;
import com.jogamp.nativewindow.awt.AWTGraphicsScreen;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;

/** Provides a pluggable mechanism for arbitrary window toolkits to
    adapt their components to the {@link NativeWindow} interface,
    which provides a platform-independent mechanism of accessing the
    information required to perform operations like
    hardware-accelerated rendering using the OpenGL API. */

public abstract class NativeWindowFactory {
    protected static final boolean DEBUG;

    /** OpenKODE/EGL type, as retrieved with {@link #getNativeWindowType(boolean)}. String is canonical via {@link String#intern()}.*/
    public static final String TYPE_EGL = ".egl".intern();

    /** Microsoft Windows type, as retrieved with {@link #getNativeWindowType(boolean)}. String is canonical via {@link String#intern()}. */
    public static final String TYPE_WINDOWS = ".windows".intern();

    /** X11 type, as retrieved with {@link #getNativeWindowType(boolean)}. String is canonical via {@link String#intern()}. */
    public static final String TYPE_X11 = ".x11".intern();

    /** Broadcom VC IV/EGL type, as retrieved with {@link #getNativeWindowType(boolean)}. String is canonical via {@link String#intern()}. */
    public static final String TYPE_BCM_VC_IV = ".bcm.vc.iv".intern();

    /** Android/EGL type, as retrieved with {@link #getNativeWindowType(boolean)}. String is canonical via {@link String#intern()}.*/
    public static final String TYPE_ANDROID = ".android".intern();

    /** Mac OS X type, as retrieved with {@link #getNativeWindowType(boolean)}. String is canonical via {@link String#intern()}. */
    public static final String TYPE_MACOSX = ".macosx".intern();

    /** Generic AWT type, as retrieved with {@link #getNativeWindowType(boolean)}. String is canonical via {@link String#intern()}. */
    public static final String TYPE_AWT = ".awt".intern();

    /** Generic DEFAULT type, where platform implementation don't care, as retrieved with {@link #getNativeWindowType(boolean)}. String is canonical via {@link String#intern()}. */
    public static final String TYPE_DEFAULT = ".default".intern();

    private static final String nativeWindowingTypePure;   // canonical String via String.intern()
    private static final String nativeWindowingTypeCustom; // canonical String via String.intern()
    
    private static NativeWindowFactory defaultFactory;
    private static Map<Class<?>, NativeWindowFactory> registeredFactories;
    
    private static Class<?> nativeWindowClass;
    private static boolean isAWTAvailable;
    
    private static final String JAWTUtilClassName = "jogamp.nativewindow.jawt.JAWTUtil" ;
    /** {@link jogamp.nativewindow.x11.X11Util} implements {@link ToolkitProperties}. */
    private static final String X11UtilClassName = "jogamp.nativewindow.x11.X11Util";
    /** {@link jogamp.nativewindow.macosx.OSXUtil} implements {@link ToolkitProperties}. */
    private static final String OSXUtilClassName = "jogamp.nativewindow.macosx.OSXUtil";
    /** {@link jogamp.nativewindow.windows.GDIUtil} implements {@link ToolkitProperties}. */
    private static final String GDIClassName = "jogamp.nativewindow.windows.GDIUtil";
    
    private static ToolkitLock jawtUtilJAWTToolkitLock;
    
    private static boolean requiresToolkitLock;
    private static boolean desktopHasThreadingIssues;

    // Shutdown hook mechanism for the factory
    private static volatile boolean isJVMShuttingDown = false;
    private static final List<Runnable> customShutdownHooks = new ArrayList<Runnable>();
    
    /** Creates a new NativeWindowFactory instance. End users do not
        need to call this method. */
    protected NativeWindowFactory() {
    }

    private static final boolean guessBroadcomVCIV() {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            private final File vcliblocation = new File(
                    "/opt/vc/lib/libbcm_host.so");
                public Boolean run() {
                    if ( vcliblocation.isFile() ) {
                        return Boolean.TRUE;
                    }
                    return Boolean.FALSE;
                }
        } ).booleanValue();
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
              if( guessBroadcomVCIV() ) {
                return TYPE_BCM_VC_IV;
              }
              return TYPE_X11;
        }
    }

    static {
        final boolean[] _DEBUG = new boolean[] { false };
        final String[] _tmp = new String[] { null };
        
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                Platform.initSingleton(); // last resort ..
                _DEBUG[0] = Debug.debug("NativeWindow");
                _tmp[0] = Debug.getProperty("nativewindow.ws.name", true);
                Runtime.getRuntime().addShutdownHook(
                    new Thread(new Runnable() {
                                public void run() {
                                    NativeWindowFactory.shutdown(true);
                                } }, "NativeWindowFactory_ShutdownHook" ) ) ;                        
                return null;
            } } ) ;
        
        DEBUG = _DEBUG[0];
        if(DEBUG) {
            System.err.println(Thread.currentThread().getName()+" - Info: NativeWindowFactory.<init>");
            // Thread.dumpStack();
        }
        
        // Gather the windowing TK first
        nativeWindowingTypePure = _getNativeWindowingType();
        if(null==_tmp[0] || _tmp[0].length()==0) {
            nativeWindowingTypeCustom = nativeWindowingTypePure;
        } else {
            nativeWindowingTypeCustom = _tmp[0].intern(); // canonical representation
        }
    }

    static boolean initialized = false;

    private static void initSingletonNativeImpl(final ClassLoader cl) {
        final String clazzName;
        if( TYPE_X11 == nativeWindowingTypePure ) {
            clazzName = X11UtilClassName;
        } else if( TYPE_WINDOWS == nativeWindowingTypePure ) {
            clazzName = GDIClassName;
        } else if( TYPE_MACOSX == nativeWindowingTypePure ) {
            clazzName = OSXUtilClassName;
        } else {
            clazzName = null;
        }
        if( null != clazzName ) {
            ReflectionUtil.callStaticMethod(clazzName, "initSingleton", null, null, cl );
            
            final Boolean res1 = (Boolean) ReflectionUtil.callStaticMethod(clazzName, "requiresToolkitLock", null, null, cl);
            requiresToolkitLock = res1.booleanValue();
            final Boolean res2 = (Boolean) ReflectionUtil.callStaticMethod(clazzName, "hasThreadingIssues", null, null, cl);
            desktopHasThreadingIssues = res2.booleanValue();
        } else {            
            requiresToolkitLock = false;
            desktopHasThreadingIssues = false;
        }
    }

    /** Returns true if the JVM is shutting down, otherwise false. */ 
    public static final boolean isJVMShuttingDown() { return isJVMShuttingDown; }
    
    /** 
     * Add a custom shutdown hook to be performed at JVM shutdown before shutting down NativeWindowFactory instance.
     *  
     * @param head if true add runnable at the start, otherwise at the end
     * @param runnable runnable to be added.
     */
    public static void addCustomShutdownHook(boolean head, Runnable runnable) {
        synchronized( customShutdownHooks ) {
            if( !customShutdownHooks.contains( runnable ) ) {
                if( head ) {
                    customShutdownHooks.add(0, runnable);
                } else {
                    customShutdownHooks.add( runnable );
                }
            }
        }
    }

    /** 
     * Cleanup resources at JVM shutdown
     */
    public static synchronized void shutdown(boolean _isJVMShuttingDown) {
        isJVMShuttingDown = _isJVMShuttingDown;
        if(DEBUG) {
            System.err.println("NativeWindowFactory.shutdown() START: JVM Shutdown "+isJVMShuttingDown+", on thread "+Thread.currentThread().getName());
        }
        synchronized(customShutdownHooks) {
            final int cshCount = customShutdownHooks.size();
            for(int i=0; i < cshCount; i++) {
                try {
                    if( DEBUG ) { 
                        System.err.println("NativeWindowFactory.shutdown - customShutdownHook #"+(i+1)+"/"+cshCount);
                    }
                    customShutdownHooks.get(i).run();
                } catch(Throwable t) {
                    System.err.println("NativeWindowFactory.shutdown: Catched "+t.getClass().getName()+" during customShutdownHook #"+(i+1)+"/"+cshCount);
                    if( DEBUG ) { 
                        t.printStackTrace(); 
                    }
                }
            }
            customShutdownHooks.clear();
        }
        if(DEBUG) {
            System.err.println("NativeWindowFactory.shutdown(): Post customShutdownHook");
        }
        
        if(initialized) {
            initialized = false;
            if(null != registeredFactories) {
                registeredFactories.clear();
                registeredFactories = null;
            }
            GraphicsConfigurationFactory.shutdown();
        }
        
        shutdownNativeImpl(NativeWindowFactory.class.getClassLoader()); // always re-shutdown
        // SharedResourceToolkitLock.shutdown(DEBUG); // not used yet
        if(DEBUG) {
            System.err.println(Thread.currentThread().getName()+" - NativeWindowFactory.shutdown() END JVM Shutdown "+isJVMShuttingDown);
        }
    }
    
    private static void shutdownNativeImpl(final ClassLoader cl) {
        final String clazzName;
        if( TYPE_X11 == nativeWindowingTypePure ) {
            clazzName = X11UtilClassName;
        } else if( TYPE_WINDOWS == nativeWindowingTypePure ) {
            clazzName = GDIClassName;
        } else if( TYPE_MACOSX == nativeWindowingTypePure ) {
            clazzName = OSXUtilClassName;
        } else {
            clazzName = null;
        }
        if( null != clazzName ) {
            ReflectionUtil.callStaticMethod(clazzName, "shutdown", null, null, cl );
        }        
    }
    
    /**
     * Static one time initialization of this factory.<br>
     * This initialization method <b>must be called</b> once by the program or utilizing modules!
     */
    public static synchronized void initSingleton() {
        if(!initialized) {
            initialized = true;

            if(DEBUG) {
                System.err.println(Thread.currentThread().getName()+" - NativeWindowFactory.initSingleton()");
            }

            final ClassLoader cl = NativeWindowFactory.class.getClassLoader();

            isAWTAvailable = false; // may be set to true below

            if( Platform.AWT_AVAILABLE &&
                ReflectionUtil.isClassAvailable("com.jogamp.nativewindow.awt.AWTGraphicsDevice", cl) ) {
                
                Method[] jawtUtilMethods = AccessController.doPrivileged(new PrivilegedAction<Method[]>() {
                    public Method[] run() {
                        try {
                            Class<?> _jawtUtilClass = Class.forName(JAWTUtilClassName, true, NativeWindowFactory.class.getClassLoader());
                            Method jawtUtilIsHeadlessMethod = _jawtUtilClass.getDeclaredMethod("isHeadlessMode", (Class[])null);
                            jawtUtilIsHeadlessMethod.setAccessible(true);
                            Method jawtUtilInitMethod = _jawtUtilClass.getDeclaredMethod("initSingleton", (Class[])null);
                            jawtUtilInitMethod.setAccessible(true);
                            Method jawtUtilGetJAWTToolkitLockMethod = _jawtUtilClass.getDeclaredMethod("getJAWTToolkitLock", new Class[]{});
                            jawtUtilGetJAWTToolkitLockMethod.setAccessible(true);
                            return new Method[] { jawtUtilInitMethod, jawtUtilIsHeadlessMethod, jawtUtilGetJAWTToolkitLockMethod }; 
                        } catch (Exception e) {
                            if(DEBUG) {
                                e.printStackTrace();
                            }
                        }
                        return null;
                    }
                });
                if(null != jawtUtilMethods) {
                    final Method jawtUtilInitMethod = jawtUtilMethods[0];
                    final Method jawtUtilIsHeadlessMethod = jawtUtilMethods[1];
                    final Method jawtUtilGetJAWTToolkitLockMethod = jawtUtilMethods[2];
                    
                    ReflectionUtil.callMethod(null, jawtUtilInitMethod);

                    Object resO = ReflectionUtil.callMethod(null, jawtUtilIsHeadlessMethod);
                    if(resO instanceof Boolean) {
                        // AWT is only available in case all above classes are available
                        // and AWT is not int headless mode
                        isAWTAvailable = ((Boolean)resO).equals(Boolean.FALSE);
                    } else {
                        throw new RuntimeException("JAWTUtil.isHeadlessMode() didn't return a Boolean");
                    }
                    resO = ReflectionUtil.callMethod(null, jawtUtilGetJAWTToolkitLockMethod);            
                    if(resO instanceof ToolkitLock) {
                        jawtUtilJAWTToolkitLock = (ToolkitLock) resO;
                    } else {
                        throw new RuntimeException("JAWTUtil.getJAWTToolkitLock() didn't return a ToolkitLock");
                    }                    
                }
            }
            
            // X11 initialization after possible AWT initialization
            // This is performed post AWT initialization, allowing AWT to complete the same,
            // which may have been triggered before NativeWindow initialization. 
            // This way behavior is more uniforms across configurations (Applet/RCP, applications, ..). 
            initSingletonNativeImpl(cl);
            
            registeredFactories = Collections.synchronizedMap(new HashMap<Class<?>, NativeWindowFactory>());

            // register our default factory -> NativeWindow
            NativeWindowFactory factory = new NativeWindowFactoryImpl();
            nativeWindowClass = javax.media.nativewindow.NativeWindow.class;
            registerFactory(nativeWindowClass, factory);
            defaultFactory = factory;
        
            if ( isAWTAvailable ) {
                // register either our default factory or (if exist) the X11/AWT one -> AWT Component
                registerFactory(ReflectionUtil.getClass(ReflectionUtil.AWTNames.ComponentClass, false, cl), factory);
            }
            
            if(DEBUG) {
                System.err.println("NativeWindowFactory requiresToolkitLock "+requiresToolkitLock+", desktopHasThreadingIssues "+desktopHasThreadingIssues);
                System.err.println("NativeWindowFactory isAWTAvailable "+isAWTAvailable+", defaultFactory "+factory);
            }
            
            GraphicsConfigurationFactory.initSingleton();
        }
    }

    /** @return true if the underlying toolkit requires locking, otherwise false. */
    public static boolean requiresToolkitLock() {
        return requiresToolkitLock;
    }
    
    /** @return true if not headless, AWT Component and NativeWindow's AWT part available */
    public static boolean isAWTAvailable() { return isAWTAvailable; }

    /**
     * @param useCustom if false return the native value, if true return a custom value if set, otherwise fallback to the native value.
     * @return the native window type, e.g. {@link #TYPE_X11}, which is canonical via {@link String#intern()}. 
     *        Hence {@link String#equals(Object)} and <code>==</code> produce the same result.
     */
    public static String getNativeWindowType(boolean useCustom) {
        return useCustom?nativeWindowingTypeCustom:nativeWindowingTypePure;
    }

    /** Don't know if we shall add this factory here .. 
    public static AbstractGraphicsDevice createGraphicsDevice(String type, String connection, int unitID, long handle, ToolkitLock locker) {
        if(TYPE_EGL == type) {
            return new
        } else if(TYPE_X11 == type) {
        } else if(TYPE_WINDOWS == type) {
        } else if(TYPE_MACOSX == type)) {
        } else if(TYPE_AWT == type) {
        } else if(TYPE_DEFAULT == type) {
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
     * Returns the AWT {@link ToolkitLock} (JAWT based) if {@link #isAWTAvailable}, otherwise null.
     * <p>
     * The JAWT based {@link ToolkitLock} also locks the global lock,
     * which matters if the latter is required.
     * </p> 
     */ 
    public static ToolkitLock getAWTToolkitLock() {
        return jawtUtilJAWTToolkitLock;
    }

    public static ToolkitLock getNullToolkitLock() {
        return NativeWindowFactoryImpl.getNullToolkitLock();
    }
    
    /**
     * Provides the system default {@link ToolkitLock} for the default system windowing type.
     * @see #getNativeWindowType(boolean)
     * @see #getDefaultToolkitLock(java.lang.String)
     */
    public static ToolkitLock getDefaultToolkitLock() {
        return getDefaultToolkitLock(getNativeWindowType(false));
    }

    /**
     * Provides the default {@link ToolkitLock} for <code>type</code>.
     * <ul>
     *   <li> JAWT {@link ToolkitLock} if required and <code>type</code> is of {@link #TYPE_AWT} and AWT available,</li>
     *   <li> {@link jogamp.nativewindow.ResourceToolkitLock} if required, otherwise</li>
     *   <li> {@link jogamp.nativewindow.NullToolkitLock} </li>
     * </ul>
     */
    public static ToolkitLock getDefaultToolkitLock(String type) {
        if( requiresToolkitLock ) {
            if( TYPE_AWT == type && isAWTAvailable() ) {
                return getAWTToolkitLock();
            }
            return ResourceToolkitLock.create();
        }
        return NativeWindowFactoryImpl.getNullToolkitLock();
    }

    /**
     * Provides the default {@link ToolkitLock} for <code>type</code> and <code>deviceHandle</code>.
     * <ul>
     *   <li> JAWT {@link ToolkitLock} if required and <code>type</code> is of {@link #TYPE_AWT} and AWT available,</li>
     *   <li> {@link jogamp.nativewindow.ResourceToolkitLock} if required, otherwise</li>
     *   <li> {@link jogamp.nativewindow.NullToolkitLock} </li>
     * </ul>
     */
    public static ToolkitLock getDefaultToolkitLock(String type, long deviceHandle) {
        if( requiresToolkitLock ) {
            if( TYPE_AWT == type && isAWTAvailable() ) {
                return getAWTToolkitLock();
            }
            return ResourceToolkitLock.create();
        }
        return NativeWindowFactoryImpl.getNullToolkitLock();
    }
    
    /**
     * @param device
     * @param screen -1 is default screen of the given device, e.g. maybe 0 or determined by native API. >= 0 is specific screen
     * @return newly created AbstractGraphicsScreen matching device's native type
     */
    public static AbstractGraphicsScreen createScreen(AbstractGraphicsDevice device, int screen) {
        final String type = device.getType();
        if( TYPE_X11 == type ) {
            final X11GraphicsDevice x11Device = (X11GraphicsDevice)device;
            if(0 > screen) {
                screen = x11Device.getDefaultScreen();
            }
            return new X11GraphicsScreen(x11Device, screen);
        }
        if(0 > screen) {
            screen = 0; // FIXME: Needs native API utilization
        }
        if( TYPE_AWT == type ) {
            final AWTGraphicsDevice awtDevice = (AWTGraphicsDevice) device;
            return new AWTGraphicsScreen(awtDevice);
        }
        return new DefaultGraphicsScreen(device, screen);
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
    
    /**
     * Returns true if the given visualID is valid for further processing, i.e. OpenGL usage,
     * otherwise return false.
     * <p>
     * On certain platforms, i.e. X11, a valid visualID is required at window creation.
     * Other platforms may determine it later on, e.g. OSX and Windows. </p>
     * <p>
     * If the visualID is {@link VisualIDHolder#VID_UNDEFINED} and the platform requires it
     * at creation time (see above), it is not valid for further processing.
     * </p>
     */
    public static boolean isNativeVisualIDValidForProcessing(int visualID) {
        return NativeWindowFactory.TYPE_X11 != NativeWindowFactory.getNativeWindowType(false) || 
               VisualIDHolder.VID_UNDEFINED != visualID ;
    }
        
}
