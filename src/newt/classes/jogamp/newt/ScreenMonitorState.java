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

package jogamp.newt;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Screen;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.event.MonitorEvent;
import com.jogamp.newt.event.MonitorModeListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ScreenMonitorState {
    private static boolean DEBUG = Screen.DEBUG;

    private final RecursiveLock lock = LockFactory.createRecursiveLock();
    private final ArrayHashSet<MonitorDevice> allMonitors;
    private final ArrayHashSet<MonitorMode> allMonitorModes;
    private final MonitorDevice primaryMonitor;
    private final ArrayList<MonitorModeListener> listener = new ArrayList<MonitorModeListener>();

    private static HashMap<String, ScreenMonitorState> screenFQN2ScreenMonitorState = new HashMap<String, ScreenMonitorState>();
    private static RecursiveLock screen2ScreenMonitorState = LockFactory.createRecursiveLock();

    protected static void mapScreenMonitorState(final String screenFQN, final ScreenMonitorState sms) {
        screen2ScreenMonitorState.lock();
        try {
            final ScreenMonitorState _sms = screenFQN2ScreenMonitorState.get(screenFQN);
            if( null != _sms ) {
                throw new RuntimeException("ScreenMonitorState "+_sms+" already mapped to "+screenFQN);
            }
            screenFQN2ScreenMonitorState.put(screenFQN, sms);
            if(DEBUG) {
                System.err.println("ScreenMonitorState.map "+screenFQN+" -> "+sms);
            }
        } finally {
            screen2ScreenMonitorState.unlock();
        }
    }

    /**
     * @param screen the prev user
     * @return true if mapping is empty, ie no more usage of the mapped ScreenMonitorState
     */
    protected static void unmapScreenMonitorState(final String screenFQN) {
        screen2ScreenMonitorState.lock();
        try {
            unmapScreenMonitorStateUnlocked(screenFQN);
        } finally {
            screen2ScreenMonitorState.unlock();
        }
    }
    protected static void unmapScreenMonitorStateUnlocked(final String screenFQN) {
        final ScreenMonitorState sms = screenFQN2ScreenMonitorState.remove(screenFQN);
        if(DEBUG) {
            System.err.println("ScreenMonitorState.unmap "+screenFQN+" -> "+sms);
        }
    }

    protected static ScreenMonitorState getScreenMonitorState(final String screenFQN) {
        screen2ScreenMonitorState.lock();
        try {
            return getScreenMonitorStateUnlocked(screenFQN);
        } finally {
            screen2ScreenMonitorState.unlock();
        }
    }
    protected static ScreenMonitorState getScreenMonitorStateUnlocked(final String screenFQN) {
        return screenFQN2ScreenMonitorState.get(screenFQN);
    }

    protected static void lockScreenMonitorState() {
        screen2ScreenMonitorState.lock();
    }

    protected static void unlockScreenMonitorState() {
        screen2ScreenMonitorState.unlock();
    }

    public ScreenMonitorState(final ArrayHashSet<MonitorDevice> allMonitors,
                              final ArrayHashSet<MonitorMode> allMonitorModes,
                              final MonitorDevice primaryMonitor) {
        this.allMonitors = allMonitors;
        this.allMonitorModes = allMonitorModes;
        this.primaryMonitor = primaryMonitor;
    }

    protected ArrayHashSet<MonitorDevice> getMonitorDevices() {
        return allMonitors;
    }

    protected MonitorDevice getPrimaryMonitorDevice() {
        return primaryMonitor;
    }

    protected ArrayHashSet<MonitorMode> getMonitorModes() {
        return allMonitorModes;
    }

    protected final int addListener(final MonitorModeListener l) {
        lock();
        try {
            listener.add(l);
            if(DEBUG) {
                System.err.println("ScreenMonitorState.addListener (size: "+listener.size()+"): "+l);
            }
            return listener.size();
        } finally {
            unlock();
        }
    }

    protected final int removeListener(final MonitorModeListener l) {
        lock();
        try {
            if(!listener.remove(l)) {
                throw new RuntimeException("MonitorModeListener "+l+" not contained");
            }
            if(DEBUG) {
                System.err.println("ScreenMonitorState.removeListener (size: "+listener.size()+"): "+l);
            }
            return listener.size();
        } finally {
            unlock();
        }
    }

    protected final MonitorDevice getMonitor(final MonitorDevice monitor) {
        return allMonitors.get(monitor);
    }

    protected final void validateMonitor(final MonitorDevice monitor) {
        final MonitorDevice md = allMonitors.get(monitor);
        if( null == md ) {
            throw new InternalError("Monitor unknown: "+monitor);
        }
    }

    protected final void fireMonitorModeChangeNotify(final MonitorDevice monitor, final MonitorMode desiredMode) {
        lock();
        try {
            validateMonitor(monitor);
            final MonitorEvent me = new MonitorEvent(MonitorEvent.EVENT_MONITOR_MODE_CHANGE_NOTIFY, monitor, System.currentTimeMillis(), desiredMode);
            for(int i=0; i<listener.size(); i++) {
                listener.get(i).monitorModeChangeNotify(me);
            }
        } finally {
            unlock();
        }
    }

    protected void fireMonitorModeChanged(final MonitorDevice monitor, final MonitorMode currentMode, final boolean success) {
        lock();
        try {
            validateMonitor(monitor);
            final MonitorEvent me = new MonitorEvent(MonitorEvent.EVENT_MONITOR_MODE_CHANGED, monitor, System.currentTimeMillis(), currentMode);
            for(int i=0; i<listener.size(); i++) {
                listener.get(i).monitorModeChanged(me, success);
            }
        } finally {
            unlock();
        }
    }

    protected final void lock() throws RuntimeException {
        lock.lock();
    }

    protected final void unlock() throws RuntimeException {
        lock.unlock();
    }
}
