package com.jogamp.nativewindow;

import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;

public class DelegatedUpstreamSurfaceHookMutableSize extends UpstreamSurfaceHookMutableSize {
    final UpstreamSurfaceHook upstream;

    /**
     * @param upstream optional upstream UpstreamSurfaceHook used for {@link #create(ProxySurface)} and {@link #destroy(ProxySurface)}.
     * @param width initial width
     * @param height initial height
     */
    public DelegatedUpstreamSurfaceHookMutableSize(UpstreamSurfaceHook upstream, int width, int height) {
        super(width, height);
        this.upstream = upstream;
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
    public String toString() {
        return getClass().getSimpleName()+"[ "+ pixWidth + "x" + pixHeight + ", " + upstream + "]";
    }

}

