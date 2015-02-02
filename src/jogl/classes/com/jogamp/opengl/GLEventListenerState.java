/**
 * Copyright 2013 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.DefaultGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLRunnable;

import jogamp.opengl.Debug;

import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.nativewindow.MutableGraphicsConfiguration;
import com.jogamp.opengl.util.GLDrawableUtil;

/**
 * GLEventListenerState is holding {@link GLAutoDrawable} components crucial
 * to relocating all its {@link GLEventListener} w/ their operating {@link GLContext}, etc.
 * The components are:
 * <ul>
 *   <li>{@link GLContext}</li>
 *   <li>All {@link GLEventListener}, incl. their init state</li>
 *   <li>{@link GLAnimatorControl}</li>
 *   <!--li>{@link GLCapabilitiesImmutable} for compatibility check</li-->
 *   <li>{@link AbstractGraphicsDevice} for compatibility check and preserving the native device handle incl. ownership</li>
 * </ul>
 * <p>
 * A GLEventListenerState instance can be created while components are {@link #moveFrom(GLAutoDrawable) moved from} a {@link GLAutoDrawable}
 * to the new instance, which gains {@link #isOwner() ownership} of the moved components.
 * </p>
 * <p>
 * A GLEventListenerState instance's components can be {@link #moveTo(GLAutoDrawable) moved to} a {@link GLAutoDrawable},
 * while loosing {@link #isOwner() ownership} of the moved components.
 * </p>
 */
public class GLEventListenerState {
    private static final boolean DEBUG = Debug.debug("GLDrawable") || Debug.debug("GLEventListenerState");

    private GLEventListenerState(final AbstractGraphicsDevice upstreamDevice, final boolean proxyOwnsUpstreamDevice, final AbstractGraphicsDevice device,
                                 final GLCapabilitiesImmutable caps,
                                 final RecursiveLock upstreamLock, final NativeSurface lockedSurface,
                                 final GLContext context, final int count, final GLAnimatorControl anim, final boolean animStarted) {
        this.upstreamDevice = upstreamDevice;
        this.proxyOwnsUpstreamDevice = proxyOwnsUpstreamDevice;
        this.device = device;
        this.caps = caps;
        this.upstreamLock = upstreamLock;
        this.lockedSurface = lockedSurface;
        this.context = context;
        this.listeners = new GLEventListener[count];
        this.listenersInit = new boolean[count];
        this.anim = anim;
        this.animStarted = animStarted;

        this.owner = true;
    }
    /**
     * Returns <code>true</code>, if this instance is the current owner of the components,
     * otherwise <code>false</code>.
     * <p>
     * Ownership is lost if {@link #moveTo(GLAutoDrawable)} is being called successfully
     * and all components are transferred to the new {@link GLAutoDrawable}.
     * </p>
     */
    public final boolean isOwner() { return owner; }

    public final int listenerCount() { return listeners.length; }

    public final AbstractGraphicsDevice upstreamDevice;
    public final boolean proxyOwnsUpstreamDevice;
    public final AbstractGraphicsDevice device;
    public final GLCapabilitiesImmutable caps;
    public final GLContext context;
    public final GLEventListener[] listeners;
    public final boolean[] listenersInit;
    public final GLAnimatorControl anim;
    public final boolean animStarted;

    private volatile RecursiveLock upstreamLock;
    private volatile NativeSurface lockedSurface;
    private volatile boolean owner;

    /**
     * Returns a {@link Runnable} {@link NativeSurface#unlockSurface() unlocking} an eventually locked {@link NativeSurface},
     * see {@link #moveFrom(GLAutoDrawable, boolean)} and {@link #moveTo(GLAutoDrawable, Runnable)}.
     */
    public Runnable getUnlockSurfaceOp() { return unlockOp; }

    private final Runnable unlockOp = new Runnable() {
        public void run() {
            final RecursiveLock rl = upstreamLock;
            final NativeSurface ls = lockedSurface;
            upstreamLock = null;
            lockedSurface = null;
            if( null != rl ) {
                rl.unlock();
            }
            if( null != ls ) {
                ls.unlockSurface();
            }
        } };

