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

package javax.media.nativewindow;

/** Specifies a set of capabilities that a window's rendering context
    must support, such as color depth per channel. It currently
    contains the minimal number of routines which allow configuration
    on all supported window systems. */

public class Capabilities implements Cloneable {
  private int     redBits        = 8;
  private int     greenBits      = 8;
  private int     blueBits       = 8;
  private int     alphaBits      = 0;

  /** Creates a Capabilities object. All attributes are in a default
      state.
    */
  public Capabilities() {}

  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw new NativeWindowException(e);
    }
  }

  /** Returns the number of bits requested for the color buffer's red
      component. On some systems only the color depth, which is the
      sum of the red, green, and blue bits, is considered. */
  public int getRedBits() {
    return redBits;
  }

  /** Sets the number of bits requested for the color buffer's red
      component. On some systems only the color depth, which is the
      sum of the red, green, and blue bits, is considered. */
  public void setRedBits(int redBits) {
    this.redBits = redBits;
  }

  /** Returns the number of bits requested for the color buffer's
      green component. On some systems only the color depth, which is
      the sum of the red, green, and blue bits, is considered. */
  public int getGreenBits() {
    return greenBits;
  }

  /** Sets the number of bits requested for the color buffer's green
      component. On some systems only the color depth, which is the
      sum of the red, green, and blue bits, is considered. */
  public void setGreenBits(int greenBits) {
    this.greenBits = greenBits;
  }

  /** Returns the number of bits requested for the color buffer's blue
      component. On some systems only the color depth, which is the
      sum of the red, green, and blue bits, is considered. */
  public int getBlueBits() {
    return blueBits;
  }

  /** Sets the number of bits requested for the color buffer's blue
      component. On some systems only the color depth, which is the
      sum of the red, green, and blue bits, is considered. */
  public void setBlueBits(int blueBits) {
    this.blueBits = blueBits;
  }
  
  /** Returns the number of bits requested for the color buffer's
      alpha component. On some systems only the color depth, which is
      the sum of the red, green, and blue bits, is considered. */
  public int getAlphaBits() {
    return alphaBits;
  }

  /** Sets the number of bits requested for the color buffer's alpha
      component. On some systems only the color depth, which is the
      sum of the red, green, and blue bits, is considered. */
  public void setAlphaBits(int alphaBits) {
    this.alphaBits = alphaBits;
  }

  /** Returns a textual representation of this Capabilities
      object. */ 
  public String toString() {
    return ("Capabilities [" +
	    "Red: " + redBits +
	    ", Green: " + greenBits +
	    ", Blue: " + blueBits +
	    ", Alpha: " + alphaBits +
	    " ]");
  }
}
