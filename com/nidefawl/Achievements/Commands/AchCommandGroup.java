package com.nidefawl.Achievements.Commands;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.reader.UnicodeReader;

import com.nidefawl.Achievements.Achievements;
import com.nidefawl.MyGeneral.types.MyGUser;

public class AchCommandGroup {
	
	protected static boolean handleCommand(Achievements plugin,Player player, String[] s) {
		if (s.length < 2) {
			Achievements.LogError("Bad command (not enough arguments) correct is: group groupname");
			return false;
		}
		if (plugin.myGeneral() != null) {
			MyGUser myGPlayer = plugin.myGeneral().getDataSource().getPlayer(player.getName());
			myGPlayer.addGroup(s[1]);
			if(plugin.myGeneral().getDataSource().doesPlayerExist(player.getName())) {
				plugin.myGeneral().getDataSource().modifyPlayer(myGPlayer);
			} else {
				plugin.myGeneral().getDataSource().addPlayer(myGPlayer);	
			}
			return true;
		}
		else if (plugin.groupManager() != null) {
			User grpUser = plugin.groupManager().getData().getUser(player.getName());
			Group newGrp = plugin.groupManager().getData().getGroup(s[1]);
			if(grpUser!=null&&newGrp!=null) {
				grpUser.setGroup(newGrp);
			}
			return true;
		} else {
			try {
				ModifyGroup(player,s[1]);
				plugin.Stats().ReloadPerms();
			} catch (Exception e) {
				Achievements.LogError("group command failed: " +e.getMessage());
				return false;
			}
			return true;
		}
	}	
	@SuppressWarnings("unchecked")
	public static void ModifyGroup(Player player, String newGroup) throws Exception
	{
		Map<String, Object> data;
		File file = new File("plugins/Permissions/config.yml");

		if (!file.exists()) throw new Exception("This server does not use Permissions.");

		FileInputStream rx = new FileInputStream(file);
		try
		{
			data = (Map<String, Object>)Achievements.getYaml().load(new UnicodeReader(rx));
			if (data == null) throw new NullPointerException();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			throw new Exception("This server does not have Permissions configured properly.", ex);
		}
		finally
		{
			rx.close();
		}

		Map<String, Object> users = (Map<String, Object>)data.get("users");
		if (users == null) users = new HashMap<String, Object>();
		Map<String, Object> userData = (Map<String, Object>)users.get(player.getName());
		if (userData == null) userData = new HashMap<String, Object>();

		if (userData.get("permissions") == null) userData.put("permissions", new String[0]);

		userData.put("group", newGroup);
		users.put(player.getName(), userData);
		data.put("users", users);

		FileWriter tx = new FileWriter(file, false);
		try
		{
			tx.write(Achievements.getYaml().dump(data));
			tx.flush();
		}
		finally
		{
			tx.close();
		}
	}
	
}
