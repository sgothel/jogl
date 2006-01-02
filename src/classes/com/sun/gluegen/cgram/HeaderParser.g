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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

header {
        package com.sun.gluegen.cgram;

        import java.io.*;
        import java.util.*;

        import antlr.CommonAST;
        import com.sun.gluegen.cgram.types.*;
}

class HeaderParser extends GnuCTreeParser;
options {
        k = 1;
}

{
    /** Name assigned to a anonymous EnumType (e.g., "enum { ... }"). */
    public static final String ANONYMOUS_ENUM_NAME = "<anonymous>";

    /** Set the dictionary mapping typedef names to types for this
        HeaderParser. Must be done before parsing. */
    public void setTypedefDictionary(TypeDictionary dict) {
        this.typedefDictionary = dict;
    }

    /** Returns the typedef dictionary this HeaderParser uses. */
    public TypeDictionary getTypedefDictionary() {
        return typedefDictionary;
    }    
    
    /** Set the dictionary mapping struct names (i.e., the "foo" in
        "struct foo { ... };") to types for this HeaderParser. Must be done
        before parsing. */
    public void setStructDictionary(TypeDictionary dict) {
        this.structDictionary = dict;
    }

    /** Returns the struct name dictionary this HeaderParser uses. */
    public TypeDictionary getStructDictionary() {
        return structDictionary;
    }    
    
    /** Get the canonicalization map, which is a regular HashMap
        mapping Type to Type and which is used for looking up the unique
        instances of e.g. pointer-to-structure types that have been typedefed
        and therefore have names. */
    public Map getCanonMap() {
        return canonMap;
    }

    /** Pre-define the list of EnumTypes for this HeaderParser. Must be
		done before parsing. */
    public void setEnums(List/*<EnumType>*/ enumTypes) {
        // FIXME: Need to take the input set of EnumTypes, extract all
        // the enumerates from each EnumType, and fill in the enumHash
        // so that each enumerate maps to the enumType to which it
        // belongs.
		throw new RuntimeException("setEnums is Unimplemented!");
    }

    /** Returns the EnumTypes this HeaderParser processed. */
    public List/*<EnumType>*/ getEnums() {
        return new ArrayList(enumHash.values());
    }    
    
    /** Clears the list of functions this HeaderParser has parsed.
        Useful when reusing the same HeaderParser for more than one
        header file. */
    public void clearParsedFunctions() {
        functions.clear();
    }

    /** Returns the list of FunctionSymbols this HeaderParser has parsed. */
    public List getParsedFunctions() {
        return functions;
    }

    private CompoundType lookupInStructDictionary(String typeName,
                                                  CompoundTypeKind kind,
                                                  int cvAttrs) {
        CompoundType t = (CompoundType) structDictionary.get(typeName);
        if (t == null) {
            t = new CompoundType(null, null, kind, cvAttrs);
            t.setStructName(typeName);
            structDictionary.put(typeName, t);
        }
        return t;
    }

    private Type lookupInTypedefDictionary(String typeName) {
        Type t = typedefDictionary.get(typeName);
        if (t == null) {
            throw new RuntimeException("Undefined reference to typedef name " + typeName);
        }
        return t;
    }

    static class ParameterDeclaration {
        private String id;
        private Type   type;

        ParameterDeclaration(String id, Type type) {
            this.id = id;
            this.type = type;
        }
        String id()             { return id; }
        Type   type()           { return type; }
    }

    // A box for a Type. Allows type to be passed down to be modified by recursive rules.
    static class TypeBox {
        private Type origType;
        private Type type;
        private boolean isTypedef;

        TypeBox(Type type) {
            this(type, false);
        }

        TypeBox(Type type, boolean isTypedef) {
            this.origType = type;
            this.isTypedef = isTypedef;
        }

        Type type() {
            if (type == null) {
                return origType;
            }
            return type;
        }
        void setType(Type type) {
            this.type = type;
        }
        void reset() {
            type = null;
        }

        boolean isTypedef()     { return isTypedef; }

	    // for easier debugging
	    public String toString() { 
	       String tStr = "Type=NULL_REF";
	       if (type == origType) {
			 tStr = "Type=ORIG_TYPE";
	  	   } else if (type != null) {
		     tStr = "Type: name=\"" + type.getCVAttributesString() + " " + 
                    type.getName() + "\"; signature=\"" + type + "\"; class " + 
					type.getClass().getName();
	       }
	       String oStr = "OrigType=NULL_REF";
	       if (origType != null) {
		     oStr = "OrigType: name=\"" + origType.getCVAttributesString() + " " + 
             origType.getName() + "\"; signature=\"" + origType + "\"; class " + 
			origType.getClass().getName();
	       }
	       return "<["+tStr + "] [" + oStr + "] " + " isTypedef=" + isTypedef+">"; 
	    }
    }

    private boolean doDeclaration;   // Used to only process function typedefs
    private String  declId;
    private List    parameters;
    private TypeDictionary typedefDictionary;
    private TypeDictionary structDictionary;
    private List/*<FunctionSymbol>*/ functions = new ArrayList();
    // hash from name of an enumerated value to the EnumType to which it belongs
    private HashMap/*<String,EnumType>*/ enumHash = new HashMap();

    // Storage class specifiers
    private static final int AUTO     = 1 << 0;
    private static final int REGISTER = 1 << 1;
    private static final int TYPEDEF  = 1 << 2;
    // Function storage class specifiers
    private static final int EXTERN   = 1 << 3;
    private static final int STATIC   = 1 << 4;
    private static final int INLINE   = 1 << 5;
    // Type qualifiers
    private static final int CONST    = 1 << 6;
    private static final int VOLATILE = 1 << 7;
    private static final int SIGNED   = 1 << 8;
    private static final int UNSIGNED = 1 << 9;

    private void initDeclaration() {
        doDeclaration = false;
        declId = null;
    }

    private void doDeclaration() {
        doDeclaration = true;
    }

    private void processDeclaration(Type returnType) {
        if (doDeclaration) {
            FunctionSymbol sym = new FunctionSymbol(declId, new FunctionType(null, null, returnType, 0));
	        if (parameters != null) { // handle funcs w/ empty parameter lists (e.g., "foo()")
                for (Iterator iter = parameters.iterator(); iter.hasNext(); ) {
                    ParameterDeclaration pd = (ParameterDeclaration) iter.next();
                    sym.addArgument(pd.type(), pd.id());
                }
	        }
            functions.add(sym);
        }
    }

    private int attrs2CVAttrs(int attrs) {
        int cvAttrs = 0;
        if ((attrs & CONST) != 0) {
            cvAttrs |= CVAttributes.CONST;
        }
        if ((attrs & VOLATILE) != 0) {
            cvAttrs |= CVAttributes.VOLATILE;
        }
        return cvAttrs;
    }

    /** Helper routine which handles creating a pointer or array type
        for [] expressions */
    private void handleArrayExpr(TypeBox tb, AST t) {
        if (t != null) {
            try {
                // FIXME: this doesn't take into account struct alignment, which may be necessary
                // See also FIXMEs in ArrayType.java
                int len = parseIntConstExpr(t);
                tb.setType(canonicalize(new ArrayType(tb.type(), SizeThunk.mul(SizeThunk.constant(len), tb.type().getSize()), len, 0)));
                return;
            } catch (RecognitionException e) {
                // Fall through
            }
        }
        tb.setType(canonicalize(new PointerType(SizeThunk.POINTER,
                                                tb.type(),
                                                0)));
    }

    private int parseIntConstExpr(AST t) throws RecognitionException {
        return intConstExpr(t);
    }

  /** Utility function: creates a new EnumType with the given name, or
	  returns an existing one if it has already been created. */
  private EnumType getEnumType(String enumTypeName) {
	EnumType enumType = null;
	Iterator it = enumHash.values().iterator(); 
	while (it.hasNext()) {
	  EnumType potentialMatch = (EnumType)it.next();
	  if (potentialMatch.getName().equals(enumTypeName)) {
		enumType = potentialMatch;
		break;	
	  }
	}
	
	if (enumType == null) {
      // This isn't quite correct. In theory the enum should expand to
      // the size of the largest element, so if there were a long long
      // entry the enum should expand to e.g. int64. However, using
      // "long" here (which is what used to be the case) was 
      // definitely incorrect and caused problems.
	  enumType = new EnumType(enumTypeName, SizeThunk.INT);
	}  
	
	return enumType;
  }	
  
  // Map used to canonicalize types. For example, we may typedef
  // struct foo { ... } *pfoo; subsequent references to struct foo* should
  // point to the same PointerType object that had its name set to "pfoo".
  private Map canonMap = new HashMap();
  private Type canonicalize(Type t) {
    Type res = (Type) canonMap.get(t);
    if (res != null) {
      return res;
    }
    canonMap.put(t, t);
    return t;
  }
}

