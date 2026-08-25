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

import dev.noellx.outpost.utils.MiscUtil;
import dev.noellx.outpost.utils.WGUtils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wrapper for a Bukkit player that exposes Outpost related methods.
 */

public class OutpostPlayer {

    // TODO implement
    public enum PlayerRegionRelationship {
        OWNER,
        MEMBER,
        LANDLORD,
        TENANT,
        NONMEMBER,
    }

    UUID uuid;

    Player p;

    /**
     * Adapt a UUID into a NOPlayer wrapper.
     *
     * @param uuid the uuid to wrap
     * @return the NOPlayer object
     */

    public static OutpostPlayer fromUUID(UUID uuid) {
        return new OutpostPlayer(checkNotNull(uuid));
    }

    /**
     * Adapt a Bukkit player into a NOPlayer wrapper.
     *
     * @param p the player to wrap
     * @return the NOPlayer object
     */

    public static OutpostPlayer fromPlayer(Player p) {
        return new OutpostPlayer(checkNotNull(p));
    }

    public static OutpostPlayer fromPlayer(OfflinePlayer p) {
        if (checkNotNull(p) instanceof Player) {
            return new OutpostPlayer((Player) p);
        } else {
            return new OutpostPlayer(p.getUniqueId());
        }
    }

    public OutpostPlayer(Player player) {
        this.p = player;
        this.uuid = player.getUniqueId();
    }

    public OutpostPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Get the wrapped player's uuid.
     * @return the uuid
     */

    public UUID getUuid() {
        return this.uuid;
    }

    /**
     * Get the wrapped Bukkit player.
     * It may return null if the object wraps a UUID that does not exist.
     *
     * @return the player, or null
     */

    public Player getPlayer() {
        if (p == null) return Bukkit.getPlayer(uuid);
        return p;
    }

    /**
     * Get the wrapped Bukkit offline player.
     * Safer to use than getPlayer (this does not return a null).
     * It may return an empty player if the object wraps a UUID that does not exist.
     *
     * @return the offline player
     */

    public OfflinePlayer getOfflinePlayer() {
        if (p == null) return Bukkit.getOfflinePlayer(uuid);
        return p;
    }

    public String getName() {
        return getOfflinePlayer().getName();
    }

    static class CannotAccessOfflinePlayerPermissionsException extends RuntimeException {}

    /**
     * Get a player's permission limits for each protection block (Outpost.limit.alias.x)
     * Protection blocks that aren't specified in the player's permissions will not be returned in the map.
     * If LuckPerms support isn't enabled and the player is not online, then the method will throw a CannotAccessOfflinePlayerPermissionsException.
     *
     * @return a hashmap containing a psprotectblock object to an integer, which is the number of protection regions of that type the player is allowed to place
     */

    public HashMap<OutpostProtectBlock, Integer> getRegionLimits() {
        HashMap<OutpostProtectBlock, Integer> regionLimits = new HashMap<>();

        List<String> permissions;

        if (getPlayer() != null) {
            permissions = getPlayer().getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission).collect(Collectors.toList());
        } else if (getOfflinePlayer().getPlayer() != null) {
            permissions = getOfflinePlayer().getPlayer().getEffectivePermissions().stream().map(PermissionAttachmentInfo::getPermission).collect(Collectors.toList());
        } else if (Outpost.getInstance().isLuckPermsSupportEnabled()) {
            // use luckperms to obtain all of an offline player's permissions (vault and spigot api are unable to do this)
            try {
                permissions = MiscUtil.getLuckPermsUserPermissions(getUuid());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                throw new CannotAccessOfflinePlayerPermissionsException();
            }
        } else {
            throw new CannotAccessOfflinePlayerPermissionsException();
        }

