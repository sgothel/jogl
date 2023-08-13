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

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.Scene.PMVMatrixSetup;
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.Gap;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.graph.ui.util.GraphUIDemoArgs;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Recti;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.math.Vec4f;
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
public class UILayoutGrid01 {
    static GraphUIDemoArgs options = new GraphUIDemoArgs(1280, 720, Region.VBAA_RENDERING_BIT);

    /**
     * Our PMVMatrixSetup:
     * - gluPerspective like Scene's default
     * - no normal scale to 1, keep a longer distance to near plane for rotation effects. We scale Shapes
     */
    public static class MyPMVMatrixSetup implements PMVMatrixSetup {
        static float Z_DIST = -1f;
        @Override
        public void set(final PMVMatrix pmv, final Recti viewport) {
            final float ratio = (float)viewport.width()/(float)viewport.height();
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glLoadIdentity();
            pmv.gluPerspective(Scene.DEFAULT_ANGLE, ratio, Scene.DEFAULT_ZNEAR, Scene.DEFAULT_ZFAR);
            pmv.glTranslatef(0f, 0f, Z_DIST); // Scene.DEFAULT_SCENE_DIST);

            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();
        }

        @Override
        public void setPlaneBox(final AABBox planeBox, final PMVMatrix pmv, final Recti viewport) {
            final float orthoDist = -Z_DIST; // Scene.DEFAULT_SCENE_DIST;
            final Vec3f obj00Coord = new Vec3f();
            final Vec3f obj11Coord = new Vec3f();

            Scene.winToPlaneCoord(pmv, viewport, Scene.DEFAULT_ZNEAR, Scene.DEFAULT_ZFAR, viewport.x(), viewport.y(), orthoDist, obj00Coord);
            Scene.winToPlaneCoord(pmv, viewport, Scene.DEFAULT_ZNEAR, Scene.DEFAULT_ZFAR, viewport.width(), viewport.height(), orthoDist, obj11Coord);

            planeBox.setSize( obj00Coord, obj11Coord );
        }
    };

