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
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import dev.noellx.outpost.utils.BlockUtil;
import dev.noellx.outpost.utils.WGUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents an instance of a NullaeOutpost protected region.
 */

public abstract class OutpostRegion {
    RegionManager rgmanager;
    World world;

    OutpostRegion(RegionManager rgmanager, World world) {
        this.rgmanager = checkNotNull(rgmanager);
        this.world = checkNotNull(world);
    }

    // ~~~~~~~~~~~~~~~~~ static ~~~~~~~~~~~~~~~~~

    /**
     * Get the protection stone region that the location is in, or the closest one if there are overlapping regions.
     * Returns either {@link OutpostGroupRegion}, {@link OutpostStandardRegion} or {@link OutpostMergedRegion}.
     *
     * @param l the location
     * @return the {@link OutpostRegion} object if the location is in a region, or null if the location is not in a region
     */
    public static OutpostRegion fromLocation(Location l) {
        OutpostRegion r = fromLocationUnsafe(l);
        return r == null || r.getTypeOptions() == null ? null : r;
    }

    /**
     * Get the protection stone region that the location is in, or the closest one if there are overlapping regions.
     * May return a region with an unconfigured block type (getTypeOptions returns null).
     * Returns either {@link OutpostGroupRegion}, {@link OutpostStandardRegion} or {@link OutpostMergedRegion}.
     *
     * @param l the location
     * @return the {@link OutpostRegion} object if the location is in a region, or null if the location is not in a region
     */
    public static OutpostRegion fromLocationUnsafe(Location l) {
        checkNotNull(checkNotNull(l).getWorld());
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(l.getWorld());
        if (rgm == null) return null;

        // check exact location first for merged region block
        OutpostMergedRegion pr = OutpostMergedRegion.getMergedRegion(l);
        if (pr != null) return pr;

        return fromLocationGroupUnsafe(l);
    }

    /**
     * Get the protection stone parent region that the location is in.
     * Returns either {@link OutpostGroupRegion} or {@link OutpostStandardRegion}.
     *
     * @param l the location
     * @return the {@link OutpostRegion} object if the location is in a region, or null if the location is not in a region
     */
    public static OutpostRegion fromLocationGroup(Location l) {
        OutpostRegion r = fromLocationGroupUnsafe(l);
        return r == null || r.getTypeOptions() == null ? null : r;
    }

    /**
     * Get the protection stone parent region that the location is in.
     * May return a region with an unconfigured block type (getTypeOptions returns null).
     * Returns either {@link OutpostGroupRegion} or {@link OutpostStandardRegion}.
     *
     * @param l the location
     * @return the {@link OutpostRegion} object if the location is in a region, or null if the location is not in a region
     */
    public static OutpostRegion fromLocationGroupUnsafe(Location l) {
        checkNotNull(checkNotNull(l).getWorld());
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(l.getWorld());
        if (rgm == null) return null;

        // check if location is in a region
        String psID = WGUtils.matchLocationToNOID(l);
        ProtectedRegion r = rgm.getRegion(psID);

        if (r == null) {
            return null;
        } else if (r.getFlag(FlagHandler.NO_MERGED_REGIONS) != null) {
            return new OutpostGroupRegion(r, rgm, l.getWorld());
        } else {
            return new OutpostStandardRegion(r, rgm, l.getWorld());
        }
    }

    /**
     * Get the protection stone region with the world and region.
     * It returns a WGRegion with a null type if the block type isn't configured in the config.
     *
     * @param w the world
     * @param r the WorldGuard region
     * @return the {@link OutpostRegion} based on the parameters, or null if the region given is not a NullaeOutpost region
     */
    public static OutpostRegion fromWGRegion(World w, ProtectedRegion r) {
        if (!Outpost.isNORegionFormat(r)) return null;
        if (r.getFlag(FlagHandler.NO_MERGED_REGIONS) != null) {
            return new OutpostGroupRegion(r, WGUtils.getRegionManagerWithWorld(checkNotNull(w)), w);
        } else {
            return new OutpostStandardRegion(r, WGUtils.getRegionManagerWithWorld(checkNotNull(w)), w);
        }
    }

