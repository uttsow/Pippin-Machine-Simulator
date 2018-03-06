package project;

import java.util.IllegalFormatFlagsException;

public class ParityCheckException extends IllegalFormatFlagsException{
	public ParityCheckException(String str) {
		super(str);
	}
}
