package com.nidefawl.Achievements.Commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.earth2me.essentials.Essentials;
import com.nidefawl.Achievements.Achievements;
import com.nidefawl.Achievements.Messaging.AchMessaging;

public class AchCommandWarp {
	public static boolean handleCommand(Achievements plugin, Player player, String[] s) {
		if (s.length < 2) {
			Achievements.LogError("Bad command (not enough arguments) correct is: warp warpname");
			return false;
		}
		try {

			Plugin plugEssentials = plugin.getServer().getPluginManager().getPlugin("Essentials");
			if (plugEssentials != null && plugEssentials.isEnabled()) {
				com.earth2me.essentials.User user = com.earth2me.essentials.User.get(player);
				if(Essentials.getWarps().getWarp(s[1]) != null && user !=null)  {
					user.warpTo(s[1]);
				}
			} else {
				WarpTo(player,s[1]);
			}
		} catch (Exception e) {
			Achievements.LogError("warp command failed: " + e.getMessage());
			return false;
		}

		AchMessaging.send(player, plugin.color + "Woosh!");
		return true;
	}

	public static void WarpTo(Player player, String warpName) throws Exception {
		double x = 0, y = 0, z = 0;
		float yaw = 0, pitch = 0;
		File file = new File("warps.txt");
		if (!file.exists())
			throw new Exception("warp " + warpName + " does not exist.");
		BufferedReader rx = new BufferedReader(new FileReader(file));
		boolean found = false;
		for (String[] parts = new String[0]; rx.ready(); parts = rx.readLine().split(":")) {
			if (parts.length < 6)
				continue;
			System.out.println(parts[0]);
			if (!parts[0].equalsIgnoreCase(warpName))
				continue;
			x = Double.parseDouble(parts[1].trim());
			y = Double.parseDouble(parts[2].trim());
			z = Double.parseDouble(parts[3].trim());
			yaw = Float.parseFloat(parts[4].trim());
			pitch = Float.parseFloat(parts[5].trim());
			found = true;
			break;
		}
		if (!found)
			throw new Exception("That warp does not exist.");
		player.teleport(new Location(player.getWorld(), x, y, z, yaw, pitch));
	}
}
