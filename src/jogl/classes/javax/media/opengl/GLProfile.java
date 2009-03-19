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

import javax.media.opengl.fixedfunc.*;
import java.lang.reflect.*;
import java.security.*;
import com.sun.opengl.impl.*;
import com.sun.nativewindow.impl.NWReflection;

public class GLProfile {
  //
  // Public (user-visible) profiles
  //

  /** The desktop (OpenGL 2.0) profile */
  public static final String GL2   = "GL2";

  /** The OpenGL ES 1 (really, 1.1) profile */
  public static final String GLES1 = "GLES1";

  /** The OpenGL ES 2 (really, 2.0) profile */
  public static final String GLES2 = "GLES2";

  /** The intersection of the desktop (OpenGL 2.0) and OpenGL ES 1.x profiles */
  public static final String GL2ES1 = "GL2ES1";

  /** The intersection of the desktop (OpenGL 2.0) and OpenGL ES 2.x profiles */
  public static final String GL2ES2 = "GL2ES2";

  //
  // Profiles which are implementation details
  //

  // The intersection between desktop OpenGL and the union of the OpenGL ES profiles
  // This is here only to avoid having separate GL2ES1Impl and GL2ES2Impl classes
  private static final String GL2ES12 = "GL2ES12";

  /** The JVM/process wide chosen GL profile **/
  private static String profile = null;

  /** The "real" profile implementing the chosen profile; for example,
      both GL2ES1 and GL2ES2 currently map to GL2ES12 */
  private static String realProfile = null;

  private static final Throwable tryLibrary() 
  {
    try {
        Class clazz = Class.forName(getGLImplBaseClassName()+"Impl");
        if(GL2.equals(realProfile)) {
            // See DRIHack.java for an explanation of why this is necessary
            DRIHack.begin();
            NativeLibLoader.loadGL2();
            DRIHack.end();
        } if(GL2ES12.equals(realProfile)) {
            // See DRIHack.java for an explanation of why this is necessary
            DRIHack.begin();
            NativeLibLoader.loadGL2ES12();
            DRIHack.end();
        } else if(GLES1.equals(realProfile) || GLES2.equals(realProfile)) {
            Object eGLDrawableFactory = NWReflection.createInstance("com.sun.opengl.impl.egl.EGLDrawableFactory");
            if(null==eGLDrawableFactory) {
                throw new GLException("com.sun.opengl.impl.egl.EGLDrawableFactory not available");
            }
        }
        System.out.println("Successfully loaded profile " + profile);
        return null;
    } catch (Throwable e) {
        if (Debug.debug("GLProfile")) {
            e.printStackTrace();
        }
        profile=null;
        realProfile = null;
        return e;
    }
  }

  private static void computeRealProfile() {
    if (GL2ES1.equals(profile) ||
        GL2ES2.equals(profile)) {
      realProfile = GL2ES12;
    } else {
      realProfile = profile;
    }
  }

  public static synchronized final void setProfile(String profile) 
    throws GLException
  {
    if(null==GLProfile.profile) {
        GLProfile.profile = profile;
        computeRealProfile();
        Throwable t = tryLibrary();
        if (GLProfile.profile == null) {
            throw new GLException("Profile " + profile + " not available", t);
        }
    } else {
        if(!GLProfile.profile.equals(profile)) {
              throw new GLException("Requested profile ("+profile+") doesn't match already chosen one: "+GLProfile.profile);
        }
    }
  }

  public static synchronized final void setProfile(String[] profiles) 
    throws GLException
  {
    Throwable t = null;
    for(int i=0; profile==null && i<profiles.length; i++) {
        profile = profiles[i];
        computeRealProfile();
        if (t == null) {
          t = tryLibrary();
        } else {
          tryLibrary();
        }
    }
    if(null==profile) {
      StringBuffer msg = new StringBuffer();
      msg.append("[");
      for (int i = 0; i < profiles.length; i++) {
          if (i > 0)
              msg.append(", ");
          msg.append(profiles[i]);
      }
      msg.append("]");
      throw new GLException("Profiles "+msg.toString()+" not available", t);
    }
  }

