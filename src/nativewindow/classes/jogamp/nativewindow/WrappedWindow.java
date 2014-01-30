package jogamp.nativewindow;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.Point;

public class WrappedWindow extends WrappedSurface implements NativeWindow {
    private final long windowHandle;
    private final InsetsImmutable insets = new Insets(0, 0, 0, 0);

    /**
     * Utilizes a {@link UpstreamSurfaceHook.MutableSize} to hold the size information,
     * which is being passed to the {@link ProxySurface} instance.
     *
     * @param cfg the {@link AbstractGraphicsConfiguration} to be used
     * @param surfaceHandle the wrapped pre-existing native surface handle, maybe 0 if not yet determined
     * @param initialWidth
     * @param initialHeight
     * @param ownsDevice <code>true</code> if this {@link ProxySurface} instance
     *                  owns the {@link AbstractGraphicsConfiguration}'s {@link AbstractGraphicsDevice},
     *                  otherwise <code>false</code>. Owning the device implies closing it at {@link #destroyNotify()}.
     */
    public WrappedWindow(AbstractGraphicsConfiguration cfg, long surfaceHandle, int initialWidth, int initialHeight, boolean ownsDevice, long windowHandle) {
        super(cfg, surfaceHandle, initialWidth, initialHeight, ownsDevice);
        this.windowHandle = windowHandle;
    }

    /**
     * @param cfg the {@link AbstractGraphicsConfiguration} to be used
     * @param surfaceHandle the wrapped pre-existing native surface handle, maybe 0 if not yet determined
     * @param upstream the {@link UpstreamSurfaceHook} to be used
     * @param ownsDevice <code>true</code> if this {@link ProxySurface} instance
     *                  owns the {@link AbstractGraphicsConfiguration}'s {@link AbstractGraphicsDevice},
     *                  otherwise <code>false</code>.
     */
    public WrappedWindow(AbstractGraphicsConfiguration cfg, long surfaceHandle, UpstreamSurfaceHook upstream, boolean ownsDevice, long windowHandle) {
        super(cfg, surfaceHandle, upstream, ownsDevice);
        this.windowHandle = windowHandle;
    }

    @Override
    public void destroy() {
    }

    @Override
    public NativeWindow getParent() {
        return null;
    }

    @Override
    public long getWindowHandle() {
        return windowHandle;
    }

    @Override
    public InsetsImmutable getInsets() {
        return insets;
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public Point getLocationOnScreen(Point point) {
        if(null!=point) {
          return point;
        } else {
          return new Point(0, 0);
        }
    }

    @Override
    public boolean hasFocus() {
        return false;
    }
}
