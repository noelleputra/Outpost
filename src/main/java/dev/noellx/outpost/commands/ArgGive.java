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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import dev.noellx.outpost.OutpostL;
import dev.noellx.outpost.OutpostProtectBlock;
import dev.noellx.outpost.Outpost;

import java.util.*;

public class ArgGive implements NOCommandArg {

    @Override
    public List<String> getNames() {
        return Collections.singletonList("give");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return true;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Collections.singletonList("NullaeOutpost.give");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender p, String[] args, HashMap<String, String> flags) {
        if (!p.hasPermission("NullaeOutpost.give"))
            return OutpostL.msg(p, OutpostL.NO_PERMISSION_GIVE.msg());

        if (args.length < 3)
            return OutpostL.msg(p, OutpostL.GIVE_HELP.msg());

        // check if player online
        if (Bukkit.getPlayer(args[2]) == null)
            return OutpostL.msg(p, OutpostL.PLAYER_NOT_FOUND.msg() + " (" + args[2] + ")");

        // check if argument is valid block
        OutpostProtectBlock cp = Outpost.getProtectBlockFromAlias(args[1]);
        if (cp == null)
            return OutpostL.msg(p, OutpostL.INVALID_BLOCK.msg());

        // check if item was able to be added (inventory not full)
        Player no = Bukkit.getPlayer(args[2]);

        ItemStack item = cp.createItem();
        if (args.length >= 4 && args[3].matches("-?\\d+"))
            item.setAmount(Integer.parseInt(args[3]));

        if (!no.getInventory().addItem(item).isEmpty()) {
            if (Outpost.getInstance().getConfigOptions().dropItemWhenInventoryFull) {
                OutpostL.msg(no, OutpostL.NO_ROOM_DROPPING_ON_FLOOR.msg());
                no.getWorld().dropItem(no.getLocation(), cp.createItem());
            } else {
                return OutpostL.msg(p, OutpostL.GIVE_NO_INVENTORY_ROOM.msg());
            }
        }

        return OutpostL.msg(p, OutpostL.GIVE_GIVEN.msg().replace("%block%", args[1]).replace("%player%", Bukkit.getPlayer(args[2]).getDisplayName()));
    }

    // tab completion
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        List<String> l = new ArrayList<>();
        if (args.length == 2) {
            for (OutpostProtectBlock b : Outpost.getInstance().getConfiguredBlocks()) l.add(b.alias);
            return StringUtil.copyPartialMatches(args[1], l, new ArrayList<>());
        } else if (args.length == 3) {
            for (Player p : Bukkit.getOnlinePlayers()) l.add(p.getName());
            return StringUtil.copyPartialMatches(args[2], l, new ArrayList<>());
        }
        return null;
    }

}
