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
import com.jogamp.graph.ui.shapes.BaseButton;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * RangeSlider {@link Widget}
 * @see #RangeSlider(int, float, float, float, float, float, float)
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
         * Slide dragged by user
         * @param w the {@link RangeSlider} widget owning the slider
         * @param old_val previous absolute value position of the slider
         * @param val the absolute value position of the slider
         * @param old_val_pct previous percentage value position of the slider
         * @param val_pct the percentage value position of the slider
         */
        void dragged(RangeSlider w, float old_val, float val, float old_val_pct, float val_pct);
    }

    private final boolean horizontal;
    private final float knobSz;
    private final float width, height;
    private final Group barAndKnob;
    private final Rectangle bar;
    private final BaseButton knob;
    private final Vec4f colBar = new Vec4f(0, 0, 1, 1);
    private final Vec4f colKnob = new Vec4f(1, 0, 0, 1);
    private SliderListener sliderListener = null;
    private float min, max;
    private float val, val_pct;

    /**
     * Constructs a {@link RangeSlider}, i.e. its shapes and controls.
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param width width of this slider box. A horizontal slider has width >= height.
     * @param height height of this slider box. A vertical slider has width < height.
     * @param knobScale multiple of slider-bar height for {@link #getKnobSize()}
     * @param min minimum value of slider
     * @param max maximum value of slider
     * @param value current value of slider
     */
    public RangeSlider(final int renderModes, final float width, final float height, final float knobScale,
                       final float min, final float max, final float value) {
        this.horizontal = width >= height;
        if( horizontal ) {
            knobSz = height*knobScale;
            this.width = width - knobSz; // half knobSz left and right
            this.height = height;
        } else {
            knobSz = width*knobScale;
            this.width = width;
            this.height = height - knobSz; // half knobSz bottom and top
        }
        barAndKnob = new Group();
        barAndKnob.setInteractive(true).setDraggable(false).setToggleable(false);
        bar = new Rectangle(renderModes & ~Region.AA_RENDERING_MASK, this.width, this.height, 0);
        bar.setToggleable(false);
        bar.setColor(colBar);
        knob = new BaseButton(renderModes  & ~Region.AA_RENDERING_MASK, knobSz*1.01f, knobSz);
        knob.setToggleable(false);
        knob.setColor(colKnob);
        setName(getName());
        barAndKnob.addShape( bar );
        barAndKnob.addShape( knob );
        addShape(barAndKnob);

        setMinMax(min, max, value);

        knob.onMove((final Shape s, final Vec3f origin, final Vec3f dest) -> {
            final float old_val = val;
            final float old_val_pct = val_pct;
            setValuePct( getKnobValuePct( dest.x(), dest.y(), knobSz*0.5f ) );
            // System.err.println("KnobMove "+getName()+": "+origin+" -> "+dest+": "+old_val+" -> "+val+", "+(old_val_pct*100f)+"% -> "+(val_pct*100f)+"%");
            if( null != sliderListener ) {
                sliderListener.dragged(this, old_val, val, old_val_pct, val_pct);
            }
        });
        barAndKnob.addMouseListener(new Shape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                setValuePct( getKnobValuePct( shapeEvent.objPos.x(), shapeEvent.objPos.y(), 0 ) );
                if( null != sliderListener ) {
                    sliderListener.clicked(RangeSlider.this, e);
                }
            }
        });
        knob.addMouseListener(new Shape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if( null != sliderListener ) {
                    sliderListener.clicked(RangeSlider.this, e);
                }
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
            @Override
            public void mouseWheelMoved(final MouseEvent e) {
                // Support ?
            }
        });
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
    public BaseButton getKnob() { return knob; }

    public final float getWidth() { return width; }
    public final float getHeight() { return height; }
    public final float getKnobSize() { return knobSz; }

    public float getMin() { return min; }
    public float getMax() { return max; }
    public float getRange() { return max - min; }
    public float getValue() { return val; }
    public float getValuePct() { return val_pct; }

    /**
     * Sets slider value range and current value
     * @param min minimum value of slider
     * @param max maximum value of slider
     * @param value current value of slider
     * @return this instance of chaining
     */
    public RangeSlider setMinMax(final float min, final float max, final float value) {
        this.min = min;
        this.max = max;
        this.val = Math.max(min, Math.min(max, value));
        this.val_pct = ( value - min ) / getRange();
        setKnob();
        return this;
    }

    public RangeSlider setValuePct(final float v) {
        val_pct = v;
        val = min + ( val_pct * getRange() );
        setKnob();
        return this;
    }

    public RangeSlider setValue(final float v) {
        val = v;
        val_pct = ( val - min ) / getRange();
        setKnob();
        return this;
    }

    /**
     * Knob position reflects value on its center and ranges from zero to max.
     */
    private Vec2f getKnobPos(final Vec2f posRes, final float val_pct) {
        if( horizontal ) {
            posRes.setX( val_pct*width - knobSz*0.5f );
            posRes.setY( -( knobSz - height ) * 0.5f );
        } else {
            posRes.setX( -( knobSz - width ) * 0.5f );
            posRes.setY( val_pct*height - knobSz*0.5f );
        }
        return posRes;
    }
    private float getKnobValuePct(final float pos_x, final float pos_y, final float adjustment) {
        final float v;
        if( horizontal ) {
            v = ( pos_x + adjustment ) / width;
        } else {
            v = ( pos_y + adjustment ) / height;
        }
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    private void setKnob() {
        final Vec2f pos = getKnobPos(new Vec2f(), val_pct);
        knob.moveTo(pos.x(), pos.y(), Button.DEFAULT_LABEL_ZOFFSET);
    }
}
