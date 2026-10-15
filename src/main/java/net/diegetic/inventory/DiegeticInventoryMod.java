package net.diegetic.inventory;

import net.diegetic.inventory.network.DiegeticStateC2SPayload;
import net.diegetic.inventory.network.DiegeticSyncS2CPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Общая точка входа мода (серверная и клиентская регистрация сети).
 */
public class DiegeticInventoryMod implements ModInitializer {
    public static final String MOD_ID = "diegetic_inventory";

    @Override
    public void onInitialize() {
        // Регистрация типов сетевых пакетов для Fabric Networking API
        PayloadTypeRegistry.playC2S().register(DiegeticStateC2SPayload.ID, DiegeticStateC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DiegeticSyncS2CPayload.ID, DiegeticSyncS2CPayload.CODEC);

        // Обработка пакета от клиента на сервере: ретрансляция всем игрокам в радиусе видимости (друзьям)
        ServerPlayNetworking.registerGlobalReceiver(DiegeticStateC2SPayload.ID, (payload, context) -> {
            ServerPlayerEntity sender = context.player();

            DiegeticSyncS2CPayload syncPayload = new DiegeticSyncS2CPayload(
                    sender.getUuid(),
                    payload.isOpen(),
                    payload.cursorU(),
                    payload.cursorV(),
                    payload.hoveredSlot(),
                    payload.cursorStack(),
                    payload.stacks()
            );

            // Транслируем всем игрокам, которые видят данного персонажа в мире
            for (ServerPlayerEntity trackingPlayer : PlayerLookup.tracking(sender)) {
                ServerPlayNetworking.send(trackingPlayer, syncPayload);
            }
        });
    }
}
