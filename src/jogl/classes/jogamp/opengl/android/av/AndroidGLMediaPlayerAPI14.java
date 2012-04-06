/**
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package jogamp.opengl.android.av;

import java.io.IOException;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLES2;

import jogamp.common.os.android.StaticContext;
import jogamp.opengl.av.GLMediaPlayerImpl;

import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;

import com.jogamp.opengl.util.texture.Texture;

/***
 * Android API Level 14: {@link MediaPlayer#setSurface(Surface)}
 * Android API Level 14: {@link Surface#Surface(android.graphics.SurfaceTexture)}
 */
public class AndroidGLMediaPlayerAPI14 extends GLMediaPlayerImpl {
    MediaPlayer mp;
    boolean updateSurface = false;
    Object updateSurfaceLock = new Object();
    AndroidTextureFrame atex = null;
    
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
    public float getPlaySpeed() {
        return 0;
    }
    
    @Override
    protected boolean startImpl() {
        if(null != mp) {        
            try {
                mp.start();
                return true;
            } catch (IllegalStateException ise) {
                if(DEBUG) {
                    ise.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    protected boolean pauseImpl() {
        if(null != mp) {
            try {
                mp.pause();
                return true;
            } catch (IllegalStateException ise) {
                if(DEBUG) {
                    ise.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    protected boolean stopImpl() {
        if(null != mp) {
            try {
                mp.stop();
                return true;
            } catch (IllegalStateException ise) {
                if(DEBUG) {
                    ise.printStackTrace();
                }
            }
        }
        return false;
    }
    
    @Override
    protected long seekImpl(long msec) {
        if(null != mp) {
            mp.seekTo((int)msec);
            return mp.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public TextureFrame getLastTexture() {
        return atex;
    }

    @Override
    public TextureFrame getNextTexture() {
        if(null != atex && null != mp) {
            final boolean _updateSurface;
            synchronized(updateSurfaceLock) {
                _updateSurface = updateSurface;
                updateSurface = false;
            }
            if(_updateSurface) {
                atex.getSurfaceTexture().updateTexImage();
                // atex.getSurfaceTexture().getTransformMatrix(atex.getSTMatrix());
            }
            return atex;
        } else {
            return null;
        }
    }
    
    @Override
    public long getCurrentPosition() {
        return null != mp ? mp.getCurrentPosition() : 0;
    }

    @Override
    protected void destroyImpl(GL gl) {
        if(null != mp) {
            mp.release();
            mp = null;
        }
    }
    
    @Override
    protected void initStreamImplPreGL() throws IOException {
        if(null!=mp && null!=urlConn) {
            try {
                final Uri uri = Uri.parse(urlConn.getURL().toExternalForm());        
                mp.setDataSource(StaticContext.getContext(), uri);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (IllegalStateException e) {
                throw new RuntimeException(e);
            }
            try {
                mp.prepare();
            } catch (IOException ioe) {
                throw new IOException("MediaPlayer failed to process stream <"+urlConn.getURL().toExternalForm()+">: "+ioe.getMessage(), ioe);
            }
            
            width = mp.getVideoWidth();
            height = mp.getVideoHeight();
            fps = 0;
            bps = 0;
            totalFrames = 0;
            duration = mp.getDuration();
            acodec = "unknown";
            vcodec = "unknown";
        }
    }
    
    @Override
    protected void destroyTexImage(GLContext ctx, TextureFrame imgTex) {
        final AndroidTextureFrame atf = (AndroidTextureFrame) imgTex;
        atf.getSurfaceTexture().release();
        atex = null;
        super.destroyTexImage(ctx, imgTex);
    }
    
    @Override
    protected TextureFrame createTexImage(GLContext ctx, int idx, int[] tex) {
        final Texture texture = super.createTexImageImpl(ctx, idx, tex, true);
                
        final SurfaceTexture stex = new SurfaceTexture(tex[idx]);
        stex.setOnFrameAvailableListener(onFrameAvailableListener);
        final Surface surf = new Surface(stex);
        mp.setSurface(surf);
        surf.release();
        
        atex = new AndroidTextureFrame( texture, stex );
        return atex;
    }

    protected OnFrameAvailableListener onFrameAvailableListener = new OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            synchronized(updateSurfaceLock) {
                updateSurface = true;
            }
            AndroidGLMediaPlayerAPI14.this.newFrameAvailable(atex);
        }        
    };        
}