    /**
     * Last resort to destroy and loose ownership
     */
    public void destroy() {
        if( owner ) {
            final int aSz = listenerCount();
            for(int i=0; i<aSz; i++) {
                listeners[i] = null;
            }
            // context.destroy(); - NPE (null drawable)
            unlockOp.run();
            device.close();
            owner = false;
        }
    }

    private static AbstractGraphicsDevice cloneDevice(final AbstractGraphicsDevice aDevice) {
        return (AbstractGraphicsDevice) aDevice.clone();
    }

    /**
     * Moves all GLEventListenerState components from the given {@link GLAutoDrawable}
     * to a newly created instance.
     * <p>
     * Note that all components are removed from the {@link GLAutoDrawable},
     * i.e. the {@link GLContext}, all {@link GLEventListener}.
     * </p>
     * <p>
     * If the {@link GLAutoDrawable} was added to a {@link GLAnimatorControl}, it is removed
     * and the {@link GLAnimatorControl} added to the GLEventListenerState.
     * </p>
     * <p>
     * The returned GLEventListenerState instance is the {@link #isOwner() owner of the components}.
     * </p>
     * <p>
     * Locking is performed on the {@link GLAutoDrawable auto-drawable's}
     * {@link GLAutoDrawable#getUpstreamLock() upstream-lock} and {@link GLAutoDrawable#getNativeSurface() surface}.
     * See <a href="../../../com/jogamp/opengl/GLAutoDrawable.html#locking">GLAutoDrawable Locking</a>.</li>
     * </p>
     *
     * @param src {@link GLAutoDrawable} source to move components from
     * @return new GLEventListenerState instance {@link #isOwner() owning} moved components.
     *
     * @see #moveTo(GLAutoDrawable)
     */
    public static GLEventListenerState moveFrom(final GLAutoDrawable src) {
        return GLEventListenerState.moveFrom(src, false);
    }

