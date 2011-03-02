package com.nidefawl.Achievements;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.File;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.ensifera.animosity.craftirc.CraftIRC;
import com.nidefawl.Achievements.Messaging.AchLister;
import com.nidefawl.Achievements.Messaging.AchMessaging;
import com.nidefawl.MyGeneral.MyGeneral;
import com.nidefawl.Stats.Stats;
import com.nidefawl.Stats.StatsSettings;

public class Achievements extends JavaPlugin {
	public final static Logger log = Logger.getLogger("Minecraft");
	public String color = "&b";
	public String obtainedColor = "&a";
	public boolean enabled = false;
	public boolean useSQL = false;
	public boolean useCraftIRC = false;
	public final static String version = "0.54";
	public final static String logprefix = "[Achievements-" + version + "]";
	private final static Yaml yaml = new Yaml(new SafeConstructor());
	private String name = "Achievements";
	private String directory = "achievements";
	private String listLocation = "achievements.txt";
	private long delay = 300;
	private Stats statsInstance = null;
	private AchievementsListener listener = null;
	public HashMap<String, AchievementListData> achievementList = new HashMap<String, AchievementListData>();
	public HashMap<String, PlayerAchievement> playerAchievements = new HashMap<String, PlayerAchievement>();
	public List<String> formatAchDetail = new ArrayList<String>();
	public List<String> formatAchList = new ArrayList<String>();
	public String formatAchNotifyBroadcast = "+playername has been awarded +achname!";
	public String formatAchNotifyPlayer = null;
	public ArrayList<String> ircTags = new ArrayList<String>();
	protected final Object achsLock = new Object();
	public int achsPerPage = 8;

	private String dbUrl;
	private String dbUsername;
	private String dbPassword;
	protected String dbTable;


	boolean CheckStatsPlugin() {
		if (statsInstance != null)
			return true;
		Plugin plug = getServer().getPluginManager().getPlugin("Stats");
		if (plug == null) {
			log.log(Level.SEVERE, logprefix + " Stats plugin not found, aborting load of " + name);
			return false;
		}
		statsInstance = (Stats) plug;
		log.info(logprefix + " Found required plugin: " + plug.getDescription().getName());
		return true;
	}
	public Stats Stats() {
		CheckStatsPlugin();
		return statsInstance;
	}
	public boolean loadConfig() {
		this.enabled = false;
		if (!new File(directory).exists()) {
			try {
				(new File(directory)).mkdir();
			} catch (Exception ex) {
				log.log(Level.SEVERE, logprefix + " Unable to create " + directory + "/directory");
			}
			log.log(Level.INFO, logprefix + " directory '" + directory + "' created!");
			log.log(Level.INFO, logprefix + " make sure to check achievements/achievements.properties and mysql.properties ");
		}
		AchPropertiesFile properties = new AchPropertiesFile(new File("achievements"+File.separator+"achievements.properties"));
		this.listLocation = properties.getString("achievements-list", "achievements.txt", "");
		this.delay = (long)properties.getInt("achievements-delay", 60, "");
	    this.color = ("&" + properties.getString("achievements-color", "b", ""));
	    this.obtainedColor = ("&" + properties.getString("achievements-obtainedcolor", "a", ""));
	    this.useSQL = properties.getBoolean("achievements-use-sql", false, "");
	    this.useCraftIRC = properties.getBoolean("achievements-craftirc", CheckCraftIRC(), "");
		for(String tag : properties.getString("achievements-craftirc-tags", "admin,defaul", "").split(",")) {
			ircTags.add(tag.trim());
		}
	    this.achsPerPage = properties.getInt("achievements-list-perpage", 8, "");
		this.formatAchNotifyBroadcast = properties.getString("achievements-format-notifybroadcast", "&b+playername has been awarded +achname!", "check documentation for details");
		this.formatAchNotifyPlayer = properties.getString("achievements-format-notifyplayer", "(+description)", "");
		this.formatAchList.add(properties.getString("achievements-format-list", "+id +shortenedachname &6[&f+category&6 - &f+key&6:&f +value&6]", "check documentation for details"));
		this.formatAchList.add(properties.getString("achievements-format-list2", "", ""));
		this.formatAchDetail.add(properties.getString("achievements-format-detail", "+id Name: +achname", ""));
		this.formatAchDetail.add(properties.getString("achievements-format-detail2", "Desc: +description", ""));
		this.formatAchDetail.add(properties.getString("achievements-format-detail3", "Requirement: &6[&f+category&6 - &f+key&6:&f +value&6]", ""));
		this.formatAchDetail.add(properties.getString("achievements-format-detail4", "Reward: &6[&f+reward&6]", ""));
		this.formatAchDetail.add(properties.getString("achievements-format-detail5", "", ""));
		this.formatAchDetail.add(properties.getString("achievements-format-detail6", "", ""));
		this.formatAchDetail.add(properties.getString("achievements-format-detail7", "", ""));
		this.formatAchDetail.add(properties.getString("achievements-format-detail8", "", ""));
		properties.save();
		
		
		if (!CheckStatsPlugin()) {
			Disable();
			return false;	
		}
		if (useCraftIRC && getServer().getPluginManager().getPlugin("CraftIRC") == null) {
			log.log(Level.INFO, logprefix + " CraftIRC not found. Disabling this feature");
		}

		
		achievementList = AchievementsLoader.LoadAchievementsList(this, directory, listLocation);
		if(achievementList==null||achievementList.isEmpty()) {
			Achievements.log.info(Achievements.logprefix + " " + listLocation + " is empty");
			onDisable();
			return false;
		}

		Achievements.log.info(Achievements.logprefix + " loaded " + achievementList.size() + " achievements definitions");
		this.enabled = true;
		return true;
	}

