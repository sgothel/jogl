/**
 * Copyright 2010-2024 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.demos.graph.ui;

import java.io.IOException;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventAdapter;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.graph.ui.testshapes.Glyph03FreeMonoRegular_M;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.util.Animator;

/**
 * Basic UIShape demo using a Scene and Shape
 *
 * Action Cursor-Keys:
 * - With Shift  : Move the clipping-rectangle itself
 * - With Control: Resize Left and Bottom Clipping Edge of AABBox
 * - No Modifiers: Resize Right and Top Clipping Edge of AABBox
 */
public class UIShapeDemo02a {
    static CommandlineOptions options = new CommandlineOptions(1280, 720, Region.VBAA_RENDERING_BIT);

    public static void main(final String[] args) throws IOException, InterruptedException {
        boolean ok_shape = false;
        boolean use_glyph = false;
        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if(args[idx[0]].equals("-ok")) {
                    ok_shape = true;
                } else if(args[idx[0]].equals("-glyph")) {
                    use_glyph = true;
                }
            }
        }
        System.err.println(options);
        System.err.println("ok_shape "+ok_shape);
        final GLProfile reqGLP = GLProfile.get(options.glProfileName);
        System.err.println("GLProfile: "+reqGLP);

        final Scene scene = new Scene(options.graphAASamples);
        // scene.setPMVMatrixSetup(new Scene.DefaultPMVMatrixSetup(-1f)); // better distance for perspective action
        scene.setPMVMatrixSetup(new MyMatrixSetup());
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setAAQuality(options.graphAAQuality);
        final Animator animator = new Animator(0 /* w/o AWT */);

        final GLCapabilities reqCaps = options.getGLCaps();
        System.out.println("Requested: " + reqCaps);

        final GLWindow window = GLWindow.create(reqCaps);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(UIShapeDemo02a.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.setVisible(true);
        System.out.println("Chosen: " + window.getChosenGLCapabilities());
        window.addGLEventListener(scene);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UIShapeDemo02a.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
            }
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                animator.stop();
            }
        });

        final GraphShape testShape;
        final Rectangle rectShape;
        if( use_glyph ) {
            testShape = new Glyph03FreeMonoRegular_M(options.renderModes);
            testShape.setColor(0.8f, 0.8f, 0.8f, 1);
            testShape.scale(1000, 1000, 1);
            rectShape = null;
            testShape.onDraw((final Shape s, final GL2ES2 gl, final RegionRenderer renderer) -> {
                final GLRegion region = ((GraphShape)s).getRegion();
                renderer.getRenderState().setDebugBits(RenderState.DEBUG_LINESTRIP);
                renderer.setColorStatic(new Vec4f(0, 0, 1, 1));
                region.draw(gl, renderer);
                renderer.getRenderState().clearDebugBits(RenderState.DEBUG_LINESTRIP);
                return false;
            });
            scene.addShape(testShape);
        } else {
            testShape = null;
            rectShape = new Rectangle(options.renderModes, 1, 1, 0);
            rectShape.setColor(0, 0, 1, 1);
            scene.addShape(rectShape);
        }

        scene.attachInputListenerTo(window);
        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent arg0) {
                final short keySym = arg0.getKeySymbol();
                if( keySym == KeyEvent.VK_F4 || keySym == KeyEvent.VK_ESCAPE || keySym == KeyEvent.VK_Q ) {
                    new InterruptSource.Thread( () -> { window.destroy(); } ).start();
                }
            }
        });
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(final WindowEvent e) {
                animator.stop();
            }
        });

        animator.add(window);
        animator.start();
        scene.waitUntilDisplayed();
        final AABBox sbox = scene.getBounds();
        System.err.println("Scene "+sbox);
        if( null != rectShape ) {
            final float sw = sbox.getWidth();
            final float sh = sbox.getHeight();
            final float w, h, w2, lineWidth, delta;
            {
                w = 500f;
                h = 500f;
                lineWidth = ok_shape ? 1.0f : 1.1f;
                delta = 1;
                w2 = w-14*delta;
            }
            rectShape.setDimension(w2, h, lineWidth);
            System.err.printf("R_0: w %30.30f x %30.30f%n", w2, h);
            Thread.sleep(500);
            if( false ) {
                while( window.isNativeValid() ) {
                    for(int i=0; i<300; ++i) {
                        final float wi = w-i*delta;
                        rectShape.setDimension(wi, h, lineWidth);
                        System.err.printf("R_1: %d: w %30.30f x %30.30f%n", i, wi, h);
                        // System.err.println("R_1: "+rect);
                        Thread.sleep(17);
                    }
                }
            }
        }
        scene.screenshot(true, scene.nextScreenshotFile(null, UIShapeDemo02a.class.getSimpleName(), options.renderModes, reqCaps, null));
    }

    private static final class MyMatrixSetup implements Scene.PMVMatrixSetup {

        @Override
        public float getSceneDist() {
            return -0.2f;
        }

        @Override
        public float getAngle() {
            return 0;
        }

        @Override
        public float getZNear() {
            return 0.1f;
        }

        @Override
        public float getZFar() {
            // return 7000f;
            return 1f;
        }

        @Override
        public void set(final PMVMatrix4f pmv, final Recti viewport) {
            final float ratio = (float) viewport.width() / (float) viewport.height();
            pmv.loadPIdentity();
            pmv.orthoP(0, viewport.width(), 0, viewport.height(), getZNear(), getZFar());
            // pmv.perspectiveP(FloatUtil.QUARTER_PI, ratio, getZNear(), getZFar());
            pmv.translateP(0f, 0f, getSceneDist());

            pmv.loadMvIdentity();
        }

        @Override
        public void setPlaneBox(final AABBox planeBox, final PMVMatrix4f pmv, final Recti viewport) {
                final float orthoDist = -getSceneDist();
                final Vec3f obj00Coord = new Vec3f();
                final Vec3f obj11Coord = new Vec3f();

                Scene.winToPlaneCoord(pmv, viewport, getZNear(), getZFar(), viewport.x(), viewport.y(), orthoDist, obj00Coord);
                Scene.winToPlaneCoord(pmv, viewport, getZNear(), getZFar(), viewport.width(), viewport.height(), orthoDist, obj11Coord);

                planeBox.setSize( obj00Coord, obj11Coord );
        }
    }

};


