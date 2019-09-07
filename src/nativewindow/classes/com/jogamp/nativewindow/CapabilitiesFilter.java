/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.VisualIDHolder.VIDType;

/**
 * Diverse reusable {@link CapabilitiesImmutable} list filter
 */
public class CapabilitiesFilter {
    protected CapabilitiesFilter() {}

    /** Generic filter criteria */
    public static interface Test<C extends CapabilitiesImmutable> {
        public boolean match(final C cap);
    }

    public static class TestLessColorCompBits<C extends CapabilitiesImmutable> implements Test<C> {
        final int minColorCompBits;
        public TestLessColorCompBits(final int minColorCompBits) {
            this.minColorCompBits = minColorCompBits;
        }
        public final boolean match(final C cap) {
            return cap.getRedBits() < minColorCompBits ||
                   cap.getGreenBits() < minColorCompBits ||
                   cap.getBlueBits() < minColorCompBits ||
                   cap.getAlphaBits() < minColorCompBits;
        }
    }
    public static class TestMoreColorCompBits<C extends CapabilitiesImmutable> implements Test<C> {
        final int maxColorCompBits;
        public TestMoreColorCompBits(final int maxColorCompBits) {
            this.maxColorCompBits = maxColorCompBits;
        }
        public final boolean match(final C cap) {
            return cap.getRedBits() > maxColorCompBits ||
                   cap.getGreenBits() > maxColorCompBits ||
                   cap.getBlueBits() > maxColorCompBits ||
                   cap.getAlphaBits() > maxColorCompBits;
        }
    }
    public static class TestUnmatchedNativeVisualID<C extends CapabilitiesImmutable> implements Test<C> {
        final int requiredNativeVisualID;
        public TestUnmatchedNativeVisualID(final int requiredNativeVisualID) {
            this.requiredNativeVisualID = requiredNativeVisualID;
        }
        public final boolean match(final C cap) {
            return cap.getVisualID(VIDType.NATIVE) != requiredNativeVisualID;
        }
    }

    /**
     * Removing all {@link CapabilitiesImmutable} derived elements matching the given {@code criteria} {@link Test} list.
     * @param availableCaps {@link CapabilitiesImmutable} derived list to be filtered
     * @param criteria {@link Test} list run on all non-removed {@link CapabilitiesImmutable} derived elements
     * @return the list of removed {@link CapabilitiesImmutable} derived elements, might be of size 0 if none were removed.
     */
    public static <C extends CapabilitiesImmutable> ArrayList<C> removeMatching(final ArrayList<C> availableCaps, final List<Test<C>> criteria) {
        final ArrayList<C> removedCaps = new ArrayList<C>();
        for(int i=0; i<availableCaps.size(); ) {
            final C cap = availableCaps.get(i);
            boolean removed = false;
            for(int j=0; !removed && j<criteria.size(); j++) {
                if( criteria.get(j).match(cap) ) {
                    removedCaps.add(availableCaps.remove(i));
                    removed = true;
                }
            }
            if( !removed ) {
                i++;
            }
        }
        return removedCaps;
    }

    /**
     * If {@code requiredNativeVisualID} is not {@link VisualIDHolder.VID_UNDEFINED} and hence specific,
     * this filter removes all non-matching nativeVisualID {@link VIDType.NATIVE}.
     * <p>
     * Otherwise, if {@code requiredNativeVisualID} equals {@link VisualIDHolder.VID_UNDEFINED}, none is removed.
     * </p>
     * @param availableCaps list of {@link CapabilitiesImmutable} derived elements to be filtered
     * @param requiredNativeVisualID if not {@link VisualIDHolder.VID_UNDEFINED}, remove all non-matching nativeVisualID's
     * @return the list of removed {@link CapabilitiesImmutable} derived elements, might be of size 0 if none were removed.
     */
    public static <C extends CapabilitiesImmutable> ArrayList<C> removeUnmatchingNativeVisualID(final ArrayList<C> availableCaps,
                                                                      final int requiredNativeVisualID) {
        if( VisualIDHolder.VID_UNDEFINED == requiredNativeVisualID) {
            return new ArrayList<C>();
        }
        final ArrayList<Test<C>> criteria = new ArrayList<Test<C>>();
        criteria.add(new CapabilitiesFilter.TestUnmatchedNativeVisualID<C>(requiredNativeVisualID));
        return removeMatching(availableCaps, criteria);
    }

    /**
     * Filter removing all {@link CapabilitiesImmutable} derived elements having color components > {@code maxColorCompBits} including alpha.
     * @param availableCaps list of {@link CapabilitiesImmutable} derived elements to be filtered
     * @param maxColorCompBits maximum tolerated color component bits
     * @return the list of removed {@link CapabilitiesImmutable} derived elements, might be of size 0 if none were removed.
     */
    public static <C extends CapabilitiesImmutable> ArrayList<C> removeMoreColorComps(final ArrayList<C> availableCaps,
                                                                      final int maxColorCompBits) {
        final ArrayList<Test<C>> criteria = new ArrayList<Test<C>>();
        criteria.add(new CapabilitiesFilter.TestMoreColorCompBits<C>(maxColorCompBits));
        return removeMatching(availableCaps, criteria);
    }

    /**
     * Filter removing all {@link CapabilitiesImmutable} derived elements having color components > {@code maxColorCompBits} including alpha.
     * <p>
     * If {@code requiredNativeVisualID} is not {@link VisualIDHolder.VID_UNDEFINED} and hence specific,
     * this filter also removes all non-matching nativeVisualID {@link VIDType.NATIVE}.
     * </p>
     * @param availableCaps list of {@link CapabilitiesImmutable} derived elements to be filtered
     * @param maxColorCompBits maximum tolerated color component bits
     * @param requiredNativeVisualID if not {@link VisualIDHolder.VID_UNDEFINED}, also remove all non-matching nativeVisualID's
     * @return the list of removed {@link CapabilitiesImmutable} derived elements, might be of size 0 if none were removed.
     * @see #removeUnmatchingNativeVisualID(ArrayList, int)
     * @see #removeMoreColorComps(ArrayList, int)
     */
    public static <C extends CapabilitiesImmutable> ArrayList<C> removeMoreColorCompsAndUnmatchingNativeVisualID(final ArrayList<C> availableCaps,
                                                                      final int maxColorCompBits, final int requiredNativeVisualID) {
        final ArrayList<Test<C>> criteria = new ArrayList<Test<C>>();
        criteria.add(new CapabilitiesFilter.TestMoreColorCompBits<C>(maxColorCompBits));
        if( VisualIDHolder.VID_UNDEFINED != requiredNativeVisualID) {
            criteria.add(new CapabilitiesFilter.TestUnmatchedNativeVisualID<C>(requiredNativeVisualID));
        }
        return removeMatching(availableCaps, criteria);
    }

}
