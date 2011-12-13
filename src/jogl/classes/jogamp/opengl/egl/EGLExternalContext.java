/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package jogamp.opengl.egl;

import javax.media.opengl.*;
import jogamp.opengl.*;
import javax.media.nativewindow.*;

public class EGLExternalContext extends EGLContext {
    private GLContext lastContext;

    public EGLExternalContext(AbstractGraphicsScreen screen) {
        super(null, null);
        GLContextShareSet.contextCreated(this);
        setGLFunctionAvailability(false, 0, 0, CTX_IS_ARB_CREATED|CTX_PROFILE_ES|CTX_OPTION_ANY);
        getGLStateTracker().setEnabled(false); // external context usage can't track state in Java
    }

    public int makeCurrent() throws GLException {
        // Save last context if necessary to allow external GLContexts to
        // talk to other GLContexts created by this library
        GLContext cur = getCurrent();
        if (cur != null && cur != this) {
            lastContext = cur;
            setCurrent(null);
        }
        return super.makeCurrent();
    }  

    public void release() throws GLException {
        super.release();
        setCurrent(lastContext);
        lastContext = null;
    }

    protected void makeCurrentImpl() throws GLException {
    }

    protected void releaseImpl() throws GLException {
    }

    protected void destroyImpl() throws GLException {
    }

    public void bindPbufferToTexture() {
        throw new GLException("Should not call this");
    }

    public void releasePbufferFromTexture() {
        throw new GLException("Should not call this");
    }

}
