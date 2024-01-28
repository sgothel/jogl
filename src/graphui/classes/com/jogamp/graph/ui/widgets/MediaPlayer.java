/**
 * Copyright 2010-2024 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.ui.widgets;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import com.jogamp.common.av.PTS;
import com.jogamp.common.net.Uri;
import com.jogamp.common.os.Clock;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.TooltipText;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.BoxLayout;
import com.jogamp.graph.ui.layout.Gap;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.MediaButton;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.graph.ui.widgets.RangeSlider.SliderAdapter;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventAdapter;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.EventMask;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.texture.TextureSequence;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

/**
 * Media player {@link Widget}, embedding a {@link MediaButton} and its controls.
 * @see #MediaPlayer(int, Scene, GLMediaPlayer, Uri, int, float, boolean, float, List)
 */
public class MediaPlayer extends Widget {
    private static final boolean DEBUG = false;
    public static final Vec2f FixedSymSize = new Vec2f(0.0f, 1.0f);
    public static final Vec2f SymSpacing = new Vec2f(0f, 0.2f);
    public static final float CtrlButtonWidth = 1f;
    public static final float CtrlButtonHeight = 1f;
    public static final Vec4f CtrlCellCol = new Vec4f(0, 0, 0, 0);
    private static final float BorderSz = 0.01f;
    private static final float BorderSzS = 0.03f;
    private static final Vec4f BorderColor = new Vec4f(0, 0, 0, 0.5f);
    private static final Vec4f BorderColorA = new Vec4f(0, 0, 0.5f, 0.5f);
    private static final float AlphaBlend = 0.3f;
    private static final float KnobScale = 3f;

    private final MediaButton mButton;

