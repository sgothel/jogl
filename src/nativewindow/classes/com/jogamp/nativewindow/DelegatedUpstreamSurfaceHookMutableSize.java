package com.jogamp.nativewindow;

import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;

public class DelegatedUpstreamSurfaceHookMutableSize extends UpstreamSurfaceHookMutableSize {
    final UpstreamSurfaceHook upstream;

    /**
     * @param upstream optional upstream UpstreamSurfaceHook used for {@link #create(ProxySurface)} and {@link #destroy(ProxySurface)}.
     * @param width initial width
     * @param height initial height
     */
    public DelegatedUpstreamSurfaceHookMutableSize(final UpstreamSurfaceHook upstream, final int width, final int height) {
        super(width, height);
        this.upstream = upstream;
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
    public String toString() {
        return getClass().getSimpleName()+"[ "+ pixWidth + "x" + pixHeight + ", " + upstream + "]";
    }

}

