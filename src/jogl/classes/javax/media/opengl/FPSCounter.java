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
package javax.media.opengl;

import java.io.PrintStream;

/**
 * FPSCounter feature.<br>
 * An implementation initially has the FPSCounter feature disabled.<br>
 * Use {@link #setUpdateFPSFrames(int, PrintStream)} to enable and disable the FPSCounter feature.
 */
public interface FPSCounter {
    public static final int DEFAULT_FRAMES_PER_INTERVAL = 5*60;
    
    /**
     * @param frames Update interval in frames.<br> At every rendered <i>frames</i> interval the currentTime and fps values are updated. 
     *        If the <i>frames</i> interval is <= 0, no update will be issued, ie the FPSCounter feature is turned off. You may choose {@link #DEFAULT_FRAMES_PER_INTERVAL}.
     * @param out optional print stream where the fps values gets printed if not null at every <i>frames</i> interval 
     */
    void setUpdateFPSFrames(int frames, PrintStream out);
    
    /**
     * Reset all performance counter (startTime, currentTime, frame number)
     */
    void resetFPSCounter();
    
    /**
     * @return update interval in frames
     * 
     * @see #setUpdateFPSFrames(int, PrintStream)
     */
    int getUpdateFPSFrames();
    
    /**
     * Returns the time of the first display call in milliseconds after enabling this feature via {@link #setUpdateFPSFrames(int, PrintStream)}.<br> 
     * This value is reset via {@link #resetFPSCounter()}.
     *
     * @see #setUpdateFPSFrames(int, PrintStream)
     * @see #resetFPSCounter()
     */
    long getFPSStartTime();

    /**
     * Returns the time of the last update interval in milliseconds, if this feature is enabled via {@link #setUpdateFPSFrames(int, PrintStream)}.<br>
     * This value is reset via {@link #resetFPSCounter()}.
     *
     * @see #setUpdateFPSFrames(int, PrintStream)
     * @see #resetFPSCounter()
     */
    long getLastFPSUpdateTime();

    /**
     * @return Duration of the last update interval in milliseconds.
     *
     * @see #setUpdateFPSFrames(int, PrintStream)
     * @see #resetFPSCounter()
     */
    long getLastFPSPeriod();
    
    /**
     * @return Last update interval's frames per seconds, {@link #getUpdateFPSFrames()} / {@link #getLastFPSPeriod()}
     * 
     * @see #setUpdateFPSFrames(int, PrintStream)
     * @see #resetFPSCounter()
     */
    float getLastFPS(); 
    
    /**
     * @return Number of frame rendered since {@link #getFPSStartTime()} up to {@link #getLastFPSUpdateTime()}
     *  
     * @see #setUpdateFPSFrames(int, PrintStream)
     * @see #resetFPSCounter()
     */
    int getTotalFPSFrames();

    /**
     * @return Total duration in milliseconds, {@link #getLastFPSUpdateTime()} - {@link #getFPSStartTime()}
     *
     * @see #setUpdateFPSFrames(int, PrintStream)
     * @see #resetFPSCounter()
     */
    long getTotalFPSDuration();


    /**
     * @return Total frames per seconds, {@link #getTotalFPSFrames()} / {@link #getTotalFPSDuration()} 
     * 
     * @see #setUpdateFPSFrames(int, PrintStream)
     * @see #resetFPSCounter()
     */
    float getTotalFPS();       
}
