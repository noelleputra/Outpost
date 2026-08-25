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

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import dev.noellx.outpost.OutpostL;
import dev.noellx.outpost.Outpost;
import dev.noellx.outpost.utils.upgrade.LegacyUpgrade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.*;

/*
 * To add new sub commands, add them here, and in ArgAdminHelp manually
 */

public class ArgAdmin implements NOCommandArg {

    // has to be a method, because the base command config option is not available until the plugin is loaded
    public static String getFlagHelp() {
        return ChatColor.AQUA + "> " + ChatColor.GRAY + "/" + Outpost.getInstance().getConfigOptions().base_command +
                " admin flag [world] [flagname] [value|null|default]";
    }

    @Override
    public List<String> getNames() {
        return Collections.singletonList("admin");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return true;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Collections.singletonList("Outpost.admin");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    // /no admin [arg]
    @Override
    public boolean executeArgument(CommandSender s, String[] args, HashMap<String, String> flags) {
        if (!s.hasPermission("Outpost.admin")) {
            return OutpostL.msg(s, OutpostL.NO_PERMISSION_ADMIN.msg());
        }

        if (args.length < 2) {
            ArgAdminHelp.argumentAdminHelp(s, args);
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "help":
                return ArgAdminHelp.argumentAdminHelp(s, args);
            case "version":
                s.sendMessage(ChatColor.AQUA + "Outpost: " + ChatColor.GRAY + Outpost.getInstance().getDescription().getVersion());
                s.sendMessage(ChatColor.AQUA + "Developers: " + ChatColor.GRAY + Outpost.getInstance().getDescription().getAuthors());
                s.sendMessage(ChatColor.AQUA + "Bukkit:  " + ChatColor.GRAY + Bukkit.getVersion());
                s.sendMessage(ChatColor.AQUA + "WG: " + ChatColor.GRAY + WorldGuardPlugin.inst().getDescription().getVersion());
                break;
            case "hide":
                return ArgAdminHide.argumentAdminHide(s, args);
            case "unhide":
                return ArgAdminHide.argumentAdminHide(s, args);
            case "flag":
                return ArgAdminFlag.argumentAdminFlag(s, args);
            case "fixregions":
                s.sendMessage(ChatColor.YELLOW + "Fixing...");
                LegacyUpgrade.upgradeRegions();
                s.sendMessage(ChatColor.YELLOW + "Done!");
                break;
            case "debug":
                if (Outpost.getInstance().isDebug()) {
                    s.sendMessage(ChatColor.YELLOW + "Debug mode is now off.");
                    Outpost.getInstance().setDebug(false);
                } else {
                    s.sendMessage(ChatColor.YELLOW + "Debug mode is now on.");
                    Outpost.getInstance().setDebug(true);
                }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 2) {
            List<String> arg = Arrays.asList("version", "hide", "unhide", "flag", "fixregions", "debug");
            return StringUtil.copyPartialMatches(args[1], arg, new ArrayList<>());
        }
        return null;
    }

}
