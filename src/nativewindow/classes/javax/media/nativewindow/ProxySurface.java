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

import com.jogamp.common.util.locks.RecursiveLock;

public abstract class ProxySurface implements NativeSurface {
    protected RecursiveLock recurLock = new RecursiveLock();
    protected AbstractGraphicsConfiguration config;
    protected long displayHandle;
    protected int height;
    protected int scrnIndex;
    protected int width;

    public ProxySurface(AbstractGraphicsConfiguration cfg) {
        invalidate();
        config = cfg;
        displayHandle=cfg.getScreen().getDevice().getHandle();
    }

    public final void invalidate() {
        recurLock.lock();
        try {
            displayHandle = 0;
            invalidateImpl();
        } finally {
            recurLock.unlock();
        }
    }
    protected abstract void invalidateImpl();

    public final long getDisplayHandle() {
        return displayHandle;
    }

    public final AbstractGraphicsConfiguration getGraphicsConfiguration() {
        return config;
    }

    public final int getScreenIndex() {
        return config.getScreen().getIndex();
    }

    public abstract long getSurfaceHandle();

    public final int getWidth() {
        return width;
    }

    public final int getHeight() {
        return height;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean surfaceSwap() {
        return false;
    }

    public void surfaceUpdated(Object updater, NativeSurface ns, long when) {
    }

    public int lockSurface() throws NativeWindowException {
        recurLock.lock();
        int res = recurLock.getRecursionCount() == 0 ? LOCK_SURFACE_NOT_READY : LOCK_SUCCESS;

        if ( LOCK_SURFACE_NOT_READY == res ) {
            try {
                final AbstractGraphicsDevice adevice = config.getScreen().getDevice();
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
                    recurLock.unlock();
                }
            }
        }
        return res;
    }

    public final void unlockSurface() {
        recurLock.validateLocked();

        if (recurLock.getRecursionCount() == 0) {
            final AbstractGraphicsDevice adevice = config.getScreen().getDevice();
            try {
                unlockSurfaceImpl();
            } finally {
                adevice.unlock();
            }
        }
        recurLock.unlock();
    }

    protected abstract int lockSurfaceImpl();

    protected abstract void unlockSurfaceImpl() ;

    public final void validateSurfaceLocked() {
        recurLock.validateLocked();
    }

    public final boolean isSurfaceLocked() {
        return recurLock.isLocked();
    }

    public final boolean isSurfaceLockedByOtherThread() {
        return recurLock.isLockedByOtherThread();
    }

    public final Thread getSurfaceLockOwner() {
        return recurLock.getOwner();
    }

    public final int getSurfaceRecursionCount() {
        return recurLock.getRecursionCount();
    }

    public abstract String toString();
}
