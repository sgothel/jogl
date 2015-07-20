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

package com.jogamp.oculusvr;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.common.util.JogampVersion;
import com.jogamp.oculusvr.OVR;
import com.jogamp.oculusvr.ovrHmdDesc;
import com.jogamp.oculusvr.ovrSizei;
import com.jogamp.oculusvr.ovrVector2i;

import java.util.jar.Manifest;

public class OVRVersion extends JogampVersion {

    /**
     * Default init-params for {@link OVR#ovr_Initialize(ovrInitParams)},
     * w/ flags {@link OVR#ovrInit_ServerOptional}.
     */
    public static final ovrInitParams defaultInitParams;
    static {
        defaultInitParams = ovrInitParams.create();
        defaultInitParams.setFlags(OVR.ovrInit_ServerOptional);
    }

    protected static volatile OVRVersion jogampCommonVersionInfo;

    protected OVRVersion(final String packageName, final Manifest mf) {
        super(packageName, mf);
    }

    public static OVRVersion getInstance() {
        if(null == jogampCommonVersionInfo) { // volatile: ok
            synchronized(OVRVersion.class) {
                if( null == jogampCommonVersionInfo ) {
                    final String packageName = "com.jogamp.oculusvr";
                    final Manifest mf = VersionUtil.getManifest(OVRVersion.class.getClassLoader(), packageName);
                    jogampCommonVersionInfo = new OVRVersion(packageName, mf);
                }
            }
        }
        return jogampCommonVersionInfo;
    }

    public static StringBuilder getAvailableCapabilitiesInfo(final int ovrHmdIndex, StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        if( !OVR.ovr_Initialize(defaultInitParams) ) { // recursive ..
            sb.append("\tOVR not available").append(Platform.getNewline());
        } else {
            final ovrHmdDesc hmdDesc = OVR.ovrHmd_Create(ovrHmdIndex);
            if( null != hmdDesc ) {
                getAvailableCapabilitiesInfo(hmdDesc, ovrHmdIndex, sb);
                OVR.ovrHmd_Destroy(hmdDesc);
            } else {
                sb.append("\thmd."+ovrHmdIndex+" not available").append(Platform.getNewline());
            }
        }
        // Nope .. ovr.ovr_Shutdown();
        sb.append(Platform.getNewline());
        return sb;
    }
    /**
     *
     * @param hmdDesc
     * @param ovrHmdIndex only for informal purposes, OVR HMD index of <code>hmdDesc</code>
     * @param sb
     * @return
     */
    public static StringBuilder getAvailableCapabilitiesInfo(final ovrHmdDesc hmdDesc, final int ovrHmdIndex, StringBuilder sb) {
        if(null == hmdDesc) {
            throw new IllegalArgumentException("null hmdDesc");
        }
        final OvrHmdContext hmdCtx = hmdDesc.getHandle();
        if(null == hmdCtx) {
            throw new IllegalArgumentException("null hmdCtx");
        }
        if(null==sb) {
            sb = new StringBuilder();
        }
        sb.append("\thmd."+ovrHmdIndex+".productName: "+hmdDesc.getProductNameAsString()).append(Platform.getNewline());
        sb.append("\thmd."+ovrHmdIndex+".vendorName: "+hmdDesc.getManufacturerAsString()).append(Platform.getNewline());
        sb.append("\thmd."+ovrHmdIndex+".deviceName: "+hmdDesc.getDisplayDeviceNameAsString()).append(Platform.getNewline());
        sb.append("\thmd."+ovrHmdIndex+".productId: 0x"+Integer.toHexString(hmdDesc.getProductId())).append(Platform.getNewline());
        sb.append("\thmd."+ovrHmdIndex+".vendorId: 0x"+Integer.toHexString(hmdDesc.getVendorId())).append(Platform.getNewline());
        sb.append("\thmd."+ovrHmdIndex+".serial: "+hmdDesc.getSerialNumberAsString()).append(Platform.getNewline());
        sb.append("\thmd."+ovrHmdIndex+".firmware: "+hmdDesc.getFirmwareMajor()+"."+hmdDesc.getFirmwareMinor()).append(Platform.getNewline());
        sb.append("\thmd."+ovrHmdIndex+".type: "+hmdDesc.getType()).append(Platform.getNewline());
        sb.append("\thmd."+ovrHmdIndex+".hmdCaps: "+toHexString(hmdDesc.getHmdCaps())).append(Platform.getNewline());
        sb.append("\thmd."+ovrHmdIndex+".distorCaps: "+toHexString(hmdDesc.getDistortionCaps())).append(Platform.getNewline());
        sb.append("\thmd."+ovrHmdIndex+".sensorCaps: "+toHexString(hmdDesc.getTrackingCaps())).append(Platform.getNewline());
        final ovrSizei resolution = hmdDesc.getResolution();
        sb.append("\thmd."+ovrHmdIndex+".resolution: "+resolution.getW()+"x"+resolution.getH()).append(Platform.getNewline());
        final ovrVector2i winPos = hmdDesc.getWindowsPos();
        sb.append("\thmd."+ovrHmdIndex+".winPos: "+winPos.getX()+" / "+winPos.getY()).append(Platform.getNewline());
        sb.append("\thmd."+ovrHmdIndex+".cameraFrustum.Z: "+hmdDesc.getCameraFrustumNearZInMeters()+" - "+hmdDesc.getCameraFrustumFarZInMeters()+" meter").append(Platform.getNewline());
        sb.append("\thmd."+ovrHmdIndex+".cameraFrustum.Fov: H "+hmdDesc.getCameraFrustumHFovInRadians()+" rad, V "+hmdDesc.getCameraFrustumVFovInRadians()+" rad").append(Platform.getNewline());
        return sb;
    }
    private static String toHexString(final int v) { return "0x"+Integer.toHexString(v); }

    public static StringBuilder getAllAvailableCapabilitiesInfo(StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        sb.append(Platform.getNewline()).append(Platform.getNewline());
        sb.append("HMD.0 Capabilities: ").append(Platform.getNewline());
        getAvailableCapabilitiesInfo(0, sb);
        return sb;
    }

    public static void main(final String args[]) {
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(GlueGenVersion.getInstance());
        // System.err.println(NativeWindowVersion.getInstance());
        System.err.println(OVRVersion.getInstance());
        System.err.println(OVRVersion.getAllAvailableCapabilitiesInfo(null).toString());
    }
}

