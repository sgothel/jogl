/*
 * Copyright (c) 2003-2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.jogamp.opengl;

import com.jogamp.nativewindow.Capabilities;
import com.jogamp.nativewindow.CapabilitiesImmutable;

/** Specifies a set of OpenGL capabilities.<br>
    At creation time of a {@link GLDrawable} using {@link GLDrawableFactory},
    an instance of this class is passed,
    describing the desired capabilities that a rendering context
    must support, such as the OpenGL profile, color depth and whether stereo is enabled.<br>

    The actual capabilites of created {@link GLDrawable}s are then reflected by their own
    GLCapabilites instance, which can be queried with {@link GLDrawable#getChosenGLCapabilities()}.
    <br>

    It currently contains the minimal number of routines which allow
    configuration on all supported window systems. */
public class GLCapabilities extends Capabilities implements Cloneable, GLCapabilitiesImmutable {
  private GLProfile glProfile    = null;
  private boolean isPBuffer      = false;
  private boolean isFBO          = false;
  private boolean doubleBuffered = true;
  private boolean stereo         = false;
  private boolean hardwareAccelerated = true;
  private int     depthBits      = 16;
  private int     stencilBits    = 0;
  private int     accumRedBits   = 0;
  private int     accumGreenBits = 0;
  private int     accumBlueBits  = 0;
  private int     accumAlphaBits = 0;
  // Shift bits from PIXELFORMATDESCRIPTOR not present because they
  // are unlikely to be supported on Windows anyway

  // Support for full-scene antialiasing (FSAA)
  private String  sampleExtension = DEFAULT_SAMPLE_EXTENSION;
  private boolean sampleBuffers = false;
  private int     numSamples    = 2;

  /** Creates a GLCapabilities object. All attributes are in a default state.
    * @param glp GLProfile, or null for the default GLProfile
    * @throws GLException if no profile is given and no default profile is available for the default device.
    */
  public GLCapabilities(final GLProfile glp) throws GLException {
      glProfile = (null!=glp)?glp:GLProfile.getDefault(GLProfile.getDefaultDevice());
  }

  @Override
  public Object cloneMutable() {
    return clone();
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (final RuntimeException e) {
      throw new GLException(e);
    }
  }

  /**
   * Copies all {@link GLCapabilitiesImmutable} values
   * from <code>source</code> into this instance.
   * @return this instance
   */
  public GLCapabilities copyFrom(final GLCapabilitiesImmutable source) {
    super.copyFrom(source);
    glProfile = source.getGLProfile();
    isPBuffer = source.isPBuffer();
    isFBO = source.isFBO();
    doubleBuffered = source.getDoubleBuffered();
    stereo = source.getStereo();
    hardwareAccelerated = source.getHardwareAccelerated();
    depthBits = source.getDepthBits();
    stencilBits = source.getStencilBits();
    accumRedBits = source.getAccumRedBits();
    accumGreenBits = source.getAccumGreenBits();
    accumBlueBits = source.getAccumBlueBits();
    accumAlphaBits = source.getAccumAlphaBits();
    sampleBuffers = source.getSampleBuffers();
    numSamples = source.getNumSamples();
    sampleExtension = source.getSampleExtension();
    return this;
  }

  @Override
  public int hashCode() {
    // 31 * x == (x << 5) - x
    int hash = super.hashCode();
    hash = ((hash << 5) - hash) + this.glProfile.hashCode() ;
    hash = ((hash << 5) - hash) + ( this.hardwareAccelerated ? 1 : 0 );
    hash = ((hash << 5) - hash) + ( this.stereo ? 1 : 0 );
    hash = ((hash << 5) - hash) + ( this.isFBO ? 1 : 0 );
    hash = ((hash << 5) - hash) + ( this.isPBuffer ? 1 : 0 );
    hash = ((hash << 5) - hash) + ( this.sampleBuffers ? 1 : 0 );
    hash = ((hash << 5) - hash) + this.getNumSamples();
    hash = ((hash << 5) - hash) + this.sampleExtension.hashCode();
    hash = ((hash << 5) - hash) + this.depthBits;
    hash = ((hash << 5) - hash) + this.stencilBits;
    hash = ((hash << 5) - hash) + this.accumRedBits;
    hash = ((hash << 5) - hash) + this.accumGreenBits;
    hash = ((hash << 5) - hash) + this.accumBlueBits;
    hash = ((hash << 5) - hash) + this.accumAlphaBits;
    return hash;
  }

