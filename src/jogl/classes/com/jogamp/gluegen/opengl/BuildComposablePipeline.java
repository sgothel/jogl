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
package com.jogamp.gluegen.opengl;

import com.jogamp.gluegen.CodeGenUtils;
import com.jogamp.gluegen.JavaType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BuildComposablePipeline {

    /** <p>Default: true</p>. */
    public static final int GEN_DEBUG = 1 << 0;
    /** <p>Default: true</p>. */
    public static final int GEN_TRACE = 1 << 1;
    /** <p>Default: false</p>. */
    public static final int GEN_CUSTOM = 1 << 2;
    /**
     * By extra command-line argument: <code>prolog_xor_downstream</code>.
     * <p>
     * If true, either prolog (if exist) is called or downstream's method, but not both.
     * By default, both methods would be called.
     * </p>
     * <p>Default: false</p>
     */
    public static final int GEN_PROLOG_XOR_DOWNSTREAM = 1 << 3;
    /**
     * By extra command-line argument: <code>gl_identity_by_assignable_class</code>.
     * <p>
     * If true, implementation does not utilize downstream's <code>isGL*()</code>
     * implementation, but determines whether the GL profile is matched by interface inheritance.
     * </p>
     * <p>Default: false</p>
     */
    public static final int GEN_GL_IDENTITY_BY_ASSIGNABLE_CLASS = 1 << 4;

    private static final HashMap<String, String> addedGLHooks = new HashMap<String, String>();
    private static final String[] addedGLHookMethodNames = new String[] {
            "mapBuffer", "mapBufferRange",
            "mapNamedBuffer", "mapNamedBufferRange" };
    static {
        for(int i=0; i<addedGLHookMethodNames.length; i++) {
            addedGLHooks.put(addedGLHookMethodNames[i], addedGLHookMethodNames[i]);
        }
    }

    int mode;
    private final String outputDir;
    private final String outputPackage;
    private final String outputName;
    private final Class<?> classToComposeAround;
    private final Class<?> classPrologOpt;
    private final Class<?> classDownstream;
    // Only desktop OpenGL has immediate mode glBegin / glEnd
    private boolean hasImmediateMode;
    // Desktop OpenGL and GLES1 have GL_STACK_OVERFLOW and GL_STACK_UNDERFLOW errors
    private boolean hasGL2ES1StackOverflow;

    public static Class<?> getClass(final String name) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(name);
        } catch (final Exception e) {
            throw new RuntimeException(
                    "Could not find class \"" + name + "\"", e);
        }
        return clazz;
    }

    public static Method getMethod(final Class<?> clazz, final Method m) {
        Method res = null;
        try {
            res = clazz.getMethod(m.getName(), m.getParameterTypes());
        } catch (final Exception e) {
        }
        return res;
    }

    public static void main(final String[] args) {
        final String classToComposeAroundName = args[0];
        Class<?> classPrologOpt, classDownstream;
        final Class<?> classToComposeAround = getClass(classToComposeAroundName);

        final String outputDir = args[1];
        String outputPackage, outputName;
        int mode;

        if (args.length > 2) {
            final String outputClazzName = args[2];
            outputPackage = getPackageName(outputClazzName);
            outputName = getBaseClassName(outputClazzName);
            classPrologOpt = getClass(args[3]);
            classDownstream = getClass(args[4]);
            mode = GEN_CUSTOM;
            if (args.length > 5) {
                for(int i=5; i<args.length; i++) {
                    if (args[i].equals("prolog_xor_downstream")) {
                        mode |= GEN_PROLOG_XOR_DOWNSTREAM;
                    } else if (args[i].equals("gl_identity_by_assignable_class")) {
                        mode |= GEN_GL_IDENTITY_BY_ASSIGNABLE_CLASS;
                    }
                }
            }
        } else {
            outputPackage = getPackageName(classToComposeAroundName);
            outputName = null; // TBD ..
            classPrologOpt = null;
            classDownstream = classToComposeAround;
            mode = GEN_DEBUG | GEN_TRACE ;
        }

        final BuildComposablePipeline composer =
                new BuildComposablePipeline(mode, outputDir, outputPackage, outputName, classToComposeAround, classPrologOpt, classDownstream);

        try {
            composer.emit();
        } catch (final IOException e) {
            throw new RuntimeException(
                    "Error generating composable pipeline source files", e);
        }
    }

    protected BuildComposablePipeline(final int mode, final String outputDir, final String outputPackage, final String outputName,
            final Class<?> classToComposeAround, final Class<?> classPrologOpt, final Class<?> classDownstream) {
        this.mode = mode;
        this.outputDir = outputDir;
        this.outputPackage = outputPackage;
        this.outputName = outputName;
        this.classToComposeAround = classToComposeAround;
        this.classPrologOpt = classPrologOpt;
        this.classDownstream = classDownstream;

        if (!classToComposeAround.isInterface()) {
            throw new IllegalArgumentException(
                    classToComposeAround.getName() + " is not an interface class");
        }

        try {
            // Keep assignment w/ null comparison for clarification.
            // If no exception is thrown, return value is always non-null;
            hasImmediateMode =
                    null != classToComposeAround.getMethod("glBegin", new Class<?>[]{Integer.TYPE});
        } catch (final Exception e) {
        }

        try {
            hasGL2ES1StackOverflow = hasImmediateMode &&
                    (classToComposeAround.getField("GL_STACK_OVERFLOW") != null);
        } catch (final Exception e) {
        }
    }

    /**
     * Emit the java source code for the classes that comprise the composable
     * pipeline.
     */
    public void emit() throws IOException {

        final List<Method> publicMethodsRaw = Arrays.asList(classToComposeAround.getMethods());

        final Set<PlainMethod> publicMethodsPlainSet = new HashSet<PlainMethod>();
        for (final Iterator<Method> iter = publicMethodsRaw.iterator(); iter.hasNext();) {
            final Method method = iter.next();
            // Don't hook methods which aren't real GL methods,
            // such as the synthetic "isGL2ES2" "getGL2ES2"
            final String name = method.getName();
            if ( !name.equals("getDownstreamGL") &&
                 !name.equals("toString") ) {
                final boolean syntheticIsGL = name.startsWith("isGL");
                final boolean syntheticGetGL = name.startsWith("getGL");
                final boolean runHooks = name.startsWith("gl") || syntheticIsGL || syntheticGetGL || addedGLHooks.containsKey(name);
                publicMethodsPlainSet.add(new PlainMethod(method, runHooks, syntheticIsGL, syntheticGetGL));
            }
        }

        // sort methods to make them easier to find
        final List<PlainMethod> publicMethodsPlainSorted = new ArrayList<PlainMethod>();
        publicMethodsPlainSorted.addAll(publicMethodsPlainSet);
        Collections.sort(publicMethodsPlainSorted, new Comparator<PlainMethod>() {
                @Override
                public int compare(final PlainMethod o1, final PlainMethod o2) {
                    return o1.getWrappedMethod().getName().compareTo(o2.getWrappedMethod().getName());
                }
            });

        if (0 != (mode & GEN_DEBUG)) {
            (new DebugPipeline(outputDir, outputPackage, classToComposeAround, classDownstream)).emit(publicMethodsPlainSorted.iterator());
        }
        if (0 != (mode & GEN_TRACE)) {
            (new TracePipeline(outputDir, outputPackage, classToComposeAround, classDownstream)).emit(publicMethodsPlainSorted.iterator());
        }
        if (0 != (mode & GEN_CUSTOM)) {
            (new CustomPipeline(mode, outputDir, outputPackage, outputName, classToComposeAround, classPrologOpt, classDownstream)).emit(publicMethodsPlainSorted.iterator());
        }
    }

    public static String getPackageName(final String clazzName) {
        final int lastDot = clazzName.lastIndexOf('.');
        if (lastDot == -1) {
            // no package, class is at root level
            return null;
        }
        return clazzName.substring(0, lastDot);
    }

    public static String getBaseClassName(final String clazzName) {
        final int lastDot = clazzName.lastIndexOf('.');
        if (lastDot == -1) {
            // no package, class is at root level
            return clazzName;
        }
        return clazzName.substring(lastDot + 1);
    }

    //-------------------------------------------------------
    protected static class PlainMethod {

        final Method m;
        final boolean runHooks;
        final boolean isSynthethicIsGL;
        final boolean isSynthethicGetGL;

        PlainMethod(final Method m, final boolean runHooks, final boolean isSynthethicIsGL, final boolean isSynthethicGetGL) {
            this.m = m;
            this.runHooks = runHooks;
            this.isSynthethicIsGL = isSynthethicIsGL;
            this.isSynthethicGetGL = isSynthethicGetGL;
        }

        public Method getWrappedMethod() {
            return m;
        }

        public boolean runHooks() {
            return runHooks;
        }

        public boolean isSynthetic() { return isSynthethicIsGL || isSynthethicGetGL; }
        public boolean isSyntheticIsGL() { return isSynthethicIsGL; }
        public boolean isSyntheticGetGL() { return isSynthethicGetGL; }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof PlainMethod) {
                final PlainMethod b = (PlainMethod) obj;
                final boolean res =
                        m.getName().equals(b.m.getName())
                        && m.getModifiers() == b.m.getModifiers()
                        && m.getReturnType().equals(b.m.getReturnType())
                        && Arrays.equals(m.getParameterTypes(), b.m.getParameterTypes());
                return res;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = m.getName().hashCode() ^ m.getModifiers() ^ m.getReturnType().hashCode();
            final Class<?>[] args = m.getParameterTypes();
            for (int i = 0; i < args.length; i++) {
                hash ^= args[i].hashCode();
            }
            return hash;
        }

        @Override
        public String toString() {
            final Class<?>[] args = m.getParameterTypes();
            final StringBuilder argsString = new StringBuilder();
            argsString.append("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    argsString.append(", ");
                }
                argsString.append(args[i].getName());
            }
            argsString.append(")");
            return m.toString()
                    + "\n\tname: " + m.getName()
                    + "\n\tsynt: isGL " + isSynthethicIsGL+", getGL "+isSynthethicGetGL
                    + "\n\tmods: " + m.getModifiers()
                    + "\n\tretu: " + m.getReturnType()
                    + "\n\targs[" + args.length + "]: " + argsString.toString();
        }
    }

    /**
     * Emits a Java source file that represents one element of the composable
     * pipeline.
     */
    protected abstract class PipelineEmitter {

        private File file;
        protected String basePackage;
        protected String baseName; // does not include package!
        protected String downstreamPackage;
        protected String downstreamName; // does not include package!
        protected String prologPackageOpt = null;
        protected String prologNameOpt = null; // does not include package!
        protected String outputDir;
        protected String outputPackage;
        protected Class<?> baseInterfaceClass;
        protected Class<?> prologClassOpt = null;
        protected Class<?> downstreamClass;

        /**
         * @param outputDir the directory into which the pipeline classes will be
         * generated.
         * @param baseInterfaceClassName the full class name (including package,
         * e.g. "java.lang.String") of the interface that the pipeline wraps
         * @exception IllegalArgumentException if classToComposeAround is not an
         * interface.
         */
        PipelineEmitter(final String outputDir, final String outputPackage, final Class<?> baseInterfaceClass, final Class<?> prologClassOpt, final Class<?> downstreamClass) {
            this.outputDir = outputDir;
            this.outputPackage = outputPackage;
            this.baseInterfaceClass = baseInterfaceClass;
            this.prologClassOpt = prologClassOpt;
            this.downstreamClass = downstreamClass;

            basePackage = getPackageName(baseInterfaceClass.getName());
            baseName = getBaseClassName(baseInterfaceClass.getName());
            downstreamPackage = getPackageName(downstreamClass.getName());
            downstreamName = getBaseClassName(downstreamClass.getName());
            if (null != prologClassOpt) {
                prologPackageOpt = getPackageName(prologClassOpt.getName());
                prologNameOpt = getBaseClassName(prologClassOpt.getName());
            }
        }

        public void emit(final Iterator<PlainMethod> methodsToWrap) throws IOException {
            final String outputClassName = getOutputName();
            this.file = new File(outputDir + File.separatorChar + outputClassName + ".java");
            final String parentDir = file.getParent();
            if (parentDir != null) {
                final File pDirFile = new File(parentDir);
                pDirFile.mkdirs();
            }

            final PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(file)));

            final List<Class<?>> baseInterfaces = Arrays.asList(baseInterfaceClass.getInterfaces());
            final HashSet<Class<?>> clazzList = new HashSet<Class<?>>();
            clazzList.add(baseInterfaceClass);
            clazzList.addAll(baseInterfaces);
            final int ifNamesNumber = clazzList.size();

            // keep original order ..
            clazzList.clear();
            final String[] ifNames = new String[ifNamesNumber];
            {
                int i = 0;

                for (final Iterator<Class<?>> iter = baseInterfaces.iterator(); iter.hasNext();) {
                    final Class<?> ifClass = iter.next();
                    if (!clazzList.contains(ifClass)) {
                        ifNames[i++] = ifClass.getName();
                        clazzList.add(ifClass);
                    }
                }

                if ( !clazzList.contains(baseInterfaceClass) ) {
                    ifNames[i++] = baseInterfaceClass.getName();
                    clazzList.add(baseInterfaceClass);
                }
            }

            clazzList.add(downstreamClass);
            if (null != prologClassOpt) {
                clazzList.add(prologClassOpt);
            }

            final ArrayList<String> imports = new ArrayList<String>();
            imports.add("java.io.*");
            imports.add("com.jogamp.opengl.*");
            imports.add("com.jogamp.gluegen.runtime.*");
            imports.add(Buffer.class.getPackage().getName()+".*");
            for (final Class<?> clasS : clazzList) {
                imports.add(clasS.getName());
            }

            CodeGenUtils.emitJavaHeaders(output,
                    outputPackage,
                    outputClassName,
                    true,
                    imports,
                    new String[]{"public"},
                    ifNames,
                    null,
                    new CodeGenUtils.EmissionCallback() {
                        @Override
                        public void emit(final PrintWriter w) {
                            emitClassDocComment(w);
                        }
                    });

            preMethodEmissionHook(output);

            constructorHook(output);

            emitSyntheticGLMethods(output);

            while (methodsToWrap.hasNext()) {
                final PlainMethod pm = methodsToWrap.next();
                final Method m = pm.getWrappedMethod();
                emitMethodDocComment(output, m);
                emitSignature(output, m);
                emitBody(output, pm);
            }

            postMethodEmissionHook(output);

            output.println();
            output.print("  private " + downstreamName + " " + getDownstreamObjectName() + ";");

            // end the class
            output.println();
            output.print("} // end class ");
            output.println(outputClassName);

            output.flush();
            output.close();

            System.out.println("wrote to file: " + file);
        }

        /** Get the name of the object through which API calls should be routed. */
        protected String getDownstreamObjectName() {
            return "downstream" + downstreamName;
        }

        /** Get the name of the object which shall be called as a prolog. */
        protected String getPrologObjectNameOpt() {
            if (null != prologNameOpt) {
                return "prolog" + prologNameOpt;
            }
            return null;
        }

        protected void emitMethodDocComment(final PrintWriter output, final Method m) {
        }

        protected void emitSignature(final PrintWriter output, final Method m) {
            output.format("  @Override%n  public %s %s(%s)%n",
                          JavaType.createForClass(m.getReturnType()).getName(),
                          m.getName(),
                          getArgListAsString(m, true, true));
        }

        protected void emitBody(final PrintWriter output, final PlainMethod pm) {
            final boolean runHooks = pm.runHooks();
            final Method m = pm.getWrappedMethod();
            output.println("  {");
            final Class<?> retType = m.getReturnType();

            final boolean callPreDownstreamHook = runHooks && hasPreDownstreamCallHook(pm);
            final boolean callPostDownstreamHook = runHooks && hasPostDownstreamCallHook(pm);
            final boolean callDownstream = (null != getMethod(downstreamClass, m))
                    && !(0 != (GEN_PROLOG_XOR_DOWNSTREAM & getMode()) && callPreDownstreamHook);
            final boolean hasResult = (retType != Void.TYPE);

            if (!callDownstream) {
                if (!emptyDownstreamAllowed()) {
                    throw new RuntimeException("Method " + m + " has no downstream (" + downstreamName + ")");
                }
            }

            if (!callPreDownstreamHook && !callPostDownstreamHook && !callDownstream) {
                if (!emptyMethodAllowed()) {
                    throw new RuntimeException("Method " + m + " is empty, no downstream (" + downstreamName + ") nor prolog (" + prologNameOpt + ").");
                } else {
                    output.print("    if(DEBUG) { System.out.println(\"WARNING: No prolog, no downstream, empty: \"+");
                    printFunctionCallString(output, m);
                    output.println("); } ");
                }
            }

            if (callPreDownstreamHook) {
                if (hasResult && !callDownstream) {
                    if (callPostDownstreamHook) {
                        output.print("    " + JavaType.createForClass(retType).getName());
                        output.print(" _res = ");
                    } else {
                        output.print("    return ");
                    }
                }
                preDownstreamCallHook(output, pm);
            }

            if (callDownstream) {
                if( pm.isSyntheticIsGL() ) {
                    emitGLIsMethodBody(output, pm);
                } else if( pm.isSyntheticGetGL() ) {
                    emitGLGetMethodBody(output, pm);
                } else {
                    if (hasResult) {
                        if (callPostDownstreamHook) {
                            output.print("    " + JavaType.createForClass(retType).getName());
                            output.print(" _res = ");
                        } else {
                            output.print("    return ");
                        }
                    }
                    else {
                        output.print("    ");
                    }
                    output.print(getDownstreamObjectName());
                    output.print('.');
                    output.print(m.getName());
                    output.print('(');
                    output.print(getArgListAsString(m, false, true));
                    output.println(");");
                }
            }

            if (callPostDownstreamHook) {
                postDownstreamCallHook(output, pm);
            }

            if (hasResult && callDownstream && callPostDownstreamHook) {
                output.println("    return _res;");
            }

            output.println("  }");
        }

        protected String getArgListAsString(final Method m, final boolean includeArgTypes, final boolean includeArgNames) {
            final StringBuilder buf = new StringBuilder(256);
            if (!includeArgNames && !includeArgTypes) {
                throw new IllegalArgumentException(
                        "Cannot generate arglist without both arg types and arg names");
            }

            final Class<?>[] argTypes = m.getParameterTypes();
            for (int i = 0; i < argTypes.length; ++i) {
                if (includeArgTypes) {
                    buf.append(JavaType.createForClass(argTypes[i]).getName());
                    buf.append(' ');
                }

                if (includeArgNames) {
                    buf.append("arg");
                    buf.append(i);
                }
                if (i < argTypes.length - 1) {
                    buf.append(',');
                }
            }

            return buf.toString();
        }

        /** The name of the class around which this pipeline is being
         * composed. E.g., if this pipeline was constructed with
         * "java.util.Set" as the baseInterfaceClassName, then this method will
         * return "Set".
         */
        protected String getBaseInterfaceName() {
            return baseName;
        }

        /** Get the output name for this pipeline class. */
        protected abstract String getOutputName();

        /**
         * Called after the class headers have been generated, but before any
         * method wrappers have been generated.
         */
        protected void preMethodEmissionHook(final PrintWriter output) {
            output.println("  public static final boolean DEBUG = jogamp.opengl.Debug.debug(\"" + getOutputName() + "\");");
        }

        /**
         * Emits the constructor for the pipeline; called after the preMethodEmissionHook.
         */
        protected abstract void constructorHook(PrintWriter output);

        /**
         * Called after the method wrappers have been generated, but before the
         * closing parenthesis of the class is emitted.
         */
        protected void postMethodEmissionHook(final PrintWriter output) {
            output.println("  @Override");
            output.println("  public String toString() {");
            output.println("    StringBuilder sb = new StringBuilder();");
            output.println("    sb.append(\"" + getOutputName() + " [this 0x\"+Integer.toHexString(hashCode())+\" implementing " + baseInterfaceClass.getName() + ",\\n\\t\");");
            if (null != prologClassOpt) {
                output.println("    sb.append(\" prolog: \"+" + getPrologObjectNameOpt() + ".toString()+\",\\n\\t\");");
            }
            output.println("    sb.append(\" downstream: \"+" + getDownstreamObjectName() + ".toString()+\"\\n\\t]\");");
            output.println("    return sb.toString();");
            output.println("  }");
        }

        /**
         * Called before the pipeline routes the call to the downstream object.
         */
        protected abstract void preDownstreamCallHook(PrintWriter output, PlainMethod pm);

        protected abstract boolean hasPreDownstreamCallHook(PlainMethod pm);

        /**
         * Called after the pipeline has routed the call to the downstream object,
         * but before the calling function exits or returns a value.
         */
        protected abstract void postDownstreamCallHook(PrintWriter output, PlainMethod pm);

        protected abstract boolean hasPostDownstreamCallHook(PlainMethod pm);

        protected abstract int getMode();

        protected abstract boolean emptyMethodAllowed();

        protected abstract boolean emptyDownstreamAllowed();

        /** Emit a Javadoc comment for this pipeline class. */
        protected abstract void emitClassDocComment(PrintWriter output);

        /**
         * Emits one of the isGL* methods.
         */
        protected void emitGLIsMethodBody(final PrintWriter output, final PlainMethod plainMethod) {
            final String methodName = plainMethod.getWrappedMethod().getName();
            final String type = methodName.substring(2);

            if( type.equals("GL") ) {
                output.println("    return true;");
            } else if( 0 != ( GEN_GL_IDENTITY_BY_ASSIGNABLE_CLASS & getMode() ) &&
                       !type.equals("GLES") &&
                       !type.endsWith("core") &&
                       !type.endsWith("Compatible") )
            {
                final Class<?> clazz = BuildComposablePipeline.getClass("com.jogamp.opengl." + type);
                if (clazz.isAssignableFrom(baseInterfaceClass)) {
                    output.println("    return true;");
                } else {
                    output.println("    return false;");
                }
            } else {
                output.println("    return " + getDownstreamObjectName() + ".is" + type + "();");
            }
        }

        /**
         * Emits one of the getGL* methods.
         */
        protected void emitGLGetMethodBody(final PrintWriter output, final PlainMethod plainMethod) {
            final String methodName = plainMethod.getWrappedMethod().getName();
            final String type = methodName.substring(3);

            if( type.equals("GL") ) {
                output.println("    return this;");
            } else if( type.equals("GLProfile") ) {
                output.println("    return " + getDownstreamObjectName() + ".getGLProfile();");
            } else {
                final Class<?> clazz = BuildComposablePipeline.getClass("com.jogamp.opengl." + type);
                if (clazz.isAssignableFrom(baseInterfaceClass)) {
                    output.println("    if( is" + type + "() ) { return this; }");
                    output.println("    throw new GLException(\"Not a " + type + " implementation\");");
                } else {
                    output.println("    throw new GLException(\"Not a " + type + " implementation\");");
                }
            }
        }

        /**
         * Emits all synthetic GL* methods, but not isGL* nor getGL*
         */
        protected void emitSyntheticGLMethods(final PrintWriter output) {
            output.println("  @Override");
            output.println("  public final GL getDownstreamGL() throws GLException {");
            output.println("    return " + getDownstreamObjectName() + ";");
            output.println("  }");
        }
    } // end class PipelineEmitter

    //-------------------------------------------------------
    protected class CustomPipeline extends PipelineEmitter {

        String className;
        int mode;

        CustomPipeline(final int mode, final String outputDir, final String outputPackage, final String outputName, final Class<?> baseInterfaceClass, final Class<?> prologClassOpt, final Class<?> downstreamClass) {
            super(outputDir, outputPackage, baseInterfaceClass, prologClassOpt, downstreamClass);
            className = outputName;
            this.mode = mode;
        }

        @Override
        protected String getOutputName() {
            return className;
        }

        @Override
        protected int getMode() {
            return mode;
        }

        @Override
        protected boolean emptyMethodAllowed() {
            return true;
        }

        @Override
        protected boolean emptyDownstreamAllowed() {
            return true;
        }

        @Override
        protected void preMethodEmissionHook(final PrintWriter output) {
            super.preMethodEmissionHook(output);
        }

        @Override
        protected void constructorHook(final PrintWriter output) {
            output.print("  public " + getOutputName() + "(");
            output.print(downstreamName + " " + getDownstreamObjectName());
            if (null != prologNameOpt) {
                output.println(", " + prologNameOpt + " " + getPrologObjectNameOpt() + ")");
            } else {
                output.println(")");
            }
            output.println("  {");
            output.println("    if (" + getDownstreamObjectName() + " == null) {");
            output.println("      throw new IllegalArgumentException(\"null " + getDownstreamObjectName() + "\");");
            output.println("    }");
            output.print("    this." + getDownstreamObjectName());
            output.println(" = " + getDownstreamObjectName() + ";");
            if (null != prologNameOpt) {
                output.print("    this." + getPrologObjectNameOpt());
                output.println(" = " + getPrologObjectNameOpt() + ";");
            }
            output.println("  }");
            output.println();
        }

        @Override
        protected void postMethodEmissionHook(final PrintWriter output) {
            super.postMethodEmissionHook(output);
            if (null != prologNameOpt) {
                output.print("  private " + prologNameOpt + " " + getPrologObjectNameOpt() + ";");
            }
        }

        @Override
        protected void emitClassDocComment(final PrintWriter output) {
            output.println("/**");
            output.println(" * Composable pipeline {@link " + outputPackage + "." + outputName + "}, implementing the interface");
            output.println(" * {@link " + baseInterfaceClass.getName() + "}");
            output.println(" * <p>");
            output.println(" * Each method follows the call graph <ul>");
            if (null != prologClassOpt) {
                output.println(" *   <li> call <em>prolog</em> {@link " + prologClassOpt.getName() + "} if available");
            }
            output.println(" *   <li> call <em>downstream</em> {@link " + downstreamClass.getName() + "} if available");
            if (null != prologClassOpt && 0 != (GEN_PROLOG_XOR_DOWNSTREAM & getMode())) {
                output.println(" *        <strong>and</strong> if no call to {@link " + prologClassOpt.getName() + "} is made");
            }
            output.println(" * </ul><p>");
            output.println(" * ");
            output.println(" * <ul>");
            output.println(" *   <li> <em>Interface</em> {@link " + baseInterfaceClass.getName() + "}");
            if (null != prologClassOpt) {
                output.println(" *   <li> <em>Prolog</em> {@link " + prologClassOpt.getName() + "}");
            }
            output.println(" *   <li> <em>Downstream</em> {@link " + downstreamClass.getName() + "}");
            output.println(" * </ul><p>");
            output.println(" *  Sample code which installs this pipeline: </P>");
            output.println(" * ");
            output.println("<PRE>");
            if (null != prologNameOpt) {
                output.println("     GL gl = drawable.setGL( new " + className + "( drawable.getGL().getGL2ES2(), new " + prologNameOpt + "( drawable.getGL().getGL2ES2() ) ) );");
            } else {
                output.println("     GL gl = drawable.setGL( new " + className + "( drawable.getGL().getGL2ES2() ) );");
            }
            output.println("</PRE>");
            output.println("*/");
        }

        @Override
        protected boolean hasPreDownstreamCallHook(final PlainMethod pm) {
            return null != getMethod(prologClassOpt, pm.getWrappedMethod());
        }

        @Override
        protected void preDownstreamCallHook(final PrintWriter output, final PlainMethod pm) {
            final Method m = pm.getWrappedMethod();
            if (null != prologNameOpt) {
                output.print(getPrologObjectNameOpt());
                output.print('.');
                output.print(m.getName());
                output.print('(');
                output.print(getArgListAsString(m, false, true));
                output.println(");");
            }
        }

        @Override
        protected boolean hasPostDownstreamCallHook(final PlainMethod pm) {
            return false;
        }

        @Override
        protected void postDownstreamCallHook(final PrintWriter output, final PlainMethod pm) {
        }
    } // end class CustomPipeline

    protected class DebugPipeline extends PipelineEmitter {

        String className;

        DebugPipeline(final String outputDir, final String outputPackage, final Class<?> baseInterfaceClass, final Class<?> downstreamClass) {
            super(outputDir, outputPackage, baseInterfaceClass, null, downstreamClass);
            className = "Debug" + getBaseInterfaceName();
        }

        @Override
        protected String getOutputName() {
            return className;
        }

        @Override
        protected int getMode() {
            return 0;
        }

        @Override
        protected boolean emptyMethodAllowed() {
            return false;
        }

        @Override
        protected boolean emptyDownstreamAllowed() {
            return false;
        }

        @Override
        protected void preMethodEmissionHook(final PrintWriter output) {
            super.preMethodEmissionHook(output);
        }

        @Override
        protected void constructorHook(final PrintWriter output) {
            output.print("  public " + getOutputName() + "(");
            output.println(downstreamName + " " + getDownstreamObjectName() + ")");
            output.println("  {");
            output.println("    if (" + getDownstreamObjectName() + " == null) {");
            output.println("      throw new IllegalArgumentException(\"null " + getDownstreamObjectName() + "\");");
            output.println("    }");
            output.print("    this." + getDownstreamObjectName());
            output.println(" = " + getDownstreamObjectName() + ";");
            if (null != prologNameOpt) {
                output.print("    this." + getPrologObjectNameOpt());
                output.println(" = " + getPrologObjectNameOpt() + ";");
            }
            output.println("    // Fetch GLContext object for better error checking (if possible)");
            output.println("    _context = " + getDownstreamObjectName() + ".getContext();");
            output.println("  }");
            output.println();
        }

        @Override
        protected void postMethodEmissionHook(final PrintWriter output) {
            super.postMethodEmissionHook(output);
            output.println("  private int checkGLError() {");
            if (hasImmediateMode) {
                output.println("    if (insideBeginEndPair) return GL_NO_ERROR;");
                output.println();
            }
            output.format("    return %s.glGetError();%n", getDownstreamObjectName());
            output.println("  }");

            output.println("  private void writeGLError(int err, String fmt, Object... args)");
            output.println("  {");
            output.println("    StringBuilder buf = new StringBuilder();");
            output.println("    buf.append(Thread.currentThread().toString());");
            output.println("    buf.append(\" glGetError() returned the following error codes after a call to \");");
            output.println("    buf.append(String.format(fmt, args));");
            output.println("    buf.append(\": \");");
            output.println();
            output.println("    // Loop repeatedly to allow for distributed GL implementations,");
            output.println("    // as detailed in the glGetError() specification");
            output.println("    int recursionDepth = 10;");
            output.println("    do {");
            output.println("      switch (err) {");
            output.println("        case GL_INVALID_ENUM: buf.append(\"GL_INVALID_ENUM \"); break;");
            output.println("        case GL_INVALID_VALUE: buf.append(\"GL_INVALID_VALUE \"); break;");
            output.println("        case GL_INVALID_OPERATION: buf.append(\"GL_INVALID_OPERATION \"); break;");
            if (hasGL2ES1StackOverflow) {
                output.println("        case GL2ES1.GL_STACK_OVERFLOW: buf.append(\"GL_STACK_OVERFLOW \"); break;");
                output.println("        case GL2ES1.GL_STACK_UNDERFLOW: buf.append(\"GL_STACK_UNDERFLOW \"); break;");
            }
            output.println("        case GL_OUT_OF_MEMORY: buf.append(\"GL_OUT_OF_MEMORY \"); break;");
            output.println("        case GL_NO_ERROR: throw new InternalError(\"Should not be treating GL_NO_ERROR as error\");");
            output.println("        default: buf.append(\"Unknown glGetError() return value: \");");
            output.println("      }");
            output.println("      buf.append(\"( \" + err + \" 0x\"+Integer.toHexString(err).toUpperCase() + \"), \");");
            output.println("    } while ((--recursionDepth >= 0) && (err = "
                    + getDownstreamObjectName()
                    + ".glGetError()) != GL_NO_ERROR);");
            output.println("    throw new GLException(buf.toString());");
            output.println("  }");
            if (hasImmediateMode) {
                output.println("  /** True if the pipeline is inside a glBegin/glEnd pair.*/");
                output.println("  private boolean insideBeginEndPair = false;");
                output.println();
            }
            output.println("  private void checkContext() {");
            output.println("    GLContext currentContext = GLContext.getCurrent();");
            output.println("    if (currentContext == null) {");
            output.println("      throw new GLException(\"No OpenGL context is current on this thread\");");
            output.println("    }");
            output.println("    if ((_context != null) && (_context != currentContext)) {");
            output.println("      throw new GLException(\"This GL object is being incorrectly used with a different GLContext than that which created it\");");
            output.println("    }");
            output.println("  }");
            output.println("  private GLContext _context;");
        }

        @Override
        protected void emitClassDocComment(final PrintWriter output) {
            output.println("/**");
            output.println(" * <p>");
            output.println(" * Composable pipeline which wraps an underlying {@link GL} implementation,");
            output.println(" * providing error checking after each OpenGL method call. If an error occurs,");
            output.println(" * causes a {@link GLException} to be thrown at exactly the point of failure.");
            output.println(" * </p>");
            output.println(" * <p>");
            output.println(" * Sample code which installs this pipeline:");
            output.println(" * <pre>");
            output.println(" *   gl = drawable.setGL(new DebugGL(drawable.getGL()));");
            output.println(" * </pre>");
            output.println(" * For automatic instantiation see {@link GLPipelineFactory#create(String, Class, GL, Object[])}");
            output.println(" * </p>");
            output.println(" */");
        }

        @Override
        protected boolean hasPreDownstreamCallHook(final PlainMethod pm) {
            return !pm.isSynthetic();
        }

        @Override
        protected void preDownstreamCallHook(final PrintWriter output, final PlainMethod pm) {
            output.println("    checkContext();");
        }

        @Override
        protected boolean hasPostDownstreamCallHook(final PlainMethod pm) {
            return !pm.isSynthetic();
        }

        @Override
        protected void postDownstreamCallHook(final PrintWriter output, final PlainMethod pm) {
            final Method m = pm.getWrappedMethod();
            if (m.getName().equals("glBegin")) {
                output.println("    insideBeginEndPair = true;");
                output.println("    // NOTE: can't check glGetError(); it's not allowed inside glBegin/glEnd pair");
            } else {
                if (m.getName().equals("glEnd")) {
                    output.println("    insideBeginEndPair = false;");
                }

                output.println("    final int err = checkGLError();");
                output.println("    if (err != GL_NO_ERROR) {");

                final StringBuilder fmtsb = new StringBuilder();
                final StringBuilder argsb = new StringBuilder();

                fmtsb.append("\"%s(");
                argsb.append("\"").append(m.getName()).append("\"");
                final Class<?>[] params = m.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) {
                        fmtsb.append(", ");
                    }
                    fmtsb.append("<").append(params[i].getName()).append(">");
                    if (params[i].isArray()) {
                        //nothing
                    } else if (params[i].equals(int.class)) {
                        fmtsb.append(" 0x%X");
                        argsb.append(", arg").append(i);
                    } else {
                        fmtsb.append(" %s");
                        argsb.append(", arg").append(i);
                    }
                }
                fmtsb.append(")\",");
                argsb.append(");");

                // calls to glGetError() are only allowed outside of glBegin/glEnd pairs
                output.print("      writeGLError(err, ");
                output.println(fmtsb.toString());
                output.print("                   ");
                output.println(argsb.toString());
                output.println("    }");
            }
        }
    } // end class DebugPipeline

    //-------------------------------------------------------
    protected class TracePipeline extends PipelineEmitter {

        String className;

        TracePipeline(final String outputDir, final String outputPackage, final Class<?> baseInterfaceClass, final Class<?> downstreamClass) {
            super(outputDir, outputPackage, baseInterfaceClass, null, downstreamClass);
            className = "Trace" + getBaseInterfaceName();
        }

        @Override
        protected String getOutputName() {
            return className;
        }

        @Override
        protected int getMode() {
            return 0;
        }

        @Override
        protected boolean emptyMethodAllowed() {
            return false;
        }

        @Override
        protected boolean emptyDownstreamAllowed() {
            return false;
        }

        @Override
        protected void preMethodEmissionHook(final PrintWriter output) {
            super.preMethodEmissionHook(output);
        }

        @Override
        protected void constructorHook(final PrintWriter output) {
            output.print("  public " + getOutputName() + "(");
            output.println(downstreamName + " " + getDownstreamObjectName() + ", PrintStream " + getOutputStreamName() + ")");
            output.println("  {");
            output.println("    if (" + getDownstreamObjectName() + " == null) {");
            output.println("      throw new IllegalArgumentException(\"null " + getDownstreamObjectName() + "\");");
            output.println("    }");
            output.print("    this." + getDownstreamObjectName());
            output.println(" = " + getDownstreamObjectName() + ";");
            output.print("    this." + getOutputStreamName());
            output.println(" = " + getOutputStreamName() + ";");
            output.println("  }");
            output.println();
        }

        @Override
        protected void postMethodEmissionHook(final PrintWriter output) {
            super.postMethodEmissionHook(output);
            output.println("private PrintStream " + getOutputStreamName() + ";");
            output.println("private int indent = 0;");
            output.println("protected String dumpArray(Object obj)");
            output.println("{");
            output.println("  if (obj == null) return \"[null]\";");
            output.println("  StringBuilder sb = new StringBuilder(\"[\");");
            output.println("  int len  = java.lang.reflect.Array.getLength(obj);");
            output.println("  int count = Math.min(len,16);");
            output.println("  for ( int i =0; i < count; i++ ) {");
            output.println("    sb.append(java.lang.reflect.Array.get(obj,i));");
            output.println("    if (i < count-1)");
            output.println("      sb.append(',');");
            output.println("  }");
            output.println("  if ( len > 16 )");
            output.println("    sb.append(\"...\").append(len);");
            output.println("  sb.append(']');");
            output.println("  return sb.toString();");
            output.println("}");
            output.println("protected void print(String str)");
            output.println("{");
            output.println("  " + getOutputStreamName() + ".print(str);");
            output.println("}");
            output.println("protected void println(String str)");
            output.println("{");
            output.println("  " + getOutputStreamName() + ".println(str);");
            output.println("}");
            output.println("protected void printIndent()");
            output.println("{");
            output.println("  for( int i =0; i < indent; i++) {" + getOutputStreamName() + ".print(' ');}");
            output.println("}");
        }

        @Override
        protected void emitClassDocComment(final PrintWriter output) {
            output.println("/**");
            output.println(" * <p>");
            output.println(" * Composable pipeline which wraps an underlying {@link GL} implementation,");
            output.println(" * providing tracing information to a user-specified {@link java.io.PrintStream}");
            output.println(" * before and after each OpenGL method call.");
            output.println(" * </p>");
            output.println(" * <p>");
            output.println(" * Sample code which installs this pipeline:");
            output.println(" * <pre>");
            output.println(" *   gl = drawable.setGL(new TraceGL(drawable.getGL(), System.err));");
            output.println(" * </pre>");
            output.println(" * For automatic instantiation see {@link GLPipelineFactory#create(String, Class, GL, Object[])}");
            output.println(" * </p>");
            output.println(" */");
        }

        @Override
        protected boolean hasPreDownstreamCallHook(final PlainMethod pm) {
            return !pm.isSynthetic();
        }

        @Override
        protected void preDownstreamCallHook(final PrintWriter output, final PlainMethod pm) {
            final Method m = pm.getWrappedMethod();
            if (m.getName().equals("glEnd") || m.getName().equals("glEndList")) {
                output.println("    indent-=2;");
                output.println("    printIndent();");
            } else {
                output.println("    printIndent();");
            }

            output.print("    print(");
            printFunctionCallString(output, m);
            output.println(");");
        }

        @Override
        protected boolean hasPostDownstreamCallHook(final PlainMethod pm) {
            return !pm.isSynthetic();
        }

        @Override
        protected void postDownstreamCallHook(final PrintWriter output, final PlainMethod pm) {
            final Method m = pm.getWrappedMethod();
            final Class<?> ret = m.getReturnType();
            if (ret != Void.TYPE) {
                output.println("    println(\" = \"+_res);");
            } else {
                output.println("    println(\"\");");
            }

            if (m.getName().equals("glBegin"))
                output.println("    indent+=2;");
        }

        private String getOutputStreamName() {
            return "stream";
        }
    } // end class TracePipeline

    public static final void printFunctionCallString(final PrintWriter output, final Method m) {
        final Class<?>[] params = m.getParameterTypes();
        output.print("    \"" + m.getName() + "(\"");
        for (int i = 0; i < params.length; i++) {
            output.print("+\"<" + params[i].getName() + ">");
            if (params[i].isArray()) {
                output.print("\"");
            } else if (params[i].equals(int.class)) {
                output.print(" 0x\"+Integer.toHexString(arg" + i + ").toUpperCase()");
            } else {
                output.print(" \"+arg" + i);
            }
            if (i < params.length - 1) {
                output.print("+\", \"");
            }
        }
        output.print("+\")\"");
    }
}
