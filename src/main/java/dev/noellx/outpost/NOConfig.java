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

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.conversion.Path;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import dev.noellx.outpost.utils.BlockUtil;
import dev.noellx.outpost.utils.RecipeUtil;
import dev.noellx.outpost.utils.upgrade.ConfigUpgrades;
import dev.noellx.outpost.utils.upgrade.LegacyUpgrade;

import org.bukkit.Material;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the global config (config.toml) settings.
 */

public class NOConfig {

    // config options
    // config.toml will be loaded into these fields
    // uses autoboxing for primitives to allow for null (because of the way config upgrades work)
    @Path("config_version")
    public int configVersion;
    @Path("uuidupdated")
    public Boolean uuidupdated;
    @Path("region_negative_min_max_updated")
    public Boolean regionNegativeMinMaxUpdated;
    @Path("placing_cooldown")
    public Integer placingCooldown;
    @Path("allow_duplicate_region_names")
    public Boolean allowDuplicateRegionNames;
    @Path("async_load_uuid_cache")
    public Boolean asyncLoadUUIDCache;
    @Path("ps_view_cooldown")
    public Integer psViewCooldown;
    @Path("base_command")
    public String base_command;
    @Path("aliases")
    public List<String> aliases;
    @Path("drop_item_when_inventory_full")
    public Boolean dropItemWhenInventoryFull;
    @Path("regions_must_be_adjacent")
    public Boolean regionsMustBeAdjacent;
    @Path("allow_merging_regions")
    public Boolean allowMergingRegions;
    @Path("allow_merging_holes")
    public Boolean allowMergingHoles;
    @Path("default_protection_block_placement_off")
    public Boolean defaultProtectionBlockPlacementOff;
    @Path("allow_addowner_for_offline_players_without_lp")
    public Boolean allowAddownerForOfflinePlayersWithoutLp;
    @Path("allow_home_teleport_for_members")
    public Boolean allowHomeTeleportForMembers;

    @Path("admin.cleanup_delete_regions_with_members_but_no_owners")
    public Boolean cleanupDeleteRegionsWithMembersButNoOwners;

