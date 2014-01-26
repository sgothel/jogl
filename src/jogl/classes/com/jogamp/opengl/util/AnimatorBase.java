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

package com.jogamp.opengl.util;

import jogamp.opengl.Debug;
import jogamp.opengl.FPSCounterImpl;

import java.io.PrintStream;
import java.util.ArrayList;

import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

/**
 * Base implementation of GLAnimatorControl<br>
 * <p>
 * The change synchronization is done via synchronized blocks on the AnimatorBase instance.<br>
 * Status get / set activity is synced with a RecursiveLock, used as a memory barrier.<br>
 * This is suitable, since all change requests are allowed to be expensive
 * as they are not expected to be called at every frame.
 * </p>
 */
public abstract class AnimatorBase implements GLAnimatorControl {
    protected static final boolean DEBUG = Debug.debug("Animator");

    /** A 1s timeout while waiting for a native action response, limiting {@link #finishLifecycleAction(Condition, long)} */
    protected static final long TO_WAIT_FOR_FINISH_LIFECYCLE_ACTION = 1000;

    protected static final long POLLP_WAIT_FOR_FINISH_LIFECYCLE_ACTION = 32; // 2 frames @ 60Hz

    /**
     * If present in <code>modeBits</code> field and
     * {@link GLProfile#isAWTAvailable() AWT is available},
     * implementation is aware of the AWT EDT, otherwise not.
     * <p>
     * This is the <i>default</i>.
     * </p>
     * @see #setModeBits(boolean, int)
     */
    public static final int MODE_EXPECT_AWT_RENDERING_THREAD = 1 << 0;

    public interface AnimatorImpl {
        void display(ArrayList<GLAutoDrawable> drawables, boolean ignoreExceptions, boolean printExceptions);
        boolean blockUntilDone(Thread thread);
    }

    protected int modeBits;
    protected AnimatorImpl impl;
    protected String baseName;

    protected ArrayList<GLAutoDrawable> drawables = new ArrayList<GLAutoDrawable>();
    protected boolean drawablesEmpty;
    protected Thread animThread;
    protected boolean ignoreExceptions;
    protected boolean printExceptions;
    protected boolean exclusiveContext;
    protected Thread userExclusiveContextThread;
    protected FPSCounterImpl fpsCounter = new FPSCounterImpl();

    private final static Class<?> awtAnimatorImplClazz;
    static {
        GLProfile.initSingleton();
        if( GLProfile.isAWTAvailable() ) {
            Class<?> clazz;
            try {
                clazz = Class.forName("com.jogamp.opengl.util.AWTAnimatorImpl");
            } catch (Exception e) {
                clazz = null;
            }
            awtAnimatorImplClazz =  clazz;
        } else {
            awtAnimatorImplClazz = null;
        }
    }

    /**
     * Creates a new, empty Animator instance
     * while expecting an AWT rendering thread if AWT is available.
     *
     * @see GLProfile#isAWTAvailable()
     */
    public AnimatorBase() {
        modeBits = MODE_EXPECT_AWT_RENDERING_THREAD; // default!
        drawablesEmpty = true;
    }

    private static final boolean useAWTAnimatorImpl(int modeBits) {
        return 0 != ( MODE_EXPECT_AWT_RENDERING_THREAD & modeBits ) && null != awtAnimatorImplClazz;
    }

    /**
     * Initializes implementation details post setup,
     * invoked at {@link #add(GLAutoDrawable)}, {@link #start()}, ..
     * <p>
     * Operation is a NOP if <code>force</code> is <code>false</code>
     * and this instance is already initialized.
     * </p>
     *
     * @throws GLException if Animator is {@link #isStarted()}
     */
    protected final synchronized void initImpl(boolean force) {
        if( force || null == impl ) {
            if( useAWTAnimatorImpl( modeBits ) ) {
                try {
                    impl = (AnimatorImpl) awtAnimatorImplClazz.newInstance();
                    baseName = getBaseName("AWT");
                } catch (Exception e) { e.printStackTrace(); }
            }
            if( null == impl ) {
                impl = new DefaultAnimatorImpl();
                baseName = getBaseName("");
            }
            if(DEBUG) {
                System.err.println("Animator.initImpl: baseName "+baseName+", implClazz "+impl.getClass().getName()+" - "+toString()+" - "+getThreadName());
            }
        }
    }
    protected abstract String getBaseName(String prefix);

