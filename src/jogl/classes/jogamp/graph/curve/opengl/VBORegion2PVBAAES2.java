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
import javax.media.opengl.fixedfunc.GLMatrixFunc;

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
import com.jogamp.opengl.util.glsl.ShaderState;

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


    private GLArrayDataServer verticeTxtAttr;
    private GLArrayDataServer texCoordTxtAttr;
    private GLArrayDataServer indicesTxtBuffer;
    private GLArrayDataServer verticeFboAttr;
    private GLArrayDataServer texCoordFboAttr;
    private GLArrayDataServer indicesFbo;

    private FBObject fbo;
    private TextureAttachment texA;
    private final PMVMatrix fboPMVMatrix;
    GLUniformData mgl_fboPMVMatrix;

    private int fboWidth = 0;
    private int fboHeight = 0;
    private boolean fboDirty = true;
    GLUniformData mgl_ActiveTexture;
    GLUniformData mgl_TextureSize;

    final int[] maxTexSize = new int[] { -1 } ;

    public VBORegion2PVBAAES2(final int renderModes, final int textureUnit) {
        super(renderModes);
        final int initialElementCount = 256;
        fboPMVMatrix = new PMVMatrix();
        mgl_fboPMVMatrix = new GLUniformData(UniformNames.gcu_PMVMatrix, 4, 4, fboPMVMatrix.glGetPMvMatrixf());
        mgl_ActiveTexture = new GLUniformData(UniformNames.gcu_TextureUnit, textureUnit);

        indicesTxtBuffer = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, initialElementCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
        verticeTxtAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                      false, initialElementCount, GL.GL_STATIC_DRAW);
        texCoordTxtAttr = GLArrayDataServer.createGLSL(AttributeNames.TEXCOORD_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                       false, initialElementCount, GL.GL_STATIC_DRAW);
    }

    @Override
    protected final void clearImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegion2PES2 Clear: " + this);
            // Thread.dumpStack();
        }
        if( null != indicesTxtBuffer ) {
            indicesTxtBuffer.seal(gl, false);
            indicesTxtBuffer.rewind();
        }
        if( null != verticeTxtAttr ) {
            verticeTxtAttr.seal(gl, false);
            verticeTxtAttr.rewind();
        }
        if( null != texCoordTxtAttr ) {
            texCoordTxtAttr.seal(gl, false);
            texCoordTxtAttr.rewind();
        }
        fboDirty = true;
    }

    @Override
    protected final void pushVertex(float[] coords, float[] texParams) {
        verticeTxtAttr.putf(coords[0]);
        verticeTxtAttr.putf(coords[1]);
        verticeTxtAttr.putf(coords[2]);

        texCoordTxtAttr.putf(texParams[0]);
        texCoordTxtAttr.putf(texParams[1]);
        texCoordTxtAttr.putf(texParams[2]);
    }

    @Override
    protected final void pushIndex(int idx) {
        indicesTxtBuffer.puts((short)idx);
    }

    @Override
    protected void updateImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        if(null == indicesFbo) {
            final ShaderState st = renderer.getShaderState();

            indicesFbo = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, 2, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);
            indicesFbo.puts((short) 0); indicesFbo.puts((short) 1); indicesFbo.puts((short) 3);
            indicesFbo.puts((short) 1); indicesFbo.puts((short) 2); indicesFbo.puts((short) 3);
            indicesFbo.seal(true);

            texCoordFboAttr = GLArrayDataServer.createGLSL(AttributeNames.TEXCOORD_ATTR_NAME, 2, GL2ES2.GL_FLOAT,
                                                           false, 4, GL.GL_STATIC_DRAW);
            st.ownAttribute(texCoordFboAttr, true);
            texCoordFboAttr.putf(0); texCoordFboAttr.putf(0);
            texCoordFboAttr.putf(0); texCoordFboAttr.putf(1);
            texCoordFboAttr.putf(1); texCoordFboAttr.putf(1);
            texCoordFboAttr.putf(1); texCoordFboAttr.putf(0);
            texCoordFboAttr.seal(true);

            verticeFboAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                                                          false, 4, GL.GL_STATIC_DRAW);
            st.ownAttribute(verticeFboAttr, true);

            st.ownAttribute(verticeTxtAttr, true);
            st.ownAttribute(texCoordTxtAttr, true);

            if(Region.DEBUG_INSTANCE) {
                System.err.println("VBORegion2PVBAAES2 Create: " + this);
            }
        }
        // seal buffers
        indicesTxtBuffer.seal(gl, true);
        indicesTxtBuffer.enableBuffer(gl, false);
        texCoordTxtAttr.seal(gl, true);
        texCoordTxtAttr.enableBuffer(gl, false);
        verticeTxtAttr.seal(gl, true);
        verticeTxtAttr.enableBuffer(gl, false);

        // update all bbox related data
        verticeFboAttr.seal(gl, false);
        verticeFboAttr.rewind();
        verticeFboAttr.putf(box.getMinX()); verticeFboAttr.putf(box.getMinY()); verticeFboAttr.putf(box.getMinZ());
        verticeFboAttr.putf(box.getMinX()); verticeFboAttr.putf(box.getMaxY()); verticeFboAttr.putf(box.getMinZ());
        verticeFboAttr.putf(box.getMaxX()); verticeFboAttr.putf(box.getMaxY()); verticeFboAttr.putf(box.getMinZ());
        verticeFboAttr.putf(box.getMaxX()); verticeFboAttr.putf(box.getMinY()); verticeFboAttr.putf(box.getMinZ());
        verticeFboAttr.seal(gl, true);
        verticeFboAttr.enableBuffer(gl, false);

        fboPMVMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        fboPMVMatrix.glLoadIdentity();
        fboPMVMatrix.glOrthof(box.getMinX(), box.getMaxX(), box.getMinY(), box.getMaxY(), -1, 1);

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
        if( 0 >= indicesTxtBuffer.getElementCount() ) {
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
                verticeFboAttr.seal(false);
                {
                    final FloatBuffer fb = (FloatBuffer)verticeFboAttr.getBuffer();
                    fb.put(0, minX); fb.put( 1, minY);
                    fb.put(3, minX); fb.put( 4, maxY);
                    fb.put(6, maxX); fb.put( 7, maxY);
                    fb.put(9, maxX); fb.put(10, minY);
                }
                verticeFboAttr.seal(true);
                fboPMVMatrix.glLoadIdentity();
                fboPMVMatrix.glOrthof(minX, maxX, minY, maxY, -1, 1);
                renderRegion2FBO(gl, rs, targetFboWidth, targetFboHeight, newFboWidth, newFboHeight, vpWidth, vpHeight, sampleCount[0]);
            }
            renderFBO(gl, rs, targetFboWidth, targetFboHeight, vpWidth, vpHeight, sampleCount[0]);
        }
    }
    private void setTexSize(final GL2ES2 gl, final ShaderState st, boolean firstPass, int width, int height, int sampleCount) {
        if(null == mgl_TextureSize) {
            mgl_TextureSize = new GLUniformData(UniformNames.gcu_TextureSize, 3, Buffers.newDirectFloatBuffer(3));
        }
        final FloatBuffer texSize = (FloatBuffer) mgl_TextureSize.getBuffer();
        texSize.put(0, width);
        texSize.put(1, height);
        if( firstPass ) {
            texSize.put(2, 0f);
        } else {
            texSize.put(2, sampleCount);
        }
        st.uniform(gl, mgl_TextureSize);
    }

    private void renderFBO(final GL2ES2 gl, final RenderState rs, final int targetFboWidth, final int targetFboHeight,
                           final int vpWidth, final int vpHeight, int sampleCount) {
        final ShaderState st = rs.getShaderState();

        gl.glViewport(0, 0, vpWidth, vpHeight);
        st.uniform(gl, mgl_ActiveTexture);
        gl.glActiveTexture(GL.GL_TEXTURE0 + mgl_ActiveTexture.intValue());
        setTexSize(gl, st, false, fboWidth, fboHeight, sampleCount);

        fbo.use(gl, texA);
        verticeFboAttr.enableBuffer(gl, true);
        texCoordFboAttr.enableBuffer(gl, true);
        indicesFbo.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesFbo.getElementCount() * indicesFbo.getComponentCount(), GL2ES2.GL_UNSIGNED_SHORT, 0);

        indicesFbo.bindBuffer(gl, false);
        texCoordFboAttr.enableBuffer(gl, false);
        verticeFboAttr.enableBuffer(gl, false);
        fbo.unuse(gl);

        // setback: gl.glActiveTexture(currentActiveTextureEngine[0]);
    }

    private void renderRegion2FBO(final GL2ES2 gl, final RenderState rs,
                                  final int targetFboWidth, final int targetFboHeight, final int newFboWidth, final int newFboHeight,
                                  final int vpWidth, final int vpHeight, final int sampleCount) {
        final ShaderState st = rs.getShaderState();

        if( 0 >= targetFboWidth || 0 >= targetFboHeight ) {
            throw new IllegalArgumentException("fboSize must be greater than 0: "+targetFboWidth+"x"+targetFboHeight);
        }

        if(null == fbo) {
            fboWidth  = newFboWidth;
            fboHeight  = newFboHeight;
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
        } else {
            fbo.bind(gl);
        }
        setTexSize(gl, st, true, vpWidth, vpHeight, sampleCount);

        //render texture
        gl.glViewport(0, 0, fboWidth, fboHeight);
        st.uniform(gl, mgl_fboPMVMatrix); // use orthogonal matrix

        gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT | GL2ES2.GL_DEPTH_BUFFER_BIT);
        renderRegion(gl);
        fbo.unbind(gl);
        fboDirty = false;

        st.uniform(gl, rs.getPMVMatrix()); // switch back to real PMV matrix
    }

    private void renderRegion(final GL2ES2 gl) {
        verticeTxtAttr.enableBuffer(gl, true);
        texCoordTxtAttr.enableBuffer(gl, true);
        indicesTxtBuffer.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesTxtBuffer.getElementCount() * indicesTxtBuffer.getComponentCount(), GL2ES2.GL_UNSIGNED_SHORT, 0);

        indicesTxtBuffer.bindBuffer(gl, false);
        texCoordTxtAttr.enableBuffer(gl, false);
        verticeTxtAttr.enableBuffer(gl, false);
    }

    @Override
    protected void destroyImpl(final GL2ES2 gl, final RegionRenderer renderer) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegion2PES2 Destroy: " + this);
            // Thread.dumpStack();
        }
        final ShaderState st = renderer.getShaderState();
        if(null != fbo) {
            fbo.destroy(gl);
            fbo = null;
            texA = null;
        }
        if(null != verticeTxtAttr) {
            st.ownAttribute(verticeTxtAttr, false);
            verticeTxtAttr.destroy(gl);
            verticeTxtAttr = null;
        }
        if(null != texCoordTxtAttr) {
            st.ownAttribute(texCoordTxtAttr, false);
            texCoordTxtAttr.destroy(gl);
            texCoordTxtAttr = null;
        }
        if(null != indicesTxtBuffer) {
            indicesTxtBuffer.destroy(gl);
            indicesTxtBuffer = null;
        }
        if(null != verticeFboAttr) {
            st.ownAttribute(verticeFboAttr, false);
            verticeFboAttr.destroy(gl);
            verticeFboAttr = null;
        }
        if(null != texCoordFboAttr) {
            st.ownAttribute(texCoordFboAttr, false);
            texCoordFboAttr.destroy(gl);
            texCoordFboAttr = null;
        }
        if(null != indicesFbo) {
            indicesFbo.destroy(gl);
            indicesFbo = null;
        }
    }
}