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

import jogamp.nativewindow.SurfaceUpdatedHelper;

import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

public abstract class ProxySurface implements NativeSurface {
    private SurfaceUpdatedHelper surfaceUpdatedHelper = new SurfaceUpdatedHelper();
    private AbstractGraphicsConfiguration config; // control access due to delegation
    protected RecursiveLock surfaceLock = LockFactory.createRecursiveLock();
    protected long displayHandle;
    protected int height;
    protected int scrnIndex;
    protected int width;

    public ProxySurface(AbstractGraphicsConfiguration cfg) {
        invalidate();
        config = cfg;
        displayHandle=cfg.getNativeGraphicsConfiguration().getScreen().getDevice().getHandle();
    }

    void invalidate() {
        displayHandle = 0;
        invalidateImpl();
    }
    protected abstract void invalidateImpl();

    public final long getDisplayHandle() {
        return displayHandle;
    }

    protected final AbstractGraphicsConfiguration getPrivateGraphicsConfiguration() {
        return config;
    }
    
    public final AbstractGraphicsConfiguration getGraphicsConfiguration() {
        return config.getNativeGraphicsConfiguration();
    }

    public final int getScreenIndex() {
        return getGraphicsConfiguration().getScreen().getIndex();
    }

    public abstract long getSurfaceHandle();

    public final int getWidth() {
        return width;
    }

    public final int getHeight() {
        return height;
    }

    public void surfaceSizeChanged(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean surfaceSwap() {
        return false;
    }

    public void addSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        surfaceUpdatedHelper.addSurfaceUpdatedListener(l);
    }

    public void addSurfaceUpdatedListener(int index, SurfaceUpdatedListener l) throws IndexOutOfBoundsException {
        surfaceUpdatedHelper.addSurfaceUpdatedListener(index, l);
    }

    public void removeSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        surfaceUpdatedHelper.removeSurfaceUpdatedListener(l);
    }

    public void surfaceUpdated(Object updater, NativeSurface ns, long when) {
        surfaceUpdatedHelper.surfaceUpdated(updater, ns, when);
    }    
    
    public int lockSurface() throws NativeWindowException {
        surfaceLock.lock();
        int res = surfaceLock.getHoldCount() == 1 ? LOCK_SURFACE_NOT_READY : LOCK_SUCCESS; // new lock ?

        if ( LOCK_SURFACE_NOT_READY == res ) {
            try {
                final AbstractGraphicsDevice adevice = getGraphicsConfiguration().getScreen().getDevice();
                adevice.lock();
                try {
                    res = lockSurfaceImpl();
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

    public final void unlockSurface() {
        surfaceLock.validateLocked();

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

    public final boolean isSurfaceLocked() {
        return surfaceLock.isLocked();
    }

    public final boolean isSurfaceLockedByOtherThread() {
        return surfaceLock.isLockedByOtherThread();
    }

    public final Thread getSurfaceLockOwner() {
        return surfaceLock.getOwner();
    }

    public abstract String toString();    
}
