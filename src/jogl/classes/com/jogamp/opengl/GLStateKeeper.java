/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl;

/**
 * Interface adding a {@link GLEventListenerState} protocol to {@link GLAutoDrawable}s
 * or other self-contained compound types combining {@link GLDrawable}, {@link GLContext} and {@link GLEventListener}.
 * <p>
 * Implementing classes {@link #isGLStatePreservationSupported() may support} preservation
 * of the {@link GLContext} state and it's associated {@link GLEventListener}.
 * </p>
 */
public interface GLStateKeeper {

    /** Listener for preserve and restore notifications. */
    public static interface Listener {
        /** Invoked before preservation. */
        void glStatePreserveNotify(GLStateKeeper glsk);
        /** Invoked after restoration. */
        void glStateRestored(GLStateKeeper glsk);
    }

    /**
     * Sets a {@link Listener}, overriding the old one.
     * @param l the new {@link Listener}.
     * @return the previous {@link Listener}.
     */
    public Listener setGLStateKeeperListener(Listener l);

    /**
     * @return <code>true</code> if GL state preservation is supported in implementation and on current platform, <code>false</code> otherwise.
     * @see #preserveGLStateAtDestroy(boolean)
     * @see #getPreservedGLState()
     * @see #clearPreservedGLState()
     */
    public boolean isGLStatePreservationSupported();

    /**
     * If set to <code>true</code>, the next {@link GLAutoDrawable#destroy()} operation will
     * {@link #preserveGLEventListenerState() preserve} the {@link GLEventListenerState}.
     * <p>
     * This is a one-shot flag, i.e. after preserving the {@link GLEventListenerState},
     * the flag is cleared.
     * </p>
     * <p>
     * A preserved {@link GLEventListenerState} will be
     * {@link #restoreGLEventListenerState() restored} again.
     * </p>
     * @return <code>true</code> if supported and successful, <code>false</code> otherwise.
     * @see #isGLStatePreservationSupported()
     * @see #getPreservedGLState()
     * @see #clearPreservedGLState()
     */
    public boolean preserveGLStateAtDestroy(boolean value);

    /**
     * Returns the preserved {@link GLEventListenerState} if preservation was performed,
     * otherwise <code>null</code>.
     * @see #isGLStatePreservationSupported()
     * @see #preserveGLStateAtDestroy(boolean)
     * @see #clearPreservedGLState()
     */
    public GLEventListenerState getPreservedGLState();

    /**
     * Clears the preserved {@link GLEventListenerState} from this {@link GLStateKeeper}, without destroying it.
     *
     * @return the preserved and cleared {@link GLEventListenerState} if preservation was performed,
     *         otherwise <code>null</code>.
     * @see #isGLStatePreservationSupported()
     * @see #preserveGLStateAtDestroy(boolean)
     * @see #getPreservedGLState()
     */
    public GLEventListenerState clearPreservedGLState();
}
