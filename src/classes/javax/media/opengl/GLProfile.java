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
 */

package javax.media.opengl;

import java.lang.reflect.*;
import java.security.*;
import com.sun.opengl.impl.*;

public class GLProfile {
  /** The desktop (OpenGL 2.0) profile */
  public static final String GL2   = "GL2";

  /** The OpenGL ES 1 (really, 1.1) profile */
  public static final String GLES1 = "GLES1";

  /** The OpenGL ES 2 (really, 2.0) profile */
  public static final String GLES2 = "GLES2";

  /** The JVM/process wide choosen GL profile **/
  private static String profile = null;

  public static final void setProfile(String profile) 
    throws GLException
  {
    if(null==GLProfile.profile) {
        // preset profile .. 1st come 1st serve
        GLProfile.profile = profile;
    } else {
        if(!GLProfile.profile.equals(profile)) {
              throw new GLException("Choosen profile ("+profile+") doesn't match preset one: "+GLProfile.profile);
        }
    }
  }

  public static final String getProfile() {
    return profile;
  }
  
  public static final boolean isGL2() {
    return GL2.equals(profile);
  }

  public static final boolean isGLES1() {
    return GLES1.equals(profile);
  }

  public static final boolean isGL2ES1() {
    return isGL2() || isGLES1();
  }

  public static final boolean isGLES2() {
    return GLES2.equals(profile);
  }

  public static final boolean isGL2ES2() {
    return isGL2() || isGLES2();
  }

  public static final boolean isGLES() {
    return isGLES2() || isGLES1();
  }

  public static final boolean matches(String test_profile) {
    return (null==test_profile)?false:test_profile.equals(profile);
  }

  public static final boolean instanceOf(Object obj, String clazzName) {
    Class clazz = obj.getClass();
    do {
        if(clazz.getName().equals(clazzName)) {
            return true;
        }
        clazz = clazz.getSuperclass();
    } while (clazz!=null);
    return false;
  }

  public static final boolean implementationOf(Object obj, String faceName) {
    Class[] clazzes = obj.getClass().getInterfaces();
    for(int i=clazzes.length-1; i>=0; i--) {
        Class face = clazzes[i];
        if(face.getName().equals(faceName)) {
            return true;
        }
    }
    return false;
  }

  public static final boolean implementationOfGL2(Object obj) {
    return implementationOf(obj, "javax.media.opengl.GL2");
  }

  public static final boolean implementationOfGLES1(Object obj) {
    return implementationOf(obj, "javax.media.opengl.GLES1");
  }

  public static final boolean implementationOfGLES2(Object obj) {
    return implementationOf(obj, "javax.media.opengl.GLES2");
  }

  public static final boolean implementationOfGLES(Object obj) {
    return implementationOfGLES1(obj) || implementationOfGLES2(obj);
  }

  public static final boolean implementationOfGL2ES1(Object obj) {
    return implementationOf(obj, "javax.media.opengl.GL2ES1");
  }

  public static final boolean implementationOfGL2ES2(Object obj) {
    return implementationOf(obj, "javax.media.opengl.GL2ES2");
  }

}

