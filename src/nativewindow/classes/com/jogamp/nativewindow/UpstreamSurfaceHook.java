/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

/**
 * Interface allowing upstream caller to pass lifecycle actions and size info
 * to a {@link ProxySurface} instance.
 */
public interface UpstreamSurfaceHook {
    /** called within {@link ProxySurface#createNotify()} within lock, before using surface. */
    public void create(ProxySurface s);
    /** called within {@link ProxySurface#destroyNotify()} within lock, before clearing fields. */
    public void destroy(ProxySurface s);

    /**
     * Returns the optional upstream {@link NativeSurface} if used by implementation, otherwise <code>null</code>.
     * <p>
     * One example is the JOGL EGLWrappedSurface, which might be backed up by a
     * native platform NativeSurface (X11, WGL, CGL, ..).
     * </p>
     */
    public NativeSurface getUpstreamSurface();

    /** Returns the width of the upstream surface in pixels, used if {@link ProxySurface#UPSTREAM_PROVIDES_SIZE} is set. */
    public int getSurfaceWidth(ProxySurface s);
    /** Returns the height of the upstream surface in pixels, used if {@link ProxySurface#UPSTREAM_PROVIDES_SIZE} is set. */
    public int getSurfaceHeight(ProxySurface s);

    /**
     * {@link UpstreamSurfaceHook} w/ mutable size, allowing it's {@link ProxySurface} user to resize.
     */
    public interface MutableSize extends UpstreamSurfaceHook {
        /**
         * Resizes the upstream surface.
         * @param width new width in pixel units
         * @param height new height in pixel units
         */
        public void setSurfaceSize(int width, int height);
    }
}
