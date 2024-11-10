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
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
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
    public String MarkerSetIdSubzones = "res-subzones";

    public String BMResLinkSpigot = "https://www.spigotmc.org/resources/107389/";

    public String MarkerSetLabelResidence = "Residences";
    public String MarkerSetLabelSubzones = "Residences";

    public boolean papiState;

    private Scheduler.Task updateTimer;

    @Override
    public void onEnable() {
        registerFiles();
        MarkerSetLabelResidence = getConfig().getString("marker.name", "Residences");
        MarkerSetLabelSubzones = getConfig().getString("marker.subzone.name", "Residences");
        new UpdateChecker(this, 107389).getVersion(version -> {
            if (!this.getDescription().getVersion().equalsIgnoreCase(version)) {
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
                info("Version " + this.getDescription().getVersion() + " is activated");
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
            List<ClaimedResidence> subzones = res.getSubzones();
            //all areas
            generateMarkers(areas, resid, "r", res);

            //all subzones
            for (ClaimedResidence subzone : subzones) {
                generateMarkers(subzone.getAreaArray(), resid, "s", subzone);
            }
        }
    }

    private void generateMarkers(CuboidArea[] areas, String resid, String type, ClaimedResidence res) {
        for(int i = 0; i < areas.length; i++) {
            String AreaResid = resid + "%" + type + i; //Make area ID for cuboid
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
                final MarkerSet markerSetSubzones = map.getMarkerSets().getOrDefault(MarkerSetIdSubzones, MarkerSet.builder().label(MarkerSetLabelSubzones).build());

                int flagCount = 0; // Define flag count (to maxFlags config)

                //Initialize all markers
                ExtrudeMarker rectangleExtrudeMarker;
                ShapeMarker rectangleExtrudeMarker2D;
                POIMarker pointMarker;
                ExtrudeMarker circleExtrudeMarker;
                ShapeMarker circleExtrudeMarker2D;
                ExtrudeMarker ellipseExtrudeMarker;
                ShapeMarker ellipseExtrudeMarker2D;

                //**** Residence for sale ****\\
                if(res.isForSell()) {
                    String mtypeSale = "marker-For_Sale.type";
                    if(Objects.equals(type, "s")) mtypeSale = "marker-For_Sale.subzone.type";

                    StringBuilder flagsSale = new StringBuilder(); //Define string flags for ForSale res
                    //Load config detail and register placeholders for res that are for sale
                    String ConfDetailSale = getConfig().getString("marker-For_Sale.detail");
                    if(Objects.equals(type, "s")) ConfDetailSale = getConfig().getString("marker-For_Sale.subzone.detail");
                    assert ConfDetailSale != null;
                    ConfDetailSale = ConfDetailSale.replace("[ResName]", res.getName());
                    ConfDetailSale = ConfDetailSale.replace("[OwnerName]", res.getRPlayer().getName());

                    //Register PlaceholderAPI placeholders (ForSale res)
                    String newDetailSale = Placeholder(res.getRPlayer().getPlayer(), ConfDetailSale);
                    //Add flags to res detail
                    newDetailSale += flagsSale;

                    int maxFlagsSale = getConfig().getInt("marker-For_Sale.maxFlags", -1);
                    if(Objects.equals(type, "s")) maxFlagsSale = getConfig().getInt("marker-For_Sale.subzone.maxFlags", -1);

                    //Add all use flags to flags string to ForSale res
                    for(Map.Entry<String, Boolean> flag : res.getPermissions().getFlags().entrySet()) {
                        String toFlagsSale = getConfig().getString("marker-For_Sale.flagDetail", "[FlagKey]: [FlagValue]<br>");
                        if(Objects.equals(type, "s")) toFlagsSale = getConfig().getString("marker-For_Sale.subzone.flagDetail", "[FlagKey]: [FlagValue]<br>");
                        toFlagsSale = toFlagsSale.replace("[FlagKey]", flag.getKey());
                        toFlagsSale = toFlagsSale.replace("[FlagValue]", flag.getValue().toString());
                        if(maxFlagsSale != 0 && flagCount < maxFlagsSale) { //If flags enabled and flagCount is right
                            flagsSale.append(toFlagsSale);
                            flagCount++;
                        } else if(maxFlagsSale < 0) { //Unlimited flags
                            flagsSale.append(toFlagsSale);
                        }
                    }

                    //Define Line color for sale
                    Color LineColorSale = new Color(getConfig().getInt("marker-For_Sale.LineColor.r", 255), getConfig().getInt("marker-For_Sale.LineColor.g",0), getConfig().getInt("marker-For_Sale.LineColor.b",0), (float) getConfig().getDouble("marker-For_Sale.LineColor.a",1.0));
                    if(Objects.equals(type, "s")) LineColorSale = new Color(getConfig().getInt("marker-For_Sale.subzone.LineColor.r", 255), getConfig().getInt("marker-For_Sale.subzone.LineColor.g",0), getConfig().getInt("marker-For_Sale.subzone.LineColor.b",0), (float) getConfig().getDouble("marker-For_Sale.subzone.LineColor.a",1.0));
                    //Define fill color for sale
                    Color FillColorSale = new Color(getConfig().getInt("marker-For_Sale.FillColor.r", 200), getConfig().getInt("marker-For_Sale.FillColor.g",0), getConfig().getInt("marker-For_Sale.FillColor.b",0), (float) getConfig().getDouble("marker-For_Sale.FillColor.a",0.3));
                    if(Objects.equals(type, "s")) FillColorSale = new Color(getConfig().getInt("marker-For_Sale.subzone.FillColor.r", 200), getConfig().getInt("marker-For_Sale.subzone.FillColor.g",0), getConfig().getInt("marker-For_Sale.subzone.FillColor.b",0), (float) getConfig().getDouble("marker-For_Sale.subzone.FillColor.a",0.3));
                    //Define line width for sale
                    int lineWidthSale = getConfig().getInt("marker-For_Sale.LineWidth", 3);
                    if(Objects.equals(type, "s")) lineWidthSale = getConfig().getInt("marker-For_Sale.subzone.LineWidth", 3);
                    //Define Y height for sale
                    int yHeightSale = getConfig().getInt("marker-For_Sale.Yheight", 60);
                    if(Objects.equals(type, "s")) yHeightSale = getConfig().getInt("marker-For_Sale.subzone.Yheight", 60);
                    //DepthTest
                    boolean depthTestSale = getConfig().getBoolean("marker-For_Sale.depth-test", true);
                    if(Objects.equals(type, "s")) depthTestSale = getConfig().getBoolean("marker-For_Sale.subzone.depth-test", true);
                    //CenterPointer position
                    boolean centerPointerSale = getConfig().getBoolean("marker-For_Sale.centerPointerMarkerHeight", true);
                    if(Objects.equals(type, "s")) centerPointerSale = getConfig().getBoolean("marker-For_Sale.subzone.centerPointerMarkerHeight", true);
                    //icon
                    String iconSale = getConfig().getString("marker-For_Sale.icon.url", "https://raw.githubusercontent.com/BlueMap-Minecraft/BlueMap/master/BlueMapCommon/webapp/public/assets/poi.svg");
                    if(Objects.equals(type, "s")) iconSale = getConfig().getString("marker-For_Sale.subzone.icon.url", "https://raw.githubusercontent.com/BlueMap-Minecraft/BlueMap/master/BlueMapCommon/webapp/public/assets/poi.svg");
                    //anchor X point
                    int anchorXSale = getConfig().getInt("marker-For_Sale.icon.anchorX");
                    if(Objects.equals(type, "s")) anchorXSale = getConfig().getInt("marker-For_Sale.subzone.icon.anchorX");
                    //anchor Y point
                    int anchorYSale = getConfig().getInt("marker-For_Sale.icon.anchorY");
                    if(Objects.equals(type, "s")) anchorYSale = getConfig().getInt("marker-For_Sale.subzone.icon.anchorY");
                    //points
                    int pointsSale = getConfig().getInt("marker-For_Sale.points", 100);
                    if(Objects.equals(type, "s")) pointsSale = getConfig().getInt("marker-For_Sale.subzone.points", 100);

                    double pointYSale = centY;
                    if(centerPointerSale) pointYSale = yHeightSale;

                    //Add marker to marker set
                    if(Objects.equals(type, "s") && !MarkerSetLabelResidence.equals(MarkerSetLabelSubzones)) {
                        switch (Objects.requireNonNull(getConfig().getString(mtypeSale)).toLowerCase()) {
                            case "point":
                                pointMarker = generatePointMarker(newDetailSale, centX, pointYSale, centZ, iconSale, anchorXSale, anchorYSale);
                                markerSetSubzones.getMarkers().put(AreaResid, pointMarker);
                                break;
                            case "circle":
                                circleExtrudeMarker = generateCircleExtrudeMarker(newDetailSale, LineColorSale, FillColorSale, lineWidthSale, depthTestSale, centX, y, centZ, dist, pointsSale);
                                markerSetSubzones.getMarkers().put(AreaResid, circleExtrudeMarker);
                                break;
                            case "ellipse":
                                ellipseExtrudeMarker = generateEllipseExtrudeMarker(newDetailSale, LineColorSale, FillColorSale, lineWidthSale, depthTestSale, centX, y, centZ, distX, distZ, pointsSale);
                                markerSetSubzones.getMarkers().put(AreaResid, ellipseExtrudeMarker);
                                break;
                            case "rectangle2d":
                                rectangleExtrudeMarker2D = generateRectangleExtrudeMarker2D(newDetailSale, LineColorSale, FillColorSale, lineWidthSale, depthTestSale, x, yHeightSale, z);
                                markerSetSubzones.getMarkers().put(AreaResid, rectangleExtrudeMarker2D);
                                break;
                            case "circle2d":
                                circleExtrudeMarker2D = generateCircleExtrudeMarker2D(newDetailSale, LineColorSale, FillColorSale, lineWidthSale, depthTestSale, centX, yHeightSale, centZ, dist, pointsSale);
                                markerSetSubzones.getMarkers().put(AreaResid, circleExtrudeMarker2D);
                                break;
                            case "ellipse2d":
                                ellipseExtrudeMarker2D = generateEllipseExtrudeMarker2D(newDetailSale, LineColorSale, FillColorSale, lineWidthSale, depthTestSale, centX, yHeightSale, centZ, distX, distZ, pointsSale);
                                markerSetSubzones.getMarkers().put(AreaResid, ellipseExtrudeMarker2D);
                                break;
                            default:
                                rectangleExtrudeMarker = generateRectangleExtrudeMarker(newDetailSale, LineColorSale, FillColorSale, lineWidthSale, depthTestSale, x, y, z);
                                markerSetSubzones.getMarkers().put(AreaResid, rectangleExtrudeMarker);
                                break;
                        }
                    } else {
                        switch (Objects.requireNonNull(getConfig().getString(mtypeSale)).toLowerCase()) {
                            case "point":
                                pointMarker = generatePointMarker(newDetailSale, centX, pointYSale, centZ, iconSale, anchorXSale, anchorYSale);
                                markerSetRes.getMarkers().put(AreaResid, pointMarker);
                                break;
                            case "circle":
                                circleExtrudeMarker = generateCircleExtrudeMarker(newDetailSale, LineColorSale, FillColorSale, lineWidthSale, depthTestSale, centX, y, centZ, dist, pointsSale);
                                markerSetRes.getMarkers().put(AreaResid, circleExtrudeMarker);
                                break;
                            case "ellipse":
                                ellipseExtrudeMarker = generateEllipseExtrudeMarker(newDetailSale, LineColorSale, FillColorSale, lineWidthSale, depthTestSale, centX, y, centZ, distX, distZ, pointsSale);
                                markerSetRes.getMarkers().put(AreaResid, ellipseExtrudeMarker);
                                break;
                            case "rectangle2d":
                                rectangleExtrudeMarker2D = generateRectangleExtrudeMarker2D(newDetailSale, LineColorSale, FillColorSale, lineWidthSale, depthTestSale, x, yHeightSale, z);
                                markerSetRes.getMarkers().put(AreaResid, rectangleExtrudeMarker2D);
                                break;
                            case "circle2d":
                                circleExtrudeMarker2D = generateCircleExtrudeMarker2D(newDetailSale, LineColorSale, FillColorSale, lineWidthSale, depthTestSale, centX, yHeightSale, centZ, dist, pointsSale);
                                markerSetRes.getMarkers().put(AreaResid, circleExtrudeMarker2D);
                                break;
                            case "ellipse2d":
                                ellipseExtrudeMarker2D = generateEllipseExtrudeMarker2D(newDetailSale, LineColorSale, FillColorSale, lineWidthSale, depthTestSale, centX, yHeightSale, centZ, distX, distZ, pointsSale);
                                markerSetRes.getMarkers().put(AreaResid, ellipseExtrudeMarker2D);
                                break;
                            default:
                                rectangleExtrudeMarker = generateRectangleExtrudeMarker(newDetailSale, LineColorSale, FillColorSale, lineWidthSale, depthTestSale, x, y, z);
                                markerSetRes.getMarkers().put(AreaResid, rectangleExtrudeMarker);
                                break;
                        }
                    }
                }


                //**** Residence for rent ****\\
                else if(res.isForRent()) {
                    String mtypeRent = "marker-For_Rent.type";
                    if(Objects.equals(type, "s")) mtypeRent = "marker-For_Rent.subzone.type";

                    StringBuilder flagsRent = new StringBuilder(); //Define string flags for ForRent res
                    //Load config detail and register placeholders for res that are for rent
                    String ConfDetailRent = getConfig().getString("marker-For_Rent.detail");
                    if(Objects.equals(type, "s")) ConfDetailRent = getConfig().getString("marker-For_Rent.subzone.detail");
                    assert ConfDetailRent != null;
                    ConfDetailRent = ConfDetailRent.replace("[ResName]", res.getName());
                    ConfDetailRent = ConfDetailRent.replace("[OwnerName]", res.getRPlayer().getName());

                    //Register PlaceholderAPI placeholders (ForRent res)
                    String newDetailRent = Placeholder(res.getRPlayer().getPlayer(), ConfDetailRent);
                    //Add flags to res detail
                    newDetailRent += flagsRent;

                    int maxFlagsRent = getConfig().getInt("marker-For_Rent.maxFlags", -1);
                    if(Objects.equals(type, "s")) maxFlagsRent = getConfig().getInt("marker-For_Rent.subzone.maxFlags", -1);

                    //Add all use flags to flags string to ForRent res
                    for(Map.Entry<String, Boolean> flag : res.getPermissions().getFlags().entrySet()) {
                        String toFlagsRent = getConfig().getString("marker-For_Rent.flagDetail", "[FlagKey]: [FlagValue]<br>");
                        if(Objects.equals(type, "s")) toFlagsRent = getConfig().getString("marker-For_Rent.subzone.flagDetail", "[FlagKey]: [FlagValue]<br>");
                        toFlagsRent = toFlagsRent.replace("[FlagKey]", flag.getKey());
                        toFlagsRent = toFlagsRent.replace("[FlagValue]", flag.getValue().toString());
                        if(maxFlagsRent != 0 && flagCount < maxFlagsRent) { //If flags enabled and flagCount is right
                            flagsRent.append(toFlagsRent);
                            flagCount++;
                        } else if(maxFlagsRent < 0) { //Unlimited flags
                            flagsRent.append(toFlagsRent);
                        }
                    }

                    //Define Line color for rent
                    Color LineColorRent = new Color(getConfig().getInt("marker-For_Rent.LineColor.r", 255), getConfig().getInt("marker-For_Rent.LineColor.g",0), getConfig().getInt("marker-For_Rent.LineColor.b",0), (float) getConfig().getDouble("marker-For_Rent.LineColor.a",1.0));
                    if(Objects.equals(type, "s")) LineColorRent = new Color(getConfig().getInt("marker-For_Rent.subzone.LineColor.r", 255), getConfig().getInt("marker-For_Rent.subzone.LineColor.g",0), getConfig().getInt("marker-For_Rent.subzone.LineColor.b",0), (float) getConfig().getDouble("marker-For_Rent.subzone.LineColor.a",1.0));
                    //Define fill color for rent
                    Color FillColorRent = new Color(getConfig().getInt("marker-For_Rent.FillColor.r", 200), getConfig().getInt("marker-For_Rent.FillColor.g",0), getConfig().getInt("marker-For_Rent.FillColor.b",0), (float) getConfig().getDouble("marker-For_Rent.FillColor.a",0.3));
                    if(Objects.equals(type, "s")) FillColorRent = new Color(getConfig().getInt("marker-For_Rent.subzone.FillColor.r", 200), getConfig().getInt("marker-For_Rent.subzone.FillColor.g",0), getConfig().getInt("marker-For_Rent.subzone.FillColor.b",0), (float) getConfig().getDouble("marker-For_Rent.subzone.FillColor.a",0.3));
                    //Define line width for rent
                    int lineWidthRent = getConfig().getInt("marker-For_Rent.LineWidth", 3);
                    if(Objects.equals(type, "s")) lineWidthRent = getConfig().getInt("marker-For_Rent.subzone.LineWidth", 3);
                    //Define Y height for rent
                    int yHeightRent = getConfig().getInt("marker-For_Rent.Yheight", 60);
                    if(Objects.equals(type, "s")) yHeightRent = getConfig().getInt("marker-For_Rent.subzone.Yheight", 60);
                    //DepthTest
                    boolean depthTestRent = getConfig().getBoolean("marker-For_Rent.depth-test", true);
                    if(Objects.equals(type, "s")) depthTestRent = getConfig().getBoolean("marker-For_Rent.subzone.depth-test", true);
                    //CenterPointer position
                    boolean centerPointerRent = getConfig().getBoolean("marker-For_Rent.centerPointerMarkerHeight", true);
                    if(Objects.equals(type, "s")) centerPointerRent = getConfig().getBoolean("marker-For_Rent.subzone.centerPointerMarkerHeight", true);
                    //icon
                    String iconRent = getConfig().getString("marker-For_Rent.icon.url", "https://raw.githubusercontent.com/BlueMap-Minecraft/BlueMap/master/BlueMapCommon/webapp/public/assets/poi.svg");
                    if(Objects.equals(type, "s")) iconRent = getConfig().getString("marker-For_Rent.subzone.icon.url", "https://raw.githubusercontent.com/BlueMap-Minecraft/BlueMap/master/BlueMapCommon/webapp/public/assets/poi.svg");
                    //anchor X point
                    int anchorXRent = getConfig().getInt("marker-For_Rent.icon.anchorX");
                    if(Objects.equals(type, "s")) anchorXRent = getConfig().getInt("marker-For_Rent.subzone.icon.anchorX");
                    //anchor Y point
                    int anchorYRent = getConfig().getInt("marker-For_Rent.icon.anchorY");
                    if(Objects.equals(type, "s")) anchorYRent = getConfig().getInt("marker-For_Rent.subzone.icon.anchorY");
                    //points
                    int pointsRent = getConfig().getInt("marker-For_Rent.points", 100);
                    if(Objects.equals(type, "s")) pointsRent = getConfig().getInt("marker-For_Rent.subzone.points", 100);

                    double pointYRent= centY;
                    if(centerPointerRent) pointYRent = yHeightRent;

                    //Add marker to marker set
                    if(Objects.equals(type, "s") && !MarkerSetLabelResidence.equals(MarkerSetLabelSubzones)) {
                        switch (Objects.requireNonNull(getConfig().getString(mtypeRent)).toLowerCase()) {
                            case "point":
                                pointMarker = generatePointMarker(newDetailRent, centX, pointYRent, centZ, iconRent, anchorXRent, anchorYRent);
                                markerSetSubzones.getMarkers().put(AreaResid, pointMarker);
                                break;
                            case "circle":
                                circleExtrudeMarker = generateCircleExtrudeMarker(newDetailRent, LineColorRent, FillColorRent, lineWidthRent, depthTestRent, centX, y, centZ, dist, pointsRent);
                                markerSetSubzones.getMarkers().put(AreaResid, circleExtrudeMarker);
                                break;
                            case "ellipse":
                                ellipseExtrudeMarker = generateEllipseExtrudeMarker(newDetailRent, LineColorRent, FillColorRent, lineWidthRent, depthTestRent, centX, y, centZ, distX, distZ, pointsRent);
                                markerSetSubzones.getMarkers().put(AreaResid, ellipseExtrudeMarker);
                                break;
                            case "rectangle2d":
                                rectangleExtrudeMarker2D = generateRectangleExtrudeMarker2D(newDetailRent, LineColorRent, FillColorRent, lineWidthRent, depthTestRent, x, yHeightRent, z);
                                markerSetSubzones.getMarkers().put(AreaResid, rectangleExtrudeMarker2D);
                                break;
                            case "circle2d":
                                circleExtrudeMarker2D = generateCircleExtrudeMarker2D(newDetailRent, LineColorRent, FillColorRent, lineWidthRent, depthTestRent, centX, yHeightRent, centZ, dist, pointsRent);
                                markerSetSubzones.getMarkers().put(AreaResid, circleExtrudeMarker2D);
                                break;
                            case "ellipse2d":
                                ellipseExtrudeMarker2D = generateEllipseExtrudeMarker2D(newDetailRent, LineColorRent, FillColorRent, lineWidthRent, depthTestRent, centX, yHeightRent, centZ, distX, distZ, pointsRent);
                                markerSetSubzones.getMarkers().put(AreaResid, ellipseExtrudeMarker2D);
                                break;
                            default:
                                rectangleExtrudeMarker = generateRectangleExtrudeMarker(newDetailRent, LineColorRent, FillColorRent, lineWidthRent, depthTestRent, x, y, z);
                                markerSetSubzones.getMarkers().put(AreaResid, rectangleExtrudeMarker);
                                break;
                        }
                    } else {
                        switch (Objects.requireNonNull(getConfig().getString(mtypeRent)).toLowerCase()) {
                            case "point":
                                pointMarker = generatePointMarker(newDetailRent, centX, pointYRent, centZ, iconRent, anchorXRent, anchorYRent);
                                markerSetRes.getMarkers().put(AreaResid, pointMarker);
                                break;
                            case "circle":
                                circleExtrudeMarker = generateCircleExtrudeMarker(newDetailRent, LineColorRent, FillColorRent, lineWidthRent, depthTestRent, centX, y, centZ, dist, pointsRent);
                                markerSetRes.getMarkers().put(AreaResid, circleExtrudeMarker);
                                break;
                            case "ellipse":
                                ellipseExtrudeMarker = generateEllipseExtrudeMarker(newDetailRent, LineColorRent, FillColorRent, lineWidthRent, depthTestRent, centX, y, centZ, distX, distZ, pointsRent);
                                markerSetRes.getMarkers().put(AreaResid, ellipseExtrudeMarker);
                                break;
                            case "rectangle2d":
                                rectangleExtrudeMarker2D = generateRectangleExtrudeMarker2D(newDetailRent, LineColorRent, FillColorRent, lineWidthRent, depthTestRent, x, yHeightRent, z);
                                markerSetRes.getMarkers().put(AreaResid, rectangleExtrudeMarker2D);
                                break;
                            case "circle2d":
                                circleExtrudeMarker2D = generateCircleExtrudeMarker2D(newDetailRent, LineColorRent, FillColorRent, lineWidthRent, depthTestRent, centX, yHeightRent, centZ, dist, pointsRent);
                                markerSetRes.getMarkers().put(AreaResid, circleExtrudeMarker2D);
                                break;
                            case "ellipse2d":
                                ellipseExtrudeMarker2D = generateEllipseExtrudeMarker2D(newDetailRent, LineColorRent, FillColorRent, lineWidthRent, depthTestRent, centX, yHeightRent, centZ, distX, distZ, pointsRent);
                                markerSetRes.getMarkers().put(AreaResid, ellipseExtrudeMarker2D);
                                break;
                            default:
                                rectangleExtrudeMarker = generateRectangleExtrudeMarker(newDetailRent, LineColorRent, FillColorRent, lineWidthRent, depthTestRent, x, y, z);
                                markerSetRes.getMarkers().put(AreaResid, rectangleExtrudeMarker);
                                break;
                        }
                    }
                }

                //**** Normal residence ****\\
                else {
                    String mtype = "marker.type";
                    if(Objects.equals(type, "s")) mtype = "marker.subzone.type";

                    StringBuilder flags = new StringBuilder(); //Define string flags
                    //Load config detail and register placeholders
                    String ConfDetail = getConfig().getString("marker.detail");
                    if(Objects.equals(type, "s")) ConfDetail = getConfig().getString("marker.subzone.detail");
                    assert ConfDetail != null;
                    ConfDetail = ConfDetail.replace("[ResName]", res.getName());
                    ConfDetail = ConfDetail.replace("[OwnerName]", res.getRPlayer().getName());

                    //Register PlaceholderAPI placeholders
                    String newDetail = Placeholder(res.getRPlayer().getPlayer(), ConfDetail);
                    //Add flags to res detail
                    newDetail += flags;

                    int maxFlags = getConfig().getInt("marker.maxFlags", -1);
                    if(Objects.equals(type, "s")) maxFlags = getConfig().getInt("marker.subzone.maxFlags", -1);

                    //Add all use flags to flags string
                    for(Map.Entry<String, Boolean> flag : res.getPermissions().getFlags().entrySet()) {
                        String toFlags = getConfig().getString("marker.flagDetail", "[FlagKey]: [FlagValue]<br>");
                        if(Objects.equals(type, "s")) toFlags = getConfig().getString("marker.subzone.flagDetail", "[FlagKey]: [FlagValue]<br>");
                        toFlags = toFlags.replace("[FlagKey]", flag.getKey());
                        toFlags = toFlags.replace("[FlagValue]", flag.getValue().toString());
                        if(maxFlags != 0 && flagCount < maxFlags) { //If flags enabled and flagCount is right
                            flags.append(toFlags);
                            flagCount++;
                        } else if(maxFlags < 0) { //Unlimited flags
                            flags.append(toFlags);
                        }
                    }

                    //Define Line color
                    Color LineColor = new Color(getConfig().getInt("marker.LineColor.r", 255), getConfig().getInt("marker.LineColor.g",0), getConfig().getInt("marker.LineColor.b",0), (float) getConfig().getDouble("marker.LineColor.a",1.0));
                    if(Objects.equals(type, "s")) LineColor = new Color(getConfig().getInt("marker.subzone.LineColor.r", 255), getConfig().getInt("marker.subzone.LineColor.g",0), getConfig().getInt("marker.subzone.LineColor.b",0), (float) getConfig().getDouble("marker.subzone.LineColor.a",1.0));
                    //Define fill color
                    Color FillColor = new Color(getConfig().getInt("marker.FillColor.r", 200), getConfig().getInt("marker.FillColor.g",0), getConfig().getInt("marker.FillColor.b",0), (float) getConfig().getDouble("marker.FillColor.a",0.3));
                    if(Objects.equals(type, "s")) FillColor = new Color(getConfig().getInt("marker.subzone.FillColor.r", 200), getConfig().getInt("marker.subzone.FillColor.g",0), getConfig().getInt("marker.subzone.FillColor.b",0), (float) getConfig().getDouble("marker.subzone.FillColor.a",0.3));
                    //Define line width
                    int lineWidth = getConfig().getInt("marker.LineWidth", 3);
                    if(Objects.equals(type, "s")) lineWidth = getConfig().getInt("marker.subzone.LineWidth", 3);
                    //Define Y height
                    int yHeight = getConfig().getInt("marker.Yheight", 60);
                    if(Objects.equals(type, "s")) yHeight = getConfig().getInt("marker.subzone.Yheight", 60);
                    //DepthTest
                    boolean depthTest = getConfig().getBoolean("marker-For_Sale.depth-test", true);
                    if(Objects.equals(type, "s")) depthTest = getConfig().getBoolean("marker-For_Sale.subzone.depth-test", true);
                    //centerPointer position
                    boolean centerPointer = getConfig().getBoolean("marker-For_Sale.centerPointerMarkerHeight", true);
                    if(Objects.equals(type, "s")) centerPointer = getConfig().getBoolean("marker-For_Sale.subzone.centerPointerMarkerHeight", true);
                    //Icon
                    String icon = getConfig().getString("marker.icon.url", "https://raw.githubusercontent.com/BlueMap-Minecraft/BlueMap/master/BlueMapCommon/webapp/public/assets/poi.svg");
                    if(Objects.equals(type, "s")) icon = getConfig().getString("marker.subzone.icon.url", "https://raw.githubusercontent.com/BlueMap-Minecraft/BlueMap/master/BlueMapCommon/webapp/public/assets/poi.svg");
                    //anchor point X
                    int anchorX = getConfig().getInt("marker.icon.anchorX");
                    if(Objects.equals(type, "s")) anchorX = getConfig().getInt("marker.subzone.icon.anchorX");
                    //anchor point Y
                    int anchorY = getConfig().getInt("marker.icon.anchorY");
                    if(Objects.equals(type, "s")) anchorY = getConfig().getInt("marker.subzone.icon.anchorY");
                    //points
                    int points = getConfig().getInt("marker.points", 100);
                    if(Objects.equals(type, "s")) points = getConfig().getInt("marker.subzone.points", 100);

                    double pointY = centY;
                    if(centerPointer) pointY = yHeight;

                    //Add marker to marker set
                    if(Objects.equals(type, "s") && !MarkerSetLabelResidence.equals(MarkerSetLabelSubzones)) {
                        switch (Objects.requireNonNull(getConfig().getString(mtype)).toLowerCase()) {
                            case "point":
                                pointMarker = generatePointMarker(newDetail, centX, pointY, centZ, icon, anchorX, anchorY);
                                markerSetSubzones.getMarkers().put(AreaResid, pointMarker);
                                break;
                            case "circle":
                                circleExtrudeMarker = generateCircleExtrudeMarker(newDetail, LineColor, FillColor, lineWidth, depthTest, centX, y, centZ, dist, points);
                                markerSetSubzones.getMarkers().put(AreaResid, circleExtrudeMarker);
                                break;
                            case "ellipse":
                                ellipseExtrudeMarker = generateEllipseExtrudeMarker(newDetail, LineColor, FillColor, lineWidth, depthTest, centX, y, centZ, distX, distZ, points);
                                markerSetSubzones.getMarkers().put(AreaResid, ellipseExtrudeMarker);
                                break;
                            case "rectangle2d":
                                rectangleExtrudeMarker2D = generateRectangleExtrudeMarker2D(newDetail, LineColor, FillColor, lineWidth, depthTest, x, yHeight, z);
                                markerSetSubzones.getMarkers().put(AreaResid, rectangleExtrudeMarker2D);
                                break;
                            case "circle2d":
                                circleExtrudeMarker2D = generateCircleExtrudeMarker2D(newDetail, LineColor, FillColor, lineWidth, depthTest, centX, yHeight, centZ, dist, points);
                                markerSetSubzones.getMarkers().put(AreaResid, circleExtrudeMarker2D);
                                break;
                            case "ellipse2d":
                                ellipseExtrudeMarker2D = generateEllipseExtrudeMarker2D(newDetail, LineColor, FillColor, lineWidth, depthTest, centX, yHeight, centZ, distX, distZ, points);
                                markerSetSubzones.getMarkers().put(AreaResid, ellipseExtrudeMarker2D);
                                break;
                            default:
                                rectangleExtrudeMarker = generateRectangleExtrudeMarker(newDetail, LineColor, FillColor, lineWidth, depthTest, x, y, z);
                                markerSetSubzones.getMarkers().put(AreaResid, rectangleExtrudeMarker);
                                break;
                        }
                    } else {
                        switch (Objects.requireNonNull(getConfig().getString(mtype)).toLowerCase()) {
                            case "point":
                                pointMarker = generatePointMarker(newDetail, centX, pointY, centZ, icon, anchorX, anchorY);
                                markerSetRes.getMarkers().put(AreaResid, pointMarker);
                                break;
                            case "circle":
                                circleExtrudeMarker = generateCircleExtrudeMarker(newDetail, LineColor, FillColor, lineWidth, depthTest, centX, y, centZ, dist, points);
                                markerSetRes.getMarkers().put(AreaResid, circleExtrudeMarker);
                                break;
                            case "ellipse":
                                ellipseExtrudeMarker = generateEllipseExtrudeMarker(newDetail, LineColor, FillColor, lineWidth, depthTest, centX, y, centZ, distX, distZ, points);
                                markerSetRes.getMarkers().put(AreaResid, ellipseExtrudeMarker);
                                break;
                            case "rectangle2d":
                                rectangleExtrudeMarker2D = generateRectangleExtrudeMarker2D(newDetail, LineColor, FillColor, lineWidth, depthTest, x, yHeight, z);
                                markerSetRes.getMarkers().put(AreaResid, rectangleExtrudeMarker2D);
                                break;
                            case "circle2d":
                                circleExtrudeMarker2D = generateCircleExtrudeMarker2D(newDetail, LineColor, FillColor, lineWidth, depthTest, centX, yHeight, centZ, dist, points);
                                markerSetRes.getMarkers().put(AreaResid, circleExtrudeMarker2D);
                                break;
                            case "ellipse2d":
                                ellipseExtrudeMarker2D = generateEllipseExtrudeMarker2D(newDetail, LineColor, FillColor, lineWidth, depthTest, centX, yHeight, centZ, distX, distZ, points);
                                markerSetRes.getMarkers().put(AreaResid, ellipseExtrudeMarker2D);
                                break;
                            default:
                                rectangleExtrudeMarker = generateRectangleExtrudeMarker(newDetail, LineColor, FillColor, lineWidth, depthTest, x, y, z);
                                markerSetRes.getMarkers().put(AreaResid, rectangleExtrudeMarker);
                                break;
                        }
                    }
                }

                //Add subzones marker set to map
                if(Objects.equals(type, "s") && !MarkerSetLabelResidence.equals(MarkerSetLabelSubzones)) map.getMarkerSets(). put(MarkerSetIdSubzones, markerSetSubzones);
                //same for the residences
                else map.getMarkerSets().put(MarkerSetIdResidence, markerSetRes);
            }));
        }
    }

    private ExtrudeMarker generateRectangleExtrudeMarker(String detail, Color line, Color fill, int width, boolean depthTest, double[] x, float[] y, double[] z) {
        return ExtrudeMarker.builder()
            .label(res.getName())
            .detail(detail)
            .shape(Shape.createRect(x[0],z[0],x[1], z[1]),y[0], y[1])
            .centerPosition()
            .lineColor(line)
            .fillColor(fill)
            .lineWidth(width)
            .depthTestEnabled(depthTest)
            .build();
    }

    private ShapeMarker generateRectangleExtrudeMarker2D(String detail, Color line, Color fill, int width, boolean depthTest, double[] x, int yHeight, double[] z) {
        //Create shape rectangle marker
        return ShapeMarker.builder()
            .label(res.getName())
            .detail(detail)
            .shape(Shape.createRect(x[0], z[0], x[1], z[1]),yHeight)
            .centerPosition()
            .lineColor(line)
            .fillColor(fill)
            .lineWidth(width)
            .depthTestEnabled(depthTest)
            .build();
    }

    private POIMarker generatePointMarker(String detail, double centX, double pointY, double centZ, String icon, int anchorX, int anchorY) {
        return POIMarker.builder()
            .label(res.getName())
            .detail(detail)
            .position(centX, pointY, centZ) //calculate center of res
            .icon(icon, anchorX, anchorY)
            .build();
    }

    private ExtrudeMarker generateCircleExtrudeMarker(String detail, Color line, Color fill, int width, boolean depthTest, double centX, float[] y, double centZ, double dist, int points) {
        return ExtrudeMarker.builder()
            .label(res.getName())
            .detail(detail)
            .shape(Shape.createCircle(centX, centZ, dist, points), y[0], y[1])
            .centerPosition()
            .lineColor(line)
            .fillColor(fill)
            .lineWidth(width)
            .depthTestEnabled(depthTest)
            .build();
    }

    private ShapeMarker generateCircleExtrudeMarker2D(String detail, Color line, Color fill, int width, boolean depthTest, double centX, int yHeight, double centZ, double dist, int points) {
        return ShapeMarker.builder()
            .label(res.getName())
            .detail(detail)
            .shape(Shape.createCircle(centX, centZ, dist, points), yHeight)
            .centerPosition()
            .lineColor(line)
            .fillColor(fill)
            .lineWidth(width)
            .depthTestEnabled(depthTest)
            .build();
    }

    private ExtrudeMarker generateEllipseExtrudeMarker(String detail, Color line, Color fill, int width, boolean depthTest, double centX, float[] y, double centZ, double distX, double distZ, int points) {
        return ExtrudeMarker.builder()
                .label(res.getName())
                .detail(detail)
                .shape(Shape.createEllipse(centX, centZ, distX, distZ, points), y[0], y[1])
                .centerPosition()
                .lineColor(line)
                .fillColor(fill)
                .lineWidth(width)
                .depthTestEnabled(depthTest)
                .build();
    }

    private ShapeMarker generateEllipseExtrudeMarker2D(String detail, Color line, Color fill, int width, boolean depthTest, double centX, int yHeight, double centZ, double distX, double distZ, int points) {
        return ShapeMarker.builder()
                .label(res.getName())
                .detail(detail)
                .shape(Shape.createEllipse(centX, centZ, distX, distZ, points), yHeight)
                .centerPosition()
                .lineColor(line)
                .fillColor(fill)
                .lineWidth(width)
                .depthTestEnabled(depthTest)
                .build();
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
    }

}
