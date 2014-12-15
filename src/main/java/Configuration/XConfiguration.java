package Configuration;

import entropy.configuration.Configuration;
import entropy.configuration.ManagedElementSet;
import entropy.configuration.Node;
import entropy.configuration.VirtualMachine;

/**
 * Created with IntelliJ IDEA.
 * User: alebre
 * Date: 20/08/13
 * Time: 17:21
 * To change this template use File | Settings | File Templates.
 */
public interface XConfiguration extends Configuration {
   public boolean isViable();
   public boolean isViable(Node pm);
   public boolean isViable(String nodeName);

   public double load();

   public void updateVM(VirtualMachine vm, int load);

   public void relocateVM(String vmName, String destinationNodeName);

   public void writeNodeRepresentation(Node n);

   public void writeVMRepresentation(VirtualMachine vm);

    public XConfiguration cloneSorted();
}