    /**
     * Enables or disables the given <code>bitValues</code>
     * in this Animators <code>modeBits</code>.
     * @param enable
     * @param bitValues
     *
     * @throws GLException if Animator is {@link #isStarted()} and {@link #MODE_EXPECT_AWT_RENDERING_THREAD} about to change
     * @see AnimatorBase#MODE_EXPECT_AWT_RENDERING_THREAD
     */
    public final synchronized void setModeBits(boolean enable, int bitValues) throws GLException {
        final int _oldModeBits = modeBits;
        if(enable) {
            modeBits |=  bitValues;
        } else {
            modeBits &= ~bitValues;
        }
        if( useAWTAnimatorImpl( _oldModeBits ) != useAWTAnimatorImpl( modeBits ) ) {
            if( isStarted() ) {
                throw new GLException("Animator already started");
            }
            initImpl(true);
        }
    }
    public synchronized int getModeBits() { return modeBits; }


    @Override
    public final synchronized void add(final GLAutoDrawable drawable) {
        if(DEBUG) {
            System.err.println("Animator add: 0x"+Integer.toHexString(drawable.hashCode())+" - "+toString()+" - "+getThreadName());
        }
        if( drawables.contains(drawable) ) {
            throw new IllegalArgumentException("Drawable already added to animator: "+this+", "+drawable);
        }
        initImpl(false);
        pause();
        if( isStarted() ) {
            drawable.setExclusiveContextThread( exclusiveContext ? getExclusiveContextThread() : null ); // if already running ..
        }
        drawables.add(drawable);
        drawablesEmpty = drawables.size() == 0;
        drawable.setAnimator(this);
        if( isPaused() ) { // either paused by pause() above, or if previously drawablesEmpty==true
            resume();
        }
        final Condition waitForAnimatingAndECTCondition = new Condition() {
            @Override
            public boolean eval() {
                final Thread dect = drawable.getExclusiveContextThread();
                return isStarted() && !isPaused() && !isAnimating() && ( exclusiveContext && null == dect || !exclusiveContext && null != dect );
            } };
        final boolean res = finishLifecycleAction(waitForAnimatingAndECTCondition, 0);
        if(DEBUG) {
            System.err.println("Animator add: Wait for Animating/ECT OK: "+res+", "+toString()+", dect "+drawable.getExclusiveContextThread());
        }
        notifyAll();
    }

    @Override
    public final synchronized void remove(final GLAutoDrawable drawable) {
        if(DEBUG) {
            System.err.println("Animator remove: 0x"+Integer.toHexString(drawable.hashCode())+" - "+toString()+" - "+getThreadName());
        }
        if( !drawables.contains(drawable) ) {
            throw new IllegalArgumentException("Drawable not added to animator: "+this+", "+drawable);
        }

        if( exclusiveContext && isAnimating() ) {
            drawable.setExclusiveContextThread( null );
            final Condition waitForNullECTCondition = new Condition() {
                @Override
                public boolean eval() {
                    return null != drawable.getExclusiveContextThread();
                } };
            final boolean res = finishLifecycleAction(waitForNullECTCondition, POLLP_WAIT_FOR_FINISH_LIFECYCLE_ACTION);
            if(DEBUG) {
                System.err.println("Animator remove: Wait for Null-ECT OK: "+res+", "+toString()+", dect "+drawable.getExclusiveContextThread());
            }
        }

        boolean paused = pause();
        drawables.remove(drawable);
        drawablesEmpty = drawables.size() == 0;
        drawable.setAnimator(null);
        if(paused) {
            resume();
        }
        final boolean res = finishLifecycleAction(waitForNotAnimatingIfEmptyCondition, 0);
        if(DEBUG) {
            System.err.println("Animator remove: Wait for !Animating-if-empty OK: "+res+", "+toString());
        }
        notifyAll();
    }
    private final Condition waitForNotAnimatingIfEmptyCondition = new Condition() {
        @Override
        public boolean eval() {
            return isStarted() && drawablesEmpty && isAnimating();
        } };


