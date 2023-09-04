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
import com.jogamp.common.util.IOUtil;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.graph.FontSetDemos;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * Res independent Shape, Scene attached to GLWindow showing simple linear Shape movement.
 * <p>
 * This variation of {@link UISceneDemo00} shows a text animation assembling one line of text,
 * each glyph coming from the right moving to its destination sequentially.
 * </p>
 * <p>
 * Pass '-keep' to main-function to keep running.
 * Pass '-auto' to main-function to keep running and change speed for each animation cycle
 * </p>
 */
public class UISceneDemo02 {
    static CommandlineOptions options = new CommandlineOptions(1280, 720, Region.VBAA_RENDERING_BIT);
    static float req_total_dur_s = 6f; // [s]

    public static void main(final String[] args) throws IOException {
        int autoSpeed = 0;

        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if(args[idx[0]].equals("-aspeed")) {
                    autoSpeed = 1;
                    req_total_dur_s = 1f;
                    options.keepRunning = true;
                }
            }
        }
        System.err.println(options);
        final GLProfile reqGLP = GLProfile.get(options.glProfileName);

        //
        // Resolution independent, no screen size
        //
        final Font font = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerif.ttf", FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        // final Font font = FontFactory.get(IOUtil.getResource("jogamp/graph/font/fonts/ubuntu/Ubuntu-R.ttf", FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        System.err.println("Font: "+font.getFullFamilyName());
        final Font fontStatus = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMono.ttf", FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);

        final Label destText = new Label(options.renderModes, font, "");
        destText.setColor(0.1f, 0.1f, 0.1f, 1);
        final Label movingGlyph = new Label(options.renderModes, font, "");
        movingGlyph.setColor(0.1f, 0.1f, 0.1f, 1);

        final Scene scene = new Scene(options.graphAASamples);
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.addShape(destText);
        scene.addShape(movingGlyph);

        final Animator animator = new Animator(0 /* w/o AWT */);
        animator.setUpdateFPSFrames(1*60, null); // System.err);

        final GLCapabilities caps = new GLCapabilities(reqGLP);
        caps.setAlphaBits(4);
        System.out.println("Requested: " + caps);

        final GLWindow window = GLWindow.create(caps);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(UISceneDemo02.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.setVisible(true);
        window.addGLEventListener(scene);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UISceneDemo02.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
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

        final GLProfile hasGLP = window.getChosenGLCapabilities().getGLProfile();
        final AABBox sceneBox = scene.getBounds();
        System.err.println("SceneBox "+sceneBox);

        if( options.wait_to_start ) {
            MiscUtils.waitForKey("Start");
        }

        final Label statusLabel;
        {
            final AABBox fbox = fontStatus.getGlyphBounds("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            // final float statusLabelScale = (sceneBox.getHeight() / 30f) * fbox.getHeight();
            final float statusLabelScale = sceneBox.getWidth() / fbox.getWidth();
            System.err.println("StatusLabel Scale: " + statusLabelScale + " = " + sceneBox.getWidth() + " / " + fbox.getWidth() + ", " + fbox);

            statusLabel = new Label(options.renderModes, fontStatus, "Nothing there yet");
            statusLabel.setScale(statusLabelScale, statusLabelScale, 1f);
            statusLabel.setColor(0.1f, 0.1f, 0.1f, 1.0f);
            statusLabel.moveTo(sceneBox.getMinX(), sceneBox.getMinY() + statusLabelScale * (fontStatus.getMetrics().getLineGap() - fontStatus.getMetrics().getDescent()), 0f);
            scene.addShape(statusLabel);
        }

        final String originalText = "GraphUI & JOGL, Resolution Agnostic Curve Rendering @ GPU via OpenGL® on Java™";
        // String originalText = "JOGL, Java™ Binding for the OpenGL® API";

        //
        // Compute the metric animation values -> shape obj-velocity
        //
        float req_dur_s = 0f;
        float req_velocity = 0f;
        final float init_velocity = 2000/1e3f; // [m]/[s]

        final float pixPerMM, dpiV;
        {
            final float[] tmp = window.getPixelsPerMM(new float[2]);
            pixPerMM = tmp[0]; // [px]/[mm]
            final float[] sDPI = MonitorDevice.perMMToPerInch( tmp );
            dpiV = sDPI[1];
        }

        do {
            final AABBox fbox = font.getGlyphBounds(originalText);
            final float fontScale = sceneBox.getWidth() / fbox.getWidth();
            System.err.println("FontScale: "+fontScale+" = "+sceneBox.getWidth()+" / "+fbox.getWidth());
            destText.setScale(fontScale, fontScale, 1f);
            movingGlyph.setScale(fontScale, fontScale, 1f);

            destText.moveTo(sceneBox.getMinX(), 0f, 0f);

            final long t0_us = Clock.currentNanos() / 1000; // [us]
            float exp_total_dur_s = 0f;
            float total_dist_m = 0f;
            for(int idx = 0; idx < originalText.length() && window.isNativeValid(); ++idx ) {
                boolean skipChar = false;
                final String[] movingChar = { null };
                do {
                    movingChar[0] = originalText.substring(idx, idx+1);
                    if( Character.isWhitespace(movingChar[0].charAt(0)) ) {
                        destText.setText(destText.getText() + movingChar[0]);
                        ++idx;
                        skipChar = true;
                    } else {
                        skipChar = false;
                    }
                } while( skipChar && idx < originalText.length() );
                if( movingChar[0].isEmpty() ) {
                    break; // bail
                }
                // sync point
                destText.validate(hasGLP);
                movingGlyph.setText(hasGLP, movingChar[0]);
                final float start_pos = sceneBox.getMaxX() - movingGlyph.getScaledWidth();
                final float end_pos = sceneBox.getMinX() + ( destText.getText().length() == 0 ? 0 : destText.getScaledWidth() );
                movingGlyph.moveTo(start_pos, 0f, 0f);

                final PMVMatrix pmv = new PMVMatrix();
                final int[] destTextSizePx = destText.getSurfaceSize(scene, pmv, new int[2]); // [px]
                final int[] movingGlyphSizePx = movingGlyph.getSurfaceSize(scene, pmv, new int[2]); // [px]
                final float[] movingGlyphPixPerShapeUnit = movingGlyph.getPixelPerShapeUnit(movingGlyphSizePx, new float[2]); // [px]/[shapeUnit]

                final float dist_px = scene.getWidth() - movingGlyphSizePx[0]; // [px]
                final float dist_m = dist_px/pixPerMM/1e3f; // [m]
                final float exp_dur_s = dist_m / init_velocity; // [s]
                total_dist_m += dist_m;
                if( 0 == idx ) {
                    exp_total_dur_s = ( exp_dur_s * originalText.length() ) / 2f; // Gauss'ian sum estimate
                    req_dur_s = ( req_total_dur_s * 2f ) / originalText.length();
                    req_velocity = dist_m / req_dur_s;
                    // req_dur_s = exp_dur_s;
                    // req_velocity = init_velocity;
                }
                final float velocity_px = req_velocity * 1e3f * pixPerMM; // [px]/[s]
                final float velocity_obj = velocity_px / movingGlyphPixPerShapeUnit[0]; // [shapeUnit]/[s]

                if( 0 == idx ) {
                    System.err.println();
                    System.err.printf("DestText: %d x %d [pixel], %s%n", destTextSizePx[0], destTextSizePx[1], destText.getText());
                    System.err.printf("MovingGl: %d x %d [pixel], %.4f px/su, %s%n", movingGlyphSizePx[0], movingGlyphSizePx[1], movingGlyphPixPerShapeUnit[0], movingGlyph.getText());
                    // System.err.printf("Shape: %s%n", movingGlyph);
                    System.err.println();
                    System.err.printf("Distance: %.0f pixel @ %.3f px/mm, %.3f mm%n", dist_px, pixPerMM, dist_m*1e3f);
                    System.err.printf("Velocity: init %.3f mm/s, req %.3f mm/s, %.3f px/s, %.6f obj/s, duration exp %.3f s, req %.3f s%n",
                            init_velocity*1e3f, req_velocity*1e3f, velocity_px, velocity_obj, exp_dur_s, req_dur_s);
                    // System.err.println();
                    // System.err.printf("Path: start %.4f, end %.4f, pos %.5f%n", start_pos, end_pos, movingGlyph.getPosition()[0]);
                }

                final long t1_us = Clock.currentNanos() / 1000; // [us]
                final long[] t2_us = { t1_us };
                while( movingGlyph.getPosition().x() > end_pos && window.isNativeValid() ) {
                    // Move on GL thread to have vsync for free
                    // Otherwise we would need to employ a sleep(..) w/ manual vsync
                    final long[] t3_us = { 0 };
                    window.invoke(true, (drawable) -> {
                        t3_us[0] = Clock.currentNanos() / 1000;
                        final float dt_s = ( t3_us[0] - t2_us[0] ) / 1e6f;
                        final float dx = -1f * velocity_obj * dt_s; // [shapeUnit]
                        movingGlyph.move(dx, 0f, 0f);
                        final String text = String.format("%s, anim-duration %.1f s",
                                scene.getStatusText(drawable, options.renderModes, 0, dpiV), req_total_dur_s);
                        statusLabel.setText(text);
                        return true;
                    });
                    t2_us[0] = t3_us[0];
                }
                if( 0 == idx ) {
                    final float has_dur_s = ( ( Clock.currentNanos() / 1000 ) - t1_us ) / 1e6f; // [us]
                    System.err.printf("Actual char travel-duration %.3f s, %.3f mm/s, delay exp %.3f s, req %.3f%n",
                            has_dur_s, (dist_m/has_dur_s)*1e3f, has_dur_s-exp_dur_s, has_dur_s-req_dur_s);
                }
                destText.setText( new StringBuilder(destText.getText()).append(movingGlyph.getText()) );
                movingGlyph.setText("");
            }
            final float has_dur_s = ( ( Clock.currentNanos() / 1000 ) - t0_us ) / 1e6f; // [us]
            System.err.printf("Text travel-duration %.3f s, dist %.3f mm, %.3f mm/s, %d chars, %.3f s/char; Exp %.3f s, delay %.3f s, Req %.3f s, delay %.3f s%n",
                    has_dur_s, total_dist_m*1e3f, (total_dist_m/has_dur_s)*1e3f, originalText.length(), has_dur_s / originalText.length(),
                    exp_total_dur_s, has_dur_s - exp_total_dur_s,
                    req_total_dur_s, has_dur_s - req_total_dur_s);
            try { Thread.sleep(1000); } catch (final InterruptedException e1) { }
            destText.setText("");
            if( autoSpeed > 0 ) {
                if( req_total_dur_s > 3f ) {
                    req_total_dur_s -= 3f;
                } else {
                    req_total_dur_s += 3f;
                    autoSpeed = -1;
                }
            } else  if( autoSpeed < 0 ) {
                if( req_total_dur_s < 10f ) {
                    req_total_dur_s += 3f;
                } else {
                    req_total_dur_s -= 3f;
                    autoSpeed = 1;
                }
            }
        } while ( options.keepRunning && window.isNativeValid() );
        if( !options.keepRunning ) {
            window.destroy();
        }
    }
}
