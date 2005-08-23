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
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package net.java.games.gluegen;

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

import net.java.games.gluegen.cgram.types.*;
import net.java.games.jogl.util.BufferUtils;

/** Emits the Java-side component of the Java<->C JNI binding. */
public class JavaMethodBindingImplEmitter extends JavaMethodBindingEmitter
{
  private boolean isUnimplemented;

  public JavaMethodBindingImplEmitter(MethodBinding binding, PrintWriter output, String runtimeExceptionType)
  {
    this(binding, output, runtimeExceptionType, false, false);
  }

  public JavaMethodBindingImplEmitter(MethodBinding binding,
                                      PrintWriter output,
                                      String runtimeExceptionType,
                                      boolean isUnimplemented, 
                                      boolean arrayImplExpansion)
  {
    super(binding, output, runtimeExceptionType);
    setCommentEmitter(defaultJavaCommentEmitter);
    this.isUnimplemented = isUnimplemented;
    this.forArrayImplementingMethodCall = arrayImplExpansion;
  }

  public JavaMethodBindingImplEmitter(JavaMethodBindingEmitter arg) {
    super(arg);
    if (arg instanceof JavaMethodBindingImplEmitter) {
      this.isUnimplemented = ((JavaMethodBindingImplEmitter) arg).isUnimplemented;
    }
  }

  protected void emitBody(PrintWriter writer)
  {    
    MethodBinding binding = getBinding();
    if (needsBody()) {
      writer.println();
      writer.println("  {");
      if (isUnimplemented) {
        writer.println("    throw new " + getRuntimeExceptionType() + "(\"Unimplemented\");");
      } else {
        emitPreCallSetup(binding, writer);
        //emitReturnVariableSetup(binding, writer);
        emitReturnVariableSetupAndCall(binding, writer);
      }
      writer.println("  }");
    } else {
      writer.println(";");
    }
  }


 protected boolean generateIndirectBufferInterface = false;

  public boolean isGenerateIndirectBufferInterface() {
    return generateIndirectBufferInterface;
  }
                                                                                                                                     
  public void setGenerateIndirectBufferInterface(boolean generateIndirect) {
     generateIndirectBufferInterface = generateIndirect;
  }


  protected boolean isUnimplemented() {
    return isUnimplemented;
  }

  protected boolean needsBody() {
    return (isUnimplemented ||
            getBinding().signatureUsesNIO() ||
            getBinding().signatureUsesCArrays() ||
            getBinding().signatureUsesPrimitiveArrays() ||
            getBinding().hasContainingType());
  }

  protected void emitPreCallSetup(MethodBinding binding, PrintWriter writer) {
    if(isGenerateIndirectBufferInterface()) {
        // case for when indirect Buffer is a possibility
        emitArrayLengthAndNIOInDirectBufferChecks(binding, writer);
    } else {
        emitArrayLengthAndNIOBufferChecks(binding, writer);
    }
  }


  protected void emitArrayLengthAndNIOBufferChecks(MethodBinding binding, PrintWriter writer) {
     int numBufferOffsetArrayArgs = 0;
    // Check lengths of any incoming arrays if necessary
    for (int i = 0; i < binding.getNumArguments(); i++) {
      Type type = binding.getCArgumentType(i);
      JavaType javaType = binding.getJavaArgumentType(i);
      if (type.isArray()) {
        ArrayType arrayType = type.asArray();
        writer.println("    if (" + binding.getArgumentName(i) + ".length < " + arrayType.getLength() + ")");
        writer.println("      throw new " + getRuntimeExceptionType() + "(\"Length of array \\\"" + binding.getArgumentName(i) +
                       "\\\" was less than the required " + arrayType.getLength() + "\");");
      } else {
        if (javaType.isNIOBuffer()) {
          writer.println("    if (!BufferFactory.isDirect(" + binding.getArgumentName(i) + "))");
          writer.println("      throw new " + getRuntimeExceptionType() + "(\"Argument \\\"" +
                         binding.getArgumentName(i) + "\\\" was not a direct buffer\");");
        } else if (javaType.isNIOBufferArray()) {
          numBufferOffsetArrayArgs++;
          String argName = binding.getArgumentName(i);
          String arrayName =  byteOffsetArrayConversionArgName(numBufferOffsetArrayArgs);
          writer.println("    int[] " + arrayName + " = new int[" + argName + ".length];");
          // Check direct buffer properties of all buffers within
          writer.println("    if (" + argName + " != null) {");
          writer.println("      for (int _ctr = 0; _ctr < " + argName + ".length; _ctr++) {");
          writer.println("        if (!BufferFactory.isDirect(" + argName + "[_ctr])) {");
          writer.println("          throw new " + getRuntimeExceptionType() + 
                         "(\"Element \" + _ctr + \" of argument \\\"" +
                         binding.getArgumentName(i) + "\\\" was not a direct buffer\");");
          writer.println("        }");
          // get the Buffer Array offset values and save them into another array to send down to JNI
          writer.print("         " + arrayName + "[_ctr] = BufferFactory.getDirectBufferByteOffset(");
          writer.println(argName + "[_ctr]);");
          writer.println("      }");
          writer.println("    }");
        } else if (javaType.isArray() && !javaType.isNIOBufferArray() &&!javaType.isStringArray()) {
           String argName = binding.getArgumentName(i);
           String offsetArg = argName + "_offset";
           writer.println("    if(" + argName + " != null && " + argName + ".length <= " + offsetArg + ")");
           writer.print("         throw new " + getRuntimeExceptionType()); 
           writer.println("(\"array offset argument \\\"" + offsetArg + "\\\" equals or exceeds array length\");");
        }
      }
    }
  }


