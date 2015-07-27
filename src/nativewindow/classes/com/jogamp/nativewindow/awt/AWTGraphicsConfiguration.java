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

package com.jogamp.nativewindow.awt;

import com.jogamp.nativewindow.*;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.image.ColorModel;
import com.jogamp.nativewindow.AbstractGraphicsConfiguration;

import jogamp.nativewindow.Debug;

/** A wrapper for an AWT GraphicsConfiguration allowing it to be
    handled in a toolkit-independent manner. */

public class AWTGraphicsConfiguration extends DefaultGraphicsConfiguration implements Cloneable {
  private final GraphicsConfiguration config;
  AbstractGraphicsConfiguration encapsulated;

  public AWTGraphicsConfiguration(final AWTGraphicsScreen screen,
                                  final CapabilitiesImmutable capsChosen, final CapabilitiesImmutable capsRequested,
                                  final GraphicsConfiguration config, final AbstractGraphicsConfiguration encapsulated) {
    super(screen, capsChosen, capsRequested);
    this.config = config;
    this.encapsulated=encapsulated;
  }

  private AWTGraphicsConfiguration(final AWTGraphicsScreen screen, final CapabilitiesImmutable capsChosen, final CapabilitiesImmutable capsRequested,
                                   final GraphicsConfiguration config) {
    super(screen, capsChosen, capsRequested);
    this.config = config;
    this.encapsulated=null;
  }

  /**
   * @deprecated Use {@link #create(GraphicsConfiguration, CapabilitiesImmutable, CapabilitiesImmutable)}
   * Method constructs a new {@link AWTGraphicsConfiguration} primarily based
   * on the given {@link Component}'s {@link GraphicsConfiguration}.
   * @param awtComp the {@link Component}, which {@link GraphicsConfiguration} is used for the resulting {@link AWTGraphicsConfiguration}
   * @param capsChosen if null, <code>capsRequested</code> is copied and aligned
   *        with the graphics {@link Capabilities} of the AWT Component to produce the chosen {@link Capabilities}.
   *        Otherwise the <code>capsChosen</code> is used.
   * @param capsRequested if null, default {@link Capabilities} are used, otherwise the given values.
   */
  public static AWTGraphicsConfiguration create(final Component awtComp, final CapabilitiesImmutable capsChosen, final CapabilitiesImmutable capsRequested) {
      if(null==awtComp) {
          throw new IllegalArgumentException("Null AWT Component");
      }
      final GraphicsConfiguration gc = awtComp.getGraphicsConfiguration();
      if( null == gc ) {
          throw new NativeWindowException("Null AWT GraphicsConfiguration @ "+awtComp);
      }
      return create(gc, capsChosen, capsRequested);
  }

  /**
   * Method constructs a new {@link AWTGraphicsConfiguration} primarily based
   * on the given {@link GraphicsConfiguration}.
   * @param gc the {@link GraphicsConfiguration} for the resulting {@link AWTGraphicsConfiguration}
   * @param capsChosen if null, <code>capsRequested</code> is copied and aligned
   *        with the graphics {@link Capabilities} of the AWT Component to produce the chosen {@link Capabilities}.
   *        Otherwise the <code>capsChosen</code> is used.
   * @param capsRequested if null, default {@link Capabilities} are used, otherwise the given values.
   */
  public static AWTGraphicsConfiguration create(final GraphicsConfiguration gc, CapabilitiesImmutable capsChosen, CapabilitiesImmutable capsRequested) {
      if(null==gc) {
          throw new IllegalArgumentException("Null AWT GraphicsConfiguration");
      }
      final GraphicsDevice awtGraphicsDevice = gc.getDevice();
      if(null==awtGraphicsDevice) {
          throw new NativeWindowException("Null AWT GraphicsDevice @ "+gc);
      }

      // Create Device/Screen
      final AWTGraphicsDevice awtDevice = new AWTGraphicsDevice(awtGraphicsDevice, AbstractGraphicsDevice.DEFAULT_UNIT);
      final AWTGraphicsScreen awtScreen = new AWTGraphicsScreen(awtDevice);

      if(null==capsRequested) {
          capsRequested = new Capabilities();
      }
      if(null==capsChosen) {
          capsChosen = AWTGraphicsConfiguration.setupCapabilitiesRGBABits(capsRequested, gc);
      }
      final GraphicsConfigurationFactory factory = GraphicsConfigurationFactory.getFactory(awtDevice, capsChosen);
      final AbstractGraphicsConfiguration config = factory.chooseGraphicsConfiguration(capsChosen, capsRequested, null, awtScreen, VisualIDHolder.VID_UNDEFINED);
      if(config instanceof AWTGraphicsConfiguration) {
          return (AWTGraphicsConfiguration) config;
      }
      // System.err.println("Info: AWTGraphicsConfiguration.create: Expected AWTGraphicsConfiguration got: "+config.getClass()+" w/ factory "+factory.getClass()+" - Unable to encapsulate native GraphicsConfiguration.");
      return new AWTGraphicsConfiguration(awtScreen, capsChosen, capsRequested, gc);
  }

  // open access to superclass method
  @Override
  public void setChosenCapabilities(final CapabilitiesImmutable capsChosen) {
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
  public static CapabilitiesImmutable setupCapabilitiesRGBABits(final CapabilitiesImmutable capabilitiesIn, final GraphicsConfiguration gc) {
    final Capabilities capabilities = (Capabilities) capabilitiesIn.cloneMutable();

    final ColorModel cm = gc.getColorModel();
    if(null==cm) {
        throw new NativeWindowException("Could not determine AWT ColorModel");
    }
    final int cmBitsPerPixel = cm.getPixelSize();
    int bitsPerPixel = 0;
    final int[] bitesPerComponent = cm.getComponentSize();
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
