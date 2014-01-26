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

package jogamp.nativewindow;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.SurfaceUpdatedListener;
import javax.media.nativewindow.UpstreamSurfaceHook;


import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

public abstract class ProxySurfaceImpl implements ProxySurface {
    private final SurfaceUpdatedHelper surfaceUpdatedHelper = new SurfaceUpdatedHelper();
    private AbstractGraphicsConfiguration config; // control access due to delegation
    private UpstreamSurfaceHook upstream;
    private long surfaceHandle_old;
    private final RecursiveLock surfaceLock = LockFactory.createRecursiveLock();
    private int implBitfield;
    private boolean upstreamSurfaceHookLifecycleEnabled;

    /**
     * @param cfg the {@link AbstractGraphicsConfiguration} to be used
     * @param upstream the {@link UpstreamSurfaceHook} to be used
     * @param ownsDevice <code>true</code> if this {@link ProxySurface} instance
     *                  owns the {@link AbstractGraphicsConfiguration}'s {@link AbstractGraphicsDevice},
     *                  otherwise <code>false</code>. Owning the device implies closing it at {@link #destroyNotify()}.
     */
    protected ProxySurfaceImpl(AbstractGraphicsConfiguration cfg, UpstreamSurfaceHook upstream, boolean ownsDevice) {
        if(null == cfg) {
            throw new IllegalArgumentException("null AbstractGraphicsConfiguration");
        }
        if(null == upstream) {
            throw new IllegalArgumentException("null UpstreamSurfaceHook");
        }
        this.config = cfg;
        this.upstream = upstream;
        this.surfaceHandle_old = 0;
        this.implBitfield = 0;
        this.upstreamSurfaceHookLifecycleEnabled = true;
        if(ownsDevice) {
            addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_DEVICE );
        }
    }

    @Override
    public NativeSurface getUpstreamSurface() { return null; }

    @Override
    public final UpstreamSurfaceHook getUpstreamSurfaceHook() { return upstream; }

    @Override
    public void setUpstreamSurfaceHook(UpstreamSurfaceHook hook) {
        if(null == hook) {
            throw new IllegalArgumentException("null UpstreamSurfaceHook");
        }
        upstream = hook;
    }

    @Override
    public final void enableUpstreamSurfaceHookLifecycle(boolean enable) {
        upstreamSurfaceHookLifecycleEnabled = enable;
    }

    @Override
    public void createNotify() {
        if(upstreamSurfaceHookLifecycleEnabled) {
            upstream.create(this);
        }
        this.surfaceHandle_old = 0;
    }

    @Override
    public void destroyNotify() {
        if(upstreamSurfaceHookLifecycleEnabled) {
            upstream.destroy(this);
            if( containsUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_DEVICE ) ) {
                final AbstractGraphicsDevice aDevice = getGraphicsConfiguration().getScreen().getDevice();
                aDevice.close();
                clearUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_DEVICE );
            }
            invalidateImpl();
        }
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
    public final AbstractGraphicsConfiguration getGraphicsConfiguration() {
        return config.getNativeGraphicsConfiguration();
    }

    @Override
    public final long getDisplayHandle() {
        return config.getNativeGraphicsConfiguration().getScreen().getDevice().getHandle();
    }

    @Override
    public final void setGraphicsConfiguration(AbstractGraphicsConfiguration cfg) {
        config = cfg;
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
        return upstream.getWidth(this);
    }

    @Override
    public final int getHeight() {
        return upstream.getHeight(this);
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
                            System.err.println("ProxySurfaceImpl: surface change 0x"+Long.toHexString(surfaceHandle_old)+" -> 0x"+Long.toHexString(getSurfaceHandle()));
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
    public final StringBuilder getUpstreamOptionBits(StringBuilder sink) {
        if(null == sink) {
            sink = new StringBuilder();
        }
        sink.append("UOB[ ");
        if(0 == implBitfield) {
            sink.append("]");
            return sink;
        }
        boolean needsOr = false;
        if( 0 != ( implBitfield & OPT_PROXY_OWNS_UPSTREAM_SURFACE ) ) {
            sink.append("OWNS_SURFACE");
            needsOr = true;
        }
        if( 0 != ( implBitfield & OPT_PROXY_OWNS_UPSTREAM_DEVICE ) ) {
            if(needsOr) {
                sink.append(" | ");
            }
            sink.append("OWNS_DEVICE");
            needsOr = true;
        }
        if( 0 != ( implBitfield & OPT_UPSTREAM_WINDOW_INVISIBLE ) ) {
            if(needsOr) {
                sink.append(" | ");
            }
            sink.append("WINDOW_INVISIBLE");
            needsOr = true;
        }
        sink.append(" ]");
        return sink;
    }

    @Override
    public final int getUpstreamOptionBits() { return implBitfield; }

    @Override
    public final boolean containsUpstreamOptionBits(int v) {
        return v == ( implBitfield & v ) ;
    }

    @Override
    public final void addUpstreamOptionBits(int v) { implBitfield |= v; }

    @Override
    public final void clearUpstreamOptionBits(int v) { implBitfield &= ~v; }

    @Override
    public StringBuilder toString(StringBuilder sink) {
        if(null == sink) {
            sink = new StringBuilder();
        }
        sink.append("displayHandle 0x" + Long.toHexString(getDisplayHandle())).
        append("\n, surfaceHandle 0x" + Long.toHexString(getSurfaceHandle())).
        append("\n, size " + getWidth() + "x" + getHeight()).append("\n, ");
        getUpstreamOptionBits(sink);
        sink.append("\n, "+config).
        append("\n, surfaceLock "+surfaceLock+"\n, ").
        append(getUpstreamSurfaceHook()).
        append("\n, upstreamSurface "+(null != getUpstreamSurface()));
        // append("\n, upstreamSurface "+getUpstreamSurface());
        return sink;
    }

    @Override
    public String toString() {
        StringBuilder msg = new StringBuilder();
        msg.append(getClass().getSimpleName()).append("[ ");
        toString(msg);
        msg.append(" ]");
        return msg.toString();
    }

}
