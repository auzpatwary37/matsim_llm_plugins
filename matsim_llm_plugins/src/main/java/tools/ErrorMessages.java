package tools;

import java.util.ArrayList;
import java.util.List;

public class ErrorMessages{
	private List<String> errorMessages = new ArrayList<>();
	
	public void addErrorMessages(String m) {
		this.errorMessages.add(m);
	}
	
	public void addErrorMessages(ErrorMessages m) {
		this.errorMessages.addAll(m.getErrorMessages());
	}
	
	public List<String> getErrorMessages() {
		return this.errorMessages;
	}
	
	public String getCombinedErrorMessages() {
		String s = "";
		String sep = "";
		for(String ss:this.errorMessages) {
			s=s+sep+ss;
			sep = "\n";
		}
		return s;
	}
	
	public boolean isEmpty() {
		return this.errorMessages.isEmpty();
	}
}
