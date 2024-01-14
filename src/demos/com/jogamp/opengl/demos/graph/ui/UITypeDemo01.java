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

import java.io.File;
import java.io.IOException;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.Font.Glyph;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.shapes.CrossHair;
import com.jogamp.graph.ui.shapes.Rectangle;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.plane.AffineTransform;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.FPSCounter;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.demos.graph.MSAATool;
import com.jogamp.opengl.demos.graph.ui.testshapes.Glyph03FreeMonoRegular_M;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;

/**
 * Basic UIShape and Type Rendering demo.
 *
 * Action Keys:
 * - 1/2: zoom in/out
 * - 4/5: increase/decrease shape/text spacing
 * - 6/7: increase/decrease corner size
 * - 0/9: rotate
 * - v: toggle v-sync
 * - s: screenshot
 */
public class UITypeDemo01 implements GLEventListener {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    static CommandlineOptions options = new CommandlineOptions(1280, 720, Region.VBAA_RENDERING_BIT);

    public static void main(final String[] args) throws IOException {
        String text = "Hello Origin.";
        Font font = null;
        int glyph_id = Glyph.ID_UNKNOWN;
        final int[] idx = { 0 };
        for (idx[0] = 0; idx[0] < args.length; ++idx[0]) {
            if( options.parse(args, idx) ) {
                continue;
            } else if(args[idx[0]].equals("-font")) {
                idx[0]++;
                font = FontFactory.get(new File(args[idx[0]]));
            } else if(args[idx[0]].equals("-text")) {
                idx[0]++;
                text = args[idx[0]];
            } else if(args[idx[0]].equals("-glyph")) {
                idx[0]++;
                glyph_id = MiscUtils.atoi(args[idx[0]], 0);
            }
        }
        System.err.println(options);
        if( null == font ) {
            font = FontFactory.get(FontFactory.UBUNTU).get(FontSet.FAMILY_LIGHT, FontSet.STYLE_SERIF);
        }
        System.err.println("Font: "+font.getFullFamilyName());
        System.err.println("Text: "+text);

        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        System.out.println("Requested: " + caps);

        final GLWindow window = GLWindow.create(caps);
        // window.setPosition(10, 10);
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(UITypeDemo01.class.getSimpleName()+": "+window.getSurfaceWidth()+" x "+window.getSurfaceHeight());
        final UITypeDemo01 uiGLListener = new UITypeDemo01(font, glyph_id, text, options.renderModes, DEBUG, TRACE);
        uiGLListener.attachInputListenerTo(window);
        window.addGLEventListener(uiGLListener);
        window.setVisible(true);

        final Animator animator = new Animator(0 /* w/o AWT */);
        // animator.setUpdateFPSFrames(60, System.err);
        animator.add(window);

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

        animator.start();
    }

    private final Vec4f fg_color = new Vec4f( 0, 0, 0, 1 );
    private final Font font;
    private final String text;
    private final int glyph_id;
    private final GLReadBufferUtil screenshot;
    private final int renderModes;
    private final RegionRenderer rRenderer;
    private final boolean debug;
    private final boolean trace;

    private final CrossHair crossHair;
    private final Shape testObj;

    private KeyAction keyAction;
    private MouseAction mouseAction;

    private volatile GLAutoDrawable autoDrawable = null;

    private final float[] position = new float[] {0,0,0};

    private static final float xTran = 0f;
    private static final float yTran = 0f;
    private static final float zTran = -1/5f;
    private static final float zNear = 0.1f;
    private static final float zFar = 7000.0f;

    boolean ignoreInput = false;

    protected final AffineTransform tempT1 = new AffineTransform();
    protected final AffineTransform tempT2 = new AffineTransform();

