package Configuration;

import Common.*;
import entropy.configuration.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: alebre
 * Date: 20/08/13
 * Time: 17:12
 * To change this template use File | Settings | File Templates.
 */
public class XSimpleConfiguration extends SimpleConfiguration implements XConfiguration {

    public XSimpleConfiguration() {
        super();
    }


    @Override
    public XConfiguration clone() {
        final XConfiguration c = new XSimpleConfiguration();
        for (Node n : getOfflines()) {
            c.addOffline(n.clone());
        }

        for (VirtualMachine vm : getWaitings()) {
            c.addWaiting(vm.clone());
        }

        for (Node n : getOnlines()) {
            c.addOnline(n.clone());
            for (VirtualMachine vm : getRunnings(n)) {
                c.setRunOn(vm.clone(), n);
            }
            for (VirtualMachine vm : getSleepings(n)) {
                c.setSleepOn(vm.clone(), n);
            }
        }
        return c;
    }


    // Just a way to pass the same configuration to Entropy (the VMs are sorted in asc order on a particular PM)
    public XConfiguration cloneSorted() {
        final XSimpleConfiguration c = new XSimpleConfiguration();
        for (Node n : getOfflines()) {
            c.addOffline(n.clone());
        }

        for (VirtualMachine vm : getWaitings()) {
            c.addWaiting(vm.clone());
        }

        for (Node n : getOnlines()) {
            c.addOnline(n.clone());

            // Retrieve the set and sort it
            ManagedElementSet<VirtualMachine> tmpSet = new SimpleManagedElementSet<VirtualMachine>();
            tmpSet.addAll(getRunnings(n));
            Collections.sort(tmpSet, new Comparator<VirtualMachine>() {
                @Override
                public int compare(VirtualMachine v1, VirtualMachine v2) {
                    return Integer.parseInt(v1.getName().split("-")[1]) - Integer.parseInt(v2.getName().split("-")[1]);
                }
            });

            for (VirtualMachine vm : tmpSet) {
                c.setRunOn(vm.clone(), n);
            }

            for (VirtualMachine vm : getSleepings(n)) {
                c.setSleepOn(vm.clone(), n);
            }
        }
        return c;
    }



