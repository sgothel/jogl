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

package jogamp.nativewindow;

import java.util.ArrayList;

import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.SurfaceUpdatedListener;

public class SurfaceUpdatedHelper implements SurfaceUpdatedListener {
    private Object surfaceUpdatedListenersLock = new Object();
    private ArrayList<SurfaceUpdatedListener> surfaceUpdatedListeners = new ArrayList<SurfaceUpdatedListener>();

    //
    // Management Utils
    // 
    public int size() { return surfaceUpdatedListeners.size(); }
    public SurfaceUpdatedListener get(int i) { return surfaceUpdatedListeners.get(i); }
    
    //
    // Implementation of NativeSurface SurfaceUpdatedListener methods
    // 
    
    public void addSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        addSurfaceUpdatedListener(-1, l);
    }

    public void addSurfaceUpdatedListener(int index, SurfaceUpdatedListener l) 
        throws IndexOutOfBoundsException
    {
        if(l == null) {
            return;
        }
        synchronized(surfaceUpdatedListenersLock) {
            if(0>index) { 
                index = surfaceUpdatedListeners.size(); 
            }
            surfaceUpdatedListeners.add(index, l);
        }
    }

    public void removeSurfaceUpdatedListener(SurfaceUpdatedListener l) {
        if (l == null) {
            return;
        }
        synchronized(surfaceUpdatedListenersLock) {
            surfaceUpdatedListeners.remove(l);
        }
    }

    public void surfaceUpdated(Object updater, NativeSurface ns, long when) {
        synchronized(surfaceUpdatedListenersLock) {
          for(int i = 0; i < surfaceUpdatedListeners.size(); i++ ) {
            SurfaceUpdatedListener l = surfaceUpdatedListeners.get(i);
            l.surfaceUpdated(updater, ns, when);
          }
        }
    }
}
