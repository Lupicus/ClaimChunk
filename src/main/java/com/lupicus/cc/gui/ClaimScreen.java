package com.lupicus.cc.gui;

import com.lupicus.cc.block.ClaimBlock;
import com.lupicus.cc.network.ClaimUpdatePacket;
import com.lupicus.cc.network.Network;
import com.lupicus.cc.tileentity.ClaimTileEntity;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class ClaimScreen extends Screen
{
	ClaimTileEntity te;
	private TextFieldWidget access;
	private Button enabled;
	private Button done;

	public ClaimScreen(ClaimTileEntity te) {
		super(NarratorChatListener.EMPTY);
		this.te = te;
	}

	@Override
	protected void func_231160_c_() { // initGui
		field_230706_i_.keyboardListener.enableRepeatEvents(true);
		access = new TextFieldWidget(field_230712_o_, field_230708_k_ / 2 - 152, 20, 300, 20,
				new TranslationTextComponent("cc.gui.access"));
		access.setMaxStringLength(128);
		access.setText(te.accessList);
		field_230705_e_.add(access);
		enabled = func_230480_a_(new Button(field_230708_k_ / 2 - 4 - 150, 50, 150, 20,
				new TranslationTextComponent("cc.gui.enable"), (p_238828_1_) -> {
					enableBlock();
				}));
		enabled.field_230693_o_ = !isEnabled();
		// done
		done = func_230480_a_(
				new Button(field_230708_k_ / 2 - 4 - 150, 210, 150, 20, DialogTexts.field_240632_c_, (p_238825_1_) -> {
					doneCB();
				}));
		// cancel
		func_230480_a_(
				new Button(field_230708_k_ / 2 + 4, 210, 150, 20, DialogTexts.field_240633_d_, (p_238825_1_) -> {
					cancelCB();
				}));
		setFocusedDefault(access);
	}

	@Override
	public void func_231152_a_(Minecraft p_231152_1_, int p_231152_2_, int p_231152_3_) {
		String s = access.getText();
		func_231158_b_(p_231152_1_, p_231152_2_, p_231152_3_);
		access.setText(s);
	}

	@Override
	public void func_231164_f_() {  // closeGui
		field_230706_i_.keyboardListener.enableRepeatEvents(false);
	}

	@Override
	public void func_231023_e_() {
		access.tick();
	}

	private void doneCB() {
		sendData();
		field_230706_i_.displayGuiScreen((Screen) null);
	}

	private void cancelCB() {
		field_230706_i_.displayGuiScreen((Screen) null);
	}

	private boolean isEnabled()
	{
		World world = te.getWorld();
		BlockState state = world.getBlockState(te.getPos());
		return state.get(ClaimBlock.ENABLED);
	}

	private void enableBlock() {
		enabled.field_230693_o_ = false;
		Network.sendToServer(new ClaimUpdatePacket(te.getPos(), true));
	}

	private void sendData()
	{
		String[] names = access.getText().split(",");
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < names.length; ++i)
		{
			if (i > 0)
				buf.append(",");
			buf.append(names[i].trim());
		}
		Network.sendToServer(new ClaimUpdatePacket(te.getPos(), buf.toString()));
	}

	@Override
	public void func_231175_as__() { // Escape
		cancelCB();
	}

	@Override
	public boolean func_231046_a_(int key, int scancode, int modifiers) { // keyPressed
		if (super.func_231046_a_(key, scancode, modifiers)) {
			return true;
		} else if (!done.field_230693_o_ || key != 257 && key != 335) {
			return false;
		} else {
			doneCB();
			return true;
		}
	}

	@Override
	public void func_230430_a_(MatrixStack mStack, int mouseX, int mouseY, float partialTicks) { // render
		func_230446_a_(mStack);
		func_238476_c_(mStack, field_230712_o_, I18n.format("cc.gui.access"),
				field_230708_k_ / 2 - 153, 10, 10526880);
		access.func_230430_a_(mStack, mouseX, mouseY, partialTicks);

		super.func_230430_a_(mStack, mouseX, mouseY, partialTicks);
	}
}
