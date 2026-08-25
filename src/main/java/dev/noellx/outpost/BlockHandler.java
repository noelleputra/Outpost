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

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import dev.noellx.outpost.commands.ArgMerge;
import dev.noellx.outpost.event.NOCreateEvent;
import dev.noellx.outpost.utils.LimitUtil;
import dev.noellx.outpost.utils.MiscUtil;
import dev.noellx.outpost.utils.WGMerge;
import dev.noellx.outpost.utils.WGUtils;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BlockHandler {
    private static HashMap<Player, Double> lastProtectStonePlaced = new HashMap<>();

    private static String checkCooldown(Player p) {
        double currentTime = System.currentTimeMillis();
        if (lastProtectStonePlaced.containsKey(p)) {
            double cooldown = NullaeOutpost.getInstance().getConfigOptions().placingCooldown; // seconds
            double lastPlace = lastProtectStonePlaced.get(p); // milliseconds

            if (lastPlace + cooldown * 1000 > currentTime) { // if cooldown has not been finished
                return String.format("%.1f", cooldown - ((currentTime - lastPlace) / 1000));
            }
            lastProtectStonePlaced.remove(p);
        }
        lastProtectStonePlaced.put(p, currentTime);
        return null;
    }

    private static boolean isFarEnoughFromOtherClaims(NOProtectBlock blockOptions, World w, LocalPlayer lp, double bx, double by, double bz) {
        BlockVector3 min = WGUtils.getMinVector(bx, by, bz, blockOptions.distanceBetweenClaims, blockOptions.distanceBetweenClaims, blockOptions.distanceBetweenClaims);
        BlockVector3 max = WGUtils.getMaxVector(bx, by, bz, blockOptions.distanceBetweenClaims, blockOptions.distanceBetweenClaims, blockOptions.distanceBetweenClaims);

        ProtectedRegion td = new ProtectedCuboidRegion("regionRadiusTest" + (long) (bx + by + bz), true, min, max);
        td.setPriority(blockOptions.priority);
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(w);

        // if the radius test region overlaps an unowned region
        if (rgm.overlapsUnownedRegion(td, lp)) {
            for (ProtectedRegion rg : rgm.getApplicableRegions(td)) {
                // skip if the user is already an owner
                if (rg.isOwner(lp)) continue;

                if (NullaeOutpost.isNORegion(rg) && rg.getFlag(Flags.PASSTHROUGH) != StateFlag.State.ALLOW) {
                    // if it is a NO region, and "passthrough allow" is not set, then it is not far enough
                    return false;
                } else if (rg.getPriority() >= td.getPriority()) {
                    // if the priorities are the same for plain WorldGuard regions, it is not far enough
                    return false;
                }
            }
        }

        return true;
    }

    // create NO region from a block place event
    public static void createNORegion(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();

        // check if the block is a protection stone
        if (!NullaeOutpost.isProtectBlockType(b)) return;
        NOProtectBlock blockOptions = NullaeOutpost.getBlockOptions(b);

        // check if the item was created by protection stones (stored in custom tag)
        // block must have restrictObtaining enabled for blocking place
        if (blockOptions.restrictObtaining && !NullaeOutpost.isProtectBlockItem(e.getItemInHand(), true)) return;

        // check if player has toggled off placement of protection stones
        if (NullaeOutpost.toggleList.contains(p.getUniqueId())) return;

        // check if player can place block in that area
        if (!WorldGuardPlugin.inst().createProtectionQuery().testBlockPlace(p, b.getLocation(), b.getType())) {
            NOL.msg(p, NOL.CANT_PROTECT_THAT.msg());
            e.setCancelled(true);
            return;
        }

        // check if it is in a WorldGuard region
        RegionManager rgm = WGUtils.getRegionManagerWithPlayer(p);
        if (!blockOptions.allowPlacingInWild && rgm.getApplicableRegions(BlockVector3.at(b.getLocation().getX(), b.getLocation().getY(), b.getLocation().getZ())).size() == 0) {
            NOL.msg(p, NOL.MUST_BE_PLACED_IN_EXISTING_REGION.msg());
            e.setCancelled(true);
            return;
        }

        // create region, and cancel if it fails
        if (!createNORegion(p, b.getLocation(), blockOptions)) {
            e.setCancelled(true);
        }
    }

    // create a NO region (no checks for items)
    public static boolean createNORegion(Player p, Location l, NOProtectBlock blockOptions) {
        // check permission
        if (!p.hasPermission("NullaeOutpost.create")) {
            NOL.msg(p, NOL.NO_PERMISSION_CREATE.msg());
            return false;
        }
        if (!blockOptions.permission.equals("") && !p.hasPermission(blockOptions.permission)) {
            NOL.msg(p, NOL.NO_PERMISSION_CREATE_SPECIFIC.msg());
            return false;
        }

        // check cooldown
        if (NullaeOutpost.getInstance().getConfigOptions().placingCooldown != -1) {
            String time = checkCooldown(p);
            if (time != null) {
                NOL.msg(p, NOL.COOLDOWN.msg().replace("%time%", time));
                return false;
            }
        }

        // check if player reached region limit
        if (!LimitUtil.check(p, blockOptions)) {
            return false;
        }

        // non-admin checks
        if (!p.hasPermission("NullaeOutpost.admin")) {
            // check if in world blacklist or not in world whitelist
            boolean containsWorld = blockOptions.worlds.contains(p.getLocation().getWorld().getName());

            if ((containsWorld && blockOptions.worldListType.equalsIgnoreCase("blacklist")) || (!containsWorld && blockOptions.worldListType.equalsIgnoreCase("whitelist"))) {
                if (blockOptions.preventBlockPlaceInRestrictedWorld) {
                    NOL.msg(p, NOL.WORLD_DENIED_CREATE.msg());
                    return false;
                } else {
                    return true;
                }
            }

        } // end of non-admin checks

        if (createActualRegion(p, l, blockOptions)) { // region creation successful
            return true;
        } else { // region creation failed
            return false;
        }
    }

    // create the actual WG region for NO region
    public static boolean createActualRegion(Player p, Location l, NOProtectBlock blockOptions) {
        // create region
        double bx = l.getX(), by = l.getY(), bz = l.getZ();

        RegionManager rm = WGUtils.getRegionManagerWithPlayer(p);
        LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(p);

        String id = WGUtils.createNOID(bx, by, bz);

        // if the region's id already exists, possibly placing block where a region is hidden
        if (rm.hasRegion(id)) {
            NOL.msg(p, NOL.REGION_ALREADY_IN_LOCATION_IS_HIDDEN.msg());
            return false;
        }

        // check for minimum distance between claims by using fake region
        if (blockOptions.distanceBetweenClaims != -1 && !p.hasPermission("NullaeOutpost.superowner")) {
            if (!isFarEnoughFromOtherClaims(blockOptions, p.getWorld(), lp, bx, by, bz)) {
                NOL.msg(p, NOL.REGION_TOO_CLOSE.msg().replace("%num%", "" + blockOptions.distanceBetweenClaims));
                return false;
            }
        }

        // create actual region
        ProtectedRegion region = WGUtils.getDefaultProtectedRegion(blockOptions, WGUtils.parseNORegionToLocation(id));
        region.getOwners().addPlayer(p.getUniqueId());
        region.setPriority(blockOptions.priority);
        rm.addRegion(region); // added to the region manager, be careful in implementing checks

        // check if new region overlaps more powerful region
        if (!blockOptions.allowOverlapUnownedRegions && !p.hasPermission("NullaeOutpost.superowner") && WGUtils.overlapsStrongerRegion(p.getWorld(), region, lp)) {
            rm.removeRegion(id);
            NOL.msg(p, NOL.REGION_OVERLAP.msg());
            return false;
        }

        // add corresponding flags to new region by cloning blockOptions default flags
        HashMap<Flag<?>, Object> flags = new HashMap<>(blockOptions.regionFlags);

        // replace greeting and farewell messages with player name
        FlagHandler.initDefaultFlagPlaceholders(flags, p);

        // set flags
        try {
            region.setFlags(flags);
        } catch (Exception e) {
            NullaeOutpost.getPluginLogger().severe(String.format("Region flags have failed to initialize for: %s (%s)", blockOptions.alias, blockOptions.type));
            throw e;
        }
        FlagHandler.initCustomFlagsForNO(region, l, blockOptions);

        // check for player's number of adjacent region groups
        if (NullaeOutpost.getInstance().getConfigOptions().regionsMustBeAdjacent) {
            if (MiscUtil.getPermissionNumber(p, "NullaeOutpost.adjacent.", 1) >= 0 && !p.hasPermission("NullaeOutpost.admin")) {
                HashMap<String, ArrayList<String>> adjGroups = WGUtils.getPlayerAdjacentRegionGroups(p, rm);

                int permNum = MiscUtil.getPermissionNumber(p, "NullaeOutpost.adjacent.", 1);
                if (adjGroups.size() > permNum && permNum != -1) {
                    NOL.msg(p, NOL.REGION_NOT_ADJACENT.msg());
                    rm.removeRegion(id);
                    return false;
                }
            }
        }

        // fire event and check if cancelled
        NOCreateEvent event = new NOCreateEvent(NORegion.fromWGRegion(p.getWorld(), region), p);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            rm.removeRegion(id);
            return false;
        }

        NOL.msg(p, NOL.PROTECTED.msg());

        // hide block if auto hide is enabled
        if (blockOptions.autoHide) {
            NOL.msg(p, NOL.REGION_HIDDEN.msg());
            // run on next tick so placing tile entities don't complain
            Bukkit.getScheduler().runTask(NullaeOutpost.getInstance(), () -> l.getBlock().setType(Material.AIR));
        }

        // show merge menu
        if (NullaeOutpost.getInstance().getConfigOptions().allowMergingRegions && blockOptions.allowMerging && p.hasPermission("NullaeOutpost.merge")) {
            NORegion r = NORegion.fromWGRegion(p.getWorld(), region);
            if (r != null) playerMergeTask(p, r);
        }

        return true;
    }

    // merge behaviour after a region is created
    private static void playerMergeTask(Player p, NORegion r) {
        boolean showGUI = true;

        // auto merge to nearest region if only one exists
        if (r.getTypeOptions().autoMerge) {
            NORegion mergeTo = null;
            for (NORegion psr : r.getMergeableRegions(p)) {
                if (mergeTo == null) {
                    mergeTo = psr;
                    showGUI = false;
                } else {
                    showGUI = true;
                    break;
                }
            }

            // actually do auto merge
            if (!showGUI) {
                NORegion finalMergeTo = mergeTo;
                Bukkit.getScheduler().runTaskAsynchronously(NullaeOutpost.getInstance(), () -> {
                    try {
                        WGMerge.mergeRealRegions(p.getWorld(), r.getWGRegionManager(), finalMergeTo, Arrays.asList(finalMergeTo, r));
                        NOL.msg(p, NOL.MERGE_AUTO_MERGED.msg().replace("%region%", finalMergeTo.getId()));
                    } catch (WGMerge.RegionHoleException e) {
                        NOL.msg(p, NOL.NO_REGION_HOLES.msg()); // TODO github issue #120, prevent holes even if showGUI is true
                    }
                });
            }
        }

        // show merge gui
        if (showGUI) {
            List<TextComponent> tc = ArgMerge.getGUI(p, r);
            if (!tc.isEmpty()) { // if there are regions you can merge into
                p.sendMessage(ChatColor.WHITE + ""); // send empty line
                NOL.msg(p, NOL.MERGE_INTO.msg());
                NOL.msg(p, NOL.MERGE_HEADER.msg().replace("%region%", r.getId()));
                for (TextComponent t : tc) p.spigot().sendMessage(t);
                p.sendMessage(ChatColor.WHITE + ""); // send empty line
            }
        }
    }
}
