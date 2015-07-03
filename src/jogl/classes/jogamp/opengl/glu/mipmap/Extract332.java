/*
 * License Applicability. Except to the extent portions of this file are
 * made subject to an alternative license as permitted in the SGI Free
 * Software License B, Version 2.0 (the "License"), the contents of this
 * file are subject only to the provisions of the License. You may not use
 * this file except in compliance with the License. You may obtain a copy
 * of the License at Silicon Graphics, Inc., attn: Legal Services, 1600
 * Amphitheatre Parkway, Mountain View, CA 94043-1351, or at:
 *
 * http://oss.sgi.com/projects/FreeB
 *
 * Note that, as provided in the License, the Software is distributed on an
 * "AS IS" basis, with ALL EXPRESS AND IMPLIED WARRANTIES AND CONDITIONS
 * DISCLAIMED, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES AND
 * CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A
 * PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
 *
 * NOTE:  The Original Code (as defined below) has been licensed to Sun
 * Microsystems, Inc. ("Sun") under the SGI Free Software License B
 * (Version 1.1), shown above ("SGI License").   Pursuant to Section
 * 3.2(3) of the SGI License, Sun is distributing the Covered Code to
 * you under an alternative license ("Alternative License").  This
 * Alternative License includes all of the provisions of the SGI License
 * except that Section 2.2 and 11 are omitted.  Any differences between
 * the Alternative License and the SGI License are offered solely by Sun
 * and not by SGI.
 *
 * Original Code. The Original Code is: OpenGL Sample Implementation,
 * Version 1.2.1, released January 26, 2000, developed by Silicon Graphics,
 * Inc. The Original Code is Copyright (c) 1991-2000 Silicon Graphics, Inc.
 * Copyright in any portions created by third parties is as indicated
 * elsewhere herein. All Rights Reserved.
 *
 * Additional Notice Provisions: The application programming interfaces
 * established by SGI in conjunction with the Original Code are The
 * OpenGL(R) Graphics System: A Specification (Version 1.2.1), released
 * April 1, 1999; The OpenGL(R) Graphics System Utility Library (Version
 * 1.3), released November 4, 1998; and OpenGL(R) Graphics with the X
 * Window System(R) (Version 1.3), released October 19, 1998. This software
 * was created using the OpenGL(R) version 1.2.1 Sample Implementation
 * published by SGI, but has not been independently verified as being
 * compliant with the OpenGL(R) version 1.2.1 Specification.
 */

package jogamp.opengl.glu.mipmap;

import java.nio.ByteBuffer;

/**
 *
 * @author Administrator
 */
public class Extract332 implements Extract {

  /** Creates a new instance of Extract332 */
  public Extract332() {
  }

  @Override
  public void extract( final boolean isSwap, final ByteBuffer packedPixel, final float[] extractComponents ) {
    // 11100000 == 0xe0
    // 00011100 == 0x1c
    // 00000011 == 0x03
    final byte ubyte = packedPixel.get();
    extractComponents[0] = ((ubyte & 0xe0) >> 5) / 7.0f;
    extractComponents[1] = ((ubyte & 0x1c) >> 2) / 7.0f;
    extractComponents[2] = ((ubyte & 0x03)) / 3.0f;
  }

  @Override
  public void shove( final float[] shoveComponents, final int index, final ByteBuffer packedPixel ) {
    // 11100000 == 0xE0
    // 00011100 == 0x1C
    // 00000011 == 0x03

    assert( 0.0f <= shoveComponents[0] && shoveComponents[0] <= 1.0f );
    assert( 0.0f <= shoveComponents[1] && shoveComponents[1] <= 1.0f );
    assert( 0.0f <= shoveComponents[2] && shoveComponents[2] <= 1.0f );

    // due to limited precision, need to round before shoving
    byte b = (byte)( ( (int)( ( shoveComponents[0] * 7 ) + 0.5f ) << 5 ) & 0xE0 );
    b |= (byte)( ( (int)( ( shoveComponents[1] * 7 ) + 0.5f ) << 2 ) & 0x1C );
    b |= (byte)( ( (int)( ( shoveComponents[2] * 3 ) + 0.5f ) ) & 0x03 );
    packedPixel.put( index, b );
  }
}
