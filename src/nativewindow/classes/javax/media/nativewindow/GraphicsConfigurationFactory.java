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

import com.jogamp.common.util.ReflectionUtil;
import jogamp.nativewindow.Debug;
import jogamp.nativewindow.DefaultGraphicsConfigurationFactoryImpl;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides the mechanism by which the graphics configuration for a
 * window can be chosen before the window is created. The graphics 
 * configuration decides parameters related to hardware accelerated rendering such
 * as the OpenGL pixel format. <br>
 * On some window systems (EGL/OpenKODE and X11 in particular) it is necessary to
 * choose the graphics configuration early at window creation time. <br>
 * Note that the selection of the graphics configuration is an algorithm which does not have
 * strong dependencies on the particular Java window toolkit in use
 * (e.g., AWT) and therefore it is strongly desirable to factor this
 * functionality out of the core {@link NativeWindowFactory} so that
 * new window toolkits can replace just the {@link
 * NativeWindowFactory} and reuse the graphics configuration selection
 * algorithm provided by, for example, an OpenGL binding.
 */

public abstract class GraphicsConfigurationFactory {
    protected static final boolean DEBUG = Debug.debug("GraphicsConfiguration");

    private static Map<Class<?>, GraphicsConfigurationFactory> registeredFactories =
        Collections.synchronizedMap(new HashMap<Class<?>, GraphicsConfigurationFactory>());
    private static Class<?> abstractGraphicsDeviceClass;

