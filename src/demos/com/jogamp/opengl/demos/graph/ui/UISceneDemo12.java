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
import com.jogamp.graph.ui.layout.BoxLayout;
import com.jogamp.graph.ui.layout.GridLayout;
import com.jogamp.graph.ui.layout.Margin;
import com.jogamp.graph.ui.layout.Padding;
import com.jogamp.graph.ui.shapes.Button;
import com.jogamp.graph.ui.shapes.Label;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.graph.ui.shapes.BaseButton;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.graph.ui.util.GraphUIDemoArgs;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;

/**
 * Res independent {@link Shape}s in a {@link Group} using a {@link GridLayout}, contained within a Scene attached to GLWindow.
 * <p>
 * Pass '-keep' to main-function to keep running after animation,
 * then user can test Shape drag-move and drag-resize w/ 1-pointer.
 * </p>
 */
public class UISceneDemo12 {
    static GraphUIDemoArgs options = new GraphUIDemoArgs(1280, 720, Region.VBAA_RENDERING_BIT);

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
        window.setTitle(UISceneDemo12.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UISceneDemo12.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
            }
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                animator.stop();
            }
        });


        final Scene scene = new Scene();
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
        System.err.println("SceneBox "+sceneBox);

        final float sxy = 1/8f;
        float nextPos = 0;

        final Group groupA0 = new Group(new BoxLayout( new Padding(0.15f, 0.15f) ) );
        {
            groupA0.addShape( new BaseButton(options.renderModes, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1) );
            groupA0.addShape( new Button(options.renderModes, font, "stack1", 0.50f, 0.50f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupA0.addShape( new Label(options.renderModes, font, 0.70f/4f, "pajq").setDragAndResizeable(false).setColor(0, 0, 1, 1) );
        }
        groupA0.setInteractive(true);
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
            groupA1.addShape( new BaseButton(options.renderModes, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1) );
            groupA1.addShape( new Button(options.renderModes, font, "stack2", 0.50f, 0.50f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupA1.addShape( new Label(options.renderModes, font, 0.70f/4f, "pajq").setDragAndResizeable(false).setColor(0, 0, 1, 1) );
        }
        groupA1.setInteractive(true);
        groupA1.scale(sxy, sxy, 1);
        groupA1.moveTo(sceneBox.getLow()).move(nextPos, 0, 0);
        groupA1.validate(reqGLP);
        System.err.println("Group-A1 "+groupA1);
        System.err.println("Group-A1 Layout "+groupA1.getLayout());
        groupA1.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA1);
        scene.addShape( new Rectangle(options.renderModes, 1f, 1f, 0.01f).scale(sxy, sxy, 1).moveTo(sceneBox.getLow()).move(nextPos, 0, 0).setInteractive(false).setColor(0, 0, 0, 1) );
        nextPos += groupA1.getScaledWidth() * 1.5f;

        final Group groupA2 = new Group(new BoxLayout( 1f, 1f, new Margin(0.10f, Margin.CENTER), new Padding(0.05f, 0) ) );
        {
            groupA2.addShape( new BaseButton(options.renderModes, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1) );
            groupA2.addShape( new Button(options.renderModes, font, "stack3", 0.50f, 0.50f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupA2.addShape( new Label(options.renderModes, font, 0.70f/4f, "pajq").setDragAndResizeable(false).setColor(0, 0, 1, 1) );
        }
        groupA2.setInteractive(true);
        groupA2.scale(sxy, sxy, 1);
        groupA2.moveTo(sceneBox.getLow()).move(nextPos, 0, 0);
        groupA2.validate(reqGLP);
        System.err.println("Group-A2 "+groupA2);
        System.err.println("Group-A2 Layout "+groupA2.getLayout());
        groupA2.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA2);
        scene.addShape( new Rectangle(options.renderModes, 1f, 1f, 0.01f).scale(sxy, sxy, 1).moveTo(sceneBox.getLow()).move(nextPos, 0, 0).setInteractive(false).setColor(0, 0, 0, 1) );
        nextPos += groupA2.getScaledWidth() * 1.5f;

        final Group groupA3 = new Group(new BoxLayout( 1f, 1f, new Margin(Margin.CENTER) ) );
        {
            groupA3.addShape( new BaseButton(options.renderModes, 0.70f, 0.70f).setCorner(0f).setInteractive(false).setColor(0, 1, 0, 1) );
            groupA3.addShape( new Button(options.renderModes, font, "stack4", 0.50f, 0.50f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupA3.addShape( new Label(options.renderModes, font, 0.70f/4f, "pajq").setDragAndResizeable(false).setColor(0, 0, 1, 1) );
        }
        groupA3.setInteractive(true);
        groupA3.scale(sxy, sxy, 1);
        groupA3.moveTo(sceneBox.getLow()).move(nextPos, 0, 0);
        groupA3.validate(reqGLP);
        System.err.println("Group-A1 "+groupA3);
        System.err.println("Group-A1 Layout "+groupA3.getLayout());
        groupA3.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupA3);
        scene.addShape( new Rectangle(options.renderModes, 1f, 1f, 0.01f).scale(sxy, sxy, 1).moveTo(sceneBox.getLow()).move(nextPos, 0, 0).setInteractive(false).setColor(0, 0, 0, 1) );
        nextPos += groupA3.getScaledWidth() * 1.5f;

        //
        //
        //
        nextPos = 0;

        final Group groupB0 = new Group(new GridLayout(2, 1f, 1/2f, new Padding(0.05f, 0.05f)));
        {
            groupB0.addShape( new Button(options.renderModes, font, "r1 c1", 1f, 1f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupB0.addShape( new Button(options.renderModes, font, "r1 c2", 1f, 1f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupB0.addShape( new Button(options.renderModes, font, "r2 c1", 1f, 1f/2f).setCorner(0f).setDragAndResizeable(false) );
            groupB0.addShape( new Button(options.renderModes, font, "r2 c2", 1f, 1f/2f).setCorner(0f).setDragAndResizeable(false) );
        }
        groupB0.setInteractive(true);
        groupB0.scale(sxy, sxy, 1);
        groupB0.moveTo(sceneBox.getLow()).move(nextPos, sceneBox.getHeight()/2f, 0);
        groupB0.validate(reqGLP);
        System.err.println("Group-B0 "+groupB0);
        System.err.println("Group-B0 Layout "+groupB0.getLayout());
        groupB0.forAll( (shape) -> { System.err.println("Shape... "+shape); return false; });
        scene.addShape(groupB0);
        nextPos = groupB0.getScaledWidth() * 1.5f;

        try { Thread.sleep(1000); } catch (final InterruptedException e1) { }
        if( !options.stayOpen ) {
            window.destroy();
        }
    }
}
