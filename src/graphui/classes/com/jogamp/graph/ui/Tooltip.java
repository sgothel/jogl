/**
 * Copyright 2023-2024 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.ui;

import com.jogamp.common.os.Clock;
import com.jogamp.math.util.PMVMatrix4f;

/** A HUD tooltip for {@link Shape}, see {@link Shape#setToolTip(CharSequence, com.jogamp.graph.font.Font, float, Scene)}. */
public abstract class Tooltip {

    /** Shape belonging to this tooltip's tool. */
    protected final Shape tool;
    protected final long delayMS;
    protected final Scene scene;
    /** Delay t1, time to show tooltip, i.e. t0 + delayMS */
    protected volatile long delayT1;

    protected Tooltip(final Shape tool, final long delayMS, final Scene scene) {
        this.tool = tool;
        this.delayMS = delayMS;
        this.scene = scene;
        this.delayT1 = 0;
    }

    /** Stops the timer. */
    public void stop() {
        this.delayT1 = 0;
    }

    /** Starts the timer. */
    public void start() {
        this.delayT1 = Clock.currentMillis() + delayMS;
    }

    /**
     * Send tick to this tooltip
     * @return true if timer has been reached to {@link #createTip(PMVMatrix4f)}, otherwise false
     */
    public boolean tick() {
        if( 0 == delayT1 ) {
            return false;
        }
        if( Clock.currentMillis() < delayT1 ) {
            return false;
        }
        this.delayT1 = 0;
        return true;
    }

    /**
     * Create a new HUD tip shape
     * @param pmv {@link PMVMatrix4f}, which shall be properly initialized, e.g. via {@link Scene#setupMatrix(PMVMatrix4f)}
     * @return newly created HUD tip shape
     */
    public abstract GraphShape createTip(final PMVMatrix4f pmv);

}