    /**
     * Get the protection stones regions that have the given name as their set nickname (/no name)
     *
     * @param w    the world to look for regions in
     * @param name the nickname of the region
     * @return the list of regions that have that name
     */

    public static List<OutpostRegion> fromName(World w, String name) {
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(w);
        if (rgm == null) return new ArrayList<>();

        List<OutpostRegion> l = new ArrayList<>();

        List<String> rIds = Outpost.regionNameToID.get(w.getUID()).get(name);
        if (rIds == null) return l;

        for (int i = 0; i < rIds.size(); i++) {
            String id = rIds.get(i);
            if (rgm.getRegion(id) == null) { // cleanup cache
                rIds.remove(i);
                i--;
            } else {
                l.add(fromWGRegion(w, rgm.getRegion(id)));
            }
        }
        return l;
    }

    /**
     * Get the protection stones regions that have the given name as their set nickname (/no name), from all worlds.
     *
     * @param name the nickname of the regions
     * @return the map of worlds, to the regions that have the name
     */

    public static HashMap<World, List<OutpostRegion>> fromName(String name) {
        HashMap<World, List<OutpostRegion>> regions = new HashMap<>();
        for (UUID worldUid : Outpost.regionNameToID.keySet()) {
            World w = Bukkit.getWorld(worldUid);
            regions.put(w, fromName(w, name));
        }
        return regions;
    }

    // ~~~~~~~~~~~ instance ~~~~~~~~~~~~~~~~

    /**
     * @return gets the world that the region is in
     */
    public World getWorld() {
        return world;
    }

    @Deprecated
    public String getID() {
        return getId();
    }

    /**
     * Get the WorldGuard ID of the region. Note that this is not guaranteed to be unique between worlds.
     * @return the id of the region
     */
    public abstract String getId();

    /**
     * Get the name (nickname) of the region from /no name.
     * @return the name of the region, or null if the region does not have a name
     */

    public abstract String getName();

    /**
     * Set the name of the region (from /no name).
     * @param name new name, or null to remove the name
     */

    public abstract void setName(String name);

    /**
     * Set the parent of this region.
     * @param r the region to be the parent, or null for no parent
     * @throws ProtectedRegion.CircularInheritanceException thrown when the parent already inherits from the child
     */

    public abstract void setParent(OutpostRegion r) throws ProtectedRegion.CircularInheritanceException;

    /**
     * Get the parent of this region, if there is one.
     * @return the parent of the region, or null if there isn't one
     */

    public abstract OutpostRegion getParent();

    /**
     * Get the location of the set home the region has (for /no tp).
     * @return the location of the home, or null if the ps_home flag is not set.
     */
    public abstract Location getHome();

    /**
     * Set the home of the region (internally changes the flag).
     * @param blockX block x location
     * @param blockY block y location
     * @param blockZ block z location
     */
    public abstract void setHome(double blockX, double blockY, double blockZ);

    /**
     * Set the home of the region (internally changes the flag).
     * @param blockX block x location
     * @param blockY block y location
     * @param blockZ block z location
     * @param yaw location yaw
     * @param pitch location pitch
     */
    public abstract void setHome(double blockX, double blockY, double blockZ, float yaw, float pitch);

    // -=-=-=-=- Other -=-=-=-=-=-

    /**
     * Must be run sync (calls Bukkit API)
     * @return whether or not the protection block is hidden (/no hide)
     */
    public boolean isHidden() {
        return !this.getType().equals(BlockUtil.getProtectBlockType(this.getProtectBlock()));
    }

