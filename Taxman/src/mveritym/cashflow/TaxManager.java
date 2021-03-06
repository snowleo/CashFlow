package mveritym.cashflow;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

import com.nijikokun.register.payment.Method.MethodAccount;

@SuppressWarnings("deprecation")
public class TaxManager {
	protected static CashFlow cashFlow;
    protected static Configuration conf;
    protected static Configuration uconf;
    protected File confFile;
    List<String> taxes;
    List<String> payingGroups;
    List<String> payingPlayers;
    ListIterator<String> iterator;
    Timer timer = new Timer();
    Collection<Taxer> taxTasks = new ArrayList<Taxer>();
    
	public TaxManager(CashFlow cashFlow) {
    	TaxManager.cashFlow = cashFlow;
    	
    	conf = null;
    	
    	loadConf();
    	taxes = conf.getStringList("taxes.list", null);
    }
    
    public void loadConf() {
    	File f = new File(TaxManager.cashFlow.getDataFolder(), "config.yml");

        if (f.exists()) {
        	conf = new Configuration(f);
        	conf.load();
        }
        else {
        	System.out.println("[" + TaxManager.cashFlow.info.getName() + "] No CashFlow config file found. Creating config file.");
        	this.confFile = new File(TaxManager.cashFlow.getDataFolder(), "config.yml");
            TaxManager.conf = new Configuration(confFile);  
            List<String> tempList = null;
            conf.setProperty("taxes.list", tempList);
            conf.save();
        }
    }
    
	public void createTax(CommandSender sender, String name, String tax, String interval, String taxReceiver) {
		String taxName = name;
		double taxInterval = Double.parseDouble(interval);
		List<String> payingGroups = null;
		List<String> payingPlayers = null;
		
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		iterator = taxes.listIterator();
		
		//Checks for if tax is a percent.
		if(tax.contains("%")) {
			double percentIncome = Double.parseDouble(tax.split("%")[0]);
			if(percentIncome > 100 || percentIncome <= 0) {
				sender.sendMessage(ChatColor.RED + "Please choose a % of income between 0 and 100.");
				return;
			}
		} 
		
		//Checks arguments in general.
		if(taxInterval <= 0) {
			sender.sendMessage(ChatColor.RED + "Please choose a tax interval greater than 0.");
			return;
		} else if(!(TaxManager.cashFlow.permsManager.isPlayer(taxReceiver)) && !(taxReceiver.equals("null"))) {
			sender.sendMessage(ChatColor.RED + "Player not found.");
			return;
		} else {
			while(iterator.hasNext()) {
				if(iterator.next().equals(taxName)) {
					sender.sendMessage(ChatColor.RED + "A tax with that name has already been created.");
					return;
				}
			}
		}
		
		taxes.add(taxName);	
		conf.setProperty("taxes.list", taxes);
		conf.setProperty("taxes." + taxName + ".tax", tax);
		conf.setProperty("taxes." + taxName + ".taxInterval", taxInterval);
		conf.setProperty("taxes." + taxName + ".receiver", taxReceiver);
		conf.setProperty("taxes." + taxName + ".payingGroups", payingGroups);
		conf.setProperty("taxes." + taxName + ".payingPlayers", payingPlayers);
		conf.setProperty("taxes." + taxName + ".lastPaid", null);
		conf.setProperty("taxes." + taxName + ".exceptedPlayers", null);
		conf.setProperty("taxes." + taxName + ".onlineOnly.isEnabled", false);
		conf.setProperty("taxes." + taxName + ".onlineOnly.interval", 0.0);
		conf.save();
	
		sender.sendMessage(ChatColor.GREEN + "New tax " + taxName + " created successfully.");
	}
	
	public void deleteTax(CommandSender sender, String name) {
		String taxName = name;
		
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		
		if(taxes.contains(taxName)) {
			taxes.remove(taxName);
			
			for(Taxer task : taxTasks) {
				if(task.getName().equals(name)) {
					task.cancel();
				}
			}
			
			conf.setProperty("taxes.list", taxes);
			conf.removeProperty("taxes." + taxName);
			conf.save();
			
			sender.sendMessage(ChatColor.GREEN + "Tax " + taxName + " deleted successfully.");
		} else {
			sender.sendMessage(ChatColor.RED + "No tax, " + taxName);
		}
		
		return;
	}
	
