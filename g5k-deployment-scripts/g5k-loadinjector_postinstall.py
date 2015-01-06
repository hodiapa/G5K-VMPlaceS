#!/usr/bin/env python
from execo import TaktukRemote, TaktukPut, default_connection_params, logger, Process
from execo_g5k import get_host_site
from vm5k import destroy_vms
import sys
from time import sleep 
from vm5k.services.dnsmasq import get_server_ip
 
# use root to connect on the host
default_connection_params['user'] = 'root'
# retrieve the list of hosts from the file
hosts = [line.strip() for line in open('hosts.list')]

## Configure Host OSes
logger.info('Configure HostOSes')
## Install missing packages
logger.info('| - Install Packages')
## Use specific debian snapshots
test = TaktukPut(hosts, ['/home/alebre/g5k-loadinjector/sources.list' ], remote_location='/etc/apt').run()
for p in test.processes:
	if not p.ok:
		print p.host
		print p.stdout
		print p.stderr
# Wait 5 seconds to guarantee that the new sources.list has been correctly copied on each node.
sleep(5)

install_packages = TaktukRemote('export DEBIAN_MASTER=noninteractive ;  export https_proxy="https://proxy:3128"; apt-get -o Acquire::Check-Valid-Until=false update && apt-get install -y --force-yes libxml-xpath-perl libsys-virt-perl uuid-runtime cpufrequtils kanif -o Acquire::Check-Valid-Until=false -o Dpkgtions::="--force-confdef" -o Dpkgtions::="--force-confold"', hosts).run()
## Normal repo
#install_packages = TaktukRemote('export DEBIAN_MASTER=noninteractive ; apt-get update && apt-get install -y --force-yes libxml-xpath-perl libsys-virt-perl uuid-runtime -o Dpkgtions::="--force-confdef" -o Dpkgtions::="--force-confold"', hosts).run()
check_install = TaktukRemote('dpkg --configure -a --force-confdef --force-confold', hosts).run()

logger.info('| - Copy scripts')
# Deployment of migrate_vm.sh and get-ip.pl scripts.
test = TaktukPut(hosts, ['/home/alebre/g5k-loadinjector/migrate_vm.sh', '/home/alebre/g5k-loadinjector/get-ip.pl' ], remote_location='/bin').run()
for p in test.processes:
	if not p.ok:
		print p.host
		print p.stdout
		print p.stderr

test = TaktukPut(hosts, ['/home/alebre/g5k-loadinjector/cpufrequtils' ], remote_location='/etc/init.d').run()
for p in test.processes:
	if not p.ok:
		print p.host
		print p.stdout
		print p.stderr

logger.info('| - Set correct rights')
cmd = 'chmod +x /bin/migrate_vm.sh ; chmod +x /bin/get-ip.pl ; ' + \
'tc qdisc add dev eth0 root sfq perturb 10 ; ' + \
'tc qdisc add dev br0 root sfq perturb 10 ; ' + \
'/etc/init.d/cpufrequtils restart' 
test = TaktukRemote(cmd, hosts).run()
for p in test.processes:
	if not p.ok:
		print p.host
		print p.stdout
		print p.stderr

logger.info('| - Create and copy /etc/host')
# creation du fichier
f = open('/tmp/etc_hosts', 'w')
for host in hosts:
        ip = get_server_ip(host)
        f.write(ip + '\t' + host + '\n')
vms = [line.strip().split('\t') for line in open('vms.list')]
for vm in vms:
        f.write(vm[0] + '\t' + vm[1]+ '\n')
f.close()
# copie /tmp/etc_host on the host
TaktukPut(hosts, ['/tmp/etc_hosts']).run()
# and copy it at the end of the hostfile
cmd = '[ -f /etc/hosts.bak ] && cp /etc/hosts.bak /etc/hosts || ' + \
      ' cp /etc/hosts /etc/hosts.bak ; ' + \
      'cat /root/etc_hosts  >> /etc/hosts'   
update_hosts = TaktukRemote(cmd, hosts).run()

logger.info('Configure Service Node')
## Configure service node
service_node = hosts[-1] 
install_service_node = TaktukRemote('export DEBIAN_MASTER=noninteractive  ; apt-get -o Acquire::Check-Valid-Until=false update && apt-get install -y --force-yes openjdk-7-jre nfs-common -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" -o Acquire::Check-Valid-Until=false', service_node).run(); 
site = get_host_site(service_node)
mount_nfs = TaktukRemote('mount -t nfs nfs.' + site +'.grid5000.fr:/export/home /home', service_node).run()
#kill_vms = TaktutRemote('for i in `seq 96 101`; do virsh shutdown vm-$i ; done', service_node).run()


## configure Guest Oses
# Attention vms.list contient deux colonnes, il faut donc spliter
logger.info('Configure  VMs')
vms = [line.strip().split('\t')[0] for line in open('vms.list')]
complete_vm = False 
if complete_vm:
	test = TaktukPut(vms, ['/home/alebre/g5k-loadinjector/tools.tgz' ], remote_location='/root').run()
	for p in test.processes:
		if not p.ok:
			print p.host.address
			print p.stdout
			print p.stderr
	install_vms = TaktukRemote('tar xzf tools.tgz ; cd ./tools ; ./build.sh ; cp ./bin/* /bin/. ', vms).run()
	## Just wait for completion 
	sleep (8); 

part_size = '768'
init_vms = TaktukRemote('init_cpu_load.sh  ' + part_size, vms).run()
for p in init_vms.processes:
        if not p.ok:
                print p.host.address
                print p.stdout
                print p.stderr

test_part = TaktukRemote("df -h |grep shm", vms).run()
for p in test_part.processes:
	splitted_line = p.stdout.split()
	if len(splitted_line)>=2:
		if splitted_line[2] != part_size + 'M':
			logger.error(p.host.address + 'has wrong partition size\n' + p.stdout)
	else:
		logger.error(p.host.address + 'has no partition /dev/shm\n' + p.stdout)


# Just wait to be sure to complete the previous action
sleep(5)

## finalize node service
logger.info('Finalize service node configuration')
destroy_vms([service_node])
p = Process('head -n -1 hosts.list').run()
f = open('worker_nodes.txt', 'w')
f.write(p.stdout)
f.close()
test = TaktukPut(service_node, ['worker_nodes.txt' ], remote_location='/home/alebre/load_injector_experiment/config/').run()
for p in test.processes: 
	if not p.ok:
		print p.host
		print p.stdout
		print p.stderr


test = TaktukRemote('cd /home/alebre/load_injector_experiment/ ; rm ./exp.traces ; rm all* ; rm logs.txt ; rm -rf ./logs/*', service_node).run()
for p in test.processes: 
	if not p.ok:
		print p.host
		print p.stdout
		print p.stderr

logger.info('ssh root@'+str(service_node))