    static final boolean reLayout = false;
    static final int reLayoutSleep = 500;

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
        window.setTitle(UILayoutGrid01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UILayoutGrid01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
            }
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                animator.stop();
            }
        });


        final Scene scene = new Scene();
        scene.setPMVMatrixSetup(new MyPMVMatrixSetup());
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setFrustumCullingEnabled(true);
        scene.attachInputListenerTo(window);
        window.addGLEventListener(scene);
        window.setVisible(true);
        scene.waitUntilDisplayed();

        animator.setUpdateFPSFrames(1*60, null); // System.err);
        animator.add(window);
        animator.start();

        /**
         * We can share this instance w/ all UI elements,
         * since only mouse action / gesture is complete for a single one (press, drag, released and click).
         */
        final Shape.MouseGestureAdapter dragZoomRotateListener = new Shape.MouseGestureAdapter() {
            @Override
            public void mouseWheelMoved(final MouseEvent e) {
                final Shape.EventInfo shapeEvent = (Shape.EventInfo) e.getAttachment();
                final Shape shape = shapeEvent.shape;
                final Vec3f rot = new Vec3f(e.getRotation()).scale( FloatUtil.PI / 180.0f );
                // swap axis for onscreen rotation matching natural feel
                final float tmp = rot.x(); rot.setX( rot.y() ); rot.setY( tmp );
                shape.getRotation().rotateByEuler( rot.scale( 2f ) );
            }
        };

        //
        // Resolution independent, no screen size
        //
        final Font font = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_SERIF);
        System.err.println("Font: "+font.getFullFamilyName());

        final AABBox sceneBox = scene.getBounds();
        System.err.println("SceneBox "+sceneBox);

        final Vec4f borderColor = new Vec4f(0, 0, 1f, 0.6f);
        final float borderThickness = 0.01f;
        final float sxy = 1/10f * sceneBox.getWidth();
        float nextXPos = 0, nextYTop = sceneBox.getHeight();

        final Group groupA0 = new Group(new GridLayout(1, 1f, 1/2f, Alignment.Fill, new Gap(0.10f)));
        groupA0.addShape( new Button(options.renderModes, font, "ro co", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).addMouseListener(dragZoomRotateListener) );
        groupA0.setBorder(borderThickness).setBorderColor(borderColor);
        groupA0.scale(sxy, sxy, 1);
        groupA0.setInteractive(true);
        groupA0.validate(reqGLP);
        groupA0.moveTo(sceneBox.getLow()).move(nextXPos, nextYTop-groupA0.getScaledHeight(), 0);
        System.err.println("Group-A0 "+groupA0);
        System.err.println("Group-A0 Layout "+groupA0.getLayout());
        groupA0.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA0);
        nextXPos += groupA0.getScaledWidth() * 1.1f;
        if( reLayout ) {
            try { Thread.sleep(reLayoutSleep); } catch (final InterruptedException e1) { }
            groupA0.markShapeDirty();
            groupA0.validate(reqGLP);
            System.err.println("Group-A0.2 "+groupA0);
            System.err.println("Group-A0 Layout.2 "+groupA0.getLayout());
            groupA0.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        }

        final Group groupA1 = new Group(new GridLayout(1, 1, 1/2f, Alignment.Fill, new Gap(0.10f)));
        groupA1.addShape( new Button(options.renderModes, font, "ro co", 1f, 1/2f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
        groupA1.addShape( new Button(options.renderModes, font, "r1 r1", 1f, 1/2f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
        groupA1.setBorder(sxy*borderThickness).setBorderColor(borderColor);
        groupA1.scale(sxy, sxy, 1);
        groupA1.setInteractive(true);
        groupA1.validate(reqGLP);
        groupA1.moveTo(sceneBox.getLow()).move(nextXPos, nextYTop-groupA1.getScaledHeight(), 0);
        System.err.println("Group-A1 "+groupA1);
        System.err.println("Group-A1 Layout "+groupA1.getLayout());
        groupA1.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA1);
        nextXPos += groupA1.getScaledWidth() * 1.1f;
        if( reLayout ) {
            try { Thread.sleep(reLayoutSleep); } catch (final InterruptedException e1) { }
            groupA1.markShapeDirty();
            groupA1.validate(reqGLP);
            System.err.println("Group-A1.2 "+groupA1);
            System.err.println("Group-A1 Layout.2 "+groupA1.getLayout());
            groupA1.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        }

        final Group groupA2 = new Group(new GridLayout(1, 1/2f, Alignment.Fill, new Gap(0.10f), 1));
        groupA2.addShape( new Button(options.renderModes, font, "ro co", 1f, 1/2f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
        groupA2.addShape( new Button(options.renderModes, font, "r1 c1", 1f, 1/2f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
        groupA2.setBorder(sxy*borderThickness).setBorderColor(borderColor);
        groupA2.scale(sxy, sxy, 1);
        groupA2.setInteractive(true);
        groupA2.validate(reqGLP);
        groupA2.moveTo(sceneBox.getLow()).move(nextXPos, nextYTop-groupA2.getScaledHeight(), 0);
        System.err.println("Group-A2 "+groupA2);
        System.err.println("Group-A2 Layout "+groupA2.getLayout());
        groupA2.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA2);
        nextXPos += groupA2.getScaledWidth() * 1.1f;
        if( reLayout ) {
            try { Thread.sleep(reLayoutSleep); } catch (final InterruptedException e1) { }
            groupA2.markShapeDirty();
            groupA2.validate(reqGLP);
            System.err.println("Group-A2.2 "+groupA2);
            System.err.println("Group-A2 Layout.2 "+groupA2.getLayout());
            groupA2.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        }

        final Group groupA3 = new Group(new GridLayout(2, 1f, 1/2f, Alignment.Fill, new Gap(0.10f)));
        {
            groupA3.addShape( new Button(options.renderModes, font, "ro co", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).addMouseListener(dragZoomRotateListener) );
            groupA3.addShape( new Button(options.renderModes, font, "r1 c2", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            groupA3.addShape( new Button(options.renderModes, font, "r2 c1", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            groupA3.addShape( new Button(options.renderModes, font, "r2 c2", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).addMouseListener(dragZoomRotateListener) );
        }
        groupA3.setBorder(borderThickness).setBorderColor(borderColor);
        groupA3.scale(sxy, sxy, 1);
        groupA3.setInteractive(true);
        groupA3.validate(reqGLP);
        groupA3.moveTo(sceneBox.getLow()).move(nextXPos, nextYTop-groupA3.getScaledHeight(), 0);
        System.err.println("Group-A3 "+groupA3);
        System.err.println("Group-A3 Layout "+groupA3.getLayout());
        groupA3.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA3);
        nextXPos += groupA3.getScaledWidth() * 1.1f;
        if( reLayout ) {
            try { Thread.sleep(reLayoutSleep); } catch (final InterruptedException e1) { }
            groupA3.markShapeDirty();
            groupA3.validate(reqGLP);
            System.err.println("Group-A3.2 "+groupA3);
            System.err.println("Group-A3 Layout.2 "+groupA3.getLayout());
            groupA3.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        }

        final Group groupA4 = new Group(new GridLayout(2, 1f, 1/2f, Alignment.Fill, new Gap(0.10f)));
        {
            groupA4.addShape( new Button(options.renderModes, font, "ro co", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).addMouseListener(dragZoomRotateListener) );
            groupA4.addShape( new Button(options.renderModes, font, "r1 c2", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            groupA4.addShape( new Button(options.renderModes, font, "r2 c1", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            groupA4.addShape( new Button(options.renderModes, font, "r2 c2", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).addMouseListener(dragZoomRotateListener) );
            groupA4.addShape( new Button(options.renderModes, font, "r3 c1", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
        }
        groupA4.setBorder(borderThickness).setBorderColor(borderColor);
        groupA4.scale(sxy, sxy, 1);
        groupA4.setInteractive(true);
        groupA4.validate(reqGLP);
        groupA4.moveTo(sceneBox.getLow()).move(nextXPos, nextYTop-groupA4.getScaledHeight(), 0);
        System.err.println("Group-A4 "+groupA4);
        System.err.println("Group-A4 Layout "+groupA4.getLayout());
        groupA4.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA4);
        nextXPos += groupA4.getScaledWidth() * 1.1f;
        if( reLayout ) {
            try { Thread.sleep(reLayoutSleep); } catch (final InterruptedException e1) { }
            groupA4.markShapeDirty();
            groupA4.validate(reqGLP);
            System.err.println("Group-A4.2 "+groupA4);
            System.err.println("Group-A4 Layout.2 "+groupA4.getLayout());
            groupA4.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        }

        // next line
        nextXPos = 0;
        nextYTop = 2f*sceneBox.getHeight()/3f;

        final Group groupB0 = new Group(new GridLayout(2, sxy, sxy/2f, Alignment.Fill, new Gap(sxy*0.10f)));
        {
            groupB0.addShape( new Button(options.renderModes, font, "ro co", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).addMouseListener(dragZoomRotateListener) );
            groupB0.addShape( new Button(options.renderModes, font, "r1 c2", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            groupB0.addShape( new Button(options.renderModes, font, "r2 c1", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            groupB0.addShape( new Button(options.renderModes, font, "r2 c2", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).addMouseListener(dragZoomRotateListener) );
            groupB0.addShape( new Button(options.renderModes, font, "r3 c1", 1f, 1f/2f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
        }
        groupB0.setBorder(sxy*borderThickness).setBorderColor(borderColor);
        // groupB0.scale(2*sxy, 2*sxy, 1);
        groupB0.setInteractive(true);
        groupB0.validate(reqGLP);
        groupB0.moveTo(sceneBox.getLow()).move(nextXPos, nextYTop-groupB0.getScaledHeight(), 0);
        System.err.println("Group-B0 "+groupB0);
        System.err.println("Group-B0 Layout "+groupB0.getLayout());
        groupB0.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupB0);
        nextXPos += groupB0.getScaledWidth() * 1.1f;
        if( reLayout ) {
            try { Thread.sleep(reLayoutSleep); } catch (final InterruptedException e1) { }
            groupB0.markShapeDirty();
            groupB0.validate(reqGLP);
            System.err.println("Group-B0.2 "+groupB0);
            System.err.println("Group-B0 Layout.2 "+groupB0.getLayout());
            groupB0.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        }

        final Group groupB1 = new Group(new GridLayout(2, sxy, sxy/2f, Alignment.FillCenter, new Gap(sxy*0.10f)));
        {
            final Padding p = new Padding(0.05f);
            groupB1.addShape( new Button(options.renderModes, font, "ro co", 1, 1/2f).setCorner(0f).setPaddding(p).setBorder(borderThickness).addMouseListener(dragZoomRotateListener) );
            groupB1.addShape( new Button(options.renderModes, font, "r1 c2", 1, 1/2f).setCorner(0f).setPaddding(p).setBorder(borderThickness).setDragAndResizeable(false) );
            groupB1.addShape( new Button(options.renderModes, font, "r2 c1", 1, 1/2f).setCorner(0f).setPaddding(p).setBorder(borderThickness).setDragAndResizeable(false) );
            groupB1.addShape( new Button(options.renderModes, font, "r2 c2", 1, 1/2f).setCorner(0f).setPaddding(p).setBorder(borderThickness).addMouseListener(dragZoomRotateListener) );
            groupB1.addShape( new Button(options.renderModes, font, "r3 c1", 1, 1/2f).setCorner(0f).setPaddding(p).setBorder(borderThickness).addMouseListener(dragZoomRotateListener) );
        }
        groupB1.setBorder(sxy*borderThickness).setBorderColor(borderColor);
        // groupB1.scale(2*sxy, 2*sxy, 1);
        groupB1.setInteractive(true);
        groupB1.validate(reqGLP);
        groupB1.moveTo(sceneBox.getLow()).move(nextXPos, nextYTop-groupB1.getScaledHeight(), 0);
        System.err.println("Group-B1 "+groupB1);
        System.err.println("Group-B1 Layout "+groupB1.getLayout());
        groupB1.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupB1);
        nextXPos += groupB1.getScaledWidth() * 1.1f;
        if( reLayout ) {
            try { Thread.sleep(reLayoutSleep); } catch (final InterruptedException e1) { }
            groupB1.markShapeDirty();
            groupB1.validate(reqGLP);
            System.err.println("Group-B1.2 "+groupB1);
            System.err.println("Group-B1 Layout.2 "+groupB1.getLayout());
            groupB1.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        }

        final Group groupB2 = new Group(new GridLayout(2, sxy, sxy/2f, Alignment.FillCenter, new Gap(sxy*0.10f)));
        {
            final Padding p = new Padding(sxy/2f*0.05f);
            groupB2.addShape( new Button(options.renderModes, font, "ro co", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy/2f*borderThickness).addMouseListener(dragZoomRotateListener) );
            groupB2.addShape( new Button(options.renderModes, font, "r1 c2", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy/2f*borderThickness).setDragAndResizeable(false) );
            groupB2.addShape( new Button(options.renderModes, font, "r2 c1", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy/2f*borderThickness).setDragAndResizeable(false) );
            groupB2.addShape( new Button(options.renderModes, font, "r2 c2", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy/2f*borderThickness).addMouseListener(dragZoomRotateListener) );
            groupB2.addShape( new Button(options.renderModes, font, "r3 c1", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy/2f*borderThickness).addMouseListener(dragZoomRotateListener) );
        }
        groupB2.setBorder(sxy*borderThickness).setBorderColor(borderColor);
        // groupB2.scale(2*sxy, 2*sxy, 1);
        groupB2.setInteractive(true);
        groupB2.validate(reqGLP);
        groupB2.moveTo(sceneBox.getLow()).move(nextXPos, nextYTop-groupB2.getScaledHeight(), 0);
        System.err.println("Group-B2 "+groupB2);
        System.err.println("Group-B2 Layout "+groupB2.getLayout());
        groupB2.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupB2);
        nextXPos += groupB2.getScaledWidth() * 1.1f;
        if( reLayout ) {
            try { Thread.sleep(reLayoutSleep); } catch (final InterruptedException e1) { }
            groupB2.markShapeDirty();
            groupB2.validate(reqGLP);
            System.err.println("Group-B2.2 "+groupB2);
            System.err.println("Group-B2 Layout.2 "+groupB2.getLayout());
            groupB2.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        }

        final Group groupB3 = new Group(new GridLayout(2, sxy, sxy/2f, Alignment.Fill, new Gap(sxy*0.10f)));
        {
            final Padding p = new Padding(sxy/2f*0.05f);
            groupB3.addShape( new Button(options.renderModes, font, "ro co", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy/2f*borderThickness).addMouseListener(dragZoomRotateListener) );
            groupB3.addShape( new Button(options.renderModes, font, "r1 c2", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy/2f*borderThickness).setDragAndResizeable(false) );
            groupB3.addShape( new Button(options.renderModes, font, "r2 c1", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy/2f*borderThickness).setDragAndResizeable(false) );
            groupB3.addShape( new Button(options.renderModes, font, "r2 c2", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy/2f*borderThickness).addMouseListener(dragZoomRotateListener) );
            groupB3.addShape( new Button(options.renderModes, font, "r3 c1", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy/2f*borderThickness).addMouseListener(dragZoomRotateListener) );
        }
        groupB3.setBorder(sxy*borderThickness).setBorderColor(borderColor);
        // groupB3.scale(2*sxy, 2*sxy, 1);
        groupB3.setInteractive(true);
        groupB3.validate(reqGLP);
        groupB3.moveTo(sceneBox.getLow()).move(nextXPos, nextYTop-groupB3.getScaledHeight(), 0);
        System.err.println("Group-B3 "+groupB3);
        System.err.println("Group-B3 Layout "+groupB3.getLayout());
        groupB3.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupB3);
        nextXPos += groupB3.getScaledWidth() * 1.1f;
        if( reLayout ) {
            try { Thread.sleep(reLayoutSleep); } catch (final InterruptedException e1) { }
            groupB3.markShapeDirty();
            groupB3.validate(reqGLP);
            System.err.println("Group-B3.2 "+groupB3);
            System.err.println("Group-B3 Layout.2 "+groupB3.getLayout());
            groupB3.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        }

        // next line
        nextXPos = 0;
        nextYTop = sceneBox.getHeight()/3f;

        final Group groupC0 = new Group(new GridLayout(2, 0, 0, Alignment.Fill, new Gap(0.03f)));
        {
            final Group glyphGrid = new Group(new GridLayout(2, 0.3f, 0.3f, Alignment.Fill, new Gap(0.3f * 0.10f)));
            glyphGrid.addShape( new Button(options.renderModes, font, "0.0", 1f, 1f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            glyphGrid.addShape( new Button(options.renderModes, font, "0.1", 1f, 1f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            glyphGrid.addShape( new Button(options.renderModes, font, "1.0", 1f, 1f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            glyphGrid.addShape( new Button(options.renderModes, font, "1.1", 1f, 1f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            groupC0.addShape(glyphGrid.setBorder(borderThickness));

            final Group infoGrid = new Group(new GridLayout(1, 1/4f, 1/2f, Alignment.Fill, new Gap(0.02f)));
            // final Group glyphView = new Group();
            // glyphView.addShape(new Rectangle(options.renderModes, 1f, 1f, 0.005f).setInteractive(false));
            // glyphView.addShape(new Button(options.renderModes, font, "S", 1f, 1f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            // infoGrid.addShape(glyphView.setBorder(borderThickness));
            infoGrid.addShape(new Button(options.renderModes, font, " S ", 1/2f, 1f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );

            // final Group infoView = new Group();
            // infoView.addShape(new Button(options.renderModes, font, "Info", 1f, 1f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            // infoGrid.addShape(infoView.setBorder(borderThickness));
            infoGrid.addShape(new Button(options.renderModes, font, " Info ", 1/2f, 1f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
            groupC0.addShape(infoGrid.setBorder(borderThickness));
            // groupC0.addShape(new Button(options.renderModes, font, "S", 1/4f, 0.5f).setCorner(0f).setBorder(borderThickness).setDragAndResizeable(false) );
        }
        groupC0.setBorder(sxy*borderThickness).setBorderColor(borderColor);
        groupC0.scale(2*sxy, 2*sxy, 1);
        groupC0.setInteractive(true);
        groupC0.validate(reqGLP);
        groupC0.moveTo(sceneBox.getLow()).move(nextXPos, nextYTop-groupC0.getScaledHeight(), 0);
        System.err.println("Group-C0 "+groupC0);
        System.err.println("Group-C0 Layout "+groupC0.getLayout());
        groupC0.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupC0);
        nextXPos += groupC0.getScaledWidth() * 1.1f;
        if( reLayout ) {
            try { Thread.sleep(reLayoutSleep); } catch (final InterruptedException e1) { }
            groupC0.markShapeDirty();
            groupC0.validate(reqGLP);
            System.err.println("Group-C0.2 "+groupC0);
            System.err.println("Group-C0 Layout.2 "+groupC0.getLayout());
            groupC0.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        }

        final Group groupC3 = new Group(new GridLayout(2, sxy, sxy/2f, Alignment.Center, new Gap(sxy*0.10f)));
        {
            final Padding p = new Padding(sxy*0.05f);
            groupC3.addShape( new Button(options.renderModes, font, "ro co", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy*borderThickness).addMouseListener(dragZoomRotateListener) );
            groupC3.addShape( new Button(options.renderModes, font, "r1 c2", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy*borderThickness).setDragAndResizeable(false) );
            groupC3.addShape( new Button(options.renderModes, font, "r2 c1", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy*borderThickness).setDragAndResizeable(false) );
            groupC3.addShape( new Button(options.renderModes, font, "r2 c2", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy*borderThickness).addMouseListener(dragZoomRotateListener) );
            groupC3.addShape( new Button(options.renderModes, font, "r3 c1", sxy/2f, sxy/4f).setCorner(0f).setPaddding(p).setBorder(sxy*borderThickness).addMouseListener(dragZoomRotateListener) );
        }
        groupC3.setBorder(sxy*borderThickness).setBorderColor(borderColor);
        // groupC3.scale(2*sxy, 2*sxy, 1);
        groupC3.setInteractive(true);
        groupC3.validate(reqGLP);
        groupC3.moveTo(sceneBox.getLow()).move(nextXPos, nextYTop-groupC3.getScaledHeight(), 0);
        System.err.println("Group-C3 "+groupC3);
        System.err.println("Group-C3 Layout "+groupC3.getLayout());
        groupC3.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupC3);
        nextXPos += groupC3.getScaledWidth() * 1.1f;
        if( reLayout ) {
            try { Thread.sleep(reLayoutSleep); } catch (final InterruptedException e1) { }
            groupC3.markShapeDirty();
            groupC3.validate(reqGLP);
            System.err.println("Group-C3.2 "+groupC3);
            System.err.println("Group-C3 Layout.2 "+groupC3.getLayout());
            groupC3.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        }

        try { Thread.sleep(1000); } catch (final InterruptedException e1) { }
        scene.screenshot(true, scene.nextScreenshotFile(null, UILayoutGrid01.class.getSimpleName(), options.renderModes, caps, null));
        if( !options.stayOpen ) {
            window.destroy();
        }
    }
}
