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

package dev.noellx.outpost.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import dev.noellx.outpost.NOProtectBlock;
import dev.noellx.outpost.NullaeOutpost;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockUtil {
    static final int MAX_USERNAME_LENGTH = 16;
    public static HashMap<String, String> uuidToBase64Head = new HashMap<>();

    public static ItemStack getProtectBlockItemFromType(String type) {
        if (type.startsWith(Material.PLAYER_HEAD.toString())) {
            return new ItemStack(Material.PLAYER_HEAD);
        } else {
            return new ItemStack(Material.getMaterial(type));
        }
    }

    // used for preventing unnecessary calls to .getOwningPlayer() which could cause server freezes
    private static boolean isOwnedSkullTypeConfigured() {
        for (NOProtectBlock b : NullaeOutpost.getInstance().getConfiguredBlocks()) {
            if (b.type.startsWith("PLAYER_HEAD:")) {
                return true;
            }
        }
        return false;
    }

    public static String getProtectBlockType(ItemStack i) {
        if (i.getType() == Material.PLAYER_HEAD || i.getType() == Material.LEGACY_SKULL_ITEM) {
            SkullMeta sm = (SkullMeta) i.getItemMeta();

            // PLAYER_HEAD
            if (!sm.hasOwner() || !isOwnedSkullTypeConfigured()) {
                return Material.PLAYER_HEAD.toString();
            }

            // PLAYER_HEAD:base64
            PlayerProfile offlineProfile = sm.getOwnerProfile();
            if (offlineProfile == null) {
                return Material.PLAYER_HEAD.toString();
            }
            if (NullaeOutpost.getBlockOptions("PLAYER_HEAD:" + offlineProfile.getUniqueId()) != null) {
                return Material.PLAYER_HEAD + ":" + offlineProfile.getUniqueId();
            }

            // PLAYER_HEAD:name
            return Material.PLAYER_HEAD + ":" + offlineProfile.getName();
        }
        return i.getType().toString();
    }

    public static String getProtectBlockType(Block block) {
        if (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {

            Skull s = (Skull) block.getState();
            if (s.hasOwner() && isOwnedSkullTypeConfigured()) {
                PlayerProfile offlineProfile = s.getOwnerProfile();
                if (offlineProfile == null) {
                    return Material.PLAYER_HEAD.toString();
                }
                if (NullaeOutpost.getBlockOptions("PLAYER_HEAD:" + offlineProfile.getUniqueId()) != null) {
                    return Material.PLAYER_HEAD + ":" + offlineProfile.getUniqueId();
                }
                return Material.PLAYER_HEAD + ":" + offlineProfile.getName();
            }
            return Material.PLAYER_HEAD.toString();
        } else if (block.getType() == Material.CREEPER_WALL_HEAD) {
            return Material.CREEPER_HEAD.toString();
        } else if (block.getType() == Material.DRAGON_WALL_HEAD) {
            return Material.DRAGON_HEAD.toString();
        } else if (block.getType() == Material.ZOMBIE_WALL_HEAD) {
            return Material.ZOMBIE_HEAD.toString();
        } else if (block.getType() == Material.SKELETON_WALL_SKULL) {
            return Material.SKELETON_SKULL.toString();
        } else if (block.getType() == Material.WITHER_SKELETON_WALL_SKULL) {
            return Material.WITHER_SKELETON_SKULL.toString();
        } else {
            return block.getType().toString();
        }
    }

    public static void setHeadType(String psType, Block b) {
        if (psType.split(":").length < 2) return;
        String name = psType.split(":")[1];
        if (name.length() > MAX_USERNAME_LENGTH) {
            blockWithBase64(b, name);
        } else {
            OfflinePlayer op = Bukkit.getOfflinePlayer(psType.split(":")[1]);
            Skull s = (Skull) b.getState();
            s.setOwningPlayer(op);
            s.update();
        }
    }

    public static PlayerProfile getProfile(String uuid, String base64) {
        String name = uuid.substring(0, 16);
        PlayerProfile profile = Bukkit.getServer().createPlayerProfile(UUID.fromString(uuid), name);
        PlayerTextures textures = profile.getTextures();

        // decode base64 to URL
        byte[] decodedBytes = Base64.getDecoder().decode(base64);
        String decodedString = new String(decodedBytes);

        // read decoded string as JSON object (targeted regex instead of a full JSON parser,
        // since this payload always has the fixed shape below)
        // sample: {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/..."}}}
        Matcher matcher = Pattern.compile("\"SKIN\"\\s*:\\s*\\{[^}]*\"url\"\\s*:\\s*\"([^\"]+)\"").matcher(decodedString);
        if (!matcher.find()) {
            throw new RuntimeException("Invalid JSON retrieved from base64 " + decodedString);
        }
        String url = matcher.group(1);

        URL urlObject;
        try {
            urlObject = new URL(url);
        } catch (MalformedURLException exception) {
            throw new RuntimeException("Invalid decoded URL from head data: " + url, exception);
        }

        textures.setSkin(urlObject);
        profile.setTextures(textures);
        return profile;
    }

    public static ItemStack setHeadType(String psType, ItemStack item) {
        String name = psType.split(":")[1];
        if (name.length() > MAX_USERNAME_LENGTH) { // base 64 head
            String uuid = name;

            // decode base64 to URL
            String base64 = uuidToBase64Head.get(name);
            PlayerProfile profile = getProfile(uuid, base64);

            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwnerProfile(profile);
            item.setItemMeta(meta);

            return item;
        } else { // normal name head
            SkullMeta sm = (SkullMeta) item.getItemMeta();
            sm.setOwningPlayer(Bukkit.getOfflinePlayer(name));
            item.setItemMeta(sm);
            return item;
        }
    }

    private static void blockWithBase64(Block block, String uuid) {
        String base64 = uuidToBase64Head.get(uuid);
        PlayerProfile profile = getProfile(uuid, base64);

        Skull skull = (Skull) block.getState();
        skull.setOwnerProfile(profile);
        skull.update(false);
    }

    public static boolean isBase64NOHead(String type) {
        return type.startsWith("PLAYER_HEAD") && type.split(":").length > 1 && type.split(":")[1].length() > MAX_USERNAME_LENGTH;
    }

    public static String getUUIDFromBase64NO(NOProtectBlock b) {
        String base64 = b.type.split(":")[1];
        // return UUID.nameUUIDFromBytes(base64.getBytes()).toString(); <- I should be using this

        // the below is bad, because hashcode should really not be used... unfortunately, this is used in production so it will have to stay like this
        // until I can find a way to convert items to the new uuid
        // see github issue #126
        return new UUID(base64.hashCode(), base64.hashCode()).toString();
    }
}
