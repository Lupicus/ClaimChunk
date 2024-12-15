var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode')
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
    			var fn = "getSpread"
    			for (var i = 0; i < classNode.methods.size(); ++i) {
    				var obj = classNode.methods.get(i)
    				if (obj.name == fn) {
    					patch_spread(obj)
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

// add canSpreadTo call
function patch_spread(obj) {
	var fn = "canBeReplacedWith"
	var owner = "net/minecraft/world/level/material/FluidState"
	var node = asmapi.findFirstMethodCall(obj, asmapi.MethodType.VIRTUAL, owner, fn, "(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/Direction;)Z")
	if (node) {
		node = node.getNext()
		if (node.getOpcode() == opc.IFEQ) {
			var lb = node.label
			var op1 = new VarInsnNode(opc.ALOAD, 1)
			var op2 = new VarInsnNode(opc.ALOAD, 2)
			var op3 = new VarInsnNode(opc.ALOAD, 9)
			var op4 = new VarInsnNode(opc.ALOAD, 8)
			var op5 = asmapi.buildMethodCall("com/lupicus/cc/events/BlockEvents", "canSpreadTo", "(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z", asmapi.MethodType.STATIC)
			var op6 = new JumpInsnNode(opc.IFEQ, lb)
			var list = asmapi.listOf(op1, op2, op3, op4, op5, op6)
			obj.instructions.insert(node, list)
		}
		else
			asmapi.log("ERROR", "Failed to modify FlowingFluid: code is different")
	}
	else
		asmapi.log("ERROR", "Failed to modify FlowingFluid: call not found")
}
