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

package jogamp.newt.driver.awt;

import java.awt.EventQueue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.nativewindow.NativeWindowException;

import com.jogamp.newt.Display;
import com.jogamp.newt.util.EDTUtil;
import jogamp.newt.Debug;

public class AWTEDTUtil implements EDTUtil {
    public static final boolean DEBUG = Debug.debug("EDT");

    private static Timer pumpMessagesTimer=null;
    private static TimerTask pumpMessagesTimerTask=null;
    private static final Map<Display, Runnable> pumpMessageDisplayMap = new HashMap<Display, Runnable>();
    private static AWTEDTUtil singletonMainThread = new AWTEDTUtil(); // one singleton MainThread
    private static long pollPeriod = EDTUtil.defaultEDTPollPeriod;
    
    public static AWTEDTUtil getSingleton() {
        return singletonMainThread;
    }

    AWTEDTUtil() {
        // package private access ..
    }

    final public long getPollPeriod() {
        return pollPeriod;
    }

    final public void setPollPeriod(long ms) {
        pollPeriod = ms;
    }
    
    final public void reset() {
        // nop AWT is always running
    }

    final public void start() {
        // nop AWT is always running
    }

    final public boolean isCurrentThreadEDT() {
        return EventQueue.isDispatchThread();
    }

    final public boolean isRunning() {
        return true; // AWT is always running
    }

    final public void invokeStop(Runnable r) {
        invoke(true, r); // AWT is always running
    }

    final public void invoke(boolean wait, Runnable r) {
        if(r == null) {
            return;
        }

        // handover to AWT MainThread ..
        try {
            if ( isCurrentThreadEDT() ) {
                r.run();
                return;
            }
            if(wait) {
                EventQueue.invokeAndWait(r);
            } else {
                EventQueue.invokeLater(r);
            }
        } catch (Exception e) {
            throw new NativeWindowException(e);
        }
    }

    final public void waitUntilIdle() {
        // wait until previous events are processed, at least ..
        try {
            EventQueue.invokeAndWait( new Runnable() {
                public void run() { }
            });
        } catch (Exception e) { }
    }

    final public void waitUntilStopped() {
        // nop: AWT is always running
    }

    public static void addPumpMessage(Display dpy, Runnable pumpMessage) {
        if(DEBUG) {
            System.err.println("AWTEDTUtil.addPumpMessage(): "+Thread.currentThread().getName()+" - dpy "+dpy);
        }
        
        synchronized (pumpMessageDisplayMap) {
            if(null == pumpMessagesTimer) {
                // AWT pump messages .. MAIN_THREAD uses main thread
                pumpMessagesTimer = new Timer();
                pumpMessagesTimerTask = new TimerTask() {
                    public void run() {
                        synchronized(pumpMessageDisplayMap) {
                            for(Iterator<Runnable> i = pumpMessageDisplayMap.values().iterator(); i.hasNext(); ) {
                                AWTEDTUtil.getSingleton().invoke(true, i.next());
                                // AWTEDTUtil.getSingleton().invoke(false, i.next());
                                // i.next().run();
                            }
                        }
                    }
                };
                pumpMessagesTimer.scheduleAtFixedRate(pumpMessagesTimerTask, 0, pollPeriod);
            }
            pumpMessageDisplayMap.put(dpy, pumpMessage);
        }
    }

}


