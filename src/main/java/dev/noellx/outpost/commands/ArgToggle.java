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

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.noellx.outpost.OutpostL;
import dev.noellx.outpost.Outpost;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ArgToggle implements NOCommandArg {

    // /no on
    public static class ArgToggleOn implements NOCommandArg {
        @Override
        public List<String> getNames() {
            return Collections.singletonList("on");
        }
        @Override
        public boolean allowNonPlayersToExecute() {
            return false;
        }
        @Override
        public List<String> getPermissionsToExecute() {
            return Collections.singletonList("Outpost.toggle");
        }
        @Override
        public HashMap<String, Boolean> getRegisteredFlags() {
            return null;
        }
        @Override
        public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
            Player p = (Player) s;
            if (p.hasPermission("Outpost.toggle")) {
                Outpost.toggleList.remove(p.getUniqueId());
                p.sendMessage(OutpostL.TOGGLE_ON.msg());
            } else {
                p.sendMessage(OutpostL.NO_PERMISSION_TOGGLE.msg());
            }
            return true;
        }
        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return null;
        }
    }

    // /no off
    public static class ArgToggleOff implements NOCommandArg {
        @Override
        public List<String> getNames() {
            return Collections.singletonList("off");
        }
        @Override
        public boolean allowNonPlayersToExecute() {
            return false;
        }
        @Override
        public List<String> getPermissionsToExecute() {
            return Collections.singletonList("Outpost.toggle");
        }
        @Override
        public HashMap<String, Boolean> getRegisteredFlags() {
            return null;
        }
        @Override
        public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
            Player p = (Player) s;
            if (p.hasPermission("Outpost.toggle")) {
                Outpost.toggleList.add(p.getUniqueId());
                p.sendMessage(OutpostL.TOGGLE_OFF.msg());
            } else {
                p.sendMessage(OutpostL.NO_PERMISSION_TOGGLE.msg());
            }
            return true;
        }
        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return null;
        }
    }

    @Override
    public List<String> getNames() {
        return Collections.singletonList("toggle");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Collections.singletonList("Outpost.toggle");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        Player p = (Player) s;
        if (p.hasPermission("Outpost.toggle")) {
            if (!Outpost.toggleList.contains(p.getUniqueId())) {
                Outpost.toggleList.add(p.getUniqueId());
                p.sendMessage(OutpostL.TOGGLE_OFF.msg());
            } else {
                Outpost.toggleList.remove(p.getUniqueId());
                p.sendMessage(OutpostL.TOGGLE_ON.msg());
            }
        } else {
            p.sendMessage(OutpostL.NO_PERMISSION_TOGGLE.msg());
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }
}
