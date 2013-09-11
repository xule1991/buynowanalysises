import org.apache.log4j.Logger;

public class ErrorInfo implements Comparable<ErrorInfo> {
    private String errorType;
    private String firstLine;
    private String block;

    public ErrorInfo(String errorType, String firstLine, String block) {
        this.block = block;
        this.errorType = errorType;
        this.firstLine = firstLine;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getFirstLine() {
        return firstLine;
    }

    public String getBlock() {
        return block;
    }

    @Override
    public int compareTo(ErrorInfo o) {
        return this.errorType.compareTo(o.getErrorType());
    }

    public static void main(String[] args) {
        Logger logger = Logger.getLogger(ErrorInfo.class);
        logger.info("Info Level");
        logger.error("This is an error!");
        logger.error(new RuntimeException("Log an exception"));
    }


}
