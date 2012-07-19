package jogamp.opengl.egl;

import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.opengl.GLException;

import com.jogamp.nativewindow.egl.EGLGraphicsDevice;

public class EGLUpstreamSurfaceHook implements ProxySurface.UpstreamSurfaceHook {
    private final NativeSurface upstreamSurface;
    
    public EGLUpstreamSurfaceHook(NativeSurface upstream) {
        upstreamSurface = upstream;
    }
    
    public final NativeSurface getUpstreamSurface() { return upstreamSurface; }
    
    @Override
    public final void create(ProxySurface surface) {
        if(upstreamSurface instanceof ProxySurface) {
            ((ProxySurface)upstreamSurface).createNotify();
            if(NativeSurface.LOCK_SURFACE_NOT_READY >= upstreamSurface.lockSurface()) {
                throw new GLException("Could not lock: "+upstreamSurface);
            }
        }
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) surface.getGraphicsConfiguration().getScreen().getDevice();
        eglDevice.open();
    }

    @Override
    public final void destroy(ProxySurface surface) {
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) surface.getGraphicsConfiguration().getScreen().getDevice();
        eglDevice.close();
        if(upstreamSurface instanceof ProxySurface) {
            upstreamSurface.unlockSurface();
            ((ProxySurface)upstreamSurface).destroyNotify();
        }
    }

    @Override
    public final int getWidth(ProxySurface s) {
        return upstreamSurface.getWidth();
    }

    @Override
    public final int getHeight(ProxySurface s) {
        return upstreamSurface.getHeight();
    }
    
    @Override
    public String toString() {
        final String us_s = null != upstreamSurface ? ( upstreamSurface.getClass().getName() + ": " + upstreamSurface ) : "nil"; 
        return "EGLUpstreamSurfaceHook[upstream: "+us_s+"]";
    }

}
