public enum ErrorCodes {
    UNPLUGGED(-1.0),
    NOT_CONNECTED(-2.0),
    CHECK_SUM_ERROR(-3.0),
    ;
    public final double code;
    ErrorCodes(double code) {
        this.code = code;
    }
}
