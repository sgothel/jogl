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

import jogamp.opengl.Debug;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.DesktopGLDynamicLookupHelper;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.common.util.cache.TempJarCache;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveThreadGroupLock;
import com.jogamp.nativewindow.NativeWindowVersion;
import com.jogamp.opengl.JoglVersion;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.fixedfunc.GLPointerFunc;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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

    static {
        // Also initializes TempJarCache if shall be used.
        Platform.initSingleton();
    }
    
    /**
     * Static initialization of JOGL.
     * <p>
     * The parameter <code>firstUIActionOnProcess</code> has an impact on concurrent locking,<br>
     * see {@link javax.media.nativewindow.NativeWindowFactory#initSingleton(boolean) NativeWindowFactory.initSingleton(firstUIActionOnProcess)}.
     * </p>
     * <p>
     * Applications using this method may place it's call before any other UI invocation
     * in the <code>main class</code>'s static block or within the <code>main function</code>.
     * In such case, applications may pass <code>firstUIActionOnProcess=true</code> to use native toolkit locking.</P>
     * <P>
     * RCP Application (Applet's, Webstart, Netbeans, ..) using JOGL are not be able to initialize JOGL
     * before the first UI action.
     * In such case you shall pass <code>firstUIActionOnProcess=false</code>.</P>
     * <P>
     * In case this method is not invoked, GLProfile is initialized implicit by
     * the first call to {@link #getDefault()}, {@link #get(java.lang.String)} passing <code>firstUIActionOnProcess=false</code>.
     * <P>
     *
     * @param firstUIActionOnProcess Should be <code>true</code> if called before the first UI action of the running program,
     * otherwise <code>false</code>.
     * 
     * @deprecated This method shall not need to be called for other reasons than having a defined initialization sequence.
     *             To ensure homogeneous behavior with application not calling this method, you shall pass <code>firstUIActionOnProcess=false</code>.
     *             This method is subject to be removed in future versions of JOGL. 
     */
    public static void initSingleton(final boolean firstUIActionOnProcess) {
        initLock.lock();
        try {
            if(!initialized) { // volatile: ok
                initialized = true;
                if(DEBUG) {
                    System.err.println("GLProfile.initSingleton(firstUIActionOnProcess: "+firstUIActionOnProcess+") - thread "+Thread.currentThread().getName());
                    Thread.dumpStack();
                }
    
                // run the whole static initialization privileged to speed up,
                // since this skips checking further access
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        Platform.initSingleton();
                        
                        if(TempJarCache.isInitialized()) {
                           String[] atomicNativeJarBaseNames = new String[] { "nativewindow", "jogl", null };
                           if( ReflectionUtil.isClassAvailable("com.jogamp.newt.NewtFactory", GLProfile.class.getClassLoader()) ) {
                               atomicNativeJarBaseNames[2] = "newt";
                           }
                           JNILibLoaderBase.addNativeJarLibs(GLProfile.class, "jogl-all", atomicNativeJarBaseNames);
                        }
                        initProfilesForDefaultDevices(firstUIActionOnProcess);
                        return null;
                    }
                });
            }
        } finally {
            initLock.unlock();
        }
    }
    
    /**
     * Static initialization of JOGL.
     *
     * <p>
     * This method shall not need to be called for other reasons than having a defined initialization sequence.
     * </p>
     */
    public static void initSingleton() {
        GLProfile.initSingleton(false);
    }

    /**
     * Trigger eager initialization of GLProfiles for the given device,
     * in case it isn't done yet.
     * 
     * @throws GLException if no profile for the given device is available.
     */
    public static void initProfiles(AbstractGraphicsDevice device) throws GLException {
        getProfileMap(device, true);
    }

    /** 
     * Shutdown type for {@link GLProfile#shutdown(ShutdownType)}.
     * <p>
     * {@link #SHARED_ONLY} For thread based resources only, suitable for eg. {@link java.applet.Applet Applet} restart.<br>
     * {@link #COMPLETE} Everything.<br>
     * </p>
     */ 
    public enum ShutdownType {
        /* Shared thread based resources only, eg. for Applets */ 
        SHARED_ONLY,
        /* Everything */
        COMPLETE;
    }
    
    /**
     * Manual shutdown method, may be called after your last JOGL use
     * within the running JVM.<br>
     * It releases all temporary created resources, ie issues {@link javax.media.opengl.GLDrawableFactory#shutdown()}.<br>
     * The shutdown implementation is called via the JVM shutdown hook, if not manually invoked.<br>
     * <p>
     * This method shall not need to be called for other reasons than issuing a proper shutdown of resources.
     * </p>
     * @param type the shutdown type, see {@link ShutdownType}.
     */
    public static void shutdown(ShutdownType type) {
        initLock.lock();
        try {
            if(initialized) { // volatile: ok
                initialized = false;
                if(DEBUG) {
                    System.err.println("GLProfile.shutdown(type: "+type+") - thread "+Thread.currentThread().getName());
                    Thread.dumpStack();
                }                    
                GLDrawableFactory.shutdown(type);
                if(ShutdownType.COMPLETE == type) {
                    GLContext.shutdown();
                }
                NativeWindowFactory.shutdown();
            }
        } finally {
            initLock.unlock();
        }
    }

    //
    // Query platform available OpenGL implementation
    //

    /** 
     * Returns the availability of a profile on a device.
     * 
     * @param device a valid AbstractGraphicsDevice, or <code>null</null> for the default device.
     * @param profile a valid GLProfile name ({@link #GL4bc}, {@link #GL4}, {@link #GL2}, ..), 
     *        or <code>[ null, GL ]</code> for the default profile.
     * @return true if the profile is available for the device, otherwise false.
     */
    public static boolean isAvailable(AbstractGraphicsDevice device, String profile) {
        initSingleton();
        return isAvailableImpl(getProfileMap(device, false), profile);
    }
    private static boolean isAvailableImpl(HashMap<String /*GLProfile_name*/, GLProfile> map, String profile) {
        return null != map && null != map.get(profile);
    }
    
    /** 
     * Returns the availability of a profile on the default device.
     * 
     * @param profile a valid GLProfile name ({@link #GL4bc}, {@link #GL4}, {@link #GL2}, ..), 
     *        or <code>[ null, GL ]</code> for the default profile.
     * @return true if the profile is available for the default device, otherwise false.
     */
    public static boolean isAvailable(String profile) {
        return isAvailable(null, profile);
    }
    
    /** 
     * Returns the availability of any profile on the default device.
     * 
     * @return true if any profile is available for the default device, otherwise false.
     */
    public static boolean isAnyAvailable() {
        return isAvailable(null, null);
    }
    
    public static String glAvailabilityToString(AbstractGraphicsDevice device) {
        boolean avail;
        StringBuffer sb = new StringBuffer();

        initSingleton();
        
        if(null==device) {
            device = defaultDevice;
        }
        final HashMap<String /*GLProfile_name*/, GLProfile> map = getProfileMap(device, false);
        
        sb.append("GLAvailability[Native[GL4bc ");
        avail=isAvailableImpl(map, GL4bc);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb, 4, GLContext.CTX_PROFILE_COMPAT);
        }

        sb.append(", GL4 ");
        avail=isAvailableImpl(map, GL4);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb, 4, GLContext.CTX_PROFILE_CORE);
        }

        sb.append(", GL3bc ");
        avail=isAvailableImpl(map, GL3bc);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb, 3, GLContext.CTX_PROFILE_COMPAT);
        }

        sb.append(", GL3 ");
        avail=isAvailableImpl(map, GL3);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb, 3, GLContext.CTX_PROFILE_CORE);
        }

        sb.append(", GL2 ");
        avail=isAvailableImpl(map, GL2);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb, 2, GLContext.CTX_PROFILE_COMPAT);
        }

        sb.append(", GL2ES1 ");
        sb.append(isAvailableImpl(map, GL2ES1));

        sb.append(", GLES1 ");
        avail=isAvailableImpl(map, GLES1);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb, 1, GLContext.CTX_PROFILE_ES);
        }

        sb.append(", GL2ES2 ");
        sb.append(isAvailableImpl(map, GL2ES2));

        sb.append(", GLES2 ");
        avail=isAvailableImpl(map, GLES2);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb, 2, GLContext.CTX_PROFILE_ES);
        }

        sb.append("], Profiles[");
        if(null != map) {
            for(Iterator<GLProfile> i=map.values().iterator(); i.hasNext(); ) {
                sb.append(i.next().toString());
                sb.append(", ");
            }
            sb.append(", default ");
            try {
                sb.append(getDefault(device));
            } catch (GLException gle) {
                sb.append("n/a");
            }
        }
        sb.append("]]");

        return sb.toString();
    }
    
    /** Uses the default device */
    public static String glAvailabilityToString() {
        return glAvailabilityToString(null);
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

    /** The default profile, used for the device default profile map  */
    private static final String GL_DEFAULT = "GL_DEFAULT";

    /** 
     * All GL Profiles in the order of default detection.
     * Desktop compatibility profiles (the one with fixed function pipeline) comes first
     * from highest to lowest version.
     * <p> This includes the generic subset profiles GL2GL3, GL2ES2 and GL2ES1.</p>
     *
     * <ul>
     *  <li> GL4bc
     *  <li> GL3bc
     *  <li> GL2
     *  <li> GL4
     *  <li> GL3
     *  <li> GL2GL3
     *  <li> GLES2
     *  <li> GL2ES2
     *  <li> GLES1
     *  <li> GL2ES1
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_ALL = new String[] { GL4bc, GL3bc, GL2, GL4, GL3, GL2GL3, GLES2, GL2ES2, GLES1, GL2ES1 };

    /**
     * Order of maximum profiles.
     *
     * <ul>
     *  <li> GL4bc
     *  <li> GL4
     *  <li> GL3bc
     *  <li> GL3
     *  <li> GL2
     *  <li> GLES2
     *  <li> GLES1
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MAX = new String[] { GL4bc, GL4, GL3bc, GL3, GL2, GLES2, GLES1 };

    /**
     * Order of minimum profiles.
     *
     * <ul>
     *  <li> GLES1
     *  <li> GLES2
     *  <li> GL2
     *  <li> GL3
     *  <li> GL3bc
     *  <li> GL4
     *  <li> GL4bc
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MIN = new String[] { GLES1, GLES2, GL2, GL3, GL3bc, GL4, GL4bc };
    
    /**
     * Order of minimum original desktop profiles.
     *
     * <ul>
     *  <li> GL2
     *  <li> GL3bc
     *  <li> GL4bc
     *  <li> GL3
     *  <li> GL4
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MIN_DESKTOP = new String[] { GL2, GL3bc, GL4bc, GL3, GL4 };
    
    /**
     * Order of maximum fixed function profiles
     *
     * <ul>
     *  <li> GL4bc
     *  <li> GL3bc
     *  <li> GL2
     *  <li> GLES1
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MAX_FIXEDFUNC = new String[] { GL4bc, GL3bc, GL2, GLES1 };

    /**
     * Order of maximum programmable shader profiles
     *
     * <ul>
     *  <li> GL4bc
     *  <li> GL4
     *  <li> GL3bc
     *  <li> GL3
     *  <li> GL2
     *  <li> GLES2
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MAX_PROGSHADER   = new String[] { GL4bc, GL4, GL3bc, GL3, GL2, GLES2 };

    /** Returns a default GLProfile object, reflecting the best for the running platform.
     * It selects the first of the set {@link GLProfile#GL_PROFILE_LIST_ALL}
     * @throws GLException if no profile is available for the device.
     * @see #GL_PROFILE_LIST_ALL
     */
    public static GLProfile getDefault(AbstractGraphicsDevice device) {
        GLProfile glp = get(device, GL_DEFAULT);
        return glp;
    }

    /** Uses the default device 
     * @throws GLException if no profile is available for the default device.
     */
    public static GLProfile getDefault() {
        return getDefault(defaultDevice);
    }

    /**
     * Returns the highest profile.
     * It selects the first of the set: {@link GLProfile#GL_PROFILE_LIST_MAX}
     *
     * @throws GLException if no profile is available for the device.
     * @see #GL_PROFILE_LIST_MAX
     */
    public static GLProfile getMaximum(AbstractGraphicsDevice device)
        throws GLException
    {
        return get(device, GL_PROFILE_LIST_MAX);
    }

    /** Uses the default device 
     * @throws GLException if no profile is available for the default device.
     * @see #GL_PROFILE_LIST_MAX
     */
    public static GLProfile getMaximum()
        throws GLException
    {
        return get(GL_PROFILE_LIST_MAX);
    }

    /**
     * Returns the lowest profile.
     * It selects the first of the set: {@link GLProfile#GL_PROFILE_LIST_MIN}
     *
     * @throws GLException if no desktop profile is available for the device.
     * @see #GL_PROFILE_LIST_MIN
     */
    public static GLProfile getMinimum(AbstractGraphicsDevice device)
        throws GLException
    {
        return get(device, GL_PROFILE_LIST_MIN);
    }

    /** Uses the default device 
     * @throws GLException if no desktop profile is available for the default device.
     * @see #GL_PROFILE_LIST_MIN
     */
    public static GLProfile getMinimum()
        throws GLException
    {
        return get(GL_PROFILE_LIST_MIN);
    }


    /**
     * Returns the highest profile, implementing the fixed function pipeline.
     * It selects the first of the set: {@link GLProfile#GL_PROFILE_LIST_MAX_FIXEDFUNC}
     *
     * @throws GLException if no fixed function profile is available for the device.
     * @see #GL_PROFILE_LIST_MAX_FIXEDFUNC
     */
    public static GLProfile getMaxFixedFunc(AbstractGraphicsDevice device)
        throws GLException
    {
        return get(device, GL_PROFILE_LIST_MAX_FIXEDFUNC);
    }

    /** Uses the default device 
     * @throws GLException if no fixed function profile is available for the default device.
     * @see #GL_PROFILE_LIST_MAX_FIXEDFUNC
     */
    public static GLProfile getMaxFixedFunc()
        throws GLException
    {
        return get(GL_PROFILE_LIST_MAX_FIXEDFUNC);
    }

    /**
     * Returns the highest profile, implementing the programmable shader pipeline.
     * It selects the first of the set: {@link GLProfile#GL_PROFILE_LIST_MAX_PROGSHADER}
     *
     * @throws GLException if no programmable profile is available for the device.
     * @see #GL_PROFILE_LIST_MAX_PROGSHADER
     */
    public static GLProfile getMaxProgrammable(AbstractGraphicsDevice device)
        throws GLException
    {
        return get(device, GL_PROFILE_LIST_MAX_PROGSHADER);
    }

    /** Uses the default device 
     * @throws GLException if no programmable profile is available for the default device.
     * @see #GL_PROFILE_LIST_MAX_PROGSHADER
     */
    public static GLProfile getMaxProgrammable()
        throws GLException
    {
        return get(GL_PROFILE_LIST_MAX_PROGSHADER);
    }

    /** 
     * Returns the GL2ES1 profile implementation, hence compatible w/ GL2ES1.<br/>
     * It returns:
     * <pre>
     *   GLProfile.get(device, GLProfile.GL2ES1).getImpl());
     * </pre>
     *
     * @throws GLException if no GL2ES1 compatible profile is available for the default device.
     * @see #get(AbstractGraphicsDevice, String)
     * @see #getImpl()
     */
    public static GLProfile getGL2ES1(AbstractGraphicsDevice device)
        throws GLException
    {
        return get(device, GL2ES1).getImpl();
    }

    /** 
     * Calls {@link #getGL2ES1(AbstractGraphicsDevice)} using the default device. 
     * @see #getGL2ES1(AbstractGraphicsDevice)
     */
    public static GLProfile getGL2ES1()
        throws GLException
    {
        return get(defaultDevice, GL2ES1).getImpl();
    }

    /** 
     * Returns the GL2ES2 profile implementation, hence compatible w/ GL2ES2.<br/>
     * It returns:
     * <pre>
     *   GLProfile.get(device, GLProfile.GL2ES2).getImpl());
     * </pre>
     *
     * @throws GLException if no GL2ES2 compatible profile is available for the default device.
     * @see #get(AbstractGraphicsDevice, String)
     * @see #getImpl()
     */
    public static GLProfile getGL2ES2(AbstractGraphicsDevice device)
        throws GLException
    {
        return get(device, GL2ES2).getImpl();
    }

    /** 
     * Calls {@link #getGL2ES2(AbstractGraphicsDevice)} using the default device. 
     * @see #getGL2ES2(AbstractGraphicsDevice)
     */
    public static GLProfile getGL2ES2()
        throws GLException
    {
        return get(defaultDevice, GL2ES2).getImpl();
    }

    /** Returns a GLProfile object.
     * verifies the given profile and chooses an appropriate implementation.
     * A generic value of <code>null</code> or <code>GL</code> will result in
     * the default profile.
     *
     * @param device a valid AbstractGraphicsDevice, or <code>null</null> for the default device.
     * @param profile a valid GLProfile name ({@link #GL4bc}, {@link #GL4}, {@link #GL2}, ..), 
     *        or <code>[ null, GL ]</code> for the default profile.
     * @throws GLException if the requested profile is not available for the device.
     */
    public static GLProfile get(AbstractGraphicsDevice device, String profile)
        throws GLException
    {
        if(null==profile || profile.equals("GL")) {
            profile = GL_DEFAULT;
        }
        final HashMap<String /*GLProfile_name*/, GLProfile> glpMap = getProfileMap(device, true);
        final GLProfile glp = glpMap.get(profile);
        if(null == glp) {
            throw new GLException("Profile "+profile+" is not available on "+device+", but: "+glpMap.values());
        }
        return glp;
    }

    /** Uses the default device 
     * @param profile a valid GLProfile name ({@link #GL4bc}, {@link #GL4}, {@link #GL2}, ..), 
     *        or <code>[ null, GL ]</code> for the default profile.
     * @throws GLException if the requested profile is not available for the default device.
     */
    public static GLProfile get(String profile)
        throws GLException
    {
        return get(defaultDevice, profile);
    }

    /**
     * Returns the first profile from the given list,
     * where an implementation is available.
     *
     * @param device a valid AbstractGraphicsDevice, or <code>null</null> for the default device.
     * @param profiles array of valid GLProfile name ({@link #GL4bc}, {@link #GL4}, {@link #GL2}, ..) 
     * @throws GLException if the non of the requested profiles is available for the device.
     */
    public static GLProfile get(AbstractGraphicsDevice device, String[] profiles)
        throws GLException
    {
        HashMap<String /*GLProfile_name*/, GLProfile> map = getProfileMap(device, true);
        for(int i=0; i<profiles.length; i++) {
            String profile = profiles[i];
            GLProfile glProfile = map.get(profile);
            if(null!=glProfile) {
                return glProfile;
            }
        }
        throw new GLException("Profiles "+array2String(profiles)+" not available on device "+device);
    }

    /** Uses the default device 
     * @param profiles array of valid GLProfile name ({@link #GL4bc}, {@link #GL4}, {@link #GL2}, ..) 
     * @throws GLException if the non of the requested profiles is available for the default device.
     */
    public static GLProfile get(String[] profiles)
        throws GLException
    {
        return get(defaultDevice, profiles);
    }
    
    /** Indicates whether the native OpenGL ES1 profile is in use. 
     * This requires an EGL interface.
     */
    public static boolean usesNativeGLES1(String profileImpl) {
        return GLES1.equals(profileImpl);
    }

    /** Indicates whether the native OpenGL ES2 profile is in use. 
     * This requires an EGL or ES2 compatible interface.
     */
    public static boolean usesNativeGLES2(String profileImpl) {
        return GLES2.equals(profileImpl);
    }

    /** Indicates whether either of the native OpenGL ES profiles are in use. */
    public static boolean usesNativeGLES(String profileImpl) {
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
        return getGLImplBaseClassName(getImplName());
    }

    /**
     * @param o GLProfile object to compare with
     * @return true if given Object is a GLProfile and
     *         if both, profile and profileImpl is equal with this.
     */
    public final boolean equals(Object o) {
        if(this==o) { return true; }
        if(o instanceof GLProfile) {
            GLProfile glp = (GLProfile)o;
            return getName().equals(glp.getName()) && getImplName().equals(glp.getImplName()) ;
        }
        return false;
    }

    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + getImplName().hashCode();
        hash = 97 * hash + getName().hashCode();
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

    /** return this profiles name */
    public final String getName() {
        return profile;
    }

    /** return this profiles implementation, eg. GL2ES2 -> GL2, or GL3 -> GL3 */
    public final GLProfile getImpl() {
        return null != profileImpl ? profileImpl : this;
    }
    
    /** 
     * return this profiles implementation name, eg. GL2ES2 -> GL2, or GL3 -> GL3 
     */
    public final String getImplName() {
        return null != profileImpl ? profileImpl.getName() : getName();
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
        return GLES1.equals(getImplName());
    }

    /** Indicates whether this profile uses the native OpenGL ES2 implementations. */
    public final boolean usesNativeGLES2() {
        return GLES2.equals(getImplName());
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
            /** unable to validate .. could be any valid type/component combination
            switch(comps) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    break;
                default: 
                    if(throwException) {
                        throw new GLException("Illegal component number for "+arrayName+" on profile GLES2: "+comps);
                    }
                    return false;
            } */
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
        return "GLProfile[" + getName() + "/" + getImplName() + "]";
    }

    private static /*final*/ boolean isAWTAvailable;

    private static /*final*/ boolean hasDesktopGLFactory;    
    private static /*final*/ boolean hasGL234Impl;
    private static /*final*/ boolean hasEGLFactory;
    private static /*final*/ boolean hasGLES2Impl;
    private static /*final*/ boolean hasGLES1Impl;

    private static /*final*/ GLDrawableFactoryImpl eglFactory;
    private static /*final*/ GLDrawableFactoryImpl desktopFactory;
    private static /*final*/ AbstractGraphicsDevice defaultDevice;
    private static /*final*/ AbstractGraphicsDevice defaultDesktopDevice;
    private static /*final*/ AbstractGraphicsDevice defaultEGLDevice;

    private static volatile boolean initialized = false;
    private static RecursiveThreadGroupLock initLock = LockFactory.createRecursiveThreadGroupLock();

    /**
     * Tries the profiles implementation and native libraries.
     */
    private static void initProfilesForDefaultDevices(boolean firstUIActionOnProcess) {
        NativeWindowFactory.initSingleton(firstUIActionOnProcess);

        if(DEBUG) {
            System.err.println("GLProfile.init firstUIActionOnProcess: "+ firstUIActionOnProcess
                               + ", thread: " + Thread.currentThread().getName());
            System.err.println(VersionUtil.getPlatformInfo());
            System.err.println(GlueGenVersion.getInstance());
            System.err.println(NativeWindowVersion.getInstance());
            System.err.println(JoglVersion.getInstance());
        }

        ClassLoader classloader = GLProfile.class.getClassLoader();

        isAWTAvailable = NativeWindowFactory.isAWTAvailable() &&
                         ReflectionUtil.isClassAvailable("javax.media.opengl.awt.GLCanvas", classloader) ; // JOGL

        // depends on hasDesktopGLFactory
        hasGL234Impl   = ReflectionUtil.isClassAvailable("jogamp.opengl.gl4.GL4bcImpl", classloader);
        
        // depends on hasEGLFactory
        hasGLES1Impl   = ReflectionUtil.isClassAvailable("jogamp.opengl.es1.GLES1Impl", classloader);
        hasGLES2Impl   = ReflectionUtil.isClassAvailable("jogamp.opengl.es2.GLES2Impl", classloader);
        
        //
        // Iteration of desktop GL availability detection
        // utilizing the detected GL version in the shared context.
        //
        // - Instantiate GLDrawableFactory incl its shared dummy drawable/context,
        //   which will register at GLContext ..
        //
        GLDrawableFactory.initSingleton();
        
        Throwable t=null;
        // if successfull it has a shared dummy drawable and context created
        try {
            desktopFactory = (GLDrawableFactoryImpl) GLDrawableFactory.getFactoryImpl(GL2);
            if(null != desktopFactory) {
                DesktopGLDynamicLookupHelper glLookupHelper = (DesktopGLDynamicLookupHelper) desktopFactory.getGLDynamicLookupHelper(0);
                if(null!=glLookupHelper) {
                    hasDesktopGLFactory = glLookupHelper.isLibComplete() && hasGL234Impl;
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
            if(null == desktopFactory) {
                System.err.println("Info: GLProfile.init - Desktop GLDrawable factory not available");
            }
        }

        if(null == desktopFactory) {
            hasDesktopGLFactory   = false;
            hasGL234Impl   = false;
        } else {
            defaultDesktopDevice = desktopFactory.getDefaultDevice();
            defaultDevice = defaultDesktopDevice;
        }

        if ( ReflectionUtil.isClassAvailable("jogamp.opengl.egl.EGLDrawableFactory", classloader) ) {
            t=null;
            try {
                eglFactory = (GLDrawableFactoryImpl) GLDrawableFactory.getFactoryImpl(GLES2);
                if(null != eglFactory) {
                    hasEGLFactory = true;
                    GLDynamicLookupHelper eglLookupHelper = eglFactory.getGLDynamicLookupHelper(2);
                    // update hasGLES1Impl, hasGLES2Impl based on EGL
                    if(null!=eglLookupHelper) {
                        hasGLES2Impl = eglLookupHelper.isLibComplete() && hasGLES2Impl;
                    }
                    eglLookupHelper = eglFactory.getGLDynamicLookupHelper(1);
                    if(null!=eglLookupHelper) {
                        hasGLES1Impl = eglLookupHelper.isLibComplete() && hasGLES1Impl;
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
            if(DEBUG) {
                if(null!=t) {
                    t.printStackTrace();
                }
                if(null == eglFactory) {
                    System.err.println("Info: GLProfile.init - EGL GLDrawable factory not available");
                }
            }
        }

        if(null == eglFactory) {
            hasGLES2Impl   = false;
            hasGLES1Impl   = false;
        } else {
            defaultEGLDevice = eglFactory.getDefaultDevice();
            if (null==defaultDevice) {
                defaultDevice = defaultEGLDevice;
            }
        }

        final boolean addedDesktopProfile = initProfilesForDevice(defaultDesktopDevice);
        final boolean addedEGLProfile = initProfilesForDevice(defaultEGLDevice);
        final boolean addedAnyProfile = addedDesktopProfile || addedEGLProfile ;

        if(DEBUG) {
            System.err.println("GLProfile.init addedAnyProfile(d/e) "+addedAnyProfile+" ("+addedDesktopProfile+"/"+addedEGLProfile+")");
            System.err.println("GLProfile.init isAWTAvailable       "+isAWTAvailable);
            System.err.println("GLProfile.init hasDesktopGLFactory  "+hasDesktopGLFactory);
            System.err.println("GLProfile.init hasGL234Impl         "+hasGL234Impl);
            System.err.println("GLProfile.init hasEGLFactory        "+hasEGLFactory);
            System.err.println("GLProfile.init hasGLES1Impl         "+hasGLES1Impl);
            System.err.println("GLProfile.init hasGLES2Impl         "+hasGLES2Impl);
            System.err.println("GLProfile.init defaultDesktopDevice "+defaultDesktopDevice);
            System.err.println("GLProfile.init defaultEGLDevice     "+defaultEGLDevice);
            System.err.println("GLProfile.init defaultDevice        "+defaultDevice);
            System.err.println("GLProfile.init: "+array2String(GL_PROFILE_LIST_ALL)+", "+ glAvailabilityToString());
        }
    }

    /**
     * @param device the device for which profiles shall be initialized
     * @return true if any profile for the device exists, otherwise false
     */
    private static boolean initProfilesForDevice(AbstractGraphicsDevice device) {
        if(null == device) {
            return false;
        }
        initLock.lock();
        try {
            GLDrawableFactory factory = GLDrawableFactory.getFactoryImpl(device);
            factory.enterThreadCriticalZone();
            try {
                return initProfilesForDeviceCritical(device);
            } finally {
                factory.leaveThreadCriticalZone();
            }
        } finally {
            initLock.unlock();
        }
    }
    private static boolean initProfilesForDeviceCritical(AbstractGraphicsDevice device) {
        boolean isSet = GLContext.getAvailableGLVersionsSet(device);

        if(DEBUG) {
            String msg = "Info: GLProfile.initProfilesForDevice: "+device+", isSet "+isSet;
            Throwable t = new Throwable(msg);
            t.printStackTrace();
            // System.err.println(msg);
        }
        if(isSet) {
            return GLProfile.isAvailable(device, GL_DEFAULT);
        }

        boolean addedDesktopProfile = false;
        boolean addedEGLProfile = false;
        boolean deviceIsDesktopCompatible = false;
        boolean deviceIsEGLCompatible = false;
        
        if( hasDesktopGLFactory && ( deviceIsDesktopCompatible = desktopFactory.getIsDeviceCompatible(device)) ) {
            // 1st pretend we have all Desktop and EGL profiles ..
            computeProfileMap(device, true /* desktopCtxUndef*/, true  /* esCtxUndef */);

            // Triggers eager initialization of share context in GLDrawableFactory for the device,
            // hence querying all available GLProfiles
            final Thread sharedResourceThread = desktopFactory.getSharedResourceThread();
            if(null != sharedResourceThread) {
                initLock.addOwner(sharedResourceThread);
            }
            boolean desktopSharedCtxAvail = desktopFactory.createSharedResource(device);
            if(null != sharedResourceThread) {
                initLock.removeOwner(sharedResourceThread);
            }
            if(!desktopSharedCtxAvail) {
                hasDesktopGLFactory = false;
            }
            if (DEBUG) {
                System.err.println("GLProfile.initProfilesForDevice: "+device+": desktop Shared Ctx "+desktopSharedCtxAvail);
            }
            if( hasDesktopGLFactory && null == GLContext.getAvailableGLVersion(device, 2, GLContext.CTX_PROFILE_COMPAT) ) {
                // nobody yet set the available desktop versions, see {@link GLContextImpl#makeCurrent},
                // so we have to add the usual suspect
                GLContext.mapAvailableGLVersion(device,
                                                2, GLContext.CTX_PROFILE_COMPAT,
                                                1, 5, GLContext.CTX_PROFILE_COMPAT|GLContext.CTX_OPTION_ANY);
            }
            addedDesktopProfile = computeProfileMap(device, false /* desktopCtxUndef*/, false /* esCtxUndef */);
        } else if( hasEGLFactory && ( hasGLES2Impl || hasGLES1Impl ) && 
                   ( deviceIsEGLCompatible = eglFactory.getIsDeviceCompatible(device)) ) {
            // 1st pretend we have all EGL profiles ..
            computeProfileMap(device, false /* desktopCtxUndef*/, true /* esCtxUndef */);

            // Triggers eager initialization of share context in GLDrawableFactory for the device,
            // hence querying all available GLProfiles
            final Thread sharedResourceThread = eglFactory.getSharedResourceThread();
            if(null != sharedResourceThread) {
                initLock.addOwner(sharedResourceThread);
            }
            boolean eglSharedCtxAvail = eglFactory.createSharedResource(device);
            if(null != sharedResourceThread) {
                initLock.removeOwner(sharedResourceThread);
            }
            if(!eglSharedCtxAvail) {
                // Remark: On Windows there is a libEGL.dll delivered w/ Chrome 15.0.874.121m and Firefox 8.0.1
                // but it seems even EGL.eglInitialize(eglDisplay, null, null) 
                // fails in some scenarios (eg VirtualBox 4.1.6) w/ EGL error 0x3001 (EGL_NOT_INITIALIZED).
                hasEGLFactory = false;
                hasGLES2Impl = false;
                hasGLES1Impl = false;
            }
            if (DEBUG) {
                System.err.println("GLProfile.initProfilesForDevice: "+device+": egl Shared Ctx "+eglSharedCtxAvail);
            }
            if(hasGLES2Impl && null == GLContext.getAvailableGLVersion(device, 2, GLContext.CTX_PROFILE_ES) ) {
                // nobody yet set the available desktop versions, see {@link GLContextImpl#makeCurrent},
                // so we have to add the usual suspect
                GLContext.mapAvailableGLVersion(device,
                                                2, GLContext.CTX_PROFILE_ES,
                                                2, 0, GLContext.CTX_PROFILE_ES|GLContext.CTX_OPTION_ANY);
            }
            if(hasGLES1Impl && null == GLContext.getAvailableGLVersion(device, 1, GLContext.CTX_PROFILE_ES)) {
                // nobody yet set the available desktop versions, see {@link GLContextImpl#makeCurrent},
                // so we have to add the usual suspect
                GLContext.mapAvailableGLVersion(device,
                                                1, GLContext.CTX_PROFILE_ES,
                                                1, 0, GLContext.CTX_PROFILE_ES|GLContext.CTX_OPTION_ANY);
            }
            addedEGLProfile = computeProfileMap(device, false /* desktopCtxUndef*/, false /* esCtxUndef */);
        } else {
            setProfileMap(device, new HashMap<String /*GLProfile_name*/, GLProfile>()); // empty
            if(DEBUG) {
                System.err.println("GLProfile: device could not be initialized: "+device);
                System.err.println("GLProfile: compatible w/ desktop: "+deviceIsDesktopCompatible+
                                            ", egl "+deviceIsEGLCompatible);
                System.err.println("GLProfile: desktoplFactory      "+desktopFactory);
                System.err.println("GLProfile: eglFactory           "+eglFactory);
                System.err.println("GLProfile: hasGLES1Impl         "+hasGLES1Impl);
                System.err.println("GLProfile: hasGLES2Impl         "+hasGLES2Impl);
            }
        }

        if(!GLContext.getAvailableGLVersionsSet(device)) {
            GLContext.setAvailableGLVersionsSet(device);
        }

        if (DEBUG) {
            System.err.println("GLProfile.initProfilesForDevice: "+device.getConnection()+": added profile(s): desktop "+addedDesktopProfile+", egl "+addedEGLProfile);
            System.err.println("GLProfile.initProfilesForDevice: "+device.getConnection()+": "+glAvailabilityToString(device));
            if(addedDesktopProfile) {
                dumpGLInfo(desktopFactory, device);
                List<GLCapabilitiesImmutable> availCaps = desktopFactory.getAvailableCapabilities(device);
                for(int i=0; i<availCaps.size(); i++) {
                    System.err.println(availCaps.get(i));
                }
            } else if(addedEGLProfile) {
                dumpGLInfo(eglFactory, device);
                List<GLCapabilitiesImmutable> availCaps = eglFactory.getAvailableCapabilities(device);
                for(int i=0; i<availCaps.size(); i++) {
                    System.err.println(availCaps.get(i));
                }
            }
        }

        return addedDesktopProfile || addedEGLProfile;
    }

    private static void dumpGLInfo(GLDrawableFactoryImpl factory, AbstractGraphicsDevice device)  {
        GLContext ctx = factory.getOrCreateSharedContext(device);
        if(null != ctx) {
            System.err.println("GLProfile.dumpGLInfo: "+ctx);
            ctx.makeCurrent();
            try {
                System.err.println(JoglVersion.getGLInfo(ctx.getGL(), null));
            } finally {
                ctx.release();
            }
        } else {
            System.err.println("GLProfile.dumpGLInfo: shared context n/a");
        }
    }

    public static AbstractGraphicsDevice getDefaultDevice() {
        initSingleton();
        return defaultDevice;
    }

    public static AbstractGraphicsDevice getDefaultDesktopDevice() {
        initSingleton();
        return defaultDesktopDevice;
    }

    public static AbstractGraphicsDevice getDefaultEGLDevice() {
        initSingleton();
        return defaultEGLDevice;
    }

    private static String array2String(String[] list) {
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

    private static void glAvailabilityToString(AbstractGraphicsDevice device, StringBuffer sb, int major, int profile) {
        String str = GLContext.getAvailableGLVersionAsString(device, major, profile);
        if(null==str) {
            throw new GLException("Internal Error");
        }
        sb.append("[");
        sb.append(str);
        sb.append("]");
    }

    private static boolean computeProfileMap(AbstractGraphicsDevice device, boolean desktopCtxUndef, boolean esCtxUndef) {
        if (DEBUG) {
            System.err.println("GLProfile.init map "+device.getConnection()+", desktopCtxUndef "+desktopCtxUndef+", esCtxUndef "+esCtxUndef);
        }
        GLProfile defaultGLProfile = null;
        HashMap<String, GLProfile> _mappedProfiles = new HashMap<String, GLProfile>(GL_PROFILE_LIST_ALL.length + 1 /* default */);
        for(int i=0; i<GL_PROFILE_LIST_ALL.length; i++) {
            String profile = GL_PROFILE_LIST_ALL[i];
            String profileImpl = computeProfileImpl(device, profile, desktopCtxUndef, esCtxUndef);
            if(null!=profileImpl) {
                final GLProfile glProfile;
                if(profile.equals(profileImpl)) {
                    glProfile = new GLProfile(profile, null);
                } else {
                    final GLProfile _mglp = _mappedProfiles.get(profileImpl);
                    if(null == _mglp) {
                        throw new InternalError("XXX0");
                    }
                    glProfile = new GLProfile(profile, _mglp);
                }
                _mappedProfiles.put(profile, glProfile);
                if (DEBUG) {
                    System.err.println("GLProfile.init map "+glProfile+" on devide "+device.getConnection());
                }
                if(null==defaultGLProfile) {
                    defaultGLProfile=glProfile;
                    if (DEBUG) {
                        System.err.println("GLProfile.init map default "+glProfile+" on device "+device.getConnection());
                    }
                }
            } else {
                if (DEBUG) {
                    System.err.println("GLProfile.init map *** no mapping for "+profile+" on device "+device.getConnection());
                }
            }
        }
        if(null!=defaultGLProfile) {
            _mappedProfiles.put(GL_DEFAULT, defaultGLProfile);
        }
        setProfileMap(device, _mappedProfiles);
        return _mappedProfiles.size() > 0;
    }

    /**
     * Returns the profile implementation
     */
    private static String computeProfileImpl(AbstractGraphicsDevice device, String profile, boolean desktopCtxUndef, boolean esCtxUndef) {
        // OSX GL3.. doesn't support GLSL<150, 
        // hence GL2ES2 and GL2GL3 need to be mapped on GL2 on OSX for GLSL compatibility.
        final boolean isOSX = Platform.OS_TYPE == Platform.OSType.MACOS;
        
        if (GL2ES1.equals(profile)) {
            if(hasGL234Impl) {
                if(GLContext.isGL4bcAvailable(device)) {
                    return GL4bc;
                } else if(GLContext.isGL3bcAvailable(device)) {
                    return GL3bc;
                } else if(desktopCtxUndef || GLContext.isGL2Available(device)) {
                    return GL2;
                }
            }
            if(hasGLES1Impl && ( esCtxUndef || GLContext.isGLES1Available(device))) {
                return GLES1;
            }
        } else if (GL2ES2.equals(profile)) {
            if(hasGL234Impl) {
                if(!isOSX && GLContext.isGL4bcAvailable(device)) {
                    return GL4bc;
                } else if(!isOSX && GLContext.isGL4Available(device)) {
                    return GL4;
                } else if(!isOSX && GLContext.isGL3bcAvailable(device)) {
                    return GL3bc;
                } else if(!isOSX && GLContext.isGL3Available(device)) {
                    return GL3;
                } else if(desktopCtxUndef || GLContext.isGL2Available(device)) {
                    return GL2;
                }
            }
            if(hasGLES2Impl && ( esCtxUndef || GLContext.isGLES2Available(device))) {
                return GLES2;
            }
        } else if(GL2GL3.equals(profile)) {
            if(hasGL234Impl) {
                if(!isOSX && GLContext.isGL4bcAvailable(device)) {
                    return GL4bc;
                } else if(!isOSX && GLContext.isGL4Available(device)) {
                    return GL4;
                } else if(!isOSX && GLContext.isGL3bcAvailable(device)) {
                    return GL3bc;
                } else if(!isOSX && GLContext.isGL3Available(device)) {
                    return GL3;
                } else if(desktopCtxUndef || GLContext.isGL2Available(device)) {
                    return GL2;
                }
            }
        } else if(GL4bc.equals(profile) && hasGL234Impl && ( desktopCtxUndef || GLContext.isGL4bcAvailable(device))) {
            return GL4bc;
        } else if(GL4.equals(profile) && hasGL234Impl && ( desktopCtxUndef || GLContext.isGL4Available(device))) {
            return GL4;
        } else if(GL3bc.equals(profile) && hasGL234Impl && ( desktopCtxUndef || GLContext.isGL3bcAvailable(device))) {
            return GL3bc;
        } else if(GL3.equals(profile) && hasGL234Impl && ( desktopCtxUndef || GLContext.isGL3Available(device))) {
            return GL3;
        } else if(GL2.equals(profile) && hasGL234Impl && ( desktopCtxUndef || GLContext.isGL2Available(device))) {
            return GL2;
        } else if(GLES2.equals(profile) && hasGLES2Impl && ( esCtxUndef || GLContext.isGLES2Available(device))) {
            return GLES2;
        } else if(GLES1.equals(profile) && hasGLES1Impl && ( esCtxUndef || GLContext.isGLES1Available(device))) {
            return GLES1;
        }
        return null;
    }

    private static String getGLImplBaseClassName(String profileImpl) {
        if ( GL4bc.equals(profileImpl) ||
             GL4.equals(profileImpl)   ||
             GL3bc.equals(profileImpl) ||
             GL3.equals(profileImpl)   ||
             GL2.equals(profileImpl) ) {
            return "jogamp.opengl.gl4.GL4bc";
        } else if(GLES1.equals(profileImpl) || GL2ES1.equals(profileImpl)) {
            return "jogamp.opengl.es1.GLES1";
        } else if(GLES2.equals(profileImpl) || GL2ES2.equals(profileImpl)) {
            return "jogamp.opengl.es2.GLES2";
        } else {
            throw new GLException("unsupported profile \"" + profileImpl + "\"");
        }
    }

    private static /*final*/ HashMap<String /*device_connection*/, HashMap<String /*GLProfile_name*/, GLProfile>> deviceConn2ProfileMap = 
                new HashMap<String /*device_connection*/, HashMap<String /*GLProfile_name*/, GLProfile>>();

    /**
     * This implementation support lazy initialization, while avoiding recursion/deadlocks.<br>
     * If no mapping 'device -> GLProfiles-Map' exists yet, it triggers<br>
     *  - create empty mapping device -> GLProfiles-Map <br>
     *  - initialization<br<
     *
     * @param device the key 'device -> GLProfiles-Map'
     * @param throwExceptionOnZeroProfile true if <code>GLException</code> shall be thrown in case of no mapped profile, otherwise false.
     * @return the GLProfile HashMap if exists, otherwise null 
     * @throws GLException if no profile for the given device is available.
     */
    private static HashMap<String /*GLProfile_name*/, GLProfile> getProfileMap(AbstractGraphicsDevice device, boolean throwExceptionOnZeroProfile) 
        throws GLException 
    {
        initSingleton();

        if(null==device) {
            device = defaultDevice;
        }
        String deviceKey = device.getUniqueID();
        HashMap<String /*GLProfile_name*/, GLProfile> map = deviceConn2ProfileMap.get(deviceKey);
        if( null != map ) {
            return map;
        }
        if( !initProfilesForDevice(device) ) {
            if( throwExceptionOnZeroProfile ) {
                throw new GLException("No Profile available for "+device);
            } else {
                return null;
            }
        }
        map = deviceConn2ProfileMap.get(deviceKey);
        if( null == map && throwExceptionOnZeroProfile ) {
            throw new InternalError("initProfilesForDevice(..) didn't setProfileMap(..) for "+device);
        }
        return map;
    }

    private static void setProfileMap(AbstractGraphicsDevice device, HashMap<String /*GLProfile_name*/, GLProfile> mappedProfiles) {
        synchronized ( deviceConn2ProfileMap ) {
            deviceConn2ProfileMap.put(device.getUniqueID(), mappedProfiles);
        }
    }

    private GLProfile(String profile, GLProfile profileImpl) {
        this.profile = profile;
        this.profileImpl = profileImpl;
    }

    private GLProfile profileImpl = null;
    private String profile = null;
}
