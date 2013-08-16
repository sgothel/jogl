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
package jogamp.opengl.util.av;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GLProfile;

import jogamp.opengl.util.av.GLMediaPlayerImpl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.TextureSequence;

/***
 * A dummy null media player implementation using a static test frame
 * available on all platforms.
 */
public class NullGLMediaPlayer extends GLMediaPlayerImpl {
    private TextureData texData = null;
    private int pos_ms = 0;
    private long pos_start = 0;
    
    public NullGLMediaPlayer() {
        super();
    }

    @Override
    protected final boolean setPlaySpeedImpl(float rate) {
        return false;
    }

    @Override
    protected final boolean playImpl() {
        pos_start = Platform.currentTimeMillis();
        return true;
    }

    @Override
    protected final boolean pauseImpl() {
        return true;
    }

    @Override
    protected final int seekImpl(int msec) {
        pos_ms = msec;
        validatePos();
        return pos_ms;
    }
    
    @Override
    protected final boolean getNextTextureImpl(GL gl, TextureFrame nextFrame, boolean blocking, boolean issuePreAndPost) {
        nextFrame.setPTS( getAudioPTSImpl() );
        return true;
    }
    
    @Override
    protected final int getAudioPTSImpl() { 
        pos_ms = (int) ( Platform.currentTimeMillis() - pos_start );
        validatePos();
        return pos_ms;
    }

    @Override
    protected final void destroyImpl(GL gl) {
        if(null != texData) {
            texData.destroy();
            texData = null;
        }                      
    }
        
    @Override
    protected final void initGLStreamImpl(GL gl, int vid, int aid) throws IOException {
        try {
            URLConnection urlConn = IOUtil.getResource("jogl/util/data/av/test-ntsc01-160x90.png", this.getClass().getClassLoader());
            if(null != urlConn) {
                texData = TextureIO.newTextureData(GLProfile.getGL2ES2(), urlConn.getInputStream(), false, TextureIO.PNG);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        final int _w, _h;
        if(null != texData) {
            _w = texData.getWidth();
            _h = texData.getHeight();            
        } else {
            _w = 640;
            _h = 480;
            ByteBuffer buffer = Buffers.newDirectByteBuffer(_w*_h*4);
            while(buffer.hasRemaining()) {
                buffer.put((byte) 0xEA); buffer.put((byte) 0xEA); buffer.put((byte) 0xEA); buffer.put((byte) 0xEA);
            }
            buffer.rewind();
            texData = new TextureData(GLProfile.getGL2ES2(),
                   GL.GL_RGBA, _w, _h, 0,
                   GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, false, 
                   false, false, buffer, null);
        }
        final int r_aid = GLMediaPlayer.STREAM_ID_NONE == aid ? GLMediaPlayer.STREAM_ID_NONE : GLMediaPlayer.STREAM_ID_AUTO; 
        final float _fps = 24f;
        final int _duration = 10*60*1000; // msec
        final int _totalFrames = (int) ( (_duration/1000)*_fps );
        updateAttributes(GLMediaPlayer.STREAM_ID_AUTO, r_aid, 
                         _w, _h, 0, 
                         0, 0, _fps, 
                         _totalFrames, 0, _duration, "png-static", null);
    }
    
    @Override
    protected final TextureSequence.TextureFrame createTexImage(GL gl, int texName) {
        final Texture texture = super.createTexImageImpl(gl, texName, width, height, false);
        if(null != texData) {
            texture.updateImage(gl, texData);
        }                      
        return new TextureSequence.TextureFrame( texture );
    }
    
    @Override
    protected final void destroyTexFrame(GL gl, TextureSequence.TextureFrame frame) {
        super.destroyTexFrame(gl, frame);
    }
    
    private void validatePos() {
        boolean considerPausing = false;
        if( 0 > pos_ms) {
            pos_ms = 0;
            considerPausing = true;
        } else if ( pos_ms > getDuration() ) {
            pos_ms = getDuration();
            considerPausing = true;
        }
        if(considerPausing && state == State.Playing) {
            state = State.Paused;
        }
    }
}
