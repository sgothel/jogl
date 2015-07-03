/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util;

/**
 * Integer time frame in milliseconds, maybe specialized for texture/video, audio, .. animated content.
 * <p>
 * Type and value range has been chosen to suit embedded CPUs
 * and characteristics of audio / video streaming and animations.
 * Milliseconds of type integer with a maximum value of {@link Integer#MAX_VALUE}
 * will allow tracking time up 2,147,483.647 seconds or
 * 24 days 20 hours 31 minutes and 23 seconds.
 * </p>
 * <p>
 * Milliseconds granularity is also more than enough to deal with A-V synchronization,
 * where the threshold usually lies within 22ms.
 * </p>
 * <p>
 * Milliseconds granularity for displaying video frames might seem inaccurate
 * for each single frame, i.e. 60Hz != 16ms, however, accumulated values diminish
 * this error and vertical sync is achieved by build-in V-Sync of the video drivers.
 * </p>
 */
public class TimeFrameI {
    /** Constant marking an invalid PTS, i.e. Integer.MIN_VALUE == 0x80000000 == {@value}. Sync w/ native code. */
    public static final int INVALID_PTS = 0x80000000;

    /** Constant marking the end of the stream PTS, i.e. Integer.MIN_VALUE - 1 == 0x7FFFFFFF == {@value}. Sync w/ native code. */
    public static final int END_OF_STREAM_PTS = 0x7FFFFFFF;

    protected int pts;
    protected int duration;

    public TimeFrameI() {
        pts = INVALID_PTS;
        duration = 0;
    }
    public TimeFrameI(final int pts, final int duration) {
        this.pts = pts;
        this.duration = duration;
    }

    /** Get this frame's presentation timestamp (PTS) in milliseconds. */
    public final int getPTS() { return pts; }
    /** Set this frame's presentation timestamp (PTS) in milliseconds. */
    public final void setPTS(final int pts) { this.pts = pts; }
    /** Get this frame's duration in milliseconds. */
    public final int getDuration() { return duration; }
    /** Set this frame's duration in milliseconds. */
    public final void setDuration(final int duration) { this.duration = duration; }

    @Override
    public String toString() {
        return "TimeFrame[pts " + pts + " ms, l " + duration + " ms]";
    }
}
