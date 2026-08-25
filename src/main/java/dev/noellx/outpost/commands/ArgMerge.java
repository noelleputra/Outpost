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

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import dev.noellx.outpost.*;
import dev.noellx.outpost.utils.WGMerge;
import dev.noellx.outpost.utils.WGUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ArgMerge implements NOCommandArg {
    @Override
    public List<String> getNames() {
        return Arrays.asList("merge");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("NullaeOutpost.merge");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    public static List<TextComponent> getGUI(Player p, NORegion r) {
        return r.getMergeableRegions(p).stream()
                .map(psr -> {
                    TextComponent tc = new TextComponent(ChatColor.AQUA + "> " + ChatColor.WHITE + psr.getId());
                    if (psr.getName() != null) tc.addExtra(" (" + psr.getName() + ")"); // name
                    tc.addExtra(" (" + psr.getTypeOptions().alias + ")"); // region type

                    tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + NullaeOutpost.getInstance().getConfigOptions().base_command + " merge " + r.getId() + " " + psr.getId()));
                    tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(NOL.MERGE_CLICK_TO_MERGE.msg().replace("%region%", psr.getId())).create()));
                    return tc;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        if (!s.hasPermission("NullaeOutpost.merge"))
            return NOL.msg(s, NOL.NO_PERMISSION_MERGE.msg());

        if (!NullaeOutpost.getInstance().getConfigOptions().allowMergingRegions)
            return NOL.msg(s, NOL.MERGE_DISABLED.msg());

        Player p = (Player) s;
        if (args.length == 1) { // GUI

            NORegion r = NORegion.fromLocationGroup(p.getLocation());
            if (r == null)
                return NOL.msg(s, NOL.NOT_IN_REGION.msg());

            if (r.getTypeOptions() == null) {
                NOL.msg(p, ChatColor.RED + "This region is problematic, and the block type (" + r.getType() + ") is not configured. Please contact an administrator.");
                Bukkit.getLogger().info(ChatColor.RED + "This region is problematic, and the block type (" + r.getType() + ") is not configured.");
                return true;
            }

            if (!r.getTypeOptions().allowMerging)
                return NOL.msg(s, NOL.MERGE_NOT_ALLOWED.msg());

            List<TextComponent> components = getGUI(p, r);
            if (components.isEmpty()) {
                NOL.msg(p, NOL.MERGE_NO_REGIONS.msg());
            } else {
                p.sendMessage(ChatColor.WHITE + ""); // send empty line
                NOL.msg(p, NOL.MERGE_HEADER.msg().replace("%region%", r.getId()));
                NOL.msg(p, NOL.MERGE_WARNING.msg());
                for (TextComponent tc : components) p.spigot().sendMessage(tc);
                p.sendMessage(ChatColor.WHITE + ""); // send empty line
            }

        } else if (args.length == 3) { // /no merge [region] [root]
            RegionManager rm = WGUtils.getRegionManagerWithPlayer(p);
            ProtectedRegion region = rm.getRegion(args[1]), root = rm.getRegion(args[2]);
            LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(p);

            if (!NullaeOutpost.isNORegion(region) || !NullaeOutpost.isNORegion(root))
                return NOL.msg(p, NOL.MULTI_REGION_DOES_NOT_EXIST.msg());

            if (!p.hasPermission("NullaeOutpost.admin") && (!region.isOwner(lp) || !root.isOwner(lp)))
                return NOL.msg(p, NOL.NO_ACCESS.msg());

            // check if region is actually overlapping the region
            var overlappingRegionIds = WGUtils.findOverlapOrAdjacentRegions(root, rm, p.getWorld()).stream().map(ProtectedRegion::getId).collect(Collectors.toList());
            if (!overlappingRegionIds.contains(region.getId()))
                return NOL.msg(p, NOL.REGION_NOT_OVERLAPPING.msg());

            // check if merging is allowed in config
            NORegion aRegion = NORegion.fromWGRegion(p.getWorld(), region), aRoot = NORegion.fromWGRegion(p.getWorld(), root);
            if (!aRegion.getTypeOptions().allowMerging || !aRoot.getTypeOptions().allowMerging)
                return NOL.msg(p, NOL.MERGE_NOT_ALLOWED.msg());

            // check if the region types allow for it
            if (!WGUtils.canMergeRegionTypes(aRegion.getTypeOptions(), aRoot))
                return NOL.msg(p, NOL.MERGE_NOT_ALLOWED.msg());

            Bukkit.getScheduler().runTaskAsynchronously(NullaeOutpost.getInstance(), () -> {
                try {
                    WGMerge.mergeRealRegions(p.getWorld(), rm, aRoot, Arrays.asList(aRegion, aRoot));
                } catch (WGMerge.RegionHoleException e) {
                    NOL.msg(p, NOL.NO_REGION_HOLES.msg());
                    return;
                }
                NOL.msg(p, NOL.MERGE_MERGED.msg());

                // show menu again if the new region still has overlapping regions
                Bukkit.getScheduler().runTask(NullaeOutpost.getInstance(), () -> {
                    if (!getGUI(p, NORegion.fromWGRegion(p.getWorld(), rm.getRegion(aRoot.getId()))).isEmpty()) {
                        Bukkit.dispatchCommand(p, NullaeOutpost.getInstance().getConfigOptions().base_command + " merge");
                    }
                });
            });

        } else {
            NOL.msg(s, NOL.MERGE_HELP.msg());
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
