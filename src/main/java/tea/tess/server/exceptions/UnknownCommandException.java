package tea.tess.server.exceptions;

/**
 * Created by arseniy on 12.10.15.
 */
public class UnknownCommandException extends Exception {
    public UnknownCommandException(String message) {
        super(message);
    }
}