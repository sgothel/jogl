package jogamp.nativewindow.windows;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;

import com.jogamp.nativewindow.UpstreamSurfaceHookMutableSize;

public class GDIDummyUpstreamSurfaceHook extends UpstreamSurfaceHookMutableSize {
    /**
     * @param width the initial width as returned by {@link NativeSurface#getSurfaceWidth()} via {@link UpstreamSurfaceHook#getSurfaceWidth(ProxySurface)},
     *        not the actual dummy surface width.
     *        The latter is platform specific and small
     * @param height the initial height as returned by {@link NativeSurface#getSurfaceHeight()} via {@link UpstreamSurfaceHook#getSurfaceHeight(ProxySurface)},
     *        not the actual dummy surface height,
     *        The latter is platform specific and small
     */
    public GDIDummyUpstreamSurfaceHook(final int width, final int height) {
        super(width, height);
    }

    @Override
    public final void create(final ProxySurface s) {
        final GDISurface ms = (GDISurface)s;
        if(0 == ms.getWindowHandle()) {
            final long windowHandle = GDIUtil.CreateDummyWindow(0, 0, 64, 64);
            if(0 == windowHandle) {
                throw new NativeWindowException("Error windowHandle 0, werr: "+GDI.GetLastError());
            }
            ms.setWindowHandle(windowHandle);
            ms.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
        }
        s.addUpstreamOptionBits(ProxySurface.OPT_UPSTREAM_WINDOW_INVISIBLE);
    }

    @Override
    public final void destroy(final ProxySurface s) {
        final GDISurface ms = (GDISurface)s;
        if( ms.containsUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE ) ) {
            if( 0 == ms.getWindowHandle() ) {
                throw new InternalError("Owns upstream surface, but no GDI window: "+ms);
            }
            GDI.ShowWindow(ms.getWindowHandle(), GDI.SW_HIDE);
            GDIUtil.DestroyDummyWindow(ms.getWindowHandle());
            ms.setWindowHandle(0);
            ms.clearUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
        }
    }
}
