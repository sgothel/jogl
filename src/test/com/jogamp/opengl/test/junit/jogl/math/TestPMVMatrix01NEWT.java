/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.math;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import jogamp.common.os.PlatformPropsImpl;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.geom.Frustum;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.PMVMatrix;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPMVMatrix01NEWT extends UITestCase {

    static final float epsilon = 0.00001f;

    // matrix 2 rows x 3 columns - In row major order
    static FloatBuffer matrix2x3R = FloatBuffer.wrap( new float[] {  1.0f,  2.0f,  3.0f,
                                                                     4.0f,  5.0f,  6.0f } );

    // matrix 2 rows x 3 columns - In column major order
    static FloatBuffer matrix2x3C = FloatBuffer.wrap( new float[] {  1.0f,  4.0f,
                                                                     2.0f,  5.0f,
                                                                     3.0f,  6.0f } );

    // matrix 3 rows x 2 columns - In row major order
    static FloatBuffer matrix3x2R = FloatBuffer.wrap( new float[] {  1.0f,  2.0f,
                                                                     3.0f,  4.0f,
                                                                     5.0f,  6.0f  } );

    // matrix 3 rows x 2 columns - In column major order
    static FloatBuffer matrix3x2C = FloatBuffer.wrap( new float[] {  1.0f,  3.0f, 5.0f,
                                                                     2.0f,  4.0f, 6.0f  } );

    // Translated xyz 123 - Row - In row major order !
    static FloatBuffer translated123R = FloatBuffer.wrap( new float[] {  1.0f,  0.0f,  0.0f,  1.0f,
                                                                         0.0f,  1.0f,  0.0f,  2.0f,
                                                                         0.0f,  0.0f,  1.0f,  3.0f,
                                                                         0.0f,  0.0f,  0.0f,  1.0f } );

    // Translated xyz 123 - Column - In column major order !
    static FloatBuffer translated123C = FloatBuffer.wrap( new float[] {  1.0f,  0.0f,  0.0f,  0.0f,
                                                                         0.0f,  1.0f,  0.0f,  0.0f,
                                                                         0.0f,  0.0f,  1.0f,  0.0f,
                                                                         1.0f,  2.0f,  3.0f,  1.0f } );

    // Translated xyz 123 - Inverse - In column major order !
    static FloatBuffer translated123I = FloatBuffer.wrap( new float[] {  1.0f,  0.0f,  0.0f,  0.0f,
                                                                         0.0f,  1.0f,  0.0f,  0.0f,
                                                                         0.0f,  0.0f,  1.0f,  0.0f,
                                                                        -1.0f, -2.0f, -3.0f,  1.0f } );

    // Translated xyz 123 - Inverse and Transposed - In column major order !
    static FloatBuffer translated123IT = FloatBuffer.wrap( new float[] {  1.0f,  0.0f,  0.0f, -1.0f,
                                                                          0.0f,  1.0f,  0.0f, -2.0f,
                                                                          0.0f,  0.0f,  1.0f, -3.0f,
                                                                          0.0f,  0.0f,  0.0f,  1.0f } );

    @Test
    @SuppressWarnings("deprecation")
    public void test00MatrixToString() {
        final String s4x4Cpmv = PMVMatrix.matrixToString(null, "%10.5f", translated123C).toString();
        final String s4x4Cflu = FloatUtil.matrixToString(null, null, "%10.5f", translated123C, 0, 4, 4, false).toString();
        final String s4x4Rflu = FloatUtil.matrixToString(null, null, "%10.5f", translated123R, 0, 4, 4, true).toString();
        System.err.println("PMV-C-O 4x4: ");
        System.err.println(s4x4Cpmv);
        System.err.println();
        System.err.println("FLU-C-O 4x4: ");
        System.err.println(s4x4Cflu);
        System.err.println();
        System.err.println("FLU-R-O 4x4: ");
        System.err.println(s4x4Rflu);
        System.err.println();
        Assert.assertEquals(s4x4Cpmv, s4x4Cflu);
        Assert.assertEquals(s4x4Cflu, s4x4Rflu);

        final String s2x3Rflu = FloatUtil.matrixToString(null, null, "%10.5f", matrix2x3R, 0, 2, 3, true).toString();
        final String s2x3Cflu = FloatUtil.matrixToString(null, null, "%10.5f", matrix2x3C, 0, 2, 3, false).toString();
        System.err.println("FLU-R-O 2x3: ");
        System.err.println(s2x3Rflu);
        System.err.println();
        System.err.println("FLU-C-O 2x3: ");
        System.err.println(s2x3Cflu);
        System.err.println();
        Assert.assertEquals(s2x3Cflu, s2x3Rflu);

        final String s3x2Rflu = FloatUtil.matrixToString(null, null, "%10.5f", matrix3x2R, 0, 3, 2, true).toString();
        final String s3x2Cflu = FloatUtil.matrixToString(null, null, "%10.5f", matrix3x2C, 0, 3, 2, false).toString();
        System.err.println("FLU-R-O 3x2: ");
        System.err.println(s3x2Rflu);
        System.err.println();
        System.err.println("FLU-C-O 3x2: ");
        System.err.println(s3x2Cflu);
        System.err.println();
        Assert.assertEquals(s3x2Cflu, s3x2Rflu);
    }

    /**
     * Test using traditional access workflow, i.e. 1) operation 2) get-matrix references
     * <p>
     * The Mvi, Mvit and Frustum dirty-bits and request-mask will be validated.
     * </p>
     */
    @SuppressWarnings("deprecation")
    @Test
    public void test01MviUpdateTraditionalAccess() {
        FloatBuffer p, mv, mvi, mvit;
        Frustum frustum;
        boolean b;
        final PMVMatrix pmv = new PMVMatrix();
        // System.err.println("P0: "+pmv.toString());

        Assert.assertTrue("Dirty bits clean, "+pmv.toString(), 0 != pmv.getDirtyBits());
        Assert.assertEquals("Remaining dirty bits not Mvi|Mvit|Frustum, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW|PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getDirtyBits());
        Assert.assertEquals("Request bits not zero, "+pmv.toString(), 0, pmv.getRequestMask());

        //
        // Action #0
        //
        final FloatBuffer ident;
        {
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glLoadIdentity();
            ident = pmv.glGetPMatrixf();

            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();
        }
        Assert.assertTrue("Modified bits zero", 0 != pmv.getModifiedBits(true)); // clear & test
        Assert.assertTrue("Dirty bits clean, "+pmv.toString(), 0 != pmv.getDirtyBits());
        Assert.assertEquals("Remaining dirty bits not Mvi|Mvit|Frustum, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW|PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getDirtyBits());
        Assert.assertEquals("Request bits not zero, "+pmv.toString(), 0, pmv.getRequestMask());

        //
        // Action #1
        //
        pmv.glTranslatef(1f, 2f, 3f); // all dirty !
        Assert.assertTrue("Modified bits zero", 0 != pmv.getModifiedBits(true)); // clear & test
        Assert.assertTrue("Dirty bits clean, "+pmv.toString(), 0 != pmv.getDirtyBits());
        Assert.assertEquals("Remaining dirty bits not Mvi|Mvit|Frustum, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW|PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getDirtyBits());
        Assert.assertEquals("Request bits not zero, "+pmv.toString(), 0, pmv.getRequestMask());
        // System.err.println("P1: "+pmv.toString());

        b = pmv.update(); // will not clean dirty bits, since no request has been made -> false
        Assert.assertEquals("Update has been perfomed, but non requested", false, b);
        Assert.assertTrue("Dirty bits clean, "+pmv.toString(), 0 != pmv.getDirtyBits());
        Assert.assertEquals("Remaining dirty bits not Mvi|Mvit|Frustum, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW|PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getDirtyBits());
        Assert.assertEquals("Request bits not zero, "+pmv.toString(), 0, pmv.getRequestMask());
        // System.err.println("P2: "+pmv.toString());

        //
        // Get
        //
        p = pmv.glGetPMatrixf();
        MiscUtils.assertFloatBufferEquals("P not identity, "+pmv.toString(), ident, p, epsilon);
        mv = pmv.glGetMvMatrixf();
        MiscUtils.assertFloatBufferEquals("Mv not translated123, "+pmv.toString(), translated123C, mv, epsilon);
        mvi = pmv.glGetMviMatrixf();
        MiscUtils.assertFloatBufferEquals("Mvi not translated123, "+pmv.toString(), translated123I, mvi, epsilon);
        Assert.assertEquals("Request bit Mvi not set, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW, pmv.getRequestMask());
        Assert.assertEquals("Remaining dirty bits not Mvit|Frustum, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getDirtyBits());

        frustum = pmv.glGetFrustum();
        Assert.assertNotNull("Frustum is null"+pmv.toString(), frustum); // FIXME: Test Frustum value!
        Assert.assertEquals("Remaining dirty bits not Mvit, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW, pmv.getDirtyBits());
        Assert.assertEquals("Request bits Mvi|Frustum not set, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getRequestMask());
        // System.err.println("P3: "+pmv.toString());

        mvit = pmv.glGetMvitMatrixf();
        MiscUtils.assertFloatBufferEquals("Mvit not translated123, "+pmv.toString()+pmv.toString(), translated123IT, mvit, epsilon);
        Assert.assertTrue("Dirty bits not clean, "+pmv.toString(), 0 == pmv.getDirtyBits());
        Assert.assertEquals("Request bits Mvi|Mvit|Frustum not set, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW | PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getRequestMask());
        // System.err.println("P4: "+pmv.toString());

        //
        // Action #2
        //
        pmv.glLoadIdentity(); // all dirty
        Assert.assertTrue("Modified bits zero", 0 != pmv.getModifiedBits(true)); // clear & test
        Assert.assertTrue("Dirty bits clean, "+pmv.toString(), 0 != pmv.getDirtyBits());
        Assert.assertEquals("Remaining dirty bits not Mvi|Mvit|Frustum, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW|PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getDirtyBits());
        Assert.assertEquals("Request bits Mvi|Mvit|Frustum not set, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW | PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getRequestMask());
        MiscUtils.assertFloatBufferEquals("P not identity, "+pmv.toString(), ident, p, epsilon);
        MiscUtils.assertFloatBufferEquals("Mv not identity, "+pmv.toString(), ident, mv, epsilon);
        MiscUtils.assertFloatBufferNotEqual("Mvi already identity w/o update, "+pmv.toString(), ident, mvi, epsilon);
        MiscUtils.assertFloatBufferNotEqual("Mvit already identity w/o update, "+pmv.toString(), ident, mvit, epsilon);
        MiscUtils.assertFloatBufferEquals("Mvi not translated123, "+pmv.toString()+pmv.toString(), translated123I, mvi, epsilon);
        MiscUtils.assertFloatBufferEquals("Mvit not translated123, "+pmv.toString()+pmv.toString(), translated123IT, mvit, epsilon);
        Assert.assertNotNull("Frustum is null"+pmv.toString(), frustum); // FIXME: Test Frustum value!

        b = pmv.update(); // will clean dirty bits, since request has been made -> true
        Assert.assertEquals("Update has not been perfomed, but requested", true, b);
        Assert.assertTrue("Dirty bits not clean, "+pmv.toString(), 0 == pmv.getDirtyBits());
        Assert.assertEquals("Request bits Mvi|Mvit|Frustum not set, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW | PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getRequestMask());
        MiscUtils.assertFloatBufferEquals("Mvi not identity after update, "+pmv.toString(), ident, mvi, epsilon);
        MiscUtils.assertFloatBufferEquals("Mvit not identity after update, "+pmv.toString(), ident, mvit, epsilon);
        Assert.assertNotNull("Frustum is null"+pmv.toString(), frustum); // FIXME: Test Frustum value!
    }

    /**
     * Test using shader access workflow, i.e. 1) get-matrix references 2) operations
     * <p>
     * The Mvi, Mvit and Frustum dirty-bits and request-mask will be validated.
     * </p>
     */
    @SuppressWarnings("deprecation")
    @Test
    public void test02MviUpdateShaderAccess() {
        final FloatBuffer p, mv, mvi, mvit;
        Frustum frustum;
        boolean b;
        final PMVMatrix pmv = new PMVMatrix();
        // System.err.println("P0: "+pmv.toString());

        Assert.assertTrue("Dirty bits clean, "+pmv.toString(), 0 != pmv.getDirtyBits());
        Assert.assertEquals("Remaining dirty bits not Mvi|Mvit|Frustum, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW|PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getDirtyBits());
        Assert.assertEquals("Request bits not zero, "+pmv.toString(), 0, pmv.getRequestMask());

        //
        // Action #0
        //
        final FloatBuffer ident;
        {
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glLoadIdentity();
            ident = pmv.glGetPMatrixf();

            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();
        }
        // System.err.println("P0: "+pmv.toString());
        Assert.assertTrue("Modified bits zero", 0 != pmv.getModifiedBits(true)); // clear & test
        Assert.assertTrue("Dirty bits clean, "+pmv.toString(), 0 != pmv.getDirtyBits());
        Assert.assertEquals("Remaining dirty bits not Mvi|Mvit|Frustum, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW|PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getDirtyBits());
        Assert.assertEquals("Request bits not zero, "+pmv.toString(), 0, pmv.getRequestMask());
        // System.err.println("P1: "+pmv.toString());

        //
        // Get
        //
        p    = pmv.glGetPMatrixf();
        MiscUtils.assertFloatBufferEquals("P not identity, "+pmv.toString(), ident, p, epsilon);
        mv   = pmv.glGetMvMatrixf();
        MiscUtils.assertFloatBufferEquals("Mv not identity, "+pmv.toString(), ident, mv, epsilon);
        Assert.assertTrue("Dirty bits clean, "+pmv.toString(), 0 != pmv.getDirtyBits());
        Assert.assertEquals("Request bits not zero, "+pmv.toString(), 0, pmv.getRequestMask());

        mvi  = pmv.glGetMviMatrixf();
        MiscUtils.assertFloatBufferEquals("Mvi not identity, "+pmv.toString(), ident, mvi, epsilon);
        Assert.assertEquals("Remaining dirty bits not Mvit|Frustum, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getDirtyBits());
        Assert.assertEquals("Request bit Mvi not set, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW, pmv.getRequestMask());

        mvit = pmv.glGetMvitMatrixf();
        MiscUtils.assertFloatBufferEquals("Mvi not identity, "+pmv.toString(), ident, mvit, epsilon);
        Assert.assertEquals("Remaining dirty bits not Frustum, "+pmv.toString(), PMVMatrix.DIRTY_FRUSTUM, pmv.getDirtyBits());
        Assert.assertEquals("Request bits Mvi and Mvit not set, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW | PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW, pmv.getRequestMask());

        frustum = pmv.glGetFrustum();
        Assert.assertNotNull("Frustum is null"+pmv.toString(), frustum); // FIXME: Test Frustum value!
        Assert.assertTrue("Dirty bits not clean, "+pmv.toString(), 0 == pmv.getDirtyBits());
        Assert.assertEquals("Request bits Mvi|Mvit|Frustum not set, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW | PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getRequestMask());

        //
        // Action #1
        //
        pmv.glTranslatef(1f, 2f, 3f); // all dirty !
        Assert.assertTrue("Modified bits zero", 0 != pmv.getModifiedBits(true)); // clear & test
        Assert.assertTrue("Dirty bits clean, "+pmv.toString(), 0 != pmv.getDirtyBits());
        Assert.assertEquals("Remaining dirty bits not Mvi|Mvit|Frustum, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW|PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getDirtyBits());
        MiscUtils.assertFloatBufferEquals("P not identity, "+pmv.toString()+pmv.toString(), ident, p, epsilon);
        MiscUtils.assertFloatBufferEquals("Mv not translated123, "+pmv.toString()+pmv.toString(), translated123C, mv, epsilon);
        MiscUtils.assertFloatBufferNotEqual("Mvi already translated123 w/o update, "+pmv.toString()+pmv.toString(), translated123I, mvi, epsilon);
        MiscUtils.assertFloatBufferNotEqual("Mvit already translated123 w/o update, "+pmv.toString()+pmv.toString(), translated123IT, mvit, epsilon);
        MiscUtils.assertFloatBufferEquals("Mvi not identity, "+pmv.toString()+pmv.toString(), ident, mvi, epsilon);
        MiscUtils.assertFloatBufferEquals("Mvit not identity, "+pmv.toString()+pmv.toString(), ident, mvit, epsilon);
        Assert.assertNotNull("Frustum is null"+pmv.toString(), frustum); // FIXME: Test Frustum value!

        b = pmv.update(); // will clean dirty bits, since all requests has been made -> true
        Assert.assertEquals("Update has not been perfomed, but requested", true, b);
        Assert.assertTrue("Dirty bits not clean, "+pmv.toString(), 0 == pmv.getDirtyBits());
        Assert.assertEquals("Request bits Mvi|Mvit|Frustum not set, "+pmv.toString(), PMVMatrix.DIRTY_INVERSE_MODELVIEW | PMVMatrix.DIRTY_INVERSE_TRANSPOSED_MODELVIEW | PMVMatrix.DIRTY_FRUSTUM, pmv.getRequestMask());
        MiscUtils.assertFloatBufferEquals("Mvi not translated123, "+pmv.toString()+pmv.toString(), translated123I, mvi, epsilon);
        MiscUtils.assertFloatBufferEquals("Mvit not translated123, "+pmv.toString()+pmv.toString(), translated123IT, mvit, epsilon);
        // System.err.println("P2: "+pmv.toString());
    }

    @Test
    public void test03MvTranslate() {
        final FloatBuffer pmvMv;
        // final FloatBuffer pmvMvi, pmvMvit;
        {
            final PMVMatrix pmv = new PMVMatrix();
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glLoadIdentity();
            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();
            pmv.glTranslatef(5f, 6f, 7f);

            pmvMv = pmv.glGetMvMatrixf();
            // pmvMvi = pmv.glGetMviMatrixf();
            // pmvMvit = pmv.glGetMvitMatrixf();
        }

        final FloatBuffer glMv = FloatBuffer.allocate(16);
        {
            final GL2ES1 gl = dc.glc.getGL().getGL2ES1();
            gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(5f, 6f, 7f);

            gl.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, glMv);
        }
        // System.err.println(PMVMatrix.matrixToString(null, "%10.5f", glMv, pmvMv).toString());

        MiscUtils.assertFloatBufferEquals("Arrays not equal, expected"+PlatformPropsImpl.NEWLINE+PMVMatrix.matrixToString(null, "%10.5f", glMv).toString()+
                ", actual"+PlatformPropsImpl.NEWLINE+PMVMatrix.matrixToString(null, "%10.5f", pmvMv).toString(),
                glMv, pmvMv, epsilon);

        // System.err.println("pmvMvi:  "+Platform.NEWLINE+PMVMatrix.matrixToString(null, "%10.5f", pmvMvi));
        // System.err.println("pmvMvit: "+Platform.NEWLINE+PMVMatrix.matrixToString(null, "%10.5f", pmvMvit));
    }

    @Test
    public void test04MvTranslateRotate() {
        final FloatBuffer pmvMv;
        // final FloatBuffer pmvMvi, pmvMvit;
        {
            final PMVMatrix pmv = new PMVMatrix();
            pmv.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            pmv.glLoadIdentity();
            pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            pmv.glLoadIdentity();
            pmv.glTranslatef(5f, 6f, 7f);
            pmv.glRotatef(90f, 1f, 0f, 0f);

            pmvMv = pmv.glGetMvMatrixf();
            // pmvMvi = pmv.glGetMviMatrixf();
            // pmvMvit = pmv.glGetMvitMatrixf();
        }

        final FloatBuffer glMv = FloatBuffer.allocate(16);
        {
            final GL2ES1 gl = dc.glc.getGL().getGL2ES1();
            gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(5f, 6f, 7f);
            gl.glRotatef(90f, 1f, 0f, 0f);

            gl.glGetFloatv(GLMatrixFunc.GL_MODELVIEW_MATRIX, glMv);
        }
        // System.err.println(PMVMatrix.matrixToString(null, "%10.5f", glMv, pmvMv).toString());

        MiscUtils.assertFloatBufferEquals("Arrays not equal, expected"+PlatformPropsImpl.NEWLINE+PMVMatrix.matrixToString(null, "%10.5f", glMv).toString()+
                ", actual"+PlatformPropsImpl.NEWLINE+PMVMatrix.matrixToString(null, "%10.5f", pmvMv).toString(),
                glMv, pmvMv, epsilon);

        // System.err.println("pmvMvi:  "+Platform.NEWLINE+PMVMatrix.matrixToString(null, "%10.5f", pmvMvi));
        // System.err.println("pmvMvit: "+Platform.NEWLINE+PMVMatrix.matrixToString(null, "%10.5f", pmvMvit));
    }

    static DrawableContext dc;

    @BeforeClass
    public static void setup() throws Throwable {
        try {
            dc = createOffscreenDrawableAndCurrentFFPContext();
        } catch (final Throwable t) {
            setTestSupported(false);
            throw t;
        }
    }

    @AfterClass
    public static void cleanup() {
        destroyDrawableContext(dc);
    }

    static class DrawableContext {
        DrawableContext(final GLDrawable d, final GLContext glc) {
            this.d = d;
            this.glc = glc;
        }
        GLDrawable d;
        GLContext glc;
    }

    private static DrawableContext createOffscreenDrawableAndCurrentFFPContext() throws Throwable {
        final GLProfile glp = GLProfile.getMaxFixedFunc(true);
        final GLCapabilities glCaps = new GLCapabilities(glp);
        glCaps.setOnscreen(false);
        glCaps.setPBuffer(true);
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
        final GLDrawable d = factory.createOffscreenDrawable(null, glCaps, null, 64, 64);
        d.setRealized(true);
        GLContext glc = null;
        try {
            glc = d.createContext(null);
            Assert.assertTrue("Context could not be made current", GLContext.CONTEXT_NOT_CURRENT < glc.makeCurrent());
            return new DrawableContext(d, glc);
        } catch (final Throwable t) {
            if(null != glc) {
                glc.destroy();
            }
            d.setRealized(false);
            throw t;
        }
    }

    private static void destroyDrawableContext(final DrawableContext dc) {
        if(null != dc.glc) {
            dc.glc.destroy();
            dc.glc = null;
        }
        if(null != dc.d) {
            dc.d.setRealized(false);
            dc.d = null;
        }
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestPMVMatrix01NEWT.class.getName());
    }
}
