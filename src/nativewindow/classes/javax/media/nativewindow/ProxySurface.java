/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package javax.media.nativewindow;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.SurfaceUpdatedHelper;

import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

public abstract class ProxySurface implements NativeSurface, MutableSurface {
    public static final boolean DEBUG = Debug.debug("ProxySurface");
    
    /** 
     * Implementation specific bitvalue stating the upstream's {@link AbstractGraphicsDevice} is owned by this {@link ProxySurface}.
     * @see #setImplBitfield(int)
     * @see #getImplBitfield()
     */ 
    public static final int OWN_DEVICE = 1 << 7;
    
    /** 
     * Implementation specific bitvalue stating the upstream's {@link NativeSurface} is an invisible window, i.e. maybe incomplete.
     * @see #setImplBitfield(int)
     * @see #getImplBitfield()
     */ 
    public static final int INVISIBLE_WINDOW = 1 << 8;

    /** Interface allowing upstream caller to pass lifecycle actions and size info to a {@link ProxySurface} instance. */ 
    public interface UpstreamSurfaceHook {
        /** called within {@link ProxySurface#createNotify()} within lock, before using surface. */
        public void create(ProxySurface s);
        /** called within {@link ProxySurface#destroyNotify()} within lock, before clearing fields. */
        public void destroy(ProxySurface s);

        /** Returns the width of the upstream surface */ 
        public int getWidth(ProxySurface s);
        /** Returns the height of the upstream surface */ 
        public int getHeight(ProxySurface s);
    }
    
    private final SurfaceUpdatedHelper surfaceUpdatedHelper = new SurfaceUpdatedHelper();
    private final AbstractGraphicsConfiguration config; // control access due to delegation
    private final UpstreamSurfaceHook upstream;
    public final int initialWidth;
    public final int initialHeight;
    private long surfaceHandle_old;
    protected RecursiveLock surfaceLock = LockFactory.createRecursiveLock();
    protected long displayHandle;
    protected int scrnIndex;
    protected int implBitfield;

    /**
     * @param cfg the {@link AbstractGraphicsConfiguration} to be used
     * @param initialWidth the initial width
     * @param initialHeight the initial height
     */
    protected ProxySurface(AbstractGraphicsConfiguration cfg, int initialWidth, int initialHeight, UpstreamSurfaceHook upstream) {
        if(null == cfg) {
            throw new IllegalArgumentException("null config");
        }
        this.config = cfg;
        this.upstream = upstream;
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
        this.displayHandle=config.getNativeGraphicsConfiguration().getScreen().getDevice().getHandle();
        this.surfaceHandle_old = 0;
        this.implBitfield = 0;
    }

    public final UpstreamSurfaceHook getUpstreamSurfaceHook() { return upstream; }
    
    /** 
     * If a valid {@link UpstreamSurfaceHook} instance is passed in the 
     * {@link ProxySurface#ProxySurface(AbstractGraphicsConfiguration, int, int, UpstreamSurfaceHook) constructor}, 
     * {@link UpstreamSurfaceHook#create(ProxySurface)} is being issued and the proxy surface/window handles shall be set.
     */ 
    public void createNotify() {
        if(null != upstream) {
            upstream.create(this);
        }
        this.displayHandle=config.getNativeGraphicsConfiguration().getScreen().getDevice().getHandle();
        this.surfaceHandle_old = 0;
    }
    
    /** 
     * If a valid {@link UpstreamSurfaceHook} instance is passed in the 
     * {@link ProxySurface#ProxySurface(AbstractGraphicsConfiguration, int, int, UpstreamSurfaceHook) constructor}, 
     * {@link UpstreamSurfaceHook#destroy(ProxySurface)} is being issued and all fields are cleared.
     */ 
    public void destroyNotify() {
        if(null != upstream) {
            upstream.destroy(this);
            invalidateImpl();
        }
        this.displayHandle = 0;
        this.surfaceHandle_old = 0;
    }
    
