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

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.DefaultGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLRunnable;

import jogamp.opengl.Debug;

import com.jogamp.nativewindow.MutableGraphicsConfiguration;

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
                                 final GLContext context, final int count, final GLAnimatorControl anim, final boolean animStarted) {
        this.upstreamDevice = upstreamDevice;
        this.proxyOwnsUpstreamDevice = proxyOwnsUpstreamDevice;
        this.device = device;
        this.caps = caps;
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

    private boolean owner;

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
     *
     * @param a {@link GLAutoDrawable} source to move components from
     * @return new GLEventListenerState instance {@link #isOwner() owning} moved components.
     *
     * @see #moveTo(GLAutoDrawable)
     */
    public static GLEventListenerState moveFrom(final GLAutoDrawable a) {
        final GLAnimatorControl aAnim = a.getAnimator();
        final boolean aAnimStarted;
        if( null != aAnim ) {
            aAnimStarted = aAnim.isStarted();
            aAnim.remove(a); // also handles ECT
        } else {
            aAnimStarted = false;
        }

        final GLEventListenerState glls;
        final NativeSurface aSurface = a.getNativeSurface();
        final boolean surfaceLocked = false; // NativeSurface.LOCK_SURFACE_NOT_READY < aSurface.lockSurface();
        try {
            final int aSz = a.getGLEventListenerCount();

            // Create new AbstractGraphicsScreen w/ cloned AbstractGraphicsDevice for future GLAutoDrawable
            // allowing this AbstractGraphicsDevice to loose ownership -> not closing display/device!
            final AbstractGraphicsConfiguration aCfg = aSurface.getGraphicsConfiguration();
            final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) aCfg.getChosenCapabilities();
            final AbstractGraphicsDevice aDevice1 = aCfg.getScreen().getDevice();
            final AbstractGraphicsDevice aDevice2 = cloneDevice(aDevice1);
            aDevice1.clearHandleOwner();  // don't close device handle
            if( DEBUG ) {
                System.err.println("GLEventListenerState.moveFrom.0a: orig 0x"+Integer.toHexString(aDevice1.hashCode())+", "+aDevice1);
                System.err.println("GLEventListenerState.moveFrom.0b: pres 0x"+Integer.toHexString(aDevice2.hashCode())+", "+aDevice2);
                System.err.println("GLEventListenerState.moveFrom.1: "+aSurface.getClass().getName()/*+", "+aSurface*/);
            }
            final AbstractGraphicsDevice aUpDevice2;
            final boolean proxyOwnsUpstreamDevice;
            {
                AbstractGraphicsDevice _aUpDevice2 = null;
                if(aSurface instanceof ProxySurface) {
                    final ProxySurface aProxy = (ProxySurface)aSurface;
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
                            System.err.println("GLEventListenerState.moveFrom.3c: "+aSurface.getClass().getName()+", "+aSurface);
                            System.err.println("GLEventListenerState.moveFrom.3d: "+aUpSurface.getClass().getName()/*+", "+aUpSurface+", "*/+aProxy.getUpstreamOptionBits(null).toString());
                        }
                    }
                } else {
                    proxyOwnsUpstreamDevice = false;
                }
                aUpDevice2 = _aUpDevice2;
            }

            glls = new GLEventListenerState(aUpDevice2, proxyOwnsUpstreamDevice, aDevice2, caps, a.getContext(), aSz, aAnim, aAnimStarted);

            //
            // remove and cache all GLEventListener and their init-state
            //
            for(int i=0; i<aSz; i++) {
                final GLEventListener l = a.getGLEventListener(0);
                glls.listenersInit[i] = a.getGLEventListenerInitState(l);
                glls.listeners[i] = a.removeGLEventListener( l );
            }

            //
            // trigger glFinish to sync GL ctx
            //
            a.invoke(true, glFinish);

            a.setContext( null, false );

        } finally {
            if( surfaceLocked ) {
                aSurface.unlockSurface();
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
     * Note: After this operation, the GLEventListenerState reference should be released.
     * </p>
     *
     * @param a {@link GLAutoDrawable} destination to move GLEventListenerState components to
     *
     * <!-- @throws GLException if the {@link GLAutoDrawable}'s configuration is incompatible, i.e. different {@link GLCapabilitiesImmutable}. -->
     * @throws GLException if this preserved {@link AbstractGraphicsDevice} is incompatible w/ the given destination one.
     *
     * @see #moveFrom(GLAutoDrawable)
     * @see #isOwner()
     */
    public final void moveTo(final GLAutoDrawable a) {
        final GLAnimatorControl aAnim = a.getAnimator();
        final boolean hasAnimator = null != aAnim;
        final boolean aPaused;
        if( hasAnimator ) {
            aPaused = aAnim.pause();
            aAnim.remove(a); // also handles ECT
            if( aPaused ) {
                aAnim.resume();
            }
        } else {
            aPaused = false;
        }

        final List<GLRunnable> aGLCmds = new ArrayList<GLRunnable>();
        final int aSz = listenerCount();

        final NativeSurface aSurface = a.getNativeSurface();
        final boolean surfaceLocked = false; // NativeSurface.LOCK_SURFACE_NOT_READY < aSurface.lockSurface();
        final boolean aRealized;
        try {

            final MutableGraphicsConfiguration aCfg = (MutableGraphicsConfiguration) aSurface.getGraphicsConfiguration();
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
            if(aSurface instanceof ProxySurface) {
                aProxy = (ProxySurface)aSurface;
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
            a.setContext( null, true );
            aRealized = a.isRealized();
            if( aRealized && null != aUpSurface ) {
                // Unrealize due to device dependencies of an upstream surface, e.g. EGLUpstreamSurfaceHook
                a.getDelegatedDrawable().setRealized(false);
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

            if( aRealized && null != aUpSurface ) {
                a.getDelegatedDrawable().setRealized(true);
            }
            if( DEBUG ) {
                System.err.println("GLEventListenerState.moveTo.X : has aProxy "+(null!=aProxy));
                System.err.println("GLEventListenerState.moveTo.X : has aUpSurface "+(null!=aUpSurface));
            }
            a.setContext( context, false );
        } finally {
            if( surfaceLocked ) {
                aSurface.unlockSurface();
            }
        }
        owner = false;

        //
        // Trigger GL-Viewport reset and reshape of all initialized GLEventListeners
        //
        aGLCmds.add(setViewport);
        for(int i=0; i<aSz; i++) {
            if( listenersInit[i] ) {
                aGLCmds.add(new ReshapeGLEventListener( listeners[i] ) );
            }
        }
        aGLCmds.add(glFinish);
        a.invoke(aRealized, aGLCmds); // only wait if already realized

        // add all cached GLEventListener to their destination and fix their init-state
        for(int i=0; i<aSz; i++) {
            final GLEventListener l = listeners[i];
            a.addGLEventListener( l );
            a.setGLEventListenerInitState(l, listenersInit[i]);
            listeners[i] = null;
        }

        if( hasAnimator ) {
            // prefer already bound animator
            aAnim.add(a);
            if( aPaused ) {
                aAnim.resume();
            }
        } else if ( null != anim ) {
            // use previously bound animator
            anim.add(a); // also handles ECT
            if(animStarted) {
                anim.start();
            }
        }
    }

    public static GLRunnable setViewport = new GLRunnable() {
        @Override
        public boolean run(final GLAutoDrawable drawable) {
            drawable.getGL().glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
            return true;
        }
    };

    public static GLRunnable glFinish = new GLRunnable() {
        @Override
        public boolean run(final GLAutoDrawable drawable) {
            drawable.getGL().glFinish();
            return true;
        }
    };

    public static class ReshapeGLEventListener implements GLRunnable {
        private final GLEventListener listener;
        public ReshapeGLEventListener(final GLEventListener listener) {
            this.listener = listener;
        }
        @Override
        public boolean run(final GLAutoDrawable drawable) {
            listener.reshape(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
            return true;
        }
    }
}
