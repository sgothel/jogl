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
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLES2;
import com.jogamp.opengl.GLException;
import com.jogamp.common.os.AndroidVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.opengl.util.TimeFrameI;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureSequence;

import jogamp.common.os.PlatformPropsImpl;
import jogamp.common.os.android.StaticContext;
import jogamp.opengl.util.av.GLMediaPlayerImpl;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.view.Surface;

/***
 * Android implementation utilizes API level 14 (4.0.? ICS) features
 * as listed below.
 * <p>
 * Implementation is single threaded only, since we are not able to utilize multiple textures.
 * We would need to add an implementation for API level 16 using MediaCodec/MediaExtractor
 * to expose multithreading on multiple surface/textures.
 * </p>
 * <p>
 * We utilize the {@link MediaPlayer} with direct to texture streaming.
 * The MediaPlayer uses <code>libstagefright</code> to access the OpenMAX AL implementation
 * for hardware decoding.
 * </p>
 * <ul>
 *   <li>Android API Level 14: {@link MediaPlayer#setSurface(Surface)}</li>
 *   <li>Android API Level 14: {@link Surface#Surface(android.graphics.SurfaceTexture)}</li>
 * </ul>
 * <p>
 * Since the MediaPlayer API can only deal w/ <i>one</i> SurfaceTexture,
 * we enforce <code>textureCount</code> = 2 via {@link #validateTextureCount(int)}
 * and duplicate the single texture via {@link #createTexFrames(GL, int)} .. etc.
 * Two instanceds of TextureFrame are required due our framework implementation w/ Ringbuffer and 'lastFrame' access.
 * </p>
 */
public class AndroidGLMediaPlayerAPI14 extends GLMediaPlayerImpl {
    static final boolean available;

    static {
        boolean _avail = false;
        if(PlatformPropsImpl.OS_TYPE.equals(Platform.OSType.ANDROID)) {
            if(AndroidVersion.SDK_INT >= 14) {
                _avail = true;
            }
        }
        available = _avail;
    }

    public static final boolean isAvailable() { return available; }

