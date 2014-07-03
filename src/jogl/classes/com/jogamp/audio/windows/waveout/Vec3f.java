/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.jogamp.audio.windows.waveout;

/** 3-element single-precision vector */

class Vec3f {
  public static final Vec3f X_AXIS     = new Vec3f( 1,  0,  0);
  public static final Vec3f Y_AXIS     = new Vec3f( 0,  1,  0);
  public static final Vec3f Z_AXIS     = new Vec3f( 0,  0,  1);
  public static final Vec3f NEG_X_AXIS = new Vec3f(-1,  0,  0);
  public static final Vec3f NEG_Y_AXIS = new Vec3f( 0, -1,  0);
  public static final Vec3f NEG_Z_AXIS = new Vec3f( 0,  0, -1);

  private float x;
  private float y;
  private float z;

  public Vec3f() {}

  public Vec3f(final Vec3f arg) {
    set(arg);
  }

  public Vec3f(final float x, final float y, final float z) {
    set(x, y, z);
  }

  public Vec3f copy() {
    return new Vec3f(this);
  }

  public void set(final Vec3f arg) {
    set(arg.x, arg.y, arg.z);
  }

  public void set(final float x, final float y, final float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /** Sets the ith component, 0 <= i < 3 */
  public void set(final int i, final float val) {
    switch (i) {
    case 0: x = val; break;
    case 1: y = val; break;
    case 2: z = val; break;
    default: throw new IndexOutOfBoundsException();
    }
  }

  /** Gets the ith component, 0 <= i < 3 */
  public float get(final int i) {
    switch (i) {
    case 0: return x;
    case 1: return y;
    case 2: return z;
    default: throw new IndexOutOfBoundsException();
    }
  }

  public float x() { return x; }
  public float y() { return y; }
  public float z() { return z; }

  public void setX(final float x) { this.x = x; }
  public void setY(final float y) { this.y = y; }
  public void setZ(final float z) { this.z = z; }

  public float dot(final Vec3f arg) {
    return x * arg.x + y * arg.y + z * arg.z;
  }

  public float length() {
    return (float) Math.sqrt(lengthSquared());
  }

  public float lengthSquared() {
    return this.dot(this);
  }

  public void normalize() {
    final float len = length();
    if (len == 0.0f) return;
    scale(1.0f / len);
  }

  /** Returns this * val; creates new vector */
  public Vec3f times(final float val) {
    final Vec3f tmp = new Vec3f(this);
    tmp.scale(val);
    return tmp;
  }

  /** this = this * val */
  public void scale(final float val) {
    x *= val;
    y *= val;
    z *= val;
  }

  /** Returns this + arg; creates new vector */
  public Vec3f plus(final Vec3f arg) {
    final Vec3f tmp = new Vec3f();
    tmp.add(this, arg);
    return tmp;
  }

  /** this = this + b */
  public void add(final Vec3f b) {
    add(this, b);
  }

  /** this = a + b */
  public void add(final Vec3f a, final Vec3f b) {
    x = a.x + b.x;
    y = a.y + b.y;
    z = a.z + b.z;
  }

  /** Returns this + s * arg; creates new vector */
  public Vec3f addScaled(final float s, final Vec3f arg) {
    final Vec3f tmp = new Vec3f();
    tmp.addScaled(this, s, arg);
    return tmp;
  }

  /** this = a + s * b */
  public void addScaled(final Vec3f a, final float s, final Vec3f b) {
    x = a.x + s * b.x;
    y = a.y + s * b.y;
    z = a.z + s * b.z;
  }

  /** Returns this - arg; creates new vector */
  public Vec3f minus(final Vec3f arg) {
    final Vec3f tmp = new Vec3f();
    tmp.sub(this, arg);
    return tmp;
  }

  /** this = this - b */
  public void sub(final Vec3f b) {
    sub(this, b);
  }

  /** this = a - b */
  public void sub(final Vec3f a, final Vec3f b) {
    x = a.x - b.x;
    y = a.y - b.y;
    z = a.z - b.z;
  }

  /** Returns this cross arg; creates new vector */
  public Vec3f cross(final Vec3f arg) {
    final Vec3f tmp = new Vec3f();
    tmp.cross(this, arg);
    return tmp;
  }

  /** this = a cross b. NOTE: "this" must be a different vector than
      both a and b. */
  public void cross(final Vec3f a, final Vec3f b) {
    x = a.y * b.z - a.z * b.y;
    y = a.z * b.x - a.x * b.z;
    z = a.x * b.y - a.y * b.x;
  }

  /** Sets each component of this vector to the product of the
      component with the corresponding component of the argument
      vector. */
  public void componentMul(final Vec3f arg) {
    x *= arg.x;
    y *= arg.y;
    z *= arg.z;
  }

  @Override
  public String toString() {
    return "(" + x + ", " + y + ", " + z + ")";
  }
}
