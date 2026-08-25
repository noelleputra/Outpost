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
        return Collections.singletonList("NullaeOutpost.unclaim");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;


        if (!p.hasPermission("NullaeOutpost.unclaim")) {
            NOL.msg(p, NOL.NO_PERMISSION_UNCLAIM.msg());
            return true;
        }

        if (args.length >= 2) { // /no unclaim [list|region-id] (unclaim remote region)

            if (!p.hasPermission("NullaeOutpost.unclaim.remote")) {
                NOL.msg(p, NOL.NO_PERMISSION_UNCLAIM_REMOTE.msg());
                return true;
            }

            NOPlayer psp = NOPlayer.fromPlayer(p);

            // list of regions that the player owns
            List<NORegion> regions = psp.getNORegionsCrossWorld(psp.getPlayer().getWorld(), false);

            if (args[1].equalsIgnoreCase("list")) {
                displayNORegions(s, regions, args.length == 2 ? 0 : tryParseInt(args[2]) - 1);
            } else {
                for (NORegion psr : regions) {
                    if (psr.getId().equalsIgnoreCase(args[1])) {
                        return unclaimBlock(psr, p);
                    }
                }
                NOL.msg(p, NOL.REGION_DOES_NOT_EXIST.msg());
            }

            return true;
        } else { // /no unclaim (no arguments, unclaim current region)
            NORegion r = NORegion.fromLocationGroupUnsafe(p.getLocation()); // allow unclaiming unconfigured regions

            if (r == null) {
                NOL.msg(p, NOL.NOT_IN_REGION.msg());
                return true;
            }

            if (!r.isOwner(p.getUniqueId()) && !p.hasPermission("NullaeOutpost.superowner")) {
                NOL.msg(p, NOL.NO_REGION_PERMISSION.msg());
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

    private void displayNORegions(CommandSender s, List<NORegion> regions, int page) {
        List<TextComponent> entries = new ArrayList<>();
        for (NORegion rs : regions) {
            String msg;
            if (rs.getName() == null) {
                msg = ChatColor.GRAY + "> " + ChatColor.AQUA + rs.getId();
            } else {
                msg = ChatColor.GRAY + "> " + ChatColor.AQUA + rs.getName() + " (" + rs.getId() + ")";
            }
            TextComponent tc = new TextComponent(ChatColor.AQUA + " [-] " + msg);
            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to unclaim " + rs.getId()).create()));
            tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + NullaeOutpost.getInstance().getConfigOptions().base_command + " unclaim " + rs.getId()));
            entries.add(tc);
        }
        TextGUI.displayGUI(s, NOL.UNCLAIM_HEADER.msg(), "/" + NullaeOutpost.getInstance().getConfigOptions().base_command + " unclaim list %page%", page, 17, entries, true);
    }

    private boolean unclaimBlock(NORegion r, Player p) {
        NOProtectBlock cpb = r.getTypeOptions();
        if (cpb != null && !cpb.noDrop) {
            // return protection stone
            List<ItemStack> items = new ArrayList<>();

            if (r instanceof NOGroupRegion) {
                for (NORegion rp : ((NOGroupRegion) r).getMergedRegions()) {
                    if (rp.getTypeOptions() != null) items.add(rp.getTypeOptions().createItem());
                }
            } else {
                items.add(cpb.createItem());
            }

            for (ItemStack item : items) {
                if (!p.getInventory().addItem(item).isEmpty()) {
                    // method will return not empty if item couldn't be added
                    if (NullaeOutpost.getInstance().getConfigOptions().dropItemWhenInventoryFull) {
                        NOL.msg(p, NOL.NO_ROOM_DROPPING_ON_FLOOR.msg());
                        p.getWorld().dropItem(p.getLocation(), item);
                    } else {
                        NOL.msg(p, NOL.NO_ROOM_IN_INVENTORY.msg());
                        return true;
                    }
                }
            }
        }
        // remove region
        // check if removing the region and firing region remove event blocked it
        if (!r.deleteRegion(true, p)) {
            if (!NullaeOutpost.getInstance().getConfigOptions().allowMergingHoles) { // side case if the removing creates a hole and those are prevented
                NOL.msg(p, NOL.DELETE_REGION_PREVENTED_NO_HOLES.msg());
            }
            return true;
        }

        NOL.msg(p, NOL.NO_LONGER_PROTECTED.msg());

        return true;
    }
}
