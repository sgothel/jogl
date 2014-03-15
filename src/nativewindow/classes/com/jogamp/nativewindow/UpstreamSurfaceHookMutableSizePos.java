package com.jogamp.nativewindow;

public class UpstreamSurfaceHookMutableSizePos extends UpstreamSurfaceHookMutableSize {
    int x, y;

    /**
     * @param width initial width
     * @param height initial height
     */
    public UpstreamSurfaceHookMutableSizePos(int x, int y, int width, int height) {
        super(width, height);
        this.x= x;
        this.y= y;
    }

    // @Override
    public final void setPos(int x, int y) {
        this.x= x;
        this.y= y;
    }

    public final int getX() {
        return x;
    }

    public final int getY() {
        return y;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[ "+ x + "/" + y + " " + width + "x" + height + "]";
    }

}

