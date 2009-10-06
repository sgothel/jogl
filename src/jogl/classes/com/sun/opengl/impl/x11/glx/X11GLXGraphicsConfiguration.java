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
 */

package com.sun.opengl.impl.x11.glx;

import java.util.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.x11.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.gluegen.runtime.NativeLibrary;
import com.sun.gluegen.runtime.PointerBuffer;
import com.sun.nativewindow.impl.x11.*;

public class X11GLXGraphicsConfiguration extends X11GraphicsConfiguration implements Cloneable {
    protected static final boolean DEBUG = Debug.debug("GraphicsConfiguration");
    
    public static final int MAX_ATTRIBS = 128;
    private long fbConfig;
    private int  fbConfigID;
    private GLCapabilitiesChooser chooser; 

    public X11GLXGraphicsConfiguration(X11GraphicsScreen screen, 
                                       GLCapabilities capsChosen, GLCapabilities capsRequested, GLCapabilitiesChooser chooser,
                                       XVisualInfo info, long fbcfg, int fbcfgID) {
        super(screen, capsChosen, capsRequested, info);
        this.chooser=chooser;
        fbConfig = fbcfg;
        fbConfigID = fbcfgID;
    }

    public static X11GLXGraphicsConfiguration create(GLProfile glp, X11GraphicsScreen x11Screen, int fbcfgID) {
      long display = x11Screen.getDevice().getHandle();
      if(0==display) {
          throw new GLException("Display null of "+x11Screen);
      }
      int screen = x11Screen.getIndex();
      long fbcfg = glXFBConfigID2FBConfig(display, screen, fbcfgID);
      if(0==fbcfg) {
          throw new GLException("FBConfig null of 0x"+Integer.toHexString(fbcfgID));
      }
      if(null==glp) {
        glp = GLProfile.getDefault();
      }
      GLCapabilities caps = GLXFBConfig2GLCapabilities(glp, display, fbcfg, true, true, true, GLXUtil.isMultisampleAvailable(display));
      if(null==caps) {
          throw new GLException("GLCapabilities null of 0x"+Long.toHexString(fbcfg));
      }
      XVisualInfo xvi = GLX.glXGetVisualFromFBConfigCopied(display, fbcfg);
      if(null==xvi) {
          throw new GLException("XVisualInfo null of 0x"+Long.toHexString(fbcfg));
      }
      return new X11GLXGraphicsConfiguration(x11Screen, caps, caps, new DefaultGLCapabilitiesChooser(), xvi, fbcfg, fbcfgID);
    }

    public Object clone() {
        return super.clone();
    }

    public long getFBConfig()   { return fbConfig; }
    public int  getFBConfigID() { return fbConfigID; }

    protected void updateGraphicsConfiguration() {
        X11GLXGraphicsConfiguration newConfig = (X11GLXGraphicsConfiguration)
            GraphicsConfigurationFactory.getFactory(getScreen().getDevice()).chooseGraphicsConfiguration(getRequestedCapabilities(),
                                                                                                         chooser,
                                                                                                         getScreen());
        if(null!=newConfig) {
            // FIXME: setScreen( ... );
            setXVisualInfo(newConfig.getXVisualInfo());
            setChosenCapabilities(newConfig.getChosenCapabilities());
            fbConfig = newConfig.getFBConfig();
            fbConfigID = newConfig.getFBConfigID();
            if(DEBUG) {
                System.err.println("!!! updateGraphicsConfiguration: "+this);
            }
        }
    }