  protected void emitArrayLengthAndNIOInDirectBufferChecks(MethodBinding binding, PrintWriter writer) {
     int numBufferOffsetArrayArgs = 0;
     boolean firstBuffer = true;
    // Check lengths of any incoming arrays if necessary
    for (int i = 0; i < binding.getNumArguments(); i++) {
      Type type = binding.getCArgumentType(i);
      if (type.isArray()) {
        ArrayType arrayType = type.asArray();
        writer.println("    if (" + binding.getArgumentName(i) + ".length < " +
                         arrayType.getLength() + ")");
        writer.println("      throw new " + getRuntimeExceptionType() +
                       "(\"Length of array \\\"" + binding.getArgumentName(i) +
                       "\\\" was less than the required " + arrayType.getLength() + "\");");
      } else {
        JavaType javaType = binding.getJavaArgumentType(i);
        if (javaType.isNIOBuffer()) {
          if(firstBuffer == true) {
               firstBuffer = false;
               writer.println("     boolean direct = true, firstTime = true, result = true;");
          }
          writer.println("    result = BufferFactory.isDirect(" + binding.getArgumentName(i) + ");");
          writer.println("    if(firstTime == true) {");
          writer.println("       direct = result;");
          writer.println("       firstTime = false;");
          writer.println("    } else {");
          writer.println("            if(direct != result)");
          writer.println("                 throw new " + getRuntimeExceptionType() +
                         "(\"Argument \\\"" + binding.getArgumentName(i) +
                         "\\\" :Not all Buffers in this method were direct or indirect\");");
          writer.println("    }");
        } else if (javaType.isNIOBufferArray()) {
          if(firstBuffer == true) {
               firstBuffer = false;
               writer.println("     boolean direct = true, firstTime = true, result = true;");
          }
          numBufferOffsetArrayArgs++;
          String argName = binding.getArgumentName(i);
          String arrayName =  byteOffsetArrayConversionArgName(numBufferOffsetArrayArgs);
          writer.println("    int[] " + arrayName + " = new int[" + argName + ".length];");
          writer.println("    if (" + argName + " != null) {");
          // Check direct/indirect buffer properties of all buffers within
          writer.println("      for (int _ctr = 0; _ctr < " + argName + ".length; _ctr++) {");
          writer.println("         result = BufferFactory.isDirect(" + argName + "[_ctr]);");
                                                                                                                                     
          writer.println("         if(firstTime == true) {");
          writer.println("            direct = result;");
          writer.println("            firstTime = false;");
          writer.println("         } else {");
          writer.println("             if(direct != result)");
          writer.println("          throw new " + getRuntimeExceptionType() + "(\"Element \" + _ctr + \" of argument \\\"" + binding.getArgumentName(i) + "\\\":Mixture of Direct/Indirect Buffers in Method Args\");");
          writer.println("        }");
          // get the Buffer Array offset values and save them into another array to send down to JNI
          writer.println("        if(direct)");
          writer.print("              " + arrayName + "[_ctr] = BufferFactory.getDirectBufferByteOffset(");
          writer.println(argName + "[_ctr]);");
          writer.println("        else");
          writer.print("              " + arrayName + "[_ctr] = BufferFactory.getIndirectBufferByteOffset(");
          writer.println(argName + "[_ctr]);");
          writer.println("      }");
          writer.println("    }");
        }
      }
    }
   }
                                                                                                                                     


/* old method before indirect buffer support was added
  protected void emitReturnVariableSetup(MethodBinding binding, PrintWriter writer) {
    writer.print("    ");
    JavaType returnType = binding.getJavaReturnType();
    if (!returnType.isVoid()) {
      if (returnType.isCompoundTypeWrapper() ||
          returnType.isNIOByteBuffer()) {
        writer.println("ByteBuffer _res;");
        writer.print("    _res = ");
      } else if (returnType.isArrayOfCompoundTypeWrappers()) {
        writer.println("ByteBuffer[] _res;");
        writer.print("    _res = ");
      } else {
        writer.print("return ");
      }
    }
  }
*/


