#!/bin/bash

currdir=~/pusher_test
inotifywait -m -e create --format '%w%f' ${currdir} | while read NEWFILE
do
echo file ${NEWFILE} has been created!
NEWFILE=$(basename ${NEWFILE})
python test.py ${NEWFILE}
done