    private MediaPlayer mp;
    private Camera cam;
    private long playStart = 0;
    private volatile boolean updateSurface = false;
    private final Object updateSurfaceLock = new Object();
    private SurfaceTextureFrame singleSTexFrame = null;
    private int sTexFrameCount = 0;
    private boolean sTexFrameAttached = false;
    private volatile boolean eos = false;

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
    }

    @Override
    protected final boolean setPlaySpeedImpl(final float rate) {
        // FIXME
        return false;
    }

    @Override
    protected final boolean setAudioVolumeImpl(final float v) {
        if(null != mp) {
            try {
                mp.setVolume(v, v);
                return true;
            } catch (final IllegalStateException ise) {
                if(DEBUG) {
                    ise.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    protected final boolean playImpl() {
        playStart = Platform.currentTimeMillis();
        if(null != mp) {
            try {
                mp.start();
                eos = false;
                mp.setOnCompletionListener(onCompletionListener);
                return true;
            } catch (final IllegalStateException ise) {
                if(DEBUG) {
                    ise.printStackTrace();
                }
            }
        } else if( null != cam ) {
            try {
                if( sTexFrameAttached ) {
                    cam.startPreview();
                }
                return true;
            } catch (final IllegalStateException ise) {
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
            } catch (final IllegalStateException ise) {
                if(DEBUG) {
                    ise.printStackTrace();
                }
            }
        } else if( null != cam ) {
            wakeUp(false);
            try {
                cam.stopPreview();
                return true;
            } catch (final IllegalStateException ise) {
                if(DEBUG) {
                    ise.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    protected final int seekImpl(final int msec) {
        if(null != mp) {
            mp.seekTo(msec);
            return mp.getCurrentPosition();
        }
        return 0;
    }

    private void wakeUp(final boolean newFrame) {
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
    protected final void destroyImpl(final GL gl) {
        if(null != mp) {
            wakeUp(false);
            try {
                mp.stop();
            } catch (final IllegalStateException ise) {
                if(DEBUG) {
                    ise.printStackTrace();
                }
            }
            mp.release();
            mp = null;
        }
        if( null != cam ) {
            wakeUp(false);
            try {
                cam.stopPreview();
            } catch (final IllegalStateException ise) {
                if(DEBUG) {
                    ise.printStackTrace();
                }
            }
            cam.release();
            cam = null;
        }
    }

    public static class SurfaceTextureFrame extends TextureSequence.TextureFrame {
        public SurfaceTextureFrame(final Texture t, final SurfaceTexture stex) {
            super(t);
            this.surfaceTex = stex;
        }

        public String toString() {
            return "SurfaceTextureFrame[pts " + pts + " ms, l " + duration + " ms, texID "+ texture.getTextureObject() + ", " + surfaceTex + "]";
        }
        public final SurfaceTexture surfaceTex;
    }

    @Override
    protected final void initStreamImpl(final int vid, final int aid) throws IOException {
        if( null == getUri() ) {
            return;
        }
        if( null == mp && null == cam ) {
            if( null == cameraPath ) {
                mp = new MediaPlayer();
            } else {
                int cameraId = 0;
                try {
                    cameraId = Integer.parseInt(cameraPath.decode());
                } catch (final NumberFormatException nfe) {}
                if( 0 <= cameraId && cameraId < Camera.getNumberOfCameras() ) {
                    cam = Camera.open(cameraId);
                } else {
                    cam = Camera.open();
                }
            }
        }

        if(null!=mp) {
            if( GLMediaPlayer.STREAM_ID_NONE == aid ) {
                mp.setVolume(0f, 0f);
                // FIXME: Disable audio handling
            } // else FIXME: Select aid !
            // Note: Both FIXMEs seem to be n/a via Android's MediaPlayer -> Switch to API level 16 MediaCodec/MediaExtractor ..
            try {
                final Uri _uri = Uri.parse(getUri().toString());
                mp.setDataSource(StaticContext.getContext(), _uri);
            } catch (final IllegalArgumentException e) {
                throw new RuntimeException(e);
            } catch (final SecurityException e) {
                throw new RuntimeException(e);
            } catch (final IllegalStateException e) {
                throw new RuntimeException(e);
            }
            mp.setSurface(null);
            try {
                mp.prepare();
            } catch (final IOException ioe) {
                throw new IOException("MediaPlayer failed to process stream <"+getUri().toString()+">: "+ioe.getMessage(), ioe);
            }
            final int r_aid = GLMediaPlayer.STREAM_ID_NONE == aid ? GLMediaPlayer.STREAM_ID_NONE : 1 /* fake */;
            final String icodec = "android";
            updateAttributes(0 /* fake */, r_aid,
                             mp.getVideoWidth(), mp.getVideoHeight(), 0,
                             0, 0, 0f,
                             0, 0, mp.getDuration(), icodec, icodec);
            /**
                mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(final MediaPlayer mp) {
                        final int r_aid = GLMediaPlayer.STREAM_ID_NONE == aid ? GLMediaPlayer.STREAM_ID_NONE : 1; // fake
                        final String icodec = "android";
                        updateAttributes(0, r_aid, // fake
                                         mp.getVideoWidth(), mp.getVideoHeight(), 0,
                                         0, 0, 0f,
                                         0, 0, mp.getDuration(), icodec, icodec);
                    }
                });
                mp.prepareAsync();
             *
             */
        } else if( null != cam ) {
            final String icodec = "android";
            final int[] fpsRange = { 0, 0 };
            final Camera.Parameters p = cam.getParameters();
            p.getPreviewFpsRange(fpsRange);
            final Camera.Size size = p.getPreviewSize();
            if( DEBUG ) {
                final int picFmt = p.getPictureFormat();
                final Camera.Size prefSize = p.getPreferredPreviewSizeForVideo();
                System.err.println("MediaPlayer.Camera: fps "+fpsRange[0]+".."+fpsRange[1]+", size[pref "+camSz2Str(prefSize)+", cur "+camSz2Str(size)+"], fmt "+picFmt);
                final List<Camera.Size> supSizes = p.getSupportedVideoSizes();
                if( null != supSizes  ) {
                    for(int i=0; i<supSizes.size(); i++) {
                        System.err.println("size #"+i+": "+camSz2Str(supSizes.get(i)));
                    }
                }
            }
            updateAttributes(0 /* fake */, GLMediaPlayer.STREAM_ID_NONE,
                             size.width, size.height,
                             0, 0, 0,
                             fpsRange[1]/1000f,
                             0, 0, 0, icodec, icodec);
        }
    }
    private static String camSz2Str(final Camera.Size csize) {
        if( null != csize ) {
            return csize.width+"x"+csize.height;
        } else {
            return "n/a";
        }
    }
    @Override
    protected final void initGLImpl(final GL gl) throws IOException, GLException {
        // NOP
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@link #TEXTURE_COUNT_MIN}, using a single texture
     * </p>
     */
    @Override
    protected int validateTextureCount(final int desiredTextureCount) {
        return TEXTURE_COUNT_MIN;
    }

    @Override
    protected final int getNextTextureImpl(final GL gl, final TextureFrame nextFrame) throws InterruptedException {
        int pts = TimeFrameI.INVALID_PTS;
        if(null != mp || null != cam) {
            final SurfaceTextureFrame sTexFrame = null != nextFrame ? (SurfaceTextureFrame) nextFrame : singleSTexFrame;
            final SurfaceTexture surfTex = sTexFrame.surfaceTex;
            if( !sTexFrameAttached ) {
                sTexFrameAttached = true;
                final Surface surface;
                if( null != mp ) {
                    surface = new Surface(sTexFrame.surfaceTex);
                    mp.setSurface(surface);
                } else {
                    surface = null;
                    try {
                        cam.setPreviewTexture(sTexFrame.surfaceTex);
                        cam.startPreview();
                    } catch (final IOException ioe) {
                        throw new RuntimeException("MediaPlayer failed to process stream <"+getUri().toString()+">: "+ioe.getMessage(), ioe);
                    }
                }
                if( null != surface ) {
                    surface.release();
                }
                surfTex.setOnFrameAvailableListener(onFrameAvailableListener);
            }
            if( eos || (null != mp && !mp.isPlaying() ) ) {
                eos = true;
                pts = TimeFrameI.END_OF_STREAM_PTS;
            } else {
                // Only block once, no while-loop.
                // This relaxes locking code of non crucial resources/events.
                boolean update = updateSurface;
                if( !update ) {
                    synchronized(updateSurfaceLock) {
                        while(!updateSurface) { // volatile OK.
                            updateSurfaceLock.wait(); // propagates InterruptedException
                        }
                        update = updateSurface;
                        updateSurface = false;
                    }
                }
                if(update) {
                    surfTex.updateTexImage();
                    // nextFrame.setPTS( (int) ( nextSTex.getTimestamp() / 1000000L ) ); // nano -9 -> milli -3
                    if( null != mp ) {
                        pts = mp.getCurrentPosition();
                    } else {
                        pts = (int) ( Platform.currentTimeMillis() - playStart );
                    }
                    // stex.getTransformMatrix(atex.getSTMatrix());
                }
            }
            sTexFrame.setPTS( pts );
        }
        return pts;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Creates only one single texture and duplicated content to 2 TextureFrames
     * </p>
     */
    @Override
    protected TextureFrame[] createTexFrames(final GL gl, final int count) {
        final int[] texNames = new int[1];
        gl.glGenTextures(1, texNames, 0);
        final int err = gl.glGetError();
        if( GL.GL_NO_ERROR != err ) {
            throw new RuntimeException("TextureNames creation failed (num: 1/"+count+"): err "+toHexString(err));
        }
        final TextureFrame[] texFrames = new TextureFrame[count];
        for(int i=0; i<count; i++) {
            texFrames[i] = createTexImage(gl, texNames[0]);
        }
        return texFrames;
    }
    /**
     * {@inheritDoc}
     * <p>
     * Returns the single texture, which is created at 1st call.
     * </p>
     */
    @Override
    protected final TextureSequence.TextureFrame createTexImage(final GL gl, final int texName) {
        sTexFrameCount++;
        if( 1 == sTexFrameCount ) {
            singleSTexFrame = new SurfaceTextureFrame( createTexImageImpl(gl, texName, getWidth(), getHeight()), new SurfaceTexture(texName) );
        }
        return singleSTexFrame;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Destroys the single texture at last call.
     * </p>
     */
    @Override
    protected final void destroyTexFrame(final GL gl, final TextureSequence.TextureFrame frame) {
        sTexFrameCount--;
        if( 0 == sTexFrameCount ) {
            singleSTexFrame = null;
            sTexFrameAttached = false;
            final SurfaceTextureFrame sFrame = (SurfaceTextureFrame) frame;
            sFrame.surfaceTex.release();
            super.destroyTexFrame(gl, frame);
        }
    }

    private final OnFrameAvailableListener onFrameAvailableListener = new OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
            wakeUp(true);
        }
    };

    private final OnCompletionListener onCompletionListener = new OnCompletionListener() {
        @Override
        public void onCompletion(final MediaPlayer mp) {
            eos = true;
        }
    };
}
