package com.nidefawl.Achievements.Commands;

import java.util.logging.Logger;
import java.util.ArrayList;
import org.bukkit.entity.Player;
import com.nidefawl.Achievements.Achievements;
import com.nidefawl.Stats.StatsSettings;

public class AchCommandHandler {
	private boolean empty;
	private ArrayList<String[]> commandList = new ArrayList<String[]>();

	static final Logger log = Logger.getLogger("Minecraft");
	public Achievements plugin = null;
	public final String logprefix = "[Achievements-v0.3b]";

	public AchCommandHandler(Achievements plugin, String commands) {
		this.empty = true;
		if (commands == null)
			return;
		this.plugin = plugin;
		String[] split = commands.split(";");
		for (String c : split) {
			if (c.length() <= 1)
				continue;
			String[] s = c.split(" ");
			if (s.length < 2) {
				Achievements.LogError("Invalid command " + c);
				continue;
			}
			this.commandList.add(s);
			this.empty = false;
		}
	}

	public boolean preCheck(Player player) {

		if (isEmpty())
			return true;
		for (String[] s : commandList) {
			if (s[0].equalsIgnoreCase("item")) {
				if (!AchCommandItem.checkInv(plugin, player, s))
					return false;
			}
		}
		return true;
	}

	public boolean preCheck() {
		if (isEmpty())
			return true;
		for (String[] s : commandList) {
			if (s[0].equalsIgnoreCase("item")) {
				if (s.length < 3) {
					Achievements.LogError("Bad command '" + s[0] + "' (not enough arguments) correct is: item <itemname> <amount>");
					return false;
				}
				if (plugin.Stats() == null) {
					Achievements.LogError("Cannot resolve item names!");
					return false;
				}
				int item = plugin.Stats().getItems().getItem(s[1]);
				if (item == 0) {
					Achievements.LogError("Bad command '" + s[0] + " " + s[1] + " " + s[2] + "' (invalid item) correct is: item <itemname> <amount>");
					return false;
				}
				try {
					Integer.parseInt(s[2]);
				} catch (NumberFormatException ex) {
					Achievements.LogError("Bad command '" + s[0] + " " + s[1] + " " + s[2] + "'(amount is not a number) correct is: item <itemname> <amount>");
					return false;
				}
			}
		}
		return true;
	}

	public void run(Player player) {
		if (isEmpty())
			return;

		for (String[] s : commandList) {
			if (s[0].equalsIgnoreCase("item")) {
				if (StatsSettings.debugOutput)
					Achievements.LogInfo("giving '" + player.getName() + "' item (" + s[0] + ")");
				AchCommandItem.handleCommand(plugin, player, s);
				continue;
			}

			if (s[0].equalsIgnoreCase("group")) {
				if (StatsSettings.debugOutput)
					Achievements.LogInfo("adding '" + player.getName() + "' to group (" + s[0] + ")");
				AchCommandGroup.handleCommand(plugin, player, s);
				continue;
			}

			if (s[0].equalsIgnoreCase("money")) {
				if (StatsSettings.debugOutput)
					Achievements.LogInfo("giving '" + player.getName() + "' money (" + s[0] + ")");
				AchCommandMoney.handleCommand(plugin, player, s);
				continue;
			}
			if (s[0].equalsIgnoreCase("warp")) {
				if (StatsSettings.debugOutput)
					Achievements.LogInfo("warping '" + player.getName() + "' to (" + s[0] + ")");
				AchCommandWarp.handleCommand(plugin, player, s);
				continue;
			}
			Achievements.LogError("Unknown command " + s[0]);
		}
	}

	public boolean isEmpty() {
		return this.empty;
	}

	@Override
	public String toString() {
		boolean second = false;

		if (isEmpty())
			return "";

		String ret = "";
		for (String[] s : commandList) {
			if (second)
				ret = ret + ";";
			for (String c : s) {
				ret = ret + c + " ";
			}
			second = true;
		}
		return ret;
	}

}
