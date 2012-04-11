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
package com.jogamp.opengl.av;

import java.io.IOException;
import java.net.URLConnection;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;

import jogamp.opengl.Debug;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;

/**
 * Lifecycle of an GLMediaPlayer:
 * <table border="1">
 *   <tr><th>action</th>                                   <th>state before</th>        <th>state after</th></tr>
 *   <tr><td>{@link #initGLStream(GL, URLConnection)}</td> <td>Uninitialized</td>       <td>Stopped</td></tr>
 *   <tr><td>{@link #start()}</td>                         <td>Stopped, Paused</td>     <td>Playing</td></tr>
 *   <tr><td>{@link #stop()}</td>                          <td>Playing, Paused</td>     <td>Stopped</td></tr>
 *   <tr><td>{@link #pause()}</td>                         <td>Playing</td>             <td>Paused</td></tr>
 *   <tr><td>{@link #destroy(GL)}</td>                     <td>ANY</td>                 <td>Uninitialized</td></tr>
 * </table>
 */
public interface GLMediaPlayer {
    public static final boolean DEBUG = Debug.debug("GLMediaPlayer");
    
    public enum State {
        Uninitialized(0), Stopped(1), Playing(2), Paused(3); 
        
        public final int id;

        State(int id){
            this.id = id;
        }
    }
    
    public static class TextureFrame {
        public TextureFrame(Texture t) {
            texture = t;
            // stMatrix = new float[4*4];
            // ProjectFloat.makeIdentityf(stMatrix, 0);
        }
        
        public final Texture getTexture() { return texture; }
        // public final float[] getSTMatrix() { return stMatrix; }
        
        public String toString() {
            return "TextureFrame[" + texture + "]";
        }
        protected final Texture texture;
        // protected final float[] stMatrix;
    }
    
    public int getTextureCount();
    
    public int getTextureTarget();
    
    /** Defaults to 0 */
    public void setTextureUnit(int u);
    public int getTextureUnit();
    
    /** Sets the texture min-mag filter, defaults to {@link GL#GL_NEAREST}. */
    public void setTextureMinMagFilter(int[] minMagFilter);
    public int[] getTextureMinMagFilter();
    
    /** Sets the texture min-mag filter, defaults to {@link GL#GL_CLAMP_TO_EDGE}. */
    public void setTextureWrapST(int[] wrapST);
    public int[] getTextureWrapST();
    
    /** 
     * Sets the stream to be used. Initializes all stream related states inclusive OpenGL ones,
     * if <code>gl</code> is not null.
     * <p>
     * Uninitialized -> Stopped
     * </p>
     * @param gl current GL object. If null, no video output and textures will be available.
     * @param urlConn the stream connection
     * @return the new state
     * 
     * @throws IllegalStateException if not invoked in state Uninitialized 
     * @throws IOException in case of difficulties to open or process the stream
     * @throws GLException in case of difficulties to initialize the GL resources
     */
    public State initGLStream(GL gl, URLConnection urlConn) throws IllegalStateException, GLException, IOException;
    
    /**
     * Releases the GL and stream resources.
     * <p>
     * <code>ANY</code> -> Uninitialized
     * </p>
     */
    public State destroy(GL gl);

    public void setPlaySpeed(float rate);

    public float getPlaySpeed();

    /**
     * Stopped/Paused -> Playing
     */
    public State start();

    /**
     * Playing -> Paused
     */
    public State pause();

    /**
     * Playing/Paused -> Stopped
     */
    public State stop();
    
    /**
     * @return the current state, either Uninitialized, Stopped, Playing, Paused
     */
    public State getState();
    
    /**
     * @return time current position in milliseconds 
     **/
    public long getCurrentPosition();

    /**
     * Allowed in state Stopped, Playing and Paused, otherwise ignored.
     * 
     * @param msec absolute desired time position in milliseconds 
     * @return time current position in milliseconds, after seeking to the desired position  
     **/
    public long seek(long msec);

    /**
     * @return the last updated texture. Maybe <code>null</code> in case no last frame is available. 
     *         Not blocking. 
     */
    public TextureFrame getLastTexture();
    
    /**
     * Returns the next texture to be rendered. 
     * <p>
     * Implementation shall block until next frame is available if <code>blocking</code> is <code>true</code>,
     * otherwise it shall return the last frame in case a new frame is not available.
     * </p>
     * <p>
     * Shall return <code>null</code> in case <i>no</i> frame is available.
     * </p>
     * 
     * @see #addEventListener(GLMediaEventListener)
     * @see GLMediaEventListener#newFrameAvailable(GLMediaPlayer, long)
     */
    public TextureFrame getNextTexture(GL gl, boolean blocking);
    
    public TextureCoords getTextureCoords();
    
    public URLConnection getURLConnection();

    /**
     * <i>Warning:</i> Optional information, may not be supported by implementation.
     * @return the code of the video stream, if available 
     */
    public String getVideoCodec();

    /**
     * <i>Warning:</i> Optional information, may not be supported by implementation.
     * @return the code of the audio stream, if available 
     */
    public String getAudioCodec();

    /**
     * <i>Warning:</i> Optional information, may not be supported by implementation.
     * @return the total number of video frames
     */
    public long getTotalFrames();

    /**
     * @return total duration of stream in msec.
     */
    public long getDuration();
    
    /**
     * <i>Warning:</i> Optional information, may not be supported by implementation.
     * @return the overall bitrate of the stream.  
     */
    public long getBitrate();

    /**
     * <i>Warning:</i> Optional information, may not be supported by implementation.
     * @return the framerate of the video
     */
    public int getFramerate();

    public int getWidth();

    public int getHeight();

    public String toString();

    public void addEventListener(GLMediaEventListener l);

    public void removeEventListener(GLMediaEventListener l);

    public GLMediaEventListener[] getEventListeners();
}