    /**
     * Moves all GLEventListenerState components from the given {@link GLAutoDrawable}
     * to a newly created instance.
     * <p>
     * Note that all components are removed from the {@link GLAutoDrawable},
     * i.e. the {@link GLContext}, all {@link GLEventListener}.
     * </p>
     * <p>
     * If the {@link GLAutoDrawable} was added to a {@link GLAnimatorControl}, it is removed
     * and the {@link GLAnimatorControl} added to the GLEventListenerState.
     * </p>
     * <p>
     * The returned GLEventListenerState instance is the {@link #isOwner() owner of the components}.
     * </p>
     * <p>
     * Locking is performed on the {@link GLAutoDrawable auto-drawable's}
     * {@link GLAutoDrawable#getUpstreamLock() upstream-lock} and {@link GLAutoDrawable#getNativeSurface() surface},
     * which is <i>not released</i> if <code>keepLocked</code> is <code>true</code>.
     * See <a href="../../../com/jogamp/opengl/GLAutoDrawable.html#locking">GLAutoDrawable Locking</a>.</li>
     * </p>
     * <p>
     * <code>keepLocked</code> may be utilized if swapping a context between drawables
     * and to ensure atomicity of operation.
     * Here, the {@link #getUnlockSurfaceOp()} shall be passed to {@link #moveTo(GLAutoDrawable, Runnable)}.
     * See {@link GLDrawableUtil#swapGLContextAndAllGLEventListener(GLAutoDrawable, GLAutoDrawable)}.
     * </p>
     *
     * @param src {@link GLAutoDrawable} source to move components from
     * @param keepLocked keep {@link GLAutoDrawable#getUpstreamLock() upstream-lock} and {@link GLAutoDrawable#getNativeSurface() surface} locked, see above
     * @return new GLEventListenerState instance {@link #isOwner() owning} moved components.
     *
     * @see #moveTo(GLAutoDrawable, Runnable)
     */
    public static GLEventListenerState moveFrom(final GLAutoDrawable src, final boolean keepLocked) {
        final GLAnimatorControl srcAnim = src.getAnimator();
        final boolean srcAnimStarted;
        if( null != srcAnim ) {
            srcAnimStarted = srcAnim.isStarted();
            srcAnim.remove(src); // also handles ECT
        } else {
            srcAnimStarted = false;
        }

        final GLEventListenerState glls;
        final RecursiveLock srcUpstreamLock = src.getUpstreamLock();
        srcUpstreamLock.lock();
        try {
            final NativeSurface srcSurface = src.getNativeSurface();
            final boolean srcSurfaceLocked = NativeSurface.LOCK_SURFACE_NOT_READY < srcSurface.lockSurface();
            if( src.isRealized() && !srcSurfaceLocked ) {
                throw new GLException("Could not lock realized surface "+src);
            }

            try {
                final int aSz = src.getGLEventListenerCount();

                // Create new AbstractGraphicsScreen w/ cloned AbstractGraphicsDevice for future GLAutoDrawable
                // allowing this AbstractGraphicsDevice to loose ownership -> not closing display/device!
                final AbstractGraphicsConfiguration aCfg = srcSurface.getGraphicsConfiguration();
                final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) aCfg.getChosenCapabilities();
                final AbstractGraphicsDevice aDevice1 = aCfg.getScreen().getDevice();
                final AbstractGraphicsDevice aDevice2 = cloneDevice(aDevice1);
                aDevice1.clearHandleOwner();  // don't close device handle
                if( DEBUG ) {
                    System.err.println("GLEventListenerState.moveFrom.0a: orig 0x"+Integer.toHexString(aDevice1.hashCode())+", "+aDevice1);
                    System.err.println("GLEventListenerState.moveFrom.0b: pres 0x"+Integer.toHexString(aDevice2.hashCode())+", "+aDevice2);
                    System.err.println("GLEventListenerState.moveFrom.1: "+srcSurface.getClass().getName()/*+", "+aSurface*/);
                }
                final AbstractGraphicsDevice aUpDevice2;
                final boolean proxyOwnsUpstreamDevice;
                {
                    AbstractGraphicsDevice _aUpDevice2 = null;
                    if(srcSurface instanceof ProxySurface) {
                        final ProxySurface aProxy = (ProxySurface)srcSurface;
                        proxyOwnsUpstreamDevice = aProxy.containsUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_DEVICE );
                        final NativeSurface aUpSurface = aProxy.getUpstreamSurface();
                        if(DEBUG && null != aUpSurface) {
                            System.err.println("GLEventListenerState.moveFrom.2: "+aUpSurface.getClass().getName()+", "+aUpSurface);
                        }
                        if(null != aUpSurface) {
                            final AbstractGraphicsDevice aUpDevice1 = aUpSurface.getGraphicsConfiguration().getScreen().getDevice();
                            _aUpDevice2 = cloneDevice(aUpDevice1);
                            aUpDevice1.clearHandleOwner(); // don't close device handle
                            if(DEBUG) {
                                System.err.println("GLEventListenerState.moveFrom.3a: up-orig 0x"+Integer.toHexString(aUpDevice1.hashCode())+", "+aUpDevice1);
                                System.err.println("GLEventListenerState.moveFrom.3b: up-pres 0x"+Integer.toHexString(_aUpDevice2.hashCode())+", "+_aUpDevice2);
                                System.err.println("GLEventListenerState.moveFrom.3c: "+srcSurface.getClass().getName()+", "+srcSurface);
                                System.err.println("GLEventListenerState.moveFrom.3d: "+aUpSurface.getClass().getName()/*+", "+aUpSurface+", "*/+aProxy.getUpstreamOptionBits(null).toString());
                            }
                        }
                    } else {
                        proxyOwnsUpstreamDevice = false;
                    }
                    aUpDevice2 = _aUpDevice2;
                }

                glls = new GLEventListenerState(aUpDevice2, proxyOwnsUpstreamDevice, aDevice2, caps,
                                                keepLocked ? srcUpstreamLock : null,
                                                srcSurfaceLocked && keepLocked ? srcSurface : null,
                                                src.getContext(), aSz, srcAnim, srcAnimStarted);

                //
                // remove and cache all GLEventListener and their init-state
                //
                for(int i=0; i<aSz; i++) {
                    final GLEventListener l = src.getGLEventListener(0);
                    glls.listenersInit[i] = src.getGLEventListenerInitState(l);
                    glls.listeners[i] = src.removeGLEventListener( l );
                }

                src.setContext( null, false ); // implicit glFinish() ctx/drawable sync

            } finally {
                if( srcSurfaceLocked && !keepLocked ) {
                    srcSurface.unlockSurface();
                }
            }
        } finally {
            if( !keepLocked ) {
                srcUpstreamLock.unlock();
            }
        }
        return glls;
    }

    /**
     * Moves all GLEventListenerState components to the given {@link GLAutoDrawable}
     * from this instance, while loosing {@link #isOwner() ownership}.
     * <p>
     * If the previous {@link GLAutoDrawable} was removed from a {@link GLAnimatorControl} by previous {@link #moveFrom(GLAutoDrawable)},
     * the given {@link GLAutoDrawable} is added to the cached {@link GLAnimatorControl}.
     * This operation is skipped, if the given {@link GLAutoDrawable} is already added to a {@link GLAnimatorControl} instance.
     * </p>
     * <p>
     * Locking is performed on the {@link GLAutoDrawable auto-drawable's}
     * {@link GLAutoDrawable#getUpstreamLock() upstream-lock} and {@link GLAutoDrawable#getNativeSurface() surface}.
     * See <a href="../../../com/jogamp/opengl/GLAutoDrawable.html#locking">GLAutoDrawable Locking</a>.</li>
     * </p>
     * <p>
     * Note: After this operation, the GLEventListenerState reference should be released.
     * </p>
     *
     * @param dest {@link GLAutoDrawable} destination to move GLEventListenerState components to
     *
     * @throws GLException if a realized surface could not be locked.
     * @throws GLException if this preserved {@link AbstractGraphicsDevice} is incompatible w/ the given destination one.
     * <!-- @throws GLException if the {@link GLAutoDrawable}'s configuration is incompatible, i.e. different {@link GLCapabilitiesImmutable}. -->
     *
     * @see #moveFrom(GLAutoDrawable)
     * @see #isOwner()
     */
    public final void moveTo(final GLAutoDrawable dest) throws GLException  {
        this.moveTo(dest, null);
    }

    /**
     * Moves all GLEventListenerState components to the given {@link GLAutoDrawable}
     * from this instance, while loosing {@link #isOwner() ownership}.
     * <p>
     * If the previous {@link GLAutoDrawable} was removed from a {@link GLAnimatorControl} by previous {@link #moveFrom(GLAutoDrawable, boolean)},
     * the given {@link GLAutoDrawable} is added to the cached {@link GLAnimatorControl}.
     * This operation is skipped, if the given {@link GLAutoDrawable} is already added to a {@link GLAnimatorControl} instance.
     * </p>
     * <p>
     * Locking is performed on the {@link GLAutoDrawable auto-drawable's}
     * {@link GLAutoDrawable#getUpstreamLock() upstream-lock} and {@link GLAutoDrawable#getNativeSurface() surface}.
     * See <a href="../../../com/jogamp/opengl/GLAutoDrawable.html#locking">GLAutoDrawable Locking</a>.</li>
     * </p>
     * <p>
     * If the {@link GLAutoDrawable} <code>dest</code> has been kept locked by {@link #moveFrom(GLAutoDrawable, boolean)},
     * it's {@link #getUnlockSurfaceOp()} shall be passed here to <code>destUnlockOperation</code> to be unlocked.
     * </p>
     * <p>
     * Note: After this operation, the GLEventListenerState reference should be released.
     * </p>
     *
     * @param dest {@link GLAutoDrawable} destination to move GLEventListenerState components to
     * @param destUnlockOperation optional unlock operation for <code>dest</code>, see {@link #moveFrom(GLAutoDrawable, boolean)}.
     *
     * @throws GLException if a realized surface could not be locked.
     * @throws GLException if this preserved {@link AbstractGraphicsDevice} is incompatible w/ the given destination one.
     * <!-- @throws GLException if the {@link GLAutoDrawable}'s configuration is incompatible, i.e. different {@link GLCapabilitiesImmutable}. -->
     *
     * @see #moveFrom(GLAutoDrawable, boolean)
     * @see #isOwner()
     */
    public final void moveTo(final GLAutoDrawable dest, final Runnable destUnlockOperation) throws GLException {
        final GLAnimatorControl destAnim = dest.getAnimator();
        final boolean destAnimPaused;
        if( null != destAnim ) {
            destAnimPaused = destAnim.pause();
            destAnim.remove(dest); // also handles ECT
        } else {
            destAnimPaused = false;
        }

        final List<GLRunnable> aGLCmds = new ArrayList<GLRunnable>();
        final int aSz = listenerCount();

        final RecursiveLock destUpstreamLock = dest.getUpstreamLock();
        destUpstreamLock.lock();
        final boolean destIsRealized;
        try {
            final NativeSurface destSurface = dest.getNativeSurface();
            final boolean destSurfaceLocked = NativeSurface.LOCK_SURFACE_NOT_READY < destSurface.lockSurface();
            if( dest.isRealized() && !destSurfaceLocked ) {
                throw new GLException("Could not lock realized surface "+dest);
            }
            try {

                final MutableGraphicsConfiguration aCfg = (MutableGraphicsConfiguration) destSurface.getGraphicsConfiguration();
                /**
                final GLCapabilitiesImmutable aCaps = (GLCapabilitiesImmutable) aCfg.getChosenCapabilities();
                if( caps.getVisualID(VisualIDHolder.VIDType.INTRINSIC) != aCaps.getVisualID(VisualIDHolder.VIDType.INTRINSIC) ||
                    caps.getVisualID(VisualIDHolder.VIDType.NATIVE) != aCaps.getVisualID(VisualIDHolder.VIDType.NATIVE) ) {
                    throw new GLException("Incompatible Capabilities - Prev-Holder: "+caps+", New-Holder "+caps);
                } */
                final DefaultGraphicsDevice aDevice1 = (DefaultGraphicsDevice) aCfg.getScreen().getDevice();
                final DefaultGraphicsDevice aDevice2 = (DefaultGraphicsDevice) device;
                if( !aDevice1.getUniqueID().equals( aDevice2.getUniqueID() ) ) {
                    throw new GLException("Incompatible devices: Preserved <"+aDevice2.getUniqueID()+">, target <"+aDevice1.getUniqueID()+">");
                }

                // collect optional upstream surface info
                final ProxySurface aProxy;
                final NativeSurface aUpSurface;
                if(destSurface instanceof ProxySurface) {
                    aProxy = (ProxySurface)destSurface;
                    aUpSurface = aProxy.getUpstreamSurface();
                } else {
                    aProxy = null;
                    aUpSurface = null;
                }
                if( DEBUG ) {
                    System.err.println("GLEventListenerState.moveTo.0 : has aProxy "+(null!=aProxy));
                    System.err.println("GLEventListenerState.moveTo.0 : has aUpSurface "+(null!=aUpSurface));
                }
                if( null==aUpSurface && null != upstreamDevice ) {
                    throw new GLException("Incompatible Surface config - Has Upstream-Surface: Prev-Holder = true, New-Holder = false");
                }

                // Destroy and remove currently associated GLContext, if any (will be replaced)
                dest.setContext( null, true );
                destIsRealized = dest.isRealized();
                if( destIsRealized && null != aUpSurface ) {
                    // Unrealize due to device dependencies of an upstream surface, e.g. EGLUpstreamSurfaceHook
                    dest.getDelegatedDrawable().setRealized(false);
                }

                // Set new Screen and close previous one
                {
                    if( DEBUG ) {
                        System.err.println("GLEventListenerState.moveTo.0a: orig 0x"+Integer.toHexString(aDevice1.hashCode())+", "+aDevice1);
                        System.err.println("GLEventListenerState.moveTo.0b: pres 0x"+Integer.toHexString(aDevice2.hashCode())+", "+aDevice2);
                    }
                    DefaultGraphicsDevice.swapDeviceHandleAndOwnership(aDevice1, aDevice2);
                    aDevice2.close();
                    if( DEBUG ) {
                        System.err.println("GLEventListenerState.moveTo.1a: orig 0x"+Integer.toHexString(aDevice1.hashCode())+", "+aDevice1);
                        System.err.println("GLEventListenerState.moveTo.1b: pres 0x"+Integer.toHexString(aDevice2.hashCode())+", "+aDevice2);
                    }
                }

                // If using a ProxySurface w/ an upstream surface, set new Screen and close previous one on it
                if( null != aUpSurface ) {
                    final MutableGraphicsConfiguration aUpCfg = (MutableGraphicsConfiguration) aUpSurface.getGraphicsConfiguration();
                    if( null != upstreamDevice ) {
                        final DefaultGraphicsDevice aUpDevice1 = (DefaultGraphicsDevice) aUpCfg.getScreen().getDevice();
                        final DefaultGraphicsDevice aUpDevice2 = (DefaultGraphicsDevice)upstreamDevice;
                        if( !aUpDevice1.getUniqueID().equals( aUpDevice2.getUniqueID() ) ) {
                            throw new GLException("Incompatible updtream devices: Preserved <"+aUpDevice2.getUniqueID()+">, target <"+aUpDevice1.getUniqueID()+">");
                        }
                        if( DEBUG ) {
                            System.err.println("GLEventListenerState.moveTo.2a: up-orig 0x"+Integer.toHexString(aUpDevice1.hashCode())+", "+aUpDevice1);
                            System.err.println("GLEventListenerState.moveTo.2b: up-pres 0x"+Integer.toHexString(aUpDevice2.hashCode())+", "+aUpDevice2);
                            System.err.println("GLEventListenerState.moveTo.2c:  "+aUpSurface.getClass().getName()/*+", "+aUpSurface+", "*/+aProxy.getUpstreamOptionBits(null).toString());
                        }
                        DefaultGraphicsDevice.swapDeviceHandleAndOwnership(aUpDevice1, aUpDevice2);
                        aUpDevice2.close();
                        if( proxyOwnsUpstreamDevice ) {
                            aProxy.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_DEVICE );
                        }
                        if( DEBUG ) {
                            System.err.println("GLEventListenerState.moveTo.3a: up-orig 0x"+Integer.toHexString(aUpDevice1.hashCode())+", "+aUpDevice1);
                            System.err.println("GLEventListenerState.moveTo.3b: up-pres 0x"+Integer.toHexString(aUpDevice2.hashCode())+", "+aUpDevice2);
                            System.err.println("GLEventListenerState.moveTo.3c:  "+aUpSurface.getClass().getName()/*+", "+aUpSurface+", "*/+aProxy.getUpstreamOptionBits(null).toString());
                        }
                    } else {
                        throw new GLException("Incompatible Surface config - Has Upstream-Surface: Prev-Holder = false, New-Holder = true");
                    }
                }

                if( destIsRealized && null != aUpSurface ) {
                    dest.getDelegatedDrawable().setRealized(true);
                }
                if( DEBUG ) {
                    System.err.println("GLEventListenerState.moveTo.X : has aProxy "+(null!=aProxy));
                    System.err.println("GLEventListenerState.moveTo.X : has aUpSurface "+(null!=aUpSurface));
                }
                dest.setContext( context, false );
            } finally {
                if( destSurfaceLocked ) {
                    destSurface.unlockSurface();
                }
            }
        } finally {
            destUpstreamLock.unlock();
        }
        if( null != destUnlockOperation ) {
            destUnlockOperation.run();
        }

        owner = false;

        //
        // Trigger GL-Viewport reset and reshape of all initialized GLEventListeners
        //
        aGLCmds.add(setViewport);
        for(int i=0; i<aSz; i++) {
            if( listenersInit[i] ) {
                aGLCmds.add(new GLDrawableUtil.ReshapeGLEventListener( listeners[i], false ) );
            }
        }
        aGLCmds.add(glFinish);
        dest.invoke(destIsRealized, aGLCmds); // only wait if already realized

        // add all cached GLEventListener to their destination and fix their init-state
        for(int i=0; i<aSz; i++) {
            final GLEventListener l = listeners[i];
            dest.addGLEventListener( l );
            dest.setGLEventListenerInitState(l, listenersInit[i]);
            listeners[i] = null;
        }

        if( null != destAnim ) {
            // prefer already bound animator
            destAnim.add(dest);
            if( destAnimPaused ) {
                destAnim.resume();
            }
        } else if ( null != anim ) {
            // use previously bound animator
            anim.add(dest); // also handles ECT
            if(animStarted) {
                anim.start();
            }
        }
    }

    private static final GLRunnable setViewport = new GLRunnable() {
        @Override
        public boolean run(final GLAutoDrawable drawable) {
            drawable.getGL().glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
            return true;
        }
    };

    private static final GLRunnable glFinish = new GLRunnable() {
        @Override
        public boolean run(final GLAutoDrawable drawable) {
            drawable.getGL().glFinish();
            return true;
        }
    };
}
