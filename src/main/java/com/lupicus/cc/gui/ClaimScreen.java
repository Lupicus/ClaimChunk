package com.lupicus.cc.gui;

import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.network.ClaimUpdatePacket;
import com.lupicus.cc.network.Network;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
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
	private Button enabled;
	private Button done;
	private boolean activate = false;

	public ClaimScreen(ClaimTileEntity te) {
		super(CommonComponents.EMPTY); // to bridge 1.19.1+ (GameNarrator.NO_TITLE)
		this.te = te;
	}

	@Override
	protected void init() {
		minecraft.keyboardHandler.setSendRepeatsToGui(true);
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
			enabled = addRenderableWidget(new Button(width / 2 - 4 - 150, 90, 150, 20,
					Component.translatable("cc.gui.enable"), (button) -> {
						activate = !activate;
					}) {
				@Override
				public Component getMessage() {
					return CommonComponents.optionStatus(super.getMessage(), ClaimScreen.this.activate);
				}
			});
		}
		// done
		done = addRenderableWidget(
				new Button(width / 2 - 4 - 150, 210, 150, 20, CommonComponents.GUI_DONE, (p_238825_1_) -> {
					doneCB();
				}));
		// cancel
		addRenderableWidget(
				new Button(width / 2 + 4, 210, 150, 20, CommonComponents.GUI_CANCEL, (p_238825_1_) -> {
					cancelCB();
				}));
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

	@Override
	public void removed() {
		minecraft.keyboardHandler.setSendRepeatsToGui(false);
	}

	@Override
	public void tick() {
		access.tick();
		modify.tick();
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
	public void render(PoseStack mStack, int mouseX, int mouseY, float partialTicks) {
		renderBackground(mStack);
		drawString(mStack, font, I18n.get("cc.gui.access"),
				width / 2 - 153, 10, 10526880);
		access.render(mStack, mouseX, mouseY, partialTicks);
		drawString(mStack, font, I18n.get("cc.gui.modify"),
				width / 2 - 153, 50, 10526880);
		modify.render(mStack, mouseX, mouseY, partialTicks);

		super.render(mStack, mouseX, mouseY, partialTicks);
	}
}
