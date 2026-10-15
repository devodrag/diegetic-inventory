package net.diegetic.inventory.client;

import net.diegetic.inventory.client.render.WorldInventoryRenderer;
import net.diegetic.inventory.network.DiegeticSyncS2CPayload;
import net.diegetic.inventory.network.PeerPlayerState;
import net.diegetic.inventory.network.PeerStateManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;

/**
 * Клиентская точка входа мода:
 * 1. Принимает сетевые пакеты синхронизации инвентарей игроков.
 * 2. Отрисовывает 3D-панель ванильного инвентаря перед персонажами в мире (для друзей и в режиме F5).
 * 3. Обновляет плавную интерполяцию курсора каждый тик.
 */
public class DiegeticInventoryClient implements ClientModInitializer {
    public static final String MOD_ID = "diegetic_inventory";

    @Override
    public void onInitializeClient() {
        // 1. Прием сетевого пакета от сервера: обновление состояния инвентаря друга
        ClientPlayNetworking.registerGlobalReceiver(DiegeticSyncS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                PeerPlayerState state = PeerStateManager.getInstance().getOrCreate(payload.playerUuid());
                state.update(
                        payload.isOpen(),
                        payload.cursorU(),
                        payload.cursorV(),
                        payload.hoveredSlot(),
                        payload.cursorStack(),
                        payload.stacks()
                );
            });
        });

        // 2. Отрисовка 3D-инвентаря перед игроками в мире
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (context.world() == null || context.camera() == null) return;

            MinecraftClient client = MinecraftClient.getInstance();
            float tickDelta = (context.tickCounter() != null) ? context.tickCounter().getTickDelta(false) : 1.0f;

            for (AbstractClientPlayerEntity peer : context.world().getPlayers()) {
                // В первом лице игрок пользуется своим 2D экраном, не перекрывая обзор в мире.
                // В третьем лице (F5) или для других игроков (друзей) 3D-инвентарь рендерится перед персонажем!
                if (peer == client.player && client.options.getPerspective().isFirstPerson()) {
                    continue;
                }

                PeerPlayerState peerState = PeerStateManager.getInstance().getPeer(peer.getUuid());
                if (peerState != null && peerState.isOpen()) {
                    WorldInventoryRenderer.getInstance().renderPlayerInventory(
                            context.matrixStack(),
                            context.consumers(),
                            context.camera(),
                            peer,
                            peerState,
                            tickDelta
                    );
                }
            }
        });

        // 3. Плавная интерполяция движения курсоров других игроков
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PeerStateManager.getInstance().tick();
        });
    }
}