    /**
     * Dedicate all {@link GLAutoDrawable}'s context to the given exclusive context thread.
     * <p>
     * The given thread will be exclusive to all {@link GLAutoDrawable}'s context while {@link #isAnimating()}.
     * </p>
     * <p>
     * If already started and disabling, method waits
     * until change is propagated to all {@link GLAutoDrawable} if not
     * called from the animator thread or {@link #getExclusiveContextThread() exclusive context thread}.
     * </p>
     * <p>
     * Note: Utilizing this feature w/ AWT could lead to an AWT-EDT deadlock, depending on the AWT implementation.
     * Hence it is advised not to use it with native AWT GLAutoDrawable like GLCanvas.
     * </p>
     *
     * @param enable
     * @return previous value
     * @see #setExclusiveContext(boolean)
     * @see #getExclusiveContextThread()
     * @see #isExclusiveContextEnabled()
     */
    // @Override
    public final synchronized Thread setExclusiveContext(Thread t) {
        final boolean enable = null != t;
        final Thread old = userExclusiveContextThread;
        if( enable && t != animThread ) { // disable: will be cleared at end after propagation && filter out own animThread usae
            userExclusiveContextThread=t;
        }
        setExclusiveContext(enable);
        return old;
    }

    /**
     * Dedicate all {@link GLAutoDrawable}'s context to this animator thread.
     * <p>
     * The given thread will be exclusive to all {@link GLAutoDrawable}'s context while {@link #isAnimating()}.
     * </p>
     * <p>
     * If already started and disabling, method waits
     * until change is propagated to all {@link GLAutoDrawable} if not
     * called from the animator thread or {@link #getExclusiveContextThread() exclusive context thread}.
     * </p>
     * <p>
     * Note: Utilizing this feature w/ AWT could lead to an AWT-EDT deadlock, depending on the AWT implementation.
     * Hence it is advised not to use it with native AWT GLAutoDrawable like GLCanvas.
     * </p>
     *
     * @param enable
     * @return previous value
     * @see #setExclusiveContext(Thread)
     * @see #getExclusiveContextThread()
     * @see #isExclusiveContextEnabled()
     */
    // @Override
    public final boolean setExclusiveContext(boolean enable) {
        final boolean propagateState;
        final boolean oldExclusiveContext;
        final Thread _exclusiveContextThread;
        synchronized (AnimatorBase.this) {
            propagateState = isStarted() && !drawablesEmpty;
            _exclusiveContextThread = userExclusiveContextThread;
            oldExclusiveContext = exclusiveContext;
            exclusiveContext = enable;
            if(DEBUG) {
                System.err.println("AnimatorBase.setExclusiveContextThread: "+oldExclusiveContext+" -> "+exclusiveContext+", propagateState "+propagateState+", "+this);
            }
        }
        final Thread dECT = enable ? ( null != userExclusiveContextThread ? userExclusiveContextThread : animThread ) : null ;
        if( propagateState ) {
            setDrawablesExclCtxState(enable);
            if( !enable ) {
                if( Thread.currentThread() == getThread() || Thread.currentThread() == _exclusiveContextThread ) {
                    display();
                } else {
                    final boolean resumed = isAnimating() ? false : resume();
                    int counter = 10;
                    while( 0<counter && isAnimating() && !validateDrawablesExclCtxState(dECT) ) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) { }
                        counter--;
                    }
                    if(resumed) {
                        pause();
                    }
                }
                synchronized(AnimatorBase.this) {
                    userExclusiveContextThread=null;
                }
            }
        }
        if(DEBUG) {
            System.err.println("AnimatorBase.setExclusiveContextThread: all-GLAD Ok: "+validateDrawablesExclCtxState(dECT)+", "+this);
        }
        return oldExclusiveContext;
    }

    /**
     * Returns <code>true</code>, if the exclusive context thread is enabled, otherwise <code>false</code>.
     *
     * @see #setExclusiveContext(boolean)
     * @see #setExclusiveContext(Thread)
     */
    // @Override
    public final synchronized boolean isExclusiveContextEnabled() {
        return exclusiveContext;
    }

    /**
     * Returns the exclusive context thread if {@link #isExclusiveContextEnabled()} and {@link #isStarted()}, otherwise <code>null</code>.
     * <p>
     * If exclusive context is enabled via {@link #setExclusiveContext(boolean)}
     * the {@link #getThread() animator thread} is returned if above conditions are met.
     * </p>
     * <p>
     * If exclusive context is enabled via {@link #setExclusiveContext(Thread)}
     * the user passed thread is returned if above conditions are met.
     * </p>
     * @see #setExclusiveContext(boolean)
     * @see #setExclusiveContext(Thread)
     */
    // @Override
    public final synchronized Thread getExclusiveContextThread() {
        return ( isStarted() && exclusiveContext ) ? ( null != userExclusiveContextThread ? userExclusiveContextThread : animThread ) : null ;
    }

    /**
     * Should be called at {@link #start()} and {@link #stop()}
     * from within the animator thread.
     * <p>
     * At {@link #stop()} an additional {@link #display()} call shall be issued
     * to allow propagation of releasing the exclusive thread.
     * </p>
     */
    protected final synchronized void setDrawablesExclCtxState(boolean enable) {
        if(DEBUG) {
            System.err.println("AnimatorBase.setExclusiveContextImpl exlusive "+exclusiveContext+": Enable "+enable+" for "+this+" - "+Thread.currentThread());
            // Thread.dumpStack();
        }
        final Thread ect = getExclusiveContextThread();
        for (int i=0; i<drawables.size(); i++) {
            try {
                drawables.get(i).setExclusiveContextThread( enable ? ect : null );
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }
    protected final boolean validateDrawablesExclCtxState(Thread expected) {
        for (int i=0; i<drawables.size(); i++) {
            if( expected != drawables.get(i).getExclusiveContextThread() ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final synchronized Thread getThread() {
        return animThread;
    }

    /** Called every frame to cause redrawing of all of the
        GLAutoDrawables this Animator manages. Subclasses should call
        this to get the most optimized painting behavior for the set of
        components this Animator manages, in particular when multiple
        lightweight widgets are continually being redrawn. */
    protected final void display() {
        impl.display(drawables, ignoreExceptions, printExceptions);
        fpsCounter.tickFPS();
    }

    @Override
    public final void setUpdateFPSFrames(int frames, PrintStream out) {
        fpsCounter.setUpdateFPSFrames(frames, out);
    }

    @Override
    public final void resetFPSCounter() {
        fpsCounter.resetFPSCounter();
    }

    @Override
    public final int getUpdateFPSFrames() {
        return fpsCounter.getUpdateFPSFrames();
    }

    @Override
    public final long getFPSStartTime()   {
        return fpsCounter.getFPSStartTime();
    }

    @Override
    public final long getLastFPSUpdateTime() {
        return fpsCounter.getLastFPSUpdateTime();
    }

    @Override
    public final long getLastFPSPeriod() {
        return fpsCounter.getLastFPSPeriod();
    }

    @Override
    public final float getLastFPS() {
        return fpsCounter.getLastFPS();
    }

    @Override
    public final int getTotalFPSFrames() {
        return fpsCounter.getTotalFPSFrames();
    }

    @Override
    public final long getTotalFPSDuration() {
        return fpsCounter.getTotalFPSDuration();
    }

    @Override
    public final float getTotalFPS() {
        return fpsCounter.getTotalFPS();
    }

    /** Sets a flag causing this Animator to ignore exceptions produced
    while redrawing the drawables. By default this flag is set to
    false, causing any exception thrown to halt the Animator. */
    public final void setIgnoreExceptions(boolean ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
    }

    /** Sets a flag indicating that when exceptions are being ignored by
    this Animator (see {@link #setIgnoreExceptions}), to print the
    exceptions' stack traces for diagnostic information. Defaults to
    false. */
    public final void setPrintExceptions(boolean printExceptions) {
        this.printExceptions = printExceptions;
    }

    protected interface Condition {
        /**
         * @return true if branching (continue waiting, action), otherwise false
         */
        boolean eval();
    }

    /**
     * @param waitCondition method will wait until TO is reached or {@link Condition#eval() waitCondition.eval()} returns <code>false</code>.
     * @param pollPeriod if <code>0</code>, method will wait until TO is reached or being notified.
     *                   if &gt; <code>0</code>, method will wait for the given <code>pollPeriod</code> in milliseconds.
     * @return <code>true</code> if {@link Condition#eval() waitCondition.eval()} returned <code>false</code>, otherwise <code>false</code>.
     */
    protected final synchronized boolean finishLifecycleAction(Condition waitCondition, long pollPeriod) {
        /**
         * It's hard to tell whether the thread which changes the lifecycle has
         * dependencies on the Animator's internal thread. Currently we
         * use a couple of heuristics to determine whether we should do
         * the blocking wait().
         */
        initImpl(false);
        final boolean blocking;
        long remaining;
        boolean nok;
        if( impl.blockUntilDone(animThread) ) {
            blocking = true;
            remaining = TO_WAIT_FOR_FINISH_LIFECYCLE_ACTION;
            if( 0 >= pollPeriod ) {
                pollPeriod = remaining;
            }
            nok = waitCondition.eval();
            while ( nok && remaining>0 ) {
                final long t1 = System.currentTimeMillis();
                if( pollPeriod > remaining ) { pollPeriod = remaining; }
                notifyAll();
                try {
                    wait(pollPeriod);
                } catch (InterruptedException ie) {  }
                remaining -= System.currentTimeMillis() - t1 ;
                nok = waitCondition.eval();
            }
        } else {
            /**
             * Even though we are not able to block until operation is completed at this point,
             * best effort shall be made to preserve functionality.
             * Here: Issue notifyAll() if waitCondition still holds and test again.
             *
             * Non blocking reason could be utilizing AWT Animator while operation is performed on AWT-EDT.
             */
            blocking = false;
            remaining = 0;
            nok = waitCondition.eval();
            if( nok ) {
                notifyAll();
                nok = waitCondition.eval();
            }
        }
        if(DEBUG || blocking && nok) { // Info only if DEBUG or ( blocking && not-ok ) ; !blocking possible if AWT
            if( blocking && remaining<=0 && nok ) {
                System.err.println("finishLifecycleAction(" + waitCondition.getClass().getName() + "): ++++++ timeout reached ++++++ " + getThreadName());
            }
            System.err.println("finishLifecycleAction(" + waitCondition.getClass().getName() + "): OK "+(!nok)+
                    "- pollPeriod "+pollPeriod+", blocking "+blocking+
                    ", waited " + (blocking ? ( TO_WAIT_FOR_FINISH_LIFECYCLE_ACTION - remaining ) : 0 ) + "/" + TO_WAIT_FOR_FINISH_LIFECYCLE_ACTION +
                    " - " + getThreadName());
            System.err.println(" - "+toString());
            if(nok) {
                Thread.dumpStack();
            }
        }
        return !nok;
    }

    @Override
    public synchronized boolean isStarted() {
        return animThread != null ;
    }

    protected static String getThreadName() { return Thread.currentThread().getName(); }

    @Override
    public String toString() {
        return getClass().getName()+"[started "+isStarted()+", animating "+isAnimating()+", paused "+isPaused()+", drawable "+drawables.size()+
               ", totals[dt "+getTotalFPSDuration()+", frames "+getTotalFPSFrames()+", fps "+getTotalFPS()+
               "], modeBits "+modeBits+", init'ed "+(null!=impl)+", animThread "+getThread()+", exclCtxThread "+exclusiveContext+"("+getExclusiveContextThread()+")]";
    }
}
