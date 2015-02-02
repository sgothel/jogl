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

package com.jogamp.nativewindow;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.ReflectionUtil;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.DefaultGraphicsConfigurationFactoryImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    protected static final boolean DEBUG;

    private static class DeviceCapsType {
        public final Class<?> deviceType;
        public final Class<?> capsType;
        private final int hash32;

        public DeviceCapsType(final Class<?> deviceType, final Class<?> capsType) {
            this.deviceType = deviceType;
            this.capsType = capsType;

            // 31 * x == (x << 5) - x
            int hash32 = 31 + deviceType.hashCode();
            hash32 = ((hash32 << 5) - hash32) + capsType.hashCode();
            this.hash32 = hash32;
        }

        @Override
        public final int hashCode() {
            return hash32;
        }

        @Override
        public final boolean equals(final Object obj) {
            if(this == obj)  { return true; }
            if (obj instanceof DeviceCapsType) {
                final DeviceCapsType dct = (DeviceCapsType)obj;
                return deviceType == dct.deviceType && capsType == dct.capsType;
            }
            return false;
        }

        @Override
        public final String toString() {
            return "DeviceCapsType["+deviceType.getName()+", "+capsType.getName()+"]";
        }

    }

    private static final Map<DeviceCapsType, GraphicsConfigurationFactory> registeredFactories;
    private static final DeviceCapsType defaultDeviceCapsType;
    static boolean initialized = false;

    static {
        DEBUG = Debug.debug("GraphicsConfiguration");
        if(DEBUG) {
            System.err.println(Thread.currentThread().getName()+" - Info: GraphicsConfigurationFactory.<init>");
            // Thread.dumpStack();
        }
        registeredFactories = Collections.synchronizedMap(new HashMap<DeviceCapsType, GraphicsConfigurationFactory>());
        defaultDeviceCapsType = new DeviceCapsType(AbstractGraphicsDevice.class, CapabilitiesImmutable.class);
    }

    public static synchronized void initSingleton() {
        if(!initialized) {
            initialized = true;

            if(DEBUG) {
                System.err.println(Thread.currentThread().getName()+" - GraphicsConfigurationFactory.initSingleton()");
            }

            // Register the default no-op factory for arbitrary
            // AbstractGraphicsDevice implementations, including
            // AWTGraphicsDevice instances -- the OpenGL binding will take
            // care of handling AWTGraphicsDevices on X11 platforms (as
            // well as X11GraphicsDevices in non-AWT situations)
            registerFactory(defaultDeviceCapsType.deviceType, defaultDeviceCapsType.capsType, new DefaultGraphicsConfigurationFactoryImpl());

            if (NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(true)) {
                try {
                    ReflectionUtil.callStaticMethod("jogamp.nativewindow.x11.X11GraphicsConfigurationFactory",
                                                    "registerFactory", null, null, GraphicsConfigurationFactory.class.getClassLoader());
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
                if(NativeWindowFactory.isAWTAvailable()) {
                    try {
                        ReflectionUtil.callStaticMethod("jogamp.nativewindow.x11.awt.X11AWTGraphicsConfigurationFactory",
                                                        "registerFactory", null, null, GraphicsConfigurationFactory.class.getClassLoader());
                    } catch (final Exception e) { /* n/a */ }
                }
            }
        }
    }

    public static synchronized void shutdown() {
        if(initialized) {
            initialized = false;
            if(DEBUG) {
                System.err.println(Thread.currentThread().getName()+" - GraphicsConfigurationFactory.shutdown()");
            }
            registeredFactories.clear();
        }
    }

    protected static String getThreadName() {
        return Thread.currentThread().getName();
    }

    protected static String toHexString(final int val) {
        return "0x" + Integer.toHexString(val);
    }

    protected static String toHexString(final long val) {
        return "0x" + Long.toHexString(val);
    }

    /** Creates a new NativeWindowFactory instance. End users do not
        need to call this method. */
    protected GraphicsConfigurationFactory() {
    }

    /**
     * Returns the graphics configuration factory for use with the
     * given device and capability.
     *
     * @see #getFactory(Class, Class)
     */
    public static GraphicsConfigurationFactory getFactory(final AbstractGraphicsDevice device, final CapabilitiesImmutable caps) {
        if (device == null) {
            throw new IllegalArgumentException("null device");
        }
        if (caps == null) {
            throw new IllegalArgumentException("null caps");
        }
        return getFactory(device.getClass(), caps.getClass());
    }

    /**
     * Returns the graphics configuration factory for use with the
     * given device and capability class.
     * <p>
     * Note: Registered device types maybe classes or interfaces, where capabilities types are interfaces only.
     * </p>
     *
     * <p>
     * Pseudo code for finding a suitable factory is:
     * <pre>
        For-All devT := getTopDownDeviceTypes(deviceType)
            For-All capsT := getTopDownCapabilitiesTypes(capabilitiesType)
               f = factory.get(devT, capsT);
               if(f) { return f; }
            end
        end
     * </pre>
     * </p>
     *
     * @param deviceType the minimum capabilities class type accepted, must implement or extend {@link AbstractGraphicsDevice}
     * @param capabilitiesType the minimum capabilities class type accepted, must implement or extend {@link CapabilitiesImmutable}
     *
     * @throws IllegalArgumentException if the deviceType does not implement {@link AbstractGraphicsDevice} or
     *                                  capabilitiesType does not implement {@link CapabilitiesImmutable}
     */
    public static GraphicsConfigurationFactory getFactory(final Class<?> deviceType, final Class<?> capabilitiesType)
        throws IllegalArgumentException, NativeWindowException
    {
        if (!(defaultDeviceCapsType.deviceType.isAssignableFrom(deviceType))) {
            throw new IllegalArgumentException("Given class must implement AbstractGraphicsDevice");
        }
        if (!(defaultDeviceCapsType.capsType.isAssignableFrom(capabilitiesType))) {
            throw new IllegalArgumentException("Given capabilities class must implement CapabilitiesImmutable");
        }
        if(DEBUG) {
            ExceptionUtils.dumpStack(System.err);
            System.err.println("GraphicsConfigurationFactory.getFactory: "+deviceType.getName()+", "+capabilitiesType.getName());
            dumpFactories();
        }

        final List<Class<?>> deviceTypes = getAllAssignableClassesFrom(defaultDeviceCapsType.deviceType, deviceType, false);
        if(DEBUG) {
            System.err.println("GraphicsConfigurationFactory.getFactory() deviceTypes: " + deviceTypes);
        }
        final List<Class<?>> capabilitiesTypes = getAllAssignableClassesFrom(defaultDeviceCapsType.capsType, capabilitiesType, true);
        if(DEBUG) {
            System.err.println("GraphicsConfigurationFactory.getFactory() capabilitiesTypes: " + capabilitiesTypes);
        }
        for(int j=0; j<deviceTypes.size(); j++) {
            final Class<?> interfaceDevice = deviceTypes.get(j);
            for(int i=0; i<capabilitiesTypes.size(); i++) {
                final Class<?> interfaceCaps = capabilitiesTypes.get(i);
                final DeviceCapsType dct = new DeviceCapsType(interfaceDevice, interfaceCaps);
                final GraphicsConfigurationFactory factory = registeredFactories.get(dct);
                if (factory != null) {
                    if(DEBUG) {
                        System.err.println("GraphicsConfigurationFactory.getFactory() found "+dct+" -> "+factory);
                    }
                    return factory;
                }
            }
        }
        // Return the default
        final GraphicsConfigurationFactory factory = registeredFactories.get(defaultDeviceCapsType);
        if(DEBUG) {
            System.err.println("GraphicsConfigurationFactory.getFactory() DEFAULT "+defaultDeviceCapsType+" -> "+factory);
        }
        return factory;
    }
    private static ArrayList<Class<?>> getAllAssignableClassesFrom(final Class<?> superClassOrInterface, final Class<?> fromClass, final boolean interfacesOnly) {
        // Using a todo list avoiding a recursive loop!
        final ArrayList<Class<?>> inspectClasses  = new ArrayList<Class<?>>();
        final ArrayList<Class<?>> resolvedInterfaces = new ArrayList<Class<?>>();
        inspectClasses.add(fromClass);
        for(int j=0; j<inspectClasses.size(); j++) {
            final Class<?> clazz = inspectClasses.get(j);
            getAllAssignableClassesFrom(superClassOrInterface, clazz, interfacesOnly, resolvedInterfaces, inspectClasses);
        }
        return resolvedInterfaces;
    }
    private static void getAllAssignableClassesFrom(final Class<?> superClassOrInterface, final Class<?> fromClass, final boolean interfacesOnly, final List<Class<?>> resolvedInterfaces, final List<Class<?>> inspectClasses) {
        final ArrayList<Class<?>> types = new ArrayList<Class<?>>();
        if( superClassOrInterface.isAssignableFrom(fromClass) && !resolvedInterfaces.contains(fromClass)) {
            if( !interfacesOnly || fromClass.isInterface() ) {
                types.add(fromClass);
            }
        }
        types.addAll(Arrays.asList(fromClass.getInterfaces()));

        for(int i=0; i<types.size(); i++) {
            final Class<?> iface = types.get(i);
            if( superClassOrInterface.isAssignableFrom(iface) && !resolvedInterfaces.contains(iface) ) {
                resolvedInterfaces.add(iface);
                if( !superClassOrInterface.equals(iface) && !inspectClasses.contains(iface) ) {
                    inspectClasses.add(iface); // safe add to todo list, avoiding a recursive nature
                }
            }
        }
        final Class<?> parentClass = fromClass.getSuperclass();
        if( null != parentClass && superClassOrInterface.isAssignableFrom(parentClass) && !inspectClasses.contains(parentClass) ) {
            inspectClasses.add(parentClass); // safe add to todo list, avoiding a recursive nature
        }
    }
    private static void dumpFactories() {
        final Set<DeviceCapsType> dcts = registeredFactories.keySet();
        int i=0;
        for(final Iterator<DeviceCapsType> iter = dcts.iterator(); iter.hasNext(); ) {
            final DeviceCapsType dct = iter.next();
            System.err.println("Factory #"+i+": "+dct+" -> "+registeredFactories.get(dct));
            i++;
        }
    }

    /**
     * Registers a GraphicsConfigurationFactory handling
     * the given graphics device and capability class.
     * <p>
     * This does not need to be called by end users, only implementors of new
     * GraphicsConfigurationFactory subclasses.
     * </p>
     *
     * <p>
     * Note: Registered device types maybe classes or interfaces, where capabilities types are interfaces only.
     * </p>
     *
     * <p>See {@link #getFactory(Class, Class)} for a description of the find algorithm.</p>
     *
     * @param deviceType the minimum capabilities class type accepted, must implement or extend interface {@link AbstractGraphicsDevice}
     * @param capabilitiesType the minimum capabilities class type accepted, must extend interface {@link CapabilitiesImmutable}
     * @return the previous registered factory, or null if none
     * @throws IllegalArgumentException if the given class does not implement AbstractGraphicsDevice
     */
    protected static GraphicsConfigurationFactory registerFactory(final Class<?> abstractGraphicsDeviceImplementor, final Class<?> capabilitiesType, final GraphicsConfigurationFactory factory)
        throws IllegalArgumentException
    {
        if (!(defaultDeviceCapsType.deviceType.isAssignableFrom(abstractGraphicsDeviceImplementor))) {
            throw new IllegalArgumentException("Given device class must implement AbstractGraphicsDevice");
        }
        if (!(defaultDeviceCapsType.capsType.isAssignableFrom(capabilitiesType))) {
            throw new IllegalArgumentException("Given capabilities class must implement CapabilitiesImmutable");
        }
        final DeviceCapsType dct = new DeviceCapsType(abstractGraphicsDeviceImplementor, capabilitiesType);
        final GraphicsConfigurationFactory prevFactory;
        if(null == factory) {
            prevFactory = registeredFactories.remove(dct);
            if(DEBUG) {
                System.err.println("GraphicsConfigurationFactory.registerFactory() remove "+dct+
                                   ", deleting: "+prevFactory);
            }
        } else {
            prevFactory = registeredFactories.put(dct, factory);
            if(DEBUG) {
                System.err.println("GraphicsConfigurationFactory.registerFactory() put "+dct+" -> "+factory+
                                   ", overridding: "+prevFactory);
            }
        }
        return prevFactory;
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
     * com.jogamp.nativewindow.awt.AWTGraphicsDevice AWTGraphicsDevice} objects and returns {@link
     * com.jogamp.nativewindow.awt.AWTGraphicsConfiguration AWTGraphicsConfiguration} objects. On
     * X11 platforms where the AWT is not in use, it also accepts
     * {@link com.jogamp.nativewindow.x11.X11GraphicsDevice
     * X11GraphicsDevice} objects and returns {@link
     * com.jogamp.nativewindow.x11.X11GraphicsConfiguration
     * X11GraphicsConfiguration} objects.</P>
     *
     * @param capsChosen     the intermediate chosen capabilities to be refined by this implementation, may be equal to capsRequested
     * @param capsRequested  the original requested capabilities
     * @param chooser        the choosing implementation
     * @param screen         the referring Screen
     * @param nativeVisualID if not {@link VisualIDHolder#VID_UNDEFINED} it reflects a pre-chosen visualID of the native platform's windowing system.
     * @return               the complete GraphicsConfiguration
     *
     * @throws IllegalArgumentException if the data type of the passed
     *         AbstractGraphicsDevice is not supported by this
     *         NativeWindowFactory.
     * @throws NativeWindowException if any window system-specific errors caused
     *         the selection of the graphics configuration to fail.
     *
     * @see com.jogamp.nativewindow.GraphicsConfigurationFactory#chooseGraphicsConfiguration(Capabilities, CapabilitiesChooser, AbstractGraphicsScreen)
     * @see com.jogamp.nativewindow.DefaultGraphicsConfiguration#setChosenCapabilities(Capabilities caps)
     */
    public final AbstractGraphicsConfiguration
        chooseGraphicsConfiguration(final CapabilitiesImmutable capsChosen, final CapabilitiesImmutable capsRequested,
                                    final CapabilitiesChooser chooser,
                                    final AbstractGraphicsScreen screen, final int nativeVisualID)
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
        final AbstractGraphicsDevice device =  screen.getDevice();
        if(null==device) {
            throw new NativeWindowException("Screen's Device is null");
        }
        device.lock();
        try {
            return chooseGraphicsConfigurationImpl(capsChosen, capsRequested, chooser, screen, nativeVisualID);
        } finally {
            device.unlock();
        }
    }

    protected abstract AbstractGraphicsConfiguration
        chooseGraphicsConfigurationImpl(CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested,
                                        CapabilitiesChooser chooser, AbstractGraphicsScreen screen, int nativeVisualID)
        throws IllegalArgumentException, NativeWindowException;

}
