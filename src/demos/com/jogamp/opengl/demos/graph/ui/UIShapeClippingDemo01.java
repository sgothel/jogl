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
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Vec2f;
import com.jogamp.math.Vec3f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.Cube;
import com.jogamp.math.geom.Frustum;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.util.Animator;

/**
 * Basic UIShape Clipping demo using a Scene and Shape within a clipping Group
 */
public class UIShapeClippingDemo01 {
    static CommandlineOptions options = new CommandlineOptions(1280, 720, Region.VBAA_RENDERING_BIT);

    public static void main(final String[] args) throws IOException {
        boolean _useFixedSize = true;
        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if(args[idx[0]].equals("-NoFixedSize")) {
                    _useFixedSize = false;
                }
            }
        }
        final boolean useFixedSize = _useFixedSize;
        System.err.println(options);
        System.err.println("useFixedSize "+useFixedSize);
        final GLProfile reqGLP = GLProfile.get(options.glProfileName);
        System.err.println("GLProfile: "+reqGLP);

        // Resolution independent, no screen size
        //
        final Scene scene = new Scene(options.graphAASamples);
        scene.setPMVMatrixSetup(new Scene.DefaultPMVMatrixSetup(-1f)); // better distance for perspective action
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setAAQuality(options.graphAAQuality);
        final Animator animator = new Animator(0 /* w/o AWT */);

        final GLCapabilities reqCaps = options.getGLCaps();
        System.out.println("Requested: " + reqCaps);

        final GLWindow window = GLWindow.create(reqCaps);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(UIShapeClippingDemo01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.setVisible(true);
        System.out.println("Chosen: " + window.getChosenGLCapabilities());
        window.addGLEventListener(scene);
        window.addGLEventListener(new GLEventListener() {
            GraphShape shape = null;
            Group contentBox = null;

            @Override
            public void init(final GLAutoDrawable drawable) {
                final AABBox sbox = scene.getBounds();
                System.err.println("Init Scene "+sbox);
                // shape = new Button(options.renderModes, font, "Hello JogAmp", sbox.getWidth()/8f, sbox.getWidth()/16f);
                shape = new Rectangle(options.renderModes, sbox.getWidth()/8f, sbox.getWidth()/16f, 0);

                contentBox = new Group();
                contentBox.setBorder(0.005f);
                contentBox.setInteractive(true);
                contentBox.setClipOnBounds(true);
                contentBox.addShape(shape);
                {
                    final float w = sbox.getWidth()*0.6f;
                    final float h = sbox.getHeight()*0.6f;
                    if( useFixedSize ) {
                        contentBox.setFixedSize(new Vec2f(w, h));
                    }
                    contentBox.move(-w/2f, -h/2f, 0);
                    System.err.println("XXX contentBox "+contentBox.getBounds(drawable.getGLProfile()));
                    System.err.println("XXX shape "+shape.getBounds());
                }

                contentBox.addMouseListener( new Shape.MouseGestureAdapter() {
                    @Override
                    public void mouseWheelMoved(final MouseEvent e) {
                        final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                        final Vec3f rot = new Vec3f(e.getRotation()).scale( FloatUtil.PI / 180.0f );
                        // swap axis for onscreen rotation matching natural feel
                        final float tmp = rot.x(); rot.setX( rot.y() ); rot.setY( tmp );
                        shapeEvent.shape.setRotation( shapeEvent.shape.getRotation().rotateByEuler( rot.scale( 2f ) ) );
                    }
                });
                scene.addShape(contentBox);
            }

            @Override
            public void dispose(final GLAutoDrawable drawable) { }
            @Override
            public void display(final GLAutoDrawable drawable) {
                final RegionRenderer renderer = scene.getRenderer();
                final PMVMatrix4f pmv = renderer.getMatrix();

                pmv.pushMv();
                contentBox.applyMatToMv(pmv);
                {
                    final AABBox box = contentBox.getBounds();
                    final Cube cube = tempC00.set(box);
                    final Frustum frustumCbMv = tempC01.set(cube).transform(pmv.getMv()).updateFrustumPlanes(new Frustum());

                    pmv.pushMv();
                    shape.applyMatToMv(pmv);
                    {
                        final AABBox shapeBox = shape.getBounds();
                        final Cube shapedMv = tempC10.set(shapeBox).transform(pmv.getMv());

                        final boolean isOutMv = frustumCbMv.isOutside( shapedMv );

                        final Frustum frustumPMv = pmv.getPMv().updateFrustumPlanes(new Frustum());
                        final boolean isOutPMv = frustumPMv.isOutside( shapeBox );

                        System.err.println("ClipBox  "+box);
                        System.err.println("ShapeBox "+shapeBox);
                        System.err.println("FrusPMv  "+isOutPMv+", "+frustumPMv);
                        System.err.println("FsCbMv   1 "+isOutMv+", "+frustumCbMv);
                    }
                    pmv.popMv();
                }
                pmv.popMv();
            }
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
            private final Cube tempC00 = new Cube(); // OK, synchronized
            private final Cube tempC01 = new Cube(); // OK, synchronized
            private final Cube tempC10 = new Cube(); // OK, synchronized
        });

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UIShapeClippingDemo01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
            }
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                animator.stop();
            }
        });

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
    }
}
