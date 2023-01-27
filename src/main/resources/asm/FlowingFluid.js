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
    			'name': 'net.minecraft.fluid.FlowingFluid'
    		},
    		'transformer': function(classNode) {
    			var count = 0
    			var fn = asmapi.mapMethod('func_205570_b') // canFlow
    			for (var i = 0; i < classNode.methods.size(); ++i) {
    				var obj = classNode.methods.get(i)
    				if (obj.name == fn) {
    					patch_func_205570_b(obj)
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
function patch_func_205570_b(obj) {
	var fn = asmapi.mapMethod('func_211761_a') // isBlocked
	var owner = "net/minecraft/fluid/FlowingFluid"
	var node = asmapi.findFirstMethodCall(obj, asmapi.MethodType.SPECIAL, owner, fn, "(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/Fluid;)Z")
	if (node) {
		node = node.getNext()
		if (node.getOpcode() == opc.IFEQ) {
			var lb = node.label
			var op1 = new VarInsnNode(opc.ALOAD, 1)
			var op2 = new VarInsnNode(opc.ALOAD, 2)
			var op3 = new VarInsnNode(opc.ALOAD, 5)
			var op4 = new VarInsnNode(opc.ALOAD, 4)
			var op5 = asmapi.buildMethodCall("com/lupicus/cc/events/BlockEvents", "canSpreadTo", "(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/Direction;)Z", asmapi.MethodType.STATIC)
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
