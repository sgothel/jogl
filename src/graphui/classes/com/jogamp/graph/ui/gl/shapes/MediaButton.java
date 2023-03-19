/**
 * Copyright 2014-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.ui.gl.shapes;

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
 * A GraphUI {@link GLMediaPlayer} based {@link TexSeqButton} {@link Shape}.
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * This button is rendered with a round oval shape.
 * To render it rectangular, {@link #setCorner(float)} to zero.
 * </p>
 */
public class MediaButton extends TexSeqButton {
    private boolean verbose = false;

    /**
     * @param factory
     * @param renderModes
     * @param width
     * @param height
     * @param mPlayer
     * @param mPlayerListener
     */
    public MediaButton(final Factory<? extends Vertex> factory, final int renderModes,
                             final float width, final float height,
                             final GLMediaPlayer mPlayer) {
        super(factory, renderModes, width, height, mPlayer);
        setColor(0.8f, 0.8f, 0.8f, 1.0f);
        setPressedColorMod(1.1f, 1.1f, 1.1f, 0.7f);
        setToggleOffColorMod(0.8f, 0.8f, 0.8f, 1.0f);
        setToggleOnColorMod(1.0f, 1.0f, 1.0f, 1.0f);
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
                if( verbose ) {
                    System.err.println("MediaButton AttributesChanges: events_mask 0x"+Integer.toHexString(event_mask)+", when "+when);
                    System.err.println("MediaButton State: "+mp);
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_INIT & event_mask ) ) {
                    resetGL = true;
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_SIZE & event_mask ) ) {
                    // FIXME: mPlayer.resetGLState();
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_EOS & event_mask ) ) {
                    new InterruptSource.Thread() {
                        @Override
                        public void run() {
                            // loop for-ever ..
                            mp.seek(0);
                            mp.resume();
                        } }.start();
                } else if( 0 != ( GLMediaEventListener.EVENT_CHANGE_ERR & event_mask ) ) {
                    final StreamException se = mp.getStreamException();
                    if( null != se ) {
                        se.printStackTrace();
                    }
                }
            } };


    @Override
    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        ((GLMediaPlayer)texSeq).destroy(gl);
    }

    volatile boolean resetGL = true;

    @Override
    public void draw(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        final GLMediaPlayer mPlayer = (GLMediaPlayer)texSeq;
        if( resetGL ) {
            resetGL = false;
            try {
                mPlayer.initGL(gl);
                region.markShapeDirty(); // reset texture data
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        super.draw(gl, renderer, sampleCount);
        markStateDirty(); // keep on going
    };

}
