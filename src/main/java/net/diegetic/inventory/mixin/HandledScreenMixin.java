package net.diegetic.inventory.mixin;

import net.diegetic.inventory.client.InventorySyncClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Перехватывает открытие, закрытие и рендер стандартного экрана инвентаря игрока
 * (InventoryScreen и CreativeInventoryScreen) для передачи координат курсора и слотов в 3D-мир.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;
    @Shadow protected Slot focusedSlot;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Unique
    private boolean isPlayerInventoryScreen() {
        return ((Object) this instanceof InventoryScreen) || ((Object) this instanceof CreativeInventoryScreen);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (isPlayerInventoryScreen()) {
            InventorySyncClient.getInstance().onScreenOpened(this.x, this.y, this.backgroundWidth, this.backgroundHeight);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        if (isPlayerInventoryScreen()) {
            InventorySyncClient.getInstance().onScreenClosed();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (isPlayerInventoryScreen()) {
            InventorySyncClient.getInstance().onScreenRender(
                    this.x, this.y, this.backgroundWidth, this.backgroundHeight,
                    this.focusedSlot, mouseX, mouseY
            );
        }
    }
}
