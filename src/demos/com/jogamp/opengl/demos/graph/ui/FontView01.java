/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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

import java.io.File;
import java.io.IOException;

import com.jogamp.common.util.IOUtil;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.GlyphShape;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.graph.FontSetDemos;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;

/**
 * This may become a little font viewer application, having FontForge as its role model.
 */
public class FontView01 {
    static GraphUIDemoArgs options = new GraphUIDemoArgs(1280, 720, Region.VBAA_RENDERING_BIT);

    public static void main(final String[] args) throws IOException {
        String fontfilename = null;
        int gridWidth = 10;
        int gridHeight = 10;
        final int startGlyphID = 0;
        boolean showUnderline = false;

        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if(args[idx[0]].equals("-font")) {
                    idx[0]++;
                    fontfilename = args[idx[0]];
                } else if(args[idx[0]].equals("-gridWidth")) {
                    idx[0]++;
                    gridWidth = MiscUtils.atoi(args[idx[0]], gridWidth);
                } else if(args[idx[0]].equals("-gridHeight")) {
                    idx[0]++;
                    gridHeight = MiscUtils.atoi(args[idx[0]], gridHeight);
                } else if(args[idx[0]].equals("-showUnderline")) {
                    showUnderline = true;
                }
            }
        }
        System.err.println(options);

        Font font;
        if( null == fontfilename ) {
            font = FontFactory.get(IOUtil.getResource("fonts/freefont/FreeSerif.ttf",
                                   FontSetDemos.class.getClassLoader(), FontSetDemos.class).getInputStream(), true);
        } else {
            font = FontFactory.get( new File( fontfilename ) );
        }
        System.err.println("Font "+font.getFullFamilyName());
        System.err.println("Glyph Grid "+gridWidth+" x "+gridHeight);

        final GLProfile reqGLP = GLProfile.get(options.glProfileName);
        System.err.println("GLProfile: "+reqGLP);

        final float gridSize = gridWidth > gridHeight ? 1f/gridWidth : 1f/gridHeight;
        final Group grid = new Group(new GridLayout(gridHeight, gridSize, gridSize, new Padding(gridSize*0.1f, gridSize*0.1f)));
        final int cellCount = gridWidth * gridHeight;
        for(int i=0; i<cellCount; ++i) {
            final GlyphShape g = new GlyphShape(options.renderModes, (char)0, font.getGlyph(startGlyphID + i), 0, 0);
            g.setColor(0.1f, 0.1f, 0.1f, 1);
            g.setDragAndResizeable(false);
            g.onClicked( new Shape.Listener() {
                @Override
                public void run(final Shape shape) {
                    System.err.println( ((GlyphShape)shape).getGlyph().toString() );
                }
            });

            // grid.addShape( g ); // GridLayout handles bottom-left offset and scale
            // Group each GlyphShape with its bounding box Rectangle
            final Group c = new Group();
            c.addShape(new Rectangle(options.renderModes, 1f, 1f, 0.02f).setInteractive(false));
            final AABBox gbox = g.getBounds(reqGLP);
            if( showUnderline && gbox.getMinY() < 0f ) {
                final Shape underline = new Rectangle(options.renderModes, 1f, gbox.getHeight(), 0.02f).setInteractive(false);
                underline.move( 0f, ( 1f - gbox.getHeight() ) / 2f, 0f ); // vert-center
                underline.move( 0f, gbox.getMinY() * -1f, 0f );           // remove glyph underline
                underline.setColor(0f, 0f, 1f, 1f);
                c.addShape(underline);
            }
            g.move( ( 1f - gbox.getWidth() ) / 2f, ( 1f - gbox.getHeight() ) / 2f, 0f ); // center
            g.move( gbox.getLow().mul(-1f) ); // remove bottom-left delta, here glyph underline
            c.addShape(g);
            grid.addShape(c);
        }

        final Scene scene = new Scene();
        scene.setClearParams(new float[] { 1f, 1f, 1f, 1f}, GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        scene.setFrustumCullingEnabled(true);

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
        window.setTitle(FontView01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.setVisible(true);
        window.addGLEventListener(scene);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(FontView01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
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

        grid.validate(reqGLP); // pre-validate to move & scale before display
        final AABBox sceneBox = scene.getBounds();
        System.err.println("SceneBox "+sceneBox);
        final float sxy = sceneBox.getWidth() < sceneBox.getHeight() ? sceneBox.getWidth() : sceneBox.getHeight();
        final AABBox gridBox = grid.getBounds();
        final float gxy = gridBox.getWidth() > gridBox.getHeight() ? gridBox.getWidth() : gridBox.getHeight();
        final float sgxy = sxy / gxy;
        grid.scale(sgxy, sgxy, 1f);
        grid.moveTo(sceneBox.getMinX(), sceneBox.getMinY(), 0f);
        scene.addShape(grid); // late add at correct position and size
        System.err.println("Grid "+grid);
        System.err.println("Grid "+grid.getLayout());
        System.err.println("Grid[0] "+grid.getShapes().get(0));
        scene.screenshot(true, options.renderModes, FontView01.class.getSimpleName());
        // stay open ..
    }
}
