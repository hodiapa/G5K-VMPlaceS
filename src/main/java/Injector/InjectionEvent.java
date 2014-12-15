package Injector;

import entropy.configuration.VirtualMachine;

public class InjectionEvent {

	private long id ;
	private double time; // In seconds
	private VirtualMachine vm;
	private int newCPULoad;
	
	public InjectionEvent(long id, double time, VirtualMachine vm, int newCPULoad) {
		this.id=id; 
		this.time= time;
		this.vm=vm;
		this.newCPULoad = newCPULoad;
	}
	
	public long getId(){
		return this.id; 
	}
	public double getTime() {
		return this.time;
	}
	
	public VirtualMachine getVm(){
	  return this.vm;
	}
    public int getCPULoad(){
      return this.newCPULoad;
    }
	
    public String toString(){
    	return this.getTime()+"/"+this.getVm().getName()+"/"+this.getCPULoad();
    }
}
