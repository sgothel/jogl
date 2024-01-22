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

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.widgets.RangeSlider.SliderAdapter;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.Cube;
import com.jogamp.math.geom.Frustum;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * Ranged {@link Group} {@link Widget}, displaying a clipped content {@link Group}
 * with optional horizontal and/or vertical {@link RangeSlider}.
 */
public class RangedGroup extends Widget {
    private final Group content;
    private final Group clippedContent;
    private final RangeSlider horizSlider, vertSlider;
    private final Vec2f contentPosZero = new Vec2f();

    /** {@link RangeSlider} configuration parameter for {@link RangedGroup}. */
    public static final class SliderParam {
        /** width and height of this slider box. A horizontal slider has width >= height. */
        public final Vec2f size;
        /** size of one unit (element) in sliding direction */
        public final float unitSize;
        /**
         * Toggle whether this slider uses an inverted value range,
         * e.g. top 0% and bottom 100% for an vertical inverted slider
         * instead of bottom 0% and top 100% for a vertical non-inverted slider.
         */
        public final boolean inverted;

        /**
         *
         * @param size width and height of this slider box. A horizontal slider has width >= height.
         * @param unitSize size of one unit (element) in sliding direction
         * @param inverted toggle to invert value range, see {@link #inverted}
         */
        public SliderParam(final Vec2f size, final float unitSize, final boolean inverted) {
            this.size = size;
            this.unitSize = unitSize;
            this.inverted = inverted;
        }
    }

    /**
     * Construct a {@link RangedGroup}
     * @param renderModes Graph's {@link Region} render modes, see {@link GLRegion#create(GLProfile, int, TextureSequence) create(..)}.
     * @param content the {@link Group} with content to view
     * @param contentSize the fixed size of the clipped content to view, i.e. page-size
     * @param horizSliderParam optional horizontal slider parameters, null for none
     * @param vertSliderParam optional vertical slider parameters, null for none
     */
    public RangedGroup(final int renderModes, final Group content, final Vec2f contentSize,
                      final SliderParam horizSliderParam, final SliderParam vertSliderParam)
    {
        super( new GridLayout(1 + (null != vertSliderParam ? 1 : 0), 0f, 0f, Alignment.None)); // vertical slider adds to the right column
        this.content = content;
        this.clippedContent = new Group( new GridLayout(1, 0f, 0f, Alignment.None));
        this.clippedContent.setFixedSize(contentSize);
        this.clippedContent.addShape(content);
        addShape(clippedContent);

        if( null != horizSliderParam ) {
            horizSlider = new RangeSlider(renderModes, horizSliderParam.size,
                                          new Vec2f(0, content.getBounds().getWidth()), horizSliderParam.unitSize, contentSize.x(), 0).setInverted(horizSliderParam.inverted);
            addShape(horizSlider);
            horizSlider.addSliderListener( new SliderAdapter() {
                @Override
                public void dragged(final RangeSlider w, final float old_val, final float val, final float old_val_pct, final float val_pct) {
                    final Vec3f oldPos = content.getPosition();
                    final float newXPos = w.getValue();
                    content.moveTo(contentPosZero.x()+newXPos, oldPos.y(), oldPos.z());
                }
            } );
        } else {
            horizSlider = null;
        }
        if( null != vertSliderParam ) {
            vertSlider = new RangeSlider(renderModes, vertSliderParam.size,
                                         new Vec2f(0, content.getBounds().getHeight()), vertSliderParam.unitSize, contentSize.y(), 0).setInverted(vertSliderParam.inverted);
            addShape(vertSlider);
            vertSlider.addSliderListener( new SliderAdapter() {
                @Override
                public void dragged(final RangeSlider w, final float old_val, final float val, final float old_val_pct, final float val_pct) {
                    final Vec3f oldPos = content.getPosition();
                    final float newYPos = w.getValue();
                    content.moveTo(oldPos.x(), contentPosZero.y()+newYPos, oldPos.z());
                }
            } );
        } else {
            vertSlider = null;
        }
        this.onInit( (final Shape shape) -> {
            content.move(contentPosZero.x(), contentPosZero.y(), 0);
            return true;
        });
    }

    public Group getContent() { return content; }
    public Vec2f getContentSize(final Vec2f out) { return clippedContent.getFixedSize(out); }
    public Group getClippedContent() { return clippedContent; }
    public RangeSlider getHorizSlider() { return horizSlider; }
    public RangeSlider getVertSlider() { return vertSlider; }

    @Override
    protected void validateImpl(final GL2ES2 gl, final GLProfile glp) {
        if( isShapeDirty() ) {
            super.validateImpl(gl, glp);

            final AABBox b = content.getBounds();
            final Vec3f contentSize = clippedContent.getFixedSize();
            contentPosZero.set(0, 0);
            if( null != horizSlider ) {
                horizSlider.setMinMax(new Vec2f(0, content.getBounds().getWidth()), 0);
                if( horizSlider.isInverted() ) {
                    contentPosZero.setX( contentSize.x() - b.getWidth() );
                }
            }
            if( null != vertSlider ) {
                vertSlider.setMinMax(new Vec2f(0, content.getBounds().getHeight()), 0);
                if( vertSlider.isInverted() ) {
                    contentPosZero.setY( contentSize.y() - b.getHeight() );
                }
            }
        }
    }
    @Override
    protected void drawImpl0(final GL2ES2 gl, final RegionRenderer renderer, final Vec4f rgba) {
        if( content.isVisible() ) {
            final PMVMatrix4f pmv = renderer.getMatrix();

            // Mv pre-multiplied Frustum, clippedContent is on same PMV
            final Frustum clipFrustum = tempC00.set( clippedContent.getBounds() ).transform( pmv.getMv() ).updateFrustumPlanes(tempF00);
            content.setClipMvFrustum(clipFrustum);
            super.drawImpl0(gl, renderer, rgba);
            content.setClipMvFrustum(null);
        }
    }
    private final Frustum tempF00 = new Frustum(); // OK, synchronized
    private final Cube tempC00 = new Cube(); // OK, synchronized
}
