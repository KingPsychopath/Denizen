package net.aufdemrand.denizen.scripts.commands.world;

import net.aufdemrand.denizen.objects.*;
import net.aufdemrand.denizen.utilities.Utilities;
import org.bukkit.Effect;
import org.bukkit.Material;

import net.aufdemrand.denizen.exceptions.CommandExecutionException;
import net.aufdemrand.denizen.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.aufdemrand.denizen.scripts.commands.AbstractCommand;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizen.utilities.ParticleEffect;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lets you play a Bukkit effect or a ParticleEffect from the
 * ParticleEffect Library at a certain location.
 *
 * Arguments: [] - Required, () - Optional
 * [location:<x,y,z,world>] specifies location of the effect
 * [effect:<name>] sets the name of effect to be played
 * (data:<#>) sets the special data value of the effect
 * (visibility:<#>) adjusts the radius within which players can observe the effect
 * (qty:<#>) sets the number of times the effect will be played
 * (offset:<#>) sets the offset of ParticleEffects.
 *
 * Example Usage:
 * playeffect location:123,65,765,world effect:record_play data:2259 radius:7
 * playeffect location:<npc.location> e:smoke r:3
 * playeffect location:<npc.location> effect:heart radius:7 qty:1000 offset:20
 *
 * @author David Cernat
 */

public class PlayEffectCommand extends AbstractCommand {

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        // Iterate through arguments
        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

            if (!scriptEntry.hasObject("location")
                    && arg.matchesArgumentList(dLocation.class)) {

                scriptEntry.addObject("location", arg.asType(dList.class).filter(dLocation.class));
            }

            else if (!scriptEntry.hasObject("effect") &&
                    !scriptEntry.hasObject("particleeffect") &&
                    !scriptEntry.hasObject("iconcrack")) {

                if (arg.matchesEnum(ParticleEffect.values())) {
                    scriptEntry.addObject("particleeffect",
                            ParticleEffect.valueOf(arg.getValue().toUpperCase()));
                }
                else if (arg.matches("random")) {
                    // Get another effect if "RANDOM" is used
                    ParticleEffect effect = null;
                    // Make sure the new effect is not an invisible effect
                    while (effect == null || effect.toString().matches("^(BUBBLE|SUSPEND|DEPTH_SUSPEND)$")) {
                        effect = ParticleEffect.values()[Utilities.getRandom().nextInt(ParticleEffect.values().length)];
                    }
                    scriptEntry.addObject("particleeffect", effect);
                }
                else if (arg.startsWith("iconcrack_")) {
                    // Allow iconcrack_[id] for item break effects (ex: iconcrack_1)
                    Element typeId = new Element(arg.getValue().substring(10));
                    if (typeId.isInt() && typeId.asInt() > 0 && Material.getMaterial(typeId.asInt()) != null)
                        scriptEntry.addObject("iconcrack", typeId);
                    else
                        dB.echoError("Invalid iconcrack_[id]. Must be a valid Material ID, besides 0.");
                }
                else if (arg.matchesEnum(Effect.values())) {
                    scriptEntry.addObject("effect", Effect.valueOf(arg.getValue().toUpperCase()));
                }
            }

            else if (!scriptEntry.hasObject("radius")
                    && arg.matchesPrimitive(aH.PrimitiveType.Double)
                    && arg.matchesPrefix("visibility, v, radius, r")) {

                scriptEntry.addObject("radius", arg.asElement());
            }

            else if (!scriptEntry.hasObject("data")
                    && arg.matchesPrimitive(aH.PrimitiveType.Double)
                    && arg.matchesPrefix("data, d")) {

                scriptEntry.addObject("data", arg.asElement());
            }

            else if (!scriptEntry.hasObject("qty")
                    && arg.matchesPrimitive(aH.PrimitiveType.Integer)
                    && arg.matchesPrefix("qty, q")) {

                scriptEntry.addObject("qty", arg.asElement());
            }

            else if (!scriptEntry.hasObject("offset")
                    && arg.matchesPrimitive(aH.PrimitiveType.Double)
                    && arg.matchesPrefix("offset, o")) {

                scriptEntry.addObject("offset", arg.asElement());
            }

            else if (!scriptEntry.hasObject("targets")
                && arg.matchesArgumentList(dPlayer.class)
                && arg.matchesPrefix("targets, target, t")) {

                scriptEntry.addObject("targets", arg.asType(dList.class).filter(dPlayer.class));
            }

            else
                arg.reportUnhandled();
        }

        // Use default values if necessary
        scriptEntry.defaultObject("location",
                scriptEntry.hasNPC() ? Arrays.asList(scriptEntry.getNPC().getLocation()) : null,
                scriptEntry.hasPlayer() ? Arrays.asList(scriptEntry.getPlayer().getLocation()): null);
        scriptEntry.defaultObject("data", new Element(0));
        scriptEntry.defaultObject("radius", new Element(15));
        scriptEntry.defaultObject("qty", new Element(1));
        scriptEntry.defaultObject("offset", new Element(0.5));

        // Check to make sure required arguments have been filled

        if (!scriptEntry.hasObject("effect") &&
                !scriptEntry.hasObject("particleeffect") &&
                !scriptEntry.hasObject("iconcrack"))
            throw new InvalidArgumentsException("Missing effect argument!");

        if (!scriptEntry.hasObject("location"))
            throw new InvalidArgumentsException("Missing location argument!");
    }

    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        // Extract objects from ScriptEntry
        List<dLocation> locations = (List<dLocation>) scriptEntry.getObject("location");
        List<dPlayer> targets = (List<dPlayer>) scriptEntry.getObject("targets");
        Effect effect = (Effect) scriptEntry.getObject("effect");
        ParticleEffect particleEffect = (ParticleEffect) scriptEntry.getObject("particleeffect");
        Element iconcrack = scriptEntry.getElement("iconcrack");
        Element radius = scriptEntry.getElement("radius");
        Element data = scriptEntry.getElement("data");
        Element qty = scriptEntry.getElement("qty");
        Element offset = scriptEntry.getElement("offset");

        // Report to dB
        dB.report(scriptEntry, getName(), (effect != null ? aH.debugObj("effect", effect.name()) :
                particleEffect != null ? aH.debugObj("special effect", particleEffect.name()) :
                iconcrack.debug()) +
                aH.debugObj("locations", locations.toString()) +
                (targets != null ? aH.debugObj("targets", targets.toString()): "") +
                radius.debug() +
                data.debug() +
                qty.debug() +
                (effect != null ? "" : offset.debug()));

        for (dLocation location: locations) {
            // Slightly increase the location's Y so effects don't seem to come out of the ground
            location.add(0, 1, 0);

            // Play the Bukkit effect the number of times specified
            if (effect != null) {
                for (int n = 0; n < qty.asInt(); n++) {
                    if (targets != null) {
                        for (dPlayer player: targets)
                            if (player.isValid() && player.isOnline()) player.getPlayerEntity().playEffect(location, effect, data.asInt());
                    }
                    else {
                        location.getWorld().playEffect(location, effect, data.asInt(), radius.asInt());
                    }
                }
            }

            // Play a ParticleEffect
            else if (particleEffect != null) {
                float os = offset.asFloat();
                List<Player> players = new ArrayList<Player>();
                if (targets == null)
                    players = ParticleEffect.getPlayersInRange(location, radius.asDouble());
                else {
                    for (dPlayer player: targets)
                        if (player.isValid() && player.isOnline()) players.add(player.getPlayerEntity());
                }
                particleEffect.display(location, os, os, os, data.asFloat(), qty.asInt(), players);
            }

            // Play an iconcrack (item break) effect
            else {
                float os = offset.asFloat();
                List<Player> players = new ArrayList<Player>();
                if (targets == null)
                    players = ParticleEffect.getPlayersInRange(location, radius.asDouble());
                else {
                    for (dPlayer player: targets)
                        if (player.isValid() && player.isOnline()) players.add(player.getPlayerEntity());
                }
                ParticleEffect.displayIconCrack(location, iconcrack.asInt(),
                        os, os, os, data.asFloat(), qty.asInt(), players);
            }
        }
    }
}