  @Override
  public boolean equals(final Object obj) {
    if(this == obj)  { return true; }
    if(!(obj instanceof GLCapabilitiesImmutable)) {
        return false;
    }
    final GLCapabilitiesImmutable other = (GLCapabilitiesImmutable)obj;
    boolean res = super.equals(obj) &&
                  other.getGLProfile()==glProfile &&
                  other.isPBuffer()==isPBuffer &&
                  other.isFBO()==isFBO &&
                  other.getDoubleBuffered() == doubleBuffered &&
                  other.getStereo()==stereo &&
                  other.getHardwareAccelerated()==hardwareAccelerated &&
                  other.getDepthBits()==depthBits &&
                  other.getStencilBits()==stencilBits &&
                  other.getAccumRedBits()==accumRedBits &&
                  other.getAccumGreenBits()==accumGreenBits &&
                  other.getAccumBlueBits()==accumBlueBits &&
                  other.getAccumAlphaBits()==accumAlphaBits &&
                  other.getSampleBuffers()==sampleBuffers;
    if(res && sampleBuffers) {
        res = other.getNumSamples()==getNumSamples() &&
              other.getSampleExtension().equals(sampleExtension) ;
    }
    return res;
  }

  /** comparing hw/sw, stereo, multisample, stencil, RGBA and depth only */
  @Override
  public int compareTo(final CapabilitiesImmutable o) {
    if ( ! ( o instanceof GLCapabilitiesImmutable ) ) {
        final Class<?> c = (null != o) ? o.getClass() : null ;
        throw new ClassCastException("Not a GLCapabilitiesImmutable object, but " + c);
    }
    final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) o;

    if(hardwareAccelerated && !caps.getHardwareAccelerated()) {
        return 1;
    } else if(!hardwareAccelerated && caps.getHardwareAccelerated()) {
        return -1;
    }

    if(stereo && !caps.getStereo()) {
        return 1;
    } else if(!stereo && caps.getStereo()) {
        return -1;
    }

    if(doubleBuffered && !caps.getDoubleBuffered()) {
        return 1;
    } else if(!doubleBuffered && caps.getDoubleBuffered()) {
        return -1;
    }

    final int ms = getNumSamples();
    final int xms = caps.getNumSamples() ;

    if(ms > xms) {
        return 1;
    } else if( ms < xms ) {
        return -1;
    }
    // ignore the sample extension

    if(stencilBits > caps.getStencilBits()) {
        return 1;
    } else if(stencilBits < caps.getStencilBits()) {
        return -1;
    }

    final int sc = super.compareTo(caps); // RGBA
    if(0 != sc) {
        return sc;
    }

    if(depthBits > caps.getDepthBits()) {
        return 1;
    } else if(depthBits < caps.getDepthBits()) {
        return -1;
    }

