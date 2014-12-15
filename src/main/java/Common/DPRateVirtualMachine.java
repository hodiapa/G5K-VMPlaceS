package Common;

import entropy.configuration.SimpleVirtualMachine;

/**
 * A stupid VM extension to associate a daemon to the VM
 */
public class DPRateVirtualMachine extends SimpleVirtualMachine implements Cloneable {
    private int dpIntensity;
    private int netBW;
    private int migNetBW;
    private int ramsize;
    private int currentLoad;
    private String ip;

    private boolean firstLoad;

    public DPRateVirtualMachine(String name,
               int nbCores, int ramsize, int netBW,  int migNetBW, int dpIntensity, String ip){
        super(name, nbCores,  0, ramsize, 0, ramsize);

        this.currentLoad = 0;
        this.netBW = netBW ;
        this.migNetBW=migNetBW;
        this. dpIntensity = dpIntensity ;
        this.ramsize= ramsize;

        this.ip = ip;
        this.firstLoad = false;
    }

    public int getDpIntensity() {
        return dpIntensity;
    }

    public String getIP(){
        return this.ip ;
    }

    public int getNetBW() {
        return this.netBW;
    }

    public boolean isFirstLoad() {
        return firstLoad;
    }

    public void setFirstLoad(boolean firstLoad) {
        this.firstLoad = firstLoad;
    }

    public DPRateVirtualMachine clone(){
        // TODO, this current clone is not accurate. It overrides the clone method implemented in
        // the SimpleVirtualMachine class but only partially (vm.template is not managed for instance).

        DPRateVirtualMachine clone = new DPRateVirtualMachine(this.getName(),
                   this.getNbOfCPUs(), this.getMemoryDemand(), this.getNetBW(),
                    this.getMigNetBW(), this.getDpIntensity(), this.getIP());
        clone.setCPUConsumption(this.getCPUConsumption());
        clone.setCPUDemand(this.getCPUDemand());
        clone.setMemoryConsumption(this.getMemoryConsumption());
        clone.setMemoryDemand(this.getMemoryDemand());
        return clone;
    }

    public int getMigNetBW() {
        return this.migNetBW;
    }
}
