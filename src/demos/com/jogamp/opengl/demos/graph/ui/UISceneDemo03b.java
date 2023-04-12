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
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.Scene.PMVMatrixSetup;
import com.jogamp.graph.ui.shapes.GlyphShape;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.graph.FontSetDemos;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.Recti;
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
 * - Pass '-keep' to main-function to keep running.
 * - Pass '-aspeed' to vary velocity
 * </p>
 */
public class UISceneDemo03b {
    static final boolean DEBUG = false;

    static final String[] originalTexts = {
            " JOGL, Java™ Binding for the OpenGL® API ",
            " GraphUI, Resolution Independent Curves ",
            " JogAmp, Java™ libraries for 3D & Media "
    };

    static GraphUIDemoArgs options = new GraphUIDemoArgs(1280, 720, Region.VBAA_RENDERING_BIT);
    static float frame_velocity = 5f / 1e3f; // [m]/[s]
    static float velocity = 30 / 1e3f; // [m]/[s]
    static float rot_step = velocity * 1;

    static void setVelocity(final float v) {
        velocity = v; // Math.max(1/1e3f, v);
        rot_step = velocity * 1;
    }

    public static void main(final String[] args) throws IOException {
        int autoSpeed = 0;

        if (0 != args.length) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if (args[idx[0]].equals("-v")) {
                    ++idx[0];
                    setVelocity(MiscUtils.atoi(args[idx[0]], (int) velocity * 1000) / 1000f);
                } else if(args[idx[0]].equals("-aspeed")) {
                    autoSpeed = -1;
                    setVelocity(80/1000f);
                    options.keepRunning = true;
                }
            }
        }
        // renderModes |= Region.COLORCHANNEL_RENDERING_BIT;
        System.err.println(options);

        final GLProfile reqGLP = GLProfile.get(options.glProfileName);
        System.err.println("GLProfile: "+reqGLP);

        //
        // Resolution independent, no screen size
        //
        final Font font = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerif.ttf",FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        // final Font font = FontFactory.get(IOUtil.getResource("jogamp/graph/font/fonts/ubuntu/Ubuntu-R.ttf",FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        System.err.println("Font: " + font.getFullFamilyName());
        final Font fontStatus = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeMono.ttf", FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);

        final Scene scene = new Scene();
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f }, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setPMVMatrixSetup(new MyPMVMatrixSetup());
        scene.setDebugBox(options.debugBoxThickness);

        final Group glyphGroup = new Group();
        scene.addShape(glyphGroup);

        // scene.setFrustumCullingEnabled(true);
        glyphGroup.setFrustumCullingEnabled(true);

        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(1 * 60, null); // System.err);

        final GLCapabilities caps = new GLCapabilities(reqGLP);
        caps.setAlphaBits(4);
        if( options.sceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(options.sceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        final GLWindow window = GLWindow.create(caps);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(UISceneDemo03b.class.getSimpleName() + ": " + window.getSurfaceWidth() + " x " + window.getSurfaceHeight());
        window.setVisible(true);
        window.addGLEventListener(scene);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UISceneDemo03b.class.getSimpleName() + ": " + window.getSurfaceWidth() + " x " + window.getSurfaceHeight());
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

        window.invoke(true, (drawable) -> {
            final GL gl = drawable.getGL();
            gl.glEnable(GL.GL_DEPTH_TEST);
            return true;
        });

        final GLProfile hasGLP = window.getChosenGLCapabilities().getGLProfile();
        final AABBox sceneBox = scene.getBounds();
        System.err.println("SceneBox " + sceneBox);
        System.err.println("Frustum " + scene.getMatrix().getFrustum());
        System.err.println("GlyphGroup.0: "+glyphGroup);

        final Label statusLabel;
        {
            final AABBox fbox = fontStatus.getGlyphBounds("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            final float statusLabelScale = sceneBox.getWidth() / fbox.getWidth();
            System.err.println("StatusLabelScale: " + statusLabelScale + " = " + sceneBox.getWidth() + " / " + fbox.getWidth() + ", " + fbox);

            statusLabel = new Label(options.renderModes, fontStatus, "Nothing there yet");
            statusLabel.setScale(statusLabelScale, statusLabelScale, 1f);
            statusLabel.setColor(0.1f, 0.1f, 0.1f, 1.0f);
            statusLabel.moveTo(sceneBox.getMinX(), sceneBox.getMinY() + statusLabelScale * (fontStatus.getMetrics().getLineGap() - fontStatus.getMetrics().getDescent()), 0f);
            scene.addShape(statusLabel);
        }

        {
            final StringBuilder sb = new StringBuilder();
            for(final String s : originalTexts) {
                sb.append(s).append("\n");
            }
            final Label l = new Label(options.renderModes, font, sb.toString()); // originalTexts[0]);
            l.validate(hasGLP);
            final float scale = sceneBox.getWidth() / l.getBounds().getWidth();
            l.setScale(scale, scale, 1f);
            l.setColor(0.1f, 0.1f, 0.1f, 1.0f);
            l.moveTo(sceneBox.getMinX(), 0f, 0f);
            scene.addShape(l);

            if( options.wait_to_start ) {
                statusLabel.setText("Press enter to continue");
                MiscUtils.waitForKey("Start");
            }

            window.invoke(true, (drawable) -> {
                final GL2ES2 gl = drawable.getGL().getGL2ES2();
                scene.screenshot(gl, options.renderModes, UISceneDemo03b.class.getSimpleName());
                scene.removeShape(gl, l);
                return true;
            });
        }

        //
        // Setup the moving glyphs
        //

        final List<GlyphShape> glyphShapes = new ArrayList<GlyphShape>();

        final float pixPerMM, dpiV;
        {
            final float[] tmp = window.getPixelsPerMM(new float[2]);
            pixPerMM = tmp[0]; // [px]/[mm]
            final float[] sDPI = MonitorDevice.perMMToPerInch( tmp );
            dpiV = sDPI[1];
        }
        final float s_w = sceneBox.getWidth();
        final float s_h = sceneBox.getHeight();

        boolean z_only = true;
        int txt_idx = 0;

        final AABBox glyphBox = glyphGroup.getBounds().resize(sceneBox);
        final float g_w = glyphBox.getWidth();
        final float g_h = glyphBox.getHeight();
        glyphGroup.scale(0.8f, 0.8f, 1f);
        glyphGroup.validate(hasGLP);
        System.err.println("GlyphBox " + glyphBox);

        glyphGroup.addMouseListener( new Shape.MouseGestureAdapter() {
            @Override
            public void mouseWheelMoved(final MouseEvent e) {
                final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                final Shape shape = shapeEvent.shape;
                final Quaternion rot = shape.getRotation();
                final Vec3f euler = rot.toEuler(new Vec3f());
                final Vec3f eulerOld = euler.copy();
                if( !e.isShiftDown() ) {
                    final float eps = FloatUtil.adegToRad(5f);
                    float diff = e.getRotation()[1] < 0f ? FloatUtil.adegToRad(-1f) : FloatUtil.adegToRad(1f);
                    final float sign = diff >= 0f ? 1f : -1f;
                    final float v;
                    if( e.isAltDown() ) {
                        shape.scale(1f+sign/10f, 1f+sign/10f, 1f);
                        System.err.println("Scaled: "+shape);
                        return;
                    } else if( e.isControlDown() ) {
                        v = euler.x();
                    } else {
                        v = euler.y();
                    }
                    final float av = Math.abs(v);
                    if( 1f*FloatUtil.HALF_PI - eps <= av && av <= 1f*FloatUtil.HALF_PI + eps ||
                        3f*FloatUtil.HALF_PI - eps <= av && av <= 3f*FloatUtil.HALF_PI + eps) {
                        diff = 2f * eps * sign;
                    }
                    if( e.isAltDown() ) {
                        euler.add(0, 0, diff);
                    } else if( e.isControlDown() ) {
                        euler.add(diff, 0, 0);
                    } else {
                        euler.add(0, diff, 0);
                    }
                    System.err.println("Rot: diff "+diff+" (eps "+eps+"): "+eulerOld+" -> "+euler);
                    rot.setFromEuler(euler);
                }
            }
        });
        glyphGroup.onToggle((final Shape shape) -> {
                System.err.println("Toggle: "+shape);
            });
        glyphGroup.setInteractive(true);
        glyphGroup.setDraggable(false);
        glyphGroup.setResizable(false);
        glyphGroup.setToggleable(true);
        glyphGroup.setToggle(true);
        System.err.println("GlyphGroup.1: "+glyphGroup);

        window.addGLEventListener(new GLEventListener() {
            float dir = 1f;
            @Override
            public void init(final GLAutoDrawable drawable) {}
            @Override
            public void dispose(final GLAutoDrawable drawable) {}
            @Override
            public void display(final GLAutoDrawable drawable) {
                if( glyphGroup.isToggleOn() ) {
                    final Quaternion rot = glyphGroup.getRotation();
                    final Vec3f euler = rot.toEuler(new Vec3f());
                    if( FloatUtil.HALF_PI <= euler.y() ) {
                        dir = -1f;
                    } else if( euler.y() <= -FloatUtil.HALF_PI ) {
                        dir = 1f;
                    }
                    glyphGroup.getRotation().rotateByAngleY( frame_velocity * dir );
                }
            }
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {}
        });

        do {
            final float fontScale;
            {
                final AABBox fbox = font.getGlyphBounds(originalTexts[txt_idx]);
                // fontScale = s_w / fbox.getWidth();
                // System.err.println("FontScale: " + fontScale + " = " + s_w + " / " + fbox.getWidth());
                fontScale = g_w / fbox.getWidth();
                System.err.println("FontScale: " + fontScale + " = " + g_w + " / " + fbox.getWidth());
            }
            z_only = !z_only;
            window.invoke(true, (drawable) -> {
                glyphGroup.removeAllShapes(drawable.getGL().getGL2ES2(), scene.getRenderer());
                return true;
            });
            glyphGroup.addShape( new Rectangle(options.renderModes, g_w, g_h, g_w*0.01f) );
            glyphGroup.getShapes().get(0).setInteractive(false);

            final float[] movingGlyphPixPerShapeUnit;
            {
                final Random random = new Random();

                final GlyphShape testGlyph = new GlyphShape(options.renderModes, font, 'X', 0, 0);
                testGlyph.setScale(fontScale, fontScale, 1f);
                testGlyph.validate(hasGLP);
                final PMVMatrix pmv = new PMVMatrix();
                final int[] movingGlyphSizePx = testGlyph.getSurfaceSize(scene, pmv, new int[2]); // [px]
                movingGlyphPixPerShapeUnit = testGlyph.getPixelPerShapeUnit(movingGlyphSizePx, new float[2]); // [px]/[shapeUnit]

                final AABBox box = GlyphShape.processString(glyphShapes, options.renderModes, font, originalTexts[txt_idx]);
                System.err.println("Shapes: "+box);
                for(final GlyphShape gs : glyphShapes) {
                    gs.setScale(fontScale, fontScale, 1f);
                    gs.setColor(0.1f, 0.1f, 0.1f, 1);
                    final Vec3f target = gs.getOrigPos(fontScale).add(glyphBox.getMinX(), 0f, 0f);

                    final float start_pos_x = z_only ? target.x() :
                                                       glyphBox.getMinX() + random.nextFloat() * glyphBox.getWidth();
                    final float start_pos_y = z_only ? target.y() :
                                                       glyphBox.getMinY() + random.nextFloat() * glyphBox.getHeight();
                    final float start_pos_z = 0f + random.nextFloat() * glyphBox.getHeight() * 1f;
                    gs.moveTo(start_pos_x, start_pos_y, start_pos_z);
                }
                // just add destText to scene to be cleaned up, invisible
                testGlyph.setEnabled(false);
                glyphGroup.addShape(testGlyph);
            }
            glyphGroup.addShapes(glyphShapes);

            final float pos_eps = FloatUtil.EPSILON * 5000; // ~= 0.0005960
            final float rot_eps = FloatUtil.adegToRad(0.5f); // 1 adeg ~= 0.01745 rad

            final long t0_us = Clock.currentNanos() / 1000; // [us]
            final long[] t2_us = { t0_us };
            while (!glyphShapes.isEmpty()) {
                window.invoke(true, (drawable) -> {
                    final long t3_us = Clock.currentNanos() / 1000;
                    final float dt_s = (t3_us - t2_us[0]) / 1e6f;
                    t2_us[0] = t3_us;

                    final float velocity_px = velocity * 1e3f * pixPerMM; // [px]/[s]
                    final float velocity_obj = velocity_px / movingGlyphPixPerShapeUnit[0]; // [shapeUnit]/[s]
                    final float dxy = velocity_obj * dt_s; // [shapeUnit]

                    for (int idx = glyphShapes.size() - 1; 0 <= idx; --idx) {
                        final GlyphShape glyph = glyphShapes.get(idx);
                        final Vec3f pos = new Vec3f(glyph.getPosition());
                        final Vec3f target = glyph.getOrigPos(fontScale).add(glyphBox.getMinX(), 0f, 0f);
                        final Vec3f p_t = target.minus(pos);
                        final float p_t_diff = p_t.length();
                        final Quaternion q = glyph.getRotation();
                        final Vec3f euler = q.toEuler(new Vec3f());
                        final float radY = euler.y();
                        final float radYdiff = Math.min(Math.abs(radY), FloatUtil.TWO_PI - Math.abs(radY));
                        final boolean pos_ok = p_t_diff <= pos_eps;
                        final boolean pos_near = p_t_diff <= glyph.getBounds().getSize() * fontScale * 2f;
                        final boolean rot_ok = pos_near && ( radYdiff <= rot_eps || radYdiff <= rot_step * 2f );
                        if ( pos_ok && rot_ok ) {
                            // arrived
                            if( DEBUG ) {
                                if( 0 == idx ) {
                                    System.err.println("F: rot: "+radY+" ("+FloatUtil.radToADeg(radY)+"), diff "+radYdiff+" ("+FloatUtil.radToADeg(radYdiff)+"), step "+rot_step+" ("+FloatUtil.radToADeg(rot_step)+")");
                                }
                            }
                            glyph.moveTo(target.x(), target.y(), target.z());
                            q.setIdentity();
                            glyphShapes.remove(idx);
                            continue;
                        }
                        if( !pos_ok ) {
                            if( DEBUG ) {
                                if( 0 == idx ) {
                                    System.err.println("p_t_diff: "+p_t_diff+", dxy "+dxy);
                                }
                            }
                            if( p_t_diff <= dxy || p_t_diff <= pos_eps ) {
                                glyph.moveTo(target.x(), target.y(), target.z());
                            } else {
                                p_t.normalize();
                                pos.add( p_t.scale( dxy ) );
                                glyph.moveTo(pos.x(), pos.y(), pos.z());
                            }
                            if( !rot_ok ) {
                                if( pos_near ) {
                                    q.rotateByAngleY( rot_step * 2f );
                                } else {
                                    q.rotateByAngleY( rot_step );
                                }
                            }
                        } else {
                            if( DEBUG ) {
                                if( 0 == idx ) {
                                    System.err.println("P: rot: "+radY+" ("+FloatUtil.radToADeg(radY)+"), diff "+radYdiff+" ("+FloatUtil.radToADeg(radYdiff)+"), step "+rot_step+" ("+FloatUtil.radToADeg(rot_step)+")");
                                }
                            }
                            if( radYdiff <= rot_step * 3f || radYdiff <= rot_eps ) {
                                q.setIdentity();
                            } else {
                                q.rotateByAngleY( rot_step * 3f );
                            }
                        }
                    }

                    final String text = String.format("%s, v %.1f mm/s, r %.3f",
                            scene.getStatusText(drawable, options.renderModes, 0, dpiV), velocity * 1e3f, rot_step);
                    statusLabel.setText(text);
                    return true;
                });
            }
            final float has_dur_s = ((Clock.currentNanos() / 1000) - t0_us) / 1e6f; // [us]
            System.err.printf("Text travel-duration %.3f s, %d chars%n", has_dur_s, originalTexts[txt_idx].length());
            if( scene.getScreenshotCount() < 1 + originalTexts.length ) {
                window.invoke(true, (drawable) -> {
                    scene.screenshot(drawable.getGL(), options.renderModes, UISceneDemo03b.class.getSimpleName());
                    return true;
                });
            }
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
        } while (options.keepRunning && window.isNativeValid());
        if (!options.stayOpen) {
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
        public void set(final PMVMatrix pmv, final Recti viewport) {
            final float ratio = (float) viewport.width() / (float) viewport.height();
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glLoadIdentity();
            pmv.gluPerspective(Scene.DEFAULT_ANGLE, ratio, Scene.DEFAULT_ZNEAR, Scene.DEFAULT_ZFAR);
            pmv.glTranslatef(0f, 0f, Scene.DEFAULT_SCENE_DIST);

            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();
        }

        @Override
        public void setPlaneBox(final AABBox planeBox, final PMVMatrix pmv, final Recti viewport) {
            Scene.getDefaultPMVMatrixSetup().setPlaneBox(planeBox, pmv, viewport);
        }
    };
}
