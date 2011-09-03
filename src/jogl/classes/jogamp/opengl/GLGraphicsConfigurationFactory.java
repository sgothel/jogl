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

package jogamp.opengl;

import java.util.List;
import javax.media.nativewindow.CapabilitiesChooser;
import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeWindowException;
import javax.media.opengl.DefaultGLCapabilitiesChooser;

public abstract class GLGraphicsConfigurationFactory extends GraphicsConfigurationFactory {

    protected static int chooseCapabilities(CapabilitiesChooser chooser, CapabilitiesImmutable capsRequested,
                                            List /*<CapabilitiesImmutable>*/ availableCaps, int recommendedIndex) {
        if (null == capsRequested) {
            throw new NativeWindowException("Null requested capabilities");
        }
        if ( 0 == availableCaps.size() ) {
            if (DEBUG) {
                System.err.println("Empty available capabilities");
            }
            return -1; // none available
        }

        if(null == chooser && 0 <= recommendedIndex && capsRequested.isBackgroundOpaque()) {
            if (DEBUG) {
                System.err.println("chooseCapabilities: Using recommendedIndex (opaque): idx " + recommendedIndex);
            }
            return recommendedIndex;
        }
        int chosenIndex = recommendedIndex;

        if (null == chooser) {
            chooser = new DefaultGLCapabilitiesChooser();
        }

        try {
            chosenIndex = chooser.chooseCapabilities(capsRequested, availableCaps, recommendedIndex);
            if(0 <= chosenIndex) {
                if (DEBUG) {
                    System.err.println("chooseCapabilities: Chosen idx " + chosenIndex);
                }
                return chosenIndex;
            }
        } catch (NativeWindowException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
        }

        // keep on going ..
        // seek first available one ..
        for (chosenIndex = 0; chosenIndex < availableCaps.size() && availableCaps.get(chosenIndex) == null; chosenIndex++) {
            // nop
        }
        if (chosenIndex == availableCaps.size()) {
            // give up ..
            if (DEBUG) {
                System.err.println("chooseCapabilities: Failed .. nothing available, bail out");
            }
            return -1;
        }
        if (DEBUG) {
            System.err.println("chooseCapabilities: Fall back to 1st available idx " + chosenIndex);
        }

        return chosenIndex;
    }

}
