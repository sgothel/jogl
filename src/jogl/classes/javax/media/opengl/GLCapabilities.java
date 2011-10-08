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

package javax.media.opengl;

import javax.media.nativewindow.Capabilities;

/** Specifies a set of OpenGL capabilities.<br>
    At creation time of a {@link GLDrawable} using {@link GLDrawableFactory},
    an instance of this class is passed,
    describing the desired capabilities that a rendering context
    must support, such as the OpenGL profile, color depth and whether stereo is enabled.<br>

    The actual capabilites of created {@link GLDrawable}s are then reflected by their own
    GLCapabilites instance, which can be queried with {@link GLDrawable#getGLCapabilities()}.<br>

    It currently contains the minimal number of routines which allow
    configuration on all supported window systems. */
public class GLCapabilities extends Capabilities implements Cloneable, GLCapabilitiesImmutable {
  private GLProfile glProfile = null;
  private boolean pbuffer = false;
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

  // Bits for pbuffer creation
  private boolean pbufferFloatingPointBuffers;
  private boolean pbufferRenderToTexture;
  private boolean pbufferRenderToTextureRectangle;

  /** Creates a GLCapabilities object. All attributes are in a default state.
    * @param glp GLProfile, or null for the default GLProfile
    * @throws GLException if no profile is given and no default profile is available for the default device.
    */
  public GLCapabilities(GLProfile glp) throws GLException {
      glProfile = (null!=glp)?glp:GLProfile.getDefault(GLProfile.getDefaultDevice());
  }

  public Object cloneMutable() {
    return clone();
  }

  public Object clone() {
    try {
      return super.clone();
    } catch (RuntimeException e) {
      throw new GLException(e);
    }
  }

  public int hashCode() {
    // 31 * x == (x << 5) - x
    int hash = 31 + this.glProfile.hashCode() ;
    hash = ((hash << 5) - hash) + ( this.pbuffer ? 1 : 0 );
    hash = ((hash << 5) - hash) + ( this.stereo ? 1 : 0 );
    hash = ((hash << 5) - hash) + ( this.hardwareAccelerated ? 1 : 0 );
    hash = ((hash << 5) - hash) + this.depthBits;
    hash = ((hash << 5) - hash) + this.stencilBits;
    hash = ((hash << 5) - hash) + this.accumRedBits;
    hash = ((hash << 5) - hash) + this.accumGreenBits;
    hash = ((hash << 5) - hash) + this.accumBlueBits;
    hash = ((hash << 5) - hash) + this.accumAlphaBits;
    hash = ((hash << 5) - hash) + ( this.sampleBuffers ? 1 : 0 );
    hash = ((hash << 5) - hash) + this.numSamples;
    hash = ((hash << 5) - hash) + this.sampleExtension.hashCode();
    hash = ((hash << 5) - hash) + ( this.pbufferFloatingPointBuffers ? 1 : 0 );
    hash = ((hash << 5) - hash) + ( this.pbufferRenderToTexture ? 1 : 0 );
    hash = ((hash << 5) - hash) + ( this.pbufferRenderToTextureRectangle ? 1 : 0 );
    return hash;
  }

  public boolean equals(Object obj) {
    if(this == obj)  { return true; }
    if(!(obj instanceof GLCapabilitiesImmutable)) {
        return false;
    }
    GLCapabilitiesImmutable other = (GLCapabilitiesImmutable)obj;
    boolean res = super.equals(obj) &&
                  other.getGLProfile()==glProfile &&
                  other.isPBuffer()==pbuffer &&
                  other.getStereo()==stereo &&
                  other.getHardwareAccelerated()==hardwareAccelerated &&
                  other.getDepthBits()==depthBits &&
                  other.getStencilBits()==stencilBits &&
                  other.getAccumRedBits()==accumRedBits &&
                  other.getAccumGreenBits()==accumGreenBits &&
                  other.getAccumBlueBits()==accumBlueBits &&
                  other.getAccumAlphaBits()==accumAlphaBits &&
                  other.getSampleBuffers()==sampleBuffers &&
                  other.getPbufferFloatingPointBuffers()==pbufferFloatingPointBuffers &&
                  other.getPbufferRenderToTexture()==pbufferRenderToTexture &&
                  other.getPbufferRenderToTextureRectangle()==pbufferRenderToTextureRectangle;
    if(sampleBuffers) {
        res = res &&
              other.getNumSamples()==numSamples && 
              other.getSampleExtension().equals(sampleExtension) ;
    }
    return res;
  }

