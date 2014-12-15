package Launcher;

import Common.*;
import Injector.InjectorThread;
import Resolver.CentralizedResolverThread;
import entropy.configuration.*;
import Configuration.XConfiguration;
import Configuration.XSimpleConfiguration;
import entropy.configuration.parser.FileConfigurationSerializerFactory;
import org.discovery.model.IVirtualMachine;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: alebre
 * Date: 25/07/13
 * Time: 15:20
 * To change this template use File | Settings | File Templates.
 */
public class Main {

    private static XConfiguration currentConfig = null;
    private static boolean isExperimentFinished = false;

    public static long startingTime = 0;


     /* **** STATIC METHODS **** */

    public static boolean isExperimentFinished() {
        return isExperimentFinished;
    }

    // GENERATE INITIAL CONFIGURATION FILE
    public static XConfiguration generateConfigurationFileFromXPATH(
            String outputConfigurationFileName,
            int nbOfNodes, int nbOfCPUsPerNode, int cpuCapacityPerNode, int memoryTotalPerNode,
            int nbOfVMs) {
        System.out.println("Generating initial configuration file");

        XConfiguration initialConfiguration = new XSimpleConfiguration();
        ManagedElementSet<Node> nodes = makeNodes(SimulatorProperties.getWorkerNodesFile());
        System.out.println("MakeNode done");


        // BEGIN: creating the vm configuration from data returned by the virtualization driver

        Deque<VirtualMachine> vms = new LinkedList<VirtualMachine>();

        Random r = new Random(SimulatorProperties.getSeed());
        int nbOfVMClasses = VMClasses.CLASSES.size();
        VMClasses.VMClass vmClass;

        try {

            ShellAdaptor shell = new ShellAdaptor();

            shell.executeShellAsynchronously("rm config/essai.xml");
            shell.executeShellSynchronously("ssh -oStrictHostKeyChecking=no alebre@frontend."+SimulatorProperties.getSite()+".grid5000.fr \"bash ~/load_injector_experiment/generate_conf.sh ~/load_injector_experiment/config/worker_nodes.txt > essai.xml\"");
            shell.executeShellSynchronously("scp -oStrictHostKeyChecking=no alebre@frontend."+SimulatorProperties.getSite()+".grid5000.fr:essai.xml config/essai.xml");

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse("config/essai.xml");
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();


            for (Node node : nodes) {

                initialConfiguration.addOnline(node);

                String nodeNameForQuery = node.getName().replaceAll("g5k","grid5000.fr");

                XPathExpression expr = xpath.compile(String.format(
                        "//domain[@host='%s']",
                        nodeNameForQuery));

                NodeList vmsAsXpathNodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                // iterating over the virtual machines

                for (int i = 0; i < vmsAsXpathNodeList.getLength(); i++) {

                    org.w3c.dom.Node vmAsNode = vmsAsXpathNodeList.item(i);

                    // BEGIN: get the name of the vm
                    String nameQuery = String.format(
                            "//domain[@host='%s'][%d]/name/text()",
                            nodeNameForQuery,
                            i + 1);
                    XPathExpression exprName = xpath.compile(nameQuery);

                    String name = (String) exprName.evaluate(vmAsNode, XPathConstants.STRING);
                    // END: get the name of the vm


                    // BEGIN: get the memory size of the vm
                    String memoryQuery = String.format(
                            "//domain[@host='%s'][%d]/memory/text()",
                            nodeNameForQuery,
                            i + 1);
                    XPathExpression exprMemory = xpath.compile(memoryQuery);

                    String memoryAsString = (String) exprMemory.evaluate(vmAsNode, XPathConstants.STRING);
                    int memory = Integer.parseInt(memoryAsString) / 1024;
                    // END: get the memory size of the vm

                    // BEGIN: get the memory size of the vm
                    String cpuCountQuery = String.format(
                            "//vcpu_count[@id='%s']/@number",
                            name);
                    XPathExpression exprCpuCount = xpath.compile(cpuCountQuery);

                    String cpuCountAsString = (String) exprCpuCount.evaluate(doc, XPathConstants.STRING);
                    int cpuCount = Integer.parseInt(cpuCountAsString);
                    // END: get the memory size of the vm

                    // BEGIN: get the IP of the vm
                    String ipQuery = String.format(
                            "//domain[@name='%s']/@ip",
                            name);
                    XPathExpression exprIP = xpath.compile(ipQuery);

                    String ip = (String) exprIP.evaluate(vmAsNode, XPathConstants.STRING);
                    // END: get the name of the vm

                    System.out.println(String.format("vm: %s, %d, %d, %s",
                            name,
                            memory,
                            cpuCount,
                            ip
                    ));

                    vmClass = VMClasses.CLASSES.get(r.nextInt(nbOfVMClasses));

                    Log.info("vm " + name + " is " + vmClass.getName() + ", dp is " + vmClass.getMemIntensity());

                    int netBW = vmClass.getNetBW();
                    int migNetBW = vmClass.getMigNetBW();
                    int memIntensity = vmClass.getMemIntensity();

//                    InjectorDriver.VirtualMachineModel model = driver.getVmModel(vm);


                    DPRateVirtualMachine dpRateVm = new DPRateVirtualMachine(name, cpuCount,
                            memory, netBW, migNetBW, memIntensity, ip);

                    vms.add(dpRateVm);
                    initialConfiguration.setRunOn(dpRateVm, node);
                }
            }


            FileConfigurationSerializerFactory.getInstance().write(
                initialConfiguration,
                SimulatorProperties.getConfigurationFile()
            );

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }


        System.out.println("makeVMs done");
        System.out.println("MakeVMonNode done");


        // END: creating the vm configuration from data returned by the virtualization driver

        // BEGIN: creating the vm configuration from data read in vm_class.txt
        //Deque<VirtualMachine> vms = makeVMs(nbOfVMs);
        //System.out.println("makeVMs done");
        //addNodesAndVMs(initialConfiguration, nodes, vms);
        //System.out.println("MakeVMonNode done");
        // END: creating the vm configuration from data read in vm_class.txt

        // write the initialConfiguration to an output file.
        try {
            FileConfigurationSerializerFactory.getInstance().write(initialConfiguration, outputConfigurationFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Create the corresponding Node/VM representation to the HDD
        for (Node node : initialConfiguration.getAllNodes()) {
            initialConfiguration.writeNodeRepresentation(node);
        }


        // Prepare all VM files
        for (VirtualMachine vm : initialConfiguration.getAllVirtualMachines()) {
            initialConfiguration.writeVMRepresentation(vm);
        }


        return initialConfiguration;
    }


    public static XConfiguration generateConfigurationFileFromDriver(
            String outputConfigurationFileName,
            int nbOfNodes, int nbOfCPUsPerNode, int cpuCapacityPerNode, int memoryTotalPerNode,
            int nbOfVMs) {
        System.out.println("Generating initial configuration file");

        XConfiguration initialConfiguration = new XSimpleConfiguration();
        ManagedElementSet<Node> nodes = makeNodes(SimulatorProperties.getWorkerNodesFile());
        System.out.println("MakeNode done");


        // BEGIN: creating the vm configuration from data returned by the virtualization driver

        Deque<VirtualMachine> vms = new LinkedList<VirtualMachine>();

        Random r = new Random(SimulatorProperties.getSeed());
        int nbOfVMClasses = VMClasses.CLASSES.size();
        VMClasses.VMClass vmClass;

        for (Node node : nodes) {

            String nodeName = node.getName();
            String hypervisorUrl = String.format(
                    "qemu+ssh://root@%s/session?socket=/var/run/libvirt/libvirt-sock",
                    nodeName
            );

            InjectorDriver driver = new InjectorDriver(nodeName, hypervisorUrl, "/usr/local/bin/virsh");
            driver.connect();

            List<IVirtualMachine> vmsReturnedByDriver = driver.getRunningVms();

            initialConfiguration.addOnline(node);

            for (IVirtualMachine vm : vmsReturnedByDriver) {

                vmClass = VMClasses.CLASSES.get(r.nextInt(nbOfVMClasses));

                Log.info("vm " + vm.getName() + " is " + vmClass.getName() + ", dp is " + vmClass.getMemIntensity());

                // TODO: add correct values (how?)
                int netBW = vmClass.getNetBW();
                int migNetBW = vmClass.getMigNetBW();
                int memIntensity = vmClass.getMemIntensity();

                InjectorDriver.VirtualMachineModel model = driver.getVmModel(vm);


                DPRateVirtualMachine dpRateVm = new DPRateVirtualMachine(vm.getName(), model.cpuCount,
                        model.memorySize / 1024, netBW, migNetBW, memIntensity, model.ip);

                vms.add(dpRateVm);
                initialConfiguration.setRunOn(dpRateVm, node);
            }
        }

        System.out.println("makeVMs done");
        System.out.println("MakeVMonNode done");


        // END: creating the vm configuration from data returned by the virtualization driver

        // BEGIN: creating the vm configuration from data read in vm_class.txt
        //Deque<VirtualMachine> vms = makeVMs(nbOfVMs);
        //System.out.println("makeVMs done");
        //addNodesAndVMs(initialConfiguration, nodes, vms);
        //System.out.println("MakeVMonNode done");
        // END: creating the vm configuration from data read in vm_class.txt

        // write the initialConfiguration to an output file.
        try {
            FileConfigurationSerializerFactory.getInstance().write(initialConfiguration, outputConfigurationFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Create the corresponding Node/VM representation to the HDD
        for (Node node : initialConfiguration.getAllNodes()) {
            initialConfiguration.writeNodeRepresentation(node);
        }


        // Prepare all VM files
        for (VirtualMachine vm : initialConfiguration.getAllVirtualMachines()) {
            initialConfiguration.writeVMRepresentation(vm);
        }


        return initialConfiguration;
    }

    public static XConfiguration generateConfigurationFile(
            String outputConfigurationFileName,
            int nbOfNodes, int nbOfCPUsPerNode, int cpuCapacityPerNode, int memoryTotalPerNode,
            int nbOfVMs) {
        System.out.println("Generating initial configuration file");

        XConfiguration initialConfiguration = new XSimpleConfiguration();
        ManagedElementSet<Node> nodes = makeNodes(SimulatorProperties.getWorkerNodesFile());
        System.out.println("MakeNode done");


        // BEGIN: creating the vm configuration from data read in vm_class.txt
        Deque<VirtualMachine> vms = makeVMs(nbOfVMs);
        System.out.println("makeVMs done");
        addNodesAndVMs(initialConfiguration, nodes, vms);
        System.out.println("MakeVMonNode done");
        // END: creating the vm configuration from data read in vm_class.txt

        // write the initialConfiguration to an output file.
        try {
            FileConfigurationSerializerFactory.getInstance().write(initialConfiguration, outputConfigurationFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Create the corresponding Node/VM representation to the HDD
        for (Node node : initialConfiguration.getAllNodes()) {
            initialConfiguration.writeNodeRepresentation(node);
        }


        // Prepare all VM files
        for (VirtualMachine vm : initialConfiguration.getAllVirtualMachines()) {
            initialConfiguration.writeVMRepresentation(vm);
        }


        return initialConfiguration;
    }

    private static Deque<VirtualMachine> makeVMs(int nbOfVMs) {
        Deque<VirtualMachine> vms = new LinkedList<VirtualMachine>();
        String formatNbOfVMs = "%0" + String.valueOf(nbOfVMs).length() + "d";
        Random r = new Random(SimulatorProperties.getSeed());
        int nbOfVMClasses = VMClasses.CLASSES.size();
        VMClasses.VMClass vmClass;

        for (int i = 0; i < nbOfVMs; i++) {
            vmClass = VMClasses.CLASSES.get(r.nextInt(nbOfVMClasses));
            vms.add(new DPRateVirtualMachine("vm" + String.format(formatNbOfVMs, i), vmClass.getNbOfCPUs(),
                    vmClass.getMemSize(), vmClass.getNetBW(), vmClass.getMigNetBW(), vmClass.getMemIntensity(), ""));


        }
        return vms;
    }

    public static ManagedElementSet<Node> makeNodes(String workerNodeFile) {
        ManagedElementSet<Node> nodes = null;
        if (workerNodeFile != null) {
            nodes = new SimpleManagedElementSet<Node>();
            int nbOfVMs = -1;
            int totalCPUs = -1;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(workerNodeFile));
                String nodeName;
                int nodeCount = 0;

                while ((nodeName = reader.readLine()) != null) {
                    nodes.add(new SimpleNode(nodeName,
                            G5kNodes.getNbOfCPUs(nodeName),
                            G5kNodes.getCPUCapacity(nodeName),
                            G5kNodes.getMemoryTotal(nodeName)));
                    nodeCount++;
                    totalCPUs += G5kNodes.getNbOfCPUs(nodeName);
                }

                nbOfVMs = (int) (Common.SimulatorProperties.getNbOfCPUConsumptionSlots() == 2 ?
                        totalCPUs / (float) (Common.SimulatorProperties.getMaxPercentageOfActiveVMs() / (float) 100) :
                        totalCPUs * 1.6);
                Common.SimulatorProperties.INSTANCE.put(Common.SimulatorProperties.NB_OF_VMS, "" + nbOfVMs);

                if (nodeCount < SimulatorProperties.getNbOfNodes())
                    System.err.println("ERROR in node file: " + SimulatorProperties.getNbOfNodes() + " names expected; " + nodeCount + " found");

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            System.err.println("ERROR WorkerNode Lists property should be defined in simulator.properties");
        }
        return nodes;
    }


    private static ManagedElementSet<Node> makeNodes(int nbOfNodes, int nbOfCPUsPerNode, int cpuCapacityPerNode, int memoryTotalPerNode) {
        ManagedElementSet<Node> nodes = new SimpleManagedElementSet<Node>();
        String formatNbOfNodes = "%0" + String.valueOf(nbOfNodes).length() + "d";

        if (SimulatorProperties.getVirtualNodesNamesFile() == null) {
            for (int i = 1; i <= nbOfNodes; i++) {
                //	nodes.add(new SimpleNode("node" + String.format(formatNbOfNodes, i), nbOfCPUsPerNode, cpuCapacityPerNode, memoryTotalPerNode));
                nodes.add(new SimpleNode("node" + i, nbOfCPUsPerNode, cpuCapacityPerNode, memoryTotalPerNode));
            }
        }
        return nodes;
    }

    private static void addNodesAndVMs(XConfiguration cfg, ManagedElementSet<Node> nodes, Deque<VirtualMachine> vms) {
        try {
            int totalNbOfVCPUs = 0;
            int totalNbOfCPUs = 0;

            for (VirtualMachine vm : vms)
                totalNbOfVCPUs += vm.getNbOfCPUs();

            // Add nodes
            for (Node node : nodes) {
                cfg.addOnline(node);
                totalNbOfCPUs += node.getNbOfCPUs();
            }

            ListIterator<Node> nodeIter = nodes.listIterator();
            Node node;
            int nodeIndex;
            int[] nodeMemCons = new int[nodes.size()];
            int nodeNbOfVCPUs;
            VirtualMachine vm;

            //Add VMs to each node according to the nb of CPUs of that node
            while (nodeIter.hasNext()) {
                nodeIndex = nodeIter.nextIndex();
                node = nodeIter.next();

                nodeMemCons[nodeIndex] = 0;
                nodeNbOfVCPUs = 0;

                while (100 * nodeNbOfVCPUs / node.getNbOfCPUs() < 100 * totalNbOfVCPUs / totalNbOfCPUs
                        && nodeMemCons[nodeIndex] + vms.getFirst().getMemoryConsumption() <= node.getMemoryCapacity()) {
                    vm = vms.removeFirst();
                    cfg.setRunOn(vm, node);
                    nodeMemCons[nodeIndex] += vm.getMemoryConsumption();
                    nodeNbOfVCPUs += vm.getNbOfCPUs();
                }
            }

            //Affect the remaining VMs in a round robin way
            //This code may be useless
            boolean bVMPlaced;
            while (!vms.isEmpty()) {
                nodeIter = nodes.listIterator();
                bVMPlaced = false;
                while (nodeIter.hasNext()) {
                    nodeIndex = nodeIter.nextIndex();
                    node = nodeIter.next();

                    if (nodeMemCons[nodeIndex] + vms.getFirst().getMemoryConsumption() <= node.getMemoryCapacity()) {
                        bVMPlaced = true;
                        vm = vms.removeFirst();
                        cfg.setRunOn(vm, node);
                        nodeMemCons[nodeIndex] += vm.getMemoryConsumption();
                    }
                }
                if (!bVMPlaced) {
                    System.err.println("It is impossible to position all VMs (vm id: " + vms.getFirst().getName() + ")");
                    System.err.println("Current affectation :" + nodeMemCons.toString());
                    System.exit(-1);
                }
            }
        } catch (NoSuchElementException e) {
        }
    }

    public static XConfiguration generateConfigurationFile(String outputConfigurationFileName) {
        // TOOD add a simulator properties to specify whether we want to use the driver for retrieving the initial conf.
       //  return generateConfigurationFile(outputConfigurationFileName,
       return generateConfigurationFileFromXPATH(outputConfigurationFileName,
                //Capacity of each node
                SimulatorProperties.getNbOfNodes(), SimulatorProperties.getNbOfCPUs(), SimulatorProperties.getCPUCapacity(), SimulatorProperties.getMemoryTotal(),

                //Maximum resource consumption of each VM
                SimulatorProperties.getNbOfVMs());
    }

    public static XConfiguration generateConfigurationFile() {
        return generateConfigurationFile(SimulatorProperties.getConfigurationFile());
    }
    // ENDS OF CODE RELATED TO INITIAL CONFIGURATION FILE CREATION


    // GENERAL CODE

    public static void setConfiguration(XConfiguration newConfiguration) {
        currentConfig = newConfiguration;
    }


    public static XConfiguration getCurrentConfig() {
        return currentConfig;
    }


    public static long getCurrentTime() {
        return System.currentTimeMillis() - startingTime;
    }
    public static long  getStartingTime(){
        return startingTime;
    }


    public static void main (String[] args){
        // Generation of the Injection file
        Main.setConfiguration(Main.generateConfigurationFile());


        // Check that the generated configuration is viable
        if (!Main.getCurrentConfig().isViable()) {
            System.err.println("Initial Configuration should be viable !");
            System.exit(1);
        }

        startingTime = System.currentTimeMillis();

        System.out.println("STARTING EXPERIMENT "
                + Main.getCurrentConfig().getAllNodes().size() + " nodes / "
                + Main.getCurrentConfig().getAllVirtualMachines().size() + " vms :");

        InjectorThread injector = new InjectorThread(Main.getCurrentConfig(), SimulatorProperties.getDuration(), SimulatorProperties.getEvtPeriod());
        injector.start();
        CentralizedResolverThread centralized = new CentralizedResolverThread();
        centralized.start();
        try {
            injector.join();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        Log.info("EXPERIMENT ENDED (time:" + Main.getCurrentTime() + ")");

        // TODO : Adrien -> Jonathan: I don't  know why but the thread CentralizedResolved continues after the end of the launcher (i.e. its father).
        // Kill the CentralizedThread
        isExperimentFinished = true;
    }

}
