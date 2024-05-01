var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode')
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode')

function initializeCoreMod() {
    return {
    	'ArmorStand': {
    		'target': {
    			'type': 'CLASS',
    			'name': 'net.minecraft.world.entity.decoration.ArmorStand'
    		},
    		'transformer': function(classNode) {
    			var count = 0
    			var fn = "hurt"
    			for (var i = 0; i < classNode.methods.size(); ++i) {
    				var obj = classNode.methods.get(i)
    				if (obj.name == fn) {
    					patch_hurt(obj)
    					count++
    				}
    			}
    			if (count < 1)
    				asmapi.log("ERROR", "Failed to modify ArmorStand: Method not found")
    			return classNode;
    		}
    	}
    }
}

// add conditional return
function patch_hurt(obj) {
	var fn = "isMarker"
	var node = asmapi.findFirstMethodCall(obj, asmapi.MethodType.VIRTUAL, "net/minecraft/world/entity/decoration/ArmorStand", fn, "()Z")
	if (node) {
		var node2 = node.getPrevious().getPrevious()
		node = node.getNext()
		if (node.getOpcode() == opc.IFEQ && node2.getOpcode() == opc.IFNE) {
			var op1 = new JumpInsnNode(opc.IFNE, node2.label)
			var op2 = new VarInsnNode(opc.ALOAD, 0)
			var op3 = new VarInsnNode(opc.ALOAD, 1)
			var op4 = asmapi.buildMethodCall("com/lupicus/cc/events/PlayerEvents", "cancelEntityHurt", "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)Z", asmapi.MethodType.STATIC)
			var list = asmapi.listOf(op1, op2, op3, op4)
			obj.instructions.insertBefore(node, list)
		}
		else
			asmapi.log("ERROR", "Failed to modify ArmorStand: call is different")
	}
	else
		asmapi.log("ERROR", "Failed to modify ArmorStand: call not found")
}
