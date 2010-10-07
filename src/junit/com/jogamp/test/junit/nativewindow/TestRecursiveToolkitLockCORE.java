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
 
package com.jogamp.test.junit.nativewindow;

import java.lang.reflect.*;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import com.jogamp.nativewindow.impl.RecursiveToolkitLock;

public class TestRecursiveToolkitLockCORE {

    static final int YIELD_NONE = 0;
    static final int YIELD_YIELD = 1;
    static final int YIELD_SLEEP = 2;

    static void yield(int mode) {
        switch(mode) {
            case YIELD_YIELD:
                Thread.yield();
                break;
            case YIELD_SLEEP:
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                break;
            default:
                break;
        }

    }

    static class LockedObject {
        static final boolean DEBUG = false;

        public LockedObject() {
            locker = new RecursiveToolkitLock();
            actionCounter = 0;
        }

        public final void action1Direct(int l, int yieldMode) {
            if(DEBUG) {
                System.err.print("<a1");
            }
            lock();
            try {
                if(DEBUG) {
                    System.err.print("+");
                }
                while(l>0) l--;
                actionCounter++;
                yield(yieldMode);
            } finally {
                if(DEBUG) {
                    System.err.print("-");
                }
                unlock();
                if(DEBUG) {
                    System.err.println(">");
                }
            }
        }

        class Action2 implements Runnable {
            int l, yieldMode;
            Action2(int l, int yieldMode) {
                this.l=l;
                this.yieldMode=yieldMode;
            }
            public void run() {
                if(DEBUG) {
                    System.err.print("[a2");
                }
                lock();
                try {
                    if(DEBUG) {
                        System.err.print("+");
                    }
                    while(l>0) l--;
                    actionCounter++;
                    yield(yieldMode);
                } finally {
                    if(DEBUG) {
                        System.err.print("-");
                    }
                    unlock();
                    if(DEBUG) {
                        System.err.println("]");
                    }
                }
            }
        }

        public final void action2Deferred(int l, int yieldMode) {
            Thread thread = new Thread(new Action2(l, yieldMode), Thread.currentThread()+"-action2Deferred");
            thread.start();
        }

        public final void lock() {
            locker.lock();
        }

        public final void unlock() {
            locker.unlock();
        }

        public final boolean isLocked() {
            return locker.isLocked();
        }

        RecursiveToolkitLock locker;
        int actionCounter;
    }

    interface LockedObjectIf extends Runnable {
        void stop();
        boolean isStopped();
        int remaining();
    }

    class LockedObjectAction1 implements LockedObjectIf {
        boolean shouldStop;
        boolean stopped;
        LockedObject lo;
        volatile int loops;
        int iloops;
        int yieldMode;

        public LockedObjectAction1(LockedObject lo, int loops, int iloops, int yieldMode) {
            this.lo = lo;
            this.loops = loops;
            this.iloops = iloops;
            this.shouldStop = false;
            this.stopped = false;
            this.yieldMode = yieldMode;
        }

        public final synchronized void stop() {
            shouldStop = true;
        }

        public final synchronized boolean isStopped() {
            return stopped;
        }

        public final int remaining() {
            return loops;
        }

        public void run() {
            while(!shouldStop && loops>0) {
                lo.action1Direct(iloops, yieldMode);
                lo.action2Deferred(iloops, yieldMode);
                loops--;
            }
            synchronized(this) {
                stopped = true;
                notifyAll();
            }
        }
    }

    protected void testLockedObjectImpl(int threadNum, int loops, int iloops, int yieldMode) throws InterruptedException {
        LockedObject lo = new LockedObject();
        LockedObjectIf[] runners = new LockedObjectIf[threadNum];
        Thread[] threads = new Thread[threadNum];
        int i;

        for(i=0; i<threadNum; i++) {
            runners[i] = new LockedObjectAction1(lo, loops, iloops, yieldMode);
            threads[i] = new Thread( runners[i], Thread.currentThread()+"-ActionThread-"+i+"/"+threadNum);
            threads[i].start();
        }

        int active;
        do {
            active = threadNum;
            for(i=0; i<threadNum; i++) {
                if(runners[i].isStopped()) {
                    active--;
                }
            }
            yield(yieldMode);
        } while(0<active);
    }

    // @Test
    public void testLockedObjectThreading1x100() throws InterruptedException {
        System.err.println("++++ TestRecursiveToolkitLockCORE.testLockedObjectThreading1x100");
        testLockedObjectImpl(1, 100, 100, YIELD_NONE);
        System.err.println("---- TestRecursiveToolkitLockCORE.testLockedObjectThreading1x100");
    }

    @Test
    public void testLockedObjectThreading200x200Yield() throws InterruptedException {
        System.err.println("++++ TestRecursiveToolkitLockCORE.testLockedObjectThreading200x200-Yield");
        testLockedObjectImpl(200, 200, 100, YIELD_YIELD);
        System.err.println("---- TestRecursiveToolkitLockCORE.testLockedObjectThreading200x200-Yield");
    }

    // @Test
    public void testLockedObjectThreading200x200Sleep() throws InterruptedException {
        System.err.println("++++ TestRecursiveToolkitLockCORE.testLockedObjectThreading200x200-Sleep");
        testLockedObjectImpl(200, 200, 100, YIELD_SLEEP);
        System.err.println("---- TestRecursiveToolkitLockCORE.testLockedObjectThreading200x200-Sleep");
    }

    @Test
    public void testLockedObjectThreading200x200None() throws InterruptedException {
        System.err.println("++++ TestRecursiveToolkitLockCORE.testLockedObjectThreading200x200-None");
        testLockedObjectImpl(200, 200, 100, YIELD_NONE);
        System.err.println("---- TestRecursiveToolkitLockCORE.testLockedObjectThreading200x200-None");
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) throws IOException {
        /*for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        System.out.println("durationPerTest: "+durationPerTest);
        */
        String tstname = TestRecursiveToolkitLockCORE.class.getName();
        /*
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } ); */
        org.junit.runner.JUnitCore.main(tstname);
    }

}
