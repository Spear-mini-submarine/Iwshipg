package cc.deya.iwshipg.client;

import cc.deya.iwshipg.inventory.HandleArtilleryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class HandleArtilleryScreen extends AbstractContainerScreen<HandleArtilleryMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/dispenser.png");

    public HandleArtilleryScreen(HandleArtilleryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // 1. 获取 GUI 的起始坐标
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 2. 【核心】不渲染图片，而是直接画一个半透明矩形作为背景
        // 颜色代码 0x80000000 是半透明黑色 (ARGB)
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0x80000000);

        // 3. (可选) 给两个格子画个白边，不然你找不到在哪放东西
        // 槽位 1 (56, 35), 槽位 2 (116, 35)
        graphics.renderOutline(x + 55, y + 34, 18, 18, 0xFFFFFFFF);
        graphics.renderOutline(x + 115, y + 34, 18, 18, 0xFFFFFFFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // 渲染暗色背景蒙版（原版风格）
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        // 渲染物品悬浮提示
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}