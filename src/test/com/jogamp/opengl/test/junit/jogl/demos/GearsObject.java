/**
 * Copyright (C) 2011 JogAmp Community. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * BRIAN PAUL BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.jogamp.opengl.test.junit.jogl.demos;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.GLArrayDataServer;

/**
 * GearsObject.java <BR>
 * @author Brian Paul (converted to Java by Ron Cemer and Sven Gothel) <P>
 */
public abstract class GearsObject {
    public static final FloatBuffer red = Buffers.newDirectFloatBuffer( new float[] { 0.8f, 0.1f, 0.0f, 0.7f } );
    public static final FloatBuffer green = Buffers.newDirectFloatBuffer( new float[] { 0.0f, 0.8f, 0.2f, 0.7f } );
    public static final FloatBuffer blue = Buffers.newDirectFloatBuffer( new float[] { 0.2f, 0.2f, 1.0f, 0.7f } );
    public static final float M_PI = (float)Math.PI;

    public final FloatBuffer gearColor;
    public GLArrayDataServer frontFace;
    public GLArrayDataServer frontSide;
    public GLArrayDataServer backFace;
    public GLArrayDataServer backSide;
    public GLArrayDataServer outwardFace;
    public GLArrayDataServer insideRadiusCyl;
    public boolean isShared;
    protected boolean validateBuffers = false;

    public abstract GLArrayDataServer createInterleaved(boolean useMappedBuffers, int comps, int dataType, boolean normalized, int initialSize, int vboUsage);
    public abstract void addInterleavedVertexAndNormalArrays(GLArrayDataServer array, int components);
    public abstract void draw(GL gl, float x, float y, float angle);

    private GLArrayDataServer createInterleavedClone(final GLArrayDataServer ads) {
      final GLArrayDataServer n = new GLArrayDataServer(ads);
      n.setInterleavedOffset(0);
      return n;
    }

    private void init(final GL gl, final GLArrayDataServer array) {
        array.enableBuffer(gl, true);
        array.enableBuffer(gl, false);
    }

    public void destroy(final GL gl) {
        if(!isShared) {
            // could be already destroyed by shared configuration
            if(null != frontFace) {
                frontFace.destroy(gl);
            }
            if(null != frontSide) {
                frontSide.destroy(gl);
            }
            if(null != backFace) {
                backFace.destroy(gl);
            }
            if(null != backSide) {
                backSide.destroy(gl);
            }
            if(null != outwardFace) {
                outwardFace.destroy(gl);
            }
            if(null != insideRadiusCyl) {
                insideRadiusCyl.destroy(gl);
            }
        }
        frontFace=null;
        frontSide=null;
        backFace=null;
        backSide=null;
        outwardFace=null;
        insideRadiusCyl=null;
        isShared = false;
    }

    public GearsObject ( final GearsObject shared ) {
        isShared = true;
        validateBuffers = shared.validateBuffers;
        frontFace = createInterleavedClone(shared.frontFace);
        addInterleavedVertexAndNormalArrays(frontFace, 3);
        backFace = createInterleavedClone(shared.backFace);
        addInterleavedVertexAndNormalArrays(backFace, 3);
        frontSide = createInterleavedClone(shared.frontSide);
        addInterleavedVertexAndNormalArrays(frontSide, 3);
        backSide= createInterleavedClone(shared.backSide);
        addInterleavedVertexAndNormalArrays(backSide, 3);
        outwardFace = createInterleavedClone(shared.outwardFace);
        addInterleavedVertexAndNormalArrays(outwardFace, 3);
        insideRadiusCyl = createInterleavedClone(shared.insideRadiusCyl);
        addInterleavedVertexAndNormalArrays(insideRadiusCyl, 3);
        gearColor = shared.gearColor;
    }

