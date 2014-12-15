package Common;

import java.util.HashMap;
import java.util.Map;

import entropy.configuration.SimpleNode;

public class G5kNodes {

	public final static int UNINITIALIZED_INT_VALUE = -1;
	
	private final static Map<String, DPSimpleNode> g5kNodes = new HashMap<String, DPSimpleNode>(){
		private static final long serialVersionUID = -7448828279371546170L;
	{

        // Jonathan's computer
        put("jonathan", new DPSimpleNode(null, 2, 200 , 4096,-1,-1));

		//Bordeaux
		put("bordeplage", new DPSimpleNode(null, 2, 200 , 2048,-1,-1));
		put("bordereau", new DPSimpleNode(null, 4, 400, 4096,-1,-1));
		put("borderline", new DPSimpleNode(null, 8, 800, 32768,-1,-1));
		
		//Grenoble
		put("adonis", new DPSimpleNode(null, 8, 800, 24576,-1,-1));
		put("edel", new DPSimpleNode(null, 8, 800, 24576,-1,-1));
		put("genepi", new DPSimpleNode(null, 8, 800, 8192,-1,-1));
		
		//Lille
		put("chicon", new DPSimpleNode(null, 4, 400, 4096,-1,-1));
		put("chimint", new DPSimpleNode(null, 8, 800, 16384,-1,-1));
		put("chinqchint", new DPSimpleNode(null, 8, 800, 8192,-1,-1));
		put("chirloute", new DPSimpleNode(null, 8, 800, 8192,-1,-1));
		
		//Lyon
		put("capricorne", new DPSimpleNode(null, 2, 200, 2048,-1,-1));
		put("sagittaire", new DPSimpleNode(null, 2, 200, 2048,-1,-1));
		
		//Nancy
		put("griffon", new DPSimpleNode(null, 8, 800, 16384, 1214967.788475, 7513266.608608));
		put("graphene", new DPSimpleNode(null, 4, 400, 16384, 345403.983925,63693635.062458));

        //Nantes (please note that the first values are just wrong ;) )
        put("econome", new DPSimpleNode(null, 4, 400, 16384, 346587.737692, 63221077.437421));
		
		//Reims
		put("stremi", new DPSimpleNode(null, 24, 2400, 49152,-1,-1));
		
		//Rennes
		put("paradent", new DPSimpleNode(null, 8, 800, 32768, 1214967.788475, 7473261.785402));
		put("paramount", new DPSimpleNode(null, 4, 400, 8192,-1,-1));
		put("parapide", new DPSimpleNode(null, 8, 800, 24576,-1,-1));
		put("parapluie", new DPSimpleNode(null, 24, 2400, 49152,559949.493139, 6699922.315851));
		
		//Sophia
		put("helios", new DPSimpleNode(null, 4, 400, 4096,-1,-1));
		put("sol", new DPSimpleNode(null, 4,400 , 4096,643147.295611,9291461.264815));
		put("suno", new DPSimpleNode(null, 8, 800, 32768,741925.004637,48114514.880358));
		
		//Toulouse
		put("pastel", new DPSimpleNode(null, 4, 400, 8192,-1,-1));
		put("violette", new DPSimpleNode(null, 2, 200, 2048,-1,-1));
	}};

    public static String getClusterName(String nodeName){
		int end = nodeName.indexOf("-");
		return nodeName.substring(0, end);
	}
	
	private static DPSimpleNode getDVMSNodeTemplate(String nodeName){
		String clusterName = getClusterName(nodeName);
		return g5kNodes.get(clusterName);
	}
	
	public static int getNbOfCPUs(String nodeName){
		SimpleNode template = getDVMSNodeTemplate(nodeName);
		return template == null ?
				SimulatorProperties.getNbOfCPUs() : 
					template.getNbOfCPUs();
	}
	
	public static int getCPUCapacity(String nodeName){
		SimpleNode template = getDVMSNodeTemplate(nodeName);
		return template == null ?
				SimulatorProperties.getCPUCapacity() :
					template.getCPUCapacity();

	}
	
	public static int getMemoryTotal(String nodeName){
		SimpleNode template = getDVMSNodeTemplate(nodeName);
		return template == null ?
				SimulatorProperties.getMemoryTotal() : 
					template.getMemoryCapacity();
	}


    public static double getCalibratedCPUSpeed(String nodeName) {
        DPSimpleNode template = getDVMSNodeTemplate(nodeName);
        return template == null ?
                -1:
                template.getCalibratedCPU();

    }

    public static double getCalibratedMemorySpeed(String nodeName) {
        DPSimpleNode template = getDVMSNodeTemplate(nodeName);
        return template == null ?
                                 -1:
                                 template.getCalibratedMem();

    }

	private static void displayNodeChara(String nodeName){
		System.out.println(nodeName
				+ " [nb CPU = " + getNbOfCPUs(nodeName)
				+ " ; RAM = " + getMemoryTotal(nodeName) + "]");
	}
	
	public static void main(String[] args){
		displayNodeChara("chicon-1-kavlan-18.lille.grid5000.fr");
		displayNodeChara("chimint-1-kavlan-18");
		displayNodeChara("chinqchint-19");
		displayNodeChara("chirloute-19");
		
		displayNodeChara("griffon-5");
		displayNodeChara("graphene-144");
		
		displayNodeChara("paradent-15");
		displayNodeChara("paramount-9");
		displayNodeChara("parapide-25-kavlan-18");
		displayNodeChara("parapluie-32.rennes.grid5000.fr");
		
		displayNodeChara("helios-18");
		displayNodeChara("sol-23");
		displayNodeChara("suno-45");
	}
}
