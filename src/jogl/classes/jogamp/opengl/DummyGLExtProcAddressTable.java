/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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
package jogamp.opengl;

import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.common.util.SecurityUtil;

/**
 * Representing the non-existing platform GL extension, i.e. a dummy type.
 * <p>
 * This table is a cache of pointers to the dynamically-linkable C library.
 * </p>
 * @see ProcAddressTable
 */
public final class DummyGLExtProcAddressTable extends ProcAddressTable {

  public DummyGLExtProcAddressTable(){ super(); }

  public DummyGLExtProcAddressTable(final com.jogamp.gluegen.runtime.FunctionAddressResolver resolver){ super(resolver); }

  @Override
  protected boolean isFunctionAvailableImpl(final String functionNameUsr) throws IllegalArgumentException  {
    return false;
  }
  @Override
  public long getAddressFor(final String functionNameUsr) throws SecurityException, IllegalArgumentException {
    SecurityUtil.checkAllLinkPermission();
    final String functionNameBase = com.jogamp.gluegen.runtime.opengl.GLNameResolver.normalizeVEN(com.jogamp.gluegen.runtime.opengl.GLNameResolver.normalizeARB(functionNameUsr, true), true);
    // The user is calling a bogus function or one which is not
    // runtime linked
    throw new RuntimeException(
        "WARNING: Address field query failed for \"" + functionNameBase + "\"/\"" + functionNameUsr +
        "\"; it's either statically linked or address field is not a known " +
        "function");
  }
}
