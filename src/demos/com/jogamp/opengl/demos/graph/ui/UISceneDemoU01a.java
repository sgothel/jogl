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

import java.io.IOException;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.graph.ui.GraphShape;
import com.jogamp.graph.ui.shapes.CrossHair;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.demos.graph.ui.util.GraphUIDemoArgs;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Matrix4f;
import com.jogamp.opengl.math.Recti;
import com.jogamp.opengl.math.Vec2f;
import com.jogamp.opengl.math.Vec2i;
import com.jogamp.opengl.math.Vec3f;
import com.jogamp.opengl.math.Vec4f;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PMVMatrix;

/**
 * Res independent Graph + GraphUI integration demo
 * using a GraphUI Shape and Graph text rendering
 * within a regular GLEventListener, attached to a GLWindow.
 * <p>
 * This demo showcases how to integrate Graph and GraphUI with different projection variations.
 * </p>
 * <p>
 * Pass '-projPersp' to main-function to use perspective projection, otherwise orthogonal projection is used.
 * </p>
 * <p>
 * Pass '-projWin' to main-function to use orthogonal projection with window coordinates, otherwise [-0.5, 0.5] is being used
 * </p>
 * <p>
 * Default projection is orthogonal with width = 1, world-model range [-0.5, 0.5].
 * </p>
 * <p>
 * The world-model height is always scaled to window aspect ratio.
 * </p>
 * <p>
 * 0/0 origin in its bottom-left corner, same as GraphUI
 * </p>
 * <p>
 * Pass '-x <int>' and '-y <int>' widget position in window coordinates (bottom left origin). Default is center, i.e. half window width and height.<br/>
 * Note: Reshape won't adjust and this is merely to demonstrate the coordinate space.
 * </p>
 */
public class UISceneDemoU01a {
    static final GraphUIDemoArgs options = new GraphUIDemoArgs(1280, 720, Region.VBAA_RENDERING_BIT );
    static final Vec4f text_color = new Vec4f( 0, 1, 0, 1 );
    static Font font;
    static boolean projOrtho = true;
    static boolean projOrthoWin = false;
    static boolean textOnly = false;
    static int pass2TexUnit = GLRegion.DEFAULT_TWO_PASS_TEXTURE_UNIT;
    static final Vec2i winOrigin = new Vec2i(options.surface_width/2, options.surface_height/2);
    static final float normWidgetSize = 1/4f;

    public static void main(final String[] args) throws IOException {
        if( 0 != args.length ) {
            final int[] idx = { 0 };
            for(idx[0]=0; idx[0]<args.length; ++idx[0]) {
                if( options.parse(args, idx) ) {
                    continue;
                } else if(args[idx[0]].equals("-projPersp")) {
                    projOrtho = false;
                    projOrthoWin = false;
                } else if(args[idx[0]].equals("-projWin")) {
                    projOrtho = true;
                    projOrthoWin = true;
                } else if(args[idx[0]].equals("-texUnit")) {
                    idx[0]++;
                    pass2TexUnit = MiscUtils.atoi(args[idx[0]], pass2TexUnit);
                } else if(args[idx[0]].equals("-x")) {
                    idx[0]++;
                    winOrigin.setX( MiscUtils.atoi(args[idx[0]], winOrigin.x()) );
                } else if(args[idx[0]].equals("-y")) {
                    idx[0]++;
                    winOrigin.setY( MiscUtils.atoi(args[idx[0]], winOrigin.y()) );
                } else if(args[idx[0]].equals("-textOnly")) {
                    textOnly = true;
                }
            }
        }
        System.err.println(JoglVersion.getInstance().toString());

        System.err.println(options);
        System.err.println("Ortho Projection "+projOrtho+", Ortho-Win "+projOrthoWin);
        System.err.println("pass2TexUnit "+pass2TexUnit);
        final GLProfile reqGLP = GLProfile.get(options.glProfileName);
        System.err.println("GLProfile: "+reqGLP);

        //
        // Resolution independent, no screen size
        //
        font = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_SERIF);
        System.err.println("Font: "+font.getFullFamilyName());

        final Animator animator = new Animator(0 /* w/o AWT */);

        final GLCapabilities caps = new GLCapabilities(reqGLP);
        caps.setAlphaBits(4);
        System.out.println("Requested: " + caps);

        final MyRenderer renderer = new MyRenderer();

