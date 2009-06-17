/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 */
package com.sun.javafx.newt;

import javax.media.nativewindow.*;
import com.sun.javafx.newt.impl.Debug;
import java.util.*;

public class DisplayActionThread extends Thread {
    private Object taskWorkerLock=new Object();
    private boolean isRunning = false;
    private boolean shouldStop = false;
    private List/*DisplayAction*/ displayActions = new ArrayList();

    public synchronized void addAction(Display.Action da) {
        List newListeners = (List) ((ArrayList) displayActions).clone();
        newListeners.add(da);
        displayActions = newListeners;
    }

    public synchronized void removeAction(Display.Action da) {
        List newListeners = (List) ((ArrayList) displayActions).clone();
        newListeners.remove(da);
        displayActions = newListeners;
    }

    public boolean isRunning() {
        synchronized(taskWorkerLock) { 
            return isRunning;
        }
    }

    public void exit() {
        synchronized(taskWorkerLock) {
            if(isRunning) {
                shouldStop = true;
                taskWorkerLock.notifyAll();
            }
        }
        Map displayMap = Display.getCurrentDisplayMap();
        synchronized(displayMap) {
            displayMap.notifyAll();
        }
    }

    public void start() { 
        synchronized(taskWorkerLock) {
            if(!isRunning) {
                shouldStop = false;
                taskWorkerLock.notifyAll();
                super.start();
            }
        }
    }

    public void run() {
        synchronized(taskWorkerLock) {
            isRunning = true;
            taskWorkerLock.notifyAll();
        }
        while(!shouldStop) {
            Map displayMap = Display.getCurrentDisplayMap();
            // wait for something todo ..
            synchronized(displayMap) {
                while(!shouldStop && displayMap.size()==0) {
                    try {
                        displayMap.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            Iterator iter = Display.getCurrentDisplays().iterator();
            while(iter.hasNext()) {
                Display display = (Display) iter.next();
                Iterator iterDA = displayActions.iterator();
                while(iterDA.hasNext()) {
                    ((Display.Action)iterDA.next()).run(display);
                }
            }
        }
        synchronized(taskWorkerLock) {
            isRunning = false;
            taskWorkerLock.notifyAll();
        }
    }
}
