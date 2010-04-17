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

package com.jogamp.nativewindow.impl;

import java.lang.reflect.*;
import javax.media.nativewindow.*;

public final class NWReflection {
    
  public static final boolean DEBUG = Debug.debug("NWReflection");

    /**
     * Returns true only if the class could be loaded.
     */
    public static final boolean isClassAvailable(String clazzName) {
        try {
            return null != Class.forName(clazzName, false, NWReflection.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Loads and returns the class or null.
     * @see Class#forName(java.lang.String, boolean, java.lang.ClassLoader)
     */
    public static final Class getClass(String clazzName, boolean initialize) {
        try {
            return getClassImpl(clazzName, initialize);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Class getClassImpl(String clazzName, boolean initialize) throws ClassNotFoundException {
        return Class.forName(clazzName, initialize, NWReflection.class.getClassLoader());
    }

    /**
     * @throws NativeWindowException if the constructor can not be delivered.
     */
    public static final Constructor getConstructor(String clazzName, Class[] cstrArgTypes) {
        try {
            return getConstructor(getClassImpl(clazzName, true), cstrArgTypes);
        } catch (ClassNotFoundException ex) {
            throw new NativeWindowException(clazzName + " not available", ex);
        }
    }

    /**
     * @throws NativeWindowException if the constructor can not be delivered.
     */
    public static final Constructor getConstructor(Class clazz, Class[] cstrArgTypes) {
        try {
            return clazz.getDeclaredConstructor(cstrArgTypes);
        } catch (NoSuchMethodException ex) {
            String args = "";
            for (int i = 0; i < cstrArgTypes.length; i++) {
                args += cstrArgTypes[i].getName();
                if(i != cstrArgTypes.length-1) {
                     args+= ", ";
                }
            }
            throw new NativeWindowException("Constructor: '" + clazz + "(" + args + ")' not found", ex);
        }
    }

  public static final Constructor getConstructor(String clazzName) {
    return getConstructor(clazzName, new Class[0]);
  }

    /**
     * @throws NativeWindowException if the instance can not be created.
     */
    public static final Object createInstance(Class clazz, Class[] cstrArgTypes, Object[] cstrArgs) {
        try {
            return getConstructor(clazz, cstrArgTypes).newInstance(cstrArgs);
        } catch (InstantiationException ex) {
            throw new NativeWindowException("can not create instance of class "+clazz, ex);
        } catch (InvocationTargetException ex) {
            throw new NativeWindowException("can not create instance of class "+clazz, ex);
        } catch (IllegalAccessException ex) {
            throw new NativeWindowException("can not create instance of class "+clazz, ex);
        }
    }

  public static final Object createInstance(Class clazz, Object[] cstrArgs) {
    Class[] cstrArgTypes = new Class[cstrArgs.length];
    for(int i=0; i<cstrArgs.length; i++) {
        cstrArgTypes[i] = cstrArgs[i].getClass();
    }
    return createInstance(clazz, cstrArgTypes, cstrArgs);
  }

    public static final Object createInstance(String clazzName, Class[] cstrArgTypes, Object[] cstrArgs) {
        try {
            return createInstance(getClassImpl(clazzName, true), cstrArgTypes, cstrArgs);
        } catch (ClassNotFoundException ex) {
            throw new NativeWindowException(clazzName + " not available", ex);
        }
    }

  public static final Object createInstance(String clazzName, Object[] cstrArgs) {
    Class[] cstrArgTypes = new Class[cstrArgs.length];
    for(int i=0; i<cstrArgs.length; i++) {
        cstrArgTypes[i] = cstrArgs[i].getClass();
    }
    return createInstance(clazzName, cstrArgTypes, cstrArgs);
  }

  public static final Object createInstance(String clazzName) {
    return createInstance(clazzName, new Class[0], null);
  }

  public static final boolean instanceOf(Object obj, String clazzName) {
    return instanceOf(obj.getClass(), clazzName);
  }
  public static final boolean instanceOf(Class clazz, String clazzName) {
    do {
        if(clazz.getName().equals(clazzName)) {
            return true;
        }
        clazz = clazz.getSuperclass();
    } while (clazz!=null);
    return false;
  }

  public static final boolean implementationOf(Object obj, String faceName) {
    return implementationOf(obj.getClass(), faceName);
  }
  public static final boolean implementationOf(Class clazz, String faceName) {
    do {
        Class[] clazzes = clazz.getInterfaces();
        for(int i=clazzes.length-1; i>=0; i--) {
            Class face = clazzes[i];
            if(face.getName().equals(faceName)) {
                return true;
            }
        }
        clazz = clazz.getSuperclass();
    } while (clazz!=null);
    return false;
  }

  public static boolean isAWTComponent(Object target) {
      return instanceOf(target, "java.awt.Component");
  }

  public static boolean isAWTComponent(Class clazz) {
      return instanceOf(clazz, "java.awt.Component");
  }

}

