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
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;

public class SharedResourceRunner implements Runnable {
    protected static final boolean DEBUG = GLDrawableImpl.DEBUG;

    public static interface Resource {
      AbstractGraphicsDevice getDevice();
      AbstractGraphicsScreen getScreen();
      GLDrawableImpl getDrawable();
      GLContextImpl getContext();
    }

    public static interface Implementation {
        /**
         * @param connection for creation a {@link AbstractGraphicsDevice} instance. 
         * @return A new shared resource instance 
         */
        Resource createSharedResource(String connection);
        void releaseSharedResource(Resource shared);
        void clear();

        Resource mapPut(String connection, Resource resource);
        Resource mapGet(String connection);
        Collection<Resource> mapValues();
    }

    final HashSet<String> devicesTried = new HashSet<String>();
    final Implementation impl;

    Thread thread;
    boolean ready;
    boolean released;
    boolean shouldRelease;
    String initConnection;
    String releaseConnection;

    private boolean getDeviceTried(String connection) {
        synchronized (devicesTried) {
            return devicesTried.contains(connection);
        }
    }
    private void addDeviceTried(String connection) {
        synchronized (devicesTried) {
            devicesTried.add(connection);
        }
    }
    private void removeDeviceTried(String connection) {
        synchronized (devicesTried) {
            devicesTried.remove(connection);
        }
    }

    public SharedResourceRunner(Implementation impl) {
        this.impl = impl;
        resetState();
    }
    
    private void resetState() {
        devicesTried.clear();
        thread = null;
        ready = false;
        released = false;
        shouldRelease = false;
        initConnection = null;
        releaseConnection = null;
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
        if(null != thread && !thread.isAlive()) {
            // thread was killed unrecognized ..
            if (DEBUG) {
                System.err.println("SharedResourceRunner.start() - dead-old-thread cleanup - "+Thread.currentThread().getName());
            }
            releaseSharedResources();
            thread = null;
        }        
        if(null == thread) {
            if (DEBUG) {
                System.err.println("SharedResourceRunner.start() - start new Thread - "+Thread.currentThread().getName());
            }
            resetState();
            thread = new Thread(this, Thread.currentThread().getName()+"-SharedResourceRunner");
            thread.setDaemon(true); // Allow JVM to exit, even if this one is running
            thread.start();
        }
        return thread;
    }
    