declarator[TypeBox tb] returns [String s] {
    initDeclaration();
    s = null;
    List params = null;
    String funcPointerName = null;
    TypeBox dummyTypeBox = null;
}
        :   #( NDeclarator
                ( pointerGroup[tb] )?

                ( id:ID  { s = id.getText(); }
                | LPAREN funcPointerName = declarator[dummyTypeBox] RPAREN
                )

                (   #( NParameterTypeList
                      (
                        params = parameterTypeList
                        | (idList)?
                      ) 
                      RPAREN
                    )  {
                           if (id != null) {
                               declId = id.getText();
                               parameters = params; // FIXME: Ken, why are we setting this class member here? 
                               doDeclaration();
                           } else if ( funcPointerName != null ) {
                               /* TypeBox becomes function pointer in this case */
                               FunctionType ft = new FunctionType(null, null, tb.type(), 0);
                               if (params == null) {
			  	                   // If the function pointer has no declared parameters, it's a 
			                       // void function. I'm not sure if the parameter name is 
			                       // ever referenced anywhere when the type is VoidType, so
                                   // just in case I'll set it to a comment string so it will
			                       // still compile if written out to code anywhere.
			  	                   ft.addArgument(new VoidType(0), "/*unnamed-void*/");
			                   } else {
			  	                   for (Iterator iter = params.iterator(); iter.hasNext(); ) {
                                     ParameterDeclaration pd = (ParameterDeclaration) iter.next();
                                     ft.addArgument(pd.type(), pd.id());
				                   }
                               }
                               tb.setType(canonicalize(new PointerType(SizeThunk.POINTER,
                                                                       ft,
                                                                       0)));
                               s = funcPointerName;
                           }
                       }
                 | LBRACKET ( e:expr )? RBRACKET { handleArrayExpr(tb, e); }
                )*
             )
        ;

