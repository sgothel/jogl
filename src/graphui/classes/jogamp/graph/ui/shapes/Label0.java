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
package jogamp.graph.ui.shapes;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.opengl.math.Vec2f;
import com.jogamp.opengl.math.Vec4f;
import com.jogamp.opengl.math.geom.AABBox;

public class Label0 {
    protected Font font;
    protected String text;
    protected final Vec4f rgbaColor;

    public Label0(final Font font, final String text, final Vec4f rgbaColor) {
        this.font = font;
        this.text = text;
        this.rgbaColor = rgbaColor;
    }

    public final String getText() { return text; }

    public final Vec4f getColor() { return rgbaColor; }

    public final void setColor(final float r, final float g, final float b, final float a) {
        this.rgbaColor.set(r, g, b, a);
    }

    public final void setColor(final Vec4f v) {
        this.rgbaColor.set(v);
    }

    public final void setText(final String text) {
        this.text = text;
    }

    public final Font getFont() { return font; }

    public final void setFont(final Font font) {
        this.font = font;
    }

    public final AABBox addShapeToRegion(final float scale, final Region region, final Vec2f txy,
                                         final AffineTransform tmp1, final AffineTransform tmp2, final AffineTransform tmp3)
    {
        tmp1.setToTranslation(txy.x(), txy.y());
        tmp1.scale(scale, scale, tmp2);
        return TextRegionUtil.addStringToRegion(region, font, tmp1, text, rgbaColor, tmp2, tmp3);
    }

    @Override
    public final String toString(){
        final int m = Math.min(text.length(), 8);
        return "Label0 ['" + text.substring(0, m) + "']";
    }
}