    public static int[] GLCapabilities2AttribList(GLCapabilities caps,
                                                  boolean forFBAttr,
                                                  boolean isMultisampleAvailable,
                                                  long display,
                                                  int screen) 
    {
        int colorDepth = (caps.getRedBits() +
                          caps.getGreenBits() +
                          caps.getBlueBits());
        if (colorDepth < 15) {
          throw new GLException("Bit depths < 15 (i.e., non-true-color) not supported");
        }
        int[] res = new int[MAX_ATTRIBS];
        int idx = 0;

        if (forFBAttr) {
          res[idx++] = GLX.GLX_DRAWABLE_TYPE;
          res[idx++] = caps.isOnscreen() ? ( GLX.GLX_WINDOW_BIT ) : ( caps.isPBuffer() ? GLX.GLX_PBUFFER_BIT : GLX.GLX_PIXMAP_BIT ) ;
        }

        if (forFBAttr) {
          res[idx++] = GLX.GLX_RENDER_TYPE;
          res[idx++] = GLX.GLX_RGBA_BIT;
        } else {
          res[idx++] = GLX.GLX_RGBA;
        }

        // FIXME: Still a bug is Mesa: PBUFFER && GLX_STEREO==GL_FALSE ?
        if (forFBAttr) {
            res[idx++] = GLX.GLX_DOUBLEBUFFER;
            res[idx++] = caps.getDoubleBuffered()?GL.GL_TRUE:GL.GL_FALSE;
            res[idx++] = GLX.GLX_STEREO;
            res[idx++] = caps.getStereo()?GL.GL_TRUE:GL.GL_FALSE;
            res[idx++] = GLX.GLX_TRANSPARENT_TYPE;
            res[idx++] = caps.isBackgroundOpaque()?GLX.GLX_NONE:GLX.GLX_TRANSPARENT_RGB;
            if(!caps.isBackgroundOpaque()) {
                res[idx++] = GLX.GLX_TRANSPARENT_RED_VALUE;
                res[idx++] = caps.getTransparentRedValue()>=0?caps.getTransparentRedValue():(int)GLX.GLX_DONT_CARE;
                res[idx++] = GLX.GLX_TRANSPARENT_GREEN_VALUE;
                res[idx++] = caps.getTransparentGreenValue()>=0?caps.getTransparentGreenValue():(int)GLX.GLX_DONT_CARE;
                res[idx++] = GLX.GLX_TRANSPARENT_BLUE_VALUE;
                res[idx++] = caps.getTransparentBlueValue()>=0?caps.getTransparentBlueValue():(int)GLX.GLX_DONT_CARE;
                res[idx++] = GLX.GLX_TRANSPARENT_ALPHA_VALUE;
                res[idx++] = caps.getTransparentAlphaValue()>=0?caps.getTransparentAlphaValue():(int)GLX.GLX_DONT_CARE;
            }
        } else {
            if (caps.getDoubleBuffered()) {
              res[idx++] = GLX.GLX_DOUBLEBUFFER;
            }
            if (caps.getStereo()) {
              res[idx++] = GLX.GLX_STEREO;
            }
        }

        res[idx++] = GLX.GLX_RED_SIZE;
        res[idx++] = caps.getRedBits();
        res[idx++] = GLX.GLX_GREEN_SIZE;
        res[idx++] = caps.getGreenBits();
        res[idx++] = GLX.GLX_BLUE_SIZE;
        res[idx++] = caps.getBlueBits();
        res[idx++] = GLX.GLX_ALPHA_SIZE;
        res[idx++] = caps.getAlphaBits();
        res[idx++] = GLX.GLX_DEPTH_SIZE;
        res[idx++] = caps.getDepthBits();
        if (caps.getStencilBits() > 0) {
          res[idx++] = GLX.GLX_STENCIL_SIZE;
          res[idx++] = caps.getStencilBits();
        }
        if (caps.getAccumRedBits()   > 0 ||
            caps.getAccumGreenBits() > 0 ||
            caps.getAccumBlueBits()  > 0 ||
            caps.getAccumAlphaBits() > 0) {
          res[idx++] = GLX.GLX_ACCUM_RED_SIZE;
          res[idx++] = caps.getAccumRedBits();
          res[idx++] = GLX.GLX_ACCUM_GREEN_SIZE;
          res[idx++] = caps.getAccumGreenBits();
          res[idx++] = GLX.GLX_ACCUM_BLUE_SIZE;
          res[idx++] = caps.getAccumBlueBits();
          res[idx++] = GLX.GLX_ACCUM_ALPHA_SIZE;
          res[idx++] = caps.getAccumAlphaBits();
        }
        if (isMultisampleAvailable && caps.getSampleBuffers()) {
          res[idx++] = GLX.GLX_SAMPLE_BUFFERS;
          res[idx++] = GL.GL_TRUE;
          res[idx++] = GLX.GLX_SAMPLES;
          res[idx++] = caps.getNumSamples();
        }
        if (caps.isPBuffer()) {
          if (caps.getPbufferFloatingPointBuffers()) {
            String glXExtensions = GLX.glXQueryExtensionsString(display, screen);
            if (glXExtensions == null ||
                glXExtensions.indexOf("GLX_NV_float_buffer") < 0) {
              throw new GLException("Floating-point pbuffers on X11 currently require NVidia hardware: "+glXExtensions);
            }
            res[idx++] = GLXExt.GLX_FLOAT_COMPONENTS_NV;
            res[idx++] = GL.GL_TRUE;
          }
        }
        res[idx++] = 0;
        return res;
  }

