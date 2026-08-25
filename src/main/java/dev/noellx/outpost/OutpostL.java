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
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum OutpostL {
    // messages.yml

    COOLDOWN("cooldown", ChatColor.RED + "Please wait for %time% seconds before placing again!"),
    NO_SUCH_COMMAND("no_such_command", ChatColor.RED + "No such command. please type /no help for more info"),
    NO_ACCESS("no_access", ChatColor.RED + "You are not allowed to do that here."),
    NO_ROOM_IN_INVENTORY("no_room_in_inventory", ChatColor.RED + "You don't have enough room in your inventory."),
    NO_ROOM_DROPPING_ON_FLOOR("no_room_dropping_on_floor", ChatColor.RED + "You don't have enough room in your inventory. Dropping item on floor."),
    INVALID_BLOCK("invalid_block", ChatColor.RED + "Invalid protection block."),
    INVALID_WORLD("invalid_world", ChatColor.RED + "Invalid world."),
    MUST_BE_PLAYER("must_be_player", ChatColor.RED + "You must be a player to execute this command."),
    GO_BACK_PAGE("go_back_page", "Go back a page."),
    GO_NEXT_PAGE("go_next_page", "Go to next page."),
    PAGE_DOES_NOT_EXIST("page_does_not_exist", ChatColor.RED + "Page does not exist."),

    HELP("help", ChatColor.DARK_GRAY + "Usage: /no help"),
    HELP_NEXT("help_next", ChatColor.GRAY + "Do /no help %page% to go to the next page!"),

    COMMAND_REQUIRES_PLAYER_NAME("command_requires_player_name", ChatColor.RED + "This command requires a player name."),

    NO_PERMISSION_TOGGLE("no_permission_toggle", ChatColor.RED + "You don't have permission to use the toggle command."),
    NO_PERMISSION_CREATE("no_permission_create", ChatColor.RED + "You don't have permission to place a protection block."),
    NO_PERMISSION_CREATE_SPECIFIC("no_permission_create_specific", ChatColor.RED + "You don't have permission to place this protection block type."),
    NO_PERMISSION_DESTROY("no_permission_destroy", ChatColor.RED + "You don't have permission to destroy a protection block."),
    NO_PERMISSION_MEMBERS("no_permission_members", ChatColor.RED + "You don't have permission to use member commands."),
    NO_PERMISSION_OWNERS("no_permission_owners", ChatColor.RED + "You don't have permission to use owner commands."),
    NO_PERMISSION_ADMIN("no_permission_admin", ChatColor.RED + "You do not have permission to use that command."),
    NO_PERMISSION_COUNT("no_permission_count", ChatColor.RED + "You do not have permission to use that command."),
    NO_PERMISSION_COUNT_OTHERS("no_permission_count_others", ChatColor.RED + "You do not have permission to use that command."),
    NO_PERMISSION_FLAGS("no_permission_flags", ChatColor.RED + "You do not have permission to use flag commands."),
    NO_PERMISSION_PER_FLAG("no_permission_per_flag", ChatColor.RED + "You do not have permission to use that flag."),
    NO_PERMISSION_UNHIDE("no_permission_unhide", ChatColor.RED + "You do not have permission to unhide protection blocks."),
    NO_PERMISSION_HIDE("no_permission_hide", ChatColor.RED + "You do not have permission to hide protection blocks."),
    NO_PERMISSION_INFO("no_permission_info", ChatColor.RED + "You do not have permission to use the region info command."),
    NO_PERMISSION_REGION("no_permission_region", ChatColor.RED + "You do not have permission to use region commands."),
    NO_PERMISSION_TP("no_permission_tp", ChatColor.RED + "You do not have permission to teleport to other players' protection blocks."),
    NO_PERMISSION_HOME("no_permission_home", ChatColor.RED + "You do not have permission to teleport to your protection blocks."),
    NO_PERMISSION_UNCLAIM("no_permission_unclaim", ChatColor.RED + "You do not have permission to use the unclaim command."),
    NO_PERMISSION_UNCLAIM_REMOTE("no_permission_unclaim_remote", ChatColor.RED + "You do not have permission to use the unclaim remote command."),
    NO_PERMISSION_VIEW("no_permission_view", ChatColor.RED + "You do not have permission to use the view command."),
    NO_PERMISSION_GIVE("no_permission_give", ChatColor.RED + "You do not have permission to use the give command."),
    NO_PERMISSION_GET("no_permission_get", ChatColor.RED + "You do not have permission to use the get command."),
    NO_PERMISSION_SETHOME("no_permission_sethome", ChatColor.RED + "You do not have permission to use the sethome command."),
    NO_PERMISSION_LIST("no_permission_list", ChatColor.RED + "You do not have permission to use the list command."),
    NO_PERMISSION_LIST_OTHERS("no_permission_list_others", ChatColor.RED + "You do not have permission to use the list command for others."),
    NO_PERMISSION_NAME("no_permission_name", ChatColor.RED + "You do not have permission to use the name command."),
    NO_PERMISSION_MERGE("no_permission_merge", ChatColor.RED + "You do not have permission to use /no merge."),

    ADDED_TO_REGION("psregion.added_to_region", ChatColor.GREEN + "%player%" + ChatColor.GRAY + " has been added to this region."),
    ADDED_TO_REGION_SPECIFIC("psregion.added_to_region_specific", ChatColor.GREEN + "%player%" + ChatColor.GRAY + " has been added to region %region%."),
    REMOVED_FROM_REGION("psregion.removed_from_region", ChatColor.GREEN + "%player%" + ChatColor.GRAY + " has been removed from region."),
    REMOVED_FROM_REGION_SPECIFIC("psregion.removed_from_region_specific", ChatColor.GREEN + "%player%" + ChatColor.GRAY + " has been removed from region %region%."),
    NOT_IN_REGION("psregion.not_in_region", ChatColor.RED + "You are not in a protection stones region!"),
    PLAYER_NOT_FOUND("psregion.player_not_found", ChatColor.RED + "Player not found."),
    NOT_NO_REGION("psregion.not_ps_region", ChatColor.RED + "Not a protection stones region."),
    REGION_DOES_NOT_EXIST("psregion.region_does_not_exist", ChatColor.RED + "Region does not exist."),
    NO_REGIONS_OWNED("psregion.no_regions_owned", ChatColor.RED + "You don't own any protected regions in this world!"),
    NO_REGION_PERMISSION("psregion.no_region_permission", ChatColor.RED + "You do not have permission to do this in this region."),
    PROTECTED("psregion.protected", ChatColor.GREEN + "This area is now protected."),
    NO_LONGER_PROTECTED("psregion.no_longer_protected", ChatColor.YELLOW + "This area is no longer protected."),
    CANT_PROTECT_THAT("psregion.cant_protect_that", ChatColor.RED + "You can't protect that area."),
    REACHED_REGION_LIMIT("psregion.reached_region_limit", ChatColor.RED + "You can not have any more protected regions (%limit%)."),
    REACHED_PER_BLOCK_REGION_LIMIT("psregion.reached_per_block_region_limit", ChatColor.RED + "You can not have any more regions of this type (%limit%)."),
    WORLD_DENIED_CREATE("psregion.world_denied_create", ChatColor.RED + "You can not create protections in this world."),
    REGION_OVERLAP("psregion.region_overlap", ChatColor.RED + "You can not place a protection block here as it overlaps another region."),
    REGION_TOO_CLOSE("psregion.region_too_close", ChatColor.RED + "Your protection block must be a minimum of %num% blocks from the edge of other regions!"),
    REGION_CANT_TELEPORT("psregion.cant_teleport", ChatColor.RED + "Your teleportation was blocked by a protection region!"),
    SPECIFY_ID_INSTEAD_OF_ALIAS("psregion.specify_id_instead_of_alias", ChatColor.GRAY + "There were multiple regions found with this name! Please use an ID instead.\n Regions with this name: " + ChatColor.GREEN + "%regions%"),
    REGION_NOT_ADJACENT("psregion.region_not_adjacent", ChatColor.RED + "You've passed the limit of non-adjacent regions! Try putting your protection block closer to other regions you already own."),
    REGION_NOT_OVERLAPPING("psregion.not_overlapping", ChatColor.RED + "These regions don't overlap each other!"),
    MULTI_REGION_DOES_NOT_EXIST("psregion.multi_region_does_not_exist", "One of these regions don't exist!"),
    NO_REGION_HOLES("psregion.no_region_holes", ChatColor.RED + "Unprotected area detected inside region! This is not allowed!"),
    DELETE_REGION_PREVENTED_NO_HOLES("psregion.delete_region_prevented", ChatColor.GRAY + "The region could not be removed, possibly because it creates a hole in the existing region."),
    NOT_OWNER("psregion.not_owner", ChatColor.RED + "You are not an owner of this region!"),
    NO_PERMISSION_REGION_TYPE("psregion.no_permission_region_type", ChatColor.RED + "You do not have permission to have this region type."),
    REGION_HIDDEN("psregion.hidden", ChatColor.GRAY + "The protection block is now hidden."),
    MUST_BE_PLACED_IN_EXISTING_REGION("psregion.must_be_placed_in_existing_region", ChatColor.RED + "This must be placed inside of an existing region!"),
    REGION_ALREADY_IN_LOCATION_IS_HIDDEN("psregion.already_in_location_is_hidden", ChatColor.RED + "A region already exists in this location (is the protection block hidden?)"),
    CANNOT_REMOVE_YOURSELF_LAST_OWNER("psregion.cannot_remove_yourself_last_owner", ChatColor.RED + "You cannot remove yourself as you are the last owner."),
    CANNOT_REMOVE_YOURSELF_FROM_ALL_REGIONS("psregion.cannot_remove_yourself_all_regions", ChatColor.RED + "You cannot remove yourself from all of your regions at once, for safety reasons."),

    // no toggle
    TOGGLE_HELP("toggle.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no toggle|on|off"),
    TOGGLE_HELP_DESC("toggle.help_desc", "Use this command to turn on or off placement of protection blocks."),
    TOGGLE_ON("toggle.toggle_on", ChatColor.GREEN + "Protection block placement turned on."),
    TOGGLE_OFF("toggle.toggle_off", ChatColor.GREEN + "Protection block placement turned off."),

    // no count
    COUNT_HELP("count.count_help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no count [player (optional)]"),
    COUNT_HELP_DESC("count.count_help_desc", "Count the number of regions you own or another player."),
    PERSONAL_REGION_COUNT("count.personal_region_count", ChatColor.GRAY + "Your region count in this world: " + ChatColor.GREEN + "%num%"),
    PERSONAL_REGION_COUNT_MERGED("count.personal_region_count_merged", ChatColor.GRAY + "- Including each merged region: " + ChatColor.GREEN + "%num%"),
    OTHER_REGION_COUNT("count.other_region_count", ChatColor.GRAY + "%player%'s region count in this world: " + ChatColor.GREEN + "%num%"),
    OTHER_REGION_COUNT_MERGED("count.other_region_count_merged", ChatColor.GRAY + "- Including each merged region: " + ChatColor.GREEN + "%num%"),

    // no flag
    FLAG_HELP("flag.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no flag [flagname] [value|null|default]"),
    FLAG_HELP_DESC("flag.help_desc", "Use this command to set a flag in your protected region."),
    FLAG_SET("flag.flag_set", ChatColor.GREEN + "%flag%" + ChatColor.GRAY + " flag has been set."),
    FLAG_NOT_SET("flag.flag_not_set", ChatColor.GREEN + "%flag%" + ChatColor.GRAY + " flag has " + ChatColor.RED + "not" + ChatColor.GRAY + " been set. Check your values again."),
    FLAG_PREVENT_EXPLOIT("flag.flag_prevent_exploit", ChatColor.RED + "This has been disabled to prevent exploits."),
    FLAG_PREVENT_EXPLOIT_HOVER("flag.flag_prevent_exploit_hover", ChatColor.RED + "Disabled for security reasons."),
    FLAG_GUI_HEADER("flag.gui_header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Flags (click to change) " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    FLAG_GUI_HOVER_SET("flag.gui_hover_set", ChatColor.GREEN + "Click to set."),
    FLAG_GUI_HOVER_SET_TEXT("flag.gui_hover_set_text", ChatColor.GREEN + "Click to change." + ChatColor.WHITE + "\nCurrent value:\n%value%"),
    FLAG_GUI_HOVER_CHANGE_GROUP("flag.hover_change_group", "Click to set this flag to apply to only %group%."),
    FLAG_GUI_HOVER_CHANGE_GROUP_NULL("flag.hover_change_group_null", ChatColor.RED + "You must set this flag to a value before changing the group."),

    // no rent

    // no buy

    // no sell

    // no hide/unhide
    VISIBILITY_HIDE_HELP("visibility.hide_help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no hide"),
    VISIBILITY_HIDE_HELP_DESC("visibility.hide_help_desc", "Use this command to hide or unhide your protection block."),
    VISIBILITY_UNHIDE_HELP("visibility.unhide_help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no unhide"),
    VISIBILITY_UNHIDE_HELP_DESC("visibility.unhide_help_desc", "Use this command to hide or unhide your protection block."),
    ALREADY_NOT_HIDDEN("visibility.already_not_hidden", ChatColor.GRAY + "The protection stone doesn't appear hidden..."),
    ALREADY_HIDDEN("visibility.already_hidden", ChatColor.GRAY + "The protection stone appears to already be hidden..."),

    // no info
    INFO_HELP("info.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no info members|owners|flags"),
    INFO_HELP_DESC("info.help_desc", "Use this command inside a no region to see more information about it."),
    INFO_HEADER("info.header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " NO Info " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    INFO_TYPE2("info.type2", "&9Type: &7%type%", "%type%"),
    INFO_MAY_BE_MERGED("info.may_be_merged", "(may be merged with other types)"),
    INFO_MERGED2("info.merged2", ChatColor.BLUE + "Merged regions: " + ChatColor.GRAY + "%merged%", "%merged%"),
    INFO_MEMBERS2("info.members2", "&9Members: &7%members%", "%members%"),
    INFO_NO_MEMBERS("info.no_members", ChatColor.RED + "(no members)"),
    INFO_OWNERS2("info.owners2", "&9Owners: &7%owners%", "%owners%"),
    INFO_NO_OWNERS("info.no_owners", ChatColor.RED + "(no owners)"),
    INFO_FLAGS2("info.flags2", "&9Flags: &7%flags%", "%flags%"),
    INFO_NO_FLAGS("info.no_flags", "(none)"),
    INFO_REGION2("info.region2", "&9Region: &b%region%", "%region%"),
    INFO_PRIORITY2("info.priority2", "&9Priority: &b%priority%", "%priority%"),
    INFO_PARENT2("info.parent2", "&9Parent: &b%parentregion%", "%parentregion%"),
    INFO_BOUNDS_XYZ("info.bounds_xyz", "&9Bounds: &b(%minx%,%miny%,%minz%) -> (%maxx%,%maxy%,%maxz%)",
            "%minx%", "%miny%", "%minz%", "%maxx%", "%maxy%", "%maxz%"
    ),
    INFO_BOUNDS_XZ("info.bounds_xz", "&9Bounds: &b(%minx%, %minz%) -> (%maxx%, %maxz%)",
            "%minx%", "%minz%", "%maxx%", "%maxz%"
    ),

    // no priority

    // no region
    REGION_HELP("region.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no region [list|remove|disown] [playername]"),
    REGION_HELP_DESC("region.help_desc", "Use this command to find information or edit other players' (or your own) protected regions."),
    REGION_NOT_FOUND_FOR_PLAYER("region.not_found_for_player", ChatColor.GRAY + "No regions found for %player% in this world."),
    REGION_LIST("region.list", ChatColor.GRAY + "%player%'s regions in this world: " + ChatColor.GREEN + "%regions%"),
    REGION_REMOVE("region.remove", ChatColor.YELLOW + "%player%'s regions have been removed in this world, and they have been removed from regions they co-owned."),
    REGION_DISOWN("region.disown", ChatColor.YELLOW + "%player% has been removed as owner from all regions on this world."),
    REGION_ERROR_SEARCH("region.error_search", ChatColor.RED + "Error while searching for %player%'s regions. Please make sure you have entered the correct name."),

    // no tp
    TP_HELP("tp.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no tp [id/player] [num (optional)]"),
    TP_HELP_DESC("tp.help_desc", "Teleports you to one of a given player's regions."),
    NUMBER_ABOVE_ZERO("tp.number_above_zero", ChatColor.RED + "Please enter a number above 0."),
    TP_VALID_NUMBER("tp.valid_number", ChatColor.RED + "Please enter a valid number."),
    ONLY_HAS_REGIONS("tp.only_has_regions", ChatColor.RED + "%player% only has %num% protected regions in this world!"),
    TPING("tp.tping", ChatColor.GREEN + "Teleporting..."),
    TP_ERROR_NAME("tp.error_name", ChatColor.RED + "Error in teleporting to protected region! (parsing WG region name error)"),
    TP_ERROR_TP("tp.error_tp", ChatColor.RED + "Error in finding the region to teleport to!"),
    TP_IN_SECONDS("tp.in_seconds", ChatColor.GRAY + "Teleporting in " + ChatColor.GREEN + "%seconds%" + ChatColor.GRAY + " seconds."),
    TP_CANCELLED_MOVED("tp.cancelled_moved", ChatColor.RED + "Teleport cancelled. You moved!"),

    // no home
    HOME_HELP("home.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no home [name/id]"),
    HOME_HELP_DESC("home.help_desc", "Teleports you to one of your protected regions."),
    HOME_HEADER("home.header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Homes (click to teleport) " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    HOME_CLICK_TO_TP("home.click_to_tp", "Click to teleport!"),
    HOME_NEXT("home.next_page", ChatColor.GRAY + "Do /no home -p %page% to go to the next page!"),

    // no unclaim
    UNCLAIM_HELP("unclaim.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no unclaim"),
    UNCLAIM_HELP_DESC("unclaim.help_desc", "Use this command to pickup a placed protection stone and remove the region."),
    UNCLAIM_HEADER("unclaim.header",ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Unclaim (click to unclaim) " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),

    // no view
    VIEW_HELP("view.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no view"),
    VIEW_HELP_DESC("view.help_desc", "Use this command to view the borders of a protected region."),
    VIEW_COOLDOWN("view.cooldown", ChatColor.RED + "Please wait a while before using /no view again."),
    VIEW_GENERATING("view.generating", ChatColor.GRAY + "Generating border..."),
    VIEW_GENERATE_DONE("view.generate_done", ChatColor.GREEN + "Done! The border will disappear after 30 seconds!"),
    VIEW_REMOVING("view.removing", ChatColor.GREEN + "Removing border...\n" + ChatColor.GREEN + "If you still see ghost blocks, relog!"),

    // no admin
    ADMIN_HELP("admin.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no admin"),
    ADMIN_HELP_DESC("admin.help_desc", "Do /no admin help for more information."),
    ADMIN_CLEANUP_HEADER("admin.cleanup_header", ChatColor.YELLOW + "Cleanup %arg% %days% days\n================"),
    ADMIN_CLEANUP_FOOTER("admin.cleanup_footer", ChatColor.YELLOW + "================\nCompleted %arg% cleanup."),
    ADMIN_HIDE_TOGGLED("admin.hide_toggled", ChatColor.YELLOW + "All protection stones have been %message% in this world."),
    ADMIN_LAST_LOGON("admin.last_logon", ChatColor.YELLOW + "%player% last played %days% days ago."),
    ADMIN_IS_BANNED("admin.is_banned", ChatColor.YELLOW + "%player% is banned."),
    ADMIN_ERROR_PARSING("admin.error_parsing", ChatColor.RED + "Error parsing days, are you sure it is a number?"),
    ADMIN_CONSOLE_WORLD("admin.console_world", ChatColor.RED + "Please specify the world as the last parameter."),
    ADMIN_LASTLOGONS_HEADER("admin.lastlogons_header", ChatColor.YELLOW + "%days% Days Plus:\n================"),
    ADMIN_LASTLOGONS_LINE("admin.lastlogons_line", ChatColor.YELLOW + "%player% %time% days"),
    ADMIN_LASTLOGONS_FOOTER("admin.lastlogons_footer", ChatColor.YELLOW + "================\n%count% Total Players Shown\n%checked% Total Players Checked"),

    // no reload
    RELOAD_HELP("reload.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no reload"),
    RELOAD_HELP_DESC("reload.help_desc", "Reload settings from the config."),
    RELOAD_START("reload.start", ChatColor.GREEN + "Reloading config..."),
    RELOAD_COMPLETE("reload.complete", ChatColor.GREEN + "Completed config reload!"),

    // no add/remove
    ADDREMOVE_HELP("addremove.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no add|remove [playername]"),
    ADDREMOVE_HELP_DESC("addremove.help_desc", "Use this command to add or remove a member of your protected region."),
    ADDREMOVE_OWNER_HELP("addremove.owner_help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no addowner|removeowner [playername]"),
    ADDREMOVE_OWNER_HELP_DESC("addremove.owner_help_desc", "Use this command to add or remove an owner of your protected region."),
    ADDREMOVE_PLAYER_REACHED_LIMIT("addremove.player_reached_limit", ChatColor.RED + "This player has reached their region limit."),
    ADDREMOVE_PLAYER_NEEDS_TO_BE_ONLINE("addremove.player_needs_to_be_online", ChatColor.RED + "The player needs to be online to add them."),

    // no get
    GET_HELP("get.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no get [block]"),
    GET_HELP_DESC("get.help_desc", "Use this command to get or purchase a protection block."),
    GET_GOTTEN("get.gotten", ChatColor.GREEN + "Added protection block to inventory!"),
    GET_NO_PERMISSION_BLOCK("get.no_permission_block", ChatColor.RED + "You don't have permission to get this block."),
    GET_HEADER("get.header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Protect Blocks (click to get) " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    GET_GUI_BLOCK("get.gui_block", ChatColor.GRAY + "> " + ChatColor.GREEN + "%alias% " + ChatColor.GRAY + "- %description% (" + ChatColor.WHITE + "$%price%" + ChatColor.GRAY + ")"),
    GET_GUI_HOVER("get.gui_hover", "Click to buy a %alias%!"),

    // no give
    GIVE_HELP("give.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no give [block] [player] [amount (optional)]"),
    GIVE_HELP_DESC("give.help_desc", "Use this command to give a player a protection block."),
    GIVE_GIVEN("give.given", ChatColor.GRAY + "Gave " + ChatColor.GREEN + "%block%" + ChatColor.GRAY + " to " + ChatColor.GREEN + "%player%" + ChatColor.GRAY + "."),
    GIVE_NO_INVENTORY_ROOM("give.no_inventory_room", ChatColor.RED + "The player does not have enough inventory room."),

    // no sethome
    SETHOME_HELP("sethome.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no sethome"),
    SETHOME_HELP_DESC("sethome.help_desc", "Use this command to set the home of a region to where you are right now."),
    SETHOME_SET("sethome.set", ChatColor.GRAY + "The home for " + ChatColor.GREEN + "%psid%" + ChatColor.GRAY + " has been set to your location."),

    // no list
    LIST_HELP("list.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no list [player (optional)]"),
    LIST_HELP_DESC("list.help_desc", "Use this command to list the regions you, or another player owns."),
    LIST_HEADER("list.header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " %player%'s Regions " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    LIST_OWNER("list.owner", ChatColor.GRAY + "Owner of:"),
    LIST_MEMBER("list.member", ChatColor.GRAY + "Member of:"),
    LIST_NO_REGIONS("list.no_regions", ChatColor.GRAY + "You currently do not own and are not a member of any regions."),
    LIST_NO_REGIONS_PLAYER("list.no_regions_player", ChatColor.GREEN + "%player% " + ChatColor.GRAY + "does not own and is not a member of any regions."),

    // no name
    NAME_HELP("name.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no name [name|none]"),
    NAME_HELP_DESC("name.help_desc", "Use this command to give a nickname to your region, to make identifying your region easier."),
    NAME_REMOVED("name.removed", ChatColor.GRAY + "Removed the name for %id%."),
    NAME_SET_NAME("name.set_name", ChatColor.GRAY + "Set the name of %id% to " + ChatColor.GREEN + "%name%" + ChatColor.GRAY + "."),
    NAME_TAKEN("name.taken", ChatColor.GRAY + "The region name " + ChatColor.GREEN + "%name%" + ChatColor.GRAY + " has already been taken! Try another one."),

    // no setparent

    // no merge
    MERGE_HELP("merge.help", ChatColor.GREEN + "> " + ChatColor.GRAY + "/no merge"),
    MERGE_HELP_DESC("merge.help_desc", "Use this command to merge the region you are in with other overlapping regions."),
    MERGE_DISABLED("merge.disabled", "Merging regions is disabled in the config!"),
    MERGE_MERGED("merge.merged", ChatColor.GREEN + "Regions were successfully merged!"),
    MERGE_HEADER("merge.header", ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "=====" + ChatColor.RESET + " Merge %region% (click to merge) " + ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH + "====="),
    MERGE_WARNING("merge.warning", ChatColor.GRAY + "Note: This will delete all of the settings for the current region!"),
    MERGE_NOT_ALLOWED("merge.not_allowed", ChatColor.RED + "You are not allowed to merge this protection region type."),
    MERGE_INTO("merge.into", ChatColor.GREEN + "This region overlaps other regions you can merge into!"),
    MERGE_NO_REGIONS("merge.no_region", ChatColor.GRAY + "There are no overlapping regions to merge into."),
    MERGE_CLICK_TO_MERGE("merge.click_to_merge", "Click to merge with %region%!"),
    MERGE_AUTO_MERGED("merge.auto_merged", ChatColor.GRAY + "Region automatically merged with " + ChatColor.GREEN + "%region%" + ChatColor.GRAY + "."),

    ;

    private final String path;
    private final String defaultMessage;

    private final String[] placeholders;
    private final int placeholdersCount;
    private String message;
    private boolean isEmpty;

    private static final File conf = new File(Outpost.getInstance().getDataFolder(), "messages.yml");

    OutpostL(String path, String defaultMessage, String... placeholders) {
        this.path = path;
        this.defaultMessage = defaultMessage;

        this.placeholders = placeholders;
        this.placeholdersCount = placeholders.length;
        this.message = defaultMessage;
        this.isEmpty = message.isEmpty();
    }

    public String msg() {
        return message;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    @Nullable
    public String format(final Object... args) {
        if (isEmpty) {
            return null;
        }

        if (this.placeholdersCount == 0) {
            return this.message;
        }

        if (this.placeholdersCount != args.length) {
            throw new IllegalArgumentException("Expected " + this.placeholdersCount + " arguments but got " + args.length);
        }

        return replaceEachSimultaneous(
                this.message,
                this.placeholders,
                Arrays.stream(args).filter(Objects::nonNull).map(Object::toString).toArray(String[]::new)
        );
    }

    /**
     * Native replacement for {@code StringUtils.replaceEach}: replaces every occurrence of
     * {@code search[i]} with {@code replacement[i]} in a single left-to-right pass, so replacement
     * values are never re-scanned for further matches (unlike chained {@link String#replace}).
     */
    private static String replaceEachSimultaneous(String text, String[] search, String[] replacement) {
        if (text == null || search == null || search.length == 0) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        int i = 0;
        outer:
        while (i < text.length()) {
            for (int s = 0; s < search.length; s++) {
                String needle = search[s];
                if (needle != null && !needle.isEmpty() && text.startsWith(needle, i)) {
                    result.append(replacement[s] != null ? replacement[s] : "");
                    i += needle.length();
                    continue outer;
                }
            }
            result.append(text.charAt(i));
            i++;
        }
        return result.toString();
    }

    public boolean send(@NotNull final CommandSender receiver, @NotNull final Object... args) {
        final String msg = this.format(args);

        if (msg != null) {
            receiver.sendMessage(msg);
        }

        return true;
    }

    public void append(@NotNull final StringBuilder builder, @NotNull final Object... args) {
        final String msg = this.format(args);

        if (msg != null) {
            builder.append(msg);
        }
    }

    // Sends a message to a commandsender if the string is not empty

    public static boolean msg(CommandSender p, String str) {
        if (str != null && !str.isEmpty() && p != null) {
            p.sendMessage(str);
        }
        return true;
    }

    public static boolean msg(OutpostPlayer p, String str) {
        return msg(p.getPlayer(), str);
    }

    public static void loadConfig() {
        YamlConfiguration yml = new YamlConfiguration();

        // check if messages.yml exists
        if (!conf.exists()) {
            try {
                conf.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // load config
        try {
            yml.load(conf); // can throw error
            for (OutpostL psl : OutpostL.values()) {

                // fix message if need be
                if (yml.getString(psl.path) == null) { // if msg not found in config
                    yml.set(psl.path, applyConfigColours(psl.defaultMessage));
                } else {
                    // perform message upgrades
                    messageUpgrades(psl, yml);
                }

                // load message
                psl.message = applyInGameColours(yml.getString(psl.path));
                psl.isEmpty = psl.message.isEmpty();
            }
            try {
                yml.save(conf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) { // prevent bad messages.yml file from resetting the file
            e.printStackTrace();
        }
    }

    // message upgrades over time
    private static void messageUpgrades(OutpostL psl, YamlConfiguration yml) {
        String value = yml.getString(psl.path);
        assert(value != null);

        // psl upgrade conversions
        if (psl == OutpostL.REACHED_REGION_LIMIT && value.equals("&cYou can not create any more protected regions.")) {
            yml.set(psl.path, psl.defaultMessage);
        } else if (psl == OutpostL.REACHED_PER_BLOCK_REGION_LIMIT && value.equals("&cYou can not create any more regions of this type.")) {
            yml.set(psl.path, psl.defaultMessage);
        } else if (value.contains("§")) {
            yml.set(psl.path, applyConfigColours(value));
        }
    }

    // match all &#123abc format for hex
    private static final Pattern hexPattern = Pattern.compile("(?<!\\\\\\\\)(&#[a-fA-F0-9]{6})");

    private static String applyInGameColours(String msg) {

        Matcher matcher = hexPattern.matcher(msg);
        while (matcher.find()) {
            String color = msg.substring(matcher.start() + 1, matcher.end());
            msg = msg.replace(msg.substring(matcher.start(), matcher.end()), "" + net.md_5.bungee.api.ChatColor.of(color));
        }

        return msg.replace('&', '§');
    }

    private static String applyConfigColours(String msg) {
        return msg.replace('§', '&');
    }
}
