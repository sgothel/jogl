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
package jogamp.opengl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.opengl.GLProfile;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.opengl.GLRendererQuirks;

public class SharedResourceRunner implements Runnable {
    protected static final boolean DEBUG = GLDrawableImpl.DEBUG;

    public static interface Resource {
      boolean isAvailable();
      AbstractGraphicsDevice getDevice();
      AbstractGraphicsScreen getScreen();
      GLDrawableImpl getDrawable();
      GLContextImpl getContext();
      GLRendererQuirks getRendererQuirks(GLProfile glp);
    }

    public static interface Implementation {
        /**
         * <p>
         * Called within synchronized block.
         * </p>
         * @param device for creation a {@link AbstractGraphicsDevice} instance.
         * @return <code>true</code> if the device supports all protocols required for the implementation, otherwise <code>false</code>.
         */
        boolean isDeviceSupported(final AbstractGraphicsDevice device);

        /**
         * <p>
         * Called within synchronized block.
         * </p>
         * @param device for creation a {@link AbstractGraphicsDevice} instance.
         * @return A new shared resource instance
         */
        Resource createSharedResource(final AbstractGraphicsDevice device);

        /** Called within synchronized block. */
        void releaseSharedResource(Resource shared);
        /** Called within synchronized block. */
        void clear();

        /** Called within synchronized block. */
        Resource mapPut(final AbstractGraphicsDevice device, final Resource resource);
        /** Called within synchronized block. */
        Resource mapGet(final AbstractGraphicsDevice device);
        /** Called within synchronized block. */
        Collection<Resource> mapValues();
    }

    final HashSet<String> devicesTried = new HashSet<String>();
    final Implementation impl;

    Thread thread;
    boolean running;
    boolean ready;
    boolean shouldRelease;
    AbstractGraphicsDevice initDevice;
    AbstractGraphicsDevice releaseDevice;

    private boolean getDeviceTried(final AbstractGraphicsDevice device) { // synchronized call
        return devicesTried.contains(device.getConnection());
    }
    private void addDeviceTried(final AbstractGraphicsDevice device) { // synchronized call
        devicesTried.add(device.getConnection());
    }
    private void removeDeviceTried(final AbstractGraphicsDevice device) { // synchronized call
        devicesTried.remove(device.getConnection());
    }

    public SharedResourceRunner(final Implementation impl) {
        this.impl = impl;
        resetState();
    }

    private void resetState() { // synchronized call
        devicesTried.clear();
        thread = null;
        ready = false;
        running = false;
        shouldRelease = false;
        initDevice = null;
        releaseDevice = null;
    }

    /**
     * Start the shared resource runner thread, if not running.
     * <p>
     * Validate the thread upfront and release all related resource if it was killed.
     * </p>
     *
     * @return the shared resource runner thread.
     */
    public Thread start() {
        synchronized (this) {
            if(null != thread && !thread.isAlive()) {
                // thread was killed unrecognized ..
                if (DEBUG) {
                    System.err.println("SharedResourceRunner.start() - dead-old-thread cleanup - "+getThreadName());
                }
                releaseSharedResources();
                thread = null;
                running = false;
            }
            if( null == thread ) {
                if (DEBUG) {
                    System.err.println("SharedResourceRunner.start() - start new Thread - "+getThreadName());
                }
                resetState();
                thread = new Thread(this, getThreadName()+"-SharedResourceRunner");
                thread.setDaemon(true); // Allow JVM to exit, even if this one is running
                thread.start();
                while (!running) {
                    try {
                        this.wait();
                    } catch (final InterruptedException ex) { }
                }
            }
        }
        return thread;
    }

    public void stop() {
        synchronized (this) {
            if(null != thread) {
                if (DEBUG) {
                    System.err.println("SharedResourceRunner.stop() - "+getThreadName());
                }
                synchronized (this) {
                    shouldRelease = true;
                    this.notifyAll();

                    while (running) {
                        try {
                            this.wait();
                        } catch (final InterruptedException ex) { }
                    }
                }
            }
        }
    }

    public SharedResourceRunner.Resource getOrCreateShared(final AbstractGraphicsDevice device) {
        SharedResourceRunner.Resource sr = null;
        if(null != device) {
            synchronized (this) {
                start();
                sr = impl.mapGet(device);
                if (null == sr) {
                    if ( !getDeviceTried(device) ) {
                        addDeviceTried(device);
                        if (DEBUG) {
                            System.err.println("SharedResourceRunner.getOrCreateShared() " + device + ": trying - "+getThreadName());
                            ExceptionUtils.dumpStack(System.err);
                        }
                        if ( impl.isDeviceSupported(device) ) {
                            doAndWait(device, null);
                            sr = impl.mapGet(device);
                        }
                        if (DEBUG) {
                            System.err.println("SharedResourceRunner.getOrCreateShared() " + device + ": "+ ( ( null != sr ) ? "success" : "failed" ) +" - "+getThreadName());
                        }
                    }
                }
            }
        }
        return sr;
    }

