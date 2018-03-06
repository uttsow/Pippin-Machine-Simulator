package project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class FullAssembler implements Assembler {
	private boolean readingCode;

	@Override
	public int assemble(String inputFileName, String outputFileName, StringBuilder error) {
		readingCode = true;
		if (error == null)
			throw new IllegalArgumentException("Coding error: the error buffer is null");
		int retVal = 0;

		File file = new File(inputFileName);
		try (Scanner scanner = new Scanner(new FileReader(file))){
			boolean blankLineFound = false;
			boolean blankLineReported = false;
			int blankLineNum = 0;
			boolean dataFound = false;
			int i = 0;
			while (scanner.hasNext()) {
				i++;
				String line = scanner.nextLine();
				if (line.trim().length() == 0 && !blankLineFound) {
					blankLineFound = true;
					blankLineNum = i;
				}
				else if (line.trim().length() != 0 && blankLineFound  && !blankLineReported) {
					error.append("\nIllegal blank line in the source file");
					retVal = blankLineNum;
					blankLineReported = true;
				}
				else if (line.trim().length() != 0) {
					if (line.charAt(0) == ' ' || line.charAt(0) == '\t') {
						error.append("\nLine starts with illegal white space");
						retVal = i;
					}
					if (line.trim().toUpperCase().equals("DATA")) {
						if (!readingCode) {
							error.append("\nFile contains more than one DATA seperator");
							retVal = i;
						}
						if (!line.trim().equals("DATA") && readingCode) {
							error.append("\nLine does not have DATA in upper case");
							retVal = i;
						}
						readingCode = false;

					}
					String[] parts = line.trim().split("\\s+");
					if (readingCode) {
						if (Instruction.opcodes.keySet().contains(parts[0].trim().toUpperCase())) {
							if (!Instruction.opcodes.keySet().contains(parts[0].trim())) {
								error.append("\nError on line " + (i) + ": mnemonic must be upper case");
								retVal = i;
							}
							if (noArgument.contains(parts[0]) && parts.length != 1) {
								error.append("\nError on line " + (i) + ": this mnemonic cannot take arguments");
								retVal = i;
							}
							else if (!noArgument.contains(parts[0]) && parts.length > 2) {
								error.append("\nError on line " + (i) + ": this mnemonic has too many arguments");
								retVal = i;
							}
							else if (!noArgument.contains(parts[0]) && parts.length < 2) {
								error.append("\nError on line " + (i) + ": this mnemonic is missing an argument");
								retVal = i;
							}
							else if (!noArgument.contains(parts[0]) && parts.length == 2) {
								try{
									if (parts[1].charAt(0) == '#') {
										parts[1] = parts[1].substring(1);
									}
									else if (parts[1].charAt(0) == '@') {
										parts[1] = parts[1].substring(1);
									}
									else if (parts[1].charAt(0) == '&') {
										parts[1] = parts[1].substring(1);
									}
									Integer.parseInt(parts[1],16);
								} catch(NumberFormatException e) {
									error.append("\nError on line " + (i) + 
											": argument is not a hex number");
									retVal = i;				
								}
							}
						}
						else {
							error.append("\nError on line " + (i) + ": illegal mnemonic");
							retVal = i;
						}
					}
					else if (dataFound) {
						if (parts.length != 2) {
							error.append("\nError on line " + (i) +
									": data must have length 2");
							retVal = i;
						}
						else {
							try {
								Integer.parseInt(parts[0],16);
								Integer.parseInt(parts[1],16);
							} catch(NumberFormatException e) {
								error.append("\nError on line " + (i) + 
										": data has non-numeric memory address");
								retVal =  i;				
							}
						}
					}
					if (!readingCode)
						dataFound = true;
				}
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			error.append("\nError: Unable to write the assembled program to the output file");
			retVal = -1;
		} catch (IOException e) {
			error.append("\nUnexplained IO Exception");
			retVal = -1;
		}

		if (retVal == 0)
			new SimpleAssembler().assemble(inputFileName, outputFileName, error);
		return retVal;
	}
	public static void main(String[] args) {
		StringBuilder error = new StringBuilder();
		System.out.println("Enter the name of the file without extension: ");
		try (Scanner keyboard = new Scanner(System.in)) { 
			String filename = keyboard.nextLine();
			int i = new FullAssembler().assemble(filename + ".pasm", 
					filename + ".pexe", error);
			System.out.println("result = " + i);
			System.out.println(error);
		}
	}
}
