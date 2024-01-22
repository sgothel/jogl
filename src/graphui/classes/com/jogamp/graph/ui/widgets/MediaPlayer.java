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
    public static final Vec2f FixedSymSize = new Vec2f(0.0f, 1.0f);
    public static final Vec2f SymSpacing = new Vec2f(0f, 0.2f);
    public static final float CtrlButtonWidth = 1f;
    public static final float CtrlButtonHeight = 1f;
    public static final Vec4f CtrlCellCol = new Vec4f(0, 0, 0, 0);

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

        final Font fontInfo = getInfoFont(), fontSymbols = getSymbolsFont();
        if( null == fontInfo || null == fontSymbols ) {
            return;
        }
        final float borderSz = 0.01f;
        final float borderSzS = 0.03f;
        final Vec4f borderColor = new Vec4f(0, 0, 0, 0.5f);
        final Vec4f borderColorA = new Vec4f(0, 0, 0.5f, 0.5f);
        final float alphaBlend = 0.3f;

        final float zEpsilon = scene.getZEpsilon(16);
        final float ctrlZOffset = zEpsilon * 20f;

        final int ctrlCellsInt = 9;
        final int ctrlCells = Math.max(customCtrls.size() + ctrlCellsInt, 13);

        final float ctrlCellWidth = (aratio-2*borderSzS)/ctrlCells;
        final float ctrlCellHeight = ctrlCellWidth;
        final float ctrlSliderHeightMin = ctrlCellHeight/15f;       // bar-height
        final float ctrlSliderHeightMax = 3f * ctrlSliderHeightMin; // knob-height
        final float infoGroupHeight = 1/7f;

        final Shape[] zoomReplacement = { null };
        final Vec3f[] zoomOrigScale = { null };
        final Vec3f[] zoomOrigPos = { null };

        this.setName("mp.container");
        this.setBorderColor(borderColor).setBorder(borderSz);
        this.setInteractive(true).setFixedARatioResize(true);

        final MediaButton mButton = new MediaButton(renderModes, aratio, 1, mPlayer);
        mButton.setName("mp.mButton").setInteractive(false);
        mButton.setPerp().setPressedColorMod(1f, 1f, 1f, 0.85f);

        final RangeSlider ctrlSlider;
        {
            final float knobScale = ctrlSliderHeightMax / ctrlSliderHeightMin;
            ctrlSlider = new RangeSlider(renderModes, new Vec2f(aratio - ctrlSliderHeightMax, ctrlSliderHeightMin), knobScale, new Vec2f(0, 100), 1, 0);
            final float dx = ctrlSliderHeightMax / 2f;
            final float dy = ( ctrlSliderHeightMax - ctrlSliderHeightMin ) * 0.5f;
            ctrlSlider.setPaddding(new Padding(0, dx, ctrlCellHeight-dy, dx));
        }
        ctrlSlider.setName("mp.slider");

        final Button playButton = new Button(renderModes, fontSymbols,
                fontSymbols.getUTF16String("play_arrow"),  fontSymbols.getUTF16String("pause"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
        playButton.setName("mp.play");
        playButton.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);

        {
            mButton.setVerbose(false).addDefaultEventListener().setTextureLetterbox(letterBox);
            mPlayer.setAudioVolume( 0f );
            mPlayer.addEventListener( new GLMediaEventListener() {
                @Override
                public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) { }

                @Override
                public void attributesChanged(final GLMediaPlayer mp, final EventMask eventMask, final long when) {
                    // System.err.println("MediaButton AttributesChanges: "+eventMask+", when "+when);
                    // System.err.println("MediaButton State: "+mp);
                    if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Init) ) {
                        ctrlSlider.setMinMax(new Vec2f(0, mp.getDuration()), 0);
                        System.err.println(mp.toString());
                        for(final GLMediaPlayer.Chapter c : mp.getChapters()) {
                            System.err.println(c);
                            final Shape mark = ctrlSlider.addMark(c.start, new Vec4f(0.9f, 0.9f, 0.9f, 0.5f));
                            mark.setToolTip(new TooltipText(c.title+"\n"+PTS.millisToTimeStr(c.start, false), fontInfo, 5));
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
                final Rectangle rect = new Rectangle(renderModes & ~Region.AA_RENDERING_MASK, aratio, infoGroupHeight, 0);
                rect.setName("mp.info.blend").setInteractive(false);
                rect.setColor(0, 0, 0, alphaBlend);
                rect.setPaddding(new Padding(0, 0, 1f-infoGroupHeight, 0));
                infoGroup.addShape(rect);
            }
            {
                final int lines = 3;
                final String text = getInfo(Clock.currentMillis(), mPlayer, false);
                infoLabel = new Label(renderModes, fontInfo, aratio/40f, text);
                infoLabel.setName("mp.info.label");
                final float szw = aratio/40f;
                infoLabel.setPaddding(new Padding(0, 0, 1f-szw*lines, szw));
                infoLabel.setInteractive(false);
                infoLabel.setColor(1, 1, 1, 1);
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
                    final int durationMS = mPlayer.getDuration();
                    timeLabel.setText(getMultilineTime(ptsMS, durationMS));
                    mPlayer.seek(ptsMS);
                }
                @Override
                public void clicked(final RangeSlider w, final MouseEvent e) {
                    System.err.println("Clicked "+w.getName()+": "+PTS.millisToTimeStr(Math.round(w.getValue()), true)+"ms, "+(w.getValuePct()*100f)+"%");
                    seekPlayer( Math.round( w.getValue() ) );
                }
                @Override
                public void dragged(final RangeSlider w, final float old_val, final float val, final float old_val_pct, final float val_pct) {
                    System.err.println("Dragged "+w.getName()+": "+PTS.millisToTimeStr(Math.round(val), true)+"ms, "+(val_pct*100f)+"%");
                    seekPlayer( Math.round( val ) );
                }
            });
            this.addShape( ctrlSlider.setVisible(false) );

            ctrlBlend = new Rectangle(renderModes & ~Region.AA_RENDERING_MASK, aratio, ctrlCellHeight, 0);
            ctrlBlend.setName("ctrl.blend").setInteractive(false);
            ctrlBlend.setColor(0, 0, 0, alphaBlend);
            this.addShape( ctrlBlend.setVisible(false) );

            final float toolTipScaleY = 0.6f;
            ctrlGroup = new Group(new GridLayout(ctrlCellWidth, ctrlCellHeight, Alignment.FillCenter, Gap.None, 1));
            ctrlGroup.setName("ctrlGroup").setInteractive(false);
            ctrlGroup.setPaddding(new Padding(0, borderSzS, 0, borderSzS));
            this.addShape( ctrlGroup.move(0, 0, ctrlZOffset).setVisible(false) );
            { // 1
                playButton.onToggle((final Shape s) -> {
                    if( s.isToggleOn() ) {
                        mPlayer.resume();
                    } else {
                        mPlayer.pause(false);
                    }
                });
                playButton.setToggle(true); // on == play
                ctrlGroup.addShape(playButton);
                playButton.setToolTip(new TooltipText("Play", fontInfo, toolTipScaleY));
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
                button.setName("frev");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s) -> {
                    mPlayer.setPlaySpeed(mPlayer.getPlaySpeed() - 0.5f);
                });
                ctrlGroup.addShape(button);
                button.setToolTip(new TooltipText("Fast-Rewind", fontInfo, toolTipScaleY));
            }
            { // 4
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("fast_forward"), CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("ffwd");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onClicked((final Shape s) -> {
                    mPlayer.setPlaySpeed(mPlayer.getPlaySpeed() + 0.5f);
                });
                ctrlGroup.addShape(button);
                button.setToolTip(new TooltipText("Fast-Forward", fontInfo, toolTipScaleY));
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
                        System.err.println("Seek: "+pts0+" -> "+pts1);
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
                        System.err.println("Seek: "+pts0+" -> "+pts1);
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
                ctrlGroup.addShape(timeLabel);
            }
            for(int i=8; i<ctrlCells-1-customCtrls.size(); ++i) {
                final Button button = new Button(renderModes, fontInfo, " ", CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("ctrl_"+i);
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                ctrlGroup.addShape(button);
            }
            { // -1
                final Button button = new Button(renderModes, fontSymbols,
                        fontSymbols.getUTF16String("zoom_out_map"), fontSymbols.getUTF16String("zoom_in_map"),  CtrlButtonWidth, CtrlButtonHeight, zEpsilon);
                button.setName("zoom");
                button.setSpacing(SymSpacing, FixedSymSize).setPerp().setColor(CtrlCellCol);
                button.onToggle( (final Shape s) -> {
                    if( s.isToggleOn() ) {
                        final AABBox sbox = scene.getBounds();
                        final Group parent = this.getParent();
                        if( null != parent ) {
                            zoomReplacement[0] = new Label(renderModes, fontInfo, aratio/40f, "zoomed");
                            final boolean r = parent.replaceShape(this, zoomReplacement[0]);
                            if( r ) {
                                // System.err.println("Zoom1: p "+parent);
                                // System.err.println("Zoom1: t "+this);
                                final float sxy = sbox.getWidth() * zoomSize / this.getScaledWidth();
                                scene.addShape(this);
                                this.scale(sxy, sxy, 1f);
                                this.moveTo(sbox.getLow()).move(sbox.getWidth() * ( 1f - zoomSize )/2.0f, sbox.getHeight() * ( 1f - zoomSize )/2.0f, ctrlZOffset);
                            } else {
                                System.err.println("Zoom1: failed "+this);
                            }
                        } else {
                            zoomOrigScale[0] = this.getScale().copy();
                            zoomOrigPos[0] = this.getPosition().copy();
                            // System.err.println("Zoom2: top");
                            // System.err.println("Zoom2: t "+this);
                            final float sxy = sbox.getWidth() * zoomSize / this.getScaledWidth();
                            this.scale(sxy, sxy, 1f);
                            this.moveTo(sbox.getLow()).move(sbox.getWidth() * ( 1f - zoomSize )/2.0f, sbox.getHeight() * ( 1f - zoomSize )/2.0f, ctrlZOffset);
                        }
                    } else {
                        if( null != zoomReplacement[0] ) {
                            final Group parent = zoomReplacement[0].getParent();
                            scene.removeShape(this);
                            this.moveTo(0, 0, 0);
                            parent.replaceShape(zoomReplacement[0], this);
                            scene.invoke(true, (drawable) -> {
                                final GL2ES2 gl = drawable.getGL().getGL2ES2();
                                zoomReplacement[0].destroy(gl, scene.getRenderer());
                                return true;
                            });
                            zoomReplacement[0] = null;
                            // System.err.println("Reset1: "+parent);
                        } else if( null != zoomOrigScale[0] && null != zoomOrigPos[0] ){
                            this.scale(zoomOrigScale[0]);
                            this.moveTo(zoomOrigPos[0]);
                            zoomOrigScale[0] = null;
                            zoomOrigPos[0] = null;
                            // System.err.println("Reset2: top");
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
                this.setBorderColor(borderColorA);
            } else {
                this.setBorderColor(borderColor);
                ctrlSlider.setVisible(false);
                ctrlBlend.setVisible(false);
                ctrlGroup.setVisible(false);
                infoGroup.setVisible(false);
            }
        });
        this.addMouseListener(new Shape.MouseGestureAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                final Vec3f p = shapeEvent.objPos;
                final boolean c = ( ctrlCellHeight + ctrlSliderHeightMax ) > p.y() || p.y() > ( 1f - infoGroupHeight );
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

    /** Returns the used info font or null if n/a */
    public static Font getInfoFont() {
        try {
            return FontFactory.get(FontFactory.UBUNTU).getDefault();
        } catch(final IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

    /** Returns the used symbols font or null if n/a */
    public static Font getSymbolsFont() {
        try {
            return FontFactory.get(FontFactory.SYMBOLS).getDefault();
        } catch(final IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
    }

    public static String getInfo(final long currentMillis, final GLMediaPlayer mPlayer, final boolean full) {
        return getInfo(mPlayer.getPTS().get(currentMillis), mPlayer.getDuration(), mPlayer, full);
    }
    public static String getInfo(final int ptsMS, final int durationMS, final GLMediaPlayer mPlayer, final boolean full) {
        final String name, chapter;
        {
            final String basename;
            final String s = mPlayer.getUri().path.decode();
            final int li = s.lastIndexOf('/');
            if( 0 < li ) {
                basename = s.substring(li+1);
            } else {
                basename = s;
            }
            final int di = basename.lastIndexOf('.');
            if( 0 < di ) {
                name = basename.substring(0, di);
            } else {
                name = basename;
            }
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
            final String text1 = String.format("%s / %s (%.0f %%), %s (%01.1fx, vol %1.2f), A/R %0.2f, fps %02.1f",
                    PTS.millisToTimeStr(ptsMS, false), PTS.millisToTimeStr(durationMS, false), pct*100,
                    mPlayer.getState().toString().toLowerCase(), mPlayer.getPlaySpeed(), mPlayer.getAudioVolume(), aspect, mPlayer.getFramerate());
            final String text2 = String.format("audio: id %d, kbps %d, codec %s",
                    mPlayer.getAID(), mPlayer.getAudioBitrate()/1000, mPlayer.getAudioCodec());
            final String text3 = String.format("video: id %d, kbps %d, codec %s",
                    mPlayer.getVID(), mPlayer.getVideoBitrate()/1000, mPlayer.getVideoCodec());
            return text1+"\n"+text2+"\n"+text3+"\n"+name+chapter;
        } else {
            final String text1 = String.format("%s / %s (%.0f %%), %s (%01.1fx, vol %1.2f), A/R %.2f",
                    PTS.millisToTimeStr(ptsMS, false), PTS.millisToTimeStr(durationMS, false), pct*100,
                    mPlayer.getState().toString().toLowerCase(), mPlayer.getPlaySpeed(), mPlayer.getAudioVolume(), aspect);
            return text1+"\n"+name+chapter;
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
