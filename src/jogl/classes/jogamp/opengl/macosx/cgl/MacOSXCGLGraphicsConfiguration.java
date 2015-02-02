/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package jogamp.opengl.macosx.cgl;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.nativewindow.MutableGraphicsConfiguration;

public class MacOSXCGLGraphicsConfiguration extends MutableGraphicsConfiguration implements Cloneable {

    MacOSXCGLGraphicsConfiguration(final AbstractGraphicsScreen screen,
                                   final GLCapabilitiesImmutable capsChosen, final GLCapabilitiesImmutable capsRequested) {
        super(screen, capsChosen, capsRequested);
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    protected static List<GLCapabilitiesImmutable> getAvailableCapabilities(final MacOSXCGLDrawableFactory factory, final AbstractGraphicsDevice device) {
        final MacOSXCGLDrawableFactory.SharedResource sharedResource = factory.getOrCreateSharedResourceImpl(device);
        if(null == sharedResource) {
            throw new GLException("Shared resource for device n/a: "+device);
        }
        // MacOSXGraphicsDevice osxDevice = sharedResource.getDevice();
        return new ArrayList<GLCapabilitiesImmutable>(0);
    }

    static final IntBuffer cglInternalAttributeToken = Buffers.newDirectIntBuffer(new int[] {
        CGL.kCGLPFAOpenGLProfile,    // >= lion
        CGL.NSOpenGLPFAAccelerated,  // query only (prefer accelerated, but allow non accelerated), ignored for createPixelformat
        CGL.NSOpenGLPFANoRecovery,
        CGL.kCGLPFAColorFloat,
        CGL.NSOpenGLPFAPixelBuffer,
        CGL.NSOpenGLPFADoubleBuffer,
        CGL.NSOpenGLPFAStereo,
        CGL.NSOpenGLPFAColorSize,
        CGL.NSOpenGLPFAAlphaSize,
        CGL.NSOpenGLPFADepthSize,
        CGL.NSOpenGLPFAAccumSize,
        CGL.NSOpenGLPFAStencilSize,
        CGL.NSOpenGLPFASampleBuffers,
        CGL.NSOpenGLPFASamples });

    static IntBuffer GLCapabilities2NSAttribList(final AbstractGraphicsDevice device, final IntBuffer attrToken, final GLCapabilitiesImmutable caps, final int ctp, final int major, final int minor) {
        final int len = attrToken.remaining();
        final int off = attrToken.position();
        final IntBuffer ivalues = Buffers.newDirectIntBuffer(len);

        for (int idx = 0; idx < len; idx++) {
          final int attr = attrToken.get(idx+off);
          switch (attr) {
              case CGL.kCGLPFAOpenGLProfile:
                ivalues.put(idx, MacOSXCGLContext.GLProfile2CGLOGLProfileValue(device, ctp, major, minor));
                break;
              case CGL.NSOpenGLPFANoRecovery:
                ivalues.put(idx, caps.getHardwareAccelerated() ? 1 : 0);
                break;

              case CGL.kCGLPFAColorFloat:
                // ivalues.put(idx, ( !caps.isOnscreen() && caps.isPBuffer() && caps.getPbufferFloatingPointBuffers() ) ? 1 : 0);
                  ivalues.put(idx, 0);
                break;

              case CGL.NSOpenGLPFAPixelBuffer:
                ivalues.put(idx, ( !caps.isOnscreen() && caps.isPBuffer() ) ? 1 : 0);
                break;

              case CGL.NSOpenGLPFADoubleBuffer:
                ivalues.put(idx, (caps.getDoubleBuffered() ? 1 : 0));
                break;

              case CGL.NSOpenGLPFAStereo:
                ivalues.put(idx, (caps.getStereo() ? 1 : 0));
                break;

              case CGL.NSOpenGLPFAColorSize:
                ivalues.put(idx, (caps.getRedBits() + caps.getGreenBits() + caps.getBlueBits()));
                break;

              case CGL.NSOpenGLPFAAlphaSize:
                ivalues.put(idx, caps.getAlphaBits());
                break;

              case CGL.NSOpenGLPFADepthSize:
                ivalues.put(idx, caps.getDepthBits());
                break;

              case CGL.NSOpenGLPFAAccumSize:
                ivalues.put(idx, (caps.getAccumRedBits() + caps.getAccumGreenBits() + caps.getAccumBlueBits() + caps.getAccumAlphaBits()));
                break;

              case CGL.NSOpenGLPFAStencilSize:
                ivalues.put(idx, caps.getStencilBits());
                break;

              case CGL.NSOpenGLPFASampleBuffers:
                ivalues.put(idx, caps.getSampleBuffers() ? 1 : 0);
                break;

              case CGL.NSOpenGLPFASamples:
                ivalues.put(idx, caps.getNumSamples());
                break;

              default:
                break;
          }
        }
        return ivalues;
    }

    static long GLCapabilities2NSPixelFormat(final AbstractGraphicsDevice device, final GLCapabilitiesImmutable caps, final int ctp, final int major, final int minor) {
        final IntBuffer attrToken = cglInternalAttributeToken.duplicate();
        if ( !MacOSXCGLContext.isLionOrLater ) {
            // no OpenGLProfile
            attrToken.position(1);
        }
        final IntBuffer ivalues = GLCapabilities2NSAttribList(device, attrToken, caps, ctp, major, minor);
        return CGL.createPixelFormat(attrToken, attrToken.remaining(), ivalues);
    }

    static GLCapabilities NSPixelFormat2GLCapabilities(final GLProfile glp, final long pixelFormat) {
        return PixelFormat2GLCapabilities(glp, pixelFormat, true);
    }

    static long GLCapabilities2CGLPixelFormat(final AbstractGraphicsDevice device, final GLCapabilitiesImmutable caps, final int ctp, final int major, final int minor) {
      // Set up pixel format attributes
      final IntBuffer attrs = Buffers.newDirectIntBuffer(256);
      int i = 0;
      if(MacOSXCGLContext.isLionOrLater) {
          attrs.put(i++, CGL.kCGLPFAOpenGLProfile);
          attrs.put(i++, MacOSXCGLContext.GLProfile2CGLOGLProfileValue(device, ctp, major, minor));
      }
      /**
      if(!caps.isOnscreen() && caps.isPBuffer()) {
        attrs.put(i++, CGL.kCGLPFAPBuffer);
        if (caps.getPbufferFloatingPointBuffers()) {
          attrs.put(i++, CGL.kCGLPFAColorFloat);
        }
      } */
      if (caps.getDoubleBuffered()) {
        attrs.put(i++, CGL.kCGLPFADoubleBuffer);
      }
      if (caps.getStereo()) {
        attrs.put(i++, CGL.kCGLPFAStereo);
      }
      attrs.put(i++, CGL.kCGLPFAColorSize);
      attrs.put(i++, ( caps.getRedBits() +
                       caps.getGreenBits() +
                       caps.getBlueBits() ) );
      attrs.put(i++, CGL.kCGLPFAAlphaSize);
      attrs.put(i++, caps.getAlphaBits());
      attrs.put(i++, CGL.kCGLPFADepthSize);
      attrs.put(i++, caps.getDepthBits());
      // FIXME: should validate stencil size as is done in MacOSXWindowSystemInterface.m
      attrs.put(i++, CGL.kCGLPFAStencilSize);
      attrs.put(i++, caps.getStencilBits());
      attrs.put(i++, CGL.kCGLPFAAccumSize);
      attrs.put(i++, ( caps.getAccumRedBits() +
                       caps.getAccumGreenBits() +
                       caps.getAccumBlueBits() +
                       caps.getAccumAlphaBits() ) );
      if (caps.getSampleBuffers()) {
        attrs.put(i++, CGL.kCGLPFASampleBuffers);
        attrs.put(i++, 1);
        attrs.put(i++, CGL.kCGLPFASamples);
        attrs.put(i++, caps.getNumSamples());
      }

      // Use attribute array to select pixel format
      final PointerBuffer fmt = PointerBuffer.allocateDirect(1);
      final IntBuffer numScreens = Buffers.newDirectIntBuffer(1);
      final int res = CGL.CGLChoosePixelFormat(attrs, fmt, numScreens);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error code " + res + " while choosing pixel format");
      }
      return fmt.get(0);
    }

