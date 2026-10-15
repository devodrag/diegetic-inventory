package net.diegetic.inventory.client;

import net.diegetic.inventory.network.DiegeticStateC2SPayload;
import net.diegetic.inventory.network.PeerPlayerState;
import net.diegetic.inventory.network.PeerStateManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Отслеживает открытие стандартного инвентаря игрока (InventoryScreen / CreativeInventoryScreen),
 * перемещение курсора мыши и изменение предметов, транслируя состояние на сервер.
 * Также обновляет локальное состояние, чтобы игрок мог видеть свой 3D-инвентарь в виде от третьего лица (F5).
 */
public class InventorySyncClient {
    private static final InventorySyncClient INSTANCE = new InventorySyncClient();

    private boolean isOpen = false;
    private float cursorU = 0.5f;
    private float cursorV = 0.5f;
    private int hoveredSlot = -1;
    private long lastSyncTime = 0;
    private static final long SYNC_INTERVAL_MS = 50; // 20 обновлений в секунду

    private InventorySyncClient() {}

    public static InventorySyncClient getInstance() {
        return INSTANCE;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void onScreenOpened(int x, int y, int bgWidth, int bgHeight) {
        this.isOpen = true;
        this.cursorU = 0.5f;
        this.cursorV = 0.5f;
        this.hoveredSlot = -1;
        syncState(true);
    }

    public void onScreenClosed() {
        if (!this.isOpen) return;
        this.isOpen = false;
        this.hoveredSlot = -1;
        syncState(true);
    }

    public void onScreenRender(int x, int y, int bgWidth, int bgHeight, Slot focusedSlot, int mouseX, int mouseY) {
        if (!this.isOpen) return;

        int width = (bgWidth > 0) ? bgWidth : 176;
        int height = (bgHeight > 0) ? bgHeight : 166;

        float u = (float) (mouseX - x) / (float) width;
        float v = (float) (mouseY - y) / (float) height;
        this.cursorU = MathHelper.clamp(u, 0.0f, 1.0f);
        this.cursorV = MathHelper.clamp(v, 0.0f, 1.0f);
        this.hoveredSlot = (focusedSlot != null) ? focusedSlot.id : -1;

        long now = System.currentTimeMillis();
        if (now - lastSyncTime >= SYNC_INTERVAL_MS) {
            syncState(false);
            lastSyncTime = now;
        }
    }

    private void syncState(boolean force) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ItemStack cursorStack = ItemStack.EMPTY;
        if (client.player.currentScreenHandler != null) {
            cursorStack = client.player.currentScreenHandler.getCursorStack().copy();
        }

        List<ItemStack> stacks = new ArrayList<>();
        if (client.player.playerScreenHandler != null) {
            for (Slot slot : client.player.playerScreenHandler.slots) {
                stacks.add(slot.getStack().copy());
            }
        }

        // 1. Отправка пакета на сервер для друзей
        if (ClientPlayNetworking.canSend(DiegeticStateC2SPayload.ID)) {
            ClientPlayNetworking.send(new DiegeticStateC2SPayload(
                    this.isOpen,
                    this.cursorU,
                    this.cursorV,
                    this.hoveredSlot,
                    cursorStack,
                    stacks
            ));
        }

        // 2. Обновление локального отображения для F5 (вид от третьего лица)
        PeerPlayerState localState = PeerStateManager.getInstance().getOrCreate(client.player.getUuid());
        localState.update(
                this.isOpen,
                this.cursorU,
                this.cursorV,
                this.hoveredSlot,
                cursorStack,
                stacks
        );
    }
}
