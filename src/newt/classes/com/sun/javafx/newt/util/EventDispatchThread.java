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

package com.sun.javafx.newt.util;

import com.sun.javafx.newt.Display;
import com.sun.javafx.newt.impl.Debug;
import java.util.*;

public class EventDispatchThread {
    public static final boolean DEBUG = Debug.debug("EDT");

    private ThreadGroup threadGroup; 
    private volatile boolean shouldStop = false;
    private TaskWorker taskWorker = null;
    private Object taskWorkerLock = new Object();
    private ArrayList tasks = new ArrayList(); // one shot tasks
    private Display display = null;
    private String name;
    private long edtPollGranularity = 10;

    public EventDispatchThread(Display display, ThreadGroup tg, String name) {
        this.display = display;
        this.threadGroup = tg;
        this.name=new String("EDT-Display_"+display.getName()+"-"+name);
    }

    public String getName() { return name; }

    public ThreadGroup getThreadGroup() { return threadGroup; }

    public void start() {
        start(false);
    }

    /**
     * @param externalStimuli true indicates that another thread stimulates, 
     *                        ie. calls this TaskManager's run() loop method.
     *                        Hence no own thread is started in this case.
     *
     * @return The started Runnable, which handles the run-loop.
     *         Usefull in combination with externalStimuli=true,
     *         so an external stimuli can call it.
     */
    public Runnable start(boolean externalStimuli) {
        synchronized(taskWorkerLock) { 
            if(null==taskWorker) {
                taskWorker = new TaskWorker(threadGroup, name);
            }
            if(!taskWorker.isRunning()) {
                shouldStop = false;
                taskWorker.start(externalStimuli);
            }
            taskWorkerLock.notifyAll();
        }
        return taskWorker;
    }

    public void stop() {
        synchronized(taskWorkerLock) { 
            if(null!=taskWorker && taskWorker.isRunning()) {
                shouldStop = true;
            }
            taskWorkerLock.notifyAll();
            if(DEBUG) {
                System.out.println(Thread.currentThread()+": EDT signal STOP");
            }
        }
    }

    public boolean isThreadEDT(Thread thread) {
        return null!=taskWorker && taskWorker == thread;
    }

    public boolean isCurrentThreadEDT() {
        return null!=taskWorker && taskWorker == Thread.currentThread();
    }

    public boolean isRunning() {
        return null!=taskWorker && taskWorker.isRunning() ;
    }

    public void invokeLater(Runnable task) {
        if(task == null) {
            return;
        }
        synchronized(taskWorkerLock) {
            if(null!=taskWorker && taskWorker.isRunning() && taskWorker != Thread.currentThread() ) {
                tasks.add(task);
                taskWorkerLock.notifyAll();
            } else {
                // if !running or isEDTThread, do it right away
                task.run();
            }
        }
    }

    public void invokeAndWait(Runnable task) {
        if(task == null) {
            return;
        }
        invokeLater(task);
        waitOnWorker();
    }

    public void waitOnWorker() {
        synchronized(taskWorkerLock) {
            if(null!=taskWorker && taskWorker.isRunning() && tasks.size()>0 && taskWorker != Thread.currentThread() ) {
                try {
                    taskWorkerLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void waitUntilStopped() {
        synchronized(taskWorkerLock) {
            while(null!=taskWorker && taskWorker.isRunning() && taskWorker != Thread.currentThread() ) {
                try {
                    taskWorkerLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class TaskWorker extends Thread {
        boolean isRunning = false;
        boolean externalStimuli = false;

        public TaskWorker(ThreadGroup tg, String name) {
            super(tg, name);
        }

        public synchronized boolean isRunning() {
            return isRunning;
        }

        public void start(boolean externalStimuli) throws IllegalThreadStateException {
            synchronized(this) {
                this.externalStimuli = externalStimuli;
                isRunning = true;
            }
            if(!externalStimuli) {
                super.start();
            }
        }

        /** 
         * Utilizing taskWorkerLock only for local resources and task execution,
         * not for event dispatching.
         */
        public void run() {
            if(DEBUG) {
                System.out.println(Thread.currentThread()+": EDT run() START");
            }
            while(!shouldStop) {
                try {
                    // wait for something todo
                    while(!shouldStop && tasks.size()==0) {
                        synchronized(taskWorkerLock) {
                            if(!shouldStop && tasks.size()==0) {
                                try {
                                    taskWorkerLock.wait(edtPollGranularity);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        display.pumpMessages(); // event dispatch
                    }
                    if(!shouldStop && tasks.size()>0) {
                        synchronized(taskWorkerLock) {
                            if(!shouldStop && tasks.size()>0) {
                                Runnable task = (Runnable) tasks.remove(0);
                                task.run();
                                taskWorkerLock.notifyAll();
                            }
                        }
                        display.pumpMessages(); // event dispatch
                    }
                } catch (Throwable t) {
                    // handle errors ..
                    t.printStackTrace();
                } finally {
                    // epilog - unlock locked stuff
                }
                if(externalStimuli) break; // no loop if called by external stimuli
            }
            synchronized(this) {
                isRunning = !shouldStop;
            }
            if(!isRunning) {
                synchronized(taskWorkerLock) {
                    taskWorkerLock.notifyAll();
                }
            }
            if(DEBUG) {
                System.out.println(Thread.currentThread()+": EDT run() EXIT");
            }
        }
    }
}

