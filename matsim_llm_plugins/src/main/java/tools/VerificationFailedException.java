package tools;

import java.util.List;

public class VerificationFailedException extends RuntimeException {

    private static final long serialVersionUID = 872842555090518894L;
	private final List<String> errors;
    private final String formattedMessage;

    public VerificationFailedException(List<String> errors) {
        super(); // Don't call super(String) — override getMessage instead
        this.errors = errors;
        this.formattedMessage = format(errors);
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
        return formattedMessage;
    }

    private static String format(List<String> errors) {
        if (errors.isEmpty()) return "Verification failed with no explanation.";
        StringBuilder sb = new StringBuilder("Verification failed with the following errors:\n");
        for (String err : errors) {
            sb.append(" - ").append(err).append("\n");
        }
        return sb.toString().trim();
    }
}
