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
 */

package javax.media.opengl;

import com.jogamp.common.jvm.JVMUtil;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.opengl.util.VersionInfo;
import com.jogamp.opengl.impl.Debug;
import com.jogamp.opengl.impl.GLDrawableFactoryImpl;
import com.jogamp.opengl.impl.GLDynamicLookupHelper;
import com.jogamp.opengl.impl.DesktopGLDynamicLookupHelper;
import java.util.HashMap;
import java.util.Iterator;
import java.security.*;
import javax.media.opengl.fixedfunc.GLPointerFunc;
import javax.media.nativewindow.NativeWindowFactory;

/**
 * Specifies the the OpenGL profile.
 * 
 * This class static singleton initialization queries the availability of all OpenGL Profiles
 * and instantiates singleton GLProfile objects for each available profile.
 *
 * The platform default profile may be used, using {@link GLProfile#GetProfileDefault()}, 
 * or more specialized versions using the other static GetProfile methods.
 */
public class GLProfile {
    
    public static final boolean DEBUG = Debug.debug("GLProfile");

    /**
     * Static one time initialization of JOGL.
     * <p>
     * Applications shall call this methods <b>ASAP</b>, before any other UI invocation.<br>
     * You may issue the call in your main function.<br>
     * In case applications are able to initialize JOGL before any other UI action,<br>
     * they shall invoke this method with <code>firstUIActionOnProcess=true</code> and benefit from fast native multithreading support on all platforms if possible.</P>
     * <P>
     * RCP Application (Applet's, Webstart, Netbeans, ..) using JOGL may not be able to initialize JOGL
     * before the first UI action.<br>
     * In such case you shall invoke this method with <code>firstUIActionOnProcess=false</code>.<br>
     * On some platforms, notably X11 with AWT usage, JOGL will utilize special locking mechanisms which may slow down your
     * application.</P>
     * <P>
     * Remark: NEWT is currently not affected by this behavior, ie always uses native multithreading.</P>
     * <P>
     * However, in case this method is not invoked, hence GLProfile is not initialized explicitly by the user,<br>
     * the first call to {@link #getDefault()}, {@link #get(java.lang.String)}, etc, will initialize with <code>firstUIActionOnProcess=false</code>,<br>
     * hence without the possibility to enable native multithreading.<br>
     * This is not the recommended way, since it may has a performance impact, but it allows you to run code without explicit initialization.</P>
     * <P>
     * In case no explicit initialization was invoked and the implicit initialization didn't happen,<br>
     * you may encounter the following exception:
     * <pre>
     *      javax.media.opengl.GLException: No default profile available
     * </pre></P>
     *
     * @param firstUIActionOnProcess Should be <code>true</code> if called before the first UI action of the running program,
     * otherwise <code>false</code>.
     */
    public static synchronized void initSingleton(final boolean firstUIActionOnProcess) {
        if(!initialized) {
            initialized = true;
            // run the whole static initialization privileged to speed up,
            // since this skips checking further access
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    registerFactoryShutdownHook();
                    initProfiles(firstUIActionOnProcess);
                    return null;
                }
            });

            if(null==defaultGLProfile) {
                throw new GLException("No profile available: "+array2String(GL_PROFILE_LIST_ALL)+", "+glAvailabilityToString());
            }
        }
    }

    /**
     * Manual shutdown method, may be called after your last JOGL use
     * within the running JVM.<br>
     * This method is called via the JVM shutdown hook.<br>
     * It releases all temporary created resources, ie issues {@link javax.media.opengl.GLDrawableFactory#shutdown()}.<br>
     */
    public static synchronized void shutdown() {
        if(initialized) {
            initialized = false;
            GLDrawableFactory.shutdown();
        }
    }

    //
    // Query platform available OpenGL implementation
    //

    public static final boolean isGL4bcAvailable()   { return null != mappedProfiles.get(GL4bc); }
    public static final boolean isGL4Available()     { return null != mappedProfiles.get(GL4); }
    public static final boolean isGL3bcAvailable()   { return null != mappedProfiles.get(GL3bc); }
    public static final boolean isGL3Available()     { return null != mappedProfiles.get(GL3); }
    public static final boolean isGL2Available()     { return null != mappedProfiles.get(GL2); }
    public static final boolean isGLES2Available()   { return null != mappedProfiles.get(GLES2); }
    public static final boolean isGLES1Available()   { return null != mappedProfiles.get(GLES1); }
    public static final boolean isGL2ES1Available()  { return null != mappedProfiles.get(GL2ES1); }
    public static final boolean isGL2ES2Available()  { return null != mappedProfiles.get(GL2ES2); }

    public static final String glAvailabilityToString() {
        boolean avail;
        StringBuffer sb = new StringBuffer();

        sb.append("GLAvailability[Native[GL4bc ");
        avail=isGL4bcAvailable();
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(sb, 4, GLContext.CTX_PROFILE_COMPAT);
        }

        sb.append(", GL4 ");
        avail=isGL4Available();
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(sb, 4, GLContext.CTX_PROFILE_CORE);
        }

        sb.append(", GL3bc ");
        avail=isGL3bcAvailable();
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(sb, 3, GLContext.CTX_PROFILE_COMPAT);
        }

        sb.append(", GL3 ");
        avail=isGL3Available();
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(sb, 3, GLContext.CTX_PROFILE_CORE);
        }

        sb.append(", GL2 ");
        avail=isGL2Available();
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(sb, 2, GLContext.CTX_PROFILE_COMPAT);
        }

        sb.append(", GL2ES1 ");
        sb.append(isGL2ES1Available());

        sb.append(", GLES1 ");
        avail=isGLES1Available();
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(sb, 1, GLContext.CTX_PROFILE_ES);
        }

        sb.append(", GL2ES2 ");
        sb.append(isGL2ES2Available());

        sb.append(", GLES2 ");
        avail=isGLES2Available();
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(sb, 2, GLContext.CTX_PROFILE_ES);
        }

        sb.append("], Profiles[");
        for(Iterator i=mappedProfiles.values().iterator(); i.hasNext(); ) {
            sb.append(((GLProfile)i.next()).toString());
            sb.append(", ");
        }
        sb.append(", default ");
        sb.append(defaultGLProfile);
        sb.append("]]");

        return sb.toString();
    }

    //
    // Public (user-visible) profiles
    //

    /** The desktop OpenGL compatibility profile 4.x, with x >= 0, ie GL2 plus GL4.<br>
        <code>bc</code> stands for backward compatibility. */
    public static final String GL4bc = "GL4bc";

    /** The desktop OpenGL core profile 4.x, with x >= 0 */
    public static final String GL4   = "GL4";

    /** The desktop OpenGL compatibility profile 3.x, with x >= 1, ie GL2 plus GL3.<br>
        <code>bc</code> stands for backward compatibility. */
    public static final String GL3bc = "GL3bc";

    /** The desktop OpenGL core profile 3.x, with x >= 1 */
    public static final String GL3   = "GL3";

    /** The desktop OpenGL profile 1.x up to 3.0 */
    public static final String GL2   = "GL2";

    /** The embedded OpenGL profile ES 1.x, with x >= 0 */
    public static final String GLES1 = "GLES1";

    /** The embedded OpenGL profile ES 2.x, with x >= 0 */
    public static final String GLES2 = "GLES2";

    /** The intersection of the desktop GL2 and embedded ES1 profile */
    public static final String GL2ES1 = "GL2ES1";

    /** The intersection of the desktop GL3, GL2 and embedded ES2 profile */
    public static final String GL2ES2 = "GL2ES2";

    /** The intersection of the desktop GL3 and GL2 profile */
    public static final String GL2GL3 = "GL2GL3";

    /** 
     * All GL Profiles in the order of default detection.
     * Desktop compatibility profiles (the one with fixed function pipeline) comes first.
     *
     * FIXME GL3GL4: Due to GL3 and GL4 implementation bugs, we still choose GL2 first, if available!
     *
     * <ul>
     *  <li> GL2
     *  <li> GL3bc
     *  <li> GL4bc
     *  <li> GL2GL3
     *  <li> GL3
     *  <li> GL4
     *  <li> GL2ES2
     *  <li> GLES2
     *  <li> GL2ES1
     *  <li> GLES1
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_ALL = new String[] { GL2, GL3bc, GL4bc, GL2GL3, GL3, GL4, GL2ES2, GLES2, GL2ES1, GLES1 };

    /**
     * Order of maximum fixed function profiles
     *
     * <ul>
     *  <li> GL4bc
     *  <li> GL3bc
     *  <li> GL2
     *  <li> GL2ES1
     *  <li> GLES1
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MAX_FIXEDFUNC = new String[] { GL4bc, GL3bc, GL2, GL2ES1, GLES1 };

    /**
     * Order of maximum programmable shader profiles
     *
     * <ul>
     *  <li> GL4
     *  <li> GL4bc
     *  <li> GL3
     *  <li> GL3bc
     *  <li> GL2
     *  <li> GL2ES2
     *  <li> GLES2
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MAX_PROGSHADER   = new String[] { GL4, GL4bc, GL3, GL3bc, GL2, GL2ES2, GLES2 };

    /**
     * All GL2ES2 Profiles in the order of default detection.
     *
     * FIXME GL3GL4: Due to GL3 and GL4 implementation bugs, we still choose GL2 first, if available!
     *
     * <ul>
     *  <li> GL2ES2
     *  <li> GL2
     *  <li> GL3
     *  <li> GL4
     *  <li> GLES2
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_GL2ES2 = new String[] { GL2ES2, GL2, GL3, GL4, GLES2 };

    /**
     * All GL2ES1 Profiles in the order of default detection.
     *
     * FIXME GL3GL4: Due to GL3 and GL4 implementation bugs, we still choose GL2 first, if available!
     *
     * <ul>
     *  <li> GL2ES1
     *  <li> GL2
     *  <li> GL3bc
     *  <li> GL4bc
     *  <li> GLES1
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_GL2ES1 = new String[] { GL2ES1, GL2, GL3bc, GL4bc, GLES1 };

    /** Returns a default GLProfile object, reflecting the best for the running platform.
     * It selects the first of the set {@link GLProfile#GL_PROFILE_LIST_ALL}
     * @see #GL_PROFILE_LIST_ALL
     */
    public static final GLProfile getDefault() {
        validateInitialization();
        if(null==defaultGLProfile) {
            throw new GLException("No default profile available"); // should never be reached 
        }
        return defaultGLProfile;
    }

    /**
     * Returns the highest profile, implementing the fixed function pipeline
     * It selects the first of the set: {@link GLProfile#GL_PROFILE_LIST_MAX_FIXEDFUNC}
     *
     * @throws GLException if no implementation for the given profile is found.
     * @see #GL_PROFILE_LIST_MAX_FIXEDFUNC
     */
    public static final GLProfile getMaxFixedFunc() 
        throws GLException
    {
        return get(GL_PROFILE_LIST_MAX_FIXEDFUNC);
    }

    /**
     * Returns the highest profile, implementing the programmable shader pipeline.
     * It selects the first of the set: {@link GLProfile#GL_PROFILE_LIST_MAX_PROGSHADER}
     *
     * @throws GLException if no implementation for the given profile is found.
     * @see #GL_PROFILE_LIST_MAX_PROGSHADER
     */
    public static final GLProfile getMaxProgrammable() 
        throws GLException
    {
        return get(GL_PROFILE_LIST_MAX_PROGSHADER);
    }

    /**
     * Returns a profile, implementing the interface GL2ES1.
     * It selects the first of the set: {@link GLProfile#GL_PROFILE_LIST_GL2ES1}
     *
     * @throws GLException if no implementation for the given profile is found.
     * @see #GL_PROFILE_LIST_GL2ES1
     */
    public static final GLProfile getGL2ES1() 
        throws GLException
    {
        return get(GL_PROFILE_LIST_GL2ES1);
    }

    /**
     * Returns a profile, implementing the interface GL2ES2.
     * It selects the first of the set: {@link GLProfile#GL_PROFILE_LIST_GL2ES2}
     *
     * @throws GLException if no implementation for the given profile is found.
     * @see #GL_PROFILE_LIST_GL2ES2
     */
    public static final GLProfile getGL2ES2() 
        throws GLException
    {
        return get(GL_PROFILE_LIST_GL2ES2);
    }

    /** Returns a GLProfile object.
     * Verfifies the given profile and chooses an apropriate implementation.
     * A generic value of <code>null</code> or <code>GL</code> will result in
     * the default profile.
     *
     * @throws GLException if no implementation for the given profile is found.
     */
    public static final GLProfile get(String profile) 
        throws GLException
    {
        validateInitialization();
        if(null==profile || profile.equals("GL")) return getDefault();
        GLProfile glProfile = (GLProfile) mappedProfiles.get(profile);
        if(null==glProfile) {
            throw new GLException("No implementation for profile "+profile+" available");
        }
        return glProfile;
    }

    /**
     * Returns the first profile from the given list,
     * where an implementation is available.
     *
     * @throws GLException if no implementation for the given profile is found.
     */
    public static final GLProfile get(String[] profiles) 
        throws GLException
    {
        validateInitialization();
        for(int i=0; i<profiles.length; i++) {
            String profile = profiles[i];
            GLProfile glProfile = (GLProfile) mappedProfiles.get(profile);
            if(null!=glProfile) {
                return glProfile;
            }
        }
        throw new GLException("Profiles "+array2String(profiles)+" not available");
    }

    /** Indicates whether the native OpenGL ES1 profile is in use. 
     * This requires an EGL interface.
     */
    public static final boolean usesNativeGLES1(String profileImpl) {
        return GLES1.equals(profileImpl);
    }

    /** Indicates whether the native OpenGL ES2 profile is in use. 
     * This requires an EGL interface.
     */
    public static final boolean usesNativeGLES2(String profileImpl) {
        return GLES2.equals(profileImpl);
    }

    /** Indicates whether either of the native OpenGL ES profiles are in use. */
    public static final boolean usesNativeGLES(String profileImpl) {
        return usesNativeGLES2(profileImpl) || usesNativeGLES1(profileImpl);
    }

    /** @return {@link javax.media.nativewindow.NativeWindowFactory#isAWTAvailable()} and
        JOGL's AWT part */
    public static boolean isAWTAvailable() { return isAWTAvailable; }

    public static String getGLTypeName(int type) {
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

    public static String getGLArrayName(int array) {
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

    public final String getGLImplBaseClassName() {
        return getGLImplBaseClassName(profileImpl);
    }

    /**
     * @param o GLProfile object to compare with
     * @return true if given Object is a GLProfile and
     *         if both, profile and profileImpl is equal with this.
     */
    public final boolean equals(Object o) {
        if(o instanceof GLProfile) {
            GLProfile glp = (GLProfile)o;
            return profile.equals(glp.getName()) && profileImpl.equals(glp.getImplName()) ;
        }
        return false;
    }

    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.profileImpl != null ? this.profileImpl.hashCode() : 0);
        hash = 97 * hash + (this.profile != null ? this.profile.hashCode() : 0);
        return hash;
    }
 
    /**
     * @param glp GLProfile to compare with
     * @throws GLException if given GLProfile and this aren't equal
     */
    public final void verifyEquality(GLProfile glp) throws GLException  {
        if(!this.equals(glp)) {
            throw new GLException("GLProfiles are not equal: "+this+" != "+glp);
        }
    }

    public final String getName() {
        return profile;
    }

    public final String getImplName() {
        return profileImpl;
    }

    /** Indicates whether this profile is capable of GL4bc. */
    public final boolean isGL4bc() {
        return GL4bc.equals(profile);
    }

    /** Indicates whether this profile is capable of GL4. */
    public final boolean isGL4() {
        return isGL4bc() || GL4.equals(profile);
    }

    /** Indicates whether this profile is capable of GL3bc. */
    public final boolean isGL3bc() {
        return isGL4bc() || GL3bc.equals(profile);
    }

    /** Indicates whether this profile is capable of GL3. */
    public final boolean isGL3() {
        return isGL4() || isGL3bc() || GL3.equals(profile);
    }

    /** Indicates whether this context is a GL2 context */
    public final boolean isGL2() {
        return isGL3bc() || GL2.equals(profile);
    }

    /** Indicates whether this profile is capable of GLES1. */
    public final boolean isGLES1() {
        return GLES1.equals(profile);
    }

    /** Indicates whether this profile is capable of GLES2. */
    public final boolean isGLES2() {
        return GLES2.equals(profile);
    }

    /** Indicates whether this profile is capable of GL2ES1. */
    public final boolean isGL2ES1() {
        return GL2ES1.equals(profile) || isGL2() || isGLES1() ;
    }

    /** Indicates whether this profile is capable os GL2ES2. */
    public final boolean isGL2ES2() {
        return GL2ES2.equals(profile) || isGL2() || isGL3() || isGLES2() ;
    }

    /** Indicates whether this profile is capable os GL2GL3. */
    public final boolean isGL2GL3() {
        return GL2GL3.equals(profile) || isGL2() || isGL3() ;
    }

    /** Indicates whether this profile supports GLSL. */
    public final boolean hasGLSL() {
        return isGL2ES2() ;
    }

    /** Indicates whether this profile uses the native OpenGL ES1 implementations. */
    public final boolean usesNativeGLES1() {
        return GLES1.equals(profileImpl);
    }

    /** Indicates whether this profile uses the native OpenGL ES2 implementations. */
    public final boolean usesNativeGLES2() {
        return GLES2.equals(profileImpl);
    }

    /** Indicates whether this profile uses either of the native OpenGL ES implementations. */
    public final boolean usesNativeGLES() {
        return usesNativeGLES2() || usesNativeGLES1();
    }

    /** 
     * General validation if type is a valid GL data type
     * for the current profile
     */
    public boolean isValidDataType(int type, boolean throwException) {
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
                if( isGL3() ) {
                    return true;
                }
            case javax.media.opengl.GL2.GL_2_BYTES:
            case javax.media.opengl.GL2.GL_3_BYTES:
            case javax.media.opengl.GL2.GL_4_BYTES:
                if( isGL2() ) {
                    return true;
                }
        } 
        if(throwException) {
            throw new GLException("Illegal data type on profile "+this+": "+type);
        }
        return false;
    }
    
    public boolean isValidArrayDataType(int index, int comps, int type, 
                                        boolean isVertexAttribPointer, boolean throwException) {
        String arrayName = getGLArrayName(index);
        if(isGLES1()) {
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
        } else if(isGLES2()) {
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
        } else if( isGL2ES2() ) {
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

    public String toString() {
        return "GLProfile[" + profile + "/" + profileImpl + "]";
    }

    static {
        JVMUtil.initSingleton();
    }

    private static /*final*/ boolean isAWTAvailable;

    private static /*final*/ boolean hasGL234Impl;
    private static /*final*/ boolean hasGL4bcImpl;
    private static /*final*/ boolean hasGL4Impl;
    private static /*final*/ boolean hasGL3bcImpl;
    private static /*final*/ boolean hasGL3Impl;
    private static /*final*/ boolean hasGL2Impl;
    private static /*final*/ boolean hasGLES2Impl;
    private static /*final*/ boolean hasGLES1Impl;

    /** The JVM/process wide default GL profile **/
    private static /*final*/ GLProfile defaultGLProfile;
  
    /** All GLProfiles */
    private static /*final*/ HashMap/*<String, GLProfile>*/ mappedProfiles;

    static boolean initialized = false;

    // Shutdown hook mechanism for the factory
    private static boolean factoryShutdownHookRegistered = false;
    private static Thread factoryShutdownHook = null;

    /**
     * Tries the profiles implementation and native libraries.
     * Throws an GLException if no profile could be found at all.
     */
    private static void initProfiles(boolean firstUIActionOnProcess) {

        NativeWindowFactory.initSingleton(firstUIActionOnProcess);

        ClassLoader classloader = GLProfile.class.getClassLoader();

        isAWTAvailable = NativeWindowFactory.isAWTAvailable() &&
                         ReflectionUtil.isClassAvailable("javax.media.opengl.awt.GLCanvas", classloader) ; // JOGL

        hasGL234Impl   = ReflectionUtil.isClassAvailable("com.jogamp.opengl.impl.gl4.GL4bcImpl", classloader);
        hasGL4bcImpl   = hasGL234Impl;
        hasGL4Impl     = hasGL234Impl;
        hasGL3bcImpl   = hasGL234Impl;
        hasGL3Impl     = hasGL234Impl;
        hasGL2Impl     = hasGL234Impl;
        mappedProfiles = computeProfileMap();

        boolean hasDesktopGL = false;
        boolean hasNativeOSFactory = false;
        Throwable t;

        //
        // Iteration of desktop GL availability detection
        // utilizing the detected GL version in the shared context.
        //
        // - Instantiate GLDrawableFactory incl its shared dummy drawable/context,
        //   which will register at GLContext ..
        //

        t=null;
        // if successfull it has a shared dummy drawable and context created
        try {
            GLDrawableFactoryImpl factory = (GLDrawableFactoryImpl) GLDrawableFactory.getFactoryImpl(GL2);
            hasNativeOSFactory = null != factory;
            if(hasNativeOSFactory) {
                DesktopGLDynamicLookupHelper glLookupHelper = (DesktopGLDynamicLookupHelper) factory.getGLDynamicLookupHelper(0);
                if(null!=glLookupHelper) {
                    hasDesktopGL = glLookupHelper.hasGLBinding();
                }
            }
        } catch (LinkageError le) {
            t=le;
        } catch (RuntimeException re) {
            t=re;
        } catch (Throwable tt) {
            t=tt;
        }
        if(DEBUG) {
            if(null!=t) {
                t.printStackTrace();
            }
            if(!hasNativeOSFactory) {
                System.err.println("Info: GLProfile.init - Native platform GLDrawable factory not available");
            }
        }

        if(hasDesktopGL && !GLContext.mappedVersionsAvailableSet) {
            // nobody yet set the available desktop versions, see {@link GLContextImpl#makeCurrent},
            // so we have to add the usual suspect
            GLContext.mapVersionAvailable(2, GLContext.CTX_PROFILE_COMPAT, 1, 5, GLContext.CTX_PROFILE_COMPAT|GLContext.CTX_OPTION_ANY);
        }

        if(!hasNativeOSFactory) {
            hasDesktopGL   = false;
            hasGL234Impl   = false;
            hasGL4bcImpl   = false;
            hasGL4Impl     = false;
            hasGL3bcImpl   = false;
            hasGL3Impl     = false;
            hasGL2Impl     = false;
        } else {
            hasGL4bcImpl   = hasGL4bcImpl   && GLContext.isGL4bcAvailable();
            hasGL4Impl     = hasGL4Impl     && GLContext.isGL4Available();
            hasGL3bcImpl   = hasGL3bcImpl   && GLContext.isGL3bcAvailable();
            hasGL3Impl     = hasGL3Impl     && GLContext.isGL3Available();
            hasGL2Impl     = hasGL2Impl     && GLContext.isGL2Available();
        }

        if ( ReflectionUtil.isClassAvailable("com.jogamp.opengl.impl.egl.EGLDrawableFactory", classloader) ) {
            t=null;
            try {
                GLDrawableFactoryImpl factory = (GLDrawableFactoryImpl) GLDrawableFactory.getFactoryImpl(GLES2);
                if(null != factory) {
                    GLDynamicLookupHelper eglLookupHelper = factory.getGLDynamicLookupHelper(2);
                    if(null!=eglLookupHelper) {
                        hasGLES2Impl = eglLookupHelper.isLibComplete();
                    }
                    eglLookupHelper = factory.getGLDynamicLookupHelper(1);
                    if(null!=eglLookupHelper) {
                        hasGLES1Impl = eglLookupHelper.isLibComplete();
                    }
                }
            } catch (LinkageError le) {
                t=le;
            } catch (SecurityException se) {
                t=se;
            } catch (NullPointerException npe) {
                t=npe;
            } catch (RuntimeException re) {
                t=re;
            }
            if(DEBUG && null!=t) {
                t.printStackTrace();
            }
        }
        if(hasGLES2Impl) {
            GLContext.mapVersionAvailable(2, GLContext.CTX_PROFILE_ES, 2, 0, GLContext.CTX_PROFILE_ES|GLContext.CTX_OPTION_ANY);
        }
        if(hasGLES1Impl) {
            GLContext.mapVersionAvailable(1, GLContext.CTX_PROFILE_ES, 1, 0, GLContext.CTX_PROFILE_ES|GLContext.CTX_OPTION_ANY);
        }

        mappedProfiles = computeProfileMap();

        if (DEBUG) {
            System.err.println(VersionInfo.getPackageInfo(null, "GLProfile.init", "javax.media.opengl", "GL"));
            System.err.println(VersionInfo.getPlatformInfo(null, "GLProfile.init"));
            System.err.println("GLProfile.init firstUIActionOnProcess "+firstUIActionOnProcess);
            System.err.println("GLProfile.init isAWTAvailable "+isAWTAvailable);
            System.err.println("GLProfile.init hasNativeOSFactory "+hasNativeOSFactory);
            System.err.println("GLProfile.init hasDesktopGL "+hasDesktopGL);
            System.err.println("GLProfile.init hasGL234Impl "+hasGL234Impl);
            System.err.println("GLProfile.init "+glAvailabilityToString());
        }
    }

    private static synchronized void registerFactoryShutdownHook() {
        if (factoryShutdownHookRegistered) {
            return;
        }
        factoryShutdownHook = new Thread(new Runnable() {
            public void run() {
                GLDrawableFactory.shutdown();
            }
        });
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                Runtime.getRuntime().addShutdownHook(factoryShutdownHook);
                return null;
            }
        });
        factoryShutdownHookRegistered = true;
    }

    private static void validateInitialization() {
        if(!initialized) {
            synchronized(GLProfile.class) {
                if(!initialized) {
                    initSingleton(false);
                }
            }
        }
    }

    private static final String array2String(String[] list) {
        StringBuffer msg = new StringBuffer();
        msg.append("[");
        for (int i = 0; i < list.length; i++) {
            if (i > 0)
                msg.append(", ");
            msg.append(list[i]);
        }
        msg.append("]");
        return msg.toString();
    }

    private static final void glAvailabilityToString(StringBuffer sb, int major, int profile) {
        String str = GLContext.getGLVersionAvailable(major, profile);
        if(null==str) {
            throw new GLException("Internal Error");
        }
        sb.append("[");
        sb.append(str);
        sb.append("]");
    }

    private static HashMap computeProfileMap() {
        defaultGLProfile=null;
        HashMap/*<String, GLProfile>*/ _mappedProfiles = new HashMap(GL_PROFILE_LIST_ALL.length);
        for(int i=0; i<GL_PROFILE_LIST_ALL.length; i++) {
            String profile = GL_PROFILE_LIST_ALL[i];
            String profileImpl = computeProfileImpl(profile);
            if(null!=profileImpl) {
                GLProfile glProfile = new GLProfile(profile, profileImpl);
                _mappedProfiles.put(profile, glProfile);
                if (DEBUG) {
                    System.err.println("GLProfile.init map "+glProfile);
                }
                if(null==defaultGLProfile) {
                    defaultGLProfile=glProfile;
                    if (DEBUG) {
                        System.err.println("GLProfile.init default "+glProfile);
                    }
                }
            } else {
                if (DEBUG) {
                    System.err.println("GLProfile.init map *** no mapping for "+profile);
                }
            }
        }
        return _mappedProfiles;
    }

    private static final String getGLImplBaseClassName(String profileImpl) {
        if ( GL4bc.equals(profileImpl) ||
             GL4.equals(profileImpl)   ||
             GL3bc.equals(profileImpl) ||
             GL3.equals(profileImpl)   ||
             GL2.equals(profileImpl) ) {
            return "com.jogamp.opengl.impl.gl4.GL4bc";
        } else if(GLES1.equals(profileImpl) || GL2ES1.equals(profileImpl)) {
            return "com.jogamp.opengl.impl.es1.GLES1";
        } else if(GLES2.equals(profileImpl) || GL2ES2.equals(profileImpl)) {
            return "com.jogamp.opengl.impl.es2.GLES2";
        } else {
            throw new GLException("unsupported profile \"" + profileImpl + "\"");
        }
    }

    /**
     * Returns the profile implementation
     */
    private static String computeProfileImpl(String profile) {
        if (GL2ES1.equals(profile)) {
            if(hasGL2Impl) {
                return GL2;
            } else if(hasGL3bcImpl) {
                return GL3bc;
            } else if(hasGL4bcImpl) {
                return GL4bc;
            } else if(hasGLES1Impl) {
                return GLES1;
            }
        } else if (GL2ES2.equals(profile)) {
            if(hasGL2Impl) {
                return GL2;
            } else if(hasGL3Impl) {
                return GL3;
            } else if(hasGL4Impl) {
                return GL4;
            } else if(hasGLES2Impl) {
                return GLES2;
            }
        } else if(GL2GL3.equals(profile)) {
            if(hasGL2Impl) {
                return GL2;
            } else if(hasGL3bcImpl) {
                return GL3bc;
            } else if(hasGL4bcImpl) {
                return GL4bc;
            } else if(hasGL3Impl) {
                return GL3;
            } else if(hasGL4Impl) {
                return GL4;
            }
        } else if(GL4bc.equals(profile) && hasGL4bcImpl) {
            return GL4bc;
        } else if(GL4.equals(profile) && hasGL4Impl) {
            return GL4;
        } else if(GL3bc.equals(profile) && hasGL3bcImpl) {
            return GL3bc;
        } else if(GL3.equals(profile) && hasGL3Impl) {
            return GL3;
        } else if(GL2.equals(profile) && hasGL2Impl) {
            return GL2;
        } else if(GLES2.equals(profile) && hasGLES2Impl) {
            return GLES2;
        } else if(GLES1.equals(profile) && hasGLES1Impl) {
            return GLES1;
        }
        return null;
    }

    private GLProfile(String profile, String profileImpl) {
        this.profile = profile;
        this.profileImpl = profileImpl;
    }

    private String profileImpl = null;
    private String profile = null;
}
