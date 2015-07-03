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

package jogamp.nativewindow;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.jogamp.common.util.PropertyAccess;

/** Helper routines for logging and debugging. */

public class Debug extends PropertyAccess {
  // Some common properties
  private static final boolean verbose;
  private static final boolean debugAll;

  static {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
        @Override
        public Object run() {
            PropertyAccess.addTrustedPrefix("nativewindow.");
            return null;
    } } );

    verbose = isPropertyDefined("nativewindow.verbose", true);
    debugAll = isPropertyDefined("nativewindow.debug", true);
    if (verbose) {
       final Package p = Package.getPackage("com.jogamp.nativewindow");
       System.err.println("NativeWindow specification version " + p.getSpecificationVersion());
       System.err.println("NativeWindow implementation version " + p.getImplementationVersion());
       System.err.println("NativeWindow implementation vendor " + p.getImplementationVendor());
    }
  }

  /** Ensures static init block has been issues, i.e. if calling through to {@link PropertyAccess#isPropertyDefined(String, boolean)}. */
  public static final void initSingleton() {}

  public static final boolean verbose() {
    return verbose;
  }

  public static final boolean debugAll() {
    return debugAll;
  }

  public static final boolean debug(final String subcomponent) {
    return debugAll() || isPropertyDefined("nativewindow.debug." + subcomponent, true);
  }
}
