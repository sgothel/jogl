/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.demos.graph;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.math.geom.AABBox;

import java.io.File;
import java.io.IOException;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontScale;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;

public class GPUTextNewtDemo {
    /**
     * FIXME:
     *
     * If DEBUG is enabled:
     *
     * Caused by: com.jogamp.opengl.GLException: Thread[main-Display-X11_:0.0-1-EDT-1,5,main] glGetError() returned the following error codes after a call to glFramebufferRenderbuffer(<int> 0x8D40, <int> 0x1902, <int> 0x8D41, <int> 0x1): GL_INVALID_ENUM ( 1280 0x500),
     * at com.jogamp.opengl.DebugGL4bc.checkGLGetError(DebugGL4bc.java:33961)
     * at com.jogamp.opengl.DebugGL4bc.glFramebufferRenderbuffer(DebugGL4bc.java:33077)
     * at jogamp.graph.curve.opengl.VBORegion2PGL3.initFBOTexture(VBORegion2PGL3.java:295)
     */
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    static int SceneMSAASamples = 0;
    static int GraphVBAASamples = 4;
    static int GraphMSAASamples = 0;

    public static void main(final String[] args) throws IOException {
        Font opt_font = null;
        int opt_fontSizeHead = -1;
        int width = 800, height = 400;
        int x = 10, y = 10;
        if( 0 != args.length ) {
            for(int i=0; i<args.length; i++) {
                if(args[i].equals("-smsaa")) {
                    i++;
                    SceneMSAASamples = MiscUtils.atoi(args[i], SceneMSAASamples);
                    GraphMSAASamples = 0;
                    GraphVBAASamples = 0;
                } else  if(args[i].equals("-gmsaa")) {
                    i++;
                    SceneMSAASamples = 0;
                    GraphMSAASamples = MiscUtils.atoi(args[i], GraphMSAASamples);
                    GraphVBAASamples = 0;
                } else if(args[i].equals("-gvbaa")) {
                    i++;
                    SceneMSAASamples = 0;
                    GraphMSAASamples = 0;
                    GraphVBAASamples = MiscUtils.atoi(args[i], GraphVBAASamples);
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
                } else if(args[i].equals("-font")) {
                    i++;
                    opt_font = FontFactory.get(new File(args[i]));
                } else if(args[i].equals("-fontSize")) {
                    i++;
                    opt_fontSizeHead = MiscUtils.atoi(args[i], opt_fontSizeHead);
                }
            }
        }
        System.err.println("Desired win size "+width+"x"+height);
        System.err.println("Desired win pos  "+x+"/"+y);
        System.err.println("Scene MSAA Samples "+SceneMSAASamples);
        System.err.println("Graph MSAA Samples "+GraphMSAASamples);
        System.err.println("Graph VBAA Samples "+GraphVBAASamples);

        final GLProfile glp = GLProfile.getGL2ES2();

        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        if( SceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(SceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        int rmode = 0; // Region.VARIABLE_CURVE_WEIGHT_BIT;
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
        window.setTitle("GPU Text Newt Demo - graph[vbaa"+GraphVBAASamples+" msaa"+GraphMSAASamples+"], msaa "+SceneMSAASamples);

        final GPUTextGLListener0A textGLListener = new GPUTextGLListener0A(glp, rmode, sampleCount, true, DEBUG, TRACE);
        textGLListener.setFont(opt_font);
        textGLListener.setFontHeadSize(opt_fontSizeHead);
        // ((TextRenderer)textGLListener.getRenderer()).setCacheLimit(32);
        window.addGLEventListener(textGLListener);
        window.setVisible(true);

        {
            final Font font2 = textGLListener.getFont();
            final float[] sDPI = FontScale.ppmmToPPI( window.getPixelsPerMM(new float[2]) );
            final float font_ptpi = 12f;
            final float font_ppi = FontScale.toPixels(font_ptpi, sDPI[1]);
            final AABBox fontNameBox = font2.getMetricBounds(GPUTextRendererListenerBase01.textX1);
            System.err.println("GPU Text Newt Demo: "+font2.fullString());
            System.err.println("GPU Text Newt Demo: screen-dpi: "+sDPI[0]+"x"+sDPI[1]+", font "+font_ptpi+" pt, "+font_ppi+" pixel");
            System.err.println("GPU Text Newt Demo: textX2: "+fontNameBox+" em, "+fontNameBox.scale(font_ppi)+" px");
            final MonitorDevice monitor = window.getMainMonitor();
            System.err.println("GPU Text Newt Demo: "+monitor);
            // window.setSurfaceSize((int)(fontNameBox.getWidth()*1.1f), (int)(fontNameBox.getHeight()*2f));
        }

        // FPSAnimator animator = new FPSAnimator(60);
        final Animator animator = new Animator(0 /* w/o AWT */);
        animator.setUpdateFPSFrames(60, null);
        animator.add(window);

        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent arg0) {
                if(arg0.getKeyCode() == KeyEvent.VK_F4) {
                    new InterruptSource.Thread() {
                        @Override
                        public void run() {
                            window.destroy();
                        } }.start();
                }
            }
        });
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(final WindowEvent e) {
                animator.stop();
            }
        });

        animator.start();
    }
}
