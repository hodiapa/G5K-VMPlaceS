package Common;

import Launcher.Main;

/**
 * Created with IntelliJ IDEA.
 * User: alebre
 * Date: 21/08/13
 * Time: 13:10
 * To change this template use File | Settings | File Templates.
 */
public class Log {
    public static void info(String s){
        System.out.println(Thread.currentThread().getClass().getName()+" - "+Main.getCurrentTime()/1000+","+ Main.getCurrentTime()%1000+ " - "+s);
    }
    public static void err(String s){
        System.err.println(Thread.currentThread().getClass().getName()+" - "+Main.getCurrentTime()/1000+","+ Main.getCurrentTime()%1000+" - "+s);
    }
}
