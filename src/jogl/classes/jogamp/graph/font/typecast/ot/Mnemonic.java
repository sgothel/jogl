/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- * 
 * This software is published under the terms of the Apache Software License * 
 * version 1.1, a copy of which has been included with this distribution in  * 
 * the LICENSE file.                                                         * 
 *****************************************************************************/

package jogamp.graph.font.typecast.ot;

/**
 * The Mnemonic representations of the TrueType instruction set.
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: Mnemonic.java,v 1.1.1.1 2004-12-05 23:14:30 davidsch Exp $
 */
public class Mnemonic {

    public static final short SVTCA = 0x00;  // [a]
    public static final short SPVTCA = 0x02; // [a]
    public static final short SFVTCA = 0x04; // [a]
    public static final short SPVTL = 0x06;  // [a]
    public static final short SFVTL = 0x08;  // [a]
    public static final short SPVFS = 0x0A;
    public static final short SFVFS = 0x0B;
    public static final short GPV = 0x0C;
    public static final short GFV = 0x0D;
    public static final short SFVTPV = 0x0E;
    public static final short ISECT = 0x0F;
    public static final short SRP0 = 0x10;
    public static final short SRP1 = 0x11;
    public static final short SRP2 = 0x12;
    public static final short SZP0 = 0x13;
    public static final short SZP1 = 0x14;
    public static final short SZP2 = 0x15;
    public static final short SZPS = 0x16;
    public static final short SLOOP = 0x17;
    public static final short RTG = 0x18;
    public static final short RTHG = 0x19;
    public static final short SMD = 0x1A;
    public static final short ELSE = 0x1B;
    public static final short JMPR = 0x1C;
    public static final short SCVTCI = 0x1D;
    public static final short SSWCI = 0x1E;
    public static final short SSW = 0x1F;
    public static final short DUP = 0x20;
    public static final short POP = 0x21;
    public static final short CLEAR = 0x22;
    public static final short SWAP = 0x23;
    public static final short DEPTH = 0x24;
    public static final short CINDEX = 0x25;
    public static final short MINDEX = 0x26;
    public static final short ALIGNPTS = 0x27;
    public static final short UTP = 0x29;
    public static final short LOOPCALL = 0x2A;
    public static final short CALL = 0x2B;
    public static final short FDEF = 0x2C;
    public static final short ENDF = 0x2D;
    public static final short MDAP = 0x2E;  // [a]
    public static final short IUP = 0x30;   // [a]
    public static final short SHP = 0x32;
    public static final short SHC = 0x34;   // [a]
    public static final short SHZ = 0x36;   // [a]
    public static final short SHPIX = 0x38;
    public static final short IP = 0x39;
    public static final short MSIRP = 0x3A; // [a]
    public static final short ALIGNRP = 0x3C;
    public static final short RTDG = 0x3D;
    public static final short MIAP = 0x3E;  // [a]
    public static final short NPUSHB = 0x40;
    public static final short NPUSHW = 0x41;
    public static final short WS = 0x42;
    public static final short RS = 0x43;
    public static final short WCVTP = 0x44;
    public static final short RCVT = 0x45;
    public static final short GC = 0x46;	// [a]
    public static final short SCFS = 0x48;
    public static final short MD = 0x49;	// [a]
    public static final short MPPEM = 0x4B;
    public static final short MPS = 0x4C;
    public static final short FLIPON = 0x4D;
    public static final short FLIPOFF = 0x4E;
    public static final short DEBUG = 0x4F;
    public static final short LT = 0x50;
    public static final short LTEQ = 0x51;
    public static final short GT = 0x52;
    public static final short GTEQ = 0x53;
    public static final short EQ = 0x54;
    public static final short NEQ = 0x55;
    public static final short ODD = 0x56;
    public static final short EVEN = 0x57;
    public static final short IF = 0x58;
    public static final short EIF = 0x59;
    public static final short AND = 0x5A;
    public static final short OR = 0x5B;
    public static final short NOT = 0x5C;
    public static final short DELTAP1 = 0x5D;
    public static final short SDB = 0x5E;
    public static final short SDS = 0x5F;
    public static final short ADD = 0x60;
    public static final short SUB = 0x61;
    public static final short DIV = 0x62;
    public static final short MUL = 0x63;
    public static final short ABS = 0x64;
    public static final short NEG = 0x65;
    public static final short FLOOR = 0x66;
    public static final short CEILING = 0x67;
    public static final short ROUND = 0x68;  // [ab]
    public static final short NROUND = 0x6C; // [ab]
    public static final short WCVTF = 0x70;
    public static final short DELTAP2 = 0x71;
    public static final short DELTAP3 = 0x72;
    public static final short DELTAC1 = 0x73;
    public static final short DELTAC2 = 0x74;
    public static final short DELTAC3 = 0x75;
    public static final short SROUND = 0x76;
    public static final short S45ROUND = 0x77;
    public static final short JROT = 0x78;
    public static final short JROF = 0x79;
    public static final short ROFF = 0x7A;
    public static final short RUTG = 0x7C;
    public static final short RDTG = 0x7D;
    public static final short SANGW = 0x7E;
    public static final short AA = 0x7F;
    public static final short FLIPPT = 0x80;
    public static final short FLIPRGON = 0x81;
    public static final short FLIPRGOFF = 0x82;
    public static final short SCANCTRL = 0x85;
    public static final short SDPVTL = 0x86; // [a]
    public static final short GETINFO = 0x88;
    public static final short IDEF = 0x89;
    public static final short ROLL = 0x8A;
    public static final short MAX = 0x8B;
    public static final short MIN = 0x8C;
    public static final short SCANTYPE = 0x8D;
    public static final short INSTCTRL = 0x8E;
    public static final short PUSHB = 0xB0; // [abc]
    public static final short PUSHW = 0xB8; // [abc]
    public static final short MDRP = 0xC0;  // [abcde]
    public static final short MIRP = 0xE0;  // [abcde]