    // CODE RELATED TO THE UPDATE OF THE NODE AND VM REPRESENTATION ON THE HDD
    public void writeNodeRepresentation(Node node) {
        FileWriter fw;
        try {
            fw = new FileWriter("/tmp/" + node.getName(), true);
            BufferedWriter output = new BufferedWriter(fw);            /*
			 * Please note that this code is a simple Copy/Paste from
             * entropy.configuration.parser.PlainTextConfigurationSerializer:
			 * String writeVirtualMachine(VirtualMachine vm);
			 * The method is private so ugly but efficient ;) - Adrien
			 */
            StringBuilder buffer = new StringBuilder(50);
            boolean sep = false;
            for (VirtualMachine vm : this.getRunnings(node)) {
                if (!sep)
                    sep = true;
                else
                    buffer.append(" ");
                buffer.append(vm.getName());
            }

            output.append(buffer.toString());
            output.flush();
            output.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeVMRepresentation(VirtualMachine vm) {
        FileWriter fw;
        try {
            fw = new FileWriter("/tmp/" + vm.getName(), true);
            BufferedWriter output = new BufferedWriter(fw);
			/*
			 * Please note that this code is a simple Copy/Paste from
             * entropy.configuration.parser.PlainTextConfigurationSerializer:
			 * String writeVirtualMachine(VirtualMachine vm);
			 * The method is private so ugly but efficient ;) - Adrien
			 */
            StringBuilder buffer = new StringBuilder(50);
            buffer.append(vm.getName());
            buffer.append(" ");
            buffer.append(vm.getNbOfCPUs());
            buffer.append(" ");
            buffer.append(vm.getCPUConsumption());
            if (vm.getCPUDemand() != vm.getCPUConsumption()) {
                buffer.append(" ");
                buffer.append(vm.getCPUDemand());
            }
            buffer.append(" ");
            buffer.append(vm.getMemoryConsumption());
            if (vm.getMemoryConsumption() != vm.getMemoryDemand()) {
                buffer.append(" ");
                buffer.append(vm.getMemoryDemand());
            }
            buffer.append(" ");
            buffer.append(vm.getCPUMax());
            buffer.append('\n');

            output.write(buffer.toString());
            output.flush();
            output.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    // ENDS OF CODE RELATED TO THE UPDATE OF THE NODE AND VM REPRESENTATION ON THE HDD


    /**
     * check whether a pm is viable or not (currently only for the CPU dimension)
     *
     * @param nodeName, the name of the pm to test
     * @return boolean true if the PM is non viable (i.e. overloaded from the CPU viewpoint)
     */
    public boolean isViable(String nodeName) {
        return isViable(this.getNodeByName(nodeName));
    }

    public boolean isViable(Node pm) {
        return (this.load(pm) <= pm.getCPUCapacity());
    }


    public double load(Node pm) {
        double cons = 0;
        for (VirtualMachine v : this.getRunnings(pm))
            cons += v.getCPUDemand();
        return cons;
    }

    /**
     * Return the average load of the configuration (a %)
     *
     * @return
     */
    public double load() {
        double cons = 0;
        double tmpLoad = 0;
        for (Node pm : this.getAllNodes()) {
            tmpLoad = load(pm) * 100 / pm.getCPUCapacity();
            //Log.info("load of "+pm.getName()+":"+tmpLoad);
            cons += tmpLoad;
        }
        return cons / this.getAllNodes().size();
    }

    public boolean isViable() {
        for (Node node : this.getAllNodes()) {
            if (!isViable(node))
                return false;
        }
        return true;
    }


    public Node getNodeByName(String name) {
        Node tmpNode = null;
        for (Node n : this.getAllNodes()) {
            if (n.getName().equals(name)) {
                tmpNode = n;
                break;
            }
        }
        return tmpNode;
    }

    public VirtualMachine getVMByName(String name) {
        VirtualMachine tmpVM = null;
        for (VirtualMachine vm : this.getAllVirtualMachines()) {
            if (vm.getName().equals(name)) {
                tmpVM = vm;
                break;
            }
        }
        return tmpVM;
    }

    public void updateVM(VirtualMachine vm, int load) {
        Node host = this.getLocation(vm);
        boolean previouslyViable = this.isViable(host);

        Log.info("Change load of " + vm.getName() + "(from " + vm.getCPUDemand() + " to " + load + ")");

        vm.setCPUConsumption(0);
        vm.setCPUDemand(load);

        ShellAdaptor shell = new ShellAdaptor();

        String cmdForInitiatingLoad = "";
        String cmdForSettingLoad = "";
        String cmd = "";

       /* If this is the first time, we load the VM, we should start memtouch program */
        if (((DPRateVirtualMachine) vm).isFirstLoad()) {
            ((DPRateVirtualMachine) vm).setFirstLoad(false);

            // a cmd for initiating load
            // With Memtouch-busyloop
            //cmdForInitiatingLoad = String.format("ssh -oStrictHostKeyChecking=no root@%s \"init_cpu_load.sh %s %s %d %s\";",
            //    ((DPRateVirtualMachine) vm).getIP(),
            //    G5kNodes.getCalibratedCPUSpeed(this.getLocation(vm).getName()),
            //    G5kNodes.getCalibratedMemorySpeed(this.getLocation(vm).getName()),
            //    ((int) (vm.getMemoryDemand() * 0.9)),
            //    ((DPRateVirtualMachine) vm).getNetBW() * ((DPRateVirtualMachine) vm).getDpIntensity() / 100
            //);
            // With Memtouch-adjust-sleep
            cmdForInitiatingLoad = String.format("ssh -oStrictHostKeyChecking=no root@%s \"init_cpu_load.sh %d \";",
                    ((DPRateVirtualMachine) vm).getIP(),
                    ((int) (vm.getMemoryDemand() * SimulatorProperties.getScratchMemRatio())));

               cmd = cmdForInitiatingLoad;
        }
        else {

            // cmd for setting the Load
            // With Memtouch-busyloop
            //String cmdForSettingLoad = String.format("ssh -oStrictHostKeyChecking=no root@%s \"set_cpu_load.sh --load %d --dp_rate %d\"&",
            //        ((DPRateVirtualMachine) vm).getIP(),
            //        load,
            //        ((DPRateVirtualMachine) vm).getDpIntensity()
            //);
            // With Memtouch-adjust-sleep
            cmdForSettingLoad = String.format("ssh -oStrictHostKeyChecking=no root@%s \"set_cpu_load.sh %s %s %d %s %s\";",
                    ((DPRateVirtualMachine) vm).getIP(),
                    G5kNodes.getCalibratedCPUSpeed(this.getLocation(vm).getName()),
                    G5kNodes.getCalibratedMemorySpeed(this.getLocation(vm).getName()),
                    ((int) (vm.getMemoryDemand() * SimulatorProperties.getScratchMemRatio())),
                    ((double) load / 100),
                    ((DPRateVirtualMachine) vm).getNetBW() * ((DPRateVirtualMachine) vm).getDpIntensity() / 100
            );
            cmd=cmdForSettingLoad;
        }
/*        String cmd = String.format("%s %s",
            cmdForInitiatingLoad,
            cmdForSettingLoad);
*/
        // Inject real load
        try {

            shell.executeShellAsynchronously(cmd);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Please be rigorous ! config/simulator.properties should be in adequation of your expected load (i.e. 10% or 100%...)
        this.writeVMRepresentation(vm);

        if (previouslyViable) {
            if (!this.isViable(host)) {
                Log.info("STARTING VIOLATION ON " + host.getName() + "\n");
            }
        } else if (!previouslyViable) {
            if (this.isViable(host)) {
                Log.info("ENDING VIOLATION ON " + host.getName() + "\n");
            }
        }

        // Update load of the host
        Trace.getTracer().hostVariableSet(host.getName(), "LOAD", this.load(host));

        //Update global load
        Trace.getTracer().hostVariableSet("node0", "LOAD", this.load());
    }

    public synchronized void relocateVM(String vmName, String destinationNodeName) {
        // To ensure that the PM and the VM that are manipulated are the right ones (i.e. the ones from the main configuration),
        // we must retrieve them from the current configuration

        Node mainDestNode = this.getNodeByName(destinationNodeName);
        VirtualMachine mainVM = this.getVMByName(vmName);
        if (mainDestNode != null) {
            this.setRunOn(mainVM, mainDestNode);
        } else {
            Log.err("You are trying to relocate a VM on a non existing node");
            System.exit(-1);
        }
    }
}
