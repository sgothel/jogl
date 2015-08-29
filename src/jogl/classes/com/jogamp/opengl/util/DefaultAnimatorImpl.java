/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.jogamp.opengl.util;

import java.util.ArrayList;

import com.jogamp.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.AnimatorBase.UncaughtAnimatorException;

/** Abstraction to factor out AWT dependencies from the Animator's
    implementation in a way that still allows the FPSAnimator to pick
    up this behavior if desired. */

class DefaultAnimatorImpl implements AnimatorBase.AnimatorImpl {
    @Override
    public void display(final ArrayList<GLAutoDrawable> drawables,
                        final boolean ignoreExceptions,
                        final boolean printExceptions) throws UncaughtAnimatorException {
        boolean hasException = false;
        for (int i=0; !hasException && i<drawables.size(); i++) {
            boolean catch1 = true;
            GLAutoDrawable drawable = null;
            try {
                drawable = drawables.get(i);
                catch1 = false;
                drawable.display();
            } catch (final Throwable t) {
                if( catch1 && t instanceof IndexOutOfBoundsException ) {
                    // concurrent pulling of GLAutoDrawables ..
                    hasException = true;
                } else if (ignoreExceptions) {
                    if (printExceptions) {
                        t.printStackTrace();
                    }
                } else {
                    throw new UncaughtAnimatorException(drawable, t);
                }
            }
        }
    }

    @Override
    public boolean blockUntilDone(final Thread thread) {
        return Thread.currentThread() != thread;
    }
}
