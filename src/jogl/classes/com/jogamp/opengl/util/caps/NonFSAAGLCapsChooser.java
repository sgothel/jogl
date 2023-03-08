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
package com.jogamp.opengl.util.caps;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLCapabilitiesChooser;

/**
 * Custom {@link GLCapabilitiesChooser}, filtering out all full screen anti-aliasing (FSAA, multisample) capabilities,
 * i.e. all matching {@link GLCapabilitiesImmutable} with {@link GLCapabilitiesImmutable#getSampleBuffers()}.
 */
public class NonFSAAGLCapsChooser extends DefaultGLCapabilitiesChooser {
    private final boolean verbose;
    public NonFSAAGLCapsChooser(final boolean verbose) {
        this.verbose = verbose;
    }
    public NonFSAAGLCapsChooser() {
        this.verbose = false;
    }

    @Override
    public int chooseCapabilities(final CapabilitiesImmutable desired,
                                  final List<? extends CapabilitiesImmutable> available,
                                  int recommendedIdx) {
        final GLCapabilitiesImmutable recommended;
        if( 0 <= recommendedIdx && recommendedIdx < available.size() ) {
            recommended = (GLCapabilitiesImmutable) available.get(recommendedIdx);
        } else {
            recommended = null;
            recommendedIdx = -1;
        }
        final List<GLCapabilitiesImmutable> clean = new ArrayList<GLCapabilitiesImmutable>();
        for (int i = 0; i < available.size(); i++) {
            final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) available.get(i);
            if ( caps.getSampleBuffers() ) {
                /** if( caps.equals(recommended) ) { // the matching index is enough!
                    System.err.println("Dropping["+i+"] "+caps+", matched recommended["+recommendedIdx+"] = "+recommended);
                    recommendedIdx = -1;
                } else */
                if( recommendedIdx == i ) {
                    if( verbose ) {
                        System.err.println("Dropping["+i+"] "+caps+", sameidx recommended["+recommendedIdx+"] = "+recommended);
                    }
                    recommendedIdx = -1;
                } else if( verbose ) {
                    System.err.println("Dropping "+caps+" != recommended["+recommendedIdx+"]");
                }
            } else {
                clean.add(caps);
            }
        }
        return super.chooseCapabilities(desired, clean, recommendedIdx);
    }
}
