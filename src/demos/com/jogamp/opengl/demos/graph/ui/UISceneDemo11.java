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
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.Gap;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * Res independent {@link Shape}s in a {@link Group} using a {@link GridLayout}, contained within a Scene attached to GLWindow.
 * <p>
 * Pass '-keep' to main-function to keep running after animation,
 * then user can test Shape drag-move and drag-resize w/ 1-pointer.
 * </p>
 */
public class UISceneDemo11 {
    static CommandlineOptions options = new CommandlineOptions(1280, 720, Region.VBAA_RENDERING_BIT);

    public static void main(final String[] args) throws IOException {
        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                }
            }
        }
        System.err.println(options);

        final GLProfile reqGLP = GLProfile.get(options.glProfileName);
        System.err.println("GLProfile: "+reqGLP);

        //
        // Resolution independent, no screen size
        //
        final Font font = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_SERIF);
        System.err.println("Font: "+font.getFullFamilyName());

        final Group groupA0 = new Group(new GridLayout(2, 1f, 1/2f, Alignment.Fill, new Gap(0.10f)));
        {
            groupA0.addShape( new Button(options.renderModes, font, "r1 c1", 1f, 1f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupA0.addShape( new Button(options.renderModes, font, "r1 c2", 1f, 1f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupA0.addShape( new Button(options.renderModes, font, "r2 c1", 1f, 1f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupA0.addShape( new Button(options.renderModes, font, "r2 c2", 1f, 1f/2f).setCorner(0f).setDragAndResizeable(false) );
        }
        groupA0.setInteractive(true);
        groupA0.scale(1/8f, 1/8f, 1);
        groupA0.validate(reqGLP);
        System.err.println("Group-A0 "+groupA0);
        System.err.println("Group-A0 Layout "+groupA0.getLayout());
        groupA0.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });

        final Scene scene = new Scene(options.graphAASamples);
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.addShape(groupA0);
        scene.setFrustumCullingEnabled(true);

        final Animator animator = new Animator(0 /* w/o AWT */);

        final GLCapabilities caps = new GLCapabilities(reqGLP);
        caps.setAlphaBits(4);
        if( options.sceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(options.sceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        final GLWindow window = GLWindow.create(caps);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(UISceneDemo11.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.setVisible(true);
        window.addGLEventListener(scene);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UISceneDemo11.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
            }
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                animator.stop();
            }
        });

        scene.attachInputListenerTo(window);

        animator.setUpdateFPSFrames(1*60, null); // System.err);
        animator.add(window);
        animator.start();

        //
        // After initial display we can use screen resolution post initial Scene.reshape(..)
        // However, in this example we merely use the resolution to
        // - Compute the animation values with DPI
        scene.waitUntilDisplayed();

        final AABBox sceneBox = scene.getBounds();
        System.err.println("SceneBox "+sceneBox);
        System.err.println("Group-A0 "+groupA0);
        groupA0.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        groupA0.moveTo(0, sceneBox.getMinY(), 0f); // move shape to min start position
        try { Thread.sleep(1000); } catch (final InterruptedException e1) { }

        final Shape mobileShape = groupA0;

        if( true ) {
        //
        // Compute the metric animation values -> shape obj-velocity
        //
        final float min_obj = sceneBox.getMinX();
        final float max_obj = sceneBox.getMaxX() - mobileShape.getScaledWidth();

        final int[] shapeSizePx = mobileShape.getSurfaceSize(scene, new PMVMatrix(), new int[2]); // [px]
        final float[] pixPerShapeUnit = mobileShape.getPixelPerShapeUnit(shapeSizePx, new float[2]); // [px]/[shapeUnit]

        final float pixPerMM = window.getPixelsPerMM(new float[2])[0]; // [px]/[mm]
        final float dist_px = scene.getWidth() - shapeSizePx[0]; // [px]
        final float dist_m = dist_px/pixPerMM/1e3f; // [m]
        final float velocity = 50/1e3f; // [m]/[s]
        final float velocity_px = velocity * 1e3f * pixPerMM; // [px]/[s]
        final float velovity_obj = velocity_px / pixPerShapeUnit[0]; // [shapeUnit]/[s]
        final float exp_dur_s = dist_m / velocity; // [s]

        System.err.println();
        System.err.printf("Shape: %d x %d [pixel], %.4f px/shape_unit%n", shapeSizePx[0], shapeSizePx[1], pixPerShapeUnit[0]);
        System.err.printf("Shape: %s%n", mobileShape);
        System.err.println();
        System.err.printf("Distance: %.0f pixel @ %.3f px/mm, %.3f mm%n", dist_px, pixPerMM, dist_m*1e3f);
        System.err.printf("Velocity: %.3f mm/s, %.3f px/s, %.6f obj/s, expected travel-duration %.3f s%n",
                velocity*1e3f, velocity_px, velovity_obj, exp_dur_s);

        final long t0_us = Clock.currentNanos() / 1000; // [us]
        long t1_us = t0_us;
        mobileShape.moveTo(min_obj, sceneBox.getMinY(), 0f); // move shape to min start position
        while( mobileShape.getPosition().x() < max_obj && window.isNativeValid() ) {
            final long t2_us = Clock.currentNanos() / 1000;
            final float dt_s = ( t2_us - t1_us ) / 1e6f;
            t1_us = t2_us;

            final float dx = velovity_obj * dt_s; // [shapeUnit]
            // System.err.println("move ")

            // Move on GL thread to have vsync for free
            // Otherwise we would need to employ a sleep(..) w/ manual vsync
            window.invoke(true, (drawable) -> {
                mobileShape.move(dx, 0f, 0f);
                return true;
            });
        }
        mobileShape.moveTo(max_obj, sceneBox.getMinY(), 0f); // move shape to min start position
        final float has_dur_s = ( ( Clock.currentNanos() / 1000 ) - t0_us ) / 1e6f; // [us]
        System.err.printf("Actual travel-duration %.3f s, delay %.3f s%n", has_dur_s, has_dur_s-exp_dur_s);
        System.err.println("Group-A0 bounds "+groupA0);
        groupA0.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        try { Thread.sleep(1000); } catch (final InterruptedException e1) { }
        }
        if( !options.stayOpen ) {
            window.destroy();
        }
    }
}
