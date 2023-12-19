/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.ui.widgets;

import com.jogamp.graph.ui.Group;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.Group.Layout;

/**
 * A widget specifies specific UI semantics including individual controls.
 * <p>
 * Being a {@link Group}, implementations provide shape(s) and its instance can be added to the user's scene.
 * </p>
 * <p>
 * Due to the specific nature of widgets,
 * individual controls/listener may be provided with semantic values.
 * </p>
 */
public class Widget extends Group {
    /**
     * Create a Widget group of {@link Shape}s w/o {@link Group.Layout}.
     * <p>
     * Default is non-interactive, see {@link #setInteractive(boolean)}.
     * </p>
     */
    public Widget() {
        super(null);
    }

    /**
     * Create a Widget group of {@link Shape}s w/ given {@link Group.Layout}.
     * <p>
     * Default is non-interactive, see {@link #setInteractive(boolean)}.
     * </p>
     * @param l optional {@link Layout}, maybe {@code null}
     */
    public Widget(final Layout l) {
        super(l);
    }
}
