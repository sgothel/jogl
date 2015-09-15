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

package jogamp.newt.event;

import com.jogamp.newt.event.NEWTEvent;

/**
 * Helper class to provide a NEWTEvent queue implementation with a NEWTEvent wrapper
 * which notifies after sending the event for the <code>invokeAndWait()</code> semantics.
 */
public class NEWTEventTask {
    private final NEWTEvent event;
    private final Object notifyObject;
    private RuntimeException exception;
    private volatile boolean dispatched;

    public NEWTEventTask(final NEWTEvent event, final Object notifyObject) {
        this.event = event ;
        this.notifyObject = notifyObject ;
        this.exception = null;
        this.dispatched = false;
    }

    public final NEWTEvent get() { return event; }
    public final void setException(final RuntimeException e) { exception = e; }
    public final RuntimeException getException() { return exception; }
    public final boolean isCallerWaiting() { return null != notifyObject; }
    public final boolean isDispatched() { return dispatched; }
    public final void setDispatched() { dispatched = true; }

    /**
     * Notifies caller after {@link #setDispatched()}.
     */
    public void notifyCaller() {
        setDispatched();
        if(null != notifyObject) {
            synchronized (notifyObject) {
                notifyObject.notifyAll();
            }
        }
    }
}

