package com.nidefawl.Achievements.Commands;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import com.nidefawl.Achievements.Achievements;
import com.nidefawl.Achievements.Messaging.AchMessaging;
import com.nijiko.coelho.iConomy.iConomy;

public class AchCommandMoney {

	public static boolean handleCommand(Achievements plugin, Player player, String[] s) {
		if (s.length < 2) {
			Achievements.LogError("Bad command (not enough arguments) correct is: money amount");
			return false;
		}
		int amount = 0;
		try {
			amount = Integer.parseInt(s[1]);
		} catch (NumberFormatException ex) {
			Achievements.LogError("Bad command '" + s[0] + " " + s[1] + " " + s[2] + "'(amount is not a number) correct is: money amount");
			return false;
		}
		String currency = "money";
		Plugin plugiConomy = plugin.getServer().getPluginManager().getPlugin("iConomy");
		Plugin plugEssentialsEco = plugin.getServer().getPluginManager().getPlugin("EssentialsEco");
		if (plugiConomy != null && plugiConomy.isEnabled()) {
			currency = iConomy.getBank().getCurrency();
			if (iConomy.getBank().getAccount(player.getName()) != null) {
				iConomy.getBank().getAccount(player.getName()).add(amount);
				iConomy.getBank().getAccount(player.getName()).save();
			}
		} else if (plugEssentialsEco != null && plugEssentialsEco.isEnabled()) {
			try {
				com.earth2me.essentials.User user = com.earth2me.essentials.User.get(player);
				if (user != null) {
					user.payUser(user, amount);
				}
			} catch (Exception e) {
			}

		} else {
			Achievements.LogError("Did not find a money plugin!");
			return false;
		}
		if (amount > 0)
			AchMessaging.send(player, plugin.color + "Reward: &f" + amount + " &2" + currency);
		else
			AchMessaging.send(player, plugin.color + "Penalty: &f" + amount + " &2" + currency);
		return true;
	}
}
