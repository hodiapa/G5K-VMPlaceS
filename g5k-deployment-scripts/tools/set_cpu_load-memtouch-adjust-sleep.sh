#!/bin/bash

CPU_CALIB=$1
MEM_CALIB=$2
MEM_SCRATCHFILE_SIZE=$3
LOAD=$4
DP_RATE=$5



	echo "Usage= set_cpu_load.sh CPU_CALIB MEM_CALIB LOAD DP_RATE"
	echo "setting cpu load to $LOAD with DP_RATE ($DP_RATE in MB/S) "

        # find the memtouch processes
	memtouch_pid=$(ps aux | grep "memtouch-adjust-sleep" | grep -v "grep" | awk '{print $2}')

	if [ "$memtouch_pid" != "" ]; then

		# cleaning the running cpulimit processes
		echo "memtouch process is running: pid=$memtouch_pid"
		kill $memtouch_pid

	else
		echo "cannot find a running memtouch process."
	fi
	nohup memtouch-adjust-sleep --cmd-makeload --cpu-speed $CPU_CALIB --mem-speed $MEM_CALIB $MEM_SCRATCHFILE_SIZE $DP_RATE --cpu-bound $LOAD --mempath /dev/shm/memtouch --no-fill-random >/dev/null 2>&1 &

exit 0