    public GearsObject (
            final GL gl,
            final boolean useMappedBuffers,
            final FloatBuffer gearColor,
            final float inner_radius,
            final float outer_radius,
            final float width, final int teeth, final float tooth_depth, final boolean validateBuffers)
    {
        final float dz = width * 0.5f;
        int i;
        float r0, r1, r2;
        float angle, da;
        float u, v, len;
        final float s[] = new float[5];
        final float c[] = new float[5];
        final float normal[] = new float[3];
        // final int tris_per_tooth = 32;

        this.validateBuffers = validateBuffers;
        this.isShared = false;
        this.gearColor = gearColor;

        r0 = inner_radius;
        r1 = outer_radius - tooth_depth / 2.0f;
        r2 = outer_radius + tooth_depth / 2.0f;

        da = 2.0f * (float) Math.PI / teeth / 4.0f;

        s[4] = 0; // sin(0f)
        c[4] = 1; // cos(0f)

        final int vboUsage = GL.GL_STATIC_DRAW;

        frontFace = createInterleaved(useMappedBuffers, 6, GL.GL_FLOAT, false, 4*teeth+2, vboUsage);
        addInterleavedVertexAndNormalArrays(frontFace, 3);
        backFace = createInterleaved(useMappedBuffers, 6, GL.GL_FLOAT, false, 4*teeth+2, vboUsage);
        addInterleavedVertexAndNormalArrays(backFace, 3);
        frontSide = createInterleaved(useMappedBuffers, 6, GL.GL_FLOAT, false, 6*teeth, vboUsage);
        addInterleavedVertexAndNormalArrays(frontSide, 3);
        backSide = createInterleaved(useMappedBuffers, 6, GL.GL_FLOAT, false, 6*teeth, vboUsage);
        addInterleavedVertexAndNormalArrays(backSide, 3);
        outwardFace = createInterleaved(useMappedBuffers, 6, GL.GL_FLOAT, false, 4*4*teeth+2, vboUsage);
        addInterleavedVertexAndNormalArrays(outwardFace, 3);
        insideRadiusCyl = createInterleaved(useMappedBuffers, 6, GL.GL_FLOAT, false, 2*teeth+2, vboUsage);
        addInterleavedVertexAndNormalArrays(insideRadiusCyl, 3);

        if( useMappedBuffers ) {
            frontFace.mapStorage(gl, GL.GL_WRITE_ONLY);
            backFace.mapStorage(gl, GL.GL_WRITE_ONLY);
            frontSide.mapStorage(gl, GL.GL_WRITE_ONLY);
            backSide.mapStorage(gl, GL.GL_WRITE_ONLY);
            outwardFace.mapStorage(gl, GL.GL_WRITE_ONLY);
            insideRadiusCyl.mapStorage(gl, GL.GL_WRITE_ONLY);
        }

        for (i = 0; i < teeth; i++) {
            angle = i * 2.0f * M_PI / teeth;
            sincos(angle + da * 0f, s, 0, c, 0);
            sincos(angle + da * 1f, s, 1, c, 1);
            sincos(angle + da * 2f, s, 2, c, 2);
            sincos(angle + da * 3f, s, 3, c, 3);

            /* front  */
            normal[0] = 0.0f;
            normal[1] = 0.0f;
            normal[2] = 1.0f;

            /* front face - GL.GL_TRIANGLE_STRIP */
            vert(frontFace, r0 * c[0], r0 * s[0], dz, normal);
            vert(frontFace, r1 * c[0], r1 * s[0], dz, normal);
            vert(frontFace, r0 * c[0], r0 * s[0], dz, normal);
            vert(frontFace, r1 * c[3], r1 * s[3], dz, normal);

            /* front sides of teeth - GL.GL_TRIANGLES */
            vert(frontSide, r1 * c[0], r1 * s[0], dz, normal);
            vert(frontSide, r2 * c[1], r2 * s[1], dz, normal);
            vert(frontSide, r2 * c[2], r2 * s[2], dz, normal);
            vert(frontSide, r1 * c[0], r1 * s[0], dz, normal);
            vert(frontSide, r2 * c[2], r2 * s[2], dz, normal);
            vert(frontSide, r1 * c[3], r1 * s[3], dz, normal);

            /* back */
            normal[0] = 0.0f;
            normal[1] = 0.0f;
            normal[2] = -1.0f;

            /* back face - GL.GL_TRIANGLE_STRIP */
            vert(backFace, r1 * c[0], r1 * s[0], -dz, normal);
            vert(backFace, r0 * c[0], r0 * s[0], -dz, normal);
            vert(backFace, r1 * c[3], r1 * s[3], -dz, normal);
            vert(backFace, r0 * c[0], r0 * s[0], -dz, normal);

            /* back sides of teeth - GL.GL_TRIANGLES*/
            vert(backSide, r1 * c[3], r1 * s[3], -dz, normal);
            vert(backSide, r2 * c[2], r2 * s[2], -dz, normal);
            vert(backSide, r2 * c[1], r2 * s[1], -dz, normal);
            vert(backSide, r1 * c[3], r1 * s[3], -dz, normal);
            vert(backSide, r2 * c[1], r2 * s[1], -dz, normal);
            vert(backSide, r1 * c[0], r1 * s[0], -dz, normal);

            /* outward faces of teeth */
            u = r2 * c[1] - r1 * c[0];
            v = r2 * s[1] - r1 * s[0];
            len = (float)Math.sqrt(u * u + v * v);
            u /= len;
            v /= len;
            normal[0] =    v;
            normal[1] =   -u;
            normal[2] = 0.0f;

            vert(outwardFace, r1 * c[0], r1 * s[0],  dz, normal);
            vert(outwardFace, r1 * c[0], r1 * s[0], -dz, normal);
            vert(outwardFace, r2 * c[1], r2 * s[1],  dz, normal);
            vert(outwardFace, r2 * c[1], r2 * s[1], -dz, normal);

            normal[0] = c[0];
            normal[1] = s[0];
            vert(outwardFace, r2 * c[1], r2 * s[1],  dz, normal);
            vert(outwardFace, r2 * c[1], r2 * s[1], -dz, normal);
            vert(outwardFace, r2 * c[2], r2 * s[2],  dz, normal);
            vert(outwardFace, r2 * c[2], r2 * s[2], -dz, normal);

            normal[0] = ( r1 * s[3] - r2 * s[2] );
            normal[1] = ( r1 * c[3] - r2 * c[2] ) * -1.0f ;
            vert(outwardFace, r2 * c[2], r2 * s[2],  dz, normal);
            vert(outwardFace, r2 * c[2], r2 * s[2], -dz, normal);
            vert(outwardFace, r1 * c[3], r1 * s[3],  dz, normal);
            vert(outwardFace, r1 * c[3], r1 * s[3], -dz, normal);

            normal[0] = c[0];
            normal[1] = s[0];
            vert(outwardFace, r1 * c[3], r1 * s[3],  dz, normal);
            vert(outwardFace, r1 * c[3], r1 * s[3], -dz, normal);
            vert(outwardFace, r1 * c[0], r1 * s[0],  dz, normal);
            vert(outwardFace, r1 * c[0], r1 * s[0], -dz, normal);

            /* inside radius cylinder */
            normal[0] = c[0] * -1.0f;
            normal[1] = s[0] * -1.0f;
            normal[2] = 0.0f;
            vert(insideRadiusCyl, r0 * c[0], r0 * s[0], -dz, normal);
            vert(insideRadiusCyl, r0 * c[0], r0 * s[0],  dz, normal);
        }
        /* finish front face */
        normal[0] = 0.0f;
        normal[1] = 0.0f;
        normal[2] = 1.0f;
        vert(frontFace, r0 * c[4], r0 * s[4], dz, normal);
        vert(frontFace, r1 * c[4], r1 * s[4], dz, normal);
        frontFace.seal(true);

        /* finish back face */
        normal[2] = -1.0f;
        vert(backFace, r1 * c[4], r1 * s[4], -dz, normal);
        vert(backFace, r0 * c[4], r0 * s[4], -dz, normal);
        backFace.seal(true);

        backSide.seal(true);
        frontSide.seal(true);

        /* finish outward face */
        sincos(da * 1f, s, 1, c, 1);
        u = r2 * c[1] - r1 * c[4];
        v = r2 * s[1] - r1 * s[4];
        len = (float)Math.sqrt(u * u + v * v);
        u /= len;
        v /= len;
        normal[0] =    v;
        normal[1] =   -u;
        normal[2] = 0.0f;
        vert(outwardFace, r1 * c[4], r1 * s[4],  dz, normal);
        vert(outwardFace, r1 * c[4], r1 * s[4], -dz, normal);
        outwardFace.seal(true);

        /* finish inside radius cylinder */
        normal[0] = c[4] * -1.0f;
        normal[1] = s[4] * -1.0f;
        normal[2] = 0.0f;
        vert(insideRadiusCyl, r0 * c[4], r0 * s[4], -dz, normal);
        vert(insideRadiusCyl, r0 * c[4], r0 * s[4],  dz, normal);
        insideRadiusCyl.seal(true);

        if( useMappedBuffers ) {
            frontFace.unmapStorage(gl);
            backFace.unmapStorage(gl);
            frontSide.unmapStorage(gl);
            backSide.unmapStorage(gl);
            outwardFace.unmapStorage(gl);
            insideRadiusCyl.unmapStorage(gl);
        } else {
            /** Init VBO and data .. */
            init(gl, frontFace);
            init(gl, frontSide);
            init(gl, backFace);
            init(gl, backSide);
            init(gl, outwardFace);
            init(gl, insideRadiusCyl);
        }
    }

    @Override
    public String toString() {
        final int ffVBO = null != frontFace ? frontFace.getVBOName() : 0;
        final int fsVBO = null != frontSide ? frontSide.getVBOName() : 0;
        final int bfVBO = null != backFace ? backFace.getVBOName() : 0;
        final int bsVBO = null != backSide ? backSide.getVBOName() : 0;
        return "GearsObj[0x"+Integer.toHexString(hashCode())+", vbo ff "+ffVBO+", fs "+fsVBO+", bf "+bfVBO+", bs "+bsVBO+"]";
    }

    static void vert(final GLArrayDataServer array, final float x, final float y, final float z, final float n[]) {
        array.putf(x);
        array.putf(y);
        array.putf(z);
        array.putf(n[0]);
        array.putf(n[1]);
        array.putf(n[2]);
    }

    static void sincos(final float x, final float sin[], final int sinIdx, final float cos[], final int cosIdx) {
        sin[sinIdx] = (float) Math.sin(x);
        cos[cosIdx] = (float) Math.cos(x);
    }
}
