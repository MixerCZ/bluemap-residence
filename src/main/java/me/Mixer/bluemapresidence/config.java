package me.Mixer.bluemapresidence;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class config {


    private Main plugin;
    private FileConfiguration dataConfig = null;
    private File configFile = null;

    private String configname = "config.yml";

    public config(Main plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
    }

    public void reloadConfig() {
        if(this.configFile == null)
            this.configFile= new File(this.plugin.getDataFolder(), configname);

        this.dataConfig = YamlConfiguration.loadConfiguration(this.configFile);

        InputStream defaultStream = this.plugin.getResource(configname);
        if(defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            this.dataConfig.setDefaults(defaultConfig);
        }
    }

    public FileConfiguration getConfig() {
        if(this.dataConfig == null)
            reloadConfig();
        return this.dataConfig;
    }

    public void saveConfig() {
        if(this.dataConfig == null || this.configFile == null)
            return;

        try {
            this.getConfig().save(this.configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + this.configFile, e);
        }

    }

    public void saveDefaultConfig() {
        if(this.configFile == null)
            this.configFile = new File(this.plugin.getDataFolder(), configname);

        if(!this.configFile.exists()) {
            this.plugin.saveResource(configname, false);
        }
    }

    public void updateToDefaultConfig(String t) {
        if(this.configFile.exists()) {
            File oldFile = new File(this.plugin.getDataFolder(), configname + ".old");

            this.configFile.renameTo(oldFile);
        }

        if(this.configFile == null)
            this.configFile = new File(this.plugin.getDataFolder(), configname);

        if(!this.configFile.exists()) {
            this.plugin.saveResource(configname, false);
        }


    }

    public void testCompareConfig() {
        if(!getConfig().getBoolean("config.auto_update", true)) {
            return;
        }
        //Load server config
        YamlConfiguration serverConf = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), configname));

        //Load resource config file
        InputStream defaultStream = this.plugin.getResource(configname);

        if(defaultStream != null) {
            //Load yaml from resource
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));

            //Load all keys from resource config
            for(String key : defaultConfig.getKeys(true)) {

                //Check if server config keys is equal to resource config
                if(!serverConf.contains(key)) {
                    plugin.info("&cMissing value in config for " + key + ", automatically setting up");
                    //Get string value from config
                    String value = defaultConfig.getString(key);

                    //Check data type and setting up key and value
                    if(value == "true" || value == "false") {
                        getConfig().set(key, defaultConfig.getBoolean(key));
                    } else if(isInteger(value)) {
                        getConfig().set(key, defaultConfig.getInt(key));
                    } else if (isDouble(value)){
                        getConfig().set(key, defaultConfig.getDouble(key));
                    } else {
                        getConfig().set(key, value);
                    }
                    saveConfig();
                }
            }


                //notice server owner about useless key in config
            for(String key : serverConf.getKeys(true)) {
                if(!defaultConfig.contains(key)) {
                    plugin.info("&cKey " + key + " is useless, you can remove it");
                    //String value = serverConf.getString(key);
                }
            }
        }
    }

    public static boolean isInteger(String Parameter) {
        try {
            Integer.parseInt(Parameter);
        } catch(Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isDouble(String Parameter) {
        try {
            Double.parseDouble(Parameter);
        } catch(Exception e) {
            return false;
        }
        return true;
    }

}

