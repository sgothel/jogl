# ./scripts/make.jogl.all.linux-x86_64.sh && \
./scripts/cmpOld2New.sh
sh ./scripts/cmpOld2NewDups.sh
ls -la cmp-old2new/*.dups
# ls -l ../build-old/jogl/gensrc/classes/com/jogamp/opengl/GL*
# ls -l ../build/jogl/gensrc/classes/com/jogamp/opengl/GL*
du -ksc --apparent-size ../build-old/jogl/gensrc/classes/com/jogamp/opengl/GL*
du -ksc --apparent-size ../build/jogl/gensrc/classes/com/jogamp/opengl/GL*
# diff -Nur /usr/local/projects/JogAmp/temp/jogl/make/cmp-old2new/GL2ES2Files.dups cmp-old2new/GL2ES2Files.dups | less
# diff -Nur /usr/local/projects/JogAmp/temp/jogl/make/cmp-old2new/GL3ES3-GL2GL3.dups cmp-old2new/GL3ES3-GL2GL3.dups | less
# grep -l LALA ../build/jogl/gensrc/classes/com/jogamp/opengl/GL*