	public void taxInfo(CommandSender sender, String taxName) {
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		
		if(taxes.contains(taxName)) {
			sender.sendMessage(ChatColor.BLUE + "Tax: " + conf.getString("taxes." + taxName + ".tax"));
			sender.sendMessage(ChatColor.BLUE + "Interval: " + conf.getString("taxes." + taxName + ".taxInterval") + " hours");
			sender.sendMessage(ChatColor.BLUE + "Receiving player: " + conf.getString("taxes." + taxName + ".receiver"));
			sender.sendMessage(ChatColor.BLUE + "Paying groups: " + conf.getStringList("taxes." + taxName + ".payingGroups", null));
			sender.sendMessage(ChatColor.BLUE + "Paying players: " + conf.getStringList("taxes." + taxName + ".payingPlayers", null));
			sender.sendMessage(ChatColor.BLUE + "Excepted users: " + conf.getStringList("taxes." + taxName + ".exceptedPlayers", null));
			sender.sendMessage(ChatColor.BLUE + "Online only: " + conf.getBoolean("taxes." + taxName + ".onlineOnly.isEnabled", false)
		    		+ ", Online interval: " + conf.getDouble("taxes." + taxName + ".onlineOnly.interval", 0.0) + " hours");
		} else {
			sender.sendMessage(ChatColor.RED + "Tax not found.");
		}
		
		return;
	}
	
	public void listTaxes(CommandSender sender) {
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		iterator = taxes.listIterator();
		
		if(taxes.size() != 0) {
			while(iterator.hasNext()) {
				sender.sendMessage(ChatColor.BLUE + iterator.next());
			}
		} else {
			sender.sendMessage(ChatColor.RED + "No taxes to list.");
		}
	}
	
	public void addGroups(CommandSender sender, String taxName, String groups) {
		String[] groupNames = groups.split(",");
		for(String name : groupNames) {
			addGroup(sender, taxName, name);
		}
	}

	public void addGroup(CommandSender sender, String taxName, String groupName) {
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		payingGroups = conf.getStringList("taxes." + taxName + ".payingGroups", null);
		
		if(!(taxes.contains(taxName))) {
			sender.sendMessage(ChatColor.RED + "Tax not found.");
		} else if(!(TaxManager.cashFlow.permsManager.isGroup(groupName))){
			sender.sendMessage(ChatColor.RED + "Group not found.");
		} else {
			sender.sendMessage(ChatColor.GREEN + taxName + " applied successfully to " + groupName);
			payingGroups.add(groupName);
			conf.setProperty("taxes." + taxName + ".payingGroups", payingGroups);
			conf.save();
		}
		
		return;
	}
	
	public void addPlayers(CommandSender sender, String taxName, String players) {
		String[] playerNames = players.split(",");
		for(String name : playerNames) {
			addPlayer(sender, taxName, name);
		}
	}
	
	public void addPlayer(CommandSender sender, String taxName, String playerName) {
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		payingPlayers = conf.getStringList("taxes." + taxName + ".payingPlayers", null);
		
		if(!(taxes.contains(taxName))) {
			sender.sendMessage(ChatColor.RED + "Tax not found.");
		} else if(!(TaxManager.cashFlow.permsManager.isPlayer(playerName.toLowerCase()))){
			sender.sendMessage(ChatColor.RED + "Player not found.");
		} else if(payingPlayers.contains(playerName.toLowerCase())) {
			sender.sendMessage(ChatColor.RED + playerName + " is already paying this tax.");
		} else {
			sender.sendMessage(ChatColor.GREEN + taxName + " applied successfully to " + playerName);
			payingPlayers.add(playerName);
			conf.setProperty("taxes." + taxName + ".payingPlayers", payingPlayers);
			conf.save();
		}
		
		return;
	}
	
