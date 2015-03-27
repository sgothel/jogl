/**
 * Copyright 2014 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl.util.stereo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.NativeWindowFactory;

/**
 * Platform agnostic {@link StereoDevice} factory.
 * <p>
 * To implement a new {@link StereoDevice}, the following interfaces/classes must be implemented:
 * <ul>
 *   <li>{@link StereoDeviceFactory}</li>
 *   <li>{@link StereoDevice}</li>
 *   <li>{@link StereoDeviceRenderer}</li>
 * </ul>
 * </p>
 */
public abstract class StereoDeviceFactory {
    private static final String OVRStereoDeviceClazzName = "jogamp.opengl.oculusvr.OVRStereoDeviceFactory";
    private static final String GenericStereoDeviceClazzName = com.jogamp.opengl.util.stereo.generic.GenericStereoDeviceFactory.class.getName();
    private static final String isAvailableMethodName = "isAvailable";
    static {
        NativeWindowFactory.addCustomShutdownHook(false /* head */, new Runnable() {
           @Override
           public void run() {
               shutdownAll();
           }
        });
    }

    /** {@link StereoDevice} type used for {@link StereoDeviceFactory#createFactory(DeviceType) createFactory(type)}. */
    public static enum DeviceType {
        /**
         * Auto selection of device in the following order:
         * <ol>
         *   <li>{@link DeviceType#OculusVR}</li>
         *   <li>{@link DeviceType#Generic}</li>
         * </ol>
         */
        Default,
        /**
         * Generic software implementation.
         */
        Generic,
        /**
         * OculusVR DK1 implementation.
         */
        OculusVR,
        /**
         * OculusVR DK2 implementation.
         */
        OculusVR_DK2
    };

    public static StereoDeviceFactory createDefaultFactory() {
        final ClassLoader cl = StereoDeviceFactory.class.getClassLoader();
        StereoDeviceFactory sink = createFactory(cl, OVRStereoDeviceClazzName);
        if( null == sink ) {
            sink = createFactory(cl, GenericStereoDeviceClazzName);
        }
        return sink;
    }

    public static StereoDeviceFactory createFactory(final DeviceType type) {
        final String className;
        switch( type ) {
            case Default: return createDefaultFactory();
            case Generic: className = GenericStereoDeviceClazzName; break;
            case OculusVR: className = OVRStereoDeviceClazzName; break;
            default: throw new InternalError("Unsupported type "+type);
        }
        final ClassLoader cl = StereoDeviceFactory.class.getClassLoader();
        return createFactory(cl, className);
    }

    public static StereoDeviceFactory createFactory(final ClassLoader cl, final String implName) {
        StereoDeviceFactory res = null;
        try {
            if(((Boolean)ReflectionUtil.callStaticMethod(implName, isAvailableMethodName, null, null, cl)).booleanValue()) {
                res = (StereoDeviceFactory) ReflectionUtil.createInstance(implName, cl);
            }
        } catch (final Throwable t) { if(StereoDevice.DEBUG) { System.err.println("Caught "+t.getClass().getName()+": "+t.getMessage()); t.printStackTrace(); } }
        if( null != res ) {
            addFactory2List(res);
        }
        return res;
    }

    /**
     *
     * @param deviceIndex
     * @param config optional custom configuration, matching the implementation, i.e. {@link StereoDeviceConfig.GenericStereoDeviceConfig}.
     * @param verbose
     * @return
     */
    public final StereoDevice createDevice(final int deviceIndex, final StereoDeviceConfig config, final boolean verbose) {
        final StereoDevice device = createDeviceImpl(deviceIndex, config, verbose);
        if( null != device ) {
            addDevice2List(device);
        }
        return device;
    }
    protected abstract StereoDevice createDeviceImpl(final int deviceIndex, final StereoDeviceConfig config, final boolean verbose);

    /**
     * Returns {@code true}, if instance is created and not {@link #shutdown()}
     * otherwise returns {@code false}.
     */
    public abstract boolean isValid();

    /**
     * Shutdown factory if {@link #isValid() valid}.
     */
    public abstract void shutdown();

    private static final ArrayList<WeakReference<StereoDeviceFactory>> factoryList = new ArrayList<WeakReference<StereoDeviceFactory>>();
    private static void addFactory2List(final StereoDeviceFactory factory) {
        synchronized(factoryList) {
            // GC before add
            int i=0;
            while( i < factoryList.size() ) {
                if( null == factoryList.get(i).get() ) {
                    factoryList.remove(i);
                } else {
                    i++;
                }
            }
            factoryList.add(new WeakReference<StereoDeviceFactory>(factory));
        }
    }
    private static final ArrayList<WeakReference<StereoDevice>> deviceList = new ArrayList<WeakReference<StereoDevice>>();
    private static void addDevice2List(final StereoDevice device) {
        synchronized(deviceList) {
            // GC before add
            int i=0;
            while( i < deviceList.size() ) {
                if( null == deviceList.get(i).get() ) {
                    deviceList.remove(i);
                } else {
                    i++;
                }
            }
            deviceList.add(new WeakReference<StereoDevice>(device));
        }
    }

    private final static void shutdownAll() {
        shutdownDevices();
        shutdownFactories();
    }
    private final static void shutdownFactories() {
        while( 0 < factoryList.size() ) {
            final StereoDeviceFactory f = factoryList.remove(0).get();
            if( null != f && f.isValid() ) {
                f.shutdown();
            }
        }
    }
    private final static void shutdownDevices() {
        while( 0 < deviceList.size() ) {
            final StereoDevice d = deviceList.remove(0).get();
            if( null != d && d.isValid() ) {
                d.dispose();
            }
        }
    }
}
