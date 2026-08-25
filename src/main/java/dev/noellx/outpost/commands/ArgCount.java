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
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.noellx.outpost.OutpostGroupRegion;
import dev.noellx.outpost.OutpostL;
import dev.noellx.outpost.OutpostPlayer;
import dev.noellx.outpost.Outpost;
import dev.noellx.outpost.utils.UUIDCache;

import java.util.*;

public class ArgCount implements NOCommandArg {

    // Only NO regions, not other regions
    static int[] countRegionsOfPlayer(UUID uuid, World w) {
        int[] count = {0, 0}; // total, including merged

        OutpostPlayer psp = OutpostPlayer.fromUUID(uuid);
        psp.getNORegions(w, false).forEach(r -> {
            count[0]++;
            if (r instanceof OutpostGroupRegion) {
                count[1] += ((OutpostGroupRegion) r).getMergedRegions().size();
            }
        });

        return count;
    }

    @Override
    public List<String> getNames() {
        return Collections.singletonList("count");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("Outpost.count", "Outpost.count.others");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    // /no count
    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;
        Bukkit.getScheduler().runTaskAsynchronously(Outpost.getInstance(), () -> {
            int[] count;

            if (args.length == 1) {
                if (!p.hasPermission("Outpost.count")) {
                    OutpostL.msg(p, OutpostL.NO_PERMISSION_COUNT.msg());
                    return;
                }

                count = countRegionsOfPlayer(p.getUniqueId(), p.getWorld());
                OutpostL.msg(p, OutpostL.PERSONAL_REGION_COUNT.msg().replace("%num%", "" + count[0]));
                if (count[1] != 0) {
                    OutpostL.msg(p, OutpostL.PERSONAL_REGION_COUNT_MERGED.msg().replace("%num%", ""+count[1]));
                }

            } else if (args.length == 2) {

                if (!p.hasPermission("Outpost.count.others")) {
                    OutpostL.msg(p, OutpostL.NO_PERMISSION_COUNT_OTHERS.msg());
                    return;
                }
                if (!UUIDCache.containsName(args[1])) {
                    OutpostL.msg(p, OutpostL.PLAYER_NOT_FOUND.msg());
                    return;
                }

                UUID countUuid = UUIDCache.getUUIDFromName(args[1]);
                count = countRegionsOfPlayer(countUuid, p.getWorld());

                OutpostL.msg(p, OutpostL.OTHER_REGION_COUNT.msg()
                        .replace("%player%", UUIDCache.getNameFromUUID(countUuid))
                        .replace("%num%", "" + count[0]));
                if (count[1] != 0) {
                    OutpostL.msg(p, OutpostL.OTHER_REGION_COUNT_MERGED.msg()
                            .replace("%player%", UUIDCache.getNameFromUUID(countUuid))
                            .replace("%num%", "" + count[1]));
                }
            } else {
                OutpostL.msg(p, OutpostL.COUNT_HELP.msg());
            }
        });
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }

}
