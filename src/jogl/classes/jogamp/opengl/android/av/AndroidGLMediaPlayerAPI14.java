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
import javax.media.opengl.GLES2;
import javax.media.opengl.GLException;

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.opengl.util.TimeFrameI;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureSequence;

import jogamp.common.os.android.StaticContext;
import jogamp.opengl.util.av.GLMediaPlayerImpl;

import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;

/***
 * Android implementation utilizes API level 14 (4.0.? ICS) features
 * as listed below.
 * <p>
 * We utilize the {@link MediaPlayer} with direct to texture streaming.
 * The MediaPlayer uses <code>libstagefright</code> to access the OpenMAX AL implementation
 * for hardware decoding.
 * </p>
 * <ul>
 *   <li>Android API Level 14: {@link MediaPlayer#setSurface(Surface)}</li>
 *   <li>Android API Level 14: {@link Surface#Surface(android.graphics.SurfaceTexture)}</li>
 * </ul>
 */
public class AndroidGLMediaPlayerAPI14 extends GLMediaPlayerImpl {
    static final boolean available;
    
    static {
        boolean _avail = false;
        if(Platform.OS_TYPE.equals(Platform.OSType.ANDROID)) {
            if(AndroidVersion.SDK_INT >= 14) {
                _avail = true;
            }
        }
        available = _avail;
    }
    
    public static final boolean isAvailable() { return available; }
    
    MediaPlayer mp;
    volatile boolean updateSurface = false;
    Object updateSurfaceLock = new Object();

    /**
    private static String toString(MediaPlayer m) {
        if(null == m) return "<nil>";
        return "MediaPlayer[playing "+m.isPlaying()+", pos "+m.getCurrentPosition()/1000.0f+"s, "+m.getVideoWidth()+"x"+m.getVideoHeight()+"]";
    } */
    
    public AndroidGLMediaPlayerAPI14() {
        super();
        if(!available) {
            throw new RuntimeException("AndroidGLMediaPlayerAPI14 not available");
        }
        this.setTextureTarget(GLES2.GL_TEXTURE_EXTERNAL_OES);
        mp = new MediaPlayer();
    }

    @Override
    protected final boolean setPlaySpeedImpl(float rate) {
        // FIXME
        return false;
    }

