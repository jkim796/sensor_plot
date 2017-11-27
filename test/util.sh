#!/bin/bash

pusher_test_dir=~/pusher_test

function cp_csv() {
    if [ -z $1 ]; then
	interval=10
    else
	interval=$1
    fi

    csvfiles=($(ls ~/Downloads/*.csv))
    for f in ${csvfiles[@]}; do
	cp ${f} ${pusher_test_dir}
	echo [INFO] Copied ${f} to ${pusher_test_dir}
	echo [INFO] Sleeping for ${interval}...
	sleep ${interval}
    done
    echo [INFO] Copied all CSV files!
}
