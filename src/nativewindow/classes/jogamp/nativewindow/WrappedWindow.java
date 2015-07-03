package jogamp.nativewindow;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;
import com.jogamp.nativewindow.util.Insets;
import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.nativewindow.util.Point;

import com.jogamp.nativewindow.UpstreamWindowHookMutableSizePos;

public class WrappedWindow extends WrappedSurface implements NativeWindow {
    private final InsetsImmutable insets = new Insets(0, 0, 0, 0);
    private long windowHandle;

    /**
     * Utilizes a {@link UpstreamWindowHookMutableSizePos} to hold the size and position information,
     * which is being passed to the {@link ProxySurface} instance.
     *
     * @param cfg the {@link AbstractGraphicsConfiguration} to be used
     * @param surfaceHandle the wrapped pre-existing native surface handle, maybe 0 if not yet determined
     * @param initialWinX
     * @param initialWinY
     * @param initialWinWidth
     * @param initialWinHeight
     * @param initialPixelWidth FIXME: pixel-dim == window-dim 'for now' ?
     * @param initialPixelHeight FIXME: pixel-dim == window-dim 'for now' ?
     * @param ownsDevice <code>true</code> if this {@link ProxySurface} instance
     *                  owns the {@link AbstractGraphicsConfiguration}'s {@link AbstractGraphicsDevice},
     *                  otherwise <code>false</code>. Owning the device implies closing it at {@link #destroyNotify()}.
     */
    public WrappedWindow(final AbstractGraphicsConfiguration cfg, final long surfaceHandle,
                         final int initialWinX, final int initialWinY, final int initialWinWidth, final int initialWinHeight,
                         final int initialPixelWidth, final int initialPixelHeight,
                         final boolean ownsDevice, final long windowHandle) {
        this(cfg, surfaceHandle,
             new UpstreamWindowHookMutableSizePos(initialWinX, initialWinY, initialWinWidth, initialWinHeight,
                                                  initialPixelWidth, initialPixelHeight),
             ownsDevice, windowHandle);
    }

    /**
     * @param cfg the {@link AbstractGraphicsConfiguration} to be used
     * @param surfaceHandle the wrapped pre-existing native surface handle, maybe 0 if not yet determined
     * @param upstream the {@link UpstreamSurfaceHook} to be used
     * @param ownsDevice <code>true</code> if this {@link ProxySurface} instance
     *                  owns the {@link AbstractGraphicsConfiguration}'s {@link AbstractGraphicsDevice},
     *                  otherwise <code>false</code>.
     */
    public WrappedWindow(final AbstractGraphicsConfiguration cfg, final long surfaceHandle, final UpstreamWindowHookMutableSizePos upstream, final boolean ownsDevice, final long windowHandle) {
        super(cfg, surfaceHandle, upstream, ownsDevice);
        this.windowHandle = windowHandle;
    }

    @Override
    protected void invalidateImpl() {
      super.invalidateImpl();
      windowHandle = 0;
    }

    @Override
    public void destroy() {
        destroyNotify();
    }

    @Override
    public final NativeSurface getNativeSurface() { return this; }

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
        return ((UpstreamWindowHookMutableSizePos)getUpstreamSurfaceHook()).getX();
    }

    @Override
    public int getY() {
        return ((UpstreamWindowHookMutableSizePos)getUpstreamSurfaceHook()).getY();
    }

    @Override
    public int getWidth() {
        return ((UpstreamWindowHookMutableSizePos)getUpstreamSurfaceHook()).getWidth();
    }

    @Override
    public int getHeight() {
        return ((UpstreamWindowHookMutableSizePos)getUpstreamSurfaceHook()).getHeight();
    }

    @Override
    public Point getLocationOnScreen(final Point point) {
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
