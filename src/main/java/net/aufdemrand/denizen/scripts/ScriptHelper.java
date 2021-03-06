package net.aufdemrand.denizen.scripts;

import net.aufdemrand.denizen.events.bukkit.ScriptReloadEvent;
import net.aufdemrand.denizen.scripts.containers.core.ItemScriptHelper;
import net.aufdemrand.denizen.utilities.DenizenAPI;
import net.aufdemrand.denizen.utilities.debugging.dB;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.aufdemrand.denizen.Settings;
import net.aufdemrand.denizen.utilities.Utilities;

public class ScriptHelper {

    /*
     * Reloads and retrieves information from the Denizen/scripts.yml.
     */

    private static YamlConfiguration _yamlScripts = null;

    public static void reloadScripts() {
        String concatenated = _concatenateCoreScripts();
        _yamlScripts = new YamlConfiguration();

        try {
            _yamlScripts.loadFromString(concatenated);
        } catch (InvalidConfigurationException e) {
            dB.echoError("Could not load scripts!");
            dB.echoError(e);
        }

        // Remove all recipes added by Denizen item scripts
        ItemScriptHelper.removeDenizenRecipes();

        ScriptRegistry._buildCoreYamlScriptContainers(getScripts());
        Bukkit.getServer().getPluginManager().callEvent(new ScriptReloadEvent());
    }

    public static FileConfiguration _gs() {
        return getScripts();
    }

    private static FileConfiguration getScripts() {
        if (_yamlScripts == null) {
            reloadScripts();
        }
        return _yamlScripts;
    }


    // Console will be alerted if error was had during reload
    private static boolean hadError = false;

    public static boolean hadError() {
        return hadError;
    }

    public static void resetError() {
        hadError = false;
    }


    private static String _concatenateCoreScripts() {

        try {
            File file = null;
            // Get the script directory
            if (Settings.useDefaultScriptPath())
                file = new File(DenizenAPI.getCurrentInstance()
                        .getDataFolder() + File.separator + "scripts");
            else
                file = new File(Settings.getAlternateScriptPath().replace("/", File.separator));

            // Check if the directory exists
            if(!file.exists()) {
                dB.echoError("No script folder found, please create one.");
                hadError = true;
                return "";
            }


            // Get files using script directory
            List<File> files = Utilities.listDScriptFiles(file, Settings.LoadScriptsInSubfolders());

            if (files.size() > 0) {
                StringBuilder sb = new StringBuilder();

                YamlConfiguration yaml;
                dB.log("Processing 'util.dscript'... ");
                yaml = YamlConfiguration.loadConfiguration(DenizenAPI.getCurrentInstance()
                        .getResource("util.dscript"));
                sb.append(yaml.saveToString() + "\r\n");

                dB.log("Processing outside scripts... ");
                for (FileConfiguration outsideConfig : ScriptRegistry.outside_scripts) {
                    try {
                        sb.append(outsideConfig.saveToString() + "\r\n");
                    } catch (Exception e) {
                        dB.echoError("Woah! Error parsing outside scripts!");
                        hadError = true;
                    }
                }

                for (File f : files){
                    String fileName = f.getAbsolutePath().substring(file.getAbsolutePath().length());
                    dB.log("Processing '" + fileName + "'... ");

                    try {
                        yaml = YamlConfiguration.loadConfiguration(f);
                        String saved = yaml.saveToString();
                        if (yaml != null && saved.length() > 0)
                            sb.append(saved + "\r\n");
                        else {
                            dB.echoError(ChatColor.RED + "Woah! Error parsing " + fileName + "! This script has been skipped. No internal error - is the file empty?");
                            hadError = true;
                        }

                    } catch (RuntimeException e) {
                        dB.echoError(ChatColor.RED + "Woah! Error parsing " + fileName + "!");
                        hadError = true;
                        dB.echoError(e);
                    }
                }

                dB.echoApproval("All scripts loaded!");
                return yamlKeysToUpperCase(sb.toString());
            } else {
                dB.echoError(ChatColor.RED + "Woah! No scripts in /plugins/Denizen/scripts/ to load!");
                hadError = true;
            }

        } catch (Exception e) {
            dB.echoError(ChatColor.RED + "Woah! No script folder found in /plugins/Denizen/scripts/");
            hadError = true;
            dB.echoError(e);
        }

        return "";
    }


    /**
     * Changes YAML 'keys' to all Upper Case to de-sensitize case sensitivity when
     * reading and parsing scripts.
     */

    private static String yamlKeysToUpperCase(String string) {
        StringBuffer sb = new StringBuffer();
        Pattern pattern = Pattern.compile("(^.*?[^\\s](:\\s))", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(string);
        while (matcher.find())
            matcher.appendReplacement(sb, matcher.group().toUpperCase());
        matcher.appendTail(sb);
        return sb.toString();
    }

}
