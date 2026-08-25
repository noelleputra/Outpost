package dev.noellx.outpost.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import dev.noellx.outpost.OutpostRegion;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event that is called when a protection stones block is removed
 */
public class OutpostBreakProtectBlockEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private OutpostRegion region;
    private Player player;
    private boolean isCancelled = false;

    public OutpostBreakProtectBlockEvent(OutpostRegion psr, Player player) {
        this.region = checkNotNull(psr);
        this.player = player;
    }

    /**
     * Gets the player who triggered the event.
     *
     * @return The player.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the NullaeOutpost item associated with the region.
     *
     * @return The NullaeOutpost item.
     */
    public ItemStack getNOItem() {
        return Objects.requireNonNull(region.getTypeOptions()).createItem();
    }

    /**
     * Gets the NullaeOutpost region associated with the event.
     *
     * @return The NullaeOutpost region.
     */
    public OutpostRegion getRegion() {
        return region;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        isCancelled = cancel;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
