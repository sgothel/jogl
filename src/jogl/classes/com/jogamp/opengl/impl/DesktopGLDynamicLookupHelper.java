/*
 * Copyright (c) 2010, Sven Gothel
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Sven Gothel nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Sven Gothel BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jogamp.opengl.impl;

/**
 * Abstract implementation of the DynamicLookupHelper for GL,
 * which decouples it's dependencies to EGLDrawableFactory.
 *
 * Currently two implementations exist, one for ES1 and one for ES2.
 */
public abstract class DesktopGLDynamicLookupHelper extends GLDynamicLookupHelper {
    private boolean hasGLBinding = false;
    private boolean hasGLES12Binding = false;

    public boolean hasGLBinding() { return hasGLBinding; }
    public boolean hasGLES12Binding() { return hasGLES12Binding; }

    protected void loadGLJNILibrary() {
        Throwable t=null;

        try {
            GLJNILibLoader.loadGLDesktop();
            hasGLBinding = true;
        } catch (UnsatisfiedLinkError ule) {
            t=ule;
        } catch (SecurityException se) {
            t=se;
        } catch (NullPointerException npe) {
            t=npe;
        } catch (RuntimeException re) {
            t=re;
        }
        if(DEBUG && null!=t) {
            System.err.println("DesktopGLDynamicLookupHelper: Desktop GL Binding Library not available");
            t.printStackTrace();
        }

        try {
            GLJNILibLoader.loadGLDesktopES12();
            hasGLES12Binding = true;
        } catch (UnsatisfiedLinkError ule) {
            t=ule;
        } catch (SecurityException se) {
            t=se;
        } catch (NullPointerException npe) {
            t=npe;
        } catch (RuntimeException re) {
            t=re;
        }
        if(DEBUG && null!=t) {
            System.err.println("DesktopGLDynamicLookupHelper: Desktop GLES12 Binding Library not available");
            t.printStackTrace();
        }
    }
}

