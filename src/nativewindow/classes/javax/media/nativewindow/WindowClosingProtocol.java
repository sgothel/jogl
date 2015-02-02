/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.nativewindow;

/**
 * Protocol for handling window closing events.
 * <p>
 * The implementation shall obey either the user value set by this interface,<br>
 * an underlying toolkit set user value or it's default, eg. {@link WindowClosingMode#DO_NOTHING_ON_CLOSE DO_NOTHING_ON_CLOSE} within an AWT environment.<br>
 * If none of the above determines the operation,
 * this protocol default behavior {@link WindowClosingMode#DISPOSE_ON_CLOSE DISPOSE_ON_CLOSE} shall be used.</p>
 */
public interface WindowClosingProtocol {

    /**
     * Window closing mode if triggered by toolkit close operation.
     */
    public enum WindowClosingMode {
        /**
         * Do nothing on native window close operation.<br>
         * This is the default behavior within an AWT environment.
         */
        DO_NOTHING_ON_CLOSE,

        /**
         * Dispose resources on native window close operation.<br>
         * This is the default behavior in case no underlying toolkit defines otherwise.
         */
        DISPOSE_ON_CLOSE;
    }


    /**
     * @return the current close operation value
     * @see WindowClosingMode#DISPOSE_ON_CLOSE
     * @see WindowClosingMode#DO_NOTHING_ON_CLOSE
     */
    WindowClosingMode getDefaultCloseOperation();

    /**
     * @param op the new close operation value
     * @return the previous close operation value
     * @see WindowClosingMode#DISPOSE_ON_CLOSE
     * @see WindowClosingMode#DO_NOTHING_ON_CLOSE
     */
    WindowClosingMode setDefaultCloseOperation(WindowClosingMode op);
}