    /**
     * Hides the protection block, if it is not hidden.
     * @return whether or not the block was hidden
     */
    public boolean hide() {
        if (!isHidden()) {
            getProtectBlock().setType(Material.AIR);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Unhides the protection block, if it is hidden.
     * @return whether or not the block was unhidden
     */
    public boolean unhide() {
        if (isHidden()) {
            if (getType().startsWith("PLAYER_HEAD")) {
                getProtectBlock().setType(Material.PLAYER_HEAD);
                if (getType().split(":").length > 1) {
                    BlockUtil.setHeadType(getType(), getProtectBlock());
                }
            } else {
                getProtectBlock().setType(Material.getMaterial(getType()));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Toggle whether or not the protection block is hidden.
     */
    public void toggleHide() {
        if (!hide()) unhide();
    }

    /**
     * This method returns the block that is supposed to contain the protection block.
     * Warning: If the protection stone is hidden, this will give the block that took its place!
     *
     * @return returns the block that may contain the protection stone
     */
    public abstract Block getProtectBlock();

    /**
     * @return returns the type, or null if the type is not configured
     */
    @Nullable
    public abstract OutpostProtectBlock getTypeOptions();

    /**
     * @return returns the protect block type (may include custom player heads PLAYER_HEAD:playername) that the region is
     */
    public abstract String getType();

    /**
     * Change the type of the protection region.
     * @param type the type of protection region to switch to
     */
    public void setType(OutpostProtectBlock type) {
        if (!isHidden()) {
            Material set = Material.matchMaterial(type.type) == null ? Material.PLAYER_HEAD : Material.matchMaterial(type.type);
            getProtectBlock().setType(set);
            if (type.type.startsWith("PLAYER_HEAD") && type.type.split(":").length > 1) {
                BlockUtil.setHeadType(type.type, getProtectBlock());
            }
        }
    }

    /**
     * Get whether or not a player is an owner of this region.
     * @param uuid the player's uuid
     * @return whether or not the player is a member
     */

    public abstract boolean isOwner(UUID uuid);

    /**
     * Get whether or not a player is a member of this region.
     * @param uuid the player's uuid
     * @return whether or not the player is a member
     */

    public abstract boolean isMember(UUID uuid);

    /**
     * @return returns a list of the owners of the protected region
     */
    public abstract ArrayList<UUID> getOwners();

    /**
     * @return returns a list of the members of the protected region
     */
    public abstract ArrayList<UUID> getMembers();

    /**
     * Add an owner to the region.
     * @param uuid the uuid of the player to add
     */
    public abstract void addOwner(UUID uuid);

    /**
     * Add a member to the region.
     * @param uuid the uuid of the player to add
     */
    public abstract void addMember(UUID uuid);

    /**
     * Remove an owner of the region, and deal with side-effects.
     * Examples of side-effects: removing player as landlord, removing player as auto taxpayer
     * @param uuid the uuid of the player to remove
     */
    public abstract void removeOwner(UUID uuid);

    /**
     * Remove a member of the region, and deal with side-effects
     * @param uuid the uuid of the player to remove
     */
    public abstract void removeMember(UUID uuid);

    /**
     * @return returns a list of the bounding points of the protected region
     */
    public abstract List<BlockVector2> getPoints();

    /**
     * Get a list of regions that the current region can merge into, taking into account a player's permissions.
     *
     * @param p the player to compare permissions with
     * @return the list of regions that the current region can merge into
     */
    public abstract List<OutpostRegion> getMergeableRegions(Player p);

    /**
     * Deletes the region forever. Can be cancelled by event cancellation.
     *
     * @param deleteBlock whether or not to also set the protection block to air (if not hidden)
     * @return whether or not the region was able to be successfully removed
     */
    public abstract boolean deleteRegion(boolean deleteBlock);

    /**
     * Deletes the region forever. Can be cancelled by event cancellation.
     *
     * @param deleteBlock whether or not to also set the protection block to air (if not hidden)
     * @param cause       the player that caused the region to break
     * @return whether or not the region was able to be successfully removed
     */
    public abstract boolean deleteRegion(boolean deleteBlock, Player cause);

    /**
     * @return returns the WorldGuard region object directly
     */
    public abstract ProtectedRegion getWGRegion();

    /**
     * @return returns the WorldGuard region manager that stores this region
     */
    public RegionManager getWGRegionManager() {
        return rgmanager;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutpostRegion psRegion = (OutpostRegion) o;
        return Objects.equals(getId(), psRegion.getId()) && Objects.equals(getWorld(), psRegion.getWorld());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getWorld());
    }
}
