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
        Resource createSharedResource(String connection);
        void releaseSharedResource(Resource shared);
        void clear();

        Resource mapPut(String connection, Resource resource);
        Resource mapGet(String connection);
        Collection/*<Resource>*/ mapValues();
    }

    Implementation impl = null;

    boolean ready = false;
    boolean released = false;
    boolean shouldRelease = false;
    String initConnection = null;
    String releaseConnection = null;

    HashSet devicesTried = new HashSet();

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
    }

    public SharedResourceRunner.Resource getOrCreateShared(AbstractGraphicsDevice device) {
        SharedResourceRunner.Resource sr = null;
        if(null != device) {
            String connection = device.getConnection();
            sr = impl.mapGet(connection);
            if (null == sr && !getDeviceTried(connection)) {
                addDeviceTried(connection);
                if (DEBUG) {
                    System.err.println("getOrCreateShared() " + connection + ": trying");
                }
                doAndWait(connection, null);
                sr = impl.mapGet(connection);
                if (DEBUG) {
                    System.err.println("getOrCreateShared() " + connection + ": "+ ( ( null != sr ) ? "success" : "failed" ) );               
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
                    System.err.println("releaseShared() " + connection + ": trying");
                }
                doAndWait(null, connection);
                if (DEBUG) {
                    System.err.println("releaseShared() " + connection + ": done");
                }
            }
        }
        return sr;
    }

    private final void doAndWait(String initConnection, String releaseConnection) {
        // wait until thread becomes ready to init new device,
        // pass the device and release the sync
        String threadName = Thread.currentThread().getName();
        if (DEBUG) {
            System.err.println(threadName + " doAndWait START init: " + initConnection + ", release: "+releaseConnection);
        }
        synchronized (this) {
            while (!ready) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (DEBUG) {
                System.err.println(threadName + " initializeAndWait set command init: " + initConnection + ", release: "+releaseConnection);
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
                System.err.println(threadName + " initializeAndWait END init: " + initConnection + ", release: "+releaseConnection);
            }
        }
        // done
    }

    public final void releaseAndWait() {
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

    public final void run() {
        String threadName = Thread.currentThread().getName();

        if (DEBUG) {
            System.err.println(threadName + " STARTED");
        }

        synchronized (this) {
            while (!shouldRelease) {
                try {
                    // wait for stop or init
                    ready = true;
                    if (DEBUG) {
                        System.err.println(threadName + " -> ready");
                    }
                    notifyAll();
                    this.wait();
                } catch (InterruptedException ex) { }
                ready = false;

                if (!shouldRelease) {
                    if (DEBUG) {
                        System.err.println(threadName + " woke up for device connection init: " + initConnection +
                                                        ", release: " + releaseConnection);
                    }
                    if(null != initConnection) {
                        if (DEBUG) {
                            System.err.println(threadName + " create Shared for: " + initConnection);
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
                            System.err.println(threadName + " release Shared for: " + releaseConnection);
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
                System.err.println(threadName + " release START");
            }

            releaseSharedResources();

            if (DEBUG) {
                System.err.println(threadName + " release END");
            }

            released = true;
            ready = false;
            notifyAll();
        }
    }

    private void releaseSharedResources() {
        Collection/*<Resource>*/ sharedResources = impl.mapValues();
        for (Iterator iter = sharedResources.iterator(); iter.hasNext();) {
            Resource sr = (Resource) iter.next();
            impl.releaseSharedResource(sr);
        }
        impl.clear();
    }
}