  // FBConfig

  public static boolean GLXFBConfigValid(long display, long fbcfg) {
    int[] tmp = new int[1];
    if(GLX.GLX_BAD_ATTRIBUTE == GLX.glXGetFBConfigAttrib(display, fbcfg, GLX.GLX_RENDER_TYPE, tmp, 0)) {
      return false;
    }
    return true;
  }

  public static boolean GLXFBConfigDrawableTypeVerify(int val, boolean onscreen, boolean usePBuffer) {
    boolean res;

    if ( onscreen ) {
        res = ( 0 != (val & GLX.GLX_WINDOW_BIT) ) ;
    } else {
        res = ( 0 != (val & GLX.GLX_PIXMAP_BIT) ) || usePBuffer ;
    }
    if ( usePBuffer ) {
        res = res && ( 0 != (val & GLX.GLX_PBUFFER_BIT) ) ;
    }

    return res;
  }

  public static GLCapabilities GLXFBConfig2GLCapabilities(GLProfile glp, long display, long fbcfg, 
                                                          boolean relaxed, boolean onscreen, boolean usePBuffer, boolean isMultisampleEnabled) {
    int[] tmp = new int[1];
    int val;
    val = glXGetFBConfig(display, fbcfg, GLX.GLX_RENDER_TYPE, tmp, 0);
    if (val != GLX.GLX_RGBA_BIT) {
      throw new GLException("Visual does not support RGBA");
    }
    GLCapabilities res = new GLCapabilities(glp);

    val = glXGetFBConfig(display, fbcfg, GLX.GLX_DRAWABLE_TYPE, tmp, 0);
    if(GLXFBConfigDrawableTypeVerify(val, onscreen, usePBuffer)) {
        res.setOnscreen(onscreen);
        res.setPBuffer(usePBuffer);
    } else if(relaxed) {
        res.setOnscreen( 0 != (val & GLX.GLX_WINDOW_BIT) );
        res.setPBuffer ( 0 != (val & GLX.GLX_PBUFFER_BIT) );
    } else {
        throw new GLException("GLX_DRAWABLE_TYPE does not match !!!");
    }
    res.setDoubleBuffered(glXGetFBConfig(display, fbcfg, GLX.GLX_DOUBLEBUFFER,     tmp, 0) != 0);
    res.setStereo        (glXGetFBConfig(display, fbcfg, GLX.GLX_STEREO,           tmp, 0) != 0);
    res.setHardwareAccelerated(glXGetFBConfig(display, fbcfg, GLX.GLX_CONFIG_CAVEAT, tmp, 0) != GLX.GLX_SLOW_CONFIG);
    res.setDepthBits     (glXGetFBConfig(display, fbcfg, GLX.GLX_DEPTH_SIZE,       tmp, 0));
    res.setStencilBits   (glXGetFBConfig(display, fbcfg, GLX.GLX_STENCIL_SIZE,     tmp, 0));
    res.setRedBits       (glXGetFBConfig(display, fbcfg, GLX.GLX_RED_SIZE,         tmp, 0));
    res.setGreenBits     (glXGetFBConfig(display, fbcfg, GLX.GLX_GREEN_SIZE,       tmp, 0));
    res.setBlueBits      (glXGetFBConfig(display, fbcfg, GLX.GLX_BLUE_SIZE,        tmp, 0));
    res.setAlphaBits     (glXGetFBConfig(display, fbcfg, GLX.GLX_ALPHA_SIZE,       tmp, 0));
    res.setAccumRedBits  (glXGetFBConfig(display, fbcfg, GLX.GLX_ACCUM_RED_SIZE,   tmp, 0));
    res.setAccumGreenBits(glXGetFBConfig(display, fbcfg, GLX.GLX_ACCUM_GREEN_SIZE, tmp, 0));
    res.setAccumBlueBits (glXGetFBConfig(display, fbcfg, GLX.GLX_ACCUM_BLUE_SIZE,  tmp, 0));
    res.setAccumAlphaBits(glXGetFBConfig(display, fbcfg, GLX.GLX_ACCUM_ALPHA_SIZE, tmp, 0));
    if (isMultisampleEnabled) {
      res.setSampleBuffers(glXGetFBConfig(display, fbcfg, GLX.GLX_SAMPLE_BUFFERS, tmp, 0) != 0);
      res.setNumSamples   (glXGetFBConfig(display, fbcfg, GLX.GLX_SAMPLES,        tmp, 0));
    }
    res.setBackgroundOpaque(glXGetFBConfig(display, fbcfg, GLX.GLX_TRANSPARENT_TYPE, tmp, 0) == GLX.GLX_NONE);
    if(!res.isBackgroundOpaque()) {
        glXGetFBConfig(display, fbcfg, GLX.GLX_TRANSPARENT_RED_VALUE,  tmp, 0);
        res.setTransparentRedValue(tmp[0]==GLX.GLX_DONT_CARE?-1:tmp[0]);

        glXGetFBConfig(display, fbcfg, GLX.GLX_TRANSPARENT_GREEN_VALUE,  tmp, 0);
        res.setTransparentGreenValue(tmp[0]==GLX.GLX_DONT_CARE?-1:tmp[0]);

        glXGetFBConfig(display, fbcfg, GLX.GLX_TRANSPARENT_BLUE_VALUE,  tmp, 0);
        res.setTransparentBlueValue(tmp[0]==GLX.GLX_DONT_CARE?-1:tmp[0]);

        glXGetFBConfig(display, fbcfg, GLX.GLX_TRANSPARENT_ALPHA_VALUE,  tmp, 0);
        res.setTransparentAlphaValue(tmp[0]==GLX.GLX_DONT_CARE?-1:tmp[0]);
    }
    try { 
        res.setPbufferFloatingPointBuffers(glXGetFBConfig(display, fbcfg, GLXExt.GLX_FLOAT_COMPONENTS_NV, tmp, 0) != GL.GL_FALSE);
    } catch (Exception e) {}
    return res;
  }

