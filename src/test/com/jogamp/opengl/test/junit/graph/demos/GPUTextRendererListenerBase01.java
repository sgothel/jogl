/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.graph.demos;

import java.io.IOException;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.PMVMatrix;

/**
 *
 * GPURendererListenerBase01 Keys:
 * - 1/2: zoom in/out
 * - 6/7: 2nd pass texture size
 * - 0/9: rotate
 * - v: toggle v-sync
 * - s: screenshot
 *
 * Additional Keys:
 * - 3/4: font +/-
 * - h: toogle draw 'font set'
 * - f: toggle draw fps
 * - space: toggle font (ubuntu/java)
 * - i: live input text input (CR ends it, backspace supported)
 */
public abstract class GPUTextRendererListenerBase01 extends GPURendererListenerBase01 {
    public final TextRegionUtil textRegionUtil;
    private final GLRegion regionFPS, regionBottom;
    int fontSet = FontFactory.UBUNTU;
    Font font;

    int headType = 0;
    boolean drawFPS = true;
    final float fontSizeFName = 8f;
    final float fontSizeFPS = 10f;
    final int[] sampleCountFPS = new int[] { 8 };
    float fontSizeHead = 12f;
    float fontSizeBottom = 16f;
    float dpiH = 96;
    final int fontSizeModulo = 100;
    String fontName;
    AABBox fontNameBox;
    String headtext;
    AABBox headbox;

    static final String text1 = "abcdefghijklmnopqrstuvwxyz\nABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789.:,;(*!?/\\\")$%^&-+@~#<>{}[]";
    static final String text2 = "The quick brown fox jumps over the lazy dog";
    static final String textX =
        "JOGAMP graph demo using Resolution Independent NURBS\n"+
        "JOGAMP JOGL - OpenGL ES2 profile\n"+
        "Press 1/2 to zoom in/out the below text\n"+
        "Press 3/4 to incr/decs font size (alt: head, w/o bottom)\n"+
        "Press 6/7 to edit texture size if using VBAA\n"+
        "Press 0/9 to rotate the below string\n"+
        "Press v to toggle vsync\n"+
        "Press i for live input text input (CR ends it, backspace supported)\n"+
        "Press f to toggle fps. H for different text, space for font type\n";

    static final String textX2 =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec nec sapien tellus. \n"+
        "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel urna. Mauris ultricies \n"+
        "quam iaculis urna cursus ornare. Nullam ut felis a ante ultrices ultricies nec a elit. \n"+
        "In hac habitasse platea dictumst. Vivamus et mi a quam lacinia pharetra at venenatis est.\n"+
        "Morbi quis bibendum nibh. Donec lectus orci, sagittis in consequat nec, volutpat nec nisi.\n"+
        "Donec ut dolor et nulla tristique varius. In nulla magna, fermentum id tempus quis, semper \n"+
        "in lorem. Maecenas in ipsum ac justo scelerisque sollicitudin. Quisque sit amet neque lorem,\n" +
        "-------Press H to change text---------";

