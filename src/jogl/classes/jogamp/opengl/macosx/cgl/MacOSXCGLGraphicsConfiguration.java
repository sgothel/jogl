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

import java.util.ArrayList;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.nativewindow.MutableGraphicsConfiguration;

import com.jogamp.common.nio.PointerBuffer;

public class MacOSXCGLGraphicsConfiguration extends MutableGraphicsConfiguration implements Cloneable {
    long pixelformat;

    MacOSXCGLGraphicsConfiguration(AbstractGraphicsScreen screen, 
                                   GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested,
                                   long pixelformat) {
        super(screen, capsChosen, capsRequested);
        this.pixelformat=pixelformat;
    }

    public Object clone() {
        return super.clone();
    }

    void setChosenPixelFormat(long pixelformat) {
        this.pixelformat=pixelformat;
    }

    protected static List<GLCapabilitiesImmutable> getAvailableCapabilities(MacOSXCGLDrawableFactory factory, AbstractGraphicsDevice device) {
        MacOSXCGLDrawableFactory.SharedResource sharedResource = factory.getOrCreateOSXSharedResource(device);
        if(null == sharedResource) {
            throw new GLException("Shared resource for device n/a: "+device);
        }
        // MacOSXGraphicsDevice osxDevice = sharedResource.getDevice();
        return new ArrayList<GLCapabilitiesImmutable>(0);
    }
    
    static final int[] cglInternalAttributeToken = new int[] {
        CGL.kCGLPFAOpenGLProfile,
        CGL.kCGLPFAColorFloat,
        CGL.NSOpenGLPFANoRecovery,
        CGL.NSOpenGLPFAAccelerated,
        CGL.NSOpenGLPFAPixelBuffer,
        CGL.NSOpenGLPFADoubleBuffer,
        CGL.NSOpenGLPFAStereo,
        CGL.NSOpenGLPFAColorSize,
        CGL.NSOpenGLPFAAlphaSize,
        CGL.NSOpenGLPFADepthSize,
        CGL.NSOpenGLPFAAccumSize,
        CGL.NSOpenGLPFAStencilSize,
        CGL.NSOpenGLPFASampleBuffers,
        CGL.NSOpenGLPFASamples };

    static int[] GLCapabilities2NSAttribList(GLCapabilitiesImmutable caps, int ctp, int major, int minor) {
        int len = cglInternalAttributeToken.length;
        int off = 0;
        if ( !MacOSXCGLContext.isLionOrLater ) {
            // no OpenGLProfile
            off++;
            len--;
        }        
        int[] ivalues = new int[len];

        for (int idx = 0; idx < len; idx++) {
          final int attr = cglInternalAttributeToken[idx+off];
          switch (attr) {
              case CGL.kCGLPFAOpenGLProfile: 
                ivalues[idx] = MacOSXCGLContext.GLProfile2CGLOGLProfileValue(ctp, major, minor);
                break;
              case CGL.kCGLPFAColorFloat:
                ivalues[idx] = caps.getPbufferFloatingPointBuffers() ? 1 : 0;
                break;

              case CGL.NSOpenGLPFANoRecovery:
                ivalues[idx] = caps.getHardwareAccelerated() ? 1 : 0;
                break;
              case CGL.NSOpenGLPFAAccelerated:
                ivalues[idx] = caps.getHardwareAccelerated() ? 1 : 0;
                break;
                  
              case CGL.NSOpenGLPFAPixelBuffer:
                ivalues[idx] = caps.isPBuffer() ? 1 : 0;
                break;

              case CGL.NSOpenGLPFADoubleBuffer:
                ivalues[idx] = (caps.getDoubleBuffered() ? 1 : 0);
                break;

              case CGL.NSOpenGLPFAStereo:
                ivalues[idx] = (caps.getStereo() ? 1 : 0);
                break;

              case CGL.NSOpenGLPFAColorSize:
                ivalues[idx] = (caps.getRedBits() + caps.getGreenBits() + caps.getBlueBits());
                break;

              case CGL.NSOpenGLPFAAlphaSize:
                ivalues[idx] = caps.getAlphaBits();
                break;

              case CGL.NSOpenGLPFADepthSize:
                ivalues[idx] = caps.getDepthBits();
                break;

              case CGL.NSOpenGLPFAAccumSize:
                ivalues[idx] = (caps.getAccumRedBits() + caps.getAccumGreenBits() + caps.getAccumBlueBits() + caps.getAccumAlphaBits());
                break;

              case CGL.NSOpenGLPFAStencilSize:
                ivalues[idx] = caps.getStencilBits();
                break;

              case CGL.NSOpenGLPFASampleBuffers:
                ivalues[idx] = caps.getSampleBuffers() ? 1 : 0;
                break;

              case CGL.NSOpenGLPFASamples:
                ivalues[idx] = caps.getSampleBuffers() ? ivalues[idx] = caps.getNumSamples() : 0;
                break;

              default:
                break;
          }
        }
        return ivalues;
    }

