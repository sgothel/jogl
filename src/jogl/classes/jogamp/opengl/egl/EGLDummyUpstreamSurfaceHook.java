package jogamp.opengl.egl;

import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;

import com.jogamp.nativewindow.UpstreamSurfaceHookMutableSize;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;

/** Uses a PBuffer offscreen surface */
public class EGLDummyUpstreamSurfaceHook extends UpstreamSurfaceHookMutableSize {
    /**
     * @param width the initial width as returned by {@link NativeSurface#getWidth()} via {@link UpstreamSurfaceHook#getWidth(ProxySurface)},
     *        not the actual dummy surface width.
     *        The latter is platform specific and small
     * @param height the initial height as returned by {@link NativeSurface#getHeight()} via {@link UpstreamSurfaceHook#getHeight(ProxySurface)},
     *        not the actual dummy surface height,
     *        The latter is platform specific and small
     */
    public EGLDummyUpstreamSurfaceHook(int width, int height) {
        super(width, height);
    }

    @Override
    public final void create(ProxySurface s) {
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) s.getGraphicsConfiguration().getScreen().getDevice();
        eglDevice.lock();
        try {
            if(0 == eglDevice.getHandle()) {
                eglDevice.open();
                s.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_DEVICE );
            }
            if( EGL.EGL_NO_SURFACE == s.getSurfaceHandle() ) {
                s.setSurfaceHandle( EGLDrawableFactory.createPBufferSurfaceImpl((EGLGraphicsConfiguration)s.getGraphicsConfiguration(), 64, 64, false) );
                s.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
            }
            s.addUpstreamOptionBits(ProxySurface.OPT_UPSTREAM_WINDOW_INVISIBLE);
        } finally {
            eglDevice.unlock();
        }
    }

    @Override
    public final void destroy(ProxySurface s) {
        if( s.containsUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE ) ) {
            final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) s.getGraphicsConfiguration().getScreen().getDevice();
            if( EGL.EGL_NO_SURFACE == s.getSurfaceHandle() ) {
                throw new InternalError("Owns upstream surface, but no EGL surface: "+s);
            }
            eglDevice.lock();
            try {
                EGL.eglDestroySurface(eglDevice.getHandle(), s.getSurfaceHandle());
                s.setSurfaceHandle(EGL.EGL_NO_SURFACE);
                s.clearUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
            } finally {
                eglDevice.unlock();
            }
        }
    }
}