    static {
        abstractGraphicsDeviceClass = javax.media.nativewindow.AbstractGraphicsDevice.class;
        
        // Register the default no-op factory for arbitrary
        // AbstractGraphicsDevice implementations, including
        // AWTGraphicsDevice instances -- the OpenGL binding will take
        // care of handling AWTGraphicsDevices on X11 platforms (as
        // well as X11GraphicsDevices in non-AWT situations)
        registerFactory(abstractGraphicsDeviceClass, new DefaultGraphicsConfigurationFactoryImpl());
        
        if (NativeWindowFactory.TYPE_X11.equals(NativeWindowFactory.getNativeWindowType(true))) {
            try {
                ReflectionUtil.callStaticMethod("jogamp.nativewindow.x11.X11GraphicsConfigurationFactory", 
                                                "registerFactory", null, null, GraphicsConfigurationFactory.class.getClassLoader());                
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if(NativeWindowFactory.isAWTAvailable()) {
                try {
                    ReflectionUtil.callStaticMethod("jogamp.nativewindow.x11.awt.X11AWTGraphicsConfigurationFactory", 
                                                    "registerFactory", null, null, GraphicsConfigurationFactory.class.getClassLoader());                
                } catch (Exception e) { /* n/a */ }
            }
        }
    }

    protected static String getThreadName() {
        return Thread.currentThread().getName();
    }

    protected static String toHexString(int val) {
        return "0x" + Integer.toHexString(val);
    }

    protected static String toHexString(long val) {
        return "0x" + Long.toHexString(val);
    }

    /** Creates a new NativeWindowFactory instance. End users do not
        need to call this method. */
    protected GraphicsConfigurationFactory() {
    }

    /** Returns the factory for use with the given type of
        AbstractGraphicsDevice. */
    public static GraphicsConfigurationFactory getFactory(AbstractGraphicsDevice device) {
        if (device == null) {
            return getFactory(AbstractGraphicsDevice.class);
        }
        return getFactory(device.getClass());
    }

    /**
     * Returns the graphics configuration factory for use with the
     * given class, which must implement the {@link
     * AbstractGraphicsDevice} interface.
     *
     * @throws IllegalArgumentException if the given class does not implement AbstractGraphicsDevice
     */
    public static GraphicsConfigurationFactory getFactory(Class<?> abstractGraphicsDeviceImplementor)
        throws IllegalArgumentException, NativeWindowException
    {
        if (!(abstractGraphicsDeviceClass.isAssignableFrom(abstractGraphicsDeviceImplementor))) {
            throw new IllegalArgumentException("Given class must implement AbstractGraphicsDevice");
        }

        GraphicsConfigurationFactory factory = null;
        Class<?> clazz = abstractGraphicsDeviceImplementor;
        while (clazz != null) {
            factory = registeredFactories.get(clazz);
            if (factory != null) {
                if(DEBUG) {
                    System.err.println("GraphicsConfigurationFactory.getFactory() "+abstractGraphicsDeviceImplementor+" -> "+factory);
                }
                return factory;
            }
            clazz = clazz.getSuperclass();
        }
        // Return the default
        factory = registeredFactories.get(abstractGraphicsDeviceClass);
        if(DEBUG) {
            System.err.println("GraphicsConfigurationFactory.getFactory() DEFAULT "+abstractGraphicsDeviceClass+" -> "+factory);
        }
        return factory;
    }

    /** Registers a GraphicsConfigurationFactory handling graphics
     * device objects of the given class. This does not need to be
     * called by end users, only implementors of new
     * GraphicsConfigurationFactory subclasses.
     *
     * @throws IllegalArgumentException if the given class does not implement AbstractGraphicsDevice
     */
    protected static void registerFactory(Class<?> abstractGraphicsDeviceImplementor, GraphicsConfigurationFactory factory)
        throws IllegalArgumentException
    {
        if (!(abstractGraphicsDeviceClass.isAssignableFrom(abstractGraphicsDeviceImplementor))) {
            throw new IllegalArgumentException("Given class must implement AbstractGraphicsDevice");
        }
        if(DEBUG) {
            System.err.println("GraphicsConfigurationFactory.registerFactory() "+abstractGraphicsDeviceImplementor+" -> "+factory);
        }
        registeredFactories.put(abstractGraphicsDeviceImplementor, factory);
    }

    /**
     * <P> Selects a graphics configuration on the specified graphics
     * device compatible with the supplied {@link Capabilities}. Some
     * platforms (e.g.: X11, EGL, KD) require the graphics configuration
     * to be specified when the native window is created. 
     * These architectures have seperated their device, screen, window and drawable
     * context and hence are capable of quering the capabilities for each screen.
     * A fully established window is not required.</P>
     *
     * <P>Other platforms (e.g. Windows, MacOSX) don't offer the mentioned seperation
     * and hence need a fully established window and it's drawable.
     * Here the validation of the capabilities is performed later.
     * In this case, the AbstractGraphicsConfiguration implementation 
     * must allow an overwrite of the Capabilites, for example
     * {@link DefaultGraphicsConfiguration#setChosenCapabilities DefaultGraphicsConfiguration.setChosenCapabilities(..)}.
     * </P>
     *
     * <P>
     * This method is mainly intended to be both used and implemented by the
     * OpenGL binding.</P>
     *
     * <P> The concrete data type of the passed graphics device and
     * returned graphics configuration must be specified in the
     * documentation binding this particular API to the underlying
     * window toolkit. The Reference Implementation accepts {@link
     * javax.media.nativewindow.awt.AWTGraphicsDevice AWTGraphicsDevice} objects and returns {@link
     * javax.media.nativewindow.awt.AWTGraphicsConfiguration AWTGraphicsConfiguration} objects. On
     * X11 platforms where the AWT is not in use, it also accepts
     * {@link javax.media.nativewindow.x11.X11GraphicsDevice
     * X11GraphicsDevice} objects and returns {@link
     * javax.media.nativewindow.x11.X11GraphicsConfiguration
     * X11GraphicsConfiguration} objects.</P>
     *
     * @param capsChosen     the intermediate chosen capabilities to be refined by this implementation, may be equal to capsRequested
     * @param capsRequested  the original requested capabilities
     * @param chooser        the choosing implementation
     * @param screen         the referring Screen
     * @return               the complete GraphicsConfiguration
     *
     * @throws IllegalArgumentException if the data type of the passed
     *         AbstractGraphicsDevice is not supported by this
     *         NativeWindowFactory.
     * @throws NativeWindowException if any window system-specific errors caused
     *         the selection of the graphics configuration to fail.
     *
     * @see javax.media.nativewindow.GraphicsConfigurationFactory#chooseGraphicsConfiguration(Capabilities, CapabilitiesChooser, AbstractGraphicsScreen)
     * @see javax.media.nativewindow.DefaultGraphicsConfiguration#setChosenCapabilities(Capabilities caps)
     */
    public final AbstractGraphicsConfiguration
        chooseGraphicsConfiguration(CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested,
                                    CapabilitiesChooser chooser,
                                    AbstractGraphicsScreen screen)
        throws IllegalArgumentException, NativeWindowException {
        if(null==capsChosen) {
            throw new NativeWindowException("Chosen Capabilities are null");
        }
        if(null==capsRequested) {
            throw new NativeWindowException("Requested Capabilities are null");
        }
        if(null==screen) {
            throw new NativeWindowException("Screen is null");
        }
        AbstractGraphicsDevice device =  screen.getDevice();
        if(null==device) {
            throw new NativeWindowException("Screen's Device is null");
        }
        device.lock();
        try {
            return chooseGraphicsConfigurationImpl(capsChosen, capsRequested, chooser, screen);
        } finally {
            device.unlock();
        }
    }

    protected abstract AbstractGraphicsConfiguration
        chooseGraphicsConfigurationImpl(CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested,
                                        CapabilitiesChooser chooser, AbstractGraphicsScreen screen)
        throws IllegalArgumentException, NativeWindowException;

}
