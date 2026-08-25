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

package dev.noellx.outpost.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import dev.noellx.outpost.OutpostRegion;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event that is called when a Outpost region is created, either by a player, or by the plugin.
 */

public class OutpostCreateEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private OutpostRegion region;
    private Player p = null;
    private boolean isCancelled = false;

    public OutpostCreateEvent(OutpostRegion psr, Player player) {
        this.region = checkNotNull(psr);
        this.p = player;
    }

    public OutpostCreateEvent(OutpostRegion psr) {
        this.region = checkNotNull(psr);
    }

    /**
     * Returns the player that created the protection region, if applicable
     * @return the player, or null if the region was not created because of a player
     */
    public Player getPlayer() {
        return p;
    }

    /**
     * Returns the region being created.
     * @return the region being created
     */
    public OutpostRegion getRegion() {
        return region;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
