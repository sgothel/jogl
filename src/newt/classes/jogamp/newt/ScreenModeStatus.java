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
import com.jogamp.common.util.IntIntHashMap;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.newt.Screen;
import com.jogamp.newt.ScreenMode;
import com.jogamp.newt.event.ScreenModeListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ScreenModeStatus {
    private static boolean DEBUG = Screen.DEBUG;

    private RecursiveLock lock = LockFactory.createRecursiveLock();
    private ArrayHashSet<ScreenMode> screenModes;
    private IntIntHashMap screenModesIdx2NativeIdx;
    private ScreenMode currentScreenMode;
    private ScreenMode originalScreenMode;
    private boolean screenModeChangedByOwner; 
    private ArrayList<ScreenModeListener> listener = new ArrayList<ScreenModeListener>();

    private static HashMap<String, ScreenModeStatus> screenFQN2ScreenModeStatus = new HashMap<String, ScreenModeStatus>();
    private static RecursiveLock screen2ScreenModeStatusLock = LockFactory.createRecursiveLock();

    protected static void mapScreenModeStatus(String screenFQN, ScreenModeStatus sms) {
        screen2ScreenModeStatusLock.lock();
        try {
            ScreenModeStatus _sms = screenFQN2ScreenModeStatus.get(screenFQN);
            if( null != _sms ) {
                throw new RuntimeException("ScreenModeStatus "+_sms+" already mapped to "+screenFQN);
            }
            screenFQN2ScreenModeStatus.put(screenFQN, sms);
            if(DEBUG) {
                System.err.println("ScreenModeStatus.map "+screenFQN+" -> "+sms);
            }
        } finally {
            screen2ScreenModeStatusLock.unlock();
        }
    }

    /**
     * @param screen the prev user
     * @return true if mapping is empty, ie no more usage of the mapped ScreenModeStatus
     */
    protected static void unmapScreenModeStatus(String screenFQN) {
        screen2ScreenModeStatusLock.lock();
        try {
            unmapScreenModeStatusUnlocked(screenFQN);
        } finally {
            screen2ScreenModeStatusLock.unlock();
        }
    }
    protected static void unmapScreenModeStatusUnlocked(String screenFQN) {
        ScreenModeStatus sms = screenFQN2ScreenModeStatus.remove(screenFQN);
        if(DEBUG) {
            System.err.println("ScreenModeStatus.unmap "+screenFQN+" -> "+sms);
        }
    }

    protected static ScreenModeStatus getScreenModeStatus(String screenFQN) {
        screen2ScreenModeStatusLock.lock();
        try {
            return getScreenModeStatusUnlocked(screenFQN);
        } finally {
            screen2ScreenModeStatusLock.unlock();
        }
    }
    protected static ScreenModeStatus getScreenModeStatusUnlocked(String screenFQN) {
        return screenFQN2ScreenModeStatus.get(screenFQN);
    }

    protected static void lockScreenModeStatus() {
        screen2ScreenModeStatusLock.lock();
    }

    protected static void unlockScreenModeStatus() {
        screen2ScreenModeStatusLock.unlock();
    }
    
    public ScreenModeStatus(ArrayHashSet<ScreenMode> screenModes,
                            IntIntHashMap screenModesIdx2NativeIdx) {
        this.screenModes = screenModes;
        this.screenModesIdx2NativeIdx = screenModesIdx2NativeIdx;
        this.screenModeChangedByOwner = false;
    }

    protected final void setOriginalScreenMode(ScreenMode originalScreenMode) {
        this.originalScreenMode = originalScreenMode;
        this.currentScreenMode = originalScreenMode;
    }

    public final ScreenMode getOriginalScreenMode() {
        return originalScreenMode;
    }

    public final ScreenMode getCurrentScreenMode() {
        lock();
        try {
            return currentScreenMode;
        } finally {
            unlock();
        }
    }

    /**
     * We cannot guarantee that we won't interfere w/ another running
     * application's screen mode change.
     * <p>
     * At least we only return <code>true</true> if the owner, ie. the Screen,
     * has changed the screen mode and if the original screen mode 
     * is not current the current one.
     * </p>
     * @return
     */
    public final boolean isOriginalModeChangedByOwner() {
        lock();
        try {
            return screenModeChangedByOwner && !isCurrentModeOriginalMode();
        } finally {
            unlock();
        }
    }

    protected final boolean isCurrentModeOriginalMode() {
        if(null != currentScreenMode && null != originalScreenMode) {
            return currentScreenMode.hashCode() == originalScreenMode.hashCode();
        }
        return true;
    }
    
    protected final ArrayHashSet<ScreenMode> getScreenModes() {
        return screenModes;
    }

    protected final IntIntHashMap getScreenModesIdx2NativeIdx() {
        return screenModesIdx2NativeIdx;
    }

    protected final int addListener(ScreenModeListener l) {
        lock();
        try {
            listener.add(l);
            if(DEBUG) {
                System.err.println("ScreenModeStatus.addListener (size: "+listener.size()+"): "+l);
            }
            return listener.size();
        } finally {
            unlock();
        }
    }

    protected final int removeListener(ScreenModeListener l) {
        lock();
        try {
            if(!listener.remove(l)) {
                throw new RuntimeException("ScreenModeListener "+l+" not contained");
            }
            if(DEBUG) {
                System.err.println("ScreenModeStatus.removeListener (size: "+listener.size()+"): "+l);
            }
            return listener.size();
        } finally {
            unlock();
        }
    }

    protected final void fireScreenModeChangeNotify(ScreenMode desiredScreenMode) {
        lock();
        try {
            for(int i=0; i<listener.size(); i++) {
                listener.get(i).screenModeChangeNotify(desiredScreenMode);
            }
        } finally {
            unlock();
        }
    }

    protected void fireScreenModeChanged(ScreenMode currentScreenMode, boolean success) {
        lock();
        try {
            if(success) {
                this.currentScreenMode = currentScreenMode;
                this.screenModeChangedByOwner = !isCurrentModeOriginalMode();
            }
            for(int i=0; i<listener.size(); i++) {
                listener.get(i).screenModeChanged(currentScreenMode, success);
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
