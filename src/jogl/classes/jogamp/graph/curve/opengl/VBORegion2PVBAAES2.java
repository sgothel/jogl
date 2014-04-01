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
package jogamp.graph.curve.opengl;

import java.nio.FloatBuffer;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL;
import javax.media.opengl.GLUniformData;

import jogamp.graph.curve.opengl.shader.AttributeNames;
import jogamp.graph.curve.opengl.shader.UniformNames;
import jogamp.opengl.Debug;

import com.jogamp.common.nio.Buffers;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderProgram;

public class VBORegion2PVBAAES2  extends GLRegion {
    private static final boolean DEBUG_FBO_1 = false;
    private static final boolean DEBUG_FBO_2 = false;

    /**
     * Boundary triggering FBO resize if
     * <pre>
     *      fbo[Width|Height] - targetFbo[Width|Height] > RESIZE_BOUNDARY.
     * </pre>
     * <p>
     * Increasing the FBO will add RESIZE_BOUNDARY/2.
     * </p>
     * <p>
     * Reducing FBO resize to gain performance.
     * </p>
     * <p>
     * Defaults to disabled since:
     *   - not working properly
     *   - FBO texture rendered > than desired size
     *   - FBO resize itself should be fast enough ?!
     * </p>
     */
    private static final int RESIZE_BOUNDARY;

    static {
        Debug.initSingleton();
        final String key = "jogl.debug.graph.curve.vbaa.resizeLowerBoundary";
        RESIZE_BOUNDARY = Math.max(0, Debug.getIntProperty(key, true, 0));
        if( RESIZE_BOUNDARY > 0 ) {
            System.err.println("key: "+RESIZE_BOUNDARY);
        }
    }


    // Pass-1:
    private GLArrayDataServer gca_VerticesAttr;
    private GLArrayDataServer gca_CurveParamsAttr;
    private GLArrayDataServer gca_ColorsAttr;
    private GLArrayDataServer indicesBuffer;
    private ShaderProgram spPass1 = null;

    // Pass-2:
    private GLArrayDataServer gca_FboVerticesAttr;
    private GLArrayDataServer gca_FboTexCoordsAttr;
    private GLArrayDataServer indicesFbo;
    private final GLUniformData gcu_FboTexUnit;
    private final GLUniformData gcu_FboTexSize;
    private final PMVMatrix fboPMVMatrix;
    private final GLUniformData gcu_PMVMatrix02;
    private boolean gcu_FboTexSize_dirty = true;
    private boolean gcu_PMVMatrix02_dirty = true;
    private ShaderProgram spPass2 = null;

    private FBObject fbo;
    private TextureAttachment texA;

    private int fboWidth = 0;
    private int fboHeight = 0;
    private boolean fboDirty = true;

    final int[] maxTexSize = new int[] { -1 } ;

    public void useShaderProgram(final GL2ES2 gl, final RegionRenderer renderer, final int renderModes, final boolean pass1, final int quality, final int sampleCount) {
        final RenderState rs = renderer.getRenderState();
        final boolean updateLocation0 = renderer.useShaderProgram(gl, renderModes, pass1, quality, sampleCount);
        final ShaderProgram sp = renderer.getRenderState().getShaderProgram();
        final boolean updateLocation;
        if( pass1 ) {
            updateLocation = !sp.equals(spPass1);
            spPass1 = sp;
            rs.update(gl, updateLocation, renderModes, true);
            rs.updateUniformLoc(gl, updateLocation, gcu_PMVMatrix02);
            rs.updateAttributeLoc(gl, updateLocation, gca_VerticesAttr);
            rs.updateAttributeLoc(gl, updateLocation, gca_CurveParamsAttr);
            if( null != gca_ColorsAttr ) {
                rs.updateAttributeLoc(gl, updateLocation, gca_ColorsAttr);
            }
            System.err.println("XXX changedSP.p1 "+updateLocation+" / "+updateLocation0+", "+rs);
        } else {
            updateLocation = !sp.equals(spPass2);
            spPass2 = sp;
            rs.update(gl, updateLocation, renderModes, false);
            rs.updateAttributeLoc(gl, updateLocation, gca_FboVerticesAttr);
            rs.updateAttributeLoc(gl, updateLocation, gca_FboTexCoordsAttr);
            rs.updateUniformDataLoc(gl, updateLocation, true, gcu_FboTexUnit);
            rs.updateUniformLoc(gl, updateLocation, gcu_FboTexSize);
            System.err.println("XXX changedSP.p2 "+updateLocation+" / "+updateLocation0+", "+rs);
        }
    }

