package jogamp.opengl.egl;

import javax.media.nativewindow.NativeSurface;

import jogamp.nativewindow.WrappedSurface;

public class EGLWrappedSurface extends WrappedSurface {

    public static EGLWrappedSurface get(NativeSurface surface) {
        if(surface instanceof EGLWrappedSurface) {
            return (EGLWrappedSurface)surface;
        }
        return new EGLWrappedSurface(surface);
    }
    
    public EGLWrappedSurface(NativeSurface surface) {
        super(surface.getGraphicsConfiguration(), EGL.EGL_NO_SURFACE, new EGLUpstreamSurfaceHook(surface), false /* tbd in UpstreamSurfaceHook */);
        if(EGLDrawableFactory.DEBUG) {
            System.err.println("EGLWrappedSurface.ctor(): "+this);            
        }
    }

    public final NativeSurface getUpstreamSurface() { 
        return ((EGLUpstreamSurfaceHook)super.getUpstreamSurfaceHook()).getUpstreamSurface(); 
    }    
}
