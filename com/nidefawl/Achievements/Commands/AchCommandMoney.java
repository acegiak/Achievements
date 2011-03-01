package com.nidefawl.Achievements.Commands;


import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import com.nidefawl.Achievements.Achievements;
import com.nidefawl.Achievements.Messaging.AchMessaging;
import com.nidefawl.MyGeneral.types.MyGUser;
import com.nijiko.coelho.iConomy.iConomy;

public class AchCommandMoney {

	public static boolean handleCommand(Achievements plugin, Player player, String[] s) {
		Plugin plug = plugin.getServer().getPluginManager().getPlugin("iConomy");
		if (plug == null) {
			Achievements.LogError("Did not find plugin: iConomy");
			return false;
		}
		if (s.length < 2) {
			Achievements.LogError("Bad command (not enough arguments) correct is: money amount");
			return false;
		}
		int amount = 0;
		try {
			amount = Integer.parseInt(s[1]);
		} catch (NumberFormatException ex) {
			Achievements.LogError("Bad command '"+s[0]+" "+s[1]+" "+s[2]+"'(amount is not a number) correct is: money amount");
			return false;
		}
		String currency = iConomy.getBank().getCurrency();
		if (plugin.myGeneral() != null) {
			MyGUser myGPlayer = plugin.myGeneral().getDataSource().getPlayer(player.getName());
			myGPlayer.addToBalance(amount);
			
		}
		else
		{
			if(iConomy.getBank().getAccount(player.getName())!=null) {
				iConomy.getBank().getAccount(player.getName()).add(amount);
				iConomy.getBank().getAccount(player.getName()).save();
			}
		}
		if(amount>0)
			AchMessaging.send(player,plugin.color + "Reward: &f"+amount+" &2"+currency);
		else
			AchMessaging.send(player,plugin.color + "Penalty: &f"+amount+" &2"+currency);
		return true;
	}
}