    /**
     * Gets the mnemonic text for the specified opcode
     * @param opcode The opcode for which the mnemonic is required
     * @return The mnemonic, with a description
     */
    public static String getMnemonic(short opcode) {
        if (opcode >= MIRP) return "MIRP["+((opcode&16)==0?"nrp0,":"srp0,")+((opcode&8)==0?"nmd,":"md,")+((opcode&4)==0?"nrd,":"rd,")+(opcode&3)+"]";
        else if (opcode >= MDRP) return "MDRP["+((opcode&16)==0?"nrp0,":"srp0,")+((opcode&8)==0?"nmd,":"md,")+((opcode&4)==0?"nrd,":"rd,")+(opcode&3)+"]";
        else if (opcode >= PUSHW) return "PUSHW["+((opcode&7)+1)+"]";
        else if (opcode >= PUSHB) return "PUSHB["+((opcode&7)+1)+"]";
        else if (opcode >= INSTCTRL) return "INSTCTRL";
        else if (opcode >= SCANTYPE) return "SCANTYPE";
        else if (opcode >= MIN) return "MIN";
        else if (opcode >= MAX) return "MAX";
        else if (opcode >= ROLL) return "ROLL";
        else if (opcode >= IDEF) return "IDEF";
        else if (opcode >= GETINFO) return "GETINFO";
        else if (opcode >= SDPVTL) return "SDPVTL["+(opcode&1)+"]";
        else if (opcode >= SCANCTRL) return "SCANCTRL";
        else if (opcode >= FLIPRGOFF) return "FLIPRGOFF";
        else if (opcode >= FLIPRGON) return "FLIPRGON";
        else if (opcode >= FLIPPT) return "FLIPPT";
        else if (opcode >= AA) return "AA";
        else if (opcode >= SANGW) return "SANGW";
        else if (opcode >= RDTG) return "RDTG";
        else if (opcode >= RUTG) return "RUTG";
        else if (opcode >= ROFF) return "ROFF";
        else if (opcode >= JROF) return "JROF";
        else if (opcode >= JROT) return "JROT";
        else if (opcode >= S45ROUND) return "S45ROUND";
        else if (opcode >= SROUND) return "SROUND";
        else if (opcode >= DELTAC3) return "DELTAC3";
        else if (opcode >= DELTAC2) return "DELTAC2";
        else if (opcode >= DELTAC1) return "DELTAC1";
        else if (opcode >= DELTAP3) return "DELTAP3";
        else if (opcode >= DELTAP2) return "DELTAP2";
        else if (opcode >= WCVTF) return "WCVTF";
        else if (opcode >= NROUND) return "NROUND["+(opcode&3)+"]";
        else if (opcode >= ROUND) return "ROUND["+(opcode&3)+"]";
        else if (opcode >= CEILING) return "CEILING";
        else if (opcode >= FLOOR) return "FLOOR";
        else if (opcode >= NEG) return "NEG";
        else if (opcode >= ABS) return "ABS";
        else if (opcode >= MUL) return "MUL";
        else if (opcode >= DIV) return "DIV";
        else if (opcode >= SUB) return "SUB";
        else if (opcode >= ADD) return "ADD";
        else if (opcode >= SDS) return "SDS";
        else if (opcode >= SDB) return "SDB";
        else if (opcode >= DELTAP1) return "DELTAP1";
        else if (opcode >= NOT) return "NOT";
        else if (opcode >= OR) return "OR";
        else if (opcode >= AND) return "AND";
        else if (opcode >= EIF) return "EIF";
        else if (opcode >= IF) return "IF";
        else if (opcode >= EVEN) return "EVEN";
        else if (opcode >= ODD) return "ODD";
        else if (opcode >= NEQ) return "NEQ";
        else if (opcode >= EQ) return "EQ";
        else if (opcode >= GTEQ) return "GTEQ";
        else if (opcode >= GT) return "GT";
        else if (opcode >= LTEQ) return "LTEQ";
        else if (opcode >= LT) return "LT";
        else if (opcode >= DEBUG) return "DEBUG";
        else if (opcode >= FLIPOFF) return "FLIPOFF";
        else if (opcode >= FLIPON) return "FLIPON";
        else if (opcode >= MPS) return "MPS";
        else if (opcode >= MPPEM) return "MPPEM";
        else if (opcode >= MD) return "MD["+(opcode&1)+"]";
        else if (opcode >= SCFS) return "SCFS";
        else if (opcode >= GC) return "GC["+(opcode&1)+"]";
        else if (opcode >= RCVT) return "RCVT";
        else if (opcode >= WCVTP) return "WCVTP";
        else if (opcode >= RS) return "RS";
        else if (opcode >= WS) return "WS";
        else if (opcode >= NPUSHW) return "NPUSHW";
        else if (opcode >= NPUSHB) return "NPUSHB";
        else if (opcode >= MIAP) return "MIAP["+((opcode&1)==0?"nrd+nci":"rd+ci")+"]";
        else if (opcode >= RTDG) return "RTDG";
        else if (opcode >= ALIGNRP) return "ALIGNRP";
        else if (opcode >= MSIRP) return "MSIRP["+(opcode&1)+"]";
        else if (opcode >= IP) return "IP";
        else if (opcode >= SHPIX) return "SHPIX";
        else if (opcode >= SHZ) return "SHZ["+(opcode&1)+"]";
        else if (opcode >= SHC) return "SHC["+(opcode&1)+"]";
        else if (opcode >= SHP) return "SHP";
        else if (opcode >= IUP) return "IUP["+((opcode&1)==0?"y":"x")+"]";
        else if (opcode >= MDAP) return "MDAP["+((opcode&1)==0?"nrd":"rd")+"]";
        else if (opcode >= ENDF) return "ENDF";
        else if (opcode >= FDEF) return "FDEF";
        else if (opcode >= CALL) return "CALL";
        else if (opcode >= LOOPCALL) return "LOOPCALL";
        else if (opcode >= UTP) return "UTP";
        else if (opcode >= ALIGNPTS) return "ALIGNPTS";
        else if (opcode >= MINDEX) return "MINDEX";
        else if (opcode >= CINDEX) return "CINDEX";
        else if (opcode >= DEPTH) return "DEPTH";
        else if (opcode >= SWAP) return "SWAP";
        else if (opcode >= CLEAR) return "CLEAR";
        else if (opcode >= POP) return "POP";
        else if (opcode >= DUP) return "DUP";
        else if (opcode >= SSW) return "SSW";
        else if (opcode >= SSWCI) return "SSWCI";
        else if (opcode >= SCVTCI) return "SCVTCI";
        else if (opcode >= JMPR) return "JMPR";
        else if (opcode >= ELSE) return "ELSE";
        else if (opcode >= SMD) return "SMD";
        else if (opcode >= RTHG) return "RTHG";
        else if (opcode >= RTG) return "RTG";
        else if (opcode >= SLOOP) return "SLOOP";
        else if (opcode >= SZPS) return "SZPS";
        else if (opcode >= SZP2) return "SZP2";
        else if (opcode >= SZP1) return "SZP1";
        else if (opcode >= SZP0) return "SZP0";
        else if (opcode >= SRP2) return "SRP2";
        else if (opcode >= SRP1) return "SRP1";
        else if (opcode >= SRP0) return "SRP0";
        else if (opcode >= ISECT) return "ISECT";
        else if (opcode >= SFVTPV) return "SFVTPV";
        else if (opcode >= GFV) return "GFV";
        else if (opcode >= GPV) return "GPV";
        else if (opcode >= SFVFS) return "SFVFS";
        else if (opcode >= SPVFS) return "SPVFS";
        else if (opcode >= SFVTL) return "SFVTL["+((opcode&1)==0?"y-axis":"x-axis")+"]";
        else if (opcode >= SPVTL) return "SPVTL["+((opcode&1)==0?"y-axis":"x-axis")+"]";
        else if (opcode >= SFVTCA) return "SFVTCA["+((opcode&1)==0?"y-axis":"x-axis")+"]";
        else if (opcode >= SPVTCA) return "SPVTCA["+((opcode&1)==0?"y-axis":"x-axis")+"]";
        else if (opcode >= SVTCA) return "SVTCA["+((opcode&1)==0?"y-axis":"x-axis")+"]";
        else return "????";
    }

