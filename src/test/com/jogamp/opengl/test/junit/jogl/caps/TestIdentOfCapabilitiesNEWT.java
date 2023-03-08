/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.caps;

import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.JoglVersion;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.junit.util.SingletonJunitCase;
import com.jogamp.nativewindow.AbstractGraphicsDevice;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestIdentOfCapabilitiesNEWT extends SingletonJunitCase {

  public static void main(final String[] args) {
     final String tstname = TestIdentOfCapabilitiesNEWT.class.getName();
     org.junit.runner.JUnitCore.main(tstname);
  }

  @Test
  public void test01DesktopCapsEquals() throws InterruptedException {
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(false);
        if( null == factory ) {
            System.err.println("No desktop factory available, bailing out");
            return;
        }
        testEquals(factory);
  }
  @Test
  public void test02EGLCapsEquals() throws InterruptedException {
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(true);
        if( null == factory ) {
            System.err.println("No EGL factory available, bailing out");
            return;
        }
        testEquals(factory);
  }

  private static List<GLCapabilitiesImmutable> getEquals(final GLCapabilitiesImmutable caps, final List<GLCapabilitiesImmutable> availCaps) {
      final List<GLCapabilitiesImmutable> res = new ArrayList<GLCapabilitiesImmutable>();
      for(final GLCapabilitiesImmutable c : availCaps) {
          if( c.equals(caps) ) {
              res.add(c);
          }
      }
      return res;
  }

  private void testEquals(final GLDrawableFactory factory) {
      final AbstractGraphicsDevice device = factory.getDefaultDevice();
      Assert.assertNotNull(device);
      // System.err.println(JoglVersion.getDefaultOpenGLInfo(device, null, true).toString());

      try {
          final List<GLCapabilitiesImmutable> availCaps = factory.getAvailableCapabilities(device);
          if( null != availCaps ) {
              for(int i=0; i < availCaps.size(); i++) {
                  final GLCapabilitiesImmutable one = availCaps.get(i);
                  final List<GLCapabilitiesImmutable> res = getEquals(one, availCaps);
                  if( 1 != res.size() ) {
                      // oops
                      System.err.println("Error: "+one+" matches more than one in list, not unique:");
                      res.forEach(System.err::println);
                  }
                  Assert.assertEquals(1,  res.size());
                  System.err.printf("#%3d/%d: %s%n", (i+1), availCaps.size(), one);
              }
          }
      } catch (final GLException gle) { /* n/a */ }
  }


}
