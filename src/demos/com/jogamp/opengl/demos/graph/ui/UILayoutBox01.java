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
import com.jogamp.graph.ui.layout.BoxLayout;
import com.jogamp.graph.ui.layout.Margin;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.graph.ui.shapes.BaseButton;
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
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * Res independent {@link Shape}s in a {@link Group} using a {@link BoxLayout}, contained within a Scene attached to GLWindow.
 * <p>
 * Pass '-keep' to main-function to keep running after animation,
 * then user can test Shape drag-move and drag-resize w/ 1-pointer.
 * </p>
 */
public class UILayoutBox01 {
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

        final Animator animator = new Animator();

        final GLCapabilities caps = new GLCapabilities(reqGLP);
        caps.setAlphaBits(4);
        if( options.sceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(options.sceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        final GLWindow window = GLWindow.create(caps);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(UILayoutBox01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UILayoutBox01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
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

        final float sxy = 1/8f * sceneBox.getWidth();
        float nextPos = 0;

        final Group groupA0 = new Group(new BoxLayout( new Padding(0.15f, 0.15f) ) );
        {
            groupA0.addShape( new BaseButton(options.renderModes, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1) );
            groupA0.addShape( new Button(options.renderModes, font, "stack-0", 0.50f, 0.50f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupA0.addShape( new Label(options.renderModes, font, 0.70f/4f, "A0 pajq").setDragAndResizeable(false).setColor(0, 0, 1, 1) );
        }
        groupA0.setInteractive(true);
        groupA0.addMouseListener(dragZoomRotateListener);
        groupA0.scale(sxy, sxy, 1);
        groupA0.moveTo(sceneBox.getLow()).move(nextPos, 0, 0);
        groupA0.validate(reqGLP);
        System.err.println("Group-A0 "+groupA0);
        System.err.println("Group-A0 Layout "+groupA0.getLayout());
        groupA0.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA0);
        scene.addShape( new Rectangle(options.renderModes, 1f, 1f, 0.01f).scale(sxy, sxy, 1).moveTo(sceneBox.getLow()).move(nextPos, 0, 0).setInteractive(false).setColor(0, 0, 0, 1) );
        nextPos = groupA0.getScaledWidth() * 1.5f;

        final Group groupA1 = new Group(new BoxLayout( 1f, 1f, new Margin(0.05f, 0.05f), new Padding(0.10f, 0.10f) ) );
        {
            // groupA1.addShape( new BaseButton(options.renderModes, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1) );
            groupA1.addShape( new BaseButton(options.renderModes | Region.COLORCHANNEL_RENDERING_BIT, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1)
                    .setBorder(0.01f).setPaddding(new Padding(0.0f)) );
            groupA1.addShape( new Button(options.renderModes, font, "stack-1", 0.50f, 0.50f/2f).setCorner(0f).addMouseListener(dragZoomRotateListener) );
            groupA1.addShape( new Label(options.renderModes, font, 0.70f/4f, "A1 pajq").setDragAndResizeable(false).setColor(0, 0, 1, 1) );
        }
        // groupA1.setInteractive(true);
        groupA1.scale(sxy, sxy, 1);
        groupA1.moveTo(sceneBox.getLow()).move(nextPos, 0, 0);
        groupA1.validate(reqGLP);
        System.err.println("Group-A1 "+groupA1);
        System.err.println("Group-A1 Layout "+groupA1.getLayout());
        groupA1.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA1);
        // scene.addShape( new Rectangle(options.renderModes, 1f, 1f, 0.01f).scale(sxy, sxy, 1).moveTo(sceneBox.getLow()).move(nextPos, 0, 0).setInteractive(false).setColor(0, 0, 0, 1) );
        scene.addShape( new Button(options.renderModes, font, "stack-1b", 0.50f, 0.50f/2f).setCorner(0f)
                .scale(sxy, sxy, 1f).moveTo(sceneBox.getLow()).move(nextPos, groupA1.getScaledHeight(), 0)
                .addMouseListener(dragZoomRotateListener) );

        nextPos += groupA1.getScaledWidth() * 1.5f;

        final Group groupA2 = new Group(new BoxLayout( 1f, 1f, new Margin(0.10f, Margin.CENTER), new Padding(0.05f, 0) ) );
        {
            // groupA2.addShape( new BaseButton(options.renderModes, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1) );
            groupA2.addShape( new BaseButton(options.renderModes | Region.COLORCHANNEL_RENDERING_BIT, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1)
                    .setBorder(0.01f).setPaddding(new Padding(0.0f)).setBorderColor(1, 0, 0, 1) );
            groupA2.addShape( new Button(options.renderModes, font, "stack-2", 0.50f, 0.50f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupA2.addShape( new Label(options.renderModes, font, 0.70f/4f, "A2 pajq").setDragAndResizeable(false).setColor(0, 0, 1, 1) );
        }
        groupA2.setInteractive(true);
        groupA2.scale(sxy, sxy, 1);
        groupA2.moveTo(sceneBox.getLow()).move(nextPos, 0, 0);
        groupA2.validate(reqGLP);
        System.err.println("Group-A2 "+groupA2);
        System.err.println("Group-A2 Layout "+groupA2.getLayout());
        groupA2.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA2);
        // scene.addShape( new Rectangle(options.renderModes, 1f, 1f, 0.01f).scale(sxy, sxy, 1).moveTo(sceneBox.getLow()).move(nextPos, 0, 0).setInteractive(false).setColor(0, 0, 0, 1) );
        nextPos += groupA2.getScaledWidth() * 1.5f;

        final Group groupA3 = new Group(new BoxLayout( 1f, 1f, new Margin(0.10f, Margin.CENTER), new Padding(0.05f, 0f) ) );
        {
            // groupA3.addShape( new BaseButton(options.renderModes, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1) );
            groupA3.addShape( new BaseButton(options.renderModes | Region.COLORCHANNEL_RENDERING_BIT, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1)
                    .setBorder(0.01f).setPaddding(new Padding(0.0f)).setBorderColor(0, 0, 1, 1) );
            groupA3.addShape( new Button(options.renderModes, font, "stack-3", 0.50f, 0.50f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupA3.addShape( new Label(options.renderModes, font, 0.70f/4f, "A3 pajq").setDragAndResizeable(false).setColor(0, 0, 1, 1) );
        }
        groupA3.scale(sxy, sxy, 1);
        groupA3.moveTo(sceneBox.getLow()).move(nextPos, 0, 0);
        groupA3.validate(reqGLP);
        System.err.println("Group-A3 "+groupA3);
        System.err.println("Group-A3 Layout "+groupA3.getLayout());
        groupA3.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA3);
        // scene.addShape( new Rectangle(options.renderModes, 1f, 1f, 0.01f).scale(sxy, sxy, 1).moveTo(sceneBox.getLow()).move(nextPos, 0, 0).setInteractive(false).setColor(0, 0, 0, 1) );
        nextPos += groupA3.getScaledWidth() * 1.5f;

        final Group groupA4 = new Group(new BoxLayout( 1f, 1f, new Margin(Margin.CENTER), new Padding(0.0f, 0f) ) );
        {
            // groupA4.addShape( new BaseButton(options.renderModes, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1) );
            groupA4.addShape( new BaseButton(options.renderModes | Region.COLORCHANNEL_RENDERING_BIT, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1)
                    .setBorder(0.01f).setPaddding(new Padding(0.0f)).setBorderColor(0f, 0f, 0f, 1) );
            groupA4.addShape( new Button(options.renderModes, font, "stack-4", 0.50f, 0.50f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupA4.addShape( new Label(options.renderModes, font, 0.70f/4f, "A4 pajq").setDragAndResizeable(false).setColor(0, 0, 1, 1) );
        }
        groupA4.scale(sxy, sxy, 1);
        groupA4.moveTo(sceneBox.getLow()).move(nextPos, 0, 0);
        groupA4.validate(reqGLP);
        System.err.println("Group-A4 "+groupA4);
        System.err.println("Group-A4 Layout "+groupA4.getLayout());
        groupA4.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA4);
        // scene.addShape( new Rectangle(options.renderModes, 1f, 1f, 0.01f).scale(sxy, sxy, 1).moveTo(sceneBox.getLow()).move(nextPos, 0, 0).setInteractive(false).setColor(0, 0, 0, 1) );
        nextPos += groupA4.getScaledWidth() * 1.5f;

        if( reLayout ) {
            try { Thread.sleep(1000); } catch (final InterruptedException e1) { }
            groupA4.markShapeDirty();
            groupA4.validate(reqGLP);
            System.err.println("Group-A4.2 "+groupA4);
            System.err.println("Group-A4 Layout.2 "+groupA4.getLayout());
        }

        //
        //
        //
        nextPos = 0;

        try { Thread.sleep(1000); } catch (final InterruptedException e1) { }
        if( !options.stayOpen ) {
            window.destroy();
        }
    }
}
