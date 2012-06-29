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

package jogamp.opengl;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

public abstract class GLDrawableImpl implements GLDrawable {
  protected static final boolean DEBUG = Debug.debug("GLDrawable");

  protected GLDrawableImpl(GLDrawableFactory factory,
                           NativeSurface comp,
                           boolean realized) {
      this.factory = factory;
      this.surface = comp;
      this.realized = realized;
      this.requestedCapabilities = (GLCapabilitiesImmutable) surface.getGraphicsConfiguration().getRequestedCapabilities();
  }

  /**
   * Returns the DynamicLookupHelper
   */
  public abstract GLDynamicLookupHelper getGLDynamicLookupHelper();

  public final GLDrawableFactoryImpl getFactoryImpl() {
    return (GLDrawableFactoryImpl) getFactory();
  }

  /** For offscreen GLDrawables (pbuffers and "pixmap" drawables),
      indicates that native resources should be reclaimed. */
  public void destroy() {
      surface.getGraphicsConfiguration().getScreen().getDevice().lock();
      try {
          destroyImpl();
      } finally {
          surface.getGraphicsConfiguration().getScreen().getDevice().unlock();
      }
  }
  protected void destroyImpl() {
    throw new GLException("Should not call this (should only be called for offscreen GLDrawables)");
  }

  @Override
  public final void swapBuffers() throws GLException {
    if( !realized ) {
        return; // destroyed already
    }
    GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable)surface.getGraphicsConfiguration().getChosenCapabilities();
    if ( caps.getDoubleBuffered() ) {
        if(!surface.surfaceSwap()) {
            int lockRes = lockSurface(); // it's recursive, so it's ok within [makeCurrent .. release]
            if (NativeSurface.LOCK_SURFACE_NOT_READY == lockRes) {
                return;
            }
            try {
                if (NativeSurface.LOCK_SURFACE_CHANGED == lockRes) {
                    updateHandle();
                }
                swapBuffersImpl();
            } finally {
                unlockSurface();
            }
        }
    } else {
        GLContext ctx = GLContext.getCurrent();
        if(null!=ctx && ctx.getGLDrawable()==this) {
            ctx.getGL().glFinish();
        }
    }
    surface.surfaceUpdated(this, surface, System.currentTimeMillis());
  }
  protected abstract void swapBuffersImpl();

  public final static String toHexString(long hex) {
    return "0x" + Long.toHexString(hex);
  }

  @Override
  public final GLProfile getGLProfile() {
    return requestedCapabilities.getGLProfile();
  }

  @Override
  public GLCapabilitiesImmutable getChosenGLCapabilities() {
    return  (GLCapabilitiesImmutable) surface.getGraphicsConfiguration().getChosenCapabilities();
  }

  public final GLCapabilitiesImmutable getRequestedGLCapabilities() {
    return requestedCapabilities;
  }

  @Override
  public NativeSurface getNativeSurface() {
    return surface;
  }

  /** called with locked surface @ setRealized(false) */
  protected void destroyHandle() {}

  /** called with locked surface @ setRealized(true) or @ lockSurface(..) when surface changed */
  protected void updateHandle() {}

  @Override
  public long getHandle() {
    return surface.getSurfaceHandle();
  }

  @Override
  public final GLDrawableFactory getFactory() {
    return factory;
  }

  @Override
  public final synchronized void setRealized(boolean realizedArg) {
    if ( realized != realizedArg ) {
        if(DEBUG) {
            System.err.println(getThreadName() + ": setRealized: "+getClass().getSimpleName()+" "+realized+" -> "+realizedArg);
        }
        realized = realizedArg;
        AbstractGraphicsDevice aDevice = surface.getGraphicsConfiguration().getScreen().getDevice();
        if(realizedArg) {
            if(NativeSurface.LOCK_SURFACE_NOT_READY >= lockSurface()) {
                throw new GLException("GLDrawableImpl.setRealized(true): already realized, but surface not ready (lockSurface)");
            }
        } else {
            aDevice.lock();
        }
        try {
            setRealizedImpl();
            if(realizedArg) {
                updateHandle();
            } else {
                destroyHandle();
            }
        } finally {
            if(realizedArg) {
                unlockSurface();
            } else {
                aDevice.unlock();
            }
        }
    } else if(DEBUG) {
        System.err.println(getThreadName() + ": setRealized: "+getClass().getName()+" "+this.realized+" == "+realizedArg);
    }
  }
  protected abstract void setRealizedImpl();

  @Override
  public final synchronized boolean isRealized() {
    return realized;
  }

  @Override
  public int getWidth() {
    return surface.getWidth();
  }

  @Override
  public int getHeight() {
    return surface.getHeight();
  }

  public final int lockSurface() throws GLException {
    return surface.lockSurface();
  }

  public final void unlockSurface() {
    surface.unlockSurface();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()+"[Realized "+isRealized()+
                ",\n\tFactory   "+getFactory()+
                ",\n\thandle    "+toHexString(getHandle())+
                ",\n\tWindow    "+getNativeSurface()+"]";
  }

  protected static String getThreadName() {
    return Thread.currentThread().getName();
  }

  protected GLDrawableFactory factory;
  protected NativeSurface surface;
  protected GLCapabilitiesImmutable requestedCapabilities;

  // Indicates whether the surface (if an onscreen context) has been
  // realized. Plausibly, before the surface is realized the JAWT
  // should return an error or NULL object from some of its
  // operations; this appears to be the case on Win32 but is not true
  // at least with Sun's current X11 implementation (1.4.x), which
  // crashes with no other error reported if the DrawingSurfaceInfo is
  // fetched from a locked DrawingSurface during the validation as a
  // result of calling show() on the main thread. To work around this
  // we prevent any JAWT or OpenGL operations from being done until
  // addNotify() is called on the surface.
  protected boolean realized;

}
