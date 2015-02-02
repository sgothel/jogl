package jogamp.nativewindow.macosx;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;

import com.jogamp.nativewindow.UpstreamSurfaceHookMutableSize;

public class OSXDummyUpstreamSurfaceHook extends UpstreamSurfaceHookMutableSize {
    long nsWindow;

    /**
     * @param width the initial width as returned by {@link NativeSurface#getSurfaceWidth()} via {@link UpstreamSurfaceHook#getSurfaceWidth(ProxySurface)},
     *        not the actual dummy surface width.
     *        The latter is platform specific and small
     * @param height the initial height as returned by {@link NativeSurface#getSurfaceHeight()} via {@link UpstreamSurfaceHook#getSurfaceHeight(ProxySurface)},
     *        not the actual dummy surface height,
     *        The latter is platform specific and small
     */
    public OSXDummyUpstreamSurfaceHook(final int width, final int height) {
        super(width, height);
        nsWindow = 0;
    }

    @Override
    public final void create(final ProxySurface s) {
        if(0 == nsWindow && 0 == s.getSurfaceHandle()) {
            nsWindow = OSXUtil.CreateNSWindow(0, 0, 64, 64);
            if(0 == nsWindow) {
                throw new NativeWindowException("Error NS window 0");
            }
            final long nsView = OSXUtil.GetNSView(nsWindow);
            if(0 == nsView) {
                throw new NativeWindowException("Error NS view 0");
            }
            s.setSurfaceHandle(nsView);
            s.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
        }
        s.addUpstreamOptionBits(ProxySurface.OPT_UPSTREAM_WINDOW_INVISIBLE);
    }

    @Override
    public final void destroy(final ProxySurface s) {
        if( s.containsUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE ) ) {
            if( 0 == nsWindow || 0 == s.getSurfaceHandle() ) {
                throw new InternalError("Owns upstream surface, but no OSX view/window: "+s+", nsWindow 0x"+Long.toHexString(nsWindow));
            }
            OSXUtil.DestroyNSWindow(nsWindow);
            nsWindow = 0;
            s.setSurfaceHandle(0);
            s.clearUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
        }
    }

}
