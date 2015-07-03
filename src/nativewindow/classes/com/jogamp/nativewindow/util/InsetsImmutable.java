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

package com.jogamp.nativewindow.util;

import com.jogamp.common.type.WriteCloneable;

/**
 * Immutable insets representing rectangular window decoration insets on all four edges
 * in window units.
 */
public interface InsetsImmutable extends WriteCloneable {

    /** @return left inset width in window units. */
    int getLeftWidth();

    /** @return right inset width in window units. */
    int getRightWidth();

    /** @return total width in window units, ie. <code>left_width + right_width</code> */
    int getTotalWidth();

    /** @return top inset height in window units. */
    int getTopHeight();

    /** @return bottom inset height in window units. */
    int getBottomHeight();

    /** @return total height in window units, ie. <code>top_height + bottom_height</code> */
    int getTotalHeight();

    /**
     * Checks whether two rect objects are equal. Two instances
     * of <code>Insets</code> are equal if the four integer values
     * of the fields <code>left</code>, <code>right</code>,
     * <code>top</code>, and <code>bottom</code> are all equal.
     * @return      <code>true</code> if the two Insets are equal;
     * otherwise <code>false</code>.
     */
    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

}
