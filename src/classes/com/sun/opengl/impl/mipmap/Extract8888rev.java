/*
 * License Applicability. Except to the extent portions of this file are
 * made subject to an alternative license as permitted in the SGI Free
 * Software License B, Version 1.1 (the "License"), the contents of this
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

package com.sun.opengl.impl.mipmap;

import java.nio.*;

/**
 *
 * @author Administrator
 */
public class Extract8888rev implements Extract {
  
  /** Creates a new instance of Extract8888rev */
  public Extract8888rev() {
  }
  
  public void extract( boolean isSwap, ByteBuffer packedPixel, float[] extractComponents ) {
    long uint = 0;
    
    if( isSwap ) {
      uint = 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( packedPixel.getInt() );
    } else {
      uint = 0x00000000FFFFFFFF & packedPixel.getInt();
    }
    
    // 11111000,00000000 == 0xF800
    // 00000111,11000000 == 0x07C0
    // 00000000,00111110 == 0x003E
    // 00000000,00000001 == 0x0001
    
    extractComponents[0] = (float)( ( uint & 0x000000FF )       ) / 255.0f;
    extractComponents[1] = (float)( ( uint & 0x0000FF00 ) >>  8 ) / 255.0f;
    extractComponents[2] = (float)( ( uint & 0x00FF0000 ) >> 16 ) / 255.0f;
    extractComponents[3] = (float)( ( uint & 0xFF000000 ) >> 24 ) / 255.0f;
  }
  
  public void shove( float[] shoveComponents, int index, ByteBuffer packedPixel ) {
    // 11110000,00000000 == 0xF000
    // 00001111,00000000 == 0x0F00
    // 00000000,11110000 == 0x00F0
    // 00000000,00001111 == 0x000F
    
    assert( 0.0f <= shoveComponents[0] && shoveComponents[0] <= 1.0f );
    assert( 0.0f <= shoveComponents[1] && shoveComponents[1] <= 1.0f );
    assert( 0.0f <= shoveComponents[2] && shoveComponents[2] <= 1.0f );
    assert( 0.0f <= shoveComponents[3] && shoveComponents[3] <= 1.0f );
    
    // due to limited precision, need to round before shoving
    long uint = (((int)((shoveComponents[0] * 255) + 0.5f)    ) & 0x000000FF );
    uint |= (((int)((shoveComponents[1] * 255) + 0.5f) <<    8) & 0x0000FF00 );
    uint |= (((int)((shoveComponents[2] * 255) + 0.5f) <<   16) & 0x00FF0000 );
    uint |= (((int)((shoveComponents[3] * 255) + 0.5f) <<   24) & 0xFF000000 );
    packedPixel.asIntBuffer().put( index, (int)uint );
  }
}