  /**
   * Selects a profile, implementing the interface GL2ES1.
   * Order: GL2ES1, GL2, GLES1
   */
  public static synchronized final void setProfileGL2ES1() {
    setProfile(new String[] { GL2ES1, GL2, GLES1 });
  }

  /**
   * Selects a profile, implementing the interface GL2ES2.
   * Order: GL2ES2, GL2, GLES2
   */
  public static synchronized final void setProfileGL2ES2() {
    setProfile(new String[] { GL2ES2, GL2, GLES2 });
  }

  /**
   * Selects a profile, implementing the interface GL
   * Order: GL2, GL2ES2, GL2ES1, GLES2, GLES1
   */
  public static synchronized final void setProfileGLAny() {
    setProfile(new String[] { GL2, GL2ES2, GL2ES1, GLES2, GLES1 });
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

  public static final boolean isGLES2() {
    return GLES2.equals(profile);
  }

  public static final boolean isGL2ES1() {
    return GL2ES1.equals(profile);
  }

  public static final boolean isGL2ES2() {
    return GL2ES2.equals(profile);
  }

  /** Indicates whether either of the OpenGL ES profiles are in use. */
  public static final boolean isGLES() {
    return isGLES2() || isGLES1();
  }

  public static final boolean matches(String test_profile) {
    return (null==test_profile)?false:test_profile.equals(profile);
  }

  public static final String getGLImplBaseClassName() {
        if(isGL2()) {
            return "com.sun.opengl.impl.gl2.GL2";
        } else if(isGL2ES1() || isGL2ES2()) {
            return "com.sun.opengl.impl.gl2es12.GL2ES12";
        } else if(isGLES1()) {
            return "com.sun.opengl.impl.es1.GLES1";
        } else if(isGLES2()) {
            return "com.sun.opengl.impl.es2.GLES2";
        } else {
            throw new GLException("unsupported profile \"" + profile + "\"");
        }
  }

  private static String getGLTypeName(int type) {
    switch (type) {
        case GL.GL_UNSIGNED_BYTE:
            return "GL_UNSIGNED_BYTE";
        case GL.GL_BYTE:
            return "GL_BYTE";
        case GL.GL_UNSIGNED_SHORT:
            return "GL_UNSIGNED_SHORT";
        case GL.GL_SHORT:
            return "GL_SHORT";
        case GL.GL_FLOAT:
            return "GL_FLOAT";
        case GL.GL_FIXED:
            return "GL_FIXED";
        case javax.media.opengl.GL2ES2.GL_INT:
            return "GL_INT";
        case javax.media.opengl.GL2ES2.GL_UNSIGNED_INT:
            return "GL_UNSIGNED_INT";
        case javax.media.opengl.GL2.GL_DOUBLE:
            return "GL_DOUBLE";
        case javax.media.opengl.GL2.GL_2_BYTES:
            return "GL_2_BYTES";
        case javax.media.opengl.GL2.GL_3_BYTES:
            return "GL_3_BYTES";
        case javax.media.opengl.GL2.GL_4_BYTES:
            return "GL_4_BYTES";
    }
    return null;
  }

  private static String getGLArrayName(int array) {
      switch(array) {
          case GLPointerFunc.GL_VERTEX_ARRAY:
              return "GL_VERTEX_ARRAY";
          case GLPointerFunc.GL_NORMAL_ARRAY:
              return "GL_NORMAL_ARRAY";
          case GLPointerFunc.GL_COLOR_ARRAY:
              return "GL_COLOR_ARRAY";
          case GLPointerFunc.GL_TEXTURE_COORD_ARRAY:
              return "GL_TEXTURE_COORD_ARRAY";
      }
      return null;
  }

  /** 
   * General validation if type is a valid GL data type
   * for the current profile
   */
  public static boolean isValidDataType(int type, boolean throwException) {
    switch(type) {
        case GL.GL_UNSIGNED_BYTE:
        case GL.GL_BYTE:
        case GL.GL_UNSIGNED_SHORT:
        case GL.GL_SHORT:
        case GL.GL_FLOAT:
        case GL.GL_FIXED:
            return true;
        case javax.media.opengl.GL2ES2.GL_INT:
        case javax.media.opengl.GL2ES2.GL_UNSIGNED_INT:
            if( isGL2ES2() ) {
                return true;
            }
        case javax.media.opengl.GL2.GL_DOUBLE:
        case javax.media.opengl.GL2.GL_2_BYTES:
        case javax.media.opengl.GL2.GL_3_BYTES:
        case javax.media.opengl.GL2.GL_4_BYTES:
            if( isGL2() ) {
                return true;
            }
    } 
    if(throwException) {
        throw new GLException("Illegal data type on profile "+GLProfile.getProfile()+": "+type);
    }
    return false;
  }

  public static boolean isValidArrayDataType(int index, int comps, int type, 
                                             boolean isVertexAttribPointer, boolean throwException) {
    String arrayName = getGLArrayName(index);
    if(GLProfile.isGLES1()) {
        if(isVertexAttribPointer) {
            if(throwException) {
                throw new GLException("Illegal array type for "+arrayName+" on profile GLES1: VertexAttribPointer");
            }
            return false;
        }
        switch(index) {
            case GLPointerFunc.GL_VERTEX_ARRAY:
            case GLPointerFunc.GL_TEXTURE_COORD_ARRAY:
                switch(type) {
                    case GL.GL_BYTE:
                    case GL.GL_SHORT:
                    case GL.GL_FIXED:
                    case GL.GL_FLOAT:
                        break;
                    default: 
                        if(throwException) {
                            throw new GLException("Illegal data type for "+arrayName+" on profile GLES1: "+type);
                        }
                        return false;
                }
                switch(comps) {
                    case 0:
                    case 2:
                    case 3:
                    case 4:
                        break;
                    default: 
                        if(throwException) {
                            throw new GLException("Illegal component number for "+arrayName+" on profile GLES1: "+comps);
                        }
                        return false;
                }
                break;
            case GLPointerFunc.GL_NORMAL_ARRAY:
                switch(type) {
                    case GL.GL_BYTE:
                    case GL.GL_SHORT:
                    case GL.GL_FIXED:
                    case GL.GL_FLOAT:
                        break;
                    default: 
                        if(throwException) {
                            throw new GLException("Illegal data type for "+arrayName+" on profile GLES1: "+type);
                        }
                        return false;
                }
                switch(comps) {
                    case 0:
                    case 3:
                        break;
                    default: 
                        if(throwException) {
                            throw new GLException("Illegal component number for "+arrayName+" on profile GLES1: "+comps);
                        }
                        return false;
                }
                break;
            case GLPointerFunc.GL_COLOR_ARRAY:
                switch(type) {
                    case GL.GL_UNSIGNED_BYTE:
                    case GL.GL_FIXED:
                    case GL.GL_FLOAT:
                        break;
                    default: 
                        if(throwException) {
                            throw new GLException("Illegal data type for "+arrayName+" on profile GLES1: "+type);
                        }
                        return false;
                }
                switch(comps) {
                    case 0:
                    case 4:
                        break;
                    default: 
                        if(throwException) {
                            throw new GLException("Illegal component number for "+arrayName+" on profile GLES1: "+comps);
                        }
                        return false;
                }
                break;
        }
    } else if(GLProfile.isGLES2()) {
        // simply ignore !isVertexAttribPointer case, since it is simulated anyway ..

        switch(type) {
            case GL.GL_UNSIGNED_BYTE:
            case GL.GL_BYTE:
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_SHORT:
            case GL.GL_FLOAT:
            case GL.GL_FIXED:
                break;
            default: 
                if(throwException) {
                    throw new GLException("Illegal data type for "+arrayName+" on profile GLES2: "+type);
                }
                return false;
        }
        switch(comps) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                break;
            default: 
                if(throwException) {
                    throw new GLException("Illegal component number for "+arrayName+" on profile GLES1: "+comps);
                }
                return false;
        }
    } else if(GLProfile.isGL2ES2() || GLProfile.isGL2()) {
        if(isVertexAttribPointer) {
            switch(type) {
                case GL.GL_UNSIGNED_BYTE:
                case GL.GL_BYTE:
                case GL.GL_UNSIGNED_SHORT:
                case GL.GL_SHORT:
                case GL.GL_FLOAT:
                case javax.media.opengl.GL2ES2.GL_INT:
                case javax.media.opengl.GL2ES2.GL_UNSIGNED_INT:
                case javax.media.opengl.GL2.GL_DOUBLE:
                    break;
                default: 
                    if(throwException) {
                        throw new GLException("Illegal data type for "+arrayName+" on profile GL2: "+type);
                    }
                    return false;
            }
            switch(comps) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    break;
                default: 
                    if(throwException) {
                        throw new GLException("Illegal component number for "+arrayName+" on profile GL2: "+comps);
                    }
                    return false;
            }
        } else {
            switch(index) {
                case GLPointerFunc.GL_VERTEX_ARRAY:
                    switch(type) {
                        case GL.GL_SHORT:
                        case GL.GL_FLOAT:
                        case javax.media.opengl.GL2ES2.GL_INT:
                        case javax.media.opengl.GL2.GL_DOUBLE:
                            break;
                        default: 
                            if(throwException) {
                                throw new GLException("Illegal data type for "+arrayName+" on profile GL2: "+type);
                            }
                            return false;
                    }
                    switch(comps) {
                        case 0:
                        case 2:
                        case 3:
                        case 4:
                            break;
                        default: 
                            if(throwException) {
                                throw new GLException("Illegal component number for "+arrayName+" on profile GL2: "+comps);
                            }
                            return false;
                    }
                    break;
                case GLPointerFunc.GL_NORMAL_ARRAY:
                    switch(type) {
                        case GL.GL_BYTE:
                        case GL.GL_SHORT:
                        case GL.GL_FLOAT:
                        case javax.media.opengl.GL2ES2.GL_INT:
                        case javax.media.opengl.GL2.GL_DOUBLE:
                            break;
                        default: 
                            if(throwException) {
                                throw new GLException("Illegal data type for "+arrayName+" on profile GL2: "+type);
                            }
                            return false;
                    }
                    switch(comps) {
                        case 0:
                        case 3:
                            break;
                        default: 
                            if(throwException) {
                                throw new GLException("Illegal component number for "+arrayName+" on profile GLES1: "+comps);
                            }
                            return false;
                    }
                    break;
                case GLPointerFunc.GL_COLOR_ARRAY:
                    switch(type) {
                        case GL.GL_UNSIGNED_BYTE:
                        case GL.GL_BYTE:
                        case GL.GL_UNSIGNED_SHORT:
                        case GL.GL_SHORT:
                        case GL.GL_FLOAT:
                        case javax.media.opengl.GL2ES2.GL_INT:
                        case javax.media.opengl.GL2ES2.GL_UNSIGNED_INT:
                        case javax.media.opengl.GL2.GL_DOUBLE:
                            break;
                        default: 
                            if(throwException) {
                                throw new GLException("Illegal data type for "+arrayName+" on profile GL2: "+type);
                            }
                            return false;
                    }
                    switch(comps) {
                        case 0:
                        case 3:
                        case 4:
                            break;
                        default: 
                            if(throwException) {
                                throw new GLException("Illegal component number for "+arrayName+" on profile GL2: "+comps);
                            }
                            return false;
                    }
                    break;
                case GLPointerFunc.GL_TEXTURE_COORD_ARRAY:
                    switch(type) {
                        case GL.GL_SHORT:
                        case GL.GL_FLOAT:
                        case javax.media.opengl.GL2ES2.GL_INT:
                        case javax.media.opengl.GL2.GL_DOUBLE:
                            break;
                        default: 
                            if(throwException) {
                                throw new GLException("Illegal data type for "+arrayName+" on profile GL2: "+type);
                            }
                            return false;
                    }
                    switch(comps) {
                        case 0:
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                            break;
                        default: 
                            if(throwException) {
                                throw new GLException("Illegal component number for "+arrayName+" on profile GL2: "+comps);
                            }
                            return false;
                    }
                    break;
            }
        }
    }
    return true;
  }
}