  protected void emitReturnVariableSetupAndCall(MethodBinding binding, PrintWriter writer) {

    boolean returnFunction = false;

    writer.print("    ");
    JavaType returnType = binding.getJavaReturnType();
    if (!returnType.isVoid()) {
      if (returnType.isCompoundTypeWrapper() ||
          returnType.isNIOByteBuffer()) {
        writer.println("ByteBuffer _res;");
        writer.print("    _res = ");
      } else if (returnType.isArrayOfCompoundTypeWrappers()) {
        writer.println("ByteBuffer[] _res;");
        writer.print("    _res = ");
      } else {
         if(isGenerateIndirectBufferInterface())
               returnFunction = true;
          else 
               writer.print("return ");
      }
    }


    if(isGenerateIndirectBufferInterface()) {
          //binding.setIndirectVariant(false);
          //writer.println("    boolean direct = true;");
          writer.println("    if(direct)  {");
          writer.print("         ");
    };
    if(returnFunction) writer.print("return ");
    writer.print(getImplMethodName());
    writer.print("(");
    emitCallArguments(binding, writer, false);
    writer.print(")");
    if(isGenerateIndirectBufferInterface()) {
          writer.println(";");
          //binding.setIndirectVariant(true);
          writer.println("    }  else  { ");
          if(returnFunction) writer.print("return ");
          // get the indirect Buffer implementation name
          setIndirectBufferInterface(true);
          writer.print("         " + getImplMethodName());
          writer.print("(");
          setIndirectBufferInterface(false);
          emitCallArguments(binding, writer, true);
          writer.println(");");
          writer.println("    }");
    };
    emitCallResultReturn(binding, writer);
  }
 
 
  protected int emitCallArguments(MethodBinding binding, PrintWriter writer, boolean indirectCase) {
    boolean needComma = false;
    int numArgsEmitted = 0;
    int numBufferOffsetArgs = 0, numBufferOffsetArrayArgs = 0;
    boolean generateDirectAndIndirect;

    generateDirectAndIndirect = isGenerateIndirectBufferInterface();

    if (binding.hasContainingType()) {
      // Emit this pointer
      assert(binding.getContainingType().isCompoundTypeWrapper());
      writer.print("getBuffer()");
      needComma = true;
      ++numArgsEmitted;
      numBufferOffsetArgs++;
      //writer.print(", " + byteOffsetConversionArgName(numBufferOffsetArgs));
      writer.print(", BufferFactory.getDirectBufferByteOffset(getBuffer())");
    }
    for (int i = 0; i < binding.getNumArguments(); i++) {
      JavaType type = binding.getJavaArgumentType(i);
      if (type.isJNIEnv() || binding.isArgumentThisPointer(i)) {
        // Don't need to expose these at the Java level
        continue;
      }

      if (type.isVoid()) {
        // Make sure this is the only param to the method; if it isn't,
        // there's something wrong with our parsing of the headers.
        assert(binding.getNumArguments() == 1);
        continue;
      } 

      if (needComma) {
        writer.print(", ");
      }

      if (type.isCompoundTypeWrapper()) {
        writer.print("((");
      }

      if(type.isNIOBuffer() && !type.isNIOBufferArray() && generateDirectAndIndirect && indirectCase) {
         writer.print("BufferFactory.getArray(" + binding.getArgumentName(i) + ")");
      } else {
         writer.print(binding.getArgumentName(i));
      }

      if (type.isCompoundTypeWrapper()) {
        writer.print(" == null) ? null : ");
        writer.print(binding.getArgumentName(i));
        writer.print(".getBuffer())");
        numBufferOffsetArgs++;
        //writer.print(", " + byteOffsetConversionArgName(numBufferOffsetArgs));
        writer.print(", BufferFactory.getDirectBufferByteOffset(((" + binding.getArgumentName(i));
        writer.print(" == null) ? null : " + binding.getArgumentName(i) + ".getBuffer()))");
      }
      needComma = true;
      ++numArgsEmitted;
      if(type.isNIOBuffer() || type.isNIOBufferArray()) {
             if(!type.isArray()) {
                  numBufferOffsetArgs++;
                  if(generateDirectAndIndirect) {
                         if(!indirectCase) {
                      writer.print
                        (", BufferFactory.getDirectBufferByteOffset(" + binding.getArgumentName(i) + ")");
                         } else {
                      writer.print
                        (", BufferFactory.getIndirectBufferByteOffset(" + binding.getArgumentName(i) + ")");
                         }
                  } else {
                      writer.print
                        (", BufferFactory.getDirectBufferByteOffset(" + binding.getArgumentName(i) + ")");
                  }
             } else {
                   numBufferOffsetArrayArgs++;
                   writer.print(", " + byteOffsetArrayConversionArgName(numBufferOffsetArrayArgs));
            }
      }

      // Add Array offset parameter for primitive arrays
      if(type.isArray() && !type.isNIOBufferArray() && !type.isStringArray()) {
           // writer.print(", " + binding.getArgumentName(i) + "_offset");
              if(type.isFloatArray()) {
                     writer.print(", BufferFactory.SIZEOF_FLOAT * " + binding.getArgumentName(i) + "_offset");
              } else if(type.isDoubleArray()) {
                     writer.print(", BufferFactory.SIZEOF_DOUBLE * " + binding.getArgumentName(i) + "_offset");
              } else if(type.isByteArray()) {
                     writer.print(", " + binding.getArgumentName(i) + "_offset");
              } else if(type.isLongArray()) {
                     writer.print(", BufferFactory.SIZEOF_LONG * " + binding.getArgumentName(i) + "_offset");
              } else if(type.isShortArray()) {
                     writer.print(", BufferFactory.SIZEOF_SHORT * " + binding.getArgumentName(i) + "_offset");
              } else if(type.isIntArray()) {
                     writer.print(", BufferFactory.SIZEOF_INT * " + binding.getArgumentName(i) + "_offset");
              } else {
                    throw new RuntimeException("Unsupported type for calculating array offset argument for " +
              binding.getArgumentName(i) +  "-- error occurred while processing Java glue code for " + binding.getName());
              }         
      }

    }
    return numArgsEmitted;
  }

