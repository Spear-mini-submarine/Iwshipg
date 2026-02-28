package cc.deya.iwshipg.client;

import cc.deya.iwshipg.inventory.HandleAAMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HandleAAScreen extends AbstractContainerScreen<HandleAAMenu> {

    public HandleAAScreen(HandleAAMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 200; // 稍微加高，给 16 个格子留空间
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 1. 基础背景：高科技深蓝半透明感
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xCC050A15);

        // 2. 绘制 2x2 组的物理分隔线（十字准星感）
        int centerX = x + imageWidth / 2;
        int centerY = y + 55;
        graphics.fill(centerX - 60, centerY, centerX + 60, centerY + 1, 0x44FFFFFF); // 横线
        graphics.fill(centerX, centerY - 45, centerX + 1, centerY + 45, 0x44FFFFFF); // 纵线

        // 3. 循环绘制 4 个弹药组的装饰
        String[] groupNames = {"ALPHA", "BRAVO", "CHARLIE", "DELTA"};
        int[][] groupCoords = {{40, 20}, {90, 20}, {40, 70}, {90, 70}};

        for (int i = 0; i < 4; i++) {
            int gx = x + groupCoords[i][0];
            int gy = y + groupCoords[i][1];

            // 绘制组背景阴影
            graphics.fill(gx - 2, gy - 2, gx + 38, gy + 38, 0x22FFFFFF);

            // 绘制组标签（小字）
            graphics.pose().pushPose();
            graphics.pose().scale(0.6f, 0.6f, 0.6f);
            graphics.drawString(this.font, groupNames[i], (int) ((gx) / 0.6f), (int) ((gy - 8) / 0.6f), 0xAAAAAA);
            graphics.pose().popPose();

            // 绘制 4 个槽位的白边框
            for (int j = 0; j < 4; j++) {
                int sx = gx + (j % 2) * 18;
                int sy = gy + (j / 2) * 18;
                graphics.renderOutline(sx, sy, 18, 18, 0x88FFFFFF);
            }
        }

        // 4. 底部状态装饰
        graphics.drawString(this.font, "AUTO-LOADER ACTIVE", x + 10, y + imageHeight - 100, 0x00FF00);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 将标题移到最上方
        graphics.drawString(this.font, this.title, 8, 6, 0xFFFFFF);
        // 玩家背包标题
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0xAAAAAA);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}