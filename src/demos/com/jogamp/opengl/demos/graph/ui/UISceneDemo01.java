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
package com.jogamp.opengl.demos.graph.ui;

import java.io.IOException;

import com.jogamp.common.os.Clock;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.ui.gl.Scene;
import com.jogamp.graph.ui.gl.Scene.PMVMatrixSetup;
import com.jogamp.graph.ui.gl.Shape;
import com.jogamp.graph.ui.gl.shapes.GLButton;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.es2.GearsES2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * Res independent Shape, in Scene attached to GLWindow showing simple linear Shape movement.
 * <p>
 * This variation of {@link UISceneDemo00} uses a {@link GLButton} shape with animating and rotating gears
 * and sets up an own {@link Scene.PMVMatrixSetup} with a plane dimension of 100.
 * </p>
 * <p>
 * Pass '-keep' to main-function to keep running after animation,
 * then user can test Shape drag-move and drag-resize w/ 1-pointer.
 * </p>
 */
public class UISceneDemo01 {
    public static void main(final String[] args) throws IOException {
        final int surface_width = 1280, surface_height = 720;
        final int renderModes = Region.VBAA_RENDERING_BIT;
        final GLProfile glp = GLProfile.getGL2ES2();

        boolean keepRunning = false;
        if( 0 != args.length ) {
            for(int i=0; i<args.length; i++) {
                if(args[i].equals("-keep")) {
                    keepRunning = true;
                }
            }
        }

        //
        // Resolution independent, no screen size
        //
        final Font font = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_SERIF);
        System.err.println("Font: "+font.getFullFamilyName());

        final Shape shape = makeShape(font, renderModes);
        System.err.println("Shape bounds "+shape.getBounds(glp));
        System.err.println("Shape "+shape);

        final Scene scene = new Scene();
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setPMVMatrixSetup(new MyPMVMatrixSetup());
        scene.addShape(shape);

        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(1*60, null); // System.err);

        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        System.out.println("Requested: " + caps);

