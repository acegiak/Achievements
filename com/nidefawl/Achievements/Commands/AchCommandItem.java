package com.nidefawl.Achievements.Commands;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.nidefawl.Achievements.Achievements;
import com.nidefawl.Achievements.Messaging.AchMessaging;

public class AchCommandItem {
	protected static boolean checkInv(Achievements plugin,Player player,String[] s) {
		if (s.length < 3) {
			Achievements.LogError("Bad command '"+s[0]+"' (not enough arguments) correct is: item <itemname> <amount>");
			return false;
		}
		if (plugin.Stats() == null) {
			Achievements.LogError("Cannot resolve item names!");
			return false;
		}
		int item = plugin.Stats().getItems().getItem(s[1]);
		if (item == 0) {
			Achievements.LogError("Bad command '"+s[0]+" "+s[1]+" "+s[2]+"' (invalid item) correct is: item <itemname> <amount>");
			return false;
		}
		try {
			Integer.parseInt(s[2]);
		} catch (NumberFormatException ex) {
			Achievements.LogError("Bad command '"+s[0]+" "+s[1]+" "+s[2]+"'(amount is not a number) correct is: item <itemname> <amount>");
			return false;
		}
		if (player.getInventory().firstEmpty() == -1) {
			AchMessaging.send(player,plugin.color + "An achievement reward item is waiting for you!");
			AchMessaging.send(player,plugin.color + "You should get some free space in your inventory!");
			return false;
		}
		return true;
	}
	@SuppressWarnings("deprecation")
	protected static boolean handleCommand(Achievements plugin,Player player, String[] s) {
		int item, amount;
		if (s.length < 3) {
			Achievements.LogError("Bad command '"+s[0]+"' (not enough arguments) correct is: item <itemname> <amount>");
			return false;
		}
		if (plugin.Stats() == null) {
			Achievements.LogError("Cannot resolve item names!");
			return false;
		}
		item = plugin.Stats().getItems().getItem(s[1]);
		if (item == 0) {
			Achievements.LogError("Bad command '"+s[0]+" "+s[1]+" "+s[2]+"' (invalid item) correct is: item <itemname> <amount>");
			return false;
		}
		try {
			amount = Integer.parseInt(s[2]);
		} catch (NumberFormatException ex) {
			Achievements.LogError("Bad command '"+s[0]+" "+s[1]+" "+s[2]+"'(amount is not a number) correct is: item <itemname> <amount>");
			return false;
		}
		ItemStack itemstack = new ItemStack(item, amount);
		player.getInventory().addItem(itemstack);
		player.updateInventory();
		AchMessaging.send(player,plugin.color + "Reward: &f"+amount+" "+s[1]);
		return true;
	}
}
