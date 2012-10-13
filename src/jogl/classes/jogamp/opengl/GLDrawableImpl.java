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
import javax.media.nativewindow.ProxySurface;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

public abstract class GLDrawableImpl implements GLDrawable {
  protected static final boolean DEBUG = GLDrawableFactoryImpl.DEBUG;
  
  protected GLDrawableImpl(GLDrawableFactory factory, NativeSurface comp, boolean realized) {
      this(factory, comp, (GLCapabilitiesImmutable) comp.getGraphicsConfiguration().getRequestedCapabilities(), realized);
  }
  
  protected GLDrawableImpl(GLDrawableFactory factory, NativeSurface comp, GLCapabilitiesImmutable requestedCapabilities, boolean realized) {
      this.factory = factory;
      this.surface = comp;
      this.realized = realized;
      this.requestedCapabilities = requestedCapabilities;
  }

  /**
   * Returns the DynamicLookupHelper
   */
  public abstract GLDynamicLookupHelper getGLDynamicLookupHelper();

  public final GLDrawableFactoryImpl getFactoryImpl() {
    return (GLDrawableFactoryImpl) getFactory();
  }

  @Override
  public final void swapBuffers() throws GLException {
    if( !realized ) {
        return; // destroyed already
    }
    int lockRes = lockSurface(); // it's recursive, so it's ok within [makeCurrent .. release]
    if (NativeSurface.LOCK_SURFACE_NOT_READY == lockRes) {
        return;
    }
    try {
        if (NativeSurface.LOCK_SURFACE_CHANGED == lockRes) {
            updateHandle();
        }
        final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable)surface.getGraphicsConfiguration().getChosenCapabilities();
        if ( caps.getDoubleBuffered() ) {
            if(!surface.surfaceSwap()) {
                swapBuffersImpl(true);
            }
        } else {
            final GLContext ctx = GLContext.getCurrent();
            if(null!=ctx && ctx.getGLDrawable()==this) {
                ctx.getGL().glFlush();
            }
            swapBuffersImpl(false);
        }
    } finally {
        unlockSurface();
    }        
    surface.surfaceUpdated(this, surface, System.currentTimeMillis());
  }
  
  /**
   * Platform and implementation depending surface swap.
   * <p>The surface is locked.</p>
   * <p>
   * If <code>doubleBuffered</code> is <code>true</code>, 
   * an actual platform dependent surface swap shall be executed.
   * </p>
   * <p>
   * If <code>doubleBuffered</code> is <code>false</code>, 
   * {@link GL#glFlush()} has been called already and 
   * the implementation may execute implementation specific code.
   * </p>
   */
  protected abstract void swapBuffersImpl(boolean doubleBuffered);

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
            if(surface instanceof ProxySurface) {
                ((ProxySurface)surface).createNotify();
            }
            if(NativeSurface.LOCK_SURFACE_NOT_READY >= lockSurface()) {
                throw new GLException("GLDrawableImpl.setRealized(true): Surface not ready (lockSurface)");
            }
        } else {
            aDevice.lock();
        }
        try {
            if(realizedArg) {
                setRealizedImpl();
                updateHandle();
            } else {
                destroyHandle();
                setRealizedImpl();
            }
        } finally {
            if(realizedArg) {
                unlockSurface();
            } else {
                aDevice.unlock();
                if(surface instanceof ProxySurface) {
                    ((ProxySurface)surface).destroyNotify();
                }
            }
        }
    } else if(DEBUG) {
        System.err.println(getThreadName() + ": setRealized: "+getClass().getName()+" "+this.realized+" == "+realizedArg);
    }
  }
  /**
   * Platform specific realization of drawable 
   */
  protected abstract void setRealizedImpl();

  /** 
   * Callback for special implementations, allowing GLContext to trigger GL related lifecycle: <code>construct</code>, <code>destroy</code>.
   * <p>
   * If <code>realized</code> is <code>true</code>, the context has just been created and made current.
   * </p>
   * <p>
   * If <code>realized</code> is <code>false</code>, the context is still current and will be released and destroyed after this method returns.
   * </p>
   * <p>
   * @see #contextMadeCurrent(GLContext, boolean)
   */
  protected void contextRealized(GLContext glc, boolean realized) {}
  
  /** 
   * Callback for special implementations, allowing GLContext to trigger GL related lifecycle: <code>makeCurrent</code>, <code>release</code>.
   * <p>
   * If <code>current</code> is <code>true</code>, the context has just been made current.
   * </p>
   * <p>
   * If <code>current</code> is <code>false</code>, the context is still current and will be release after this method returns.
   * </p>
   * <p>
   * Note: Will also be called after {@link #contextRealized(GLContext, boolean) contextRealized(ctx, true)}
   * but not at context destruction, i.e. {@link #contextRealized(GLContext, boolean) contextRealized(ctx, false)}.
   * </p>
   * @see #contextRealized(GLContext, boolean)
   */ 
  protected void contextMadeCurrent(GLContext glc, boolean current) { }

  /**
   * Callback for special implementations, allowing to associate bound context to this drawable (bound == true) 
   * or to remove such association (bound == false).
   * @param ctx the just bounded or unbounded context
   * @param bound if <code>true</code> create an association, otherwise remove it
   */
  protected void associateContext(GLContext ctx, boolean bound) { }
  
  /** Callback for special implementations, allowing GLContext to fetch a custom default render framebuffer. Defaults to zero.*/
  protected int getDefaultDrawFramebuffer() { return 0; }
  /** Callback for special implementations, allowing GLContext to fetch a custom default read framebuffer. Defaults to zero. */
  protected int getDefaultReadFramebuffer() { return 0; }
  
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

  /** @see NativeSurface#lockSurface() */
  public final int lockSurface() throws GLException {
    return surface.lockSurface();
  }

  /** @see NativeSurface#unlockSurface() */
  public final void unlockSurface() {
    surface.unlockSurface();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()+"[Realized "+isRealized()+
                ",\n\tFactory   "+getFactory()+
                ",\n\tHandle    "+toHexString(getHandle())+
                ",\n\tSurface   "+getNativeSurface()+"]";
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
