package net.diegetic.inventory.client.render;

import net.diegetic.inventory.network.PeerPlayerState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Отрисовывает 2D-панель классического инвентаря игрока (176x166) сзади персонажа (как было изначально).
 * - Панель расположена сзади игрока (180 - bodyYaw, смещение 0.50м, наклон 15 градусов).
 * - Никаких лагов: ресурсоемкий 3D-скин полностью удален.
 * - Все слои монолитны: микронные смещения Z (0.03 - 0.20), нет отслоения в воздухе.
 * - Статичные плоские 2D-иконки предметов.
 * - Текст и числа стаков гарантированно отрисовываются благодаря вызову immediate.draw()!
 */
public class WorldInventoryRenderer {
    private static final WorldInventoryRenderer INSTANCE = new WorldInventoryRenderer();

    public static final Identifier INVENTORY_TEXTURE = Identifier.of("minecraft", "textures/gui/container/inventory.png");
    private static final float GUI_WIDTH = 176.0f;
    private static final float GUI_HEIGHT = 166.0f;
    private static final float WORLD_SCALE = 0.0050f; // Размер в мире: ~0.88м на ~0.83м

    private WorldInventoryRenderer() {}

    public static WorldInventoryRenderer getInstance() {
        return INSTANCE;
    }

    public void renderPlayerInventory(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Camera camera,
            AbstractClientPlayerEntity player,
            PeerPlayerState state,
            float tickDelta
    ) {
        if (!state.isOpen()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ItemRenderer itemRenderer = client.getItemRenderer();
        TextRenderer textRenderer = client.textRenderer;

        // 1. Позиционирование инвентаря: СЗАДИ персонажа (исходная полюбившаяся позиция)
        double playerX = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double playerY = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double playerZ = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

        double camX = camera.getPos().x;
        double camY = camera.getPos().y;
        double camZ = camera.getPos().z;

        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float chestY = (float) (playerY + player.getStandingEyeHeight() * 0.65 - camY);

        matrices.push();
        matrices.translate(playerX - camX, chestY, playerZ - camZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
        // Смещение назад от персонажа
        matrices.translate(0.0, 0.0, 0.50);
        // Классический легкий наклон вверх на 15 градусов
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0f));

        // Масштабирование из пикселей GUI (176x166) в метры мира
        matrices.scale(WORLD_SCALE, -WORLD_SCALE, WORLD_SCALE);
        matrices.translate(-GUI_WIDTH / 2.0f, -GUI_HEIGHT / 2.0f, 0.0f);

        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        // 2. Тонкая подложка сзади (Z = -0.05)
        renderBackplate(matrices, consumers, light);

        // 3. Текстура ванильного инвентаря Minecraft (176x166) на Z = 0.0
        renderInventoryTexture(posMatrix, consumers, light);
        flush(consumers);

        // 4. Окно аватара персонажа (скин игрока в окошке инвентаря)
        renderPlayerAvatar(matrices, consumers, player, light);

        // 5. Подсветка наведенной ячейки (Z = 0.03)
        int hovered = state.getHoveredSlot();
        if (hovered >= 0 && hovered <= 45) {
            int hx = getSlotX(hovered);
            int hy = getSlotY(hovered);
            renderHoverQuad(posMatrix, consumers, hx, hy, light);
            flush(consumers);
        }

        List<ItemStack> stacks = state.getStacks();

        // 5. ПРОХОД 1: Отрисовка плоских статичных иконок предметов (Z = 0.06)
        for (int slotId = 0; slotId <= 45; slotId++) {
            if (slotId >= stacks.size()) break;

            ItemStack stack = stacks.get(slotId);
            if (stack == null || stack.isEmpty()) continue;

            int slotX = getSlotX(slotId);
            int slotY = getSlotY(slotId);

            matrices.push();
            matrices.translate(slotX + 8.0f, slotY + 8.0f, 0.06f);
            matrices.scale(16.0f, -16.0f, 0.0001f); // Полное уплощение в плоскую 2D иконку
            itemRenderer.renderItem(
                    stack,
                    ModelTransformationMode.GUI,
                    light,
                    OverlayTexture.DEFAULT_UV,
                    matrices,
                    consumers,
                    player.getWorld(),
                    0
            );
            matrices.pop();

            // Полоска прочности (Z = 0.10)
            if (stack.isItemBarVisible()) {
                int barStep = stack.getItemBarStep();
                int barColor = stack.getItemBarColor();
                renderItemBar(matrices, consumers, slotX + 2, slotY + 13, barStep, barColor, light);
            }
        }
        flush(consumers);

        // 6. ПРОХОД 2: Отрисовка текста и количества предметов СТРОГО ПОВЕРХ ВСЕГО (Z = 0.15)
        // 6.1 Имя игрока сверху над инвентарем
        Text nameText = player.getDisplayName();
        int nameWidth = textRenderer.getWidth(nameText);
        matrices.push();
        matrices.translate(0.0f, 0.0f, 0.15f);
        textRenderer.draw(
                nameText,
                (GUI_WIDTH - nameWidth) / 2.0f,
                -12.0f,
                0xFFFFFFFF,
                true,
                matrices.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                light
        );
        matrices.pop();

        // 6.2 Количество предметов в слотах (>1)
        for (int slotId = 0; slotId <= 45; slotId++) {
            if (slotId >= stacks.size()) break;

            ItemStack stack = stacks.get(slotId);
            if (stack == null || stack.isEmpty()) continue;

            if (stack.getCount() > 1) {
                int slotX = getSlotX(slotId);
                int slotY = getSlotY(slotId);

                String countStr = String.valueOf(stack.getCount());
                int countWidth = textRenderer.getWidth(countStr);

                matrices.push();
                matrices.translate(0.0f, 0.0f, 0.15f);
                textRenderer.draw(
                        countStr,
                        slotX + 17.0f - countWidth,
                        slotY + 9.0f,
                        0xFFFFFFFF,
                        true, // Четкая черная тень Minecraft
                        matrices.peek().getPositionMatrix(),
                        consumers,
                        TextRenderer.TextLayerType.NORMAL,
                        0,
                        light
                );
                matrices.pop();
            }
        }
        // ВАЖНО: Принудительный сброс буфера текста на видеокарту!
        flush(consumers);

        // 7. Курсор мыши и перетаскиваемый предмет (Z = 0.20 - 0.25)
        float cursorU = state.getLerpedCursorU(tickDelta);
        float cursorV = state.getLerpedCursorV(tickDelta);
        float cursorPixelX = cursorU * GUI_WIDTH;
        float cursorPixelY = cursorV * GUI_HEIGHT;

        renderCursor(matrices, consumers, cursorPixelX, cursorPixelY, light);

        ItemStack cursorStack = state.getCursorStack();
        if (cursorStack != null && !cursorStack.isEmpty()) {
            matrices.push();
            matrices.translate(cursorPixelX + 8.0f, cursorPixelY + 8.0f, 0.25f);
            matrices.scale(16.0f, -16.0f, 0.0001f);
            itemRenderer.renderItem(
                    cursorStack,
                    ModelTransformationMode.GUI,
                    light,
                    OverlayTexture.DEFAULT_UV,
                    matrices,
                    consumers,
                    player.getWorld(),
                    0
            );
            matrices.pop();

            if (cursorStack.getCount() > 1) {
                String countStr = String.valueOf(cursorStack.getCount());
                int countWidth = textRenderer.getWidth(countStr);
                matrices.push();
                matrices.translate(0.0f, 0.0f, 0.30f);
                textRenderer.draw(
                        countStr,
                        cursorPixelX + 17.0f - countWidth,
                        cursorPixelY + 9.0f,
                        0xFFFFFFFF,
                        true,
                        matrices.peek().getPositionMatrix(),
                        consumers,
                        TextRenderer.TextLayerType.NORMAL,
                        0,
                        light
                );
                matrices.pop();
            }
        }
        flush(consumers);

        matrices.pop();
    }

