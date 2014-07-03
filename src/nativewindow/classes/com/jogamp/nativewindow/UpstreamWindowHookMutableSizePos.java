package com.jogamp.nativewindow;

public class UpstreamWindowHookMutableSizePos extends UpstreamSurfaceHookMutableSize {
    int winX, winY, winWidth, winHeight;

    /**
     * @param winX initial window x-pos
     * @param winY initial window y-pos
     * @param winWidth initial window width
     * @param winHeight initial window height
     * @param pixWidth initial surface pixel width, FIXME: pixel-dim == window-dim 'for now' ?
     * @param pixHeight initial surface pixel height, FIXME: pixel-dim == window-dim 'for now' ?
     */
    public UpstreamWindowHookMutableSizePos(final int winX, final int winY, final int winWidth, final int winHeight, final int pixWidth, final int pixHeight) {
        super(pixWidth, pixHeight);
        this.winX= winX;
        this.winY= winY;
        this.winWidth = winWidth;
        this.winHeight = winHeight;
    }

    // @Override
    public final void setWinPos(final int winX, final int winY) {
        this.winX= winX;
        this.winY= winY;
    }
    // @Override
    public final void setWinSize(final int winWidth, final int winHeight) {
        this.winWidth= winWidth;
        this.winHeight= winHeight;
        // FIXME HiDPI: Use pixelScale ?!
        // FIXME HiDPI: Consider setting winWidth/winHeight by setSurfaceSize(..) (back-propagation)
        this.setSurfaceSize(winWidth, winHeight);
    }

    public final int getX() {
        return winX;
    }

    public final int getY() {
        return winY;
    }
    public final int getWidth() {
        return winWidth;
    }
    public final int getHeight() {
        return winHeight;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[window "+ winX + "/" + winY + " " + winWidth + "x" + winHeight + ", pixel " + pixWidth + "x" + pixHeight + "]";
    }

}

