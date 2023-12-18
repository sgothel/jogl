/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.ui.layout;

import java.util.List;

/**
 * Immutable layout alignment options, including {@link Bit#Fill}.
 */
public final class Alignment {
    /** No alignment constant. */
    public static final Alignment None = new Alignment();
    /** {@link Bit#CenterHoriz} and {@link Bit#CenterVert} alignment constant. */
    public static final Alignment Center = new Alignment(Alignment.Bit.CenterHoriz.value | Alignment.Bit.CenterVert.value );
    /** {@link Bit#Fill} alignment constant. */
    public static final Alignment Fill = new Alignment(Alignment.Bit.Fill.value);
    /** {@link Bit#Fill}, {@link Bit#CenterHoriz} and {@link Bit#CenterVert} alignment constant. */
    public static final Alignment FillCenter = new Alignment(Alignment.Bit.Fill.value | Alignment.Bit.CenterHoriz.value | Alignment.Bit.CenterVert.value);

    public enum Bit {
        /** Left alignment. */
        Left ( ( 1 << 0 ) ),

        /** Right alignment. */
        Right ( ( 1 << 1 ) ),

        /** Bottom alignment. */
        Bottom ( ( 1 << 2 ) ),

        /** Top alignment. */
        Top ( ( 1 << 3 ) ),

        /** Scale object to parent size, e.g. fill {@link GridLayout} or {@link BoxLayout} cell size. */
        Fill ( ( 1 << 4 ) ),

        /** Horizontal center alignment. */
        CenterHoriz ( ( 1 << 5 ) ),

        /** Vertical center alignment. */
        CenterVert ( ( 1 << 6 ) );

        Bit(final int v) { value = v; }
        public final int value;
    }
    public final int mask;

    public static int getBits(final List<Bit> v) {
        int res = 0;
        for(final Bit b : v) {
            res |= b.value;
        }
        return res;
    }
    public Alignment(final List<Bit> v) {
        mask = getBits(v);
    }
    public Alignment(final Bit v) {
        mask = v.value;
    }
    public Alignment(final int v) {
        mask = v;
    }
    private Alignment() {
        mask = 0;
    }

    public boolean isSet(final Bit bit) { return bit.value == ( mask & bit.value ); }
    public boolean isSet(final List<Bit> bits) { final int bits_i = getBits(bits); return bits_i == ( mask & bits_i ); }
    public boolean isSet(final int bits) { return bits == ( mask & bits ); }

    @Override
    public String toString() {
        int count = 0;
        final StringBuilder out = new StringBuilder();
        for (final Bit dt : Bit.values()) {
            if( isSet(dt) ) {
                if( 0 < count ) { out.append(", "); }
                out.append(dt.name()); count++;
            }
        }
        if( 0 == count ) {
            out.append("None");
        } else if( 1 < count ) {
            out.insert(0, "[");
            out.append("]");
        }
        return out.toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        return (other instanceof Alignment) &&
               this.mask == ((Alignment)other).mask;
    }
}
