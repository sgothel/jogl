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

package com.jogamp.opengl.test.junit.jogl.acore.glels;

import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.Threading;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLDrawableUtil;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Test re-association (switching) of GLWindow /GLDrawables,
 * from GLWindow/GLOffscreenAutoDrawable to an GLOffscreenAutoDrawable and back.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class GLContextDrawableSwitchBase0 extends UITestCase {
    static int width, height;
    static boolean testEvenUnsafeSwapGLContext = false;

    static GLCapabilities getCaps(final String profile) {
        if( !GLProfile.isAvailable(profile) )  {
            System.err.println("Profile "+profile+" n/a");
            return null;
        }
        return new GLCapabilities(GLProfile.get(profile));
    }

    @BeforeClass
    public static void initClass() {
        width  = 256;
        height = 256;
    }

    public abstract GLAutoDrawable createGLAutoDrawable(final QuitAdapter quitAdapter, final GLCapabilitiesImmutable caps, final int width, final int height) throws InterruptedException, InvocationTargetException;
    public abstract void destroyGLAutoDrawable(final GLAutoDrawable glad) throws InterruptedException, InvocationTargetException;

    @Test(timeout=30000)
    public void test01aSwitch2Onscreen2OnscreenGL2ES2_Def() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        testImpl(reqGLCaps, true);
    }

    @Test(timeout=30000)
    public void test01bSwitch2Onscreen2OffscreenGL2ES2_Def() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        testImpl(reqGLCaps, false);
    }

    @Test(timeout=30000)
    public void test01cSwitch2Offscreen2OffscreenGL2ES2_Def() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        testImpl(reqGLCaps, false);
    }

    @Test(timeout=30000)
    public void test01dSwitch2Offscreen2OnscreenGL2ES2_Def() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        testImpl(reqGLCaps, true);
    }

    @Test(timeout=30000)
    public void test02aSwitch2Onscreen2OnscreenGL2ES2_MSAA() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setNumSamples(4);
        reqGLCaps.setSampleBuffers(true);
        testImpl(reqGLCaps, true);
    }

    @Test(timeout=30000)
    public void test02bSwitch2Onscreen2OffscreenGL2ES2_MSAA() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setNumSamples(4);
        reqGLCaps.setSampleBuffers(true);
        testImpl(reqGLCaps, false);
    }

    @Test(timeout=30000)
    public void test02cSwitch2Offscreen2OffscreenGL2ES2_MSAA() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setNumSamples(4);
        reqGLCaps.setSampleBuffers(true);
        testImpl(reqGLCaps, false);
    }

    @Test(timeout=30000)
    public void test02dSwitch2Offscreen2OnscreenGL2ES2_MSAA() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setNumSamples(4);
        reqGLCaps.setSampleBuffers(true);
        testImpl(reqGLCaps, true);
    }

    @Test(timeout=30000)
    public void test03aSwitch2Onscreen2OnscreenGL2ES2_Accu() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setAccumRedBits(1);
        reqGLCaps.setAccumGreenBits(1);
        reqGLCaps.setAccumBlueBits(1);
        testImpl(reqGLCaps, true);
    }

    @Test(timeout=30000)
    public void test03bSwitch2Onscreen2OffscreenGL2ES2_Accu() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setAccumRedBits(1);
        reqGLCaps.setAccumGreenBits(1);
        reqGLCaps.setAccumBlueBits(1);
        testImpl(reqGLCaps, false);
    }

    @Test(timeout=30000)
    public void test03cSwitch2Offscreen2OffscreenGL2ES2_Accu() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setAccumRedBits(1);
        reqGLCaps.setAccumGreenBits(1);
        reqGLCaps.setAccumBlueBits(1);
        testImpl(reqGLCaps, false);
    }

    @Test(timeout=30000)
    public void test03dSwitch2Offscreen2OnscreenGL2ES2_Accu() throws InterruptedException, InvocationTargetException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setAccumRedBits(1);
        reqGLCaps.setAccumGreenBits(1);
        reqGLCaps.setAccumBlueBits(1);
        testImpl(reqGLCaps, true);
    }

    private void testImpl(final GLCapabilitiesImmutable srcCapsRequested, final boolean dstOnscreen) throws InterruptedException, InvocationTargetException {
        final QuitAdapter quitAdapter = new QuitAdapter();
        final GLAutoDrawable gladSource = createGLAutoDrawable(quitAdapter, srcCapsRequested, width, height);

        final GLCapabilitiesImmutable srcCapsChosen = gladSource.getChosenGLCapabilities();

        final GLCapabilities dstCaps = (GLCapabilities) srcCapsChosen.cloneMutable();
        dstCaps.setOnscreen(dstOnscreen);

        final boolean isSwapGLContextSafe = GLDrawableUtil.isSwapGLContextSafe(srcCapsRequested, srcCapsChosen, dstCaps);
        System.err.println("Source Caps Requested: "+srcCapsRequested);
        System.err.println("Source Caps Chosen   : "+srcCapsChosen);
        System.err.println("Dest   Caps Requested: "+dstCaps);
        System.err.println("Is SwapGLContext safe: "+isSwapGLContextSafe);

        if( !isSwapGLContextSafe && !testEvenUnsafeSwapGLContext ) {
            System.err.println("Supressing unsafe tests ...");
            destroyGLAutoDrawable(gladSource);
            return;
        }

        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        final GearsES2 gears = new GearsES2(1);
        gears.setVerbose(false);
        gladSource.addGLEventListener(gears);
        gladSource.addGLEventListener(snapshotGLEventListener);
        snapshotGLEventListener.setMakeSnapshot();

        final Animator animator = new Animator();
        animator.add(gladSource);
        animator.start();

        int s = 0;
        final long t0 = System.currentTimeMillis();
        long t1 = t0;

        final GLAutoDrawable gladDest = createGLAutoDrawable(quitAdapter, dstCaps, width, height);
        RuntimeException caught = null;
        try {
            while( !quitAdapter.shouldQuit() && ( t1 - t0 ) < duration ) {
                if( ( t1 - t0 ) / period > s) {
                    s++;
                    System.err.println(s+" - switch - START "+ ( t1 - t0 ));

                    final Runnable switchAction = new Runnable() {
                            public void run() {
                                GLDrawableUtil.swapGLContextAndAllGLEventListener(gladSource, gladDest);
                            } };

                    // switch context _and_ the demo synchronously
                    if( gladSource.isThreadGLCapable() && gladDest.isThreadGLCapable() ) {
                        switchAction.run();
                    } else {
                        Threading.invokeOnOpenGLThread(true, switchAction);
                    }
                    snapshotGLEventListener.setMakeSnapshot();

                    System.err.println(s+" - switch - END "+ ( t1 - t0 ));
                }
                Thread.sleep(100);
                t1 = System.currentTimeMillis();
            }
        } catch (final RuntimeException t) {
            caught = t;
        }

        animator.stop();
        destroyGLAutoDrawable(gladDest);
        destroyGLAutoDrawable(gladSource);

        if( null != caught ) {
            throw caught;
        }
    }

    // default timing for 2 switches
    static long duration = 2900; // ms
    static long period = 1000; // ms
}
