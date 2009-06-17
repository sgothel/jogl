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
 */

package javax.media.nativewindow;

/** Provides an interface for locking and unlocking the underlying
    window toolkit, where this is necessary in the OpenGL
    implementation. This mechanism is generally only needed on X11
    platforms. Currently it is only used when the AWT is in use.
    Implementations of this lock, if they are not no-ops, must support
    reentrant locking and unlocking. <P>
    
    The ToolkitLock implementation can be aquired by 
    {@link NativeWindowFactory#getToolkitLock NativeWindowFactory's getToolkitLock()}.<P>

    All toolkit shared resources shall be accessed by encapsulating the
    code with a locking block as follows.
    <PRE>
    NativeWindowFactory.getDefaultFactory().getToolkitLock().lock();
    try {
      long displayHandle = X11Util.getThreadLocalDefaultDisplay();
      ... some code dealing with shared resources 
      ... ie the window surface
    } finally {
      NativeWindowFactory.getDefaultFactory().getToolkitLock().unlock();
    }
    </PRE><P>

    The underlying toolkit's locking mechanism may relate to {@link NativeWindow}'s 
    {@link NativeWindow#lockSurface lockSurface()}. Hence it is important
    that both implementation harmonize well, ie {@link NativeWindow#lockSurface lockSurface()}
    shall issue a ToolkitLock lock befor it aquires it's surface lock. This is true
    in the AWT implementation for example. Otherwise the surface lock would <i>steal</i>
    the ToolkitLock's lock and a deadlock would be unavoidable.<P>

    However the necessity of needing a global state synchronization will of course
    impact your performance very much, especially in case of a multithreaded/multiwindow case.
    */
public interface ToolkitLock {
    /** Locks the toolkit. */
    public void lock();

    /** Unlocks the toolkit. */
    public void unlock();
}