        for (String perm : permissions) {
            if (perm.startsWith("Outpost.limit")) {
                String[] spl = perm.split("\\.");

                if (spl.length == 4 && Outpost.getProtectBlockFromAlias(spl[2]) != null) {
                    OutpostProtectBlock block = Outpost.getProtectBlockFromAlias(spl[2]);
                    int limit = Integer.parseInt(spl[3]);
                    if (regionLimits.get(block) == null || regionLimits.get(block) < limit) { // only use max limit
                        regionLimits.put(block, limit);
                    }
                }
            }
        }
        return regionLimits;
    }

    /**
     * Get a player's total protection limit from permission (Outpost.limit.x)
     * If there is no attached Player object to this NOPlayer, and LuckPerms is not enabled, this throws a CannotAccessOfflinePlayerPermissionsException.
     *
     * @return the number of protection regions the player can have, or -1 if there is no limit set.
     */

    public int getGlobalRegionLimits() {
        if (getPlayer() != null) {
            return MiscUtil.getPermissionNumber(getPlayer(), "Outpost.limit.", -1);
        } else if (Outpost.getInstance().isLuckPermsSupportEnabled()) {
            // use LuckPerms to obtain all of an offline player's permissions (vault and spigot api are unable to do this)
            try {
                List<String> permissions = MiscUtil.getLuckPermsUserPermissions(getUuid());
                return MiscUtil.getPermissionNumber(permissions, "Outpost.limit.", -1);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                throw new CannotAccessOfflinePlayerPermissionsException();
            }
        } else {
            throw new CannotAccessOfflinePlayerPermissionsException();
        }
    }

    /**
     * Get the list of regions that a player owns, or is a member of. It is recommended to run this asynchronously
     * since the query can be slow.
     *
     * @param w           world to search for regions in
     * @param canBeMember whether or not to add regions where the player is a member, not owner
     * @return list of regions that the player owns (or is a part of if canBeMember is true)
     */

    public List<OutpostRegion> getNORegions(World w, boolean canBeMember) {
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(w);
        if (rgm == null) return new ArrayList<>();

        return rgm.getRegions().values().stream()
                .filter(Outpost::isNORegion)
                .filter(r -> r.getOwners().contains(uuid) || (canBeMember && r.getMembers().contains(uuid)))
                .map(r -> OutpostRegion.fromWGRegion(w, r))
                .collect(Collectors.toList());
    }

    /**
     * Get the list of regions that a player owns, or is a member of. It is recommended to run this asynchronously
     * since the query can be slow.
     *
     * Note: Regions that the player owns that are named will be cross-world, otherwise this only searches in one world.
     *
     * @param w           world to search for regions in
     * @param canBeMember whether or not to add regions where the player is a member, not owner
     * @return list of regions that the player owns (or is a part of if canBeMember is true)
     */

    public List<OutpostRegion> getNORegionsCrossWorld(World w, boolean canBeMember) {
        List<OutpostRegion> regions = getNORegions(w, canBeMember);
        // set entry format: "worldName regionId"
        Set<String> regionIdAdded = regions.stream().map(r -> w.getName() + " " + r.getId()).collect(Collectors.toSet());

        // obtain cross-world named worlds
        Outpost.regionNameToID.forEach((rw, rs) -> {
            World world = Bukkit.getWorld(rw);
            RegionManager rm = WGUtils.getRegionManagerWithWorld(world);
            if (rm != null) {
                rs.values().forEach(rIds -> rIds.forEach(rId -> {

                    ProtectedRegion r = rm.getRegion(rId);
                    if (r != null && r.getOwners().contains(uuid) && Outpost.isNORegion(r)) {
                        // check if it has already been added
                        String setId = world.getName() + " " + r.getId();
                        if (!world.getName().equals(w.getName()) || !regionIdAdded.contains(setId)) {
                            regions.add(OutpostRegion.fromWGRegion(world, r));
                            regionIdAdded.add(setId);
                        }
                    }
                }));
            }
        });

        return regions;
    }

    /**
     * Get the list of homes a player owns. It is recommended to run this asynchronously.
     *
     * Note: Regions that the player owns that are named will be cross-world, otherwise this only searches in one world.
     *
     * @param w world to search for regions in
     * @return list of regions that are the player's homes
     */

    public List<OutpostRegion> getHomes(World w) {
        return getNORegionsCrossWorld(w, Outpost.getInstance().getConfigOptions().allowHomeTeleportForMembers)
                .stream()
                .filter(r -> r.getTypeOptions() != null && !r.getTypeOptions().preventPsHome)
                .collect(Collectors.toList());
    }

}
