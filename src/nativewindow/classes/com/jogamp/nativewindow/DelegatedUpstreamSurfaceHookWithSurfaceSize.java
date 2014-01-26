package com.jogamp.nativewindow;

import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;

public class DelegatedUpstreamSurfaceHookWithSurfaceSize implements UpstreamSurfaceHook {
    final UpstreamSurfaceHook upstream;
    final NativeSurface surface;

    /**
     * @param upstream optional upstream UpstreamSurfaceHook used for {@link #create(ProxySurface)} and {@link #destroy(ProxySurface)}.
     * @param surface mandatory {@link NativeSurface} used for {@link #getWidth(ProxySurface)} and {@link #getHeight(ProxySurface)}
     */
    public DelegatedUpstreamSurfaceHookWithSurfaceSize(UpstreamSurfaceHook upstream, NativeSurface surface) {
        this.upstream = upstream;
        this.surface = surface;
        if(null == surface) {
            throw new IllegalArgumentException("given surface is null");
        }
    }

    @Override
    public final void create(ProxySurface s) {
        if(null != upstream) {
            upstream.create(s);
        }
    }

    @Override
    public final void destroy(ProxySurface s) {
        if(null != upstream) {
            upstream.destroy(s);
        }
    }

    @Override
    public final int getWidth(ProxySurface s) {
        return surface.getWidth();
    }

    @Override
    public final int getHeight(ProxySurface s) {
        return surface.getHeight();
    }

    @Override
    public String toString() {
        final String us_s = null != surface ? ( surface.getClass().getName() + ": 0x" + Long.toHexString(surface.getSurfaceHandle()) + " " +surface.getWidth() + "x" + surface.getHeight() ) : "nil";
        return getClass().getSimpleName()+"["+upstream+", "+us_s+"]";
    }

}

