import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;

public class BankingSys{

    private static final Bank bank = new Bank();
    private static final Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Welcome to the Banking System");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> openSavings();
                    case "2" -> openChecking();
                    case "3" -> deposit();
                    case "4" -> withdraw();
                    case "5" -> transfer();
                    case "6" -> statement();
                    case "7" -> interest();
                    case "8" -> listAccounts();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid choice, try again.");
                }
            } catch (InsufficientFundsException e) {
                System.out.println("Transaction declined: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    private static void printMenu() {
        System.out.println("\n1) Open savings account   5) Transfer");
        System.out.println("2) Open checking account  6) Account statement");
        System.out.println("3) Deposit                7) Apply monthly interest");
        System.out.println("4) Withdraw               8) List all accounts");
        System.out.println("0) Exit");
        System.out.print("Choose: ");
    }

    private static void deposit() {
        Account account = bank.getAccount(readInt("Account number: "));
        account.deposit(readAmount("Amount: "));
        System.out.println("New balance: " + account.getBalance());
    }

    private static void openSavings() {
        String ownerName = readText("Owner name: ");
        BigDecimal rate = readAmount("Interest rate (as a percent): ").divide(BigDecimal.valueOf(100));
        SavingsAccount account = bank.openSavings(ownerName, rate);
        System.out.println("New savings account opened: " + account.getAccountNumber());
    }

    private static void openChecking() {
        String ownerName = readText("Owner name: ");
        BigDecimal overdraftLimit = readAmount("Overdraft limit: ");
        CheckingAccount account = bank.openChecking(ownerName, overdraftLimit);
        System.out.println("New checking account opened: " + account.getAccountNumber());
    }

    private static void withdraw() throws InsufficientFundsException {
        Account account = bank.getAccount(readInt("Account number: "));
        account.withdraw(readAmount("Amount: "));
        System.out.println("New balance: " + account.getBalance());
    }

    private static void transfer() throws InsufficientFundsException {
        int from = readInt("From account number: ");
        int to = readInt("To account number: ");
        BigDecimal amount = readAmount("Amount: ");
        bank.transfer(from, to, amount);
        System.out.println("Transfer complete.");
    }

    private static void statement() {
        Account account = bank.getAccount(readInt("Account number: "));
        System.out.println(account.getStatement());
    }

    private static void interest() {
        int count = bank.applyMonthlyInterest();
        System.out.println(count + " accounts received monthly interest.");
    }

    private static void listAccounts() {
        List<Account> accounts = bank.getAllAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
        } else {
            for (Account account : accounts) {
                String type = account instanceof SavingsAccount ? "Savings" : "Checking";
                System.out.printf("Account Number: %d, Owner: %s, Type: %s, Balance: %s%n",
                        account.getAccountNumber(), account.getOwnerName(), type, account.getBalance());
            }
        }
    }


    private static String readText(String prompt) {
        System.out.print(prompt);
        return in.nextLine();
    }

    private static int readInt(String prompt) {
        try {
            return Integer.parseInt(readText(prompt).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a whole number.");
        }
    }

    private static BigDecimal readAmount(String prompt) {
        try {
            return new BigDecimal(readText(prompt).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid number.");
        }
    }
}

abstract class Account{
    private int accnumber;
    private String ownername;
    private BigDecimal balance;
    private List<Transaction> transactions = new ArrayList<>();
    
    public Account(int accnumber, String ownername){
        if(ownername == null || ownername.isBlank()){
            throw new IllegalArgumentException("Owner name cannot be null or empty.");
        }
        this.accnumber = accnumber;
        this.ownername = ownername;
        this.balance = BigDecimal.ZERO;
    
    }
    public int getAccountNumber(){
        return accnumber;
    }

    public String getOwnerName(){
        return ownername;
    }

    public BigDecimal getBalance(){
        return balance;
    }

    public void deposit(BigDecimal amount){
        credit(amount, Transaction.Type.DEPOSIT);
    }

    protected abstract boolean canWithdraw(BigDecimal amount);

    public void withdraw(BigDecimal amount) throws InsufficientFundsException{
        debit(amount, Transaction.Type.WITHDRAWAL);
    }
    public List<Transaction> getTransactions(){
        return Collections.unmodifiableList(transactions);
    }
    public String getStatement(){
        StringBuilder statement = new StringBuilder();
        statement.append("Account Number: ").append(accnumber).append("\n");
        statement.append("Owner Name: ").append(ownername).append("\n");
        statement.append("Current Balance: ").append(balance).append("\n");
        statement.append("Transactions:\n");
        for(Transaction transaction : transactions){
            statement.append(transaction.toString()).append("\n");
        }
        return statement.toString();
    }

    void credit(BigDecimal amount, Transaction.Type type){
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Credit amount must be positive.");
        }
        balance = balance.add(amount);
        transactions.add(new Transaction(type, amount, balance));
    }
    void debit(BigDecimal amount, Transaction.Type type) throws InsufficientFundsException{
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Debit amount must be positive.");
        }
        if(canWithdraw(amount)){
            balance = balance.subtract(amount);
            transactions.add(new Transaction(type, amount, balance));
        } else {
            throw new InsufficientFundsException("Insufficient funds for debit.");
        }
    }
}
class InsufficientFundsException extends Exception{
    public InsufficientFundsException(String message){
        super(message);
    }
}

