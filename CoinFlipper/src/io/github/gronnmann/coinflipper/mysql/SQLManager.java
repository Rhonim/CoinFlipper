package io.github.gronnmann.coinflipper.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import io.github.gronnmann.coinflipper.ConfigManager;
import io.github.gronnmann.coinflipper.customizable.ConfigVar;
import io.github.gronnmann.coinflipper.CoinFlipper;
import io.github.gronnmann.coinflipper.stats.Stats;
import io.github.gronnmann.coinflipper.stats.StatsManager;
import io.github.gronnmann.utils.coinflipper.Debug;
import io.github.gronnmann.utils.sql.coinflipper.MySQL;
import io.github.gronnmann.utils.sql.coinflipper.SQLite;

public class SQLManager {
	private SQLManager(){}
	private static SQLManager mng = new SQLManager();
	public static SQLManager getManager(){
		return mng;
	}
	
	private MySQL msql;
	private SQLite sqli;
	private Connection conn;
	private boolean enabled;
	
	public void setup(){
		
		enabled = ConfigVar.MYSQL_ENABLED.getBoolean();
		
		
		
		if (!enabled){
			System.out.println("[CoinFlipper] Connecting to SQLite...");
			
			sqli = new SQLite(CoinFlipper.getMain(), "stats");
			
			conn = sqli.getConnection();
			
			if (!sqli.tableExists("coinflipper_stats")){
				try{
					
					PreparedStatement createCoinFlipperTable = conn.prepareStatement(
							"create table coinflipper_stats(uuid char(36), gamesWon int, gamesLost int, moneySpent double, moneyWon double)"
							);
					
					createCoinFlipperTable.execute();
					
					
				}catch(Exception e){
					e.printStackTrace();
				}
				
			}
			
		}else{
			System.out.println("[CoinFlipper] Connecting to MySQL...");
			
			FileConfiguration conf = ConfigManager.getManager().getMySQL();
			
			msql = new MySQL(conf.getString("server"),
					conf.getString("database"),
					conf.getString("user"),
					conf.getString("password"));
			
			conn = msql.getConnection();
			
			
			if (!msql.tableExists("coinflipper_stats")){
				try{
					
					PreparedStatement createCoinFlipperTable = conn.prepareStatement(
							"create table coinflipper_stats(uuid char(36), gamesWon int, gamesLost int, moneySpent double, moneyWon double)"
							);
					
					createCoinFlipperTable.execute();
					
					
				}catch(Exception e){
					e.printStackTrace();
				}
				
			}
		}
		
			
		
		
	}
	
	public void loadStats(final String uuid){
		new BukkitRunnable() {
			
			@Override
			public void run(){
				try{	
					
				Debug.print("Fetching stats for: " + uuid);
					
				PreparedStatement getStats = conn.prepareStatement(
							"select * from coinflipper_stats where uuid='" + uuid + "'"
							);	
				ResultSet res = getStats.executeQuery();
				
					
				if (!res.next()){
					StatsManager.getManager().createClearStats(uuid);
					return;
				}
					
				Stats loadedStats = new Stats(
						res.getInt("gamesWon"),
						res.getInt("gamesLost"),
						res.getDouble("moneySpent"),
						res.getDouble("moneyWon"));
					
				StatsManager.getManager().setStats(uuid, loadedStats);
				
				
				res.close();
				
				}catch(Exception e){
					e.printStackTrace();
				}
				
			}
			}.runTaskAsynchronously(CoinFlipper.getMain());
		}
	
	
	public void saveStats(final String owner, final Stats s){
			try{
				PreparedStatement delete = conn.prepareStatement(
						"delete from coinflipper_stats where uuid='" + owner + "'");
				delete.execute();
				
				PreparedStatement putNew = conn.prepareStatement(
						"insert into coinflipper_stats(uuid, gamesWon, gamesLost, moneySpent, moneyWon) values (?,?,?,?,?)"
						);
					
				putNew.setString(1, owner);
				putNew.setInt(2, s.getGamesWon());
				putNew.setInt(3, s.getGamesLost());
				putNew.setDouble(4, s.getMoneySpent());					
				putNew.setDouble(5, s.getMoneyWon());
				
				putNew.execute();
				
				
				}catch(Exception e){
					e.printStackTrace();
				}
				
		
	}
	
	public Connection getSQLConnection(){
		return conn;
	}
	public MySQL getMySQL(){
		return msql;
	}
	
	public boolean isEnabled(){
		//Little workaround for SQLite to always work.
		return true;
		//return enabled;
	}
	
}