        final GLWindow window = GLWindow.create(caps);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(UISceneDemoU01a.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        window.setVisible(true);
        System.out.println("Chosen: " + window.getChosenGLCapabilities());
        window.addGLEventListener(renderer);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowResized(final WindowEvent e) {
                window.setTitle(UISceneDemoU01a.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
            }
            @Override
            public void windowDestroyNotify(final WindowEvent e) {
                animator.stop();
            }
        });

        animator.setUpdateFPSFrames(1*60, null); // System.err);
        animator.add(window);
        animator.start();

    }

    public static class MyRenderer implements GLEventListener {
        private final float angle;
        private final float zNear;
        private final float zFar;
        private final float sceneDist;

        /** World dimension in world-model (object) coordinates. */
        private final Vec2f worldDim = new Vec2f(1f, 1f);
        /** World origin (bottom left) offset.  */
        private final Vec3f worldOrigin = new Vec3f();
        /** Sample count for Graph-VBAA */
        private final int[] sampleCount = { 4 };
        /** Graph region renderer */
        private final RegionRenderer renderer;
        /** The Graph region for text */
        private GLRegion textRegion;
        /** The GraphUI shape */
        private GraphShape shape;

        public MyRenderer() {
            if( projOrtho ) {
                angle = 0.0f;
                zNear = -1f;
                zFar = 1f;
                sceneDist = zNear;
            } else {
                angle = 45.0f;
                zNear = 0.1f;
                zFar = 7000.0f;
                sceneDist = -zNear;
            }
            renderer = RegionRenderer.create(RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
        }

        @Override
        public void init(final GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();

            if( !textOnly ) {
                shape = new CrossHair(options.renderModes, normWidgetSize, normWidgetSize, normWidgetSize/100f); // normalized: 1 is 100% surface size (width and/or height)
                shape.setTextureUnit(pass2TexUnit);
                shape.setColor(0, 0, 1, 1);
                System.err.println("Init: Shape bounds "+shape.getBounds(drawable.getGLProfile()));
                System.err.println("Init: Shape "+shape);
            }

            renderer.init(gl);

            if( null == textRegion ) {
                textRegion = GLRegion.create(gl.getGLProfile(), options.renderModes, null, pass2TexUnit, 0, 0);
            }
        }

        @Override
        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
            renderer.reshapeNotify(x, y, width, height);
            setMatrix(renderer.getMatrix(), renderer.getViewport());

            // scale shapes from normalized size 1 and to keep aspect ratio
            if( !textOnly ) {
                final float s = Math.min(worldDim.x(), worldDim.y());
                shape.setScale(s, s, 1f);
            }
        }
        private void setMatrix(final PMVMatrix pmv, final Recti viewport) {
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glLoadIdentity();
            final float ratio = (float)viewport.width()/(float)viewport.height();
            if( projOrthoWin ) {
                worldDim.setX( viewport.width() );
                worldDim.setY( worldDim.x() / ratio ); // adjust aspect ratio
                pmv.glOrthof(0, worldDim.x(), 0, worldDim.y(), zNear, zFar);
                // similar: renderer.reshapeOrtho(viewport.width(), viewport.height(), zNear, zFar);
            } else if( projOrtho ) {
                worldDim.setY( worldDim.x() / ratio ); // adjust aspect ratio
                pmv.glOrthof(-worldDim.x()/2f, worldDim.x()/2f, -worldDim.y()/2f, worldDim.y()/2f, zNear, zFar);
            } else {
                pmv.gluPerspective(angle, ratio, zNear, zFar);
                {
                    final Vec3f obj00Coord = new Vec3f();
                    final Vec3f obj11Coord = new Vec3f();

                    winToPlaneCoord(pmv, viewport, zNear, zFar, viewport.x(), viewport.y(), -sceneDist, obj00Coord);
                    winToPlaneCoord(pmv, viewport, zNear, zFar, viewport.width(), viewport.height(), -sceneDist, obj11Coord);

                    final AABBox planeBox = new AABBox();
                    planeBox.setSize( obj00Coord, obj11Coord );
                    worldDim.set(planeBox.getWidth(), planeBox.getHeight());
                }
            }
            pmv.glTranslatef(0f, 0f, sceneDist); // nose to plane

            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();

            winToPlaneCoord(pmv, viewport, zNear, zFar, winOrigin.x(), winOrigin.y(), -sceneDist, worldOrigin);
            {
                final Matrix4f p = pmv.getPMat();
                final Matrix4f mv = pmv.getMvMat();
                System.err.println("Reshape VP: "+viewport);
                System.err.println("Reshape P :"); System.err.println(p.toString());
                System.err.println("Reshape Mv:"); System.err.println(mv.toString());
                System.err.println("World Dim : "+worldDim);
                System.err.println("Window Origin: "+winOrigin);
                System.err.println("World Origin : "+worldOrigin);
            }
            pmv.glTranslatef(worldOrigin.x(), worldOrigin.y(), 0); // move to custom origin
        }
        public static void winToPlaneCoord(final PMVMatrix pmv, final Recti viewport,
                final float zNear, final float zFar,
                final float winX, final float winY, final float objOrthoZ,
                final Vec3f objPos) {
            final float winZ = FloatUtil.getOrthoWinZ(objOrthoZ, zNear, zFar);
            pmv.gluUnProject(winX, winY, winZ, viewport, objPos);
        }

        @Override
        public void display(final GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();

            gl.glClearColor(1f, 1f, 1f, 1f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

            final PMVMatrix pmv = renderer.getMatrix();
            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

            if( onceAtDisplay ) {
                final Matrix4f p = pmv.getPMat();
                final Matrix4f mv = pmv.getMvMat();
                System.err.println("Display.0: P :"); System.err.println(p.toString());
                System.err.println("Display.0: Mv:"); System.err.println(mv.toString());
            }

            renderer.enable(gl, true);
            {
                pmv.glPushMatrix();
                drawText(gl, pmv, "Hello JogAmp Users!");
                pmv.glPopMatrix();
            }
            if( !textOnly ) {
                pmv.glPushMatrix();
                shape.setTransform(pmv);

                shape.draw(gl, renderer, sampleCount);
                if( onceAtDisplay ) {
                    final Matrix4f p = pmv.getPMat();
                    final Matrix4f mv = pmv.getMvMat();
                    System.err.println("Display.1: P :"); System.err.println(p.toString());
                    System.err.println("Display.1: Mv:"); System.err.println(mv.toString());
                    System.err.println("Display.1: Shape bounds "+shape.getBounds(drawable.getGLProfile()));
                    System.err.println("Display.1: Shape "+shape);
                    final Recti shapePort = shape.getSurfacePort(pmv, renderer.getViewport(), new Recti());
                    System.err.println("Display.1: Shape SurfacePort "+shapePort);
                }
                pmv.glPopMatrix();
            }
            renderer.enable(gl, false);
            onceAtDisplay = false;
        }
        private void drawText(final GL2ES2 gl, final PMVMatrix pmv, final String text) {
            final AffineTransform tempT1 = new AffineTransform();
            final AffineTransform tempT2 = new AffineTransform();

            final AABBox txt_box_em = font.getGlyphBounds(text, tempT1, tempT2);
            final float full_width_s = worldDim.x() / txt_box_em.getWidth();
            final float full_height_s = worldDim.y() / txt_box_em.getHeight();
            final float txt_scale = full_width_s < full_height_s ? full_width_s * normWidgetSize : full_height_s * normWidgetSize;
            pmv.glScalef(txt_scale, txt_scale, 1f);
            pmv.glTranslatef(-txt_box_em.getWidth(), 0f, 0f);
            final AABBox txt_box_r = TextRegionUtil.drawString3D(gl, textRegion.clear(gl), renderer, font, text, text_color, sampleCount, tempT1, tempT2);

            if( onceAtDisplay ) {
                System.err.println("XXX: full_width: "+worldDim.x()+" / "+txt_box_em.getWidth()+" -> "+full_width_s);
                System.err.println("XXX: full_height: "+worldDim.y()+" / "+txt_box_em.getHeight()+" -> "+full_height_s);
                System.err.println("XXX: txt_scale: "+txt_scale);
                System.err.println("XXX: txt_box_em "+txt_box_em);
                System.err.println("XXX: txt_box_r  "+txt_box_r);
                final AABBox textPort = txt_box_r.mapToWindow(new AABBox(), pmv.getPMvMat(), renderer.getViewport(), true /* useCenterZ */);
                System.err.println("Display.1: Shape TextPort "+textPort);
            }
        }
        private boolean onceAtDisplay = true;

        @Override
        public void dispose(final GLAutoDrawable drawable) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            textRegion.destroy(gl);
            if( !textOnly ) {
                shape.destroy(gl, renderer);
            }
            renderer.destroy(gl);
            System.err.println("Destroyed");
        }
    }
}
