var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode')
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode')

function initializeCoreMod() {
    return {
    	'AttachedEntity': {
    		'target': {
    			'type': 'CLASS',
    			'name': 'net.minecraft.world.entity.decoration.BlockAttachedEntity'
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
    				asmapi.log("ERROR", "Failed to modify BlockAttachedEntity: Method not found")
    			return classNode;
    		}
    	}
    }
}

// add conditional return
function patch_hurt(obj) {
	var node = obj.instructions.getFirst()
	while (node) {
		if (node.getOpcode() == opc.GETFIELD && node.name == 'isClientSide')
			break;
		node = node.getNext()
	}
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
			asmapi.log("ERROR", "Failed to modify BlockAttachedEntity: code is different")
	}
	else
		asmapi.log("ERROR", "Failed to modify BlockAttachedEntity: GETFIELD not found")
}
