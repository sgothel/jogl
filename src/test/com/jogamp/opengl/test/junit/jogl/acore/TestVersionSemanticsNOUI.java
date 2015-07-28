/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.osjava.jardiff.DiffCriteria;
import org.semver.Delta;

import com.jogamp.common.util.JogampVersion;
import com.jogamp.common.util.VersionNumberString;
import com.jogamp.junit.util.SingletonJunitCase;
import com.jogamp.junit.util.VersionSemanticsUtil;
import com.jogamp.opengl.JoglVersion;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestVersionSemanticsNOUI extends SingletonJunitCase {
    static final String jarFile = "jogl-all.jar";

    static final DiffCriteria diffCriteria = new org.osjava.jardiff.SimpleDiffCriteria();
    // static final DiffCriteria diffCriteria = new org.osjava.jardiff.PublicDiffCriteria();

    static final JogampVersion curVersion = JoglVersion.getInstance();
    static final VersionNumberString curVersionNumber = new VersionNumberString(curVersion.getImplementationVersion());

    static final Set<String> excludesDefault;
    static final Set<String> excludesStereoPackageAndAppletUtils;
    static {
        excludesDefault = new HashSet<String>();
        excludesDefault.add("^\\Qjogamp/\\E.*$");
        excludesStereoPackageAndAppletUtils = new HashSet<String>(excludesDefault);
        excludesStereoPackageAndAppletUtils.add("^\\Qcom/jogamp/opengl/util/stereo/\\E.*$");
        excludesStereoPackageAndAppletUtils.add("^\\Qcom/jogamp/newt/util/applet/\\E.*$");
    }


    // @Test
    public void testVersionV212V213() throws IllegalArgumentException, IOException, URISyntaxException {
        testVersions(diffCriteria, Delta.CompatibilityType.NON_BACKWARD_COMPATIBLE, "2.1.2", "2.1.3", excludesDefault);
    }

    // @Test
    public void testVersionV213V214() throws IllegalArgumentException, IOException, URISyntaxException {
        testVersions(diffCriteria, Delta.CompatibilityType.NON_BACKWARD_COMPATIBLE, "2.1.3", "2.1.4", excludesDefault);
    }

    // @Test
    public void testVersionV214V215() throws IllegalArgumentException, IOException, URISyntaxException {
        testVersions(diffCriteria, Delta.CompatibilityType.NON_BACKWARD_COMPATIBLE, "2.1.4", "2.1.5", excludesDefault);
    }

    // @Test
    public void testVersionV215V220() throws IllegalArgumentException, IOException, URISyntaxException {
        testVersions(diffCriteria, Delta.CompatibilityType.NON_BACKWARD_COMPATIBLE, "2.1.5", "2.2.0", excludesDefault);
    }

    @Test
    public void testVersionV220V221() throws IllegalArgumentException, IOException, URISyntaxException {
        testVersions(diffCriteria, Delta.CompatibilityType.BACKWARD_COMPATIBLE_USER, "2.2.0", "2.2.1", excludesDefault);
    }

    @Test
    public void testVersionV221V230() throws IllegalArgumentException, IOException, URISyntaxException {
        testVersions(diffCriteria, Delta.CompatibilityType.NON_BACKWARD_COMPATIBLE, "2.2.1", "2.3.0", excludesDefault);
    }

    void testVersions(final DiffCriteria diffCriteria, final Delta.CompatibilityType expectedCompatibilityType,
                      final String v1, final String v2, final Set<String> excludes)
                              throws IllegalArgumentException, IOException, URISyntaxException {
        final VersionNumberString preVersionNumber = new VersionNumberString(v1);
        final File previousJar = new File("lib/v"+v1+"/"+jarFile);

        final VersionNumberString curVersionNumber = new VersionNumberString(v2);
        final File currentJar = new File("lib/v"+v2+"/"+jarFile);

        VersionSemanticsUtil.testVersion(diffCriteria, expectedCompatibilityType,
                                         previousJar, preVersionNumber,
                                         currentJar, curVersionNumber, excludes);
    }

    @Test
    public void testVersionV230V23x_00std() throws IllegalArgumentException, IOException, URISyntaxException {
        final Delta.CompatibilityType expectedCompatibilityType = Delta.CompatibilityType.NON_BACKWARD_COMPATIBLE;
        // final Delta.CompatibilityType expectedCompatibilityType = Delta.CompatibilityType.BACKWARD_COMPATIBLE_USER;

        final VersionNumberString preVersionNumber = new VersionNumberString("2.3.0");
        final File previousJar = new File("lib/v"+preVersionNumber.getVersionString()+"/"+jarFile);

        final ClassLoader currentCL = TestVersionSemanticsNOUI.class.getClassLoader();

        VersionSemanticsUtil.testVersion(diffCriteria, expectedCompatibilityType,
                                         previousJar, preVersionNumber,
                                         curVersion.getClass(), currentCL, curVersionNumber,
                                         excludesDefault);
    }
    @Test
    public void testVersionV230V23x_01patch() throws IllegalArgumentException, IOException, URISyntaxException {
        // final Delta.CompatibilityType expectedCompatibilityType = Delta.CompatibilityType.NON_BACKWARD_COMPATIBLE;
        // final Delta.CompatibilityType expectedCompatibilityType = Delta.CompatibilityType.BACKWARD_COMPATIBLE_USER;
        final Delta.CompatibilityType expectedCompatibilityType = Delta.CompatibilityType.BACKWARD_COMPATIBLE_BINARY;

        final VersionNumberString preVersionNumber = new VersionNumberString("2.3.0");
        final File previousJar = new File("lib/v"+preVersionNumber.getVersionString()+"/"+jarFile);

        final ClassLoader currentCL = TestVersionSemanticsNOUI.class.getClassLoader();

        VersionSemanticsUtil.testVersion(diffCriteria, expectedCompatibilityType,
                                         previousJar, preVersionNumber,
                                         curVersion.getClass(), currentCL, curVersionNumber,
                                         excludesStereoPackageAndAppletUtils);
    }

    public static void main(final String args[]) throws IOException {
        final String tstname = TestVersionSemanticsNOUI.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
