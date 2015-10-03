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

package com.jogamp.opengl.test.junit.jogl.acore;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class GLProfile0XBase extends UITestCase {

    //
    // GL4bc, GL4, GL3bc, GL3, GL2, GL2GL3, GL4ES3, GL3ES3, GL2ES2, GL2ES1, GLES3, GLES2, GLES1
    //
    // Real: GL4bc, GL4, GL3bc, GL3, GL2, GLES3, GLES2, GLES1
    // Maps: GL2GL3, GL4ES3, GL3ES3, GL2ES2, GL2ES1
    //

    protected static void validateGLProfileGL4bc(final GLProfile glp) {
        Assert.assertTrue(glp.isGL4bc());
        Assert.assertTrue(glp.isGL4());
        Assert.assertTrue(glp.isGL3bc());
        Assert.assertTrue(glp.isGL3());
        Assert.assertTrue(glp.isGL2());
        Assert.assertFalse(glp.isGLES3());
        Assert.assertFalse(glp.isGLES2());
        Assert.assertFalse(glp.isGLES1());
        Assert.assertTrue(glp.isGL2GL3());
        Assert.assertTrue(glp.isGL4ES3());
        Assert.assertTrue(glp.isGL3ES3());
        Assert.assertTrue(glp.isGL2ES2());
        Assert.assertTrue(glp.isGL2ES1());
    }
    protected static void validateGL4bc(final GL gl) {
        final GLContext ctx = gl.getContext();
        final boolean gles3CompatAvail = ctx.isGLES3Compatible();

        Assert.assertTrue(gl.isGL4bc());
        Assert.assertTrue(gl.isGL4());
        Assert.assertTrue(gl.isGL3bc());
        Assert.assertTrue(gl.isGL3());
        Assert.assertTrue(gl.isGL2());
        Assert.assertTrue(gl.isGL2GL3());
        if( gles3CompatAvail ) {
            Assert.assertTrue(gl.isGL4ES3());
        } else {
            Assert.assertFalse(gl.isGL4ES3());
        }
        Assert.assertTrue(gl.isGL3ES3());
        Assert.assertTrue(gl.isGL2ES2());
        Assert.assertTrue(gl.isGL2ES1());
        Assert.assertFalse(gl.isGLES3());
        Assert.assertFalse(gl.isGLES2());
        Assert.assertFalse(gl.isGLES1());

        Assert.assertTrue(ctx.isGL4bc());
        Assert.assertTrue(ctx.isGL4());
        Assert.assertTrue(ctx.isGL3bc());
        Assert.assertTrue(ctx.isGL3());
        Assert.assertTrue(ctx.isGL2());
        Assert.assertTrue(ctx.isGL2GL3());
        if( gles3CompatAvail ) {
            Assert.assertTrue(ctx.isGL4ES3());
        } else {
            Assert.assertFalse(ctx.isGL4ES3());
        }
        Assert.assertTrue(ctx.isGL3ES3());
        Assert.assertTrue(ctx.isGL2ES2());
        Assert.assertTrue(ctx.isGL2ES1());
        Assert.assertFalse(ctx.isGLES3());
        Assert.assertFalse(ctx.isGLES2());
        Assert.assertFalse(ctx.isGLES1());
    }

    protected static void validateGLProfileGL4(final GLProfile glp) {
        Assert.assertFalse(glp.isGL4bc());
        Assert.assertTrue(glp.isGL4());
        Assert.assertFalse(glp.isGL3bc());
        Assert.assertTrue(glp.isGL3());
        Assert.assertFalse(glp.isGL2());
        Assert.assertFalse(glp.isGLES3());
        Assert.assertFalse(glp.isGLES2());
        Assert.assertFalse(glp.isGLES1());
        Assert.assertTrue(glp.isGL2GL3());
        Assert.assertTrue(glp.isGL4ES3());
        Assert.assertTrue(glp.isGL3ES3());
        Assert.assertTrue(glp.isGL2ES2());
        Assert.assertFalse(glp.isGL2ES1());
    }
    protected static void validateGL4(final GL gl) {
        final GLContext ctx = gl.getContext();
        final boolean gles3CompatAvail = ctx.isGLES3Compatible();

        Assert.assertFalse(gl.isGL4bc());
        Assert.assertTrue(gl.isGL4());
        Assert.assertFalse(gl.isGL3bc());
        Assert.assertTrue(gl.isGL3());
        Assert.assertFalse(gl.isGL2());
        Assert.assertTrue(gl.isGL2GL3());
        if( gles3CompatAvail ) {
            Assert.assertTrue(gl.isGL4ES3());
        } else {
            Assert.assertFalse(gl.isGL4ES3());
        }
        Assert.assertTrue(gl.isGL3ES3());
        Assert.assertTrue(gl.isGL2ES2());
        Assert.assertFalse(gl.isGL2ES1());
        Assert.assertFalse(gl.isGLES3());
        Assert.assertFalse(gl.isGLES2());
        Assert.assertFalse(gl.isGLES1());

        Assert.assertFalse(ctx.isGL4bc());
        Assert.assertTrue(ctx.isGL4());
        Assert.assertFalse(ctx.isGL3bc());
        Assert.assertTrue(ctx.isGL3());
        Assert.assertFalse(ctx.isGL2());
        Assert.assertTrue(ctx.isGL2GL3());
        if( gles3CompatAvail ) {
            Assert.assertTrue(ctx.isGL4ES3());
        } else {
            Assert.assertFalse(ctx.isGL4ES3());
        }
        Assert.assertTrue(ctx.isGL3ES3());
        Assert.assertTrue(ctx.isGL2ES2());
        Assert.assertFalse(ctx.isGL2ES1());
        Assert.assertFalse(ctx.isGLES3());
        Assert.assertFalse(ctx.isGLES2());
        Assert.assertFalse(ctx.isGLES1());
    }

    protected static void validateGLProfileGL3bc(final GLProfile glp) {
        Assert.assertFalse(glp.isGL4bc());
        Assert.assertFalse(glp.isGL4());
        Assert.assertTrue(glp.isGL3bc());
        Assert.assertTrue(glp.isGL3());
        Assert.assertTrue(glp.isGL2());
        Assert.assertFalse(glp.isGLES3());
        Assert.assertFalse(glp.isGLES2());
        Assert.assertFalse(glp.isGLES1());
        Assert.assertTrue(glp.isGL2GL3());
        Assert.assertFalse(glp.isGL4ES3());
        Assert.assertTrue(glp.isGL3ES3());
        Assert.assertTrue(glp.isGL2ES2());
        Assert.assertTrue(glp.isGL2ES1());
    }
    protected static void validateGL3bc(final GL gl) {
        final GLContext ctx = gl.getContext();
        final boolean gles3CompatAvail = ctx.isGLES3Compatible();

        Assert.assertFalse(gl.isGL4bc());
        Assert.assertFalse(gl.isGL4());
        Assert.assertTrue(gl.isGL3bc());
        Assert.assertTrue(gl.isGL3());
        Assert.assertTrue(gl.isGL2());
        Assert.assertTrue(gl.isGL2GL3());
        if( gles3CompatAvail ) { // possible w/ GL3 implementations!
            Assert.assertTrue(gl.isGL4ES3());
        } else {
            Assert.assertFalse(gl.isGL4ES3());
        }
        Assert.assertTrue(gl.isGL3ES3());
        Assert.assertTrue(gl.isGL2ES2());
        Assert.assertTrue(gl.isGL2ES1());
        Assert.assertFalse(gl.isGLES3());
        Assert.assertFalse(gl.isGLES2());
        Assert.assertFalse(gl.isGLES1());

        Assert.assertFalse(ctx.isGL4bc());
        Assert.assertFalse(ctx.isGL4());
        Assert.assertTrue(ctx.isGL3bc());
        Assert.assertTrue(ctx.isGL3());
        Assert.assertTrue(ctx.isGL2());
        Assert.assertTrue(ctx.isGL2GL3());
        if( gles3CompatAvail ) { // possible w/ GL3 implementations!
            Assert.assertTrue(ctx.isGL4ES3());
        } else {
            Assert.assertFalse(ctx.isGL4ES3());
        }
        Assert.assertTrue(ctx.isGL3ES3());
        Assert.assertTrue(ctx.isGL2ES2());
        Assert.assertTrue(ctx.isGL2ES1());
        Assert.assertFalse(ctx.isGLES3());
        Assert.assertFalse(ctx.isGLES2());
        Assert.assertFalse(ctx.isGLES1());
    }

    protected static void validateGLProfileGL3(final GLProfile glp) {
        Assert.assertFalse(glp.isGL4bc());
        Assert.assertFalse(glp.isGL4());
        Assert.assertFalse(glp.isGL3bc());
        Assert.assertTrue(glp.isGL3());
        Assert.assertFalse(glp.isGL2());
        Assert.assertFalse(glp.isGLES3());
        Assert.assertFalse(glp.isGLES2());
        Assert.assertFalse(glp.isGLES1());
        Assert.assertTrue(glp.isGL2GL3());
        Assert.assertFalse(glp.isGL4ES3());
        Assert.assertTrue(glp.isGL3ES3());
        Assert.assertTrue(glp.isGL2ES2());
        Assert.assertFalse(glp.isGL2ES1());
    }
    protected static void validateGL3(final GL gl) {
        final GLContext ctx = gl.getContext();
        final boolean gles3CompatAvail = ctx.isGLES3Compatible();

        Assert.assertFalse(gl.isGL4bc());
        Assert.assertFalse(gl.isGL4());
        Assert.assertFalse(gl.isGL3bc());
        Assert.assertTrue(gl.isGL3());
        Assert.assertFalse(gl.isGL2());
        Assert.assertTrue(gl.isGL2GL3());
        if( gles3CompatAvail ) { // possible w/ GL3 implementations!
            Assert.assertTrue(gl.isGL4ES3());
        } else {
            Assert.assertFalse(gl.isGL4ES3());
        }
        Assert.assertTrue(gl.isGL3ES3());
        Assert.assertTrue(gl.isGL2ES2());
        Assert.assertFalse(gl.isGL2ES1());
        Assert.assertFalse(gl.isGLES3());
        Assert.assertFalse(gl.isGLES2());
        Assert.assertFalse(gl.isGLES1());

        Assert.assertFalse(ctx.isGL4bc());
        Assert.assertFalse(ctx.isGL4());
        Assert.assertFalse(ctx.isGL3bc());
        Assert.assertTrue(ctx.isGL3());
        Assert.assertFalse(ctx.isGL2());
        Assert.assertTrue(ctx.isGL2GL3());
        if( gles3CompatAvail ) { // possible w/ GL3 implementations!
            Assert.assertTrue(ctx.isGL4ES3());
        } else {
            Assert.assertFalse(ctx.isGL4ES3());
        }
        Assert.assertTrue(ctx.isGL3ES3());
        Assert.assertTrue(ctx.isGL2ES2());
        Assert.assertFalse(ctx.isGL2ES1());
        Assert.assertFalse(ctx.isGLES3());
        Assert.assertFalse(ctx.isGLES2());
        Assert.assertFalse(ctx.isGLES1());
    }

    protected static void validateGLProfileGL2(final GLProfile glp) {
        Assert.assertFalse(glp.isGL4bc());
        Assert.assertFalse(glp.isGL4());
        Assert.assertFalse(glp.isGL3bc());
        Assert.assertFalse(glp.isGL3());
        Assert.assertTrue(glp.isGL2());
        Assert.assertFalse(glp.isGLES3());
        Assert.assertFalse(glp.isGLES2());
        Assert.assertFalse(glp.isGLES1());
        Assert.assertTrue(glp.isGL2GL3());
        Assert.assertFalse(glp.isGL4ES3());
        Assert.assertFalse(glp.isGL3ES3());
        Assert.assertTrue(glp.isGL2ES2());
        Assert.assertTrue(glp.isGL2ES1());
    }
    protected static void validateGL2(final GL gl) {
        final GLContext ctx = gl.getContext();
        final boolean gles3CompatAvail = ctx.isGLES3Compatible();

        Assert.assertFalse(gl.isGL4bc());
        Assert.assertFalse(gl.isGL4());
        Assert.assertFalse(gl.isGL3bc());
        Assert.assertFalse(gl.isGL3());
        Assert.assertTrue(gl.isGL2());
        Assert.assertTrue(gl.isGL2GL3());
        Assert.assertFalse(gl.isGL4ES3());
        Assert.assertFalse(gl.isGL3ES3());
        Assert.assertTrue(gl.isGL2ES2());
        Assert.assertTrue(gl.isGL2ES1());
        Assert.assertFalse(gl.isGLES3());
        Assert.assertFalse(gl.isGLES2());
        Assert.assertFalse(gl.isGLES1());

        Assert.assertFalse(ctx.isGL4bc());
        Assert.assertFalse(ctx.isGL4());
        Assert.assertFalse(ctx.isGL3bc());
        Assert.assertFalse(ctx.isGL3());
        Assert.assertTrue(ctx.isGL2());
        Assert.assertTrue(ctx.isGL2GL3());
        Assert.assertFalse(ctx.isGL4ES3());
        Assert.assertFalse(ctx.isGL3ES3());
        Assert.assertFalse(gles3CompatAvail);
        Assert.assertTrue(ctx.isGL2ES2());
        Assert.assertTrue(ctx.isGL2ES1());
        Assert.assertFalse(ctx.isGLES3());
        Assert.assertFalse(ctx.isGLES2());
        Assert.assertFalse(ctx.isGLES1());
    }

    protected static void validateGLProfileGLES3(final GLProfile glp) {
        Assert.assertFalse(glp.isGL4bc());
        Assert.assertFalse(glp.isGL4());
        Assert.assertFalse(glp.isGL3bc());
        Assert.assertFalse(glp.isGL3());
        Assert.assertFalse(glp.isGL2());
        Assert.assertTrue(glp.isGLES3());
        Assert.assertTrue(glp.isGLES2());
        Assert.assertFalse(glp.isGLES1());
        Assert.assertFalse(glp.isGL2GL3());
        Assert.assertTrue(glp.isGL4ES3());
        Assert.assertTrue(glp.isGL3ES3());
        Assert.assertTrue(glp.isGL2ES2());
        Assert.assertFalse(glp.isGL2ES1());
    }
    protected static void validateGLES3(final GL gl) {
        final GLContext ctx = gl.getContext();
        final boolean gles3CompatAvail = ctx.isGLES3Compatible();

        Assert.assertFalse(gl.isGL4bc());
        Assert.assertFalse(gl.isGL4());
        Assert.assertFalse(gl.isGL3bc());
        Assert.assertFalse(gl.isGL3());
        Assert.assertFalse(gl.isGL2());
        Assert.assertFalse(gl.isGL2GL3());
        Assert.assertTrue(gl.isGL4ES3());
        Assert.assertTrue(gl.isGL3ES3());
        Assert.assertTrue(gl.isGL2ES2());
        Assert.assertFalse(gl.isGL2ES1());
        Assert.assertTrue(gl.isGLES3());
        Assert.assertTrue(gl.isGLES2());
        Assert.assertFalse(gl.isGLES1());

        Assert.assertFalse(ctx.isGL4bc());
        Assert.assertFalse(ctx.isGL4());
        Assert.assertFalse(ctx.isGL3bc());
        Assert.assertFalse(ctx.isGL3());
        Assert.assertFalse(ctx.isGL2());
        Assert.assertFalse(ctx.isGL2GL3());
        Assert.assertTrue(ctx.isGL4ES3());
        Assert.assertTrue(ctx.isGL3ES3());
        Assert.assertTrue(gles3CompatAvail);
        Assert.assertTrue(ctx.isGL2ES2());
        Assert.assertFalse(ctx.isGL2ES1());
        Assert.assertTrue(ctx.isGLES3());
        Assert.assertTrue(ctx.isGLES2());
        Assert.assertFalse(ctx.isGLES1());
    }

    protected static void validateGLProfileGLES2(final GLProfile glp) {
        Assert.assertFalse(glp.isGL4bc());
        Assert.assertFalse(glp.isGL4());
        Assert.assertFalse(glp.isGL3bc());
        Assert.assertFalse(glp.isGL3());
        Assert.assertFalse(glp.isGL2());
        Assert.assertFalse(glp.isGLES3());
        Assert.assertTrue(glp.isGLES2());
        Assert.assertFalse(glp.isGLES1());
        Assert.assertFalse(glp.isGL2GL3());
        Assert.assertFalse(glp.isGL4ES3());
        Assert.assertFalse(glp.isGL3ES3());
        Assert.assertTrue(glp.isGL2ES2());
        Assert.assertFalse(glp.isGL2ES1());
    }
    protected static void validateGLES2(final GL gl) {
        final GLContext ctx = gl.getContext();
        final boolean gles3CompatAvail = ctx.isGLES3Compatible();

        Assert.assertFalse(gl.isGL4bc());
        Assert.assertFalse(gl.isGL4());
        Assert.assertFalse(gl.isGL3bc());
        Assert.assertFalse(gl.isGL3());
        Assert.assertFalse(gl.isGL2());
        Assert.assertFalse(gl.isGL2GL3());
        Assert.assertFalse(gl.isGL4ES3());
        Assert.assertFalse(gl.isGL3ES3());
        Assert.assertTrue(gl.isGL2ES2());
        Assert.assertFalse(gl.isGL2ES1());
        Assert.assertFalse(gl.isGLES3());
        Assert.assertTrue(gl.isGLES2());
        Assert.assertFalse(gl.isGLES1());

        Assert.assertFalse(ctx.isGL4bc());
        Assert.assertFalse(ctx.isGL4());
        Assert.assertFalse(ctx.isGL3bc());
        Assert.assertFalse(ctx.isGL3());
        Assert.assertFalse(ctx.isGL2());
        Assert.assertFalse(ctx.isGL2GL3());
        Assert.assertFalse(ctx.isGL4ES3());
        Assert.assertFalse(ctx.isGL3ES3());
        Assert.assertFalse(gles3CompatAvail);
        Assert.assertTrue(ctx.isGL2ES2());
        Assert.assertFalse(ctx.isGL2ES1());
        Assert.assertFalse(ctx.isGLES3());
        Assert.assertTrue(ctx.isGLES2());
        Assert.assertFalse(ctx.isGLES1());
    }

    protected static void validateGLProfileGLES1(final GLProfile glp) {
        Assert.assertFalse(glp.isGL4bc());
        Assert.assertFalse(glp.isGL4());
        Assert.assertFalse(glp.isGL3bc());
        Assert.assertFalse(glp.isGL3());
        Assert.assertFalse(glp.isGL2());
        Assert.assertFalse(glp.isGLES3());
        Assert.assertFalse(glp.isGLES2());
        Assert.assertTrue(glp.isGLES1());
        Assert.assertFalse(glp.isGL2GL3());
        Assert.assertFalse(glp.isGL4ES3());
        Assert.assertFalse(glp.isGL3ES3());
        Assert.assertFalse(glp.isGL2ES2());
        Assert.assertTrue(glp.isGL2ES1());
    }
    protected static void validateGLES1(final GL gl) {
        final GLContext ctx = gl.getContext();
        final boolean gles3CompatAvail = ctx.isGLES3Compatible();

        Assert.assertFalse(gl.isGL4bc());
        Assert.assertFalse(gl.isGL4());
        Assert.assertFalse(gl.isGL3bc());
        Assert.assertFalse(gl.isGL3());
        Assert.assertFalse(gl.isGL2());
        Assert.assertFalse(gl.isGL2GL3());
        Assert.assertFalse(gl.isGL4ES3());
        Assert.assertFalse(gl.isGL3ES3());
        Assert.assertFalse(gl.isGL2ES2());
        Assert.assertTrue(gl.isGL2ES1());
        Assert.assertFalse(gl.isGLES3());
        Assert.assertFalse(gl.isGLES2());
        Assert.assertTrue(gl.isGLES1());

        Assert.assertFalse(ctx.isGL4bc());
        Assert.assertFalse(ctx.isGL4());
        Assert.assertFalse(ctx.isGL3bc());
        Assert.assertFalse(ctx.isGL3());
        Assert.assertFalse(ctx.isGL2());
        Assert.assertFalse(ctx.isGL2GL3());
        Assert.assertFalse(ctx.isGL4ES3());
        Assert.assertFalse(ctx.isGL3ES3());
        Assert.assertFalse(gles3CompatAvail);
        Assert.assertFalse(ctx.isGL2ES2());
        Assert.assertTrue(ctx.isGL2ES1());
        Assert.assertFalse(ctx.isGLES3());
        Assert.assertFalse(ctx.isGLES2());
        Assert.assertTrue(ctx.isGLES1());
    }

    protected static void validateGLProfileGL2GL3(final GLProfile glp) {
        if( glp.isGL4bc() ) {
            validateGLProfileGL4bc(glp);
        } else if(glp.isGL3bc()) {
            validateGLProfileGL3bc(glp);
        } else if(glp.isGL2()) {
            validateGLProfileGL2(glp);
        } else if(glp.isGL4()) {
            validateGLProfileGL4(glp);
        } else if(glp.isGL3()) {
            validateGLProfileGL3(glp);
        } else {
            throw new GLException("GL2GL3 is neither GL4bc, GL3bc, GL2, GL4 nor GL3");
        }
    }
    protected static void validateGL2GL3(final GL gl) {
        if( gl.isGL4bc() ) {
            validateGL4bc(gl);
        } else if(gl.isGL3bc()) {
            validateGL3bc(gl);
        } else if(gl.isGL2()) {
            validateGL2(gl);
        } else if(gl.isGL4()) {
            validateGL4(gl);
        } else if(gl.isGL3()) {
            validateGL3(gl);
        } else {
            throw new GLException("GL2GL3 is neither GL4bc, GL3bc, GL2, GL4 nor GL3");
        }
    }

    protected static void validateGLProfileGL4ES3(final GLProfile glp) {
        if( glp.isGL4bc() ) {
            validateGLProfileGL4bc(glp);
        } else if( glp.isGL4() ) {
            validateGLProfileGL4(glp);
        } else if( glp.isGLES3() ) {
            validateGLProfileGLES3(glp);
        } else {
            throw new GLException("GL4ES3 is neither GL4bc, GL4 nor GLES3");
        }
    }
    protected static void validateGL4ES3(final GL gl) {
        if( gl.isGL4bc() ) {
            validateGL4bc(gl);
        } else if( gl.isGL4() ) {
            validateGL4(gl);
        } else if( gl.isGLES3() ) {
            validateGLES3(gl);
        } else {
            throw new GLException("GL4ES3 is neither GL4bc, GL4 nor GLES3");
        }
    }

    protected static void validateGLProfileGL2ES2(final GLProfile glp) {
        if( glp.isGL4bc() ) {
            validateGLProfileGL4bc(glp);
        } else if(glp.isGL3bc()) {
            validateGLProfileGL3bc(glp);
        } else if(glp.isGL2()) {
            validateGLProfileGL2(glp);
        } else if(glp.isGL4()) {
            validateGLProfileGL4(glp);
        } else if(glp.isGL3()) {
            validateGLProfileGL3(glp);
        } else if(glp.isGLES3()) {
            validateGLProfileGLES3(glp);
        } else if(glp.isGLES2()) {
            validateGLProfileGLES2(glp);
        } else {
            throw new GLException("GL2ES2 is neither GL4bc, GL3bc, GL2, GL4, GL3, GLES3 nor GLES2");
        }
    }
    protected static void validateGL2ES2(final GL gl) {
        if( gl.isGL4bc() ) {
            validateGL4bc(gl);
        } else if(gl.isGL3bc()) {
            validateGL3bc(gl);
        } else if(gl.isGL2()) {
            validateGL2(gl);
        } else if(gl.isGL4()) {
            validateGL4(gl);
        } else if(gl.isGL3()) {
            validateGL3(gl);
        } else if(gl.isGLES3()) {
            validateGLES3(gl);
        } else if(gl.isGLES2()) {
            validateGLES2(gl);
        } else {
            throw new GLException("GL2ES2 is neither GL4bc, GL3bc, GL2, GL4, GL3, GLES3 nor GLES2");
        }
    }

    protected static void validateGLProfileGL2ES1(final GLProfile glp) {
        if( glp.isGL4bc() ) {
            validateGLProfileGL4bc(glp);
        } else if(glp.isGL3bc()) {
            validateGLProfileGL3bc(glp);
        } else if(glp.isGL2()) {
            validateGLProfileGL2(glp);
        } else if(glp.isGLES1()) {
            validateGLProfileGLES1(glp);
        } else {
            throw new GLException("GL2ES1 is neither GL4bc, GL3bc, GL2 nor GLES1");
        }
    }
    protected static void validateGL2ES1(final GL gl) {
        if( gl.isGL4bc() ) {
            validateGL4bc(gl);
        } else if(gl.isGL3bc()) {
            validateGL3bc(gl);
        } else if(gl.isGL2()) {
            validateGL2(gl);
        } else if(gl.isGLES1()) {
            validateGLES1(gl);
        } else {
            throw new GLException("GL2ES1 is neither GL4bc, GL3bc, GL2 nor GLES1");
        }
    }

    protected static void validateOffline(final String requestedProfile, final GLProfile glp) {
        System.err.println("GLProfile Mapping "+requestedProfile+" -> "+glp);

        final boolean gles3CompatAvail = GLContext.isGLES3CompatibleAvailable(GLProfile.getDefaultDevice());
        if( glp.getImplName().equals(GLProfile.GL4bc) ) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL4bc));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL4));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL3bc));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL3));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2GL3));
            if( gles3CompatAvail ) {
                Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL4ES3));
            } else {
                Assert.assertFalse(GLProfile.isAvailable(GLProfile.GL4ES3));
            }
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES1));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        } else if(glp.getImplName().equals(GLProfile.GL3bc)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL3bc));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL3));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2GL3));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES1));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        } else if(glp.getImplName().equals(GLProfile.GL2)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2GL3));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES1));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        } else if(glp.getImplName().equals(GLProfile.GL4)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL4));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL3));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2GL3));
            if( gles3CompatAvail ) {
                Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL4ES3));
            } else {
                Assert.assertFalse(GLProfile.isAvailable(GLProfile.GL4ES3));
            }
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        } else if(glp.getImplName().equals(GLProfile.GL3)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL3));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2GL3));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        } else if(glp.getImplName().equals(GLProfile.GLES3)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GLES3));
            if( gles3CompatAvail ) {
                Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL4ES3));
            } else {
                Assert.assertFalse(GLProfile.isAvailable(GLProfile.GL4ES3));
            }
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        } else if(glp.getImplName().equals(GLProfile.GLES2)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GLES2));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        } else if(glp.getImplName().equals(GLProfile.GLES1)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GLES1));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES1));
        }
        if( glp.isGL4bc() ) {
            validateGLProfileGL4bc(glp);
        } else if(glp.isGL3bc()) {
            validateGLProfileGL3bc(glp);
        } else if(glp.isGL2()) {
            validateGLProfileGL2(glp);
        } else if(glp.isGL4()) {
            validateGLProfileGL4(glp);
        } else if(glp.isGL3()) {
            validateGLProfileGL3(glp);
        } else if(glp.isGLES3()) {
            validateGLProfileGLES3(glp);
        } else if(glp.isGLES2()) {
            validateGLProfileGLES2(glp);
        } else if(glp.isGLES1()) {
            validateGLProfileGLES1(glp);
        }

        if( requestedProfile == GLProfile.GL4bc ) {
            validateGLProfileGL4bc(glp);
        } else if( requestedProfile == GLProfile.GL3bc ) {
            validateGLProfileGL3bc(glp);
        } else if( requestedProfile == GLProfile.GL2 ) {
            validateGLProfileGL2(glp);
        } else if( requestedProfile == GLProfile.GL4 ) {
            validateGLProfileGL4(glp);
        } else if( requestedProfile == GLProfile.GL3 ) {
            validateGLProfileGL3(glp);
        } else if( requestedProfile == GLProfile.GLES3 ) {
            validateGLProfileGLES3(glp);
        } else if( requestedProfile == GLProfile.GLES2 ) {
            validateGLProfileGLES2(glp);
        } else if( requestedProfile == GLProfile.GLES1 ) {
            validateGLProfileGLES1(glp);
        } else if( requestedProfile == GLProfile.GL2GL3 ) {
            validateGLProfileGL2GL3(glp);
        } else if( requestedProfile == GLProfile.GL4ES3 ) {
            validateGLProfileGL4ES3(glp);
        } else if( requestedProfile == GLProfile.GL2ES2 ) {
            validateGLProfileGL2ES2(glp);
        } else if( requestedProfile == GLProfile.GL2ES1 ) {
            validateGLProfileGL2ES1(glp);
        }

    }

    protected static void validateOnline(final String requestedProfile, final GLProfile glpReq, final GL gl) {
        final GLContext ctx = gl.getContext();
        final GLProfile glp = gl.getGLProfile();

        System.err.println("GLContext Mapping "+requestedProfile+" -> "+glpReq+" -> "+glp+" -> "+ctx.getGLVersion());

        System.err.println("GL impl. class "+gl.getClass().getName());
        if( gl.isGL4() ) {
            Assert.assertNotNull( gl.getGL4() );
            System.err.println("GL Mapping "+glp+" -> GL4");
        }
        if( gl.isGL4bc() ) {
            Assert.assertNotNull( gl.getGL4bc() );
            System.err.println("GL Mapping "+glp+" -> GL4bc");
        }
        if( gl.isGL3() ) {
            Assert.assertNotNull( gl.getGL3() );
            System.err.println("GL Mapping "+glp+" -> GL3");
        }
        if( gl.isGL3bc() ) {
            Assert.assertNotNull( gl.getGL3bc() );
            System.err.println("GL Mapping "+glp+" -> GL3bc");
        }
        if( gl.isGL2() ) {
            Assert.assertNotNull( gl.getGL2() );
            System.err.println("GL Mapping "+glp+" -> GL2");
        }
        if( gl.isGLES3() ) {
            Assert.assertNotNull( gl.getGLES3() );
            System.err.println("GL Mapping "+glp+" -> GLES3");
        }
        if( gl.isGLES2() ) {
            Assert.assertNotNull( gl.getGLES2() );
            System.err.println("GL Mapping "+glp+" -> GLES2");
        }
        if( gl.isGLES1() ) {
            Assert.assertNotNull( gl.getGLES1() );
            System.err.println("GL Mapping "+glp+" -> GLES1");
        }
        if( gl.isGL4ES3() ) {
            Assert.assertNotNull( gl.getGL4ES3() );
            System.err.println("GL Mapping "+glp+" -> GL4ES3");
        }
        if( gl.isGL3ES3() ) {
            Assert.assertNotNull( gl.getGL3ES3() );
            System.err.println("GL Mapping "+glp+" -> GL3ES3");
        }
        if( gl.isGL2GL3() ) {
            Assert.assertNotNull( gl.getGL2GL3() );
            System.err.println("GL Mapping "+glp+" -> GL2GL3");
        }
        if( gl.isGL2ES2() ) {
            Assert.assertNotNull( gl.getGL2ES2() );
            System.err.println("GL Mapping "+glp+" -> GL2ES2");
        }
        if( gl.isGL2ES1() ) {
            Assert.assertNotNull( gl.getGL2ES1() );
            System.err.println("GL Mapping "+glp+" -> GL2ES1");
        }

        if( gl.isGL4bc() ) {
            validateGL4bc(gl);
        } else if(gl.isGL3bc()) {
            validateGL3bc(gl);
        } else if(gl.isGL2()) {
            validateGL2(gl);
        } else if(gl.isGL4()) {
            validateGL4(gl);
        } else if(gl.isGL3()) {
            validateGL3(gl);
        } else if(gl.isGLES3()) {
            validateGLES3(gl);
        } else if(gl.isGLES2()) {
            validateGLES2(gl);
        } else if(gl.isGLES1()) {
            validateGLES1(gl);
        }

        if( requestedProfile == GLProfile.GL4bc ) {
            validateGL4bc(gl);
        } else if( requestedProfile == GLProfile.GL3bc ) {
            if( gl.isGL4bc() ) {
                validateGL4bc(gl);
            } else if( gl.isGL3bc() ) {
                validateGL3bc(gl);
            } else {
                throw new GLException("GL3bc is neither GL4bc nor GL3bc");
            }
        } else if( requestedProfile == GLProfile.GL2 ) {
            if( gl.isGL4bc() ) {
                validateGL4bc(gl);
            } else if( gl.isGL3bc() ) {
                validateGL3bc(gl);
            } else if( gl.isGL2() ) {
                validateGL2(gl);
            } else {
                throw new GLException("GL2 is neither GL4bc, GL3bc, GL2");
            }
        } else if( requestedProfile == GLProfile.GL4 ) {
            if( gl.isGL4bc() ) {
                validateGL4bc(gl);
            } else if( gl.isGL4() ) {
                validateGL4(gl);
            } else {
                throw new GLException("GL4 is neither GL4bc, nor GL4");
            }
        } else if( requestedProfile == GLProfile.GL3 ) {
            if( gl.isGL4bc() ) {
                validateGL4bc(gl);
            } else if( gl.isGL3bc() ) {
                validateGL3bc(gl);
            } else if( gl.isGL4() ) {
                validateGL4(gl);
            } else if( gl.isGL3() ) {
                validateGL3(gl);
            } else {
                throw new GLException("GL3 is neither GL4bc, GL3bc, GL4 nor GL3");
            }
        } else if( requestedProfile == GLProfile.GLES3 ) {
            validateGLES3(gl);
        } else if( requestedProfile == GLProfile.GLES2 ) {
            if( gl.isGLES3() ) {
                validateGLES3(gl);
            } else if( gl.isGLES2() ) {
                validateGLES2(gl);
            } else {
                throw new GLException("GLES2 is neither GLES3 nor GLES2");
            }
        } else if( requestedProfile == GLProfile.GLES1 ) {
            validateGLES1(gl);
        } else if( requestedProfile == GLProfile.GL2GL3 ) {
            validateGL2GL3(gl);
        } else if( requestedProfile == GLProfile.GL4ES3 ) {
            validateGL4ES3(gl);
        } else if( requestedProfile == GLProfile.GL2ES2 ) {
            validateGL2ES2(gl);
        } else if( requestedProfile == GLProfile.GL2ES1 ) {
            validateGL2ES1(gl);
        }
    }

    protected void validateOnlineOnscreen(final String requestedProfile, final GLProfile glp) throws InterruptedException {
        final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle(getSimpleTestName("."));

        glWindow.addGLEventListener(new GLEventListener() {

            public void init(final GLAutoDrawable drawable) {
                final GL gl = drawable.getGL();
                System.err.println(JoglVersion.getGLStrings(gl, null, false));

                validateOnline(requestedProfile, glp, gl);
            }

            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
            }

            public void display(final GLAutoDrawable drawable) {
            }

            public void dispose(final GLAutoDrawable drawable) {
            }
        });

        glWindow.setSize(128, 128);
        glWindow.setVisible(true);
        Assert.assertTrue(AWTRobotUtil.waitForRealized(glWindow, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(glWindow, true));

        glWindow.display();
        Thread.sleep(100);
        // Same definition as in GLProfile for 'disableOpenGLARBContext'
        final boolean isOSX = Platform.OSType.MACOS == Platform.getOSType();
        final boolean disableOpenGLARBContext = null != System.getProperty("jogl.disable.openglarbcontext") && !isOSX;
        if( disableOpenGLARBContext ) {
            Assert.assertFalse("Property 'jogl.disable.openglarbcontext' set, but created w/ ARB",
                               glWindow.getContext().isCreatedWithARBMethod());
        }
        glWindow.destroy();
        Assert.assertTrue(AWTRobotUtil.waitForVisible(glWindow, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(glWindow, false));
    }

    protected void validateOnlineOffscreen(final String requestedProfile, final GLCapabilitiesImmutable caps)
            throws InterruptedException {
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
        final GLAutoDrawable glad = factory.createOffscreenAutoDrawable(null, caps, null, 128, 128);
        Assert.assertNotNull(glad);

        glad.addGLEventListener(new GLEventListener() {
            public void init(final GLAutoDrawable drawable) {
                final GL gl = drawable.getGL();
                System.err.println(JoglVersion.getGLStrings(gl, null, false));

                validateOnline(requestedProfile, caps.getGLProfile(), gl);
            }

            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
            }

            public void display(final GLAutoDrawable drawable) {
            }

            public void dispose(final GLAutoDrawable drawable) {
            }
        });
        glad.display();
        Thread.sleep(100);
        // Same definition as in GLProfile for 'disableOpenGLARBContext'
        final boolean isOSX = Platform.OSType.MACOS == Platform.getOSType();
        final boolean disableOpenGLARBContext = PropertyAccess.isPropertyDefined("jogl.disable.openglarbcontext", true) && !isOSX;
        if( disableOpenGLARBContext ) {
            Assert.assertFalse("Property 'jogl.disable.openglarbcontext' set, but created w/ ARB",
                               glad.getContext().isCreatedWithARBMethod());
        }
        glad.destroy();
    }
}