	private Connection conn;
	protected Connection getSQLConnection() throws SQLException {
		if (conn == null || conn.isClosed()) {
			conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
		}
		return conn;
	}
	public int getIndexOfAch(String achName) {
		int index = 0;
		for(String a : achievementList.keySet()) {
			if(a.equals(achName))
			return index;
			index++;
		}
		return -1;
	}
	public void onEnable() {
	}
	public void Enable() {
		formatAchDetail = new ArrayList<String>();
		formatAchList = new ArrayList<String>();
		achievementList = new HashMap<String, AchievementListData>();
		playerAchievements = new HashMap<String, PlayerAchievement>();
		if(!loadConfig()) return;

		
		if(useSQL) {
			AchPropertiesFile properties = new AchPropertiesFile(new File("mysql.properties"));
			dbUrl = properties.getString("sql-db", "jdbc:mysql://localhost:3306/minecraft", "");
			dbUsername = properties.getString("sql-user", "root", "");
			dbPassword = properties.getString("sql-pass", "root", "");
			dbTable = properties.getString("sql-table-achievements", "playerachievements", "");
			properties.save();
	
			try {
				Class.forName("com.mysql.jdbc.Driver");
				if (getSQLConnection() == null) {
					log.log(Level.SEVERE, logprefix + " MySQL-Connection could not be established");
					return;
				}
				if(!checkSchema()) {
					log.log(Level.SEVERE,this.getClass().getName()+": Could not create table. Stats is disabled");
					return;
				}
			} catch (SQLException e) {
				log.log(Level.SEVERE, logprefix + " MySQL-Connection could not be established");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				log.log(Level.SEVERE, logprefix + " MySQL-Connection could not be established");
				e.printStackTrace();
			}
		}
		listener = new AchievementsListener();
		CheckCraftIRC();
		initialize();
		log.info(logprefix + " " + name + " " + version + " Plugin Enabled");

		for (Player p : getServer().getOnlinePlayers()) {
			load(p);
		}
		getServer().getScheduler().scheduleAsyncRepeatingTask(this, new CheckerTask(this), delay*20, delay*20);
	}
	private static class CheckerTask implements Runnable {
		private Achievements achievements;

		CheckerTask(Achievements plugin) {
			achievements = plugin;
		}

		public void run() {
			achievements.checkAchievements();
		}
	}
	public void Disable() {
		checkAchievements();
		enabled = false;
		getServer().getScheduler().cancelTasks(this);
		playerAchievements = null;
		try {
			if(conn!=null)
			conn.close();
		} catch (SQLException e) {
			log.info(logprefix+" Error on closing MySQLConnection:"+e.getMessage());
			e.printStackTrace();
		}
		log.info(logprefix + " " + name + " " + version + " Plugin Disabled");
	}

	public void onDisable() {
	}

