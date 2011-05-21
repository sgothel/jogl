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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BuildComposablePipeline {

    public static final int GEN_DEBUG = 1 << 0; // default
    public static final int GEN_TRACE = 1 << 1; // default
    public static final int GEN_CUSTOM = 1 << 2;
    public static final int GEN_PROLOG_XOR_DOWNSTREAM = 1 << 3;
    int mode;
    private String outputDir;
    private String outputPackage;
    private String outputName;
    private Class<?> classToComposeAround;
    private Class<?> classPrologOpt;
    private Class<?> classDownstream;
    // Only desktop OpenGL has immediate mode glBegin / glEnd
    private boolean hasImmediateMode;
    // Desktop OpenGL and GLES1 have GL_STACK_OVERFLOW and GL_STACK_UNDERFLOW errors
    private boolean hasStackOverflow;

    public static Class<?> getClass(String name) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(name);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not find class \"" + name + "\"", e);
        }
        return clazz;
    }

    public static Method getMethod(Class<?> clazz, Method m) {
        Method res = null;
        try {
            res = clazz.getMethod(m.getName(), m.getParameterTypes());
        } catch (Exception e) {
        }
        return res;
    }

    public static void main(String[] args) {
        String classToComposeAroundName = args[0];
        Class<?> classPrologOpt, classDownstream;
        Class<?> classToComposeAround = getClass(classToComposeAroundName);

        String outputDir = args[1];
        String outputPackage, outputName;
        int mode;

        if (args.length > 2) {
            String outputClazzName = args[2];
            outputPackage = getPackageName(outputClazzName);
            outputName = getBaseClassName(outputClazzName);
            classPrologOpt = getClass(args[3]);
            classDownstream = getClass(args[4]);
            mode = GEN_CUSTOM;
            if (args.length > 5) {
                if (args[5].equals("prolog_xor_downstream")) {
                    mode |= GEN_PROLOG_XOR_DOWNSTREAM;
                }
            }
        } else {
            outputPackage = getPackageName(classToComposeAroundName);
            outputName = null; // TBD ..
            classPrologOpt = null;
            classDownstream = classToComposeAround;
            mode = GEN_DEBUG | GEN_TRACE;
        }

        BuildComposablePipeline composer =
                new BuildComposablePipeline(mode, outputDir, outputPackage, outputName, classToComposeAround, classPrologOpt, classDownstream);

        try {
            composer.emit();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error generating composable pipeline source files", e);
        }
    }

    protected BuildComposablePipeline(int mode, String outputDir, String outputPackage, String outputName,
            Class<?> classToComposeAround, Class<?> classPrologOpt, Class<?> classDownstream) {
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
            hasImmediateMode =
                    (classToComposeAround.getMethod("glBegin", new Class<?>[]{Integer.TYPE}) != null);
        } catch (Exception e) {
        }

        try {
            hasStackOverflow =
                    (classToComposeAround.getField("GL_STACK_OVERFLOW") != null);
        } catch (Exception e) {
        }
    }

    /**
     * Emit the java source code for the classes that comprise the composable
     * pipeline.
     */
    public void emit() throws IOException {

        List<Method> publicMethodsRaw = Arrays.asList(classToComposeAround.getMethods());

        Set<PlainMethod> publicMethodsPlain = new HashSet<PlainMethod>();
        for (Iterator<Method> iter = publicMethodsRaw.iterator(); iter.hasNext();) {
            Method method = iter.next();
            // Don't hook methods which aren't real GL methods,
            // such as the synthetic "isGL2ES2" "getGL2ES2"
            String name = method.getName();
            boolean runHooks = name.startsWith("gl");
            if (!name.startsWith("getGL") && !name.startsWith("isGL") && !name.equals("toString")) {
                publicMethodsPlain.add(new PlainMethod(method, runHooks));
            }
        }

        if (0 != (mode & GEN_DEBUG)) {
            (new DebugPipeline(outputDir, outputPackage, classToComposeAround, classDownstream)).emit(publicMethodsPlain.iterator());
        }
        if (0 != (mode & GEN_TRACE)) {
            (new TracePipeline(outputDir, outputPackage, classToComposeAround, classDownstream)).emit(publicMethodsPlain.iterator());
        }
        if (0 != (mode & GEN_CUSTOM)) {
            (new CustomPipeline(mode, outputDir, outputPackage, outputName, classToComposeAround, classPrologOpt, classDownstream)).emit(publicMethodsPlain.iterator());
        }
    }

    public static String getPackageName(String clazzName) {
        int lastDot = clazzName.lastIndexOf('.');
        if (lastDot == -1) {
            // no package, class is at root level
            return null;
        }
        return clazzName.substring(0, lastDot);
    }

    public static String getBaseClassName(String clazzName) {
        int lastDot = clazzName.lastIndexOf('.');
        if (lastDot == -1) {
            // no package, class is at root level
            return clazzName;
        }
        return clazzName.substring(lastDot + 1);
    }

    //-------------------------------------------------------
    protected class PlainMethod {

        Method m;
        boolean runHooks;

        PlainMethod(Method m, boolean runHooks) {
            this.m = m;
            this.runHooks = runHooks;
        }

        public Method getWrappedMethod() {
            return m;
        }

        public boolean runHooks() {
            return runHooks;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PlainMethod) {
                PlainMethod b = (PlainMethod) obj;
                boolean res =
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
            Class<?>[] args = m.getParameterTypes();
            for (int i = 0; i < args.length; i++) {
                hash ^= args[i].hashCode();
            }
            return hash;
        }

        @Override
        public String toString() {
            Class<?>[] args = m.getParameterTypes();
            StringBuilder argsString = new StringBuilder();
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
        PipelineEmitter(String outputDir, String outputPackage, Class<?> baseInterfaceClass, Class<?> prologClassOpt, Class<?> downstreamClass) {
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

        public void emit(Iterator<PlainMethod> methodsToWrap) throws IOException {
            String outputClassName = getOutputName();
            this.file = new File(outputDir + File.separatorChar + outputClassName + ".java");
            String parentDir = file.getParent();
            if (parentDir != null) {
                File pDirFile = new File(parentDir);
                pDirFile.mkdirs();
            }

            PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(file)));

            List<Class<?>> baseInterfaces = Arrays.asList(baseInterfaceClass.getInterfaces());
            HashSet<Class<?>> clazzList = new HashSet<Class<?>>();
            clazzList.add(baseInterfaceClass);
            clazzList.addAll(baseInterfaces);
            int ifNamesNumber = clazzList.size();

            // keep original order ..
            clazzList.clear();
            String[] ifNames = new String[ifNamesNumber];
            {
                int i = 0;

                for (Iterator<Class<?>> iter = baseInterfaces.iterator(); iter.hasNext();) {
                    Class<?> ifClass = iter.next();
                    if (!clazzList.contains(ifClass)) {
                        ifNames[i++] = ifClass.getName();
                        clazzList.add(ifClass);
                    }
                }

                if (null != baseInterfaceClass && !clazzList.contains(baseInterfaceClass)) {
                    ifNames[i++] = baseInterfaceClass.getName();
                    clazzList.add(baseInterfaceClass);
                }
            }

            clazzList.add(downstreamClass);
            if (null != prologClassOpt) {
                clazzList.add(prologClassOpt);
            }

            ArrayList<String> imports = new ArrayList<String>();
            imports.add("java.io.*");
            imports.add("javax.media.opengl.*");
            imports.add("com.jogamp.gluegen.runtime.*");
            imports.add(Buffer.class.getPackage().getName()+".*");
            for (Class<?> clasS : clazzList) {
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
                        public void emit(PrintWriter w) {
                            emitClassDocComment(w);
                        }
                    });

            preMethodEmissionHook(output);

            constructorHook(output);

            emitGLIsMethods(output);
            emitGLGetMethods(output);

            while (methodsToWrap.hasNext()) {
                PlainMethod pm = methodsToWrap.next();
                Method m = pm.getWrappedMethod();
                emitMethodDocComment(output, m);
                emitSignature(output, m);
                emitBody(output, m, pm.runHooks());
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

            System.out.println("wrote to file: " + file); // JAU
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

        protected void emitMethodDocComment(PrintWriter output, Method m) {
        }

        protected void emitSignature(PrintWriter output, Method m) {
            output.print("  public ");
            output.print(' ');
            output.print(JavaType.createForClass(m.getReturnType()).getName());
            output.print(' ');
            output.print(m.getName());
            output.print('(');
            output.print(getArgListAsString(m, true, true));
            output.println(")");
        }

        protected void emitBody(PrintWriter output, Method m, boolean runHooks) {
            output.println("  {");
            output.print("    ");
            Class<?> retType = m.getReturnType();

            boolean callPreDownstreamHook = runHooks && hasPreDownstreamCallHook(m);
            boolean callPostDownstreamHook = runHooks && hasPostDownstreamCallHook(m);
            boolean callDownstream = (null != getMethod(downstreamClass, m))
                    && !(0 != (GEN_PROLOG_XOR_DOWNSTREAM & getMode()) && callPreDownstreamHook);
            boolean hasResult = (retType != Void.TYPE);

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
                preDownstreamCallHook(output, m);
            }

            if (callDownstream) {
                if (hasResult) {
                    if (callPostDownstreamHook) {
                        output.print("    " + JavaType.createForClass(retType).getName());
                        output.print(" _res = ");
                    } else {
                        output.print("    return ");
                    }
                }
                output.print(getDownstreamObjectName());
                output.print('.');
                output.print(m.getName());
                output.print('(');
                output.print(getArgListAsString(m, false, true));
                output.println(");");
            }

            if (callPostDownstreamHook) {
                postDownstreamCallHook(output, m);
            }

            if (hasResult && callDownstream && callPostDownstreamHook) {
                output.println("    return _res;");
            }
            output.println("  }");

        }

        protected String getArgListAsString(Method m, boolean includeArgTypes, boolean includeArgNames) {
            StringBuilder buf = new StringBuilder(256);
            if (!includeArgNames && !includeArgTypes) {
                throw new IllegalArgumentException(
                        "Cannot generate arglist without both arg types and arg names");
            }

            Class<?>[] argTypes = m.getParameterTypes();
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
        protected void preMethodEmissionHook(PrintWriter output) {
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
        protected void postMethodEmissionHook(PrintWriter output) {
            output.println("  public String toString() {");
            output.println("    StringBuffer sb = new StringBuffer();");
            output.println("    sb.append(\"" + getOutputName() + " [ implementing " + baseInterfaceClass.getName() + ",\\n\\t\");");
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
        protected abstract void preDownstreamCallHook(PrintWriter output, Method m);

        protected abstract boolean hasPreDownstreamCallHook(Method m);

        /**
         * Called after the pipeline has routed the call to the downstream object,
         * but before the calling function exits or returns a value.
         */
        protected abstract void postDownstreamCallHook(PrintWriter output, Method m);

        protected abstract boolean hasPostDownstreamCallHook(Method m);

        protected abstract int getMode();

        protected abstract boolean emptyMethodAllowed();

        protected abstract boolean emptyDownstreamAllowed();

        /** Emit a Javadoc comment for this pipeline class. */
        protected abstract void emitClassDocComment(PrintWriter output);

        /**
         * Emits one of the isGL* methods.
         */
        protected void emitGLIsMethod(PrintWriter output, String type) {
            output.println("  public boolean is" + type + "() {");
            Class<?> clazz = BuildComposablePipeline.getClass("javax.media.opengl." + type);
            if (clazz.isAssignableFrom(baseInterfaceClass)) {
                output.println("    return true;");
            } else {
                output.println("    return false;");
            }
            output.println("  }");
        }

        /**
         * Emits all of the isGL* methods.
         */
        protected void emitGLIsMethods(PrintWriter output) {
            emitGLIsMethod(output, "GL");
            emitGLIsMethod(output, "GL4bc");
            emitGLIsMethod(output, "GL4");
            emitGLIsMethod(output, "GL3bc");
            emitGLIsMethod(output, "GL3");
            emitGLIsMethod(output, "GL2");
            emitGLIsMethod(output, "GLES1");
            emitGLIsMethod(output, "GLES2");
            emitGLIsMethod(output, "GL2ES1");
            emitGLIsMethod(output, "GL2ES2");
            emitGLIsMethod(output, "GL2GL3");
            output.println("  public boolean isGLES() {");
            output.println("    return isGLES2() || isGLES1();");
            output.println("  }");
        }

        /**
         * Emits one of the getGL* methods.
         */
        protected void emitGLGetMethod(PrintWriter output, String type) {
            output.println("  public javax.media.opengl." + type + " get" + type + "() {");
            Class<?> clazz = BuildComposablePipeline.getClass("javax.media.opengl." + type);
            if (clazz.isAssignableFrom(baseInterfaceClass)) {
                output.println("    return this;");
            } else {
                output.println("    throw new GLException(\"Not a " + type + " implementation\");");
            }
            output.println("  }");
        }

        /**
         * Emits all of the getGL* methods.
         */
        protected void emitGLGetMethods(PrintWriter output) {
            emitGLGetMethod(output, "GL");
            emitGLGetMethod(output, "GL4bc");
            emitGLGetMethod(output, "GL4");
            emitGLGetMethod(output, "GL3bc");
            emitGLGetMethod(output, "GL3");
            emitGLGetMethod(output, "GL2");
            emitGLGetMethod(output, "GLES1");
            emitGLGetMethod(output, "GLES2");
            emitGLGetMethod(output, "GL2ES1");
            emitGLGetMethod(output, "GL2ES2");
            emitGLGetMethod(output, "GL2GL3");
            output.println("  public GLProfile getGLProfile() {");
            output.println("    return " + getDownstreamObjectName() + ".getGLProfile();");
            output.println("  }");
        }
    } // end class PipelineEmitter

    //-------------------------------------------------------
    protected class CustomPipeline extends PipelineEmitter {

        String className;
        int mode;

        CustomPipeline(int mode, String outputDir, String outputPackage, String outputName, Class<?> baseInterfaceClass, Class<?> prologClassOpt, Class<?> downstreamClass) {
            super(outputDir, outputPackage, baseInterfaceClass, prologClassOpt, downstreamClass);
            className = outputName;
            this.mode = mode;
        }

        protected String getOutputName() {
            return className;
        }

        protected int getMode() {
            return mode;
        }

        protected boolean emptyMethodAllowed() {
            return true;
        }

        protected boolean emptyDownstreamAllowed() {
            return true;
        }

        @Override
        protected void preMethodEmissionHook(PrintWriter output) {
            super.preMethodEmissionHook(output);
        }

        protected void constructorHook(PrintWriter output) {
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
        protected void postMethodEmissionHook(PrintWriter output) {
            super.postMethodEmissionHook(output);
            if (null != prologNameOpt) {
                output.print("  private " + prologNameOpt + " " + getPrologObjectNameOpt() + ";");
            }
        }

        protected void emitClassDocComment(PrintWriter output) {
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

        protected boolean hasPreDownstreamCallHook(Method m) {
            return null != getMethod(prologClassOpt, m);
        }

        protected void preDownstreamCallHook(PrintWriter output, Method m) {
            if (null != prologNameOpt) {
                output.print(getPrologObjectNameOpt());
                output.print('.');
                output.print(m.getName());
                output.print('(');
                output.print(getArgListAsString(m, false, true));
                output.println(");");
            }
        }

        protected boolean hasPostDownstreamCallHook(Method m) {
            return false;
        }

        protected void postDownstreamCallHook(PrintWriter output, Method m) {
        }
    } // end class CustomPipeline

    protected class DebugPipeline extends PipelineEmitter {

        String className;

        DebugPipeline(String outputDir, String outputPackage, Class<?> baseInterfaceClass, Class<?> downstreamClass) {
            super(outputDir, outputPackage, baseInterfaceClass, null, downstreamClass);
            className = "Debug" + getBaseInterfaceName();
        }

        protected String getOutputName() {
            return className;
        }

        protected int getMode() {
            return 0;
        }

        protected boolean emptyMethodAllowed() {
            return false;
        }

        protected boolean emptyDownstreamAllowed() {
            return false;
        }

        @Override
        protected void preMethodEmissionHook(PrintWriter output) {
            super.preMethodEmissionHook(output);
        }

        protected void constructorHook(PrintWriter output) {
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
        protected void postMethodEmissionHook(PrintWriter output) {
            super.postMethodEmissionHook(output);
            output.println("  private void checkGLGetError(String caller)");
            output.println("  {");
            if (hasImmediateMode) {
                output.println("    if (insideBeginEndPair) {");
                output.println("      return;");
                output.println("    }");
                output.println();
            }
            output.println("    // Debug code to make sure the pipeline is working; leave commented out unless testing this class");
            output.println("    //System.err.println(\"Checking for GL errors "
                    + "after call to \" + caller);");
            output.println();
            output.println("    int err = "
                    + getDownstreamObjectName()
                    + ".glGetError();");
            output.println("    if (err == GL_NO_ERROR) { return; }");
            output.println();
            output.println("    StringBuffer buf = new StringBuffer(Thread.currentThread()+");
            output.println("      \" glGetError() returned the following error codes after a call to \" + caller + \": \");");
            output.println();
            output.println("    // Loop repeatedly to allow for distributed GL implementations,");
            output.println("    // as detailed in the glGetError() specification");
            output.println("    int recursionDepth = 10;");
            output.println("    do {");
            output.println("      switch (err) {");
            output.println("        case GL_INVALID_ENUM: buf.append(\"GL_INVALID_ENUM \"); break;");
            output.println("        case GL_INVALID_VALUE: buf.append(\"GL_INVALID_VALUE \"); break;");
            output.println("        case GL_INVALID_OPERATION: buf.append(\"GL_INVALID_OPERATION \"); break;");
            if (hasStackOverflow) {
                output.println("        case GL_STACK_OVERFLOW: buf.append(\"GL_STACK_OVERFLOW \"); break;");
                output.println("        case GL_STACK_UNDERFLOW: buf.append(\"GL_STACK_UNDERFLOW \"); break;");
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

        protected void emitClassDocComment(PrintWriter output) {
            output.println("/** <P> Composable pipeline which wraps an underlying {@link GL} implementation,");
            output.println("    providing error checking after each OpenGL method call. If an error occurs,");
            output.println("    causes a {@link GLException} to be thrown at exactly the point of failure.");
            output.println("    Sample code which installs this pipeline: </P>");
            output.println();
            output.println("<PRE>");
            output.println("     GL gl = drawable.setGL(new DebugGL(drawable.getGL()));");
            output.println("</PRE>");
            output.println("*/");
        }

        protected boolean hasPreDownstreamCallHook(Method m) {
            return true;
        }

        protected void preDownstreamCallHook(PrintWriter output, Method m) {
            output.println("    checkContext();");
        }

        protected boolean hasPostDownstreamCallHook(Method m) {
            return true;
        }

        protected void postDownstreamCallHook(PrintWriter output, Method m) {
            if (m.getName().equals("glBegin")) {
                output.println("    insideBeginEndPair = true;");
                output.println("    // NOTE: can't check glGetError(); it's not allowed inside glBegin/glEnd pair");
            } else {
                if (m.getName().equals("glEnd")) {
                    output.println("    insideBeginEndPair = false;");
                }

                output.println("    String txt = new String(\"" + m.getName() + "(\" +");
                Class<?>[] params = m.getParameterTypes();
                for (int i = 0; params != null && i < params.length; i++) {
                    output.print("    \"<" + params[i].getName() + ">");
                    if (params[i].isArray()) {
                        output.print("\" +");
                    } else if (params[i].equals(int.class)) {
                        output.print(" 0x\"+Integer.toHexString(arg" + i + ").toUpperCase() +");
                    } else {
                        output.print(" \"+arg" + i + " +");
                    }
                    if (i < params.length - 1) {
                        output.println("    \", \" +");
                    }
                }
                output.println("    \")\");");
                // calls to glGetError() are only allowed outside of glBegin/glEnd pairs
                output.println("    checkGLGetError( txt );");
            }
        }
    } // end class DebugPipeline

    //-------------------------------------------------------
    protected class TracePipeline extends PipelineEmitter {

        String className;

        TracePipeline(String outputDir, String outputPackage, Class<?> baseInterfaceClass, Class<?> downstreamClass) {
            super(outputDir, outputPackage, baseInterfaceClass, null, downstreamClass);
            className = "Trace" + getBaseInterfaceName();
        }

        protected String getOutputName() {
            return className;
        }

        protected int getMode() {
            return 0;
        }

        protected boolean emptyMethodAllowed() {
            return false;
        }

        protected boolean emptyDownstreamAllowed() {
            return false;
        }

        @Override
        protected void preMethodEmissionHook(PrintWriter output) {
            super.preMethodEmissionHook(output);
        }

        protected void constructorHook(PrintWriter output) {
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
        protected void postMethodEmissionHook(PrintWriter output) {
            super.postMethodEmissionHook(output);
            output.println("private PrintStream " + getOutputStreamName() + ";");
            output.println("private int indent = 0;");
            output.println("protected String dumpArray(Object obj)");
            output.println("{");
            output.println("  if (obj == null) return \"[null]\";");
            output.println("  StringBuffer sb = new StringBuffer(\"[\");");
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

        protected void emitClassDocComment(PrintWriter output) {
            output.println("/** <P> Composable pipeline which wraps an underlying {@link GL} implementation,");
            output.println("    providing tracing information to a user-specified {@link java.io.PrintStream}");
            output.println("    before and after each OpenGL method call. Sample code which installs this pipeline: </P>");
            output.println();
            output.println("<PRE>");
            output.println("     GL gl = drawable.setGL(new TraceGL(drawable.getGL(), System.err));");
            output.println("</PRE>");
            output.println("*/");
        }

        protected boolean hasPreDownstreamCallHook(Method m) {
            return true;
        }

        protected void preDownstreamCallHook(PrintWriter output, Method m) {
            if (m.getName().equals("glEnd") || m.getName().equals("glEndList")) {
                output.println("indent-=2;");
                output.println("    printIndent();");
            } else {
                output.println("printIndent();");
            }

            output.print("    print(");
            printFunctionCallString(output, m);
            output.println(");");
        }

        protected boolean hasPostDownstreamCallHook(Method m) {
            return true;
        }

        protected void postDownstreamCallHook(PrintWriter output, Method m) {
            Class<?> ret = m.getReturnType();
            if (ret != Void.TYPE) {
                output.println("    println(\" = \"+_res);");
            } else {
                output.println("    println(\"\");");
            }
        }

        private String getOutputStreamName() {
            return "stream";
        }
    } // end class TracePipeline

    public static final void printFunctionCallString(PrintWriter output, Method m) {
        Class<?>[] params = m.getParameterTypes();
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
