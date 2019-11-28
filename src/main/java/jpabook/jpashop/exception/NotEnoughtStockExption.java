package jpabook.jpashop.exception;

public class NotEnoughtStockExption extends RuntimeException{
    public NotEnoughtStockExption() {
        super();
    }

    public NotEnoughtStockExption(String message) {
        super(message);
    }

    public NotEnoughtStockExption(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEnoughtStockExption(Throwable cause) {
        super(cause);
    }

    protected NotEnoughtStockExption(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
