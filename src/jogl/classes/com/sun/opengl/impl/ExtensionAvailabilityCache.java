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
  private static final boolean DEBUG_AVAILABILITY = Debug.isPropertyDefined("ExtensionAvailabilityCache", true);

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
        System.out.println("ExtensionAvailabilityCache: Flush availability OpenGL "+majorVersion+"."+minorVersion);
    }
    availableExtensionCache.clear();
    initialized = false;
    majorVersion = 1;
    minorVersion = 0;
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

  public int getMajorVersion() {
    initAvailableExtensions();
    return majorVersion;
  }

  public int getMinorVersion() {
    initAvailableExtensions();
    return minorVersion;
  }

  private void initAvailableExtensions() {
    // if hash is empty (meaning it was flushed), pre-cache it with the list
    // of extensions that are in the GL_EXTENSIONS string
    if (availableExtensionCache.isEmpty() || !initialized) {
      GL gl = context.getGL();

      if (DEBUG) {
         System.err.println("ExtensionAvailabilityCache: Pre-caching init "+gl+", GL_VERSION "+gl.glGetString(GL.GL_VERSION));
      }

      // Set version
      Version version = new Version(gl.glGetString(GL.GL_VERSION));
      if (version.isValid()) {
        majorVersion = version.getMajor();
        minorVersion = version.getMinor();

        if( !gl.isGL3() &&
            ( majorVersion > 3 ||
              ( majorVersion == 3 && minorVersion >= 1 ) ) ) {
            // downsize version to 3.0 in case we are not using GL3 (3.1)
            majorVersion = 3;
            minorVersion = 0;
        }
      }

      boolean useGetStringi = false;

      if ( majorVersion > 3 ||
           ( majorVersion == 3 && minorVersion >= 0 ) ||
           gl.isGL3() ) {
          if ( ! gl.isGL2GL3() ) {
            if(DEBUG) {
                System.err.println("ExtensionAvailabilityCache: GL >= 3.1 usage, but no GL2GL3 interface: "+gl.getClass().getName());
            }
          } else if ( ! gl.isFunctionAvailable("glGetStringi") ) {
            if(DEBUG) {
                System.err.println("ExtensionAvailabilityCache: GL >= 3.1 usage, but no glGetStringi");
            }
          } else {
              useGetStringi = true;
          }
      }

      if (DEBUG) {
        System.err.println("ExtensionAvailabilityCache: Pre-caching extension availability OpenGL "+majorVersion+"."+minorVersion+
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

      // Put GL version strings in the table as well
      // FIXME: this needs to be adjusted when the major rev changes
      // beyond the known ones
      int major = majorVersion;
      int minor = minorVersion;
      while (major > 0) {
        while (minor >= 0) {
          availableExtensionCache.add("GL_VERSION_" + major + "_" + minor);
          if (DEBUG) {
            System.err.println("ExtensionAvailabilityCache: Added GL_VERSION_" + major + "_" + minor + " to known extensions");
          }
          --minor;
        }

        switch (major) {
        case 3:
          if(gl.isGL3()) {
              // GL3 is a GL 3.1 forward compatible context,
              // hence no 2.0, 1.0 - 1.5 GL versions are supported.
              major=0; 
          }
          // Restart loop at version 2.1
          minor = 1;
          break;
        case 2:
          // Restart loop at version 1.5
          minor = 5;
          break;
        case 1:
          break;
        }

        --major;
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
  private int majorVersion = 1;
  private int minorVersion = 0;
  private String glExtensions = null;
  private String glXExtensions = null;
  private HashSet availableExtensionCache = new HashSet(50);
  private GLContextImpl context;

  /**
   * A class for storing and comparing OpenGL version numbers.
   * This only works for desktop OpenGL at the moment.
   */
  private static class Version implements Comparable
  {
    private boolean valid;
    private int major, minor, sub;
    public Version(int majorRev, int minorRev, int subMinorRev)
    {
      major = majorRev;
      minor = minorRev;
      sub = subMinorRev;
    }

    /**
     * @param versionString must be of the form "GL_VERSION_X" or
     * "GL_VERSION_X_Y" or "GL_VERSION_X_Y_Z" or "X.Y", where X, Y,
     * and Z are integers.
     *
     * @exception IllegalArgumentException if the argument is not a valid
     * OpenGL version identifier
     */
    public Version(String versionString)
    {
      try 
      {
        if (versionString.startsWith("GL_VERSION_"))
        {
          StringTokenizer tok = new StringTokenizer(versionString, "_");

          tok.nextToken(); // GL_
          tok.nextToken(); // VERSION_ 
          if (!tok.hasMoreTokens()) { major = 0; return; }
          major = Integer.valueOf(tok.nextToken()).intValue();
          if (!tok.hasMoreTokens()) { minor = 0; return; }
          minor = Integer.valueOf(tok.nextToken()).intValue();
          if (!tok.hasMoreTokens()) { sub = 0; return; }
          sub = Integer.valueOf(tok.nextToken()).intValue();
        }
        else
        {
          int radix = 10;
          if (versionString.length() > 2) {
            if (Character.isDigit(versionString.charAt(0)) &&
                versionString.charAt(1) == '.' &&
                Character.isDigit(versionString.charAt(2))) {
              major = Character.digit(versionString.charAt(0), radix);
              minor = Character.digit(versionString.charAt(2), radix);

              // See if there's version-specific information which might
              // imply a more recent OpenGL version
              StringTokenizer tok = new StringTokenizer(versionString, " ");
              if (tok.hasMoreTokens()) {
                tok.nextToken();
                if (tok.hasMoreTokens()) {
                  String token = tok.nextToken();
                  int i = 0;
                  while (i < token.length() && !Character.isDigit(token.charAt(i))) {
                    i++;
                  }
                  if (i < token.length() - 2 &&
                      Character.isDigit(token.charAt(i)) &&
                      token.charAt(i+1) == '.' &&
                      Character.isDigit(token.charAt(i+2))) {
                    int altMajor = Character.digit(token.charAt(i), radix);
                    int altMinor = Character.digit(token.charAt(i+2), radix);
                    // Avoid possibly confusing situations by putting some
                    // constraints on the upgrades we do to the major and
                    // minor versions
                    if ((altMajor == major && altMinor > minor) ||
                        altMajor == major + 1) {
                      major = altMajor;
                      minor = altMinor;
                    }
                  }
                }
              }
            }
          }
        }
        valid = true;
      }
      catch (Exception e)
      {
        // FIXME: refactor desktop OpenGL dependencies and make this
        // class work properly for OpenGL ES
        System.err.println("ExtensionAvailabilityCache: FunctionAvailabilityCache.Version.<init>: "+e);
        major = 1;
        minor = 0;
        /*
        throw (IllegalArgumentException)
          new IllegalArgumentException(
            "Illegally formatted version identifier: \"" + versionString + "\"")
              .initCause(e);
        */
      }
    }

    public boolean isValid() {
      return valid;
    }

    public int compareTo(Object o)
    {
      Version vo = (Version)o;
      if (major > vo.major) return 1; 
      else if (major < vo.major) return -1; 
      else if (minor > vo.minor) return 1; 
      else if (minor < vo.minor) return -1; 
      else if (sub > vo.sub) return 1; 
      else if (sub < vo.sub) return -1; 

      return 0; // they are equal
    }

    public int getMajor() {
      return major;
    }

    public int getMinor() {
      return minor;
    }
    
  } // end class Version
}
