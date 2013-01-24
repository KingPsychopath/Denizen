package net.aufdemrand.denizen.npc.actions;

import net.aufdemrand.denizen.Denizen;
import net.aufdemrand.denizen.npc.DenizenNPC;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizen.utilities.debugging.dB.DebugElement;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class ActionHandler {

    Denizen denizen;
    
    public ActionHandler(Denizen denizen) {
        this.denizen = denizen;
    }
    
    public void doAction(String actionName, DenizenNPC npc, Player player, String assignment) {
        dB.echoDebug(ChatColor.YELLOW + "ACTION! "
                + ChatColor.DARK_GRAY + "NAME='" + ChatColor.AQUA + actionName + ChatColor.DARK_GRAY + "', "
                + ChatColor.DARK_GRAY + "NPC='" + ChatColor.AQUA + npc.getName() + "/" + npc.getId() + ChatColor.DARK_GRAY + "', "
                + ChatColor.DARK_GRAY + "ASSIGNMENT='" + ChatColor.AQUA + assignment + ChatColor.DARK_GRAY
                + ChatColor.DARK_GRAY + (player != null ? ", PLAYER='" + ChatColor.AQUA + player.getName() + ChatColor.DARK_GRAY + "', " : "'"));

        // Fetch script from Actions
        List<String> script = denizen.getScriptEngine().getScriptHelper().getStringListIgnoreCase(assignment + ".actions.on " + actionName);
        if (script.isEmpty()) return;
        
        dB.echoDebug(DebugElement.Header, "Building action 'On " + actionName.toUpperCase() + "' for " + npc.toString());
        
        // Build script entries
        List<ScriptEntry> scriptEntries = denizen.getScriptEngine().getScriptBuilder().buildScriptEntries(player, npc, script, null, null);

        // Execute scriptEntries
        for (ScriptEntry scriptEntry : scriptEntries)
           denizen.getScriptEngine().getScriptExecuter().execute(scriptEntry);
        
    }
    
}
