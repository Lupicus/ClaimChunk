var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode')
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode')

function initializeCoreMod() {
    return {
    	'Player': {
    		'target': {
    			'type': 'CLASS',
    			'name': 'net.minecraft.world.entity.player.Player'
    		},
    		'transformer': function(classNode) {
    			var count = 0
    			var fn = "attack"
    			for (var i = 0; i < classNode.methods.size(); ++i) {
    				var obj = classNode.methods.get(i)
    				if (obj.name == fn) {
    					patch_attack(obj)
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

// add cancelEntityAttack call
function patch_attack(obj) {
	var fn = "distanceToSqr"
	var node = asmapi.findFirstMethodCall(obj, asmapi.MethodType.VIRTUAL, "net/minecraft/world/entity/player/Player", fn, "(Lnet/minecraft/world/entity/Entity;)D")
	if (node) {
		var node2 = node.getPrevious()
		while (node2.getOpcode() == -1) // skip misplaced junk
			node2 = node2.getPrevious()
		node = node.getNext().getNext().getNext()
		if (node2.getOpcode() == opc.ALOAD && node.getOpcode() == opc.IFGE) {
			var lb = node.label
			var op1 = new VarInsnNode(opc.ALOAD, 0)
			var op2 = new VarInsnNode(opc.ALOAD, node2.var)
			var op3 = asmapi.buildMethodCall("com/lupicus/cc/events/PlayerEvents", "cancelEntityAttack", "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)Z", asmapi.MethodType.STATIC)
			var op4 = new JumpInsnNode(opc.IFNE, lb)
			var list = asmapi.listOf(op1, op2, op3, op4)
			obj.instructions.insert(node, list)
		}
		else
			asmapi.log("ERROR", "Failed to modify Player: code is different")
	}
	else
		asmapi.log("ERROR", "Failed to modify Player: call not found")
}
