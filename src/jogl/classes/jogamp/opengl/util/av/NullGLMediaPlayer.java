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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

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
    protected final boolean setPlaySpeedImpl(final float rate) {
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
    protected final int seekImpl(final int msec) {
        pos_ms = msec;
        validatePos();
        return pos_ms;
    }

    @Override
    protected final int getNextTextureImpl(final GL gl, final TextureFrame nextFrame) {
        final int pts = getAudioPTSImpl();
        nextFrame.setPTS( pts );
        return pts;
    }

    @Override
    protected final int getAudioPTSImpl() {
        pos_ms = (int) ( Platform.currentTimeMillis() - pos_start );
        validatePos();
        return pos_ms;
    }

    @Override
    protected final void destroyImpl(final GL gl) {
        if(null != texData) {
            texData.destroy();
            texData = null;
        }
    }

    public final static TextureData createTestTextureData() {
        TextureData res = null;
        try {
            final URLConnection urlConn = IOUtil.getResource("jogl/util/data/av/test-ntsc01-28x16.png", NullGLMediaPlayer.class.getClassLoader());
            if(null != urlConn) {
                res = TextureIO.newTextureData(GLProfile.getGL2ES2(), urlConn.getInputStream(), false, TextureIO.PNG);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        if(null == res) {
            final int w = 160;
            final int h =  90;
            final ByteBuffer buffer = Buffers.newDirectByteBuffer(w*h*4);
            while(buffer.hasRemaining()) {
                buffer.put((byte) 0xEA); buffer.put((byte) 0xEA); buffer.put((byte) 0xEA); buffer.put((byte) 0xEA);
            }
            buffer.rewind();
            res = new TextureData(GLProfile.getGL2ES2(),
                   GL.GL_RGBA, w, h, 0,
                   GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, false,
                   false, false, buffer, null);
        }
        return res;
    }

    @Override
    protected final void initStreamImpl(final int vid, final int aid) throws IOException {
        texData = createTestTextureData();
        final float _fps = 24f;
        final int _duration = 10*60*1000; // msec
        final int _totalFrames = (int) ( (_duration/1000)*_fps );
        updateAttributes(0 /* fake */, GLMediaPlayer.STREAM_ID_NONE,
                         texData.getWidth(), texData.getHeight(), 0,
                         0, 0, _fps,
                         _totalFrames, 0, _duration, "png-static", null);
    }
    @Override
    protected final void initGLImpl(final GL gl) throws IOException, GLException {
        setIsGLOriented(true);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@link GLMediaPlayer#TEXTURE_COUNT_MIN}.
     * </p>
     */
    @Override
    protected int validateTextureCount(final int desiredTextureCount) {
        return TEXTURE_COUNT_MIN;
    }

    @Override
    protected final TextureSequence.TextureFrame createTexImage(final GL gl, final int texName) {
        final Texture texture = super.createTexImageImpl(gl, texName, getWidth(), getHeight());
        if(null != texData) {
            texture.updateImage(gl, texData);
        }
        return new TextureSequence.TextureFrame( texture );
    }

    @Override
    protected final void destroyTexFrame(final GL gl, final TextureSequence.TextureFrame frame) {
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
        if( considerPausing && State.Playing == getState() ) {
            setState(State.Paused);
        }
    }
}
