#!/bin/bash

echo "Building stresser for G5K experiments"

cd cpulimit-master
make clean; make all
cd ..

cd memtouch
make clean; make all
cd ..

mkdir bin

cp cpulimit-master/src/cpulimit bin/cpulimit
cp memtouch/memtouch-with-busyloop3 bin/memtouch-with-busyloop3
cp memtouch/memtouch-adjust-sleep bin/memtouch-adjust-sleep
cp set_cpu_load.rb bin/set_cpu_load.rb
cp set_cpu_load.sh bin/set_cpu_load.sh
cp init_cpu_load.sh bin/init_cpu_load.sh

cd cpulimit-master
make clean
cd ..

cd memtouch
make clean
cd ..

echo "Done!"
