/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.jogamp.newt.util;

import com.jogamp.common.util.RunnableTask;
import com.jogamp.newt.Display;
import com.jogamp.newt.impl.Debug;
import java.util.*;

public class DefaultEDTUtil implements EDTUtil {
    public static final boolean DEBUG = Debug.debug("EDT");

    private ThreadGroup threadGroup; 
    private volatile boolean shouldStop = false;
    private EventDispatchThread edt = null;
    private Object edtLock = new Object();
    private ArrayList tasks = new ArrayList(); // one shot tasks
    private String name;
    private Runnable pumpMessages;

    public DefaultEDTUtil(ThreadGroup tg, String name, Runnable pumpMessages) {
        this.threadGroup = tg;
        this.name=new String(Thread.currentThread().getName()+"-"+"EDT-"+name);
        this.pumpMessages=pumpMessages;
    }

    public void start() {
        synchronized(edtLock) { 
            if(null==edt) {
                edt = new EventDispatchThread(threadGroup, name);
            }
            if(!edt.isRunning()) {
                shouldStop = false;
                edt.start();
            }
            edtLock.notifyAll();
        }
    }

    public void stop() {
        synchronized(edtLock) { 
            if(null!=edt && edt.isRunning()) {
                shouldStop = true;
            }
            edtLock.notifyAll();
            if(DEBUG) {
                System.out.println(Thread.currentThread()+": EDT signal STOP");
            }
        }
    }

    public boolean isCurrentThreadEDT() {
        return null!=edt && edt == Thread.currentThread();
    }

    public boolean isRunning() {
        return null!=edt && edt.isRunning() ;
    }

    private void invokeLater(Runnable task) {
        synchronized(edtLock) {
            if(null!=edt && edt.isRunning() && edt != Thread.currentThread() ) {
                tasks.add(task);
                edtLock.notifyAll();
            } else {
                // if !running or isEDTThread, do it right away
                task.run();
            }
        }
    }

    public void invoke(boolean wait, Runnable task) {
        if(task == null) {
            return;
        }
        boolean doWait = wait && null!=edt && edt.isRunning() && edt != Thread.currentThread();
        Object lock = new Object();
        RunnableTask rTask = new RunnableTask(task, doWait?lock:null, true);
        Throwable throwable = null;
        synchronized(lock) {
            invokeLater(rTask);
            if( doWait ) {
                try {
                    lock.wait();
                } catch (InterruptedException ie) {
                    throwable = ie;
                }
            }
        }
        if(null==throwable) {
            throwable = rTask.getThrowable();
        }
        if(null!=throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public void waitUntilIdle() {
        synchronized(edtLock) {
            if(null!=edt && edt.isRunning() && tasks.size()>0 && edt != Thread.currentThread() ) {
                try {
                    edtLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void waitUntilStopped() {
        synchronized(edtLock) {
            while(null!=edt && edt.isRunning() && edt != Thread.currentThread() ) {
                try {
                    edtLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class EventDispatchThread extends Thread {
        boolean isRunning = false;

        public EventDispatchThread(ThreadGroup tg, String name) {
            super(tg, name);
        }

        public synchronized boolean isRunning() {
            return isRunning;
        }

        public void start() throws IllegalThreadStateException {
            synchronized(this) {
                isRunning = true;
            }
            super.start();
        }

        /** 
         * Utilizing edtLock only for local resources and task execution,
         * not for event dispatching.
         */
        public void run() {
            if(DEBUG) {
                System.out.println(Thread.currentThread()+": EDT run() START");
            }
            try {
                while(!shouldStop) {
                    // wait for something todo
                    while(!shouldStop && tasks.size()==0) {
                        synchronized(edtLock) {
                            if(!shouldStop && tasks.size()==0) {
                                try {
                                    edtLock.wait(defaultEDTPollGranularity);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        pumpMessages.run(); // event dispatch
                    }
                    if(!shouldStop && tasks.size()>0) {
                        synchronized(edtLock) {
                            if(!shouldStop && tasks.size()>0) {
                                Runnable task = (Runnable) tasks.remove(0);
                                task.run(); // FIXME: could be run outside of lock
                                edtLock.notifyAll();
                            }
                        }
                        pumpMessages.run(); // event dispatch
                    }
                }
            } catch (Throwable t) {
                // handle errors ..
                shouldStop = true;
                throw new RuntimeException(t);
            } finally {
                synchronized(this) {
                    isRunning = !shouldStop;
                }
                if(!isRunning) {
                    synchronized(edtLock) {
                        edtLock.notifyAll();
                    }
                }
                if(DEBUG) {
                    System.out.println(Thread.currentThread()+": EDT run() EXIT");
                }
            }
        }
    }
}

