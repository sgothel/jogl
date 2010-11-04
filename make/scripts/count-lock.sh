
echo lock
grep -RI "lock()" ../src/ | wc
echo unlock
grep -RI "unlock()" ../src/ | wc
echo
echo

for i in `grep -RIl "unlock()" ../src/` ; do
    echo $i
    echo lock
    grep -RI "lock()" $i | wc
    echo unlock
    grep -RI "unlock()" $i | wc
    echo
    echo
done