  private static String glXGetFBConfigErrorCode(int err) {
    switch (err) {
      case GLX.GLX_NO_EXTENSION:  return "GLX_NO_EXTENSION";
      case GLX.GLX_BAD_ATTRIBUTE: return "GLX_BAD_ATTRIBUTE";
      default:                return "Unknown error code " + err;
    }
  }

  public static int glXGetFBConfig(long display, long cfg, int attrib, int[] tmp, int tmp_offset) {
    if (display == 0) {
      throw new GLException("No display connection");
    }
    int res = GLX.glXGetFBConfigAttrib(display, cfg, attrib, tmp, tmp_offset);
    if (res != 0) {
      throw new GLException("glXGetFBConfig(0x"+Long.toHexString(attrib)+") failed: error code " + glXGetFBConfigErrorCode(res));
    }
    return tmp[tmp_offset];
  }

  public static int glXFBConfig2FBConfigID(long display, long cfg) {
      int[] tmpID = new int[1];
      return glXGetFBConfig(display, cfg, GLX.GLX_FBCONFIG_ID, tmpID, 0);
  }

  public static long glXFBConfigID2FBConfig(long display, int screen, int id) {
      int[] attribs = new int[] { GLX.GLX_FBCONFIG_ID, id, 0 };
      int[] count = { -1 };
      PointerBuffer fbcfgsL = GLX.glXChooseFBConfigCopied(display, screen, attribs, 0, count, 0);
      if (fbcfgsL == null || fbcfgsL.limit()<1) {
          return 0;
      }
      return fbcfgsL.get(0);
  }

  // Visual Info

  public static XVisualInfo XVisualID2XVisualInfo(long display, long visualID) {
      XVisualInfo res = null;
      NativeWindowFactory.getDefaultFactory().getToolkitLock().lock();
      try{
          int[] count = new int[1];
          XVisualInfo template = XVisualInfo.create();
          template.visualid(visualID);
          XVisualInfo[] infos = X11Lib.XGetVisualInfoCopied(display, X11Lib.VisualIDMask, template, count, 0);
          if (infos == null || infos.length == 0) {
            return null;
          }  
        res = XVisualInfo.create(infos[0]);
      } finally {
          NativeWindowFactory.getDefaultFactory().getToolkitLock().unlock();
      }
      if (DEBUG) {
        System.err.println("!!! Fetched XVisualInfo for visual ID 0x" + Long.toHexString(visualID));
        System.err.println("!!! Resulting XVisualInfo: visualid = 0x" + Long.toHexString(res.visualid()));
      }
      return res;
  }