    static long GLCapabilities2NSPixelFormat(GLCapabilitiesImmutable caps, int ctp, int major, int minor) {
        int len = cglInternalAttributeToken.length;
        int off = 0;
        if ( !MacOSXCGLContext.isLionOrLater ) {
            // no OpenGLProfile
            off++;
            len--;
        }        
        int[] ivalues = GLCapabilities2NSAttribList(caps, ctp, major, minor);
        return CGL.createPixelFormat(cglInternalAttributeToken, off, len, ivalues, 0);
    }

    static GLCapabilitiesImmutable NSPixelFormat2GLCapabilities(GLProfile glp, long pixelFormat) {
        return PixelFormat2GLCapabilities(glp, pixelFormat, true);
    }

    static long GLCapabilities2CGLPixelFormat(GLCapabilitiesImmutable caps, int ctp, int major, int minor) {
      // Set up pixel format attributes
      int[] attrs = new int[256];
      int i = 0;
      if(MacOSXCGLContext.isLionOrLater) {
          attrs[i++] = CGL.kCGLPFAOpenGLProfile; 
          attrs[i++] = MacOSXCGLContext.GLProfile2CGLOGLProfileValue(ctp, major, minor);
      }
      if(caps.isPBuffer()) {
        attrs[i++] = CGL.kCGLPFAPBuffer;
      }
      if (caps.getPbufferFloatingPointBuffers()) {
        attrs[i++] = CGL.kCGLPFAColorFloat;
      }
      if (caps.getDoubleBuffered()) {
        attrs[i++] = CGL.kCGLPFADoubleBuffer;
      }
      if (caps.getStereo()) {
        attrs[i++] = CGL.kCGLPFAStereo;
      }
      attrs[i++] = CGL.kCGLPFAColorSize;
      attrs[i++] = (caps.getRedBits() +
                    caps.getGreenBits() +
                    caps.getBlueBits());
      attrs[i++] = CGL.kCGLPFAAlphaSize;
      attrs[i++] = caps.getAlphaBits();
      attrs[i++] = CGL.kCGLPFADepthSize;
      attrs[i++] = caps.getDepthBits();
      // FIXME: should validate stencil size as is done in MacOSXWindowSystemInterface.m
      attrs[i++] = CGL.kCGLPFAStencilSize;
      attrs[i++] = caps.getStencilBits();
      attrs[i++] = CGL.kCGLPFAAccumSize;
      attrs[i++] = (caps.getAccumRedBits() +
                    caps.getAccumGreenBits() +
                    caps.getAccumBlueBits() +
                    caps.getAccumAlphaBits());
      if (caps.getSampleBuffers()) {
        attrs[i++] = CGL.kCGLPFASampleBuffers;
        attrs[i++] = 1;
        attrs[i++] = CGL.kCGLPFASamples;
        attrs[i++] = caps.getNumSamples();
      }

      // Use attribute array to select pixel format
      PointerBuffer fmt = PointerBuffer.allocateDirect(1);
      int[] numScreens = new int[1];
      int res = CGL.CGLChoosePixelFormat(attrs, 0, fmt, numScreens, 0);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error code " + res + " while choosing pixel format");
      }
      return fmt.get(0);
    }
    
    static GLCapabilitiesImmutable CGLPixelFormat2GLCapabilities(long pixelFormat) {
        return PixelFormat2GLCapabilities(null, pixelFormat, false);
    }

    private static GLCapabilitiesImmutable PixelFormat2GLCapabilities(GLProfile glp, long pixelFormat, boolean nsUsage) {
        int len = cglInternalAttributeToken.length;
        int off = 0;
        if ( !MacOSXCGLContext.isLionOrLater ) {
            // no OpenGLProfile
            off++;
            len--;
        }        
        int[] ivalues = new int[len];

        // On this platform the pixel format is associated with the
        // context and not the drawable. However it's a reasonable
        // approximation to just store the chosen pixel format up in the
        // NativeSurface's AbstractGraphicsConfiguration, 
        // since the public API doesn't provide for a different GLCapabilities per context.
        // Note: These restrictions of the platform's API might be considered as a bug anyways.

        // Figure out what attributes we really got
        if(nsUsage) {
            CGL.queryPixelFormat(pixelFormat, cglInternalAttributeToken, off, len, ivalues, 0);
        } else {
            CGL.CGLQueryPixelFormat(pixelFormat, cglInternalAttributeToken, off, len, ivalues, 0);
        }
        if(null == glp && MacOSXCGLContext.isLionOrLater) {
            // pre-scan for OpenGL Profile
            for (int i = 0; i < len; i++) {
                if(CGL.kCGLPFAOpenGLProfile == cglInternalAttributeToken[i+off]) {
                    switch(ivalues[i]) {
                        case CGL.kCGLOGLPVersion_3_2_Core:
                            glp = GLProfile.get(GLProfile.GL3);
                            break;
                        case CGL.kCGLOGLPVersion_Legacy:
                            glp = GLProfile.get(GLProfile.GL2);
                            break;                            
                        default:
                            throw new RuntimeException("Unhandled OSX OpenGL Profile: 0x"+Integer.toHexString(ivalues[i]));
                    }
                }            
            }
        }
        if(null == glp) {
            glp = GLProfile.get(GLProfile.GL2);
        }
        GLCapabilities caps = new GLCapabilities(glp);
        for (int i = 0; i < len; i++) {
          int attr = cglInternalAttributeToken[i+off];
          switch (attr) {
              case CGL.kCGLPFAColorFloat:
                caps.setPbufferFloatingPointBuffers(ivalues[i] != 0);
                break;

              case CGL.NSOpenGLPFAAccelerated:
                caps.setHardwareAccelerated(ivalues[i] != 0);
                break;
                
              case CGL.NSOpenGLPFAPixelBuffer:
                caps.setPBuffer(ivalues[i] != 0);
                break;

              case CGL.NSOpenGLPFADoubleBuffer:
                caps.setDoubleBuffered(ivalues[i] != 0);
                break;

              case CGL.NSOpenGLPFAStereo:
                caps.setStereo(ivalues[i] != 0);
                break;

              case CGL.NSOpenGLPFAColorSize:
                {
                  int bitSize = ivalues[i];
                  if (bitSize == 32)
                    bitSize = 24;
                  bitSize /= 3;
                  caps.setRedBits(bitSize);
                  caps.setGreenBits(bitSize);
                  caps.setBlueBits(bitSize);
                }
                break;

              case CGL.NSOpenGLPFAAlphaSize:
                caps.setAlphaBits(ivalues[i]);
                break;

              case CGL.NSOpenGLPFADepthSize:
                caps.setDepthBits(ivalues[i]);
                break;

              case CGL.NSOpenGLPFAAccumSize:
                {
                  int bitSize = ivalues[i] / 4;
                  caps.setAccumRedBits(bitSize);
                  caps.setAccumGreenBits(bitSize);
                  caps.setAccumBlueBits(bitSize);
                  caps.setAccumAlphaBits(bitSize);
                }
                break;

              case CGL.NSOpenGLPFAStencilSize:
                caps.setStencilBits(ivalues[i]);
                break;

              case CGL.NSOpenGLPFASampleBuffers:
                caps.setSampleBuffers(ivalues[i] != 0);
                break;

              case CGL.NSOpenGLPFASamples:
                caps.setNumSamples(ivalues[i]);
                break;

              default:
                break;
          }
        }

        return caps;
      }
}

