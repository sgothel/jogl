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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

package net.java.games.gluegen;

import net.java.games.gluegen.cgram.types.*;

public class StructLayout {
  private int baseOffset;
  private int structAlignment;

  protected StructLayout(int baseOffset,
                         int structAlignment) {
    this.baseOffset = baseOffset;
    this.structAlignment = structAlignment;
  }

  public void layout(CompoundType t) {
    int n = t.getNumFields();
    int curOffset = baseOffset;
    int maxSize   = 0;
    for (int i = 0; i < n; i++) {
      Field f = t.getField(i);
      Type ft = f.getType();
      if (ft.isInt() || ft.isFloat() || ft.isDouble() || ft.isPointer()) {
        int sz = ft.getSize();
        if ((curOffset % sz) != 0) {
          curOffset += sz - (curOffset % sz);
        }
        f.setOffset(curOffset);
        if (t.isUnion()) {
          maxSize = Math.max(maxSize, sz);
        } else {
          curOffset += sz;
        }
      } else if (ft.isCompound()) {
        new StructLayout(0, structAlignment).layout(ft.asCompound());
        if ((curOffset % structAlignment) != 0) {
          curOffset += structAlignment - (curOffset % structAlignment);
        }
        f.setOffset(curOffset);
        if (t.isUnion()) {
          maxSize = Math.max(maxSize, ft.getSize());
        } else {
          curOffset += ft.getSize();
        }
      } else if (ft.isArray()) {
        ArrayType arrayType = ft.asArray();
        CompoundType compoundElementType = arrayType.getBaseElementType().asCompound();
        if (compoundElementType != null) {
          new StructLayout(0, structAlignment).layout(compoundElementType);
          arrayType.recomputeSize();
        }
        // Note: not sure how this rounding is done
        if ((curOffset % structAlignment) != 0) {
          curOffset += structAlignment - (curOffset % structAlignment);
        }
        f.setOffset(curOffset);
        curOffset += ft.getSize();
      } else {
        // FIXME
        String name = t.getName();
        if (name == null) {
          name = t.toString();
        }
        throw new RuntimeException("Complicated field types (" + ft +
                                   " " + f.getName() +
                                   " in type " + name +
                                   ") not implemented yet");
      }
    }
    // FIXME: I think the below is wrong; better check with some examples
    //    if ((curOffset % structAlignment) != 0) {
    //      curOffset += structAlignment - (curOffset % structAlignment);
    //    }
    if (t.isUnion()) {
      t.setSize(maxSize);
    } else {
      t.setSize(curOffset);
    }
  }

  

  public static StructLayout createForCurrentPlatform() {
    String os = System.getProperty("os.name").toLowerCase();
    String cpu = System.getProperty("os.arch").toLowerCase();
    if ((os.startsWith("windows") && cpu.equals("x86")) ||
        (os.startsWith("linux") && cpu.equals("i386")) ||
        (os.startsWith("sunos") && cpu.equals("sparc")) ||
        (os.startsWith("sunos") && cpu.equals("i386"))||
        (os.startsWith("mac os") && cpu.equals("ppc"))
	) {
      // FIXME: make struct alignment configurable? May need to change
      // packing rules on a per-type basis?
      return new StructLayout(0, 8);
    } else {
      // FIXME: add more ports
      throw new RuntimeException("Please port StructLayout to your OS (" + os + ") and CPU (" + cpu + ")");
    }
  }
}
