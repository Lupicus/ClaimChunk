package com.lupicus.cc.gui;

import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.network.ClaimUpdatePacket;
import com.lupicus.cc.network.Network;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ClaimScreen extends Screen
{
	ClaimTileEntity te;
	private EditBox access;
	private EditBox modify;
	private CycleButton<Boolean> enabled;
	private Button done;
	private boolean activate = false;

	public ClaimScreen(ClaimTileEntity te) {
		super(GameNarrator.NO_TITLE);
		this.te = te;
	}

	@Override
	protected void init() {
		access = new EditBox(font, width / 2 - 152, 20, 300, 20,
				Component.translatable("cc.gui.access"));
		access.setMaxLength(256);
		access.setValue(te.getAccess());
		addWidget(access);
		modify = new EditBox(font, width / 2 - 152, 60, 300, 20,
				Component.translatable("cc.gui.modify"));
		modify.setMaxLength(256);
		modify.setValue(te.getModify());
		addWidget(modify);
		if (!isEnabled())
		{
			enabled = addRenderableWidget(CycleButton.onOffBuilder(activate).create(width / 2 - 4 - 150, 90, 150, 20,
					Component.translatable("cc.gui.enable"), (button, value) -> {
						activate = value;
					}));
		}
		// done
		done = addRenderableWidget(
				Button.builder(CommonComponents.GUI_DONE, (button) -> {
					doneCB();
				}).bounds(width / 2 - 4 - 150, 210, 150, 20).build());
		// cancel
		addRenderableWidget(
				Button.builder(CommonComponents.GUI_CANCEL, (button) -> {
					cancelCB();
				}).bounds(width / 2 + 4, 210, 150, 20).build());
		setInitialFocus(access);
	}

	@Override
	public void resize(Minecraft p_231152_1_, int p_231152_2_, int p_231152_3_) {
		String s = access.getValue();
		String s1 = modify.getValue();
		init(p_231152_1_, p_231152_2_, p_231152_3_);
		access.setValue(s);
		modify.setValue(s1);
	}

	private void doneCB() {
		sendData();
		minecraft.setScreen((Screen) null);
	}

	private void cancelCB() {
		minecraft.setScreen((Screen) null);
	}

	private boolean isEnabled()
	{
		Level world = te.getLevel();
		BlockState state = world.getBlockState(te.getBlockPos());
		return state.getValue(ClaimBlock.ENABLED);
	}

	private void sendData()
	{
		String[] names = access.getValue().split(",");
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < names.length; ++i)
		{
			if (i > 0)
				buf.append(",");
			buf.append(names[i].trim());
		}
		String temp1 = buf.toString();
		buf.setLength(0);
		names = modify.getValue().split(",");
		for (int i = 0; i < names.length; ++i)
		{
			if (i > 0)
				buf.append(",");
			buf.append(names[i].trim());
		}
		String temp2 = buf.toString();
		Network.sendToServer(new ClaimUpdatePacket(te.getBlockPos(), temp1, temp2));
		if (enabled != null && activate)
			Network.sendToServer(new ClaimUpdatePacket(te.getBlockPos(), true));
	}

	@Override
	public void onClose() {
		cancelCB();
	}

	@Override
	public boolean keyPressed(int key, int scancode, int modifiers) {
		if (super.keyPressed(key, scancode, modifiers)) {
			return true;
		} else if (!done.active || key != 257 && key != 335) {
			return false;
		} else {
			doneCB();
			return true;
		}
	}

	@Override
	public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
		super.render(gg, mouseX, mouseY, partialTicks);
		gg.drawString(font, I18n.get("cc.gui.access"),
				width / 2 - 153, 10, 10526880);
		access.render(gg, mouseX, mouseY, partialTicks);
		gg.drawString(font, I18n.get("cc.gui.modify"),
				width / 2 - 153, 50, 10526880);
		modify.render(gg, mouseX, mouseY, partialTicks);
	}
}