    public VBORegion2PVBAAES2(final int renderModes, final int textureUnit) {
        super(renderModes);
        final int initialElementCount = 256;

        // Pass 1:
        indicesBuffer = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, initialElementCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);

        gca_VerticesAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                      false, initialElementCount, GL.GL_STATIC_DRAW);

        gca_CurveParamsAttr = GLArrayDataServer.createGLSL(AttributeNames.CURVEPARAMS_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                       false, initialElementCount, GL.GL_STATIC_DRAW);
        if( hasColorChannel() ) {
            gca_ColorsAttr = GLArrayDataServer.createGLSL(AttributeNames.COLOR_ATTR_NAME, 4, GL2ES2.GL_FLOAT,
                                                          false, initialElementCount, GL.GL_STATIC_DRAW);
        } else {
            gca_ColorsAttr = null;
        }

        // Pass 2:
        fboPMVMatrix = new PMVMatrix();
        gcu_PMVMatrix02 = new GLUniformData(UniformNames.gcu_PMVMatrix02, 4, 4, fboPMVMatrix.glGetPMvMatrixf());
        gcu_FboTexUnit = new GLUniformData(UniformNames.gcu_FboTexUnit, textureUnit);
        gcu_FboTexSize = new GLUniformData(UniformNames.gcu_FboTexSize, 2, Buffers.newDirectFloatBuffer(2));

        indicesFbo = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, 2, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
        indicesFbo.puts((short) 0); indicesFbo.puts((short) 1); indicesFbo.puts((short) 3);
        indicesFbo.puts((short) 1); indicesFbo.puts((short) 2); indicesFbo.puts((short) 3);
        indicesFbo.seal(true);

        gca_FboTexCoordsAttr = GLArrayDataServer.createGLSL(AttributeNames.FBO_TEXCOORDS_ATTR_NAME, 2, GL2ES2.GL_FLOAT,
                                                           false, 4, GL.GL_STATIC_DRAW);
        gca_FboTexCoordsAttr.putf(0); gca_FboTexCoordsAttr.putf(0);
        gca_FboTexCoordsAttr.putf(0); gca_FboTexCoordsAttr.putf(1);
        gca_FboTexCoordsAttr.putf(1); gca_FboTexCoordsAttr.putf(1);
        gca_FboTexCoordsAttr.putf(1); gca_FboTexCoordsAttr.putf(0);
        gca_FboTexCoordsAttr.seal(true);

        gca_FboVerticesAttr = GLArrayDataServer.createGLSL(AttributeNames.FBO_VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                           false, 4, GL.GL_STATIC_DRAW);
    }

    @Override
    protected final void clearImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegion2PES2 Clear: " + this);
            // Thread.dumpStack();
        }
        if( null != indicesBuffer ) {
            indicesBuffer.seal(gl, false);
            indicesBuffer.rewind();
        }
        if( null != gca_VerticesAttr ) {
            gca_VerticesAttr.seal(gl, false);
            gca_VerticesAttr.rewind();
        }
        if( null != gca_CurveParamsAttr ) {
            gca_CurveParamsAttr.seal(gl, false);
            gca_CurveParamsAttr.rewind();
        }
        if( null != gca_ColorsAttr ) {
            gca_ColorsAttr.seal(gl, false);
            gca_ColorsAttr.rewind();
        }
        fboDirty = true;
    }

    @Override
    protected final void pushVertex(final float[] coords, final float[] texParams, float[] rgba) {
        gca_VerticesAttr.putf(coords[0]);
        gca_VerticesAttr.putf(coords[1]);
        gca_VerticesAttr.putf(coords[2]);

        gca_CurveParamsAttr.putf(texParams[0]);
        gca_CurveParamsAttr.putf(texParams[1]);
        gca_CurveParamsAttr.putf(texParams[2]);

        if( null != gca_ColorsAttr ) {
            if( null != rgba ) {
                gca_ColorsAttr.putf(rgba[0]);
                gca_ColorsAttr.putf(rgba[1]);
                gca_ColorsAttr.putf(rgba[2]);
                gca_ColorsAttr.putf(rgba[3]);
            } else {
                throw new IllegalArgumentException("Null color given for COLOR_CHANNEL rendering mode");
            }
        }
    }

    @Override
    protected final void pushIndex(int idx) {
        indicesBuffer.puts((short)idx);
    }

    @Override
    protected void updateImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        if(null == indicesFbo) {
            if(Region.DEBUG_INSTANCE) {
                System.err.println("VBORegion2PVBAAES2 Create: " + this);
            }
        }
        // seal buffers
        indicesBuffer.seal(gl, true);
        indicesBuffer.enableBuffer(gl, false);
        gca_CurveParamsAttr.seal(gl, true);
        gca_CurveParamsAttr.enableBuffer(gl, false);
        gca_VerticesAttr.seal(gl, true);
        gca_VerticesAttr.enableBuffer(gl, false);
        if( null != gca_ColorsAttr ) {
            gca_ColorsAttr.seal(gl, true);
            gca_ColorsAttr.enableBuffer(gl, false);
        }

        // update all bbox related data
        gca_FboVerticesAttr.seal(gl, false);
        {
            final FloatBuffer fb = (FloatBuffer)gca_FboVerticesAttr.getBuffer();
            fb.put( 2, box.getMinZ());
            fb.put( 5, box.getMinZ());
            fb.put( 8, box.getMinZ());
            fb.put(11, box.getMinZ());
        }
        // Pending .. (follow fboDirty)
        // gca_FboVerticesAttr.seal(gl, true);
        // gca_FboVerticesAttr.enableBuffer(gl, false);
        // fboPMVMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        // fboPMVMatrix.glLoadIdentity();
        // fboPMVMatrix.glOrthof(box.getMinX(), box.getMaxX(), box.getMinY(), box.getMaxY(), -1, 1);

        // push data 2 GPU ..
        indicesFbo.seal(gl, true);
        indicesFbo.enableBuffer(gl, false);

        fboDirty = true;
        // the buffers were disabled, since due to real/fbo switching and other vbo usage
    }

    private final AABBox drawWinBox = new AABBox();
    private final int[] drawView = new int[] { 0, 0, 0, 0 };
    private final float[] drawTmpV3 = new float[3];
    private final int border = 2; // surrounding border, i.e. width += 2*border, height +=2*border

    @Override
    protected void drawImpl(final GL2ES2 gl, final RegionRenderer renderer, final int[/*1*/] sampleCount) {
        if( 0 >= indicesBuffer.getElementCount() ) {
            if(DEBUG_INSTANCE) {
                System.err.printf("VBORegion2PVBAAES2.drawImpl: Empty%n");
            }
            return; // empty!
        }
        if( Float.isInfinite(box.getWidth()) || Float.isInfinite(box.getHeight()) ) {
            if(DEBUG_INSTANCE) {
                System.err.printf("VBORegion2PVBAAES2.drawImpl: Inf %s%n", box);
            }
            return; // inf
        }
        final int vpWidth = renderer.getWidth();
        final int vpHeight = renderer.getHeight();
        if(vpWidth <=0 || vpHeight <= 0 || null==sampleCount || sampleCount[0] <= 0){
            renderRegion(gl);
        } else {
            if(0 > maxTexSize[0]) {
                gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, maxTexSize, 0);
            }
            final RenderState rs = renderer.getRenderState();
            final float winWidth, winHeight;

            final float ratioObjWinWidth, ratioObjWinHeight;
            final float diffObjWidth, diffObjHeight;
            final float diffObjBorderWidth, diffObjBorderHeight;
            int targetFboWidth, targetFboHeight;
            {
                final float diffWinWidth, diffWinHeight;
                final int targetWinWidth, targetWinHeight;

                // Calculate perspective pixel width/height for FBO,
                // considering the sampleCount.
                drawView[2] = vpWidth;
                drawView[3] = vpHeight;
                box.mapToWindow(drawWinBox, renderer.getMatrix(), drawView, true /* useCenterZ */, drawTmpV3);
                winWidth = drawWinBox.getWidth();
                winHeight = drawWinBox.getHeight();
                targetWinWidth = (int)Math.ceil(winWidth);
                targetWinHeight = (int)Math.ceil(winHeight);
                diffWinWidth = targetWinWidth-winWidth;
                diffWinHeight = targetWinHeight-winHeight;

                ratioObjWinWidth = box.getWidth() / winWidth;
                ratioObjWinHeight= box.getHeight() / winHeight;
                diffObjWidth = diffWinWidth * ratioObjWinWidth;
                diffObjHeight = diffWinHeight * ratioObjWinHeight;
                diffObjBorderWidth = border * ratioObjWinWidth;
                diffObjBorderHeight = border * ratioObjWinHeight;

                targetFboWidth = (targetWinWidth+2*border)*sampleCount[0];
                targetFboHeight = (targetWinHeight+2*border)*sampleCount[0];

                if( DEBUG_FBO_2 ) {
                    final float ratioWinWidth, ratioWinHeight;
                    ratioWinWidth = winWidth/targetWinWidth;
                    ratioWinHeight = winHeight/targetWinHeight;
                    final float renderFboWidth, renderFboHeight;
                    renderFboWidth = (winWidth+2*border)*sampleCount[0];
                    renderFboHeight = (winHeight+2*border)*sampleCount[0];
                    final float ratioFboWidth, ratioFboHeight;
                    ratioFboWidth = renderFboWidth/targetFboWidth;
                    ratioFboHeight = renderFboHeight/targetFboHeight;
                    final float diffFboWidth, diffFboHeight;
                    diffFboWidth = targetFboWidth-renderFboWidth;
                    diffFboHeight = targetFboHeight-renderFboHeight;

                    System.err.printf("XXX.MinMax obj %s%n", box.toString());
                    System.err.printf("XXX.MinMax obj d[%.3f, %.3f], r[%f, %f], b[%f, %f]%n",
                            diffObjWidth, diffObjHeight, ratioObjWinWidth, ratioObjWinWidth, diffObjBorderWidth, diffObjBorderHeight);
                    System.err.printf("XXX.MinMax win %s%n", drawWinBox.toString());
                    System.err.printf("XXX.MinMax view[%d, %d] -> win[%.3f, %.3f], i[%d x %d], d[%.3f, %.3f], r[%f, %f]: FBO f[%.3f, %.3f], i[%d x %d], d[%.3f, %.3f], r[%f, %f], samples %d%n",
                            drawView[2], drawView[3],
                            winWidth, winHeight, targetWinWidth, targetWinHeight, diffWinWidth,
                            diffWinHeight, ratioWinWidth, ratioWinHeight,
                            renderFboWidth, renderFboHeight, targetFboWidth, targetFboHeight,
                            diffFboWidth, diffFboHeight, ratioFboWidth, ratioFboHeight,
                            sampleCount[0]);
                }
            }
            if( 0 >= targetFboWidth || 0 >= targetFboHeight ) {
                // Nothing ..
                return;
            }
            final int deltaFboWidth = Math.abs(targetFboWidth-fboWidth);
            final int deltaFboHeight = Math.abs(targetFboHeight-fboHeight);
            final boolean hasDelta = 0!=deltaFboWidth || 0!=deltaFboHeight;
            if( DEBUG_FBO_2 ) {
                System.err.printf("XXX.maxDelta: hasDelta %b: %d / %d,  %.3f, %.3f%n",
                        hasDelta, deltaFboWidth, deltaFboHeight, (float)deltaFboWidth/fboWidth, (float)deltaFboHeight/fboHeight);
                System.err.printf("XXX.Scale %d * [%f x %f]: %d x %d%n",
                        sampleCount[0], winWidth, winHeight, targetFboWidth, targetFboHeight);
            }
            if( hasDelta || fboDirty || null == fbo ) {
                final int maxLength = Math.max(targetFboWidth, targetFboHeight);
                if( maxLength > maxTexSize[0] ) {
                    if( targetFboWidth > targetFboHeight ) {
                        sampleCount[0] = (int)Math.floor(maxTexSize[0] / (winWidth+2*border));
                    } else {
                        sampleCount[0] = (int)Math.floor(maxTexSize[0] / (winHeight+2*border));
                    }
                    final float renderFboWidth, renderFboHeight;
                    renderFboWidth = (winWidth+2*border)*sampleCount[0];
                    renderFboHeight = (winWidth+2*border)*sampleCount[0];
                    targetFboWidth = (int)Math.ceil(renderFboWidth);
                    targetFboHeight = (int)Math.ceil(renderFboHeight);
                    if( DEBUG_FBO_1 ) {
                        System.err.printf("XXX.Rescale (MAX): win[%.3f, %.3f]: FBO f[%.3f, %.3f], i[%d x %d], msaa %d%n",
                                winWidth, winHeight,
                                renderFboWidth, renderFboHeight, targetFboWidth, targetFboHeight, sampleCount[0]);
                    }
                    if( sampleCount[0] <= 0 ) {
                        // Last way out!
                        renderRegion(gl);
                        return;
                    }
                }

                final int newFboWidth, newFboHeight, resizeCase;
                if( 0 >= RESIZE_BOUNDARY ) {
                    // Resize w/o optimization
                    newFboWidth = targetFboWidth;
                    newFboHeight = targetFboHeight;
                    resizeCase = 0;
                } else {
                    if( 0 >= fboWidth || 0 >= fboHeight || null == fbo ) {
                        // Case: New FBO
                        newFboWidth = targetFboWidth;
                        newFboHeight = targetFboHeight;
                        resizeCase = 1;
                    } else if( targetFboWidth > fboWidth || targetFboHeight > fboHeight ) {
                        // Case: Inscrease FBO Size, add boundary/2 if avail
                        newFboWidth = ( targetFboWidth + RESIZE_BOUNDARY/2 < maxTexSize[0] ) ? targetFboWidth + RESIZE_BOUNDARY/2 : targetFboWidth;
                        newFboHeight = ( targetFboHeight+ RESIZE_BOUNDARY/2 < maxTexSize[0] ) ? targetFboHeight + RESIZE_BOUNDARY/2 : targetFboHeight;
                        resizeCase = 2;
                    } else if( targetFboWidth < fboWidth && targetFboHeight < fboHeight &&
                               fboWidth - targetFboWidth < RESIZE_BOUNDARY &&
                               fboHeight - targetFboHeight < RESIZE_BOUNDARY ) {
                        // Case: Decreased FBO Size Request within boundary
                        newFboWidth = fboWidth;
                        newFboHeight = fboHeight;
                        resizeCase = 3;
                    } else {
                        // Case: Decreased-Size-Beyond-Boundary or No-Resize
                        newFboWidth = targetFboWidth;
                        newFboHeight = targetFboHeight;
                        resizeCase = 4;
                    }
                }
                final int dResizeWidth = newFboWidth - targetFboWidth;
                final int dResizeHeight = newFboHeight - targetFboHeight;
                final float diffObjResizeWidth = dResizeWidth*ratioObjWinWidth;
                final float diffObjResizeHeight = dResizeHeight*ratioObjWinHeight;
                if( DEBUG_FBO_1 ) {
                    System.err.printf("XXX.resizeFBO: case %d, has %dx%d > target %dx%d, resize: i[%d x %d], f[%.3f x %.3f] -> %dx%d%n",
                            resizeCase, fboWidth, fboHeight, targetFboWidth, targetFboHeight,
                            dResizeWidth, dResizeHeight, diffObjResizeWidth, diffObjResizeHeight,
                            newFboWidth, newFboHeight);
                }

                final float minX = box.getMinX()-diffObjBorderWidth;
                final float minY = box.getMinY()-diffObjBorderHeight;
                final float maxX = box.getMaxX()+diffObjBorderWidth+diffObjWidth+diffObjResizeWidth;
                final float maxY = box.getMaxY()+diffObjBorderHeight+diffObjHeight+diffObjResizeHeight;
                gca_FboVerticesAttr.seal(false);
                {
                    final FloatBuffer fb = (FloatBuffer)gca_FboVerticesAttr.getBuffer();
                    fb.put(0, minX); fb.put( 1, minY);
                    fb.put(3, minX); fb.put( 4, maxY);
                    fb.put(6, maxX); fb.put( 7, maxY);
                    fb.put(9, maxX); fb.put(10, minY);
                }
                gca_FboVerticesAttr.seal(true);
                fboPMVMatrix.glLoadIdentity();
                fboPMVMatrix.glOrthof(minX, maxX, minY, maxY, -1, 1);
                gcu_PMVMatrix02_dirty = true;
                useShaderProgram(gl, renderer, getRenderModes(), true, getQuality(), sampleCount[0]);
                renderRegion2FBO(gl, rs, targetFboWidth, targetFboHeight, newFboWidth, newFboHeight, vpWidth, vpHeight, sampleCount[0]);
            }
            useShaderProgram(gl, renderer, getRenderModes(), false, getQuality(), sampleCount[0]);
            renderFBO(gl, rs, targetFboWidth, targetFboHeight, vpWidth, vpHeight, sampleCount[0]);
        }
    }

    private void renderFBO(final GL2ES2 gl, final RenderState rs, final int targetFboWidth, final int targetFboHeight,
                           final int vpWidth, final int vpHeight, final int sampleCount) {
        gl.glViewport(0, 0, vpWidth, vpHeight);

        if( gcu_FboTexSize_dirty ) {
            gl.glUniform(gcu_FboTexSize);
            gcu_FboTexSize_dirty = false;
        }

        gl.glActiveTexture(GL.GL_TEXTURE0 + gcu_FboTexUnit.intValue());

        fbo.use(gl, texA);
        gca_FboVerticesAttr.enableBuffer(gl, true);
        gca_FboTexCoordsAttr.enableBuffer(gl, true);
        indicesFbo.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesFbo.getElementCount() * indicesFbo.getComponentCount(), GL2ES2.GL_UNSIGNED_SHORT, 0);

        indicesFbo.bindBuffer(gl, false);
        gca_FboTexCoordsAttr.enableBuffer(gl, false);
        gca_FboVerticesAttr.enableBuffer(gl, false);
        fbo.unuse(gl);

        // setback: gl.glActiveTexture(currentActiveTextureEngine[0]);
    }

    private void renderRegion2FBO(final GL2ES2 gl, final RenderState rs,
                                  final int targetFboWidth, final int targetFboHeight, final int newFboWidth, final int newFboHeight,
                                  final int vpWidth, final int vpHeight, final int sampleCount) {
        if( 0 >= targetFboWidth || 0 >= targetFboHeight ) {
            throw new IllegalArgumentException("fboSize must be greater than 0: "+targetFboWidth+"x"+targetFboHeight);
        }

        if(null == fbo) {
            fboWidth  = newFboWidth;
            fboHeight  = newFboHeight;
            final FloatBuffer fboTexSize = (FloatBuffer) gcu_FboTexSize.getBuffer();
            {
                fboTexSize.put(0, fboWidth);
                fboTexSize.put(1, fboHeight);
                gcu_FboTexSize_dirty=true;
            }
            fbo = new FBObject();
            fbo.reset(gl, fboWidth, fboHeight);
            // Shall not use bilinear (GL_LINEAR), due to own VBAA. Result is smooth w/o it now!
            // FIXME: FXAA requires bilinear filtering!
            // texA = fbo.attachTexture2D(gl, 0, true, GL2ES2.GL_LINEAR, GL2ES2.GL_LINEAR, GL2ES2.GL_CLAMP_TO_EDGE, GL2ES2.GL_CLAMP_TO_EDGE);
            texA = fbo.attachTexture2D(gl, 0, true, GL2ES2.GL_NEAREST, GL2ES2.GL_NEAREST, GL2ES2.GL_CLAMP_TO_EDGE, GL2ES2.GL_CLAMP_TO_EDGE);
            fbo.attachRenderbuffer(gl, Attachment.Type.DEPTH, 24);
            if( DEBUG_FBO_1 ) {
                System.err.printf("XXX.createFBO: %dx%d%n%s%n", fboWidth, fboHeight, fbo.toString());
            }
        } else if( newFboWidth != fboWidth || newFboHeight != fboHeight ) {
            fbo.reset(gl, newFboWidth, newFboHeight);
            fbo.bind(gl);
            if( DEBUG_FBO_1 ) {
                System.err.printf("XXX.resetFBO: %dx%d -> %dx%d, target %dx%d%n", fboWidth, fboHeight, newFboWidth, newFboHeight, targetFboWidth, targetFboHeight);
            }
            fboWidth  = newFboWidth;
            fboHeight  = newFboHeight;
            final FloatBuffer fboTexSize = (FloatBuffer) gcu_FboTexSize.getBuffer();
            {
                fboTexSize.put(0, fboWidth);
                fboTexSize.put(1, fboHeight);
                gcu_FboTexSize_dirty=true;
            }
        } else {
            fbo.bind(gl);
        }

        //render texture
        gl.glViewport(0, 0, fboWidth, fboHeight);

        if( gcu_PMVMatrix02_dirty ) {
            gl.glUniform(gcu_PMVMatrix02);
            gcu_PMVMatrix02_dirty = false;
        }

        gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT | GL2ES2.GL_DEPTH_BUFFER_BIT);
        renderRegion(gl);
        fbo.unbind(gl);
        fboDirty = false;
    }

    private void renderRegion(final GL2ES2 gl) {
        gca_VerticesAttr.enableBuffer(gl, true);
        gca_CurveParamsAttr.enableBuffer(gl, true);
        if( null != gca_ColorsAttr ) {
            gca_ColorsAttr.enableBuffer(gl, true);
        }
        indicesBuffer.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesBuffer.getElementCount() * indicesBuffer.getComponentCount(), GL2ES2.GL_UNSIGNED_SHORT, 0);

        indicesBuffer.bindBuffer(gl, false);
        if( null != gca_ColorsAttr ) {
            gca_ColorsAttr.enableBuffer(gl, false);
        }
        gca_CurveParamsAttr.enableBuffer(gl, false);
        gca_VerticesAttr.enableBuffer(gl, false);
    }

    @Override
    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegion2PES2 Destroy: " + this);
            // Thread.dumpStack();
        }
        if(null != fbo) {
            fbo.destroy(gl);
            fbo = null;
            texA = null;
        }
        if(null != gca_VerticesAttr) {
            gca_VerticesAttr.destroy(gl);
            gca_VerticesAttr = null;
        }
        if(null != gca_CurveParamsAttr) {
            gca_CurveParamsAttr.destroy(gl);
            gca_CurveParamsAttr = null;
        }
        if(null != gca_ColorsAttr) {
            gca_ColorsAttr.destroy(gl);
            gca_ColorsAttr = null;
        }
        if(null != indicesBuffer) {
            indicesBuffer.destroy(gl);
            indicesBuffer = null;
        }

        if(null != gca_FboVerticesAttr) {
            gca_FboVerticesAttr.destroy(gl);
            gca_FboVerticesAttr = null;
        }
        if(null != gca_FboTexCoordsAttr) {
            gca_FboTexCoordsAttr.destroy(gl);
            gca_FboTexCoordsAttr = null;
        }
        if(null != indicesFbo) {
            indicesFbo.destroy(gl);
            indicesFbo = null;
        }
    }
}
