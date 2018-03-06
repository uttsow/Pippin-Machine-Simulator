package project;

import static project.Instruction.*;


import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class MachineModel {

	private class CPU {
		private int accum;
		private int pc;
	}

	public final Map<Integer, Consumer<Instruction>> ACTION = new TreeMap<>();
	private CPU cpu = new CPU();
	private Memory memory = new Memory();
	private boolean withGUI = false;
	private HaltCallback callBack;

	public MachineModel(boolean withGUI, HaltCallback cb) {
		this.withGUI = withGUI;
		callBack = cb;

		// ACTION entry for "NOP" (0)
		ACTION.put(opcodes.get("NOP"), instr -> {
			int flags = instr.opcode & 6; // remove parity bit that will have been verified
			if (flags != 0) {
				String fString = "(" + (flags % 8 > 3 ? "1" : "0") + (flags % 4 > 1 ? "1" : "0") + ")";
				throw new IllegalInstructionException("Illegal flags for this instruction: " + fString);
			}
			cpu.pc++;
		});

		// NOT (logical not operation on the accumulator), key for ACTION is 1
		// (opcodes.get("NOT").
		// As in the case of NOT, all flags must be 0 to avoid an exception.
		// if the accumulator is 0, set it to 1, otherwise set it to 0. Increment the
		// program counter.
		ACTION.put(opcodes.get("NOT"), instr -> {
			int flags = instr.opcode & 6; // removes parity
			if (flags == 0) {
				if (cpu.accum == 0)
					cpu.accum = 1;
				else
					cpu.accum = 0;
			} else {
				String fString = "(" + (flags % 8 > 3 ? "1" : "0") + (flags % 4 > 1 ? "1" : "0") + ")";
				throw new IllegalInstructionException("Illegal flags for this instruction: " + fString);
			}
			cpu.pc++;
		});

		// HALT (halt execution), key for ACTION is 2 (opcodes.get("HALT").
		// As in the case of NOP, all flags must be 0 to avoid an exception. Call the
		// method halt().
		// Do not increment the program counter.
		ACTION.put(opcodes.get("HALT"), instr -> {
			int flags = instr.opcode & 6;
			if (flags == 0)
				halt();
			else {
				String fString = "(" + (flags % 8 > 3 ? "1" : "0") + (flags % 4 > 1 ? "1" : "0") + ")";
				throw new IllegalInstructionException("Illegal flags for this instruction: " + fString);
			}

		});

		// LOD (load accumulator), key for ACTION is 3 (opcodes.get("LOD").
		// For direct addressing puts the value memory.getData(arg) in cpu.accum;
		// for immediate addressing puts arg in cpu.accum;
		// for indirect addressing puts the value memory.getData(memory.getData(arg)) in
		// cpu.accum.
		// The other value of flags causes IllegalInstructionException
		ACTION.put(opcodes.get("LOD"), instr -> {
			int flags = instr.opcode & 6;
			if (flags == 0) { // direct accessing
				cpu.accum = memory.getData(instr.arg);
			} else if (flags == 2) { // immediate addressing
				cpu.accum = instr.arg;
			} else if (flags == 4) { // Indirect addressing
				cpu.accum = memory.getData(memory.getData(instr.arg));
			} else { // illegal case: "01", "11"
				String fString = "(" + (flags % 8 > 3 ? "1" : "0") + (flags % 4 > 1 ? "1" : "0") + ")";
				throw new IllegalInstructionException("Illegal flags for this instruction: " + fString);
			}
			cpu.pc++;
		});

		// STO (store accumulator in memory), key for ACTION is 4 (opcodes.get("STO").
		// For direct addressing puts the value cpu.accum in memory at index arg using
		// setData ;
		// immediate addressing is not legal and is handled in the else part;
		// for indirect addressing puts the value cpu.accum in memory at index
		// memory.getData(arg) using setData.
		// Any other values of flags cause IllegalInstructionException
		ACTION.put(opcodes.get("STO"), instr -> {
			int flags = instr.opcode & 6;
			if (flags == 0) {
				memory.setData(instr.arg, cpu.accum);
			} else if (flags == 4) {
				memory.setData(memory.getData(instr.arg), cpu.accum);
			} else { // cant have "10" (2)
				String fString = "(" + (flags % 8 > 3 ? "1" : "0") + (flags % 4 > 1 ? "1" : "0") + ")";
				throw new IllegalInstructionException("Illegal flags for this instruction: " + fString);
			}
			cpu.pc++;
		});

		// ADD, key is 5
		ACTION.put(opcodes.get("ADD"), instr -> {
			int flags = instr.opcode & 6; // remove parity bit that will have been verified
			if (flags == 0) { // direct addressing
				cpu.accum += memory.getData(instr.arg);
			} else if (flags == 2) { // immediate addressing
				cpu.accum += instr.arg;
			} else if (flags == 4) { // indirect addressing
				cpu.accum += memory.getData(memory.getData(instr.arg));
			} else {
				String fString = "(" + (flags % 8 > 3 ? "1" : "0") + (flags % 4 > 1 ? "1" : "0") + ")";
				throw new IllegalInstructionException("Illegal flags for this instruction: " + fString);
			}
			cpu.pc++;
		});

		// SUB (subtract from the accumulator), key for ACTION is 6 (opcodes.get("SUP").
		// Just repeat ADD but replace "+=" by "-="
		ACTION.put(opcodes.get("SUB"), instr -> {
			int flags = instr.opcode & 6; // remove parity bit that will have been verified
			if (flags == 0) { // direct addressing
				cpu.accum -= memory.getData(instr.arg);
			} else if (flags == 2) { // immediate addressing
				cpu.accum -= instr.arg;
			} else if (flags == 4) { // indirect addressing
				cpu.accum -= memory.getData(memory.getData(instr.arg));
			} else {
				String fString = "(" + (flags % 8 > 3 ? "1" : "0") + (flags % 4 > 1 ? "1" : "0") + ")";
				throw new IllegalInstructionException("Illegal flags for this instruction: " + fString);
			}
			cpu.pc++;
		});
		// MUL (multiply the accumulator), key for ACTION is 7 (opcodes.get("MUL").
		// Just repeat ADD but replace "+=" by "*="
		ACTION.put(opcodes.get("MUL"), instr -> {
			int flags = instr.opcode & 6; // remove parity bit that will have been verified
			if (flags == 0) { // direct addressing
				cpu.accum *= memory.getData(instr.arg);
			} else if (flags == 2) { // immediate addressing
				cpu.accum *= instr.arg;
			} else if (flags == 4) { // indirect addressing
				cpu.accum *= memory.getData(memory.getData(instr.arg));
			} else {
				String fString = "(" + (flags % 8 > 3 ? "1" : "0") + (flags % 4 > 1 ? "1" : "0") + ")";
				throw new IllegalInstructionException("Illegal flags for this instruction: " + fString);
			}
			cpu.pc++;
		});

		// DIV (divide the accumulator), key for ACTION is 8 (opcodes.get("DIV").
		// Basically just repeat ADD but replace "+=" by "/=",
		// BUT before each division, check if the division will be by 0.
		// If the division is by 0, throw DivideByZeroException instead.
		ACTION.put(opcodes.get("DIV"), instr -> {
			int flags = instr.opcode & 6; // remove parity bit that will have been verified
			if (flags == 0) { // direct addressing
				if (memory.getData(instr.arg) == 0)
					throw new DivideByZeroException("Cant divide by zero");
				else
					cpu.accum /= memory.getData(instr.arg);
			} else if (flags == 2) { // immediate addressing
				if (instr.arg == 0)
					throw new DivideByZeroException("Cant divide by zero");
				else
					cpu.accum /= instr.arg;
			} else if (flags == 4) { // indirect addressing
				if (memory.getData(memory.getData(instr.arg)) == 0)
					throw new DivideByZeroException("Cant divide by zero");
				else
					cpu.accum /= memory.getData(memory.getData(instr.arg));
			} else {
				String fString = "(" + (flags % 8 > 3 ? "1" : "0") + (flags % 4 > 1 ? "1" : "0") + ")";
				throw new IllegalInstructionException("Illegal flags for this instruction: " + fString);
			}
			cpu.pc++;
		});

		// AND (logical and with the accumulator--not 0 means FALSE, non-zero means
		// TRUE), key for ACTION is 9 (opcodes.get("AND").
		// Only direct and immediate addressing are permitted, the other flags values
		// throw the exception.
		// For direct addressing, if the accumulator and memory.getData(arg) are not
		// zero, set the accumulator to 1, otherwise to 0.
		// For immediate addressing, if the accumulator and arg are not zero,
		// set the accumulator to 1, otherwise to 0. In all cases increment the program
		// counter.
		ACTION.put(opcodes.get("AND"), instr -> {
			int flags = instr.opcode & 6;
			if (flags == 0) { // direct addressing
				if (cpu.accum != 0 && memory.getData(instr.arg) != 0)
					cpu.accum = 1;
				else
					cpu.accum = 0;
			} else if (flags == 2) { // immediate addressing
				if (cpu.accum != 0 && instr.arg != 0)
					cpu.accum = 1;
				else
					cpu.accum = 0;
			} else {
				String fString = "(" + (flags % 8 > 3 ? "1" : "0") + (flags % 4 > 1 ? "1" : "0") + ")";
				throw new IllegalInstructionException("Illegal flags for this instruction: " + fString);
			}
			cpu.pc++;
		});

		// JUMP (jump the program counter to a new location in the code), key for ACTION
		// is 0xA (opcodes.get("JUMP").
		// For direct addressing adds the value arg to cpu.pc (this is a relative jump);
		// for immediate addressing puts arg in cpu.pc (this is an absolute jump);
		// for indirect addressing adds the value memory.getData(arg) to cpu.pc.
		// For the other value of flags (actually it is 6) loads value
		// memory.getData(arg) in cpu.pc.
		// Note that all values of flags have a meaning so there is no exception.<br />
		ACTION.put(opcodes.get("JUMP"), instr -> {
			int flags = instr.opcode & 6;
			if (flags == 0) { // direct addressing
				cpu.pc += instr.arg;
			} else if (flags == 2) { // immediate addressing
				cpu.pc = instr.arg;
			} else if (flags == 4) { // indrect addressing
				cpu.pc += memory.getData(instr.arg);
			} else {
				cpu.pc = memory.getData(instr.arg);
			}
		});

		// JMPZ (conditionally jump the program counter to a new location in the code),
		// key for ACTION is 0xB (opcodes.get("JMPZ").
		// Does all the things JUMP does if cpu.accum is zero and otherwise only
		// increments cpu.pc
		ACTION.put(opcodes.get("JMPZ"), instr -> {
			int flags = instr.opcode & 6;
			if (cpu.accum == 0) {
				if (flags == 0) { // direct addressing
					cpu.pc += instr.arg;
				} else if (flags == 2) { // immediate addressing
					cpu.pc = instr.arg;
				} else if (flags == 4) { // indrect addressing
					cpu.pc += memory.getData(instr.arg);
				} else {
					cpu.pc = memory.getData(instr.arg);
				}
			} else
				cpu.pc++;
		});

		// CMPL (compare less than 0), key for ACTION is 0xC (opcodes.get("CMPL").
		// As in the case of NOP, all flags must be 0 to avoid an exception.
		// if memory.getData(arg) is less than 0, set it the accumulator to 1, otherwise
		// set it to 0. Increment the program counter.
		ACTION.put(opcodes.get("CMPL"), instr -> {
			int flags = instr.opcode & 6;
			if (flags == 0) {
				if (memory.getData(instr.arg) < 0)
					cpu.accum = 1;
				else
					cpu.accum = 0;
			} else {
				String fString = "(" + (flags % 8 > 3 ? "1" : "0") + (flags % 4 > 1 ? "1" : "0") + ")";
				throw new IllegalInstructionException("Illegal flags for this instruction: " + fString);
			}
			cpu.pc++;
		});

		// CMPZ (compare to 0), key for ACTION is 0xD (opcodes.get("CMPZ").
		// As in the case of NOP, all flags must be 0 to avoid an exception.
		// if memory.getData(arg) is 0, set it the accumulator to 1, otherwise set it to
		// 0. Increment the program counter.
		ACTION.put(opcodes.get("CMPZ"), instr -> {
			int flags = instr.opcode & 6;
			if (flags == 0) {
				if (memory.getData(instr.arg) == 0)
					cpu.accum = 1;
				else
					cpu.accum = 0;
			} else {
				String fString = "(" + (flags % 8 > 3 ? "1" : "0") + (flags % 4 > 1 ? "1" : "0") + ")";
				throw new IllegalInstructionException("Illegal flags for this instruction: " + fString);
			}
			cpu.pc++;

		});

	}

	// 2nd contructor
	public MachineModel() {
		this(false, null);
	}

	// -----------------------------------------methods---------
	
	public void halt() {
		if(!withGUI) System.exit(0);
		callBack.halt();
	}
	
	public int getData(int index) {
		return memory.getData(index);
	}

	public void setData(int i, int j) {
		memory.setData(i, j);
	}

	int[] getData() {
		return memory.getData();
	}

	int[] getData(int min, int max) {
		return memory.getData(min, max);
	}

	public Instruction getCode(int index) {
		return memory.getCode(index);
	}

	public void setCode(int i, Instruction j) {
		memory.setCode(i, j);
	}

	public Instruction[] getCode() {
		return memory.getCode();
	}

	public Instruction[] getCode(int min, int max) {
		return memory.getCode(min, max);
	}

	public int getProgramSize() {
		return memory.getProgramSize();
	}

	public int getAccum() {
		return cpu.accum;
	}

	public void setAccum(int pc) {
		cpu.accum = pc;
	}

	public int getPC() {
		return cpu.pc;
	}
	
	public void setPC(int pc) {
		cpu.pc = pc;
	}

	public int getChangedDataIndex() {
		return memory.getChangedDataIndex();
	}
	
	public void setProgramSize(int i) {
		memory.setProgramSize(i);
	}
	
	public void clear() {
		memory.clearCode();
		memory.clearData();
		cpu.accum = 0;
		cpu.pc = 0;
	}
	
	public void step() {
		try {
			Instruction instr = memory.getCode(cpu.pc);
			Instruction.checkParity(instr);
			ACTION.get(instr.opcode/8).accept(instr);	
		}catch(Exception e){
			halt();
			throw e;
		}
	}
	
	
	

	

	
	
	
	
	
	
	
	/*
	 * public static void main(String[] args) { MachineModel model = new
	 * MachineModel(false); for(int i = 0; i < Memory.DATA_SIZE; i++)
	 * model.memory.setData(i, 3*i);
	 * System.out.println(Arrays.toString(model.memory.getData(0,20))); Instruction
	 * instr = new Instruction((byte)0,0); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); System.out.println("NOP");
	 * System.out.println("Acc: " + model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00001001,0); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); System.out.println("NOT");
	 * System.out.println("Acc: " + model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00011000,12); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("LOD direct addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00011011,12); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("LOD immediate addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00011101,3); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("LOD indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00100001,3); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("STO direct addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00100100,2); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("STO indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00101000,12); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("ADD direct addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00101011,12); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("ADD immediate addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00101101,4); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("ADD indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00110000,12); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("SUB direct addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00110011,12); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("SUB immediate addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00110101,4); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("SUB indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00111001,12); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("MUL direct addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00111010,12); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("MUL immediate addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b00111100,4); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("MUL indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01000001,12); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("DIV direct addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01000010,12); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("DIV immediate addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01000100,4); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("DIV indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01001000,12); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("AND direct addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01001011,12); Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("AND immediate addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01001000,12); Instruction.checkParity(instr);
	 * model.setAccum(0); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("AND direct addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01001011,12); Instruction.checkParity(instr);
	 * model.setAccum(0); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("AND immediate addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01001000,0); Instruction.checkParity(instr);
	 * model.setAccum(10); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("AND direct addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01001011,0); Instruction.checkParity(instr);
	 * model.setAccum(10); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("AND immediate addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01010011,5); Instruction.checkParity(instr);
	 * model.setAccum(10); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("JUMP immediate addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01010000,25); Instruction.checkParity(instr);
	 * model.setAccum(10); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("JUMP direct addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01010101,10); Instruction.checkParity(instr);
	 * model.setAccum(10); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("JUMP indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01010110,11); Instruction.checkParity(instr);
	 * model.setAccum(10); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("JUMP indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01011010,5); Instruction.checkParity(instr);
	 * model.setAccum(0); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("JMPZ immediate addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01011001,25); Instruction.checkParity(instr);
	 * model.setAccum(0); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("JMPZ direct addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01011100,10); Instruction.checkParity(instr);
	 * model.setAccum(0); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("JMPZ indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01011111,11); Instruction.checkParity(instr);
	 * model.setAccum(0); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("JMPZ indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01011010,5); Instruction.checkParity(instr);
	 * model.setAccum(1); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("JMPZ immediate addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01011001,25); Instruction.checkParity(instr);
	 * model.setAccum(2); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("JMPZ direct addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01011100,10); Instruction.checkParity(instr);
	 * model.setAccum(3); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("JMPZ indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01011111,11); Instruction.checkParity(instr);
	 * model.setAccum(4); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("JMPZ indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01100000,10); Instruction.checkParity(instr);
	 * model.setAccum(3); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("CMPL indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01100000,10); Instruction.checkParity(instr);
	 * model.setData(10, -2); model.setAccum(0);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("CMPL indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01101001,10); Instruction.checkParity(instr);
	 * model.setAccum(3); model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("CMPZ indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01101001,10); Instruction.checkParity(instr);
	 * model.setData(10, 2); model.setAccum(3);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("CMPZ indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * instr = new Instruction((byte)0b01101001,10); Instruction.checkParity(instr);
	 * model.setData(10, 0); model.setAccum(3);
	 * model.ACTION.get(instr.opcode/8).accept(instr);
	 * System.out.println("CMPZ indirect addressing"); System.out.println("Acc: " +
	 * model.getAccum() + ", PC: " + model.getPC());
	 * System.out.println(Arrays.toString(model.memory.getData(0,20)));
	 * 
	 * 
	 * try { instr = new Instruction((byte)0b00000001, 0);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("1-NOP parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("1-NOP parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00000011,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("2-NOP flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) { System.out.println("2-NOP flags check OK");
	 * }
	 * 
	 * try { instr = new Instruction((byte)0b00000010,0);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("3-NOP parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("3-NOP parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00000101,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("4-NOP flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) { System.out.println("4-NOP flags check OK");
	 * }
	 * 
	 * try { instr = new Instruction((byte)0b00000100,0);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("5-NOP parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("5-NOP parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00000110,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("6-NOP flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) { System.out.println("6-NOP flags check OK");
	 * }
	 * 
	 * try { instr = new Instruction((byte)0b00000111,0);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("7-NOP parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("7-NOP parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00001000,0);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("8-NOT parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("8-NOT parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00001010,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("9-NOT flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) { System.out.println("9-NOT flags check OK");
	 * }
	 * 
	 * try { instr = new Instruction((byte)0b00001011,0);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("10-NOT parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("10-NOT parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00001100,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("11-NOT flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("11-NOT flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00001101,0);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("12-NOT parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("12-NOT parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00001111,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("13-NOT flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("13-NOT flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00001110,0);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("14-NOT parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("14-NOT parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00011001,12);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("15-LOD parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("15-LOD parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00011010,12);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("16-LOD parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("16-LOD parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00011100,3);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("17-LOD parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("17-LOD parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00011110,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("18-LOD flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("18-LOD flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00011111,0);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("19-LOD parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("19-LOD parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00100000,3);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("20-STO parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("20-STO parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00100101,3);
	 * Instruction.checkParity(instr); throw new
	 * RuntimeException("21-STO parity check FAILED>>>>>>>>>>>>>"); } catch
	 * (ParityCheckException e) { System.out.println("21-STO parity check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00100010,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("22-STO flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("22-STO flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00100111,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("23-STO flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("23-STO flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00101110,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("24-ADD flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("24-ADD flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00110110,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("25-SUB flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("25-SUB flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b00111111,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("26-MUL flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("26-MUL flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b01000111,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("27-DIV flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("27-DIV flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b01000010,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("28-DIV zero div immed check FAILED>>>>>>>>>>>>>"); } catch
	 * (DivideByZeroException e) {
	 * System.out.println("28-DIV zero div immed check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b01000001,10);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("29-DIV zero div dir check FAILED>>>>>>>>>>>>>"); } catch
	 * (DivideByZeroException e) {
	 * System.out.println("29-DIV zero div dir check OK"); }
	 * 
	 * model.setData(2, 10); try { instr = new Instruction((byte)0b01000100,2);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("30-DIV zero div indir check FAILED>>>>>>>>>>>>>"); } catch
	 * (DivideByZeroException e) {
	 * System.out.println("30-DIV zero div indir check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b01001101,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("31-AND flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("31-AND flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b01001110,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("32-AND flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("32-AND flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b01100011,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("33-CMPL flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("33-CMPL flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b01100101,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("34-CMPL flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("34-CMPL flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b01100110,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("35-CMPL flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("35-CMPL flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b01101010,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("36-CMPZ flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("36-CMPZ flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b01101100,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("37-CMPZ flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("37-CMPZ flags check OK"); }
	 * 
	 * try { instr = new Instruction((byte)0b01101111,0);
	 * Instruction.checkParity(instr);
	 * model.ACTION.get(instr.opcode/8).accept(instr); throw new
	 * RuntimeException("38-CMPZ flag check FAILED>>>>>>>>>>>>>"); } catch
	 * (IllegalInstructionException e) {
	 * System.out.println("38-CMPZ flags check OK"); } }
	 */
}
