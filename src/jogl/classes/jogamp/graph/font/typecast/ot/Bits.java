/*
 * Copyright (c) 2020 Business Operation Systems GmbH. All Rights Reserved.
 */
package jogamp.graph.font.typecast.ot;

/**
 * Utilities for bit manipulations.
 *
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 */
public class Bits {

    /** 
     * Checks whether the bit with the given number is set.
     */
    public static boolean bit(int bitSet, int n) {
        return (bitSet & mask(n)) > 0;
    }

    /**
     * Sets or clears the the bit with the given index in the given bit set
     * depending on the given boolean value.
     */
    public static short bit(short bitSet, int n, boolean value) {
        return (short)bit((int)bitSet, n, value);
    }
    
    /**
     * Sets or clears the the bit with the given index in the given bit set
     * depending on the given boolean value.
     */
    public static int bit(int bitSet, int n, boolean value) {
        if (value) {
            return (bitSet | mask(n));
        } else {
            return (bitSet & mask(n));
        }
    }
    
    private static int mask(int n) {
        return 0x01 << n;
    }

    /** 
     * Whether all bits in the given mask are set in the given bit set.
     */
    public static boolean isSet(int bitSet, int mask) {
        return (bitSet & mask) == mask;
    }

}
