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

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import dev.noellx.outpost.commands.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OutpostCommand extends Command {

    OutpostCommand(String name) {
        super(name);
    }

    static void addDefaultArguments() {
        Outpost.getInstance().addCommandArgument(new ArgAddRemove());
        Outpost.getInstance().addCommandArgument(new ArgAdmin());
        Outpost.getInstance().addCommandArgument(new ArgCount());
        Outpost.getInstance().addCommandArgument(new ArgFlag());
        Outpost.getInstance().addCommandArgument(new ArgGet());
        Outpost.getInstance().addCommandArgument(new ArgGive());
        Outpost.getInstance().addCommandArgument(new ArgHideUnhide());
        Outpost.getInstance().addCommandArgument(new ArgHome());
        Outpost.getInstance().addCommandArgument(new ArgInfo());
        Outpost.getInstance().addCommandArgument(new ArgList());
        Outpost.getInstance().addCommandArgument(new ArgMerge());
        Outpost.getInstance().addCommandArgument(new ArgName());
        Outpost.getInstance().addCommandArgument(new ArgRegion());
        Outpost.getInstance().addCommandArgument(new ArgReload());
        Outpost.getInstance().addCommandArgument(new ArgSethome());
        Outpost.getInstance().addCommandArgument(new ArgToggle());
        Outpost.getInstance().addCommandArgument(new ArgToggle.ArgToggleOn());
        Outpost.getInstance().addCommandArgument(new ArgToggle.ArgToggleOff());
        Outpost.getInstance().addCommandArgument(new ArgTp());
        Outpost.getInstance().addCommandArgument(new ArgUnclaim());
        Outpost.getInstance().addCommandArgument(new ArgView());
        Outpost.getInstance().addCommandArgument(new ArgHelp());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 1) {
            List<String> l = new ArrayList<>();
            for (NOCommandArg no : Outpost.getInstance().getCommandArguments()) {
                boolean hasPerm = false;
                if (no.getPermissionsToExecute() == null) {
                    hasPerm = true;
                } else {
                    for (String perm : no.getPermissionsToExecute()) {
                        if (sender.hasPermission(perm)) {
                            hasPerm = true;
                            break;
                        }
                    }
                }
                if (hasPerm) l.addAll(no.getNames());
            }
            return StringUtil.copyPartialMatches(args[0], l, new ArrayList<>());
        } else if (args.length >= 2) {
            for (NOCommandArg no : Outpost.getInstance().getCommandArguments()) {
                for (String arg : no.getNames()) {
                    if (arg.equalsIgnoreCase(args[0])) {
                        return no.tabComplete(sender, alias, args);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean execute(CommandSender s, String label, String[] args) {
        if (args.length == 0) { // no arguments
            if (s instanceof ConsoleCommandSender) {
                s.sendMessage(ChatColor.RED + "You can only use /no reload, /no admin, /no give from console.");
            } else {
                new ArgHelp().executeArgument(s, args, null);
            }
            return true;
        }
        for (NOCommandArg command : Outpost.getInstance().getCommandArguments()) {
            if (command.getNames().contains(args[0])) {
                if (command.allowNonPlayersToExecute() || s instanceof Player) {

                    // extract flags
                    List<String> nArgs = new ArrayList<>();
                    HashMap<String, String> flags = new HashMap<>();
                    for (int i = 0; i < args.length; i++) {

                        if (command.getRegisteredFlags() != null && command.getRegisteredFlags().containsKey(args[i])) {
                            if (command.getRegisteredFlags().get(args[i])) { // has value after
                                if (i != args.length-1) {
                                    flags.put(args[i], args[++i]);
                                }
                            } else {
                                flags.put(args[i], null);
                            }
                        } else {
                            nArgs.add(args[i]);
                        }
                    }

                    return command.executeArgument(s, nArgs.toArray(new String[0]), flags);
                } else if (!command.allowNonPlayersToExecute()) {
                    s.sendMessage(ChatColor.RED + "You can only use /no reload, /no admin, /no give from console.");
                    return true;
                }
            }
        }

        OutpostL.msg(s, OutpostL.NO_SUCH_COMMAND.msg());
        return true;
    }
}
