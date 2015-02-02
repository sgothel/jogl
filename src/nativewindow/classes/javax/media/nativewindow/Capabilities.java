/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.nativewindow;

/** Specifies a set of capabilities that a window's rendering context
    must support, such as color depth per channel. It currently
    contains the minimal number of routines which allow configuration
    on all supported window systems. */
public class Capabilities implements CapabilitiesImmutable, Cloneable {
  protected final static String na_str = "----" ;

  private int     redBits        = 8;
  private int     greenBits      = 8;
  private int     blueBits       = 8;
  private int     alphaBits      = 0;

  // Support for transparent windows containing OpenGL content
  private boolean backgroundOpaque = true;
  private int     transparentValueRed = 0;
  private int     transparentValueGreen = 0;
  private int     transparentValueBlue = 0;
  private int     transparentValueAlpha = 0;

  // Switch for on- or offscreen
  private boolean onscreen  = true;

  // offscreen bitmap mode
  private boolean isBitmap  = false;

  /** Creates a Capabilities object. All attributes are in a default
      state.
    */
  public Capabilities() {}

  @Override
  public Object cloneMutable() {
    return clone();
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (final CloneNotSupportedException e) {
      throw new NativeWindowException(e);
    }
  }

  /**
   * Copies all {@link Capabilities} values
   * from <code>source</code> into this instance.
   * @return this instance
   */
  public Capabilities copyFrom(final CapabilitiesImmutable other) {
    redBits = other.getRedBits();
    greenBits = other.getGreenBits();
    blueBits = other.getBlueBits();
    alphaBits = other.getAlphaBits();
    backgroundOpaque = other.isBackgroundOpaque();
    onscreen = other.isOnscreen();
    isBitmap = other.isBitmap();
    transparentValueRed = other.getTransparentRedValue();
    transparentValueGreen = other.getTransparentGreenValue();
    transparentValueBlue = other.getTransparentBlueValue();
    transparentValueAlpha = other.getTransparentAlphaValue();
    return this;
  }

  @Override
  public int hashCode() {
    // 31 * x == (x << 5) - x
    int hash = 31 + this.redBits;
    hash = ((hash << 5) - hash) + ( this.onscreen ? 1 : 0 );
    hash = ((hash << 5) - hash) + ( this.isBitmap ? 1 : 0 );
    hash = ((hash << 5) - hash) + this.greenBits;
    hash = ((hash << 5) - hash) + this.blueBits;
    hash = ((hash << 5) - hash) + this.alphaBits;
    hash = ((hash << 5) - hash) + ( this.backgroundOpaque ? 1 : 0 );
    hash = ((hash << 5) - hash) + this.transparentValueRed;
    hash = ((hash << 5) - hash) + this.transparentValueGreen;
    hash = ((hash << 5) - hash) + this.transparentValueBlue;
    hash = ((hash << 5) - hash) + this.transparentValueAlpha;
    return hash;
  }

  @Override
  public boolean equals(final Object obj) {
    if(this == obj)  { return true; }
    if(!(obj instanceof CapabilitiesImmutable)) {
        return false;
    }
    final CapabilitiesImmutable other = (CapabilitiesImmutable)obj;
    boolean res = other.getRedBits()==redBits &&
                  other.getGreenBits()==greenBits &&
                  other.getBlueBits()==blueBits &&
                  other.getAlphaBits()==alphaBits &&
                  other.isBackgroundOpaque()==backgroundOpaque &&
                  other.isOnscreen()==onscreen &&
                  other.isBitmap()==isBitmap;
    if(res && !backgroundOpaque) {
     res = other.getTransparentRedValue()==transparentValueRed &&
           other.getTransparentGreenValue()==transparentValueGreen &&
           other.getTransparentBlueValue()==transparentValueBlue &&
           other.getTransparentAlphaValue()==transparentValueAlpha;
    }

    return res;
  }