    /**
     * Отрисовывает полностью плоский, статичный 2D-спрайт скина игрока (как в классическом 2D-интерфейсе).
     * Без 3D-моделей, без нагрузки, с идеальным качеством базового слоя и оверлеев (шапка, куртка, штаны, рукава).
     */
    private void renderPlayerAvatar(
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            AbstractClientPlayerEntity player,
            int light
    ) {
        SkinTextures skinTextures = player.getSkinTextures();
        Identifier skinTexture = skinTextures.texture();
        boolean slim = skinTextures.model() == SkinTextures.Model.SLIM;

        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityTranslucent(skinTexture));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float scale = 1.85f;
        float armWidth = slim ? (3.0f * scale) : (4.0f * scale);
        float armUWidth = slim ? (3.0f / 64.0f) : (4.0f / 64.0f);
        float torsoWidth = 8.0f * scale;
        float torsoHeight = 12.0f * scale;
        float headSize = 8.0f * scale;
        float legWidth = 4.0f * scale;
        float legHeight = 12.0f * scale;

        // Центрирование в окне аватара (X: 26..75, Y: 8..78)
        float headX = 51.0f - (torsoWidth / 2.0f);
        float headY = 13.4f;

        float torsoX = headX;
        float torsoY = headY + headSize;

        float rightArmX = torsoX - armWidth;
        float rightArmY = torsoY;

