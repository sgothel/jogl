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
package com.jogamp.opengl.util.stereo;

import com.jogamp.opengl.math.FovHVHalves;

/**
 * Constant single eye parameter of the viewer, relative to its {@link ViewerPose}.
 */
public final class EyeParameter {
    /** Eye number, <code>0</code> for the left eye and <code>1</code> for the right eye. */
    public final int number;

    /** float[3] eye position vector used to define eye height in meter relative to <i>actor</i>. */
    public final float[] positionOffset;

    /** Field of view in both directions, may not be centered, either {@link FovHVHalves#inTangents} or radians. */
    public final FovHVHalves fovhv;

    /** IPD related horizontal distance from nose to pupil in meter. */
    public final float distNoseToPupilX;

    /** Vertical distance from middle-line to pupil in meter. */
    public final float distMiddleToPupilY;

    /** Z-axis eye relief in meter. */
    public final float eyeReliefZ;

    public EyeParameter(final int number, final float[] positionOffset, final FovHVHalves fovhv,
                        final float distNoseToPupil, final float verticalDelta, final float eyeRelief) {
        this.number = number;
        this.positionOffset = new float[3];
        System.arraycopy(positionOffset, 0, this.positionOffset, 0, 3);
        this.fovhv = fovhv;
        this.distNoseToPupilX = distNoseToPupil;
        this.distMiddleToPupilY = verticalDelta;
        this.eyeReliefZ = eyeRelief;
    }
    public final String toString() {
        return "EyeParam[num "+number+", posOff["+positionOffset[0]+", "+positionOffset[1]+", "+positionOffset[2]+"], "+fovhv+
                      ", distPupil[noseX "+distNoseToPupilX+", middleY "+distMiddleToPupilY+", reliefZ "+eyeReliefZ+"]]";
    }
}