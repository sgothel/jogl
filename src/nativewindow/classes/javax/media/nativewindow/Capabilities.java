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

  // Support for transparent windows containing OpenGL content
  private boolean backgroundOpaque = true;
  private int     transparentValueRed = -1;
  private int     transparentValueGreen = -1;
  private int     transparentValueBlue = -1;
  private int     transparentValueAlpha = -1;

  // Switch for on- or offscreen
  private boolean onscreen  = true;

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

  public boolean equals(Object obj) {
    if(!(obj instanceof Capabilities)) {
        return false;
    }
    Capabilities other = (Capabilities)obj;
    boolean res = other.getRedBits()==redBits &&
                  other.getGreenBits()==greenBits &&
                  other.getBlueBits()==blueBits &&
                  other.getAlphaBits()==alphaBits &&
                  other.isBackgroundOpaque()==backgroundOpaque &&
                  other.isOnscreen()==onscreen;
    if(!backgroundOpaque) {
     res = res && other.getTransparentRedValue()==transparentValueRed &&
                  other.getTransparentGreenValue()==transparentValueGreen &&
                  other.getTransparentBlueValue()==transparentValueBlue &&
                  other.getTransparentAlphaValue()==transparentValueAlpha;
    }

    return res;
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

  /** For on-screen OpenGL contexts on some platforms, sets whether
      the background of the context should be considered opaque. On
      supported platforms, setting this to false, in conjunction with
      the transparency values, may allow
      hardware-accelerated OpenGL content inside of windows of
      arbitrary shape. To achieve this effect it is necessary to use
      an OpenGL clear color with an alpha less than 1.0. The default
      value for this flag is <code>true</code>; setting it to false
      may incur a certain performance penalty, so it is not
      recommended to arbitrarily set it to false.<br>
      If not set already, the transparency values for red, green, blue and alpha
      are set to their default value, which is half of the value range
      of the framebuffer's corresponding component,
      ie <code> redValue = ( 1 << ( redBits - 1 ) ) -1 </code>.
    */
  public void setBackgroundOpaque(boolean opaque) {
    backgroundOpaque = opaque;
    if(!opaque) {
        if(transparentValueRed<0)
            transparentValueRed = ( 1 << ( getRedBits() - 1 ) )  - 1 ;
        if(transparentValueGreen<0)
            transparentValueGreen = ( 1 << ( getGreenBits() - 1 ) )  - 1 ;
        if(transparentValueBlue<0)
            transparentValueBlue = ( 1 << ( getBlueBits() - 1 ) )  - 1 ;
        if(transparentValueAlpha<0)
            transparentValueAlpha = ( 1 << ( getAlphaBits() - 1 ) )  - 1 ;
    }
  }

  /** Indicates whether the background of this OpenGL context should
      be considered opaque. Defaults to true.

      @see #setBackgroundOpaque
  */
  public boolean isBackgroundOpaque() {
    return backgroundOpaque;
  }

  /** Sets whether the drawable surface supports onscreen.
      Defaults to true.
  */
  public void setOnscreen(boolean onscreen) {
    this.onscreen=onscreen;
  }

  /** Indicates whether the drawable surface is onscreen.
      Defaults to true.
  */
  public boolean isOnscreen() {
    return onscreen;
  }

  /** Gets the transparent red value for the frame buffer configuration.
    * This value is undefined if {@link #isBackgroundOpaque()} equals true.
    * @see #setTransparentRedValue
    */
  public int getTransparentRedValue() { return transparentValueRed; }

  /** Gets the transparent green value for the frame buffer configuration.
    * This value is undefined if {@link #isBackgroundOpaque()} equals true.
    * @see #setTransparentGreenValue
    */
  public int getTransparentGreenValue() { return transparentValueGreen; }

  /** Gets the transparent blue value for the frame buffer configuration.
    * This value is undefined if {@link #isBackgroundOpaque()} equals true.
    * @see #setTransparentBlueValue
    */
  public int getTransparentBlueValue() { return transparentValueBlue; }

  /** Gets the transparent alpha value for the frame buffer configuration.
    * This value is undefined if {@link #isBackgroundOpaque()} equals true.
    * @see #setTransparentAlphaValue
    */
  public int getTransparentAlphaValue() { return transparentValueAlpha; }

  /** Sets the transparent red value for the frame buffer configuration,
      ranging from 0 to the maximum frame buffer value for red.
      This value is ignored if {@link #isBackgroundOpaque()} equals true.<br>
      It defaults to half of the frambuffer value for red. <br>
      A value of -1 is interpreted as any value. */
  public void setTransparentRedValue(int transValueRed) { transparentValueRed=transValueRed; }

  /** Sets the transparent green value for the frame buffer configuration,
      ranging from 0 to the maximum frame buffer value for green.
      This value is ignored if {@link #isBackgroundOpaque()} equals true.<br>
      It defaults to half of the frambuffer value for green.<br>
      A value of -1 is interpreted as any value. */
  public void setTransparentGreenValue(int transValueGreen) { transparentValueGreen=transValueGreen; }

  /** Sets the transparent blue value for the frame buffer configuration,
      ranging from 0 to the maximum frame buffer value for blue.
      This value is ignored if {@link #isBackgroundOpaque()} equals true.<br>
      It defaults to half of the frambuffer value for blue.<br>
      A value of -1 is interpreted as any value. */
  public void setTransparentBlueValue(int transValueBlue) { transparentValueBlue=transValueBlue; }

  /** Sets the transparent alpha value for the frame buffer configuration,
      ranging from 0 to the maximum frame buffer value for alpha.
      This value is ignored if {@link #isBackgroundOpaque()} equals true.<br>
      It defaults to half of the frambuffer value for alpha.<br>
      A value of -1 is interpreted as any value. */
  public void setTransparentAlphaValue(int transValueAlpha) { transparentValueAlpha=transValueAlpha; }


  /** Returns a textual representation of this Capabilities
      object. */ 
  public String toString() {
    StringBuffer msg = new StringBuffer();
    msg.append("Capabilities[");
	msg.append("Onscreen: "+ onscreen +
        ", Red: " + redBits +
	    ", Green: " + greenBits +
	    ", Blue: " + blueBits +
	    ", Alpha: " + alphaBits +
        ", Opaque: " + backgroundOpaque);
    if(!backgroundOpaque) {
        msg.append(", Transparent RGBA: [0x"+ Integer.toHexString(transparentValueRed)+
                   " 0x"+ Integer.toHexString(transparentValueGreen)+
                   " 0x"+ Integer.toHexString(transparentValueBlue)+
                   " 0x"+ Integer.toHexString(transparentValueAlpha)+"] ");
    }
	msg.append("]");
    return msg.toString();
  }
}
