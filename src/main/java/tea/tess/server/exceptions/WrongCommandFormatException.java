package tea.tess.server.exceptions;

/**
 * Created by arseniy on 12.10.15.
 */
public class WrongCommandFormatException extends Exception {
    public WrongCommandFormatException(String message) {
        super(message);
    }
}