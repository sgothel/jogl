package com.jogamp.opengl.av;

import java.io.IOException;
import java.net.URL;

import javax.media.opengl.GL;

import com.jogamp.opengl.util.texture.Texture;

/**
 * Lifecycle of an GLMediaPlayer:
 * <ul>
 *   <li>{@link #setStream(GL, URL)}</li>
 *   <li>{@link #start()}</li>
 *   <li>{@link #stop()}</li>
 *   <li>{@link #destroy(GL)}</li>
 * </ul>
 */
public interface GLMediaPlayer {

    public static class TextureFrame {
        public TextureFrame(Texture t) {
            texture = t;
        }
        
        public final Texture getTexture() { return texture; }
        
        public String toString() {
            return "TextureFrame[" + texture + "]";
        }
        protected final Texture texture;
    }
    
    /** Sets the stream to be used. Initializes all stream related states and GL resources. */
    public void setStream(GL gl, URL url) throws IOException;

    /** Releases the GL and stream resources. */
    public void destroy(GL gl);

    public void setPlaySpeed(float rate);

    public float getPlaySpeed();

    public void start();

    public void pause();

    public void stop();

    /**
     * @return time current position in milliseconds 
     **/
    public int getCurrentPosition();

    /**
     * @param msec absolute desired time position in milliseconds 
     * @return time current position in milliseconds, after seeking to the desired position  
     **/
    public int seek(int msec);

    public Texture getLastTextureID();

    public Texture getNextTextureID();

    public boolean isValid();

    public URL getURL();

    public String getVideoCodec();

    public String getAudioCodec();

    public long getTotalFrames();

    public long getBitrate();

    public int getFramerate();

    public int getWidth();

    public int getHeight();

    public String toString();

    public void addEventListener(GLMediaEventListener l);

    public void removeEventListener(GLMediaEventListener l);

    public GLMediaEventListener[] getEventListeners();
}
