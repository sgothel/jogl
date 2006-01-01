/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.gluegen.cgram.types;

public class MachineDescription {
  private int charSizeInBytes;
  private int shortSizeInBytes;
  private int intSizeInBytes;
  private int longSizeInBytes;
  private int int64SizeInBytes;
  private int floatSizeInBytes;
  private int doubleSizeInBytes;
  private int pointerSizeInBytes;
  private int structAlignmentInBytes;

  public MachineDescription(int charSizeInBytes,
                            int shortSizeInBytes,
                            int intSizeInBytes,
                            int longSizeInBytes,
                            int int64SizeInBytes,
                            int floatSizeInBytes,
                            int doubleSizeInBytes,
                            int pointerSizeInBytes,
                            int structAlignmentInBytes) {
    this.charSizeInBytes    = charSizeInBytes;
    this.shortSizeInBytes   = shortSizeInBytes;
    this.intSizeInBytes     = intSizeInBytes;
    this.longSizeInBytes    = longSizeInBytes;
    this.int64SizeInBytes   = int64SizeInBytes;
    this.floatSizeInBytes   = floatSizeInBytes;
    this.doubleSizeInBytes  = doubleSizeInBytes;
    this.pointerSizeInBytes = pointerSizeInBytes;
    this.structAlignmentInBytes = structAlignmentInBytes;
  }

  public int charSizeInBytes()    { return charSizeInBytes;   }
  public int shortSizeInBytes()   { return shortSizeInBytes;  }
  public int intSizeInBytes()     { return intSizeInBytes;    }
  public int longSizeInBytes()    { return longSizeInBytes;   }
  public int int64SizeInBytes()   { return int64SizeInBytes;  }
  public int floatSizeInBytes()   { return floatSizeInBytes;  }
  public int doubleSizeInBytes()  { return doubleSizeInBytes; }
  public int pointerSizeInBytes() { return pointerSizeInBytes; }
  public int structAlignmentInBytes() { return structAlignmentInBytes; }
}
