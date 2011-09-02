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

import javax.media.opengl.GL;

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
    
    public GLArrayDataServer frontFace;
    public GLArrayDataServer frontSide;
    public GLArrayDataServer backFace;
    public GLArrayDataServer backSide;
    public GLArrayDataServer outwardFace;
    public GLArrayDataServer insideRadiusCyl;
    public boolean isShared;

    public abstract GLArrayDataServer createInterleaved(int comps, int dataType, boolean normalized, int initialSize, int vboUsage);
    public abstract void addInterleavedVertexAndNormalArrays(GLArrayDataServer array, int components);
    public abstract void draw(GL gl, float x, float y, float angle, FloatBuffer color);
    
    public void destroy(GL gl) {
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
    
    public GearsObject ( GearsObject shared ) {
        isShared = true;
        frontFace = shared.frontFace;
        frontSide = shared.frontSide;
        backFace = shared.backFace;
        backSide = shared.backSide;
        outwardFace = shared.outwardFace;
        insideRadiusCyl = shared.insideRadiusCyl;
    }
            
    public GearsObject (
            float inner_radius,
            float outer_radius,
            float width,
            int teeth,
            float tooth_depth)
    {
        final float dz = width * 0.5f; 
        int i;
        float r0, r1, r2;
        float angle, da;
        float u, v, len;
        float s[] = new float[5];
        float c[] = new float[5];
        float normal[] = new float[3];
        // final int tris_per_tooth = 32;

        isShared = false;
        
        r0 = inner_radius;
        r1 = outer_radius - tooth_depth / 2.0f;
        r2 = outer_radius + tooth_depth / 2.0f;

        da = 2.0f * (float) Math.PI / teeth / 4.0f;

        s[4] = 0; // sin(0f)
        c[4] = 1; // cos(0f)

        frontFace = createInterleaved(6, GL.GL_FLOAT, false, 4*teeth+2, GL.GL_STATIC_DRAW);
        addInterleavedVertexAndNormalArrays(frontFace, 3);
        backFace = createInterleaved(6, GL.GL_FLOAT, false, 4*teeth+2, GL.GL_STATIC_DRAW);
        addInterleavedVertexAndNormalArrays(backFace, 3);
        frontSide = createInterleaved(6, GL.GL_FLOAT, false, 6*teeth, GL.GL_STATIC_DRAW);
        addInterleavedVertexAndNormalArrays(frontSide, 3);
        backSide = createInterleaved(6, GL.GL_FLOAT, false, 6*teeth, GL.GL_STATIC_DRAW);
        addInterleavedVertexAndNormalArrays(backSide, 3);
        outwardFace = createInterleaved(6, GL.GL_FLOAT, false, 4*4*teeth+2, GL.GL_STATIC_DRAW);
        addInterleavedVertexAndNormalArrays(outwardFace, 3);
        insideRadiusCyl = createInterleaved(6, GL.GL_FLOAT, false, 2*teeth+2, GL.GL_STATIC_DRAW);
        addInterleavedVertexAndNormalArrays(insideRadiusCyl, 3);

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
    }

    static void vert(GLArrayDataServer array, float x, float y, float z, float n[]) {
        array.putf(x);
        array.putf(y);
        array.putf(z);
        array.putf(n[0]);
        array.putf(n[1]);
        array.putf(n[2]);
    }
    
    static void sincos(float x, float sin[], int sinIdx, float cos[], int cosIdx) {
        sin[sinIdx] = (float) Math.sin(x);
        cos[cosIdx] = (float) Math.cos(x);
    }
}
