import java.math.BigDecimal;


public class BankTest {

    private static int passed = 0;
    private static int failed = 0;

    interface Action {
        void run() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        testDeposit();
        testInvalidDepositsRejected();
        testSavingsCannotOverdraw();
        testCheckingOverdraftWithinLimit();
        testCheckingBlockedBeyondLimit();
        testInvalidWithdrawalRejected();
        testTransferMovesMoney();
        testFailedTransferChangesNothing();
        testTransferToSameAccountRejected();
        testTransferToUnknownAccountRejected();
        testMonthlyInterest();
        testTransactionHistoryIsReadOnly();
        testBadConstructorsRejected();
        testAccountNumbersAreSequential();
        testFailedCreationDoesNotUseAccountNumber();

        System.out.println("\nPassed: " + passed + "  Failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }


    static void testDeposit() {
        Bank bank = new Bank();
        Account a = bank.openSavings("Ali", bd("0.05"));
        a.deposit(bd("100"));
        check("deposit increases balance", eq(a.getBalance(), "100"));
        check("deposit is recorded in history",
                a.getTransactions().size() == 1
                        && a.getTransactions().get(0).getType() == Transaction.Type.DEPOSIT);
    }

    static void testInvalidDepositsRejected() {
        Bank bank = new Bank();
        Account a = bank.openSavings("Ali", bd("0.05"));
        check("deposit of null rejected", throwsException(IllegalArgumentException.class, () -> a.deposit(null)));
        check("deposit of 0 rejected", throwsException(IllegalArgumentException.class, () -> a.deposit(bd("0"))));
        check("deposit of negative rejected", throwsException(IllegalArgumentException.class, () -> a.deposit(bd("-5"))));
        check("rejected deposits change nothing", a.getBalance().signum() == 0 && a.getTransactions().isEmpty());
    }

    static void testSavingsCannotOverdraw() {
        Bank bank = new Bank();
        Account a = bank.openSavings("Ali", bd("0.05"));
        a.deposit(bd("50"));
        check("savings withdrawal above balance is declined",
                throwsException(InsufficientFundsException.class, () -> a.withdraw(bd("80"))));
        check("declined withdrawal leaves balance and history unchanged",
                eq(a.getBalance(), "50") && a.getTransactions().size() == 1);
    }

    static void testCheckingOverdraftWithinLimit() throws Exception {
        Bank bank = new Bank();
        Account a = bank.openChecking("Sara", bd("200"));
        a.deposit(bd("50"));
        a.withdraw(bd("200")); 
        check("checking can go negative within the overdraft limit", eq(a.getBalance(), "-150"));
    }

    static void testCheckingBlockedBeyondLimit() throws Exception {
        Bank bank = new Bank();
        Account a = bank.openChecking("Sara", bd("200"));
        a.deposit(bd("50"));
        a.withdraw(bd("200")); // -150
        check("checking blocked beyond the overdraft limit",
                throwsException(InsufficientFundsException.class, () -> a.withdraw(bd("100"))));
        check("balance unchanged after blocked withdrawal", eq(a.getBalance(), "-150"));
    }

    static void testInvalidWithdrawalRejected() {
        Bank bank = new Bank();
        Account a = bank.openSavings("Ali", bd("0.05"));
        a.deposit(bd("100"));
        check("withdraw of null rejected", throwsException(IllegalArgumentException.class, () -> a.withdraw(null)));
        check("withdraw of 0 rejected", throwsException(IllegalArgumentException.class, () -> a.withdraw(bd("0"))));
        check("withdraw of negative rejected", throwsException(IllegalArgumentException.class, () -> a.withdraw(bd("-5"))));
        check("rejected withdrawals change nothing", eq(a.getBalance(), "100"));
    }

    static void testTransferMovesMoney() throws Exception {
        Bank bank = new Bank();
        Account a = bank.openSavings("Ali", bd("0.05"));
        Account b = bank.openChecking("Sara", bd("0"));
        a.deposit(bd("300"));
        bank.transfer(a.getAccountNumber(), b.getAccountNumber(), bd("120"));
        check("transfer moves money correctly", eq(a.getBalance(), "180") && eq(b.getBalance(), "120"));
        check("transfer is recorded as TRANSFER_OUT and TRANSFER_IN",
                a.getTransactions().get(1).getType() == Transaction.Type.TRANSFER_OUT
                        && b.getTransactions().get(0).getType() == Transaction.Type.TRANSFER_IN);
    }

    static void testFailedTransferChangesNothing() {
        Bank bank = new Bank();
        Account a = bank.openSavings("Ali", bd("0.05"));
        Account b = bank.openSavings("Sara", bd("0.05"));
        a.deposit(bd("10"));
        check("transfer above balance is declined",
                throwsException(InsufficientFundsException.class,
                        () -> bank.transfer(a.getAccountNumber(), b.getAccountNumber(), bd("500"))));
        check("failed transfer changes no balances", eq(a.getBalance(), "10") && b.getBalance().signum() == 0);
        check("failed transfer adds no history entries",
                a.getTransactions().size() == 1 && b.getTransactions().isEmpty());
    }

    static void testTransferToSameAccountRejected() {
        Bank bank = new Bank();
        Account a = bank.openSavings("Ali", bd("0.05"));
        a.deposit(bd("100"));
        check("transfer to the same account rejected",
                throwsException(IllegalArgumentException.class,
                        () -> bank.transfer(a.getAccountNumber(), a.getAccountNumber(), bd("10"))));
        check("balance unchanged after rejected self-transfer", eq(a.getBalance(), "100"));
    }

    static void testTransferToUnknownAccountRejected() {
        Bank bank = new Bank();
        Account a = bank.openSavings("Ali", bd("0.05"));
        a.deposit(bd("100"));
        check("transfer to a missing account rejected",
                throwsException(IllegalArgumentException.class,
                        () -> bank.transfer(a.getAccountNumber(), 9999, bd("10"))));
        check("balance unchanged after transfer to missing account", eq(a.getBalance(), "100"));
    }

    static void testMonthlyInterest() {
        Bank bank = new Bank();
        Account funded = bank.openSavings("Ali", bd("0.12")); // 12% a year = 1% a month
        Account empty = bank.openSavings("Empty", bd("0.12"));
        Account checking = bank.openChecking("Sara", bd("0"));
        funded.deposit(bd("1000"));
        checking.deposit(bd("1000"));
        int updated = bank.applyMonthlyInterest();
        check("monthly interest is 1% at 12% annual", eq(funded.getBalance(), "1010.00"));
        check("interest is recorded as INTEREST",
                funded.getTransactions().get(1).getType() == Transaction.Type.INTEREST);
        check("only the funded savings account got interest", updated == 1);
        check("empty savings and checking accounts get no interest",
                empty.getBalance().signum() == 0 && eq(checking.getBalance(), "1000"));
    }

    static void testTransactionHistoryIsReadOnly() throws Exception {
        Bank bank = new Bank();
        Account a = bank.openChecking("Ali", bd("0"));
        a.deposit(bd("100"));
        a.withdraw(bd("30"));
        check("history has 2 entries", a.getTransactions().size() == 2);
        check("history cannot be edited from outside",
                throwsException(UnsupportedOperationException.class, () -> a.getTransactions().clear()));
    }

    static void testBadConstructorsRejected() {
        check("empty owner name rejected",
                throwsException(IllegalArgumentException.class, () -> new CheckingAccount(1, "", bd("100"))));
        check("blank owner name rejected",
                throwsException(IllegalArgumentException.class, () -> new SavingsAccount(1, "   ", bd("0.05"))));
        check("negative interest rate rejected",
                throwsException(IllegalArgumentException.class, () -> new SavingsAccount(1, "Ali", bd("-1"))));
        check("negative overdraft limit rejected",
                throwsException(IllegalArgumentException.class, () -> new CheckingAccount(1, "Ali", bd("-1"))));
    }

    static void testAccountNumbersAreSequential() {
        Bank bank = new Bank();
        Account a = bank.openSavings("Ali", bd("0.05"));
        Account b = bank.openChecking("Sara", bd("0"));
        check("account numbers start at 1001 and increase by 1",
                a.getAccountNumber() == 1001 && b.getAccountNumber() == 1002);
    }

    static void testFailedCreationDoesNotUseAccountNumber() {
        Bank bank = new Bank();
        throwsException(IllegalArgumentException.class, () -> bank.openSavings("", bd("0.05")));
        throwsException(IllegalArgumentException.class, () -> bank.openChecking("Ali", bd("-5")));
        Account a = bank.openSavings("Ali", bd("0.05"));
        check("failed account creation does not skip an account number", a.getAccountNumber() == 1001);
    }



    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

   
    private static boolean eq(BigDecimal actual, String expected) {
        return actual.compareTo(new BigDecimal(expected)) == 0;
    }


    private static boolean throwsException(Class<? extends Exception> type, Action action) {
        try {
            action.run();
            return false;
        } catch (Exception e) {
            return type.isInstance(e);
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }
}
