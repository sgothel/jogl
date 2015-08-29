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

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import com.jogamp.common.ExceptionUtils;

public abstract class GLDrawableImpl implements GLDrawable {
  protected static final boolean DEBUG = GLDrawableFactoryImpl.DEBUG;

  protected GLDrawableImpl(final GLDrawableFactory factory, final NativeSurface comp, final boolean realized) {
      this(factory, comp, (GLCapabilitiesImmutable) comp.getGraphicsConfiguration().getRequestedCapabilities(), realized);
  }

  protected GLDrawableImpl(final GLDrawableFactory factory, final NativeSurface comp, final GLCapabilitiesImmutable requestedCapabilities, final boolean realized) {
      this.factory = factory;
      this.surface = comp;
      this.realized = realized;
      this.requestedCapabilities = requestedCapabilities;
  }

  public final GLDrawableFactoryImpl getFactoryImpl() {
    return (GLDrawableFactoryImpl) getFactory();
  }

  @Override
  public final void swapBuffers() throws GLException {
    if( !realized ) { // volatile OK (locked below)
        return; // destroyed already
    }
    final int lockRes = lockSurface(); // it's recursive, so it's ok within [makeCurrent .. release]
    if (NativeSurface.LOCK_SURFACE_NOT_READY == lockRes) {
        return;
    }
    try {
        if( realized ) { // volatile OK
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
   * @param doubleBuffered indicates whether double buffering is enabled, see above.
   */
  protected abstract void swapBuffersImpl(boolean doubleBuffered);

  public final static String toHexString(final long hex) {
    return "0x" + Long.toHexString(hex);
  }

  @Override
  public final GLProfile getGLProfile() {
    return requestedCapabilities.getGLProfile();
  }

  @Override
  public final GLCapabilitiesImmutable getChosenGLCapabilities() {
    return  (GLCapabilitiesImmutable) surface.getGraphicsConfiguration().getChosenCapabilities();
  }

  @Override
  public final GLCapabilitiesImmutable getRequestedGLCapabilities() {
    return requestedCapabilities;
  }

  @Override
  public NativeSurface getNativeSurface() {
    return surface;
  }

  /**
   * called with locked surface @ setRealized(false) or @ lockSurface(..) when surface changed
   * <p>
   * Must be paired w/ {@link #createHandle()}.
   * </p>
   */
  protected void destroyHandle() {}

  /**
   * called with locked surface @ setRealized(true) or @ lockSurface(..) when surface changed
   * <p>
   * Must be paired w/ {@link #destroyHandle()}.
   * </p>
   */
  protected void createHandle() {}

  @Override
  public long getHandle() {
    return surface.getSurfaceHandle();
  }

  @Override
  public final GLDrawableFactory getFactory() {
    return factory;
  }

  @Override
  public final void setRealized(final boolean realizedArg) {
    if ( realized != realizedArg ) { // volatile: OK (locked below)
        final boolean isProxySurface = surface instanceof ProxySurface;
        if(DEBUG) {
            System.err.println(getThreadName() + ": setRealized: drawable "+getClass().getSimpleName()+", surface "+surface.getClass().getSimpleName()+", isProxySurface "+isProxySurface+": "+realized+" -> "+realizedArg);
            ExceptionUtils.dumpStack(System.err);
        }
        final AbstractGraphicsDevice aDevice = surface.getGraphicsConfiguration().getScreen().getDevice();
        if(realizedArg) {
            if(isProxySurface) {
                ((ProxySurface)surface).createNotify();
            }
            if(NativeSurface.LOCK_SURFACE_NOT_READY >= surface.lockSurface()) {
                throw new GLException("GLDrawableImpl.setRealized(true): Surface not ready (lockSurface)");
            }
        } else {
            aDevice.lock();
        }
        try {
            if ( realized != realizedArg ) { // volatile: OK
                realized = realizedArg;
                if(realizedArg) {
                    setRealizedImpl();
                    createHandle();
                } else {
                    destroyHandle();
                    setRealizedImpl();
                }
            }
        } finally {
            if(realizedArg) {
                surface.unlockSurface();
            } else {
                aDevice.unlock();
                if(isProxySurface) {
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
   * Callback for special implementations, allowing
   * <ul>
   *   <li>to associate bound context to this drawable (bound == true)
   *       or to remove such association (bound == false).</li>
   *   <li>to trigger GLContext/GLDrawable related lifecycle: <code>construct</code>, <code>destroy</code>.</li>
   * </ul>
   * <p>
   * If <code>bound</code> is <code>true</code>, the context is current and being newly associated w/ this drawable.
   * </p>
   * <p>
   * If <code>bound</code> is <code>false</code>, the context is still current and will be unbound (released and destroyed, or simply disassociated).
   * </p>
   * <p>
   * Being called by {@link GLContextImpl#associateDrawable(boolean)}.
   * </p>
   * @param ctx the just bounded or unbounded context
   * @param bound if <code>true</code> create an association, otherwise remove it
   */
  protected void associateContext(final GLContext ctx, final boolean bound) { }

  /**
   * Callback for special implementations, allowing GLContext to trigger GL related lifecycle: <code>makeCurrent</code>, <code>release</code>.
   * <p>
   * If <code>current</code> is <code>true</code>, the context has just been made current.
   * </p>
   * <p>
   * If <code>current</code> is <code>false</code>, the context is still current and will be release after this method returns.
   * </p>
   * <p>
   * Being called by {@link GLContextImpl#contextMadeCurrent(boolean)}.
   * </p>
   * @see #associateContext(GLContext, boolean)
   */
  protected void contextMadeCurrent(final GLContext glc, final boolean current) { }

  /** Callback for special implementations, allowing GLContext to fetch a custom default render framebuffer. Defaults to zero.*/
  protected int getDefaultDrawFramebuffer() { return 0; }
  /** Callback for special implementations, allowing GLContext to fetch a custom default read framebuffer. Defaults to zero. */
  protected int getDefaultReadFramebuffer() { return 0; }
  /** Callback for special implementations, allowing GLContext to fetch a custom default read buffer of current framebuffer. */
  protected int getDefaultReadBuffer(final GL gl, final boolean hasDedicatedDrawableRead) {
      if( gl.isGLES() || hasDedicatedDrawableRead || getChosenGLCapabilities().getDoubleBuffered() ) {
          // Note-1: Neither ES1 nor ES2 supports selecting the read buffer via glReadBuffer
          // Note-2: ES3 only supports GL_BACK, GL_NONE or GL_COLOR_ATTACHMENT0+i
          return GL.GL_BACK;
      }
      return GL.GL_FRONT ;
  }

  @Override
  public final boolean isRealized() {
    return realized;
  }

  @Override
  public int getSurfaceWidth() {
    return surface.getSurfaceWidth();
  }

  @Override
  public int getSurfaceHeight() {
    return surface.getSurfaceHeight();
  }

  @Override
  public boolean isGLOriented() {
      return true;
  }

  /**
   * {@link NativeSurface#lockSurface() Locks} the underlying windowing toolkit's {@link NativeSurface surface}.
   * <p>
   * <i>If</i> drawable is {@link #setRealized(boolean) realized},
   * the {@link #getHandle() drawable handle} is valid after successfully {@link NativeSurface#lockSurface() locking}
   * it's {@link NativeSurface surface} until being {@link #unlockSurface() unlocked}.
   * </p>
   * <p>
   * In case the {@link NativeSurface surface} has changed as indicated by it's
   * {@link NativeSurface#lockSurface() lock} result {@link NativeSurface#LOCK_SURFACE_CHANGED},
   * the implementation is required to update this information as needed within it's implementation.
   * </p>
   *
   * @see NativeSurface#lockSurface()
   * @see #getHandle()
   */
  public final int lockSurface() throws GLException {
    final int lockRes = surface.lockSurface();
    if ( NativeSurface.LOCK_SURFACE_CHANGED == lockRes && realized ) {
        // Update the drawable handle, in case the surface handle has changed.
        final long _handle1 = getHandle();
        destroyHandle();
        createHandle();
        final long _handle2 = getHandle();
        if(DEBUG) {
            if( _handle1 != _handle2) {
                System.err.println(getThreadName() + ": Drawable handle changed: "+toHexString(_handle1)+" -> "+toHexString(_handle2));
            }
        }
    }
    return lockRes;

  }

  /**
   * {@link NativeSurface#unlockSurface() Unlocks} the underlying windowing toolkit {@link NativeSurface surface},
   * which may render the {@link #getHandle() drawable handle} invalid.
   *
   * @see NativeSurface#unlockSurface()
   * @see #getHandle()
   */
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

  protected static String getThreadName() { return Thread.currentThread().getName(); }

  protected final GLDrawableFactory factory;
  protected final NativeSurface surface;
  protected final GLCapabilitiesImmutable requestedCapabilities;

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
  protected volatile boolean realized;

}
