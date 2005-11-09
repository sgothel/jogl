/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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

/** Provides a level of indirection between the definition of a type's
    size and the absolute value of this size. Necessary when
    generating glue code for two different CPU architectures (e.g.,
    32-bit and 64-bit) from the same internal representation of the
    various types involved. */

public abstract class SizeThunk {
  // Private constructor because there are only a few of these
  private SizeThunk() {}

  public abstract long compute(MachineDescription machDesc);

  public static final SizeThunk CHAR = new SizeThunk() {
      public long compute(MachineDescription machDesc) {
        return machDesc.charSizeInBytes();
      }
    };

  public static final SizeThunk SHORT = new SizeThunk() {
      public long compute(MachineDescription machDesc) {
        return machDesc.shortSizeInBytes();
      }
    };

  public static final SizeThunk INT = new SizeThunk() {
      public long compute(MachineDescription machDesc) {
        return machDesc.intSizeInBytes();
      }
    };

  public static final SizeThunk LONG = new SizeThunk() {
      public long compute(MachineDescription machDesc) {
        return machDesc.longSizeInBytes();
      }
    };

  public static final SizeThunk INT64 = new SizeThunk() {
      public long compute(MachineDescription machDesc) {
        return machDesc.int64SizeInBytes();
      }
    };

  public static final SizeThunk FLOAT = new SizeThunk() {
      public long compute(MachineDescription machDesc) {
        return machDesc.floatSizeInBytes();
      }
    };

  public static final SizeThunk DOUBLE = new SizeThunk() {
      public long compute(MachineDescription machDesc) {
        return machDesc.doubleSizeInBytes();
      }
    };

  public static final SizeThunk POINTER = new SizeThunk() {
      public long compute(MachineDescription machDesc) {
        return machDesc.pointerSizeInBytes();
      }
    };

  // Factory methods for performing certain limited kinds of
  // arithmetic on these values
  public static SizeThunk add(final SizeThunk thunk1,
                              final SizeThunk thunk2) {
    return new SizeThunk() {
        public long compute(MachineDescription machDesc) {
          return thunk1.compute(machDesc) + thunk2.compute(machDesc);
        }
      };
  }

  public static SizeThunk sub(final SizeThunk thunk1,
                              final SizeThunk thunk2) {
    return new SizeThunk() {
        public long compute(MachineDescription machDesc) {
          return thunk1.compute(machDesc) - thunk2.compute(machDesc);
        }
      };
  }

  public static SizeThunk mul(final SizeThunk thunk1,
                              final SizeThunk thunk2) {
    return new SizeThunk() {
        public long compute(MachineDescription machDesc) {
          return thunk1.compute(machDesc) * thunk2.compute(machDesc);
        }
      };
  }

  public static SizeThunk mod(final SizeThunk thunk1,
                              final SizeThunk thunk2) {
    return new SizeThunk() {
        public long compute(MachineDescription machDesc) {
          return thunk1.compute(machDesc) % thunk2.compute(machDesc);
        }
      };
  }

  public static SizeThunk roundUp(final SizeThunk thunk1,
                                  final SizeThunk thunk2) {
    return new SizeThunk() {
        public long compute(MachineDescription machDesc) {
          long sz1 = thunk1.compute(machDesc);
          long sz2 = thunk2.compute(machDesc);
          long rem = (sz1 % sz2);
          if (rem == 0) {
            return sz1;
          }
          return sz1 + (sz2 - rem);
        }
      };
  }

  public static SizeThunk max(final SizeThunk thunk1,
                              final SizeThunk thunk2) {
    return new SizeThunk() {
        public long compute(MachineDescription machDesc) {
          return Math.max(thunk1.compute(machDesc), thunk2.compute(machDesc));
        }
      };
  }

  public static SizeThunk constant(final int constant) {
    return new SizeThunk() {
        public long compute(MachineDescription machDesc) {
          return constant;
        }
      };
  }
}