  /**
   * Comparing RGBA values only
   **/
  @Override
  public int compareTo(final CapabilitiesImmutable caps) {
    /**
    if ( ! ( o instanceof CapabilitiesImmutable ) ) {
        Class<?> c = (null != o) ? o.getClass() : null ;
        throw new ClassCastException("Not a CapabilitiesImmutable object, but " + c);
    }
    final CapabilitiesImmutable caps = (CapabilitiesImmutable) o; */

    final int rgba = redBits * greenBits * blueBits * ( alphaBits + 1 );

    final int xrgba = caps.getRedBits() * caps.getGreenBits() * caps.getBlueBits() * ( caps.getAlphaBits() + 1 );

    if(rgba > xrgba) {
        return 1;
    } else if(rgba < xrgba) {
        return -1;
    }

    return 0; // they are equal: RGBA
  }

  @Override
  public int getVisualID(final VIDType type) throws NativeWindowException {
      switch(type) {
          case INTRINSIC:
          case NATIVE:
              return VisualIDHolder.VID_UNDEFINED;
          default:
              throw new NativeWindowException("Invalid type <"+type+">");
      }
  }

  @Override
  public final int getRedBits() {
    return redBits;
  }

  /** Sets the number of bits requested for the color buffer's red
      component. On some systems only the color depth, which is the
      sum of the red, green, and blue bits, is considered. */
  public void setRedBits(final int redBits) {
    this.redBits = redBits;
  }

  @Override
  public final int getGreenBits() {
    return greenBits;
  }

  /** Sets the number of bits requested for the color buffer's green
      component. On some systems only the color depth, which is the
      sum of the red, green, and blue bits, is considered. */
  public void setGreenBits(final int greenBits) {
    this.greenBits = greenBits;
  }

  @Override
  public final int getBlueBits() {
    return blueBits;
  }

  /** Sets the number of bits requested for the color buffer's blue
      component. On some systems only the color depth, which is the
      sum of the red, green, and blue bits, is considered. */
  public void setBlueBits(final int blueBits) {
    this.blueBits = blueBits;
  }

  @Override
  public final int getAlphaBits() {
    return alphaBits;
  }

  /**
   * Sets the number of bits requested for the color buffer's alpha
   * component. On some systems only the color depth, which is the
   * sum of the red, green, and blue bits, is considered.
   * <p>
   * <b>Note:</b> If alpha bits are <code>zero</code>, they are set to <code>one</code>
   * by {@link #setBackgroundOpaque(boolean)} and it's OpenGL specialization <code>GLCapabilities::setSampleBuffers(boolean)</code>.<br/>
   * Ensure to call this method after the above to ensure a <code>zero</code> value.</br>
   * The above automated settings takes into account, that the user calls this method to <i>request</i> alpha bits,
   * not to <i>reflect</i> a current state. Nevertheless if this is the case - call it at last.
   * </p>
   */
  public void setAlphaBits(final int alphaBits) {
    this.alphaBits = alphaBits;
  }

  /**
   * Sets whether the surface shall be opaque or translucent.
   * <p>
   * Platform implementations may need an alpha component in the surface (eg. Windows),
   * or expect pre-multiplied alpha values (eg. X11/XRender).<br>
   * To unify the experience, this method also invokes {@link #setAlphaBits(int) setAlphaBits(1)}
   * if {@link #getAlphaBits()} == 0.<br>
   * Please note that in case alpha is required on the platform the
   * clear color shall have an alpha lower than 1.0 to allow anything shining through.
   * </p>
   * <p>
   * Mind that translucency may cause a performance penalty
   * due to the composite work required by the window manager.
   * </p>
   */
  public void setBackgroundOpaque(final boolean opaque) {
      backgroundOpaque = opaque;
      if(!opaque && getAlphaBits()==0) {
          setAlphaBits(1);
      }
  }

  @Override
  public final boolean isBackgroundOpaque() {
    return backgroundOpaque;
  }

  /**
   * Sets whether the surface shall be on- or offscreen.
   * <p>
   * Defaults to true.
   * </p>
   * <p>
   * If requesting an offscreen surface without further selection of it's mode,
   * e.g. FBO, Pbuffer or {@link #setBitmap(boolean) bitmap},
   * the implementation will choose the best available offscreen mode.
   * </p>
   * @param onscreen
   */
  public void setOnscreen(final boolean onscreen) {
    this.onscreen=onscreen;
  }

  @Override
  public final boolean isOnscreen() {
    return onscreen;
  }