    public SharedResourceRunner.Resource releaseShared(final AbstractGraphicsDevice device) {
        SharedResourceRunner.Resource sr = null;
        if(null != device) {
            synchronized (this) {
                sr = impl.mapGet(device);
                if (null != sr) {
                    removeDeviceTried(device);
                    if (DEBUG) {
                        System.err.println("SharedResourceRunner.releaseShared() " + device + ": trying - "+getThreadName());
                    }
                    doAndWait(null, device);
                    if (DEBUG) {
                        System.err.println("SharedResourceRunner.releaseShared() " + device + ": done - "+getThreadName());
                    }
                }
            }
        }
        return sr;
    }

    private final void doAndWait(final AbstractGraphicsDevice initDevice, final AbstractGraphicsDevice releaseDevice) {
        synchronized (this) {
            // wait until thread becomes ready to init new device,
            // pass the device and release the sync
            final String threadName = getThreadName();
            if (DEBUG) {
                System.err.println("SharedResourceRunner.doAndWait() START init: " + initDevice + ", release: "+releaseDevice+" - "+threadName);
            }
            while (!ready && running) {
                try {
                    this.wait();
                } catch (final InterruptedException ex) { }
            }
            if (DEBUG) {
                System.err.println("SharedResourceRunner.doAndWait() set command: " + initDevice + ", release: "+releaseDevice+" - "+threadName);
            }
            this.initDevice = initDevice;
            this.releaseDevice = releaseDevice;
            this.notifyAll();

            // wait until thread has init/released the device
            while ( running && ( !ready || null != this.initDevice || null != this.releaseDevice ) ) {
                try {
                    this.wait();
                } catch (final InterruptedException ex) { }
            }
            if (DEBUG) {
                System.err.println("SharedResourceRunner.initializeAndWait END init: " + initDevice + ", release: "+releaseDevice+" - "+threadName);
            }
        }
        // done
    }

    @Override
    public final void run() {
        final String threadName = getThreadName();

        if (DEBUG) {
            System.err.println("SharedResourceRunner.run(): STARTED - " + threadName);
        }

        synchronized (this) {
            running = true;

            while (!shouldRelease) {
                try {
                    // wait for stop or init
                    ready = true;
                    if (DEBUG) {
                        System.err.println("SharedResourceRunner.run(): READY - " + threadName);
                    }
                    notifyAll();
                    this.wait();
                } catch (final InterruptedException ex) {
                    shouldRelease = true;
                    if(DEBUG) {
                        System.err.println("SharedResourceRunner.run(): INTERRUPTED - "+threadName);
                        ex.printStackTrace();
                    }
                }
                ready = false;

                if (!shouldRelease) {
                    if (DEBUG) {
                        System.err.println("SharedResourceRunner.run(): WOKE UP for device connection init: " + initDevice +
                                           ", release: " + releaseDevice + " - " + threadName);
                    }
                    if(null != initDevice) {
                        if (DEBUG) {
                            System.err.println("SharedResourceRunner.run(): create Shared for: " + initDevice + " - " + threadName);
                        }
                        Resource sr = null;
                        try {
                            sr = impl.createSharedResource(initDevice);
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                        if (null != sr) {
                            impl.mapPut(initDevice, sr);
                        }
                    }
                    if(null != releaseDevice) {
                        if (DEBUG) {
                            System.err.println("SharedResourceRunner.run(): release Shared for: " + releaseDevice + " - " + threadName);
                        }
                        final Resource sr = impl.mapGet(releaseDevice);
                        if (null != sr) {
                            try {
                                impl.releaseSharedResource(sr);
                                impl.mapPut(releaseDevice, null);
                            } catch (final Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                initDevice = null;
                releaseDevice = null;
            }

            if (DEBUG) {
                System.err.println("SharedResourceRunner.run(): RELEASE START - " + threadName);
            }

            releaseSharedResources();

            if (DEBUG) {
                System.err.println("SharedResourceRunner.run(): RELEASE END - " + threadName);
            }

            shouldRelease = false;
            running = false;
            thread = null;
            notifyAll();
        }
    }

    private void releaseSharedResources() { // synchronized call
        devicesTried.clear();
        final Collection<Resource> sharedResources = impl.mapValues();
        for (final Iterator<Resource> iter = sharedResources.iterator(); iter.hasNext();) {
            try {
                impl.releaseSharedResource(iter.next());
            } catch (final Throwable t) {
                System.err.println("Caught exception on thread "+getThreadName());
                t.printStackTrace();
            }
        }
        impl.clear();
    }

    protected static String getThreadName() { return Thread.currentThread().getName(); }
}
