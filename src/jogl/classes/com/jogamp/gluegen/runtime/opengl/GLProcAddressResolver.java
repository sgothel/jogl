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

/*
 * Created on Saturday, April 24 2010 16:44
 */
package com.jogamp.gluegen.runtime.opengl;

import com.jogamp.common.os.DynamicLookupHelper;
import com.jogamp.gluegen.runtime.FunctionAddressResolver;

/**
 * @author Michael Bien, et.al.
 */
public class GLProcAddressResolver implements FunctionAddressResolver {

    public static final boolean DEBUG = false;

    @Override
    public long resolve(final String name, final DynamicLookupHelper lookup) throws SecurityException {
        long newProcAddress = 0;
        final int permutations = GLNameResolver.getFuncNamePermutationNumber(name);

        for (int i = 0; 0 == newProcAddress && i < permutations; i++) {
            final String funcName = GLNameResolver.getFuncNamePermutation(name, i);
            try {
                newProcAddress = lookup.dynamicLookupFunction(funcName);
            } catch (final Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        return newProcAddress;
    }
}
