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

package com.jogamp.opengl.test.junit.graph.demos;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.util.Animator;

/** Demonstrate the rendering of multiple outlines into one region/OutlineShape
 *  These Outlines are not necessary connected or contained.
 *  The output of this demo shows two identical shapes but the left one
 *  has some vertices with off-curve flag set to true, and the right allt he vertices
 *  are on the curve. Demos the Res. Independent Nurbs based Curve rendering
 *
 */
public class GPURegionNewtDemo {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    static int SceneMSAASamples = 0;
    static int GraphVBAASamples = 4;
    static int GraphMSAASamples = 0;
    static boolean GraphUseWeight = true;

    public static void main(final String[] args) {
        int width = 800, height = 400;
        int x = 10, y = 10;
        if( 0 != args.length ) {
            SceneMSAASamples = 0;
            GraphMSAASamples = 0;
            GraphVBAASamples = 0;
            GraphUseWeight = false;

            for(int i=0; i<args.length; i++) {
                if(args[i].equals("-smsaa")) {
                    i++;
                    SceneMSAASamples = MiscUtils.atoi(args[i], SceneMSAASamples);
                } else if(args[i].equals("-gmsaa")) {
                    i++;
                    GraphMSAASamples = MiscUtils.atoi(args[i], GraphMSAASamples);
                    GraphVBAASamples = 0;
                } else if(args[i].equals("-gvbaa")) {
                    i++;
                    GraphMSAASamples = 0;
                    GraphVBAASamples = MiscUtils.atoi(args[i], GraphVBAASamples);
                } else if(args[i].equals("-gweight")) {
                    GraphUseWeight = true;
                } else if(args[i].equals("-width")) {
                    i++;
                    width = MiscUtils.atoi(args[i], width);
                } else if(args[i].equals("-height")) {
                    i++;
                    height = MiscUtils.atoi(args[i], height);
                } else if(args[i].equals("-x")) {
                    i++;
                    x = MiscUtils.atoi(args[i], x);
                } else if(args[i].equals("-y")) {
                    i++;
                    y = MiscUtils.atoi(args[i], y);
                }
            }
        }
        System.err.println("Desired win size "+width+"x"+height);
        System.err.println("Desired win pos  "+x+"/"+y);
        System.err.println("Scene MSAA Samples "+SceneMSAASamples);
        System.err.println("Graph MSAA Samples"+GraphMSAASamples);
        System.err.println("Graph VBAA Samples "+GraphVBAASamples);
        System.err.println("Graph Weight Mode "+GraphUseWeight);

        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        if( SceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(SceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        int rmode = GraphUseWeight ? Region.VARWEIGHT_RENDERING_BIT : 0;
        int sampleCount = 0;
        if( GraphVBAASamples > 0 ) {
            rmode |= Region.VBAA_RENDERING_BIT;
            sampleCount += GraphVBAASamples;
        } else if( GraphMSAASamples > 0 ) {
            rmode |= Region.MSAA_RENDERING_BIT;
            sampleCount += GraphMSAASamples;
        }

        final GLWindow window = GLWindow.create(caps);
        window.setPosition(x, y);
        window.setSize(width, height);
        window.setTitle("GPU Curve Region Newt Demo - graph[vbaa"+GraphVBAASamples+" msaa"+GraphMSAASamples+"], msaa "+SceneMSAASamples);

        final RenderState rs = RenderState.createRenderState(SVertex.factory());
        final GPURegionGLListener01 regionGLListener = new GPURegionGLListener01 (rs, rmode, sampleCount, DEBUG, TRACE);
        regionGLListener.attachInputListenerTo(window);
        window.addGLEventListener(regionGLListener);
        window.setVisible(true);

        //FPSAnimator animator = new FPSAnimator(60);
        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(60, System.err);
        animator.add(window);

        window.addKeyListener(new KeyAdapter() {
            public void keyPressed(final KeyEvent arg0) {
                if(arg0.getKeyCode() == KeyEvent.VK_F4) {
                    window.destroy();
                }
            }
        });
        window.addWindowListener(new WindowAdapter() {
            public void windowDestroyed(final WindowEvent e) {
                animator.stop();
            }
        });

        animator.start();
    }
}
