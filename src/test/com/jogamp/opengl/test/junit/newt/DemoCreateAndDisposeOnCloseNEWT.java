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
package com.jogamp.opengl.test.junit.newt;

import com.jogamp.nativewindow.WindowClosingProtocol;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.util.Animator;

/**
 * Manual test case validating closing behavior.
 * <p>
 * Validates bugs:
 * <ul>
 *   <li>Bug 882: Crash on OSX when closing NEWT window</li>
 * </ul>
 * </p>
 *
 */
public class DemoCreateAndDisposeOnCloseNEWT {
    public static void main(final String[] args) {
        int closeMode = 0; // 0 - none, 1 - window, animator, 2 - animator, window, 3 - System.exit

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-close")) {
                closeMode = MiscUtils.atoi(args[++i], closeMode);
            }
        }
        System.err.println("Close Mode: "+closeMode);

        final GLCapabilities caps = new GLCapabilities(GLProfile.getMaxProgrammable(true));
        caps.setBackgroundOpaque(true);
        caps.setDoubleBuffered(true);
        caps.setDepthBits(16);
        final Animator animator = new Animator();
        final GLWindow glWindow = GLWindow.create(caps);
        animator.add(glWindow);
        glWindow.addGLEventListener(new GLEventListener() {
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                System.out.println("GLEventListener.reshape");
            }

            @Override
            public void init(final GLAutoDrawable drawable) {
                System.out.println("GLEventListener.init");
            }

            @Override
            public void dispose(final GLAutoDrawable drawable) {
                System.out.println("GLEventListener.dispose");
            }

            @Override
            public void display(final GLAutoDrawable drawable) {
            }
        });
        glWindow.setTitle("Test");
        glWindow.setSize(1024, 768);
        glWindow.setUndecorated(false);
        glWindow.setPointerVisible(true);
        glWindow.setVisible(true);
        glWindow.setFullscreen(false);
        glWindow.setDefaultCloseOperation(WindowClosingProtocol.WindowClosingMode.DISPOSE_ON_CLOSE);
        glWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                System.out.println("GLWindow.destroyNotify");
            }

            @Override
            public void windowDestroyed(final WindowEvent e) {
                System.out.println("GLWindow.destroyed");
                animator.stop();
            }
        });

        animator.start();

        switch( closeMode ) {
            case 1:
                sleep1s();
                glWindow.destroy();
                sleep1s();
                animator.stop();
                break;
            case 2:
                sleep1s();
                animator.stop();
                sleep1s();
                glWindow.destroy();
                break;
            case 3:
                sleep1s();
                System.exit(0);
                break;
            default: break; // 0 - nop
        }
    }
    static void sleep1s() {
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

}
