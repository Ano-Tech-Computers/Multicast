package com.floyd.bukkit.multicast;


import java.io.*;

import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
//import org.bukkit.Server;
//import org.bukkit.event.Event.Priority;
//import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
//import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
//import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import java.util.logging.Logger;

//import com.nijikokun.bukkit.Permissions.Permissions;

/**
* Multicast plugin for Bukkit
*
* @author FloydATC
*/
public class Multicast extends JavaPlugin implements Listener {
//    private final MulticastPlayerListener playerListener = new MulticastPlayerListener(this);

    private final ConcurrentHashMap<Player, Boolean> debugees = new ConcurrentHashMap<Player, Boolean>();
    public final ConcurrentHashMap<String, String> settings = new ConcurrentHashMap<String, String>();

//    public static Permissions Permissions = null;
    
    String baseDir = "plugins/Multicast";
    String configFile = "settings.txt";

	public static final Logger logger = Logger.getLogger("Minecraft.Multicast");
    
//    public Multicast(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
//        super(pluginLoader, instance, desc, folder, plugin, cLoader);
//        // TODO: Place any custom initialization code here
//
//        // NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled
//    }

    public void onDisable() {
        // TODO: Place any custom disable code here

        // NOTE: All registered events are automatically unregistered when a plugin is disabled
    	
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
    	PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
    }

    public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events

    	preFlightCheck();
//    	setupPermissions();
    	loadSettings();
    	//registerCommands();
    	
        // Register our events
        PluginManager pm = getServer().getPluginManager();
//        pm.registerEvent(Event.Type.PLAYER_COMMAND, playerListener, Priority.Normal, this);
//        pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Lowest, this);
        pm.registerEvents((Listener) this, this);


        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event){

        // when someone uses /help command, change it to /me is stupid!
    	//System.out.println("[MC] preprocessing "+event.getMessage());
        // split the command
        String[] args = event.getMessage().split(" ", 2);

        if(args.length>1){
        	String cmd = args[0];
        	//System.out.println("[MC] cmd = "+cmd);
        	for (String channel : settings.keySet()) {
            	//System.out.println("[MC] channel = "+channel);
        		if (cmd.equalsIgnoreCase("/" + channel)) {
                	//System.out.println("[MC] matched");
                	event.setCancelled(true);
                	Player p = event.getPlayer();
                	if (p != null) {
                		p.chat("/multicast "+channel+" "+args[1]);
                	}
        			break;
        		}
        	}
        }

    }

    public boolean isDebugging(final Player player) {
        if (debugees.containsKey(player)) {
            return debugees.get(player);
        } else {
            return false;
        }
    }

    public void setDebugging(final Player player, final boolean value) {
        debugees.put(player, value);
    }
    
    
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args ) {
    	String cmdname = cmd.getName().toLowerCase();
        Player player = null;
        String pname = "(Server)";
        if (sender instanceof Player) {
        	player = (Player)sender;
        	pname = player.getName();
        }

        for (String command : settings.keySet()) {
        	if (cmdname.equalsIgnoreCase(command)) {
        		args = unshift(cmdname, args);
        		cmdname = "multicast";
        	}
        }
        

        
        if ((cmdname.equalsIgnoreCase("multicast") || cmdname.equalsIgnoreCase("mc")) && args.length >= 2) {
        	String message = "";
        	for (Integer index=1; index<args.length; index++ ) {
        		message = message.concat(" " + args[index]);
        	}
            for (String command : settings.keySet()) {
            	if (args[0].equalsIgnoreCase(command)) {
//            		if ( player == null || Permissions.Security.permission(player, "multicast." + command + ".send")) {
            		if ( player == null || player.hasPermission("multicast." + command + ".send")) {
            			String sendername = pname;
            			if (player != null) {
	            			String worldname = player.getLocation().getWorld().getName();
	    	        		String group = groupByPlayername(pname, worldname);
	    	        		String prefix = prefixByGroupname(group, worldname);
	    	        		String suffix = suffixByGroupname(group, worldname);
	    	        		sendername = prefix + pname + suffix;
            			}
    	        		String channel = settings.get(command);
    	            	Player[] players = getServer().getOnlinePlayers();
    	            	for (Player p: players) {
    	            		if (p.hasPermission("multicast." + command + ".receive")) {
    	            			p.sendMessage(sendername + "->" + channel + ":" + message);
    	            		}
    	            	}
    					logger.info(pname + "->" + channel + ":" + message);
    	                return true;
            		} else {
                		logger.info("[Multicast] Permission denied for '" + command + "': " + pname);
            		}
            	}
            }

        }
        return false;
    }
    
    
    private void loadSettings() {
    	String fname = baseDir + "/" + configFile;
		String line = null;

		// Load the settings hash with defaults
		settings.put("a", "Admins");
		settings.put("o", "Ops");

		// Read the current file (if it exists)
		try {
    		BufferedReader input =  new BufferedReader(new FileReader(fname));
    		while (( line = input.readLine()) != null) {
    			line = line.trim();
    			if (!line.startsWith("#") && line.contains("=")) {
    				String[] pair = line.split("=", 2);
    				settings.put(pair[0], pair[1]);
    			}
    		}
    	}
    	catch (FileNotFoundException e) {
			logger.warning( "Error reading " + e.getLocalizedMessage() + ", using defaults" );
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
/*    private void registerCommands() {
    	for (String cmd: settings.keySet()) {
    		Command obj = getCommand(cmd);
    		if (obj == null) {
    			logger.warning("[Multicast] Dynamic registration of command '"+cmd+"' failed (returned NULL)");
    		}
    	}
    }
*/    
    private void preFlightCheck() {
    	String fname = "";
    	File f;
    	
    	// Ensure that baseDir exists
    	fname = baseDir;
    	f = new File(fname);
    	if (!f.exists()) {
    		if (f.mkdir()) {
    			logger.info( "Created directory '" + fname + "'" );
    		}
    	}
    	// Ensure that configFile exists
    	fname = baseDir + "/" + configFile;
    	f = new File(fname);
    	if (!f.exists()) {
			// Ensure that configFile exists
			BufferedWriter output;
			String newline = System.getProperty("line.separator");
			try {
				output = new BufferedWriter(new FileWriter(fname));
				output.write("# command=Channelname" + newline);
				output.write("a=Admins" + newline);
				output.write("o=Ops" + newline);
				output.close();
    			logger.info( "Created config file '" + fname + "'" );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    }
    
    // Prepend a string to an array and return the new array
    private String[] unshift(String str, String[] array) {
    	String[] newarray = new String[array.length+1];
    	newarray[0] = str;
    	for (Integer i=0; i<array.length; i++) {
    		newarray[i+1] = array[i];
    	}
    	return newarray;
    }

    private String groupByPlayername(String playername, String worldname) {
//    	return Permissions.Security.getGroup(worldname, playername);
    	return "default"; // TODO
    }
    
    private String prefixByGroupname(String groupname, String worldname) {
//    	String prefix = Permissions.Security.getGroupPrefix(worldname, groupname);
    	String prefix = null; // TODO
    	if (prefix == null) { prefix = ""; }
    	return prefix;
    }
    
    private String suffixByGroupname(String groupname, String worldname) {
    	String suffix = null; // TODO
//    	String suffix = Permissions.Security.getGroupSuffix(worldname, groupname);
    	if (suffix == null) { suffix = ""; }
    	return suffix; 
    }
    
}

