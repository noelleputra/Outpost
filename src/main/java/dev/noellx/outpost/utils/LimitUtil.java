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

package dev.noellx.outpost.utils;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import dev.noellx.outpost.*;

import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class LimitUtil {

    // warning: group regions should be split into merged regions first
    public static String checkAddOwner(OutpostPlayer psp, List<OutpostProtectBlock> blocksAdded) {
        HashMap<OutpostProtectBlock, Integer> regionLimits = psp.getRegionLimits();
        int maxNO = psp.getGlobalRegionLimits();

        Outpost.getInstance().debug(String.format("Player's global limit is %d.", maxNO));
        Outpost.getInstance().debug(String.format("Player has limits on %d region types.", regionLimits.size()));

        if (maxNO != -1 || !regionLimits.isEmpty()) { // only check if limit was found

            // count player's protection blocks
            int total = 0;
            HashMap<OutpostProtectBlock, Integer> playerRegionCounts = getOwnedRegionTypeCounts(psp);

            // add the blocks
            for (OutpostProtectBlock b : blocksAdded) {
                Outpost.getInstance().debug(String.format("Adding region type %s.", b.alias));
                if (playerRegionCounts.containsKey(b)) {
                    playerRegionCounts.put(b, playerRegionCounts.get(b)+1);
                } else {
                    playerRegionCounts.put(b, 1);
                }
            }

            // check each limit
            for (OutpostProtectBlock type : playerRegionCounts.keySet()) {
                if (regionLimits.containsKey(type)) {
                    Outpost.getInstance().debug(String.format("Of type %s: player will have %d regions - Player's limit is %d regions.", type.alias, playerRegionCounts.get(type), regionLimits.get(type)));
                    if (playerRegionCounts.get(type) > regionLimits.get(type)) {
                        return OutpostL.ADDREMOVE_PLAYER_REACHED_LIMIT.msg();
                    }
                }
                total += playerRegionCounts.get(type);
            }

            // check if player has passed region limit
            Outpost.getInstance().debug(String.format("The player will have %d regions in total. Their limit is %d.", total, maxNO));
            if (total > maxNO && maxNO != -1) {
                return OutpostL.ADDREMOVE_PLAYER_REACHED_LIMIT.msg();
            }
        }
        return "";
    }

    public static boolean check(Player p, OutpostProtectBlock b) {
        if (!p.hasPermission("Outpost.admin")) {
            // check if player has limit on protection stones
            String msg = LimitUtil.hasPlayerPassedRegionLimit(OutpostPlayer.fromPlayer(p), b);
            if (!msg.isEmpty()) {
                OutpostL.msg(p, msg);
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the region counts of a player (for all worlds).
     * @param psp player
     * @return map of region types to the counts
     */
    private static HashMap<OutpostProtectBlock, Integer> getOwnedRegionTypeCounts(OutpostPlayer psp) {
        if (Outpost.getInstance().isDebug()) { // psp.getName may incur a performance penalty
            Outpost.getInstance().debug(String.format("Debug limits for: %s", psp.getName()));
        }

        HashMap<OutpostProtectBlock, Integer> counts = new HashMap<>();
        HashMap<World, RegionManager> m = WGUtils.getAllRegionManagers();

        for (World w : m.keySet()) {
            psp.getNORegions(w, false).forEach(r -> {
                if (r instanceof OutpostGroupRegion) {
                    Outpost.getInstance().debug(String.format("Checking group region %s's (world %s) (type %s) regions:", r.getId(), w.getName(), r.getTypeOptions().alias));
                    for (OutpostMergedRegion psmr : ((OutpostGroupRegion) r).getMergedRegions()) {
                        if (psmr.getTypeOptions() == null) continue;
                        if (!counts.containsKey(psmr.getTypeOptions())) {
                            counts.put(psmr.getTypeOptions(), 1);
                        } else {
                            counts.put(psmr.getTypeOptions(), counts.get(psmr.getTypeOptions())+1);
                        }

                        Outpost.getInstance().debug(String.format("Merged region %s (world %s) (type %s)", psmr.getId(), w.getName(), psmr.getTypeOptions().alias));
                    }
                } else {
                    if (r.getTypeOptions() == null) return;
                    if (!counts.containsKey(r.getTypeOptions())) {
                        counts.put(r.getTypeOptions(), 1);
                    } else {
                        counts.put(r.getTypeOptions(), counts.get(r.getTypeOptions())+1);
                    }
                    Outpost.getInstance().debug(String.format("Region %s (world %s) (type %s)", r.getId(), w.getName(), r.getTypeOptions().alias));
                }
            });
        }
        return counts;
    }

    private static String hasPlayerPassedRegionLimit(OutpostPlayer psp, OutpostProtectBlock b) {
        HashMap<OutpostProtectBlock, Integer> regionLimits = psp.getRegionLimits();
        int maxNO = psp.getGlobalRegionLimits();

        if (maxNO != -1 || !regionLimits.isEmpty()) { // only check if limit was found

            // count player's protection stones
            int total = 0, bFound = 0;
            HashMap<OutpostProtectBlock, Integer> playerRegionCounts = getOwnedRegionTypeCounts(psp);
            for (OutpostProtectBlock type : playerRegionCounts.keySet()) {
                Outpost.getInstance().debug(String.format("Adding region type %s.", b.alias));
                if (type.equals(b)) {
                    bFound = playerRegionCounts.get(type);
                }
                total += playerRegionCounts.get(type);
            }

            // check if player has passed region limit
            Outpost.getInstance().debug(String.format("The player will have %d regions in total. Their limit is %d.", total, maxNO));
            if (total >= maxNO && maxNO != -1) {
                return OutpostL.REACHED_REGION_LIMIT.msg().replace("%limit%", ""+maxNO);
            }

            // check if player has passed per block limit
            Outpost.getInstance().debug(String.format("Of type %s: player will have %d regions - Player's limit is %d regions.", b.alias, bFound, regionLimits.get(b) == null ? -1 : regionLimits.get(b)));
            if (regionLimits.get(b) != null && bFound >= regionLimits.get(b)) {
                return OutpostL.REACHED_PER_BLOCK_REGION_LIMIT.msg().replace("%limit%", ""+regionLimits.get(b));
            }
        }
        return "";
    }

}
