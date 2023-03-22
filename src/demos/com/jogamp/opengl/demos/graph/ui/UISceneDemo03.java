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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.jogamp.common.os.Clock;
import com.jogamp.common.util.IOUtil;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.ui.gl.Scene;
import com.jogamp.graph.ui.gl.Scene.PMVMatrixSetup;
import com.jogamp.graph.ui.gl.shapes.Label;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.graph.FontSetDemos;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * Res independent Shape, Scene attached to GLWindow showing simple linear Shape movement.
 * <p>
 * This variation of {@link UISceneDemo00} shows a text animation assembling one line of text,
 * each glyph coming from from a random 3D point moving to its destination all at once.
 * </p>
 * <p>
 * Pass '-keep' to main-function to keep running.
 * </p>
 */
public class UISceneDemo03 {
    // final String originalText = "JOGL, Java™ Binding for the OpenGL® API";
    static final String[] originalTexts = {
            "JOGL, Java™ Binding for the OpenGL® API",
            "GraphUI, Resolution Independent Curves",
            "JogAmp, Java™ libraries for 3D & Media"
    };

    static int renderModes = Region.VBAA_RENDERING_BIT;
    static int sceneMSAASamples = 0;
    static float velocity = 30 / 1e3f; // [m]/[s]
    static float rot_step = velocity * 1;

    static void setVelocity(final float v) {
        velocity = v; // Math.max(1/1e3f, v);
        rot_step = velocity * 1;
    }

