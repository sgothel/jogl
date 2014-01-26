package com.jogamp.nativewindow;

import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;

public class UpstreamSurfaceHookMutableSize implements UpstreamSurfaceHook.MutableSize {
    int width, height;

    /**
     * @param width initial width
     * @param height initial height
     */
    public UpstreamSurfaceHookMutableSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public final void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public final int getWidth(ProxySurface s) {
        return width;
    }

    @Override
    public final int getHeight(ProxySurface s) {
        return height;
    }
    @Override
    public void create(ProxySurface s) { /* nop */ }

    @Override
    public void destroy(ProxySurface s) { /* nop */ }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[ "+ width + "x" + height + "]";
    }

}

