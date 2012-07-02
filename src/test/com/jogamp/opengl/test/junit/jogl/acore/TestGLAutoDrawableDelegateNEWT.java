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
 
package com.jogamp.opengl.test.junit.jogl.acore;

import java.io.IOException;

import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawableDelegate;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import org.junit.Test;

import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.NEWTGLContext;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

public class TestGLAutoDrawableDelegateNEWT extends UITestCase {

    @Test
    public void test01() throws GLException, InterruptedException {
        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createOnscreenWindow(
                new GLCapabilities(GLProfile.getGL2ES2()), 640, 480, false);
        winctx.context.release();
        
        final GLAutoDrawableDelegate glad = new GLAutoDrawableDelegate(winctx.drawable, winctx.context);
        glad.addGLEventListener(new GearsES2(1));

        // add basic window interaction
        winctx.window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowRepaint(WindowUpdateEvent e) {
                glad.defaultRepaintOp();
            }
            @Override
            public void windowResized(WindowEvent e) {
                glad.defaultReshapeOp();
            }
            @Override
            public void windowDestroyNotify(WindowEvent e) {
                final GLAnimatorControl ctrl = glad.getAnimator();
                boolean isPaused = ctrl.pause();
                glad.destroy();
                NEWTGLContext.destroyWindow(winctx);
                if(isPaused) {
                    ctrl.resume();
                }
            }
        });
        
        final QuitAdapter quitAdapter = new QuitAdapter();
        winctx.window.addWindowListener(quitAdapter);
        
        final Animator animator = new Animator(glad);
        animator.setUpdateFPSFrames(60, null);
        animator.start();
        
        while(!quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        animator.stop();
        NEWTGLContext.destroyWindow(winctx);
    }

    static long duration = 2000; // ms

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); */         
        org.junit.runner.JUnitCore.main(TestGLAutoDrawableDelegateNEWT.class.getName());
    }
}