        float leftArmX = torsoX + torsoWidth;
        float leftArmY = torsoY;

        float rightLegX = torsoX;
        float rightLegY = torsoY + torsoHeight;

        float leftLegX = torsoX + legWidth;
        float leftLegY = torsoY + torsoHeight;

        float zBase = 0.04f;
        float zOverlay = 0.05f;

        // 1. БАЗОВЫЙ СЛОЙ (Лицо, тело, руки, ноги)
        // Голова (лицо)
        drawTexturedQuad(matrix, buffer, headX, headY, headSize, headSize,
                8.0f / 64.0f, 8.0f / 64.0f, 16.0f / 64.0f, 16.0f / 64.0f, zBase, light);

        // Туловище
        drawTexturedQuad(matrix, buffer, torsoX, torsoY, torsoWidth, torsoHeight,
                20.0f / 64.0f, 20.0f / 64.0f, 28.0f / 64.0f, 32.0f / 64.0f, zBase, light);

        // Правая рука (со стороны зрителя - слева)
        drawTexturedQuad(matrix, buffer, rightArmX, rightArmY, armWidth, torsoHeight,
                44.0f / 64.0f, 20.0f / 64.0f, (44.0f / 64.0f) + armUWidth, 32.0f / 64.0f, zBase, light);

        // Левая рука (со стороны зрителя - справа)
        drawTexturedQuad(matrix, buffer, leftArmX, leftArmY, armWidth, torsoHeight,
                36.0f / 64.0f, 52.0f / 64.0f, (36.0f / 64.0f) + armUWidth, 64.0f / 64.0f, zBase, light);

        // Правая нога (со стороны зрителя - слева)
        drawTexturedQuad(matrix, buffer, rightLegX, rightLegY, legWidth, legHeight,
                4.0f / 64.0f, 20.0f / 64.0f, 8.0f / 64.0f, 32.0f / 64.0f, zBase, light);

        // Левая нога (со стороны зрителя - справа)
        drawTexturedQuad(matrix, buffer, leftLegX, leftLegY, legWidth, legHeight,
                20.0f / 64.0f, 52.0f / 64.0f, 24.0f / 64.0f, 64.0f / 64.0f, zBase, light);

        // 2. ВТОРОЙ СЛОЙ ОВЕРЛЕЕВ (Hat, Jacket, Sleeves, Pants)
        float exp = 0.5f;

        // Шапка / волосы
        drawTexturedQuad(matrix, buffer, headX - exp, headY - exp, headSize + exp * 2, headSize + exp * 2,
                40.0f / 64.0f, 8.0f / 64.0f, 48.0f / 64.0f, 16.0f / 64.0f, zOverlay, light);

        // Куртка
        drawTexturedQuad(matrix, buffer, torsoX - exp, torsoY - exp, torsoWidth + exp * 2, torsoHeight + exp * 2,
                20.0f / 64.0f, 36.0f / 64.0f, 28.0f / 64.0f, 48.0f / 64.0f, zOverlay, light);

