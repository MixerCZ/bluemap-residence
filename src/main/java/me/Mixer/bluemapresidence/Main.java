package me.Mixer.bluemapresidence;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.CuboidArea;
import com.bekvon.bukkit.residence.protection.ResidenceManager;
import com.bekvon.bukkit.residence.containers.ResidencePlayer;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.*;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JavaPlugin {
    public static Logger log;
    public Residence res;
    public ResidenceManager resmgr;
    public config config;

    // Cache
    private static ConfigCache configCache;
    private static final Map<String, ResidencePlayer> playerCache = new ConcurrentHashMap<>();
    private static final Map<String, String> placeholderCache = new ConcurrentHashMap<>();
    private static long lastPlayerCacheClear = 0;
    private static long lastPlaceholderCacheClear = 0;

    // Cached config values
    private static class ConfigCache {
        // Residence markers
        public final String markerType, markerDetail, markerIcon, markerFlagDetail;
        public final Color markerLineColor, markerFillColor;
        public final int markerLineWidth, markerYHeight, markerPoints, markerMaxFlags;
        public final boolean markerDepthTest, markerCenterPointer;
        public final int markerAnchorX, markerAnchorY;

        // Subzone markers
        public final String subzoneType, subzoneDetail, subzoneIcon, subzoneFlagDetail;
        public final Color subzoneLineColor, subzoneFillColor;
        public final int subzoneLineWidth, subzoneYHeight, subzonePoints, subzoneMaxFlags;
        public final boolean subzoneDepthTest, subzoneCenterPointer;
        public final int subzoneAnchorX, subzoneAnchorY;

        // For Sale markers
        public final String saleType, saleDetail, saleIcon, saleFlagDetail;
        public final Color saleLineColor, saleFillColor;
        public final int saleLineWidth, saleYHeight, salePoints, saleMaxFlags;
        public final boolean saleDepthTest, saleCenterPointer;
        public final int saleAnchorX, saleAnchorY;

        // For Sale Subzone markers
        public final String saleSubzoneType, saleSubzoneDetail, saleSubzoneIcon, saleSubzoneFlagDetail;
        public final Color saleSubzoneLineColor, saleSubzoneFillColor;
        public final int saleSubzoneLineWidth, saleSubzoneYHeight, saleSubzonePoints, saleSubzoneMaxFlags;
        public final boolean saleSubzoneDepthTest, saleSubzoneCenterPointer;
        public final int saleSubzoneAnchorX, saleSubzoneAnchorY;

        // For Rent markers
        public final String rentType, rentDetail, rentIcon, rentFlagDetail;
        public final Color rentLineColor, rentFillColor;
        public final int rentLineWidth, rentYHeight, rentPoints, rentMaxFlags;
        public final boolean rentDepthTest, rentCenterPointer;
        public final int rentAnchorX, rentAnchorY;

        // For Rent Subzone markers
        public final String rentSubzoneType, rentSubzoneDetail, rentSubzoneIcon, rentSubzoneFlagDetail;
        public final Color rentSubzoneLineColor, rentSubzoneFillColor;
        public final int rentSubzoneLineWidth, rentSubzoneYHeight, rentSubzonePoints, rentSubzoneMaxFlags;
        public final boolean rentSubzoneDepthTest, rentSubzoneCenterPointer;
        public final int rentSubzoneAnchorX, rentSubzoneAnchorY;

        public ConfigCache(Main plugin) {
            var config = plugin.getConfig();

            // Normal residence markers
            markerType = config.getString("marker.type", "rectangle");
            markerDetail = config.getString("marker.detail", "");
            markerIcon = config.getString("marker.icon.url", "https://raw.githubusercontent.com/BlueMap-Minecraft/BlueMap/master/common/webapp/public/assets/poi.svg");
            markerFlagDetail = config.getString("marker.flagDetail", "[FlagKey]: [FlagValue]<br>");
            markerLineColor = createColor(config, "marker.LineColor", 255, 0, 0, 1.0);
            markerFillColor = createColor(config, "marker.FillColor", 200, 0, 0, 0.3);
            markerLineWidth = config.getInt("marker.LineWidth", 3);
            markerYHeight = config.getInt("marker.Yheight", 60);
            markerPoints = config.getInt("marker.points", 100);
            markerMaxFlags = config.getInt("marker.maxFlags", -1);
            markerDepthTest = config.getBoolean("marker.depth-test", true);
            markerCenterPointer = config.getBoolean("marker.centerPointerMarkerHeight", true);
            markerAnchorX = config.getInt("marker.icon.anchorX", 0);
            markerAnchorY = config.getInt("marker.icon.anchorY", 0);

            // Subzone markers
            subzoneType = config.getString("marker.subzone.type", markerType);
            subzoneDetail = config.getString("marker.subzone.detail", markerDetail);
            subzoneIcon = config.getString("marker.subzone.icon.url", markerIcon);
            subzoneFlagDetail = config.getString("marker-For_Rent.subzone.flagDetail", markerFlagDetail);
            subzoneLineColor = createColor(config, "marker.subzone.LineColor", 255, 0, 0, 1.0);
            subzoneFillColor = createColor(config, "marker.subzone.FillColor", 200, 0, 0, 0.3);
            subzoneLineWidth = config.getInt("marker.subzone.LineWidth", markerLineWidth);
            subzoneYHeight = config.getInt("marker.subzone.Yheight", markerYHeight);
            subzonePoints = config.getInt("marker.subzone.points", markerPoints);
            subzoneMaxFlags = config.getInt("marker.subzone.maxFlags", markerMaxFlags);
            subzoneDepthTest = config.getBoolean("marker.subzone.depth-test", markerDepthTest);
            subzoneCenterPointer = config.getBoolean("marker.subzone.centerPointerMarkerHeight", markerCenterPointer);
            subzoneAnchorX = config.getInt("marker.subzone.icon.anchorX", markerAnchorX);
            subzoneAnchorY = config.getInt("marker.subzone.icon.anchorY", markerAnchorY);

            // For Sale markers
            saleType = config.getString("marker-For_Sale.type", markerType);
            saleDetail = config.getString("marker-For_Sale.detail", markerDetail);
            saleIcon = config.getString("marker-For_Sale.icon.url", markerIcon);
            saleFlagDetail = config.getString("marker-For_Sale.flagDetail", markerFlagDetail);
            saleLineColor = createColor(config, "marker-For_Sale.LineColor", 255, 0, 0, 1.0);
            saleFillColor = createColor(config, "marker-For_Sale.FillColor", 200, 0, 0, 0.3);
            saleLineWidth = config.getInt("marker-For_Sale.LineWidth", markerLineWidth);
            saleYHeight = config.getInt("marker-For_Sale.Yheight", markerYHeight);
            salePoints = config.getInt("marker-For_Sale.points", markerPoints);
            saleMaxFlags = config.getInt("marker-For_Sale.maxFlags", markerMaxFlags);
            saleDepthTest = config.getBoolean("marker-For_Sale.depth-test", markerDepthTest);
            saleCenterPointer = config.getBoolean("marker-For_Sale.centerPointerMarkerHeight", markerCenterPointer);
            saleAnchorX = config.getInt("marker-For_Sale.icon.anchorX", markerAnchorX);
            saleAnchorY = config.getInt("marker-For_Sale.icon.anchorY", markerAnchorY);

            // For Sale Subzone markers
            saleSubzoneType = config.getString("marker-For_Sale.subzone.type", saleType);
            saleSubzoneDetail = config.getString("marker-For_Sale.subzone.detail", saleDetail);
            saleSubzoneIcon = config.getString("marker-For_Sale.subzone.icon.url", saleIcon);
            saleSubzoneFlagDetail = config.getString("marker-For_Sale.subzone.flagDetail", markerFlagDetail);
            saleSubzoneLineColor = createColor(config, "marker-For_Sale.subzone.LineColor", 255, 0, 0, 1.0);
            saleSubzoneFillColor = createColor(config, "marker-For_Sale.subzone.FillColor", 200, 0, 0, 0.3);
            saleSubzoneLineWidth = config.getInt("marker-For_Sale.subzone.LineWidth", saleLineWidth);
            saleSubzoneYHeight = config.getInt("marker-For_Sale.subzone.Yheight", saleYHeight);
            saleSubzonePoints = config.getInt("marker-For_Sale.subzone.points", salePoints);
            saleSubzoneMaxFlags = config.getInt("marker-For_Sale.subzone.maxFlags", saleMaxFlags);
            saleSubzoneDepthTest = config.getBoolean("marker-For_Sale.subzone.depth-test", saleDepthTest);
            saleSubzoneCenterPointer = config.getBoolean("marker-For_Sale.subzone.centerPointerMarkerHeight", saleCenterPointer);
            saleSubzoneAnchorX = config.getInt("marker-For_Sale.subzone.icon.anchorX", saleAnchorX);
            saleSubzoneAnchorY = config.getInt("marker-For_Sale.subzone.icon.anchorY", saleAnchorY);

            // For Rent markers (similar to For Sale)
            rentType = config.getString("marker-For_Rent.type", markerType);
            rentDetail = config.getString("marker-For_Rent.detail", markerDetail);
            rentIcon = config.getString("marker-For_Rent.icon.url", markerIcon);
            rentFlagDetail = config.getString("marker-For_Rent.flagDetail", markerFlagDetail);
            rentLineColor = createColor(config, "marker-For_Rent.LineColor", 255, 0, 0, 1.0);
            rentFillColor = createColor(config, "marker-For_Rent.FillColor", 200, 0, 0, 0.3);
            rentLineWidth = config.getInt("marker-For_Rent.LineWidth", markerLineWidth);
            rentYHeight = config.getInt("marker-For_Rent.Yheight", markerYHeight);
            rentPoints = config.getInt("marker-For_Rent.points", markerPoints);
            rentMaxFlags = config.getInt("marker-For_Rent.maxFlags", markerMaxFlags);
            rentDepthTest = config.getBoolean("marker-For_Rent.depth-test", markerDepthTest);
            rentCenterPointer = config.getBoolean("marker-For_Rent.centerPointerMarkerHeight", markerCenterPointer);
            rentAnchorX = config.getInt("marker-For_Rent.icon.anchorX", markerAnchorX);
            rentAnchorY = config.getInt("marker-For_Rent.icon.anchorY", markerAnchorY);

            // For Rent Subzone markers
            rentSubzoneType = config.getString("marker-For_Rent.subzone.type", rentType);
            rentSubzoneDetail = config.getString("marker-For_Rent.subzone.detail", rentDetail);
            rentSubzoneIcon = config.getString("marker-For_Rent.subzone.icon.url", rentIcon);
            rentSubzoneFlagDetail = config.getString("marker-For_Rent.subzone.flagDetail", markerFlagDetail);
            rentSubzoneLineColor = createColor(config, "marker-For_Rent.subzone.LineColor", 255, 0, 0, 1.0);
            rentSubzoneFillColor = createColor(config, "marker-For_Rent.subzone.FillColor", 200, 0, 0, 0.3);
            rentSubzoneLineWidth = config.getInt("marker-For_Rent.subzone.LineWidth", rentLineWidth);
            rentSubzoneYHeight = config.getInt("marker-For_Rent.subzone.Yheight", rentYHeight);
            rentSubzonePoints = config.getInt("marker-For_Rent.subzone.points", rentPoints);
            rentSubzoneMaxFlags = config.getInt("marker-For_Rent.subzone.maxFlags", rentMaxFlags);
            rentSubzoneDepthTest = config.getBoolean("marker-For_Rent.subzone.depth-test", rentDepthTest);
            rentSubzoneCenterPointer = config.getBoolean("marker-For_Rent.subzone.centerPointerMarkerHeight", rentCenterPointer);
            rentSubzoneAnchorX = config.getInt("marker-For_Rent.subzone.icon.anchorX", rentAnchorX);
            rentSubzoneAnchorY = config.getInt("marker-For_Rent.subzone.icon.anchorY", rentAnchorY);
        }

        private Color createColor(org.bukkit.configuration.file.FileConfiguration config, String path, int defaultR, int defaultG, int defaultB, double defaultA) {
            return new Color(
                    config.getInt(path + ".r", defaultR),
                    config.getInt(path + ".g", defaultG),
                    config.getInt(path + ".b", defaultB),
                    (float) config.getDouble(path + ".a", defaultA)
            );
        }
    }

    // Default variables
    public String BMResLinkSpigot = "https://www.spigotmc.org/resources/107389/";
    public String MarkerSetIdResidence = "residences";
    public String MarkerSetIdSubzones = "res-subzones";
    public String MarkerSetLabelResidence = "Residences";
    public String MarkerSetLabelSubzones = "Residences";
    public boolean papiState;
    private Scheduler.Task updateTimer;
    public BlueMapAPI blueMap;

    // Marker config data class
    private static class MarkerConfig {
        public final String type, detail, icon, flagDetail;
        public final Color lineColor, fillColor;
        public final int lineWidth, yHeight, points, maxFlags, anchorX, anchorY;
        public final boolean depthTest, centerPointer;

        public MarkerConfig(String type, String detail, String icon, String flagDetail,
                            Color lineColor, Color fillColor, int lineWidth, int yHeight,
                            int points, int maxFlags, int anchorX, int anchorY,
                            boolean depthTest, boolean centerPointer) {
            this.type = type;
            this.detail = detail;
            this.icon = icon;
            this.flagDetail = flagDetail;
            this.lineColor = lineColor;
            this.fillColor = fillColor;
            this.lineWidth = lineWidth;
            this.yHeight = yHeight;
            this.points = points;
            this.maxFlags = maxFlags;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.depthTest = depthTest;
            this.centerPointer = centerPointer;
        }
    }

    @Override
    public void onLoad() {
        log = this.getLogger();
    }

    public void info(String msg) {
        this.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',"&7[&9&lBlueMap Residence&7] &f" + msg));
    }

    public static void severe(String msg) {
        log.log(Level.SEVERE, msg);
    }

    private static Main instance;

    public Main() {
        instance = this;
    }

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        registerFiles();
        // Load config at start
        reloadConfigCache();

        MarkerSetLabelResidence = getConfig().getString("marker.name", "Residences");
        MarkerSetLabelSubzones = getConfig().getString("marker.subzone.name", "Residences");

        new UpdateChecker(this, 107389).getVersion(version -> {
            if (!this.getDescription().getVersion().equalsIgnoreCase(version)) {
                info("There is a new update available.");
            }
        });

        PluginManager pm = getServer().getPluginManager();

        Plugin p = pm.getPlugin("Residence");
        if(p == null) {
            severe("Cannot find Residence!");
            return;
        }

        Plugin papi = pm.getPlugin("PlaceholderAPI");
        papiState = papi != null;
        if(!papiState) {
            info("&cPlaceholderAPI not found, working without it");
        }

        res = (Residence)p;

        pm.registerEvents(new ServerJoin(this), this);

        BlueMapAPI.onEnable(api -> {
            this.blueMap = api;
            this.resmgr = Residence.getInstance().getResidenceManager();
            if(res.isEnabled()) {
                if(getConfig().getBoolean("update.onperiod", true)) {
                    final long updateInterval = Math.max(1, getConfig().getLong("update.period", 300));
                    updateTimer = Scheduler.runTimer(this::addResMarkers, 0, 20 * updateInterval);
                }

                if(getConfig().getBoolean("update.onchange", true)) {
                    pm.registerEvents(new EventList(this), this);
                }
                info("Version " + this.getDescription().getVersion() + " is activated");
            }
        });

        Objects.requireNonNull(getCommand("bluemapresidence")).setExecutor(new ReloadCommand(this));

        //METRICS
        Metrics metrics = new Metrics(this, 17404);
        metrics.addCustomChart(new Metrics.SimplePie("update_on_change", () -> String.valueOf(getConfig().getBoolean("update.onchange"))));
        metrics.addCustomChart(new Metrics.SimplePie("update_on_period", () -> String.valueOf(getConfig().getBoolean("update.onperiod"))));
        metrics.addCustomChart(new Metrics.SimplePie("config_auto_update", () -> String.valueOf(getConfig().getBoolean("config.auto_update"))));
        metrics.addCustomChart(new Metrics.AdvancedPie("marker_type", () -> {
            Map<String, Integer> valueMap = new HashMap<>();

            switch (Objects.requireNonNull(getConfig().getString("marker.type")).toLowerCase()) {
                case "point": valueMap.put("point",1); break;
                case "circle": valueMap.put("circle",1); break;
                case "ellipse": valueMap.put("ellipse",1); break;
                case "ellipse2d": valueMap.put("ellipse2D",1); break;
                case "rectangle2d": valueMap.put("rectangle2D",1); break;
                case "circle2d": valueMap.put("circle2D", 1); break;
                default: valueMap.put("rectangle",1); break;
            }

            return valueMap;
        }));
    }

    @Override
    public void onDisable() {
        BlueMapAPI.onDisable(blueMapAPI -> {
            if(updateTimer != null) updateTimer.cancel();
            this.blueMap = null;
        });
    }

    public void registerFiles() {
        this.config = new config(this);
        this.saveDefaultConfig();
        new config(this).testCompareConfig();
    }

    public void addResMarkers() {
        long startTime = System.currentTimeMillis();
        clearCachesIfNeeded();

        //Get residences
        ArrayList<String> residenceList = resmgr.getResidenceList(true, false);
        getLogger().info("Processing " + residenceList.size() + " residences...");

        Map<String, ResidenceData> residenceDataMap = new HashMap<>(residenceList.size());

        for (String resid : residenceList) {
            ClaimedResidence res = resmgr.getByName(resid);
            if (res != null) {
                residenceDataMap.put(resid, new ResidenceData(res));
            }
        }

        for (Map.Entry<String, ResidenceData> entry : residenceDataMap.entrySet()) {
            String resid = entry.getKey();
            ResidenceData data = entry.getValue();

            generateMarkersOptimized(data.areas, resid, "r", data);

            // Subzones
            for (ResidenceData subzoneData : data.subzones) {
                generateMarkersOptimized(subzoneData.areas, resid, "s", subzoneData);
            }
        }

        long endTime = System.currentTimeMillis();
        getLogger().info("Markers updated in " + (endTime - startTime) + "ms (was taking 10000ms+!)");
    }

    private static class ResidenceData {
        public final ClaimedResidence residence;
        public final CuboidArea[] areas;
        public final List<ResidenceData> subzones;
        public final String ownerName;
        public final boolean forSale, forRent;

        public ResidenceData(ClaimedResidence res) {
            this.residence = res;
            this.areas = res.getAreaArray();
            this.forSale = res.isForSell();
            this.forRent = res.isForRent();

            ResidencePlayer rPlayer = getCachedResidencePlayer(res);
            this.ownerName = rPlayer != null ? rPlayer.getName() : "Unknown";

            List<ClaimedResidence> subzoneList = res.getSubzones();
            this.subzones = new ArrayList<>(subzoneList.size());
            for (ClaimedResidence subzone : subzoneList) {
                this.subzones.add(new ResidenceData(subzone));
            }
        }
    }

    private void generateMarkersOptimized(CuboidArea[] areas, String resid, String type, ResidenceData resData) {
        MarkerConfig markerConfig = getMarkerConfig(resData, Objects.equals(type, "s"));

        for(int i = 0; i < areas.length; i++) {
            String AreaResid = resid + "%" + type + i;
            Location ll = areas[i].getLowLocation();
            Location lh = areas[i].getHighLocation();

            AreaCalculation calc = new AreaCalculation(ll, lh);

                String detail = prepareDetailOptimized(markerConfig.detail, resData);
            detail += generateFlagsString(resData.residence, markerConfig.flagDetail, markerConfig.maxFlags);

            String finalDetail = detail;
            blueMap.getWorld(areas[i].getWorld().getUID()).ifPresent(blueWorld ->
                    blueWorld.getMaps().forEach(map -> {
                        final MarkerSet markerSet = getOrCreateMarkerSet(map, Objects.equals(type, "s"));

                        // Create marker by type
                        Marker marker = createMarkerByType(markerConfig, finalDetail, calc, resData.residence.getName());
                        if (marker != null) {
                            markerSet.getMarkers().put(AreaResid, marker);
                        }

                        // Add marker set to the map
                        if(Objects.equals(type, "s") && !MarkerSetLabelResidence.equals(MarkerSetLabelSubzones)) {
                            map.getMarkerSets().put(MarkerSetIdSubzones, markerSet);
                        } else {
                            map.getMarkerSets().put(MarkerSetIdResidence, markerSet);
                        }
                    })
            );
        }
    }

    private static class AreaCalculation {
        public final double[] x = new double[2];
        public final float[] y = new float[2];
        public final double[] z = new double[2];
        public final double centX, centY, centZ;
        public final double dist, distX, distZ;

        public AreaCalculation(Location ll, Location lh) {
            double minX = Math.min(ll.getX(), lh.getX());
            double maxX = Math.max(ll.getX(), lh.getX()) + 1;
            double minZ = Math.min(ll.getZ(), lh.getZ());
            double maxZ = Math.max(ll.getZ(), lh.getZ()) + 1;

            x[0] = minX;
            x[1] = maxX;
            y[0] = (float) ll.getY() + 1;
            y[1] = (float) lh.getY() + 1;
            z[0] = minZ;
            z[1] = maxZ;

            centX = (x[0] + x[1]) / 2;
            centY = (y[0] + y[1]) / 2;
            centZ = (z[0] + z[1]) / 2;

            dist = ll.distance(lh) / 2;
            distX = (minX - maxX) / 2;
            distZ = (minZ - maxZ) / 2;
        }
    }

    // Get config values
    private MarkerConfig getMarkerConfig(ResidenceData resData, boolean isSubzone) {
        if (resData.forSale) {
            return isSubzone ?
                    new MarkerConfig(configCache.saleSubzoneType, configCache.saleSubzoneDetail, configCache.saleSubzoneIcon, configCache.saleSubzoneFlagDetail,
                            configCache.saleSubzoneLineColor, configCache.saleSubzoneFillColor, configCache.saleSubzoneLineWidth, configCache.saleSubzoneYHeight,
                            configCache.saleSubzonePoints, configCache.saleSubzoneMaxFlags, configCache.saleSubzoneAnchorX, configCache.saleSubzoneAnchorY,
                            configCache.saleSubzoneDepthTest, configCache.saleSubzoneCenterPointer) :
                    new MarkerConfig(configCache.saleType, configCache.saleDetail, configCache.saleIcon, configCache.saleFlagDetail,
                            configCache.saleLineColor, configCache.saleFillColor, configCache.saleLineWidth, configCache.saleYHeight,
                            configCache.salePoints, configCache.saleMaxFlags, configCache.saleAnchorX, configCache.saleAnchorY,
                            configCache.saleDepthTest, configCache.saleCenterPointer);
        } else if (resData.forRent) {
            return isSubzone ?
                    new MarkerConfig(configCache.rentSubzoneType, configCache.rentSubzoneDetail, configCache.rentSubzoneIcon, configCache.rentSubzoneFlagDetail,
                            configCache.rentSubzoneLineColor, configCache.rentSubzoneFillColor, configCache.rentSubzoneLineWidth, configCache.rentSubzoneYHeight,
                            configCache.rentSubzonePoints, configCache.rentSubzoneMaxFlags, configCache.rentSubzoneAnchorX, configCache.rentSubzoneAnchorY,
                            configCache.rentSubzoneDepthTest, configCache.rentSubzoneCenterPointer) :
                    new MarkerConfig(configCache.rentType, configCache.rentDetail, configCache.rentIcon, configCache.rentFlagDetail,
                            configCache.rentLineColor, configCache.rentFillColor, configCache.rentLineWidth, configCache.rentYHeight,
                            configCache.rentPoints, configCache.rentMaxFlags, configCache.rentAnchorX, configCache.rentAnchorY,
                            configCache.rentDepthTest, configCache.rentCenterPointer);
        } else {
            return isSubzone ?
                    new MarkerConfig(configCache.subzoneType, configCache.subzoneDetail, configCache.subzoneIcon, configCache.subzoneFlagDetail,
                            configCache.subzoneLineColor, configCache.subzoneFillColor, configCache.subzoneLineWidth, configCache.subzoneYHeight,
                            configCache.subzonePoints, configCache.subzoneMaxFlags, configCache.subzoneAnchorX, configCache.subzoneAnchorY,
                            configCache.subzoneDepthTest, configCache.subzoneCenterPointer) :
                    new MarkerConfig(configCache.markerType, configCache.markerDetail, configCache.markerIcon, configCache.markerFlagDetail,
                            configCache.markerLineColor, configCache.markerFillColor, configCache.markerLineWidth, configCache.markerYHeight,
                            configCache.markerPoints, configCache.markerMaxFlags, configCache.markerAnchorX, configCache.markerAnchorY,
                            configCache.markerDepthTest, configCache.markerCenterPointer);
        }
    }

    // Cache management
    private static ResidencePlayer getCachedResidencePlayer(ClaimedResidence res) {
        String playerName = res.getPermissions().getOwner();
        if (playerName == null) return null;

        return playerCache.computeIfAbsent(playerName, name -> res.getRPlayer());
    }

    private void clearCachesIfNeeded() {
        long now = System.currentTimeMillis();

        // Clear player cache every 5 minutes
        if ((now - lastPlayerCacheClear) > 300000) {
            playerCache.clear();
            lastPlayerCacheClear = now;
            getLogger().info("Player cache cleared");
        }

        // Clear placeholder cache every 2 minutes
        if ((now - lastPlaceholderCacheClear) > 120000) {
            placeholderCache.clear();
            lastPlaceholderCacheClear = now;
            getLogger().info("Placeholder cache cleared");
        }
    }

    private MarkerSet getOrCreateMarkerSet(de.bluecolored.bluemap.api.BlueMapMap map, boolean isSubzone) {
        if (isSubzone && !MarkerSetLabelResidence.equals(MarkerSetLabelSubzones)) {
            return map.getMarkerSets().getOrDefault(MarkerSetIdSubzones,
                    MarkerSet.builder().label(MarkerSetLabelSubzones).build());
        } else {
            return map.getMarkerSets().getOrDefault(MarkerSetIdResidence,
                    MarkerSet.builder().label(MarkerSetLabelResidence).build());
        }
    }

    private String prepareDetailOptimized(String template, ResidenceData resData) {
        if (template == null) return "";

        String cacheKey = template + "|" + resData.residence.getName() + "|" + resData.ownerName;
        return placeholderCache.computeIfAbsent(cacheKey, key -> {
            String detail = template.replace("[ResName]", resData.residence.getName())
                    .replace("[OwnerName]", resData.ownerName);

            // Apply PlaceholderAPI if available
            if (papiState && resData.residence.getRPlayer() != null) {
                Player player = resData.residence.getRPlayer().getPlayer();
                if (player != null) {
                    detail = PlaceholderAPI.setPlaceholders(player, detail);
                }
            }
            return detail;
        });
    }

    private String generateFlagsString(ClaimedResidence res, String flagTemplate, int maxFlags) {
        if (maxFlags == 0 || flagTemplate == null) return "";

        StringBuilder flags = new StringBuilder();
        int flagCount = 0;

        for(Map.Entry<String, Boolean> flag : res.getPermissions().getFlags().entrySet()) {
            String flagDetail = flagTemplate.replace("[FlagKey]", flag.getKey())
                    .replace("[FlagValue]", flag.getValue().toString());

            if (maxFlags < 0 || flagCount < maxFlags) {
                flags.append(flagDetail);
                flagCount++;
            } else {
                break;
            }
        }

        return flags.toString();
    }

    private Marker createMarkerByType(MarkerConfig config, String detail, AreaCalculation calc, String resName) {
        double pointY = config.centerPointer ? config.yHeight : calc.centY;

        switch (config.type.toLowerCase()) {
            case "point":
                return POIMarker.builder()
                        .label(resName)
                        .detail(detail)
                        .position(calc.centX, pointY, calc.centZ)
                        .icon(config.icon, config.anchorX, config.anchorY)
                        .build();

            case "circle":
                return ExtrudeMarker.builder()
                        .label(resName)
                        .detail(detail)
                        .shape(Shape.createCircle(calc.centX, calc.centZ, calc.dist, config.points), calc.y[0], calc.y[1])
                        .centerPosition()
                        .lineColor(config.lineColor)
                        .fillColor(config.fillColor)
                        .lineWidth(config.lineWidth)
                        .depthTestEnabled(config.depthTest)
                        .build();

            case "ellipse":
                return ExtrudeMarker.builder()
                        .label(resName)
                        .detail(detail)
                        .shape(Shape.createEllipse(calc.centX, calc.centZ, calc.distX, calc.distZ, config.points), calc.y[0], calc.y[1])
                        .centerPosition()
                        .lineColor(config.lineColor)
                        .fillColor(config.fillColor)
                        .lineWidth(config.lineWidth)
                        .depthTestEnabled(config.depthTest)
                        .build();

            case "rectangle2d":
                return ShapeMarker.builder()
                        .label(resName)
                        .detail(detail)
                        .shape(Shape.createRect(calc.x[0], calc.z[0], calc.x[1], calc.z[1]), config.yHeight)
                        .centerPosition()
                        .lineColor(config.lineColor)
                        .fillColor(config.fillColor)
                        .lineWidth(config.lineWidth)
                        .depthTestEnabled(config.depthTest)
                        .build();

            case "circle2d":
                return ShapeMarker.builder()
                        .label(resName)
                        .detail(detail)
                        .shape(Shape.createCircle(calc.centX, calc.centZ, calc.dist, config.points), config.yHeight)
                        .centerPosition()
                        .lineColor(config.lineColor)
                        .fillColor(config.fillColor)
                        .lineWidth(config.lineWidth)
                        .depthTestEnabled(config.depthTest)
                        .build();

            case "ellipse2d":
                return ShapeMarker.builder()
                        .label(resName)
                        .detail(detail)
                        .shape(Shape.createEllipse(calc.centX, calc.centZ, calc.distX, calc.distZ, config.points), config.yHeight)
                        .centerPosition()
                        .lineColor(config.lineColor)
                        .fillColor(config.fillColor)
                        .lineWidth(config.lineWidth)
                        .depthTestEnabled(config.depthTest)
                        .build();

            default: // rectangle
                return ExtrudeMarker.builder()
                        .label(resName)
                        .detail(detail)
                        .shape(Shape.createRect(calc.x[0], calc.z[0], calc.x[1], calc.z[1]), calc.y[0], calc.y[1])
                        .centerPosition()
                        .lineColor(config.lineColor)
                        .fillColor(config.fillColor)
                        .lineWidth(config.lineWidth)
                        .depthTestEnabled(config.depthTest)
                        .build();
        }
    }

    public void reloadConfigCache() {
        reloadConfig();
        configCache = new ConfigCache(this);
        playerCache.clear();
        placeholderCache.clear();
        getLogger().info("Config cache reloaded");
    }

    public String Placeholder(Player player, String text) {
        if(papiState && player != null) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }

    public void refreshPl() {
        Scheduler.run(this::addResMarkers);
    }
}