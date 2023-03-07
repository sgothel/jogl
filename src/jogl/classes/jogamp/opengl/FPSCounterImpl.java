/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package jogamp.opengl;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import com.jogamp.common.os.Clock;
import com.jogamp.opengl.FPSCounter;

/**
 * Default implementation of FPSCounter to be used for FPSCounter implementing renderer.
 */
public class FPSCounterImpl implements FPSCounter {
    private PrintStream fpsOutputStream ;

    // counter in [ns]
    private long fpsStartTimeNS, fpsLastUpdateTimeNS;

    // counter in [ms]
    private long fpsLastPeriodMS, fpsTotalDurationMS;

    // counter in events
    private int fpsUpdateFramesInterval;
    private int  fpsTotalFrames;

    // counter in fps
    private float fpsLast, fpsTotal;

    /** Creates a disabled instance */
    public FPSCounterImpl() {
        setUpdateFPSFrames(0, null);
    }

    /**
     * Increases total frame count and updates values if feature is enabled and
     * update interval is reached.<br>
     *
     * Shall be called by actual FPSCounter implementing renderer, after display a new frame.
     */
    public final synchronized void tickFPS() {
        fpsTotalFrames++;
        if(fpsUpdateFramesInterval>0 && fpsTotalFrames%fpsUpdateFramesInterval == 0) {
            final long now = Clock.currentNanos();
            fpsLastPeriodMS = TimeUnit.NANOSECONDS.toMillis(now - fpsLastUpdateTimeNS);
            fpsLastPeriodMS = Math.max(fpsLastPeriodMS, 1); // div 0
            fpsLast = ( fpsUpdateFramesInterval * 1000f ) / ( fpsLastPeriodMS ) ;

            fpsTotalDurationMS = TimeUnit.NANOSECONDS.toMillis(now - fpsStartTimeNS);
            fpsTotalDurationMS = Math.max(fpsTotalDurationMS, 1); // div 0
            fpsTotal= ( fpsTotalFrames * 1000f ) / ( fpsTotalDurationMS ) ;

            if(null != fpsOutputStream) {
                fpsOutputStream.println(toString());
            }

            fpsLastUpdateTimeNS = now;
        }
    }

    public StringBuilder toString(StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        String fpsLastS = String.valueOf(fpsLast);
        fpsLastS = fpsLastS.substring(0, fpsLastS.indexOf('.') + 2);
        String fpsTotalS = String.valueOf(fpsTotal);
        fpsTotalS = fpsTotalS.substring(0, fpsTotalS.indexOf('.') + 2);
        sb.append(fpsTotalDurationMS/1000 +" s: "+ fpsUpdateFramesInterval+" f / "+ fpsLastPeriodMS+" ms, " + fpsLastS+" fps, "+ fpsLastPeriodMS/fpsUpdateFramesInterval+" ms/f; "+
                  "total: "+ fpsTotalFrames+" f, "+ fpsTotalS+ " fps, "+ fpsTotalDurationMS/fpsTotalFrames+" ms/f");
        return sb;
    }

    @Override
    public String toString() {
        return toString(null).toString();
    }

    @Override
    public final synchronized void setUpdateFPSFrames(final int frames, final PrintStream out) {
        fpsUpdateFramesInterval = frames;
        fpsOutputStream = out;
        resetFPSCounter();
    }

    @Override
    public final synchronized void resetFPSCounter() {
        fpsStartTimeNS = Clock.currentNanos();
        fpsLastUpdateTimeNS = fpsStartTimeNS;
        fpsLastPeriodMS = 0;
        fpsTotalFrames = 0;
        fpsLast = 0f; fpsTotal = 0f;
        fpsLastPeriodMS = 0; fpsTotalDurationMS=0;
    }

    @Override
    public final synchronized int getUpdateFPSFrames() {
        return fpsUpdateFramesInterval;
    }

    @Override
    public final synchronized long getFPSStartTime()   {
        return TimeUnit.NANOSECONDS.toMillis(fpsStartTimeNS);
    }

    @Override
    public final synchronized long getLastFPSUpdateTime() {
        return TimeUnit.NANOSECONDS.toMillis(fpsLastUpdateTimeNS);
    }

    @Override
    public final synchronized long getLastFPSPeriod() {
        return fpsLastPeriodMS;
    }

    @Override
    public final synchronized float getLastFPS() {
        return fpsLast;
    }

    @Override
    public final synchronized int getTotalFPSFrames() {
        return fpsTotalFrames;
    }

    @Override
    public final synchronized long getTotalFPSDuration() {
        return fpsTotalDurationMS;
    }

    @Override
    public final synchronized float getTotalFPS() {
        return fpsTotal;
    }
}
