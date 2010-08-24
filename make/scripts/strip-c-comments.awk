#!/usr/bin/awk

BEGIN { ORS = "" ; C99=1 }

{ code = code $0 "\n" }

END {
    while ( length(code) )
    code = process( code )
}

function process( text )
{
    if ( C99 ) { 
        if ( match( text, /"|'|\/\*|\/\// ) ) {
            return span( text )
        }
    } else if ( match( text, /"|'|\/\*/ ) ) {
        return span( text )
    }
    print text
    return ""
}

function span( text , starter )
{
    print substr( text, 1, RSTART - 1 )
    starter = substr( text, RSTART, RLENGTH )
    text = substr( text, RSTART + RLENGTH )

    if ( "\"" == starter || "'" == starter ) {
        return quoted( text, starter )
    }
    if ( "//" == starter ) {
        return remove( text, "\n", "\n" )
    }
    ## Allow for
    ## /* foo *\
    ## /
    return remove( text, "\\*(\\\\\n)?/", " " )
}

function remove( text, ender, replacement )
{
    print replacement
    return substr( text, match(text, ender) + RLENGTH )
}

function quoted( text, starter )
{
    if ( "'" == starter ) {
        match( text, /^(\\.|[^'])*'/ )
    } else {
        match( text, /^(\\.|\?\?\/.|[^"])*"/ )
    }
    print starter substr( text, 1, RLENGTH )
    return substr( text, RSTART + RLENGTH )
}

