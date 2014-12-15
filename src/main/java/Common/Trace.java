package Common;

import Launcher.Main;
import entropy.configuration.VirtualMachine;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: alebre
 * Date: 11/07/13
 * Time: 09:33
 * To change this template use File | Settings | File Templates.
 */
public class Trace {


    // All.csv file (from pjdump)
    // type, NodeName, VARIABLE, START, END, DURATION, [0,] VALUE
    // if type is State we need to add 0,

    static Trace instance = null;

    private FileWriter fw = null;
    private static BufferedWriter output = null;

    private HashMap<String, Double> values;

    public static Trace getTracer() {
        if (instance == null)
            instance = new Trace();
        return instance;
    }

    protected void finalize() throws Throwable {
        try {
            output.close();
        } finally {
            super.finalize();
        }
        // TODO check but this finalize method is not invoked and thus the consolidation does not occur.
        Trace.consolidateTraces(SimulatorProperties.getTraceFile());
    }

    private static String FindDurationState(String fileName, String key, int currentLine, String stateName) {

        String result = null;
        BufferedReader br = null;
        try {

            br = new BufferedReader(new FileReader(fileName));
            String sCurrentLine;

            // Skip first lines until the current one
            for (int i = 0; i <= currentLine; i++)
                br.readLine();

            // Looks for the next event
            while ((sCurrentLine = br.readLine()) != null) {
                // System.out.println(sCurrentLine);
                if (sCurrentLine.contains(key) && !sCurrentLine.contains(stateName))
                    break;
            }

            // If we found the corresponding value
            if (sCurrentLine != null) {
                String[] splittedLine = sCurrentLine.replaceAll("\\s","").split(",");
                result = splittedLine[3];
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    public static void consolidateTraces(String fileName) {
        BufferedReader br = null;
        BufferedWriter bw = null;
        int currentLine = 0;
        // long startingTime = 0;
        double duration = 0;

        try {

            String sCurrentLine;

            br = new BufferedReader(new FileReader(fileName));
            bw = new BufferedWriter(new FileWriter("all.csv"));

            // skip the firstLine
            br.readLine();
            // startingTime=Long.parseLong(sCurrentLine);

            while ((sCurrentLine = br.readLine()) != null) {
                //  System.out.println(sCurrentLine);
                // System.out.println(currentLine);
                currentLine++;

                // remove all spaces, tabulation, ... of the line, then split it using "," as separator
                String[] splittedLine = sCurrentLine.replaceAll("\\s","").split(",");

                String sNodeName = splittedLine[1];
                String sVariableName = splittedLine[2];
                String sStart = splittedLine[3];
                String sValue = splittedLine[4];

                String sEnd;

                String kindOfLine = "Variable";
                try {
                    Double.parseDouble(sValue);
                } catch (NumberFormatException ex) {
                    kindOfLine = "State";
                }

                String traceLine = "";
                // There is two kind of lines: "Variable" and "State"
                if (kindOfLine.equals("Variable")) {

                    sEnd = sStart;
                    duration = Double.parseDouble(sEnd) - Double.parseDouble(sStart);

                    traceLine = String.format("%s, %s, %s, %s, %s, %s, %s\n",
                        kindOfLine,
                        sNodeName,
                        sVariableName,
                        sStart,
                        sEnd,
                        duration,
                        sValue
                    );

                } else if (kindOfLine.equals("State")) {

                    // In this case, the value is a State
                    String stateName = sValue;

                    String sKey = String.format("id, %s, %s",
                            sNodeName,
                            sVariableName)
                    ;

                    sEnd = FindDurationState(fileName, sKey, currentLine, stateName);
                    if (sEnd == null)
                        sEnd = "" + ((long) (SimulatorProperties.getDuration()));
                    duration = Double.parseDouble(sEnd) - Double.parseDouble(sStart);


                     traceLine = String.format("%s, %s, %s, %s, %s, %s, 0, %s\n",
                            kindOfLine,
                            sNodeName,
                            sVariableName,
                            sStart,
                            sEnd,
                            duration,
                            stateName
                    );
                }

                bw.append(traceLine);
                bw.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    public Trace() {
        try {
            fw = new FileWriter(SimulatorProperties.getTraceFile());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        output = new BufferedWriter(fw);
        values = new HashMap<String, Double>();

        String sStart = "" + Main.getStartingTime() + "\n";
        try {
            output.append(sStart);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }

    private void pushTrace(String hostName, String variable, String value) {
        long curTime = Main.getCurrentTime();

        String time = String.format("%d.%d",
            TimeUnit.MILLISECONDS.toSeconds(curTime),
            TimeUnit.MILLISECONDS.toMillis(curTime) - TimeUnit.MILLISECONDS.toSeconds(curTime) * 1000
        );

        String line = String.format("id, %s, %s, %s, %s\n",
            hostName,
            variable,
            time,
            value
        );

        try {
            output.append(line);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void hostSetState(String hostName, String variable, String state) {
        this.pushTrace(hostName, variable, state);
    }

    public void hostPushState(String hostName, String variable, String state) {
        this.pushTrace(hostName, variable, state);
    }

    private void addValues(String key, double value) {
        this.values.put(key, new Double(value));

    }

    private double getValFromValues(String key) {
        return this.values.get(key);
    }

    public void hostVariableSet(String hostName, String variable, double value) {
        addValues(hostName + variable, value);
        this.pushTrace(hostName, variable, "" + value);

    }


    public void hostVariableAdd(String hostName, String variable, double value) {
        double newValue = getValFromValues(hostName + variable) + value;
        addValues(hostName + variable, newValue);
        this.pushTrace(hostName, variable, "" + newValue);
    }

    public static void main(String args[]) {
        Trace.consolidateTraces(SimulatorProperties.getTraceFile());
        /*
        long startingTime = System.currentTimeMillis();
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        long duration = System.currentTimeMillis() - startingTime ;
       System.out.println(
               String.format("%d.%d",
                       TimeUnit.MILLISECONDS.toSeconds(duration),
                       TimeUnit.MILLISECONDS.toMillis(duration) -
                               (TimeUnit.MILLISECONDS.toSeconds(duration)*1000)));

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        duration = System.currentTimeMillis() - startingTime ;
       System.out.println(
               String.format("%d.%d",
                       TimeUnit.MILLISECONDS.toSeconds(duration),
                       TimeUnit.MILLISECONDS.toMillis(duration) -
                               (TimeUnit.MILLISECONDS.toSeconds(duration)*1000)));
*/
    }
}
