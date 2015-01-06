#!/bin/bash

if [ "$1" == "--load" ]
then

	targeted_cpu_load=$2

	echo "setting cpu load to $targeted_cpu_load of one core"

	# executing memtouch to: 
	#   * reload ram pages
	#   * to simulate a cpu load of 100% of one core
	#

	
        # find the memtouch processes
	memtouch_pid=$(ps aux | grep "memtouch-with-busyloop3" | grep -v "grep" | awk '{print $2}')

	if [ "$memtouch_pid" != "" ]; then

		# cleaning the running cpulimit processes
		ps aux| grep "cpulimit" | grep -v "grep" | awk '{print $2}' | xargs -r kill -9

		echo "memtouch process is running: pid=$memtouch_pid, the new targeted cpu load is $targeted_cpu_load% of one core."
		nohup cpulimit -l $targeted_cpu_load -p $memtouch_pid > /dev/null 2>&1 &
	else
		echo "cannot find a running memtouch process."
	fi
else
	echo "Please specify a targeted cpu load with --load <cpu_load>"
fi

exit 0