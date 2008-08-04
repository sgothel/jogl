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

  /** The desktop short profile, intersecting: GL2+GLES1+GLES2 */
  public static final String GL2ES12 = "GL2ES12";

  /** The OpenGL ES 1 (really, 1.1) profile */
  public static final String GLES1 = "GLES1";

  /** The OpenGL ES 2 (really, 2.0) profile */
  public static final String GLES2 = "GLES2";

  /** The JVM/process wide chosen GL profile **/
  private static String profile = null;

  private static final void tryLibrary() 
  {
    try {
        Class clazz = Class.forName(getGLImplBaseClassName()+"Impl");
        if(GL2.equals(profile)) {
            NativeLibLoader.loadGL2();
        } if(GL2ES12.equals(profile)) {
            NativeLibLoader.loadGL2ES12();
        } else if(GLES1.equals(profile) || GLES2.equals(profile)) {
            Object eGLDrawableFactory = GLReflection.createInstance("com.sun.opengl.impl.egl.EGLDrawableFactory");
            if(null==eGLDrawableFactory) {
                throw new GLException("com.sun.opengl.impl.egl.EGLDrawableFactory not available");
            }
        }
    } catch (Throwable e) {
        profile=null;
    }
  }

  public static synchronized final void setProfile(String profile) 
    throws GLException
  {
    if(null==GLProfile.profile) {
        GLProfile.profile = profile;
        tryLibrary();
        if (profile == null) {
            throw new GLException("Profile " + profile + " not available");
        }
    } else {
        if(!GLProfile.profile.equals(profile)) {
              throw new GLException("Chosen profile ("+profile+") doesn't match preset one: "+GLProfile.profile);
        }
    }
  }

  public static synchronized final void setProfile(String[] profiles) 
    throws GLException
  {
    for(int i=0; profile==null && i<profiles.length; i++) {
        profile = profiles[i];
        tryLibrary();
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
      throw new GLException("Profiles "+msg.toString()+" not available");
    }
  }

  /**
   * Selects a profile, implementing the interface GL2ES1.
   */
  public static synchronized final void setProfileGL2ES1() {
    setProfile(new String[] { GLES1, GL2ES12, GL2 });
  }

  /**
   * Selects a profile, implementing the interface GL2ES2.
   */
  public static synchronized final void setProfileGL2ES2() {
    setProfile(new String[] { GLES2, GL2ES12, GL2 });
  }

  /**
   * Selects a profile, implementing the interface GL
   */
  public static synchronized final void setProfileGLAny() {
    setProfile(new String[] { GLES2, GLES1, GL2ES12, GL2 });
  }

  public static final String getProfile() {
    return profile;
  }
  
  /* true profile GL2ES12 */
  public static final boolean isGL2ES12() {
    return GL2ES12.equals(profile);
  }

  /* true profile GL2 */
  public static final boolean isGL2() {
    return GL2.equals(profile);
  }

  /* true profile GLES1 */
  public static final boolean isGLES1() {
    return GLES1.equals(profile);
  }

  /* true profile GLES2 */
  public static final boolean isGLES2() {
    return GLES2.equals(profile);
  }

  /* abstract profile GL2ES12, GL2, GLES1 */
  public static final boolean isGL2ES1() {
    return isGL2ES12() || isGL2() || isGLES1();
  }

  /* abstract profile GL2ES12, GL2, GLES2 */
  public static final boolean isGL2ES2() {
    return isGL2ES12() || isGL2() || isGLES2();
  }

  /* abstract profile GLES1, GLES2 */
  public static final boolean isGLES() {
    return isGLES2() || isGLES1();
  }

  public static final boolean matches(String test_profile) {
    return (null==test_profile)?false:test_profile.equals(profile);
  }

  public static final boolean implementationOfGL2(Object obj) {
    return GLReflection.implementationOf(obj, "javax.media.opengl.GL2");
  }

  public static final boolean implementationOfGLES1(Object obj) {
    return GLReflection.implementationOf(obj, "javax.media.opengl.GLES1");
  }

  public static final boolean implementationOfGLES2(Object obj) {
    return GLReflection.implementationOf(obj, "javax.media.opengl.GLES2");
  }

  public static final boolean implementationOfGLES(Object obj) {
    return implementationOfGLES1(obj) || implementationOfGLES2(obj);
  }

  public static final boolean implementationOfGL2ES1(Object obj) {
    return GLReflection.implementationOf(obj, "javax.media.opengl.GL2ES1");
  }

  public static final boolean implementationOfGL2ES2(Object obj) {
    return GLReflection.implementationOf(obj, "javax.media.opengl.GL2ES2");
  }

  public static final String getGLImplBaseClassName() {
        if(isGL2()) {
            return "com.sun.opengl.impl.gl2.GL2";
        } else if(isGL2ES12()) {
            return "com.sun.opengl.impl.gl2es12.GL2ES12";
        } else if(isGLES1()) {
            return "com.sun.opengl.impl.es1.GLES1";
        } else if(isGLES2()) {
            return "com.sun.opengl.impl.es2.GLES2";
        } else {
            throw new GLUnsupportedException("unsupported profile \"" + profile + "\"");
        }
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
            if( isGL2ES12() || isGL2() ) {
                return true;
            }
    } 
    if(throwException) {
        throw new GLException("Illegal data type on profile "+GLProfile.getProfile()+": "+type);
    }
    return false;
  }

  public static boolean isValidateArrayDataType(int index, int comps, int type, 
                                                boolean isVertexAttribPointer, boolean throwException) {
    String indexName = GLContext.getPredefinedArrayIndexName(index);
    if(GLProfile.isGLES1()) {
        if(isVertexAttribPointer) {
            if(throwException) {
                throw new GLException("Illegal array type for "+indexName+" on profile GLES1: VertexAttribPointer");
            }
            return false;
        }
        switch(index) {
            case GL.GL_VERTEX_ARRAY:
            case GL.GL_TEXTURE_COORD_ARRAY:
                switch(type) {
                    case GL.GL_BYTE:
                    case GL.GL_SHORT:
                    case GL.GL_FIXED:
                    case GL.GL_FLOAT:
                        break;
                    default: 
                        if(throwException) {
                            throw new GLException("Illegal data type for "+indexName+" on profile GLES1: "+type);
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
                            throw new GLException("Illegal component number for "+indexName+" on profile GLES1: "+comps);
                        }
                        return false;
                }
                break;
            case GL.GL_NORMAL_ARRAY:
                switch(type) {
                    case GL.GL_BYTE:
                    case GL.GL_SHORT:
                    case GL.GL_FIXED:
                    case GL.GL_FLOAT:
                        break;
                    default: 
                        if(throwException) {
                            throw new GLException("Illegal data type for "+indexName+" on profile GLES1: "+type);
                        }
                        return false;
                }
                switch(comps) {
                    case 0:
                    case 3:
                        break;
                    default: 
                        if(throwException) {
                            throw new GLException("Illegal component number for "+indexName+" on profile GLES1: "+comps);
                        }
                        return false;
                }
                break;
            case GL.GL_COLOR_ARRAY:
                switch(type) {
                    case GL.GL_UNSIGNED_BYTE:
                    case GL.GL_FIXED:
                    case GL.GL_FLOAT:
                        break;
                    default: 
                        if(throwException) {
                            throw new GLException("Illegal data type for "+indexName+" on profile GLES1: "+type);
                        }
                        return false;
                }
                switch(comps) {
                    case 0:
                    case 4:
                        break;
                    default: 
                        if(throwException) {
                            throw new GLException("Illegal component number for "+indexName+" on profile GLES1: "+comps);
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
                    throw new GLException("Illegal data type for "+indexName+" on profile GLES2: "+type);
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
                    throw new GLException("Illegal component number for "+indexName+" on profile GLES1: "+comps);
                }
                return false;
        }
    } else if(GLProfile.isGL2ES12() || GLProfile.isGL2()) {
        if(isVertexAttribPointer) {
            switch(index) {
                case GL.GL_VERTEX_ARRAY:
                case GL.GL_TEXTURE_COORD_ARRAY:
                case GL.GL_NORMAL_ARRAY:
                case GL.GL_COLOR_ARRAY:
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
                                throw new GLException("Illegal data type for "+indexName+" on profile GL2: "+type);
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
                                throw new GLException("Illegal component number for "+indexName+" on profile GL2: "+comps);
                            }
                            return false;
                    }
                    break;
            }
        } else {
            switch(index) {
                case GL.GL_VERTEX_ARRAY:
                    switch(type) {
                        case GL.GL_SHORT:
                        case GL.GL_FLOAT:
                        case javax.media.opengl.GL2ES2.GL_INT:
                        case javax.media.opengl.GL2.GL_DOUBLE:
                            break;
                        default: 
                            if(throwException) {
                                throw new GLException("Illegal data type for "+indexName+" on profile GL2: "+type);
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
                                throw new GLException("Illegal component number for "+indexName+" on profile GL2: "+comps);
                            }
                            return false;
                    }
                    break;
                case GL.GL_NORMAL_ARRAY:
                    switch(type) {
                        case GL.GL_BYTE:
                        case GL.GL_SHORT:
                        case GL.GL_FLOAT:
                        case javax.media.opengl.GL2ES2.GL_INT:
                        case javax.media.opengl.GL2.GL_DOUBLE:
                            break;
                        default: 
                            if(throwException) {
                                throw new GLException("Illegal data type for "+indexName+" on profile GL2: "+type);
                            }
                            return false;
                    }
                    switch(comps) {
                        case 0:
                        case 3:
                            break;
                        default: 
                            if(throwException) {
                                throw new GLException("Illegal component number for "+indexName+" on profile GLES1: "+comps);
                            }
                            return false;
                    }
                    break;
                case GL.GL_COLOR_ARRAY:
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
                                throw new GLException("Illegal data type for "+indexName+" on profile GL2: "+type);
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
                                throw new GLException("Illegal component number for "+indexName+" on profile GL2: "+comps);
                            }
                            return false;
                    }
                    break;
                case GL.GL_TEXTURE_COORD_ARRAY:
                    switch(type) {
                        case GL.GL_SHORT:
                        case GL.GL_FLOAT:
                        case javax.media.opengl.GL2ES2.GL_INT:
                        case javax.media.opengl.GL2.GL_DOUBLE:
                            break;
                        default: 
                            if(throwException) {
                                throw new GLException("Illegal data type for "+indexName+" on profile GL2: "+type);
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
                                throw new GLException("Illegal component number for "+indexName+" on profile GL2: "+comps);
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
