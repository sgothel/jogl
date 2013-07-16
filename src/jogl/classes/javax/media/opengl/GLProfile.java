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

import jogamp.nativewindow.NWJNILibLoader;
import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableFactoryImpl;
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
     *
     * <p>
     * This method shall not need to be called for other reasons than having a defined initialization sequence.
     * </p>
     * 
     * <P>
     * In case this method is not invoked, GLProfile is initialized implicit by
     * the first call to {@link #getDefault()}, {@link #get(java.lang.String)}.
     * <P>
     * 
     * <p>
     * To initialize JOGL at startup ASAP, this method may be invoked in the <i>main class</i>'s 
     * static initializer block, in the <i>static main() method</i> or in the <i>Applet init() method</i>.
     * </p>
     * 
     * <p>
     * Since JOGL's initialization is complex and involves multi threading, it is <b>not</b> recommended
     * to be have it invoked on the AWT EDT thread. In case all JOGL usage is performed 
     * on the AWT EDT, invoke this method outside the AWT EDT - see above.
     * </p>
     * 
     */
    public static void initSingleton() {
        final boolean justInitialized; 
        initLock.lock();
        try {
            if(!initialized) { // volatile: ok
                initialized = true;
                justInitialized = true;
                if(DEBUG) {
                    System.err.println("GLProfile.initSingleton() - thread "+Thread.currentThread().getName());
                    Thread.dumpStack();
                }
    
                // run the whole static initialization privileged to speed up,
                // since this skips checking further access
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        Platform.initSingleton();
                        
                        // Performance hack to trigger classloading of the GL classes impl, which makes up to 12%, 800ms down to 700ms
                        new Thread(new Runnable() {
                          public void run() {
                              final ClassLoader cl = GLProfile.class.getClassLoader();
                              try {
                                  ReflectionUtil.createInstance(getGLImplBaseClassName(GL4bc)+"Impl", new Class[] { GLProfile.class, GLContextImpl.class }, new Object[] { null, null }, cl);
                              } catch (Throwable t) {}
                              try {
                                  ReflectionUtil.createInstance(getGLImplBaseClassName(GLES3)+"Impl", new Class[] { GLProfile.class, GLContextImpl.class }, new Object[] { null, null }, cl);              
                              } catch (Throwable t) {}
                              try {
                                  ReflectionUtil.createInstance(getGLImplBaseClassName(GLES1)+"Impl", new Class[] { GLProfile.class, GLContextImpl.class }, new Object[] { null, null }, cl);              
                              } catch (Throwable t) {}
                          }
                        }, "GLProfile-GL_Bootstrapping").start();      

                        
                        if(TempJarCache.isInitialized()) {
                           final ClassLoader cl = GLProfile.class.getClassLoader();
                           // either: [jogl-all.jar, jogl-all-noawt.jar, jogl-all-mobile.jar] -> jogl-all-natives-<os.and.arch>.jar
                           // or:     nativewindow-core.jar                                   -> nativewindow-natives-<os.and.arch>.jar,
                           //         jogl-core.jar                                           -> jogl-natives-<os.and.arch>.jar,
                           //        (newt-core.jar                                           -> newt-natives-<os.and.arch>.jar)? (if available)
                           final String newtFactoryClassName = "com.jogamp.newt.NewtFactory";
                           final Class<?>[] classesFromJavaJars = new Class<?>[] { NWJNILibLoader.class, GLProfile.class, null };
                           if( ReflectionUtil.isClassAvailable(newtFactoryClassName, cl) ) {
                               classesFromJavaJars[2] = ReflectionUtil.getClass(newtFactoryClassName, false, cl);
                           }
                           JNILibLoaderBase.addNativeJarLibs(classesFromJavaJars, "-all", new String[] { "-noawt", "-mobile", "-core" } );
                        }
                        initProfilesForDefaultDevices();
                        return null;
                    }
                });
            } else {
                justInitialized = false;
            }
        } finally {
            initLock.unlock();
        }
        if(DEBUG) {
            if( justInitialized && ( hasGL234Impl || hasGLES1Impl || hasGLES3Impl ) ) {
                System.err.println(JoglVersion.getDefaultOpenGLInfo(defaultDevice, null, true));
            }
        }        
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
     * Manual shutdown method, may be called after your last JOGL use
     * within the running JVM.<br>
     * It releases all temporary created resources, ie issues {@link javax.media.opengl.GLDrawableFactory#shutdown()}.<br>
     * The shutdown implementation is called via the JVM shutdown hook, if not manually invoked.<br>
     * <p>
     * This method shall not need to be called for other reasons than issuing a proper shutdown of resources at a defined time.
     * </p>
     */
    public static void shutdown() {
        initLock.lock();
        try {
            if(initialized) { // volatile: ok
                initialized = false;
                if(DEBUG) {
                    System.err.println("GLProfile.shutdown() - thread "+Thread.currentThread().getName());
                    Thread.dumpStack();
                }                    
                GLDrawableFactory.shutdown();
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
        return glAvailabilityToString(device, null).toString();
    }
    
    public static StringBuilder glAvailabilityToString(AbstractGraphicsDevice device, StringBuilder sb) {
        return glAvailabilityToString(device, sb, null, 0);
    }
    private static StringBuilder doIndent(StringBuilder sb, String indent, int indentCount) {
        while(indentCount>0) {
            sb.append(indent);
            indentCount--;
        }
        return sb;
    }
    public static StringBuilder glAvailabilityToString(AbstractGraphicsDevice device, StringBuilder sb, String indent, int indentCount) {
        boolean avail;
        if(null == sb) {
            sb = new StringBuilder();
        }
        final boolean useIndent = null != indent;

        initSingleton();
        
        if(null==device) {
            device = defaultDevice;
        }
        final HashMap<String /*GLProfile_name*/, GLProfile> map = getProfileMap(device, false);
        
        if(useIndent) {
            doIndent(sb, indent, indentCount).append("Native");
            indentCount++;
            doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("GL4bc").append(indent);
        } else {
            sb.append("Native[GL4bc ");
        }        
        avail=isAvailableImpl(map, GL4bc);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb.append(" "), 4, GLContext.CTX_PROFILE_COMPAT);
        }

        if(useIndent) {
            doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("GL4").append(indent);
        } else {
            sb.append(", GL4 ");
        }
        avail=isAvailableImpl(map, GL4);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb.append(" "), 4, GLContext.CTX_PROFILE_CORE);
        }

        if(useIndent) {
            doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("GL4ES3").append(indent);
        } else {
            sb.append(", GL4ES3 ");
        }
        sb.append(isAvailableImpl(map, GL4ES3));
        
        if(useIndent) {
            doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("GLES3").append(indent);
        } else {
            sb.append(", GLES3 ");
        }
        avail=isAvailableImpl(map, GLES3);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb.append(" "), 3, GLContext.CTX_PROFILE_ES);
        }

        if(useIndent) {
            doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("GL3bc").append(indent);
        } else {
            sb.append(", GL3bc ");
        }
        avail=isAvailableImpl(map, GL3bc);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb.append(" "), 3, GLContext.CTX_PROFILE_COMPAT);
        }

        if(useIndent) {
            doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("GL3").append(indent);
        } else {
            sb.append(", GL3 ");
        }
        avail=isAvailableImpl(map, GL3);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb.append(" "), 3, GLContext.CTX_PROFILE_CORE);
        }

        if(useIndent) {
            doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("GL2").append(indent);
        } else {
            sb.append(", GL2 ");
        }
        avail=isAvailableImpl(map, GL2);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb.append(" "), 2, GLContext.CTX_PROFILE_COMPAT);
        }

        if(useIndent) {
            doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("GL2ES2").append(indent);
        } else {
            sb.append(", GL2ES2 ");
        }
        sb.append(isAvailableImpl(map, GL2ES2));

        if(useIndent) {
            doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("GLES2").append(indent);
        } else {
            sb.append(", GLES2 ");
        }
        avail=isAvailableImpl(map, GLES2);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb.append(" "), 2, GLContext.CTX_PROFILE_ES);
        }

        if(useIndent) {
            doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("GL2ES1").append(indent);
        } else {
            sb.append(", GL2ES1 ");
        }
        sb.append(isAvailableImpl(map, GL2ES1));

        if(useIndent) {
            doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("GLES1").append(indent);
        } else {
            sb.append(", GLES1 ");
        }
        avail=isAvailableImpl(map, GLES1);
        sb.append(avail);
        if(avail) {
            glAvailabilityToString(device, sb.append(" "), 1, GLContext.CTX_PROFILE_ES);
        }

        if(useIndent) {
            indentCount--;
            doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("Profiles");
            indentCount++;
        } else {
            sb.append("], Profiles[");
        }
        
        if(null != map) {
            for(Iterator<GLProfile> i=map.values().iterator(); i.hasNext(); ) {
                if(useIndent) {
                    doIndent(sb.append(Platform.getNewline()), indent, indentCount);
                }
                sb.append(i.next().toString());
                if(!useIndent) {
                    sb.append(", ");
                }
            }
            if(useIndent) {
                doIndent(sb.append(Platform.getNewline()), indent, indentCount).append("default ");
            } else {
                sb.append(", default ");
            }
            try {
                sb.append(getDefault(device));
            } catch (GLException gle) {
                sb.append("n/a");
            }
        }
        if(useIndent) {
            sb.append(Platform.getNewline());
        } else {
            sb.append("]");
        }

        return sb;
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

    /** The embedded OpenGL profile ES 3.x, with x >= 0 */
    public static final String GLES3 = "GLES3";

    /** The intersection of the desktop GL2 and embedded ES1 profile */
    public static final String GL2ES1 = "GL2ES1";

    /** The intersection of the desktop GL3, GL2 and embedded ES2 profile */
    public static final String GL2ES2 = "GL2ES2";

    /** The intersection of the desktop GL3 and GL2 profile */
    public static final String GL2GL3 = "GL2GL3";

    /** The intersection of the desktop GL4 and ES3 profile */
    public static final String GL4ES3 = "GL4ES3";
    
    /** The default profile, used for the device default profile map  */
    private static final String GL_DEFAULT = "GL_DEFAULT";

    /** 
     * All GL Profiles in the order of default detection.
     * Desktop compatibility profiles (the one with fixed function pipeline) comes first
     * from highest to lowest version.
     * <p> This includes the generic subset profiles GL2GL3, GL2ES2 and GL2ES1.</p>
     *
     * <ul>
     *  <li> GL4bc </li>
     *  <li> GL3bc </li>
     *  <li> GL2 </li>
     *  <li> GL4 </li>
     *  <li> GL3 </li>
     *  <li> GLES3 </li>
     *  <li> GL4ES3 </li>
     *  <li> GL2GL3 </li>
     *  <li> GLES2 </li>
     *  <li> GL2ES2 </li>
     *  <li> GLES1 </li>
     *  <li> GL2ES1 </li>
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_ALL = new String[] { GL4bc, GL3bc, GL2, GL4, GL3, GLES3, GL4ES3, GL2GL3, GLES2, GL2ES2, GLES1, GL2ES1 };
    
    /**
     * Order of maximum profiles.
     *
     * <ul>
     *  <li> GL4bc </li>
     *  <li> GL4 </li>
     *  <li> GL3bc </li>
     *  <li> GL3 </li>
     *  <li> GLES3 </li>
     *  <li> GL2 </li>
     *  <li> GLES2 </li>
     *  <li> GLES1 </li>
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MAX = new String[] { GL4bc, GL4, GL3bc, GL3, GLES3, GL2, GLES2, GLES1 };

    /**
     * Order of minimum profiles.
     *
     * <ul>
     *  <li> GLES1 </li>
     *  <li> GLES2 </li>
     *  <li> GL2 </li>
     *  <li> GLES3 </li>
     *  <li> GL3 </li>
     *  <li> GL3bc </li>
     *  <li> GL4 </li>
     *  <li> GL4bc </li>
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MIN = new String[] { GLES1, GLES2, GL2, GLES3, GL3, GL3bc, GL4, GL4bc };
    
    /**
     * Order of minimum original desktop profiles.
     *
     * <ul>
     *  <li> GL2 </li>
     *  <li> GL3bc </li>
     *  <li> GL4bc </li>
     *  <li> GL3 </li>
     *  <li> GL4 </li>
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MIN_DESKTOP = new String[] { GL2, GL3bc, GL4bc, GL3, GL4 };
    
    /**
     * Order of maximum fixed function profiles
     *
     * <ul>
     *  <li> GL4bc </li>
     *  <li> GL3bc </li>
     *  <li> GL2 </li>
     *  <li> GLES1 </li>
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MAX_FIXEDFUNC = new String[] { GL4bc, GL3bc, GL2, GLES1 };

    /**
     * Order of maximum programmable shader profiles
     *
     * <ul>
     *  <li> GL4bc </li>
     *  <li> GL4 </li>
     *  <li> GL3bc </li>
     *  <li> GL3 </li>
     *  <li> GLES3 </li>
     *  <li> GL2 </li>
     *  <li> GLES2 </li>
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MAX_PROGSHADER = new String[] { GL4bc, GL4, GL3bc, GL3, GLES3, GL2, GLES2 };

    /**
     * Order of maximum programmable shader <i>core only</i> profiles
     *
     * <ul>
     *  <li> GL4 </li>
     *  <li> GL3 </li>
     *  <li> GLES3 </li>
     *  <li> GLES2 </li>
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MAX_PROGSHADER_CORE = new String[] { GL4, GL3, GLES3, GLES2 };
    
    /** Returns a default GLProfile object, reflecting the best for the running platform.
     * It selects the first of the set {@link GLProfile#GL_PROFILE_LIST_ALL}
     * and favors hardware acceleration.
     * @throws GLException if no profile is available for the device.
     * @see #GL_PROFILE_LIST_ALL
     */
    public static GLProfile getDefault(AbstractGraphicsDevice device) {
        GLProfile glp = get(device, GL_DEFAULT);
        return glp;
    }

    /** Returns a default GLProfile object, reflecting the best for the running platform.
     * It selects the first of the set {@link GLProfile#GL_PROFILE_LIST_ALL}
     * and favors hardware acceleration.
     * <p>Uses the default device.</p> 
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
    public static GLProfile getMaximum(AbstractGraphicsDevice device, boolean favorHardwareRasterizer)
        throws GLException
    {
        return get(device, GL_PROFILE_LIST_MAX, favorHardwareRasterizer);
    }

    /** Uses the default device 
     * @throws GLException if no profile is available for the default device.
     * @see #GL_PROFILE_LIST_MAX
     */
    public static GLProfile getMaximum(boolean favorHardwareRasterizer)
        throws GLException
    {
        return get(GL_PROFILE_LIST_MAX, favorHardwareRasterizer);
    }

    /**
     * Returns the lowest profile.
     * It selects the first of the set: {@link GLProfile#GL_PROFILE_LIST_MIN}
     *
     * @throws GLException if no desktop profile is available for the device.
     * @see #GL_PROFILE_LIST_MIN
     */
    public static GLProfile getMinimum(AbstractGraphicsDevice device, boolean favorHardwareRasterizer)
        throws GLException
    {
        return get(device, GL_PROFILE_LIST_MIN, favorHardwareRasterizer);
    }

    /** Uses the default device 
     * @throws GLException if no desktop profile is available for the default device.
     * @see #GL_PROFILE_LIST_MIN
     */
    public static GLProfile getMinimum(boolean favorHardwareRasterizer)
        throws GLException
    {
        return get(GL_PROFILE_LIST_MIN, favorHardwareRasterizer);
    }


    /**
     * Returns the highest profile, implementing the fixed function pipeline.
     * It selects the first of the set: {@link GLProfile#GL_PROFILE_LIST_MAX_FIXEDFUNC}
     *
     * @throws GLException if no fixed function profile is available for the device.
     * @see #GL_PROFILE_LIST_MAX_FIXEDFUNC
     */
    public static GLProfile getMaxFixedFunc(AbstractGraphicsDevice device, boolean favorHardwareRasterizer)
        throws GLException
    {
        return get(device, GL_PROFILE_LIST_MAX_FIXEDFUNC, favorHardwareRasterizer);
    }

    /** Uses the default device 
     * @throws GLException if no fixed function profile is available for the default device.
     * @see #GL_PROFILE_LIST_MAX_FIXEDFUNC
     */
    public static GLProfile getMaxFixedFunc(boolean favorHardwareRasterizer)
        throws GLException
    {
        return get(GL_PROFILE_LIST_MAX_FIXEDFUNC, favorHardwareRasterizer);
    }

    /**
     * Returns the highest profile, implementing the programmable shader pipeline.
     * It selects the first of the set: {@link GLProfile#GL_PROFILE_LIST_MAX_PROGSHADER}
     *
     * @throws GLException if no programmable profile is available for the device.
     * @see #GL_PROFILE_LIST_MAX_PROGSHADER
     */
    public static GLProfile getMaxProgrammable(AbstractGraphicsDevice device, boolean favorHardwareRasterizer)
        throws GLException
    {
        return get(device, GL_PROFILE_LIST_MAX_PROGSHADER, favorHardwareRasterizer);
    }

    /** Uses the default device 
     * @throws GLException if no programmable profile is available for the default device.
     * @see #GL_PROFILE_LIST_MAX_PROGSHADER
     */
    public static GLProfile getMaxProgrammable(boolean favorHardwareRasterizer)
        throws GLException
    {
        return get(GL_PROFILE_LIST_MAX_PROGSHADER, favorHardwareRasterizer);
    }

    /**
     * Returns the highest profile, implementing the programmable shader <i>core</i> pipeline <i>only</i>.
     * It selects the first of the set: {@link GLProfile#GL_PROFILE_LIST_MAX_PROGSHADER_CORE}
     *
     * @throws GLException if no programmable core profile is available for the device.
     * @see #GL_PROFILE_LIST_MAX_PROGSHADER_CORE
     */
    public static GLProfile getMaxProgrammableCore(AbstractGraphicsDevice device, boolean favorHardwareRasterizer)
        throws GLException
    {            
        return get(device, GL_PROFILE_LIST_MAX_PROGSHADER_CORE, favorHardwareRasterizer);
    }

    /** Uses the default device 
     * @throws GLException if no programmable core profile is available for the default device.
     * @see #GL_PROFILE_LIST_MAX_PROGSHADER_CORE
     */
    public static GLProfile getMaxProgrammableCore(boolean favorHardwareRasterizer)
        throws GLException
    {
        return get(GL_PROFILE_LIST_MAX_PROGSHADER_CORE, favorHardwareRasterizer);
    }
    
    /** 
     * Returns the GL2ES1 profile implementation, hence compatible w/ GL2ES1.<br/>
     * It returns:
     * <pre>
     *   GLProfile.get(device, GLProfile.GL2ES1).getImpl());
     * </pre>
     * <p>Selection favors hardware rasterizer.</p>
     *
     * @throws GLException if no GL2ES1 compatible profile is available for the default device.
     * @see #isGL2ES1()
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
     * <p>Selection favors hardware rasterizer.</p>
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
     * <p>Selection favors hardware rasterizer.</p>
     *
     * @throws GLException if no GL2ES2 compatible profile is available for the default device.
     * @see #isGL2ES2()
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
     * <p>Selection favors hardware rasterizer.</p>
     * @see #getGL2ES2(AbstractGraphicsDevice)
     */
    public static GLProfile getGL2ES2()
        throws GLException
    {
        return get(defaultDevice, GL2ES2).getImpl();
    }

    /** 
     * Returns the GL4ES3 profile implementation, hence compatible w/ GL4ES3.<br/>
     * It returns:
     * <pre>
     *   GLProfile.get(device, GLProfile.GL4ES3).getImpl());
     * </pre>
     * <p>Selection favors hardware rasterizer.</p>
     *
     * @throws GLException if no GL4ES3 compatible profile is available for the default device.
     * @see #isGL4ES3()
     * @see #get(AbstractGraphicsDevice, String)
     * @see #getImpl()
     */
    public static GLProfile getGL4ES3(AbstractGraphicsDevice device)
        throws GLException
    {
        return get(device, GL4ES3).getImpl();
    }

    /** 
     * Calls {@link #getGL4ES3(AbstractGraphicsDevice)} using the default device. 
     * <p>Selection favors hardware rasterizer.</p>
     * @see #getGL4ES3(AbstractGraphicsDevice)
     */
    public static GLProfile getGL4ES3()
        throws GLException
    {
        return get(defaultDevice, GL4ES3).getImpl();
    }

    /** 
     * Returns the GL2GL3 profile implementation, hence compatible w/ GL2GL3.<br/>
     * It returns:
     * <pre>
     *   GLProfile.get(device, GLProfile.GL2GL3).getImpl());
     * </pre>
     * <p>Selection favors hardware rasterizer.</p>
     *
     * @throws GLException if no GL2GL3 compatible profile is available for the default device.
     * @see #isGL2GL3()
     * @see #get(AbstractGraphicsDevice, String)
     * @see #getImpl()
     */
    public static GLProfile getGL2GL3(AbstractGraphicsDevice device)
        throws GLException
    {
        return get(device, GL2GL3).getImpl();
    }

    /** 
     * Calls {@link #getGL2GL3(AbstractGraphicsDevice)} using the default device. 
     * <p>Selection favors hardware rasterizer.</p>
     * @see #getGL2GL3(AbstractGraphicsDevice)
     */
    public static GLProfile getGL2GL3()
        throws GLException
    {
        return get(defaultDevice, GL2GL3).getImpl();
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
     * @param favorHardwareRasterizer set to true, if hardware rasterizer shall be favored, otherwise false. 
     * @throws GLException if the non of the requested profiles is available for the device.
     */
    public static GLProfile get(AbstractGraphicsDevice device, String[] profiles, boolean favorHardwareRasterizer)
        throws GLException
    {
        GLProfile glProfileAny = null;
        
        HashMap<String /*GLProfile_name*/, GLProfile> map = getProfileMap(device, true);
        for(int i=0; i<profiles.length; i++) {
            final GLProfile glProfile = map.get(profiles[i]);
            if(null!=glProfile) {
                if(!favorHardwareRasterizer) {
                    return glProfile;
                }
                if(glProfile.isHardwareRasterizer()) {
                    return glProfile;
                }
                if(null==glProfileAny) {
                    glProfileAny = glProfile;
                }                
            }
        }
        if(null!=glProfileAny) {
            return glProfileAny;
        }                
        throw new GLException("Profiles "+array2String(profiles)+" not available on device "+device);
    }

    /** Uses the default device 
     * @param profiles array of valid GLProfile name ({@link #GL4bc}, {@link #GL4}, {@link #GL2}, ..) 
     * @param favorHardwareRasterizer set to true, if hardware rasterizer shall be favored, otherwise false. 
     * @throws GLException if the non of the requested profiles is available for the default device.
     */
    public static GLProfile get(String[] profiles, boolean favorHardwareRasterizer)
        throws GLException
    {
        return get(defaultDevice, profiles, favorHardwareRasterizer);
    }
    
    /** Indicates whether the native OpenGL ES1 profile is in use. 
     * This requires an EGL interface.
     */
    public static boolean usesNativeGLES1(String profileImpl) {
        return GLES1.equals(profileImpl);
    }

    /** Indicates whether the native OpenGL ES3 or ES2 profile is in use. 
     * This requires an EGL, ES3 or ES2 compatible interface.
     */
    public static boolean usesNativeGLES2(String profileImpl) {
        return GLES3.equals(profileImpl) || GLES2.equals(profileImpl);
    }

    /** Indicates whether the native OpenGL ES2 profile is in use. 
     * This requires an EGL, ES3 compatible interface.
     */
    public static boolean usesNativeGLES3(String profileImpl) {
        return GLES3.equals(profileImpl);
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

    private static final String getGLImplBaseClassName(String profileImpl) {
        if( GLES2 == profileImpl || GLES3 == profileImpl ) {
            return "jogamp.opengl.es3.GLES3";
        } else if( GLES1 == profileImpl ) {
            return "jogamp.opengl.es1.GLES1";
        } else if ( GL4bc == profileImpl ||
                    GL4   == profileImpl ||
                    GL3bc == profileImpl ||
                    GL3   == profileImpl ||
                    GL2   == profileImpl ) {
            return "jogamp.opengl.gl4.GL4bc";
        } else {
            throw new GLException("unsupported profile \"" + profileImpl + "\"");
        }
    }
    
    /**
     * @param o GLProfile object to compare with
     * @return true if given Object is a GLProfile and
     *         if both, profile and profileImpl is equal with this.
     */
    public final boolean equals(Object o) {
        if(this==o) { return true; }
        if(o instanceof GLProfile) {
            final GLProfile glp = (GLProfile)o;
            return getName() == glp.getName() && getImplName() == glp.getImplName() ;
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
    
    /** return true if impl. is a hardware rasterizer, otherwise false. */
    public final boolean isHardwareRasterizer() {
        return isHardwareRasterizer;
    }
    
    /** 
     * return this profiles implementation name, eg. GL2ES2 -> GL2, or GL3 -> GL3 
     */
    public final String getImplName() {
        return null != profileImpl ? profileImpl.getName() : getName();
    }

    /** Indicates whether this profile is capable of GL4bc.  <p>Includes [ GL4bc ].</p> */
    public final boolean isGL4bc() {
        return GL4bc == profile;
    }

    /** Indicates whether this profile is capable of GL4.    <p>Includes [ GL4bc, GL4 ].</p> */
    public final boolean isGL4() {
        return isGL4bc() || GL4 == profile;
    }

    /** Indicates whether this profile is capable of GL3bc.  <p>Includes [ GL4bc, GL3bc ].</p> */
    public final boolean isGL3bc() {
        return isGL4bc() || GL3bc == profile;
    }

    /** Indicates whether this profile is capable of GL3.    <p>Includes [ GL4bc, GL4, GL3bc, GL3 ].</p> */
    public final boolean isGL3() {
        return isGL4() || isGL3bc() || GL3 == profile;
    }

    /** Indicates whether this profile is capable of GL2 .   <p>Includes [ GL4bc, GL3bc, GL2 ].</p> */
    public final boolean isGL2() {
        return isGL3bc() || GL2 == profile;
    }

    /** Indicates whether this profile is capable of GLES1.  <p>Includes [ GLES1 ].</p> */
    public final boolean isGLES1() {
        return GLES1 == profile;
    }

    /** Indicates whether this profile is capable of GLES2.  <p>Includes [ GLES3, GLES2 ].</p> */
    public final boolean isGLES2() {
        return GLES3 == profile || GLES2 == profile;
    }
    
    /** Indicates whether this profile is capable of GLES3.  <p>Includes [ GLES3 ].</p> */
    public final boolean isGLES3() {
        return GLES3 == profile;
    }
    
    /** Indicates whether this profile is capable of GLES.  <p>Includes [ GLES3, GLES1, GLES2 ].</p> */
    public final boolean isGLES() {
        return GLES3 == profile || GLES2 == profile || GLES1 == profile;
    }

    /** Indicates whether this profile is capable of GL2ES1. <p>Includes [ GL4bc, GL3bc, GL2, GLES1, GL2ES1 ].</p> */
    public final boolean isGL2ES1() {
        return GL2ES1 == profile || isGLES1() || isGL2();
    }

    /** Indicates whether this profile is capable of GL2GL3. <p>Includes [ GL4bc, GL4, GL3bc, GL3, GL2, GL2GL3 ].</p> */
    public final boolean isGL2GL3() {
        return GL2GL3 == profile || isGL3() || isGL2();
    }
    
    /** Indicates whether this profile is capable of GL2ES2. <p>Includes [ GL4bc, GL4, GL3bc, GL3, GLES3, GL2, GL2GL3, GL2ES2, GLES2 ].</p> */
    public final boolean isGL2ES2() {
        return GL2ES2 == profile || isGLES2() || isGL2GL3();
    }

    /** Indicates whether this profile is capable of GL3ES3. <p>Includes [ GL4bc, GL4, GL3bc, GL3, GLES3 ].</p> */
    public final boolean isGL3ES3() {
        return isGL4ES3() || isGL3();
    }

    /** Indicates whether this profile is capable of GL4ES3. <p>Includes [ GL4bc, GL4, GLES3 ].</p> */
    public final boolean isGL4ES3() {
        return GL4ES3 == profile || isGLES3() || isGL4();
    }

    /** Indicates whether this profile supports GLSL, i.e. {@link #isGL2ES2()}. */
    public final boolean hasGLSL() {
        return isGL2ES2() ;
    }

    /** Indicates whether this profile uses the native OpenGL ES1 implementations. */
    public final boolean usesNativeGLES1() {
        return GLES1 == getImplName();
    }

    /** Indicates whether this profile uses the native OpenGL ES2 implementations. */
    public final boolean usesNativeGLES2() {
        return GLES3 == getImplName() || GLES2 == getImplName();
    }

    /** Indicates whether this profile uses the native OpenGL ES2 implementations. */
    public final boolean usesNativeGLES3() {
        return GLES3 == getImplName();
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
        final String arrayName = getGLArrayName(index);
        if( isGLES1() ) {
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
        } else if( isGLES2() ) {
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
        return "GLProfile[" + getName() + "/" + getImplName() + "."+(this.isHardwareRasterizer?"hw":"sw")+"]";
    }

    private static /*final*/ boolean isAWTAvailable;

    private static /*final*/ boolean hasDesktopGLFactory;    
    private static /*final*/ boolean hasGL234Impl;
    private static /*final*/ boolean hasEGLFactory;
    private static /*final*/ boolean hasGLES3Impl;
    private static /*final*/ boolean hasGLES1Impl;

    private static /*final*/ GLDrawableFactoryImpl eglFactory = null;
    private static /*final*/ GLDrawableFactoryImpl desktopFactory = null;
    private static /*final*/ AbstractGraphicsDevice defaultDevice = null;

    private static volatile boolean initialized = false;
    private static RecursiveThreadGroupLock initLock = LockFactory.createRecursiveThreadGroupLock();

    /**
     * Tries the profiles implementation and native libraries.
     */
    private static void initProfilesForDefaultDevices() {
        NativeWindowFactory.initSingleton();
        if(DEBUG) {
            System.err.println("GLProfile.init - thread: " + Thread.currentThread().getName());
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
        hasGLES3Impl   = ReflectionUtil.isClassAvailable("jogamp.opengl.es3.GLES3Impl", classloader);
        
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
        }

        final AbstractGraphicsDevice defaultDesktopDevice;
        if(null == desktopFactory) {
            hasDesktopGLFactory  = false;
            hasGL234Impl         = false;
            defaultDesktopDevice = null;
            if(DEBUG) {
                System.err.println("Info: GLProfile.init - Desktop GLDrawable factory not available");
            }
        } else {
            defaultDesktopDevice = desktopFactory.getDefaultDevice();
        }

        if ( ReflectionUtil.isClassAvailable("jogamp.opengl.egl.EGLDrawableFactory", classloader) ) {
            t=null;
            try {
                eglFactory = (GLDrawableFactoryImpl) GLDrawableFactory.getFactoryImpl(GLES2);
                if(null != eglFactory) {
                    hasEGLFactory = true;
                    // update hasGLES1Impl, hasGLES3Impl based on EGL
                    hasGLES3Impl = null!=eglFactory.getGLDynamicLookupHelper(2) && hasGLES3Impl;
                    hasGLES1Impl = null!=eglFactory.getGLDynamicLookupHelper(1) && hasGLES1Impl;
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
            }
        }

        final AbstractGraphicsDevice defaultEGLDevice;
        if(null == eglFactory) {
            hasGLES3Impl     = false;
            hasGLES1Impl     = false;
            defaultEGLDevice = null;
            if(DEBUG) {
                System.err.println("Info: GLProfile.init - EGL GLDrawable factory not available");
            }
        } else {
            defaultEGLDevice = eglFactory.getDefaultDevice();            
        }

        if( null != defaultDesktopDevice ) {
            defaultDevice = defaultDesktopDevice;
            if(DEBUG) {
                System.err.println("Info: GLProfile.init - Default device is desktop derived: "+defaultDevice);
            }            
        } else if ( null != defaultEGLDevice ) {
            defaultDevice = defaultEGLDevice;
            if(DEBUG) {
                System.err.println("Info: GLProfile.init - Default device is EGL derived: "+defaultDevice);
            }
        } else {
            if(DEBUG) {
                System.err.println("Info: GLProfile.init - Default device not available");
            }
            defaultDevice = null;
        }
                
        // we require to initialize the EGL device 1st, if available
        final boolean addedEGLProfile     = null != defaultEGLDevice     ? initProfilesForDevice(defaultEGLDevice)     : false;
        final boolean addedDesktopProfile = null != defaultDesktopDevice ? initProfilesForDevice(defaultDesktopDevice) : false;        
        final boolean addedAnyProfile     = addedEGLProfile || addedDesktopProfile ;
    
        if(DEBUG) {
            System.err.println("GLProfile.init addedAnyProfile       "+addedAnyProfile+" (desktop: "+addedDesktopProfile+", egl "+addedEGLProfile+")");
            System.err.println("GLProfile.init isAWTAvailable        "+isAWTAvailable);
            System.err.println("GLProfile.init hasDesktopGLFactory   "+hasDesktopGLFactory);
            System.err.println("GLProfile.init hasGL234Impl          "+hasGL234Impl);
            System.err.println("GLProfile.init hasEGLFactory         "+hasEGLFactory);
            System.err.println("GLProfile.init hasGLES1Impl          "+hasGLES1Impl);
            System.err.println("GLProfile.init hasGLES3Impl          "+hasGLES3Impl);
            System.err.println("GLProfile.init defaultDevice         "+defaultDevice);
            System.err.println("GLProfile.init defaultDevice Desktop "+defaultDesktopDevice);
            System.err.println("GLProfile.init defaultDevice EGL     "+defaultEGLDevice);
            System.err.println("GLProfile.init profile order         "+array2String(GL_PROFILE_LIST_ALL));
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
            System.err.println("Info: GLProfile.initProfilesForDevice: "+device+" ("+device.getClass().getName()+"), isSet "+isSet+", hasDesktopGLFactory "+hasDesktopGLFactory+", hasEGLFactory "+hasEGLFactory);
        }
        if(isSet) {
            // Avoid recursion and check whether impl. is sane!
            final String deviceKey = device.getUniqueID();
            HashMap<String /*GLProfile_name*/, GLProfile> map = deviceConn2ProfileMap.get(deviceKey);
            if( null == map ) {
                throw new InternalError("GLContext Avail. GLVersion is set - but no profile map for device: "+device);
            }
            return null != map.get(GL_DEFAULT);
        }

        boolean addedDesktopProfile = false;
        boolean addedEGLProfile = false;

        final boolean deviceIsDesktopCompatible = hasDesktopGLFactory && desktopFactory.getIsDeviceCompatible(device);        
                
        if( deviceIsDesktopCompatible ) {
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
            if (DEBUG) {
                System.err.println("GLProfile.initProfilesForDevice: "+device+": desktop Shared Ctx "+desktopSharedCtxAvail);
            }
            if(!desktopSharedCtxAvail) {
                hasDesktopGLFactory = false;
            } else if( !GLContext.getAvailableGLVersionsSet(device) ) {
                throw new InternalError("Available GLVersions not set");
            }
            addedDesktopProfile = computeProfileMap(device, false /* desktopCtxUndef*/, false /* esCtxUndef */);
        }
        
        final boolean deviceIsEGLCompatible = hasEGLFactory && eglFactory.getIsDeviceCompatible(device);
        
        // also test GLES1, GLES2 and GLES3 on desktop, since we have implementations / emulations available.
        if( deviceIsEGLCompatible && ( hasGLES3Impl || hasGLES1Impl ) ) {
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
                hasGLES3Impl = false;
                hasGLES1Impl = false;
            }
            if (DEBUG) {
                System.err.println("GLProfile.initProfilesForDevice: "+device+": egl Shared Ctx "+eglSharedCtxAvail);
            }
            addedEGLProfile = computeProfileMap(device, false /* desktopCtxUndef*/, false /* esCtxUndef */);
        }

        if( !addedDesktopProfile && !addedEGLProfile ) {
            setProfileMap(device, new HashMap<String /*GLProfile_name*/, GLProfile>()); // empty
            if(DEBUG) {
                System.err.println("GLProfile: device could not be initialized: "+device);
                System.err.println("GLProfile: compatible w/ desktop: "+deviceIsDesktopCompatible+
                                            ", egl "+deviceIsEGLCompatible);
                System.err.println("GLProfile: desktoplFactory      "+desktopFactory);
                System.err.println("GLProfile: eglFactory           "+eglFactory);
                System.err.println("GLProfile: hasGLES1Impl         "+hasGLES1Impl);
                System.err.println("GLProfile: hasGLES3Impl         "+hasGLES3Impl);
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

    private static String array2String(String[] list) {
        StringBuilder msg = new StringBuilder();
        msg.append("[");
        for (int i = 0; i < list.length; i++) {
            if (i > 0)
                msg.append(", ");
            msg.append(list[i]);
        }
        msg.append("]");
        return msg.toString();
    }

    private static void glAvailabilityToString(AbstractGraphicsDevice device, StringBuilder sb, int major, int profile) {
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
        final boolean isHardwareRasterizer[] = new boolean[1];
        GLProfile defaultGLProfileAny = null;
        GLProfile defaultGLProfileHW = null;
        final HashMap<String, GLProfile> _mappedProfiles = new HashMap<String, GLProfile>(GL_PROFILE_LIST_ALL.length + 1 /* default */);
        for(int i=0; i<GL_PROFILE_LIST_ALL.length; i++) {
            final String profile = GL_PROFILE_LIST_ALL[i];
            final String profileImpl = computeProfileImpl(device, profile, desktopCtxUndef, esCtxUndef, isHardwareRasterizer);
            if( null != profileImpl ) {
                final GLProfile glProfile;
                if( profile.equals( profileImpl ) ) {
                    glProfile = new GLProfile(profile, null, isHardwareRasterizer[0]);
                } else {
                    final GLProfile _mglp = _mappedProfiles.get( profileImpl );
                    if( null == _mglp ) {
                        throw new InternalError("XXX0 profile["+i+"]: "+profile+" -> profileImpl "+profileImpl+" !!! not mapped ");
                    }
                    glProfile = new GLProfile(profile, _mglp, isHardwareRasterizer[0]);
                }
                _mappedProfiles.put(profile, glProfile);
                if (DEBUG) {
                    System.err.println("GLProfile.init map "+glProfile+" on device "+device.getConnection());
                }
                if( null == defaultGLProfileHW && isHardwareRasterizer[0] ) {
                    defaultGLProfileHW=glProfile;
                    if (DEBUG) {
                        System.err.println("GLProfile.init map defaultHW "+glProfile+" on device "+device.getConnection());
                    }
                } else if( null == defaultGLProfileAny ) {
                    defaultGLProfileAny=glProfile;
                    if (DEBUG) {
                        System.err.println("GLProfile.init map defaultAny "+glProfile+" on device "+device.getConnection());
                    }
                }
            } else {
                if (DEBUG) {
                    System.err.println("GLProfile.init map *** no mapping for "+profile+" on device "+device.getConnection());
                }
            }
        }
        if( null != defaultGLProfileHW ) {
            _mappedProfiles.put(GL_DEFAULT, defaultGLProfileHW);
        } else if( null != defaultGLProfileAny ) {
            _mappedProfiles.put(GL_DEFAULT, defaultGLProfileAny);
        }
        setProfileMap(device, _mappedProfiles);
        return _mappedProfiles.size() > 0;
    }

    /**
     * Returns the profile implementation
     */
    private static String computeProfileImpl(AbstractGraphicsDevice device, String profile, boolean desktopCtxUndef, boolean esCtxUndef, boolean isHardwareRasterizer[]) {
        if (GL2ES1.equals(profile)) {
            final boolean es1HardwareRasterizer[] = new boolean[1];
            final boolean gles1Available = hasGLES1Impl && ( esCtxUndef || GLContext.isGLES1Available(device, es1HardwareRasterizer) );
            final boolean gles1HWAvailable = gles1Available && es1HardwareRasterizer[0] ;
            if(hasGL234Impl) {
                if(GLContext.isGL4bcAvailable(device, isHardwareRasterizer)) {
                    if(!gles1HWAvailable || isHardwareRasterizer[0]) {
                        return GL4bc;
                    }
                }
                if(GLContext.isGL3bcAvailable(device, isHardwareRasterizer)) {
                    if(!gles1HWAvailable || isHardwareRasterizer[0]) {
                        return GL3bc;
                    }
                }
                if(desktopCtxUndef || GLContext.isGL2Available(device, isHardwareRasterizer)) {
                    if(!gles1HWAvailable || isHardwareRasterizer[0]) {
                        return GL2;
                    }
                }
            }
            if(gles1Available) {
                isHardwareRasterizer[0] = es1HardwareRasterizer[0];
                return GLES1;
            }
        } else if (GL2ES2.equals(profile)) {
            final boolean es2HardwareRasterizer[] = new boolean[1];
            final boolean gles2Available = hasGLES3Impl && ( esCtxUndef || GLContext.isGLES2Available(device, es2HardwareRasterizer) );
            final boolean gles2HWAvailable = gles2Available && es2HardwareRasterizer[0] ;
            if(hasGL234Impl) {
                if(GLContext.isGL4Available(device, isHardwareRasterizer)) {
                    if(!gles2HWAvailable || isHardwareRasterizer[0]) {
                        return GL4;
                    }
                }
                if(GLContext.isGL4bcAvailable(device, isHardwareRasterizer)) {
                    if(!gles2HWAvailable || isHardwareRasterizer[0]) {
                        return GL4bc;
                    }
                }
                if(GLContext.isGL3Available(device, isHardwareRasterizer)) {
                    if(!gles2HWAvailable || isHardwareRasterizer[0]) {
                        return GL3;
                    }
                }
                if(GLContext.isGL3bcAvailable(device, isHardwareRasterizer)) {
                    if(!gles2HWAvailable || isHardwareRasterizer[0]) {
                        return GL3bc;
                    }
                }
                if(desktopCtxUndef || GLContext.isGL2Available(device, isHardwareRasterizer)) {
                    if(!gles2HWAvailable || isHardwareRasterizer[0]) {
                        return GL2;
                    }
                }
            }
            if(gles2Available) {
                isHardwareRasterizer[0] = es2HardwareRasterizer[0];
                return GLES2;
            }
        } else if (GL4ES3.equals(profile)) {
            final boolean es3HardwareRasterizer[] = new boolean[1];
            final boolean gles3Available = hasGLES3Impl && ( esCtxUndef || GLContext.isGLES3Available(device, es3HardwareRasterizer) );
            final boolean gles3HWAvailable = gles3Available && es3HardwareRasterizer[0] ;
            if(hasGL234Impl) {
                if(GLContext.isGL4Available(device, isHardwareRasterizer)) {
                    if(!gles3HWAvailable || isHardwareRasterizer[0]) {
                        return GL4;
                    }
                }
                if(GLContext.isGL4bcAvailable(device, isHardwareRasterizer)) {
                    if(!gles3HWAvailable || isHardwareRasterizer[0]) {
                        return GL4bc;
                    }
                }
            }
            if(gles3Available) {
                isHardwareRasterizer[0] = es3HardwareRasterizer[0];
                return GLES3;
            }
        } else if(GL2GL3.equals(profile)) {
            if(hasGL234Impl) {
                if( GLContext.isGL4bcAvailable(device, isHardwareRasterizer)) {
                    return GL4bc;
                } else if( GLContext.isGL4Available(device, isHardwareRasterizer)) {
                    return GL4;
                } else if( GLContext.isGL3bcAvailable(device, isHardwareRasterizer)) {
                    return GL3bc;
                } else if( GLContext.isGL3Available(device, isHardwareRasterizer)) {
                    return GL3;
                } else if(desktopCtxUndef || GLContext.isGL2Available(device, isHardwareRasterizer)) {
                    return GL2;
                }
            }
        } else if(GL4bc.equals(profile) && hasGL234Impl && ( desktopCtxUndef || GLContext.isGL4bcAvailable(device, isHardwareRasterizer))) {
            return GL4bc;
        } else if(GL4.equals(profile) && hasGL234Impl && ( desktopCtxUndef || GLContext.isGL4Available(device, isHardwareRasterizer))) {
            return GL4;
        } else if(GL3bc.equals(profile) && hasGL234Impl && ( desktopCtxUndef || GLContext.isGL3bcAvailable(device, isHardwareRasterizer))) {
            return GL3bc;
        } else if(GL3.equals(profile) && hasGL234Impl && ( desktopCtxUndef || GLContext.isGL3Available(device, isHardwareRasterizer))) {
            return GL3;
        } else if(GL2.equals(profile) && hasGL234Impl && ( desktopCtxUndef || GLContext.isGL2Available(device, isHardwareRasterizer))) {
            return GL2;
        } else if(GLES3.equals(profile) && hasGLES3Impl && ( esCtxUndef || GLContext.isGLES3Available(device, isHardwareRasterizer))) {
            return GLES3;
        } else if(GLES2.equals(profile) && hasGLES3Impl && ( esCtxUndef || GLContext.isGLES2Available(device, isHardwareRasterizer))) {
            return GLES2;
        } else if(GLES1.equals(profile) && hasGLES1Impl && ( esCtxUndef || GLContext.isGLES1Available(device, isHardwareRasterizer))) {
            return GLES1;
        }
        return null;
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

        if(null==defaultDevice) { // avoid NPE and notify of incomplete initialization
            throw new GLException("No default device available");
        }
        
        if(null==device) {
            device = defaultDevice;
        }
        
        final String deviceKey = device.getUniqueID();
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

    private GLProfile(String profile, GLProfile profileImpl, boolean isHardwareRasterizer) {
        this.profile = profile;
        this.profileImpl = profileImpl;
        this.isHardwareRasterizer = isHardwareRasterizer;
    }

    private final GLProfile profileImpl;
    private final String profile;
    private final boolean isHardwareRasterizer;
}
