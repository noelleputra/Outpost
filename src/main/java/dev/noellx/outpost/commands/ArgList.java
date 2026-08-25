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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import dev.noellx.outpost.NOL;
import dev.noellx.outpost.NOPlayer;
import dev.noellx.outpost.NORegion;
import dev.noellx.outpost.NullaeOutpost;
import dev.noellx.outpost.utils.UUIDCache;

import java.util.*;
import java.util.stream.Collectors;

public class ArgList implements NOCommandArg {
    @Override
    public List<String> getNames() {
        return Collections.singletonList("list");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("NullaeOutpost.list");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        if (!s.hasPermission("NullaeOutpost.list"))
            return NOL.msg(s, NOL.NO_PERMISSION_LIST.msg());

        if (args.length == 2 && !s.hasPermission("NullaeOutpost.list.others"))
            return NOL.msg(s, NOL.NO_PERMISSION_LIST_OTHERS.msg());

        if (args.length == 2 && !UUIDCache.containsName(args[1]))
            return NOL.msg(s, NOL.PLAYER_NOT_FOUND.msg());

        NOPlayer psp = NOPlayer.fromPlayer((Player) s);

        // run query async to reduce load
        Bukkit.getScheduler().runTaskAsynchronously(NullaeOutpost.getInstance(), () -> {
            if (args.length == 1) {
                List<NORegion> regions = psp.getNORegionsCrossWorld(psp.getPlayer().getWorld(), true);
                display(s, regions, psp.getUuid(), true);
            } else if (args.length == 2) {
                UUID uuid = UUIDCache.getUUIDFromName(args[1]);
                List<NORegion> regions = NOPlayer.fromUUID(uuid).getNORegionsCrossWorld(psp.getPlayer().getWorld(), true);
                display(s, regions, uuid, false);
            } else {
                NOL.msg(s, NOL.LIST_HELP.msg());
            }
        });
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!sender.hasPermission("NullaeOutpost.list") || !sender.hasPermission("NullaeOutpost.list.others")) {
            return null;
        }
        if (args.length == 2) {
            // autocomplete with online player list
            return StringUtil.copyPartialMatches(args[1], Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName).collect(Collectors.toList()), new ArrayList<>());
        }

        return null;
    }

    private void display(CommandSender s, List<NORegion> regions, UUID pUUID, boolean isCurrentPlayer) {
        List<String> ownerOf = new ArrayList<>(), memberOf = new ArrayList<>();
        for (NORegion r : regions) {
            if (r.isOwner(pUUID)) {
                if (r.getName() == null) {
                    ownerOf.add(ChatColor.GRAY + "> " + ChatColor.AQUA + r.getId());
                } else {
                    ownerOf.add(ChatColor.GRAY + "> " + ChatColor.AQUA + r.getName() + " (" + r.getId() + ")");
                }
            }
            if (r.isMember(pUUID)) {
                if (r.getName() == null) {
                    memberOf.add(ChatColor.GRAY + "> " + ChatColor.AQUA + r.getId());
                } else {
                    memberOf.add(ChatColor.GRAY + "> " + ChatColor.AQUA + r.getName() + " (" + r.getId() + ")");
                }
            }
        }

        if (ownerOf.isEmpty() && memberOf.isEmpty()) {
            if (isCurrentPlayer) {
                NOL.msg(s, NOL.LIST_NO_REGIONS.msg());
            } else {
                NOL.msg(s, NOL.LIST_NO_REGIONS_PLAYER.msg().replace("%player%", UUIDCache.getNameFromUUID(pUUID)));
            }
            return;
        }

        NOL.msg(s, NOL.LIST_HEADER.msg().replace("%player%", UUIDCache.getNameFromUUID(pUUID)));

        if (!ownerOf.isEmpty()) {
            NOL.msg(s, NOL.LIST_OWNER.msg());
            for (String str : ownerOf) s.sendMessage(str);
        }
        if (!memberOf.isEmpty()) {
            NOL.msg(s, NOL.LIST_MEMBER.msg());
            for (String str : memberOf) s.sendMessage(str);
        }
    }

}
