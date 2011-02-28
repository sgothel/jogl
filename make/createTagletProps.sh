root=http://www.opengl.org/sdk/docs/

createProperties() {

    toc=index.html
    doc=${1}

    #download index
    wget ${root}/${doc}/xhtml/${toc};

    #find lines with links to gl* function doc
    grep -E .+\<a\ target=\"pagedisp\"\ href=\"gl[A-Z][^\"]+\"\>gl[A-Z][a-Z0-9]+\</a\>.+ ./${toc} > links;

    #add all links as properties to file and cleanup
    sed -r "s/.+<td><a target=\"pagedisp\" href=\"([a-Z0-9.]+)\">([a-Z0-9]+)<\/a><\/td>.*/\2 = ${doc}\/xhtml\/\1/" links | sort -u;

    rm ./${toc} ./links

}

>tmp

createProperties man | sed -e "s/man\//man2\//ig" >> tmp
createProperties man3 >> tmp
createProperties man4 >> tmp

#add doc root to properties file
echo "#Generated, do not edit, edit createTagletProps.sh instead.
#This file is used in NativeTaglet and maps the generated method names
#to the function specific OpenGL documentation man pages.
nativetaglet.baseUrl=${root}" > native-taglet.properties;

cat tmp | sort -k1,2 -r | awk '!x[$1]++' | tr -d [:blank:] | sort | sed -e "s/man2\//man\//ig" >> native-taglet.properties;
rm tmp
