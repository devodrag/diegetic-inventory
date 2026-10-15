package net.diegetic.inventory.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Пакет C2S: отправляет серверу текущее состояние инвентаря игрока:
 * открыт ли экран, нормализованные координаты курсора (cursorU, cursorV),
 * наведенный слот, предмет на курсоре и список всех предметов в слотах.
 */
public record DiegeticStateC2SPayload(
        boolean isOpen,
        float cursorU,
        float cursorV,
        int hoveredSlot,
        ItemStack cursorStack,
        List<ItemStack> stacks
) implements CustomPayload {

    public static final CustomPayload.Id<DiegeticStateC2SPayload> ID =
            new CustomPayload.Id<>(Identifier.of("diegetic_inventory", "state_c2s"));

    public static final PacketCodec<RegistryByteBuf, DiegeticStateC2SPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeBoolean(value.isOpen);
                buf.writeFloat(value.cursorU);
                buf.writeFloat(value.cursorV);
                buf.writeInt(value.hoveredSlot);
                ItemStack.OPTIONAL_PACKET_CODEC.encode(buf, value.cursorStack);
                ItemStack.OPTIONAL_LIST_PACKET_CODEC.encode(buf, value.stacks);
            },
            buf -> new DiegeticStateC2SPayload(
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