    static void initConfig() {

        // check if using config v1 or v2 (config.yml -> config.toml)
        if (new File(NullaeOutpost.getInstance().getDataFolder() + "/config.yml").exists() && !NullaeOutpost.configLocation.exists()) {
            LegacyUpgrade.upgradeFromV1V2();
        }

        // check if config files exist
        try {
            if (!NullaeOutpost.getInstance().getDataFolder().exists()) {
                NullaeOutpost.getInstance().getDataFolder().mkdir();
            }
            if (!NullaeOutpost.blockDataFolder.exists()) {
                NullaeOutpost.blockDataFolder.mkdir();
                Files.copy(NOConfig.class.getResourceAsStream("/outpost.toml"), Paths.get(NullaeOutpost.blockDataFolder.getAbsolutePath() + "/outpost.toml"), StandardCopyOption.REPLACE_EXISTING);
            }
            if (!NullaeOutpost.configLocation.exists()) {
                Files.copy(NOConfig.class.getResourceAsStream("/config.toml"), Paths.get(NullaeOutpost.configLocation.toURI()), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            Logger.getLogger(NullaeOutpost.class.getName()).log(Level.SEVERE, null, ex);
        }

        // keep in mind that there is /no reload, so clear arrays before adding config options!
        // clear data (for /no reload)
        NullaeOutpost.NullaeOutpostOptions.clear();

        // create config object
        if (NullaeOutpost.config == null) {
            NullaeOutpost.config = CommentedFileConfig.builder(NullaeOutpost.configLocation).sync().build();
        }

        // loop upgrades until the config has been updated to the latest version
        do {
            NullaeOutpost.config.load(); // load latest settings

            // load config into configOptions object
            NullaeOutpost.getInstance().setConfigOptions(new ObjectConverter().toObject(NullaeOutpost.config, NOConfig::new));

            // upgrade config if need be (v3+)
            boolean leaveLoop = ConfigUpgrades.doConfigUpgrades();
            if (leaveLoop) break; // leave loop if config version is correct

            // save config if upgrading
            NullaeOutpost.config.save();
        } while (true);

        // load protection stones to options map
        if (NullaeOutpost.blockDataFolder.listFiles().length == 0) {
            NullaeOutpost.getPluginLogger().warning("The blocks folder is empty! You do not have any protection blocks configured!");
        } else {

            // temp file to load in default no block config
            File tempFile;
            try (InputStream in = NOConfig.class.getResourceAsStream("/outpost.toml")) {
                if (in == null) {
                    NullaeOutpost.getPluginLogger().severe("Could not find /outpost.toml inside the plugin jar! " +
                            "This usually means the jar was not built correctly (missing resources) - try running " +
                            "'mvn clean package' and make sure src/main/resources/outpost.toml exists before building.");
                    return;
                }
                tempFile = File.createTempFile("psconfigtemp", ".toml");
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    in.transferTo(out);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            CommentedFileConfig template = CommentedFileConfig.of(tempFile);
            template.load();

            // iterate over block files and load into map
            NullaeOutpost.getPluginLogger().info("Protection Stone Blocks:");
            for (File file : NullaeOutpost.blockDataFolder.listFiles()) {

                CommentedFileConfig c = CommentedFileConfig.builder(file).sync().build();
                c.load();

                // check to make sure all options are not null
                boolean updated = false;
                for (String str : template.valueMap().keySet()) {
                    if (c.get(str) == null) {
                        c.set(str, template.get(str));
                        c.setComment(str, template.getComment(str));
                        updated = true;
                    } else if (c.get(str) instanceof CommentedConfig) {
                        // no DFS for now (since there's only 2 layers of config)
                        CommentedConfig template2 = template.get(str);
                        CommentedConfig c2 = c.get(str);
                        for (String str2 : template2.valueMap().keySet()) {
                            if (c2.get(str2) == null) {
                                c2.add(str2, template2.get(str2));
                                c2.setComment(str2, template2.getComment(str2));
                                updated = true;
                            }
                        }
                    }
                }
                if (updated) c.save();

                // convert toml data into object
                NOProtectBlock b = new ObjectConverter().toObject(c, NOProtectBlock::new);

                // check if material is valid, and is not a player head (since player heads also have the player name after)
                if (Material.getMaterial(b.type) == null && !(b.type.startsWith(Material.PLAYER_HEAD.toString()))) {
                    NullaeOutpost.getPluginLogger().warning("Unrecognized material: " + b.type);
                    NullaeOutpost.getPluginLogger().warning("Block will not be added. Please fix this in your config.");
                    continue;
                }

                // check for duplicates
                if (NullaeOutpost.isProtectBlockType(b.type)) {
                    NullaeOutpost.getPluginLogger().warning("Duplicate block type found! Ignoring the extra block " + b.type);
                    continue;
                }
                if (NullaeOutpost.getProtectBlockFromAlias(b.alias) != null) {
                    NullaeOutpost.getPluginLogger().warning("Duplicate block alias found! Ignoring the extra block " + b.alias);
                    continue;
                }

                NullaeOutpost.getPluginLogger().info("- " + b.type + " (" + b.alias + ")");
                FlagHandler.initDefaultFlagsForBlock(b); // process flags for block and set regionFlags field

                // for PLAYER_HEAD:base64, we need to change the entry to link to a UUID hash instead of storing the giant base64
                if (BlockUtil.isBase64NOHead(b.type)) {
                    String nuuid = BlockUtil.getUUIDFromBase64NO(b);

                    BlockUtil.uuidToBase64Head.put(nuuid, b.type.split(":")[1]);
                    b.type = "PLAYER_HEAD:" + nuuid;
                }

                NullaeOutpost.NullaeOutpostOptions.put(b.type, b); // add block
            }

            // cleanup temp file
            template.close();
            tempFile.delete();

            // setup crafting recipes for all blocks
            RecipeUtil.setupNORecipes();
        }
    }
}
