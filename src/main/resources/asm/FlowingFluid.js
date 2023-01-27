var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')
var AbstractInsnNode = Java.type('org.objectweb.asm.tree.AbstractInsnNode')
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode')
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode')
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode')

function initializeCoreMod() {
    return {
    	'FlowingFluid': {
    		'target': {
    			'type': 'CLASS',
    			'name': 'net.minecraft.world.level.material.FlowingFluid'
    		},
    		'transformer': function(classNode) {
    			var count = 0
    			var fn = asmapi.mapMethod('m_75977_') // canSpreadTo
    			for (var i = 0; i < classNode.methods.size(); ++i) {
    				var obj = classNode.methods.get(i)
    				if (obj.name == fn) {
    					patch_m_75977_(obj)
    					count++
    				}
    			}
    			if (count < 1)
    				asmapi.log("ERROR", "Failed to modify FlowingFluid: Method not found")
    			return classNode;
    		}
    	}
    }
}

// add conditional return
function patch_m_75977_(obj) {
	var fn = asmapi.mapMethod('m_75972_') // canHoldFluid
	var owner = "net/minecraft/world/level/material/FlowingFluid"
	var node = asmapi.findFirstMethodCall(obj, asmapi.MethodType.VIRTUAL, owner, fn, "(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/Fluid;)Z")
	if (node) {
		node = node.getNext()
		if (node.getOpcode() == opc.IFEQ) {
			var lb = node.label
			var op1 = new VarInsnNode(opc.ALOAD, 1)
			var op2 = new VarInsnNode(opc.ALOAD, 2)
			var op3 = new VarInsnNode(opc.ALOAD, 5)
			var op4 = new VarInsnNode(opc.ALOAD, 4)
			var op5 = asmapi.buildMethodCall("com/lupicus/cc/events/BlockEvents", "canSpreadTo", "(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z", asmapi.MethodType.STATIC)
			var op6 = new JumpInsnNode(opc.IFEQ, lb)
			var list = asmapi.listOf(op1, op2, op3, op4, op5, op6)
			obj.instructions.insert(node, list)
		}
		else
			asmapi.log("ERROR", "Failed to modify FlowingFluid: call is different")
	}
	else
		asmapi.log("ERROR", "Failed to modify FlowingFluid: call not found")
}
