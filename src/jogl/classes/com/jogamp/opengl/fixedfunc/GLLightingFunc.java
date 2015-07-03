/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.jogamp.opengl.fixedfunc;

public interface GLLightingFunc {
  public static final int GL_LIGHT0 = 0x4000;
  public static final int GL_LIGHT1 = 0x4001;
  public static final int GL_LIGHT2 = 0x4002;
  public static final int GL_LIGHT3 = 0x4003;
  public static final int GL_LIGHT4 = 0x4004;
  public static final int GL_LIGHT5 = 0x4005;
  public static final int GL_LIGHT6 = 0x4006;
  public static final int GL_LIGHT7 = 0x4007;
  public static final int GL_LIGHTING = 0xB50;
  public static final int GL_AMBIENT = 0x1200;
  public static final int GL_DIFFUSE = 0x1201;
  public static final int GL_SPECULAR = 0x1202;
  public static final int GL_POSITION = 0x1203;
  public static final int GL_SPOT_DIRECTION = 0x1204;
  public static final int GL_SPOT_EXPONENT = 0x1205;
  public static final int GL_SPOT_CUTOFF = 0x1206;
  public static final int GL_CONSTANT_ATTENUATION = 0x1207;
  public static final int GL_LINEAR_ATTENUATION = 0x1208;
  public static final int GL_QUADRATIC_ATTENUATION = 0x1209;
  public static final int GL_EMISSION = 0x1600;
  public static final int GL_SHININESS = 0x1601;
  public static final int GL_AMBIENT_AND_DIFFUSE = 0x1602;
  public static final int GL_COLOR_MATERIAL = 0xB57;
  public static final int GL_NORMALIZE = 0xBA1;

  public static final int GL_FLAT = 0x1D00;
  public static final int GL_SMOOTH = 0x1D01;

  public void glLightfv(int light, int pname, java.nio.FloatBuffer params);
  public void glLightfv(int light, int pname, float[] params, int params_offset);
  public void glMaterialf(int face, int pname, float param);
  public void glMaterialfv(int face, int pname, java.nio.FloatBuffer params);
  public void glMaterialfv(int face, int pname, float[] params, int params_offset);
  public void glColor4f(float red, float green, float blue, float alpha);
  public void glShadeModel(int mode);

}

