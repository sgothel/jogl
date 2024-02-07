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

import java.util.ArrayList;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.BaseButton;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.FloatUtil;
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
 * @see #RangeSlider(int, Vec2f, float, Vec2f, float, float)
 * @see #RangeSlider(int, Vec2f, Vec2f, float, float, float)
 */
public final class RangeSlider extends Widget {
    /**
     * {@link RangeSlider} slider value changed listener
     */
    public static interface ChangeListener {
        /**
         * Slide dragged by user (including clicked position)
         * @param w the {@link RangeSlider} widget owning the slider
         * @param old_val previous absolute value position of the slider
         * @param val the absolute value position of the slider
         * @param old_val_pct previous percentage value position of the slider
         * @param val_pct the percentage value position of the slider
         * @param pos object position relative to the slider's bar
         * @param e NEWT original event or {@code null} if sourced from non-mouse, e.g. key-event
         */
        void dragged(RangeSlider w, float old_val, float val, float old_val_pct, float val_pct, Vec3f pos, MouseEvent e);
    }
    private static interface ChangedAction {
        public void run(ChangeListener l);
    }
    /**
     * {@link RangeSlider} slider value peek listener
     */
    public static interface PeekListener {
        /**
         * Slide position/value peeked by user (mouse over/hover)
         * @param w the {@link RangeSlider} widget owning the slider
         * @param val the absolute value peeked at the slider
         * @param val_pct the percentage value position peeked at the slider
         * @param pos object position relative to the slider's bar
         * @param e NEWT original event
         */
        void peeked(RangeSlider w, float val, float val_pct, Vec3f pos, MouseEvent e);
    }
    private static interface PeekAction {
        public void run(PeekListener l);
    }

    private static final boolean DEBUG = false;
    private static final float pageKnobScale = 0.6f;     // 0.6 * barWidth
    private static final float pageBarLineScale = 0.25f; // 1/4 * ( barWidth - pageKnobWidth )
    private static final float pageKnobSizePctMin = 5f/100f;
    private final boolean horizontal;
    /** Knob thickness orthogonal to sliding direction */
    private float knobThickn;
    /** Knob length in sliding direction */
    private float knobLength;
    private final Vec2f size;
    private final Group barAndKnob, marks;
    private final Rectangle bar;
    private final GraphShape knob;
    private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
    private ArrayList<PeekListener> peekListeners = new ArrayList<PeekListener>();
    private final Vec2f minMax = new Vec2f(0, 100);
    private final float knobScale;
    private float pageSize;
    private float val=0, val_pct=0;
    private boolean inverted=false;
    private float unitSize = 1;
    private final Vec4f activeColMod = new Vec4f(0.1f, 0.1f, 0.1f, 1f);

