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

package com.jogamp.opengl.impl;

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
  private static final boolean DEBUG = GLContextImpl.DEBUG;
  private static final boolean DEBUG_AVAILABILITY = Debug.isPropertyDefined("jogl.debug.ExtensionAvailabilityCache", true);

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
    if(DEBUG) {
        System.out.println("ExtensionAvailabilityCache: Flush availability OpenGL "+context.getGLVersion());
    }
    availableExtensionCache.clear();
    initialized = false;
  }

  /**
   * Flush the cache and rebuild the cache.
   */
  public void reset() {
    flush();
    initAvailableExtensions();
  }

  public boolean isInitialized() {
    return initialized && !availableExtensionCache.isEmpty() ;
  }

  public boolean isExtensionAvailable(String glExtensionName) {
    initAvailableExtensions();
    return availableExtensionCache.contains(mapGLExtensionName(glExtensionName));
  }

  public String getPlatformExtensionsString() {
    initAvailableExtensions();
    return glXExtensions;
  }

  public String getGLExtensions() {
    initAvailableExtensions();
    if(DEBUG) {
        System.err.println("ExtensionAvailabilityCache: getGLExtensions() called");
    }
    return glExtensions;
  }

  private void initAvailableExtensions() {
    GL gl = context.getGL();
    // if hash is empty (meaning it was flushed), pre-cache it with the list
    // of extensions that are in the GL_EXTENSIONS string
    if (availableExtensionCache.isEmpty() || !initialized) {
      if (DEBUG) {
         System.err.println("ExtensionAvailabilityCache: Pre-caching init "+gl+", OpenGL "+context.getGLVersion());
      }

      boolean useGetStringi = false;

      if ( gl.isGL2GL3() ) {
          if ( ! gl.isFunctionAvailable("glGetStringi") ) {
            if(DEBUG) {
                System.err.println("GLContext: GL >= 3.1 usage, but no glGetStringi");
            }
          } else {
              useGetStringi = true;
          }
      }

      if (DEBUG) {
        System.err.println("ExtensionAvailabilityCache: Pre-caching extension availability OpenGL "+context.getGLVersion()+
                           ", use "+ ( useGetStringi ? "glGetStringi" : "glGetString" ) );
      }

      StringBuffer sb = new StringBuffer();
      if(useGetStringi) {
        GL2GL3 gl2gl3 = gl.getGL2GL3();
        int[] numExtensions = { 0 } ;
        gl2gl3.glGetIntegerv(gl2gl3.GL_NUM_EXTENSIONS, numExtensions, 0);
        for (int i = 0; i < numExtensions[0]; i++) {
            sb.append(gl2gl3.glGetStringi(gl2gl3.GL_EXTENSIONS, i));
            if(i < numExtensions[0]) {
                sb.append(" ");
            }
        }
      } else {
        sb.append(gl.glGetString(GL.GL_EXTENSIONS));
      }
      glExtensions = sb.toString();
      glXExtensions = context.getPlatformExtensionsString();

      sb.append(" ");
      sb.append(glXExtensions);

      String allAvailableExtensions = sb.toString();
      if (DEBUG_AVAILABILITY) {
        System.err.println("ExtensionAvailabilityCache: Available extensions: " + allAvailableExtensions);
        System.err.println("ExtensionAvailabilityCache: GL vendor: " + gl.glGetString(GL.GL_VENDOR));
      }
      StringTokenizer tok = new StringTokenizer(allAvailableExtensions);
      while (tok.hasMoreTokens()) {
        String availableExt = tok.nextToken().trim();
        availableExt = availableExt.intern();
        availableExtensionCache.add(availableExt);
        if (DEBUG_AVAILABILITY) {
          System.err.println("ExtensionAvailabilityCache:   Available: " + availableExt);
        }
      }

      int major[] = new int[] { context.getGLVersionMajor() };
      int minor[] = new int[] { context.getGLVersionMinor() };
      if( !gl.isGL3() && !gl.isGL4() &&
            ( major[0] > 3 ||
              ( major[0] == 3 && minor[0] >= 1 ) ) ) {
            // downsize version to 3.0 in case we are not using GL3 (>=3.1)
            major[0] = 3;
            minor[0] = 0;
      }
      while (GLContext.isValidGLVersion(major[0], minor[0])) {
        availableExtensionCache.add("GL_VERSION_" + major[0] + "_" + minor[0]);
        if (DEBUG) {
            System.err.println("ExtensionAvailabilityCache: Added GL_VERSION_" + major[0] + "_" + minor[0] + " to known extensions");
        }
        if(!GLContext.decrementGLVersion(major, minor)) break;
      }

      // put a dummy var in here so that the cache is no longer empty even if
      // no extensions are in the GL_EXTENSIONS string
      availableExtensionCache.add("<INTERNAL_DUMMY_PLACEHOLDER>");

      initialized = true;
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

  private boolean initialized = false;
  private String glExtensions = null;
  private String glXExtensions = null;
  private HashSet availableExtensionCache = new HashSet(50);
  private GLContextImpl context;

}
