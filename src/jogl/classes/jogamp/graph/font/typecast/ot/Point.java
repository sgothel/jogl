/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package jogamp.graph.font.typecast.ot;

/**
 * @version $Id: Point.java,v 1.1.1.1 2004-12-05 23:14:31 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class Point {

    public int x = 0;
    public int y = 0;
    public boolean onCurve = true;
    public boolean endOfContour = false;
    public boolean touched = false;

    public Point(int x, int y, boolean onCurve, boolean endOfContour) {
        this.x = x;
        this.y = y;
        this.onCurve = onCurve;
        this.endOfContour = endOfContour;
    }
}
