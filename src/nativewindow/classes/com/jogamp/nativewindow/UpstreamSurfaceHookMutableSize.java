package com.jogamp.nativewindow;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;

public class UpstreamSurfaceHookMutableSize implements UpstreamSurfaceHook.MutableSize {
    int pixWidth, pixHeight;

    /**
     * @param width initial width
     * @param height initial height
     */
    public UpstreamSurfaceHookMutableSize(final int width, final int height) {
        this.pixWidth = width;
        this.pixHeight = height;
    }

    @Override
    public final void setSurfaceSize(final int width, final int height) {
        this.pixWidth = width;
        this.pixHeight = height;
    }

    @Override
    public final int getSurfaceWidth(final ProxySurface s) {
        return pixWidth;
    }

    @Override
    public final int getSurfaceHeight(final ProxySurface s) {
        return pixHeight;
    }
    @Override
    public void create(final ProxySurface s) { /* nop */ }

    @Override
    public void destroy(final ProxySurface s) { /* nop */ }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[pixel "+ pixWidth + "x" + pixHeight + "]";
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

}