typelessDeclaration {
    TypeBox tb = null;
}
        :       #(NTypeMissing initDeclList[tb] SEMI)
        ;

declaration {
    TypeBox tb = null;
}
        :       #( NDeclaration
                    tb = declSpecifiers 
                    (
                        initDeclList[tb] 
                    )?
                    ( SEMI )+
                ) { processDeclaration(tb.type()); }
        ;

parameterTypeList returns [List l] { l = new ArrayList(); ParameterDeclaration decl = null; }
        :       ( decl = parameterDeclaration { if (decl != null) l.add(decl); } ( COMMA | SEMI )? )+ ( VARARGS )?
        ;

parameterDeclaration returns [ParameterDeclaration pd] {
    Type t = null;
    String decl = null;
    pd = null;
    TypeBox tb = null;
}
        :       #( NParameterDeclaration
                tb    = declSpecifiers
                (decl = declarator[tb] | nonemptyAbstractDeclarator[tb])?
                ) { pd = new ParameterDeclaration(decl, tb.type()); }
        ;

functionDef {
    TypeBox tb = null;
}
        :   #( NFunctionDef
                ( functionDeclSpecifiers)? 
                declarator[tb]
                (declaration | VARARGS)*
                compoundStatement
            )
        ;

declSpecifiers returns [TypeBox tb] {
    tb = null;
    Type t = null;
    int x = 0;
    int y = 0; 
}
        :       ( y = storageClassSpecifier { x |= y; } 
                | y = typeQualifier         { x |= y; }
                | t = typeSpecifier[x]
                )+
{
            if (t == null &&
                (x & (SIGNED | UNSIGNED)) != 0) {
                t = new IntType("int", SizeThunk.INT, ((x & UNSIGNED) != 0), attrs2CVAttrs(x));
            }
            tb = new TypeBox(t, ((x & TYPEDEF) != 0));
}
        ;