        // Правый рукав
        drawTexturedQuad(matrix, buffer, rightArmX - exp, rightArmY - exp, armWidth + exp * 2, torsoHeight + exp * 2,
                44.0f / 64.0f, 36.0f / 64.0f, (44.0f / 64.0f) + armUWidth, 48.0f / 64.0f, zOverlay, light);

        // Левый рукав
        drawTexturedQuad(matrix, buffer, leftArmX - exp, leftArmY - exp, armWidth + exp * 2, torsoHeight + exp * 2,
                52.0f / 64.0f, 52.0f / 64.0f, (52.0f / 64.0f) + armUWidth, 64.0f / 64.0f, zOverlay, light);

        // Правая штанина
        drawTexturedQuad(matrix, buffer, rightLegX - exp, rightLegY - exp, legWidth + exp * 2, legHeight + exp * 2,
                4.0f / 64.0f, 36.0f / 64.0f, 8.0f / 64.0f, 48.0f / 64.0f, zOverlay, light);

        // Левая штанина
        drawTexturedQuad(matrix, buffer, leftLegX - exp, leftLegY - exp, legWidth + exp * 2, legHeight + exp * 2,
                4.0f / 64.0f, 52.0f / 64.0f, 8.0f / 64.0f, 64.0f / 64.0f, zOverlay, light);

