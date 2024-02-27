package me.Mixer.bluemapresidence;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.CuboidArea;
import com.bekvon.bukkit.residence.protection.ResidenceManager;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.*;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JavaPlugin {
    public static Logger log;

    public Residence res;
    public ResidenceManager resmgr;

    public config config;

    @Override
    public void onLoad() {
        log = this.getLogger() ;
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

    public BlueMapAPI blueMap;

    //Default variables
    public String MarkerSetIdResidence = "residences";

    public String BMResLinkSpigot = "https://www.spigotmc.org/resources/107389/";

    public String MarkerSetLabelResidence = "Residences";

    public boolean papiState;

    private Scheduler.Task updateTimer;

    @Override
    public void onEnable() {
        registerFiles();
        MarkerSetLabelResidence = getConfig().getString("marker.name", "Residences");
        new UpdateChecker(this, 107389).getVersion(version -> {
            if (!this.getPluginMeta().getVersion().equalsIgnoreCase(version)) {
                info("There is a new update available. " + BMResLinkSpigot);
            }
        });

        PluginManager pm = getServer().getPluginManager();

        /* Get Residence */
        Plugin p = pm.getPlugin("Residence");
        if(p == null) {
            severe("Cannot find Residence!");
            return;
        }

        /* Get PlaceholderAPI */
        Plugin papi = pm.getPlugin("PlaceholderAPI");
        if(papi == null) {
            info("&cPlaceholderAPI not found, working without it");
            papiState = false;
        } else {
            papiState = true;
        }

        res = (Residence)p;

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

        pm.registerEvents(new ServerJoin(this), this);

        BlueMapAPI.onEnable(api -> {
            this.blueMap = api;
            this.resmgr = Residence.getInstance().getResidenceManager();
            if(res.isEnabled()) {
                if(getConfig().getBoolean("update.onperiod", true)) {
                    final long updateInterval = Math.max(1, getConfig().getLong("update.period", 300));
                    updateTimer = Scheduler.runTimer(this::refreshMarkers, 0, 20 * updateInterval);
                   // getServer().getScheduler().runTaskTimer(this, this::refreshMarkers, 0, 20 * updateInterval);
                }

                if(getConfig().getBoolean("update.onchange", true)) {
                    pm.registerEvents(new EventList(this), this);
                }
                info("Version " + this.getPluginMeta().getVersion() + " is activated");
            }
        });

        Objects.requireNonNull(getCommand("bluemapresidence")).setExecutor(new ReloadCommand(this));
    }

    @Override
    public void onDisable() {
        BlueMapAPI.onDisable(blueMapAPI -> {
            if(updateTimer != null) updateTimer.cancel();
            this.blueMap = null;
        });
    }

    //Register config file
    public void registerFiles() {
        this.config = new config(this);
        this.saveDefaultConfig();
        new config(this).testCompareConfig();
    }
    public void addResMarkers() {
        reloadConfig();
        for(final String resid : resmgr.getResidenceList(true, false).toArray(new String[0])) {
            ClaimedResidence res = resmgr.getByName(resid);
            CuboidArea[] areas = res.getAreaArray();

            for(int i = 0; i < areas.length; i++) {
                String AreaResid = resid + "%" + i; //Make area ID for cuboid
                Location ll = areas[i].getLowLocation(); //Lowest location
                Location lh = areas[i].getHighLocation(); //Highest location

                double dist = ll.distance(lh)/2; //Get distance between ll and lh (for circle marker)

                //Get distance between ll x and lh x (for ellipse marker)

                double minX = ll.getX();
                double maxX = lh.getX()+1;


                if(minX > maxX) {
                    minX = lh.getX();
                    maxX = ll.getX();
                }

                //Get distance between ll z and lh z (for ellipse marker)

                double minZ = ll.getZ();
                double maxZ = lh.getZ()+1;


                if(minZ > maxZ) {
                    minZ = lh.getZ();
                    maxZ = ll.getZ();
                }

                double distX = (minX - maxX) / 2;
                double distZ = (minZ - maxZ) / 2;

                double[] x = new double[2]; //Define two x points
                float[] y = new float[2]; //Define two y points
                double[] z = new double[2]; //Define two z points

                //Get two x points

                x[0] = minX;
                x[1] = maxX;

                //Get two y points

                y[0] = (float) ll.getY()+1;
                y[1] = (float) lh.getY()+1;

                //Get two z points

                z[0] = minZ;
                z[1] = maxZ;

                //Get center points

                double centX = (x[0]+x[1])/2;
                double centY = (y[0]+y[1])/2;
                double centZ = (z[0]+z[1])/2;

                blueMap.getWorld(areas[i].getWorld().getUID()).ifPresent(blueWorld -> blueWorld.getMaps().forEach(map -> {
                    //Create marker set
                    final MarkerSet markerSetRes = map.getMarkerSets().getOrDefault(MarkerSetIdResidence, MarkerSet.builder().label(MarkerSetLabelResidence).build());
                    StringBuilder flags = new StringBuilder(); //Define string flags
                    int flagCount = 0; // Define flag count (to maxFlags config)
                    //Load config detail and register placeholders
                    String ConfDetail = getConfig().getString("marker.detail");
                    assert ConfDetail != null;
                    ConfDetail = ConfDetail.replace("[ResName]", res.getName());
                    ConfDetail = ConfDetail.replace("[OwnerName]", res.getRPlayer().getName());

                    int maxFlags = getConfig().getInt("marker.maxFlags", -1);

                    //Add all use flags to flags string
                    for(Map.Entry<String, Boolean> flag : res.getPermissions().getFlags().entrySet()) {
                        String toFlags = getConfig().getString("marker.flagDetail", "[FlagKey]: [FlagValue]<br>");
                        toFlags = toFlags.replace("[FlagKey]", flag.getKey());
                        toFlags = toFlags.replace("[FlagValue]", flag.getValue().toString());
                        if(maxFlags != 0 && flagCount < maxFlags) { //If flags enabled and flagCount is right
                            flags.append(toFlags);
                            flagCount++;
                        } else if(maxFlags < 0) { //Unlimited flags
                            flags.append(toFlags);
                        }
                    }

                    //Register PlaceholderAPI placeholders
                    String newDetail = Placeholder(res.getRPlayer().getPlayer(), ConfDetail);
                    //Add flags to res detail
                    newDetail += flags;

                    //Define Line color
                    Color LineColor = new Color(getConfig().getInt("marker.LineColor.r", 255), getConfig().getInt("marker.LineColor.g",0), getConfig().getInt("marker.LineColor.b",0), (float) getConfig().getDouble("marker.LineColor.a",1.0));
                    //Define fill color
                    Color FillColor = new Color(getConfig().getInt("marker.FillColor.r", 200), getConfig().getInt("marker.FillColor.g",0), getConfig().getInt("marker.FillColor.b",0), (float) getConfig().getDouble("marker.FillColor.a",0.3));

                    //Define line width
                    int lineWidth = getConfig().getInt("marker.LineWidth", 3);

                    //Define Y height
                    int yHeight = getConfig().getInt("marker.Yheight", 3);

                    boolean depthTest = getConfig().getBoolean("marker.depth-test", true);
                    boolean centerPointer = getConfig().getBoolean("centerPointerMarkerHeight", true);

                    //Create extrude rectangle marker
                    ExtrudeMarker rectangleExtrudeMarker = ExtrudeMarker.builder()
                            .label(res.getName())
                            .detail(newDetail)
                            .shape(Shape.createRect(x[0],z[0],x[1], z[1]),y[0], y[1])
                            .centerPosition()
                            .lineColor(LineColor)
                            .fillColor(FillColor)
                            .lineWidth(lineWidth)
                            .depthTestEnabled(depthTest)
                            .build();

                    //Create shape rectangle marker
                    ShapeMarker rectangleExtrudeMarker2D = ShapeMarker.builder()
                            .label(res.getName())
                            .detail(newDetail)
                            .shape(Shape.createRect(x[0], z[0], x[1], z[1]),yHeight)
                            .depthTestEnabled(false)
                            .centerPosition()
                            .lineColor(LineColor)
                            .fillColor(FillColor)
                            .lineWidth(lineWidth)
                            .depthTestEnabled(depthTest)
                            .build();

                    //Create point
                    double pointY = centY;
                    if(centerPointer) pointY = yHeight;

                    POIMarker pointMarker = POIMarker.builder()
                            .label(res.getName())
                            .detail(newDetail)
                            .position(centX, pointY, centZ) //calculate center of res
                            .icon(getConfig().getString("marker.icon.url", "https://raw.githubusercontent.com/BlueMap-Minecraft/BlueMap/master/BlueMapCommon/webapp/public/assets/poi.svg"), getConfig().getInt("marker.icon.anchorX"), getConfig().getInt("marker.icon.anchorY"))
                            .build();

                    //Create extrude circle marker
                    ExtrudeMarker circleExtrudeMarker = ExtrudeMarker.builder()
                            .label(res.getName())
                            .detail(newDetail)
                            .shape(Shape.createCircle(centX, centZ, dist, getConfig().getInt("marker.points", 100)), y[0], y[1])
                            .centerPosition()
                            .lineColor(LineColor)
                            .fillColor(FillColor)
                            .lineWidth(lineWidth)
                            .depthTestEnabled(depthTest)
                            .build();

                    //Create shape circle marker
                    ShapeMarker circleExtrudeMarker2D = ShapeMarker.builder()
                            .label(res.getName())
                            .detail(newDetail)
                            .shape(Shape.createCircle(centX, centZ, dist, getConfig().getInt("marker.points", 100)), yHeight)
                            .centerPosition()
                            .lineColor(LineColor)
                            .fillColor(FillColor)
                            .lineWidth(lineWidth)
                            .depthTestEnabled(depthTest)
                            .build();

                    //Create extrude ellipse marker
                    ExtrudeMarker ellipseExtrudeMarker = ExtrudeMarker.builder()
                            .label(res.getName())
                            .detail(newDetail)
                            .shape(Shape.createEllipse(centX, centZ, distX, distZ, getConfig().getInt("marker.points", 100)), y[0], y[1])
                            .centerPosition()
                            .lineColor(LineColor)
                            .fillColor(FillColor)
                            .lineWidth(lineWidth)
                            .depthTestEnabled(depthTest)
                            .build();

                    //Create shape ellipse marker
                    ShapeMarker ellipseExtrudeMarker2D = ShapeMarker.builder()
                            .label(res.getName())
                            .detail(newDetail)
                            .shape(Shape.createEllipse(centX, centZ, distX, distZ, getConfig().getInt("marker.points", 100)), yHeight)
                            .centerPosition()
                            .lineColor(LineColor)
                            .fillColor(FillColor)
                            .lineWidth(lineWidth)
                            .depthTestEnabled(depthTest)
                            .build();

                    //Add marker to marker set

                    switch (Objects.requireNonNull(getConfig().getString("marker.type")).toLowerCase()) {
                        case "point": markerSetRes.getMarkers().put(AreaResid, pointMarker); break;
                        case "circle": markerSetRes.getMarkers().put(AreaResid, circleExtrudeMarker); break;
                        case "ellipse": markerSetRes.getMarkers().put(AreaResid, ellipseExtrudeMarker); break;
                        case "rectangle2d": markerSetRes.getMarkers().put(AreaResid, rectangleExtrudeMarker2D); break;
                        case "circle2d": markerSetRes.getMarkers().put(AreaResid, circleExtrudeMarker2D); break;
                        case "ellipse2d": markerSetRes.getMarkers().put(AreaResid, ellipseExtrudeMarker2D); break;
                        default: markerSetRes.getMarkers().put(AreaResid, rectangleExtrudeMarker); break;
                    }

                    //Add marker set to map
                    map.getMarkerSets().put(MarkerSetIdResidence, markerSetRes);
                }));
            }
        }
    }

    private void refreshMarkers() {
        reloadConfig();
        addResMarkers();
    }

    public String Placeholder(Player player, String text) {
        if(papiState) return PlaceholderAPI.setPlaceholders(player, text);
        else return text;
    }

    public void refreshPl() {
        Scheduler.run(this::refreshMarkers);
        //getServer().getScheduler().runTask(this, this::refreshMarkers);
    }

}