storageClassSpecifier returns [int x] { x = 0; }
        :       "auto"     { x |= AUTO;     }
        |       "register" { x |= REGISTER; }
        |       "typedef"  { x |= TYPEDEF;  }
        |       x = functionStorageClassSpecifier
        ;


functionStorageClassSpecifier returns [int x] { x = 0; }
        :       "extern" { x |= EXTERN; }
        |       "static" { x |= STATIC; }
        |       "inline" { x |= INLINE; }
        ;


typeQualifier returns [int x] { x = 0; }
        :       "const"    { x |= CONST; }
        |       "volatile" { x |= VOLATILE; }
        |       "signed"   { x |= SIGNED; }
        |       "unsigned" { x |= UNSIGNED; }
        ;

typeSpecifier[int attributes] returns [Type t] {
    t = null;
    int cvAttrs = attrs2CVAttrs(attributes);
    boolean unsigned = ((attributes & UNSIGNED) != 0);
}
        :       "void"     { t = new VoidType(cvAttrs); }
        |       "char"     { t = new IntType("char" , SizeThunk.CHAR,  unsigned, cvAttrs); }
        |       "short"    { t = new IntType("short", SizeThunk.SHORT, unsigned, cvAttrs); }
        |       "int"      { t = new IntType("int"  , SizeThunk.INT,   unsigned, cvAttrs); }
        |       "long"     { t = new IntType("long" , SizeThunk.LONG,  unsigned, cvAttrs); }
        |       "__int64"  { t = new IntType("__int64", SizeThunk.INT64, unsigned, cvAttrs); }
        |       "float"    { t = new FloatType("float", SizeThunk.FLOAT, cvAttrs); }
        |       "double"   { t = new DoubleType("double", SizeThunk.DOUBLE, cvAttrs); }
        |       t = structSpecifier[cvAttrs] ( attributeDecl )*
        |       t = unionSpecifier [cvAttrs] ( attributeDecl )*
        |       t = enumSpecifier  [cvAttrs] 
        |       t = typedefName    [cvAttrs] 
        |       #("typeof" LPAREN
                    ( (typeName )=> typeName 
                    | expr
                    )
                    RPAREN
                )
        |       "__complex"
        ;

typedefName[int cvAttrs] returns [Type t] { t = null; }
        :       #(NTypedefName id : ID)
            { 
              t = canonicalize(lookupInTypedefDictionary(id.getText()).getCVVariant(cvAttrs));
            }
        ;

structSpecifier[int cvAttrs] returns [Type t] { t = null; }
        :   #( "struct" t = structOrUnionBody[CompoundTypeKind.STRUCT, cvAttrs] )
        ;

unionSpecifier[int cvAttrs] returns [Type t] { t = null; }
        :   #( "union" t = structOrUnionBody[CompoundTypeKind.UNION, cvAttrs] )
        ;
   
