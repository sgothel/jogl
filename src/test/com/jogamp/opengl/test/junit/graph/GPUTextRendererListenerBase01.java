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
package com.jogamp.opengl.test.junit.graph;

import java.io.IOException;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontScale;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.geom.plane.AffineTransform;
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
    private final GLRegion regionFPS, regionHead, regionBottom;
    int fontSet = FontFactory.UBUNTU;
    Font font;

    int headType = 1;
    boolean drawFPS = true;
    final float fontSizeFName = 10f;
    final float fontSizeFPS = 10f;
    final int[] sampleCountFPS = new int[] { 8 };
    float fontSizeHead = 12f;
    float fontSizeCenter = 16f;
    float dpiV = 96;
    float ppmmV = 1;
    final int fontSizeModulo = 100;
    String fontName;
    AABBox fontNameBox;
    String headtext;
    AABBox headbox;

    protected final AffineTransform tempT1 = new AffineTransform();
    protected final AffineTransform tempT2 = new AffineTransform();

    static final String text2 = "The quick brown fox jumps over the lazy dog";
    public static final String text_help =
        "JOGL: Java™ Binding for OpenGL®, providing hardware-accelerated 3D graphics.\n\n"+
        "JOGAMP graph demo using Resolution Independent NURBS\n"+
        "JOGAMP JOGL - OpenGL ES2 profile\n"+
        "Press 1/2 to zoom in/out the below text\n"+
        "Press 3/4 to incr/decs font size (alt: head, w/o bottom)\n"+
        "Press 6/7 to edit texture size if using VBAA\n"+
        "Press 0/9 to rotate the below string\n"+
        "Press s to screenshot\n"+
        "Press v to toggle vsync\n"+
        "Press i for live input text input (CR ends it, backspace supported)\n"+
        "Press f to toggle fps. H for different text, space for font type\n";

    public static final String textX1 =
        "JOGL: Java™ Binding for OpenGL®, providing hardware-accelerated 3D graphics.\n\n"+
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec nec sapien tellus. \n"+
        "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel urna. Mauris ultricies \n"+
        "quam iaculis urna cursus ornare. Nullam ut felis a ante ultrices ultricies nec a elit. \n"+
        "In hac habitasse platea dictumst. Vivamus et mi a quam lacinia pharetra at venenatis est. \n"+
        "Morbi quis bibendum nibh. Donec lectus orci, sagittis in consequat nec, volutpat nec nisi. \n"+
        "Donec ut dolor et nulla tristique varius. In nulla magna, fermentum id tempus quis, semper \n"+
        "in lorem. Maecenas in ipsum ac justo scelerisque sollicitudin. Quisque sit amet neque lorem, \n" +
        "-------Press H to change text---------";

    public static final String textX2 =
        "I “Ask Jeff” or ‘Ask Jeff’. Take the chef d’œuvre! Two of [of] (of) ‘of’ “of” of? of! of*.\n"+
        "Les Woëvres, the Fôret de Wœvres, the Voire and Vauvise. Yves is in heaven; D’Amboise is in jail.\n"+
        "Lyford’s in Texas & L’Anse-aux-Griffons in Québec; the Łyna in Poland. Yriarte, Yciar and Ysaÿe are at Yale.\n"+
        "Kyoto and Ryotsu are both in Japan, Kwikpak on the Yukon delta, Kvæven in Norway, Kyulu in Kenya, not in Rwanda.…\n"+
        "Von-Vincke-Straße in Münster, Vdovino in Russia, Ytterbium in the periodic table. Are Toussaint L’Ouverture, Wölfflin, Wolfe,\n"+
        "Miłosz and Wū Wŭ all in the library? 1510–1620, 11:00 pm, and the 1980s are over. X\n"+
        "-------Press H to change text---------";

    public static final String textX20 =
        "I “Ask Jeff” or ‘Ask Jeff’. Take the chef d’œuvre! Two of [of] (of) ‘of’ “of” of? of! of*.\n"+
        "Two of [of] (of) ‘of’ “of” of? of! of*. Ydes, Yffignac and Ygrande are in France: so are Ypres,\n"+
        "Les Woëvres, the Fôret de Wœvres, the Voire and Vauvise. Yves is in heaven; D’Amboise is in jail.\n"+
        "Lyford’s in Texas & L’Anse-aux-Griffons in Québec; the Łyna in Poland. Yriarte, Yciar and Ysaÿe are at Yale.\n"+
        "Kyoto and Ryotsu are both in Japan, Kwikpak on the Yukon delta, Kvæven in Norway, Kyulu in Kenya, not in Rwanda.…\n"+
        "Walton’s in West Virginia, but «Wren» is in Oregon. Tlálpan is near Xochimilco in México.\n"+
        "The Zygos & Xylophagou are in Cyprus, Zwettl in Austria, Fænø in Denmark, the Vøringsfossen and Værøy in Norway.\n"+
        "Tchula is in Mississippi, the Tittabawassee in Michigan. Twodot is here in Montana, Ywamun in Burma.\n"+
        "Yggdrasil and Ymir, Yngvi and Vóden, Vídrið and Skeggjöld and Týr are all in the Eddas.\n"+
        "Tørberget and Våg, of course, are in Norway, Ktipas and Tmolos in Greece, but Vázquez is in Argentina, Vreden in Germany,\n"+
        "Von-Vincke-Straße in Münster, Vdovino in Russia, Ytterbium in the periodic table. Are Toussaint L’Ouverture, Wölfflin, Wolfe,\n"+
        "Miłosz and Wū Wŭ all in the library? 1510–1620, 11:00 pm, and the 1980s are over.\n"+
        "Ut purus odio, rhoncus sit amet commodo eget, ullamcorper vel urna. Mauris ultricies \n"+
        "-------Press H to change text---------";

    static final String textXLast = "abcdefghijklmnopqrstuvwxyz\nABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789.:,;(*!?/\\\")$%^&-+@~#<>{}[]";

    Window upstream_window = null;
    StringBuilder userString = new StringBuilder(textX1);
    boolean userInput = false;
    public GPUTextRendererListenerBase01(final GLProfile glp, final RenderState rs, final int renderModes, final int sampleCount, final boolean blending, final boolean debug, final boolean trace) {
        // NOTE_ALPHA_BLENDING: We use alpha-blending
        super(RegionRenderer.create(rs, blending ? RegionRenderer.defaultBlendEnable : null,
                                    blending ? RegionRenderer.defaultBlendDisable : null),
                                    renderModes, debug, trace);
        rs.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);
        this.textRegionUtil = new TextRegionUtil(renderModes);
        this.regionFPS = GLRegion.create(glp, renderModes, null);
        this.regionHead = GLRegion.create(glp, renderModes, null);
        this.regionBottom = GLRegion.create(glp, renderModes, null);
        setFontSet(fontSet, FontSet.FAMILY_LIGHT, FontSet.STYLE_NONE);
        setMatrix(0, 0, 0, 0f, sampleCount);
    }

    void switchHeadBox() {
        setHeadBox( ( headType + 1 ) % 5, true );
    }
    public int getHeadBoxType() { return headType; }
    public AABBox getHeadBox() { return headbox; }
    public void setHeadBox(final int choice, final boolean resize) {
        headType =  choice % 5 ;
        switch(headType) {
          case 0:
              headtext = null;
              break;

          case 1:
              headtext= textX1;
              break;
          case 2:
              headtext= textX2;
              break;
          case 3:
              headtext= text_help;
              break;

          default:
              headtext = textXLast;
        }
        if(resize && null != headtext) {
            headbox = font.getMetricBounds(headtext);
            if( headtext != text_help ) {
                final float pxSz = FontScale.toPixels(fontSizeHead, dpiV);
                upsizeWindowSurface(upstream_window, true, (int)(headbox.getWidth()*pxSz*1.1f), (int)(headbox.getHeight()*pxSz*2f));
            }
        }
    }

    public void setHeadBox(final String text, final boolean resize) {
        headtext = text;
        if(resize && null != headtext) {
            headbox = font.getMetricBounds(headtext);
            if( headtext != text_help ) {
                final float pxSz = FontScale.toPixels(fontSizeHead, dpiV);
                upsizeWindowSurface(upstream_window, true, (int)(headbox.getWidth()*pxSz*1.1f), (int)(headbox.getHeight()*pxSz*2f));
            }
        }
    }

    public static void upsizeWindowSurface(final Window window, final boolean off_thread, final int w, final int h) {
        if( null == window ) {
            return;
        }
        final int w2 = Math.max(window.getSurfaceWidth(), w);
        final int h2 = Math.max(window.getSurfaceHeight(), h);
        System.err.println("upsizeWindowSurface: "+window.getSurfaceWidth()+"x"+window.getSurfaceHeight()+" -> "+w+"x"+h+" -> "+w2+"x"+h2);
        if( off_thread ) {
            new InterruptSource.Thread() {
                @Override
                public void run() {
                    window.setSurfaceSize(w2, h2);
                } }.start();
        } else {
            window.setSurfaceSize(w2, h2);
        }
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        super.init(drawable);
        final Object upObj = drawable.getUpstreamWidget();
        if( upObj instanceof Window ) {
            upstream_window = (Window) upObj;
            final float[] sPpMM = upstream_window.getPixelsPerMM(new float[2]);
            final float[] sDPI = FontScale.perMMToPerInch( new float[] { sPpMM[0], sPpMM[1] } );
            dpiV = sDPI[1];
            ppmmV = sPpMM[1];
            System.err.println("Using vertical screen DPI of "+dpiV+", "+ppmmV+" pixel/mm");
        } else {
            System.err.println("Using vertical default DPI of "+dpiV+", "+ppmmV+" pixel/mm");
        }
        fontNameBox = font.getGlyphBounds(fontName, tempT1, tempT2);
        setHeadBox(headType, true);
        {
            final float pixelSizeFName = FontScale.toPixels(fontSizeFName, dpiV);
            System.err.println("XXX: fontName size "+fontSizeFName+"pt, dpiV "+dpiV+" -> "+pixelSizeFName+"px");
            System.err.println("XXX: fontName boxM fu "+font.getMetricBoundsFU(fontName));
            System.err.println("XXX: fontName boxG fu "+font.getGlyphBoundsFU(fontName, tempT1, tempT2));
            System.err.println("XXX: fontName boxM em "+font.getMetricBounds(fontName));
            System.err.println("XXX: fontName boxG em "+font.getGlyphBounds(fontName, tempT1, tempT2));
            System.err.println("XXX: fontName box height px "+(fontNameBox.getHeight() * pixelSizeFName));
        }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int xstart, final int ystart, final int width, final int height) {
        super.reshape(drawable, xstart, ystart, width, height);
        final Object upObj = drawable.getUpstreamWidget();
        if( upObj instanceof Window ) {
            upstream_window = (Window) upObj;
        }
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
        upstream_window = null;
        regionFPS.destroy(drawable.getGL().getGL2ES2());
        regionHead.destroy(drawable.getGL().getGL2ES2());
        regionBottom.destroy(drawable.getGL().getGL2ES2());
        super.dispose(drawable);
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final Object upObj = drawable.getUpstreamWidget();
        if( upObj instanceof Window ) {
            upstream_window = (Window) upObj;
        }
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
        final float pixelSizeFName = FontScale.toPixels(fontSizeFName, dpiV);
        final float pixelSizeHead = FontScale.toPixels(fontSizeHead, dpiV);
        final float mmSizeHead = pixelSizeHead / ppmmV;
        final float pixelSizeCenter = FontScale.toPixels(fontSizeCenter, dpiV);
        final float mmSizeCenter = pixelSizeCenter / ppmmV;

        renderer.enable(gl, true);

        if( drawFPS ) {
            pmv.glPushMatrix();
            final float pixelSizeFPS = FontScale.toPixels(fontSizeFPS, dpiV);
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
            final String text = String.format("%03.1f/%03.1f fps, v-sync %d, dpiV %.2f %.2f px/mm, font[head %.1fpt %.2fpx %.2fmm, center %.1fpt %.2fpx %.2fmm], %s-samples[%d, this %d], blend %b, alpha %d",
                    lfps, tfps, gl.getSwapInterval(), dpiV, ppmmV,
                    fontSizeHead, pixelSizeHead, mmSizeHead,
                    fontSizeCenter, pixelSizeCenter, mmSizeCenter,
                    modeS, getSampleCount()[0], sampleCountFPS[0],
                    renderer.getRenderState().isHintMaskSet(RenderState.BITHINT_BLENDING_ENABLED),
                    drawable.getChosenGLCapabilities().getAlphaBits());

            // bottom, half line up
            pmv.glTranslatef(nearPlaneX0, nearPlaneY0+(nearPlaneS * pixelSizeFPS / 2f), nearPlaneZ0);
            {
                final float sxy = nearPlaneS * pixelSizeFPS;
                pmv.glScalef(sxy, sxy, 1.0f);
            }
            // No cache, keep region alive!
            TextRegionUtil.drawString3D(gl, regionFPS.clear(gl), renderer, font, text, null, sampleCountFPS, tempT1, tempT2);
            pmv.glPopMatrix();
        }

        // float dx = width - ( fontNameBox.getWidth() + font.getAdvanceWidth( Glyph.ID_SPACE ) ) * pixelSizeFName;
        float dx = width - ( fontNameBox.getWidth() + 2 * font.getAdvanceWidth( font.getGlyphID('X') ) ) * pixelSizeFName;
        float dy = height - fontNameBox.getHeight() * pixelSizeFName;
        {
            pmv.glPushMatrix();
            pmv.glTranslatef(nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+(dy*nearPlaneSy), nearPlaneZ0);
            {
                final float sxy = nearPlaneS * pixelSizeFName;
                pmv.glScalef(sxy, sxy, 1.0f);
            }
            // System.err.printf("FontN: [%f %f] -> [%f %f]%n", dx, dy, nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+(dy*nearPlaneSy));
            textRegionUtil.drawString3D(gl, renderer, font, fontName, null, getSampleCount());
            pmv.glPopMatrix();
        }

        dx  =  10f;
        dy += -fontNameBox.getHeight() * pixelSizeFName - 10f;

        if(null != headtext) {
            pmv.glPushMatrix();
            // System.err.printf("Head: [%f %f] -> [%f %f]%n", dx, dy, nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+(dy*nearPlaneSy));
            pmv.glTranslatef(nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+(dy*nearPlaneSy), nearPlaneZ0);
            {
                final float sxy = nearPlaneS * pixelSizeHead;
                pmv.glScalef(sxy, sxy, 1.0f);
            }
            // pmv.glTranslatef(x0, y1, z0);
            textRegionUtil.drawString3D(gl, renderer, font, headtext, null, getSampleCount());
            pmv.glPopMatrix();
        }

        dy += ( -headbox.getHeight() - font.getLineHeight() ) * pixelSizeCenter;

        {
            pmv.glPushMatrix();
            pmv.glTranslatef(nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+(dy*nearPlaneSy), nearPlaneZ0);
            // System.err.printf("Bottom: [%f %f] -> [%f %f]%n", dx, dy, nearPlaneX0+(dx*nearPlaneSx), nearPlaneY0+(dy*nearPlaneSy));
            pmv.glTranslatef(getXTran(), getYTran(), getZTran());
            pmv.glRotatef(getAngle(), 0, 1, 0);
            {
                final float sxy = nearPlaneS * pixelSizeCenter;
                pmv.glScalef(sxy, sxy, 1.0f);
            }
            rs.setColorStatic(0.9f, 0.0f, 0.0f, 1.0f);

            if( bottomTextUseFrustum ) {
                regionBottom.setFrustum(pmv.glGetFrustum());
            }
            if(!userInput) {
                if( bottomTextUseFrustum ) {
                    TextRegionUtil.drawString3D(gl, regionBottom.clear(gl), renderer, font, text2, null, getSampleCount(), tempT1, tempT2);
                } else {
                    textRegionUtil.drawString3D(gl, renderer, font, text2, null, getSampleCount());
                }
            } else {
                if( bottomTextUseFrustum ) {
                    TextRegionUtil.drawString3D(gl, regionBottom.clear(gl), renderer, font, userString.toString(), null, getSampleCount(), tempT1, tempT2);
                } else {
                    textRegionUtil.drawString3D(gl, renderer, font, userString.toString(), null, getSampleCount());
                }
            }
            pmv.glPopMatrix();
        }
        renderer.enable(gl, false);
    }
    final boolean bottomTextUseFrustum = true;

    public Font getFont() { return font; }
    public float getFontSizeHead() { return fontSizeHead; }

    public void fontBottomIncr(final int v) {
        fontSizeCenter = Math.abs((fontSizeCenter + v) % fontSizeModulo) ;
        dumpMatrix(true);
    }

    public void fontHeadIncr(final int v) {
        fontSizeHead = Math.abs((fontSizeHead + v) % fontSizeModulo) ;
        updateFontNameBox();
        if(null != headtext) {
            headbox = font.getMetricBounds(headtext);
        }
    }

    public void setFontHeadSize(final int v) {
        if( 0 < v ) {
            fontSizeHead = v % fontSizeModulo;
            updateFontNameBox();
            if(null != headtext) {
                headbox = font.getMetricBounds(headtext);
            }
        }
    }

    public boolean nextFontSet() {
        try {
            final int set = ( fontSet == FontFactory.UBUNTU ) ? FontFactory.JAVA : FontFactory.UBUNTU ;
            final Font _font = FontFactory.get(set).getDefault();
            if(null != _font) {
                fontSet = set;
                font = _font;
                updateFontNameBox();
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
                updateFontNameBox();
                return true;
            }
        } catch (final IOException ex) {
            System.err.println("Caught: "+ex.getMessage());
        }
        return false;
    }

    public boolean setFont(final Font _font) {
        if(null != _font) {
            // fontSet = ???
            font = _font;
            updateFontNameBox();
            return true;
        }
        return false;
    }

    private void updateFontNameBox() {
        fontName = font.getFullFamilyName()+" (head "+fontSizeHead+"pt)";
        fontNameBox = font.getMetricBounds(fontName);
    }

    public boolean isUserInputMode() { return userInput; }

    void dumpMatrix(final boolean bbox) {
        System.err.println("Matrix: " + getXTran() + "/" + getYTran() + " x"+getZTran() + " @"+getAngle() +" fontSize "+fontSizeCenter);
        if(bbox) {
            System.err.println("bbox em: "+font.getMetricBounds(text2));
            System.err.println("bbox px: "+font.getMetricBounds(text2).scale(nearPlaneS * FontScale.toPixels(fontSizeCenter, dpiV), new float[3]));
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

    @Override
    public void printScreen(final GLAutoDrawable drawable, final String dir, final String tech, final String objName, final boolean exportAlpha) throws GLException, IOException {
        final String fn = font.getFullFamilyName().replace(' ', '_').replace('-', '_');
        final String modes = Region.getRenderModeString(getRenderModes());
        final String fsaa = "fsaa"+drawable.getChosenGLCapabilities().getNumSamples();
        super.printScreen(drawable, dir, tech+"-"+modes, fsaa+"-"+fn+"-text"+getHeadBoxType()+"-"+objName, exportAlpha);
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
