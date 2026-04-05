# 04 — API Contracts and Exception Handling

> **Purpose of this file:** Understand how to design clean, honest, and resilient method/class interfaces in LLD — and how to handle things going wrong without leaking internals or crashing the system. Every concept includes a bad example, a good example, and a signal for when you're violating it. This is critical for SDE2 interviews because interviewers specifically probe: *"What happens when this fails?"*

---

## Table of Contents

1. [Why API Contracts and Exception Handling Matter in LLD](#1-why-api-contracts-and-exception-handling-matter-in-lld)
2. [What is an API Contract?](#2-what-is-an-api-contract)
   - [The Three Parts of a Contract](#21-the-three-parts-of-a-contract)
   - [Contracts at the Class Level vs Method Level](#22-contracts-at-the-class-level-vs-method-level)
3. [Designing API Contracts](#3-designing-api-contracts)
   - [Interface-First Design](#31-interface-first-design)
   - [Command-Query Separation (CQS)](#32-command-query-separation-cqs)
   - [Validate at Boundaries, Trust Inside](#33-validate-at-boundaries-trust-inside)
4. [Exception Handling — The Theory](#4-exception-handling--the-theory)
   - [Checked vs Unchecked Exceptions](#41-checked-vs-unchecked-exceptions)
   - [When to Use Which](#42-when-to-use-which)
5. [Custom Exception Hierarchy](#5-custom-exception-hierarchy)
   - [Why You Need Custom Exceptions](#51-why-you-need-custom-exceptions)
   - [Designing a Domain Exception Hierarchy](#52-designing-a-domain-exception-hierarchy)
6. [Exception Handling Patterns](#6-exception-handling-patterns)
   - [Fail Fast](#61-fail-fast)
   - [Guard Clauses](#62-guard-clauses)
   - [Catch at the Right Layer](#63-catch-at-the-right-layer)
   - [Wrap and Rethrow](#64-wrap-and-rethrow)
   - [The Result / Either Pattern](#65-the-result--either-pattern)
7. [Contracts in LLD Problems](#7-contracts-in-lld-problems)
8. [Exception Anti-Patterns (What NOT to Do)](#8-exception-anti-patterns-what-not-to-do)
9. [How This Shows Up in SDE2 Interviews](#9-how-this-shows-up-in-sde2-interviews)
10. [Quick Reference Card](#10-quick-reference-card)
11. [Revision Checklist](#11-revision-checklist)

---

## 1. Why API Contracts and Exception Handling Matter in LLD

In an LLD interview, once you've drawn your classes and interfaces, the interviewer will inevitably ask: *"What happens if the parking lot is full? What if a payment fails? What if the user enters an invalid seat?"*

This is where most candidates fail. They designed the happy path well, but the moment something goes wrong, their system:
- Returns `null` silently
- Throws a raw `RuntimeException` with no message
- Lets every class handle its own errors differently
- Leaks internal details through exception messages

**An API Contract is a promise.** Your method promises: *"Give me valid input, and I will produce this output or this side effect. If I can't, here's exactly how I'll tell you."*

**Why this matters for SDE2:**
- SDE2 is expected to design systems that are **production-ready**, not just correct in the happy path
- Interviewers at SDE2 level specifically ask: *"How do you handle partial failures? How does the caller know what went wrong?"*
- Clean exception design is the difference between a brittle prototype and a maintainable service

**What bad exception handling looks like in practice:**
- A `NullPointerException` appears in prod logs with no context — nobody knows which object was null
- A method catches an exception and silently swallows it — the caller thinks it succeeded
- A UI layer catches a `SQLException` — the UI now knows about your database layer (layer leak)
- Every method has a `try-catch (Exception e) { e.printStackTrace(); }` — errors are logged and ignored

---

## 2. What is an API Contract?

An API Contract is the **formal agreement** between a method/class and its callers. It answers three questions:
1. What must the caller provide? (**preconditions**)
2. What does the method promise to deliver? (**postconditions**)
3. What must always be true about the object's state? (**invariants**)

This concept comes from **Design by Contract (DbC)**, coined by Bertrand Meyer. You don't need to know the academic term — you need to internalize the three parts.

### 2.1 The Three Parts of a Contract

#### Preconditions
What the caller must guarantee before calling the method. If the caller violates a precondition, it's the **caller's bug**.

```java
// The contract says: amount must be positive, account must exist
public void withdraw(String accountId, double amount) {
    // Preconditions — caller is responsible for these
    if (accountId == null || accountId.isBlank())
        throw new IllegalArgumentException("accountId cannot be null or blank");
    if (amount <= 0)
        throw new IllegalArgumentException("amount must be positive, got: " + amount);
}
```

#### Postconditions
What the method guarantees when it returns. If a postcondition is violated, it's the **method's bug**.

```java
public Ticket issueTicket(String vehicleNumber) {
    // ... logic ...
    Ticket ticket = new Ticket(vehicleNumber, slotId, entryTime);
    // Postcondition: the returned ticket is always valid and non-null
    // If we reach this return, the caller can trust the ticket is usable
    return ticket;
}
```

#### Invariants
What must always be true about an object's state — before and after any method call.

```java
class BankAccount {
    private double balance;

    // INVARIANT: balance must never be negative
    // Every public method must maintain this invariant

    public void withdraw(double amount) {
        if (amount > balance)
            throw new InsufficientFundsException(balance, amount);
        this.balance -= amount;
        // Invariant still holds: balance >= 0
    }
}
```

---

### 2.2 Contracts at the Class Level vs Method Level

| Level | What it covers | Example |
|---|---|---|
| **Class invariant** | Rules the object must always satisfy | `balance >= 0` in BankAccount |
| **Method precondition** | What the caller must ensure before calling | `amount > 0` before `withdraw()` |
| **Method postcondition** | What the method guarantees on return | Returned `Ticket` is always non-null |
| **Interface contract** | What all implementors must honour | `PaymentGateway.charge()` always returns a `PaymentResult`, never null |

The key insight: **contracts define what the caller can rely on**. When you change the implementation, the contract must not change. This is exactly Liskov Substitution Principle (LSP) — subclasses cannot weaken preconditions or strengthen postconditions.

---

## 3. Designing API Contracts

### 3.1 Interface-First Design

Before writing any implementation, write the interface. The interface *is* your contract.

**Bad — implementation first, interface as an afterthought:**
```java
class PaymentService {
    public boolean processPayment(String orderId, double amount, String method,
                                   String cardNumber, String cvv, String expiry) {
        // 200 lines of mixed concerns
    }
}
```
Problems: The caller must pass credit card details even for UPI payments. The boolean return tells nothing. Adding a new payment method means editing this class.

**Good — define the contract first:**
```java
// The contract: any payment gateway must implement this
interface PaymentGateway {
    PaymentResult charge(PaymentRequest request);
}

// The request object captures all valid input — null checks happen inside
class PaymentRequest {
    private final String orderId;
    private final Money amount;
    private final PaymentMethod method;

    public PaymentRequest(String orderId, Money amount, PaymentMethod method) {
        Objects.requireNonNull(orderId, "orderId required");
        Objects.requireNonNull(amount, "amount required");
        Objects.requireNonNull(method, "method required");
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
    }
    // getters only — immutable
}

// The result tells the caller exactly what happened
class PaymentResult {
    private final boolean success;
    private final String transactionId;    // present only on success
    private final String failureReason;    // present only on failure
    private final PaymentStatus status;    // COMPLETED, FAILED, PENDING

    // factory methods make intent clear
    public static PaymentResult success(String txnId) { ... }
    public static PaymentResult failure(String reason) { ... }
}
```

Now: adding UPI, crypto, or BNPL just means a new implementation of `PaymentGateway`. The contract never changes.

---

### 3.2 Command-Query Separation (CQS)

**The rule:** A method should either *do something* (command) or *answer something* (query) — never both.

**Why it matters:** When a method changes state AND returns a value, the caller can't call it to just check something without causing a side effect. It also makes the contract harder to reason about.

**Bad — mixes command and query:**
```java
// Does this park the car or just check availability? Both? Is the slot returned after parking?
public ParkingSlot parkVehicle(Vehicle vehicle) {
    ParkingSlot slot = findAvailableSlot(vehicle);
    slot.park(vehicle);         // side effect
    return slot;                // also returns a value
}
```

**Good — separate the command from the query:**
```java
// Query — no side effect, just tells you if parking is possible
public boolean isAvailable(VehicleType type) {
    return findAvailableSlot(type).isPresent();
}

// Query — finds a slot, no side effect
public Optional<ParkingSlot> findAvailableSlot(VehicleType type) {
    return slots.stream()
        .filter(s -> s.isAvailable() && s.supports(type))
        .findFirst();
}

// Command — parks the vehicle, no meaningful return value needed
public Ticket park(Vehicle vehicle) {
    ParkingSlot slot = findAvailableSlot(vehicle.getType())
        .orElseThrow(() -> new ParkingLotFullException(vehicle.getType()));
    slot.park(vehicle);
    return ticketService.issueTicket(vehicle, slot);
}
```

**Interview signal:** When an interviewer asks *"how does the caller know what slot the car is parked in?"* — the answer is through the `Ticket`, not through the method that parks it.

---

### 3.3 Validate at Boundaries, Trust Inside

**The rule:** Validate external input as it enters your system. Once validated, trust it inside. Don't re-validate at every internal method call.

**What is a boundary?**
- A public API method called by a controller or external client
- A service method called by another service (inter-layer boundary)
- A constructor of an entity (the point where the object comes into existence)

**Bad — validation scattered everywhere:**
```java
class ParkingService {
    public void park(String vehicleNumber, String type) {
        if (vehicleNumber == null) throw ... // validation in service
    }
}

class ParkingSlot {
    public void assignVehicle(String vehicleNumber, String type) {
        if (vehicleNumber == null) throw ... // repeated validation in entity
    }
}
```

**Good — validate once, at the boundary:**
```java
// Validate and create a valid Vehicle object at the boundary
class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    public Vehicle(String licensePlate, VehicleType type) {
        // Validation happens ONCE — when the object is constructed
        if (licensePlate == null || !licensePlate.matches("[A-Z]{2}\\d{2}[A-Z]{2}\\d{4}"))
            throw new InvalidLicensePlateException(licensePlate);
        Objects.requireNonNull(type, "VehicleType required");
        this.licensePlate = licensePlate;
        this.type = type;
    }
}

// Now internally, everyone trusts that a Vehicle object is always valid
class ParkingSlot {
    public void assignVehicle(Vehicle vehicle) {
        // No null check needed — Vehicle constructor guarantees validity
        this.parkedVehicle = vehicle;
        this.status = SlotStatus.OCCUPIED;
    }
}
```

**The pattern name:** This is called a **Value Object** — an object that validates itself on construction and is then always in a valid state.

---

## 4. Exception Handling — The Theory

### 4.1 Checked vs Unchecked Exceptions

Java has two flavours of exceptions. Understanding this is essential for SDE2.

| | Checked Exception | Unchecked Exception |
|---|---|---|
| **Extends** | `Exception` | `RuntimeException` |
| **Compiler enforces?** | Yes — must catch or declare `throws` | No |
| **Represents** | Conditions the caller *can reasonably recover from* | Programming errors / state violations |
| **Examples** | `IOException`, `SQLException` | `IllegalArgumentException`, `NullPointerException`, `IllegalStateException` |
| **Use for** | File not found, network timeout, DB unavailable | Null input, invalid argument, violated invariant |

---

### 4.2 When to Use Which

**Use Checked Exceptions when:**
- The failure is *external* — file system, network, third-party service
- The caller *can meaningfully recover* — retry the network call, show a fallback file
- You want the compiler to force callers to think about the failure

```java
// Caller MUST handle this — the file might genuinely not exist
public Report loadReport(String filename) throws ReportNotFoundException {
    if (!fileSystem.exists(filename))
        throw new ReportNotFoundException(filename);
    return parser.parse(fileSystem.read(filename));
}
```

**Use Unchecked Exceptions when:**
- The failure is a *programming error* — null passed where not allowed, invalid enum, corrupted state
- The caller cannot meaningfully recover — it's a bug, not a business condition
- You don't want every caller cluttered with `try-catch` for things that should never happen

```java
// This is always a programming error — throw unchecked
public void setCapacity(int capacity) {
    if (capacity <= 0)
        throw new IllegalArgumentException("Capacity must be positive, got: " + capacity);
    this.capacity = capacity;
}
```

**The modern Java / industry trend:** Most frameworks (Spring, etc.) heavily favour unchecked exceptions. The reasoning: checked exceptions pollute every method signature, and callers often just wrap and rethrow anyway. In LLD interviews, use unchecked for domain violations and checked only for genuine I/O / external calls.

---

## 5. Custom Exception Hierarchy

### 5.1 Why You Need Custom Exceptions

Raw exceptions like `RuntimeException("slot not found")` are:
- Hard to catch selectively (how do you catch just "slot not found" without catching everything?)
- Meaningless in logs — no domain context
- Impossible to carry structured data (slot ID, vehicle type, etc.)

Custom exceptions give you:
- **Selective catching** by type
- **Domain context** — an exception named `ParkingSlotNotFoundException` tells the story
- **Structured data** — carry the exact slot ID that wasn't found
- **Layer isolation** — upper layers catch domain exceptions, not raw `SQLException`

---

### 5.2 Designing a Domain Exception Hierarchy

Design your exceptions in a hierarchy that mirrors your domain.

```java
// Base exception for your entire application domain
public abstract class AppException extends RuntimeException {
    private final String errorCode;

    protected AppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected AppException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}

// Domain-specific base exceptions — one per bounded context
public class ParkingException extends AppException {
    protected ParkingException(String errorCode, String message) {
        super(errorCode, message);
    }
}

public class PaymentException extends AppException {
    protected PaymentException(String errorCode, String message) {
        super(errorCode, message);
    }
}

// Specific exceptions — carry domain-relevant data
public class ParkingLotFullException extends ParkingException {
    private final VehicleType vehicleType;

    public ParkingLotFullException(VehicleType vehicleType) {
        super("PARKING_LOT_FULL",
              "No available slots for vehicle type: " + vehicleType);
        this.vehicleType = vehicleType;
    }

    public VehicleType getVehicleType() { return vehicleType; }
}

public class VehicleAlreadyParkedException extends ParkingException {
    private final String licensePlate;

    public VehicleAlreadyParkedException(String licensePlate) {
        super("VEHICLE_ALREADY_PARKED",
              "Vehicle already parked: " + licensePlate);
        this.licensePlate = licensePlate;
    }
}

public class TicketNotFoundException extends ParkingException {
    private final String ticketId;

    public TicketNotFoundException(String ticketId) {
        super("TICKET_NOT_FOUND", "No active ticket found for ID: " + ticketId);
        this.ticketId = ticketId;
    }
}

public class PaymentFailedException extends PaymentException {
    private final String transactionId;
    private final String gatewayReason;

    public PaymentFailedException(String transactionId, String gatewayReason) {
        super("PAYMENT_FAILED",
              "Payment failed for transaction " + transactionId + ": " + gatewayReason);
        this.transactionId = transactionId;
        this.gatewayReason = gatewayReason;
    }
}
```

**How this hierarchy is used:**
```java
// Catch specifically what you can handle
try {
    parkingService.park(vehicle);
} catch (ParkingLotFullException e) {
    // Specific — tell the user no slots for their vehicle type
    displayMessage("Sorry, no slots for " + e.getVehicleType());
} catch (VehicleAlreadyParkedException e) {
    // Specific — different message
    displayMessage("This vehicle is already parked");
} catch (ParkingException e) {
    // Catch-all for any parking error we didn't anticipate
    log.error("Unexpected parking error", e);
    displayMessage("Something went wrong. Please try again.");
}
```

**The hierarchy rule:** Go as specific as you can when catching. Catch the most specific exception first, the broadest last.

---

## 6. Exception Handling Patterns

### 6.1 Fail Fast

**The principle:** Detect problems as early as possible and throw immediately. Don't accumulate errors and fail later with a confusing message.

**Bad — fails late, confusing error:**
```java
public Ticket processEntry(String vehicleNumber, String type) {
    Vehicle vehicle = new Vehicle();  // default constructor — invalid state!
    vehicle.setLicensePlate(vehicleNumber);  // what if vehicleNumber is null?
    vehicle.setType(type);  // what if type is invalid?
    // ... 50 lines later ...
    return ticketRepository.save(ticket);  // NullPointerException here — but WHY?
}
```

**Good — fails fast, clear error:**
```java
public Ticket processEntry(String vehicleNumber, String vehicleTypeName) {
    // Fail immediately with a clear message
    VehicleType type = VehicleType.fromString(vehicleTypeName)  // throws if invalid
        .orElseThrow(() -> new InvalidVehicleTypeException(vehicleTypeName));

    Vehicle vehicle = new Vehicle(vehicleNumber, type);  // constructor validates

    ParkingSlot slot = findSlot(type)
        .orElseThrow(() -> new ParkingLotFullException(type));

    // Only reach here if everything is valid
    return issueTicket(vehicle, slot);
}
```

---

### 6.2 Guard Clauses

**The pattern:** Put all your precondition checks at the top of a method, returning or throwing early. Avoid deep nesting.

**Bad — arrow code, hard to read:**
```java
public PaymentResult processPayment(PaymentRequest request) {
    if (request != null) {
        if (request.getAmount() != null) {
            if (request.getAmount().getValue() > 0) {
                if (paymentGateway.isAvailable()) {
                    // actual logic 5 levels deep
                    return paymentGateway.charge(request);
                } else {
                    return PaymentResult.failure("Gateway unavailable");
                }
            } else {
                throw new IllegalArgumentException("Amount must be positive");
            }
        } else {
            throw new IllegalArgumentException("Amount required");
        }
    } else {
        throw new IllegalArgumentException("Request required");
    }
}
```

**Good — guard clauses, flat code:**
```java
public PaymentResult processPayment(PaymentRequest request) {
    // Guard clauses — fail fast, flat structure
    Objects.requireNonNull(request, "PaymentRequest required");
    Objects.requireNonNull(request.getAmount(), "Amount required");
    if (request.getAmount().getValue() <= 0)
        throw new IllegalArgumentException("Amount must be positive");
    if (!paymentGateway.isAvailable())
        return PaymentResult.failure("Gateway unavailable");

    // Happy path — only reached if all guards passed
    return paymentGateway.charge(request);
}
```

---

### 6.3 Catch at the Right Layer

**The rule:** Exceptions should be caught at the layer that can meaningfully do something about them. Don't catch prematurely.

**The layers in a typical LLD design:**

```
┌─────────────────────────────────────┐
│         Entry Point / Controller    │  ← Catch here: translate to user message
├─────────────────────────────────────┤
│           Service / Use Case        │  ← Catch here: retry, fallback, wrap
├─────────────────────────────────────┤
│           Repository / Gateway      │  ← Let exceptions propagate (or wrap)
├─────────────────────────────────────┤
│           Domain / Entities         │  ← Throw domain exceptions
└─────────────────────────────────────┘
```

**Bad — catching too low:**
```java
class ParkingRepository {
    public ParkingSlot findSlot(int slotId) {
        try {
            return database.query("SELECT * FROM slots WHERE id = ?", slotId);
        } catch (SlotNotFoundException e) {
            return null;  // swallowed the exception, returned null — now the caller gets an NPE later
        }
    }
}
```

**Good — let it propagate to the right layer:**
```java
// Repository: wrap technical exception into domain exception
class ParkingRepository {
    public ParkingSlot findSlotById(int slotId) {
        try {
            return database.query("SELECT * FROM slots WHERE id = ?", slotId);
        } catch (DatabaseException e) {
            throw new SlotNotFoundException(slotId, e);  // wrap and rethrow as domain exception
        }
    }
}

// Service: business logic can catch and decide
class ParkingService {
    public Ticket park(Vehicle vehicle) {
        ParkingSlot slot = slotRepository.findAvailableSlot(vehicle.getType())
            .orElseThrow(() -> new ParkingLotFullException(vehicle.getType()));
        // SlotNotFoundException, if it happens, propagates up — service can't handle it
        slot.assign(vehicle);
        return ticketRepository.create(vehicle, slot);
    }
}

// Entry point: translate to user-facing response
class ParkingController {
    public Response handleEntry(EntryRequest request) {
        try {
            Ticket ticket = parkingService.park(request.toVehicle());
            return Response.success(ticket);
        } catch (ParkingLotFullException e) {
            return Response.error(e.getErrorCode(), "Parking lot is full for " + e.getVehicleType());
        } catch (InvalidLicensePlateException e) {
            return Response.error("INVALID_INPUT", "Invalid license plate: " + e.getLicensePlate());
        } catch (ParkingException e) {
            log.error("Unhandled parking exception", e);
            return Response.error("INTERNAL_ERROR", "Something went wrong");
        }
    }
}
```

---

### 6.4 Wrap and Rethrow

**When to use:** When a lower layer throws a technical exception (SQL, IO, HTTP), and the upper layer should only know about domain exceptions.

**The pattern:**
```java
public class UserRepository {
    public User findByEmail(String email) {
        try {
            return jdbcTemplate.queryForObject(SQL, userMapper, email);
        } catch (EmptyResultDataAccessException e) {
            // Translate technical exception to domain exception
            // Preserve the original cause for debugging
            throw new UserNotFoundException(email, e);
        } catch (DataAccessException e) {
            // Wrap infrastructure failure
            throw new RepositoryException("Failed to query users", e);
        }
    }
}
```

**Why preserve the cause?** `throw new UserNotFoundException(email, e)` — the second argument `e` is the original exception. Without it, you lose the stack trace of the original failure. Always pass the cause when wrapping.

---

### 6.5 The Result / Either Pattern

**When to use:** For expected failures that are part of the domain flow — not exceptional, just one of several outcomes. Avoids using exceptions for control flow.

**The idea:** Instead of throwing, return a `Result` object that can be either success or failure. The caller must explicitly check which it is.

```java
// A generic Result type — can be used anywhere
public class Result<T> {
    private final T value;
    private final String errorCode;
    private final String errorMessage;
    private final boolean success;

    private Result(T value, String errorCode, String errorMessage, boolean success) {
        this.value = value;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.success = success;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null, null, true);
    }

    public static <T> Result<T> failure(String errorCode, String message) {
        return new Result<>(null, errorCode, message, false);
    }

    public boolean isSuccess() { return success; }
    public T getValue() {
        if (!success) throw new IllegalStateException("Cannot get value of a failed result");
        return value;
    }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
}

// Usage in payment — payment failure is not "exceptional", it's a normal domain outcome
public class PaymentService {
    public Result<PaymentReceipt> processPayment(PaymentRequest request) {
        if (!request.getAmount().isPositive())
            return Result.failure("INVALID_AMOUNT", "Amount must be positive");

        PaymentGatewayResponse response = gateway.charge(request);

        if (response.isDeclined())
            return Result.failure("PAYMENT_DECLINED", response.getDeclineReason());

        return Result.success(new PaymentReceipt(response.getTransactionId(), request.getAmount()));
    }
}

// Caller is forced to handle both cases
Result<PaymentReceipt> result = paymentService.processPayment(request);
if (result.isSuccess()) {
    sendConfirmationEmail(result.getValue());
} else {
    displayError(result.getErrorMessage());
}
```

**When Result vs Exception:**
| Use Exceptions | Use Result Pattern |
|---|---|
| Truly unexpected failure (DB down, NPE) | Expected alternative outcomes (payment declined) |
| Programming errors | Business flow alternatives |
| Failures the caller cannot predict | Failures the caller needs to make a decision about |
| Cross-cutting concerns (logging, transactions) | When you want the caller to be forced to handle both paths |

---

## 7. Contracts in LLD Problems

### Parking Lot — Contracts in Action

```java
interface ParkingLot {
    // CONTRACT:
    // Precondition: vehicle is not null, vehicle is not already parked
    // Postcondition: returns a non-null Ticket with a valid slot assigned
    // Throws: ParkingLotFullException if no slot available
    //         VehicleAlreadyParkedException if vehicle is already inside
    Ticket parkVehicle(Vehicle vehicle);

    // CONTRACT:
    // Precondition: ticketId is not null, ticket must be in ACTIVE state
    // Postcondition: slot is freed, ticket is marked COMPLETED
    // Throws: TicketNotFoundException if ticket doesn't exist
    //         PaymentRequiredException if parking fee not yet paid
    void exitVehicle(String ticketId);

    // Query — no side effects, no exceptions
    boolean hasAvailableSlot(VehicleType type);
}
```

### ATM — Contracts in Action

```java
interface ATM {
    // Precondition: card is valid, PIN is correct (authenticated state)
    // Postcondition: balance reduced, cash dispensed
    // Throws: InsufficientFundsException if balance < amount
    //         ATMCashUnavailableException if ATM doesn't have enough cash
    //         DailyLimitExceededException if exceeds withdrawal limit
    void withdraw(double amount);

    // Query — safe to call any time
    double checkBalance();

    // Command — no return value needed
    void deposit(double amount);
}
```

### Tic Tac Toe — Contracts in Action

```java
interface Game {
    // Precondition: position (row, col) is within bounds
    // Postcondition: board updated, turn switched
    // Throws: InvalidPositionException if out of bounds
    //         PositionAlreadyOccupiedException if cell is taken
    //         GameAlreadyOverException if game is in terminal state
    void makeMove(Player player, int row, int col);

    // Query — always safe
    GameStatus getStatus();   // ONGOING, X_WINS, O_WINS, DRAW

    // Query — always safe
    Player getCurrentPlayer();
}
```

### How Exception Design Reflects Your Architecture

| What you throw | What it reveals about your design |
|---|---|
| `NullPointerException` | You didn't validate at boundaries |
| `RuntimeException("slot not found")` | You don't have a domain exception hierarchy |
| `SQLException` leaking from service layer | You violated layer isolation |
| `return null` on failure | You're using null as a signal — caller doesn't know why it failed |
| `ParkingLotFullException(vehicleType)` | Clean — domain exception with context, catchable selectively |

---

## 8. Exception Anti-Patterns (What NOT to Do)

### Anti-Pattern 1: Swallowing Exceptions

```java
// BAD — exception is swallowed, caller thinks it succeeded
try {
    paymentGateway.charge(request);
} catch (Exception e) {
    e.printStackTrace();  // logged and ignored — catastrophic in prod
}
```

```java
// GOOD — at minimum, log and rethrow or return a failure result
try {
    return paymentGateway.charge(request);
} catch (GatewayTimeoutException e) {
    log.error("Payment gateway timeout for order: {}", request.getOrderId(), e);
    throw new PaymentFailedException(request.getOrderId(), "Gateway timeout");
}
```

---

### Anti-Pattern 2: Returning Null on Failure

```java
// BAD — null return is an invisible failure
public ParkingSlot findSlot(VehicleType type) {
    // returns null if not found — caller must remember to check
    return slots.stream().filter(...).findFirst().orElse(null);
}
```

```java
// GOOD — use Optional to make absence explicit
public Optional<ParkingSlot> findAvailableSlot(VehicleType type) {
    return slots.stream()
        .filter(s -> s.isAvailable() && s.supports(type))
        .findFirst();
}

// Or throw if absence is truly exceptional
public ParkingSlot getSlotById(int slotId) {
    return slotMap.computeIfAbsent(slotId, id -> {
        throw new SlotNotFoundException(id);
    });
}
```

---

### Anti-Pattern 3: Using Exceptions for Control Flow

```java
// BAD — using exception as an if-else (expensive, wrong semantics)
public boolean isUserRegistered(String email) {
    try {
        userRepository.findByEmail(email);
        return true;
    } catch (UserNotFoundException e) {
        return false;  // exceptions are not meant for this
    }
}
```

```java
// GOOD — use a query method
public boolean isUserRegistered(String email) {
    return userRepository.existsByEmail(email);
}
```

---

### Anti-Pattern 4: Catching `Exception` Everywhere

```java
// BAD — too broad, catches things you didn't intend
catch (Exception e) {
    log.error("Error", e);
    return defaultValue;
}
```

This catches `OutOfMemoryError`, `StackOverflowError`, `NullPointerException` — things that signal catastrophic bugs, not recoverable conditions. Catch the most specific type you can handle.

---

### Anti-Pattern 5: Exception Message with No Context

```java
// BAD — useless message
throw new RuntimeException("Not found");

// BAD — missing the key data
throw new ParkingLotFullException("Parking lot is full");
```

```java
// GOOD — message + data in exception
throw new ParkingLotFullException(vehicleType);  // exception stores vehicleType
// Message: "No available slots for vehicle type: CAR"
```

---

### Anti-Pattern 6: Losing the Original Cause

```java
// BAD — original stack trace is lost
try {
    return db.query(sql, params);
} catch (DatabaseException e) {
    throw new UserNotFoundException(email);  // cause not passed — debugging nightmare
}
```

```java
// GOOD — original cause preserved
try {
    return db.query(sql, params);
} catch (DatabaseException e) {
    throw new UserNotFoundException(email, e);  // always pass the cause
}
```

---

## 9. How This Shows Up in SDE2 Interviews

### Questions you will be asked (and what they're testing)

**"What happens if the parking lot is full?"**
→ They want: a named domain exception (`ParkingLotFullException`), thrown by the service, caught at the entry point, translated to a user message. Not: `return null`, not `RuntimeException("full")`.

**"What if two cars try to take the last slot simultaneously?"**
→ They want: concurrency awareness — the slot assignment should be atomic. One succeeds, the other gets `ParkingLotFullException`. This is where your contract (one slot assigned to one vehicle) meets thread-safety.

**"How does the ATM communicate that the card is blocked vs PIN is wrong vs insufficient funds?"**
→ They want: separate exception types. `CardBlockedException`, `IncorrectPINException`, `InsufficientFundsException` — each catchable separately, each with different user messages and different system actions.

**"What does your payment service return?"**
→ Don't say `boolean`. Say `PaymentResult` with status, transaction ID, failure reason. Or `Result<PaymentReceipt>` if you want to force the caller to handle both paths. The return type *is* the contract.

**"How do you prevent the service layer from leaking database exceptions to the controller?"**
→ They want: wrap-and-rethrow pattern. Repository catches `DatabaseException`, wraps into `RepositoryException` or domain exception. Controller only sees domain exceptions.

### Things to say in an interview to stand out

- *"I'd design a base `DomainException` for this service, and specific subclasses for each failure mode. That way the entry point can catch selectively."*
- *"I'd use `Optional` here rather than returning null — the caller is then forced to handle the absent case explicitly."*
- *"The repository would catch any infrastructure exceptions and wrap them in domain exceptions — the service layer should never see a `SQLException`."*
- *"For payment failures, I'd use a `Result<PaymentReceipt>` pattern rather than exceptions — a declined payment is an expected business outcome, not an exceptional condition."*
- *"Preconditions are validated at the constructor and at the service boundary. Once inside the domain, we trust objects are valid."*

---

## 10. Quick Reference Card

### The Three Parts of a Contract

```
Precondition  → What the caller must guarantee before calling
Postcondition → What the method guarantees when it returns
Invariant     → What must always be true about the object's state
```

### Exception Types

```
Checked   → External failures the caller can reasonably recover from (IO, Network)
Unchecked → Programming errors and domain violations (null, invalid arg, illegal state)
```

### Exception Hierarchy Pattern

```
AppException (abstract, unchecked)
 ├── ParkingException
 │    ├── ParkingLotFullException        (carries VehicleType)
 │    ├── VehicleAlreadyParkedException  (carries licensePlate)
 │    └── TicketNotFoundException        (carries ticketId)
 └── PaymentException
      ├── PaymentFailedException         (carries transactionId, reason)
      └── InsufficientFundsException     (carries accountId, required, available)
```

### Layered Handling

```
Domain / Entities     → Throw domain exceptions
Repository / Gateway  → Wrap infra exceptions → domain exceptions
Service / Use Case    → Catch what it can handle, let rest propagate
Entry Point           → Catch all domain exceptions, translate to response
```

### When to Use What

```
null return        → Never. Use Optional or throw.
boolean return     → Only for pure yes/no queries with no failure detail needed.
Exception          → Unexpected failure, programming error, unrecoverable state.
Result<T>          → Expected alternative outcomes the caller must handle explicitly.
Custom exception   → Always, when the failure is domain-specific.
```

### Key Patterns

```
Fail Fast          → Validate immediately, don't let invalid state propagate
Guard Clauses      → Precondition checks at the top, happy path at the bottom
CQS                → Methods either DO something or ANSWER something, never both
Validate at Edges  → Validate at boundary/constructor, trust inside
Wrap and Rethrow   → Translate infra exceptions to domain exceptions with cause
Result Pattern     → Return success/failure object when failure is a normal outcome
```

### Anti-Pattern Signals

```
catch (Exception e) { e.printStackTrace(); }     → Swallowing exceptions
return null on failure                            → Silent failure, NPE later
throw new RuntimeException("not found")          → No domain context, uncatchable
Catching at wrong layer                          → DB exceptions in controller
Losing the cause in wrap                         → throw new DomainEx() without (e)
Using try-catch as if-else                       → Exceptions for control flow
```

---

## 11. Revision Checklist

Come back after a few days and answer these without re-reading. If you can't answer cleanly, re-read that section.

- [ ] What are the three parts of an API contract? Give a concrete example of each from Parking Lot.
- [ ] What is the difference between a precondition violation and a postcondition violation? Whose bug is each?
- [ ] Explain Command-Query Separation. Why is `parkVehicle()` returning the slot AND parking the car a violation? How do you fix it?
- [ ] What is the difference between checked and unchecked exceptions? When would you use a checked exception in LLD?
- [ ] Design a custom exception hierarchy for an ATM system. List at least 5 exceptions with what data each carries.
- [ ] Explain the Fail Fast principle. Give a bad example and a good example.
- [ ] What is a Guard Clause? Rewrite this with guard clauses: a method that checks null, checks positive amount, checks account exists, checks daily limit — all nested.
- [ ] Explain "catch at the right layer." How should a `DatabaseException` from a repository be handled at the service layer and then at the controller?
- [ ] What is the Wrap and Rethrow pattern? Why must you always pass the cause when wrapping?
- [ ] What is the Result pattern? When would you use it instead of throwing an exception? Give an example from a payment system.
- [ ] What is wrong with `return null` when a record is not found? What should you use instead?
- [ ] Why is `catch (Exception e) { e.printStackTrace(); }` dangerous in production code?
- [ ] In Tic Tac Toe, what exception hierarchy would you design? What would `makeMove()` throw and when?
- [ ] What does "validate at boundaries, trust inside" mean? How does it relate to Value Objects?
- [ ] An interviewer asks: "What if two users try to book the last seat simultaneously?" — answer using contracts and exceptions.

---

## Key Takeaways

1. **A method is a contract, not just code.** Define what you promise before writing the implementation.
2. **Fail fast with named exceptions.** The moment something is wrong, throw — with a descriptive domain exception, not a silent null.
3. **Custom exceptions carry domain context.** `ParkingLotFullException(vehicleType)` is infinitely more useful than `RuntimeException("full")`.
4. **Catch at the layer that can act.** Catching prematurely swallows the error. Catching too late leaks internals.
5. **Always preserve the cause.** `throw new DomainException(msg, originalException)` — never drop the stack trace.
6. **Null return is a silent failure.** Use `Optional` for "might not exist", throw for "should have existed but doesn't".
7. **CQS keeps contracts honest.** If a method changes state AND returns a value, its contract is ambiguous.
8. **Validate once at the boundary.** Constructors and service boundaries are the gates. Once inside, trust the objects.
9. **The Result pattern is not a replacement for exceptions.** Use it when failure is an expected, normal domain outcome — not for bugs.
10. **Interviewers probe the unhappy path.** Designing Parking Lot perfectly in the happy path is table stakes. Handling the full and the duplicate vehicle cleanly is what gets you the SDE2 offer.
