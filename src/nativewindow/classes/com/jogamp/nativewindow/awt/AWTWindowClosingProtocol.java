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

package com.jogamp.nativewindow.awt;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.media.nativewindow.WindowClosingProtocol;

import jogamp.nativewindow.awt.AWTMisc;

public class AWTWindowClosingProtocol implements WindowClosingProtocol {

  private Component comp;
  private Runnable closingOperation;
  private volatile boolean closingListenerSet = false;
  private Object closingListenerLock = new Object();
  private WindowClosingMode defaultCloseOperation = WindowClosingMode.DISPOSE_ON_CLOSE;
  private boolean defaultCloseOperationSetByUser = false;

  public AWTWindowClosingProtocol(Component comp, Runnable closingOperation) {
      this.comp = comp;
      this.closingOperation = closingOperation;
  }

  class WindowClosingAdapter extends WindowAdapter {
    @Override
    public void windowClosing(WindowEvent e) {
      final WindowClosingMode op = AWTWindowClosingProtocol.this.getDefaultCloseOperation();

      if( WindowClosingMode.DISPOSE_ON_CLOSE == op ) {
          // we have to issue this call right away,
          // otherwise the window gets destroyed
          closingOperation.run();
      }
    }
  }
  WindowListener windowClosingAdapter = new WindowClosingAdapter();

  final boolean addClosingListenerImpl() {
    Window w = AWTMisc.getWindow(comp);
    if(null!=w) {
        w.addWindowListener(windowClosingAdapter);
        return true;
    }
    return false;
  }

  /**
   * Adds this closing listener to the components Window if exist and only one time.<br>
   * Hence you may call this method every time to ensure it has been set,
   * ie in case the Window parent is not available yet.
   *
   * @return
   */
  public final boolean addClosingListenerOneShot() {
    if(!closingListenerSet) { // volatile: ok
      synchronized(closingListenerLock) {
        if(!closingListenerSet) {
            closingListenerSet=addClosingListenerImpl();
            return closingListenerSet;
        }
      }
    }
    return false;
  }

  public final boolean removeClosingListener() {
    if(closingListenerSet) { // volatile: ok
      synchronized(closingListenerLock) {
        if(closingListenerSet) {
            Window w = AWTMisc.getWindow(comp);
            if(null!=w) {
                w.removeWindowListener(windowClosingAdapter);
                closingListenerSet = false;
                return true;
            }
        }
      }
    }
    return false;
  }

  /**
   *
   * @return the user set close operation if set by {@link #setDefaultCloseOperation(WindowClosingMode) setDefaultCloseOperation(int)},
   *         otherwise return the AWT/Swing close operation value translated to
   *         a {@link WindowClosingProtocol} value .
   */
  public final WindowClosingMode getDefaultCloseOperation() {
      synchronized(closingListenerLock) {
        if(defaultCloseOperationSetByUser) {
          return defaultCloseOperation;
        }
      }
      // User didn't determine the behavior, use underlying AWT behavior
      return AWTMisc.getNWClosingOperation(comp);
  }

  public final WindowClosingMode setDefaultCloseOperation(WindowClosingMode op) {
      synchronized(closingListenerLock) {
          final WindowClosingMode _op = defaultCloseOperation;
          defaultCloseOperation = op;
          defaultCloseOperationSetByUser = true;
          return _op;
      }
  }
}
