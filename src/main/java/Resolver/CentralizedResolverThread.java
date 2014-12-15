package Resolver;

import Common.*;
import Launcher.Main;
import Configuration.XConfiguration;
import entropy.configuration.*;
import org.discovery.model.IVirtualMachine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import static entropy.configuration.Configurations.State.Runnings;
import static entropy.configuration.Configurations.State.Sleepings;


public class CentralizedResolverThread extends Thread{

    static private List<Node> nodesList = new ArrayList<Node>();
    static private String hostName = "noname";
    static int ongoingMigration = 0 ;
    static int loopID = 0 ;

    // Code is unused for the moment
    public static Configuration makeCurrentConfig() {

        Configuration configuration = new SimpleConfiguration();

        List<SimpleNode> nodes = new ArrayList<SimpleNode>();

        for (Node node : nodesList) {


            SimpleNode entropyNode = new SimpleNode(
                    node.getName(),
                    node.getNbOfCPUs(),
                    node.getCPUCapacity(),
                    node.getMemoryCapacity()
            );

            configuration.addOnline(entropyNode);

            String nodeName = node.getName();
            String hypervisorUrl = String.format(
                    "qemu+ssh://root@%s/session?socket=/var/run/libvirt/libvirt-sock",
                    nodeName
            );

            InjectorDriver driver = new InjectorDriver(nodeName, hypervisorUrl , "/usr/local/bin/virsh");
            driver.connect();

            for(IVirtualMachine vm : driver.getRunningVms()) {

                // TODO: use good source for getting numberOfCPU, ramCapacity, CoreCapacity

                SimpleVirtualMachine entropyVm = new SimpleVirtualMachine(
                        vm.getName(),
                        driver.getCpuCount(vm),
                        0, // currentConsumption
                        (int) driver.getMemorySize(vm), //memoryConsumption
                        100, //cpuDemand
                        (int) driver.getMemorySize(vm) //memoryDemand
                );
                configuration.setRunOn(entropyVm, entropyNode);
               int i = 0 ;
            }

        }

        return configuration;
    }

