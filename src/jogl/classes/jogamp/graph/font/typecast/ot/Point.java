/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package jogamp.graph.font.typecast.ot;

/**
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class Point {

    public int x;
    public int y;
    public boolean onCurve;
    public boolean endOfContour;
    // public boolean touched = false;

    public Point(int x, int y, boolean onCurve, boolean endOfContour) {
        this.x = x;
        this.y = y;
        this.onCurve = onCurve;
        this.endOfContour = endOfContour;
    }

    public String toString() {
        return "P["+x+"/"+y+", on "+onCurve+", end "+endOfContour+"]";
    }
}
