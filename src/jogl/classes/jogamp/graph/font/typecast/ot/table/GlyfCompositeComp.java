/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Batik" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/

package jogamp.graph.font.typecast.ot.table;

import java.io.DataInput;
import java.io.IOException;

/**
 * @version $Id: GlyfCompositeComp.java,v 1.3 2010-08-10 11:41:55 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class GlyfCompositeComp {

    public static final short ARG_1_AND_2_ARE_WORDS = 0x0001;
    public static final short ARGS_ARE_XY_VALUES = 0x0002;
    public static final short ROUND_XY_TO_GRID = 0x0004;
    public static final short WE_HAVE_A_SCALE = 0x0008;
    public static final short MORE_COMPONENTS = 0x0020;
    public static final short WE_HAVE_AN_X_AND_Y_SCALE = 0x0040;
    public static final short WE_HAVE_A_TWO_BY_TWO = 0x0080;
    public static final short WE_HAVE_INSTRUCTIONS = 0x0100;
    public static final short USE_MY_METRICS = 0x0200;

    private int _firstIndex;
    private int _firstContour;
    private short _argument1;
    private short _argument2;
    private int _flags;
    private int _glyphIndex;
    private double _xscale = 1.0;
    private double _yscale = 1.0;
    private double _scale01 = 0.0;
    private double _scale10 = 0.0;
    private int _xtranslate = 0;
    private int _ytranslate = 0;
    private int _point1 = 0;
    private int _point2 = 0;

    protected GlyfCompositeComp(int firstIndex, int firstContour, DataInput di)
    throws IOException {
        _firstIndex = firstIndex;
        _firstContour = firstContour;
        _flags = di.readUnsignedShort();
        _glyphIndex = di.readUnsignedShort();

        // Get the arguments as just their raw values
        if ((_flags & ARG_1_AND_2_ARE_WORDS) != 0) {
            _argument1 = di.readShort();
            _argument2 = di.readShort();
        } else {
            _argument1 = (short) di.readByte();
            _argument2 = (short) di.readByte();
        }

        // Assign the arguments according to the flags
        if ((_flags & ARGS_ARE_XY_VALUES) != 0) {
            _xtranslate = _argument1;
            _ytranslate = _argument2;
        } else {
            _point1 = _argument1;
            _point2 = _argument2;
        }

        // Get the scale values (if any)
        if ((_flags & WE_HAVE_A_SCALE) != 0) {
            int i = di.readShort();
            _xscale = _yscale = (double) i / (double) 0x4000;
        } else if ((_flags & WE_HAVE_AN_X_AND_Y_SCALE) != 0) {
            short i = di.readShort();
            _xscale = (double) i / (double) 0x4000;
            i = di.readShort();
            _yscale = (double) i / (double) 0x4000;
        } else if ((_flags & WE_HAVE_A_TWO_BY_TWO) != 0) {
            int i = di.readShort();
            _xscale = (double) i / (double) 0x4000;
            i = di.readShort();
            _scale01 = (double) i / (double) 0x4000;
            i = di.readShort();
            _scale10 = (double) i / (double) 0x4000;
            i = di.readShort();
            _yscale = (double) i / (double) 0x4000;
        }
    }

    public int getFirstIndex() {
        return _firstIndex;
    }

    public int getFirstContour() {
        return _firstContour;
    }

    public short getArgument1() {
        return _argument1;
    }

    public short getArgument2() {
        return _argument2;
    }

    public int getFlags() {
        return _flags;
    }

    public int getGlyphIndex() {
        return _glyphIndex;
    }

    public double getScale01() {
        return _scale01;
    }

    public double getScale10() {
        return _scale10;
    }

    public double getXScale() {
        return _xscale;
    }

    public double getYScale() {
        return _yscale;
    }

    public int getXTranslate() {
        return _xtranslate;
    }

    public int getYTranslate() {
        return _ytranslate;
    }

    /**
     * Transforms an x-coordinate of a point for this component.
     * @param x The x-coordinate of the point to transform
     * @param y The y-coordinate of the point to transform
     * @return The transformed x-coordinate
     */
    public int scaleX(int x, int y) {
        return (int)((double) x * _xscale + (double) y * _scale10);
    }

    /**
     * Transforms a y-coordinate of a point for this component.
     * @param x The x-coordinate of the point to transform
     * @param y The y-coordinate of the point to transform
     * @return The transformed y-coordinate
     */
    public int scaleY(int x, int y) {
        return (int)((double) x * _scale01 + (double) y * _yscale);
    }
}
