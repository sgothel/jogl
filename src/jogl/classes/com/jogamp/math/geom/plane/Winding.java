package com.jogamp.math.geom.plane;

/**
 * Winding direction, either clockwise (CW) or counter-clockwise (CCW).
 */
public enum Winding {
    /** Clockwise (Cw) negative winding direction */
    CW(-1),
    /** Counter-Clockwise (CCW) positive winding direction */
    CCW(1);

    /** The winding direction sign, i.e. positive 1 for CCW and negative -1 for CW. */
    public final int dir;

    Winding(final int dir) {
        this.dir = dir;
    }
}
