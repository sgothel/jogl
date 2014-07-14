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

import com.jogamp.opengl.math.Quaternion;

/**
 * Position and orientation of one eye.
 */
public final class EyePose {
    /** Eye number, <code>0</code> for the left eye and <code>1</code> for the right eye. */
    public final int number;

    /** float[3] eye position vector. */
    public final float[] position;

    /** Eye orientation */
    public final Quaternion orientation;

    public EyePose(final int number) {
        this.number = number;
        this.position = new float[3];
        this.orientation = new Quaternion();
    }
    public EyePose(final int number, final float[] position, final Quaternion orientation) {
        this(number);
        set(position, orientation);
    }

    /** Set position and orientation of this instance. */
    public final void set(final float[] position, final Quaternion orientation) {
        System.arraycopy(position, 0, this.position, 0, 3);
        this.orientation.set(orientation);
    }
    /** Set position and orientation of this instance. */
    public final void setPosition(final float posX, final float posY, final float posZ) {
        position[0] = posX;
        position[1] = posY;
        position[2] = posZ;
    }
    public final String toString() {
        return "EyePose[num "+number+", pos["+position[0]+", "+position[1]+", "+position[2]+"], "+orientation+"]";
    }
}