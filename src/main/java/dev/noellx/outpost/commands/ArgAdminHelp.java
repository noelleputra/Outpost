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

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.noellx.outpost.NullaeOutpost;

public class ArgAdminHelp {

    private static void send(CommandSender p, String text, String info, String clickCommand, boolean run) {
        // Create the main text component from legacy text.
        BaseComponent[] mainComponents = TextComponent.fromLegacyText(text);
        TextComponent mainText = new TextComponent("");
        for (BaseComponent component : mainComponents) {
            mainText.addExtra(component);
        }

        // Create the hover event from the info text, add click event after
        BaseComponent[] hoverComponents = TextComponent.fromLegacyText(info);
        mainText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponents));
        //toggle for running on mouse click, currently disabled
        if (run) {
            mainText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, ChatColor.stripColor(clickCommand)));
        } else {
            mainText.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ChatColor.stripColor(clickCommand)));
        }

        // Send the assembled message.
        p.spigot().sendMessage(mainText);
    }

    static boolean argumentAdminHelp(CommandSender p, String[] args) {
        String bc = "/" + NullaeOutpost.getInstance().getConfigOptions().base_command;
        String tx = ChatColor.AQUA + "> " + ChatColor.GRAY + bc;

        p.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "===============" +
                ChatColor.RESET + " NO Admin Help " +
                ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "===============\n");

        send(p,
                tx + " admin version",
                "Show the version number of the plugin.\n\n" + bc + " admin version",
                bc + " admin version",
                false);

        send(p,
                tx + " admin hide",
                "Hide all of the protection stone blocks in the world you are in.\n\n" + bc + " admin hide",
                bc + " admin hide",
                false);

        send(p,
                tx + " admin unhide",
                "Unhide all of the protection stone blocks in the world you are in.\n\n" + bc + " admin unhide",
                bc + " admin unhide",
                false);

        send(p,
                tx + " admin flag",
                "Set a flag for all protection stone regions in a world.\n\n" +
                        bc + " admin flag [world] [flagname] [value|null|default]",
                bc + " admin flag [world] [flagname] [value|null|default]",
                false);

        send(p,
                tx + " admin debug",
                "Toggle debug mode.\n\n" + bc + " admin debug",
                bc + " admin debug",
                false);

        send(p,
                tx + " admin fixregions",
                "Use this command to recalculate block types for NO regions in a world.\n\n" + bc + " admin fixregions",
                bc + " admin fixregions",
                false);
        //add footer since it was missing
        p.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=============================================");

        return true;
    }
}