    /** 
     * Must be overridden by implementations allowing having a {@link UpstreamSurfaceHook} being passed.
     * @see #destroyNotify() 
     */
    protected void invalidateImpl() {
        throw new InternalError("UpstreamSurfaceHook given, but required method not implemented.");        
    }
    
    @Override
    public final long getDisplayHandle() {
        return displayHandle;
    }

    protected final AbstractGraphicsConfiguration getPrivateGraphicsConfiguration() {
        return config;
    }

    @Override
    public final AbstractGraphicsConfiguration getGraphicsConfiguration() {
        return config.getNativeGraphicsConfiguration();
    }

    @Override
    public final int getScreenIndex() {
        return getGraphicsConfiguration().getScreen().getIndex();
    }

    @Override
    public abstract long getSurfaceHandle();

    @Override
    public abstract void setSurfaceHandle(long surfaceHandle);
    
    @Override
    public final int getWidth() {
        if(null != upstream) {
            return upstream.getWidth(this);
        }
        return initialWidth;
    }

    @Override
    public final int getHeight() {
        if(null != upstream) {
            return upstream.getHeight(this);
        }
        return initialHeight;
    }

    @Override
    public boolean surfaceSwap() {
        return false;
    }

    @Override
    public void addSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        surfaceUpdatedHelper.addSurfaceUpdatedListener(l);
    }

    @Override
    public void addSurfaceUpdatedListener(int index, SurfaceUpdatedListener l) throws IndexOutOfBoundsException {
        surfaceUpdatedHelper.addSurfaceUpdatedListener(index, l);
    }

    @Override
    public void removeSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        surfaceUpdatedHelper.removeSurfaceUpdatedListener(l);
    }

    @Override
    public void surfaceUpdated(Object updater, NativeSurface ns, long when) {
        surfaceUpdatedHelper.surfaceUpdated(updater, ns, when);
    }

    @Override
    public int lockSurface() throws NativeWindowException, RuntimeException  {
        surfaceLock.lock();
        int res = surfaceLock.getHoldCount() == 1 ? LOCK_SURFACE_NOT_READY : LOCK_SUCCESS; // new lock ?

        if ( LOCK_SURFACE_NOT_READY == res ) {
            try {
                final AbstractGraphicsDevice adevice = getGraphicsConfiguration().getScreen().getDevice();
                adevice.lock();
                try {
                    res = lockSurfaceImpl();
                    if(LOCK_SUCCESS == res && surfaceHandle_old != getSurfaceHandle()) {
                        res = LOCK_SURFACE_CHANGED;
                        if(DEBUG) {
                            System.err.println("ProxySurface: surface change 0x"+Long.toHexString(surfaceHandle_old)+" -> 0x"+Long.toHexString(getSurfaceHandle()));
                            // Thread.dumpStack();
                        }
                    }
                } finally {
                    if (LOCK_SURFACE_NOT_READY >= res) {
                        adevice.unlock();
                    }
                }
            } finally {
                if (LOCK_SURFACE_NOT_READY >= res) {
                    surfaceLock.unlock();
                }
            }
        }
        return res;
    }

    @Override
    public final void unlockSurface() {
        surfaceLock.validateLocked();
        surfaceHandle_old = getSurfaceHandle();

        if (surfaceLock.getHoldCount() == 1) {
            final AbstractGraphicsDevice adevice = getGraphicsConfiguration().getScreen().getDevice();
            try {
                unlockSurfaceImpl();
            } finally {
                adevice.unlock();
            }
        }
        surfaceLock.unlock();
    }

    protected abstract int lockSurfaceImpl();

    protected abstract void unlockSurfaceImpl() ;

    public final void validateSurfaceLocked() {
        surfaceLock.validateLocked();
    }

    @Override
    public final boolean isSurfaceLockedByOtherThread() {
        return surfaceLock.isLockedByOtherThread();
    }

    @Override
    public final Thread getSurfaceLockOwner() {
        return surfaceLock.getOwner();
    }
    
    @Override
    public abstract String toString();
    
    public int getImplBitfield() { return implBitfield; }    
    public void setImplBitfield(int v) { implBitfield=v; }
}
