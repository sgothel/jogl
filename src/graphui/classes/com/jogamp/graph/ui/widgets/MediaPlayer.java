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

import java.util.Arrays;
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
import com.jogamp.graph.ui.shapes.HUDShape;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.MediaButton;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventAdapter;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;
import com.jogamp.opengl.util.av.GLMediaPlayer.EventMask;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.texture.TextureSequence;

import jogamp.graph.ui.TreeTool;

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
    private static final float KnobScale = 2f;
    private static final float StillPlayerScale = 1/3f;
    private static final float ChapterTipScaleY = 5f;
    private static final float ToolTipScaleY = 0.6f;

    private final MediaButton mButton;

    /**
     * Constructs a {@link MediaPlayer}, i.e. its shapes and controls.
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param scene the used {@link Scene} to query parameter and access rendering loop
     * @param mPlayer fresh {@link GLMediaPlayer} instance owned by this {@link MediaPlayer}, may be customized via e.g. {@link GLMediaPlayer#setTextureMinMagFilter(int[])}.
     * @param medium {@link Uri} stream source, either a file or network source
     * @param aratio aspect ratio of the resulting {@link Shape}, usually 16.0f/9.0f or 4.0f/3.0f, which also denotes the width of this shape while using height 1.0.
     * @param letterBox toggles {@link GLMediaPlayer#setARatioLetterbox(boolean, Vec4f)} on or off
     * @param zoomSize zoom-size (0..1] for zoom-out control
     * @param enableStills pass {@code true} to enable still images on the time slider on mouse-over, involves a 2nd internal {@link GLMediaPlayer} instance
     * @param customCtrls optional custom controls, maybe an empty list
     */
    public MediaPlayer(final int renderModes, final Scene scene, final GLMediaPlayer mPlayer,
                       final Uri medium, final float aratio, final boolean letterBox, final float zoomSize,
                       final boolean enableStills, final List<Shape> customCtrls)
    {
        super( new BoxLayout(aratio, 1, Alignment.None) );

        final Font fontInfo = FontFactory.getDefaultFont(), fontSymbols = FontFactory.getSymbolsFont();
        if( null == fontInfo || null == fontSymbols ) {
            mButton = null;
            return;
        }
        final float zEpsilon = scene.getZEpsilon(16);
        final float superZOffset = zEpsilon * 20f;

        final int ctrlCellsInt = 11+3;
        final int ctrlCells = Math.max(customCtrls.size() + ctrlCellsInt, 20);

        final float ctrlCellWidth = (aratio-2*BorderSzS)/ctrlCells;
        final float ctrlCellHeight = ctrlCellWidth;
        final float ctrlSliderHeightMin = ctrlCellHeight/6f;       // bar-height
        final float ctrlSliderHeightMax = KnobScale * ctrlSliderHeightMin; // knob-height

        final AtomicReference<Shape> zoomReplacement = new AtomicReference<Shape>();
        final AtomicReference<Vec3f> zoomOrigScale = new AtomicReference<Vec3f>();
        final AtomicReference<Vec3f> zoomOrigPos = new AtomicReference<Vec3f>();

        this.setName("mp.container");
        this.setBorderColor(BorderColor).setBorder(BorderSz);
        this.setInteractive(true).setFixedARatioResize(true);

        mButton = new MediaButton(renderModes, aratio, 1, mPlayer);
        mButton.setName("mp.mButton").setInteractive(false);
        mButton.setPerp().setPressedColorMod(1f, 1f, 1f, 0.85f);
        mButton.setVerbose(false).addDefaultEventListener().setARatioLetterbox(letterBox, new Vec4f(1, 1, 1, 1));
        mPlayer.setAudioVolume( 0f );

        final RangeSlider ctrlSlider;
        {
            final float knobScale = ctrlSliderHeightMax / ctrlSliderHeightMin;
            ctrlSlider = new RangeSlider(renderModes, new Vec2f(aratio - ctrlSliderHeightMax, ctrlSliderHeightMin), knobScale, new Vec2f(0, 100), 1000, 0);
            final float dx = ctrlSliderHeightMax / 2f;
            final float dy = ( ctrlSliderHeightMax - ctrlSliderHeightMin ) * 0.5f;
            ctrlSlider.setPaddding(new Padding(0, dx, ctrlCellHeight-dy, dx));
            ctrlSlider.getBar().setColor(0.3f, 0.3f, 0.3f, 0.7f);
            ctrlSlider.getKnob().setColor(0.6f, 0.6f, 1f, 1f);
            ctrlSlider.setActiveKnobColorMod(new Vec4f(0.1f, 0.1f, 1, 1));
            ctrlSlider.move(0, 0, zEpsilon);
        }
        ctrlSlider.setName("mp.slider");

        final GLMediaPlayer stillPlayer;
        final Button stillTime;
        final HUDShape stillHUD;
        final Runnable reshapeStillHUD;
        {
            final Group stillGroup = new Group();
            final float labelW = aratio/4f;
            final float labelH = 1f/10f;
            stillTime = new Button(renderModes, fontInfo, PTS.toTimeStr(0), labelW, labelH, 0);
            stillTime.setName("mp.stillTime").setInteractive(false);
            stillTime.setLabelColor(0.2f, 0.2f, 0.2f, 1f);
            stillTime.setColor(1f, 1f, 1f, 1f);
            stillTime.setSpacing(0.1f, 0.3f);
            stillTime.setCorner(0.75f);
            stillTime.moveTo(aratio/2f-labelW/2f, 0, 0); // center to stillMedia
            stillGroup.addShape(stillTime);

            final MediaButton stillMedia;
            if( enableStills ) {
                stillPlayer = GLMediaPlayerFactory.createDefault();
                // stillPlayer.setTextureMinMagFilter( new int[] { GL.GL_NEAREST, GL.GL_NEAREST } );
                stillPlayer.setTextureMinMagFilter( new int[] { GL.GL_LINEAR, GL.GL_LINEAR } );
                stillPlayer.setTextureUnit(2);
                stillPlayer.addEventListener((final GLMediaPlayer mp, final EventMask eventMask, final long when) -> {
                    if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Play) ) {
                        mp.pause(true);
                    }
                });
                stillMedia = new MediaButton(renderModes, aratio, 1.0f, stillPlayer);
                stillMedia.setName("mp.stillMedia").setInteractive(false);
                stillMedia.setPerp().setPressedColorMod(1f, 1f, 1f, 0.85f);
                stillMedia.setVerbose(false).addDefaultEventListener().setARatioLetterbox(true, mButton.getARatioLetterboxBackColor());
                stillMedia.moveTo(0, labelH*1.2f, 0); // above stillTime
                stillGroup.addShape(stillMedia);
            } else {
                stillPlayer = null;
                stillMedia = null;
            }
            stillHUD = new HUDShape(scene,
                                    enableStills ? aratio*StillPlayerScale : labelW*StillPlayerScale,
                                    enableStills ? 1f*StillPlayerScale : labelH*StillPlayerScale,
                                    renderModes, ctrlSlider, stillGroup);
            stillHUD.setVisible(false);
            scene.addShape(stillHUD);
            reshapeStillHUD = () -> {
                final float ar = (float)mPlayer.getWidth()/(float)mPlayer.getHeight();
                final float labelW2 = ar/4f;
                final float labelH2 = 1f/10f;
                stillMedia.setSize(ar, 1f);
                stillMedia.moveTo(0, labelH2*1.2f, 0); // above stillTime
                stillTime.moveTo(ar/2f-labelW2/2f, 0, 0); // center to stillMedia
                stillHUD.setClientSize(ar*StillPlayerScale, 1f*StillPlayerScale);
            };
        }

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
        final Button cropButton = new Button(renderModes, fontSymbols,
                fontSymbols.getUTF16String("crop"), fontSymbols.getUTF16String("crop_free"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
        cropButton.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol).setName("ar crop");

        mPlayer.addEventListener((final GLMediaPlayer mp, final EventMask eventMask, final long when) -> {
            if( DEBUG ) {
                System.err.println("MediaButton AttributesChanges: "+eventMask+", when "+when);
                System.err.println("MediaButton State: "+mp);
            }
            if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Init) ) {
                audioLabel.setText("audio\n"+mp.getLang(mp.getAID()));
                subLabel.setText("sub\n"+mp.getLang(mp.getSID()));
                ctrlSlider.setMinMax(new Vec2f(0, mp.getDuration()), 0);
                System.err.println("Init "+mp.toString());

                for(final GLMediaPlayer.Chapter c : mp.getChapters()) {
                    if( DEBUG ) {
                        System.err.println(c);
                    }
                    final Shape mark = ctrlSlider.addMark(c.start, new Vec4f(0.9f, 0.9f, 0.9f, 0.5f));
                    mark.setTooltip(new TooltipText(c.title+"\n@ "+PTS.toTimeStr(c.start, false)+", duration "+PTS.toTimeStr(c.duration(), false), fontInfo, ChapterTipScaleY));
                }
                final float aratioVideo = (float)mPlayer.getWidth() / (float)mPlayer.getHeight();
                if( FloatUtil.isZero(Math.abs(aratio - aratioVideo), 0.1f) ) {
                    cropButton.setVisible(false);
                    System.err.println("AR Crop disabled: aratioPlayer "+aratio+", aratioVideo "+aratioVideo+": "+mPlayer.getTitle());
                } else {
                    System.err.println("AR Crop  enabled: aratioPlayer "+aratio+", aratioVideo "+aratioVideo+": "+mPlayer.getTitle());
                }
                if( enableStills ) {
                    scene.invoke(false,  (final GLAutoDrawable d) -> {
                        stillPlayer.stop();
                        stillPlayer.playStream(mPlayer.getUri(), mPlayer.getVID(), GLMediaPlayer.STREAM_ID_NONE, GLMediaPlayer.STREAM_ID_NONE, 1);
                        reshapeStillHUD.run();
                        return true;
                    });
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
        } );
        this.addShape(mButton);

        Group ctrlGroup, infoGroup;
        Shape ctrlBlend;
        final Label muteLabel, infoLabel;
        final Button timeLabel;
        final float infoGroupHeight;
        final boolean[] hud_sticky = { false };
        final boolean[] info_full = { false };
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
                final String text = "88:88 / 88:88:88 (88 %), playing (8.88x, vol 8.88), A/R 8.88, vid 8 (H264), aid 8 (eng, AC3), sid 8 (eng, HDMV_PGS)\n"+
                                    "JogAmp's GraphUI Full-Feature Media Player Widget Rocks!";
                final AABBox textBounds = fontInfo.getGlyphBounds(text);
                final float lineHeight = textBounds.getHeight()/2f; // fontInfo.getLineHeight();
                final float sxy = aratio/(1.1f*textBounds.getWidth()); // add 10%
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
                        ( mPlayer.getState() == GLMediaPlayer.State.Playing ||
                          mPlayer.getState() == GLMediaPlayer.State.Paused ) &&
                        null != anim )
                    {
                        final long t1 = anim.getTotalFPSDuration();
                        if( t1 - t0 >= 333) {
                            t0 = t1;
                            final int ptsMS = mPlayer.getPTS().getCurrent();
                            final int durationMS = mPlayer.getDuration();
                            infoLabel.setText(getInfo(ptsMS, durationMS, mPlayer, info_full[0]));
                            timeLabel.setText(getMultilineTime(ptsMS, durationMS));
                            ctrlSlider.setValue(ptsMS);
                        }
                    }
                }
            } );
            ctrlSlider.addChangeListener((final RangeSlider w, final float old_val, final float val, final float old_val_pct, final float val_pct, final Vec3f pos, final MouseEvent e) -> {
                if( DEBUG ) {
                    System.err.println("Dragged "+w.getName()+": "+PTS.toTimeStr(Math.round(val), true)+"ms, "+(val_pct*100f)+"%");
                    System.err.println("Slider.D "+ctrlSlider.getDescription());
                }
                final int dir_val = (int)Math.signum(val - old_val);
                final int currentPTS = mPlayer.getPTS().getCurrent();
                final int nextPTS = Math.round( val );
                final int dir_pts = (int)Math.signum(nextPTS - currentPTS);
                if( dir_val == dir_pts ) {
                    scene.invoke(false,  (final GLAutoDrawable d) -> {
                        final int durationMS = mPlayer.getDuration();
                        timeLabel.setText(getMultilineTime(nextPTS, durationMS));
                        mPlayer.seek(nextPTS);
                        return true;
                    } );
                }
            });

            final int[] lastPeekPTS = { 0 };
            ctrlSlider.addPeekListener((final RangeSlider w, final float val, final float val_pct, final Vec3f pos, final MouseEvent e) -> {
                final float res = Math.max(1000, w.getRange() / 300f); // ~300dpi alike less jittery
                final int nextPTS = Math.round( val/1000f ) * 1000;
                final Vec3f pos2 = new Vec3f(pos.x()-stillHUD.getClientSize().x()/2f, ctrlSliderHeightMax, pos.z() + ctrlSlider.getPosition().z());
                stillHUD.moveToHUDPos(pos2);
                // stillMedia.setARatioLetterbox(mButton.useARatioAdjustment(), mButton.getARatioLetterboxBackColor());
                stillTime.setText(PTS.toTimeStr(nextPTS, false));
                stillHUD.setVisible(true);
                if( enableStills && Math.abs(lastPeekPTS[0] - nextPTS ) >= res ) {
                    scene.invoke(false,  (final GLAutoDrawable d) -> {
                        stillPlayer.seek(nextPTS);
                        return true;
                    } );
                    lastPeekPTS[0] = nextPTS;
                }
            });
            ctrlSlider.addActivationListener((final Shape s) -> {
                if( s.isActive() ) {
                    // stillMedia.setARatioLetterbox(mButton.useARatioAdjustment(), mButton.getARatioLetterboxBackColor());
                    stillTime.setText(PTS.toTimeStr(mPlayer.getPTS().getCurrent(), false));
                    stillHUD.setVisible(true);
                } else {
                    stillHUD.setVisible(false);
                }
            });

            ctrlBlend = new Rectangle(renderModes & ~Region.AA_RENDERING_MASK, aratio, ctrlCellHeight, 0);
            ctrlBlend.setName("ctrl.blend").setInteractive(false);
            ctrlBlend.setColor(0, 0, 0, AlphaBlend);
            this.addShape( ctrlBlend.setVisible(false) );

            this.addShape( ctrlSlider.setVisible(false) );

            ctrlGroup = new Group(new GridLayout(ctrlCellWidth, ctrlCellHeight, Alignment.FillCenter, Gap.None, 1));
            ctrlGroup.setName("ctrlGroup").setInteractive(false);
            ctrlGroup.setPaddding(new Padding(0, BorderSzS, 0, BorderSzS));
            this.addShape( ctrlGroup.setVisible(false) );
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
                playButton.setTooltip(new TooltipText("Play/Pause", fontInfo, ToolTipScaleY));
            }
            { // 2
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("skip_previous"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("back");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s, final Vec3f pos, final MouseEvent e) -> {
                    final int pts = mPlayer.getPTS().getCurrent();
                    int targetMS = 0;
                    final GLMediaPlayer.Chapter c0 = mPlayer.getChapter(mPlayer.getPTS().getCurrent());
                    if( null != c0 ) {
                        if( pts > c0.start + 5000 ) {
                            targetMS = c0.start;
                        } else {
                            final GLMediaPlayer.Chapter c1 = mPlayer.getChapter(c0.start - 1);
                            if( null != c1 ) {
                                targetMS = c1.start;
                            } else {
                                targetMS = 0;
                            }
                        }
                    }
                    mPlayer.seek(targetMS);
                });
                ctrlGroup.addShape(button);
                button.setTooltip(new TooltipText("Prev Chapter", fontInfo, ToolTipScaleY));
            }
            { // 3
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("skip_next"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("next");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s, final Vec3f pos, final MouseEvent e) -> {
                    int targetMS = 0;
                    final GLMediaPlayer.Chapter c0 = mPlayer.getChapter(mPlayer.getPTS().getCurrent());
                    if( null != c0 ) {
                        final GLMediaPlayer.Chapter c1 = mPlayer.getChapter(c0.end + 1);
                        if( null != c1 ) {
                            targetMS = c1.start;
                        } else {
                            targetMS = c0.end;
                        }
                    } else if( mPlayer.getChapters().length > 0 ) {
                        targetMS = 0;
                    } else {
                        targetMS = mPlayer.getPTS().getCurrent() * ( 1 + 1 / ( 60000 * 10 ) );
                    }
                    mPlayer.seek(targetMS);
                });
                ctrlGroup.addShape(button);
                button.setTooltip(new TooltipText("Next Chapter", fontInfo, ToolTipScaleY));
            }
            { // 4
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("fast_rewind"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("rv-");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s, final Vec3f pos, final MouseEvent e) -> {
                    final float v = mPlayer.getPlaySpeed();
                    if( v <= 1.0f ) {
                        mPlayer.setPlaySpeed(v / 2.0f);
                    } else {
                        mPlayer.setPlaySpeed(v - 0.5f);
                    }
                });
                ctrlGroup.addShape(button);
                button.setTooltip(new TooltipText("replay speed: v <= 1 ? v/2 : v-0.5", fontInfo, ToolTipScaleY));
            }
            { // 5
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("fast_forward"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("rv+");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s, final Vec3f pos, final MouseEvent e) -> {
                    final float v = mPlayer.getPlaySpeed();
                    if( 1f > v && v + 0.5f > 1f ) {
                        mPlayer.setPlaySpeed(1); // reset while crossing over 1
                    } else {
                        mPlayer.setPlaySpeed(v + 0.5f);
                    }
                });
                ctrlGroup.addShape(button);
                button.setTooltip(new TooltipText("replay speed: v+0.5", fontInfo, ToolTipScaleY));
            }
            { // 6
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("replay_10"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("rew5");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s, final Vec3f pos, final MouseEvent e) -> {
                    mPlayer.seek(mPlayer.getPTS().getCurrent() - 10000);
                });
                button.addMouseListener(new Shape.MouseGestureAdapter() {
                    @Override
                    public void mouseWheelMoved(final MouseEvent e) {
                        final int pts0 = mPlayer.getPTS().getCurrent();
                        final int pts1 = pts0 + (int)(e.getRotation()[1]*10000f);
                        if( DEBUG ) {
                            System.err.println("Seek: "+pts0+" -> "+pts1);
                        }
                        mPlayer.seek(pts1);
                    } } );
                ctrlGroup.addShape(button);
                button.setTooltip(new TooltipText("Replay 10s (+scroll)", fontInfo, ToolTipScaleY));
            }
            { // 7
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("forward_10"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("fwd5");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s, final Vec3f pos, final MouseEvent e) -> {
                    mPlayer.seek(mPlayer.getPTS().getCurrent() + 10000);
                });
                button.addMouseListener(new Shape.MouseGestureAdapter() {
                    @Override
                    public void mouseWheelMoved(final MouseEvent e) {
                        final int pts0 = mPlayer.getPTS().getCurrent();
                        final int pts1 = pts0 + (int)(e.getRotation()[1]*10000f);
                        if( DEBUG ) {
                            System.err.println("Seek: "+pts0+" -> "+pts1);
                        }
                        mPlayer.seek(pts1);
                    } } );
                ctrlGroup.addShape(button);
                button.setTooltip(new TooltipText("Forward 10s (+scroll)", fontInfo, ToolTipScaleY));
            }
            { // 8
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
                button.setTooltip(new TooltipText("Volume (+scroll)", fontInfo, ToolTipScaleY));
            }
            { // 9
                audioLabel.onClicked((final Shape s, final Vec3f pos, final MouseEvent e) -> {
                    final int next_aid = mPlayer.getNextAID();
                    if( mPlayer.getAID() != next_aid ) {
                        mPlayer.switchStream(mPlayer.getVID(), next_aid, mPlayer.getSID());
                    }
                });
                ctrlGroup.addShape(audioLabel);
                audioLabel.setTooltip(new TooltipText("Audio Language", fontInfo, ToolTipScaleY));
            }
            { // 10
                subLabel.onClicked((final Shape s, final Vec3f pos, final MouseEvent e) -> {
                    final int next_sid = mPlayer.getNextSID();
                    if( mPlayer.getSID() != next_sid ) {
                        mPlayer.switchStream(mPlayer.getVID(), mPlayer.getAID(), next_sid);
                    }
                });
                ctrlGroup.addShape(subLabel);
                subLabel.setTooltip(new TooltipText("Subtitle Language", fontInfo, ToolTipScaleY));
            }
            { // 11
                ctrlGroup.addShape(timeLabel);
            }
            for(int i=11; i<ctrlCells-3-customCtrls.size(); ++i) {
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
                button.setTooltip(new TooltipText("Sticky HUD", fontInfo, ToolTipScaleY));
            }
            { // -2
                final boolean[] value = { !letterBox };
                cropButton.onToggle( (final Shape s) -> {
                    value[0] = !value[0];
                    mButton.setARatioLetterbox(!value[0], mButton.getARatioLetterboxBackColor());
                });
                ctrlGroup.addShape(cropButton);
                cropButton.setTooltip(new TooltipText("Letterbox Crop", fontInfo, ToolTipScaleY));
            }
            { // -3
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("zoom_out_map"), fontSymbols.getUTF16String("zoom_in_map"),  CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("zoom");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                final boolean fullScene = FloatUtil.isEqual(1f, zoomSize);
                final boolean wasDraggable = isDraggable();
                button.onToggle( (final Shape s) -> {
                    if( s.isToggleOn() ) {
                        // Zoom in
                        if( fullScene ) {
                            MediaPlayer.this.setBorder(0f);
                            MediaPlayer.this.setDraggable(false);
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
                                this.moveTo(sbox.getLow()).move(sbox.getWidth() * ( 1f - zoomSize )/2.0f, sbox.getHeight() * ( 1f - zoomSize )/2.0f, superZOffset);
                                scene.addShape(this);
                            } else {
                                // System.err.println("Zoom1: failed "+this);
                            }
                        } else {
                            // System.err.println("Zoom2: top, sxy "+sx+" x "+sy+" = "+sxy);
                            // System.err.println("Zoom2: t "+this);
                            this.scale(sxy, sxy, 1f);
                            this.moveTo(sbox.getLow()).move(sbox.getWidth() * ( 1f - zoomSize )/2.0f, sbox.getHeight() * ( 1f - zoomSize )/2.0f, superZOffset);
                        }
                        // System.err.println("Zoom: R "+_zoomReplacement);
                        zoomReplacement.set( _zoomReplacement );
                    } else {
                        // Zoom out
                        if( fullScene ) {
                            MediaPlayer.this.setBorder(BorderSz);
                            MediaPlayer.this.setDraggable(wasDraggable);
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
                button.setTooltip(new TooltipText("Zoom", fontInfo, ToolTipScaleY));
            }
            for(final Shape cs : customCtrls ) {
                ctrlGroup.addShape(cs);
            }
        }
        this.enableTopLevelWidget(scene);

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
            public void mouseClicked(final MouseEvent e) {
                final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                final Vec3f p = shapeEvent.objPos;
                if( p.y() > ( 1f - infoGroupHeight ) ) {
                    info_full[0] = !info_full[0];
                    final float sxy = infoLabel.getScale().x();
                    final float p_bottom_s = infoLabel.getPadding().bottom * sxy;
                    final float sxy2;
                    if( info_full[0] ) {
                        sxy2 = sxy * 0.5f;
                    } else {
                        sxy2 = sxy * 2f;
                    }
                    infoLabel.setScale(sxy2,  sxy2,  1f);
                    infoLabel.setPaddding(new Padding(0, 0, p_bottom_s/sxy2, 0.5f));
                }
            }
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
        TreeTool.forAll(this, (final Shape s) -> { s.setDraggable(false).setResizable(false); return false; });
        ctrlSlider.getKnob().setDraggable(true);
    }

    /** Toggle enabling subtitle rendering */
    public void setSubtitlesEnabled(final boolean v) {
        if( null != mButton ) {
            mButton.setSubtitlesEnabled(v);
        }
    }

    /**
     * Sets text/ASS subtitle parameter, enabling subtitle rendering
     * @param subFont text/ASS subtitle font, pass {@code null} for {@link FontFactory#getDefaultFont()}
     *                {@link FontFactory#getFallbackFont()} is used {@link Font#getBestCoverage(Font, Font, CharSequence) if providing a better coverage} of a Text/ASS subtitle line.
     * @param subLineHeightPct text/ASS subtitle line height percentage, defaults to {@link MediaButton#DEFAULT_ASS_SUB_HEIGHT}
     * @param subLineDY text/ASS y-axis offset to bottom in line-height, defaults to {@link MediaButton#DEFAULT_ASS_SUB_POS}
     * @param subAlignment text/ASS subtitle alignment, defaults to {@link #DEFAULT_ASS_SUB_ALIGNMENT}
     */
    public void setSubtitleParams(final Font subFont, final float subLineHeightPct, final float subLineDY, final Alignment subAlignment) {
        if( null != mButton ) {
            mButton.setSubtitleParams(subFont, subLineHeightPct, subLineDY, subAlignment);
        }
    }
    /**
     * Sets text/ASS subtitle colors
     * @param color color for the text/ASS, defaults to {@link MediaButton#DEFAULT_ASS_SUB_COLOR}
     * @param blend blending alpha (darkness), defaults to {@link MediaButton#DEFAULT_ASS_SUB_BLEND}
     */
    public void setSubtitleColor(final Vec4f color, final float blend) {
        if( null != mButton ) {
            mButton.setSubtitleColor(color, blend);
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
            final String text1 = String.format("%s / %s (%.0f %%), %s (%01.2fx, vol %1.2f), A/R %.2f, fps %02.1f, kbps %.2f",
                    PTS.toTimeStr(ptsMS, false), PTS.toTimeStr(durationMS, false), pct*100,
                    mPlayer.getState().toString().toLowerCase(), mPlayer.getPlaySpeed(), mPlayer.getAudioVolume(), aspect,
                    mPlayer.getFramerate(), mPlayer.getStreamBitrate()/1000.0f);
            final String text2 = String.format("video: id %d (%s), kbps %.2f, codec %s/'%s'",
                    mPlayer.getVID(), mPlayer.getLang(mPlayer.getVID()), mPlayer.getVideoBitrate()/1000.0f, mPlayer.getVideoCodecID(), mPlayer.getVideoCodec());
            final String text3 = String.format("audio: id %d/%s (%s/%s), kbps %.2f, codec %s/'%s'",
                    mPlayer.getAID(), Arrays.toString(mPlayer.getAStreams()),
                    mPlayer.getLang(mPlayer.getAID()), Arrays.toString(mPlayer.getALangs()),
                    mPlayer.getAudioBitrate()/1000.0f, mPlayer.getAudioCodecID(), mPlayer.getAudioCodec());
            final String text4 = String.format("sub  : id %d/%s (%s/%s), codec %s/'%s'",
                    mPlayer.getSID(), Arrays.toString(mPlayer.getSStreams()),
                    mPlayer.getLang(mPlayer.getSID()), Arrays.toString(mPlayer.getSLangs()),
                    mPlayer.getSubtitleCodecID(), mPlayer.getSubtitleCodec());
            return text1+"\n"+text2+"\n"+text3+"\n"+text4+"\n"+mPlayer.getTitle()+chapter;
        } else {
            final String vinfo, ainfo, sinfo;
            if( mPlayer.getVID() != GLMediaPlayer.STREAM_ID_NONE ) {
                vinfo = String.format((Locale)null, ", vid %d (%s)", mPlayer.getVID(), mPlayer.getVideoCodecID());
            } else {
                vinfo = "";
            }
            if( mPlayer.getAID() != GLMediaPlayer.STREAM_ID_NONE ) {
                ainfo = String.format((Locale)null, ", aid %d (%s, %s)", mPlayer.getAID(), mPlayer.getLang(mPlayer.getAID()), mPlayer.getAudioCodecID());
            } else {
                ainfo = "";
            }
            if( mPlayer.getSID() != GLMediaPlayer.STREAM_ID_NONE ) {
                sinfo = String.format((Locale)null, ", sid %d (%s, %s)", mPlayer.getSID(), mPlayer.getLang(mPlayer.getSID()), mPlayer.getSubtitleCodecID());
            } else {
                sinfo = "";
            }
            final String text1 = String.format("%s / %s (%.0f %%), %s (%01.2fx, vol %1.2f), A/R %.2f%s%s%s",
                    PTS.toTimeStr(ptsMS, false), PTS.toTimeStr(durationMS, false), pct*100,
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
                    pct*100, PTS.toTimeStr(ptsMS, false), PTS.toTimeStr(durationMS, false));
    }
}