    /**
     * Constructs a {@link RangeSlider}, i.e. its shapes and controls.
     * <p>
     * This slider comprises a background bar and a positional round knob,
     * with {@link #getValue()} at center position.
     * </p>
     * <p>
     * The spatial {@code size} gets automatically updated at {@link #validate(GL2ES2)}
     * </p>
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param size spatial dimension of this slider box. A horizontal slider has width >= height.
     * @param knobScale multiple of slider-bar height for {@link #getKnobThickness()}
     * @param minMax minimum- and maximum-value of slider
     * @param unitSize size of one unit (element) in sliding direction
     * @param value current value of slider
     */
    public RangeSlider(final int renderModes, final Vec2f size, final float knobScale,
                       final Vec2f minMax, final float unitSize, final float value) {
        this(renderModes, size, knobScale, minMax, unitSize, Float.NaN, value);
    }
    /**
     * Constructs a {@link RangeSlider}, i.e. its shapes and controls.
     * <p>
     * This slider comprises a framing bar and a rectangular page-sized knob,
     * with {@link #getValue()} at page-start position.
     * </p>
     * <p>
     * The spatial {@code size} and {@code pageSize} gets automatically updated at {@link #validate(GL2ES2)}
     * </p>
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param size spatial dimension of this slider box. A horizontal slider has width >= height.
     * @param minMax minimum- and maximum-value of slider
     * @param unitSize size of one unit (element) in sliding direction
     * @param pageSize size of one virtual-page, triggers rendering mode from knob to rectangle
     * @param value current value of slider
     */
    public RangeSlider(final int renderModes, final Vec2f size,
                       final Vec2f minMax, final float unitSize, final float pageSize, final float value) {
        this(renderModes, size, 0, minMax, unitSize, pageSize, value);
    }
    private RangeSlider(final int renderModes_, final Vec2f size, final float knobScale,
                       final Vec2f minMax, final float unitSize, final float pageSz, final float value) {
        final int renderModes = renderModes_ & ~(Region.COLORCHANNEL_RENDERING_BIT);
        this.knobScale = knobScale;
        this.unitSize = unitSize;
        this.pageSize = pageSz;
        this.horizontal = size.x() >= size.y();
        barAndKnob = new Group();
        barAndKnob.setInteractive(false);
        marks = new Group();
        marks.setInteractive(false);

        this.size = new Vec2f(size);
        if( DEBUG ) { System.err.println("RangeSlider.ctor0 "+getDescription()); }
        setMinMaxImpl(minMax.x(), minMax.y()); // pre-set for setKnobSize()
        setKnobSize(pageSize, false, false);
        if( DEBUG ) { System.err.println("RangeSlider.ctor1 "+getDescription()); }
        if( Float.isFinite(pageSize) ) {
            final float barLineWidth;
            if( horizontal ) {
                barLineWidth = ( size.y() - knobThickn ) * pageBarLineScale;
                knob = new Rectangle(renderModes, knobLength, knobThickn, 0);
            } else {
                barLineWidth = ( size.x() - knobThickn ) * pageBarLineScale;
                knob = new Rectangle(renderModes, knobThickn, knobLength, 0);
            }
            bar = new Rectangle(renderModes, this.size.x(), this.size.y(), barLineWidth);
        } else {
            bar = new Rectangle(renderModes, this.size.x(), this.size.y(), 0);
            knob = new BaseButton(renderModes , knobThickn*1.01f, knobThickn);
            setBackgroundBarColor(0.60f, 0.60f, 0.60f, 0.5f);
        }
        if( DEBUG ) { System.err.println("RangeSlider.ctor3 "+getDescription()); }
        setColor(0.80f, 0.80f, 0.80f, 0.7f);

        setName("RangeSlider.container");
        bar.setToggleable(false).setInteractive(true).setDragAndResizable(false).setName("RangeSlider.bar");
        knob.setToggleable(false).setInteractive(true).setResizable(false).setName("RangeSlider.knob");
        barAndKnob.addShape( bar );
        barAndKnob.addShape( marks );
        barAndKnob.addShape( knob );
        addShape(barAndKnob);

        reconfig(minMax, true, value, false, 0);

        knob.onMove((final Shape s, final Vec3f origin, final Vec3f dest, final MouseEvent e) -> {
            final float old_val = val;
            final float old_val_pct = val_pct;
            if( Float.isFinite(pageSize) ) {
                final float dy = inverted ? +knobLength: 0; // offset to knob start
                setValue(dest.x(), dest.y(), dy);
            } else {
                setValue(dest.x(), dest.y(), knobLength/2f); // centered
            }
            dispatchToListener( (final ChangeListener l) -> {
                l.dragged(RangeSlider.this, old_val, val, old_val_pct, val_pct, dest, e);
            });
        });
        bar.onClicked((final Shape s, final Vec3f pos, final MouseEvent e) -> {
            final float old_val = val;
            final float old_val_pct = val_pct;
            setValue(pos.x(), pos.y(), 0);
            dispatchToListener( (final ChangeListener l) -> {
                l.dragged(RangeSlider.this, old_val, val, old_val_pct, val_pct, pos, e);
            });
        });
        bar.onHover((final Shape s, final Vec3f pos, final MouseEvent e) -> {
            final float pval_pct = getKnobValuePct( pos.x(), pos.y(), 0 );
            final float pval = valuePctToValue( pval_pct );
            dispatchToListener( (final PeekListener l) -> {
                l.peeked(this, pval, pval_pct, pos, e);
            });
        });
        bar.addActivationListener((final Shape s) -> {
           dispatchActivationEvent(s);
        });
        final Shape.MouseGestureListener mouseListener = new Shape.MouseGestureAdapter() {
            @Override
            public void mouseWheelMoved(final MouseEvent e) {
                final float old_val = val;
                final float old_val_pct = val_pct;
                float v = old_val;
                if( !e.isControlDown() ) {
                    if( e.getRotation()[1] < 0f ) {
                        if( inverted ) {
                            v+=unitSize;
                        } else {
                            v-=unitSize;
                        }
                    } else {
                        if( inverted ) {
                            v-=unitSize;
                        } else {
                            v+=unitSize;
                        }
                    }
                } else if( Float.isFinite(pageSize) ){
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
                dispatchToListener( (final ChangeListener l) -> {
                    l.dragged(RangeSlider.this, old_val, val, old_val_pct, val_pct, knob.getPosition().minus(bar.getPosition()), e);
                });
            }
        };
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
                            v-=unitSize;
                        } else {
                            v+=unitSize;
                        }
                    } else if( keySym == KeyEvent.VK_LEFT ) {
                        action = true;
                        if( inverted ) {
                            v+=unitSize;
                        } else {
                            v-=unitSize;
                        }
                    }
                } else {
                    if( keySym == KeyEvent.VK_DOWN ) {
                        action = true;
                        if( inverted ) {
                            v+=unitSize;
                        } else {
                            v-=unitSize;
                        }
                    } else if( keySym == KeyEvent.VK_UP ) {
                        action = true;
                        if( inverted ) {
                            v-=unitSize;
                        } else {
                            v+=unitSize;
                        }
                    }
                }
                if( !action && Float.isFinite(pageSize) ) {
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
                    dispatchToListener( (final ChangeListener l) -> {
                        l.dragged(RangeSlider.this, old_val, val, old_val_pct, val_pct, knob.getPosition().minus(bar.getPosition()), null);
                    });
                }
            }
        };
        bar.addKeyListener(keyListener);
        knob.addKeyListener(keyListener);
        bar.addMouseListener(mouseListener);
        knob.addMouseListener(mouseListener);

        final Shape.Listener onActivation = new Shape.Listener() {
            private final Vec4f origCol = new Vec4f();
            private boolean oriColSet = false;
            private final Vec4f tmp = new Vec4f();
            @Override
            public void run(final Shape s) {
                if( bar.isActive() || knob.isActive() ) {
                    if( !oriColSet ) {
                        origCol.set( knob.getColor() );
                        oriColSet = true;
                    }
                    knob.setColor( tmp.mul(origCol, activeColMod) );
                } else {
                    oriColSet = false;
                    knob.setColor( origCol );
                }
            }
        };
        bar.addActivationListener(onActivation);
        knob.addActivationListener(onActivation);
    }

    @Override
    public void receiveKeyEvents(final Shape source) {
        source.addKeyListener(new Shape.ForwardKeyListener(barAndKnob));
        source.addKeyListener(new Shape.ForwardKeyListener(knob));
    }
    @Override
    public void receiveMouseEvents(final Shape source) {
        source.addMouseListener(new Shape.ForwardMouseListener(barAndKnob) {
            @Override
            public void mouseClicked(final MouseEvent e) { /* nop */ }
        });
    }

    @Override
    protected void clearImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        super.clearImpl0(gl, renderer);
        changeListeners.clear();
        peekListeners.clear();
    }
    @Override
    protected void destroyImpl0(final GL2ES2 gl, final RegionRenderer renderer) {
        super.destroyImpl0(gl, renderer);
        changeListeners.clear();
        peekListeners.clear();
    }

    public final RangeSlider addChangeListener(final ChangeListener l) {
        if(l == null) {
            return this;
        }
        @SuppressWarnings("unchecked")
        final ArrayList<ChangeListener> clonedListeners = (ArrayList<ChangeListener>) changeListeners.clone();
        clonedListeners.add(l);
        changeListeners = clonedListeners;
        return this;
    }
    public final RangeSlider removeChangeListener(final ChangeListener l) {
        if (l == null) {
            return this;
        }
        @SuppressWarnings("unchecked")
        final ArrayList<ChangeListener> clonedListeners = (ArrayList<ChangeListener>) changeListeners.clone();
        clonedListeners.remove(l);
        changeListeners = clonedListeners;
        return this;
    }
    private final void dispatchToListener(final ChangedAction action) {
        final int sz = changeListeners.size();
        for(int i = 0; i < sz; i++ ) {
            action.run( changeListeners.get(i) );
        }
    }

    public final RangeSlider addPeekListener(final PeekListener l) {
        if(l == null) {
            return this;
        }
        @SuppressWarnings("unchecked")
        final ArrayList<PeekListener> clonedListeners = (ArrayList<PeekListener>) peekListeners.clone();
        clonedListeners.add(l);
        peekListeners = clonedListeners;
        return this;
    }
    public final RangeSlider removePeekListener(final PeekListener l) {
        if (l == null) {
            return this;
        }
        @SuppressWarnings("unchecked")
        final ArrayList<PeekListener> clonedListeners = (ArrayList<PeekListener>) peekListeners.clone();
        clonedListeners.remove(l);
        peekListeners = clonedListeners;
        return this;
    }
    private final void dispatchToListener(final PeekAction action) {
        final int sz = peekListeners.size();
        for(int i = 0; i < sz; i++ ) {
            action.run( peekListeners.get(i) );
        }
    }

    public Rectangle getBar() { return bar; }
    public GraphShape getKnob() { return knob; }
    public Group getMarks() { return marks; }
    public RangeSlider clearMarks(final GL2ES2 gl, final RegionRenderer renderer) { marks.clear(gl, renderer); return this; }
    public Shape addMark(final float value, final Vec4f color) {
        final float sizex, sizey, itemLen, itemHeight;
        if( horizontal ) {
            sizey = size.y();
            sizex = 2*sizey;
            itemLen = sizex;
            itemHeight = sizey;
        } else {
            sizex = size.x();
            sizey = 2*sizex;
            itemLen = sizey;
            itemHeight = sizex;
        }
        final GraphShape mark = new Rectangle(knob.getRenderModes(), sizex, sizey, 0);
        final Vec2f pos = getItemValuePos(new Vec2f(), value, itemLen, itemHeight);
        mark.setInteractive(true).setToggleable(false).setDraggable(false).setResizable(false);
        mark.setColor(color);
        mark.moveTo(pos.x(), pos.y(), 0);
        marks.addShape(mark);
        return mark;
    }

    /** Returns spatial dimension of this slider */
    public final Vec2f getSize() { return size; }
    /** Returns spatial knob thickness orthogonal to sliding direction */
    public final float getKnobThickness() { return knobThickn; }
    /** Returns spatial knob length in sliding direction */
    public final float getKnobLength() { return knobLength; }

    /** Returns slider value range, see {@link #setMinMax(Vec2f, float)} */
    public Vec2f getMinMax() { return minMax; }
    /** Returns {@link #getMinMax()} range. */
    public float getRange() { return minMax.y() - minMax.x(); }
    private static float getRange(final Vec2f minMax) { return minMax.y() - minMax.x(); }
    /** Returns current slider value */
    public float getValue() { return val; }
    /** Returns current slider {@link #getValue() value} in percentage of {@link #getRange()}, */
    public float getValuePct() { return val_pct; }

    /**
     * Sets the page-size if a rectangular knob is being used, i.e. {@link #RangeSlider(int, Vec2f, Vec2f, float, float, float)},
     * otherwise does nothing.
     * @param pageSz the page-size, which will be clipped to {@link #getMinMax()}.
     * @return this instance of chaining
     * @see #getPageSize()
     * @see #RangeSlider(int, Vec2f, Vec2f, float, float, float)
     */
    public RangeSlider setPageSize(final float pageSz) {
        return setKnobSize(pageSz, true, true);
    }
    private RangeSlider setKnobSize(final float pageSz, final boolean adjKnob, final boolean adjValue) {
        if( Float.isFinite(pageSize) && Float.isFinite(pageSz) ) {
            final float range = getRange(minMax);
            if( Float.isFinite(range) && !FloatUtil.isZero(range) ) {
                pageSize = Math.min(minMax.y(), Math.max(minMax.x(), pageSz));
            }
            final float pageSizePct = getPageSizePct(pageKnobSizePctMin);
            final float width, height;
            if( horizontal ) {
                width = pageSizePct * this.size.x();
                height = size.y() * pageKnobScale;
                knobLength = width;
                knobThickn = height;
                if( !paddingSet ) {
                    setPaddding(new Padding(size.y()/2f, 0, size.y()/2f, 0));
                    paddingSet = true;
                }
            } else {
                width = size.x() * pageKnobScale;
                height = pageSizePct * this.size.y();
                knobLength = height;
                knobThickn = width;
                if( !paddingSet ) {
                    setPaddding(new Padding(0, size.x()/2f, 0, size.x()/2f));
                    paddingSet = true;
                }
            }
            if( adjKnob ) {
                ((Rectangle)knob).setDimension(width, height, 0);
            }
            if( adjValue ) {
                setValue( val );
            }
        } else if( Float.isFinite(pageSize) ) {
            // nop w/ invalid pageSz but valid pageSize
        } else {
            if( horizontal ) {
                knobThickn = size.y()*knobScale;
                if( !paddingSet ) {
                    setPaddding(new Padding(knobThickn/2f, 0, knobThickn/2f, 0));
                    paddingSet = true;
                }
            } else {
                knobThickn = size.x()*knobScale;
                if( !paddingSet ) {
                    setPaddding(new Padding(0, knobThickn/2f, 0, knobThickn/2f));
                    paddingSet = true;
                }
            }
            knobLength = knobThickn;
        }
        return this;
    }
    private boolean paddingSet = false;

    private void setMinMaxImpl(final float min, final float max) {
        this.minMax.set(Float.isFinite(min) ? min : 0, Float.isFinite(max) ? max : 0);
    }
    private RangeSlider reconfig(final Vec2f minMax,
                                 final boolean modValue, final float value,
                                 final boolean modKnobSz, final float pageSz)
    {
        if( null != minMax ) {
            setMinMaxImpl(minMax.x(), minMax.y());
        }
        if( modKnobSz ) {
            setKnobSize(pageSz, true, !modValue);
        }
        if( modValue ) {
            setValue( value );
        }
        if( DEBUG ) { System.err.println("RangeSlider.cfg "+getDescription()); }
        return this;
    }

    /**
     * Returns the page-size if a rectangular knob is being used, i.e. {@link #RangeSlider(int, Vec2f, Vec2f, float, float, float)},
     * otherwise returns {@link Float#NaN}.
     * @see #setPageSize(float)
     * @see #RangeSlider(int, Vec2f, Vec2f, float, float, float)
     */
    public float getPageSize() { return pageSize; }

    /**
     * Returns the page-size percentage if a rectangular knob is being used, i.e. {@link #RangeSlider(int, Vec2f, Vec2f, float, float, float)},
     * otherwise returns {@link Float#NaN}.
     * @param minPct minimum percentage to be returned, should be >= 0
     * @see #setPageSize(float)
     * @see #RangeSlider(int, Vec2f, Vec2f, float, float, float)
     */
    public float getPageSizePct(final float minPct) {
        if( Float.isFinite(pageSize) ) {
            final float range = getRange(minMax);
            return Float.isFinite(range) && !FloatUtil.isZero(range) ? Math.max(minPct, pageSize / range) : minPct;
        } else {
            return Float.NaN;
        }
    }

    /** Sets the size of one unit (element) in sliding direction */
    public RangeSlider setUnitSize(final float v) { unitSize = v; return this; }
    /** Returns the size of one unit (element) in sliding direction */
    public float getUnitSize() { return unitSize; }

    /**
     * Sets whether this slider uses an inverted value range,
     * e.g. top 0% and bottom 100% for an vertical inverted slider
     * instead of bottom 0% and top 100% for a vertical non-inverted slider.
     */
    public RangeSlider setInverted(final boolean v) { inverted = v; return setValue(val); }
    /** See {@link #setInverted(boolean)}. */
    public boolean isInverted() { return inverted; }

    /**
     * Sets slider value range and current value, also updates related pageSize parameter if used.
     * @param minMax minimum- and maximum-value of slider
     * @param value new value of slider, clipped against {@link #getMinMax()}
     * @return this instance of chaining
     */
    public RangeSlider setMinMax(final Vec2f minMax, final float value) {
        return reconfig(minMax, true, value, true, pageSize);
    }

    /**
     * Sets slider value range, also updates related pageSize parameter if used.
     * @param minMax minimum- and maximum-value of slider
     * @return this instance of chaining
     */
    public RangeSlider setMinMax(final Vec2f minMax) {
        return reconfig(minMax, false, 0, true, pageSize);
    }

    /**
     * Calls {@link #setMinMax(Vec2f, float)} and {@link #setPageSize(float)}.
     * @param minMax minimum- and maximum-value of slider
     * @param value new value of slider, clipped against {@code minMax}
     * @param pageSz the page-size, which will be clipped to {@code minMax}
     * @return this instance of chaining
     */
    public RangeSlider setMinMaxPgSz(final Vec2f minMax, final float value, final float pageSz) {
        return reconfig(minMax, true, value, true, pageSz);
    }

    /**
     * Calls {@link #setMinMax(Vec2f, float)} and {@link #setPageSize(float)}.
     * @param minMax minimum- and maximum-value of slider
     * @param pageSz the page-size, which will be clipped to {@code minMax}
     * @return this instance of chaining
     */
    public RangeSlider setMinMaxPgSz(final Vec2f minMax, final float pageSz) {
        return reconfig(minMax, false, 0, true, pageSz);
    }

    private RangeSlider setValue(final float pos_x, final float pos_y, final float adjustment) {
        return setValue( valuePctToValue( getKnobValuePct(pos_x, pos_y, adjustment) ) );
    }

    // private float getKnobValuePct(final float pos_x, final float pos_y, final float adjustment) {
    /**
     * Sets slider value
     * @param v new value of slider, clipped against {@link #getMinMax()}
     * @return this instance of chaining
     */
    public RangeSlider setValue(final float v) {
        final float v1 = Float.isFinite(v) ? v : 0f;
        final float pgsz = Float.isFinite(pageSize) ? pageSize : 0f;
        final float range = getRange();
        val = Math.max(minMax.x(), Math.min(minMax.y() - pgsz, v1));
        if( Float.isFinite(range) && !FloatUtil.isZero(range) ) {
            val_pct = ( val - minMax.x() ) / range;
        } else {
            val_pct = 0f;
        }
        setKnob();
        return this;
    }

    /**
     * Returns generic item position reflects value on its center (round-knob) or page-size start and ranges from zero to max.
     * @param posRes {@link Vec2f} result storage
     * @param value value within {@link #getMinMax()}
     * @param itemLen item length in sliding direction
     * @param itemHeight item height orthogonal to sliding direction
     */
    private Vec2f getItemValuePos(final Vec2f posRes, final float value, final float itemLen, final float itemHeight) {
        return getItemPctPos(posRes, ( value - minMax.x() ) / getRange(), itemLen, itemHeight);
    }
    /**
     * Returns generic item position reflects value on its center (round-knob) or page-size start and ranges from zero to max.
     * @param posRes {@link Vec2f} result storage
     * @param val_pct value percentage within [0..1]
     * @param itemLen item length in sliding direction
     * @param itemThickn item thickness orthogonal to sliding direction
     */
    private Vec2f getItemPctPos(final Vec2f posRes, final float val_pct, final float itemLen, final float itemThickn) {
        final float v = inverted ? 1f - val_pct : val_pct;
        final float itemAdjust;
        if( Float.isFinite(pageSize) ) {
            if( inverted ) {
                itemAdjust = itemLen; // top-edge
            } else {
                itemAdjust = 0; // bottom-edge
            }
        } else {
            itemAdjust = itemLen * 0.5f; // centered
        }
        if( horizontal ) {
            posRes.setX( Math.max(0, Math.min(size.x() - itemLen, v*size.x() - itemAdjust)) );
            posRes.setY( -( itemThickn - size.y() ) * 0.5f );
        } else {
            posRes.setX( -( itemThickn - size.x() ) * 0.5f );
            posRes.setY( Math.max(0, Math.min(size.y() - itemLen, v*size.y() - itemAdjust)) );
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
    private float valuePctToValue(final float v) {
        final float range = getRange();
        if( Float.isFinite(v) && Float.isFinite(range) && !FloatUtil.isZero(range) ) {
            final float pgsz_pct = Float.isFinite(pageSize) ? pageSize / range : 0f;
            final float pct = Math.max(0f, Math.min(1f - pgsz_pct, v));
            return minMax.x() + ( pct * range );
        } else {
            return 0f;
        }
    }

    private void setKnob() {
        final Vec2f pos = getItemPctPos(new Vec2f(), val_pct, knobLength, knobThickn);
        knob.moveTo(pos.x(), pos.y(), Button.DEFAULT_LABEL_ZOFFSET);
    }

    /**
     * Sets the slider knob color.
     * <p>
     * If this slider comprises a rectangular page-sized knob,
     * its rectangular frame also shares the same color with alpha 1.0f.
     * </p>
     * <p>
     * Base color w/o color channel, will be modulated w/ pressed- and toggle color
     * </p>
     * <p>
     * Default RGBA value is 0.80f, 0.80f, 0.80f, 0.7f
     * </p>
     */
    @Override
    public final Shape setColor(final float r, final float g, final float b, final float a) {
        super.setColor(r, g, b, a);
        knob.setColor(r, g, b, a);
        if( Float.isFinite(pageSize) ) {
            bar.setColor(r, g, b, 1.0f);
        }
        return this;
    }

    /**
     * Sets the slider knob color.
     * <p>
     * If this slider comprises a rectangular page-sized knob,
     * its rectangular frame also shares the same color with alpha 1.0f.
     * </p>
     * <p>
     * Base color w/o color channel, will be modulated w/ pressed- and toggle color
     * </p>
     * <p>
     * Default RGBA value is 0.80f, 0.80f, 0.80f, 0.7f
     * </p>
     */
    @Override
    public Shape setColor(final Vec4f c) {
        this.rgbaColor.set(c);
        knob.setColor(c);
        if( Float.isFinite(pageSize) ) {
            bar.setColor(c.x(), c.y(), c.z(), 1.0f);
        }
        return this;
    }

    /**
     * Sets the knob active modulation color
     * <p>
     * Default RGBA value is 0.1f, 0.1f, 0.1f, 1f
     * </p>
     */
    public Shape setActiveKnobColorMod(final Vec4f c) {
        if( !Float.isFinite(pageSize) ) {
            activeColMod.set(c);
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
        if( !Float.isFinite(pageSize) ) {
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
        if( !Float.isFinite(pageSize) ) {
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

    /** Return string description of current slider setting. */
    public String getDescription() {
        final String pre = "value "+val+" "+(100f*val_pct)+"%, range "+minMax;
        final String post = ", ssize "+size+", knob[l "+knobLength+", t "+knobThickn+"]";
        if( Float.isFinite(pageSize) ) {
            final float pageSizePct = getPageSizePct(pageKnobSizePctMin);
            final String detail = ", pageSize "+pageSize+" "+(pageSizePct*100f)+"% -> "+knobLength;
            if( horizontal ) {
                return "H "+pre+detail+"/"+size.x()+post;
            } else {
                return "V "+pre+detail+"/"+size.y()+post;
            }
        } else {
            if( horizontal ) {
                return "H "+pre+post;
            } else {
                return "V "+pre+post;
            }
        }
    }
    @Override
    public String getSubString() {
        return super.getSubString()+", "+getDescription()+" @ "+val+", "+(100f*val_pct)+"%";
    }
    @Override
    protected void validateImpl(final GL2ES2 gl, final GLProfile glp) {
        if( isShapeDirty() ) {
            super.validateImpl(gl, glp);
            setKnobSize(pageSize, true, true);
            if( DEBUG ) { System.err.println("RangeSlider.val "+getDescription()); }
        }
    }
}
