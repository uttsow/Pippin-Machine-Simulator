package project;

public class DivideByZeroException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public DivideByZeroException(String x){
		super(x);
	}
	
}
