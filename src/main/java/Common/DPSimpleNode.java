package Common;

import entropy.configuration.SimpleNode;

/**
 * Created with IntelliJ IDEA.
 * User: alebre
 * Date: 04/09/13
 * Time: 17:20
 * To change this template use File | Settings | File Templates.
 */

public class DPSimpleNode extends SimpleNode implements Cloneable{

    private double calibratedCPU;
    private double calibratedMem;

    DPSimpleNode(String name, int nbCores, int cpuCapacity, int memSize, double calibratedCPU, double calibratedMem){
        super(name, nbCores, cpuCapacity, memSize);
        this.calibratedCPU=calibratedCPU;
        this.calibratedMem=calibratedMem;
    }
    public double getCalibratedCPU() {
        return calibratedCPU;
    }

    public double getCalibratedMem() {
        return calibratedMem;
    }

    public DPSimpleNode clone (){
        return new DPSimpleNode(this.getName(), this.getNbOfCPUs(), this.getCPUCapacity(), this.getMemoryCapacity(), this.getCalibratedCPU(), this.getCalibratedMem());
    }
}