    @Override
    protected final boolean setAudioVolumeImpl(float v) {
        if(null != mp) {        
            try {
                mp.setVolume(v, v);
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
    protected final boolean playImpl() {
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
    protected final boolean pauseImpl() {
        if(null != mp) {
            wakeUp(false);
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
    protected final int seekImpl(int msec) {
        if(null != mp) {
            mp.seekTo(msec);
            return mp.getCurrentPosition();
        }
        return 0;
    }

    private void wakeUp(boolean newFrame) {
        synchronized(updateSurfaceLock) {
            if(newFrame) {
                updateSurface = true;
            }
            updateSurfaceLock.notifyAll();
        }
    }
    
    @Override
    protected final int getAudioPTSImpl() { return null != mp ? mp.getCurrentPosition() : 0; }    

    @Override
    protected final void destroyImpl(GL gl) {
        if(null != mp) {
            wakeUp(false);
            try {
                mp.stop();
            } catch (IllegalStateException ise) {
                if(DEBUG) {
                    ise.printStackTrace();
                }
            }
            mp.release();
            mp = null;
        }
    }
    
    SurfaceTexture stex = null;
    public static class SurfaceTextureFrame extends TextureSequence.TextureFrame {        
        public SurfaceTextureFrame(Texture t, SurfaceTexture stex) {
            super(t);
            this.surfaceTex = stex;
            this.surface = new Surface(stex);
        }
        
        public final SurfaceTexture getSurfaceTexture() { return surfaceTex; }
        public final Surface getSurface() { return surface; }
        
        public String toString() {
            return "SurfaceTextureFrame[pts " + pts + " ms, l " + duration + " ms, texID "+ texture.getTextureObject() + ", " + surfaceTex + "]";
        }
        private final SurfaceTexture surfaceTex;
        private final Surface surface; 
    }
    
    @Override
    protected final void initStreamImpl(int vid, int aid) throws IOException {
        if(null!=mp && null!=streamLoc) {
            if( GLMediaPlayer.STREAM_ID_NONE == aid ) {
                mp.setVolume(0f, 0f);
                // FIXME: Disable audio handling
            } // else FIXME: Select aid !
            // Note: Both FIXMEs seem to be n/a via Android's MediaPlayer -> Switch to API level 16 MediaCodec/MediaExtractor ..
            try {
                final Uri _uri = Uri.parse(streamLoc.toString());        
                mp.setDataSource(StaticContext.getContext(), _uri);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (SecurityException e) {
                throw new RuntimeException(e);
            } catch (IllegalStateException e) {
                throw new RuntimeException(e);
            }
            if( null == stex ) {
                throw new InternalError("XXX");
            }
            mp.setSurface(null);
            try {
                mp.prepare();
            } catch (IOException ioe) {
                throw new IOException("MediaPlayer failed to process stream <"+streamLoc.toString()+">: "+ioe.getMessage(), ioe);
            }
            final int r_aid = GLMediaPlayer.STREAM_ID_NONE == aid ? GLMediaPlayer.STREAM_ID_NONE : GLMediaPlayer.STREAM_ID_AUTO;
            final String icodec = "android";
            updateAttributes(GLMediaPlayer.STREAM_ID_AUTO, r_aid, 
                             mp.getVideoWidth(), mp.getVideoHeight(), 0, 
                             0, 0, 0f, 
                             0, 0, mp.getDuration(), icodec, icodec);
        }
    }
    @Override
    protected final void initGLImpl(GL gl) throws IOException, GLException {
        // NOP
    }
    
    @Override
    protected final int getNextTextureImpl(GL gl, TextureFrame nextFrame) {
        int pts = TimeFrameI.INVALID_PTS;
        if(null != stex && null != mp) {
            final SurfaceTextureFrame nextSFrame = (SurfaceTextureFrame) nextFrame;
            final Surface nextSurface = nextSFrame.getSurface();
            mp.setSurface(nextSurface);
            nextSurface.release();
            
            // Only block once, no while-loop. 
            // This relaxes locking code of non crucial resources/events.
            boolean update = updateSurface;
            if( !update ) {
                synchronized(updateSurfaceLock) {
                    if(!updateSurface) { // volatile OK.
                        try {
                            updateSurfaceLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    update = updateSurface;
                    updateSurface = false;
                }
            }
            if(update) {
                final SurfaceTexture nextSTex = nextSFrame.getSurfaceTexture(); 
                nextSTex.updateTexImage();
                // nextFrame.setPTS( (int) ( nextSTex.getTimestamp() / 1000000L ) ); // nano -9 -> milli -3
                pts = mp.getCurrentPosition();
                nextFrame.setPTS( pts );
                // stex.getTransformMatrix(atex.getSTMatrix());
            }
        }
        return pts;
    }
    
    @Override
    protected final TextureSequence.TextureFrame createTexImage(GL gl, int texName) {
        if( null != stex ) {
            throw new InternalError("XXX");
        }
        stex = new SurfaceTexture(texName); // only 1 texture
        stex.setOnFrameAvailableListener(onFrameAvailableListener);
        return new TextureSequence.TextureFrame( createTexImageImpl(gl, texName, width, height, true) );
    }
    
    @Override
    protected final void destroyTexFrame(GL gl, TextureSequence.TextureFrame imgTex) {
        if(null != stex) {
            stex.release();
            stex = null;
        }
        super.destroyTexFrame(gl, imgTex);
    }
    
    protected OnFrameAvailableListener onFrameAvailableListener = new OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            wakeUp(true);
        }        
    };
}
