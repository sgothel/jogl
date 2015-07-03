/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

package jogamp.opengl;

import java.util.ArrayList;

/**
 * Simple locked listener implementation stub to be used for listener handler,
 * synchronized on it's instance.
 *
 * <p>Utilizing simple locking via synchronized.</p>
 *
 * @param <E> The listener type
 */
public class ListenerSyncedImplStub<E> {
  private ArrayList<E> listeners;

  public ListenerSyncedImplStub() {
    reset();
  }

  public synchronized final void reset() {
    listeners = new ArrayList<E>();
  }

  public synchronized final void destroy() {
    listeners.clear();
    listeners = null;
  }

  public synchronized final int size() {
    return listeners.size();
  }

  public synchronized final void addListener(final E listener) {
    addListener(-1, listener);
  }

  public synchronized final void addListener(int index, final E listener) {
    if(0>index) {
        index = listeners.size();
    }
    listeners.add(index, listener);
  }

  public synchronized final void removeListener(final E listener) {
    listeners.remove(listener);
  }

  public final ArrayList<E> getListeners() {
      return listeners;
  }
}
