/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package jogamp.opengl.util.stereo;

import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.RectangleImmutable;

import com.jogamp.opengl.math.FovHVHalves;
import com.jogamp.opengl.math.VectorUtil;

/**
 * 2D scale and offset NDC class,
 * providing conversion from {@link FovHVHalves} in tangent to NDC space.
 * <p>
 * See <a href="https://www.opengl.org/wiki/Compute_eye_space_from_window_space">OpenGL.org: Compute eye space from window space</a>
 * </p>
 */
public final class ScaleAndOffset2D {
    /** Scale for x- and y-component. */
    final float[] scale;
    /** Offset for x- and y-component. */
    final float[] offset;

    private static final float[] vec2Half = new float[] { 0.5f, 0.5f };

    public String toString() {
        return "[offset "+offset[0]+" / "+offset[1]+", scale "+scale[0]+" x "+scale[1]+"]";
    }

    public ScaleAndOffset2D(final float[] scale, final float[] offset) {
        this.scale = scale;
        this.offset = offset;
    }

    /**
     * Create the <i>Normalized Device Coordinate Space</i> (NDC) [-1,+1] instance
     * from the given <code>fovHVHalves</code>.
     */
    public ScaleAndOffset2D(final FovHVHalves fovHVHalves) {
        final FovHVHalves tanFovHVHalves = fovHVHalves.toTangents();
        final float projXScale = 2.0f / ( tanFovHVHalves.left+ tanFovHVHalves.right);
        final float projYScale = 2.0f / ( tanFovHVHalves.top + tanFovHVHalves.bottom );
        final float projXOffset = ( tanFovHVHalves.left - tanFovHVHalves.right ) * projXScale * 0.5f;
        final float projYOffset = ( tanFovHVHalves.top - tanFovHVHalves.bottom ) * projYScale * 0.5f;

        this.scale = new float[] { projXScale, projYScale };
        this.offset = new float[] { projXOffset, projYOffset };
    }

    /**
     * Create the <i>Normalized Device Coordinate Space</i> (NDC) [-1,+1] instance
     * from the given <code>fovHVHalves</code>, for the subsection of the <code>render-viewport</code> within the <code>rendertarget-size</code>.
     */
    public ScaleAndOffset2D(final FovHVHalves fovHVHalves, final DimensionImmutable rendertargetSize, final RectangleImmutable renderViewport) {
        final ScaleAndOffset2D eyeToSourceNDC = new ScaleAndOffset2D(fovHVHalves);
        final float[] vec2Tmp1 = new float[2];
        final float[] vec2Tmp2 = new float[2];
        final float[] scale  = VectorUtil.scaleVec2(vec2Tmp1, eyeToSourceNDC.scale, 0.5f);
        final float[] offset = VectorUtil.addVec2(vec2Tmp2, VectorUtil.scaleVec2(vec2Tmp2, eyeToSourceNDC.offset, 0.5f), vec2Half);

        final float[] scale2 = new float[] { (float)renderViewport.getWidth() / (float)rendertargetSize.getWidth(),
                                             (float)renderViewport.getHeight() / (float)rendertargetSize.getHeight() };

        final float[] offset2 = new float[] { (float)renderViewport.getX() / (float)rendertargetSize.getWidth(),
                                              (float)renderViewport.getY() / (float)rendertargetSize.getHeight() };

        VectorUtil.scaleVec2(scale, scale, scale2);
        VectorUtil.addVec2(offset, VectorUtil.scaleVec2(offset, offset, scale2), offset2);

        this.scale = scale;
        this.offset = offset;
    }

    /**
     * Return the <i>tangent FOV space</i> of this <i>eye to source NDC</i> instance.
     */
    public final float[] convertToTanFovSpace(final float[] rendertargetNDC) {
        final float[] vec2Tmp1 = new float[2];
        return VectorUtil.divVec2(vec2Tmp1, VectorUtil.subVec2(vec2Tmp1, rendertargetNDC, this.offset), this.scale);
    }

}