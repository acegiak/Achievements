package com.nidefawl.Achievements;

import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.*;

public class PlayerAchievementSQL extends PlayerAchievement {
	static final Logger log = Logger.getLogger("Minecraft");
	public final String logprefix = "[Achievements-" + Achievements.version + "]";
	private Achievements plugin = null;

	PlayerAchievementSQL(String name, Achievements plugin) {
		super(name);
		this.plugin = plugin;
	}

	@Override
	protected void save() {

		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = plugin.getSQLConnection();
			conn.setAutoCommit(false);

			Iterator<String> achIter = achievements.keySet().iterator();
			while (achIter.hasNext()) {
				String achName = achIter.next();
				Achievement ach = achievements.get(achName);
				if (!ach.modified)
					continue;

				ps = conn.prepareStatement("INSERT INTO " + plugin.dbPlayerAchievementsTable + " (player,achievement,count) VALUES(?,?,?) ON DUPLICATE KEY UPDATE count=?", Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, name);
				ps.setString(2, achName);
				ps.setInt(3, ach.getCount());
				ps.setInt(4, ach.getCount());
				ps.executeUpdate();
			}
			conn.commit();
		} catch (SQLException ex) {
			log.log(Level.SEVERE, logprefix + " SQL exception", ex);
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, logprefix + " SQL exception on close", ex);
			}
		}
	}

	@Override
	protected void load() {

		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			conn = plugin.getSQLConnection();
			ps = conn.prepareStatement("SELECT * from " + plugin.dbPlayerAchievementsTable + " where player = ?");
			ps.setString(1, name);
			rs = ps.executeQuery();
			while (rs.next())
				put(rs.getString("achievement"), rs.getInt("count"));
		} catch (SQLException ex) {
			log.log(Level.SEVERE, logprefix + " SQL exception", ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, logprefix + " SQL exception on close", ex);
			}
		}
	}
}