    public static void main(final String[] args) throws IOException {
        final int surface_width = 1280, surface_height = 720;
        final GLProfile reqGLP = GLProfile.getGL2ES2();
        int autoSpeed = 0;
        boolean wait_to_start = false;

        boolean keepRunning = false;
        if (0 != args.length) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-keep")) {
                    keepRunning = true;
                } else if (args[i].equals("-v")) {
                    ++i;
                    setVelocity(MiscUtils.atoi(args[i], (int) velocity * 1000) / 1000f);
                } else if(args[i].equals("-aspeed")) {
                    autoSpeed = -1;
                    setVelocity(80/1000f);
                    keepRunning = true;
                } else if(args[i].equals("-wait")) {
                    wait_to_start = true;
                } else if(args[i].equals("-gnone")) {
                    sceneMSAASamples = 0;
                    renderModes = 0;
                } else if(args[i].equals("-smsaa")) {
                    i++;
                    sceneMSAASamples = MiscUtils.atoi(args[i], sceneMSAASamples);
                    renderModes = 0;
                } else if(args[i].equals("-gmsaa")) {
                    sceneMSAASamples = 0;
                    renderModes = Region.MSAA_RENDERING_BIT;
                } else if(args[i].equals("-gvbaa")) {
                    sceneMSAASamples = 0;
                    renderModes = Region.VBAA_RENDERING_BIT;
                }
            }
        }

        //
        // Resolution independent, no screen size
        //
        final Font font = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerif.ttf",FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        // final Font font = FontFactory.get(IOUtil.getResource("jogamp/graph/font/fonts/ubuntu/Ubuntu-R.ttf",FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        System.err.println("Font: " + font.getFullFamilyName());
        final Font fontStatus = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMonoBold.ttf", FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);

        final Scene scene = new Scene();
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f }, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setPMVMatrixSetup(new MyPMVMatrixSetup());

        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(1 * 60, null); // System.err);

        final GLCapabilities caps = new GLCapabilities(reqGLP);
        caps.setAlphaBits(4);
        if( sceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(sceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        final GLWindow window = GLWindow.create(caps);
        window.setSize(surface_width, surface_height);
        window.setTitle(UISceneDemo03.class.getSimpleName() + ": " + window.getSurfaceWidth() + " x " + window.getSurfaceHeight());
        window.setVisible(true);
        window.addGLEventListener(scene);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UISceneDemo03.class.getSimpleName() + ": " + window.getSurfaceWidth() + " x " + window.getSurfaceHeight());
            }

            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                animator.stop();
            }
        });
        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                if (e.getKeySymbol() == KeyEvent.VK_PLUS ||
                    e.getKeySymbol() == KeyEvent.VK_ADD)
                {
                    if (e.isShiftDown()) {
                        setVelocity(velocity + 10 / 1000f);
                    } else {
                        setVelocity(velocity + 1 / 1000f);
                    }
                } else if (e.getKeySymbol() == KeyEvent.VK_MINUS ||
                           e.getKeySymbol() == KeyEvent.VK_SUBTRACT)
                {
                    if (e.isShiftDown()) {
                        setVelocity(velocity - 10 / 1000f);
                    } else {
                        setVelocity(velocity - 1 / 1000f);
                    }
                }
            }
        });

        scene.attachInputListenerTo(window);

        animator.add(window);
        animator.start();

        //
        // After initial display we can use screen resolution post initial
        // Scene.reshape(..)
        // However, in this example we merely use the resolution to
        // - Compute the animation values with DPI
        scene.waitUntilDisplayed();

        scene.setFrustumCullingEnabled(true);
        window.invoke(true, (drawable) -> {
            drawable.getGL().glEnable(GL.GL_DEPTH_TEST);
            return true;
        });

        final GLProfile hasGLP = window.getChosenGLCapabilities().getGLProfile();
        final AABBox sceneBox = scene.getBounds();
        System.err.println("SceneBox " + sceneBox);
        System.err.println("Frustum " + scene.getMatrix().glGetFrustum());

        if( wait_to_start ) {
            MiscUtils.waitForKey("Start");
        }

        //
        //
        //
        final float fontScale;
        {
            final AABBox fbox = font.getGlyphBounds(originalTexts[0]);
            fontScale = sceneBox.getWidth() / fbox.getWidth();
            // final float fontScale = sceneBox.getWidth() / 20;
            System.err.println("FontScale: " + fontScale + " = " + sceneBox.getWidth() + " / " + fbox.getWidth());
        }
        final Label statusLabel;
        {
            final AABBox fbox = fontStatus.getGlyphBounds("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            // final float statusLabelScale = (sceneBox.getHeight() / 30f) * fbox.getHeight();
            final float statusLabelScale = sceneBox.getWidth() / fbox.getWidth();
            System.err.println("StatusLabelScale: " + statusLabelScale + " = " + sceneBox.getHeight() + " / " + fbox.getHeight() + ", " + fbox);

            statusLabel = new Label(renderModes, fontStatus, statusLabelScale, "Nothing there yet");
            statusLabel.setColor(0.1f, 0.1f, 0.1f, 1.0f);
            statusLabel.moveTo(sceneBox.getMinX(), sceneBox.getMinY() + statusLabelScale * (fontStatus.getMetrics().getLineGap() - fontStatus.getMetrics().getDescent()), 0f);
            scene.addShape(statusLabel);
        }

        //
        // Setup the moving glyphs
        //

        final List<Label> glyphs = new ArrayList<Label>();
        final List<Label> addedGlyphs = new ArrayList<Label>();
        final List<Vec3f> glyphsTarget = new ArrayList<Vec3f>();
        final List<Vec3f> glyphsPos = new ArrayList<Vec3f>();

        final float pixPerMM, dpiV;
        {
            final float[] tmp = window.getPixelsPerMM(new float[2]);
            pixPerMM = tmp[0]; // [px]/[mm]
            final float[] sDPI = MonitorDevice.perMMToPerInch( tmp );
            dpiV = sDPI[1];
        }

        boolean z_only = true;
        int txt_idx = 0;

        do {
            z_only = !z_only;
            window.invoke(true, (drawable) -> {
                scene.removeShapes(drawable.getGL().getGL2ES2(), addedGlyphs);
                addedGlyphs.clear();
                return true;
            });

            final float[] movingGlyphPixPerShapeUnit;
            {
                final Random random = new Random();

                final Label destText = new Label(renderModes, font, fontScale, "");
                destText.setColor(0.1f, 0.1f, 0.1f, 1);

                destText.setText(hasGLP, "X");
                final PMVMatrix pmv = new PMVMatrix();
                final int[] movingGlyphSizePx = destText.getSurfaceSize(scene, pmv, new int[2]); // [px]
                movingGlyphPixPerShapeUnit = destText.getPixelPerShapeUnit(movingGlyphSizePx, new float[2]); // [px]/[shapeUnit]
                destText.setText("");

                for (int idx = 0; idx < originalTexts[txt_idx].length(); ++idx) {
                    final String movingChar = originalTexts[txt_idx].substring(idx, idx + 1);
                    destText.validate(hasGLP);
                    final Label movingGlyph = new Label(renderModes, font, fontScale, movingChar);
                    movingGlyph.setColor(0.1f, 0.1f, 0.1f, 1);
                    movingGlyph.validate(hasGLP);
                    final float end_pos_x = sceneBox.getMinX() + (destText.getText().isEmpty() ? 0 : destText.getBounds().getWidth());
                    final float end_pos_y = 0f; // movingGlyph.getBounds().getCenter()[1];
                    final float end_pos_z = 0f;
                    final float start_pos_x = z_only ? end_pos_x :
                                                       sceneBox.getMinX() + random.nextFloat() * sceneBox.getWidth();
                    final float start_pos_y = z_only ? end_pos_y :
                                                       sceneBox.getMinY() + random.nextFloat() * sceneBox.getHeight();
                    final float start_pos_z = 0f + random.nextFloat() * sceneBox.getHeight() * 1f;
                    glyphsTarget.add( new Vec3f(end_pos_x, end_pos_y, end_pos_z) );
                    glyphsPos.add( new Vec3f(start_pos_x, start_pos_y, start_pos_z) );
                    movingGlyph.moveTo(start_pos_x, start_pos_y, start_pos_z);
                    glyphs.add(movingGlyph);

                    destText.setText( destText.getText() + movingGlyph.getText() );
                }
                // just add destText to scene to be cleaned up, invisible
                destText.setEnabled(false);
                scene.addShape(destText);
            }
            scene.addShapes(glyphs);
            addedGlyphs.addAll(glyphs);

            final long t0_us = Clock.currentNanos() / 1000; // [us]
            final long[] t2_us = { t0_us };
            while (!glyphs.isEmpty()) {
                window.invoke(true, (drawable) -> {
                    final long t3_us = Clock.currentNanos() / 1000;
                    final float dt_s = (t3_us - t2_us[0]) / 1e6f;
                    t2_us[0] = t3_us;

                    final float velocity_px = velocity * 1e3f * pixPerMM; // [px]/[s]
                    final float velovity_obj = velocity_px / movingGlyphPixPerShapeUnit[0]; // [shapeUnit]/[s]
                    final float dxy = velovity_obj * dt_s; // [shapeUnit]

                    for (int idx = glyphs.size() - 1; 0 <= idx; --idx) {
                        final Label glyph = glyphs.get(idx);
                        final Vec3f pos = glyphsPos.get(idx);
                        final Vec3f target = glyphsTarget.get(idx);
                        final Vec3f p_t = target.minus(pos);
                        if ( p_t.length() <= glyph.getBounds().getSize() / 5f &&
                             Math.abs( glyph.getRotation().getY() ) <= 0.4f )
                        {
                            // arrived
                            glyph.moveTo(target.x(), target.y(), 0f);
                            glyph.getRotation().setIdentity();
                            glyphs.remove(idx);
                            glyphsPos.remove(idx);
                            glyphsTarget.remove(idx);
                            continue;
                        }
                        p_t.normalize();
                        pos.add(p_t.scale(dxy));
                        glyph.moveTo(pos.x(), pos.y(), pos.z());
                        final Quaternion rot = glyph.getRotation();
                        rot.rotateByAngleY(rot_step);
                    }
                    final String text = String.format("%s, v %.1f mm/s, r %.3f",
                            scene.getStatusText(drawable, renderModes, 0, dpiV), velocity * 1e3f, rot_step);
                    statusLabel.setText(text);
                    return true;
                });
            }
            final float has_dur_s = ((Clock.currentNanos() / 1000) - t0_us) / 1e6f; // [us]
            System.err.printf("Text travel-duration %.3f s, %d chars, %.3f s/char%n", has_dur_s, originalTexts[txt_idx].length(), has_dur_s / originalTexts[txt_idx].length());
            try { Thread.sleep(2000); } catch (final InterruptedException e1) { }
            if( autoSpeed > 0 ) {
                if( velocity < 60/1000f ) {
                    setVelocity(velocity + 9/1000f);
                } else {
                    setVelocity(velocity - 9/1000f);
                    autoSpeed = -1;
                }
            } else  if( autoSpeed < 0 ) {
                if( velocity > 11/1000f ) {
                    setVelocity(velocity - 9/1000f);
                } else {
                    setVelocity(velocity + 9/1000f);
                    autoSpeed = 1;
                }
            }
            txt_idx = ( txt_idx + 1 ) % originalTexts.length;
        } while (keepRunning && window.isNativeValid());
        if (!keepRunning) {
            window.destroy();
        }
    }

    /**
     * Our PMVMatrixSetup:
     * - gluPerspective like Scene's default
     * - no normal scale to 1, keep distance to near plane for rotation effects.
     */
    public static class MyPMVMatrixSetup implements PMVMatrixSetup {
        @Override
        public void set(final PMVMatrix pmv, final int x, final int y, final int width, final int height) {
            final float ratio = (float) width / (float) height;
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glLoadIdentity();
            pmv.gluPerspective(Scene.DEFAULT_ANGLE, ratio, Scene.DEFAULT_ZNEAR, Scene.DEFAULT_ZFAR);
            pmv.glTranslatef(0f, 0f, Scene.DEFAULT_SCENE_DIST);

            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();
        }

        @Override
        public void setPlaneBox(final AABBox planeBox, final PMVMatrix pmv, final int x, final int y, final int width, final int height) {
            Scene.getDefaultPMVMatrixSetup().setPlaneBox(planeBox, pmv, x, y, width, height);
        }
    };
}
