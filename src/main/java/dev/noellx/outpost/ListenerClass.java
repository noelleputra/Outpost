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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.event.block.PlaceBlockEvent;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import dev.noellx.outpost.event.NOBreakProtectBlockEvent;
import dev.noellx.outpost.event.NOCreateEvent;
import dev.noellx.outpost.event.NORemoveEvent;
import dev.noellx.outpost.utils.RecipeUtil;
import dev.noellx.outpost.utils.UUIDCache;
import dev.noellx.outpost.utils.WGUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ListenerClass implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // update UUID cache
        UUIDCache.removeUUID(p.getUniqueId());
        UUIDCache.removeName(p.getName());
        UUIDCache.storeUUIDNamePair(p.getUniqueId(), p.getName());

        // allow worldguard to resolve all UUIDs to names
        Bukkit.getScheduler().runTaskAsynchronously(NullaeOutpost.getInstance(), () -> UUIDCache.storeWGProfile(p.getUniqueId(), p.getName()));

        // add recipes to player's recipe book
        p.discoverRecipes(RecipeUtil.getRecipeKeys());

        // if by default, players should have protection block placement toggled off
        if (NullaeOutpost.getInstance().getConfigOptions().defaultProtectionBlockPlacementOff) {
            NullaeOutpost.toggleList.add(p.getUniqueId());
        }
    }

    // specifically add WG passthrough bypass here, so other plugins can see the result
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlaceLowPriority(PlaceBlockEvent event) {
        var cause = event.getCause().getRootCause();

        if (cause instanceof Player player && event.getBlocks().size() >= 1) {
            var block = event.getBlocks().get(0);
            if (!NullaeOutpost.isProtectBlockItem(player.getInventory().getItemInHand())) {
                return;
            }

            var options = NullaeOutpost.getBlockOptions(player.getInventory().getItemInHand());

            if (options != null && options.placingBypassesWGPassthrough) {
                // check if any regions here have the passthrough flag
                // we can't query unfortunately, since null flags seem to equate to ALLOW, when we want it to be DENY
                ApplicableRegionSet set = WGUtils.getRegionManagerWithWorld(event.getWorld()).getApplicableRegions(BukkitAdapter.asBlockVector(block.getLocation()));

                // loop through regions, if any region does not have a passthrough value set, then don't allow
                for (var region : set.getRegions()) {
                    if (region.getFlag(Flags.PASSTHROUGH) == null) {
                        return;
                    }
                }

                // if every region with an explicit passthrough value, then allow passthrough of protection block
                event.setResult(Event.Result.ALLOW);
            }
        }
    }

    // we only create the region after other plugins' event handlers have run
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        BlockHandler.createNORegion(e);
    }

    // returns the error message, or "" if the player has permission to break the region
    // TODO: refactor and move this to NORegion, so that /no unclaim can use the same checks
    private String checkPermissionToBreakProtection(Player p, NORegion r) {
        // check for destroy permission
        if (!p.hasPermission("NullaeOutpost.destroy")) {
            return NOL.NO_PERMISSION_DESTROY.msg();
        }

        // check if player is owner of region
        if (!r.isOwner(p.getUniqueId()) && !p.hasPermission("NullaeOutpost.superowner")) {
            return NOL.NO_REGION_PERMISSION.msg();
        }

        return "";
    }

    // helper method for breaking protection blocks
    // IMPLEMENTATION NOTES: r may be of a non-configured type
    private boolean playerBreakProtection(Player p, NORegion r) {
        NOProtectBlock blockOptions = r.getTypeOptions();

        // check if player has permission to break the protection
        String error = checkPermissionToBreakProtection(p, r);
        if (!error.isEmpty()) {
            NOL.msg(p, error);
            return false;
        }

        // Call NOBreakEvent
        NOBreakProtectBlockEvent event = new NOBreakProtectBlockEvent(r , p);
        Bukkit.getPluginManager().callEvent(event);
        // don't give no block to player if the event is cancelled
        if (event.isCancelled()) return false;

        // return protection stone if no drop option is off
        if (blockOptions != null && !blockOptions.noDrop) {
            if (!p.getInventory().addItem(blockOptions.createItem()).isEmpty()) {
                // method will return not empty if item couldn't be added
                if (NullaeOutpost.getInstance().getConfigOptions().dropItemWhenInventoryFull) {
                    NOL.msg(p, NOL.NO_ROOM_DROPPING_ON_FLOOR.msg());
                    p.getWorld().dropItem(r.getProtectBlock().getLocation(), blockOptions.createItem());
                } else {
                    NOL.msg(p, NOL.NO_ROOM_IN_INVENTORY.msg());
                    return false;
                }
            }
        }

        // check if removing the region and firing region remove event blocked it
        if (!r.deleteRegion(true, p)) {
            if (!NullaeOutpost.getInstance().getConfigOptions().allowMergingHoles) { // side case if the removing creates a hole and those are prevented
                NOL.msg(p, NOL.DELETE_REGION_PREVENTED_NO_HOLES.msg());
            }
            return false;
        }

        NOL.msg(p, NOL.NO_LONGER_PROTECTED.msg());
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        // shift-right click block with hand to break
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && !e.isBlockInHand()
                && e.getClickedBlock() != null && NullaeOutpost.isProtectBlock(e.getClickedBlock())) {

            NOProtectBlock ppb = NullaeOutpost.getBlockOptions(e.getClickedBlock());
            if (ppb.allowShiftRightBreak && e.getPlayer().isSneaking()) {
                NORegion r = NORegion.fromLocation(e.getClickedBlock().getLocation());
                if (r != null && playerBreakProtection(e.getPlayer(), r)) { // successful
                    e.getClickedBlock().setType(Material.AIR);
                }
            }
        }
    }

    // this will be the first event handler called in the chain
    // thus we should cancel the event here if possible (so other plugins don't start acting upon it)
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakLowPriority(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block pb = e.getBlock();

        if (!NullaeOutpost.isProtectBlock(pb)) return;

        // check if player has permission to break the protection
        NORegion r = NORegion.fromLocation(pb.getLocation());
        if (r != null) {
            String error = checkPermissionToBreakProtection(p, r);
            if (!error.isEmpty()) {
                NOL.msg(p, error);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block pb = e.getBlock();

        NOProtectBlock blockOptions = NullaeOutpost.getBlockOptions(pb);

        // check if block broken is protection stone type
        if (blockOptions == null) return;

        // check if that is actually a protection stone block (owns a region)
        if (!NullaeOutpost.isProtectBlock(pb)) {
            // prevent silk touching of protection stone blocks (that aren't holding a region)
            if (blockOptions.preventSilkTouch) {
                ItemStack left = p.getInventory().getItemInMainHand();
                ItemStack right = p.getInventory().getItemInOffHand();
                if (!left.containsEnchantment(Enchantment.SILK_TOUCH) && !right.containsEnchantment(Enchantment.SILK_TOUCH)) {
                    return;
                }
                e.setDropItems(false);
            }
            return;
        }

        NORegion r = NORegion.fromLocation(pb.getLocation());

        // break protection
        if (r != null && playerBreakProtection(p, r)) { // successful
            e.setDropItems(false);
            e.setExpToDrop(0);
        } else { // unsuccessful
            e.setCancelled(true);
        }
    }

    // -=-=-=- prevent smelting protection blocks -=-=-=-

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent e) {
        // prevent protect block item to be smelt
        NOProtectBlock options = NullaeOutpost.getBlockOptions(e.getSource());
        if (options != null && !options.allowSmeltItem) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFurnaceBurnItem(FurnaceBurnEvent e) {
        // prevent protect block item to be smelt
        Furnace f = (Furnace) e.getBlock().getState();
        if (f.getInventory().getSmelting() != null) {
            NOProtectBlock options = NullaeOutpost.getBlockOptions(f.getInventory().getSmelting());
            NOProtectBlock fuelOptions = NullaeOutpost.getBlockOptions(f.getInventory().getFuel());
            if ((options != null && !options.allowSmeltItem) || (fuelOptions != null && !fuelOptions.allowSmeltItem)) {
                e.setCancelled(true);
            }
        }
    }

    // -=-=-=- prevent crafting using protection blocks -=-=-=-

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPrepareItemCraft(PrepareItemCraftEvent e) {
        for (ItemStack s : e.getInventory().getMatrix()) {
            NOProtectBlock options = NullaeOutpost.getBlockOptions(s);
            if (options != null && !options.allowUseInCrafting) {
                e.getInventory().setResult(new ItemStack(Material.AIR));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent e) {
        if (e.getBlock().getType() != Material.CRAFTER) return;
        if (!(e.getBlock().getState() instanceof Container container)) return;
        for (ItemStack item : container.getInventory().getContents()) {
            if (item == null) continue;
            NOProtectBlock options = NullaeOutpost.getBlockOptions(item);
            if (options != null && !options.allowUseInCrafting) {
                e.setCancelled(true);
                e.setResult(new ItemStack(Material.AIR));
                return;
            }
        }
    }

    // -=-=-=- disable grindstone inventory to prevent infinite exp exploit with enchanted_effect option  -=-=-=-
    // see https://github.com/espidev/NullaeOutpost/issues/324

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryClickEvent(InventoryClickEvent e) {
        if (e.getInventory().getType() == InventoryType.GRINDSTONE) {
            if (NullaeOutpost.isProtectBlockItem(e.getCurrentItem())) {
                e.setCancelled(true);
            }
        }
    }


    // -=-=-=- block changes to protection block related events -=-=-=-

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketEmptyEvent e) {
        Block clicked = e.getBlockClicked();
        BlockFace bf = e.getBlockFace();
        Block check = clicked.getWorld().getBlockAt(clicked.getX() + e.getBlockFace().getModX(), clicked.getY() + bf.getModY(), clicked.getZ() + e.getBlockFace().getModZ());
        if (NullaeOutpost.isProtectBlock(check)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent e) {
        if (NullaeOutpost.isProtectBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        if (NullaeOutpost.isProtectBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent e) {
        if (NullaeOutpost.isProtectBlock(e.getToBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        if (NullaeOutpost.isProtectBlock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent e) {
        if (NullaeOutpost.isProtectBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent e) {
        if (NullaeOutpost.isProtectBlock(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent e) {
        // unfortunately, the below fix does not really work because Spigot only triggers for the source block, despite
        // what the documentation says: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/event/block/BlockDropItemEvent.html

        // we want to replace protection blocks that have their protection block broken (ex. signs, banners)
        // the block may not exist anymore, and so we have to recreate the isProtectBlock method here
        BlockState bs = e.getBlockState();
        if (!NullaeOutpost.isProtectBlockType(bs.getType().toString())) return;

        RegionManager rgm = WGUtils.getRegionManagerWithWorld(bs.getWorld());
        if (rgm == null) return;

        // check if the block is a source block
        ProtectedRegion br = rgm.getRegion(WGUtils.createNOID(bs.getLocation()));
        if (!NullaeOutpost.isNORegion(br) && NOMergedRegion.getMergedRegion(bs.getLocation()) == null) return;

        NORegion r = NORegion.fromLocation(bs.getLocation());
        if (r == null) return;

        // puts the block back
        r.unhide();
        e.setCancelled(true);
    }

    // -=-=- prevent protection block piston effects -=-=-

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        pistonUtil(e.getBlocks(), e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        pistonUtil(e.getBlocks(), e);
    }

    private void pistonUtil(List<Block> pushedBlocks, BlockPistonEvent e) {
        for (Block b : pushedBlocks) {
            NOProtectBlock cpb = NullaeOutpost.getBlockOptions(b);
            if (cpb != null && NullaeOutpost.isProtectBlock(b) && cpb.preventPistonPush) {
                e.setCancelled(true);
            }
        }
    }

    // -=-=- prevent protection blocks from exploding -=-=-

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        explodeUtil(e.blockList(), e.getBlock().getLocation().getWorld(), false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        boolean isWindCharge = e.getEntity() instanceof WindCharge;
        explodeUtil(e.blockList(), e.getLocation().getWorld(), isWindCharge);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (!NullaeOutpost.isProtectBlock(e.getBlock())) return;

        // events like ender dragon block break, wither running into block break, etc.
        if (!blockExplodeUtil(e.getBlock().getWorld(), e.getBlock(), false)) {
            // if block shouldn't be exploded, cancel the event
            e.setCancelled(true);
        }
    }

    private void explodeUtil(List<Block> blockList, World w, boolean isWindCharge) {
        // loop through exploded blocks
        for (int i = 0; i < blockList.size(); i++) {
            Block b = blockList.get(i);

            if (NullaeOutpost.isProtectBlock(b)) {
                // always remove protection block from exploded list
                blockList.remove(i);
                i--;
            }

            blockExplodeUtil(w, b, isWindCharge);
        }
    }

    // returns whether the block is exploded
    private boolean blockExplodeUtil(World w, Block b, boolean isWindCharge) {
        if (NullaeOutpost.isProtectBlock(b)) {
            String id = WGUtils.createNOID(b.getLocation());
            NOProtectBlock blockOptions = NullaeOutpost.getBlockOptions(b);

            // if prevent explode
            if (blockOptions.preventExplode) {
                return false;
            }

            if (isWindCharge && blockOptions.preventWindChargeExplode) {
                return false;
            }

            // manually set to air if exploded so there is no natural item drop
            b.setType(Material.AIR);

            // manually add drop
            if (!blockOptions.noDrop) {
                b.getWorld().dropItem(b.getLocation(), blockOptions.createItem());
            }
            // remove region from worldguard if destroy_region_when_explode is enabled
            if (blockOptions.destroyRegionWhenExplode) {
                NullaeOutpost.removeNORegion(w, id);
            }
        }
        return true;
    }

    // check player teleporting into region behaviour
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // we only want plugin triggered teleports, ignore natural teleportation
        if (event.getCause() == TeleportCause.ENDER_PEARL || event.getCause() == TeleportCause.CHORUS_FRUIT) return;

        if (event.getPlayer().hasPermission("NullaeOutpost.tp.bypassprevent")) return;

        WorldGuardPlugin wg = WorldGuardPlugin.inst();
        RegionManager rgm = WGUtils.getRegionManagerWithWorld(event.getTo().getWorld());
        BlockVector3 v = BlockVector3.at(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());

        if (rgm != null) {
            // check if player can teleport into region (no region with preventTeleportIn = true)
            ApplicableRegionSet regions = rgm.getApplicableRegions(v);
            if (regions.getRegions().isEmpty()) return;
            boolean foundNoTeleport = false;
            for (ProtectedRegion r : regions) {
                String f = r.getFlag(FlagHandler.NO_BLOCK_MATERIAL);
                if (f != null && NullaeOutpost.getBlockOptions(f) != null && NullaeOutpost.getBlockOptions(f).preventTeleportIn)
                    foundNoTeleport = true;
                if (r.getOwners().contains(wg.wrapPlayer(event.getPlayer()))) return;
            }

            if (foundNoTeleport) {
                NOL.msg(event.getPlayer(), NOL.REGION_CANT_TELEPORT.msg());
                event.setCancelled(true);
            }
        }
    }

    // -=-=-=- player defined events -=-=-=-

    private void execEvent(String action, CommandSender s, String player, NORegion region) {
        if (player == null) player = "";

        // split action_type: action
        String[] sp = action.split(": ");
        if (sp.length == 0) return;

        StringBuilder act = new StringBuilder(sp[1]);
        for (int i = 2; i < sp.length; i++) act.append(": ").append(sp[i]); // add anything extra that has a colon

        act = new StringBuilder(act.toString()
                .replace("%player%", player)
                .replace("%world%", region.getWorld().getName())
                .replace("%region%", region.getName() == null ? region.getId() : region.getName() + " (" + region.getId() + ")")
                .replace("%block_x%", region.getProtectBlock().getX() + "")
                .replace("%block_y%", region.getProtectBlock().getY() + "")
                .replace("%block_z%", region.getProtectBlock().getZ() + ""));

        switch (sp[0]) {
            case "player_command":
                if (s != null) Bukkit.getServer().dispatchCommand(s, act.toString());
                break;
            case "console_command":
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), act.toString());
                break;
            case "message":
                if (s != null) s.sendMessage(ChatColor.translateAlternateColorCodes('&', act.toString()));
                break;
            case "global_message":
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&', act.toString()));
                }
                NullaeOutpost.getPluginLogger().info(ChatColor.translateAlternateColorCodes('&', act.toString()));
                break;
            case "console_message":
                NullaeOutpost.getPluginLogger().info(ChatColor.translateAlternateColorCodes('&', act.toString()));
                break;
        }
    }

    @EventHandler
    public void onNOCreate(NOCreateEvent event) {
        if (event.isCancelled()) return;
        if (!event.getRegion().getTypeOptions().eventsEnabled) return;

        // run on next tick (after the region is created to allow for edits to the region)
        Bukkit.getServer().getScheduler().runTask(NullaeOutpost.getInstance(), () -> {
            // run custom commands (in config)
            for (String action : event.getRegion().getTypeOptions().regionCreateCommands) {
                execEvent(action, event.getPlayer(), event.getPlayer().getName(), event.getRegion());
            }
        });
    }

    @EventHandler
    public void onNORemove(NORemoveEvent event) {
        if (event.isCancelled()) return;
        if (event.getRegion().getTypeOptions() == null) return;
        if (!event.getRegion().getTypeOptions().eventsEnabled) return;

        // run custom commands (in config)
        for (String action : event.getRegion().getTypeOptions().regionDestroyCommands) {
            if (event.getPlayer() == null) {
                execEvent(action, null, null, event.getRegion());
            } else {
                execEvent(action, event.getPlayer(), event.getPlayer().getName(), event.getRegion());
            }
        }
    }

}
