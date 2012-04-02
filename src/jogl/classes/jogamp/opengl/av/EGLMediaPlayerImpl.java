package jogamp.opengl.av;

import javax.media.opengl.GLContext;

import com.jogamp.opengl.util.texture.Texture;

import jogamp.opengl.egl.EGL;
import jogamp.opengl.egl.EGLContext;
import jogamp.opengl.egl.EGLDrawable;
import jogamp.opengl.egl.EGLExt;

public abstract class EGLMediaPlayerImpl extends GLMediaPlayerImpl {
    TextureType texType;
    boolean useKHRSync;
    
    public enum TextureType {
        GL(0), KHRImage(1); 
        
        public final int id;

        TextureType(int id){
            this.id = id;
        }
    }    
    
    public static class EGLTextureFrame extends TextureFrame {
        
        public EGLTextureFrame(Texture t, long khrImage, long khrSync) {
            super(t);
            this.image = khrImage;
            this.sync = khrSync;
        }
        
        public final long getImage() { return image; }        
        public final long getSync() { return sync; }
        
        public String toString() {
            return "EGLTextureFrame[" + texture + ", img "+ image + ", sync "+ sync+"]";
        }
        protected final long image;
        protected final long sync;
    }

    
    protected EGLMediaPlayerImpl() {
        this(TextureType.GL, false);
    }
    
    protected EGLMediaPlayerImpl(TextureType texType, boolean useKHRSync) {
        super();
        this.texType = texType;
        this.useKHRSync = useKHRSync;
    }

    @Override
    protected TextureFrame createTexImage(GLContext ctx, int idx, int[] tex) {
        final Texture texture = super.createTexImageImpl(ctx, idx, tex, true);
        final long image;
        final long sync;
        
        final EGLContext eglCtx = (EGLContext) ctx;
        final EGLExt eglExt = eglCtx.getEGLExt();
        final EGLDrawable eglDrawable = (EGLDrawable) eglCtx.getGLDrawable();            
        int[] tmp = new int[1];
        
        if(TextureType.KHRImage == texType) {
            // create EGLImage from texture
            tmp[0] = EGL.EGL_NONE;
            image =  eglExt.eglCreateImageKHR( eglDrawable.getDisplay(), eglCtx.getHandle(),
                                               EGLExt.EGL_GL_TEXTURE_2D_KHR,
                                               tex[idx], tmp, 0);
            if (0==image) {
                throw new RuntimeException("EGLImage creation failed: "+EGL.eglGetError()+", ctx "+eglCtx+", tex "+tex[idx]+", err "+toHexString(EGL.eglGetError()));
            }
        } else {
            image = 0;
        }

        if(useKHRSync) {
            // Create sync object so that we can be sure that gl has finished
            // rendering the EGLImage texture before we tell OpenMAX to fill
            // it with a new frame.
            tmp[0] = EGL.EGL_NONE;
            sync = eglExt.eglCreateSyncKHR(eglDrawable.getDisplay(), EGLExt.EGL_SYNC_FENCE_KHR, tmp, 0);
            if (0==sync) {
                throw new RuntimeException("EGLSync creation failed: "+EGL.eglGetError()+", ctx "+eglCtx+", err "+toHexString(EGL.eglGetError()));
            }
        } else {
            sync = 0;
        }
        return new EGLTextureFrame(texture, image, sync);
    }
    
    @Override
    protected void destroyTexImage(GLContext ctx, TextureFrame imgTex) {
        final EGLContext eglCtx = (EGLContext) ctx;
        final EGLExt eglExt = eglCtx.getEGLExt();
        final EGLDrawable eglDrawable = (EGLDrawable) eglCtx.getGLDrawable();
        final EGLTextureFrame eglTex = (EGLTextureFrame) imgTex;
        
        if(0!=eglTex.getImage()) {
            eglExt.eglDestroyImageKHR(eglDrawable.getDisplay(), eglTex.getImage());
        }
        if(0!=eglTex.getSync()) {
            eglExt.eglDestroySyncKHR(eglDrawable.getDisplay(), eglTex.getSync());
        }
        super.destroyTexImage(ctx, imgTex);
    }
}
