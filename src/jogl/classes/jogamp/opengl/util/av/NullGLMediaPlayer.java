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
import com.jogamp.common.util.IOUtil;
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
    private TextureSequence.TextureFrame frame = null;
    private int pos_ms = 0;
    private int pos_start = 0;
    
    public NullGLMediaPlayer() {
        super();
        this.setTextureCount(1);
    }

    @Override
    protected boolean setPlaySpeedImpl(float rate) {
        return false;
    }

    @Override
    protected boolean startImpl() {
        pos_start = (int)System.currentTimeMillis();
        return true;
    }

    @Override
    protected boolean pauseImpl() {
        return true;
    }

    @Override
    protected boolean stopImpl() {
        return true;
    }
    
    @Override
    protected int seekImpl(int msec) {
        pos_ms = msec;
        validatePos();
        return pos_ms;
    }
    
    @Override
    protected TextureSequence.TextureFrame getLastTextureImpl() {
        return frame;
    }

    @Override
    protected TextureSequence.TextureFrame getNextTextureImpl(GL gl, boolean blocking) {
        return frame;
    }
    
    @Override
    protected int getCurrentPositionImpl() {
        pos_ms = (int)System.currentTimeMillis() - pos_start;
        validatePos();
        return pos_ms;
    }

    @Override
    protected void destroyImpl(GL gl) {
    }
    
    @Override
    protected void initGLStreamImpl(GL gl, int[] texNames) throws IOException {
        try {
            URLConnection urlConn = IOUtil.getResource("jogl/util/data/av/test-ntsc01-160x90.png", this.getClass().getClassLoader());
            if(null != urlConn) {
                texData = TextureIO.newTextureData(GLProfile.getGL2ES2(), urlConn.getInputStream(), false, TextureIO.PNG);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null != texData) {
            width = texData.getWidth();
            height = texData.getHeight();            
        } else {
            width = 640;
            height = 480;
            ByteBuffer buffer = Buffers.newDirectByteBuffer(width*height*4);
            while(buffer.hasRemaining()) {
                buffer.put((byte) 0xEA); buffer.put((byte) 0xEA); buffer.put((byte) 0xEA); buffer.put((byte) 0xEA);
            }
            buffer.rewind();
            texData = new TextureData(GLProfile.getGL2ES2(),
                   GL.GL_RGBA, width, height, 0,
                   GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, false, 
                   false, false, buffer, null);
        }
        fps = 24f;
        duration = 10*60*1000; // msec
        totalFrames = (int) ( (duration/1000)*fps );
        vcodec = "png-static";
    }
    
    @Override
    protected TextureSequence.TextureFrame createTexImage(GL gl, int idx, int[] tex) {
        Texture texture = super.createTexImageImpl(gl, idx, tex, width, height, false);
        if(null != texData) {
            texture.updateImage(gl, texData);
            texData.destroy();
            texData = null;
        }                      
        frame = new TextureSequence.TextureFrame( texture );
        return frame;
    }
    
    @Override
    protected void destroyTexImage(GL gl, TextureSequence.TextureFrame imgTex) {
        frame = null;
        super.destroyTexImage(gl, imgTex);
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