    public void stop() {
        if(null != thread) {
            if (DEBUG) {
                System.err.println("SharedResourceRunner.stop() - "+Thread.currentThread().getName());
            }
            synchronized (this) {
                shouldRelease = true;
                this.notifyAll();
    
                while (!released) {
                    try {
                        this.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
    }
    
    public SharedResourceRunner.Resource getOrCreateShared(AbstractGraphicsDevice device) {
        SharedResourceRunner.Resource sr = null;
        if(null != device) {
            start();
            final String connection = device.getConnection();
            sr = impl.mapGet(connection);
            if (null == sr && !getDeviceTried(connection)) {
                addDeviceTried(connection);
                if (DEBUG) {
                    System.err.println("SharedResourceRunner.getOrCreateShared() " + connection + ": trying - "+Thread.currentThread().getName());
                }
                doAndWait(connection, null);
                sr = impl.mapGet(connection);
                if (DEBUG) {
                    System.err.println("SharedResourceRunner.getOrCreateShared() " + connection + ": "+ ( ( null != sr ) ? "success" : "failed" ) +" - "+Thread.currentThread().getName());
                }
            }
        }
        return sr;
    }

    public SharedResourceRunner.Resource releaseShared(AbstractGraphicsDevice device) {
        SharedResourceRunner.Resource sr = null;
        if(null != device) {
            String connection = device.getConnection();
            sr = impl.mapGet(connection);    
            if (null != sr) {
                removeDeviceTried(connection);
                if (DEBUG) {
                    System.err.println("SharedResourceRunner.releaseShared() " + connection + ": trying - "+Thread.currentThread().getName());
                }
                doAndWait(null, connection);
                if (DEBUG) {
                    System.err.println("SharedResourceRunner.releaseShared() " + connection + ": done - "+Thread.currentThread().getName());
                }
            }
        }
        return sr;
    }

    private final void doAndWait(String initConnection, String releaseConnection) {
        // wait until thread becomes ready to init new device,
        // pass the device and release the sync
        final String threadName = Thread.currentThread().getName();
        if (DEBUG) {
            System.err.println("SharedResourceRunner.doAndWait() START init: " + initConnection + ", release: "+releaseConnection+" - "+threadName);
        }
        synchronized (this) {
            while (!ready) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (DEBUG) {
                System.err.println("SharedResourceRunner.doAndWait() set command: " + initConnection + ", release: "+releaseConnection+" - "+threadName);
            }
            this.initConnection = initConnection;
            this.releaseConnection = releaseConnection;
            this.notifyAll();

            // wait until thread has init/released the device
            while (!ready || null != this.initConnection || null != this.releaseConnection) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (DEBUG) {
                System.err.println("SharedResourceRunner.initializeAndWait END init: " + initConnection + ", release: "+releaseConnection+" - "+threadName);
            }
        }
        // done
    }

    public final void run() {
        final String threadName = Thread.currentThread().getName();

        if (DEBUG) {
            System.err.println("SharedResourceRunner.run(): STARTED - " + threadName);
        }

        synchronized (this) {
            while (!shouldRelease) {
                try {
                    // wait for stop or init
                    ready = true;
                    if (DEBUG) {
                        System.err.println("SharedResourceRunner.run(): READY - " + threadName);
                    }
                    notifyAll();
                    this.wait();
                } catch (InterruptedException ex) { 
                    shouldRelease = true;
                    if(DEBUG) {
                        System.err.println("SharedResourceRunner.run(): INTERRUPTED - "+Thread.currentThread().getName());                        
                        ex.printStackTrace();
                    }
                }
                ready = false;

                if (!shouldRelease) {
                    if (DEBUG) {
                        System.err.println("SharedResourceRunner.run(): WOKE UP for device connection init: " + initConnection +
                                           ", release: " + releaseConnection + " - " + threadName);
                    }
                    if(null != initConnection) {
                        if (DEBUG) {
                            System.err.println("SharedResourceRunner.run(): create Shared for: " + initConnection + " - " + threadName);
                        }
                        Resource sr = null;
                        try {
                            sr = impl.createSharedResource(initConnection);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (null != sr) {
                            impl.mapPut(initConnection, sr);
                        }
                    }
                    if(null != releaseConnection) {
                        if (DEBUG) {
                            System.err.println("SharedResourceRunner.run(): release Shared for: " + releaseConnection + " - " + threadName);
                        }
                        Resource sr = impl.mapGet(releaseConnection);
                        if (null != sr) {
                            try {
                                impl.releaseSharedResource(sr);
                                impl.mapPut(releaseConnection, null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }                        
                    }
                }
                initConnection = null;
                releaseConnection = null;
            }

            if (DEBUG) {
                System.err.println("SharedResourceRunner.run(): RELEASE START - " + threadName);
            }

            releaseSharedResources();

            if (DEBUG) {
                System.err.println("SharedResourceRunner.run(): RELEASE END - " + threadName);
            }

            shouldRelease = false;
            released = true;
            thread = null;
            notifyAll();
        }
    }

    private void releaseSharedResources() {
        synchronized (devicesTried) {
            devicesTried.clear();
        }
        Collection<Resource> sharedResources = impl.mapValues();
        for (Iterator<Resource> iter = sharedResources.iterator(); iter.hasNext();) {
            try {
                impl.releaseSharedResource(iter.next());
            } catch (Throwable t) {
                System.err.println("Catched Exception: "+t.getStackTrace()+" - "+Thread.currentThread().getName());
                t.printStackTrace();
            }
        }
        impl.clear();
    }
}