    return 0; // they are equal: hw/sw, stereo, multisample, stencil, RGBA and depth
  }

  @Override
  public final GLProfile getGLProfile() {
    return glProfile;
  }

  /** Sets the GL profile you desire */
  public void setGLProfile(final GLProfile profile) {
    glProfile=profile;
  }

  @Override
  public final boolean isPBuffer() {
    return isPBuffer;
  }

  /**
   * Requesting offscreen pbuffer mode.
   * <p>
   * If enabled this method also invokes {@link #setOnscreen(boolean) setOnscreen(false)}.
   * </p>
   * <p>
   * Defaults to false.
   * </p>
   * <p>
   * Requesting offscreen pbuffer mode disables the offscreen auto selection.
   * </p>
   */
  public void setPBuffer(final boolean enable) {
    if(enable) {
      setOnscreen(false);
    }
    isPBuffer = enable;
  }

  @Override
  public final boolean isFBO() {
      return isFBO;
  }

  /**
   * Requesting offscreen FBO mode.
   * <p>
   * If enabled this method also invokes {@link #setOnscreen(boolean) setOnscreen(false)}.
   * </p>
   * <p>
   * Defaults to false.
   * </p>
   * <p>
   * Requesting offscreen FBO mode disables the offscreen auto selection.
   * </p>
   */
  public void setFBO(final boolean enable) {
    if(enable) {
      setOnscreen(false);
    }
    isFBO = enable;
  }

  @Override
  public final boolean getDoubleBuffered() {
    return doubleBuffered;
  }

  /** Enables or disables double buffering. */
  public void setDoubleBuffered(final boolean enable) {
    doubleBuffered = enable;
  }

  @Override
  public final boolean getStereo() {
    return stereo;
  }

  /** Enables or disables stereo viewing. */
  public void setStereo(final boolean enable) {
    stereo = enable;
  }

  @Override
  public final boolean getHardwareAccelerated() {
    return hardwareAccelerated;
  }

  /** Enables or disables hardware acceleration. */
  public void setHardwareAccelerated(final boolean enable) {
    hardwareAccelerated = enable;
  }

  @Override
  public final int getDepthBits() {
    return depthBits;
  }

  /** Sets the number of bits requested for the depth buffer. */
  public void setDepthBits(final int depthBits) {
    this.depthBits = depthBits;
  }

  @Override
  public final int getStencilBits() {
    return stencilBits;
  }

  /** Sets the number of bits requested for the stencil buffer. */
  public void setStencilBits(final int stencilBits) {
    this.stencilBits = stencilBits;
  }

  @Override
  public final int getAccumRedBits() {
    return accumRedBits;
  }

  /** Sets the number of bits requested for the accumulation buffer's
      red component. On some systems only the accumulation buffer
      depth, which is the sum of the red, green, and blue bits, is
      considered. */
  public void setAccumRedBits(final int accumRedBits) {
    this.accumRedBits = accumRedBits;
  }

  @Override
  public final int getAccumGreenBits() {
    return accumGreenBits;
  }

  /** Sets the number of bits requested for the accumulation buffer's
      green component. On some systems only the accumulation buffer
      depth, which is the sum of the red, green, and blue bits, is
      considered. */
  public void setAccumGreenBits(final int accumGreenBits) {
    this.accumGreenBits = accumGreenBits;
  }

  @Override
  public final int getAccumBlueBits() {
    return accumBlueBits;
  }

  /** Sets the number of bits requested for the accumulation buffer's
      blue component. On some systems only the accumulation buffer
      depth, which is the sum of the red, green, and blue bits, is
      considered. */
  public void setAccumBlueBits(final int accumBlueBits) {
    this.accumBlueBits = accumBlueBits;
  }

  @Override
  public final int getAccumAlphaBits() {
    return accumAlphaBits;
  }

  /** Sets number of bits requested for accumulation buffer's alpha
      component. On some systems only the accumulation buffer depth,
      which is the sum of the red, green, and blue bits, is
      considered. */
  public void setAccumAlphaBits(final int accumAlphaBits) {
    this.accumAlphaBits = accumAlphaBits;
  }

  /**
   * Sets the desired extension for full-scene antialiasing
   * (FSAA), default is {@link #DEFAULT_SAMPLE_EXTENSION}.
   */
  public void setSampleExtension(final String se) {
      sampleExtension = se;
  }

  @Override
  public final String getSampleExtension() {
      return sampleExtension;
  }

  /**
   * Defaults to false.<br>
   * Indicates whether sample buffers for full-scene antialiasing
   * (FSAA) should be allocated for this drawable.<br>
   * Mind that this requires the alpha component.<br>
   * If enabled this method also invokes {@link #setAlphaBits(int) setAlphaBits(1)}
   * if {@link #getAlphaBits()} == 0.<br>
   */
  public void setSampleBuffers(final boolean enable) {
    sampleBuffers = enable;
    if(sampleBuffers && getAlphaBits()==0) {
        setAlphaBits(1);
    }
  }

  @Override
  public final boolean getSampleBuffers() {
    return sampleBuffers;
  }

  /**
   * If sample buffers are enabled, indicates the number of buffers
   * to be allocated. Defaults to 2.
   * @see #getNumSamples()
   */
  public void setNumSamples(final int numSamples) {
    this.numSamples = numSamples;
  }

  @Override
  public final int getNumSamples() {
    return sampleBuffers ? numSamples : 0;
  }

  @Override
  public StringBuilder toString(StringBuilder sink) {
    if(null == sink) {
        sink = new StringBuilder();
    }

    final int samples = sampleBuffers ? numSamples : 0 ;

    super.toString(sink, false);

    sink.append(", accum-rgba ").append(accumRedBits).append(ESEP).append(accumGreenBits).append(ESEP).append(accumBlueBits).append(ESEP).append(accumAlphaBits);
    sink.append(", dp/st/ms ").append(depthBits).append(ESEP).append(stencilBits).append(ESEP).append(samples);
    if(samples>0) {
        sink.append(", sample-ext ").append(sampleExtension);
    }
    if(doubleBuffered) {
        sink.append(", dbl");
    } else {
        sink.append(", one");
    }
    if(stereo) {
        sink.append(", stereo");
    } else {
        sink.append(", mono  ");
    }
    if(hardwareAccelerated) {
        sink.append(", hw, ");
    } else {
        sink.append(", sw, ");
    }
    sink.append(glProfile);
    if(isOnscreen()) {
        sink.append(", on-scr[");
    } else {
        sink.append(", offscr[");
    }
    boolean ns=false;
    if(isFBO()) {
        sink.append("fbo");
        ns = true;
    }
    if(isPBuffer()) {
        if(ns) { sink.append(CSEP); }
        sink.append("pbuffer");
        ns = true;
    }
    if(isBitmap()) {
        if(ns) { sink.append(CSEP); }
        sink.append("bitmap");
        ns = true;
    }
    if(!ns) { // !FBO !PBuffer !Bitmap
        if(isOnscreen()) {
            sink.append(".");        // no additional off-screen modes besides on-screen
        } else {
            sink.append("auto-cfg"); // auto-config off-screen mode
        }
    }
    sink.append("]");

    return sink;
  }

  /** Returns a textual representation of this GLCapabilities
      object. */
  @Override
  public String toString() {
    final StringBuilder msg = new StringBuilder();
    msg.append("GLCaps[");
    toString(msg);
    msg.append("]");
    return msg.toString();
  }
}
