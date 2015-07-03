package com.jogamp.nativewindow;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;

public class DelegatedUpstreamSurfaceHookWithSurfaceSize implements UpstreamSurfaceHook {
    final UpstreamSurfaceHook upstream;
    final NativeSurface surface;

    /**
     * @param upstream optional upstream UpstreamSurfaceHook used for {@link #create(ProxySurface)} and {@link #destroy(ProxySurface)}.
     * @param surface mandatory {@link NativeSurface} used for {@link #getSurfaceWidth(ProxySurface)} and {@link #getSurfaceHeight(ProxySurface)}, not used for {@link #getUpstreamSurface()}.
     */
    public DelegatedUpstreamSurfaceHookWithSurfaceSize(final UpstreamSurfaceHook upstream, final NativeSurface surface) {
        this.upstream = upstream;
        this.surface = surface;
        if(null == surface) {
            throw new IllegalArgumentException("given surface is null");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns <code>null</code>.
     * </p>
     */
    @Override
    public final NativeSurface getUpstreamSurface() {
        return null;
    }

    @Override
    public final void create(final ProxySurface s) {
        if(null != upstream) {
            upstream.create(s);
        }
    }

    @Override
    public final void destroy(final ProxySurface s) {
        if(null != upstream) {
            upstream.destroy(s);
        }
    }

    @Override
    public final int getSurfaceWidth(final ProxySurface s) {
        return surface.getSurfaceWidth();
    }

    @Override
    public final int getSurfaceHeight(final ProxySurface s) {
        return surface.getSurfaceHeight();
    }

    @Override
    public String toString() {
        final String us_s = null != surface ? ( surface.getClass().getName() + ": 0x" + Long.toHexString(surface.getSurfaceHandle()) + " " +surface.getSurfaceWidth() + "x" + surface.getSurfaceHeight() ) : "nil";
        return getClass().getSimpleName()+"["+upstream+", "+us_s+"]";
    }

}

