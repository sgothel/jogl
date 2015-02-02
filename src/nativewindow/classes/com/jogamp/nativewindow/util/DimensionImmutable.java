/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

/** Immutable Dimension Interface, consisting of it's read only components:<br>
 * <ul>
 *  <li><code>width</code></li>
 *  <li><code>height</code></li>
 * </ul>
 */
public interface DimensionImmutable extends WriteCloneable, Comparable<DimensionImmutable> {

    int getHeight();

    int getWidth();

    /**
     * <p>
     * Compares square of size.
     * </p>
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final DimensionImmutable d);

    /**
     * Checks whether two dimensions objects are equal. Two instances
     * of <code>DimensionReadOnly</code> are equal if two components
     * <code>height</code> and <code>width</code> are equal.
     * @return  <code>true</code> if the two dimensions are equal;
     *          otherwise <code>false</code>.
     */
    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

}
