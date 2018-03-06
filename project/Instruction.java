package project;

import java.util.Map;
import java.util.TreeMap;

public class Instruction {

	byte opcode;
	int arg;
	public static Map<String, Integer> opcodes = new TreeMap<>();
	public static Map<Integer, String> mnemonics = new TreeMap<>();
	static {
		opcodes.put("NOP", 0);
		opcodes.put("NOT", 1);
		opcodes.put("HALT", 2);
		opcodes.put("LOD", 3);
		opcodes.put("STO", 4);
		opcodes.put("ADD", 5);
		opcodes.put("SUB", 6);
		opcodes.put("MUL", 7);
		opcodes.put("DIV", 8);
		opcodes.put("AND", 9);
		opcodes.put("JUMP", 10);
		opcodes.put("JMPZ", 11);
		opcodes.put("CMPL", 12);
		opcodes.put("CMPZ", 13);
		for(String str : opcodes.keySet()) 
			mnemonics.put(opcodes.get(str), str);
	}

	public Instruction(byte opcode, int arg) {
		super();
		this.opcode = opcode;
		this.arg = arg;
	}

	public static boolean noArgument(Instruction instr) {
		return (instr.opcode < 24);
	}

	static int numOnes(int input) {
		int numOnes = 0;
		String inputString = Integer.toUnsignedString(input, 2);
		for (char c : inputString.toCharArray())
			if (c == '1')
				numOnes++;
		return numOnes;
	}

	static void checkParity(Instruction instr) {
		if(numOnes(instr.opcode)%2 == 1)
			throw new ParityCheckException("This instruction is corrupted");
	}
	
	public String getText() {
		StringBuilder buff = new StringBuilder();
		buff.append(mnemonics.get(opcode/8));
		buff.append("  ");
		int flags = opcode & 6;
		if (flags == 2)
			buff.append("#");
		else if (flags == 4)
			buff.append("@");
		else if (flags == 6)
			buff.append("&");
		buff.append(Integer.toString(arg,16));
		return buff.toString().toUpperCase();
	}
	
	public String getBinHex() {
		String s = "00000000"+Integer.toString(opcode,2);
		StringBuilder buff = new StringBuilder();
		buff.append(s.substring(s.length()-8));
		buff.append("  ");
		buff.append(Integer.toHexString(arg));
		return buff.toString().toUpperCase();
	}
	
	public String toString() {
		return "Instruction [" + Integer.toString(opcode,2) + ", " + Integer.toString(arg, 16)+"]";
	}
}
