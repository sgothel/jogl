/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.opengl.impl;

import javax.media.opengl.*;
import java.util.*;
// FIXME: refactor Java SE dependencies
//import java.util.regex.*;
import java.lang.reflect.*;

/**
 * A utility object intended to be used by implementations to act as a cache
 * of which OpenGL extensions are currently available on both the host machine
 * and display.
 */
public final class ExtensionAvailabilityCache {
  private static final boolean DEBUG = Debug.debug("ExtensionAvailabilityCache");

  ExtensionAvailabilityCache(GLContextImpl context)
  {
    this.context = context;
  }

  /**
   * Flush the cache. The cache will be rebuilt lazily as calls to {@link
   * #isExtensionAvailable(String)} are received.
   */
  public void flush()
  {
    availableExtensionCache.clear();
  }

  public boolean isExtensionAvailable(String glExtensionName) {
    initAvailableExtensions();
    return availableExtensionCache.contains(mapGLExtensionName(glExtensionName));
  }

  protected void initAvailableExtensions() {
    // if hash is empty (meaning it was flushed), pre-cache it with the list
    // of extensions that are in the GL_EXTENSIONS string
    if (availableExtensionCache.isEmpty()) {
      GL gl = context.getGL();
      if (DEBUG) {
        System.err.println("!!! Pre-caching extension availability");
      }
      String allAvailableExtensions =
        gl.glGetString(GL.GL_EXTENSIONS) + " " + context.getPlatformExtensionsString();
      if (DEBUG) {
        System.err.println("!!! Available extensions: " + allAvailableExtensions);
        System.err.println("!!! GL vendor: " + gl.glGetString(GL.GL_VENDOR));
      }
      StringTokenizer tok = new StringTokenizer(allAvailableExtensions);
      while (tok.hasMoreTokens()) {
        String availableExt = tok.nextToken().trim();
        availableExt = availableExt.intern();
        availableExtensionCache.add(availableExt);
        if (DEBUG) {
          System.err.println("!!!   Available: " + availableExt);
        }
      }

      // put a dummy var in here so that the cache is no longer empty even if
      // no extensions are in the GL_EXTENSIONS string
      availableExtensionCache.add("<INTERNAL_DUMMY_PLACEHOLDER>");
    }
  }

  // FIXME: hack to re-enable GL_NV_vertex_array_range extension after
  // recent upgrade to new wglext.h and glxext.h headers
  private static String mapGLExtensionName(String extensionName) {
    if (extensionName != null &&
        (extensionName.equals("WGL_NV_vertex_array_range") ||
         extensionName.equals("GLX_NV_vertex_array_range"))) 
      return "GL_NV_vertex_array_range";
    return extensionName;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private HashSet availableExtensionCache = new HashSet(50);
  private GLContextImpl context;
}
