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
import com.jogamp.graph.ui.layout.Alignment;
import com.jogamp.graph.ui.layout.BoxLayout;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.AffineTransform;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.graph.ui.util.Tooltips;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.util.Animator;

/**
 * Res independent {@link Shape}s with offset (not starting at origin)
 * in a {@link Group} using {@link BoxLayout} and {@link GridLayout}, contained within a Scene attached to GLWindow.
 * <p>
 * Pass '-keep' to main-function to keep running after animation,
 * then user can test Shape drag-move and drag-resize w/ 1-pointer.
 * </p>
 */
public class UILayoutBoxGridOffset01 {
    static CommandlineOptions options = new CommandlineOptions(1920, 1080, Region.VBAA_RENDERING_BIT);

    static boolean reLayout = true;
    static final int reLayoutSleep = 500;

    private static final Vec4f groupBorderColor = new Vec4f(0, 0, 1f, 0.6f);
    private static final float borderThickness = 0.01f;

    public static void main(final String[] args) throws IOException {
        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if (args[idx[0]].equals("-no_relayout")) {
                    reLayout = false;
                }
            }
        }
        System.err.println(options);

        final GLCapabilities reqCaps = options.getGLCaps();
        System.out.println("Requested: " + reqCaps);

        final Animator animator = new Animator(0 /* w/o AWT */);

        final GLWindow window = GLWindow.create(reqCaps);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(UILayoutBoxGridOffset01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UILayoutBoxGridOffset01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
            }
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                animator.stop();
            }
        });


        final int zBits = 16;
        final Scene scene = new Scene(options.graphAASamples);
        scene.setPMVMatrixSetup(new Scene.DefaultPMVMatrixSetup(-1f));
        System.err.println("Z16-Precision: default               "+Scene.DEFAULT_Z16_EPSILON);
        System.err.println("Z16-Precision: zDist -1f, zNear 0.1f "+FloatUtil.getZBufferEpsilon(zBits, -1f, 0.1f));
        System.err.println("Z16-Precision: current               "+scene.getZEpsilon(zBits));
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setFrustumCullingEnabled(true);
        scene.attachInputListenerTo(window);
        window.addGLEventListener(scene);
        window.setVisible(true);
        scene.waitUntilDisplayed();

        animator.setUpdateFPSFrames(1*60, null); // System.err);
        animator.add(window);
        animator.start();

        //
        // Resolution independent, no screen size
        //
        final Font font = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_SERIF);
        System.err.println("Font: "+font.getFullFamilyName());

        final AABBox sceneBox = scene.getBounds();
        final float zEps = scene.getZEpsilon(zBits); // Z Epsilon, i.e. minimum recognized delta (resolution)
        System.err.println("SceneBox "+sceneBox+", zEps "+zEps);

        final float cellGapX = sceneBox.getWidth()  / 6f;
        final float cellGapY = sceneBox.getHeight() * 0.40f;
        // final float sxy = 1/10f * sceneBox.getWidth();
        final float sxy = 1/12f * sceneBox.getWidth();
        // final float sxy = 1/4f * sceneBox.getHeight();
        final Vec3f nextPos = new Vec3f();

        {
            final float minX = 0.00f, minY = 0.00f, width = 0.5f, height = 0.5f, lineWidth = 0.05f;
            if( true ) {
                final Group g = setupGroup(new Group(new BoxLayout(1f, 1f, Alignment.None)),
                    reqCaps.getGLProfile(), scene, zEps,
                    sxy, nextPos, cellGapX,
                    font, 11,
                    (final Group gp) -> {
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                    }, minX, minY );
                nextPos.setX( nextPos.x() + cellGapX );
            }
        }
        {
            final float minX = 0.125f, minY = 0.125f, width = 0.5f, height = 0.5f, lineWidth = 0.05f;
            if( true ) {
                final Group g = setupGroup(new Group(new BoxLayout(1f, 1f, Alignment.None)),
                    reqCaps.getGLProfile(), scene, zEps,
                    sxy, nextPos, cellGapX,
                    font, 12,
                    (final Group gp) -> {
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                    }, minX, minY );
                nextPos.setX( nextPos.x() + cellGapX );
            }
            if( true ) {
                final Group g = setupGroup(new Group(new BoxLayout(1f, 1f, Alignment.Fill)),
                    reqCaps.getGLProfile(), scene, zEps,
                    sxy, nextPos, cellGapX,
                    font, 13,
                    (final Group gp) -> {
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                    }, minX, minY );
                nextPos.setX( nextPos.x() + cellGapX );
            }
            if( true ) {
                final Group g = setupGroup(new Group(new BoxLayout(1f, 1f, Alignment.Center)),
                    reqCaps.getGLProfile(), scene, zEps,
                    sxy, nextPos, cellGapX,
                    font, 14,
                    (final Group gp) -> {
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                    }, minX, minY );
                nextPos.setX( nextPos.x() + cellGapX );
            }
            if( true ) {
                final Group g = setupGroup(new Group(new BoxLayout(1f, 1f, Alignment.FillCenter)),
                    reqCaps.getGLProfile(), scene, zEps,
                    sxy, nextPos, cellGapX,
                    font, 15,
                    (final Group gp) -> {
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                    }, minX, minY );
                nextPos.setX( nextPos.x() + cellGapX );
            }
        }


        //
        //
        // next line
        nextPos.set(0, nextPos.y() + cellGapY / 2f, 0 );

        {
            final float minX = 0.000f, minY = 0.000f, width = 0.5f, height = 0.5f, lineWidth = 0.05f;
            if( true ) {
                final Group g = setupGroup(new Group(new GridLayout(2, 1f, 1f, Alignment.None)),
                    reqCaps.getGLProfile(), scene, zEps,
                    sxy, nextPos, cellGapX,
                    font, 21,
                    (final Group gp) -> {
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                    }, minX, minY );
                nextPos.setX( nextPos.x() + cellGapX );
            }
        }
        {
            final float minX = 0.125f, minY = 0.125f, width = 0.5f, height = 0.5f, lineWidth = 0.05f;
            if( true ) {
                final Group g = setupGroup(new Group(new GridLayout(2, 1f, 1f, Alignment.None)),
                    reqCaps.getGLProfile(), scene, zEps,
                    sxy, nextPos, cellGapX,
                    font, 22,
                    (final Group gp) -> {
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                    }, minX, minY );
                nextPos.setX( nextPos.x() + cellGapX );
            }
            if( true ) {
                final Group g = setupGroup(new Group(new GridLayout(2, 1f, 1f, Alignment.Fill)),
                    reqCaps.getGLProfile(), scene, zEps,
                    sxy, nextPos, cellGapX,
                    font, 23,
                    (final Group gp) -> {
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                    }, minX, minY );
                nextPos.setX( nextPos.x() + cellGapX );
            }
            if( true ) {
                final Group g = setupGroup(new Group(new GridLayout(2, 1f, 1f, Alignment.Center)),
                    reqCaps.getGLProfile(), scene, zEps,
                    sxy, nextPos, cellGapX,
                    font, 24,
                    (final Group gp) -> {
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                    }, minX, minY );
                nextPos.setX( nextPos.x() + cellGapX );
            }
            if( true ) {
                final Group g = setupGroup(new Group(new GridLayout(2, 1f, 1f, Alignment.FillCenter)),
                    reqCaps.getGLProfile(), scene, zEps,
                    sxy, nextPos, cellGapX,
                    font, 25,
                    (final Group gp) -> {
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                        gp.addShape( new Rectangle(options.renderModes, minX, minY, width, height, lineWidth, 0).setDragAndResizeable(false) );
                    }, minX, minY );
                nextPos.setX( nextPos.x() + cellGapX );
            }
        }

        // next line
        nextPos.set(0, nextPos.y() + cellGapY, 0 );

        // ...

        {
            final AABBox sceneDim = scene.getBounds();
            final String text = " Press group description to magnify! ";
            final AABBox textDim = font.getGlyphBounds(text, new AffineTransform(), new AffineTransform());
            final float l_sxy = 1/4f * sceneDim.getWidth() / textDim.getWidth();

            final Shape label = new Label(options.renderModes, font, text).setColor(0, 0, 0, 1).setInteractive(false)
                                    .scale(l_sxy, l_sxy, 1).moveTo(sceneDim.getLow())
                                    .move(sceneDim.getWidth() - textDim.getWidth()*l_sxy, sceneDim.getHeight() - textDim.getHeight()*l_sxy, 0);
            scene.addShape(label);
        }

        try { Thread.sleep(1000); } catch (final InterruptedException e1) { }
        scene.screenshot(true, scene.nextScreenshotFile(null, UILayoutBoxGridOffset01.class.getSimpleName(), options.renderModes, reqCaps, null));
        if( !options.stayOpen ) {
            window.destroy();
        }
    }

    static interface GroupMod {
        void mod(Group group);
    }
    static Group setupGroup(final Group g, final GLProfile reqGLP, final Scene scene, final float zEps,
                            final float sxy, final Vec3f nextPos, final float cellGap,
                            final Font font, final int id,
                            final GroupMod modImpl, final float offX, final float offY) {
        final String suffix = String.format("%2d", id);
        g.setID(id);
        final AABBox sceneBox = scene.getBounds();
        modImpl.mod(g);
        g.setBorder(borderThickness).setBorderColor(groupBorderColor);
        g.scale(sxy, sxy, 1);
        g.setInteractive(true);
        g.validate(reqGLP);
        g.moveTo(sceneBox.getLow()).move(nextPos);
        System.err.println("Group-"+suffix+" "+g);
        System.err.println("Group-"+suffix+" Layout "+g.getLayout());
        g.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(g);
        {
            final float X_width = font.getGlyph( ' ' ).getAdvanceWidth();
            /**
             * ID 23: G 23, size[total 2.1 x 1.7, cell 1.0 x 0.5]
             * Padding[t 0.05, r 0.05, b 0.05, l 0.05]
             * Gap[r 0.1, c 0.1], Align [CenterHoriz, CenterVert, Fill]
             */
            final String fixed_text = "Gap[r 0.1, c 0.1], Align [CenterHoriz, CenterVert, Fi";
            final float l_sxy = g.getScaledWidth() / font.getGlyphBounds(fixed_text, new AffineTransform(), new AffineTransform()).getWidth();

            final String text;
            final Group.Layout l = g.getLayout();
            if( l instanceof GridLayout ) {
                final GridLayout gl = (GridLayout)l;
                text = String.format("Grid %2d, off %.3f/%.3f, size[total %.1f x %.1f, cell %.1f x %.1f]%n%s%n%s, Align %s",
                                     id, offX, offY, g.getBounds().getWidth(), g.getBounds().getHeight(), gl.getCellSize().x(), gl.getCellSize().y(),
                                     ( null == gl.getPadding() || gl.getPadding().zeroSumSize() ) ? "Padding none" : gl.getPadding().toString(),
                                     gl.getGap().zeroSumSize() ? "Gap none" : gl.getGap().toString(),
                                     gl.getAlignment() );
            } else if( l instanceof BoxLayout ){
                final BoxLayout bl = (BoxLayout)l;
                text = String.format("Box %2d, off %.3f/%.3f, size[total %.1f x %.1f, cell %.1f x %.1f]%n%s%n%s, Align %s",
                                     id, offX, offY, g.getBounds().getWidth(), g.getBounds().getHeight(), bl.getCellSize().x(), bl.getCellSize().y(),
                                     ( null == bl.getPadding() || bl.getPadding().zeroSumSize() ) ? "Padding none" : bl.getPadding().toString(),
                                     bl.getMargin().zeroSumSize() ? "Margin none" : bl.getMargin().toString(),
                                     bl.getAlignment() );
            } else {
                text = String.format("Layout %2d, off %.3f/%.3f, size[total %.1f x %.1f", id, offX, offY, g.getBounds().getWidth(), g.getBounds().getHeight());
            }
            final Shape label = new Label(options.renderModes, font, text).setColor(0, 0, 0, 1).validate(reqGLP);
            label.scale(l_sxy, l_sxy, 1).moveTo(sceneBox.getLow()).move(nextPos).move(l_sxy*X_width, g.getScaledHeight(), 0)
                .addMouseListener(new Tooltips.ZoomLabelOnClickListener(scene, options.renderModes, 1/6f)).setDragAndResizeable(false);
            scene.addShape(label);
        }
        if( reLayout ) {
            try { Thread.sleep(reLayoutSleep); } catch (final InterruptedException e1) { }
            g.markShapeDirty();
            g.validate(reqGLP);
            System.err.println("Group-"+suffix+".2 "+g);
            System.err.println("Group-"+suffix+" Layout.2 "+g.getLayout());
            g.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        }
        return g;
    }

}
