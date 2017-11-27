#!/bin/bash

i=0

while true
do
echo Generating file $i...
touch file_$i.csv
i=$((i + 1))
sleep 10
done
