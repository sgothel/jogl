package jogamp.nativewindow.macosx;

import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;

import com.jogamp.nativewindow.UpstreamSurfaceHookMutableSize;

public class OSXDummyUpstreamSurfaceHook extends UpstreamSurfaceHookMutableSize {
    long nsWindow;

    /**
     * @param width the initial width as returned by {@link NativeSurface#getWidth()} via {@link UpstreamSurfaceHook#getWidth(ProxySurface)},
     *        not the actual dummy surface width.
     *        The latter is platform specific and small
     * @param height the initial height as returned by {@link NativeSurface#getHeight()} via {@link UpstreamSurfaceHook#getHeight(ProxySurface)},
     *        not the actual dummy surface height,
     *        The latter is platform specific and small
     */
    public OSXDummyUpstreamSurfaceHook(int width, int height) {
        super(width, height);
        nsWindow = 0;
    }

    @Override
    public final void create(ProxySurface s) {
        if(0 == nsWindow && 0 == s.getSurfaceHandle()) {
            nsWindow = OSXUtil.CreateNSWindow(0, 0, 64, 64);
            if(0 == nsWindow) {
                throw new NativeWindowException("Error NS window 0");
            }
            long nsView = OSXUtil.GetNSView(nsWindow);
            if(0 == nsView) {
                throw new NativeWindowException("Error NS view 0");
            }
            s.setSurfaceHandle(nsView);
            s.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
        }
        s.addUpstreamOptionBits(ProxySurface.OPT_UPSTREAM_WINDOW_INVISIBLE);
    }

    @Override
    public final void destroy(ProxySurface s) {
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