  /** comparing hw/sw, stereo, multisample, stencil, RGBA and depth only */
  public int compareTo(Object o) {
    if ( ! ( o instanceof GLCapabilities ) ) {
        Class<?> c = (null != o) ? o.getClass() : null ;
        throw new ClassCastException("Not a GLCapabilities object: " + c);
    }

    final GLCapabilities caps = (GLCapabilities) o;

    if(hardwareAccelerated && !caps.hardwareAccelerated) {
        return 1;
    } else if(!hardwareAccelerated && caps.hardwareAccelerated) {
        return -1;
    }

    if(stereo && !caps.stereo) {
        return 1;
    } else if(!stereo && caps.stereo) {
        return -1;
    }

    final int ms = sampleBuffers ? numSamples : 0;
    final int xms = caps.sampleBuffers ? caps.numSamples : 0;

    if(ms > xms) {
        return 1;
    } else if( ms < xms ) {
        return -1;
    }
    // ignore the sample extension 

    if(stencilBits > caps.stencilBits) {
        return 1;
    } else if(stencilBits < caps.stencilBits) {
        return -1;
    }

    final int sc = super.compareTo(caps); // RGBA
    if(0 != sc) {
        return sc;
    }

    if(depthBits > caps.depthBits) {
        return 1;
    } else if(depthBits < caps.depthBits) {
        return -1;
    }

    return 0; // they are equal: hw/sw, stereo, multisample, stencil, RGBA and depth
  }

  public GLProfile getGLProfile() {
    return glProfile;
  }

  /** Sets the GL profile you desire */
  public void setGLProfile(GLProfile profile) {
    glProfile=profile;
  }

  public boolean isPBuffer() {
    return pbuffer;
  }

  /** 
   * Enables or disables pbuffer usage.<br>
   * If enabled this method also invokes {@link #setOnscreen(int) setOnscreen(false)}<br>
   * Defaults to false.
   */
  public void setPBuffer(boolean enable) {
    if(enable) {
      setOnscreen(false);
    }
    pbuffer = enable;
  }

  /**
   * Sets whether the drawable surface supports onscreen.<br>
   * If enabled this method also invokes {@link #setPBuffer(int) setPBuffer(false)}<br>
   * Defaults to true.
  */
  public void setOnscreen(boolean onscreen) {
    if(onscreen) {
        setPBuffer(false);
    }
    super.setOnscreen(onscreen);
  }

  public boolean getDoubleBuffered() {
    return doubleBuffered;
  }

  /** Enables or disables double buffering. */
  public void setDoubleBuffered(boolean enable) {
    doubleBuffered = enable;
  }

  public boolean getStereo() {
    return stereo;
  }
  
  /** Enables or disables stereo viewing. */
  public void setStereo(boolean enable) {
    stereo = enable;
  }

  public boolean getHardwareAccelerated() {
    return hardwareAccelerated;
  }
  
  /** Enables or disables hardware acceleration. */
  public void setHardwareAccelerated(boolean enable) {
    hardwareAccelerated = enable;
  }

  public int getDepthBits() {
    return depthBits;
  }

  /** Sets the number of bits requested for the depth buffer. */
  public void setDepthBits(int depthBits) {
    this.depthBits = depthBits;
  }
  
  public int getStencilBits() {
    return stencilBits;
  }

  /** Sets the number of bits requested for the stencil buffer. */
  public void setStencilBits(int stencilBits) {
    this.stencilBits = stencilBits;
  }
  
  public int getAccumRedBits() {
    return accumRedBits;
  }

  /** Sets the number of bits requested for the accumulation buffer's
      red component. On some systems only the accumulation buffer
      depth, which is the sum of the red, green, and blue bits, is
      considered. */
  public void setAccumRedBits(int accumRedBits) {
    this.accumRedBits = accumRedBits;
  }

