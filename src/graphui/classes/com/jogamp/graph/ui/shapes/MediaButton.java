/**
 * Copyright 2014-2024 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.ui.shapes;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.av.AudioSink;
import com.jogamp.common.os.Clock;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.graph.ui.Scene;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.util.av.ASSEventLine;
import com.jogamp.opengl.util.av.ASSEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

/**
 * A GraphUI {@link GLMediaPlayer} based {@link TexSeqButton} {@link GraphShape}.
 * <p>
 * GraphUI is GPU based and resolution independent.
 * </p>
 * <p>
 * This button is rendered with a round oval shape.
 * To render it rectangular, {@link #setCorner(float)} to zero.
 * </p>
 * <p>
 * Default colors (toggle-on is full color):
 * - non-toggle: 1 * color
 * - pressed: 0.9 * color
 * - toggle-off: 0.8 * color
 * - toggle-on: 1.0 * color
 * </p>
 */
public class MediaButton extends TexSeqButton {
    private final boolean DEBUG_SUB = false;
    private boolean verbose = false;

    private final Label subLabel;
    private final float subZOffset;
    private boolean subEnabled;
    private float subLineHeightPct;
    private float subLineDY = 0.25f;
    private final Rectangle subBlend;
    private final List<ASSEventLine> assEventQueue = new ArrayList<ASSEventLine>();
    private final Object assEventLock = new Object();

    public MediaButton(final int renderModes, final float width, final float height, final GLMediaPlayer mPlayer) {
        this(renderModes, width, height, mPlayer, null, 0);
    }

    /**
     *
     * @param renderModes
     * @param width
     * @param height
     * @param mPlayer
     * @param subFont subtitle font
     * @param subLineHeightPct one subtitle line height percentage of this shape, default is 0.1f
     */
    public MediaButton(final int renderModes, final float width, final float height, final GLMediaPlayer mPlayer,
                       final Font subFont, final float subLineHeightPct)
    {
        super(renderModes & ~Region.AA_RENDERING_MASK, width, height, mPlayer);

        setColor(1.0f, 1.0f, 1.0f, 0.0f);
        setPressedColorMod(0.9f, 0.9f, 0.9f, 0.7f);
        setToggleOffColorMod(0.8f, 0.8f, 0.8f, 1.0f);
        setToggleOnColorMod(1.0f, 1.0f, 1.0f, 1.0f);

        mPlayer.setASSEventListener(assEventListener);

        final Font f;
        if( null != subFont ) {
            f = subFont;
            subEnabled = true;
        } else {
            f = Scene.getDefaultFont();
            subEnabled = false;
        }
        this.subZOffset = Button.DEFAULT_LABEL_ZOFFSET;
        this.subLineHeightPct = subLineHeightPct;
        this.subLabel = new Label(renderModes, f, "");
        this.subLabel.setColor( new Vec4f( 1, 1, 0, 1 ) );
        this.subLabel.moveTo(0, 0, subZOffset);
        this.subBlend = new Rectangle(renderModes, 1f, 1f, 0f);
        this.subBlend.setColor( new Vec4f( 0, 0, 0, 0.2f ) );
    }

    /**
     * Sets subtitle parameter
     * @param subFont subtitle font
     * @param subLineHeightPct one subtitle line height percentage of this shape, default is 1/10 (0.1f)
     * @param subLineDY y-axis offset to bottom in line-height, defaults to 1/4 (0.25f)
     */
    public void setSubtitleParams(final Font subFont, final float subLineHeightPct, final float subLineDY) {
        this.subLabel.setFont(subFont);
        this.subLineHeightPct = subLineHeightPct;
        this.subLineDY = subLineDY;
        this.subEnabled = true;
    }
    /**
     * Sets subtitle colors
     * @param color color for the text, defaults to RGBA {@code 1, 1, 0, 1}
     * @param blend blending alpha (darkness), defaults to 0.2f
     */
    public void setSubtitleColor(final Vec4f color, final float blend) {
        this.subLabel.setColor( color );
        this.subBlend.setColor( 0, 0, 0, blend );
    }

    public final ASSEventListener getASSEventListener() { return assEventListener; }
    private final ASSEventListener assEventListener = new ASSEventListener() {
        @Override
        public void run(final ASSEventLine e) {
            synchronized( assEventLock ) {
                assEventQueue.add(e);
                if( DEBUG_SUB ) {
                    System.err.println("MediaButton: GOT #"+assEventQueue.size()+": "+e);
                }
            }
        }
    };

    public MediaButton setVerbose(final boolean v) { verbose = v; return this; }

    /**
     * Add the default {@link GLMediaEventListener} to {@link #getGLMediaPlayer() this class's GLMediaPlayer}.
     */
    public MediaButton addDefaultEventListener() {
        getGLMediaPlayer().addEventListener(defGLMediaEventListener);
        return this;
    }

    public final GLMediaPlayer getGLMediaPlayer() { return (GLMediaPlayer)texSeq; }

    public final AudioSink getAudioSink() { return getGLMediaPlayer().getAudioSink(); }

    private final GLMediaEventListener defGLMediaEventListener = new GLMediaEventListener() {
            @Override
            public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) {
                // texButton.markStateDirty();
            }