  public static GLCapabilities XVisualInfo2GLCapabilities(GLProfile glp, long display, XVisualInfo info, boolean onscreen, boolean usePBuffer, boolean isMultisampleEnabled) {
    int[] tmp = new int[1];
    int val = glXGetConfig(display, info, GLX.GLX_USE_GL, tmp, 0);
    if (val == 0) {
      throw new GLException("Visual does not support OpenGL");
    }
    val = glXGetConfig(display, info, GLX.GLX_RGBA, tmp, 0);
    if (val == 0) {
      throw new GLException("Visual does not support RGBA");
    }
    GLCapabilities res = new GLCapabilities(glp);
    res.setOnscreen      (onscreen);
    res.setPBuffer       (usePBuffer);
    res.setDoubleBuffered(glXGetConfig(display, info, GLX.GLX_DOUBLEBUFFER,     tmp, 0) != 0);
    res.setStereo        (glXGetConfig(display, info, GLX.GLX_STEREO,           tmp, 0) != 0);
    // Note: use of hardware acceleration is determined by
    // glXCreateContext, not by the XVisualInfo. Optimistically claim
    // that all GLCapabilities have the capability to be hardware
    // accelerated.
    res.setHardwareAccelerated(true);
    res.setDepthBits     (glXGetConfig(display, info, GLX.GLX_DEPTH_SIZE,       tmp, 0));
    res.setStencilBits   (glXGetConfig(display, info, GLX.GLX_STENCIL_SIZE,     tmp, 0));
    res.setRedBits       (glXGetConfig(display, info, GLX.GLX_RED_SIZE,         tmp, 0));
    res.setGreenBits     (glXGetConfig(display, info, GLX.GLX_GREEN_SIZE,       tmp, 0));
    res.setBlueBits      (glXGetConfig(display, info, GLX.GLX_BLUE_SIZE,        tmp, 0));
    res.setAlphaBits     (glXGetConfig(display, info, GLX.GLX_ALPHA_SIZE,       tmp, 0));
    res.setAccumRedBits  (glXGetConfig(display, info, GLX.GLX_ACCUM_RED_SIZE,   tmp, 0));
    res.setAccumGreenBits(glXGetConfig(display, info, GLX.GLX_ACCUM_GREEN_SIZE, tmp, 0));
    res.setAccumBlueBits (glXGetConfig(display, info, GLX.GLX_ACCUM_BLUE_SIZE,  tmp, 0));
    res.setAccumAlphaBits(glXGetConfig(display, info, GLX.GLX_ACCUM_ALPHA_SIZE, tmp, 0));
    if (isMultisampleEnabled) {
      res.setSampleBuffers(glXGetConfig(display, info, GLX.GLX_SAMPLE_BUFFERS, tmp, 0) != 0);
      res.setNumSamples   (glXGetConfig(display, info, GLX.GLX_SAMPLES,        tmp, 0));
    }
    return res;
  }

  private static String glXGetConfigErrorCode(int err) {
    switch (err) {
      case GLX.GLX_NO_EXTENSION:  return "GLX_NO_EXTENSION";
      case GLX.GLX_BAD_SCREEN:    return "GLX_BAD_SCREEN";
      case GLX.GLX_BAD_ATTRIBUTE: return "GLX_BAD_ATTRIBUTE";
      case GLX.GLX_BAD_VISUAL:    return "GLX_BAD_VISUAL";
      default:                return "Unknown error code " + err;
    }
  }

  public static int glXGetConfig(long display, XVisualInfo info, int attrib, int[] tmp, int tmp_offset) {
    if (display == 0) {
      throw new GLException("No display connection");
    }
    int res = GLX.glXGetConfig(display, info, attrib, tmp, tmp_offset);
    if (res != 0) {
      throw new GLException("glXGetConfig(0x"+Long.toHexString(attrib)+") failed: error code " + glXGetConfigErrorCode(res));
    }
    return tmp[tmp_offset];
  }

  public String toString() {
    return "X11GLXGraphicsConfiguration["+getScreen()+", visualID 0x" + Long.toHexString(getVisualID()) + ", fbConfigID 0x" + Long.toHexString(fbConfigID) + 
                                        ",\n\trequested " + getRequestedCapabilities()+
                                        ",\n\tchosen    " + getChosenCapabilities()+
                                        "]";
  }
}

