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

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import dev.noellx.outpost.NOL;
import dev.noellx.outpost.NOProtectBlock;
import dev.noellx.outpost.NullaeOutpost;

import java.util.*;

public class ArgGet implements NOCommandArg {

    @Override
    public List<String> getNames() {
        return Collections.singletonList("get");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Collections.singletonList("NullaeOutpost.get");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    private boolean openGetGUI(Player p) {
        NOL.msg(p, NOL.GET_HEADER.msg());
        for (NOProtectBlock b : NullaeOutpost.getInstance().getConfiguredBlocks()) {
            if ((!b.permission.equals("") && !p.hasPermission(b.permission)) || (b.preventPsGet && !p.hasPermission("NullaeOutpost.admin"))) {
                continue; // no permission
            }

            String price = "0";

            TextComponent tc = new TextComponent(NOL.GET_GUI_BLOCK.msg()
                    .replace("%alias%", b.alias)
                    .replace("%price%", price)
                    .replace("%description%", b.description)
                    .replace("%xradius%", ""+b.xRadius)
                    .replace("%yradius%", ""+b.yRadius)
                    .replace("%zradius%", ""+b.zRadius));

            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(NOL.GET_GUI_HOVER.msg()
                    .replace("%alias%", b.alias)
                    .replace("%price%", price)
                    .replace("%description%", b.description)
                    .replace("%xradius%", ""+b.xRadius)
                    .replace("%yradius%", ""+b.yRadius)
                    .replace("%zradius%", ""+b.zRadius)).create()));
            tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + NullaeOutpost.getInstance().getConfigOptions().base_command + " get " + b.alias));

            p.spigot().sendMessage(tc);
        }
        return true;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;
        if (!p.hasPermission("NullaeOutpost.get"))
            return NOL.msg(p, NOL.NO_PERMISSION_GET.msg());

        // /no get (for GUI)
        if (args.length == 1) return openGetGUI(p);

        if (args.length != 2)
            return NOL.msg(p, NOL.GET_HELP.msg());

        // check if argument is valid block
        NOProtectBlock cp = NullaeOutpost.getProtectBlockFromAlias(args[1]);
        if (cp == null)
            return NOL.msg(p, NOL.INVALID_BLOCK.msg());

        // check for block permission (custom)
        if (!cp.permission.equals("") && !p.hasPermission(cp.permission))
            return NOL.msg(p, NOL.GET_NO_PERMISSION_BLOCK.msg());

        // check if /no get is disabled on this
        if (cp.preventPsGet && !p.hasPermission("NullaeOutpost.admin"))
            return NOL.msg(p, NOL.GET_NO_PERMISSION_BLOCK.msg());

        // check if item was able to be added (inventory not full)
        if (!p.getInventory().addItem(cp.createItem()).isEmpty()) {
            if (NullaeOutpost.getInstance().getConfigOptions().dropItemWhenInventoryFull) { // drop on floor
                NOL.msg(p, NOL.NO_ROOM_DROPPING_ON_FLOOR.msg());
                p.getWorld().dropItem(p.getLocation(), cp.createItem());
            } else { // cancel event
                NOL.msg(p, NOL.NO_ROOM_IN_INVENTORY.msg());
            }
            return true;
        }

        return NOL.msg(p, NOL.GET_GOTTEN.msg());
    }

    // tab completion
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        List<String> l = new ArrayList<>();
        for (NOProtectBlock b : NullaeOutpost.getInstance().getConfiguredBlocks()) {
            if ((!b.permission.equals("") && !sender.hasPermission(b.permission)) || (b.preventPsGet && !sender.hasPermission("NullaeOutpost.admin"))) continue; // no permission
            l.add(b.alias);
        }
        return args.length == 2 ? StringUtil.copyPartialMatches(args[1], l, new ArrayList<>()) : null;
    }

}
