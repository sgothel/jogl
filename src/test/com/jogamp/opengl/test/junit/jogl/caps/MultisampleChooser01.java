/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */
package com.jogamp.opengl.test.junit.jogl.caps;

import java.util.List;

import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;

class MultisampleChooser01 extends DefaultGLCapabilitiesChooser {

    @Override
    public int chooseCapabilities(final CapabilitiesImmutable desired,
                                  final List<? extends CapabilitiesImmutable> available,
                                  final int windowSystemRecommendedChoice) {
        boolean anyHaveSampleBuffers = false;
        for (int i = 0; i < available.size(); i++) {
            final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) available.get(i);
            if ( caps.getSampleBuffers() ) {
                anyHaveSampleBuffers = true;
                break;
            }
        }
        final int selection = super.chooseCapabilities(desired, available, windowSystemRecommendedChoice);
        if (!anyHaveSampleBuffers) {
            System.err.println("WARNING: antialiasing will be disabled because none of the available pixel formats had it to offer");
        } else {
            final GLCapabilitiesImmutable selected = (GLCapabilitiesImmutable) available.get(selection);
            if (!selected.getSampleBuffers()) {
                System.err.println("WARNING: antialiasing will be disabled because the DefaultGLCapabilitiesChooser didn't supply it");
            }
        }
        return selection;
    }
}
