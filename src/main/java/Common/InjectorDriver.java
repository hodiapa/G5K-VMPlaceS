package Common;

/* ============================================================
 * Discovery Project - G5K-LOADINJECTOR
 * http://beyondtheclouds.github.io/
 * ============================================================
 * Copyright 2013 Discovery Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============================================================ */


import org.discovery.driver.VirtualMachine;
import org.discovery.model.*;
import org.discovery.model.network.FlauncherNetworkDriver;
import org.discovery.model.network.INetworkDriver;
import org.discovery.model.network.NatNetworkDriver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class InjectorDriver implements IDriver {

    public class VirtualMachineModel {

        public String name = "noname";
        public int memorySize = -1;
        public int cpuCount = -1;
        public String ip = "";
    }

    private boolean isConnected = false;

    private String VIRSH_PATH = "/usr/local/bin/virsh";

    private String HYPERVISOR_URL = "";
    private String HYPERVISOR_ADDRESS = "";
    private String HYPERVISOR_PORT = "";

    private String NODE_LOCATION = "";

    private String VM_SSH_USERNAME = "";
    private String VM_SSH_PORT = "";

    private INetworkDriver networkDriver;




    public InjectorDriver(String nodeUrl, String hypervisorUrl, String virshPath) {

        Properties properties = new Properties();

            String path = virshPath;

            NODE_LOCATION = nodeUrl;
            HYPERVISOR_URL = hypervisorUrl;

            String networkType = properties.getProperty("network.type");

//            if(networkType.toUpperCase().equals("NAT")) {
//
//                this.networkDriver = new NatNetworkDriver(this);
//
//            } else if(networkType.toUpperCase().equals("FLAUNCHER")) {

                this.networkDriver = new FlauncherNetworkDriver(this);
//            }


            if(HYPERVISOR_URL.contains("@")) {

                int startIndex = HYPERVISOR_URL.indexOf("@")+1;
                int endIndex = HYPERVISOR_URL.indexOf("/", startIndex);

                if(endIndex == -1) {
                    endIndex = HYPERVISOR_URL.length() - 1;
                }

                String filteredHypervisorUrl = HYPERVISOR_URL.substring(startIndex, endIndex);

                if(filteredHypervisorUrl.contains(":")) {
                    HYPERVISOR_ADDRESS = filteredHypervisorUrl.split(":")[0];
                    HYPERVISOR_PORT = filteredHypervisorUrl.split(":")[1];
                } else {
                    HYPERVISOR_ADDRESS = filteredHypervisorUrl;
                    HYPERVISOR_PORT = "22";
                }

            } else {
                HYPERVISOR_ADDRESS = "127.0.0.1";
                HYPERVISOR_PORT = "22";
            }

            VM_SSH_USERNAME = properties.getProperty("vm.ssh.username", "root");
            VM_SSH_PORT = properties.getProperty("vm.ssh.port", "22");

            this.VIRSH_PATH = path;
    }

    public String getMigrationUrl() {

        return HYPERVISOR_URL;
    }


    public String execute(String cmd) throws IOException {

        Runtime runtime = Runtime.getRuntime();

        // BEGIN: make a call to a local virsh
//        String executedCmd = cmd.replace("$virsh", VIRSH_PATH + " -c " + this.HYPERVISOR_URL);
        // END: make a call to a local virsh

        // BEGIN: make a call to a remote virsh
        String executedCmd = cmd.replace("$virsh", String.format("ssh -oStrictHostKeyChecking=no %s@%s -p %s virsh",
            "root",
            HYPERVISOR_ADDRESS,
            HYPERVISOR_PORT
        ));
        // END: make a call to a remote virsh

        String[] shellCmd = {
                "/bin/sh",
                "-c",
                executedCmd
        };


        System.out.println(executedCmd);

        final Process p = runtime.exec(shellCmd);

        String result = "";

        BufferedReader in =
                new BufferedReader(new InputStreamReader(p.getInputStream()));
        String inputLine;

        while ((inputLine = in.readLine()) != null) {

            if(!result.equals("")) {
                result += "\n";
            }

            result += inputLine;
        }
        in.close();


        return result;
    }

    public boolean connect() {
        // TODO Auto-generated method stub



//        try {
//
//            try{
//                String cmd = String.format("$virsh \"connect %s; quit\"", HYPERVISOR_URL);
//                String result = execute(cmd);
//                if(result.contains("failed to connect to the hypervisor")) {
//                    throw new VirtualizationException("cannot connect to %s".format(HYPERVISOR_URL));
//                }
//            }  catch (IOException e){
//
//                // assume that virsh is not in the basic unix executable folder, we need to find it!
//                isConnected = false;
//
//                try {
//                    String result = execute(String.format("ls %s", VIRSH_PATH));
//                    if(!result.equals(VIRSH_PATH)) {
//                        throw new VirtualizationException(String.format("cannot find virsh at %s " +
//                                "please change the value of VIRSH_PATH", VIRSH_PATH));
//                    }
//
//                } catch (IOException ee){
//
//                    System.out.println("exception caught:"+e);
//                    System.out.println(e.getMessage());
//
//                }
//
//                System.out.println("exception caught:"+e);
//                System.out.println(e.getMessage());
//
//                return false;
//            }
//
//
//        } catch (VirtualizationException e){
//
//            isConnected = false;
//
//            System.out.println("exception caught:"+e);
//
//            return false;
//        }
//
//        isConnected = true;

        return true;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getHYPERVISOR_URL() {
        return HYPERVISOR_URL;
    }

    public String getHYPERVISOR_ADDRESS() {
        return HYPERVISOR_ADDRESS;
    }

    public String getHYPERVISOR_PORT() {
        return HYPERVISOR_PORT;
    }

    public List<IVirtualMachine> getRunningVms() {

        List<IVirtualMachine> result = new ArrayList<IVirtualMachine>();

        String cmdResult = "";
        try {
            cmdResult = this.execute("$virsh list");
        } catch (IOException e){

            return new ArrayList<IVirtualMachine>();
        }

        int skipCount = 2; // the first two lines are header, we have to skip them
        String[] arrayOfLines = cmdResult.split("\n");
        for(String line: arrayOfLines) {
            if(skipCount == 0) {

                try {
                    result.add(VirtualMachine.parseFromVirshLine(line));
                } catch(VirtualMachineParsingException e) {
                    System.out.println(e.getStackTrace());
                }

            } else {
                skipCount = skipCount-1;
            }
        }

        return result;
    }

    public List<IVirtualMachine> getVms() {

        List<IVirtualMachine> result = new ArrayList<IVirtualMachine>();

        String cmdResult = "";
        try {
            cmdResult = this.execute("$virsh list --all");
        } catch (IOException e){

            return new ArrayList<IVirtualMachine>();
        }

        int skipCount = 2;
        String[] arrayOfLines = cmdResult.split("\n");
        for(String line: arrayOfLines) {
            if(skipCount == 0) {

                try {
                    result.add(VirtualMachine.parseFromVirshLine(line));
                } catch(VirtualMachineParsingException e) {
                    System.out.println(e.getMessage());
                }

            } else {

                skipCount = skipCount-1;
            }
        }

        return result;
    }

    public IVirtualMachine findByName(String name) {
        for(IVirtualMachine vm: getVms()) {
            if(vm.getName().equals(name)) {
                return vm;
            }
        }
        return null;
    }

    public IVirtualMachine findById(int id) {
        for(IVirtualMachine vm: getVms()) {
            if(vm.getId() == id) {
                return vm;
            }
        }
        return null;
    }

    public String getMacAddress(IVirtualMachine machine) {

        return networkDriver.getMacAddress(machine);
    }

    public String getIpAddress(IVirtualMachine machine) {

        return networkDriver.getIpAddress(machine);
    }

    public double getUserCpu(IVirtualMachine machine) {
        double load = -1;

        String ipAddress = getIpAddress(machine);

        String cmdResult = "";
        try {

            cmdResult = this.execute(String.format(
                    "ssh %s -p %s ssh %s -p %s -l %s \"top -b -n 1 | grep \\\"Cpu(s):\\\" | sed \\\"s/%%/ /g\\\" | awk '{print \\$2}'\"",
                    HYPERVISOR_ADDRESS,
                    HYPERVISOR_PORT,
                    ipAddress,
                    VM_SSH_PORT,
                    VM_SSH_USERNAME));
            load = Double.parseDouble(cmdResult);
        } catch (IOException e){

            return -1;
        }

        return load;
    }

    public double getStealCpu(IVirtualMachine machine) {
        double load = -1;

        String ipAddress = getIpAddress(machine);

        String cmdResult = "";
        try {

            cmdResult = this.execute(String.format(
                    "ssh %s -p %s ssh %s -p %s -l %s \"top -b -n 1 | grep \\\"Cpu(s):\\\" | sed \\\"s/%%/ /g\\\" | awk '{print \\$16}'\"",
                    HYPERVISOR_ADDRESS,
                    HYPERVISOR_PORT,
                    ipAddress,
                    VM_SSH_PORT,
                    VM_SSH_USERNAME));
            load = Double.parseDouble(cmdResult);
        } catch (IOException e){

            return -1;
        }

        return load;
    }

    public VirtualMachineModel getVmModel(IVirtualMachine machine) {

        VirtualMachineModel model = new VirtualMachineModel();

        String cmdResult;
        try {

            cmdResult = this.execute(String.format(
                    "ssh root@%s \"virsh dominfo %s | grep \\\"CPU(s)\\\" | awk '{print \\$2}'; virsh dumpxml %s | grep memory | sed \\\"s/[^0-9]//g\\\"; get-ip.pl %s\"",
                    NODE_LOCATION,
                    machine.getName(),
                    machine.getName(),
                    machine.getName()

            ));


            String[] splittedResult = cmdResult.split("\n");
            model.cpuCount = Integer.parseInt(splittedResult[0]);
            model.memorySize = Integer.parseInt(splittedResult[1]);
            model.ip = splittedResult[2];

//            System.out.println(cmdResult);

        } catch (IOException e) {

            e.printStackTrace();
        }


        return model;
    }

    public int getCpuCount(IVirtualMachine machine) {
        int cpuCount;
        String cmdResult;
        try {

            cmdResult = this.execute(String.format(
                    "$virsh dominfo %s | grep \"CPU(s)\" | awk '{print $2}'",
                    machine.getName()));
            cpuCount = Integer.parseInt(cmdResult);
        } catch (IOException e) {

            return -1;
        }

        return cpuCount;
    }

    public double getMemorySize(IVirtualMachine machine) {
        double memorySize;
        String cmdResult;
        try {

            cmdResult = this.execute(
                    String.format("$virsh dumpxml %s | grep memory | sed \"s/[^0-9]//g\"", machine.getName()));
            memorySize = Double.parseDouble(cmdResult);
        } catch (IOException e) {

            return -1;
        }

        return memorySize;
    }

    public boolean start(IVirtualMachine machine) {
        String cmdResult = "";
        try {
            cmdResult = this.execute(String.format("$virsh start %s", machine.getName()));
        } catch (IOException e){

            return false;
        }

        return cmdResult.contains(String.format("Domain %s started", machine.getName()));
    }

    public boolean shutdown(IVirtualMachine machine) {
        String cmdResult = "";
        try {
            cmdResult = this.execute(String.format("$virsh shutdown %s", machine.getName()));
        } catch (IOException e){

            return false;
        }

        return cmdResult.contains(String.format("Domain %s is being shutdown", machine.getName()));
    }

    public boolean suspend(IVirtualMachine machine) {
        String cmdResult = "";
        try {
            cmdResult = this.execute(String.format("$virsh suspend %s", machine.getName()));
        } catch (IOException e){

            return false;
        }

        return cmdResult.contains(String.format("Domain %s suspended", machine.getName()));
    }

    public boolean resume(IVirtualMachine machine) {
        String cmdResult = "";
        try {
            cmdResult = this.execute(String.format("$virsh resume %s", machine.getName()));
        } catch (IOException e){

            return false;
        }

        return cmdResult.contains(String.format("Domain %s resumed", machine.getName()));
    }

    public boolean migrate(IVirtualMachine machine, INode otherNode) {
        String cmdResult = "";
        try {
            cmdResult = this.execute(String.format("ssh -oStrictHostKeyChecking=no %s@%s -p %s migrate_vm.sh %s %s",
                    "root",
                    HYPERVISOR_ADDRESS,
                    HYPERVISOR_PORT,
                    machine.getName(),
                    otherNode.getPath()));
//            cmdResult = this.execute(String.format("$virsh migrate --live %s %s", machine.getName(), otherNode.getPath()));
        } catch (IOException e){

            return false;
        }

        return cmdResult.contains(String.format("Domain %s has been migrated to %s", machine.getName(), otherNode.getPath()));
    }

    public double migrate(IVirtualMachine machine, INode otherNode, int migrationSpeed) {
        String cmdResult = "";
        double duration = -1;
        try {
            cmdResult = this.execute(String.format("ssh -oStrictHostKeyChecking=no %s@%s -p %s migrate_vm.sh %s %s %d",
                    "root",
                    HYPERVISOR_ADDRESS,
                    HYPERVISOR_PORT,
                    machine.getName(),
                    otherNode.getPath(),
                    migrationSpeed));
//            cmdResult = this.execute(String.format("$virsh migrate --live %s %s", machine.getName(), otherNode.getPath()));
              duration=Double.parseDouble(cmdResult);
        } catch (Exception e){
            Log.info("Exception" +e);
            return -1;
        }

        return duration;
    }
    public double migrate(String vmName, INode otherNode, int migrationSpeed) {
        String cmdResult = "";
        double duration = -1;
        try {
            cmdResult = this.execute(String.format("ssh -oStrictHostKeyChecking=no %s@%s -p %s migrate_vm.sh %s %s %d",
                    "root",
                    HYPERVISOR_ADDRESS,
                    HYPERVISOR_PORT,
                    vmName,
                    otherNode.getPath(),
                    migrationSpeed));
//            cmdResult = this.execute(String.format("$virsh migrate --live %s %s", machine.getName(), otherNode.getPath()));
            duration=Double.parseDouble(cmdResult);
        } catch (Exception e){
            Log.info("Exception" +e);
            return -1;
        }

        return duration;
    }
}