  protected void emitCallResultReturn(MethodBinding binding, PrintWriter writer) {
    JavaType returnType = binding.getJavaReturnType();
    boolean indirect;

    indirect = isGenerateIndirectBufferInterface();

    if (returnType.isCompoundTypeWrapper()) {
      if(!indirect) writer.println(";");
      String fmt = getReturnedArrayLengthExpression();
      writer.println("    if (_res == null) return null;");
      if (fmt == null) {
        writer.print("    return new " + returnType.getName() + "(_res.order(ByteOrder.nativeOrder()))");
      } else {
        writer.println("    _res.order(ByteOrder.nativeOrder());");
        String[] argumentNames = new String[binding.getNumArguments()];
        for (int i = 0; i < binding.getNumArguments(); i++) {
          argumentNames[i] = binding.getArgumentName(i);
        }
        String expr = new MessageFormat(fmt).format(argumentNames);
        PointerType cReturnTypePointer = binding.getCReturnType().asPointer();
        CompoundType cReturnType = null;
        if (cReturnTypePointer != null) {
          cReturnType = cReturnTypePointer.getTargetType().asCompound();
        }
        if (cReturnType == null) {
          throw new RuntimeException("ReturnedArrayLength directive currently only supported for pointers to compound types " +
                                     "(error occurred while generating Java glue code for " + binding.getName() + ")");
        }
        writer.println("    " + getReturnTypeString(false) + " _retarray = new " + getReturnTypeString(true) + "[" + expr + "];");
        writer.println("    for (int _count = 0; _count < " + expr + "; _count++) {");
        // Create temporary ByteBuffer slice
        // FIXME: probably need Type.getAlignedSize() for arrays of
        // compound types (rounding up to machine-dependent alignment)
        writer.println("      _res.position(_count * " + cReturnType.getSize() + ");");
        writer.println("      _res.limit   ((1 + _count) * " + cReturnType.getSize() + ");");
        writer.println("      ByteBuffer _tmp = _res.slice();");
        writer.println("      _tmp.order(ByteOrder.nativeOrder());");
        writer.println("      _res.position(0);");
        writer.println("      _res.limit(_res.capacity());");
        writer.println("      _retarray[_count] = new " + getReturnTypeString(true) + "(_tmp);");
        writer.println("    }");
        writer.print  ("    return _retarray");
      }
      writer.println(";");
    } else if (returnType.isNIOBuffer()) {
      if(!indirect) writer.println(";");
      writer.println("    if (_res == null) return null;");
      writer.print("    return _res.order(ByteOrder.nativeOrder())");
      writer.println(";");
    } else if (returnType.isArrayOfCompoundTypeWrappers()) {
      if(!indirect) writer.println(";");
      writer.println("    if (_res == null) return null;");
      writer.println("    " + getReturnTypeString(false) + " _retarray = new " + getReturnTypeString(true) + "[_res.length];");
      writer.println("    for (int _count = 0; _count < _res.length; _count++) {");
      writer.println("      _retarray[_count] = new " + getReturnTypeString(true) + "(_res[_count]);");
      writer.println("    }");
      writer.print  ("    return _retarray");
      writer.println(";");
    } else {
      if(!indirect) writer.println(";");
    }
  }
}