    static GLCapabilities CGLPixelFormat2GLCapabilities(final long pixelFormat) {
        return PixelFormat2GLCapabilities(null, pixelFormat, false);
    }

    private static GLCapabilities PixelFormat2GLCapabilities(GLProfile glp, final long pixelFormat, final boolean nsUsage) {
        final IntBuffer attrToken = cglInternalAttributeToken.duplicate();
        final int off;
        if ( !MacOSXCGLContext.isLionOrLater ) {
            // no OpenGLProfile
            off = 1;
        } else {
            off = 0;
        }
        attrToken.position(off);
        final int len = attrToken.remaining();
        final IntBuffer ivalues = Buffers.newDirectIntBuffer(len);

        // On this platform the pixel format is associated with the
        // context and not the drawable. However it's a reasonable
        // approximation to just store the chosen pixel format up in the
        // NativeSurface's AbstractGraphicsConfiguration,
        // since the public API doesn't provide for a different GLCapabilities per context.
        // Note: These restrictions of the platform's API might be considered as a bug anyways.

        // Figure out what attributes we really got
        if(nsUsage) {
            CGL.queryPixelFormat(pixelFormat, attrToken, len, ivalues);
        } else {
            CGL.CGLQueryPixelFormat(pixelFormat, attrToken, len, ivalues);
        }

        if(null == glp && MacOSXCGLContext.isLionOrLater) {
            // pre-scan for OpenGL Profile
            for (int i = 0; i < len; i++) {
                final int ivalue = ivalues.get(i);
                if(CGL.kCGLPFAOpenGLProfile == attrToken.get(i+off)) {
                    switch(ivalue) {
                        case CGL.kCGLOGLPVersion_GL4_Core:
                            glp = GLProfile.get(GLProfile.GL4);
                            break;
                        case CGL.kCGLOGLPVersion_GL3_Core:
                            glp = GLProfile.get(GLProfile.GL3);
                            break;
                        case CGL.kCGLOGLPVersion_Legacy:
                            glp = GLProfile.get(GLProfile.GL2);
                            break;
                        default:
                            throw new RuntimeException("Unhandled OSX OpenGL Profile: 0x"+Integer.toHexString(ivalue));
                    }
                }
            }
        }
        if(null == glp) {
            glp = GLProfile.get(GLProfile.GL2);
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        int alphaBits = 0;
        for (int i = 0; i < len; i++) {
          final int attr = attrToken.get(i+off);
          final int ivalue = ivalues.get(i);
          switch (attr) {
              case CGL.NSOpenGLPFAAccelerated:
                caps.setHardwareAccelerated(ivalue != 0);
                break;

              case CGL.kCGLPFAColorFloat:
                // caps.setPbufferFloatingPointBuffers(ivalue != 0);
                break;

              case CGL.NSOpenGLPFAPixelBuffer:
                caps.setPBuffer(ivalue != 0);
                break;

              case CGL.NSOpenGLPFADoubleBuffer:
                caps.setDoubleBuffered(ivalue != 0);
                break;

              case CGL.NSOpenGLPFAStereo:
                caps.setStereo(ivalue != 0);
                break;

              case CGL.NSOpenGLPFAColorSize:
                {
                  final int bitSize = ( 32 == ivalue ? 24 : ivalue ) / 3;
                  caps.setRedBits(bitSize);
                  caps.setGreenBits(bitSize);
                  caps.setBlueBits(bitSize);
                }
                break;

              case CGL.NSOpenGLPFAAlphaSize:
                // ALPHA shall be set at last - due to it's auto setting by !opaque / samples
                alphaBits = ivalue;
                break;

              case CGL.NSOpenGLPFADepthSize:
                caps.setDepthBits(ivalue);
                break;

              case CGL.NSOpenGLPFAAccumSize:
                {
                  final int bitSize = ivalue / 4;
                  caps.setAccumRedBits(bitSize);
                  caps.setAccumGreenBits(bitSize);
                  caps.setAccumBlueBits(bitSize);
                  caps.setAccumAlphaBits(bitSize);
                }
                break;

              case CGL.NSOpenGLPFAStencilSize:
                caps.setStencilBits(ivalue);
                break;

              case CGL.NSOpenGLPFASampleBuffers:
                caps.setSampleBuffers(ivalue != 0);
                break;

              case CGL.NSOpenGLPFASamples:
                caps.setNumSamples(ivalue);
                break;

              default:
                break;
          }
        }
        caps.setAlphaBits(alphaBits);

        return caps;
      }
}

