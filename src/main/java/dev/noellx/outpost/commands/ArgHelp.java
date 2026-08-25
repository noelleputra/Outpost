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

import dev.noellx.outpost.OutpostL;
import dev.noellx.outpost.Outpost;
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
        String base = "/" + Outpost.getInstance().getConfigOptions().base_command + " ";

        helpMenu.clear();
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.INFO_HELP.msg(), OutpostL.INFO_HELP_DESC.msg(), base + "info"), "Outpost.info"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.ADDREMOVE_HELP.msg(), OutpostL.ADDREMOVE_HELP_DESC.msg(), base), "Outpost.members"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.ADDREMOVE_OWNER_HELP.msg(), OutpostL.ADDREMOVE_OWNER_HELP_DESC.msg(), base), "Outpost.owners"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.GET_HELP.msg(), OutpostL.GET_HELP_DESC.msg(), base + "get"), "Outpost.get"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.GIVE_HELP.msg(), OutpostL.GIVE_HELP_DESC.msg(), base + "give"), "Outpost.give"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.COUNT_HELP.msg(), OutpostL.COUNT_HELP_DESC.msg(), base + "count"), "Outpost.count", "Outpost.count.others"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.LIST_HELP.msg(), OutpostL.LIST_HELP_DESC.msg(), base + "list"), "Outpost.list", "Outpost.list.others"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.NAME_HELP.msg(), OutpostL.NAME_HELP_DESC.msg(), base + "name"), "Outpost.name"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.MERGE_HELP.msg(), OutpostL.MERGE_HELP_DESC.msg(), base + "merge"), "Outpost.merge"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.FLAG_HELP.msg(), OutpostL.FLAG_HELP_DESC.msg(), base + "flag"), "Outpost.flags"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.HOME_HELP.msg(), OutpostL.HOME_HELP_DESC.msg(), base + "home"), "Outpost.home"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.SETHOME_HELP.msg(), OutpostL.SETHOME_HELP_DESC.msg(), base + "sethome"), "Outpost.sethome"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.TP_HELP.msg(), OutpostL.TP_HELP_DESC.msg(), base + "tp"), "Outpost.tp"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.VISIBILITY_HIDE_HELP.msg(), OutpostL.VISIBILITY_HIDE_HELP_DESC.msg(), base + "hide"), "Outpost.hide"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.VISIBILITY_UNHIDE_HELP.msg(), OutpostL.VISIBILITY_UNHIDE_HELP_DESC.msg(), base + "unhide"), "Outpost.unhide"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.TOGGLE_HELP.msg(), OutpostL.TOGGLE_HELP_DESC.msg(), base + "toggle"), "Outpost.toggle"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.VIEW_HELP.msg(), OutpostL.VIEW_HELP_DESC.msg(), base + "view"), "Outpost.view"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.UNCLAIM_HELP.msg(), OutpostL.UNCLAIM_HELP_DESC.msg(), base + "unclaim"), "Outpost.unclaim"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.REGION_HELP.msg(), OutpostL.REGION_HELP_DESC.msg(), base + "region"), "Outpost.region"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.ADMIN_HELP.msg(), OutpostL.ADMIN_HELP_DESC.msg(), base + "admin"), "Outpost.admin"));
        helpMenu.add(new HelpEntry(sendWithPerm(OutpostL.RELOAD_HELP.msg(), OutpostL.RELOAD_HELP_DESC.msg(), base + "reload"), "Outpost.admin"));
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

        TextGUI.displayGUI(p, OutpostL.HELP.msg(), "/" + Outpost.getInstance().getConfigOptions().base_command + " help %page%", page, GUI_SIZE, entries, false);

        if (page >= 0 && page * GUI_SIZE + GUI_SIZE < entries.size()) {
            OutpostL.msg(p, OutpostL.HELP_NEXT.msg().replace("%page%", page + 2 + ""));
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
