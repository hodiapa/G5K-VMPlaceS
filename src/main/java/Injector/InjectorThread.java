package Injector;

import Common.*;
import Launcher.Main;
import Configuration.XConfiguration;
import Resolver.EntropyProperties;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.VirtualMachine;
import entropy.configuration.Configuration;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

public class InjectorThread extends Thread {

    private Deque<InjectionEvent> evtQueue = null ;
    private boolean skipOverlappingEvent = true ;

    public InjectorThread(XConfiguration initialConf, long duration, int period){
        super();
         // Nb PMs, Nb VMs, Max load, frequency of event occurrence
        this.evtQueue= generateEventQueue(initialConf, duration, period);

        for(InjectionEvent evt: this.evtQueue){
            System.out.println(evt);
        }
    }


    /* **** STATIC METHODS **** */

	/* Compute the next exponential value for rand */
	private static double exponentialDis(Random rand, double lambda) {
		return -Math.log(1 - rand.nextDouble()) / lambda;
	}

    /* **** GENERAL CODE **** */
    /**
	 *
	 * @param currentConfig
	 * @param duration int, duration of the simulated time in minutes
	 * @param period int,  frequency of event occurrence in seconds
	 * @return the queue of the VM changes
	 * @see InjectionEvent
	 */
	public Deque<InjectionEvent> generateEventQueue(XConfiguration currentConfig, long duration, int period) {

		LinkedList<InjectionEvent> eventQueue = new LinkedList<InjectionEvent>();
		Random randExpDis=new Random(SimulatorProperties.getSeed());
		double currentTime = 0 ;
		double lambdaPerVM=1.0/period ; // Nb Evt per VM (average)

		Random randExpDis2=new Random(SimulatorProperties.getSeed());

		double mean = SimulatorProperties.getMeanLoad();
		double sigma = SimulatorProperties.getStandardDeviationLoad();

		double gLoad = 0;

		ManagedElementSet<VirtualMachine> vms = currentConfig.getAllVirtualMachines();
		double lambda=lambdaPerVM*vms.size();

		int maxCPUDemand = SimulatorProperties.getCPUCapacity()/SimulatorProperties.getNbOfCPUs();
		int nbOfCPUDemandSlots = SimulatorProperties.getNbOfCPUConsumptionSlots();
		int vmCPUDemand;
		long id=0;
		VirtualMachine tempVM;

		currentTime+=exponentialDis(randExpDis, lambda);

		Random randVMPicker = new Random(SimulatorProperties.getSeed());
		int nbOfVMs = vms.size();

        Log.info("Number of VM :"+vms.size());

		while(currentTime < duration){

            if( !skipOverlappingEvent || ((int)currentTime) % EntropyProperties.getEntropyPeriodicity() != 0){
                // select a VM
                tempVM = vms.get(randVMPicker.nextInt(nbOfVMs));
                // and change its state

                int cpuConsumptionSlot = maxCPUDemand/nbOfCPUDemandSlots;
                /*Uniform assignment of VM load */
                //int slot=(int) (Math.random()*(nbOfCPUDemandSlots+1));
                /* Gaussian law for the load assignment */
                gLoad = Math.max((randExpDis2.nextGaussian()*sigma)+mean, 0);
                int slot= (int) Math.round(Math.min(100,gLoad)*nbOfCPUDemandSlots/100);

                vmCPUDemand = slot*cpuConsumptionSlot*tempVM.getNbOfCPUs();

                // Add a new event queue
                eventQueue.add(new InjectionEvent(id++, currentTime,tempVM, vmCPUDemand));
            }
			currentTime+=exponentialDis(randExpDis, lambda);
			//        System.err.println(eventQueue.size());
		}
		Log.info("Number of events:" + eventQueue.size());
		return eventQueue;
	}

    public void run(){

        InjectionEvent evt = this.nextEvent();


        Log.info("start event  injection");


        while(evt!=null){
            if((evt.getTime()*1000) - Main.getCurrentTime()>0)
                try {
                    Thread.sleep((long)(evt.getTime()*1000) - Main.getCurrentTime());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            Main.getCurrentConfig().updateVM(evt.getVm(), evt.getCPULoad());

            Log.info("Current load (avg %):" + Main.getCurrentConfig().load());
            evt= this.nextEvent();
        }
        Log.info("end event  injection");
    }

    private InjectionEvent nextEvent() {
        return this.getEvtQueue().pollFirst();
    }

    public Deque<InjectionEvent> getEvtQueue() {
        return evtQueue;
    }
}