        final GLWindow window = GLWindow.create(caps);
        window.setSize(surface_width, surface_height);
        window.setTitle(UISceneDemo01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.setVisible(true);
        window.addGLEventListener(scene);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UISceneDemo01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
            }
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                animator.stop();
            }
        });

        scene.attachInputListenerTo(window);

        animator.add(window);
        animator.start();

        //
        // After initial display we can use screen resolution post initial Scene.reshape(..)
        // However, in this example we merely use the resolution to
        // - Compute the animation values with DPI
        scene.waitUntilDisplayed();

        final AABBox sceneBox = scene.getBounds();
        System.err.println("SceneBox "+sceneBox);
        System.err.println("Shape "+shape);
        try { Thread.sleep(1000); } catch (final InterruptedException e1) { }

        //
        // Compute the metric animation values -> shape obj-velocity
        //
        final float min_obj = sceneBox.getMinX();
        final float max_obj = sceneBox.getMaxX() - shape.getScaledWidth();

        final int[] shapeSizePx = shape.getSurfaceSize(scene, new PMVMatrix(), new int[2]); // [px]
        final float[] pixPerShapeUnit = shape.getPixelPerShapeUnit(shapeSizePx, new float[2]); // [px]/[shapeUnit]

        final float pixPerMM = window.getPixelsPerMM(new float[2])[0]; // [px]/[mm]
        final float dist_px = scene.getWidth() - shapeSizePx[0]; // [px]
        final float dist_m = dist_px/pixPerMM/1e3f; // [m]
        final float velocity = 50/1e3f; // [m]/[s]
        final float velocity_px = velocity * 1e3f * pixPerMM; // [px]/[s]
        final float velovity_obj = velocity_px / pixPerShapeUnit[0]; // [shapeUnit]/[s]
        final float exp_dur_s = dist_m / velocity; // [s]

        System.err.println();
        System.err.printf("Shape: %d x %d [pixel], %.4f px/shape_unit%n", shapeSizePx[0], shapeSizePx[1], pixPerShapeUnit[0]);
        System.err.printf("Shape: %s%n", shape);
        System.err.println();
        System.err.printf("Distance: %.0f pixel @ %.3f px/mm, %.3f mm%n", dist_px, pixPerMM, dist_m*1e3f);
        System.err.printf("Velocity: %.3f mm/s, %.3f px/s, %.6f obj/s, expected travel-duration %.3f s%n",
                velocity*1e3f, velocity_px, velovity_obj, exp_dur_s);

        final long t0_us = Clock.currentNanos() / 1000; // [us]
        long t1_us = t0_us;
        shape.moveTo(min_obj, 0f, 0f); // move shape to min start position
        while( shape.getPosition()[0] < max_obj && window.isNativeValid() ) {
            final long t2_us = Clock.currentNanos() / 1000;
            final float dt_s = ( t2_us - t1_us ) / 1e6f;
            t1_us = t2_us;

            final float dx = velovity_obj * dt_s; // [shapeUnit]
            // System.err.println("move ")

            // Move on GL thread to have vsync for free
            // Otherwise we would need to employ a sleep(..) w/ manual vsync
            window.invoke(true, (drawable) -> {
                shape.move(dx, 0f, 0f);
                return true;
            });
        }
        final float has_dur_s = ( ( Clock.currentNanos() / 1000 ) - t0_us ) / 1e6f; // [us]
        System.err.printf("Actual travel-duration %.3f s, delay %.3f s%n", has_dur_s, has_dur_s-exp_dur_s);
        try { Thread.sleep(1000); } catch (final InterruptedException e1) { }
        if( !keepRunning ) {
            window.destroy();
        }
    }

    static class MyPMVMatrixSetup implements PMVMatrixSetup {
        @Override
        public void set(final PMVMatrix pmv, final int x, final int y, final int width, final int height) {
            final float ratio = (float)width/(float)height;
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glLoadIdentity();
            pmv.gluPerspective(Scene.DEFAULT_ANGLE, ratio, Scene.DEFAULT_ZNEAR, Scene.DEFAULT_ZFAR);
            pmv.glTranslatef(0f, 0f, Scene.DEFAULT_SCENE_DIST);

            // Scale (back) to have normalized plane dimensions, 100 for the greater of width and height.
            final AABBox planeBox0 = new AABBox();
            setPlaneBox(planeBox0, pmv, x, y, width, height);
            final float sx = planeBox0.getWidth();
            final float sy = planeBox0.getHeight();
            final float sxy = sx > sy ? sx : sy;
            pmv.glScalef(sxy / 100f, sxy / 100f, 1f);

            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();
        }

        @Override
        public void setPlaneBox(final AABBox planeBox, final PMVMatrix pmv, final int x, final int y, final int width, final int height) {
            Scene.getDefaultPMVMatrixSetup().setPlaneBox(planeBox, pmv, x, y, width, height);
        }
    };

    static Shape makeShape(final Font font, final int renderModes) {
        final float sw = 25;
        final float sh = sw / 2.5f;

        final GearsES2 gears = new GearsES2(0);
        gears.setVerbose(false);
        gears.setClearColor(new float[] { 0.9f, 0.9f, 0.9f, 1f } );
        final boolean[] animate = { true };
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.err.println("Gears Anim: Waiting");
                try {
                    gears.waitForInit(true);
                } catch (final InterruptedException e) { }
                System.err.println("Gears Anim: Started");
                while( gears.isInit() ) {
                    if( animate[0] ) {
                        final float ry = ( gears.getRotY() + 1 ) % 360;
                        gears.setRotY(ry);
                    }
                    try {
                        Thread.sleep(15);
                    } catch (final InterruptedException e) { }
                }
                System.err.println("Gears Anim: End");
            }
        }).start();
        final int texUnit = 1;
        final GLButton b = new GLButton(renderModes, sw,
                                        sh, texUnit, gears, false /* useAlpha */);
        b.setToggleable(true);
        b.setToggle(true); // toggle == true -> animation
        b.setAnimate(true);
        b.addMouseListener(new Shape.MouseGestureAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                b.setAnimate( b.isToggleOn() );
                animate[0] = b.getAnimate();
            } } );
        return b;
    }

}
