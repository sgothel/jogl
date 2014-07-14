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

package jogamp.opengl;

import javax.media.opengl.GLRunnable;
import javax.media.opengl.GLAutoDrawable;

/**
 * Helper class to provide a Runnable queue implementation with a Runnable wrapper
 * which notifies after execution for the <code>invokeAndWait()</code> semantics.
 */
public class GLRunnableTask implements GLRunnable {
    GLRunnable runnable;
    Object notifyObject;
    boolean catchExceptions;
    volatile boolean isExecuted;
    volatile boolean isFlushed;

    Throwable runnableException;

    public GLRunnableTask(final GLRunnable runnable, final Object notifyObject, final boolean catchExceptions) {
        this.runnable = runnable ;
        this.notifyObject = notifyObject ;
        this.catchExceptions = catchExceptions;
        isExecuted = false;
        isFlushed = false;
    }

    @Override
    public boolean run(final GLAutoDrawable drawable) {
        boolean res = true;
        if(null == notifyObject) {
            try {
                res = runnable.run(drawable);
            } catch (final Throwable t) {
                runnableException = t;
                if(catchExceptions) {
                    runnableException.printStackTrace();
                } else {
                    throw new RuntimeException(runnableException);
                }
            } finally {
                isExecuted=true;
            }
        } else {
            synchronized (notifyObject) {
                try {
                    res = runnable.run(drawable);
                } catch (final Throwable t) {
                    runnableException = t;
                    if(catchExceptions) {
                        runnableException.printStackTrace();
                    } else {
                        throw new RuntimeException(runnableException);
                    }
                } finally {
                    isExecuted=true;
                    notifyObject.notifyAll();
                }
            }
        }
        return res;
    }

    /**
     * Simply flush this task and notify a waiting executor.
     * The executor which might have been blocked until notified
     * will be unblocked and the task removed from the queue.
     *
     * @see #isFlushed()
     * @see #isInQueue()
     */
    public void flush() {
        if(!isExecuted() && null != notifyObject) {
            synchronized (notifyObject) {
                isFlushed=true;
                notifyObject.notifyAll();
            }
        }
    }

    /**
     * @return !{@link #isExecuted()} && !{@link #isFlushed()}
     */
    public boolean isInQueue() { return !isExecuted && !isFlushed; }

    /**
     * @return whether this task has been executed.
     * @see #isInQueue()
     */
    public boolean isExecuted() { return isExecuted; }

    /**
     * @return whether this task has been flushed.
     * @see #isInQueue()
     */
    public boolean isFlushed() { return isFlushed; }

    public Throwable getThrowable() { return runnableException; }
}

