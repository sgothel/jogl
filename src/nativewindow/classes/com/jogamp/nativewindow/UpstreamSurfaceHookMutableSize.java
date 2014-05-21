package com.jogamp.nativewindow;

import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;

public class UpstreamSurfaceHookMutableSize implements UpstreamSurfaceHook.MutableSize {
    int pixWidth, pixHeight;

    /**
     * @param width initial width
     * @param height initial height
     */
    public UpstreamSurfaceHookMutableSize(int width, int height) {
        this.pixWidth = width;
        this.pixHeight = height;
    }

    @Override
    public final void setPixelSize(int width, int height) {
        this.pixWidth = width;
        this.pixHeight = height;
    }

    @Override
    public final int getPixelWidth(ProxySurface s) {
        return pixWidth;
    }

    @Override
    public final int getPixelHeight(ProxySurface s) {
        return pixHeight;
    }
    @Override
    public void create(ProxySurface s) { /* nop */ }

    @Override
    public void destroy(ProxySurface s) { /* nop */ }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[pixel "+ pixWidth + "x" + pixHeight + "]";
    }

}

