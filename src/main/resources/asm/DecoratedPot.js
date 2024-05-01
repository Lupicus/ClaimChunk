var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')
var AbstractInsnNode = Java.type('org.objectweb.asm.tree.AbstractInsnNode')
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode')
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode')

function initializeCoreMod() {
    return {
    	'DecoratedPot': {
    		'target': {
    			'type': 'CLASS',
    			'name': 'net.minecraft.world.level.block.DecoratedPotBlock'
    		},
    		'transformer': function(classNode) {
    			var count = 0
    			var fn = "onProjectileHit"
    			for (var i = 0; i < classNode.methods.size(); ++i) {
    				var obj = classNode.methods.get(i)
    				if (obj.name == fn) {
    					patch_hit(obj)
    					count++
    				}
    			}
    			if (count < 1)
    				asmapi.log("ERROR", "Failed to modify DecoratedPotBlock: Method not found")
    			return classNode;
    		}
    	}
    }
}

// add canBreakPot test
function patch_hit(obj) {
	var fn = asmapi.mapMethod('m_305640_') // mayBreak
	var node = asmapi.findFirstMethodCall(obj, asmapi.MethodType.VIRTUAL, "net/minecraft/world/entity/projectile/Projectile", fn, "(Lnet/minecraft/world/level/Level;)Z")
	if (node) {
		node = node.getNext()
		if (node.getOpcode() == opc.IFEQ) {
			var lb = node.label
			var op1 = new VarInsnNode(opc.ALOAD, 1)
			var op2 = new VarInsnNode(opc.ALOAD, 3)
			var op3 = new VarInsnNode(opc.ALOAD, 4)
			var op4 = asmapi.buildMethodCall("com/lupicus/cc/events/BlockEvents", "canBreakPot", "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/projectile/Projectile;)Z", asmapi.MethodType.STATIC)
			var op5 = new JumpInsnNode(opc.IFEQ, lb)
			var list = asmapi.listOf(op1, op2, op3, op4, op5)
			obj.instructions.insert(node, list)
		}
		else
			asmapi.log("ERROR", "Failed to modify DecoratedPotBlock: call is different")
	}
	else
		asmapi.log("ERROR", "Failed to modify DecoratedPotBlock: call not found")
}