structOrUnionBody[CompoundTypeKind kind, int cvAttrs] returns [CompoundType t] {
    t = null;
}
        :       ( (ID LCURLY) => id:ID LCURLY {
                    t = (CompoundType) canonicalize(lookupInStructDictionary(id.getText(), kind, cvAttrs));
                  } ( structDeclarationList[t] )?
                    RCURLY { t.setBodyParsed(); }
                |   LCURLY { t = new CompoundType(null, null, kind, cvAttrs); }
                    ( structDeclarationList[t] )?
                    RCURLY { t.setBodyParsed(); }
                | id2:ID { t = (CompoundType) canonicalize(lookupInStructDictionary(id2.getText(), kind, cvAttrs)); }
                )
        ;

structDeclarationList[CompoundType t]
        :       ( structDeclaration[t] )+
        ;

structDeclaration[CompoundType containingType] {
    Type t = null;
    boolean addedAny = false;
}
        :       t = specifierQualifierList addedAny = structDeclaratorList[containingType, t] {
                    if (!addedAny) {
                        if (t != null) {
                            CompoundType ct = t.asCompound();
                            if (ct.isUnion()) {
                                // Anonymous union
                                containingType.addField(new Field(null, t, null));
                            }
                        }
                    }
                }
        ;

specifierQualifierList returns [Type t] {
    t = null; int x = 0; int y = 0;
}
        :       (
                t = typeSpecifier[x]
                | y = typeQualifier { x |= y; }
                )+ {
            if (t == null &&
                (x & (SIGNED | UNSIGNED)) != 0) {
                t = new IntType("int", SizeThunk.INT, ((x & UNSIGNED) != 0), attrs2CVAttrs(x));
            }
}
        ;

structDeclaratorList[CompoundType containingType, Type t] returns [boolean addedAny] {
    addedAny = false;
    boolean y = false;
}
        :       ( y = structDeclarator[containingType, t] { addedAny = y; })+
        ;

structDeclarator[CompoundType containingType, Type t] returns [boolean addedAny] {
    addedAny = false;
    String s = null;
    TypeBox tb = new TypeBox(t);
}
        :
        #( NStructDeclarator      
            ( s = declarator[tb] { containingType.addField(new Field(s, tb.type(), null)); addedAny = true; } )?
            ( COLON expr     { /* FIXME: bit types not handled yet */ }        ) ?
            ( attributeDecl )*
        )
        ;

// FIXME: this will not correctly set the name of the enumeration when
// encountering a declaration like this:
//
//     typedef enum {  } enumName;
//                
// In this case calling getName() on the EnumType return value will
// incorrectly return HeaderParser.ANONYMOUS_ENUM_NAME instead of
// "enumName"
//
// I haven't implemented it yet because I'm not sure how to get the
// "enumName" *before* executing the enumList rule.
enumSpecifier [int cvAttrs] returns [Type t] { 
	t = null; 
}
        :       #( "enum"
                   ( ( ID LCURLY )=> i:ID LCURLY enumList[(EnumType)(t = getEnumType(i.getText()))] RCURLY 
                     | LCURLY enumList[(EnumType)(t = getEnumType(ANONYMOUS_ENUM_NAME))] RCURLY 
                     | ID { t = getEnumType(i.getText()); }
                    )
                  )
        ;

enumList[EnumType enumeration] {
	long defaultEnumerantValue = 0;
}
      :       ( defaultEnumerantValue = enumerator[enumeration, defaultEnumerantValue] )+
      ;

