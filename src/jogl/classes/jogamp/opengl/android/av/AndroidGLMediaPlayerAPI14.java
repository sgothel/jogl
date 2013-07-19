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

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.common.os.Platform;
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
    TextureSequence.TextureFrame lastTexFrame = null;

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
        this.setTextureCount(1);
        mp = new MediaPlayer();
    }

    @Override
    protected boolean setPlaySpeedImpl(float rate) {
        return false;
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
    protected boolean stopImpl() {
        if(null != mp) {
            wakeUp(false);
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
    protected int seekImpl(int msec) {
        if(null != mp) {
            mp.seekTo(msec);
            return mp.getCurrentPosition();
        }
        return 0;
    }

    @Override
    protected TextureSequence.TextureFrame getLastTextureImpl() {
        return lastTexFrame;
    }

    @Override
    protected TextureSequence.TextureFrame getNextTextureImpl(GL gl, boolean blocking) {
        if(null != stex && null != mp) {
            // Only block once, no while-loop. 
            // This relaxes locking code of non crucial resources/events.
            boolean update = updateSurface;
            if(!update && blocking) {
                synchronized(updateSurfaceLock) {
                    if(!updateSurface) { // volatile OK.
                        try {
                            updateSurfaceLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    updateSurface = false;
                    update = true;
                }
            }
            if(update) {
                stex.updateTexImage();
                // stex.getTransformMatrix(atex.getSTMatrix());
                lastTexFrame=texFrames[0];
            }
            
        }
        return lastTexFrame;
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
    protected int getCurrentPositionImpl() {
        return null != mp ? mp.getCurrentPosition() : 0;
    }

    @Override
    protected void destroyImpl(GL gl) {
        if(null != mp) {
            wakeUp(false);
            mp.release();
            mp = null;
        }
    }
    
    SurfaceTexture stex = null;
    
    @Override
    protected void initGLStreamImpl(GL gl, int[] texNames) throws IOException {
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
            stex = new SurfaceTexture(texNames[0]); // only 1 texture
            stex.setOnFrameAvailableListener(onFrameAvailableListener);
            final Surface surf = new Surface(stex);
            mp.setSurface(surf);
            surf.release();
            try {
                mp.prepare();
            } catch (IOException ioe) {
                throw new IOException("MediaPlayer failed to process stream <"+urlConn.getURL().toExternalForm()+">: "+ioe.getMessage(), ioe);
            }
            updateAttributes(mp.getVideoWidth(), mp.getVideoHeight(), 
                             0, 0, 0, 
                             0f, 0, mp.getDuration(), 
                             null, null);
        }
    }
    
    @Override
    protected TextureSequence.TextureFrame createTexImage(GL gl, int idx, int[] tex) {
        lastTexFrame = new TextureSequence.TextureFrame( createTexImageImpl(gl, idx, tex, width, height, true) );
        return lastTexFrame; 
    }
    
    @Override
    protected void destroyTexImage(GL gl, TextureSequence.TextureFrame imgTex) {
        if(null != stex) {
            stex.release();
            stex = null;
        }
        lastTexFrame = null;
        super.destroyTexImage(gl, imgTex);
    }
    
    protected OnFrameAvailableListener onFrameAvailableListener = new OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            wakeUp(true);
            AndroidGLMediaPlayerAPI14.this.newFrameAvailable();
        }        
    };        
}
