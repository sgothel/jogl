package jogamp.nativewindow.x11;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;

import jogamp.nativewindow.x11.X11Lib;

import com.jogamp.nativewindow.UpstreamSurfaceHookMutableSize;
import com.jogamp.nativewindow.x11.X11GraphicsConfiguration;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;

public class X11DummyUpstreamSurfaceHook extends UpstreamSurfaceHookMutableSize {
    /**
     * @param width the initial width as returned by {@link NativeSurface#getSurfaceWidth()} via {@link UpstreamSurfaceHook#getSurfaceWidth(ProxySurface)},
     *        not the actual dummy surface width.
     *        The latter is platform specific and small
     * @param height the initial height as returned by {@link NativeSurface#getSurfaceHeight()} via {@link UpstreamSurfaceHook#getSurfaceHeight(ProxySurface)},
     *        not the actual dummy surface height,
     *        The latter is platform specific and small
     */
    public X11DummyUpstreamSurfaceHook(final int width, final int height) {
        super(width, height);
    }

    @Override
    public final void create(final ProxySurface s) {
        final X11GraphicsConfiguration cfg = (X11GraphicsConfiguration) s.getGraphicsConfiguration();
        final X11GraphicsScreen screen = (X11GraphicsScreen) cfg.getScreen();
        final X11GraphicsDevice device = (X11GraphicsDevice) screen.getDevice();
        device.lock();
        try {
            if(0 == device.getHandle()) {
                device.open();
                s.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_DEVICE );
            }
            if( 0 == s.getSurfaceHandle() ) {
                final long windowHandle = X11Lib.CreateWindow(0, device.getHandle(), screen.getIndex(), cfg.getXVisualID(), 64, 64, false, false);
                if(0 == windowHandle) {
                    throw new NativeWindowException("Creating dummy window failed w/ "+cfg);
                }
                s.setSurfaceHandle(windowHandle);
                s.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
            }
            s.addUpstreamOptionBits( ProxySurface.OPT_UPSTREAM_WINDOW_INVISIBLE );
        } finally {
            device.unlock();
        }
    }

    @Override
    public final void destroy(final ProxySurface s) {
        if( s.containsUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE ) ) {
            final X11GraphicsDevice device = (X11GraphicsDevice) s.getGraphicsConfiguration().getScreen().getDevice();
            if( 0 == s.getSurfaceHandle() ) {
                throw new InternalError("Owns upstream surface, but no X11 window: "+s);
            }
            device.lock();
            try {
                X11Lib.DestroyWindow(device.getHandle(), s.getSurfaceHandle());
                s.setSurfaceHandle(0);
                s.clearUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
            } finally {
                device.unlock();
            }
        }
    }
}
