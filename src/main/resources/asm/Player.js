var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')
var AbstractInsnNode = Java.type('org.objectweb.asm.tree.AbstractInsnNode')
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode')
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode')
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode')

function initializeCoreMod() {
    return {
    	'Player': {
    		'target': {
    			'type': 'CLASS',
    			'name': 'net.minecraft.entity.player.PlayerEntity'
    		},
    		'transformer': function(classNode) {
    			var count = 0
    			var fn = asmapi.mapMethod('func_71059_n') // attackTargetEntityWithCurrentItem
    			for (var i = 0; i < classNode.methods.size(); ++i) {
    				var obj = classNode.methods.get(i)
    				if (obj.name == fn) {
    					patch_func_71059_n(obj)
    					count++
    				}
    			}
    			if (count < 1)
    				asmapi.log("ERROR", "Failed to modify Player: Method not found")
    			return classNode;
    		}
    	}
    }
}

// add conditional return
function patch_func_71059_n(obj) {
	var fn = asmapi.mapMethod('func_70068_e') // getDistanceSq
	var node = asmapi.findFirstMethodCall(obj, asmapi.MethodType.VIRTUAL, "net/minecraft/entity/player/PlayerEntity", fn, "(Lnet/minecraft/entity/Entity;)D")
	if (node) {
		var node2 = node.getPrevious()
		node = node.getNext().getNext().getNext()
		if (node2.getOpcode() == opc.ALOAD && node.getOpcode() == opc.IFGE) {
			var lb = node.label
			var op1 = new VarInsnNode(opc.ALOAD, 0)
			var op2 = new VarInsnNode(opc.ALOAD, node2.var)
			var op3 = asmapi.buildMethodCall("com/lupicus/cc/events/PlayerEvents", "cancelEntityAttack", "(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)Z", asmapi.MethodType.STATIC)
			var op4 = new JumpInsnNode(opc.IFNE, lb)
			var list = asmapi.listOf(op1, op2, op3, op4)
			obj.instructions.insert(node, list)
		}
		else
			asmapi.log("ERROR", "Failed to modify Player: call is different")
	}
	else
		asmapi.log("ERROR", "Failed to modify Player: call not found")
}
