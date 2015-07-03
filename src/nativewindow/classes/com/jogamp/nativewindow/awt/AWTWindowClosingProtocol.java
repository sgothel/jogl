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

import com.jogamp.nativewindow.WindowClosingProtocol;

import jogamp.nativewindow.awt.AWTMisc;

public class AWTWindowClosingProtocol implements WindowClosingProtocol {

  private final Component comp;
  private Window listenTo;
  private final Runnable closingOperationClose;
  private final Runnable closingOperationNOP;
  private final Object closingListenerLock = new Object();
  private WindowClosingMode defaultCloseOperation = WindowClosingMode.DISPOSE_ON_CLOSE;
  private boolean defaultCloseOperationSetByUser = false;

  /**
   * @param comp mandatory AWT component which AWT Window is being queried by parent traversal
   * @param closingOperationClose mandatory closing operation, triggered if windowClosing and {@link WindowClosingMode#DISPOSE_ON_CLOSE}
   * @param closingOperationNOP optional closing operation, triggered if windowClosing and {@link WindowClosingMode#DO_NOTHING_ON_CLOSE}
   */
  public AWTWindowClosingProtocol(final Component comp, final Runnable closingOperationClose, final Runnable closingOperationNOP) {
      this.comp = comp;
      this.listenTo = null;
      this.closingOperationClose = closingOperationClose;
      this.closingOperationNOP = closingOperationNOP;
  }

  class WindowClosingAdapter extends WindowAdapter {
    @Override
    public void windowClosing(final WindowEvent e) {
      final WindowClosingMode op = AWTWindowClosingProtocol.this.getDefaultCloseOperation();

      if( WindowClosingMode.DISPOSE_ON_CLOSE == op ) {
          // we have to issue this call right away,
          // otherwise the window gets destroyed
          closingOperationClose.run();
      } else if( null != closingOperationNOP ){
          closingOperationNOP.run();
      }
    }
  }
  WindowListener windowClosingAdapter = new WindowClosingAdapter();

  /**
   * Adds this closing listener to the components Window if exist and only one time.
   * <p>
   * If the closing listener is already added, and {@link IllegalStateException} is thrown.
   * </p>
   *
   * @return true if added, otherwise false.
   * @throws IllegalStateException
   */
  public final boolean addClosingListener() throws IllegalStateException {
      synchronized(closingListenerLock) {
          if(null != listenTo) {
              throw new IllegalStateException("WindowClosingListener already set");
          }
          listenTo = AWTMisc.getWindow(comp);
          if(null!=listenTo) {
              listenTo.addWindowListener(windowClosingAdapter);
              return true;
          }
      }
      return false;
  }

  public final boolean removeClosingListener() {
      synchronized(closingListenerLock) {
          if(null != listenTo) {
              listenTo.removeWindowListener(windowClosingAdapter);
              listenTo = null;
              return true;
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
  @Override
  public final WindowClosingMode getDefaultCloseOperation() {
      synchronized(closingListenerLock) {
        if(defaultCloseOperationSetByUser) {
          return defaultCloseOperation;
        }
      }
      // User didn't determine the behavior, use underlying AWT behavior
      return AWTMisc.getNWClosingOperation(comp);
  }

  @Override
  public final WindowClosingMode setDefaultCloseOperation(final WindowClosingMode op) {
      synchronized(closingListenerLock) {
          final WindowClosingMode _op = defaultCloseOperation;
          defaultCloseOperation = op;
          defaultCloseOperationSetByUser = true;
          return _op;
      }
  }
}