    StringBuilder userString = new StringBuilder();
    boolean userInput = false;
    public GPUTextRendererListenerBase01(final RenderState rs, final int renderModes, final int sampleCount, final boolean blending, final boolean debug, final boolean trace) {
        // NOTE_ALPHA_BLENDING: We use alpha-blending
        super(RegionRenderer.create(rs, blending ? RegionRenderer.defaultBlendEnable : null,
                                    blending ? RegionRenderer.defaultBlendDisable : null),
                                    renderModes, debug, trace);
        rs.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);
        this.textRegionUtil = new TextRegionUtil(renderModes);
        this.regionFPS = GLRegion.create(renderModes, null);
        this.regionBottom = GLRegion.create(renderModes, null);
        try {
            this.font = FontFactory.get(fontSet).getDefault();
            dumpFontNames();

            this.fontName = font.toString();
        } catch (final IOException ioe) {
            System.err.println("Caught: "+ioe.getMessage());
            ioe.printStackTrace();
        }
        setMatrix(0, 0, 0, 0f, sampleCount);
    }

    void dumpFontNames() {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.err.println(font.getAllNames(null, "\n"));
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    }

    void switchHeadBox() {
        headType = ( headType + 1 ) % 4 ;
        switch(headType) {
          case 0:
              headtext = null;
              break;

          case 1:
              headtext= textX2;
              break;
          case 2:
              headtext= textX;
              break;

          default:
              headtext = text1;
        }
        if(null != headtext) {
            headbox = font.getMetricBounds(headtext, font.getPixelSize(fontSizeHead, dpiH));
        }
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        super.init(drawable);
        final Object upObj = drawable.getUpstreamWidget();
        if( upObj instanceof Window ) {
            final Window window = (Window) upObj;
            final float[] sDPI = window.getPixelsPerMM(new float[2]);
            sDPI[0] *= 25.4f;
            sDPI[1] *= 25.4f;
            dpiH = sDPI[1];
            System.err.println("Using screen DPI of "+dpiH);
        } else {
            System.err.println("Using default DPI of "+dpiH);
        }
        fontNameBox = font.getMetricBounds(fontName, font.getPixelSize(fontSizeFName, dpiH));
        switchHeadBox();

    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int xstart, final int ystart, final int width, final int height) {
        super.reshape(drawable, xstart, ystart, width, height);
        final float dist = 100f;
        nearPlaneX0 = nearPlane1Box.getMinX() * dist;
        nearPlaneY0 = nearPlane1Box.getMinY() * dist;
        nearPlaneZ0 = nearPlane1Box.getMinZ() * dist;
        final float xd = nearPlane1Box.getWidth() * dist;
        final float yd = nearPlane1Box.getHeight() * dist;
        nearPlaneSx = xd  / width;
        nearPlaneSy = yd / height;
        nearPlaneS = nearPlaneSy;
        System.err.printf("Scale: [%f x %f] / [%d x %d] = [%f, %f] -> %f%n", xd, yd, width, height, nearPlaneSx, nearPlaneSy, nearPlaneS);
    }
    float nearPlaneX0, nearPlaneY0, nearPlaneZ0, nearPlaneSx, nearPlaneSy, nearPlaneS;

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        regionFPS.destroy(drawable.getGL().getGL2ES2());
        regionBottom.destroy(drawable.getGL().getGL2ES2());
        super.dispose(drawable);
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final int width = drawable.getSurfaceWidth();
        final int height = drawable.getSurfaceHeight();
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // final float zDistance0 =   500f;
        // final float zDistance1 =   400f;
        // final float[] objPos = new float[3];
        // final float[] winZ = new float[1];
        // final int[] view = new int[] { 0, 0, drawable.getWidth(),  drawable.getHeight() };

        final RegionRenderer renderer = getRenderer();
        final RenderState rs = renderer.getRenderState();
        final PMVMatrix pmv = renderer.getMatrix();
        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
        rs.setColorStatic(0.1f, 0.1f, 0.1f, 1.0f);
        final float pixelSizeFName = font.getPixelSize(fontSizeFName, dpiH);
        final float pixelSizeHead = font.getPixelSize(fontSizeHead, dpiH);
        final float pixelSizeBottom = font.getPixelSize(fontSizeBottom, dpiH);

        if( drawFPS ) {
            final float pixelSizeFPS = font.getPixelSize(fontSizeFPS, dpiH);
            final float lfps, tfps, td;
            final GLAnimatorControl animator = drawable.getAnimator();
            if( null != animator ) {
                lfps = animator.getLastFPS();
                tfps = animator.getTotalFPS();
                td = animator.getTotalFPSDuration()/1000f;
            } else {
                lfps = 0f;
                tfps = 0f;
                td = 0f;
            }
            final String modeS = Region.getRenderModeString(regionFPS.getRenderModes());
            final String text = String.format("%03.1f/%03.1f fps, v-sync %d, fontSize [head %.1f, bottom %.1f], %s-samples [%d, this %d], td %4.1f, blend %b, alpha-bits %d",
                    lfps, tfps, gl.getSwapInterval(), fontSizeHead, fontSizeBottom, modeS, getSampleCount()[0], sampleCountFPS[0], td,
                    renderer.getRenderState().isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED),
                    drawable.getChosenGLCapabilities().getAlphaBits());

            // bottom, half line up
            pmv.glTranslatef(nearPlaneX0, nearPlaneY0+(nearPlaneS * pixelSizeFPS / 2f), nearPlaneZ0);

            // No cache, keep region alive!
            TextRegionUtil.drawString3D(gl, regionFPS, renderer, font, nearPlaneS * pixelSizeFPS, text, null, sampleCountFPS,
                                        textRegionUtil.tempT1, textRegionUtil.tempT2);
        }

        float dx = width-fontNameBox.getWidth()-2f;
        float dy = height - 10f;

        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
        pmv.glTranslatef(nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+(dy*nearPlaneSy), nearPlaneZ0);
        // System.err.printf("FontN: [%f %f] -> [%f %f]%n", dx, dy, nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+(dy*nearPlaneSy));
        textRegionUtil.drawString3D(gl, renderer, font, nearPlaneS * pixelSizeFName, fontName, null, getSampleCount());

        dx  =  10f;
        dy += -fontNameBox.getHeight() - 10f;

        if(null != headtext) {
            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();
            // System.err.printf("Head: [%f %f] -> [%f %f]%n", dx, dy, nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+(dy*nearPlaneSy));
            pmv.glTranslatef(nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+(dy*nearPlaneSy), nearPlaneZ0);
            // pmv.glTranslatef(x0, y1, z0);
            textRegionUtil.drawString3D(gl, renderer, font, nearPlaneS * pixelSizeHead, headtext, null, getSampleCount());
        }

        dy += -headbox.getHeight() - font.getLineHeight(pixelSizeBottom);

        pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmv.glLoadIdentity();
        pmv.glTranslatef(nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+(dy*nearPlaneSy), nearPlaneZ0);
        // System.err.printf("Bottom: [%f %f] -> [%f %f]%n", dx, dy, nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+(dy*nearPlaneSy));
        pmv.glTranslatef(getXTran(), getYTran(), getZTran());
        pmv.glRotatef(getAngle(), 0, 1, 0);
        rs.setColorStatic(0.9f, 0.0f, 0.0f, 1.0f);

        if( bottomTextUseFrustum ) {
            regionBottom.setFrustum(pmv.glGetFrustum());
        }
        if(!userInput) {
            if( bottomTextUseFrustum ) {
                TextRegionUtil.drawString3D(gl, regionBottom, renderer, font, nearPlaneS * pixelSizeBottom, text2, null, getSampleCount(),
                                            textRegionUtil.tempT1, textRegionUtil.tempT2);
            } else {
                textRegionUtil.drawString3D(gl, renderer, font, nearPlaneS * pixelSizeBottom, text2, null, getSampleCount());
            }
        } else {
            if( bottomTextUseFrustum ) {
                TextRegionUtil.drawString3D(gl, regionBottom, renderer, font, nearPlaneS * pixelSizeBottom, userString.toString(), null, getSampleCount(),
                                            textRegionUtil.tempT1, textRegionUtil.tempT2);
            } else {
                textRegionUtil.drawString3D(gl, renderer, font, nearPlaneS * pixelSizeBottom, userString.toString(), null, getSampleCount());
            }
        }
    }
    final boolean bottomTextUseFrustum = true;

    public void fontBottomIncr(final int v) {
        fontSizeBottom = Math.abs((fontSizeBottom + v) % fontSizeModulo) ;
        dumpMatrix(true);
    }

    public void fontHeadIncr(final int v) {
        fontSizeHead = Math.abs((fontSizeHead + v) % fontSizeModulo) ;
        if(null != headtext) {
            headbox = font.getMetricBounds(headtext, font.getPixelSize(fontSizeHead, dpiH));
        }
    }

    public boolean nextFontSet() {
        try {
            final int set = ( fontSet == FontFactory.UBUNTU ) ? FontFactory.JAVA : FontFactory.UBUNTU ;
            final Font _font = FontFactory.get(set).getDefault();
            if(null != _font) {
                fontSet = set;
                font = _font;
                fontName = font.getFullFamilyName(null).toString();
                fontNameBox = font.getMetricBounds(fontName, font.getPixelSize(fontSizeFName, dpiH));
                dumpFontNames();
                return true;
            }
        } catch (final IOException ex) {
            System.err.println("Caught: "+ex.getMessage());
        }
        return false;
    }

    public boolean setFontSet(final int set, final int family, final int stylebits) {
        try {
            final Font _font = FontFactory.get(set).get(family, stylebits);
            if(null != _font) {
                fontSet = set;
                font = _font;
                fontName = font.getFullFamilyName(null).toString();
                fontNameBox = font.getMetricBounds(fontName, font.getPixelSize(fontSizeFName, dpiH));
                dumpFontNames();
                return true;
            }
        } catch (final IOException ex) {
            System.err.println("Caught: "+ex.getMessage());
        }
        return false;
    }

    public boolean isUserInputMode() { return userInput; }

    void dumpMatrix(final boolean bbox) {
        System.err.println("Matrix: " + getXTran() + "/" + getYTran() + " x"+getZTran() + " @"+getAngle() +" fontSize "+fontSizeBottom);
        if(bbox) {
            System.err.println("bbox: "+font.getMetricBounds(text2, nearPlaneS * font.getPixelSize(fontSizeBottom, dpiH)));
        }
    }

    KeyAction keyAction = null;

    @Override
    public void attachInputListenerTo(final GLWindow window) {
        if ( null == keyAction ) {
            keyAction = new KeyAction();
            window.addKeyListener(keyAction);
            super.attachInputListenerTo(window);
        }
    }

    @Override
    public void detachInputListenerFrom(final GLWindow window) {
        super.detachInputListenerFrom(window);
        if ( null == keyAction ) {
            return;
        }
        window.removeKeyListener(keyAction);
    }

    public void printScreen(final GLAutoDrawable drawable, final String dir, final String tech, final boolean exportAlpha) throws GLException, IOException {
        final String fn = font.getFullFamilyName(null).toString();
        printScreen(drawable, dir, tech, fn.replace(' ', '_'), exportAlpha);
    }

    float fontHeadScale = 1f;

    public class KeyAction implements KeyListener {
        @Override
        public void keyPressed(final KeyEvent e) {
            if(userInput) {
                return;
            }
            final short s = e.getKeySymbol();
            if(s == KeyEvent.VK_3) {
                if( e.isAltDown() ) {
                    fontHeadIncr(1);
                } else {
                    fontBottomIncr(1);
                }
            }
            else if(s == KeyEvent.VK_4) {
                if( e.isAltDown() ) {
                    fontHeadIncr(-1);
                } else {
                    fontBottomIncr(-1);
                }
            }
            else if(s == KeyEvent.VK_H) {
                switchHeadBox();
            }
            else if(s == KeyEvent.VK_F) {
                drawFPS = !drawFPS;
            }
            else if(s == KeyEvent.VK_SPACE) {
                nextFontSet();
            }
            else if(s == KeyEvent.VK_I) {
                userInput = true;
                setIgnoreInput(true);
            }
        }

        @Override
        public void keyReleased(final KeyEvent e) {
            if( !e.isPrintableKey() || e.isAutoRepeat() ) {
                return;
            }
            if(userInput) {
                final short k = e.getKeySymbol();
                if( KeyEvent.VK_ENTER == k ) {
                    userInput = false;
                    setIgnoreInput(false);
                } else if( KeyEvent.VK_BACK_SPACE == k && userString.length()>0) {
                    userString.deleteCharAt(userString.length()-1);
                } else {
                    final char c = e.getKeyChar();
                    if( font.isPrintableChar( c ) ) {
                        userString.append(c);
                    }
                }
            }
        }
    }
}
