/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.BaseButton;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * RangeSlider {@link Widget} either utilizing a simple positional round knob
 * or a rectangular page-sized knob.
 * @see #RangeSlider(int, Vec2f, float, Vec2f, float)
 * @see #RangeSlider(int, Vec2f, Vec2f, float, float)
 */
public final class RangeSlider extends Widget {
    /**
     * {@link RangeSlider} slider listener
     */
    public static interface SliderListener {
        /** Slider clicked by user (after completing pressed + released). */
        void clicked(RangeSlider w, final MouseEvent e);
        /** Slider pressed down by user. */
        void pressed(RangeSlider w, final MouseEvent e);
        /** Slider released down by user. */
        void released(RangeSlider w, final MouseEvent e);
        /**
         * Slide dragged by user (including clicked position)
         * @param w the {@link RangeSlider} widget owning the slider
         * @param old_val previous absolute value position of the slider
         * @param val the absolute value position of the slider
         * @param old_val_pct previous percentage value position of the slider
         * @param val_pct the percentage value position of the slider
         */
        void dragged(RangeSlider w, float old_val, float val, float old_val_pct, float val_pct);
    }

    private static final float pageKnobScale = 0.6f;     // 0.6 * barWidth
    private static final float pageBarLineScale = 0.25f; // 1/4 * ( barWidth - pageKnobWidth )
    private static final float pageKnobSizePctMin = 5f/100f;
    private final boolean horizontal;
    /** Width of knob orthogonal to sliding direction */
    private float knobDiameter;
    /** Half length of knob in sliding direction */
    private float knobHalfLen;
    private final Vec2f size;
    private final Group barAndKnob;
    private final Rectangle bar;
    private final Shape knob;
    private SliderListener sliderListener = null;
    private final Vec2f minMax = new Vec2f(0, 100);
    private float pageSize;
    private float val=0, val_pct=0;
    private boolean inverted=false;

