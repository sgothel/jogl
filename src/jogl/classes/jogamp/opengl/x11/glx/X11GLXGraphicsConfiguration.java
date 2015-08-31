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
 */

package jogamp.opengl.x11.glx;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.x11.XRenderDirectFormat;
import jogamp.nativewindow.x11.XRenderPictFormat;
import jogamp.nativewindow.x11.XVisualInfo;
import jogamp.opengl.GLGraphicsConfigurationUtil;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.nativewindow.x11.X11GraphicsConfiguration;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;

public class X11GLXGraphicsConfiguration extends X11GraphicsConfiguration implements Cloneable {
    public static final int MAX_ATTRIBS = 128;
    private final GLCapabilitiesChooser chooser;

    X11GLXGraphicsConfiguration(final X11GraphicsScreen screen,
                                final X11GLCapabilities capsChosen, final GLCapabilitiesImmutable capsRequested, final GLCapabilitiesChooser chooser) {
        super(screen, capsChosen, capsRequested, capsChosen.getXVisualInfo());
        this.chooser=chooser;
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    public final long getFBConfig()   {
        return ((X11GLCapabilities)capabilitiesChosen).getFBConfig();
    }
    public final int  getFBConfigID() {
        return ((X11GLCapabilities)capabilitiesChosen).getFBConfigID();
    }
    public final boolean hasFBConfig() {
        return ((X11GLCapabilities)capabilitiesChosen).hasFBConfig();
    }

    void updateGraphicsConfiguration() {
        final CapabilitiesImmutable aChosenCaps = getChosenCapabilities();
        if( !(aChosenCaps instanceof X11GLCapabilities) || VisualIDHolder.VID_UNDEFINED == aChosenCaps.getVisualID(VIDType.X11_XVISUAL) ) {
            // This case is actually quite impossible, since on X11 the visualID and hence GraphicsConfiguration
            // must be determined _before_ window creation!
            final X11GLXGraphicsConfiguration newConfig = (X11GLXGraphicsConfiguration)
                GraphicsConfigurationFactory.getFactory(getScreen().getDevice(), aChosenCaps).chooseGraphicsConfiguration(
                    aChosenCaps, getRequestedCapabilities(), chooser, getScreen(), VisualIDHolder.VID_UNDEFINED);
            if(null!=newConfig) {
                // FIXME: setScreen( ... );
                setXVisualInfo(newConfig.getXVisualInfo());
                setChosenCapabilities(newConfig.getChosenCapabilities());
                if(DEBUG) {
                    System.err.println("X11GLXGraphicsConfiguration.updateGraphicsConfiguration updated:"+this);
                }
            } else {
                throw new GLException("No native VisualID pre-chosen and update failed: "+this);
            }
        } else if (DEBUG) {
            System.err.println("X11GLXGraphicsConfiguration.updateGraphicsConfiguration kept:"+this);
        }
    }

    static X11GLXGraphicsConfiguration create(GLProfile glp, final X11GraphicsScreen x11Screen, final int fbcfgID) {
      final X11GraphicsDevice device = (X11GraphicsDevice) x11Screen.getDevice();
      final long display = device.getHandle();
      if(0==display) {
          throw new GLException("Display null of "+x11Screen);
      }
      final int screen = x11Screen.getIndex();
      final long fbcfg = glXFBConfigID2FBConfig(display, screen, fbcfgID);
      if(0==fbcfg) {
          throw new GLException("FBConfig null of "+toHexString(fbcfgID));
      }
      if(null==glp) {
        glp = GLProfile.getDefault(x11Screen.getDevice());
      }
      final X11GLXDrawableFactory factory = (X11GLXDrawableFactory) GLDrawableFactory.getDesktopFactory();
      final X11GLCapabilities caps = GLXFBConfig2GLCapabilities(device, glp, fbcfg, GLGraphicsConfigurationUtil.ALL_BITS, factory.isGLXMultisampleAvailable(device));
      if(null==caps) {
          throw new GLException("GLCapabilities null of "+toHexString(fbcfg));
      }
      return new X11GLXGraphicsConfiguration(x11Screen, caps, caps, new DefaultGLCapabilitiesChooser());
    }

    static IntBuffer GLCapabilities2AttribList(final GLCapabilitiesImmutable caps,
                                               final boolean forFBAttr, final boolean isMultisampleAvailable,
                                               final long display, final int screen)
    {
        final int colorDepth = (caps.getRedBits() +
                          caps.getGreenBits() +
                          caps.getBlueBits());
        if (colorDepth < 15) {
          throw new GLException("Bit depths < 15 (i.e., non-true-color) not supported");
        }
        final IntBuffer res = Buffers.newDirectIntBuffer(MAX_ATTRIBS);
        int idx = 0;

        if (forFBAttr) {
          res.put(idx++, GLX.GLX_DRAWABLE_TYPE);

          final int surfaceType;
          if( caps.isOnscreen() ) {
              surfaceType = GLX.GLX_WINDOW_BIT;
          } else if( caps.isFBO() ) {
              surfaceType = GLX.GLX_WINDOW_BIT;  // native replacement!
          } else if( caps.isPBuffer() ) {
              surfaceType = GLX.GLX_PBUFFER_BIT;
          } else if( caps.isBitmap() ) {
              surfaceType = GLX.GLX_PIXMAP_BIT;
          } else {
              throw new GLException("no surface type set in caps: "+caps);
          }
          res.put(idx++, surfaceType);

          res.put(idx++, GLX.GLX_RENDER_TYPE);
          res.put(idx++, GLX.GLX_RGBA_BIT);
        } else {
          res.put(idx++, GLX.GLX_RGBA);
        }

        // FIXME: Still a bug is Mesa: PBUFFER && GLX_STEREO==GL_FALSE ?
        if (forFBAttr) {
            res.put(idx++, GLX.GLX_DOUBLEBUFFER);
            res.put(idx++, caps.getDoubleBuffered()?GL.GL_TRUE:GL.GL_FALSE);
            res.put(idx++, GLX.GLX_STEREO);
            res.put(idx++, caps.getStereo()?GL.GL_TRUE:GL.GL_FALSE);
            res.put(idx++, GLX.GLX_TRANSPARENT_TYPE);
            res.put(idx++, GLX.GLX_NONE);
            /**
            res.put(idx++, caps.isBackgroundOpaque()?GLX.GLX_NONE:GLX.GLX_TRANSPARENT_RGB;
            if(!caps.isBackgroundOpaque()) {
                res.put(idx++, GLX.GLX_TRANSPARENT_RED_VALUE);
                res.put(idx++, caps.getTransparentRedValue()>=0?caps.getTransparentRedValue():(int)GLX.GLX_DONT_CARE);
                res.put(idx++, GLX.GLX_TRANSPARENT_GREEN_VALUE);
                res.put(idx++, caps.getTransparentGreenValue()>=0?caps.getTransparentGreenValue():(int)GLX.GLX_DONT_CARE);
                res.put(idx++, GLX.GLX_TRANSPARENT_BLUE_VALUE);
                res.put(idx++, caps.getTransparentBlueValue()>=0?caps.getTransparentBlueValue():(int)GLX.GLX_DONT_CARE);
                res.put(idx++, GLX.GLX_TRANSPARENT_ALPHA_VALUE);
                res.put(idx++, caps.getTransparentAlphaValue()>=0?caps.getTransparentAlphaValue():(int)GLX.GLX_DONT_CARE);
            } */
        } else {
            if (caps.getDoubleBuffered()) {
              res.put(idx++, GLX.GLX_DOUBLEBUFFER);
            }
            if (caps.getStereo()) {
              res.put(idx++, GLX.GLX_STEREO);
            }
        }

        res.put(idx++, GLX.GLX_RED_SIZE);
        res.put(idx++, caps.getRedBits());
        res.put(idx++, GLX.GLX_GREEN_SIZE);
        res.put(idx++, caps.getGreenBits());
        res.put(idx++, GLX.GLX_BLUE_SIZE);
        res.put(idx++, caps.getBlueBits());
        if(caps.getAlphaBits()>0) {
            res.put(idx++, GLX.GLX_ALPHA_SIZE);
            res.put(idx++, caps.getAlphaBits());
        }
        if (caps.getStencilBits() > 0) {
          res.put(idx++, GLX.GLX_STENCIL_SIZE);
          res.put(idx++, caps.getStencilBits());
        }
        res.put(idx++, GLX.GLX_DEPTH_SIZE);
        res.put(idx++, caps.getDepthBits());
        if (caps.getAccumRedBits()   > 0 ||
            caps.getAccumGreenBits() > 0 ||
            caps.getAccumBlueBits()  > 0 ||
            caps.getAccumAlphaBits() > 0) {
          res.put(idx++, GLX.GLX_ACCUM_RED_SIZE);
          res.put(idx++, caps.getAccumRedBits());
          res.put(idx++, GLX.GLX_ACCUM_GREEN_SIZE);
          res.put(idx++, caps.getAccumGreenBits());
          res.put(idx++, GLX.GLX_ACCUM_BLUE_SIZE);
          res.put(idx++, caps.getAccumBlueBits());
          res.put(idx++, GLX.GLX_ACCUM_ALPHA_SIZE);
          res.put(idx++, caps.getAccumAlphaBits());
        }
        if (isMultisampleAvailable && caps.getSampleBuffers()) {
          res.put(idx++, GLX.GLX_SAMPLE_BUFFERS);
          res.put(idx++, GL.GL_TRUE);
          res.put(idx++, GLX.GLX_SAMPLES);
          res.put(idx++, caps.getNumSamples());
        }
        res.put(idx++, 0);
        return res;
  }

  // FBConfig

  static boolean GLXFBConfigIDValid(final long display, final int screen, final int fbcfgid) {
    final long fbcfg = X11GLXGraphicsConfiguration.glXFBConfigID2FBConfig(display, screen, fbcfgid);
    return (0 != fbcfg) ? X11GLXGraphicsConfiguration.GLXFBConfigValid( display, fbcfg ) : false ;
  }

  static boolean GLXFBConfigValid(final long display, final long fbcfg) {
    final IntBuffer tmp = Buffers.newDirectIntBuffer(1);
    if(GLX.GLX_BAD_ATTRIBUTE == GLX.glXGetFBConfigAttrib(display, fbcfg, GLX.GLX_RENDER_TYPE, tmp)) {
      return false;
    }
    return true;
  }

  static int FBCfgDrawableTypeBits(final X11GraphicsDevice device, final long fbcfg) {
    int val = 0;

    final IntBuffer tmp = Buffers.newDirectIntBuffer(1);
    if( !glXGetFBConfig(device.getHandle(), fbcfg, GLX.GLX_DRAWABLE_TYPE, tmp) ) {
        if(DEBUG) {
            System.err.println("X11GLXGraphicsConfiguration.FBCfgDrawableTypeBits: FBConfig invalid: fbcfg: "+toHexString(fbcfg));
        }
        return 0;
    }
    final int fbtype = tmp.get(0);

    if ( 0 != ( fbtype & GLX.GLX_WINDOW_BIT ) ) {
        val |= GLGraphicsConfigurationUtil.WINDOW_BIT |
               GLGraphicsConfigurationUtil.FBO_BIT;
    }
    if ( 0 != ( fbtype & GLX.GLX_PIXMAP_BIT ) ) {
        val |= GLGraphicsConfigurationUtil.BITMAP_BIT;
    }
    if ( 0 != ( fbtype & GLX.GLX_PBUFFER_BIT ) ) {
        val |= GLGraphicsConfigurationUtil.PBUFFER_BIT;
    }
    return val;
  }

  static X11GLCapabilities GLXFBConfig2GLCapabilities(final X11GraphicsDevice device, final GLProfile glp, final long fbcfg,
                                                      final int winattrmask, final boolean isMultisampleAvailable) {
    final IntBuffer tmp = Buffers.newDirectIntBuffer(1);
    final XRenderPictFormat xRenderPictFormat= XRenderPictFormat.create();
    return GLXFBConfig2GLCapabilities(device, glp, fbcfg, winattrmask, isMultisampleAvailable, tmp, xRenderPictFormat);
  }

  static List<GLCapabilitiesImmutable> GLXFBConfig2GLCapabilities(final X11GraphicsDevice device, final GLProfile glp, final PointerBuffer fbcfgsL,
                                                                  final int winattrmask, final boolean isMultisampleAvailable, final boolean onlyFirstValid) {
    final IntBuffer tmp = Buffers.newDirectIntBuffer(1);
    final XRenderPictFormat xRenderPictFormat= XRenderPictFormat.create();
    final List<GLCapabilitiesImmutable> result = new ArrayList<GLCapabilitiesImmutable>();
    for (int i = 0; i < fbcfgsL.limit(); i++) {
        final long fbcfg = fbcfgsL.get(i);
        final GLCapabilitiesImmutable c = GLXFBConfig2GLCapabilities(device, glp, fbcfg, winattrmask, isMultisampleAvailable, tmp, xRenderPictFormat);
        if( null != c ) {
            result.add(c);
            if( onlyFirstValid ) {
                break;
            }
        }
    }
    return result;
  }
  static X11GLCapabilities GLXFBConfig2GLCapabilities(final X11GraphicsDevice device, final GLProfile glp, final long fbcfg,
                                                      final int winattrmask, final boolean isMultisampleAvailable,
                                                      final IntBuffer tmp, final XRenderPictFormat xRenderPictFormat) {
    final long display = device.getHandle();
    final int allDrawableTypeBits = FBCfgDrawableTypeBits(device, fbcfg);
    int drawableTypeBits = winattrmask & allDrawableTypeBits;
    if( 0 == allDrawableTypeBits || 0 == drawableTypeBits ) {
      if(DEBUG) {
          System.err.println("X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities: zero drawablebits: allDrawableTypeBits: "+toHexString(allDrawableTypeBits)+", drawableTypeBits "+toHexString(drawableTypeBits));
      }
      return null;
    }
    final int fbcfgid = X11GLXGraphicsConfiguration.glXFBConfig2FBConfigID(display, fbcfg);
    if( VisualIDHolder.VID_UNDEFINED == fbcfgid ) {
      if(DEBUG) {
          System.err.println("X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities: FBConfig invalid (0): fbcfg: "+toHexString(fbcfg));
      }
      return null;
    }
    final XVisualInfo visualInfo = GLX.glXGetVisualFromFBConfig(display, fbcfg);
    if(null == visualInfo) {
        if(DEBUG) {
            System.err.println("X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities: Null XVisualInfo for FBConfigID 0x" + Integer.toHexString(fbcfgid));
        }
        // onscreen must have an XVisualInfo
        drawableTypeBits &= ~(GLGraphicsConfigurationUtil.WINDOW_BIT | GLGraphicsConfigurationUtil.FBO_BIT);
    }
    if( 0 == drawableTypeBits ) {
      if(DEBUG) {
          System.err.println("X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities: zero drawablebits: winattrmask: "+toHexString(winattrmask)+", offscreen "+(null == visualInfo));
      }
      return null;
    }

    if(GLX.GLX_BAD_ATTRIBUTE == GLX.glXGetFBConfigAttrib(display, fbcfg, GLX.GLX_RENDER_TYPE, tmp)) {
      if(DEBUG) {
          System.err.println("X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities: FBConfig invalid (1): fbcfg: "+toHexString(fbcfg));
      }
      return null;
    }
    if( 0 == ( GLX.GLX_RGBA_BIT & tmp.get(0) ) ) {
      // no RGBA -> color index not supported
      if(DEBUG) {
          System.err.println("X11GLXGraphicsConfiguration.GLXFBConfig2GLCapabilities: FBConfig not RGBA (2): fbcfg: "+toHexString(fbcfg));
      }
      return null;
    }

    final X11GLCapabilities caps = new X11GLCapabilities(visualInfo, fbcfg, fbcfgid, glp);

    final XRenderDirectFormat xrmask = ( null != visualInfo ) ?
                                         XVisual2XRenderMask( display, visualInfo.getVisual(), xRenderPictFormat) :
                                         null ;

    final int _attributes[] = {
            GLX.GLX_SAMPLE_BUFFERS,
            GLX.GLX_SAMPLES,
            GLX.GLX_DOUBLEBUFFER,
            GLX.GLX_STEREO,
            GLX.GLX_CONFIG_CAVEAT,
            GLX.GLX_RED_SIZE,
            GLX.GLX_GREEN_SIZE,
            GLX.GLX_BLUE_SIZE,
            GLX.GLX_ALPHA_SIZE,
            GLX.GLX_ACCUM_RED_SIZE,
            GLX.GLX_ACCUM_GREEN_SIZE,
            GLX.GLX_ACCUM_BLUE_SIZE,
            GLX.GLX_ACCUM_ALPHA_SIZE,
            GLX.GLX_DEPTH_SIZE,
            GLX.GLX_STENCIL_SIZE
    };
    final int offset = isMultisampleAvailable ? 0 : 2;
    final IntBuffer attributes = Buffers.newDirectIntBuffer(_attributes);
    attributes.position(offset);
    final IntBuffer values = Buffers.newDirectIntBuffer(attributes.remaining());
    final int err = GLX.glXGetFBConfigAttributes(display, fbcfg, attributes, values);
    if (0 != err) {
      throw new GLException("glXGetFBConfig("+toHexString(attributes.get(offset+values.get(0)))+") failed: error code " + glXGetFBConfigErrorCode(err));
    }
    int j=0;
    if (isMultisampleAvailable) {
      caps.setSampleBuffers(values.get(j++) != 0);
      caps.setNumSamples   (values.get(j++));
    }
    final int alphaMask = ( null != xrmask ) ? xrmask.getAlphaMask() : 0;
    caps.setBackgroundOpaque( 0 >= alphaMask );
    if( !caps.isBackgroundOpaque() ) {
        caps.setTransparentRedValue(xrmask.getRedMask());
        caps.setTransparentGreenValue(xrmask.getGreenMask());
        caps.setTransparentBlueValue(xrmask.getBlueMask());
        caps.setTransparentAlphaValue(alphaMask);
    }
    // ALPHA shall be set at last - due to it's auto setting by the above (!opaque / samples)
    caps.setDoubleBuffered(values.get(j++) != 0);
    caps.setStereo        (values.get(j++) != 0);
    caps.setHardwareAccelerated(values.get(j++) != GLX.GLX_SLOW_CONFIG);
    caps.setRedBits       (values.get(j++));
    caps.setGreenBits     (values.get(j++));
    caps.setBlueBits      (values.get(j++));
    caps.setAlphaBits     (values.get(j++));
    caps.setAccumRedBits  (values.get(j++));
    caps.setAccumGreenBits(values.get(j++));
    caps.setAccumBlueBits (values.get(j++));
    caps.setAccumAlphaBits(values.get(j++));
    caps.setDepthBits     (values.get(j++));
    caps.setStencilBits   (values.get(j++));

    return (X11GLCapabilities) GLGraphicsConfigurationUtil.fixWinAttribBitsAndHwAccel(device, drawableTypeBits, caps);
  }

  private static String glXGetFBConfigErrorCode(final int err) {
    switch (err) {
      case GLX.GLX_NO_EXTENSION:  return "GLX_NO_EXTENSION";
      case GLX.GLX_BAD_ATTRIBUTE: return "GLX_BAD_ATTRIBUTE";
      default:                return "Unknown error code " + err;
    }
  }

  static boolean glXGetFBConfig(final long display, final long cfg, final int attrib, final IntBuffer tmp) {
    if (display == 0) {
      throw new GLException("No display connection");
    }
    final boolean res = GLX.GLX_BAD_ATTRIBUTE != GLX.glXGetFBConfigAttrib(display, cfg, attrib, tmp);
    if( !res ) {
        if(DEBUG) {
            System.err.println("X11GLXGraphicsConfiguration.glXGetFBConfig: FBConfig invalid: fbcfg: "+toHexString(cfg));
        }
    }
    return res;
  }

  static int glXFBConfig2FBConfigID(final long display, final long cfg) {
      final IntBuffer tmpID = Buffers.newDirectIntBuffer(1);
      if( glXGetFBConfig(display, cfg, GLX.GLX_FBCONFIG_ID, tmpID) ) {
          return tmpID.get(0);
      } else {
          return VisualIDHolder.VID_UNDEFINED; // error
      }
  }

  static long glXFBConfigID2FBConfig(final long display, final int screen, final int id) {
      final IntBuffer attribs = Buffers.newDirectIntBuffer(new int[] { GLX.GLX_FBCONFIG_ID, id, 0 });
      final IntBuffer count = Buffers.newDirectIntBuffer(1);
      count.put(0, -1);
      final PointerBuffer fbcfgsL = GLX.glXChooseFBConfig(display, screen, attribs, count);
      if (fbcfgsL == null || fbcfgsL.limit()<1) {
          return 0;
      }
      return fbcfgsL.get(0);
  }

  // Visual Info

  static XVisualInfo XVisualID2XVisualInfo(final long display, final long visualID) {
      final int[] count = new int[1];
      final XVisualInfo template = XVisualInfo.create();
      template.setVisualid(visualID);
      final XVisualInfo[] infos = X11Lib.XGetVisualInfo(display, X11Lib.VisualIDMask, template, count, 0);
      if (infos == null || infos.length == 0) {
            return null;
      }
      final XVisualInfo res = XVisualInfo.create(infos[0]);
      if (DEBUG) {
        System.err.println("Fetched XVisualInfo for visual ID " + toHexString(visualID));
        System.err.println("Resulting XVisualInfo: visualid = " + toHexString(res.getVisualid()));
      }
      return res;
  }

  static X11GLCapabilities XVisualInfo2GLCapabilities(final X11GraphicsDevice device, final GLProfile glp, final XVisualInfo info,
                                                      final int winattrmask, final boolean isMultisampleEnabled) {
    final int allDrawableTypeBits = GLGraphicsConfigurationUtil.WINDOW_BIT |
                                    GLGraphicsConfigurationUtil.BITMAP_BIT |
                                    GLGraphicsConfigurationUtil.FBO_BIT ;

    final int drawableTypeBits = winattrmask & allDrawableTypeBits;

    if( 0 == drawableTypeBits ) {
        return null;
    }

    final long display = device.getHandle();
    final IntBuffer tmp = Buffers.newDirectIntBuffer(1);
    int val = glXGetConfig(display, info, GLX.GLX_USE_GL, tmp);
    if (val == 0) {
      if(DEBUG) {
        System.err.println("Visual ("+toHexString(info.getVisualid())+") does not support OpenGL");
      }
      return null;
    }
    val = glXGetConfig(display, info, GLX.GLX_RGBA, tmp);
    if (val == 0) {
      if(DEBUG) {
        System.err.println("Visual ("+toHexString(info.getVisualid())+") does not support RGBA");
      }
      return null;
    }

    final GLCapabilities res = new X11GLCapabilities(info, glp);

    res.setDoubleBuffered(glXGetConfig(display, info, GLX.GLX_DOUBLEBUFFER,     tmp) != 0);
    res.setStereo        (glXGetConfig(display, info, GLX.GLX_STEREO,           tmp) != 0);
    // Note: use of hardware acceleration is determined by
    // glXCreateContext, not by the XVisualInfo. Optimistically claim
    // that all GLCapabilities have the capability to be hardware
    // accelerated.
    if (isMultisampleEnabled) {
      res.setSampleBuffers(glXGetConfig(display, info, GLX.GLX_SAMPLE_BUFFERS, tmp) != 0);
      res.setNumSamples   (glXGetConfig(display, info, GLX.GLX_SAMPLES,        tmp));
    }
    final XRenderDirectFormat xrmask = ( null != info ) ?
                                         XVisual2XRenderMask( display, info.getVisual() ) :
                                         null ;
    final int alphaMask = ( null != xrmask ) ? xrmask.getAlphaMask() : 0;
    res.setBackgroundOpaque( 0 >= alphaMask );
    if( !res.isBackgroundOpaque() ) {
        res.setTransparentRedValue(xrmask.getRedMask());
        res.setTransparentGreenValue(xrmask.getGreenMask());
        res.setTransparentBlueValue(xrmask.getBlueMask());
        res.setTransparentAlphaValue(alphaMask);
    }
    // ALPHA shall be set at last - due to it's auto setting by the above (!opaque / samples)
    res.setHardwareAccelerated(true);
    res.setDepthBits     (glXGetConfig(display, info, GLX.GLX_DEPTH_SIZE,       tmp));
    res.setStencilBits   (glXGetConfig(display, info, GLX.GLX_STENCIL_SIZE,     tmp));
    res.setRedBits       (glXGetConfig(display, info, GLX.GLX_RED_SIZE,         tmp));
    res.setGreenBits     (glXGetConfig(display, info, GLX.GLX_GREEN_SIZE,       tmp));
    res.setBlueBits      (glXGetConfig(display, info, GLX.GLX_BLUE_SIZE,        tmp));
    res.setAlphaBits     (glXGetConfig(display, info, GLX.GLX_ALPHA_SIZE,       tmp));
    res.setAccumRedBits  (glXGetConfig(display, info, GLX.GLX_ACCUM_RED_SIZE,   tmp));
    res.setAccumGreenBits(glXGetConfig(display, info, GLX.GLX_ACCUM_GREEN_SIZE, tmp));
    res.setAccumBlueBits (glXGetConfig(display, info, GLX.GLX_ACCUM_BLUE_SIZE,  tmp));
    res.setAccumAlphaBits(glXGetConfig(display, info, GLX.GLX_ACCUM_ALPHA_SIZE, tmp));

    return (X11GLCapabilities) GLGraphicsConfigurationUtil.fixWinAttribBitsAndHwAccel(device, drawableTypeBits, res);
  }

  private static String glXGetConfigErrorCode(final int err) {
    switch (err) {
      case GLX.GLX_NO_EXTENSION:  return "GLX_NO_EXTENSION";
      case GLX.GLX_BAD_SCREEN:    return "GLX_BAD_SCREEN";
      case GLX.GLX_BAD_ATTRIBUTE: return "GLX_BAD_ATTRIBUTE";
      case GLX.GLX_BAD_VISUAL:    return "GLX_BAD_VISUAL";
      default:                return "Unknown error code " + err;
    }
  }

  static int glXGetConfig(final long display, final XVisualInfo info, final int attrib, final IntBuffer tmp) {
    if (display == 0) {
      throw new GLException("No display connection");
    }
    final int res = GLX.glXGetConfig(display, info, attrib, tmp);
    if (res != 0) {
      throw new GLException("glXGetConfig("+toHexString(attrib)+") failed: error code " + glXGetConfigErrorCode(res));
    }
    return tmp.get(tmp.position());
  }

  @Override
  public String toString() {
    return "X11GLXGraphicsConfiguration["+getScreen()+", visualID " + toHexString(getXVisualID()) + ", fbConfigID " + toHexString(getFBConfigID()) +
                                        ",\n\trequested " + getRequestedCapabilities()+
                                        ",\n\tchosen    " + getChosenCapabilities()+
                                        "]";
  }
}