            @Override
            public void attributesChanged(final GLMediaPlayer mp, final GLMediaPlayer.EventMask eventMask, final long when) {
                if( verbose ) {
                    System.err.println("MediaButton AttributesChanges: "+eventMask+", when "+when);
                    System.err.println("MediaButton State: "+mp);
                }
                if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Uninit) ) {
                    clearSubtitleCache();
                } else if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Init) ) {
                    resetGL = true;
                    clearSubtitleCache();
                }
                if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Size) ) {
                    // FIXME: mPlayer.resetGLState();
                }
                if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.EOS) ) {
                    new InterruptSource.Thread() {
                        @Override
                        public void run() {
                            // loop for-ever ..
                            mp.seek(0);
                            mp.resume();
                        } }.start();
                } else if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Error) ) {
                    final StreamException se = mp.getStreamException();
                    if( null != se ) {
                        se.printStackTrace();
                    }
                }
            } };


    @Override
    protected void clearImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        ((GLMediaPlayer)texSeq).stop();
        ((GLMediaPlayer)texSeq).seek(0);
    }
    @Override
    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        ((GLMediaPlayer)texSeq).destroy(gl);
    }

    volatile boolean resetGL = true;

    @Override
    protected void addShapeToRegion(final GLProfile glp, final GL2ES2 gl) {
        super.addShapeToRegion(glp, gl);
    }

    @Override
    protected final void drawImpl0(final GL2ES2 gl, final RegionRenderer renderer, final Vec4f rgba) {
        final GLMediaPlayer mPlayer = (GLMediaPlayer)texSeq;
        if( resetGL ) {
            resetGL = false;
            try {
                mPlayer.initGL(gl);
                if( null != region ) {
                    region.markShapeDirty(); // reset texture data
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        super.drawImpl0(gl, renderer, rgba);
        if( subEnabled ) {
            drawSubtitle(gl, renderer);
        }
        markStateDirty(); // keep on going
    };
    private final void clearSubtitleCache() {
        draw_lastASS = null;
        synchronized( assEventLock ) {
            assEventQueue.clear();
        }
    }
    private static final int SUB_MIN_DURATION = 5; // min duration 1s, broken ASS have <= 3ms
    private final void drawSubtitle(final GL2ES2 gl, final RegionRenderer renderer) {
        final GLMediaPlayer mPlayer = (GLMediaPlayer)texSeq;
        final int pts = mPlayer.getPTS().get(Clock.currentMillis());

        // Validate draw_lastASS timeout
        ASSEventLine lastASS = draw_lastASS;
        {
            if( null != lastASS && lastASS.pts_end < pts && lastASS.getDuration() > SUB_MIN_DURATION ) {
                if( DEBUG_SUB ) {
                    System.err.println("MediaButton: Drop.0: pts "+pts+", "+lastASS);
                }
                draw_lastASS = null;
                lastASS = null;
            }
        }
        // dequeue and earmark new subtitle in time
        final ASSEventLine ass;
        final boolean newASS;
        {
            final ASSEventLine gotASS;
            synchronized( assEventLock ) {
                if( assEventQueue.size() > 0 ) {
                    final ASSEventLine e = assEventQueue.get(0);
                    if( e.getDuration() <= SUB_MIN_DURATION || ( e.pts_start <= pts && pts <= e.pts_end ) ) {
                        gotASS = e;
                        assEventQueue.remove(0);
                    } else if( e.pts_end < pts ) {
                        gotASS = null;
                        assEventQueue.remove(0);
                        if( DEBUG_SUB ) {
                            System.err.println("MediaButton: Drop.1: pts "+pts+", "+e);
                        }
                    } else {
                        gotASS = null;
                    }
                } else {
                    gotASS = null;
                }
            }
            if( null == gotASS || gotASS == lastASS ) {
                ass = lastASS;
                newASS = false;
            } else {
                draw_lastASS = gotASS;
                ass = gotASS;
                newASS = true;
            }
        }
        // drop or draw (update label for new subtitle)
        final boolean drawASS;
        if( null == ass ) {
            draw_lastASS = null;
            drawASS = false;
        } else {
            drawASS = true;
            if( newASS ) {
                subLabel.setText(ass.text);
                final AABBox subBox = subLabel.getBounds(gl.getGLProfile());
                final float subLineHeight = subBox.getHeight() / ass.lines;
                final float maxWidth = box.getWidth() * 0.95f;
                float scale = ( box.getHeight() * subLineHeightPct ) / subLineHeight;
                if( scale * subBox.getWidth() > maxWidth ) {
                    scale = maxWidth / subBox.getWidth();
                }
                subLabel.setScale(scale, scale, 1);
                final float dx_s = ( box.getWidth() - maxWidth ) * 0.5f;
                final float dy_s = subLineHeight * subLineDY * scale;
                subLabel.moveTo(dx_s, dy_s, 2*subZOffset);
                subBlend.setDimension(box.getWidth(), box.getHeight() * subLineHeightPct * ass.lines, 0f);
                subBlend.setPosition(0, dy_s, 1*subZOffset);
                if( DEBUG_SUB ) {
                    System.err.println("MediaButton: NEXT pts "+pts+", "+ass);
                }
            }
        }
        if( drawASS ) {
            subBlend.draw(gl, renderer);
            final PMVMatrix4f pmv = renderer.getMatrix();
            pmv.pushMv();
            subLabel.applyMatToMv(pmv);
            subLabel.draw(gl, renderer);
            pmv.popMv();
        }

    }
    private volatile ASSEventLine draw_lastASS;

}
