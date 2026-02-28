package cc.deya.iwshipg.client;

import cc.deya.iwshipg.inventory.HandleTorpedoMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HandleTorpedoScreen extends AbstractContainerScreen<HandleTorpedoMenu> {

    public HandleTorpedoScreen(HandleTorpedoMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166; // 标准高度即可
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 1. 基础背景：深灰色半透明 (工业重型感)
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xDD2D2D2D);

        // 2. 绘制待发架装饰 (第一行)
        // 给第一行加一个淡红色的背景暗示“就绪/危险”
        graphics.fill(x + 40, y + 18, x + 136, y + 38, 0x33FF0000);

        graphics.pose().pushPose();
        graphics.pose().scale(0.7f, 0.7f, 0.7f);
        graphics.drawString(this.font, "READY RACK (FAN-FIRE)", (int) ((x + 44) / 0.7f), (int) ((y + 10) / 0.7f), 0xFF5555);
        graphics.pose().popPose();

        // 3. 绘制备用架装饰 (第二行)
        // 给第二行加一个淡蓝色的背景暗示“储备”
        graphics.fill(x + 40, y + 43, x + 136, y + 63, 0x3300AAFF);

        graphics.pose().pushPose();
        graphics.pose().scale(0.7f, 0.7f, 0.7f);
        graphics.drawString(this.font, "RESERVE MAGAZINE", (int) ((x + 44) / 0.7f), (int) ((y + 66) / 0.7f), 0x55FFFF);
        graphics.pose().popPose();

        // 4. 绘制 10 个槽位的边框
        // 第一行 0-4
        for (int i = 0; i < 5; i++) {
            int sx = x + 44 + i * 18;
            int sy = y + 20;
            drawSlotFrame(graphics, sx, sy, 0xAAFFFFFF);
        }

        // 第二行 5-9
        for (int i = 0; i < 5; i++) {
            int sx = x + 44 + i * 18;
            int sy = y + 45;
            drawSlotFrame(graphics, sx, sy, 0x88AAAAAA);
        }

        // 5. 绘制逻辑装饰线（提示推弹方向）
        for (int i = 0; i < 5; i++) {
            int lx = x + 44 + i * 18 + 8;
            graphics.fill(lx, y + 39, lx + 2, y + 44, 0x44FFFFFF); // 垂直小短线
        }
    }

    // 辅助方法：画一个简单的槽位边框
    private void drawSlotFrame(GuiGraphics graphics, int x, int y, int color) {
        graphics.renderOutline(x - 1, y - 1, 18, 18, color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0xFFFFFF);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0xAAAAAA);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}