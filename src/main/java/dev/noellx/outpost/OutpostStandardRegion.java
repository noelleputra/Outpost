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

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.RemovalStrategy;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import dev.noellx.outpost.event.OutpostRemoveEvent;
import dev.noellx.outpost.utils.WGUtils;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents an instance of a standard NO region, that has not been merged or contains merged regions.
 */

public class OutpostStandardRegion extends OutpostRegion {
    private ProtectedRegion wgregion;

    OutpostStandardRegion(ProtectedRegion wgregion, RegionManager rgmanager, World world) {
        super(rgmanager, world);
        this.wgregion = checkNotNull(wgregion);
    }

    // ~~~~~~~~~~~ instance ~~~~~~~~~~~~~~~~

    @Override
    public String getId() {
        return wgregion.getId();
    }

    @Override
    public String getName() {
        return wgregion.getFlag(FlagHandler.NO_NAME);
    }

    @Override
    public void setName(String name) {
        HashMap<String, ArrayList<String>> m = Outpost.regionNameToID.get(getWorld().getUID());
        if (m == null) { // if the world has not been added
            Outpost.regionNameToID.put(getWorld().getUID(), new HashMap<>());
            m = Outpost.regionNameToID.get(getWorld().getUID());
        }
        if (m.get(getName()) != null) {
            m.get(getName()).remove(getId());
        }
        if (name != null) {
            if (m.containsKey(name)) {
                m.get(name).add(getId());
            } else {
                m.put(name, new ArrayList<>(Collections.singletonList(getId())));
            }
        }
        wgregion.setFlag(FlagHandler.NO_NAME, name);
    }

    @Override
    public void setParent(OutpostRegion r) throws ProtectedRegion.CircularInheritanceException {
        wgregion.setParent(r == null ? null : r.getWGRegion());
    }

    @Override
    public OutpostRegion getParent() {
        return wgregion.getParent() == null ? null : fromWGRegion(world, wgregion.getParent());
    }

    @Override
    public Location getHome() {
        String oPos = wgregion.getFlag(FlagHandler.NO_HOME);
        if (oPos == null) return null;
        String[] pos = oPos.split(" ");
        double x = Double.parseDouble(pos[0]), y = Double.parseDouble(pos[1]), z = Double.parseDouble(pos[2]);
        float yaw = pos.length >= 4 ? Float.parseFloat(pos[3]) : 0, pitch = pos.length >= 4 ? Float.parseFloat(pos[4]) : 0;
        return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public void setHome(double blockX, double blockY, double blockZ) {
        wgregion.setFlag(FlagHandler.NO_HOME, blockX + " " + blockY + " " + blockZ);
    }

    @Override
    public void setHome(double blockX, double blockY, double blockZ, float yaw, float pitch) {
        wgregion.setFlag(FlagHandler.NO_HOME, blockX + " " + blockY + " " + blockZ + " " + yaw + " " + pitch);
    }

    @Override
    public Block getProtectBlock() {
        OutspotLocation psl = WGUtils.parseNORegionToLocation(wgregion.getId());
        return world.getBlockAt(psl.x, psl.y, psl.z);
    }

    @Override
    public OutpostProtectBlock getTypeOptions() {
        return Outpost.getBlockOptions(getType());
    }

    @Override
    public String getType() {
        return wgregion.getFlag(FlagHandler.NO_BLOCK_MATERIAL);
    }

    @Override
    public void setType(OutpostProtectBlock type) {
        super.setType(type);
        getWGRegion().setFlag(FlagHandler.NO_BLOCK_MATERIAL, type.type);
    }

    @Override
    public boolean isOwner(UUID uuid) {
        return wgregion.getOwners().contains(uuid);
    }

    @Override
    public boolean isMember(UUID uuid) {
        return wgregion.getMembers().contains(uuid);
    }

    @Override
    public ArrayList<UUID> getOwners() {
        return new ArrayList<>(wgregion.getOwners().getUniqueIds());
    }

    @Override
    public ArrayList<UUID> getMembers() {
        return new ArrayList<>(wgregion.getMembers().getUniqueIds());
    }

    @Override
    public void addOwner(UUID uuid) {
        if (uuid == null) return;
        wgregion.getOwners().addPlayer(uuid);
    }

    @Override
    public void addMember(UUID uuid) {
        if (uuid == null) return;
        wgregion.getMembers().addPlayer(uuid);
    }

    @Override
    public void removeOwner(UUID uuid) {
        if (uuid == null) return;
        if (wgregion.getOwners().contains(uuid))
            wgregion.getOwners().removePlayer(uuid);
    }

    @Override
    public void removeMember(UUID uuid) {
        if (uuid == null) return;
        if (wgregion.getMembers().contains(uuid))
            wgregion.getMembers().removePlayer(uuid);
    }

    @Override
    public List<BlockVector2> getPoints() {
        return wgregion.getPoints();
    }

    @Override
    public List<OutpostRegion> getMergeableRegions(Player p) {
        return WGUtils.findOverlapOrAdjacentRegions(getWGRegion(), getWGRegionManager(), getWorld())
                .stream()
                .map(r -> OutpostRegion.fromWGRegion(getWorld(), r))
                .filter(r -> r != null && r.getTypeOptions() != null && !r.getId().equals(getId()))
                .filter(r -> r.getTypeOptions().allowMerging)
                .filter(r -> r.isOwner(p.getUniqueId()) || p.hasPermission("NullaeOutpost.admin"))
                .filter(r -> WGUtils.canMergeRegionTypes(getTypeOptions(), r))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteRegion(boolean deleteBlock) {
        return deleteRegion(deleteBlock, null);
    }

    @Override
    public boolean deleteRegion(boolean deleteBlock, Player cause) {
        OutpostRemoveEvent event = new OutpostRemoveEvent(this, cause);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) { // if event was cancelled, prevent execution
            return false;
        }

        // set the physical block to air
        if (deleteBlock && !this.isHidden()) {
            this.getProtectBlock().setType(Material.AIR);
        }

        // remove name from cache
        if (getName() != null) {
            HashMap<String, ArrayList<String>> rIds = Outpost.regionNameToID.get(getWorld().getUID());
            if (rIds != null && rIds.containsKey(getName())) {
                if (rIds.get(getName()).size() == 1) {
                    rIds.remove(getName());
                } else {
                    rIds.get(getName()).remove(getId());
                }
            }
        }

        // remove region from WorldGuard
        // specify UNSET_PARENT_IN_CHILDREN removal strategy so that region children don't get deleted
        rgmanager.removeRegion(wgregion.getId(), RemovalStrategy.UNSET_PARENT_IN_CHILDREN);

        return true;
    }

    @Override
    public ProtectedRegion getWGRegion() {
        return wgregion;
    }
}
