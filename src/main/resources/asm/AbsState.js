var asmapi = Java.type('net.minecraftforge.coremod.api.ASMAPI')
var opc = Java.type('org.objectweb.asm.Opcodes')
var AbstractInsnNode = Java.type('org.objectweb.asm.tree.AbstractInsnNode')
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode')
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode')
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode')
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode')

function initializeCoreMod() {
    return {
    	'AbsState': {
    		'target': {
    			'type': 'CLASS',
    			'name': 'net.minecraft.block.AbstractBlock$AbstractBlockState'
    		},
    		'transformer': function(classNode) {
    			var count = 0
    			var fn = asmapi.mapMethod('func_196942_a') // onBlockClicked
    			for (var i = 0; i < classNode.methods.size(); ++i) {
    				var obj = classNode.methods.get(i)
    				if (obj.name == fn) {
    					patch_func_196942_a(obj)
    					count++
    				}
    			}
    			if (count < 1)
    				asmapi.log("ERROR", "Failed to modify AbstractBlockState: Method not found")
    			return classNode;
    		}
    	}
    }
}

// add conditional return
function patch_func_196942_a(obj) {
	var lb = null
	var flag = false
	var node = obj.instructions.getLast()
	while (node) {
		if (flag) {
			if (node.getOpcode() == -1) {
				if (node.getType() == AbstractInsnNode.LABEL) {
					lb = node
					break
				}
			}
			else {
				lb = new LabelNode()
				obj.instructions.insert(node, lb)
				break
			}
		}
		else if (node.getOpcode() == opc.RETURN) {
			flag = true
		}
		node = node.getPrevious()
	}
	if (lb) {
		var node = obj.instructions.getFirst()
		if (node.getType() == AbstractInsnNode.LABEL)
			node = node.getNext()
		var op1 = new VarInsnNode(opc.ALOAD, 1)
		var op2 = new VarInsnNode(opc.ALOAD, 2)
		var op3 = new VarInsnNode(opc.ALOAD, 3)
		var op4 = asmapi.buildMethodCall("com/lupicus/cc/events/PlayerEvents", "cancelBlockClick", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)Z", asmapi.MethodType.STATIC)
		var op5 = new JumpInsnNode(opc.IFNE, lb)
		var list = asmapi.listOf(op1, op2, op3, op4, op5)
		obj.instructions.insertBefore(node, list)
	}
	else if (flag)
		asmapi.log("INFO", "AbstractBlockState patch being skipped; call not found")
	else
		asmapi.log("ERROR", "Failed to modify AbstractBlockState: RETURN not found")
}