	public void initialize() {

		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, listener, Priority.Normal, this);
		getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, listener, Priority.Normal, this);

	}


	private MyGeneral myGeneralInstance = null;
	boolean CheckMyGeneral() {
		if (myGeneralInstance != null)
			return true;
		Plugin plug = this.getServer().getPluginManager().getPlugin("MyGeneral");
		if (plug != null) {
			myGeneralInstance = (MyGeneral) plug;
			log.info(logprefix + " Found supported plugin: " + plug.getDescription().getName());
			return true;
		}
		return false;
	}
	public MyGeneral myGeneral() {
		CheckMyGeneral();
		return myGeneralInstance;
	}
	public CraftIRC craftirc = null;
	public boolean CheckCraftIRC() {
		if (craftirc != null)
			return true;
		Plugin plug = this.getServer().getPluginManager().getPlugin("CraftIRC");
		if (plug != null) {
			craftirc = (CraftIRC) plug;
			log.info(logprefix + " Found supported plugin: " + plug.getDescription().getName());
			return true;
		}
		return false;
	}
	protected AchievementListData getAchievement(String name) {
		return achievementList.get(name);
	}

	private void checkAchievements() {
		if(!enabled) return;
		
		if(StatsSettings.debugOutput) log.info(logprefix + "checking achievements...");
		for (Player p : getServer().getOnlinePlayers()) {
			if (!Stats().Perms().permission(p, "/ach")) continue;
			if(!playerAchievements.containsKey(p.getName()))
				load(p);
			PlayerAchievement pa = playerAchievements.get(p.getName());
			for (String name2 : achievementList.keySet()) {
				AchievementListData ach = achievementList.get(name2);
				if (ach == null||!ach.isEnabled()) 
					continue;
				if (!ach.conditions.meets(this, pa))
					continue;
				int playerValue = Stats().get(p.getName(), ach.getCategory(), ach.getKey());

				if (playerValue < ach.getValue()) // doesn't meet requirements,
					// skip
					continue;
				// award achievement
				if (pa.hasAchievement(ach)) {
					Achievement pad = pa.get(ach.getName());

					// already awarded
					if (pad.getCount() >= ach.getMaxawards())
						continue;

					if (pad.getCount() > 0 && playerValue < ((pad.getCount() + 1) * ach.getValue()))
						continue;

					// check for inventory space
					// player will get rewarded next check if there is no free space
					if(!ach.commands.preCheck(p))
						continue;
					pad.incrementCount();
				} else {

					// check for inventory space
					// player will get rewarded next check if there is no free space
					if(!ach.commands.preCheck(p))
						continue;
					pa.award(ach.getName());
				}

				AchLister.sendAchievementMessage(this, p, ach);
				pa.save();

				ach.commands.run(p);
			}
		}
	}
	private void load(Player player) {
		if (!Stats().Perms().permission(player, "/ach")) {
			if(StatsSettings.debugOutput) log.info(logprefix+" player " + player.getName() + " has no /ach permission. Not loading/logging actions");
			return;
		}
		if (playerAchievements.containsKey(player.getName())) {
			if(StatsSettings.debugOutput) log.info(logprefix + " "+player.getName()+" is already in list. skipping");
			return;
		}
		PlayerAchievement pa;
		if (useSQL) {
			String location = directory + File.separator + player.getName() + ".txt";
			File fold = new File(location);
			pa = new PlayerAchievementSQL(player.getName(),this);
			if (fold.exists()) {
				PlayerAchievement paold = new PlayerAchievementFile(directory, player.getName());
				paold.load();
				File fnew = new File(location + ".old");
				fold.renameTo(fnew);
				pa.copy(paold);
				pa.save();
			}
		} else
			pa = new PlayerAchievementFile(directory, player.getName());
		pa.load();
		playerAchievements.put(player.getName(), pa);
		if(StatsSettings.debugOutput) log.log(Level.INFO, logprefix + " Loaded Player '"+player.getName()+"'");

	}

	private void unload(String player) {
			if (!playerAchievements.containsKey(player)) {
				return;
			}
			PlayerAchievement pa = playerAchievements.get(player);
			pa.save();
			playerAchievements.remove(player);
	}


    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
    	if(!(sender instanceof Player)) return false;
        Player player = (Player)sender;

		if (!Stats().Perms().permission(player, "/ach")) {
			AchMessaging.send(player, ChatColor.RED + "You don't have sufficient permission.");
			return true;
		}
		load(player);
		if (cmd.getName().equals("achievements")) {
			if (playerAchievements == null || playerAchievements.get(player.getName()) == null) {
				AchMessaging.send(player, ChatColor.LIGHT_PURPLE + "You have no achievements.");
				return true;
			}
			AchLister.SendDoneAchList(this, player, args);
			return true;
		}

		if (cmd.getName().equals("listachievements")) {
			if (!Stats().Perms().permission(player, "/listach")) {
				AchMessaging.send(player, ChatColor.RED + "You don't have sufficient permission.");
				return true;
			}
			AchLister.SendAchList(this, player, args);
			return true;
		} 
		if (cmd.getName().equals("checkachievements")) {
			if (!Stats().Perms().permission(player, "/achadmin")) {
				AchMessaging.send(player, ChatColor.RED + "You don't have sufficient permission.");
				return true;
			}
			checkAchievements();
			AchMessaging.send(player, ChatColor.LIGHT_PURPLE + "Achievements updated.");
			return true;
		} else if (cmd.getName().equals("reloadachievements")) {
			if (!Stats().Perms().permission(player, "/achadmin")) {
				AchMessaging.send(player, ChatColor.RED + "You don't have sufficient permission.");
				return true;
			}

			enabled = false;
			Disable();
			Enable();
			CheckMyGeneral();
			AchMessaging.send(player, ChatColor.LIGHT_PURPLE + "Achievements reloaded.");
			return true;
		} else if (cmd.getName().equals("deleteachievements")) {
			if (!Stats().Perms().permission(player, "/achadmin")) {
				AchMessaging.send(player, ChatColor.RED + "You don't have sufficient permission.");
				return true;
			}
			AchMessaging.send(player, ChatColor.LIGHT_PURPLE + "Achievements reloaded.");
			return true;
		}
		return false;
	}

	
	
	
	public static Yaml getYaml() {
		return yaml;
	}

	private class AchievementsListener extends PlayerListener {
		public void onPlayerJoin(PlayerEvent event) {
				load(event.getPlayer());
		}

		public void onPlayerQuit(PlayerEvent event) {
				unload(event.getPlayer().getName());
		}
	}
	private boolean checkSchema() {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean result = false;
		try {
			conn = getSQLConnection();
			DatabaseMetaData dbm = conn.getMetaData();
			rs = dbm.getTables(null, null, dbTable, null);
			if (!rs.next()) {
				ps = conn.prepareStatement("CREATE TABLE `" + dbTable + "` (" + "`player` varchar(32) NOT NULL DEFAULT '-',"
						+ "`achievement` varchar(128) NOT NULL DEFAULT '-'," + "`count` int(11) NOT NULL DEFAULT '0',"
						+ "PRIMARY KEY (`player`,`achievement`));");
				ps.executeUpdate();
				log.info(logprefix + " created table '" + dbTable + "'.");
				result = true;
			} else {
				try {
					rs = dbm.getColumns(null, null, dbTable, "achievement");
					rs.next();
					if (rs.getInt("COLUMN_SIZE") != 128) {
						log.log(Level.SEVERE, logprefix + " Outdated database!");
						log.log(Level.SEVERE, logprefix + " UPGRADING FROM v0.16 TO v0.20");
						try {
							Statement statement = conn.createStatement();
							statement.executeUpdate("ALTER TABLE  `" + dbTable + "` CHANGE  `achievement`  `achievement` VARCHAR( 128 ) NOT NULL DEFAULT  '-'");
							statement.close();
							log.log(Level.SEVERE, logprefix + " UPGRADING SUCCESSFUL");

							result = true;
						} catch (SQLException e_) {
							log.log(Level.SEVERE, logprefix + " UPGRADING FAILED");
							log.info(logprefix + e_.getMessage() + e_.getSQLState());
							result = false;
						}
					} else {
						result = true;
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} catch (SQLException ex) {
			log.log(Level.SEVERE, logprefix + " SQL exception", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (rs != null)
					rs.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, logprefix + " SQL exception on close", ex);
			}
		}
		return result;
	}
	public static void LogError(String string) {
		Achievements.log.log(Level.SEVERE, Achievements.logprefix + " " + string);
	}
	public static void LogInfo(String string) {
		Achievements.log.log(Level.INFO, Achievements.logprefix + " " + string);
	}


	private GroupManager groupManagerInstance = null;
	boolean CheckGroupManager() {
		if (groupManagerInstance != null)
			return true;
		Plugin plug = this.getServer().getPluginManager().getPlugin("GroupManager");
		if (plug != null) {
			groupManagerInstance = (GroupManager) plug;
			log.info(logprefix + " Found supported plugin: " + plug.getDescription().getName());
			return true;
		}
		return false;
	}
	public GroupManager groupManager() {
		CheckGroupManager();
		return groupManagerInstance;
	}
}