        flush(consumers);
    }

    private void drawTexturedQuad(
            Matrix4f matrix,
            VertexConsumer buffer,
            float x, float y, float w, float h,
            float u0, float v0, float u1, float v1,
            float z, int light
    ) {
        buffer.vertex(matrix, x, y, z).color(255, 255, 255, 255).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        buffer.vertex(matrix, x, y + h, z).color(255, 255, 255, 255).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        buffer.vertex(matrix, x + w, y + h, z).color(255, 255, 255, 255).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        buffer.vertex(matrix, x + w, y, z).color(255, 255, 255, 255).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
    }

    private void flush(VertexConsumerProvider consumers) {
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    public static int getSlotX(int slotId) {
        if (slotId == 0) return 144; // Crafting result
        if (slotId >= 1 && slotId <= 4) {
            int c = (slotId - 1) % 2;
            return 88 + c * 18; // Crafting 2x2
        }
        if (slotId >= 5 && slotId <= 8) return 8; // Armor slots
        if (slotId >= 9 && slotId <= 35) {
            int col = (slotId - 9) % 9;
            return 8 + col * 18; // Main inventory
        }
        if (slotId >= 36 && slotId <= 44) {
            int col = (slotId - 36);
            return 8 + col * 18; // Hotbar
        }
        if (slotId == 45) return 77; // Offhand
        return 0;
    }

    public static int getSlotY(int slotId) {
        if (slotId == 0) return 36; // Crafting result
        if (slotId >= 1 && slotId <= 4) {
            int r = (slotId - 1) / 2;
            return 26 + r * 18; // Crafting 2x2
        }
        if (slotId >= 5 && slotId <= 8) {
            return 8 + (slotId - 5) * 18; // Armor slots
        }
        if (slotId >= 9 && slotId <= 35) {
            int row = (slotId - 9) / 9;
            return 84 + row * 18; // Main inventory
        }
        if (slotId >= 36 && slotId <= 44) {
            return 142; // Hotbar
        }
        if (slotId == 45) return 62; // Offhand
        return 0;
    }

    private void renderInventoryTexture(Matrix4f matrix, VertexConsumerProvider consumers, int light) {
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityTranslucent(INVENTORY_TEXTURE));

        float u0 = 0.0f;
        float u1 = GUI_WIDTH / 256.0f;
        float v0 = 0.0f;
        float v1 = GUI_HEIGHT / 256.0f;
        float z = 0.0f;

        // Лицевая сторона
        buffer.vertex(matrix, 0.0f, 0.0f, z).color(255, 255, 255, 255).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        buffer.vertex(matrix, 0.0f, GUI_HEIGHT, z).color(255, 255, 255, 255).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        buffer.vertex(matrix, GUI_WIDTH, GUI_HEIGHT, z).color(255, 255, 255, 255).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);
        buffer.vertex(matrix, GUI_WIDTH, 0.0f, z).color(255, 255, 255, 255).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1);

        // Обратная сторона
        buffer.vertex(matrix, GUI_WIDTH, 0.0f, z).color(255, 255, 255, 255).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, -1);
        buffer.vertex(matrix, GUI_WIDTH, GUI_HEIGHT, z).color(255, 255, 255, 255).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, -1);
        buffer.vertex(matrix, 0.0f, GUI_HEIGHT, z).color(255, 255, 255, 255).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, -1);
        buffer.vertex(matrix, 0.0f, 0.0f, z).color(255, 255, 255, 255).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, -1);
    }

    private void renderBackplate(MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityCutout(INVENTORY_TEXTURE));
        Matrix4f m = matrices.peek().getPositionMatrix();

        float pad = 2.0f;
        float zBack = -0.05f;

        buffer.vertex(m, -pad, -pad, zBack).color(30, 30, 30, 255).texture(0.01f, 0.01f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, -1);
        buffer.vertex(m, GUI_WIDTH + pad, -pad, zBack).color(30, 30, 30, 255).texture(0.01f, 0.01f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, -1);
        buffer.vertex(m, GUI_WIDTH + pad, GUI_HEIGHT + pad, zBack).color(30, 30, 30, 255).texture(0.01f, 0.01f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, -1);
        buffer.vertex(m, -pad, GUI_HEIGHT + pad, zBack).color(30, 30, 30, 255).texture(0.01f, 0.01f).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, -1);
    }

    private void renderHoverQuad(Matrix4f matrix, VertexConsumerProvider consumers, int x, int y, int light) {
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getGui());
        float z = 0.03f;

        buffer.vertex(matrix, x, y, z).color(255, 255, 255, 128);
        buffer.vertex(matrix, x, y + 16, z).color(255, 255, 255, 128);
        buffer.vertex(matrix, x + 16, y + 16, z).color(255, 255, 255, 128);
        buffer.vertex(matrix, x + 16, y, z).color(255, 255, 255, 128);
    }

    private void renderItemBar(MatrixStack matrices, VertexConsumerProvider consumers, int x, int y, int step, int color, int light) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getGui());

        float zBg = 0.10f;
        float zBar = 0.12f;

        buffer.vertex(m, x, y, zBg).color(0, 0, 0, 255);
        buffer.vertex(m, x, y + 2, zBg).color(0, 0, 0, 255);
        buffer.vertex(m, x + 13, y + 2, zBg).color(0, 0, 0, 255);
        buffer.vertex(m, x + 13, y, zBg).color(0, 0, 0, 255);

        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;

        buffer.vertex(m, x, y, zBar).color(r, g, b, 255);
        buffer.vertex(m, x, y + 1, zBar).color(r, g, b, 255);
        buffer.vertex(m, x + step, y + 1, zBar).color(r, g, b, 255);
        buffer.vertex(m, x + step, y, zBar).color(r, g, b, 255);
    }

    private void renderCursor(MatrixStack matrices, VertexConsumerProvider consumers, float cx, float cy, int light) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getGui());

        float z = 0.20f;

        buffer.vertex(m, cx, cy, z).color(0, 0, 0, 220);
        buffer.vertex(m, cx, cy + 12.0f, z).color(0, 0, 0, 220);
        buffer.vertex(m, cx + 9.0f, cy + 9.0f, z).color(0, 0, 0, 220);
        buffer.vertex(m, cx, cy, z).color(0, 0, 0, 220);

        buffer.vertex(m, cx + 1.0f, cy + 1.0f, z + 0.02f).color(255, 255, 255, 255);
        buffer.vertex(m, cx + 1.0f, cy + 10.0f, z + 0.02f).color(255, 255, 255, 255);
        buffer.vertex(m, cx + 8.0f, cy + 8.0f, z + 0.02f).color(255, 255, 255, 255);
        buffer.vertex(m, cx + 1.0f, cy + 1.0f, z + 0.02f).color(255, 255, 255, 255);
    }
}
