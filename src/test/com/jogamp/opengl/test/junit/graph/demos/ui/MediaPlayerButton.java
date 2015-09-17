/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.graph.demos.ui;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

/**
 * GPU based resolution independent {@link GLMediaPlayer} Button impl
 */
public class MediaPlayerButton extends TextureSeqButton {
    private boolean verbose = false;

    /**
     * @param factory
     * @param renderModes
     * @param width
     * @param height
     * @param mPlayer
     * @param mPlayerListener
     */
    public MediaPlayerButton(final Factory<? extends Vertex> factory, final int renderModes,
                             final float width, final float height,
                             final GLMediaPlayer mPlayer) {
        super(factory, renderModes, width, height, mPlayer);
        setColor(0.8f, 0.8f, 0.8f, 1.0f);
        setPressedColorMod(1.1f, 1.1f, 1.1f, 0.7f);
        setToggleOffColorMod(0.8f, 0.8f, 0.8f, 1.0f);
        setToggleOnColorMod(1.0f, 1.0f, 1.0f, 1.0f);
        setEnabled(false); // data and shader n/a yet
    }

    public void setVerbose(final boolean v) { verbose = v; }

    /**
     * Add the default {@link GLMediaEventListener} to {@link #getGLMediaPlayer() this class's GLMediaPlayer}.
     */
    public void addDefaultEventListener() {
        getGLMediaPlayer().addEventListener(defGLMediaEventListener);
    }

    public final GLMediaPlayer getGLMediaPlayer() { return (GLMediaPlayer)texSeq; }

    private final GLMediaEventListener defGLMediaEventListener = new GLMediaEventListener() {
            @Override
            public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) {
                // texButton.markStateDirty();
            }

            @Override
            public void attributesChanged(final GLMediaPlayer mp, final int event_mask, final long when) {
                final GLMediaPlayer mPlayer = (GLMediaPlayer)texSeq;
                if( verbose ) {
                    System.err.println("MovieCube AttributesChanges: events_mask 0x"+Integer.toHexString(event_mask)+", when "+when);
                    System.err.println("MovieCube State: "+mp);
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_INIT & event_mask ) ) {
                    MediaPlayerButton.this.setEnabled(true); // data and shader is available ..
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_SIZE & event_mask ) ) {
                    // FIXME: mPlayer.resetGLState();
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_EOS & event_mask ) ) {
                    new InterruptSource.Thread() {
                        public void run() {
                            // loop for-ever ..
                            mPlayer.seek(0);
                            mPlayer.play();
                        } }.start();
                } else if( 0 != ( GLMediaEventListener.EVENT_CHANGE_ERR & event_mask ) ) {
                    final StreamException se = mPlayer.getStreamException();
                    if( null != se ) {
                        se.printStackTrace();
                    }
                }
            } };


    @Override
    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        ((GLMediaPlayer)texSeq).destroy(gl);
    }

    @Override
    public void drawShape(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        final GLMediaPlayer mPlayer = (GLMediaPlayer)texSeq;
        if( GLMediaPlayer.State.Initialized == mPlayer.getState() ) {
            try {
                mPlayer.initGL(gl);
                mPlayer.setAudioVolume( 0f );
                mPlayer.play();
                markStateDirty();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        super.drawShape(gl, renderer, sampleCount);
        markStateDirty(); // keep on going
    };

}
