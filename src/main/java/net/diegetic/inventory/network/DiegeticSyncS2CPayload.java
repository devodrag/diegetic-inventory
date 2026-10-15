package net.diegetic.inventory.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;

/**
 * Пакет S2C: сервер транслирует состояние инвентаря игрока всем наблюдателям (друзьям рядом).
 */
public record DiegeticSyncS2CPayload(
        UUID playerUuid,
        boolean isOpen,
        float cursorU,
        float cursorV,
        int hoveredSlot,
        ItemStack cursorStack,
        List<ItemStack> stacks
) implements CustomPayload {

    public static final CustomPayload.Id<DiegeticSyncS2CPayload> ID =
            new CustomPayload.Id<>(Identifier.of("diegetic_inventory", "sync_s2c"));

    public static final PacketCodec<RegistryByteBuf, DiegeticSyncS2CPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.playerUuid);
                buf.writeBoolean(value.isOpen);
                buf.writeFloat(value.cursorU);
                buf.writeFloat(value.cursorV);
                buf.writeInt(value.hoveredSlot);
                ItemStack.OPTIONAL_PACKET_CODEC.encode(buf, value.cursorStack);
                ItemStack.OPTIONAL_LIST_PACKET_CODEC.encode(buf, value.stacks);
            },
            buf -> new DiegeticSyncS2CPayload(
                    buf.readUuid(),
                    buf.readBoolean(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readInt(),
                    ItemStack.OPTIONAL_PACKET_CODEC.decode(buf),
                    ItemStack.OPTIONAL_LIST_PACKET_CODEC.decode(buf)
            )
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
