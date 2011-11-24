/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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

package javax.media.nativewindow.awt;

import javax.media.nativewindow.*;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.image.ColorModel;
import javax.media.nativewindow.AbstractGraphicsConfiguration;

import jogamp.nativewindow.Debug;

/** A wrapper for an AWT GraphicsConfiguration allowing it to be
    handled in a toolkit-independent manner. */

public class AWTGraphicsConfiguration extends DefaultGraphicsConfiguration implements Cloneable {
  private GraphicsConfiguration config;
  AbstractGraphicsConfiguration encapsulated;

  public AWTGraphicsConfiguration(AWTGraphicsScreen screen, 
                                  CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested,
                                  GraphicsConfiguration config, AbstractGraphicsConfiguration encapsulated) {
    super(screen, capsChosen, capsRequested);
    this.config = config;
    this.encapsulated=encapsulated;
  }

  private AWTGraphicsConfiguration(AWTGraphicsScreen screen, CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested,
                                   GraphicsConfiguration config) {
    super(screen, capsChosen, capsRequested);
    this.config = config;
    this.encapsulated=null;
  }
  
  /**
   * @param capsChosen if null, <code>capsRequested</code> is copied and aligned 
   *        with the graphics Capabilities of the AWT Component to produce the chosen Capabilities.
   *        Otherwise the <code>capsChosen</code> is used.
   */
  public static AWTGraphicsConfiguration create(Component awtComp, CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested) {
      final GraphicsConfiguration awtGfxConfig = awtComp.getGraphicsConfiguration();
      if(null==awtGfxConfig) {
          throw new NativeWindowException("AWTGraphicsConfiguration.create: Null AWT GraphicsConfiguration @ "+awtComp);          
      }
      final GraphicsDevice awtGraphicsDevice = awtGfxConfig.getDevice();
      if(null==awtGraphicsDevice) {
          throw new NativeWindowException("AWTGraphicsConfiguration.create: Null AWT GraphicsDevice @ "+awtGfxConfig);
      }

      // Create Device/Screen
      final AWTGraphicsDevice awtDevice = new AWTGraphicsDevice(awtGraphicsDevice, AbstractGraphicsDevice.DEFAULT_UNIT);
      final AWTGraphicsScreen awtScreen = new AWTGraphicsScreen(awtDevice);

      if(null==capsChosen) {
          GraphicsConfiguration gc = awtGraphicsDevice.getDefaultConfiguration();
          capsChosen = AWTGraphicsConfiguration.setupCapabilitiesRGBABits(capsRequested, gc);
      }
      final GraphicsConfigurationFactory factory = GraphicsConfigurationFactory.getFactory(awtDevice);
      final AbstractGraphicsConfiguration config = factory.chooseGraphicsConfiguration(capsChosen, capsRequested, null, awtScreen);
      if(config instanceof AWTGraphicsConfiguration) {
          return (AWTGraphicsConfiguration) config;
      }
      // System.err.println("Info: AWTGraphicsConfiguration.create: Expected AWTGraphicsConfiguration got: "+config.getClass()+" w/ factory "+factory.getClass()+" - Unable to encapsulate native GraphicsConfiguration.");
      return new AWTGraphicsConfiguration(awtScreen, capsChosen, capsRequested, awtGfxConfig);
  }

  // open access to superclass method
  public void setChosenCapabilities(CapabilitiesImmutable capsChosen) {
      super.setChosenCapabilities(capsChosen);
  }
  
  @Override
  public Object clone() {
      return super.clone();
  }

  /** Return the AWT {@link GraphicsConfiguration}. */
  public GraphicsConfiguration getAWTGraphicsConfiguration() {
    return config;
  }

  @Override
  public AbstractGraphicsConfiguration getNativeGraphicsConfiguration() {
    return (null!=encapsulated)?encapsulated:this;
  }

  /**
   * Sets up the Capabilities' RGBA size based on the given GraphicsConfiguration's ColorModel.
   *
   * @param capabilities the Capabilities object whose red, green, blue, and alpha bits will be set
   * @param gc the GraphicsConfiguration from which to derive the RGBA bit depths
   * @return the passed Capabilities
   */
  public static CapabilitiesImmutable setupCapabilitiesRGBABits(CapabilitiesImmutable capabilitiesIn, GraphicsConfiguration gc) {
    Capabilities capabilities = (Capabilities) capabilitiesIn.cloneMutable();

    ColorModel cm = gc.getColorModel();
    if(null==cm) {
        throw new NativeWindowException("Could not determine AWT ColorModel");
    }
    int cmBitsPerPixel = cm.getPixelSize();
    int bitsPerPixel = 0;
    int[] bitesPerComponent = cm.getComponentSize();
    if(bitesPerComponent.length>=3) {
        capabilities.setRedBits(bitesPerComponent[0]);
        bitsPerPixel += bitesPerComponent[0];
        capabilities.setGreenBits(bitesPerComponent[1]);
        bitsPerPixel += bitesPerComponent[1];
        capabilities.setBlueBits(bitesPerComponent[2]);
        bitsPerPixel += bitesPerComponent[2];
    }
    if(bitesPerComponent.length>=4) {
        capabilities.setAlphaBits(bitesPerComponent[3]);
        bitsPerPixel += bitesPerComponent[3];
    } else {
        capabilities.setAlphaBits(0);
    }
    if(Debug.debugAll()) {
        if(cmBitsPerPixel!=bitsPerPixel) {
            System.err.println("AWT Colormodel bits per components/pixel mismatch: "+bitsPerPixel+" != "+cmBitsPerPixel);
        }
    }
    return capabilities;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()+"[" + getScreen() +
                                   ",\n\tchosen    " + capabilitiesChosen+
                                   ",\n\trequested " + capabilitiesRequested+ 
                                   ",\n\t" + config +
                                   ",\n\tencapsulated "+encapsulated+"]";
  }
}
