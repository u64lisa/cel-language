package language.backend.compiler.asm;

import language.backend.compiler.asm.inst.Parameter;

class AsmUtils {
	public static String getPointerName(int size) {
		return switch (size) {
			case 8 -> "byte";
			case 16 -> "word";
			case 32 -> "dword";
			case 64 -> "qword";
			default -> throw new RuntimeException();
		};
	}

	public static String getRefValue(Parameter ref, ASMProcedure proc) {
		return getStackPtr(ref, proc);
	}

	public static String getParamValue(Parameter param, ASMProcedure proc) {
		if (param.getType() == Parameter.Type.REF)
			return getStackPtr(param, proc);

		if (param.getType() == Parameter.Type.NUM)
			return param.getValueType().toString();

		return "ERROR!!!!";
	}

	public static String getRawStackPtr(Parameter ref, int offset, ASMProcedure proc) {
		return "[RBP - 0x%x]".formatted(
				proc.getStackOffset(ref) - offset
		);
	}

	public static String getStackPtr(Parameter ref, ASMProcedure proc) {
		return "%s [RBP - 0x%x]".formatted(
				getPointerName(ref),
				proc.getStackOffset(ref)
		);
	}

	public static String getPointerName(Parameter ref) {
		return getPointerName(getTypeSize(ref));
	}

	public static int getLowerTypeSize(Parameter type) {
		return ((type.getValueType().getDepth() > 1) ? getPointerSize() : (type.getValueType().getSize() >> 3)) << 3;
	}

	public static int getTypeSize(Parameter type) {
		return getTypeByteSize(type) << 3;
	}

	public static int getTypeByteSize(Parameter type) {
		return (type.getValueType().getDepth() > 0) ? getPointerSize() : (type.getValueType().getSize() >> 3);
	}

	public static int getLowerTypeByteSize(Parameter type) {
		return ((type.getValueType().getDepth() > 1) ? getPointerSize() : (type.getValueType().getSize() >> 3));
	}

	// TODO: Read this from some config
	public static int getPointerSize() {
		return 8;
	}
}