  public int getAccumGreenBits() {
    return accumGreenBits;
  }

  /** Sets the number of bits requested for the accumulation buffer's
      green component. On some systems only the accumulation buffer
      depth, which is the sum of the red, green, and blue bits, is
      considered. */
  public void setAccumGreenBits(int accumGreenBits) {
    this.accumGreenBits = accumGreenBits;
  }

  public int getAccumBlueBits() {
    return accumBlueBits;
  }

  /** Sets the number of bits requested for the accumulation buffer's
      blue component. On some systems only the accumulation buffer
      depth, which is the sum of the red, green, and blue bits, is
      considered. */
  public void setAccumBlueBits(int accumBlueBits) {
    this.accumBlueBits = accumBlueBits;
  }

  public int getAccumAlphaBits() {
    return accumAlphaBits;
  }

  /** Sets number of bits requested for accumulation buffer's alpha
      component. On some systems only the accumulation buffer depth,
      which is the sum of the red, green, and blue bits, is
      considered. */
  public void setAccumAlphaBits(int accumAlphaBits) {
    this.accumAlphaBits = accumAlphaBits;
  }

  /**
   * Sets the desired extension for full-scene antialiasing
   * (FSAA), default is {@link #DEFAULT_SAMPLE_EXTENSION}.
   */
  public void setSampleExtension(String se) { 
      sampleExtension = se; 
  }
    
  public String getSampleExtension() { 
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
  public void setSampleBuffers(boolean enable) {
    sampleBuffers = enable;
    if(sampleBuffers && getAlphaBits()==0) {
        setAlphaBits(1);
    }        
  }

  public boolean getSampleBuffers() {
    return sampleBuffers;
  }

  /** If sample buffers are enabled, indicates the number of buffers
      to be allocated. Defaults to 2. */
  public void setNumSamples(int numSamples) {
    this.numSamples = numSamples;
  }

  public int getNumSamples() {
    return numSamples;
  }

  /** For pbuffers only, indicates whether floating-point buffers
      should be used if available. Defaults to false. */
  public void setPbufferFloatingPointBuffers(boolean enable) {
    pbufferFloatingPointBuffers = enable;
  }

  public boolean getPbufferFloatingPointBuffers() {
    return pbufferFloatingPointBuffers;
  }

  /** For pbuffers only, indicates whether the render-to-texture
      extension should be used if available.  Defaults to false. */
  public void setPbufferRenderToTexture(boolean enable) {
    pbufferRenderToTexture = enable;
  }

  public boolean getPbufferRenderToTexture() {
    return pbufferRenderToTexture;
  }

  /** For pbuffers only, indicates whether the
      render-to-texture-rectangle extension should be used if
      available. Defaults to false. */
  public void setPbufferRenderToTextureRectangle(boolean enable) {
    pbufferRenderToTextureRectangle = enable;
  }

  public boolean getPbufferRenderToTextureRectangle() {
    return pbufferRenderToTextureRectangle;
  }

  public StringBuffer toString(StringBuffer sink) {
    if(null == sink) {
        sink = new StringBuffer();
    }

    int samples = sampleBuffers ? numSamples : 0 ;

    super.toString(sink);

    sink.append(", accum-rgba ").append(accumRedBits).append("/").append(accumGreenBits).append("/").append(accumBlueBits).append("/").append(accumAlphaBits);
    sink.append(", dp/st/ms: ").append(depthBits).append("/").append(stencilBits).append("/").append(samples);
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
    if(!isOnscreen()) {
        if(pbuffer) {
            sink.append(", pbuffer [r2t ").append(pbufferRenderToTexture?1:0)
                .append(", r2tr ").append(pbufferRenderToTextureRectangle?1:0)
                .append(", float ").append(pbufferFloatingPointBuffers?1:0)
                .append("]");
        } else {
            sink.append(", pixmap");
        }
    }
    
    return sink;
  }

  /** Returns a textual representation of this GLCapabilities
      object. */ 
  public String toString() {
    StringBuffer msg = new StringBuffer();
    msg.append("GLCaps[");
    toString(msg);
    msg.append("]");
    return msg.toString();
  }
}