	public void removeGroups(CommandSender sender, String taxName, String groups) {
		String[] groupNames = groups.split(",");
		for(String name : groupNames) {
			removeGroup(sender, taxName, name);
		}
	}
	
	public void removeGroup(CommandSender sender, String taxName, String groupName) {
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		payingGroups = conf.getStringList("taxes." + taxName + ".payingGroups", null);
		
		if(!(taxes.contains(taxName))) {
			sender.sendMessage(ChatColor.RED + "Tax not found.");
		} else if(!(payingGroups.contains(groupName))) {
			sender.sendMessage(ChatColor.RED + "Group not found.");
		} else {
			sender.sendMessage(ChatColor.GREEN + taxName + " removed successfully from " + groupName);
			payingGroups.remove(groupName);
			conf.setProperty("taxes." + taxName + ".payingGroups", payingGroups);
			conf.save();
		}
	}
	
	public void removePlayers(CommandSender sender, String taxName, String players) {
		String[] playerNames = players.split(",");
		for(String name : playerNames) {
			removePlayer(sender, taxName, name);
		}
	}
	
	public void removePlayer(CommandSender sender, String taxName, String playerName) {
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		payingPlayers = conf.getStringList("taxes." + taxName + ".payingPlayers", null);
		
		if(!(taxes.contains(taxName))) {
			sender.sendMessage(ChatColor.RED + "Tax not found.");
		} else if(!(payingPlayers.contains(playerName))) {
			sender.sendMessage(ChatColor.RED + "Player not found.");
		} else {
			sender.sendMessage(ChatColor.GREEN + taxName + " removed successfully from " + playerName);
			payingPlayers.remove(playerName);
			conf.setProperty("taxes." + taxName + ".payingPlayers", payingPlayers);
			conf.save();
		}
	}


	public void enable() {		
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		Double hours;
		Date lastPaid;
		
		for(String taxName : taxes) {
			hours = Double.parseDouble(conf.getString("taxes." + taxName + ".taxInterval"));
			lastPaid = (Date) conf.getProperty("taxes." + taxName + ".lastPaid");
			System.out.println("[" + TaxManager.cashFlow.info.getName() + "] Enabling " + taxName);
			Taxer taxer = new Taxer(this, taxName, hours, lastPaid);
			taxTasks.add(taxer);
		}
	}
	
	public List<String> checkOnline(List<String> users, Double interval) {
		List<String> tempPlayerList = new ArrayList<String>();			
		for(String player : users) {
			if(interval == 0 && PermissionsManager.cashflow.getServer().getPlayer(player) != null) {
				tempPlayerList.add(player);
			} else if(interval != 0 && SalaryManager.cashFlow.playerLogManager.didLog(player, interval)) {
				tempPlayerList.add(player);
			}
		}
		return tempPlayerList;
	}
	
