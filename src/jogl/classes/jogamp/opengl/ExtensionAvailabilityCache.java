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

package jogamp.opengl;

import java.util.HashMap;
import java.util.StringTokenizer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLContext;

import com.jogamp.common.util.VersionNumber;

/**
 * A utility object intended to be used by implementations to act as a cache
 * of which OpenGL extensions are currently available on both the host machine
 * and display.
 */
final class ExtensionAvailabilityCache {
  protected static final boolean DEBUG = GLContextImpl.DEBUG;

  ExtensionAvailabilityCache() {
      flush();
  }

  /**
   * Flush the cache.
   */
  final void flush()
  {
    initialized = false;
    glExtensions = null;
    glExtensionCount = 0;
    glXExtensions = null;
    glXExtensionCount = 0;
    availableExtensionCache.clear();
  }

  /**
   * Flush and rebuild the cache.
   */
  final void reset(final GLContextImpl context) {
    flush();
    initAvailableExtensions(context);
  }

  final boolean isInitialized() {
    return initialized;
  }

  final int getTotalExtensionCount() {
    validateInitialization();
    return availableExtensionCache.size();
  }

  final boolean isExtensionAvailable(final String glExtensionName) {
    validateInitialization();
    return null != availableExtensionCache.get(glExtensionName);
  }

  final int getPlatformExtensionCount() {
    validateInitialization();
    return glXExtensionCount;
  }

  final String getPlatformExtensionsString() {
    validateInitialization();
    return glXExtensions;
  }

  final int getGLExtensionCount() {
    validateInitialization();
    return glExtensionCount;
  }

  final String getGLExtensionsString() {
    validateInitialization();
    if(DEBUG) {
        System.err.println("ExtensionAvailabilityCache: getGLExtensions() called");
    }
    return glExtensions;
  }

  private final void validateInitialization() {
      if (!isInitialized()) {
          throw new InternalError("ExtensionAvailabilityCache not initialized!");
      }
  }
  private final void initAvailableExtensions(final GLContextImpl context) {
      final GL gl = context.getGL();
      // if hash is empty (meaning it was flushed), pre-cache it with the list
      // of extensions that are in the GL_EXTENSIONS string
      if (isInitialized()) {
          throw new InternalError("ExtensionAvailabilityCache already initialized!");
      }
      if (DEBUG) {
          System.err.println(getThreadName() + ":ExtensionAvailabilityCache: Pre-caching init "+gl+", OpenGL "+context.getGLVersion());
      }

      boolean useGetStringi = false;

      // Use 'glGetStringi' only for ARB GL3 and ES3 context,
      // on GL2 platforms the function might be available, but not working.
      if ( context.isGL3() || context.isGLES3() ) {
          if ( ! context.isFunctionAvailable("glGetStringi") ) {
              if(DEBUG) {
                  System.err.println("GLContext: GL >= 3.1 usage, but no glGetStringi");
              }
          } else {
              useGetStringi = true;
          }
      }

      if (DEBUG) {
          System.err.println(getThreadName() + ":ExtensionAvailabilityCache: Pre-caching extension availability OpenGL "+context.getGLVersion()+
                  ", use "+ ( useGetStringi ? "glGetStringi" : "glGetString" ) );
      }

      if(useGetStringi) {
          final GL2ES3 gl2es3 = (GL2ES3)gl; // validated via context - OK!
          final int count;
          {
              final int[] val = { 0 } ;
              gl2es3.glGetIntegerv(GL2ES3.GL_NUM_EXTENSIONS, val, 0);
              count = val[0];
          }
          final StringBuilder sb = new StringBuilder();
          for (int i = 0; i < count; i++) {
              final String ext = gl2es3.glGetStringi(GL.GL_EXTENSIONS, i);
              if( null == availableExtensionCache.put(ext, ext) ) {
                  // new one
                  if( 0 < i ) {
                      sb.append(" ");
                  }
                  sb.append(ext);
              }
          }
          if(0==count || sb.length()==0) {
              // fall back ..
              useGetStringi=false;
          } else {
              glExtensions = sb.toString();
              glExtensionCount = count;
          }
      }
      if(!useGetStringi) {
          glExtensions = gl.glGetString(GL.GL_EXTENSIONS);
          if(null != glExtensions) {
              final StringTokenizer tok = new StringTokenizer(glExtensions);
              int count = 0;
              while (tok.hasMoreTokens()) {
                  final String ext = tok.nextToken().trim();
                  if( null == availableExtensionCache.put(ext, ext) ) {
                      count++;
                  }
              }
              glExtensionCount = count;
          }
      }
      if (DEBUG) {
          System.err.println(getThreadName() + ":ExtensionAvailabilityCache: GL_EXTENSIONS: "+glExtensionCount+
                                               ", used "+ ( useGetStringi ? "glGetStringi" : "glGetString" ) );
      }

      // Platform Extensions
      {
          // unify platform extension .. might have duplicates
          final StringBuilder sb = new StringBuilder();
          final StringTokenizer tok = new StringTokenizer(context.getPlatformExtensionsStringImpl().toString());
          int count = 0;
          while (tok.hasMoreTokens()) {
              final String ext = tok.nextToken().trim();
              if( null == availableExtensionCache.put(ext, ext) ) {
                  // new one
                  if( 0 < count ) {
                      sb.append(" ");
                  }
                  sb.append(ext);
                  count++;
              }
          }
          glXExtensions = sb.toString();
          glXExtensionCount = count;
      }

      if (DEBUG) {
          System.err.println(getThreadName() + ":ExtensionAvailabilityCache: GLX_EXTENSIONS: "+glXExtensionCount);
          System.err.println(getThreadName() + ":ExtensionAvailabilityCache: GL vendor: " + gl.glGetString(GL.GL_VENDOR));
          System.err.println(getThreadName() + ":ExtensionAvailabilityCache: ALL EXTENSIONS: "+availableExtensionCache.size());
      }

      final int ctxOptions = context.getCtxOptions();
      final VersionNumber version = context.getGLVersionNumber();
      final int major[] = new int[] { version.getMajor() };
      final int minor[] = new int[] { version.getMinor() };
      do{
          final String GL_XX_VERSION = ( context.isGLES() ? "GL_ES_VERSION_" : "GL_VERSION_" ) + major[0] + "_" + minor[0];
          availableExtensionCache.put(GL_XX_VERSION, GL_XX_VERSION);
          if (DEBUG) {
              System.err.println(getThreadName() + ":ExtensionAvailabilityCache: Added "+GL_XX_VERSION+" to known extensions");
          }
      } while( GLContext.decrementGLVersion(ctxOptions, major, minor) );

      // put a dummy var in here so that the cache is no longer empty even if
      // no extensions are in the GL_EXTENSIONS string
      availableExtensionCache.put("<INTERNAL_DUMMY_PLACEHOLDER>", "<INTERNAL_DUMMY_PLACEHOLDER>");

      initialized = true;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private boolean initialized = false;
  private String glExtensions = null;
  private int glExtensionCount = 0;
  private String glXExtensions = null;
  private int glXExtensionCount = 0;
  private final HashMap<String, String> availableExtensionCache = new HashMap<String, String>(100);

  static String getThreadName() { return Thread.currentThread().getName(); }

}
