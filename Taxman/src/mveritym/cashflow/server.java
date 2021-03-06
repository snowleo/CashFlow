package mveritym.cashflow;

//Imports for MyPlugin
import com.nijikokun.register.payment.Methods;

//Bukkit Imports
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

public class server extends ServerListener {
 // Change "MyPlugin" to the name of your MAIN class file.
 // Let's say my plugins MAIN class is: MyPlugin.java
 // I would change "MyPlugin" to "MyPlugin"
 private CashFlow cashFlow;
 private Methods Methods = null;

 public server(CashFlow cashFlow) {
     this.cashFlow = cashFlow;
     this.Methods = new Methods();
 }

 @Override
 public void onPluginDisable(PluginDisableEvent event) {
     // Check to see if the plugin thats being disabled is the one we are using
     if (this.Methods != null && com.nijikokun.register.payment.Methods.hasMethod()) {
         Boolean check = com.nijikokun.register.payment.Methods.checkDisabled(event.getPlugin());

         if(check) {
             this.cashFlow.method = null;
             System.out.println("[" + cashFlow.info.getName() + "] Payment method was disabled. No longer accepting payments.");
         }
     }
 }

 @Override
 public void onPluginEnable(PluginEnableEvent event) {
     // Check to see if we need a payment method
     if (!com.nijikokun.register.payment.Methods.hasMethod()) {
         if(com.nijikokun.register.payment.Methods.setMethod(event.getPlugin().getServer().getPluginManager())) {
             // You might want to make this a public variable inside your MAIN class public method method = null;
             // then reference it through this.plugin.Method so that way you can use it in the rest of your plugin ;)
             this.cashFlow.method = com.nijikokun.register.payment.Methods.getMethod();
             System.out.println("[" + cashFlow.info.getName() + "] Payment method found (" + this.cashFlow.method.getName() + " version: " + this.cashFlow.method.getVersion() + ")");
         } else {
        	 System.out.println("Payment method not found. Disabling plugin.");
        	 this.cashFlow.pluginManager.disablePlugin(this.cashFlow);
         }
     }
 }
}
