package net.diegetic.inventory.network;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Хранит текущее состояние интерфейса инвентаря удаленного игрока (друга).
 * Включает интерполяцию движения курсора, наведенный слот, предмет на мыши и все предметы в слотах.
 */
public class PeerPlayerState {
    private final UUID playerUuid;
    private boolean open;
    private float prevCursorU = 0.5f;
    private float prevCursorV = 0.5f;
    private float currentCursorU = 0.5f;
    private float currentCursorV = 0.5f;
    private float targetCursorU = 0.5f;
    private float targetCursorV = 0.5f;
    private int hoveredSlot = -1;
    private ItemStack cursorStack = ItemStack.EMPTY;
    private List<ItemStack> stacks = new ArrayList<>();

    public PeerPlayerState(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public void update(boolean open, float cursorU, float cursorV, int hoveredSlot, ItemStack cursorStack, List<ItemStack> stacks) {
        this.open = open;
        this.targetCursorU = cursorU;
        this.targetCursorV = cursorV;
        this.hoveredSlot = hoveredSlot;
        this.cursorStack = (cursorStack != null) ? cursorStack.copy() : ItemStack.EMPTY;
        this.stacks = (stacks != null) ? new ArrayList<>(stacks) : new ArrayList<>();
    }

    /**
     * Тиковое обновление для плавной интерполяции курсора.
     */
    public void tick() {
        this.prevCursorU = this.currentCursorU;
        this.prevCursorV = this.currentCursorV;
        this.currentCursorU = MathHelper.lerp(0.4f, this.currentCursorU, this.targetCursorU);
        this.currentCursorV = MathHelper.lerp(0.4f, this.currentCursorV, this.targetCursorV);
    }

    public float getLerpedCursorU(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevCursorU, this.currentCursorU);
    }

    public float getLerpedCursorV(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevCursorV, this.currentCursorV);
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public boolean isOpen() {
        return open;
    }

    public int getHoveredSlot() {
        return hoveredSlot;
    }

    public ItemStack getCursorStack() {
        return cursorStack;
    }

    public List<ItemStack> getStacks() {
        return stacks;
    }
}
