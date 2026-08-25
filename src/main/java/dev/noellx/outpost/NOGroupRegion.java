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

package dev.noellx.outpost;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a region that exists but is a group of merged {@link NOStandardRegion}s.
 * Contains multiple {@link NOMergedRegion} representing the individual merged regions (which don't technically exist in WorldGuard).
 */

public class NOGroupRegion extends NOStandardRegion {
    NOGroupRegion(ProtectedRegion wgregion, RegionManager rgmanager, World world) {
        super(wgregion, rgmanager, world);
        assert getWGRegion().getFlag(FlagHandler.NO_MERGED_REGIONS) != null;
    }

    @Override
    public boolean hide() {
        for (NOMergedRegion r : getMergedRegions()) r.hide();
        return true;
    }

    @Override
    public boolean unhide() {
        for (NOMergedRegion r : getMergedRegions()) r.unhide();
        return true;
    }

    @Override
    public boolean deleteRegion(boolean deleteBlock, Player cause) {
        List<NOMergedRegion> l = getMergedRegions();
        if (super.deleteRegion(deleteBlock, cause)) {
            for (NOMergedRegion r : l) {
                if (deleteBlock && !r.isHidden()) {
                    r.getProtectBlock().setType(Material.AIR);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the merged region whose ID is the same as the group region ID.
     * @return the root region
     */
    public NOMergedRegion getRootRegion() {
        for (NOMergedRegion r : getMergedRegions()) {
            if (r.getId().equals(getId())) return r;
        }
        return null;
    }

    /**
     * Check if this region contains a specific merged region
     * @param id the psID that would've been generated if the merged region was a standard region
     * @return whether or not the id is a merged region
     */
    public boolean hasMergedRegion(String id) {
        return getWGRegion().getFlag(FlagHandler.NO_MERGED_REGIONS).contains(id);
    }

    /**
     * Removes the merged region's information from the object.
     * Note: This DOES NOT remove the actual NOMergedRegion object, you have to call deleteRegion() on that as well.
     * @param id the id of the merged region
     */
    public void removeMergedRegionInfo(String id) {
        getWGRegion().getFlag(FlagHandler.NO_MERGED_REGIONS).remove(id);

        // remove from no merged region types
        Iterator<String> i = getWGRegion().getFlag(FlagHandler.NO_MERGED_REGIONS_TYPES).iterator();
        while (i.hasNext()) {
            String[] spl = i.next().split(" ");
            String rid = spl[0];
            if (rid.equals(id)) {
                i.remove();
                break;
            }
        }
    }

    /**
     * Get the list of {@link NOMergedRegion} objects of the regions that were merged into this region.
     * @return the list of regions merged into this region
     */
    public List<NOMergedRegion> getMergedRegions() {
        return getMergedRegionsUnsafe().stream()
                .filter(r -> r.getTypeOptions() != null)
                .collect(Collectors.toList());
    }

    /**
     * Get the list of {@link NOMergedRegion} objects of the regions that were merged into this region.
     * Note: This is unsafe as it includes {@link NOMergedRegion}s that are of types not configured in the config.
     * @return the list of regions merged into this region
     */
    public List<NOMergedRegion> getMergedRegionsUnsafe() {
        List<NOMergedRegion> l = new ArrayList<>();
        for (String line : getWGRegion().getFlag(FlagHandler.NO_MERGED_REGIONS_TYPES)) {
            String[] spl = line.split(" ");
            String id = spl[0], type = spl[1];
            l.add(new NOMergedRegion(id, this, getWGRegionManager(), getWorld()));
        }
        return l;
    }
}