    /**
     * Constructs a {@link RangeSlider}, i.e. its shapes and controls.
     * <p>
     * This slider comprises a background bar and a positional round knob.
     * </p>
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param size width and height of this slider box. A horizontal slider has width >= height.
     * @param knobScale multiple of slider-bar height for {@link #getKnobHeight()}
     * @param minMax minimum- and maximum-value of slider
     * @param value current value of slider
     */
    public RangeSlider(final int renderModes, final Vec2f size, final float knobScale,
                       final Vec2f minMax, final float value) {
        this(renderModes, size, knobScale, minMax, Float.NaN, value);
    }
    /**
     * Constructs a {@link RangeSlider}, i.e. its shapes and controls.
     * <p>
     * This slider comprises a framing bar and a rectangular page-sized knob.
     * </p>
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param size width and height of this slider box. A horizontal slider has width >= height.
     * @param minMax minimum- and maximum-value of slider
     * @param pageSize size of one virtual-page, triggers rendering mode from knob to rectangle
     * @param value current value of slider
     */
    public RangeSlider(final int renderModes, final Vec2f size,
                       final Vec2f minMax, final float pageSize, final float value) {
        this(renderModes, size, 0, minMax, pageSize, value);
    }
    private RangeSlider(final int renderModes_, final Vec2f size, final float knobScale,
                       final Vec2f minMax, final float pageSz, final float value) {
        // final int renderModes = ( renderModes_ & ~Region.AA_RENDERING_MASK ) | Region.COLORCHANNEL_RENDERING_BIT;
        final int renderModes = renderModes_ & ~(Region.AA_RENDERING_MASK | Region.COLORCHANNEL_RENDERING_BIT);
        this.pageSize = pageSz;
        this.horizontal = size.x() >= size.y();
        barAndKnob = new Group();
        barAndKnob.setInteractive(true).setToggleable(false).setDraggable(false).setResizable(false);
        this.size = new Vec2f(size);
        if( Float.isNaN(pageSize) ) {
            if( horizontal ) {
                knobDiameter = size.y()*knobScale;
                setPaddding(new Padding(knobHalfLen, 0, knobHalfLen, 0));
            } else {
                knobDiameter = size.x()*knobScale;
                setPaddding(new Padding(0, knobHalfLen, 0, knobHalfLen));
            }
            knobHalfLen = knobDiameter * 0.5f;
            bar = new Rectangle(renderModes, this.size.x(), this.size.y(), 0);
            knob = new BaseButton(renderModes , knobDiameter*1.01f, knobDiameter);
            setBackgroundBarColor(0.60f, 0.60f, 0.60f, 0.5f);
        } else {
            final float pageSizePct = Math.max(pageKnobSizePctMin, pageSize / getRange(minMax));
            final float barLineWidth;
            final float knobWidth, knobHeight;
            if( horizontal ) {
                knobHeight = size.y() * pageKnobScale;
                knobWidth = pageSizePct * this.size.x();
                barLineWidth = ( size.y() - knobHeight ) * pageBarLineScale;
                knobHalfLen = knobWidth * 0.5f;
                knobDiameter = knobHeight;
                setPaddding(new Padding(size.y(), 0, size.y(), 0));
            } else {
                knobWidth = size.x() * pageKnobScale;
                knobHeight = pageSizePct * this.size.y();
                barLineWidth = ( size.x() - knobWidth ) * pageBarLineScale;
                knobHalfLen = knobHeight * 0.5f;
                knobDiameter = knobWidth;
                setPaddding(new Padding(0, size.x(), 0, size.x()));
                // System.err.println("ZZZ minMax "+minMax+", pageSize "+pageSize+" "+(pageSizePct*100f)+"% -> "+knobHeight+"/"+this.size.y());
            }
            bar = new Rectangle(renderModes, this.size.x(), this.size.y(), barLineWidth);
            knob = new Rectangle(renderModes, knobWidth, knobHeight, 0);
        }
        setColor(0.30f, 0.30f, 0.30f, 1.0f);

        bar.setToggleable(false).setInteractive(false);
        bar.setDraggable(false).setResizable(false);

        knob.setToggleable(false).setResizable(false);
        setName(getName());
        barAndKnob.addShape( bar );
        barAndKnob.addShape( knob );
        addShape(barAndKnob);

        setMinMax(minMax, value);

        knob.onMove((final Shape s, final Vec3f origin, final Vec3f dest) -> {
            final float old_val = val;
            final float old_val_pct = val_pct;
            setValuePct( getKnobValuePct( dest.x(), dest.y(), knobHalfLen ) );
            if( null != sliderListener ) {
                sliderListener.dragged(this, old_val, val, old_val_pct, val_pct);
            }
        });
        barAndKnob.addMouseListener(new Shape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                final float old_val = val;
                final float old_val_pct = val_pct;
                final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                setValuePct( getKnobValuePct( shapeEvent.objPos.x(), shapeEvent.objPos.y(), 0 ) );
                if( null != sliderListener ) {
                    sliderListener.dragged(RangeSlider.this, old_val, val, old_val_pct, val_pct);
                    sliderListener.clicked(RangeSlider.this, e);
                }
            }
            @Override
            public void mouseWheelMoved(final MouseEvent e) {
                final float old_val = val;
                final float old_val_pct = val_pct;
                float v = old_val;
                if( !e.isControlDown() ) {
                    if( e.getRotation()[1] < 0f ) {
                        if( inverted ) {
                            v++;
                        } else {
                            v--;
                        }
                    } else {
                        if( inverted ) {
                            v--;
                        } else {
                            v++;
                        }
                    }
                } else if( !Float.isNaN(pageSize) ){
                    if( e.getRotation()[1] < 0f ) {
                        if( inverted ) {
                            v+=pageSize;
                        } else {
                            v-=pageSize;
                        }
                    } else {
                        if( inverted ) {
                            v-=pageSize;
                        } else {
                            v+=pageSize;
                        }
                    }
                }
                setValue( v );
                if( null != sliderListener ) {
                    sliderListener.dragged(RangeSlider.this, old_val, val, old_val_pct, val_pct);
                }
            }
        });
        knob.addMouseListener(new Shape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                // if( null != sliderListener ) {
                //    sliderListener.clicked(RangeSlider.this, e);
                // }
            }
            @Override
            public void mousePressed(final MouseEvent e) {
                if( null != sliderListener ) {
                    sliderListener.pressed(RangeSlider.this, e);
                }
            }
            @Override
            public void mouseReleased(final MouseEvent e) {
                if( null != sliderListener ) {
                    sliderListener.released(RangeSlider.this, e);
                }
            }
        });
        final KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                final float old_val = val;
                final float old_val_pct = val_pct;
                float v = old_val;
                final short keySym = e.getKeySymbol();
                boolean action = false;
                if( horizontal ) {
                    if( keySym == KeyEvent.VK_RIGHT ) {
                        action = true;
                        if( inverted ) {
                            v--;
                        } else {
                            v++;
                        }
                    } else if( keySym == KeyEvent.VK_LEFT ) {
                        action = true;
                        if( inverted ) {
                            v++;
                        } else {
                            v--;
                        }
                    }
                } else {
                    if( keySym == KeyEvent.VK_DOWN ) {
                        action = true;
                        if( inverted ) {
                            v++;
                        } else {
                            v--;
                        }
                    } else if( keySym == KeyEvent.VK_UP ) {
                        action = true;
                        if( inverted ) {
                            v--;
                        } else {
                            v++;
                        }
                    }
                }
                if( !action && !Float.isNaN(pageSize) ) {
                    if( keySym == KeyEvent.VK_PAGE_DOWN ) {
                        action = true;
                        if( inverted ) {
                            v+=pageSize;
                        } else {
                            v-=pageSize;
                        }
                    } else if( keySym == KeyEvent.VK_PAGE_UP ) {
                        action = true;
                        if( inverted ) {
                            v-=pageSize;
                        } else {
                            v+=pageSize;
                        }
                    }
                }
                if( action ) {
                    setValue( v );
                    if( null != sliderListener ) {
                        sliderListener.dragged(RangeSlider.this, old_val, val, old_val_pct, val_pct);
                    }
                }
            }
        };
        barAndKnob.addKeyListener(keyListener);
        knob.addKeyListener(keyListener);
    }

    @Override
    protected void clearImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        super.clearImpl0(gl, renderer);
        sliderListener = null;
    }
    @Override
    protected void destroyImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        super.destroyImpl0(gl, renderer);
        sliderListener = null;
    }

    public RangeSlider onSlider(final SliderListener l) {
        sliderListener = l;
        return this;
    }

    public Rectangle getBar() { return bar; }
    public Shape getKnob() { return knob; }

    public final Vec2f getSize() { return size; }
    /** Height of knob orthogonal to sliding direction */
    public final float getKnobHeight() { return knobDiameter; }
    /** Length of knob in sliding direction */
    public final float getKnobLength() { return knobHalfLen*2f; }

    public Vec2f getMinMax() { return minMax; }
    public float getRange() { return minMax.y() - minMax.x(); }
    private float getRange(final Vec2f minMax) { return minMax.y() - minMax.x(); }
    public float getValue() { return val; }
    public float getValuePct() { return val_pct; }

    public RangeSlider setPageSize(final float v) {
        if( Float.isNaN(this.pageSize) || Float.isNaN(v) ) {
            return this;
        }
        this.pageSize = v;
        final float pageSizePct = Math.max(pageKnobSizePctMin, pageSize / getRange());
        final float knobWidth, knobHeight;
        if( horizontal ) {
            knobHeight = size.y() * pageKnobScale;
            knobWidth = pageSizePct * this.size.x();
            knobHalfLen = knobWidth * 0.5f;
            knobDiameter = knobHeight;
        } else {
            knobWidth = size.x() * pageKnobScale;
            knobHeight = pageSizePct * this.size.y();
            knobHalfLen = knobHeight * 0.5f;
            knobDiameter = knobWidth;
        }
        ((Rectangle)knob).setDimension(knobWidth, knobHeight, 0);
        return this;
    }
    public float getPageSize() { return this.pageSize; }

    public RangeSlider setInverted(final boolean v) { inverted = v; return setValue(val); }

    /**
     * Sets slider value range and current value
     * @param minMax minimum- and maximum-value of slider
     * @param value current value of slider
     * @return this instance of chaining
     */
    public RangeSlider setMinMax(final Vec2f minMax, final float value) {
        this.minMax.set(minMax);
        return setValue( value );
    }

    public RangeSlider setValuePct(final float v) {
        final float pct = Math.max(0.0f, Math.min(1.0f, v));
        return setValue( minMax.x() + ( pct * getRange() ) );
    }

    public RangeSlider setValue(final float v) {
        val = Math.max(minMax.x(), Math.min(minMax.y(), v));
        val_pct = ( val - minMax.x() ) / getRange();
        setKnob();
        return this;
    }

    /**
     * Knob position reflects value on its center and ranges from zero to max.
     */
    private Vec2f getKnobPos(final Vec2f posRes, final float val_pct) {
        final float v = inverted ? 1f - val_pct : val_pct;
        if( horizontal ) {
            posRes.setX( Math.max(0, Math.min(size.x(), v*size.x() - knobHalfLen)) );
            posRes.setY( -( knobDiameter - size.y() ) * 0.5f );
        } else {
            posRes.setX( -( knobDiameter - size.x() ) * 0.5f );
            posRes.setY( Math.max(0, Math.min(size.y() - 2*knobHalfLen, v*size.y() - knobHalfLen)) );
        }
        return posRes;
    }
    private float getKnobValuePct(final float pos_x, final float pos_y, final float adjustment) {
        final float v;
        if( horizontal ) {
            v = ( pos_x + adjustment ) / size.x();
        } else {
            v = ( pos_y + adjustment ) / size.y();
        }
        return Math.max(0.0f, Math.min(1.0f, inverted ? 1f - v : v));
    }

    private void setKnob() {
        final Vec2f pos = getKnobPos(new Vec2f(), val_pct);
        knob.moveTo(pos.x(), pos.y(), Button.DEFAULT_LABEL_ZOFFSET);
    }

    /**
     * Sets the slider knob color.
     * <p>
     * If this slider comprises a rectangular page-sized knob,
     * its rectangular frame also shares the same color.
     * </p>
     * <p>
     * Base color w/o color channel, will be modulated w/ pressed- and toggle color
     * </p>
     * <p>
     * Default RGBA value is 0.30f, 0.30f, 0.30f, 1.0f
     * </p>
     */
    @Override
    public final Shape setColor(final float r, final float g, final float b, final float a) {
        super.setColor(r, g, b, a);
        knob.setColor(r, g, b, a);
        if( !Float.isNaN(pageSize) ) {
            bar.setColor(r, g, b, a);
        }
        return this;
    }

    /**
     * Sets the slider knob color.
     * <p>
     * If this slider comprises a rectangular page-sized knob,
     * its rectangular frame also shares the same color.
     * </p>
     * <p>
     * Base color w/o color channel, will be modulated w/ pressed- and toggle color
     * </p>
     * <p>
     * Default RGBA value is 0.30f, 0.30f, 0.30f, 1.0f
     * </p>
     */
    @Override
    public Shape setColor(final Vec4f c) {
        this.rgbaColor.set(c);
        knob.setColor(c);
        if( !Float.isNaN(pageSize) ) {
            bar.setColor(c);
        }
        return this;
    }

    /**
     * Sets the slider background bar color, if this slider comprises only a positional round knob.
     * <p>
     * Default RGBA value is 0.60f, 0.60f, 0.60f, 0.5f
     * </p>
     */
    public Shape setBackgroundBarColor(final float r, final float g, final float b, final float a) {
        if( Float.isNaN(pageSize) ) {
            bar.setColor(r, g, b, a);
        }
        return this;
    }
    /**
     * Sets the slider background bar color, if this slider comprises only a positional round knob.
     * <p>
     * Default RGBA value is 0.60f, 0.60f, 0.60f, 0.5f
     * </p>
     */
    public Shape setBackgroundBarColor(final Vec4f c) {
        if( Float.isNaN(pageSize) ) {
            bar.setColor(c);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sets the slider bar and knob pressed color modulation.
     * </p>
     */
    @Override
    public final Shape setPressedColorMod(final float r, final float g, final float b, final float a) {
        super.setPressedColorMod(r, g, b, a);
        bar.setPressedColorMod(r, g, b, a);
        knob.setPressedColorMod(r, g, b, a);
        return this;
    }

    @Override
    public String getSubString() {
        return super.getSubString()+", range["+minMax+"] @ "+val+", "+(100f*val_pct)+"%";
    }
}
