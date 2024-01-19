/**
 * Copyright 2024 JogAmp Community. All rights reserved.
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
package com.jogamp.math.geom;

import com.jogamp.math.Matrix4f;
import com.jogamp.math.Vec3f;

/**
 * Simple 8-point {@link Vec3f} cube compound having {@code z-far <= z-near}
 * <p>
 * 8-points from far to near (z), left to right (x) and bottom to top (y) in AABB case, otherwise arbitrary
 * <ul>
 *   <li>lbf, rbf, rtf, ltf</li>
 *   <li>lbn, rbn, rtn, ltn</li>
 * </ul>
 * </p>
 * <p>
 * A cube can be used to transform an {@link AABBox}
 * from object-space to e.g. model-view (Mv) space via {@link #Cube(AABBox)} and {@link #transform(Matrix4f)}
 * as required for an Mv {@link Frustum} presentation, see {@link #updateFrustumPlanes(Frustum)}.
 * </p>
 */
public class Cube {
    /** left -bottom-far (xyz) in AABB case, otherwise arbitrary */
    public final Vec3f lbf;
    /** right-bottom-far (xyz) in AABB case, otherwise arbitrary */
    public final Vec3f rbf;
    /** right-top   -far (xyz) in AABB case, otherwise arbitrary */
    public final Vec3f rtf;
    /** left -top   -far (xyz) in AABB case, otherwise arbitrary */
    public final Vec3f ltf;

    /** left -bottom-near (xyz) in AABB case, otherwise arbitrary */
    public final Vec3f lbn;
    /** right-bottom-near (xyz) in AABB case, otherwise arbitrary */
    public final Vec3f rbn;
    /** right-top   -near (xyz) in AABB case, otherwise arbitrary */
    public final Vec3f rtn;
    /** left -top   -near (xyz) in AABB case, otherwise arbitrary */
    public final Vec3f ltn;

    @Override
    public final String toString() {
        return "[lbf "+lbf+", rbf "+rbf+", rtf "+rtf+", lbn "+lbn+", rbn "+rbn+", rtn "+rtn+", ltn "+ltn+"]";
    }

    /**
     * Construct a {@link Cube} with all points set to zero.
     */
    public Cube() {
        lbf = new Vec3f();
        rbf = new Vec3f();
        rtf = new Vec3f();
        ltf = new Vec3f();

        lbn = new Vec3f();
        rbn = new Vec3f();
        rtn = new Vec3f();
        ltn = new Vec3f();
    }

    /** Copy construct for a {@link Cube}. */
    public Cube(final Cube o) {
        lbf = new Vec3f(o.lbf);
        rbf = new Vec3f(o.rbf);
        rtf = new Vec3f(o.rtf);
        ltf = new Vec3f(o.ltf);

        lbn = new Vec3f(o.lbn);
        rbn = new Vec3f(o.rbn);
        rtn = new Vec3f(o.rtn);
        ltn = new Vec3f(o.ltn);
    }

    /** Construct a {@link Cube} with given {@link AABBox}. */
    public Cube(final AABBox box) {
        this( box.getLow(), box.getHigh());
    }

    /**
     * Construct a {@link Cube} with given {@link AABBox} minimum and maximum.
     * @param lo_lbf minimum left -bottom-far (xyz)
     * @param hi_rtn maximum right-top   -near (xyz)
     */
    public Cube(final Vec3f lo_lbf, final Vec3f hi_rtn) {
        lbf = new Vec3f(lo_lbf);
        rtn = new Vec3f(hi_rtn);

        rbf = new Vec3f(rtn.x(), lbf.y(), lbf.z());
        rtf = new Vec3f(rtn.x(), rtn.y(), lbf.z());
        ltf = new Vec3f(lbf.x(), rtn.y(), lbf.z());

        lbn = new Vec3f(lbf.x(), lbf.y(), rtn.z());
        rbn = new Vec3f(rtn.x(), lbf.y(), rtn.z());
        ltn = new Vec3f(lbf.x(), rtn.y(), rtn.z());
    }

    /**
     * Setting this cube to given {@link AABBox} minimum and maximum.
     */
    public Cube set(final AABBox box) {
        return set( box.getLow(), box.getHigh());
    }

