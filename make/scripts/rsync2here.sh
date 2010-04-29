#! /bin/sh

# USESSH="-e ssh"

SOURCE=sven@192.168.0.52::PROJECTS
#DEST=/usr/local/projects/JOGL
DEST=/cygdrive/c/JOGL

function my_rsync()
{
    # rsync $USESSH -av --perms --delete-after --exclude 'build*/' $SOURCE/JOGL/$1 $2
    rsync $USESSH -av --perms --delete-after --exclude 'build*/' $SOURCE/JOGL/$1 $2
}

function do_rsync()
{
    my_rsync gluegen            $DEST/
    my_rsync jogl               $DEST/
    my_rsync jogl-demos         $DEST/
    my_rsync lib                $DEST/
    my_rsync lib-windows-x86    $DEST/
    my_rsync lib-linux-x86      $DEST/
    my_rsync lib-linux-x86_64   $DEST/
    my_rsync setenv*            $DEST/
}

do_rsync 2>&1 | tee -a rsync.log

