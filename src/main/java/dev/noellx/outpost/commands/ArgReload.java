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

import dev.noellx.outpost.NOL;
import dev.noellx.outpost.NullaeOutpost;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ArgReload implements NOCommandArg {

    // /no reload

    @Override
    public List<String> getNames() {
        return Collections.singletonList("reload");
    }

    @Override
    public boolean allowNonPlayersToExecute() {
        return true;
    }

    @Override
    public List<String> getPermissionsToExecute() {
        return Arrays.asList("NullaeOutpost.admin");
    }

    @Override
    public HashMap<String, Boolean> getRegisteredFlags() {
        return null;
    }

    @Override
    public boolean executeArgument(CommandSender p, String[] args, HashMap<String, String> flags) {
        if (!p.hasPermission("NullaeOutpost.admin")) {
            NOL.msg(p, NOL.NO_PERMISSION_ADMIN.msg());
            return true;
        }
        NOL.msg(p, NOL.RELOAD_START.msg());
        NullaeOutpost.loadConfig(true);
        NOL.msg(p, NOL.RELOAD_COMPLETE.msg());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return null;
    }

}
