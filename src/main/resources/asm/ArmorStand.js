var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')
var AbstractInsnNode = Java.type('org.objectweb.asm.tree.AbstractInsnNode')
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode')
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode')
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
    			var fn = asmapi.mapMethod('m_6469_') // hurt
    			for (var i = 0; i < classNode.methods.size(); ++i) {
    				var obj = classNode.methods.get(i)
    				if (obj.name == fn) {
    					patch_m_6469_(obj)
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
function patch_m_6469_(obj) {
	var fn = asmapi.mapMethod('m_31677_') // isMarker
	var node = asmapi.findFirstMethodCall(obj, asmapi.MethodType.VIRTUAL, "net/minecraft/world/entity/decoration/ArmorStand", fn, "()Z")
	if (node) {
		node = node.getNext()
		if (node.getOpcode() == opc.IFNE) {
			var lb = node.label
			var op1 = new VarInsnNode(opc.ALOAD, 0)
			var op2 = new VarInsnNode(opc.ALOAD, 1)
			var op3 = asmapi.buildMethodCall("com/lupicus/cc/events/PlayerEvents", "cancelEntityHurt", "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)Z", asmapi.MethodType.STATIC)
			var op4 = new JumpInsnNode(opc.IFNE, lb)
			var list = asmapi.listOf(op1, op2, op3, op4)
			obj.instructions.insert(node, list)
		}
		else
			asmapi.log("ERROR", "Failed to modify ArmorStand: call is different")
	}
	else
		asmapi.log("ERROR", "Failed to modify ArmorStand: call not found")
}
