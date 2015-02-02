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

import jogamp.nativewindow.Debug;

/**
 * Provides a mutable {@link NativeSurface}, i.e. {@link MutableSurface}, while allowing an
 * {@link UpstreamSurfaceHook} to influence the lifecycle and information.
 *
 * @see UpstreamSurfaceHook
 * @see MutableSurface
 * @see NativeSurface
 */
public interface ProxySurface extends MutableSurface {
    public static final boolean DEBUG = Debug.debug("ProxySurface");

    /**
     * Implementation specific bit-value stating this {@link ProxySurface} owns the upstream's surface handle
     * @see #addUpstreamOptionBits(int)
     * @see #clearUpstreamOptionBits(int)
     * @see #getUpstreamOptionBits()
     */
    public static final int OPT_PROXY_OWNS_UPSTREAM_SURFACE = 1 << 6;

    /**
     * Implementation specific bit-value stating this {@link ProxySurface} owns the upstream's {@link AbstractGraphicsDevice}.
     * @see #addUpstreamOptionBits(int)
     * @see #clearUpstreamOptionBits(int)
     * @see #getUpstreamOptionBits()
     */
    public static final int OPT_PROXY_OWNS_UPSTREAM_DEVICE = 1 << 7;

    /**
     * Implementation specific bitvalue stating the upstream's {@link NativeSurface} is an invisible window, i.e. maybe incomplete.
     * @see #addUpstreamOptionBits(int)
     * @see #clearUpstreamOptionBits(int)
     * @see #getUpstreamOptionBits()
     */
    public static final int OPT_UPSTREAM_WINDOW_INVISIBLE = 1 << 8;

    /**
     * Implementation specific bitvalue stating the upstream's {@link NativeSurface}'s zero handle is valid.
     * @see #addUpstreamOptionBits(int)
     * @see #clearUpstreamOptionBits(int)
     * @see #getUpstreamOptionBits()
     */
    public static final int OPT_UPSTREAM_SURFACELESS = 1 << 9;

    /** Allow redefining the AbstractGraphicsConfiguration */
    public void setGraphicsConfiguration(AbstractGraphicsConfiguration cfg);

    /**
     * Returns the optional upstream {@link NativeSurface} if used by implementation, otherwise <code>null</code>.
     * <p>
     * The upstream {@link NativeSurface} is retrieved via {@link #getUpstreamSurfaceHook() the UpstreamSurfaceHook},
     * i.e.  {@link UpstreamSurfaceHook#getUpstreamSurface()}.
     * </p>
     * <p>
     * One example is the JOGL EGLWrappedSurface, which might be backed up by a
     * native platform NativeSurface (X11, WGL, CGL, ..).
     * </p>
     */
    public NativeSurface getUpstreamSurface();

    /** Returns the {@link UpstreamSurfaceHook} if {@link #setUpstreamSurfaceHook(UpstreamSurfaceHook) set}, otherwise <code>null</code>. */
    public UpstreamSurfaceHook getUpstreamSurfaceHook();

    /**
     * Overrides the {@link UpstreamSurfaceHook}.
     */
    public void setUpstreamSurfaceHook(UpstreamSurfaceHook hook);

    /**
     * Enables or disables the {@link UpstreamSurfaceHook} lifecycle functions
     * {@link UpstreamSurfaceHook#create(ProxySurface)} and {@link UpstreamSurfaceHook#destroy(ProxySurface)}.
     * <p>
     * Use this for small code blocks where the native resources shall not change,
     * i.e. resizing a derived (OpenGL) drawable.
     * </p>
     */
    public void enableUpstreamSurfaceHookLifecycle(boolean enable);

    /**
     * {@link UpstreamSurfaceHook#create(ProxySurface)} is being issued and the proxy surface/window handles shall be set.
     */
    public void createNotify();

    /**
     * {@link UpstreamSurfaceHook#destroy(ProxySurface)} is being issued and all proxy surface/window handles shall be cleared.
     */
    public void destroyNotify();

    public StringBuilder getUpstreamOptionBits(StringBuilder sink);
    public int getUpstreamOptionBits();

    /** Returns <code>true</code> if the give bit-mask <code>v</code> is set in this instance upstream-option-bits, otherwise <code>false</code>.*/
    public boolean containsUpstreamOptionBits(int v);

    /** Add the given bit-mask to this instance upstream-option-bits using bit-or w/ <code>v</code>.*/
    public void addUpstreamOptionBits(int v);

    /** Clear the given bit-mask from this instance upstream-option-bits using bit-and w/ <code>~v</code>*/
    public void clearUpstreamOptionBits(int v);

    public StringBuilder toString(StringBuilder sink);
    @Override
    public String toString();
}
