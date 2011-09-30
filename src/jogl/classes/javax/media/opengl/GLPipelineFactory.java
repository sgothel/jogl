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
import java.util.StringTokenizer;

import com.jogamp.common.util.ReflectionUtil;

import jogamp.opengl.*;

/**
 * Factory for pipelining GL instances
 */
public class GLPipelineFactory {
    public static final boolean DEBUG = Debug.debug("GLPipelineFactory");

    /** 
     * Creates a pipelined GL instance using the given downstream <code>downstream</code>
     * and optional arguments <code>additionalArgs</code> for the constructor.<br>
     *
     * The upstream GL instance is determined as follows:
     * <ul>
     *   <li> Use <code>pipelineClazzBaseName</code> as the class name's full basename, incl. package name</li>
     *   <li> For the <code>downstream</code> class and it's superclasses, do:</li>
     *   <ul>
     *      <li> For all <code>downstream</code> class and superclass interfaces, do:</li>
     *      <ul>
     *        <li> If <code>reqInterface</code> is not null and the interface is unequal, continue loop.</li>
     *        <li> If <code>downstream</code> is not instance of interface, continue loop.</li> 
     *        <li> If upstream class is available use it, end loop.</li>
     *      </ul>
     *   </ul>
     * </ul><br>
     *
     * @param pipelineClazzBaseName the basename of the pipline class name
     * @param reqInterface optional requested interface to be used, may be null, in which case the first matching one is used
     * @param downstream is always the 1st argument for the upstream constructor
     * @param additionalArgs additional arguments for the upstream constructor
     */
    public static final GL create(String pipelineClazzBaseName, Class<?> reqInterface, GL downstream, Object[] additionalArgs) {
        Class<?> downstreamClazz = downstream.getClass();
        Class<?> upstreamClazz = null;
        Class<?> interfaceClazz = null;

        if(DEBUG) {
            System.out.println("GLPipelineFactory: Start "+downstreamClazz.getName()+", req. Interface: "+reqInterface+" -> "+pipelineClazzBaseName);
        }

        // For all classes: child -> parent
        do {
            // For all interfaces: right -> left == child -> parent
            //   It is important that this matches with the gluegen cfg file's 'Implements' clause !
            Class<?>[] clazzes = downstreamClazz.getInterfaces();
            for(int i=clazzes.length-1; null==upstreamClazz && i>=0; i--) {
                if(DEBUG) {
                    System.out.println("GLPipelineFactory: Try "+downstreamClazz.getName()+" Interface["+i+"]: "+clazzes[i].getName());
                }
                if( reqInterface != null && !reqInterface.getName().equals(clazzes[i].getName()) ) {
                    if(DEBUG) {
                        System.out.println("GLPipelineFactory: requested Interface "+reqInterface+" is _not_ "+ clazzes[i].getName());
                    }
                    continue; // not the requested one ..
                }
                if( ! clazzes[i].isInstance(downstream) ) {
                    if(DEBUG) {
                        System.out.println("GLPipelineFactory: "+downstream.getClass().getName() + " is _not_ instance of "+ clazzes[i].getName());
                    }
                    continue; // not a compatible one 
                } else {
                    if(DEBUG) {
                        System.out.println("GLPipelineFactory: "+downstream.getClass().getName() + " _is_ instance of "+ clazzes[i].getName());
                    }
                }
                upstreamClazz = getUpstreamClazz(clazzes[i], pipelineClazzBaseName);
                if( null != upstreamClazz ) {
                    interfaceClazz = clazzes[i];
                }
            }

            if(null==upstreamClazz) {
                downstreamClazz = downstreamClazz.getSuperclass();
            }
        } while (null!=downstreamClazz && null==upstreamClazz);


        if(null==upstreamClazz) {
            throw new GLException("No pipeline ("+pipelineClazzBaseName+"*) available for :"+downstream.getClass().getName());
        }

        if(DEBUG) {
            System.out.println("GLPipelineFactory: Got : "+ upstreamClazz.getName()+", base interface: "+interfaceClazz.getName());
        }

        Class<?>[] cstrArgTypes = new Class<?>[ 1 + ( ( null==additionalArgs ) ? 0 : additionalArgs.length ) ] ;
        {
            int i = 0;
            cstrArgTypes[i++] = interfaceClazz;
            for(int j=0; null!=additionalArgs && j<additionalArgs.length; j++) {
                cstrArgTypes[i++] = additionalArgs[j].getClass();
            }
        }
        // throws exception if cstr not found!
        Constructor<?> cstr = ReflectionUtil.getConstructor(upstreamClazz, cstrArgTypes);
        Object instance = null;
        try { 
            Object[] cstrArgs = new Object[ 1 + ( ( null==additionalArgs ) ? 0 : additionalArgs.length ) ] ;
            {
                int i = 0;
                cstrArgs[i++] = downstream;
                for(int j=0; null!=additionalArgs && j<additionalArgs.length; j++) {
                    cstrArgs[i++] = additionalArgs[j];
                }
            }
            instance = cstr.newInstance( cstrArgs ) ;
        } catch (Throwable t) { t.printStackTrace(); }
        if(null==instance) {
            throw new GLException("Error: Couldn't create instance of pipeline: "+upstreamClazz.getName()+
                                  " ( "+getArgsClassNameList(downstreamClazz, additionalArgs) +" )");
        }
        if( ! (instance instanceof GL) ) {
            throw new GLException("Error: "+upstreamClazz.getName()+" not an instance of GL");
        }
        return (GL) instance;
    }

    private static final String getArgsClassNameList(Class<?> arg0, Object[] args) {
        StringBuffer sb = new StringBuffer();
        sb.append(arg0.getName());
        if(args!=null) {
            for(int j=0; j<args.length; j++) {
                sb.append(", ");
                sb.append(args[j].getClass().getName());
            }
        }
        return sb.toString();
    }

    private static final Class<?> getUpstreamClazz(Class<?> downstreamClazz, String pipelineClazzBaseName) {
        String downstreamClazzName = downstreamClazz.getName();

        StringTokenizer st = new StringTokenizer(downstreamClazzName, ".");
        String downstreamClazzBaseName = downstreamClazzName;
        while(st.hasMoreTokens()) {
            downstreamClazzBaseName = st.nextToken();
        }
        String upstreamClazzName = pipelineClazzBaseName+downstreamClazzBaseName;

        Class<?> upstreamClazz = null;
        try {
            upstreamClazz = Class.forName(upstreamClazzName, true, GLPipelineFactory.class.getClassLoader());
        } catch (Throwable e) { e.printStackTrace(); }

        return upstreamClazz;
    }
}

