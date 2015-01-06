


import sys
import os
import shlex
import subprocess
import time

sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)
sys.stderr = os.fdopen(sys.stderr.fileno(), 'w', 0)


port = int(sys.argv[1])
key_rx = "__bw_mon:%d:rx" % port
key_tx = "__bw_mon:%d:tx" % port


cmd_rx = "iptables -t filter -I INPUT  -p tcp --dport %s -m comment --comment '%s'" % (port, key_rx)
cmd_tx = "iptables -t filter -I OUTPUT -p tcp --sport %s -m comment --comment '%s'" % (port, key_tx)

os.system(cmd_rx)
os.system(cmd_tx)


try:
	prev_assigned = False

	while True:
		cmd = "/sbin/iptables -t filter -nL -v -x"
		pr = subprocess.Popen(shlex.split(cmd), stdout=subprocess.PIPE, close_fds=True)
		(stdout_data, stderr_data) = pr.communicate()
		for line in stdout_data.split("\n"):
			if line.find(key_rx) > 0:
				chunks = line.split()
				rx_pkts, rx_bytes = chunks[0], chunks[1]
			elif line.find(key_tx) > 0: 
				chunks = line.split()
				tx_pkts, tx_bytes = chunks[0], chunks[1]

		pr.wait()

		rx_bytes = int(rx_bytes)
		tx_bytes = int(tx_bytes)
		rx_pkts  = int(rx_pkts)
		tx_pkts  = int(tx_pkts)

		#print 'Debug: ', int(time.time()), rx_bytes, tx_bytes
		#print 'Debug: ', int(time.time()), rx_bytes_prev, tx_bytes_prev

		#print prev_assigned

		if prev_assigned:
			# print int(time.time()), rx_bytes, tx_bytes
			recv_bytes = rx_bytes - rx_bytes_prev
			sent_bytes = tx_bytes - tx_bytes_prev
			print int(time.time()), recv_bytes, sent_bytes

			recv_pkts = rx_pkts - rx_pkts_prev
			sent_pkts = tx_pkts - tx_pkts_prev
			#print int(time.time()), recv_pkts, sent_pkts

			# Note: consider the size of ethernet header ?
			#print int(time.time()), recv_bytes + 14 * recv_pkts, sent_bytes + 14 * sent_pkts

		else:
			print "# epoch rx tx"

		rx_bytes_prev = rx_bytes
		tx_bytes_prev = tx_bytes
		rx_pkts_prev  = rx_pkts
		tx_pkts_prev  = tx_pkts
		prev_assigned = True

		time.sleep(1)

finally:
	cmd_rx = "iptables -t filter -D INPUT  -p tcp --dport %s -m comment --comment '%s'" % (port, key_rx)
	cmd_tx = "iptables -t filter -D OUTPUT -p tcp --sport %s -m comment --comment '%s'" % (port, key_tx)

	os.system(cmd_rx)
	os.system(cmd_tx)
