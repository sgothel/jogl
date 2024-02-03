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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.av.AudioSink;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.util.av.SubTextEvent;
import com.jogamp.opengl.util.av.SubBitmapEvent;
import com.jogamp.opengl.util.av.SubtitleEvent;
import com.jogamp.opengl.util.av.SubtitleEventListener;
import com.jogamp.opengl.util.texture.ImageSequence;
import com.jogamp.opengl.util.av.CodecID;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;

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
    private static final boolean DEBUG_SUB = false;
    private static final boolean DEBUG_PGS_POSSZ = false;
    private boolean verbose = false;

    private Font subFont;
    private Font subFallbackFont;
    private final Label subLabel;
    private final float subZOffset;
    private boolean subEnabled;
    private float subLineHeightPct;
    private float subLineDY;
    private Alignment subAlignment;
    private final Rectangle subBlend;
    private final ImageButton subTexImg;

    /** Default text/ASS subtitle line height percentage, {@value}. */
    public static final float DEFAULT_ASS_SUB_HEIGHT = 0.075f;
    /** Default text/ASS subtitle y-axis offset to bottom in line-height percentage, {@value}. */
    public static final float DEFAULT_ASS_SUB_POS = 0.25f;
    /** Default color for the text/ASS subtitles, defaults to RGBA {@code 1, 1, 1, 1}. */
    public static final Vec4f DEFAULT_ASS_SUB_COLOR = new Vec4f( 1, 1, 1, 1 );
    /** Default blending alpha (darkness) for the text/ASS subtitles, defaults to {@value}. */
    public static final float DEFAULT_ASS_SUB_BLEND = 0.3f;
    /** Default text/ASS subtitle alignment, defaults to {@link Alignment#CenterHoriz}. */
    public static final Alignment DEFAULT_ASS_SUB_ALIGNMENT = Alignment.CenterHoriz;

    private static final float ASS_SUB_USED_WIDTH = 0.90f;
    private static final float ASS_SUB_BLEND_ADDED_HEIGHT = 0.25f;

    private SubtitleEvent drawLastSub;

    private final List<SubtitleEvent> subEventQueue = new ArrayList<SubtitleEvent>();
    private final Object subEventLock = new Object();

    /** Constructs a {@link MediaButton} with {@link FontFactory#getDefaultFont()} for subtitles. */
    public MediaButton(final int renderModes, final float width, final float height, final GLMediaPlayer mPlayer) {
        this(renderModes, width, height, mPlayer, null);
    }

    /**
     * Constructs a {@link MediaButton} prepared for using subtitles
     * @param renderModes
     * @param width
     * @param height
     * @param mPlayer
     * @param subFont text/ASS subtitle font, pass {@code null} for {@link FontFactory#getDefaultFont()}.
     *                {@link FontFactory#getFallbackFont()} is used {@link Font#getBestCoverage(Font, Font, CharSequence) if providing a better coverage} of a Text/ASS subtitle line.
     * @see #setSubtitleParams(Font, float, float, Alignment)
     * @see #setSubtitleColor(Vec4f, float)
     */
    public MediaButton(final int renderModes, final float width, final float height, final GLMediaPlayer mPlayer, final Font subFont)
    {
        super(renderModes & ~Region.AA_RENDERING_MASK, width, height, mPlayer);

        setColor(1.0f, 1.0f, 1.0f, 0.0f);
        setPressedColorMod(0.9f, 0.9f, 0.9f, 0.7f);
        setToggleOffColorMod(0.8f, 0.8f, 0.8f, 1.0f);
        setToggleOnColorMod(1.0f, 1.0f, 1.0f, 1.0f);

        mPlayer.setSubtitleEventListener(subEventListener);

        setSubtitleParams(subFont, DEFAULT_ASS_SUB_HEIGHT, DEFAULT_ASS_SUB_POS, DEFAULT_ASS_SUB_ALIGNMENT);
        this.subLabel = new Label(renderModes, this.subFont, "");
        this.subZOffset = Button.DEFAULT_LABEL_ZOFFSET;
        this.subLabel.moveTo(0, 0, subZOffset);
        this.subBlend = new Rectangle(renderModes, 1f, 1f, 0f);
        setSubtitleColor(DEFAULT_ASS_SUB_COLOR, DEFAULT_ASS_SUB_BLEND);

        this.subTexImg = new ImageButton(renderModes, width, height, new ImageSequence(mPlayer.getTextureUnit(), true /* useBuildInTexLookup */));
        this.subTexImg.setPerp().setToggleable(false).setDragAndResizeable(false).setInteractive(false);
        // this.subTexImg.setBorder(0.001f).setBorderColor(1, 1, 0, 1);
        this.subTexImg.getImageSequence().setParams(GL.GL_LINEAR, GL.GL_LINEAR, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
        this.subTexImg.setARatioAdjustment(false);
        this.drawLastSub = null;
    }

    /** Toggle enabling subtitle rendering */
    public void setSubtitlesEnabled(final boolean v) {
        subEnabled = v;
    }

    /**
     * Sets text/ASS subtitle parameter, enabling subtitle rendering
     * @param subFont text/ASS subtitle font, pass {@code null} for {@link FontFactory#getDefaultFont()}
     *                {@link FontFactory#getFallbackFont()} is used {@link Font#getBestCoverage(Font, Font, CharSequence) if providing a better coverage} of a Text/ASS subtitle line.
     * @param subLineHeightPct text/ASS subtitle line height percentage, defaults to {@link #DEFAULT_ASS_SUB_HEIGHT}
     * @param subLineDY text/ASS y-axis offset to bottom in line-height, defaults to {@link #DEFAULT_ASS_SUB_POS}
     * @param subAlignment text/ASS subtitle alignment, defaults to {@link #DEFAULT_ASS_SUB_ALIGNMENT}
     */
    public void setSubtitleParams(final Font subFont, final float subLineHeightPct, final float subLineDY, final Alignment subAlignment) {
        if( null != subFont ) {
            this.subFont = subFont;
        } else {
            this.subFont = FontFactory.getDefaultFont();
        }
        this.subFallbackFont = FontFactory.getFallbackFont();
        this.subLineHeightPct = subLineHeightPct;
        this.subLineDY = subLineDY;
        this.subAlignment = subAlignment;
        this.subEnabled = true;
    }
    /**
     * Sets text/ASS subtitle colors
     * @param color color for the text/ASS, defaults to {@link #DEFAULT_ASS_SUB_COLOR}
     * @param blend blending alpha (darkness), defaults to {@link #DEFAULT_ASS_SUB_BLEND}
     */
    public void setSubtitleColor(final Vec4f color, final float blend) {
        this.subLabel.setColor( color );
        this.subBlend.setColor( 0, 0, 0, blend );
    }

    public final SubtitleEventListener getSubEventListener() { return subEventListener; }
    private final SubtitleEventListener subEventListener = new SubtitleEventListener() {
        @Override
        public void run(final SubtitleEvent e) {
            synchronized( subEventLock ) {
                subEventQueue.add(e);
                if( DEBUG_SUB ) {
                    System.err.println("MediaButton: GOT #"+subEventQueue.size()+": "+e);
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
            public void attributesChanged(final GLMediaPlayer mp, final GLMediaPlayer.EventMask eventMask, final long when) {
                if( verbose ) {
                    System.err.println("MediaButton AttributesChanges: "+eventMask+", when "+when);
                    System.err.println("MediaButton State: "+mp);
                }
                if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Init) ) {
                    clearSubtitleCache();
                    resetGL = true;
                } else if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Uninit) ||
                           eventMask.isSet(GLMediaPlayer.EventMask.Bit.Play) ||
                           eventMask.isSet(GLMediaPlayer.EventMask.Bit.Seek) ||
                           eventMask.isSet(GLMediaPlayer.EventMask.Bit.SID) ) {
                    clearSubtitleCache();
                    markStateDirty();
                } else if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Pause) ) {
                    clearSubtitleCacheButLast();
                    markStateDirty();
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
        clearSubtitleCache();
    }
    @Override
    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        ((GLMediaPlayer)texSeq).stop();
        clearSubtitleCache();
        subTexImg.destroy(gl, renderer);
        subLabel.destroy(gl, renderer);
        subBlend.destroy(gl, renderer);
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
                clearSubtitleCache();
                mPlayer.initGL(gl);
                if( null != region ) {
                    region.markShapeDirty(); // reset texture data
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        super.drawImpl0(gl, renderer, rgba);
        if( subEnabled && GLMediaPlayer.STREAM_ID_NONE != mPlayer.getSID() ) {
            drawSubtitle(gl, renderer);
        }
        if( GLMediaPlayer.State.Playing == mPlayer.getState() ) {
            markStateDirty(); // keep on going
        }
    };
    private final void clearSubtitleCache() {
        synchronized( subEventLock ) {
            final SubtitleEvent lastSub = drawLastSub;
            drawLastSub = null;
            if( null != lastSub ) {
                lastSub.release();
            }
            subTexImg.getImageSequence().removeAllFrames();
            for(final SubtitleEvent e : subEventQueue) {
                e.release();
            }
            subEventQueue.clear();
            if( DEBUG_SUB ) {
                System.err.println("MediaButton.clearSubtitleCache: "+subEventQueue.size()+", last "+lastSub);
            }
        }
    }
    private final void clearSubtitleCacheButLast() {
        synchronized( subEventLock ) {
            final SubtitleEvent lastSub = drawLastSub;
            for(int i=subEventQueue.size()-1; i>=0; --i) {
                final SubtitleEvent e = subEventQueue.get(i);
                if( lastSub != e ) {
                    e.release();
                    subEventQueue.remove(i);
                }
            }
            if( DEBUG_SUB ) {
                System.err.println("MediaButton.clearSubtitleCacheButLast: "+subEventQueue.size()+", last "+lastSub);
            }
        }
    }
    private final void drawSubtitle(final GL2ES2 gl, final RegionRenderer renderer) {
        final GLMediaPlayer mPlayer = (GLMediaPlayer)texSeq;
        final int pts = mPlayer.getPTS().getCurrent();

        // Validate draw_lastSub timeout
        SubtitleEvent lastSub = drawLastSub;
        {
            if( null != lastSub && lastSub.pts_end < pts ) {
                if( DEBUG_SUB ) {
                    System.err.println("MediaButton: Drop.0: pts "+pts+", "+lastSub);
                }
                drawLastSub = null;
                lastSub.release();
                lastSub = null;
            }
        }
        // Dequeue and earmark new subtitle in time.
        // A new subtitle (empty as well) may simply replace an older one,
        // allowing PGS subtitles to work (infinite end-time)
        final SubtitleEvent sub;
        final boolean newSub;
        {
            final SubtitleEvent gotSub;
            synchronized( subEventLock ) {
                if( subEventQueue.size() > 0 ) {
                    final SubtitleEvent e = subEventQueue.get(0);
                    if( e.pts_start <= pts && pts <= e.pts_end ) {
                        gotSub = e;
                        subEventQueue.remove(0);
                    } else if( e.pts_end < pts ) {
                        gotSub = null;
                        subEventQueue.remove(0);
                        e.release();
                        if( DEBUG_SUB ) {
                            System.err.println("MediaButton: Drop.1: pts "+pts+", "+e);
                        }
                    } else {
                        // subtitle for the future, keep it
                        gotSub = null;
                    }
                } else {
                    gotSub = null;
                }
            }
            if( null == gotSub ) {
                sub = lastSub;
                newSub = false;
            } else {
                if( null != lastSub ) {
                    lastSub.release();
                }
                lastSub = null;
                if( SubtitleEvent.Type.Empty == gotSub.type ) {
                    gotSub.release();
                    sub = null;
                    newSub = false;
                    if( DEBUG_SUB ) {
                        System.err.println("MediaButton: Empty: pts "+pts+", "+gotSub);
                    }
                } else {
                    drawLastSub = gotSub;
                    sub = gotSub;
                    newSub = true;
                }
            }
        }
        // drop or draw (update label for new subtitle)
        if( null == sub ) {
            drawLastSub = null;
        } else if( SubtitleEvent.Type.Text == sub.type ) {
            final SubTextEvent assSub = (SubTextEvent)sub;
            if( newSub ) {
                subLabel.setFont( Font.getBestCoverage(subFont, subFallbackFont, assSub.text) );
                subLabel.setText(assSub.text);
                final AABBox subBox = subLabel.getBounds(gl.getGLProfile());
                final float subLineHeight = subBox.getHeight() / assSub.lines;
                final float maxWidth = box.getWidth() * ASS_SUB_USED_WIDTH;
                final float lineHeight = box.getHeight() * subLineHeightPct;
                float s_s = lineHeight / subLineHeight;
                if( s_s * subBox.getWidth() > maxWidth ) {
                    s_s = maxWidth / subBox.getWidth();
                }
                subLabel.setScale(s_s, s_s, 1);

                final float labelHeight = lineHeight * assSub.lines;
                final float blendHeight = labelHeight + lineHeight * ASS_SUB_BLEND_ADDED_HEIGHT;
                final Vec2f v_sz = new Vec2f(mPlayer.getWidth(), mPlayer.getHeight());
                final Vec2f v_sxy = new Vec2f( box.getWidth(), box.getHeight() ).div( v_sz );
                final float v_s = Math.min( v_sxy.x(), v_sxy.y() );
                final Vec2f v_ctr = new Vec2f(v_sz).scale(0.5f); // original video size center
                final Vec2f b_ctr = new Vec2f(box.getCenter()).scale(1/v_s);
                final float d_bl = ( blendHeight - labelHeight ) * 0.5f;
                final float v_maxWidth = v_sz.x() * ASS_SUB_USED_WIDTH;
                final float d_vmw = v_sz.x() - v_maxWidth;
                final Vec2f s_p0_s;
                if( subAlignment.isSet( Alignment.Bit.Left) ) {
                    // Alignment.Bit.Left
                    final Vec2f s_p0 = new Vec2f( d_vmw*0.5f,
                                                  ( subLineHeight * subLineDY * s_s ) / v_s);
                    s_p0_s = s_p0.sub( v_ctr ).add(b_ctr).scale( v_s ).add(0, d_bl);
                } else {
                    // Alignment.Bit.CenterHoriz
                    final Vec2f s_p0 = new Vec2f( d_vmw*0.5f + ( v_maxWidth - subBox.getWidth()*s_s/v_s )*0.5f,
                                                  ( subLineHeight * subLineDY * s_s ) / v_s);
                    s_p0_s = s_p0.sub( v_ctr ).add(b_ctr).scale( v_s ).add(0, d_bl);
                }
                subLabel.moveTo(s_p0_s.x(), s_p0_s.y(), 2*subZOffset);

                subBlend.setDimension(box.getWidth(), blendHeight, 0f);
                subBlend.setPosition(0, s_p0_s.y() - d_bl, 1*subZOffset);
                if( DEBUG_SUB ) {
                    System.err.println("MediaButton: NEXT pts "+pts+", "+sub);
                }
            }
            subBlend.draw(gl, renderer);
            final PMVMatrix4f pmv = renderer.getMatrix();
            pmv.pushMv();
            subLabel.applyMatToMv(pmv);
            subLabel.draw(gl, renderer);
            pmv.popMv();
        } else if( SubtitleEvent.Type.Bitmap == sub.type ) {
            final SubBitmapEvent texSub = (SubBitmapEvent)sub;
            if( newSub ) {
                if( DEBUG_SUB ) {
                    System.err.println("MediaButton: NEXT pts "+pts+", "+sub);
                }
                if( null != texSub.texture ) {
                    final ImageSequence imgSeq = subTexImg.getImageSequence();
                    imgSeq.removeAllFrames();
                    imgSeq.addFrame(gl, texSub.texture);
                    final Vec2f v_sz = new Vec2f(mPlayer.getWidth(), mPlayer.getHeight());
                    final Vec2f v_sxy = new Vec2f( box.getWidth(), box.getHeight() ).div( v_sz );
                    final float v_s = Math.min(v_sxy.x(), v_sxy.y());
                    final Vec2f s_sz_s = new Vec2f(texSub.dimension).scale(v_s);
                    subTexImg.setSize(s_sz_s.x(), s_sz_s.y());

                    final Vec2f v_ctr;
                    if( CodecID.HDMV_PGS == sub.codec && mPlayer.getWidth() < 1920 && mPlayer.getHeight() == 1080 ) {
                        // PGS subtitles < 1920 width, e.g. 4:3 1440 width but 1080p
                        // usually are positioned to 1920 width screen. FIXME: Elaborate, find metrics
                        v_ctr = new Vec2f(new Vec2f(1920, 1080)).scale(0.5f); // 1080p center
                    } else {
                        v_ctr = new Vec2f(v_sz).scale(0.5f); // original video size center
                    }
                    final Vec2f b_ctr = new Vec2f(box.getCenter()).scale(1/v_s);
                    final Vec2f s_p0 = new Vec2f(texSub.position.x(),
                                                 v_sz.y() - texSub.position.y() - texSub.dimension.y() ); // y-flip + texSub.position is top-left
                    final Vec2f s_p0_s = s_p0.minus( v_ctr ).add( b_ctr ).scale( v_s );
                    subTexImg.moveTo(s_p0_s.x(), s_p0_s.y(), 2*subZOffset);

                    if( DEBUG_PGS_POSSZ ) {
                        // Keep this to ease later adjustments due to specifications like PGS
                        final Vec2f b_sz = new Vec2f(box.getWidth(), box.getHeight());
                        final float v_ar = v_sz.x()/v_sz.y();
                        final float b_ar = b_sz.x()/b_sz.y();
                        final float s_ar = s_sz_s.x()/s_sz_s.y();
                        final float s_x_centered = ( b_sz.x() - s_sz_s.x() ) * 0.5f;
                        final Vec2f v_ctr_1080p = new Vec2f(new Vec2f(1920, 1080)).scale(0.5f); // 1080p center
                        final Vec2f v_ctr_o = new Vec2f(v_sz).scale(0.5f); // original video size center
                        final Vec2f s_sz = new Vec2f(texSub.dimension);
                        final Vec2f b_ctr_s = new Vec2f(box.getCenter());
                        final Vec2f v_p0_ctr = s_p0.minus(v_ctr); // p0 -> v_sz center
                        final Vec2f s_p1 = b_ctr.plus(v_p0_ctr);
                        System.err.println("XX   video "+v_sz+" (ar "+v_ar+"), ( v_ctr "+v_ctr_o+", v_ctr_1080p "+v_ctr_1080p+" ) -> v_ctr "+v_ctr);
                        System.err.println("XX   sub s_sz "+s_sz+", s_sz_s "+s_sz_s+" (ar "+s_ar+")");
                        System.err.println("XX   box "+b_sz+" (ar "+b_ar+"), b_ctr "+b_ctr+", b_ctr_s "+b_ctr_s);
                        System.err.println("XXX v_s "+v_sxy+" -> "+v_s+": sz "+s_sz_s);
                        System.err.println("XXX p0 "+s_p0+", v_p0_ctr "+v_p0_ctr+", s_p1 "+s_p1+" -> s_p1_s "+s_p0_s+"; sxs_2 "+s_x_centered);
                    }
                }
            }
            final PMVMatrix4f pmv = renderer.getMatrix();
            pmv.pushMv();
            subTexImg.applyMatToMv(pmv);
            subTexImg.draw(gl, renderer);
            pmv.popMv();
        }
    }
}