	public void payTax(String taxName) {
		System.out.println("[" + TaxManager.cashFlow.info.getName() + "] Paying tax " + taxName);
		
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		
		conf.setProperty("taxes." + taxName + ".lastPaid", new Date());
		conf.save();
		
		List<String> groups = conf.getStringList("taxes." + taxName + ".payingGroups", null);
		List<String> players = conf.getStringList("taxes." + taxName + ".payingPlayers", null);
		List<String> exceptedPlayers = conf.getStringList("taxes." + taxName + ".exceptedPlayers", null);
		String tax = conf.getString("taxes." + taxName + ".tax");
		String receiver = conf.getString("taxes." + taxName + ".receiver");
		Double taxRate;
		
		List<String> users = TaxManager.cashFlow.permsManager.getUsers(groups, players, exceptedPlayers);
		
		if(conf.getBoolean("taxes." + taxName + ".onlineOnly.isEnabled", false)) {
			Double onlineInterval = conf.getDouble("taxes." + taxName + ".onlineOnly.interval", 0);
			users = checkOnline(users, onlineInterval);
		}
		
		for(String user : users) {
			if(TaxManager.cashFlow.method.hasAccount(user)) {
				MethodAccount userAccount = TaxManager.cashFlow.method.getAccount(user);
				Player player = TaxManager.cashFlow.getServer().getPlayer(user);
				DecimalFormat twoDForm = new DecimalFormat("#.##");
				
				if(tax.contains("%")) {
					taxRate = Double.parseDouble(tax.split("%")[0]) / 100.0;
					taxRate *= userAccount.balance();
				} else {
					taxRate = Double.parseDouble(tax);
				}

				taxRate = Double.valueOf(twoDForm.format(taxRate));
				userAccount.subtract(taxRate);
				
				if(player != null) {
					String message = "You have paid $" + taxRate + " in tax";
					if(receiver.equals("null")) {
						message += ".";
					} else {
						message += " to " + receiver + ".";
					}
					player.sendMessage(ChatColor.BLUE + message);
				}
				
				if(!(receiver.equals("null"))) {
					MethodAccount receiverAccount = TaxManager.cashFlow.method.getAccount(receiver);
					Player receiverPlayer = TaxManager.cashFlow.getServer().getPlayer(receiver);
					
					receiverAccount.add(taxRate);
					
					if(receiverPlayer != null) {
						receiverPlayer.sendMessage(ChatColor.BLUE + "You have received $" + taxRate + " in tax from " + user + ".");
					}
				}
			}
		}
	}
	
	public void disable() {
		for(Taxer taxTask : taxTasks) {
			taxTask.cancel();
		}
	}
	
	public void setOnlineOnly(String taxName, Boolean online, Double interval) {
		loadConf();
		conf.setProperty("taxes." + taxName + ".onlineOnly.isEnabled", online);
		conf.setProperty("taxes." + taxName + ".onlineOnly.interval", interval);
		conf.save();
		return;
	}
	
	public void addException(CommandSender sender, String taxName, String userName) {
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		List<String> exceptedPlayers = conf.getStringList("taxes." + taxName + ".exceptedPlayers", null);
		
		if(!(taxes.contains(taxName))) {
			sender.sendMessage(ChatColor.RED + "Tax not found.");
		} else if(taxes.contains(userName)) {
			sender.sendMessage(ChatColor.RED + userName + " is already listed as excepted.");
		} else {
			sender.sendMessage(ChatColor.GREEN + userName + " added as an exception.");
			exceptedPlayers.add(userName);
			conf.setProperty("taxes." + taxName + ".exceptedPlayers", exceptedPlayers);
			conf.save();
		}
		
		return;
	}
	
	public void removeException(CommandSender sender, String taxName, String userName) {
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		List<String> exceptedPlayers = conf.getStringList("taxes." + taxName + ".exceptedPlayers", null);
		
		if(!(taxes.contains(taxName))) {
			sender.sendMessage(ChatColor.RED + "Tax not found.");
		} else if(!(exceptedPlayers.contains(userName))) {
			sender.sendMessage(ChatColor.RED + "Player not found.");
		} else {
			sender.sendMessage(ChatColor.GREEN + userName + " removed as an exception.");
			exceptedPlayers.remove(userName);
			conf.setProperty("taxes." + taxName + ".exceptedPlayers", exceptedPlayers);
			conf.save();
		}
	}
	
	public void setRate(CommandSender sender, String taxName, String tax) {
		loadConf();
		taxes = conf.getStringList("taxes.list", null);
		
		if(!(taxes.contains(taxName))) {
			sender.sendMessage(ChatColor.RED + "Tax not found.");
			return;
		} else if(tax.contains("%")) {
			double percentIncome = Double.parseDouble(tax.split("%")[0]);
			if(percentIncome > 100 || percentIncome <= 0) {
				sender.sendMessage(ChatColor.RED + "Please choose a % of income between 0 and 100.");
				return;
			}
		}
		
		conf.setProperty("taxes." + taxName + ".tax", tax);
		conf.save();
		sender.sendMessage(ChatColor.GREEN + "Rate of tax " + taxName + " is set to " + tax + ".");
	}
}
