package ATM;

public enum CashType {
    TEN(10), FIFTY(50), HUNDRED(100), FIVE_HUNDRED(500), THOUSAND(1000);
    private final int value;
    CashType(int value) { this.value = value; }
    public int getValue() { return value; }
}