    @SuppressWarnings("unused")
    public UITypeDemo01(final Font font, final int glyph_id, final String text, final int renderModes, final boolean debug, final boolean trace) {
        this.font = font;
        this.text = text;
        this.glyph_id = glyph_id;
        this.renderModes = renderModes;
        this.rRenderer = RegionRenderer.create(RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
        this.debug = debug;
        this.trace = trace;
        this.screenshot = new GLReadBufferUtil(false, false);

        crossHair = new CrossHair(renderModes, 1/20f, 1/20f, 1/1000f);
        crossHair.setColor(0f,0f,1f,1f);
        crossHair.setVisible(true);

        if (false ) {
            final Rectangle o = new Rectangle(renderModes, 1/10f, 1/20f, 1/1000f);
            o.move(o.getWidth(), -o.getHeight(), 0f);
            testObj = o;
        } else {
            final float scale = 0.15312886f;
            final float size_xz = 0.541f;
            final Shape o = new Glyph03FreeMonoRegular_M(renderModes);
            o.scale(scale, scale, 1f);
            // o.translate(size_xz, -size_xz, 0f);
            testObj = o;
        }
        testObj.setColor(0f,  0f,  0f,  1f);
        testObj.setVisible(true);
    }

    public final RegionRenderer getRegionRenderer() { return rRenderer; }
    public final float[] getPosition() { return position; }

    @Override
    public void init(final GLAutoDrawable drawable) {
        autoDrawable = drawable;
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(debug) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, gl, null) ).getGL2ES2();
        }
        if(trace) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err } ) ).getGL2ES2();
        }
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        getRegionRenderer().init(gl);

        gl.setSwapInterval(1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        // gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
        MSAATool.dump(drawable);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int xstart, final int ystart, final int width, final int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glViewport(xstart, ystart, width, height);
        rRenderer.reshapePerspective(FloatUtil.QUARTER_PI, width, height, zNear, zFar);
        // rRenderer.reshapeOrtho(width, height, zNear, zFar);

        lastWidth = width;
        lastHeight = height;
        if( drawable instanceof Window ) {
            ((Window)drawable).setTitle(UITypeDemo01.class.getSimpleName()+": "+drawable.getSurfaceWidth()+" x "+drawable.getSurfaceHeight());
        }
    }
    float lastWidth = 0f, lastHeight = 0f;

    final int[] sampleCount = { 4 };

    private void drawShape(final GL2ES2 gl, final PMVMatrix4f pmv, final RegionRenderer renderer, final Shape shape) {
        pmv.pushMv();
        shape.setTransformMv(pmv);
        shape.draw(gl, renderer, sampleCount);
        pmv.popMv();
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final RegionRenderer renderer = getRegionRenderer();
        final PMVMatrix4f pmv = renderer.getMatrix();
        pmv.loadMvIdentity();
        pmv.translateMv(xTran, yTran, zTran);
        renderer.enable(gl, true);
        {
            pmv.pushMv();
            pmv.scaleMv(0.8f, 0.8f, 1f);
            drawShape(gl, pmv, renderer, testObj);
            pmv.popMv();
        }
        drawShape(gl, pmv, renderer, crossHair);
        {
            final float full_width_o;
            final float full_height_o;
            {
                final float orthoDist = -zTran; // assume orthogonal plane at -zTran
                float glWinX = 0;
                float glWinY = 0;
                final float winZ = FloatUtil.getOrthoWinZ(orthoDist, zNear, zFar);
                final Vec3f objCoord0 = new Vec3f();
                final Vec3f objCoord1 = new Vec3f();
                if( pmv.mapWinToObj(glWinX, glWinY, winZ, renderer.getViewport(), objCoord0) ) {
                    if( once ) {
                        System.err.printf("winToObjCoord: win [%f, %f, %f] -> obj [%s]%n", glWinX, glWinY, winZ, objCoord0);
                    }
                }
                glWinX = drawable.getSurfaceWidth();
                glWinY = drawable.getSurfaceHeight();
                if( pmv.mapWinToObj(glWinX, glWinY, winZ, renderer.getViewport(), objCoord1) ) {
                    if( once ) {
                        System.err.printf("winToObjCoord: win [%f, %f, %f] -> obj [%s]%n", glWinX, glWinY, winZ, objCoord1);
                    }
                }
                full_width_o = objCoord1.x() - objCoord0.x();
                full_height_o = objCoord1.y() - objCoord0.y();
            }
            pmv.pushMv();

            final Font.Glyph glyph;
            if( Glyph.ID_UNKNOWN < glyph_id ) {
                glyph = font.getGlyph(glyph_id);
                if( once ) {
                    System.err.println("glyph_id "+glyph_id+": "+glyph);
                }
            } else {
                glyph = null;
            }
            if( null != glyph && glyph.getID() != Glyph.ID_UNKNOWN ) {
                final AABBox txt_box_em = glyph.getBounds();
                final float full_width_s = full_width_o / txt_box_em.getWidth();
                final float full_height_s = full_height_o / txt_box_em.getHeight();
                final float txt_scale = full_width_s < full_height_s ? full_width_s/2f : full_height_s/2f;
                pmv.scaleMv(txt_scale, txt_scale, 1f);
                pmv.translateMv(-txt_box_em.getWidth(), 0f, 0f);
                if( null != glyph.getShape() ) {
                    final GLRegion region = GLRegion.create(gl.getGLProfile(), renderModes, null, glyph.getShape());
                    region.addOutlineShape(glyph.getShape(), null, fg_color);
                    region.draw(gl, renderer, Region.MAX_AA_QUALITY, sampleCount);
                    region.destroy(gl);
                }
                if( once ) {
                    final AABBox txt_box_em2 = font.getGlyphShapeBounds(null, text);
                    System.err.println("XXX: full_width: "+full_width_o+" / "+txt_box_em.getWidth()+" -> "+full_width_s);
                    System.err.println("XXX: full_height: "+full_height_o+" / "+txt_box_em.getHeight()+" -> "+full_height_s);
                    System.err.println("XXX: txt_scale: "+txt_scale);
                    System.err.println("XXX: txt_box_em "+txt_box_em);
                    System.err.println("XXX: txt_box_e2 "+txt_box_em2);
                }
            } else if( Glyph.ID_UNKNOWN == glyph_id ) {
                final AABBox txt_box_em = font.getGlyphBounds(text, tempT1, tempT2);
                final float full_width_s = full_width_o / txt_box_em.getWidth();
                final float full_height_s = full_height_o / txt_box_em.getHeight();
                final float txt_scale = full_width_s < full_height_s ? full_width_s/2f : full_height_s/2f;
                pmv.scaleMv(txt_scale, txt_scale, 1f);
                pmv.translateMv(-txt_box_em.getWidth(), 0f, 0f);
                final AABBox txt_box_r = TextRegionUtil.drawString3D(gl, renderModes, renderer, font, text, fg_color, sampleCount, tempT1, tempT2);
                if( once ) {
                    final AABBox txt_box_em2 = font.getGlyphShapeBounds(null, text);
                    System.err.println("XXX: full_width: "+full_width_o+" / "+txt_box_em.getWidth()+" -> "+full_width_s);
                    System.err.println("XXX: full_height: "+full_height_o+" / "+txt_box_em.getHeight()+" -> "+full_height_s);
                    System.err.println("XXX: txt_scale: "+txt_scale);
                    System.err.println("XXX: txt_box_em "+txt_box_em);
                    System.err.println("XXX: txt_box_e2 "+txt_box_em2);
                    System.err.println("XXX: txt_box_rg "+txt_box_r);
                }
            }
            pmv.popMv();
            if( once ) {
                try {
                    printScreen(drawable);
                } catch (GLException | IOException e) {
                    e.printStackTrace();
                }
            }
            once = false;
        }
        renderer.enable(gl, false);
    }
    private boolean once = true;

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        crossHair.destroy(gl, getRegionRenderer());
        testObj.destroy(gl, getRegionRenderer());

        autoDrawable = null;
        screenshot.dispose(gl);
        rRenderer.destroy(gl);
    }

    /** Attach the input listener to the window */
    public void attachInputListenerTo(final GLWindow window) {
        if ( null == keyAction ) {
            keyAction = new KeyAction();
            window.addKeyListener(keyAction);
        }
        if ( null == mouseAction ) {
            mouseAction = new MouseAction();
            window.addMouseListener(mouseAction);
        }
    }

    public void detachFrom(final GLWindow window) {
        if ( null == keyAction ) {
            return;
        }
        if ( null == mouseAction ) {
            return;
        }
        window.removeGLEventListener(this);
        window.removeKeyListener(keyAction);
        window.removeMouseListener(mouseAction);
    }

    public void printScreen(final GLAutoDrawable drawable) throws GLException, IOException {
        final String dir = "./";
        final String tech="demo-"+Region.getRenderModeString(renderModes);
        final String objName = "snap"+screenshot_num;
        {
            final String sw = String.format("-%03dx%03d-Z%04d-T%04d-%s", drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), (int)Math.abs(zTran), 0, objName);

            final String filename = dir + tech + sw +".png";
            if(screenshot.readPixels(drawable.getGL(), false)) {
                screenshot.write(new File(filename));
            }
        }
        screenshot_num++;
    }
    int screenshot_num = 0;

    public void setIgnoreInput(final boolean v) {
        ignoreInput = v;
    }
    public boolean getIgnoreInput() {
        return ignoreInput;
    }

    public class MouseAction implements MouseListener{

        @Override
        public void mouseClicked(final MouseEvent e) {

        }

        @Override
        public void mouseEntered(final MouseEvent e) {
        }

        @Override
        public void mouseExited(final MouseEvent e) {
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            autoDrawable.invoke(false, new GLRunnable() { // avoid data-race
                @Override
                public boolean run(final GLAutoDrawable drawable) {
                    System.err.println("\n\nMouse: "+e);

                    final RegionRenderer renderer = getRegionRenderer();
                    final PMVMatrix4f pmv = renderer.getMatrix();
                    pmv.loadMvIdentity();
                    pmv.translateMv(xTran, yTran, zTran);

                    // flip to GL window coordinates, origin bottom-left
                    final Recti viewport = renderer.getViewport(new Recti());
                    final int glWinX = e.getX();
                    final int glWinY = viewport.height() - e.getY() - 1;

                    {
                        pmv.pushMv();
                        crossHair.setTransformMv(pmv);

                        final Vec3f objPosC = crossHair.getBounds().getCenter();
                        System.err.println("\n\nCrossHair: "+crossHair);
                        final int[] objWinPos = crossHair.shapeToWinCoord(pmv, viewport, objPosC, new int[2]);
                        System.err.println("CrossHair: Obj: Obj "+objPosC+" -> Win "+objWinPos[0]+"/"+objWinPos[1]);

                        final Vec3f objPos2 = crossHair.winToShapeCoord(pmv, viewport, objWinPos[0], objWinPos[1], new Vec3f());
                        System.err.println("CrossHair: Obj: Win "+objWinPos[0]+"/"+objWinPos[1]+" -> Obj "+objPos2);

                        final Vec3f winObjPos = crossHair.winToShapeCoord(pmv, viewport, glWinX, glWinY, new Vec3f());
                        if( null != winObjPos ) {
                            // final float[] translate = crossHair.getTranslate();
                            // final float[] objPosT = new float[] { objPosC[0]+translate[0], objPosC[1]+translate[1], objPosC[2]+translate[2] };
                            final Vec3f diff = winObjPos.minus(objPosC);
                            // final float dz = winObjPos[2] - objPosT[2];
                            if( !FloatUtil.isZero(diff.x(), FloatUtil.EPSILON) || !FloatUtil.isZero(diff.y(), FloatUtil.EPSILON) ) {
                                System.err.println("CrossHair: Move.1: Win "+glWinX+"/"+glWinY+" -> Obj "+winObjPos+" -> diff "+diff);
                                crossHair.move(diff.x(), diff.y(), 0f);
                            } else {
                                System.err.println("CrossHair: Move.0: Win "+glWinX+"/"+glWinY+" -> Obj "+winObjPos+" -> diff "+diff);
                            }
                        }

                        final int[] surfaceSize = crossHair.getSurfaceSize(pmv, viewport, new int[2]);
                        System.err.println("CrossHair: Size: Pixel "+surfaceSize[0]+" x "+surfaceSize[1]);

                        pmv.popMv();
                    }
                    return true;
                } } );

        }

        @Override
        public void mouseReleased(final MouseEvent e) {
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            // TODO Auto-generated method stub

        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            // TODO Auto-generated method stub

        }

        @Override
        public void mouseWheelMoved(final MouseEvent e) {
            // TODO Auto-generated method stub

        }

    }

    public class KeyAction implements KeyListener {
        @Override
        public void keyPressed(final KeyEvent arg0) {
            if(ignoreInput) {
                return;
            }

            if(arg0.getKeyCode() == KeyEvent.VK_1){
                crossHair.move(0f, 0f, -zTran/10f);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_2){
                crossHair.move(0f, 0f, zTran/10f);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_UP){
                crossHair.move(0f, crossHair.getHeight()/10f, 0f);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_DOWN){
                crossHair.move(0f, -crossHair.getHeight()/10f, 0f);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_LEFT){
                crossHair.move(-crossHair.getWidth()/10f, 0f, 0f);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_RIGHT){
                crossHair.move(crossHair.getWidth()/10f, 0f, 0f);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_0){
                // rotate(1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_9){
                // rotate(-1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_V) {
                if(null != autoDrawable) {
                    autoDrawable.invoke(false, new GLRunnable() {
                        @Override
                        public boolean run(final GLAutoDrawable drawable) {
                            final GL gl = drawable.getGL();
                            final int _i = gl.getSwapInterval();
                            final int i;
                            switch(_i) {
                                case  0: i =  1; break;
                                case  1: i = -1; break;
                                case -1: i =  0; break;
                                default: i =  1; break;
                            }
                            gl.setSwapInterval(i);

                            final GLAnimatorControl a = drawable.getAnimator();
                            if( null != a ) {
                                a.resetFPSCounter();
                            }
                            if(drawable instanceof FPSCounter) {
                                ((FPSCounter)drawable).resetFPSCounter();
                            }
                            System.err.println("Swap Interval: "+_i+" -> "+i+" -> "+gl.getSwapInterval());
                            return true;
                        }
                    });
                }
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_S){
                if(null != autoDrawable) {
                    autoDrawable.invoke(false, new GLRunnable() {
                        @Override
                        public boolean run(final GLAutoDrawable drawable) {
                            try {
                                printScreen(drawable);
                            } catch (final GLException e) {
                                e.printStackTrace();
                            } catch (final IOException e) {
                                e.printStackTrace();
                            }
                            return true;
                        }
                    });
                }
            }
        }
        @Override
        public void keyReleased(final KeyEvent arg0) {}
    }
}
