var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode')
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode')
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode')
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode')

function initializeCoreMod() {
    return {
    	'VehicleEntity': {
    		'target': {
    			'type': 'CLASS',
    			'name': 'net.minecraft.world.entity.vehicle.VehicleEntity'
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
    				asmapi.log("ERROR", "Failed to modify VehicleEntity: Method not found")
    			return classNode;
    		}
    	}
    }
}

// add conditional return
function patch_hurt(obj) {
	var fn = "getHurtDir"
	var node = asmapi.findFirstMethodCall(obj, asmapi.MethodType.VIRTUAL, "net/minecraft/world/entity/vehicle/VehicleEntity", fn, "()I")
	if (node) {
		node = node.getPrevious().getPrevious()
		if (node.getOpcode() == opc.ALOAD) {
			var lb = new LabelNode()
			var op1 = new VarInsnNode(opc.ALOAD, 0)
			var op2 = new VarInsnNode(opc.ALOAD, 1)
			var op3 = asmapi.buildMethodCall("com/lupicus/cc/events/PlayerEvents", "cancelEntityHurt", "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)Z", asmapi.MethodType.STATIC)
			var op4 = new JumpInsnNode(opc.IFEQ, lb)
			var op5 = new InsnNode(opc.ICONST_1)
			var op6 = new InsnNode(opc.IRETURN)
			var list = asmapi.listOf(op1, op2, op3, op4, op5, op6, lb)
			obj.instructions.insertBefore(node, list)
		}
		else
			asmapi.log("ERROR", "Failed to modify VehicleEntity: call is different")
	}
	else
		asmapi.log("ERROR", "Failed to modify VehicleEntity: call not found")
}
