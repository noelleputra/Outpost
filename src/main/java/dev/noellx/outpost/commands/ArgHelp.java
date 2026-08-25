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
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.noellx.outpost.NOL;
import dev.noellx.outpost.NullaeOutpost;
import dev.noellx.outpost.utils.MiscUtil;
import dev.noellx.outpost.utils.TextGUI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ArgHelp implements NOCommandArg {

    private static class HelpEntry {
        String[] permission;
        TextComponent msg;

        HelpEntry(TextComponent msg, String... permission) {
            this.permission = permission;
            this.msg = msg;
        }
    }

    public static List<HelpEntry> helpMenu = new ArrayList<>();

    public static void initHelpMenu() {
        String base = "/" + NullaeOutpost.getInstance().getConfigOptions().base_command + " ";

        helpMenu.clear();
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.INFO_HELP.msg(), NOL.INFO_HELP_DESC.msg(), base + "info"), "NullaeOutpost.info"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.ADDREMOVE_HELP.msg(), NOL.ADDREMOVE_HELP_DESC.msg(), base), "NullaeOutpost.members"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.ADDREMOVE_OWNER_HELP.msg(), NOL.ADDREMOVE_OWNER_HELP_DESC.msg(), base), "NullaeOutpost.owners"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.GET_HELP.msg(), NOL.GET_HELP_DESC.msg(), base + "get"), "NullaeOutpost.get"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.GIVE_HELP.msg(), NOL.GIVE_HELP_DESC.msg(), base + "give"), "NullaeOutpost.give"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.COUNT_HELP.msg(), NOL.COUNT_HELP_DESC.msg(), base + "count"), "NullaeOutpost.count", "NullaeOutpost.count.others"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.LIST_HELP.msg(), NOL.LIST_HELP_DESC.msg(), base + "list"), "NullaeOutpost.list", "NullaeOutpost.list.others"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.NAME_HELP.msg(), NOL.NAME_HELP_DESC.msg(), base + "name"), "NullaeOutpost.name"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.MERGE_HELP.msg(), NOL.MERGE_HELP_DESC.msg(), base + "merge"), "NullaeOutpost.merge"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.FLAG_HELP.msg(), NOL.FLAG_HELP_DESC.msg(), base + "flag"), "NullaeOutpost.flags"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.HOME_HELP.msg(), NOL.HOME_HELP_DESC.msg(), base + "home"), "NullaeOutpost.home"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.SETHOME_HELP.msg(), NOL.SETHOME_HELP_DESC.msg(), base + "sethome"), "NullaeOutpost.sethome"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.TP_HELP.msg(), NOL.TP_HELP_DESC.msg(), base + "tp"), "NullaeOutpost.tp"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.VISIBILITY_HIDE_HELP.msg(), NOL.VISIBILITY_HIDE_HELP_DESC.msg(), base + "hide"), "NullaeOutpost.hide"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.VISIBILITY_UNHIDE_HELP.msg(), NOL.VISIBILITY_UNHIDE_HELP_DESC.msg(), base + "unhide"), "NullaeOutpost.unhide"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.TOGGLE_HELP.msg(), NOL.TOGGLE_HELP_DESC.msg(), base + "toggle"), "NullaeOutpost.toggle"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.VIEW_HELP.msg(), NOL.VIEW_HELP_DESC.msg(), base + "view"), "NullaeOutpost.view"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.UNCLAIM_HELP.msg(), NOL.UNCLAIM_HELP_DESC.msg(), base + "unclaim"), "NullaeOutpost.unclaim"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.REGION_HELP.msg(), NOL.REGION_HELP_DESC.msg(), base + "region"), "NullaeOutpost.region"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.ADMIN_HELP.msg(), NOL.ADMIN_HELP_DESC.msg(), base + "admin"), "NullaeOutpost.admin"));
        helpMenu.add(new HelpEntry(sendWithPerm(NOL.RELOAD_HELP.msg(), NOL.RELOAD_HELP_DESC.msg(), base + "reload"), "NullaeOutpost.admin"));
    }

    @Override
    public List<String> getNames() {
        return Collections.singletonList("help");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return false;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return null;
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    private static final int GUI_SIZE = 16;

    @Override
    public boolean executeArgument(CommandSender p, String[] args, HashMap<String, String> flags) {
        int page = 0;
        if (args.length > 1 && MiscUtil.isValidInteger(args[1])) {
            page = Integer.parseInt(args[1]) - 1;
        }

        List<TextComponent> entries = new ArrayList<>();
        for (HelpEntry he : helpMenu) {
            // ignore blank lines
            if (he.msg.getText().equals("")) {
                continue;
            }
            // check player permissions
            for (String perm : he.permission) {
                if (p.hasPermission(perm)) {
                    entries.add(he.msg);
                    break;
                }
            }
        }

        TextGUI.displayGUI(p, NOL.HELP.msg(), "/" + NullaeOutpost.getInstance().getConfigOptions().base_command + " help %page%", page, GUI_SIZE, entries, false);

        if (page >= 0 && page * GUI_SIZE + GUI_SIZE < entries.size()) {
            NOL.msg(p, NOL.HELP_NEXT.msg().replace("%page%", page + 2 + ""));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }

    private static TextComponent sendWithPerm(String msg, String desc, String cmd) {
        TextComponent m = new TextComponent(msg);
        m.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd));
        m.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(desc).create()));
        return m;
    }
}