    public static String getComment(short opcode) {
        if (opcode >= MIRP) return "MIRP["+((opcode&16)==0?"nrp0,":"srp0,")+((opcode&8)==0?"nmd,":"md,")+((opcode&4)==0?"nrd,":"rd,")+(opcode&3)+"]\t\tMove Indirect Relative Point";
        else if (opcode >= MDRP) return "MDRP["+((opcode&16)==0?"nrp0,":"srp0,")+((opcode&8)==0?"nmd,":"md,")+((opcode&4)==0?"nrd,":"rd,")+(opcode&3)+"]\t\tMove Direct Relative Point";
        else if (opcode >= PUSHW) return "PUSHW["+((opcode&7)+1)+"]";
        else if (opcode >= PUSHB) return "PUSHB["+((opcode&7)+1)+"]";
        else if (opcode >= INSTCTRL) return "INSTCTRL\tINSTruction Execution ConTRol";
        else if (opcode >= SCANTYPE) return "SCANTYPE\tSCANTYPE";
        else if (opcode >= MIN) return "MIN\t\tMINimum of top two stack elements";
        else if (opcode >= MAX) return "MAX\t\tMAXimum of top two stack elements";
        else if (opcode >= ROLL) return "ROLL\t\tROLL the top three stack elements";
        else if (opcode >= IDEF) return "IDEF\t\tInstruction DEFinition";
        else if (opcode >= GETINFO) return "GETINFO\tGET INFOrmation";
        else if (opcode >= SDPVTL) return "SDPVTL["+(opcode&1)+"]\tSet Dual Projection_Vector To Line";
        else if (opcode >= SCANCTRL) return "SCANCTRL\tSCAN conversion ConTRoL";
        else if (opcode >= FLIPRGOFF) return "FLIPRGOFF\tFLIP RanGe OFF";
        else if (opcode >= FLIPRGON) return "FLIPRGON\tFLIP RanGe ON";
        else if (opcode >= FLIPPT) return "FLIPPT\tFLIP PoinT";
        else if (opcode >= AA) return "AA";
        else if (opcode >= SANGW) return "SANGW\t\tSet Angle _Weight";
        else if (opcode >= RDTG) return "RDTG\t\tRound Down To Grid";
        else if (opcode >= RUTG) return "RUTG\t\tRound Up To Grid";
        else if (opcode >= ROFF) return "ROFF\t\tRound OFF";
        else if (opcode >= JROF) return "JROF\t\tJump Relative On False";
        else if (opcode >= JROT) return "JROT\t\tJump Relative On True";
        else if (opcode >= S45ROUND) return "S45ROUND\tSuper ROUND 45 degrees";
        else if (opcode >= SROUND) return "SROUND\tSuper ROUND";
        else if (opcode >= DELTAC3) return "DELTAC3\tDELTA exception C3";
        else if (opcode >= DELTAC2) return "DELTAC2\tDELTA exception C2";
        else if (opcode >= DELTAC1) return "DELTAC1\tDELTA exception C1";
        else if (opcode >= DELTAP3) return "DELTAP3\tDELTA exception P3";
        else if (opcode >= DELTAP2) return "DELTAP2\tDELTA exception P2";
        else if (opcode >= WCVTF) return "WCVTF\t\tWrite Control Value Table in FUnits";
        else if (opcode >= NROUND) return "NROUND["+(opcode&3)+"]";
        else if (opcode >= ROUND) return "ROUND["+(opcode&3)+"]";
        else if (opcode >= CEILING) return "CEILING\tCEILING";
        else if (opcode >= FLOOR) return "FLOOR\t\tFLOOR";
        else if (opcode >= NEG) return "NEG\t\tNEGate";
        else if (opcode >= ABS) return "ABS\t\tABSolute value";
        else if (opcode >= MUL) return "MUL\t\tMULtiply";
        else if (opcode >= DIV) return "DIV\t\tDIVide";
        else if (opcode >= SUB) return "SUB\t\tSUBtract";
        else if (opcode >= ADD) return "ADD\t\tADD";
        else if (opcode >= SDS) return "SDS\t\tSet Delta_Shift in the graphics state";
        else if (opcode >= SDB) return "SDB\t\tSet Delta_Base in the graphics state";
        else if (opcode >= DELTAP1) return "DELTAP1\tDELTA exception P1";
        else if (opcode >= NOT) return "NOT\t\tlogical NOT";
        else if (opcode >= OR) return "OR\t\t\tlogical OR";
        else if (opcode >= AND) return "AND\t\tlogical AND";
        else if (opcode >= EIF) return "EIF\t\tEnd IF";
        else if (opcode >= IF) return "IF\t\t\tIF test";
        else if (opcode >= EVEN) return "EVEN";
        else if (opcode >= ODD) return "ODD";
        else if (opcode >= NEQ) return "NEQ\t\tNot EQual";
        else if (opcode >= EQ) return "EQ\t\t\tEQual";
        else if (opcode >= GTEQ) return "GTEQ\t\tGreater Than or Equal";
        else if (opcode >= GT) return "GT\t\t\tGreater Than";
        else if (opcode >= LTEQ) return "LTEQ\t\tLess Than or Equal";
        else if (opcode >= LT) return "LT\t\t\tLess Than";
        else if (opcode >= DEBUG) return "DEBUG";
        else if (opcode >= FLIPOFF) return "FLIPOFF\tSet the auto_flip Boolean to OFF";
        else if (opcode >= FLIPON) return "FLIPON\tSet the auto_flip Boolean to ON";
        else if (opcode >= MPS) return "MPS\t\tMeasure Point Size";
        else if (opcode >= MPPEM) return "MPPEM\t\tMeasure Pixels Per EM";
        else if (opcode >= MD) return "MD["+(opcode&1)+"]\t\t\tMeasure Distance";
        else if (opcode >= SCFS) return "SCFS\t\tSets Coordinate From the Stack using projection_vector and freedom_vector";
        else if (opcode >= GC) return "GC["+(opcode&1)+"]\t\t\tGet Coordinate projected onto the projection_vector";
        else if (opcode >= RCVT) return "RCVT\t\tRead Control Value Table";
        else if (opcode >= WCVTP) return "WCVTP\t\tWrite Control Value Table in Pixel units";
        else if (opcode >= RS) return "RS\t\t\tRead Store";
        else if (opcode >= WS) return "WS\t\t\tWrite Store";
        else if (opcode >= NPUSHW) return "NPUSHW";
        else if (opcode >= NPUSHB) return "NPUSHB";
        else if (opcode >= MIAP) return "MIAP["+((opcode&1)==0?"nrd+nci":"rd+ci")+"]\t\tMove Indirect Absolute Point";
        else if (opcode >= RTDG) return "RTDG\t\tRound To Double Grid";
        else if (opcode >= ALIGNRP) return "ALIGNRP\tALIGN Relative Point";
        else if (opcode >= MSIRP) return "MSIRP["+(opcode&1)+"]\t\tMove Stack Indirect Relative Point";
        else if (opcode >= IP) return "IP\t\t\tInterpolate Point by the last relative stretch";
        else if (opcode >= SHPIX) return "SHPIX\t\tSHift point by a PIXel amount";
        else if (opcode >= SHZ) return "SHZ["+(opcode&1)+"]\t\tSHift Zone by the last pt";
        else if (opcode >= SHC) return "SHC["+(opcode&1)+"]\t\tSHift Contour by the last point";
        else if (opcode >= SHP) return "SHP\t\tSHift Point by the last point";
        else if (opcode >= IUP) return "IUP["+((opcode&1)==0?"y":"x")+"]\t\tInterpolate Untouched Points through the outline";
        else if (opcode >= MDAP) return "MDAP["+((opcode&1)==0?"nrd":"rd")+"]\t\tMove Direct Absolute Point";
        else if (opcode >= ENDF) return "ENDF\t\tEND Function definition";
        else if (opcode >= FDEF) return "FDEF\t\tFunction DEFinition ";
        else if (opcode >= CALL) return "CALL\t\tCALL function";
        else if (opcode >= LOOPCALL) return "LOOPCALL\tLOOP and CALL function";
        else if (opcode >= UTP) return "UTP\t\tUnTouch Point";
        else if (opcode >= ALIGNPTS) return "ALIGNPTS\tALIGN Points";
        else if (opcode >= MINDEX) return "MINDEX\tMove the INDEXed element to the top of the stack";
        else if (opcode >= CINDEX) return "CINDEX\tCopy the INDEXed element to the top of the stack";
        else if (opcode >= DEPTH) return "DEPTH\t\tReturns the DEPTH of the stack";
        else if (opcode >= SWAP) return "SWAP\t\tSWAP the top two elements on the stack";
        else if (opcode >= CLEAR) return "CLEAR\t\tClear the entire stack";
        else if (opcode >= POP) return "POP\t\tPOP top stack element";
        else if (opcode >= DUP) return "DUP\t\tDuplicate top stack element";
        else if (opcode >= SSW) return "SSW\t\tSet Single-width";
        else if (opcode >= SSWCI) return "SSWCI\t\tSet Single_Width_Cut_In";
        else if (opcode >= SCVTCI) return "SCVTCI\tSet Control Value Table Cut In";
        else if (opcode >= JMPR) return "JMPR\t\tJuMP";
        else if (opcode >= ELSE) return "ELSE";
        else if (opcode >= SMD) return "SMD\t\tSet Minimum_ Distance";
        else if (opcode >= RTHG) return "RTHG\t\tRound To Half Grid";
        else if (opcode >= RTG) return "RTG\t\tRound To Grid";
        else if (opcode >= SLOOP) return "SLOOP\t\tSet LOOP variable";
        else if (opcode >= SZPS) return "SZPS\t\tSet Zone PointerS";
        else if (opcode >= SZP2) return "SZP2\t\tSet Zone Pointer 2";
        else if (opcode >= SZP1) return "SZP1\t\tSet Zone Pointer 1";
        else if (opcode >= SZP0) return "SZP0\t\tSet Zone Pointer 0";
        else if (opcode >= SRP2) return "SRP2\t\tSet Reference Point 2";
        else if (opcode >= SRP1) return "SRP1\t\tSet Reference Point 1";
        else if (opcode >= SRP0) return "SRP0\t\tSet Reference Point 0";
        else if (opcode >= ISECT) return "ISECT\t\tmoves point p to the InterSECTion of two lines";
        else if (opcode >= SFVTPV) return "SFVTPV\tSet Freedom_Vector To Projection Vector";
        else if (opcode >= GFV) return "GFV\t\tGet Freedom_Vector";
        else if (opcode >= GPV) return "GPV\t\tGet Projection_Vector";
        else if (opcode >= SFVFS) return "SFVFS\t\tSet Freedom_Vector From Stack";
        else if (opcode >= SPVFS) return "SPVFS\t\tSet Projection_Vector From Stack";
        else if (opcode >= SFVTL) return "SFVTL["+((opcode&1)==0?"y-axis":"x-axis")+"]\t\tSet Freedom_Vector To Line";
        else if (opcode >= SPVTL) return "SPVTL["+((opcode&1)==0?"y-axis":"x-axis")+"]\t\tSet Projection_Vector To Line";
        else if (opcode >= SFVTCA) return "SFVTCA["+((opcode&1)==0?"y-axis":"x-axis")+"]\tSet Freedom_Vector to Coordinate Axis";
        else if (opcode >= SPVTCA) return "SPVTCA["+((opcode&1)==0?"y-axis":"x-axis")+"]\tSet Projection_Vector To Coordinate Axis";
        else if (opcode >= SVTCA) return "SVTCA["+((opcode&1)==0?"y-axis":"x-axis")+"]\t\tSet freedom and projection Vectors To Coordinate Axis";
        else return "????";
    }
}
