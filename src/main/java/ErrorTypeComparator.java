import java.util.Comparator;

public class ErrorTypeComparator implements Comparator<ErrorInfo> {
    @Override
    public int compare(ErrorInfo o1, ErrorInfo o2) {
        return o1.getErrorType().compareTo(o2.getErrorType());
    }
}
