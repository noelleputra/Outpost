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
import dev.noellx.outpost.utils.WGUtils;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ArgSethome implements NOCommandArg {

    // /no sethome

    @Override
    public List<String> getNames() {
        return Collections.singletonList("sethome");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("Outpost.sethome");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;
        OutpostRegion r = OutpostRegion.fromLocationGroup(p.getLocation());

        WorldGuardPlugin wg = WorldGuardPlugin.inst();
        if (!p.hasPermission("Outpost.sethome"))
            return OutpostL.msg(p, OutpostL.NO_PERMISSION_SETHOME.msg());

        if (r == null)
            return OutpostL.msg(p, OutpostL.NOT_IN_REGION.msg());

        if (WGUtils.hasNoAccess(r.getWGRegion(), p, wg.wrapPlayer(p), false))
            return OutpostL.msg(p, OutpostL.NO_ACCESS.msg());

        Location l = p.getLocation();
        r.setHome(l.getBlockX(), l.getBlockY(), l.getBlockZ(), l.getYaw(), l.getPitch());
        return OutpostL.msg(p, OutpostL.SETHOME_SET.msg().replace("%psid%", r.getName() != null ? String.format("%s (%s)", r.getName(), r.getId()) : r.getId()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