    /**
     * Setting this cube to given {@link AABBox} minimum and maximum.
     * @param lo_lbf minimum left -bottom-far (xyz)
     * @param hi_rtn maximum right-top   -near (xyz)
     */
    public Cube set(final Vec3f lo_lbf, final Vec3f hi_rtn) {
        lbf.set(lo_lbf);
        rtn.set(hi_rtn);

        rbf.set(rtn.x(), lbf.y(), lbf.z());
        rtf.set(rtn.x(), rtn.y(), lbf.z());
        ltf.set(lbf.x(), rtn.y(), lbf.z());

        lbn.set(lbf.x(), lbf.y(), rtn.z());
        rbn.set(rtn.x(), lbf.y(), rtn.z());
        ltn.set(lbf.x(), rtn.y(), rtn.z());
        return this;
    }

    public Cube set(final Cube o) {
        lbf.set(o.lbf);
        rbf.set(o.rbf);
        rtf.set(o.rtf);
        ltf.set(o.ltf);

        lbn.set(o.lbn);
        rbn.set(o.rbn);
        rtn.set(o.rtn);
        ltn.set(o.ltn);
        return this;
    }

    /** Affine 3f-vector transformation of all 8-points with given matrix, {@link Matrix4f#mulVec3f(Vec3f)}. */
    public Cube transform(final Matrix4f mat) {
        mat.mulVec3f(lbf);
        mat.mulVec3f(rbf);
        mat.mulVec3f(rtf);
        mat.mulVec3f(ltf);

        mat.mulVec3f(lbn);
        mat.mulVec3f(rbn);
        mat.mulVec3f(rtn);
        mat.mulVec3f(ltn);
        return this;
    }

    /**
     * Calculate the frustum planes using this {@link Cube}.
     * <p>
     * One useful application is to {@link Cube#transform(Matrix4f) transform}
     * an {@link AABBox}, see {@link Cube#Cube(AABBox)} from its object-space
     * into model-view (Mv) and produce the {@link Frustum} planes using this method
     * for CPU side object culling and GPU shader side fragment clipping.
     * </p>
     * <p>
     * Frustum plane's normals will point to the inside of the viewing frustum,
     * as required by the {@link Frustum} class.
     * </p>
     * @param frustum the output frustum
     * @return the output frustum for chaining
     * @see Frustum#updateFrustumPlanes(Cube)
     * @see Cube#Cube(AABBox)
     * @see Cube#transform(Matrix4f)
     */
    public Frustum updateFrustumPlanes(final Frustum frustum) {
        // n [  0.0 /  0.0 / -1.0 ]
        frustum.getPlanes()[Frustum.NEAR].set(
                (lbf).minus(lbn).normalize(), // | lbf - lbn |, inwards
                lbn );   // closest AABB point to origin on plane

        // n [  0.0 /  0.0 /  1.0 ]
        frustum.getPlanes()[Frustum.FAR].set(
                (lbn).minus(lbf).normalize(), // | lbn - lbf |, inwards
                lbf );   // closest AABB point to origin on plane

        // n [  1.0 /  0.0 /  0.0 ]
        frustum.getPlanes()[Frustum.LEFT].set(
                (rbf).minus(lbf).normalize(), // | rbf - lbf |, inwards
                lbn );   // closest AABB point to origin on plane

        // n [ -1.0 /  0.0 /  0.0 ]
        frustum.getPlanes()[Frustum.RIGHT].set(
                (lbf).minus(rbf).normalize(), // | lbf - rbf |, inwards
                rbn );   // closest AABB point to origin on plane

        // n [  0.0 /  1.0 /  0.0 ]
        frustum.getPlanes()[Frustum.BOTTOM].set(
                (ltf).minus(lbf).normalize(), // | ltf - lbf |, inwards
                lbn );   // closest AABB point to origin on plane

        // n [  0.0 / -1.0 /  0.0 ]
        frustum.getPlanes()[Frustum.TOP].set(
                (lbf).minus(ltf).normalize(), // | lbf - ltf |, inwards
                ltn );   // closest AABB point to origin on plane

        return frustum;
    }

}
