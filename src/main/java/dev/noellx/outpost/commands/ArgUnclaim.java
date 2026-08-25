/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.noellx.outpost.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dev.noellx.outpost.*;
import dev.noellx.outpost.utils.TextGUI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ArgUnclaim implements NOCommandArg {

    // /no unclaim

    @Override
    public List<String> getNames() {
        return Collections.singletonList("unclaim");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Collections.singletonList("Outpost.unclaim");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;


        if (!p.hasPermission("Outpost.unclaim")) {
            OutpostL.msg(p, OutpostL.NO_PERMISSION_UNCLAIM.msg());
            return true;
        }

        if (args.length >= 2) { // /no unclaim [list|region-id] (unclaim remote region)

            if (!p.hasPermission("Outpost.unclaim.remote")) {
                OutpostL.msg(p, OutpostL.NO_PERMISSION_UNCLAIM_REMOTE.msg());
                return true;
            }

            OutpostPlayer psp = OutpostPlayer.fromPlayer(p);

            // list of regions that the player owns
            List<OutpostRegion> regions = psp.getNORegionsCrossWorld(psp.getPlayer().getWorld(), false);

            if (args[1].equalsIgnoreCase("list")) {
                displayNORegions(s, regions, args.length == 2 ? 0 : tryParseInt(args[2]) - 1);
            } else {
                for (OutpostRegion psr : regions) {
                    if (psr.getId().equalsIgnoreCase(args[1])) {
                        return unclaimBlock(psr, p);
                    }
                }
                OutpostL.msg(p, OutpostL.REGION_DOES_NOT_EXIST.msg());
            }

            return true;
        } else { // /no unclaim (no arguments, unclaim current region)
            OutpostRegion r = OutpostRegion.fromLocationGroupUnsafe(p.getLocation()); // allow unclaiming unconfigured regions

            if (r == null) {
                OutpostL.msg(p, OutpostL.NOT_IN_REGION.msg());
                return true;
            }

            if (!r.isOwner(p.getUniqueId()) && !p.hasPermission("Outpost.superowner")) {
                OutpostL.msg(p, OutpostL.NO_REGION_PERMISSION.msg());
                return true;
            }

            return unclaimBlock(r, p);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }

    private int tryParseInt(String arg) {
        int i = 1;
        try {
            i = Integer.parseInt(arg);
        } catch (NumberFormatException ignore) {
            //ignore
        }
        return i;
    }

    private void displayNORegions(CommandSender s, List<OutpostRegion> regions, int page) {
        List<TextComponent> entries = new ArrayList<>();
        for (OutpostRegion rs : regions) {
            String msg;
            if (rs.getName() == null) {
                msg = ChatColor.GRAY + "> " + ChatColor.AQUA + rs.getId();
            } else {
                msg = ChatColor.GRAY + "> " + ChatColor.AQUA + rs.getName() + " (" + rs.getId() + ")";
            }
            TextComponent tc = new TextComponent(ChatColor.AQUA + " [-] " + msg);
            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to unclaim " + rs.getId()).create()));
            tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + Outpost.getInstance().getConfigOptions().base_command + " unclaim " + rs.getId()));
            entries.add(tc);
        }
        TextGUI.displayGUI(s, OutpostL.UNCLAIM_HEADER.msg(), "/" + Outpost.getInstance().getConfigOptions().base_command + " unclaim list %page%", page, 17, entries, true);
    }

    private boolean unclaimBlock(OutpostRegion r, Player p) {
        OutpostProtectBlock cpb = r.getTypeOptions();
        if (cpb != null && !cpb.noDrop) {
            // return protection stone
            List<ItemStack> items = new ArrayList<>();

            if (r instanceof OutpostGroupRegion) {
                for (OutpostRegion rp : ((OutpostGroupRegion) r).getMergedRegions()) {
                    if (rp.getTypeOptions() != null) items.add(rp.getTypeOptions().createItem());
                }
            } else {
                items.add(cpb.createItem());
            }

            for (ItemStack item : items) {
                if (!p.getInventory().addItem(item).isEmpty()) {
                    // method will return not empty if item couldn't be added
                    if (Outpost.getInstance().getConfigOptions().dropItemWhenInventoryFull) {
                        OutpostL.msg(p, OutpostL.NO_ROOM_DROPPING_ON_FLOOR.msg());
                        p.getWorld().dropItem(p.getLocation(), item);
                    } else {
                        OutpostL.msg(p, OutpostL.NO_ROOM_IN_INVENTORY.msg());
                        return true;
                    }
                }
            }
        }
        // remove region
        // check if removing the region and firing region remove event blocked it
        if (!r.deleteRegion(true, p)) {
            if (!Outpost.getInstance().getConfigOptions().allowMergingHoles) { // side case if the removing creates a hole and those are prevented
                OutpostL.msg(p, OutpostL.DELETE_REGION_PREVENTED_NO_HOLES.msg());
            }
            return true;
        }

        OutpostL.msg(p, OutpostL.NO_LONGER_PROTECTED.msg());

        return true;
    }
}
