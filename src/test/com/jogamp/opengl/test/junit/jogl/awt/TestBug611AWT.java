
package com.jogamp.opengl.test.junit.jogl.awt;

import java.awt.Desktop;
import java.io.File;

import com.jogamp.opengl.GLProfile;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * As reported in Bug 611, on Windows XP is a performance issue:
 * After JOGL initialization there seems to be a huge time lag
 * when trying to open the Desktop folder.
 * <p>
 * Test disabled since showing the Desktop folder will
 * disturb the 'desktop' .. if there is another way to show
 * the performance bug, pls do so.
 * </p>
 * <p>
 * Since Windows XP is out of life .. we may not care ..
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug611AWT extends UITestCase {

    @Test
    public void test00() {
        // make junit happy
    }

    // @Test
    public void test01() {
        try {
            // System.setProperty("jogamp.gluegen.UseTempJarCache", "false");
            GLProfile.initSingleton();
            Desktop desktop;
            if (Desktop.isDesktopSupported()) {
                desktop = Desktop.getDesktop();
            } else {
                desktop = null;
            }
            if(null != desktop) {
                final String home = System.getProperty("user.home");
                File homeFolder = null;
                if(null != home) {
                    {
                        final File tst = new File(home + "/Desktop");
                        if( tst.canRead() ) {
                            homeFolder = tst;
                        }
                    }
                    if(null == homeFolder) {
                        final File tst = new File(home);
                        if( tst.canRead() ) {
                            homeFolder = tst;
                        }
                    }
                }
                if(null == homeFolder) {
                    if(Platform.getOSType() == Platform.OSType.WINDOWS) {
                        homeFolder = new File("c:\\");
                    } else {
                        homeFolder = new File("/");
                    }
                }
                if(null != homeFolder) {
                    desktop.open(homeFolder);
                }
            }
        } catch(final Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestBug611AWT.class.getName());
    }
}
