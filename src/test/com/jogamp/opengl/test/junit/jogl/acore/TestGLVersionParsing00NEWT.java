/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import jogamp.opengl.GLVersionNumber;

import com.jogamp.common.util.VersionNumberString;
import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLVersionParsing00NEWT extends UITestCase {

    public static final String[] glVersionStrings00 = new String[] {
        "GL_VERSION_2_1 DummyTool 1.2.3", // 0
        "2.1 Mesa 7.0.3-rc2",
        "4.2.12171 Compatibility Profile Context 9.01.8",
        "4.2.12198 Compatibility Profile Context 12.102.3.0",
        "2.1 Mesa 7.0.3-rc2 post odd",
        "4.2.12171 Compatibility Profile Context 9.01.8 post odd",
        "4.2.12198 Compatibility Profile Context 12.102.3.0 post odd",
        "OpenGL ES 2.0 Mesa 9.1.1", // 7
        "OpenGL ES 2.0 14.10.1",
        "OpenGL ES GLSL ES 2.0 14.10.1", // 9
        "OpenGL ES 2.0 3Com L3 11.33.44" // 10
    };
    public static final String[] glVersionStrings01 = new String[] {
        "GL_VERSION_2_1 Dummy Tool 1.2", // 0
        "2.1 Mesa 7.12",
        "2.1 Mesa 7.12-devel",
        "2.1 Mesa 7.12-devel (git-d6c318e)",
        "2.1 Mesa 7.12-devel la1 la2 li3",
        "4.3.0 NVIDIA 310.32",
        "OpenGL ES 2.0 Mesa 9.1", // 6
        "OpenGL ES 2.0 14.10",
        "OpenGL ES GLSL ES 2.0 14.10", // 8
        "OpenGL ES 2.0 3Com L3 11.33" // 9
    };
    public static final String[] glVersionStrings02 = new String[] {
        "GL_VERSION_2_1", // 0
        "OpenGL ES 2.0", // 1
        "OpenGL ES GLSL ES 2.0", // 2
        "OpenGL 2.1 LaLa", // 3
        "4.2.11762 Compatibility Profile Context" // 4
    };

    public static final VersionNumberString[] glVersionNumbers = new VersionNumberString[] {
        new VersionNumberString(2, 1, 0, glVersionStrings00[0]),
        new VersionNumberString(2, 1, 0, glVersionStrings00[1]),
        new VersionNumberString(4, 2, 0, glVersionStrings00[2]),
        new VersionNumberString(4, 2, 0, glVersionStrings00[3]),
        new VersionNumberString(2, 1, 0, glVersionStrings00[4]),
        new VersionNumberString(4, 2, 0, glVersionStrings00[5]),
        new VersionNumberString(4, 2, 0, glVersionStrings00[6]),
        new VersionNumberString(2, 0, 0, glVersionStrings00[7]),
        new VersionNumberString(2, 0, 0, glVersionStrings00[8]),
        new VersionNumberString(2, 0, 0, glVersionStrings00[9]),
        new VersionNumberString(2, 0, 0, glVersionStrings00[10]),

        new VersionNumberString(2, 1, 0, glVersionStrings01[0]),
        new VersionNumberString(2, 1, 0, glVersionStrings01[1]),
        new VersionNumberString(2, 1, 0, glVersionStrings01[2]),
        new VersionNumberString(2, 1, 0, glVersionStrings01[3]),
        new VersionNumberString(2, 1, 0, glVersionStrings01[4]),
        new VersionNumberString(4, 3, 0, glVersionStrings01[5]),
        new VersionNumberString(2, 0, 0, glVersionStrings01[6]),
        new VersionNumberString(2, 0, 0, glVersionStrings01[7]),
        new VersionNumberString(2, 0, 0, glVersionStrings01[8]),
        new VersionNumberString(2, 0, 0, glVersionStrings01[9]),

        new VersionNumberString(2, 1, 0, glVersionStrings02[0]),
        new VersionNumberString(2, 0, 0, glVersionStrings02[1]),
        new VersionNumberString(2, 0, 0, glVersionStrings02[2]),
        new VersionNumberString(2, 1, 0, glVersionStrings02[3]),
        new VersionNumberString(4, 2, 0, glVersionStrings02[4])
    };
    public static final VersionNumberString[] glVendorVersionNumbersWithSub = new VersionNumberString[] {
        new VersionNumberString(1,     2,  3, glVersionStrings00[0]),
        new VersionNumberString(7,     0,  3, glVersionStrings00[1]),
        new VersionNumberString(9,     1,  8, glVersionStrings00[2]),
        new VersionNumberString(12,  102,  3, glVersionStrings00[3]),
        new VersionNumberString(7,     0,  3, glVersionStrings00[4]),
        new VersionNumberString(9,     1,  8, glVersionStrings00[5]),
        new VersionNumberString(12,  102,  3, glVersionStrings00[6]),
        new VersionNumberString(9,     1,  1, glVersionStrings00[7]),
        new VersionNumberString(14,   10,  1, glVersionStrings00[8]),
        new VersionNumberString(14,   10,  1, glVersionStrings00[9]),
        new VersionNumberString(11,   33, 44, glVersionStrings00[10])
    };
    public static final VersionNumberString[] glVendorVersionNumbersNoSub = new VersionNumberString[] {
        new VersionNumberString(1,     2, 0, glVersionStrings01[0]),
        new VersionNumberString(7,    12, 0, glVersionStrings01[1]),
        new VersionNumberString(7,    12, 0, glVersionStrings01[2]),
        new VersionNumberString(7,    12, 0, glVersionStrings01[3]),
        new VersionNumberString(7,    12, 0, glVersionStrings01[4]),
        new VersionNumberString(310,  32, 0, glVersionStrings01[5]),
        new VersionNumberString(9,     1, 0, glVersionStrings01[6]),
        new VersionNumberString(14,   10, 0, glVersionStrings01[7]),
        new VersionNumberString(14,   10, 0, glVersionStrings01[8]),
        new VersionNumberString(11,   33, 0, glVersionStrings01[9])
    };
    public static final VersionNumberString[] glVendorVersionNumbersNone = new VersionNumberString[] {
        new VersionNumberString(0, 0, 0, glVersionStrings02[0]),
        new VersionNumberString(0, 0, 0, glVersionStrings02[1]),
        new VersionNumberString(0, 0, 0, glVersionStrings02[2]),
        new VersionNumberString(0, 0, 0, glVersionStrings02[3]),
        new VersionNumberString(0, 0, 0, glVersionStrings02[4])
    };

    @Test
    public void test01GLVersion() throws InterruptedException {
        for(int i=0; i<glVersionNumbers.length; i++) {
            final VersionNumberString exp = glVersionNumbers[i];
            final GLVersionNumber has = GLVersionNumber.create(exp.getVersionString());
            System.err.println("Test["+i+"]: "+exp+" -> "+has+", valid "+has.isValid()+", define ["+has.hasMajor()+":"+has.hasMinor()+":"+has.hasSub()+"]");
            Assert.assertTrue(has.hasMajor());
            Assert.assertTrue(has.hasMinor());
            Assert.assertTrue(!has.hasSub());
            Assert.assertTrue(has.isValid());
            Assert.assertEquals(exp, has);
        }
        {
            final GLVersionNumber has = GLVersionNumber.create("GL_VERSION_2");
            System.err.println("Test-X1: "+has+", valid "+has.isValid()+", define ["+has.hasMajor()+":"+has.hasMinor()+":"+has.hasSub()+"]");
            Assert.assertTrue(has.hasMajor());
            Assert.assertTrue(!has.hasMinor());
            Assert.assertTrue(!has.hasSub());
            Assert.assertTrue(!has.isValid());
        }
        {
            final GLVersionNumber has = GLVersionNumber.create("GL2 Buggy L3");
            System.err.println("Test-X2: "+has+", valid "+has.isValid()+", define ["+has.hasMajor()+":"+has.hasMinor()+":"+has.hasSub()+"]");
            Assert.assertTrue(has.hasMajor());
            Assert.assertTrue(!has.hasMinor());
            Assert.assertTrue(!has.hasSub());
            Assert.assertTrue(!has.isValid());
        }
        {
            final GLVersionNumber has = GLVersionNumber.create("GL Nope");
            System.err.println("Test-X3: "+has+", valid "+has.isValid()+", define ["+has.hasMajor()+":"+has.hasMinor()+":"+has.hasSub()+"]");
            Assert.assertTrue(!has.hasMajor());
            Assert.assertTrue(!has.hasMinor());
            Assert.assertTrue(!has.hasSub());
            Assert.assertTrue(!has.isValid());
        }
    }

    private void testGLVendorVersionImpl(final VersionNumberString[] versionNumberString, final boolean withMajor, final boolean withMinor, final boolean withSub) throws InterruptedException {
        for(int i=0; i<versionNumberString.length; i++) {
            final VersionNumberString exp = versionNumberString[i];
            final VersionNumberString has = GLVersionNumber.createVendorVersion(exp.getVersionString());
            System.err.println("Test["+withMajor+":"+withMinor+":"+withSub+"]["+i+"]: "+exp+" -> "+has+", define ["+has.hasMajor()+":"+has.hasMinor()+":"+has.hasSub()+"]");
            Assert.assertEquals(withMajor, has.hasMajor());
            Assert.assertEquals(withMinor, has.hasMinor());
            Assert.assertEquals(withSub,   has.hasSub());
            Assert.assertEquals(exp, has);
        }
    }

    @Test
    public void test02GLVendorVersion() throws InterruptedException {
        testGLVendorVersionImpl(glVendorVersionNumbersWithSub, true, true, true);
        testGLVendorVersionImpl(glVendorVersionNumbersNoSub, true, true, false);
        testGLVendorVersionImpl(glVendorVersionNumbersNone, false, false, false);
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestGLVersionParsing00NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}