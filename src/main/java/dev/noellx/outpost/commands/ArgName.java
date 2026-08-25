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

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import dev.noellx.outpost.OutpostL;
import dev.noellx.outpost.OutpostRegion;
import dev.noellx.outpost.Outpost;
import dev.noellx.outpost.utils.WGUtils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ArgName implements NOCommandArg {
    @Override
    public List<String> getNames() {
        return Collections.singletonList("name");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("NullaeOutpost.name");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        if (!s.hasPermission("NullaeOutpost.name")) {
            OutpostL.msg(s, OutpostL.NO_PERMISSION_NAME.msg());
            return true;
        }
        Player p = (Player) s;
        OutpostRegion r = OutpostRegion.fromLocationGroup(p.getLocation());
        if (r == null) {
            OutpostL.msg(s, OutpostL.NOT_IN_REGION.msg());
            return true;
        }
        if (WGUtils.hasNoAccess(r.getWGRegion(), p, WorldGuardPlugin.inst().wrapPlayer(p), false)) {
            OutpostL.msg(s, OutpostL.NO_ACCESS.msg());
            return true;
        }
        if (args.length < 2) {
            OutpostL.msg(s, OutpostL.NAME_HELP.msg());
            return true;
        }

        if (args[1].equals("none")) {
            r.setName(null);
            OutpostL.msg(p, OutpostL.NAME_REMOVED.msg().replace("%id%", r.getId()));
        } else {
            if (!Outpost.getInstance().getConfigOptions().allowDuplicateRegionNames && Outpost.isNONameAlreadyUsed(args[1])) {
                OutpostL.msg(p, OutpostL.NAME_TAKEN.msg().replace("%name%", args[1]));
                return true;
            }
            r.setName(args[1]);
            OutpostL.msg(p, OutpostL.NAME_SET_NAME.msg().replace("%id%", r.getId()).replace("%name%", r.getName()));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }

}

