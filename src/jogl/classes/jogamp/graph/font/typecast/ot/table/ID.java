/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2016 David Schweinsberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jogamp.graph.font.typecast.ot.table;

/**
 *
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public abstract class ID {

    // Platform IDs
    public static final short platformUnicode = 0;
    public static final short platformMacintosh = 1;
    public static final short platformISO = 2;
    public static final short platformMicrosoft = 3;

    // Unicode Encoding IDs
    private static final short encodingUnicode10Semantics = 0;
    private static final short encodingUnicode11Semantics = 1;
    private static final short encodingISO10646Semantics = 2;
    private static final short encodingUnicode20SemanticsBMPOnly = 3;
    private static final short encodingUnicode20Semantics = 4;
    private static final short encodingUnicodeVariationSequences = 5;
    private static final short encodingFullUnicodeCoverage = 6;
    
    // Microsoft Encoding IDs
//    public static final short encodingUndefined = 0;
//    public static final short encodingUGL = 1;
    public static final short encodingSymbol = 0;
    public static final short encodingUnicode = 1;
    private static final short encodingShiftJIS = 2;
    private static final short encodingPRC = 3;
    private static final short encodingBig5 = 4;
    private static final short encodingWansung = 5;
    private static final short encodingJohab = 6;
    private static final short encodingUCS4 = 10;

    // Macintosh Encoding IDs
    public static final short encodingRoman = 0;
    private static final short encodingJapanese = 1;
    private static final short encodingChinese = 2;
    private static final short encodingKorean = 3;
    private static final short encodingArabic = 4;
    private static final short encodingHebrew = 5;
    private static final short encodingGreek = 6;
    private static final short encodingRussian = 7;
    private static final short encodingRSymbol = 8;
    private static final short encodingDevanagari = 9;
    private static final short encodingGurmukhi = 10;
    private static final short encodingGujarati = 11;
    private static final short encodingOriya = 12;
    private static final short encodingBengali = 13;
    private static final short encodingTamil = 14;
    private static final short encodingTelugu = 15;
    private static final short encodingKannada = 16;
    private static final short encodingMalayalam = 17;
    private static final short encodingSinhalese = 18;
    private static final short encodingBurmese = 19;
    private static final short encodingKhmer = 20;
    private static final short encodingThai = 21;
    private static final short encodingLaotian = 22;
    private static final short encodingGeorgian = 23;
    private static final short encodingArmenian = 24;
    private static final short encodingMaldivian = 25;
    private static final short encodingTibetan = 26;
    private static final short encodingMongolian = 27;
    private static final short encodingGeez = 28;
    private static final short encodingSlavic = 29;
    private static final short encodingVietnamese = 30;
    private static final short encodingSindhi = 31;
    private static final short encodingUninterp = 32;

    // ISO Encoding IDs
    private static final short encodingASCII = 0;
    private static final short encodingISO10646 = 1;
    private static final short encodingISO8859_1 = 2;

    // Microsoft Language IDs
    private static final short languageSQI = 0x041c;
    private static final short languageEUQ = 0x042d;
    private static final short languageBEL = 0x0423;
    private static final short languageBGR = 0x0402;
    private static final short languageCAT = 0x0403;
    private static final short languageSHL = 0x041a;
    private static final short languageCSY = 0x0405;
    private static final short languageDAN = 0x0406;
    private static final short languageNLD = 0x0413;
    private static final short languageNLB = 0x0813;
    private static final short languageENU = 0x0409;
    private static final short languageENG = 0x0809;
    private static final short languageENA = 0x0c09;
    private static final short languageENC = 0x1009;
    private static final short languageENZ = 0x1409;
    private static final short languageENI = 0x1809;
    private static final short languageETI = 0x0425;
    private static final short languageFIN = 0x040b;
    private static final short languageFRA = 0x040c;
    private static final short languageFRB = 0x080c;
    private static final short languageFRC = 0x0c0c;
    private static final short languageFRS = 0x100c;
    private static final short languageFRL = 0x140c;
    private static final short languageDEU = 0x0407;
    private static final short languageDES = 0x0807;
    private static final short languageDEA = 0x0c07;
    private static final short languageDEL = 0x1007;
    private static final short languageDEC = 0x1407;
    private static final short languageELL = 0x0408;
    private static final short languageHUN = 0x040e;
    private static final short languageISL = 0x040f;
    private static final short languageITA = 0x0410;
    private static final short languageITS = 0x0810;
    private static final short languageLVI = 0x0426;
    private static final short languageLTH = 0x0427;
    private static final short languageNOR = 0x0414;
    private static final short languageNON = 0x0814;
    private static final short languagePLK = 0x0415;
    private static final short languagePTB = 0x0416;
    private static final short languagePTG = 0x0816;
    private static final short languageROM = 0x0418;
    private static final short languageRUS = 0x0419;
    private static final short languageSKY = 0x041b;
    private static final short languageSLV = 0x0424;
    private static final short languageESP = 0x040a;
    private static final short languageESM = 0x080a;
    private static final short languageESN = 0x0c0a;
    private static final short languageSVE = 0x041d;
    private static final short languageTRK = 0x041f;
    private static final short languageUKR = 0x0422;

    // Macintosh Language IDs
    private static final short languageEnglish = 0;
    private static final short languageFrench = 1;
    private static final short languageGerman = 2;
    private static final short languageItalian = 3;
    private static final short languageDutch = 4;
    private static final short languageSwedish = 5;
    private static final short languageSpanish = 6;
    private static final short languageDanish = 7;
    private static final short languagePortuguese = 8;
    private static final short languageNorwegian = 9;
    private static final short languageHebrew = 10;
    private static final short languageJapanese = 11;
    private static final short languageArabic = 12;
    private static final short languageFinnish = 13;
    private static final short languageGreek = 14;
    private static final short languageIcelandic = 15;
    private static final short languageMaltese = 16;
    private static final short languageTurkish = 17;
    private static final short languageYugoslavian = 18;
    private static final short languageChinese = 19;
    private static final short languageUrdu = 20;
    private static final short languageHindi = 21;
    private static final short languageThai = 22;

    // Name IDs
    private static final short nameCopyrightNotice = 0;
    public static final short nameFontFamilyName = 1;
    private static final short nameFontSubfamilyName = 2;
    private static final short nameUniqueFontIdentifier = 3;
    private static final short nameFullFontName = 4;
    private static final short nameVersionString = 5;
    private static final short namePostscriptName = 6;
    private static final short nameTrademark = 7;
    private static final short nameManufacturerName = 8;
    private static final short nameDesigner = 9;
    private static final short nameDescription = 10;
    private static final short nameURLVendor = 11;
    private static final short nameURLDesigner = 12;
    private static final short nameLicenseDescription = 13;
    private static final short nameLicenseInfoURL = 14;
    private static final short namePreferredFamily = 16;
    private static final short namePreferredSubfamily = 17;
    private static final short nameCompatibleFull = 18;
    private static final short nameSampleText = 19;
    private static final short namePostScriptCIDFindfontName = 20;

    public static String getPlatformName(short platformId) {
        switch (platformId) {
            case platformUnicode:   return "Unicode";
            case platformMacintosh: return "Macintosh";
            case platformISO:       return "ISO";
            case platformMicrosoft: return "Microsoft";
            default:                return "Custom";
        }
    }

    public static String getEncodingName(short platformId, short encodingId) {

        if (platformId == platformUnicode) {
            
            // Unicode specific encodings
            switch (encodingId) {
                case encodingUnicode10Semantics:        return "Unicode 1.0 semantics";
                case encodingUnicode11Semantics:        return "Unicode 1.1 semantics";
                case encodingISO10646Semantics:         return "ISO 10646:1993 semantics";
                case encodingUnicode20SemanticsBMPOnly: return "Unicode 2.0 and onwards semantics (BMP only)";
                case encodingUnicode20Semantics:        return "Unicode 2.0 and onwards semantics (non-BMP allowed)";
                case encodingUnicodeVariationSequences: return "Unicode Variation Sequences";
                case encodingFullUnicodeCoverage:       return "Full Unicode coverage";
                default:                                return "";
            }

        } else if (platformId == platformMacintosh) {

            // Macintosh specific encodings
            switch (encodingId) {
                case encodingRoman:      return "Roman";
                case encodingJapanese:   return "Japanese";
                case encodingChinese:    return "Chinese";
                case encodingKorean:     return "Korean";
                case encodingArabic:     return "Arabi";
                case encodingHebrew:     return "Hebrew";
                case encodingGreek:      return "Greek";
                case encodingRussian:    return "Russian";
                case encodingRSymbol:    return "RSymbol";
                case encodingDevanagari: return "Devanagari";
                case encodingGurmukhi:   return "Gurmukhi";
                case encodingGujarati:   return "Gujarati";
                case encodingOriya:      return "Oriya";
                case encodingBengali:    return "Bengali";
                case encodingTamil:      return "Tamil";
                case encodingTelugu:     return "Telugu";
                case encodingKannada:    return "Kannada";
                case encodingMalayalam:  return "Malayalam";
                case encodingSinhalese:  return "Sinhalese";
                case encodingBurmese:    return "Burmese";
                case encodingKhmer:      return "Khmer";
                case encodingThai:       return "Thai";
                case encodingLaotian:    return "Laotian";
                case encodingGeorgian:   return "Georgian";
                case encodingArmenian:   return "Armenian";
                case encodingMaldivian:  return "Maldivian";
                case encodingTibetan:    return "Tibetan";
                case encodingMongolian:  return "Mongolian";
                case encodingGeez:       return "Geez";
                case encodingSlavic:     return "Slavic";
                case encodingVietnamese: return "Vietnamese";
                case encodingSindhi:     return "Sindhi";
                case encodingUninterp:   return "Uninterpreted";
                default:                 return "";
            }

        } else if (platformId == platformISO) {

            // ISO specific encodings
            switch (encodingId) {
                case encodingASCII:     return "7-bit ASCII";
                case encodingISO10646:  return "ISO 10646";
                case encodingISO8859_1: return "ISO 8859-1";
                default:                return "";
            }

        } else if (platformId == platformMicrosoft) {

            // Windows specific encodings
            switch (encodingId) {
                case encodingSymbol:   return "Symbol";
                case encodingUnicode:  return "Unicode";
                case encodingShiftJIS: return "ShiftJIS";
                case encodingPRC:      return "PRC";
                case encodingBig5:     return "Big5";
                case encodingWansung:  return "Wansung";
                case encodingJohab:    return "Johab";
                case 7:                return "Reserved";
                case 8:                return "Reserved";
                case 9:                return "Reserved";
                case encodingUCS4:     return "UCS-4";
                default:               return "";
            }
        }
        return "";
    }

    public static String getLanguageName(short platformId, short languageId) {

        if (platformId == platformMacintosh) {
            switch (languageId) {
                case languageEnglish: return "English";
                case languageFrench: return "French";
                case languageGerman:  return "German";
                case languageItalian: return "Italian";
                case languageDutch: return "Dutch";
                case languageSwedish: return "Swedish";
                case languageSpanish: return "Spanish";
                case languageDanish: return "Danish";
                case languagePortuguese: return "Portuguese";
                case languageNorwegian: return "Norwegian";
                case languageHebrew: return "Hebrew";
                case languageJapanese: return "Japanese";
                case languageArabic: return "Arabic";
                case languageFinnish: return "Finnish";
                case languageGreek: return "Greek";
                case languageIcelandic: return "Icelandic";
                case languageMaltese: return "Maltese";
                case languageTurkish: return "Turkish";
                case languageYugoslavian: return "Yugoslavian";
                case languageChinese: return "Chinese";
                case languageUrdu: return "Urdu";
                case languageHindi: return "Hindi";
                case languageThai: return "Thai";
                default: return "";
            }
        } else if (platformId == platformMicrosoft) {
            switch (languageId) {
                case languageSQI: return "Albanian (Albania)";
                case languageEUQ: return "Basque (Basque)";
                case languageBEL: return "Byelorussian (Byelorussia)";
                case languageBGR: return "Bulgarian (Bulgaria)";
                case languageCAT: return "Catalan (Catalan)";
                case languageSHL: return "Croatian (Croatian)";
                case languageCSY: return "Czech (Czech)";
                case languageDAN: return "Danish (Danish)";
                case languageNLD: return "Dutch (Dutch (Standard))";
                case languageNLB: return "Dutch (Belgian (Flemish))";
                case languageENU: return "English (American)";
                case languageENG: return "English (British)";
                case languageENA: return "English (Australian)";
                case languageENC: return "English (Canadian)";
                case languageENZ: return "English (New Zealand)";
                case languageENI: return "English (Ireland)";
                case languageETI: return "Estonian (Estonia)";
                case languageFIN: return "Finnish (Finnish)";
                case languageFRA: return "French (French (Standard))";
                case languageFRB: return "French (Belgian)";
                case languageFRC: return "French (Canadian)";
                case languageFRS: return "French (Swiss)";
                case languageFRL: return "French (Luxembourg)";
                case languageDEU: return "German (German (Standard))";
                case languageDES: return "German (Swiss)";
                case languageDEA: return "German (Austrian)";
                case languageDEL: return "German (Luxembourg)";
                case languageDEC: return "German (Liechtenstein)";
                case languageELL: return "Greek (Greek)";
                case languageHUN: return "Hungarian (Hungarian)";
                case languageISL: return "Icelandic (Icelandic)";
                case languageITA: return "Italian (Italian (Standard))";
                case languageITS: return "Italian (Swiss)";
                case languageLVI: return "Latvian (Latvia)";
                case languageLTH: return "Lithuanian (Lithuania)";
                case languageNOR: return "Norwegian (Norwegian (Bokmal))";
                case languageNON: return "Norwegian (Norwegian (Nynorsk))";
                case languagePLK: return "Polish (Polish)";
                case languagePTB: return "Portuguese (Portuguese (Brazilian))";
                case languagePTG: return "Portuguese (Portuguese (Standard))";
                case languageROM: return "Romanian (Romania)";
                case languageRUS: return "Russian (Russian)";
                case languageSKY: return "Slovak (Slovak)";
                case languageSLV: return "Slovenian (Slovenia)";
                case languageESP: return "Spanish (Spanish (Traditional Sort))";
                case languageESM: return "Spanish (Mexican)";
                case languageESN: return "Spanish (Spanish (Modern Sort))";
                case languageSVE: return "Swedish (Swedish)";
                case languageTRK: return "Turkish (Turkish)";
                case languageUKR: return "Ukrainian (Ukraine)";
                default: return "";
            }
        }
        return "";
    }

    public static String getNameName(short nameId) {
        switch (nameId) {
            case nameCopyrightNotice: return "Copyright notice";
            case nameFontFamilyName: return "Font Family name";
            case nameFontSubfamilyName: return "Font Subfamily name";
            case nameUniqueFontIdentifier: return "Unique font identifier";
            case nameFullFontName: return "Full font name";
            case nameVersionString: return "Version string";
            case namePostscriptName: return "Postscript name";
            case nameTrademark: return "Trademark";
            case nameManufacturerName: return "Manufacturer Name";
            case nameDesigner: return "Designer";
            case nameDescription: return "Description";
            case nameURLVendor: return "URL Vendor";
            case nameURLDesigner: return "URL Designer";
            case nameLicenseDescription: return "License Description";
            case nameLicenseInfoURL: return "License Info URL";
            case namePreferredFamily: return "Preferred Family";
            case namePreferredSubfamily: return "Preferred Subfamily";
            case nameCompatibleFull: return "Compatible Full";
            case nameSampleText: return "Sample text";
            case namePostScriptCIDFindfontName: return "PostScript CID findfont name";
            default: return "";
        }
    }
}
