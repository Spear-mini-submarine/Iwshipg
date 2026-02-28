package cc.deya.iwshipg.client;

import cc.deya.iwshipg.inventory.HandleRapidMenu;
import cc.deya.iwshipg.item.HandleRapidCannonItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HandleRapidScreen extends AbstractContainerScreen<HandleRapidMenu> {

    public HandleRapidScreen(HandleRapidMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 222; // 增加高度以容纳 2x2 组布局和玩家背包
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // --- 修正点：只定义一次 activeGroup，且实时获取 ---
        int activeGroup = 0;
        Player player = this.minecraft.player;
        if (player != null) {
            ItemStack realGun = player.getMainHandItem();
            // 如果主手不是枪，尝试副手
            if (!(realGun.getItem() instanceof HandleRapidCannonItem)) {
                realGun = player.getOffhandItem();
            }

            if (realGun.hasTag() && realGun.getTag().contains("ShotPointer")) {
                activeGroup = realGun.getTag().getInt("ShotPointer");
            }
        }

        // 1. 基础背景
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xDD1A1A1A);

        // 2. 绘制装饰边框
        int centerX = x + imageWidth / 2;
        int centerY = y + 70;
        graphics.fill(centerX - 60, centerY, centerX + 60, centerY + 1, 0x22FFFFFF);
        graphics.fill(centerX, centerY - 55, centerX + 1, centerY + 55, 0x22FFFFFF);

        // 3. 循环绘制 4 个弹仓组 (现在它会根据上面获取的 activeGroup 动态变色了)
        String[] groupNames = {"ALPHA", "BRAVO", "CHARLIE", "DELTA"};
        int startX = 44;
        int startY = 18;
        int groupOffsetX = 54;
        int groupOffsetY = 54;

        for (int g = 0; g < 4; g++) {
            int gx = x + startX + (g % 2) * groupOffsetX;
            int gy = y + startY + (g / 2) * groupOffsetY;

            // 活跃组显示绿色，非活跃组显示暗灰色
            int bgColor = (g == activeGroup) ? 0x4400FF00 : 0x11FFFFFF;
            int frameColor = (g == activeGroup) ? 0xFF00FF00 : 0x44FFFFFF;

            graphics.fill(gx - 4, gy - 4, gx + 38, gy + 38, bgColor);

            graphics.pose().pushPose();
            graphics.pose().scale(0.6f, 0.6f, 0.6f);
            graphics.drawString(this.font, groupNames[g], (int) (gx / 0.6f), (int) ((gy - 10) / 0.6f), frameColor);
            graphics.pose().popPose();

            for (int i = 0; i < 4; i++) {
                int sx = gx + (i % 2) * 18;
                int sy = gy + (i / 2) * 18;
                graphics.renderOutline(sx, sy, 18, 18, frameColor);
            }
        }

        graphics.drawString(this.font, "MIXED FEED SYSTEM: ACTIVE", x + 10, y + 128, 0x55FFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 标题渲染
        graphics.drawString(this.font, this.title, 8, 6, 0x00FF00);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0xAAAAAA);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}