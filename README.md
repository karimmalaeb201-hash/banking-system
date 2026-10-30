# Banking System (Java)

A console-based banking application built to practice object-oriented design.

## Features
- Savings accounts (earn monthly interest, cannot go below zero)
- Checking accounts (overdraft up to an agreed limit)
- Deposit, withdraw, and transfer between accounts
- Full transaction history and account statements
- Input validation and a custom `InsufficientFundsException`
- Exact money arithmetic with `BigDecimal`
- Automated tests covering the core rules

## OOP concepts demonstrated
- **Abstraction:** `Account` is an abstract class
- **Inheritance:** `SavingsAccount` and `CheckingAccount` extend `Account`
- **Polymorphism:** each account type defines its own `canWithdraw` rule
- **Encapsulation:** private fields, validated changes, read-only transaction history

## How to run
```
javac *.java
java BankTest      # run the tests
java BankingSys    # start the app
```

## Project structure
| File | Role |
|---|---|
| `BankingSys.java` | Console menu |
| `Bank.java` | Manages accounts, transfers, and interest |
| `Account.java` | Abstract base class |
| `SavingsAccount.java` / `CheckingAccount.java` | Account types |
| `Transaction.java` | Immutable record of one money movement |
| `InsufficientFundsException.java` | Custom checked exception |
| `BankTest.java` | Automated tests |