class SavingsAccount extends Account{
    private final BigDecimal annualInterestRate;

    public SavingsAccount(int accnumber, String ownername, BigDecimal annualInterestRate){
        super(accnumber, ownername);
        if(annualInterestRate == null || annualInterestRate.compareTo(BigDecimal.ZERO) < 0){
            throw new IllegalArgumentException("Annual interest rate must be non-negative.");
        }
        this.annualInterestRate = annualInterestRate;
    }

    @Override
    protected boolean canWithdraw(BigDecimal amount){
        return getBalance().compareTo(amount) >= 0;
    }

    /** Adds one month of interest. Returns true if any interest was paid. */
    public boolean applyMonthlyInterest(){
        BigDecimal interest = getBalance()
                .multiply(annualInterestRate)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        if(interest.signum() > 0){
            credit(interest, Transaction.Type.INTEREST);
            return true;
        }
        return false;
    }
}

class CheckingAccount extends Account{
    private final BigDecimal overdraftLimit;

    public CheckingAccount(int accnumber, String ownername, BigDecimal overdraftLimit){
        super(accnumber, ownername);
        if(overdraftLimit == null || overdraftLimit.compareTo(BigDecimal.ZERO) < 0){
            throw new IllegalArgumentException("Overdraft limit must be non-negative.");
        }
        this.overdraftLimit = overdraftLimit;
    }

    @Override
    protected boolean canWithdraw(BigDecimal amount){
        return getBalance().add(overdraftLimit).compareTo(amount) >= 0;
    }
}
class Transaction{
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    enum Type { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, INTEREST }
    private final Type type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfterTransaction;
    private final LocalDateTime timestamp;
    

    public Transaction(Type type, BigDecimal amount, BigDecimal balanceAfterTransaction){
        this.type = type;
        this.amount = amount;
        this.balanceAfterTransaction = balanceAfterTransaction;
        this.timestamp = LocalDateTime.now();
    }  
    public BigDecimal getAmount(){
        return amount;
    }
    public BigDecimal getBalanceAfterTransaction(){
        return balanceAfterTransaction;
    }
    public LocalDateTime getTimestamp(){
        return timestamp;
    }
    public Type getType(){
        return type;
    }

    @Override 
    public String toString(){
        return String.format("%s of %s on %s. Balance after transaction: %s", 
            type, amount, timestamp.format(FORMAT), balanceAfterTransaction);
    }
}
class Bank {
    private final Map<Integer, Account> accounts = new LinkedHashMap<>();
    private int nextNumber = 1001;

    public SavingsAccount openSavings(String owner, BigDecimal annualRate) {
        SavingsAccount account = new SavingsAccount(nextNumber, owner, annualRate);
        nextNumber++;
        accounts.put(account.getAccountNumber(), account);
        return account;
    }

    public CheckingAccount openChecking(String owner, BigDecimal overdraftLimit) {
        CheckingAccount account = new CheckingAccount(nextNumber, owner, overdraftLimit);
        nextNumber++;
        accounts.put(account.getAccountNumber(), account);
        return account;
    }

    public Account getAccount(int accountNumber) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        return account;
    }

    public void transfer(int fromNumber, int toNumber, BigDecimal amount)
            throws InsufficientFundsException {
        Account fromAccount = getAccount(fromNumber);
        Account toAccount = getAccount(toNumber);

        if (fromAccount == toAccount) {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }

        fromAccount.debit(amount, Transaction.Type.TRANSFER_OUT);
        toAccount.credit(amount, Transaction.Type.TRANSFER_IN);
    }

    public int applyMonthlyInterest() {
        int count = 0;
        for (Account account : accounts.values()) {
            if (account instanceof SavingsAccount savings) {
                if (savings.applyMonthlyInterest()) {
                    count++;
                }
            }
        }
        return count;
    }

    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }
}