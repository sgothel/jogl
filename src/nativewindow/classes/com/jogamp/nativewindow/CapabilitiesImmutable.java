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

import com.jogamp.common.type.WriteCloneable;

/**
 * Specifies an immutable set of capabilities that a window's rendering context
 * must support, such as color depth per channel.
 *
 * @see com.jogamp.nativewindow.Capabilities
 */
public interface CapabilitiesImmutable extends VisualIDHolder, WriteCloneable, Comparable<CapabilitiesImmutable> {

    /**
     * Returns the number of bits for the color buffer's red
     * component. On some systems only the color depth, which is the sum of the
     * red, green, and blue bits, is considered.
     */
    int getRedBits();

    /**
     * Returns the number of bits for the color buffer's green
     * component. On some systems only the color depth, which is the sum of the
     * red, green, and blue bits, is considered.
     */
    int getGreenBits();

    /**
     * Returns the number of bits for the color buffer's blue
     * component. On some systems only the color depth, which is the sum of the
     * red, green, and blue bits, is considered.
     */
    int getBlueBits();

    /**
     * Returns the number of bits for the color buffer's alpha
     * component. On some systems only the color depth, which is the sum of the
     * red, green, and blue bits, is considered.
     */
    int getAlphaBits();

    /**
     * Returns whether an opaque or translucent surface is requested, supported or chosen.
     * <p>
     * Default is true, i.e. opaque.
     * </p>
     */
    boolean isBackgroundOpaque();

    /**
     * Returns whether an on- or offscreen surface is requested, available or chosen.
     * <p>
     * Default is true, i.e. onscreen.
     * </p>
     * <p>
     * Mind that an capabilities intance w/ <i>available</i> semantics
     * may show onscreen, but also the offscreen modes FBO, Pbuffer or {@link #setBitmap(boolean) bitmap}.
     * This is valid, since one native configuration maybe used for either functionality.
     * </p>
     */
    boolean isOnscreen();

    /**
     * Returns whether bitmap offscreen mode is requested, available or chosen.
     * <p>
     * Default is false.
     * </p>
     * <p>
     * For chosen capabilities, only the selected offscreen surface is set to <code>true</code>.
     * </p>
     */
    boolean isBitmap();

    /**
     * Gets the transparent red value for the frame buffer configuration. This
     * value is undefined if; equals true.
     */
    int getTransparentRedValue();

    /**
     * Gets the transparent green value for the frame buffer configuration. This
     * value is undefined if; equals true.
     */
    int getTransparentGreenValue();

    /**
     * Gets the transparent blue value for the frame buffer configuration. This
     * value is undefined if; equals true.
     */
    int getTransparentBlueValue();

    /**
     * Gets the transparent alpha value for the frame buffer configuration. This
     * value is undefined if; equals true.
     */
    int getTransparentAlphaValue();

    /** Equality over the immutable attributes of both objects */
    @Override
    boolean equals(Object obj);

    /** hash code over the immutable attributes of both objects */
    @Override
    int hashCode();

    /** Return a textual representation of this object. Use the given StringBuilder [optional]. */
    StringBuilder toString(StringBuilder sink);

    /** Returns a textual representation of this object. */
    @Override
    String toString();
}
