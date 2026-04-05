# 🏧 ATM System — Low Level Design

## 📋 Quick Revision (5-Minute Rule)

> **When revising, just read THIS section + glance at the UML diagram below. Only open `.java` files if you forget a specific implementation detail.**

### Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **State Pattern** | `ATMState` → `IdleState`, `InsertedCardState`, `AuthenticatedState`, `CompletedState` | ATM behavior changes based on its current state (idle → card inserted → authenticated → transaction done). Each state only allows valid operations. |
| **Strategy Pattern** | `TransactionStrategy` → `WithDrawStrategy`, `DepositStrategy`, `ViewBalanceStrategy` | Different transaction types share the same `execute(atm, amount)` interface. New transaction types can be added without modifying existing code (**Open-Closed Principle**). |
| **Template Method** (partial) | `BaseState` abstract class | Provides default "invalid operation" implementations so concrete states only override what they support. Reduces code duplication. |

### SOLID Principles Applied

- **S** — Single Responsibility: Each class has one job (`Account` manages balance, `ATMInventory` manages cash, `Card` handles PIN validation)
- **O** — Open/Closed: New transaction types (e.g., `TransferStrategy`) can be added without touching `AuthenticatedState`
- **L** — Liskov Substitution: All `ATMState` implementations are interchangeable via the interface
- **I** — Interface Segregation: `TransactionStrategy` has a single focused method
- **D** — Dependency Inversion: `ATM` depends on `ATMState` interface, not concrete states

### State Flow

```
Idle → [insertCard] → InsertedCard → [authenticatePin] → Authenticated → [executeTransaction] → Completed → [ejectCard] → Idle
                                        ↓ (wrong PIN)
                                      Idle (card ejected)
```

---

## 🏗️ UML Class Diagram

> The Excalidraw source file (`ATM.excalidraw`) is included in this repo. You can open it at [excalidraw.com](https://excalidraw.com) for editing.
> 
> *(Note: Export your diagram from Excalidraw as `ATM.png` or `ATM.svg` into this folder, and it will appear below)*

![ATM UML Class Diagram](./ATM.png)
<!-- If you exported as SVG, change the above line to: ![ATM UML Class Diagram](./ATM.svg) -->

---

## 🧱 Class Breakdown

### Core Classes

| Class | Responsibility |
|-------|---------------|
| `ATM` | Central orchestrator — holds current state, inventory, card, account |
| `ATMInventory` | Manages cash denominations (`HashMap<CashType, Integer>`), dispenses cash (thread-safe) |
| `Account` | Holds account number + balance, supports `withdraw()` and `deposit()` (thread-safe) |
| `Card` | Stores card number + PIN, validates PIN |
| `CashType` | Enum: `TEN(10)`, `FIFTY(50)`, `HUNDRED(100)`, `FIVE_HUNDRED(500)`, `THOUSAND(1000)` |

### State Pattern Classes

| Class | Extends | Allows |
|-------|---------|--------|
| `ATMState` (interface) | — | Defines: `insertCard`, `authenticatePin`, `selectTransaction`, `executeTransaction`, `ejectCard` |
| `BaseState` (abstract) | implements `ATMState` | Default "invalid" messages for all operations + default `ejectCard` |
| `IdleState` | `BaseState` | `insertCard()` → transitions to `InsertedCardState` |
| `InsertedCardState` | `BaseState` | `authenticatePin()` → success: `AuthenticatedState` / fail: ejects card → `IdleState` |
| `AuthenticatedState` | `BaseState` | `executeTransaction()` → runs strategy, transitions to `CompletedState` |
| `CompletedState` | `BaseState` | `ejectCard()` → back to `IdleState` |

### Strategy Pattern Classes

| Class | Interface | Behavior |
|-------|-----------|----------|
| `TransactionStrategy` | — | Interface with `execute(ATM atm, int amount)` |
| `WithDrawStrategy` | `TransactionStrategy` | Checks ATM cash + account balance → dispenses cash |
| `DepositStrategy` | `TransactionStrategy` | Adds to account balance + ATM inventory |
| `ViewBalanceStrategy` | `TransactionStrategy` | Prints current account balance |

---

## 🔑 Key Design Decisions

1. **Thread Safety**: `Account.withdraw()`, `Account.deposit()`, `ATMInventory.dispenseCash()`, `ATMInventory.getTotalCash()` are all `synchronized` — ready for concurrent access scenarios.
2. **State Validation via BaseState**: Instead of if-else chains in a single class, invalid operations throw descriptive messages from `BaseState`. Each concrete state only overrides valid operations.
3. **Strategy over Switch-Case**: Transaction types are pluggable. To add a new transaction (e.g., mini-statement, fund transfer), just create a new `TransactionStrategy` implementation — zero changes to existing code.

---

## 🚀 How to Run

```bash
cd ATM
javac ATM/*.java
java ATM.Main
```

**Expected Output:**
```
Card inserted. Please authenticate.
Auth Failed
Card ejected.
Invalid state.
```
> Note: The `Main.java` demo uses PIN `12345` while the card has PIN `1234`, so authentication intentionally fails to demonstrate the state flow.

---

## 📂 File Structure

```
ATM/
├── ATM.java                  # Central ATM class (orchestrator)
├── ATMState.java             # State interface
├── BaseState.java            # Abstract base with default invalid messages
├── IdleState.java            # Waiting for card
├── InsertedCardState.java    # Card inserted, awaiting PIN
├── AuthenticatedState.java   # PIN verified, ready for transactions
├── CompletedState.java       # Transaction done, eject card
├── TransactionStrategy.java  # Strategy interface
├── WithDrawStrategy.java     # Withdrawal logic
├── DepositStrategy.java      # Deposit logic
├── ViewBalanceStrategy.java  # Balance inquiry
├── ATMInventory.java         # Cash management
├── Account.java              # User account & balance
├── Card.java                 # Card & PIN validation
├── CashType.java             # Cash denomination enum
├── Main.java                 # Demo / Driver
├── ATM.excalidraw            # UML diagram source (open in excalidraw.com)
└── README.md                 # This file
```

---

## 🔄 Revision Checklist

Use this when revising. If you can answer these from memory, you're good:

- [ ] What are the 4 states in the ATM? What transitions between them?
- [ ] Why use State Pattern instead of a giant switch-case?
- [ ] How do you add a new transaction type without modifying existing code?
- [ ] Which methods are `synchronized` and why?
- [ ] What does `BaseState` do and why is it useful?
