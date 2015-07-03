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
package com.jogamp.nativewindow;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;

import com.jogamp.nativewindow.UpstreamSurfaceHookMutableSize;

public class GenericUpstreamSurfacelessHook extends UpstreamSurfaceHookMutableSize {
    /**
     * @param width the initial width as returned by {@link NativeSurface#getSurfaceWidth()} via {@link UpstreamSurfaceHook#getSurfaceWidth(ProxySurface)},
     *        not the actual dummy surface width.
     *        The latter is platform specific and small
     * @param height the initial height as returned by {@link NativeSurface#getSurfaceHeight()} via {@link UpstreamSurfaceHook#getSurfaceHeight(ProxySurface)},
     *        not the actual dummy surface height,
     *        The latter is platform specific and small
     */
    public GenericUpstreamSurfacelessHook(final int width, final int height) {
        super(width, height);
    }

    @Override
    public final void create(final ProxySurface s) {
        final AbstractGraphicsDevice device = s.getGraphicsConfiguration().getScreen().getDevice();
        device.lock();
        try {
            if(0 == device.getHandle()) {
                device.open();
                s.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_DEVICE );
            }
            if( 0 != s.getSurfaceHandle() ) {
                throw new InternalError("Upstream surface not null: "+s);
            }
            s.addUpstreamOptionBits( ProxySurface.OPT_UPSTREAM_SURFACELESS |
                                     ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE |
                                     ProxySurface.OPT_UPSTREAM_WINDOW_INVISIBLE );
        } finally {
            device.unlock();
        }
    }

    @Override
    public final void destroy(final ProxySurface s) {
        if( s.containsUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE ) ) {
            final AbstractGraphicsDevice device = s.getGraphicsConfiguration().getScreen().getDevice();
            if( !s.containsUpstreamOptionBits( ProxySurface.OPT_UPSTREAM_SURFACELESS ) ) {
                throw new InternalError("Owns upstream surface, but not a valid zero surface: "+s);
            }
            if( 0 != s.getSurfaceHandle() ) {
                throw new InternalError("Owns upstream valid zero surface, but non zero surface: "+s);
            }
            device.lock();
            try {
                s.clearUpstreamOptionBits( ProxySurface.OPT_UPSTREAM_SURFACELESS | ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
            } finally {
                device.unlock();
            }
        }
    }
}
