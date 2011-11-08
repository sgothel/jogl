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

import javax.media.opengl.FPSCounter;

/**
 * Default implementation of FPSCounter to be used for FPSCounter implementing renderer.
 */
public class FPSCounterImpl implements FPSCounter {
    private int fpsUpdateFramesInterval;
    private PrintStream fpsOutputStream ;
    private long fpsStartTime, fpsLastUpdateTime, fpsLastPeriod, fpsTotalDuration;
    private int  fpsTotalFrames;
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
     *  
     */
    public final synchronized void tickFPS() {
        fpsTotalFrames++;
        if(fpsUpdateFramesInterval>0 && fpsTotalFrames%fpsUpdateFramesInterval == 0) {
            final long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
            fpsLastPeriod = now - fpsLastUpdateTime;
            fpsLastPeriod = Math.max(fpsLastPeriod, 1); // div 0 
            fpsLast = ( (float)fpsUpdateFramesInterval * 1000f ) / ( (float) fpsLastPeriod ) ; 
            
            fpsTotalDuration = now - fpsStartTime;
            fpsTotalDuration = Math.max(fpsTotalDuration, 1); // div 0
            fpsTotal= ( (float)fpsTotalFrames * 1000f ) / ( (float) fpsTotalDuration ) ;
            
            if(null != fpsOutputStream) {
                fpsOutputStream.println(toString());
            }
            
            fpsLastUpdateTime = now;
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
        sb.append(fpsTotalDuration/1000 +" s: "+ fpsUpdateFramesInterval+" f / "+ fpsLastPeriod+" ms, " + fpsLastS+" fps, "+ fpsLastPeriod/fpsUpdateFramesInterval+" ms/f; "+
                  "total: "+ fpsTotalFrames+" f, "+ fpsTotalS+ " fps, "+ fpsTotalDuration/fpsTotalFrames+" ms/f");
        return sb;
    }
    
    public String toString() {
        return toString(null).toString();
    }
    
    public final synchronized void setUpdateFPSFrames(int frames, PrintStream out) {
        fpsUpdateFramesInterval = frames;
        fpsOutputStream = out;
        resetFPSCounter();
    }
    
    public final synchronized void resetFPSCounter() {
        fpsStartTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()); // overwrite startTime to real init one
        fpsLastUpdateTime   = fpsStartTime;
        fpsLastPeriod = 0;
        fpsTotalFrames = 0;
        fpsLast = 0f; fpsTotal = 0f;
    }

    public final synchronized int getUpdateFPSFrames() {
        return fpsUpdateFramesInterval;
    }
    
    public final synchronized long getFPSStartTime()   { 
        return fpsStartTime; 
    }

    public final synchronized long getLastFPSUpdateTime() {
        return fpsLastUpdateTime;
    }

    public final synchronized long getLastFPSPeriod() {
        return fpsLastPeriod;
    }
    
    public final synchronized float getLastFPS() {
        return fpsLast;
    }
    
    public final synchronized int getTotalFPSFrames() { 
        return fpsTotalFrames; 
    }

    public final synchronized long getTotalFPSDuration() { 
        return fpsTotalDuration; 
    }
    
    public final synchronized float getTotalFPS() {
        return fpsTotal;
    }        
}