enumerator[EnumType enumeration, long defaultValue] returns [long newDefaultValue] {
	newDefaultValue = defaultValue;
}
        :       eName:ID ( ASSIGN eVal:expr )? { 
					// FIXME! Integer.parseInt() will throw if its argument is in octal or hex format.
					long value = (eVal == null) ? defaultValue : Long.parseLong(eVal.getText());
					newDefaultValue = value+1;
	  				String eTxt = eName.getText();
	  				if (enumHash.containsKey(eTxt)) {
						EnumType oldEnumType = ((EnumType)enumHash.get(eTxt));
						long oldValue = oldEnumType.getEnumValue(eTxt);
	  					System.err.println("WARNING: redefinition of enumerated value '" + eTxt + "';" +
		  				   " existing definition is in enumeration '" + oldEnumType.getName() +
		  				   "' with value " + oldValue + " and new definition is in enumeration '" +
		  				   enumeration.getName() + "' with value " + value);
						// remove old definition
						oldEnumType.removeEnumerate(eTxt);
	  				}
	  				// insert new definition
	  				enumeration.addEnum(eTxt, value);
	  				enumHash.put(eTxt, enumeration);
					//System.err.println("ENUM [" + enumeration.getName() + "]: " + eTxt + " = " + enumeration.getEnumValue(eTxt) + 
				    //                   " (new default = " + newDefaultValue + ")");
				}
	    ;

initDeclList[TypeBox tb]
        :       ( initDecl[tb] )+
        ;

initDecl[TypeBox tb] {
    String declName = null;
}
        :       #( NInitDecl
                declName = declarator[tb] { 
					//System.err.println("GOT declName: " + declName + " TB=" + tb);
	  			}
                ( attributeDecl )*
                ( ASSIGN initializer
                | COLON expr
                )?
                )
{
    if ((declName != null) && (tb != null) && tb.isTypedef()) {
        Type t = tb.type();
		    //System.err.println("Adding typedef mapping: [" + declName + "] -> [" + t + "]");
        if (!t.hasTypedefName()) {
            t.setName(declName);
        }
        t = canonicalize(t);
        typedefDictionary.put(declName, t);
        // Clear out PointerGroup effects in case another typedef variant follows
        tb.reset();
    }
}
        ;

pointerGroup[TypeBox tb] { int x = 0; int y = 0; }
        :       #( NPointerGroup ( STAR { x = 0; y = 0; } ( y = typeQualifier { x |= y; } )*
                                    {
		  																	//System.err.println("IN PTR GROUP: TB=" + tb);
                                        if (tb != null) {
                                            tb.setType(canonicalize(new PointerType(SizeThunk.POINTER,
                                                                                    tb.type(),
                                                                                    attrs2CVAttrs(x))));
                                        }
                                    }
                                 )+ )
  ;
                                    

functionDeclSpecifiers
        :       
                ( functionStorageClassSpecifier
                | typeQualifier
                | typeSpecifier[0]
                )+
        ;

typeName {
    TypeBox tb = null;
}
        :       specifierQualifierList (nonemptyAbstractDeclarator[tb])?
        ;


/* FIXME: the handling of types in this rule has not been well thought
   out and is known to be incomplete. Currently it is only used to handle
   pointerGroups for unnamed parameters. */
nonemptyAbstractDeclarator[TypeBox tb]
        :   #( NNonemptyAbstractDeclarator
            (   pointerGroup[tb]
                (   (LPAREN  
                    (   nonemptyAbstractDeclarator[tb]
                        | parameterTypeList
                    )?
                    RPAREN)
                | (LBRACKET (e1:expr)? RBRACKET) { handleArrayExpr(tb, e1); }
                )*

            |  (   (LPAREN  
                    (   nonemptyAbstractDeclarator[tb]
                        | parameterTypeList
                    )?
                    RPAREN)
                | (LBRACKET (e2:expr)? RBRACKET) { handleArrayExpr(tb, e2); }
                )+
            )
            )
        ;

/* Helper routine for parsing expressions which evaluate to integer
   constants. Can be made more complicated as necessary. */
intConstExpr returns [int i] { i = -1; }
        : n:Number   { return Integer.parseInt(n.getText()); }
        ;
