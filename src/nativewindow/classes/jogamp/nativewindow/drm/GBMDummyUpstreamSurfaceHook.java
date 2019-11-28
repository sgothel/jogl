/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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
package jogamp.nativewindow.drm;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;

import com.jogamp.nativewindow.UpstreamSurfaceHookMutableSize;

public class GBMDummyUpstreamSurfaceHook extends UpstreamSurfaceHookMutableSize {
    private long gbmDevice = 0;

    /**
     * @param width the initial width as returned by {@link NativeSurface#getSurfaceWidth()} via {@link UpstreamSurfaceHook#getSurfaceWidth(ProxySurface)},
     *        not the actual dummy surface width.
     *        The latter is platform specific and small
     * @param height the initial height as returned by {@link NativeSurface#getSurfaceHeight()} via {@link UpstreamSurfaceHook#getSurfaceHeight(ProxySurface)},
     *        not the actual dummy surface height,
     *        The latter is platform specific and small
     */
    public GBMDummyUpstreamSurfaceHook(final int width, final int height) {
        super(width, height);
    }

    @Override
    public final void create(final ProxySurface s) {
        final AbstractGraphicsDevice gd = s.getGraphicsConfiguration().getScreen().getDevice();
        final int visualID = DRMUtil.GBM_FORMAT_XRGB8888;
        gd.lock();
        try {
            if( 0 == s.getSurfaceHandle() ) {
                gbmDevice = DRMLib.gbm_create_device(DRMUtil.getDrmFd());
                if(0 == gbmDevice) {
                    throw new NativeWindowException("Creating dummy GBM device failed");
                }

                final long gbmSurface = DRMLib.gbm_surface_create(gbmDevice, 64, 64, visualID,
                                          DRMLib.GBM_BO_USE_SCANOUT | DRMLib.GBM_BO_USE_RENDERING);
                if(0 == gbmSurface) {
                    throw new NativeWindowException("Creating dummy GBM surface failed");
                }
                s.setSurfaceHandle(gbmSurface);

                s.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
            }
            s.addUpstreamOptionBits( ProxySurface.OPT_UPSTREAM_WINDOW_INVISIBLE );
        } finally {
            gd.unlock();
        }
    }

    @Override
    public final void destroy(final ProxySurface s) {
        if( s.containsUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE ) ) {
            final AbstractGraphicsDevice gd = s.getGraphicsConfiguration().getScreen().getDevice();
            final long gbmSurface = s.getSurfaceHandle();
            if( 0 == gbmDevice ) {
                throw new InternalError("GBM device handle is null");
            }
            if( 0 == gbmSurface ) {
                throw new InternalError("Owns upstream surface, but has no GBM surface: "+s);
            }
            gd.lock();
            try {
                DRMLib.gbm_surface_destroy(gbmSurface);
                s.setSurfaceHandle(0);

                DRMLib.gbm_device_destroy(gbmDevice);
                gbmDevice = 0;

                s.clearUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
            } finally {
                gd.unlock();
            }
        }
    }
}
