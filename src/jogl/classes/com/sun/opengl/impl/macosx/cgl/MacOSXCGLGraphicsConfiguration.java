/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.opengl.impl.macosx.cgl;

import java.util.*;
import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.gluegen.runtime.NativeLibrary;

public class MacOSXCGLGraphicsConfiguration extends DefaultGraphicsConfiguration implements Cloneable {
    long pixelformat;

    public MacOSXCGLGraphicsConfiguration(AbstractGraphicsScreen screen, GLCapabilities capsChosen, GLCapabilities capsRequested,
                                          long pixelformat) {
        super(screen, capsChosen, capsRequested);
        this.pixelformat=pixelformat;
    }

    public Object clone() {
        return super.clone();
    }

    protected void setChosenPixelFormat(long pixelformat) {
        this.pixelformat=pixelformat;
    }

    protected void setChosenCapabilities(GLCapabilities caps) {
        super.setChosenCapabilities(caps);
    }

    protected static final int[] cglInternalAttributeToken = new int[] {
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
        CGL.NSOpenGLPFASamples };

    protected static int[] GLCapabilities2AttribList(GLCapabilities caps) {
        int[] ivalues = new int[cglInternalAttributeToken.length];

        for (int idx = 0; idx < cglInternalAttributeToken.length; idx++) {
          int attr = cglInternalAttributeToken[idx];
          switch (attr) {
              case CGL.kCGLPFAColorFloat:
                ivalues[idx] = caps.getPbufferFloatingPointBuffers() ? 1 : 0;
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

    protected static long GLCapabilities2NSPixelFormat(GLCapabilities caps) {
        int[] ivalues = GLCapabilities2AttribList(caps);
        return CGL.createPixelFormat(cglInternalAttributeToken, 0, cglInternalAttributeToken.length, ivalues, 0);
    }

    protected static GLCapabilities NSPixelFormat2GLCapabilities(GLProfile glp, long pixelFormat) {
        return PixelFormat2GLCapabilities(glp, pixelFormat, true);
    }

    protected static GLCapabilities CGLPixelFormat2GLCapabilities(GLProfile glp, long pixelFormat) {
        return PixelFormat2GLCapabilities(glp, pixelFormat, false);
    }

    private static GLCapabilities PixelFormat2GLCapabilities(GLProfile glp, long pixelFormat, boolean nsUsage) {
        int[] ivalues = new int[cglInternalAttributeToken.length];

        // On this platform the pixel format is associated with the
        // context and not the drawable. However it's a reasonable
        // approximation to just store the chosen pixel format up in the
        // NativeWindow's AbstractGraphicsConfiguration, 
        // since the public API doesn't provide for a different GLCapabilities per context.
        // Note: These restrictions of the platform's API might be considered as a bug anyways.

        // Figure out what attributes we really got
        GLCapabilities caps = new GLCapabilities(glp);
        if(nsUsage) {
            CGL.queryPixelFormat(pixelFormat, cglInternalAttributeToken, 0, cglInternalAttributeToken.length, ivalues, 0);
        } else {
            CGL.CGLQueryPixelFormat(pixelFormat, cglInternalAttributeToken, 0, cglInternalAttributeToken.length, ivalues, 0);
        }
        for (int i = 0; i < cglInternalAttributeToken.length; i++) {
          int attr = cglInternalAttributeToken[i];
          switch (attr) {
              case CGL.kCGLPFAColorFloat:
                caps.setPbufferFloatingPointBuffers(ivalues[i] != 0);
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