    public void run(){
		double period = EntropyProperties.getEntropyPeriodicity();
		int numberOfCrash = 0;

        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        /* Read hosts list (i.e. hosts that must be controlled by the resolver */
        nodesList = getNodesList();

        XConfiguration currConf = null ;
        AbstractScheduler scheduler;
        long beginTimeOfIteration;
		long endTimeOfCompute;
		long computationTime;
		AbstractScheduler.ComputingState computingState;


        Trace.getTracer().hostVariableSet(hostName, "NB_MIG", 0);

        long previousDuration = 0;

        // The "CentralizedResolverThread" will finished when the experiment's Main thread will return
		while(! Main.isExperimentFinished()) {

            long wait = ((long)(period*1000)) - previousDuration;
            if (wait > 0){
                 try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                 e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }            /* Get VMs informations */
            }
            // TODO to be able to use the makeCurrentConfig, we should find a way to correctly get the xxxDemand values
            //currConf = makeCurrentConfig();
            // Instead, we are directly getting the currentConfig from the global variable (i.e. the one use by the Injector)
            currConf = Main.getCurrentConfig() ;


			/* Compute and apply the plan */

			/* Tracing code */
            Trace.getTracer().hostSetState(hostName, "SERVICE", "compute");

            Log.info("Launching scheduler (loopId = "+loopID+") - start to compute");
			//Measure iteration length
            for (Node node: currConf.getAllNodes()){
                if(!Main.getCurrentConfig().isViable(node))
                    Trace.getTracer().hostPushState(node.getName(), "PM", "violation-det");
                Trace.getTracer().hostSetState(node.getName(), "SERVICE", "booked");
            }


            beginTimeOfIteration = System.currentTimeMillis();
            scheduler = new Entropy2RP(currConf.cloneSorted(),loopID ++);

			computingState = scheduler.computeReconfigurationPlan();
			endTimeOfCompute = System.currentTimeMillis();
			computationTime = (endTimeOfCompute - beginTimeOfIteration);

			Log.info("Computation time (in milliseconds): " + computationTime);

            previousDuration = computationTime ;

			if(computingState.equals(AbstractScheduler.ComputingState.NO_RECONFIGURATION_NEEDED)){
				Log.info("Configuration remains unchanged");
			} else if(computingState.equals(AbstractScheduler.ComputingState.VMRP_SUCCESS)){

				/* Tracing code */
                Log.info("Start reconfiguration");
				Trace.getTracer().hostSetState(hostName, "SERVICE", "reconfigure");

//                // TODO Adrien -> Adrien, try to consider only the nodes that are impacted by the reconfiguration plan
//                for (Node tmpNode: currConf.getAllNodes())
//                    Trace.getTracer().hostSetState(tmpNode.getName(), "SERVICE", "reconfigure");

                double startReconfigurationTime =  System.currentTimeMillis();
                double reconfigurationTime = 0 ;
				scheduler.applyReconfigurationPlan();
				double endReconfigurationTime =  System.currentTimeMillis();
                reconfigurationTime = endReconfigurationTime - startReconfigurationTime;
                Log.info("Reconfiguration time (in millis): " + reconfigurationTime);

                int numberOfNodesUsed = Configurations.usedNodes(currConf, EnumSet.of(Runnings, Sleepings)).size();
				Log.info("Number of nodes used: " + numberOfNodesUsed);
                previousDuration += reconfigurationTime ;

			} else {
				Log.info("The resolver does not find any solutions");
				numberOfCrash++;
				Log.info("Entropy has encountered an error (nb: " + numberOfCrash +")");
			}

            Trace.getTracer().hostSetState(hostName, "SERVICE", "free");
            for (Node node: currConf.getAllNodes()){
                Trace.getTracer().hostSetState(node.getName(), "SERVICE", "free");
            }

		}

	}

    private List<Node> getNodesList() {

        // BEGIN: returning nodes list in the virtualization driver version
        ManagedElementSet<Node> nodesAsElementSet = Main.makeNodes(SimulatorProperties.getWorkerNodesFile());
        Iterator<Node> nodeIterator = nodesAsElementSet.iterator();
        List<Node> nodes = new ArrayList<Node>();

        while (nodeIterator.hasNext())
            nodes.add(nodeIterator.next());
        // END: returning nodes list in the virtualization driver version

        // BEGIN: returning nodes list in hand made configuration version
//        List<Node> nodes = null ;
//        int nbOfNodes = SimulatorProperties.getNbOfNodes();
//        int nbOfVMs = SimulatorProperties.getNbOfVMs();
//        int totalCPUs = 0;
//
//        if(SimulatorProperties.getVirtualNodesNamesFile() != null){
//            nodes = new ArrayList<Node>();
//            try {
//                BufferedReader reader = new BufferedReader(new FileReader(SimulatorProperties.getVirtualNodesNamesFile()));
//                String nodeName;
//                int nodeCount = 0;
//
//                while((nodeName = reader.readLine()) != null){
//                    nodes.add(new SimpleNode(nodeName,
//                            G5kNodes.getNbOfCPUs(nodeName),
//                            G5kNodes.getCPUCapacity(nodeName),
//                            G5kNodes.getMemoryTotal(nodeName)));
//                    nodeCount++;
//                    totalCPUs += G5kNodes.getNbOfCPUs(nodeName);
//                }
//
//                nbOfVMs = (int) (SimulatorProperties.getNbOfCPUConsumptionSlots() == 2 ?
//                        totalCPUs / (float)(SimulatorProperties.getMaxPercentageOfActiveVMs() / (float)100) :
//                        totalCPUs * 1.6);
//                SimulatorProperties.INSTANCE.put(SimulatorProperties.NB_OF_VMS, "" + nbOfVMs);
//
//                if(nodeCount != nbOfNodes)
//                    Log.err("ERROR in node file: " + nbOfNodes + " names expected; " + nodeCount + " found");
//
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        else{
//            Log.err("ERROR VirtualNodesNamesFile property should be defined in simulator.properties");
//        }
        // END: returning nodes list in hand made configuration version

        return nodes;
    }

    public static void incMig(){
        // Increment the total number of migration that have been performed since the beginning of the experiments.
        Trace.getTracer().hostVariableAdd(hostName, "NB_MIG", 1);
        ongoingMigration++ ;
    }
    public static void decMig() {
        ongoingMigration-- ;
    }

    public static boolean ongoingMigration() {
        return (ongoingMigration != 0);
    }

    public static void relocateVM(final VirtualMachine softVM, final String sourceName, final String destinationName) {

        new Thread(
            new Runnable() {
            public void run() {

                Trace.getTracer().hostSetState(sourceName, "SERVICE", "reconfigure");
                Trace.getTracer().hostSetState(destinationName, "SERVICE", "reconfigure");

                // Migrate in the real world
                // TODO
                String hypervisorUrl = String.format(
                        "qemu+ssh://root@%s/session?socket=/var/run/libvirt/libvirt-sock",
                        sourceName
                );

                InjectorDriver driver = new InjectorDriver(sourceName, hypervisorUrl , "/usr/local/bin/virsh");
               // Adrien Comment this code to temporarily fixed the time issue due to invole virsh info --all
               // IVirtualMachine realVM = driver.findByName(softVM.getName());

                // BEGIN: outside G5K
//                driver.migrate(realVM, new WorkingNode(destinationName));
                // END: outside G5K

                // BEGIN: inside G5K
                String[] splittedDestinationName = destinationName.split("\\.");


                Log.info("Starting migration of VM " +softVM.getName() + " from "+sourceName + " to "+ destinationName);

                DPRateVirtualMachine dpRateVm = (DPRateVirtualMachine) softVM;
                // Please note that migspeed_set expects a value in mbps  - Adrien Nov 2014
                // double timeInSec = driver.migrate(realVM, new WorkingNode(splittedDestinationName[0]), dpRateVm.getMigNetBW()*8);
                // change the official call that take into parameter real VM (see above), by one that takes directly the name of the VM
                double timeInSec = driver.migrate(softVM.getName(), new WorkingNode(splittedDestinationName[0]), dpRateVm.getMigNetBW()*8);

                // BEGIN: inside G5K


                // TODO, push the change to the currentConfig since for the moment we are using it to get VM informations (see above TODO)
                Main.getCurrentConfig().relocateVM(softVM.getName(), destinationName);

                Log.info("End of migration of VM " +softVM.getName() + " from "+sourceName + " to "+ destinationName +"(Effective duration:"+timeInSec+")");
                CentralizedResolverThread.decMig();

                Trace.getTracer().hostSetState(sourceName, "SERVICE", "free");
                Trace.getTracer().hostSetState(destinationName, "SERVICE", "free");

                if(!Main.getCurrentConfig().isViable(destinationName)){
                    Log.info("POSSIBLE ARTIFICIAL VIOLATION ON "+destinationName+"\n"); //This means that the node changes its load during the migration
                    Trace.getTracer().hostSetState(destinationName, "PM", "violation-out");
                }
                if(Main.getCurrentConfig().isViable(sourceName)){
                    Log.info("SOLVED VIOLATION ON "+sourceName+"\n");
                    Trace.getTracer().hostSetState(sourceName, "PM", "normal");
                }

            }
        }).start();

    }
}
