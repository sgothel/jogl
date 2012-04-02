package jogamp.opengl.android.av;

import java.io.IOException;
import java.net.URL;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLES2;

import jogamp.common.os.android.StaticContext;
import jogamp.opengl.av.GLMediaPlayerImpl;

import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;

import com.jogamp.opengl.av.GLMediaEventListener;
import com.jogamp.opengl.av.GLMediaPlayer;
import com.jogamp.opengl.av.GLMediaPlayer.TextureFrame;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;

/***
 * Android API Level 14: {@link MediaPlayer#setSurface(Surface)}
 * Android API Level 14: {@link Surface#Surface(android.graphics.SurfaceTexture)}
 */
public class AndroidGLMediaPlayerAPI14 extends GLMediaPlayerImpl {
    MediaPlayer mp;
    boolean updateSurface = false;
    
    public static class AndroidTextureFrame extends TextureFrame {
        public AndroidTextureFrame(Texture t, SurfaceTexture stex) {
            super(t);
            this.stex = stex;
        }
        
        public final SurfaceTexture getSurfaceTexture() { return stex; }
        
        public String toString() {
            return "AndroidTextureFrame[" + texture + ", "+ stex + "]";
        }
        protected SurfaceTexture stex;
    }
    
    public AndroidGLMediaPlayerAPI14() {
        super();
        this.setTextureTarget(GLES2.GL_TEXTURE_EXTERNAL_OES);
        this.setTextureCount(1);
        mp = new MediaPlayer();
    }

    @Override
    public void setPlaySpeed(float rate) {
        // n/a
    }

    @Override
    public void start() {
        mp.start();
    }

    @Override
    public void pause() {
        mp.pause();
    }

    @Override
    public void stop() {
        mp.stop();
    }

    @Override
    public int seek(int msec) {
        mp.seekTo(msec);
        return mp.getCurrentPosition();
    }

    @Override
    public Texture getNextTextureID() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getCurrentPosition() {
        return mp.getCurrentPosition();
    }

    @Override
    public boolean isValid() {
        return true;
    }
    
    AndroidTextureFrame androidTextureFrame = null;
    
    @Override
    protected void destroyImpl(GL gl) {
        mp.release();
        mp = null;        
    }
    
    @Override
    protected void setStreamImpl() throws IOException {
        try {
            final Uri uri = Uri.parse(url.toExternalForm());        
            mp.setDataSource(StaticContext.getContext(), uri);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        }
        mp.prepare();
        
        width = mp.getVideoWidth();
        height = mp.getVideoHeight();
        fps = 0;
        bps = 0;
        totalFrames = mp.getDuration();
        acodec = "unknown";
        vcodec = "unknown";        
    }
    
    @Override
    protected void destroyTexImage(GLContext ctx, TextureFrame imgTex) {
        final AndroidTextureFrame atf = (AndroidTextureFrame) imgTex;
        atf.getSurfaceTexture().release();
        super.destroyTexImage(ctx, imgTex);
    }
    
    @Override
    protected AndroidTextureFrame createTexImage(GLContext ctx, int idx, int[] tex) {
        final GL gl = ctx.getGL();
        
        if( 0 > tex[idx] ) {
            throw new RuntimeException("TextureName "+toHexString(tex[idx])+" invalid.");
        }
        gl.glBindTexture(GLES2.GL_TEXTURE_EXTERNAL_OES, tex[idx]);
        {
            final int err = gl.glGetError();
            if( GL.GL_NO_ERROR != err ) {
                throw new RuntimeException("Couldn't bind textureName "+toHexString(tex[idx])+" to 2D target, err "+toHexString(err));
            }
        }
        // gl.glTexParameterf(GLES2.GL_TEXTURE_EXTERNAL_OES, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        // gl.glTexParameterf(GLES2.GL_TEXTURE_EXTERNAL_OES, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameterf(GLES2.GL_TEXTURE_EXTERNAL_OES, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameterf(GLES2.GL_TEXTURE_EXTERNAL_OES, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        // Clamp to edge is only option.
        gl.glTexParameteri(GLES2.GL_TEXTURE_EXTERNAL_OES, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GLES2.GL_TEXTURE_EXTERNAL_OES, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
                
        SurfaceTexture stex = new SurfaceTexture(tex[idx]);
        stex.setOnFrameAvailableListener(onFrameAvailableListener);
        
        return new AndroidTextureFrame( com.jogamp.opengl.util.texture.TextureIO.newTexture(tex[idx],
                                     GLES2.GL_TEXTURE_EXTERNAL_OES,
                                     width, height,
                                     width, height,
                                     true), stex);
    }

    protected OnFrameAvailableListener onFrameAvailableListener = new OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            frameNumber++;
            updateSurface = true;
        }        
    };
        
    @Override
    public float getPlaySpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    private float[] mSTMatrix = new float[16];
    
    @Override
    public Texture getLastTextureID() {
        if(updateSurface) {
            androidTextureFrame.getSurfaceTexture().updateTexImage();
            androidTextureFrame.getSurfaceTexture().getTransformMatrix(mSTMatrix);
            TextureCoords tc = androidTextureFrame.getTexture().getImageTexCoords();
            PMVMatrix pmv;
        }
        return androidTextureFrame.getTexture();
    }

}
