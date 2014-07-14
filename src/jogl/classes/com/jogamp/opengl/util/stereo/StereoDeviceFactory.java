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

import com.jogamp.common.util.ReflectionUtil;

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
    private static final String GenericStereoDeviceClazzName = "jogamp.opengl.util.stereo.GenericStereoDeviceFactory";
    private static final String isAvailableMethodName = "isAvailable";

    public static enum DeviceType { Default, Generic, OculusVR };

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
            default: throw new InternalError("XXX");
        }
        final ClassLoader cl = StereoDeviceFactory.class.getClassLoader();
        return createFactory(cl, className);
    }

    public static StereoDeviceFactory createFactory(final ClassLoader cl, final String implName) {
        try {
            if(((Boolean)ReflectionUtil.callStaticMethod(implName, isAvailableMethodName, null, null, cl)).booleanValue()) {
                return (StereoDeviceFactory) ReflectionUtil.createInstance(implName, cl);
            }
        } catch (final Throwable t) { if(StereoDevice.DEBUG) { System.err.println("Caught "+t.getClass().getName()+": "+t.getMessage()); t.printStackTrace(); } }
        return null;
    }

    public abstract StereoDevice createDevice(final int deviceIndex, final StereoDevice.Config config, final boolean verbose);
}