    /**
     * Constructs a {@link MediaPlayer}, i.e. its shapes and controls.
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param scene the used {@link Scene} to query parameter and access rendering loop
     * @param mPlayer fresh {@link GLMediaPlayer} instance, maybe customized via e.g. {@link GLMediaPlayer#setTextureMinMagFilter(int[])}.
     * @param medium {@link Uri} stream source, either a file or network source
     * @param aratio aspect ratio of the resulting {@link Shape}, usually 16.0f/9.0f or 4.0f/3.0f, which also denotes the width of this shape while using height 1.0.
     * @param letterBox toggles {@link Region#COLORTEXTURE_LETTERBOX_RENDERING_BIT} on or off
     * @param zoomSize zoom-size (0..1] for zoom-out control
     * @param customCtrls optional custom controls, maybe an empty list
     */
    public MediaPlayer(final int renderModes, final Scene scene, final GLMediaPlayer mPlayer,
                       final Uri medium, final float aratio, final boolean letterBox, final float zoomSize,
                       final List<Shape> customCtrls)
    {
        super( new BoxLayout(aratio, 1, Alignment.None) );

        final Font fontInfo = Scene.getDefaultFont(), fontSymbols = Scene.getSymbolsFont();
        if( null == fontInfo || null == fontSymbols ) {
            mButton = null;
            return;
        }
        final float zEpsilon = scene.getZEpsilon(16);
        final float ctrlZOffset = zEpsilon * 20f;

        final int ctrlCellsInt = 10+2;
        final int ctrlCells = Math.max(customCtrls.size() + ctrlCellsInt, 13);

        final float ctrlCellWidth = (aratio-2*BorderSzS)/ctrlCells;
        final float ctrlCellHeight = ctrlCellWidth;
        final float ctrlSliderHeightMin = ctrlCellHeight/15f;       // bar-height
        final float ctrlSliderHeightMax = KnobScale * ctrlSliderHeightMin; // knob-height

        final AtomicReference<Shape> zoomReplacement = new AtomicReference<Shape>();
        final AtomicReference<Vec3f> zoomOrigScale = new AtomicReference<Vec3f>();
        final AtomicReference<Vec3f> zoomOrigPos = new AtomicReference<Vec3f>();

        this.setName("mp.container");
        this.setBorderColor(BorderColor).setBorder(BorderSz);
        this.setInteractive(true).setFixedARatioResize(true);

        mButton = new MediaButton(renderModes, aratio, 1, mPlayer, fontInfo, 0.1f);
        mButton.setName("mp.mButton").setInteractive(false);
        mButton.setPerp().setPressedColorMod(1f, 1f, 1f, 0.85f);

        final RangeSlider ctrlSlider;
        {
            final float knobScale = ctrlSliderHeightMax / ctrlSliderHeightMin;
            ctrlSlider = new RangeSlider(renderModes, new Vec2f(aratio - ctrlSliderHeightMax, ctrlSliderHeightMin), knobScale, new Vec2f(0, 100), 1000, 0);
            final float dx = ctrlSliderHeightMax / 2f;
            final float dy = ( ctrlSliderHeightMax - ctrlSliderHeightMin ) * 0.5f;
            ctrlSlider.setPaddding(new Padding(0, dx, ctrlCellHeight-dy, dx));
        }
        ctrlSlider.setName("mp.slider");

        final Button playButton = new Button(renderModes, fontSymbols,
                fontSymbols.getUTF16String("play_arrow"),  fontSymbols.getUTF16String("pause"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
        playButton.setName("mp.play");
        playButton.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);

        final Button audioLabel = new Button(renderModes, fontInfo, "audio\nund", CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
        audioLabel.setName("mp.audio_lang").setToggleable(false);
        audioLabel.setPerp().setColor(CtrlCellCol);
        final Button subLabel = new Button(renderModes, fontInfo, "sub\nund", CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
        subLabel.setName("mp.sub_lang").setToggleable(false);
        subLabel.setPerp().setColor(CtrlCellCol);

        {
            mButton.setVerbose(false).addDefaultEventListener().setTextureLetterbox(letterBox);
            mPlayer.setAudioVolume( 0f );
            mPlayer.addEventListener( new GLMediaEventListener() {
                @Override
                public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) { }

                @Override
                public void attributesChanged(final GLMediaPlayer mp, final EventMask eventMask, final long when) {
                    if( DEBUG ) {
                        System.err.println("MediaButton AttributesChanges: "+eventMask+", when "+when);
                        System.err.println("MediaButton State: "+mp);
                    }
                    if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Init) ) {
                        audioLabel.setText("audio\n"+mp.getLang(mp.getAID()));
                        subLabel.setText("sub\n"+mp.getLang(mp.getSID()));
                        ctrlSlider.setMinMax(new Vec2f(0, mp.getDuration()), 0);
                        if( DEBUG ) {
                            System.err.println(mp.toString());
                        }
                        for(final GLMediaPlayer.Chapter c : mp.getChapters()) {
                            if( DEBUG ) {
                                System.err.println(c);
                            }
                            final Shape mark = ctrlSlider.addMark(c.start, new Vec4f(0.9f, 0.9f, 0.9f, 0.5f));
                            mark.setToolTip(new TooltipText(c.title+"\n"+PTS.millisToTimeStr(c.start, false), fontInfo, 10));
                        }
                    } else if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Play) ) {
                        playButton.setToggle(true);
                    } else if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Pause) ) {
                        playButton.setToggle(false);
                    }
                    if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.EOS) ) {
                        final StreamException err = mp.getStreamException();
                        if( null != err ) {
                            System.err.println("MovieSimple State: Exception: "+err.getMessage());
                        } else {
                            new InterruptSource.Thread() {
                                @Override
                                public void run() {
                                    mp.setPlaySpeed(1f);
                                    mp.seek(0);
                                    mp.resume();
                                }
                            }.start();
                        }
                    }
                }
            });
            this.addShape(mButton);
        }

        Group ctrlGroup, infoGroup;
        Shape ctrlBlend;
        final Label muteLabel, infoLabel;
        final Button timeLabel;
        final float infoGroupHeight;
        final boolean[] hud_sticky = { false };
        {
            muteLabel = new Label(renderModes, fontSymbols, aratio/6f, fontSymbols.getUTF16String("music_off")); // volume_mute, headset_off
            muteLabel.setName("mp.mute");
            {
                final float sz = aratio/6f;
                muteLabel.setColor(1, 0, 0, 1);
                muteLabel.setPaddding(new Padding(0, 0, 1f-sz, aratio-sz));

                muteLabel.setInteractive(false);
                muteLabel.setVisible( mPlayer.isAudioMuted() );
                this.addShape(muteLabel);
            }

            infoGroup = new Group(new BoxLayout());
            infoGroup.setName("mp.info").setInteractive(false);
            this.addShape( infoGroup.setVisible(false) );
            {
                final String text = "88:88 / 88:88 (88 %), playing (8.88x, vol 8.88), A/R 8.88\n"+
                                    "JogAmp's GraphUI Full-Feature Media Player Widget Rocks!";
                final AABBox textBounds = fontInfo.getGlyphBounds(text);
                final float lineHeight = textBounds.getHeight()/2f; // fontInfo.getLineHeight();
                final float sxy = aratio/(2f*textBounds.getWidth()); // add 100%
                infoLabel = new Label(renderModes, fontInfo, text);
                infoLabel.setName("mp.info.label");
                infoLabel.setInteractive(false);
                infoLabel.setColor(1, 1, 1, 0.9f);
                infoLabel.scale(sxy, sxy, 1f);

                final float dy = 0.5f*lineHeight*sxy;
                infoGroupHeight = 2.5f*dy + textBounds.getHeight()*sxy;
                if( DEBUG ) {
                    System.err.println("XXX: sxy "+sxy+", b "+textBounds);
                    System.err.println("XXX: GroupHeight "+infoGroupHeight+", dy "+dy+", lineHeight*sxy "+(lineHeight*sxy));
                    System.err.println("XXX: b.getHeight() * sxy "+(textBounds.getHeight() * sxy));
                    System.err.println("XXX: (1f-GroupHeight+dy)/sxy "+((1f-infoGroupHeight+dy)/sxy));
                }
                infoLabel.setPaddding(new Padding(0, 0, (1f-infoGroupHeight+dy)/sxy, 0.5f));

                final Rectangle rect = new Rectangle(renderModes & ~Region.AA_RENDERING_MASK, aratio, infoGroupHeight, 0);
                rect.setName("mp.info.blend").setInteractive(false);
                rect.setColor(0, 0, 0, AlphaBlend);
                rect.setPaddding(new Padding(0, 0, 1f-infoGroupHeight, 0));

                infoGroup.addShape(rect);
                infoGroup.addShape(infoLabel);
            }
            {
                timeLabel = new Button(renderModes, fontInfo,
                        getMultilineTime(Clock.currentMillis(), mPlayer), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                timeLabel.setName("mp.time");
                timeLabel.setPerp().setColor(CtrlCellCol);
                timeLabel.setLabelColor(1, 1, 1, 1.0f);
            }
            scene.addGLEventListener(new GLEventAdapter() {
                long t0 = 0;
                @Override
                public void display(final GLAutoDrawable drawable) {
                    final GLAnimatorControl anim = drawable.getAnimator();
                    if( ( timeLabel.isVisible() || infoLabel.isVisible() ) &&
                        mPlayer.getState() == GLMediaPlayer.State.Playing && null != anim )
                    {
                        final long t1 = anim.getTotalFPSDuration();
                        if( t1 - t0 >= 333) {
                            t0 = t1;
                            final int ptsMS = mPlayer.getPTS().get(Clock.currentMillis());
                            final int durationMS = mPlayer.getDuration();
                            infoLabel.setText(getInfo(ptsMS, durationMS, mPlayer, false));
                            timeLabel.setText(getMultilineTime(ptsMS, durationMS));
                            ctrlSlider.setValue(ptsMS);
                        }
                    }
                }
            } );
            ctrlSlider.addSliderListener(new SliderAdapter() {
                private void seekPlayer(final int ptsMS) {
                    scene.invoke(false,  (final GLAutoDrawable d) -> {
                        final int durationMS = mPlayer.getDuration();
                        timeLabel.setText(getMultilineTime(ptsMS, durationMS));
                        mPlayer.seek(ptsMS);
                        return true;
                    } );
                }
                @Override
                public void dragged(final RangeSlider w, final float old_val, final float val, final float old_val_pct, final float val_pct) {
                    if( DEBUG ) {
                        System.err.println("Dragged "+w.getName()+": "+PTS.millisToTimeStr(Math.round(val), true)+"ms, "+(val_pct*100f)+"%");
                        System.err.println("Slider.D "+ctrlSlider.getDescription());
                    }
                    seekPlayer( Math.round( val ) );
                }
            });
            this.addShape( ctrlSlider.setVisible(false) );

            ctrlBlend = new Rectangle(renderModes & ~Region.AA_RENDERING_MASK, aratio, ctrlCellHeight, 0);
            ctrlBlend.setName("ctrl.blend").setInteractive(false);
            ctrlBlend.setColor(0, 0, 0, AlphaBlend);
            this.addShape( ctrlBlend.setVisible(false) );

            final float toolTipScaleY = 0.4f;
            ctrlGroup = new Group(new GridLayout(ctrlCellWidth, ctrlCellHeight, Alignment.FillCenter, Gap.None, 1));
            ctrlGroup.setName("ctrlGroup").setInteractive(false);
            ctrlGroup.setPaddding(new Padding(0, BorderSzS, 0, BorderSzS));
            this.addShape( ctrlGroup.move(0, 0, ctrlZOffset).setVisible(false) );
            { // 1
                playButton.onToggle((final Shape s) -> {
                    if( s.isToggleOn() ) {
                        mPlayer.setPlaySpeed(1);
                        mPlayer.resume();
                    } else {
                        mPlayer.pause(false);
                    }
                });
                playButton.setToggle(true); // on == play
                ctrlGroup.addShape(playButton);
                playButton.setToolTip(new TooltipText("Play/Pause", fontInfo, toolTipScaleY));
            }
            { // 2
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("skip_previous"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("back");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s) -> {
                    mPlayer.seek(0);
                });
                ctrlGroup.addShape(button);
                button.setToolTip(new TooltipText("Back", fontInfo, toolTipScaleY));
            }
            { // 3
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("fast_rewind"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("rv-");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s) -> {
                    final float v = mPlayer.getPlaySpeed();
                    if( v <= 1.0f ) {
                        mPlayer.setPlaySpeed(v / 2.0f);
                    } else {
                        mPlayer.setPlaySpeed(v - 0.5f);
                    }
                });
                ctrlGroup.addShape(button);
                button.setToolTip(new TooltipText("replay speed: v <= 1 ? v/2 : v-0.5", fontInfo, toolTipScaleY));
            }
            { // 4
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("fast_forward"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("rv+");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s) -> {
                    final float v = mPlayer.getPlaySpeed();
                    if( 1f > v && v + 0.5f > 1f ) {
                        mPlayer.setPlaySpeed(1); // reset while crossing over 1
                    } else {
                        mPlayer.setPlaySpeed(v + 0.5f);
                    }
                });
                ctrlGroup.addShape(button);
                button.setToolTip(new TooltipText("replay speed: v+0.5", fontInfo, toolTipScaleY));
            }
            { // 5
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("replay_5"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("rew5");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s) -> {
                    mPlayer.seek(mPlayer.getPTS().get(Clock.currentMillis()) - 5000);
                });
                button.addMouseListener(new Shape.MouseGestureAdapter() {
                    @Override
                    public void mouseWheelMoved(final MouseEvent e) {
                        final int pts0 = mPlayer.getPTS().get(Clock.currentMillis());
                        final int pts1 = pts0 + (int)(e.getRotation()[1]*1000f);
                        if( DEBUG ) {
                            System.err.println("Seek: "+pts0+" -> "+pts1);
                        }
                        mPlayer.seek(pts1);
                    } } );
                ctrlGroup.addShape(button);
                button.setToolTip(new TooltipText("Replay-5", fontInfo, toolTipScaleY));
            }
            { // 6
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("forward_5"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("fwd5");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s) -> {
                    mPlayer.seek(mPlayer.getPTS().get(Clock.currentMillis()) + 5000);
                });
                button.addMouseListener(new Shape.MouseGestureAdapter() {
                    @Override
                    public void mouseWheelMoved(final MouseEvent e) {
                        final int pts0 = mPlayer.getPTS().get(Clock.currentMillis());
                        final int pts1 = pts0 + (int)(e.getRotation()[1]*1000f);
                        if( DEBUG ) {
                            System.err.println("Seek: "+pts0+" -> "+pts1);
                        }
                        mPlayer.seek(pts1);
                    } } );
                ctrlGroup.addShape(button);
                button.setToolTip(new TooltipText("Forward-5", fontInfo, toolTipScaleY));
            }
            { // 7
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("volume_up"), fontSymbols.getUTF16String("volume_mute"),  CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("mute");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                final float[] volume = { 1.0f };
                button.onToggle( (final Shape s) -> {
                    if( s.isToggleOn() ) {
                        mPlayer.setAudioVolume( volume[0] );
                    } else {
                        mPlayer.setAudioVolume( 0f );
                    }
                    muteLabel.setVisible( !s.isToggleOn() );
                });
                button.addMouseListener(new Shape.MouseGestureAdapter() {
                    @Override
                    public void mouseWheelMoved(final MouseEvent e) {
                        mPlayer.setAudioVolume( mPlayer.getAudioVolume() + e.getRotation()[1]/20f );
                        volume[0] = mPlayer.getAudioVolume();
                    } } );
                button.setToggle( !mPlayer.isAudioMuted() ); // on == volume
                ctrlGroup.addShape(button);
                button.setToolTip(new TooltipText("Volume", fontInfo, toolTipScaleY));
            }
            { // 8
                audioLabel.onClicked((final Shape s) -> {
                    final int next_aid = mPlayer.getNextAID();
                    if( mPlayer.getAID() != next_aid ) {
                        mPlayer.switchStream(mPlayer.getVID(), next_aid, mPlayer.getSID());
                    }
                });
                ctrlGroup.addShape(audioLabel);
                audioLabel.setToolTip(new TooltipText("Audio Language", fontInfo, toolTipScaleY));
            }
            { // 9
                subLabel.onClicked((final Shape s) -> {
                    final int next_sid = mPlayer.getNextSID();
                    if( mPlayer.getSID() != next_sid ) {
                        mPlayer.switchStream(mPlayer.getVID(), mPlayer.getAID(), next_sid);
                    }
                });
                ctrlGroup.addShape(subLabel);
                subLabel.setToolTip(new TooltipText("Subtitle Language", fontInfo, toolTipScaleY));
            }
            { // 10
                ctrlGroup.addShape(timeLabel);
            }
            for(int i=10; i<ctrlCells-2-customCtrls.size(); ++i) {
                final Button button = new Button(renderModes, fontInfo, " ", CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("ctrl_"+i);
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                ctrlGroup.addShape(button);
            }
            { // -1
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("visibility"), fontSymbols.getUTF16String("visibility_off"),  CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("hud");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onToggle( (final Shape s) -> {
                    hud_sticky[0] = s.isToggleOn();
                });
                button.setToggle( false );
                ctrlGroup.addShape(button);
                button.setToolTip(new TooltipText("HUD", fontInfo, toolTipScaleY));
            }
            { // -2
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("zoom_out_map"), fontSymbols.getUTF16String("zoom_in_map"),  CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("zoom");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                final boolean toggleBorder = FloatUtil.isEqual(1f, zoomSize);
                button.onToggle( (final Shape s) -> {
                    if( s.isToggleOn() ) {
                        if( toggleBorder ) {
                            MediaPlayer.this.setBorder(0f);
                            System.err.println("ZOOM: border off");
                        }
                        final AABBox sbox = scene.getBounds();
                        final Group parent = this.getParent();
                        final float sx = sbox.getWidth() * zoomSize / this.getScaledWidth();
                        final float sy = sbox.getHeight() * zoomSize / this.getScaledHeight();
                        final float sxy = Math.min(sx, sy);
                        Shape _zoomReplacement = null;
                        zoomOrigScale.set( this.getScale().copy() );
                        zoomOrigPos.set( this.getPosition().copy() );
                        if( null != parent ) {
                            // System.err.println("Zoom1: rep, sxy "+sx+" x "+sy+" = "+sxy);
                            _zoomReplacement = new Label(renderModes, fontInfo, aratio/40f, "zoomed");
                            final boolean r = parent.replaceShape(this, _zoomReplacement);
                            if( r ) {
                                // System.err.println("Zoom1: p "+parent);
                                // System.err.println("Zoom1: t "+this);
                                this.scale(sxy, sxy, 1f);
                                this.moveTo(sbox.getLow()).move(sbox.getWidth() * ( 1f - zoomSize )/2.0f, sbox.getHeight() * ( 1f - zoomSize )/2.0f, ctrlZOffset);
                                scene.addShape(this);
                            } else {
                                // System.err.println("Zoom1: failed "+this);
                            }
                        } else {
                            // System.err.println("Zoom2: top, sxy "+sx+" x "+sy+" = "+sxy);
                            // System.err.println("Zoom2: t "+this);
                            this.scale(sxy, sxy, 1f);
                            this.moveTo(sbox.getLow()).move(sbox.getWidth() * ( 1f - zoomSize )/2.0f, sbox.getHeight() * ( 1f - zoomSize )/2.0f, ctrlZOffset);
                        }
                        // System.err.println("Zoom: R "+_zoomReplacement);
                        zoomReplacement.set( _zoomReplacement );
                    } else {
                        if( toggleBorder ) {
                            MediaPlayer.this.setBorder(BorderSz);
                        }
                        final Vec3f _zoomOrigScale = zoomOrigScale.getAndSet(null);
                        final Vec3f _zoomOrigPos = zoomOrigPos.getAndSet(null);
                        final Shape _zoomReplacement = zoomReplacement.getAndSet(null);
                        if( null != _zoomReplacement ) {
                            final Group parent = _zoomReplacement.getParent();
                            scene.removeShape(this);
                            if( null != _zoomOrigScale ) {
                                this.setScale(_zoomOrigScale);
                            }
                            if( null != _zoomOrigPos ) {
                                this.moveTo(_zoomOrigPos);
                            }
                            parent.replaceShape(_zoomReplacement, this);
                            // System.err.println("Reset1: "+parent);
                        } else {
                            if( null != _zoomOrigScale ) {
                                this.setScale(_zoomOrigScale);
                            }
                            if( null != _zoomOrigPos ) {
                                this.moveTo(_zoomOrigPos);
                            }
                            // System.err.println("Reset2: top");
                        }
                        if( null != _zoomReplacement ) {
                            scene.invoke(true, (drawable) -> {
                                final GL2ES2 gl = drawable.getGL().getGL2ES2();
                                _zoomReplacement .destroy(gl, scene.getRenderer());
                                return true;
                            });
                        }
                    }
                });
                button.setToggle( false ); // on == zoom
                ctrlGroup.addShape(button);
                button.setToolTip(new TooltipText("Zoom", fontInfo, toolTipScaleY));
            }
            for(final Shape cs : customCtrls ) {
                ctrlGroup.addShape(cs);
            }
        }
        this.setWidgetMode(true);

        this.addActivationListener( (final Shape s) -> {
            if( this.isActive() ) {
                this.setBorderColor(BorderColorA);
            } else {
                final boolean hud_on = hud_sticky[0];
                this.setBorderColor(BorderColor);
                ctrlSlider.setVisible(hud_on);
                ctrlBlend.setVisible(hud_on);
                ctrlGroup.setVisible(hud_on);
                infoGroup.setVisible(hud_on);
            }
        });
        this.addMouseListener(new Shape.MouseGestureAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                final Vec3f p = shapeEvent.objPos;
                final boolean c = hud_sticky[0] || ( ctrlCellHeight + ctrlSliderHeightMax ) > p.y() || p.y() > ( 1f - infoGroupHeight );
                ctrlSlider.setVisible(c);
                ctrlBlend.setVisible(c);
                ctrlGroup.setVisible(c);
                infoGroup.setVisible(c);
            }
            @Override
            public void mouseReleased(final MouseEvent e) {
                mButton.setPressedColorMod(1f, 1f, 1f, 1f);
            }
            @Override
            public void mouseDragged(final MouseEvent e) {
                mButton.setPressedColorMod(1f, 1f, 1f, 0.85f);
            }
        } );
        this.forAll((final Shape s) -> { s.setDraggable(false).setResizable(false); return false; });
        ctrlSlider.getKnob().setDraggable(true);
    }

    /**
     * Sets subtitle parameter
     * @param subFont subtitle font
     * @param subLineHeightPct one subtitle line height percentage of this shape, default is 0.1f
     */
    public void setSubtitleParams(final Font subFont, final float subLineHeightPct) {
        if( null != mButton ) {
            mButton.setSubtitleParams(subFont, subLineHeightPct);
        }
    }

    public static String getInfo(final long currentMillis, final GLMediaPlayer mPlayer, final boolean full) {
        return getInfo(mPlayer.getPTS().get(currentMillis), mPlayer.getDuration(), mPlayer, full);
    }
    public static String getInfo(final int ptsMS, final int durationMS, final GLMediaPlayer mPlayer, final boolean full) {
        final String chapter;
        {
            final GLMediaPlayer.Chapter c = mPlayer.getChapter(ptsMS);
            if( null != c ) {
                chapter = " - "+c.title;
            } else {
                chapter = "";
            }
        }
        final float aspect = (float)mPlayer.getWidth() / (float)mPlayer.getHeight();
        final float pct = (float)ptsMS / (float)durationMS;
        if( full ) {
            final String text1 = String.format("%s / %s (%.0f %%), %s (%01.2fx, vol %1.2f), A/R %0.2f, fps %02.1f",
                    PTS.millisToTimeStr(ptsMS, false), PTS.millisToTimeStr(durationMS, false), pct*100,
                    mPlayer.getState().toString().toLowerCase(), mPlayer.getPlaySpeed(), mPlayer.getAudioVolume(), aspect, mPlayer.getFramerate());
            final String text2 = String.format("audio: id %d (%s), kbps %d, codec %s; sid %d (%s)",
                    mPlayer.getAID(), mPlayer.getLang(mPlayer.getAID()), mPlayer.getAudioBitrate()/1000, mPlayer.getAudioCodec(),
                    mPlayer.getSID(), mPlayer.getLang(mPlayer.getSID()) );
            final String text3 = String.format("video: id %d, kbps %d, codec %s",
                    mPlayer.getVID(), mPlayer.getVideoBitrate()/1000, mPlayer.getVideoCodec());
            return text1+"\n"+text2+"\n"+text3+"\n"+mPlayer.getTitle()+chapter;
        } else {
            final String vinfo, ainfo, sinfo;
            if( mPlayer.getVID() != GLMediaPlayer.STREAM_ID_NONE ) {
                vinfo = String.format((Locale)null, ", vid %d", mPlayer.getVID());
            } else {
                vinfo = "";
            }
            if( mPlayer.getAID() != GLMediaPlayer.STREAM_ID_NONE ) {
                ainfo = String.format((Locale)null, ", aid %d (%s)", mPlayer.getAID(), mPlayer.getLang(mPlayer.getAID()));
            } else {
                ainfo = "";
            }
            if( mPlayer.getSID() != GLMediaPlayer.STREAM_ID_NONE ) {
                sinfo = String.format((Locale)null, ", sid %d (%s)", mPlayer.getSID(), mPlayer.getLang(mPlayer.getSID()));
            } else {
                sinfo = "";
            }
            final String text1 = String.format("%s / %s (%.0f %%), %s (%01.2fx, vol %1.2f), A/R %.2f%s%s%s",
                    PTS.millisToTimeStr(ptsMS, false), PTS.millisToTimeStr(durationMS, false), pct*100,
                    mPlayer.getState().toString().toLowerCase(), mPlayer.getPlaySpeed(), mPlayer.getAudioVolume(), aspect, vinfo, ainfo, sinfo);
            return text1+"\n"+mPlayer.getTitle()+chapter;
        }
    }
    public static String getMultilineTime(final long currentMillis, final GLMediaPlayer mPlayer) {
        return getMultilineTime(mPlayer.getPTS().get(currentMillis), mPlayer.getDuration());
    }
    public static String getMultilineTime(final int ptsMS, final int durationMS) {
        final float pct = (float)ptsMS / (float)durationMS;
        return String.format("%.0f %%%n%s%n%s",
                    pct*100, PTS.millisToTimeStr(ptsMS, false), PTS.millisToTimeStr(durationMS, false));
    }
}