  /**
   * Requesting offscreen bitmap mode.
   * <p>
   * If enabled this method also invokes {@link #setOnscreen(int) setOnscreen(false)}.
   * </p>
   * <p>
   * Defaults to false.
   * </p>
   * <p>
   * Requesting offscreen bitmap mode disables the offscreen auto selection.
   * </p>
   */
  public void setBitmap(final boolean enable) {
    if(enable) {
      setOnscreen(false);
    }
    isBitmap = enable;
  }

  @Override
  public boolean isBitmap() {
    return isBitmap;
  }

  @Override
  public final int getTransparentRedValue() { return transparentValueRed; }

  @Override
  public final int getTransparentGreenValue() { return transparentValueGreen; }

  @Override
  public final int getTransparentBlueValue() { return transparentValueBlue; }

  @Override
  public final int getTransparentAlphaValue() { return transparentValueAlpha; }

  /** Sets the transparent red value for the frame buffer configuration,
      ranging from 0 to the maximum frame buffer value for red.
      This value is ignored if {@link #isBackgroundOpaque()} equals true.<br>
      It defaults to half of the frambuffer value for red. <br>
      A value of -1 is interpreted as any value. */
  public void setTransparentRedValue(final int transValueRed) { transparentValueRed=transValueRed; }

  /** Sets the transparent green value for the frame buffer configuration,
      ranging from 0 to the maximum frame buffer value for green.
      This value is ignored if {@link #isBackgroundOpaque()} equals true.<br>
      It defaults to half of the frambuffer value for green.<br>
      A value of -1 is interpreted as any value. */
  public void setTransparentGreenValue(final int transValueGreen) { transparentValueGreen=transValueGreen; }

  /** Sets the transparent blue value for the frame buffer configuration,
      ranging from 0 to the maximum frame buffer value for blue.
      This value is ignored if {@link #isBackgroundOpaque()} equals true.<br>
      It defaults to half of the frambuffer value for blue.<br>
      A value of -1 is interpreted as any value. */
  public void setTransparentBlueValue(final int transValueBlue) { transparentValueBlue=transValueBlue; }

  /** Sets the transparent alpha value for the frame buffer configuration,
      ranging from 0 to the maximum frame buffer value for alpha.
      This value is ignored if {@link #isBackgroundOpaque()} equals true.<br>
      It defaults to half of the frambuffer value for alpha.<br>
      A value of -1 is interpreted as any value. */
  public void setTransparentAlphaValue(final int transValueAlpha) { transparentValueAlpha=transValueAlpha; }

  @Override
  public StringBuilder toString(final StringBuilder sink) {
      return toString(sink, true);
  }

  /** Returns a textual representation of this Capabilities
      object. */
  @Override
  public String toString() {
    final StringBuilder msg = new StringBuilder();
    msg.append("Caps[");
    toString(msg);
    msg.append("]");
    return msg.toString();
  }

  /** Return a textual representation of this object's on/off screen state. Use the given StringBuilder [optional]. */
  protected StringBuilder onoffScreenToString(StringBuilder sink) {
    if(null == sink) {
        sink = new StringBuilder();
    }
    if(onscreen) {
        sink.append("on-scr");
    } else {
        sink.append("offscr[");
    }
    if(isBitmap) {
        sink.append("bitmap");
    } else if(onscreen) {
        sink.append(".");        // no additional off-screen modes besides on-screen
    } else {
        sink.append("auto-cfg"); // auto-config off-screen mode
    }
    sink.append("]");

    return sink;
  }

  /** Element separator */
  protected static final String ESEP = "/";
  /** Component separator */
  protected static final String CSEP = ", ";

  protected StringBuilder toString(StringBuilder sink, final boolean withOnOffScreen) {
    if(null == sink) {
        sink = new StringBuilder();
    }
    sink.append("rgba ").append(redBits).append(ESEP).append(greenBits).append(ESEP).append(blueBits).append(ESEP).append(alphaBits);
    if(backgroundOpaque) {
        sink.append(", opaque");
    } else {
        sink.append(", trans-rgba 0x").append(toHexString(transparentValueRed)).append(ESEP).append(toHexString(transparentValueGreen)).append(ESEP).append(toHexString(transparentValueBlue)).append(ESEP).append(toHexString(transparentValueAlpha));
    }
    if(withOnOffScreen) {
        sink.append(CSEP);
        onoffScreenToString(sink);
    }
    return sink;
  }

  protected final String toHexString(final int val) { return Integer.toHexString(val); }
}
