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

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.RegionGroupFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import dev.noellx.outpost.*;
import dev.noellx.outpost.utils.UUIDCache;
import dev.noellx.outpost.utils.WGUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class ArgInfo implements NOCommandArg {

    @Override
    public List<String> getNames() {
        return Collections.singletonList("info");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("NullaeOutpost.info");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;
        NORegion r = NORegion.fromLocationGroupUnsafe(p.getLocation());

        if (r == null)
            return NOL.NOT_IN_REGION.send(p);

        if (!p.hasPermission("NullaeOutpost.info.others") && WGUtils.hasNoAccess(r.getWGRegion(), p, WorldGuardPlugin.inst().wrapPlayer(p), true))
            return NOL.NO_ACCESS.send(p);

        if (r.getTypeOptions() == null) {
            NOL.msg(p, ChatColor.RED + "This region is problematic, and the block type (" + r.getType() + ") is not configured. Please contact an administrator.");
            Bukkit.getLogger().info(ChatColor.RED + "This region is problematic, and the block type (" + r.getType() + ") is not configured.");
            return true;
        }

        if (args.length == 1) { // info of current region player is in
            if (!p.hasPermission("NullaeOutpost.info"))
                return NOL.NO_PERMISSION_INFO.send(p);

            NOL.msg(p, NOL.INFO_HEADER.msg());

            // region: %region%, priority: %priority%
            StringBuilder sb = new StringBuilder();

            if (r.getName() == null) {
                NOL.INFO_REGION2.append(sb, r.getId());
            } else {
                NOL.INFO_REGION2.append(sb, r.getName() + " (" + r.getId() + ")");
            }

            if (!NOL.INFO_PRIORITY2.isEmpty()) {
                sb.append(", ").append(NOL.INFO_PRIORITY2.format(r.getWGRegion().getPriority()));
            }
            NOL.msg(p, sb.toString());

            // type: %type%
            if (r instanceof NOGroupRegion) {
                NOL.INFO_TYPE2.send(p, r.getTypeOptions().alias + " " + NOL.INFO_MAY_BE_MERGED.msg());
                displayMerged(p, (NOGroupRegion) r);
            } else {
                NOL.INFO_TYPE2.send(p, r.getTypeOptions().alias);
            }

            displayFlags(p, r);
            displayOwners(p, r.getWGRegion());
            displayMembers(p, r.getWGRegion());

            if (r.getParent() != null) {
                if (r.getName() != null) {
                    NOL.INFO_PARENT2.send(p, r.getParent().getName() + " (" + r.getParent().getId() + ")");
                } else {
                    NOL.INFO_PARENT2.send(p, r.getParent().getId());
                }
            }

            BlockVector3 min = r.getWGRegion().getMinimumPoint();
            BlockVector3 max = r.getWGRegion().getMaximumPoint();
            // only show x,z if it's at block limit
            if (min.getBlockY() == WGUtils.MIN_BUILD_HEIGHT && max.getBlockY() == WGUtils.MAX_BUILD_HEIGHT) {
                NOL.INFO_BOUNDS_XZ.send(p,
                        min.getBlockX(), min.getBlockZ(),
                        max.getBlockX(), max.getBlockZ()
                );
            } else {
                NOL.INFO_BOUNDS_XYZ.send(p,
                        min.getBlockX(), min.getBlockY(), min.getBlockZ(),
                        max.getBlockX(), max.getBlockY(), max.getBlockZ()
                );
            }

        } else if (args.length == 2) { // get specific information on current region

            switch (args[1].toLowerCase()) {
                case "members":
                    if (!p.hasPermission("NullaeOutpost.members"))
                        return NOL.NO_PERMISSION_MEMBERS.send(p);

                    displayMembers(p, r.getWGRegion());
                    break;
                case "owners":
                    if (!p.hasPermission("NullaeOutpost.owners"))
                        return NOL.NO_PERMISSION_OWNERS.send(p);

                    displayOwners(p, r.getWGRegion());
                    break;
                case "flags":
                    if (!p.hasPermission("NullaeOutpost.flags"))
                        return NOL.NO_PERMISSION_FLAGS.send(p);
                        displayFlags(p, r);
                    break;
                default:
                    NOL.INFO_HELP.send(p);
                    break;
            }
        } else {
            NOL.INFO_HELP.send(p);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }

    private static void displayMerged(Player p, NOGroupRegion r) {
        StringBuilder msg = new StringBuilder();
        for (NOMergedRegion pr : r.getMergedRegions()) {
            msg.append(pr.getId()).append(" (").append(pr.getTypeOptions().alias).append("), ");
        }
        NOL.INFO_MERGED2.send(p, msg);
    }

    private static void displayFlags(Player p, NORegion r) {
        ProtectedRegion region = r.getWGRegion();
        NOProtectBlock typeOptions = r.getTypeOptions();

        StringBuilder flagDisp = new StringBuilder();
        String flagValue;
        // loop through all flags
        for (Flag<?> flag : WGUtils.getFlagRegistry().getAll()) {
            if (region.getFlag(flag) != null && !typeOptions.hiddenFlagsFromInfo.contains(flag.getName())) {
                flagValue = region.getFlag(flag).toString();
                RegionGroupFlag groupFlag = flag.getRegionGroupFlag();

                if (region.getFlag(groupFlag) != null) {
                    flagDisp.append(String.format("%s: -g %s %s, ", flag.getName(), region.getFlag(groupFlag), flagValue));
                } else {
                    flagDisp.append(String.format("%s: %s, ", flag.getName(), flagValue));
                }
                flagDisp.append(ChatColor.GRAY);
            }
        }

        if (flagDisp.length() > 2) {
            flagDisp = new StringBuilder(flagDisp.substring(0, flagDisp.length() - 2) + ".");
            NOL.INFO_FLAGS2.send(p, flagDisp);
        } else {
            NOL.INFO_FLAGS2.send(p, NOL.INFO_NO_FLAGS.msg());
        }
    }

    private static void displayOwners(Player p, ProtectedRegion region) {
        DefaultDomain owners = region.getOwners();
        StringBuilder msg = new StringBuilder();
        if (owners.size() == 0) {
            NOL.INFO_NO_OWNERS.append(msg);
        } else {
            for (UUID uuid : owners.getUniqueIds()) {
                String name = UUIDCache.getNameFromUUID(uuid);
                if (name == null) name = Bukkit.getOfflinePlayer(uuid).getName();
                msg.append(name).append(", ");
            }
            for (String name : owners.getPlayers()) { // legacy purposes
                msg.append(name).append(", ");
            }
            msg = new StringBuilder(msg.substring(0, msg.length() - 2));
        }
        NOL.INFO_OWNERS2.send(p, msg);
    }

    private static void displayMembers(Player p, ProtectedRegion region) {
        DefaultDomain members = region.getMembers();
        StringBuilder msg = new StringBuilder();
        if (members.size() == 0) {
            NOL.INFO_NO_MEMBERS.append(msg);
        } else {
            for (UUID uuid : members.getUniqueIds()) {
                String name = UUIDCache.getNameFromUUID(uuid);
                if (name == null) name = uuid.toString();
                msg.append(name).append(", ");
            }
            for (String name : members.getPlayers()) { // legacy purposes
                msg.append(name).append(", ");
            }
            msg = new StringBuilder(msg.substring(0, msg.length() - 2));
        }
        NOL.INFO_MEMBERS2.send(p, msg);
    }
}
