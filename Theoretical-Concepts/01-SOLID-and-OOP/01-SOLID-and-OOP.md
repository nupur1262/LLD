# 01 — SOLID Principles and OOP Fundamentals

> **Purpose of this file:** Understand the four pillars of OOP and the five SOLID principles deeply enough to apply them in any LLD problem — not just recite them. Every principle here includes a bad example, a good example, and a signal for when you're violating it.

---

## Table of Contents

1. [Why OOP and SOLID Matter in LLD](#1-why-oop-and-solid-matter-in-lld)
2. [The Four Pillars of OOP](#2-the-four-pillars-of-oop)
   - [Encapsulation](#21-encapsulation)
   - [Abstraction](#22-abstraction)
   - [Inheritance](#23-inheritance)
   - [Polymorphism](#24-polymorphism)
3. [SOLID — Overview](#3-solid--overview)
4. [S — Single Responsibility Principle](#4-s--single-responsibility-principle)
5. [O — Open/Closed Principle](#5-o--openclosed-principle)
6. [L — Liskov Substitution Principle](#6-l--liskov-substitution-principle)
7. [I — Interface Segregation Principle](#7-i--interface-segregation-principle)
8. [D — Dependency Inversion Principle](#8-d--dependency-inversion-principle)
9. [How SOLID Shows Up in LLD Problems](#9-how-solid-shows-up-in-lld-problems)
10. [OOP vs Procedural — When OOP Wins](#10-oop-vs-procedural--when-oop-wins)
11. [Abstract Class vs Interface — When to Use Which](#11-abstract-class-vs-interface--when-to-use-which)
12. [Common Mistakes and Smells](#12-common-mistakes-and-smells)
13. [Quick Reference Card](#13-quick-reference-card)
14. [Revision Checklist](#14-revision-checklist)

---

## 1. Why OOP and SOLID Matter in LLD

In an LLD interview, the interviewer is not just checking if you can write classes. They are checking whether your design can **survive change**.

Requirements always change. A parking lot that only handled cars now needs to handle bikes and trucks. A payment system that only accepted cards now needs UPI and wallets. Your design should absorb these changes without breaking everything else.

OOP gives you the building blocks. SOLID tells you how to use those building blocks correctly. Together they produce designs that are:

- **Extensible** — add new features without modifying existing code
- **Maintainable** — change one class without breaking others
- **Testable** — isolate and test components independently
- **Readable** — another developer (or future-you) understands the intent immediately

When you violate SOLID, the cost shows up as: one class having 500 lines, adding a feature requiring changes in 8 files, a bug fix breaking something unrelated. These are design smells — SOLID violations made visible.

---

## 2. The Four Pillars of OOP

### 2.1 Encapsulation

**What it is:** Bundling data (attributes) and the methods that operate on that data together inside a class, and hiding the internal state from the outside world.

**The core idea:** Objects control their own state. No one else should be able to directly reach in and change your internals.

**Bad example:**
```java
class BankAccount {
    public double balance;  // anyone can change this directly
}

// Somewhere else in the code:
account.balance = -99999;  // This should never be allowed
```

**Good example:**
```java
class BankAccount {
    private double balance;  // hidden — nobody touches this directly

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        this.balance += amount;
    }

    public void withdraw(double amount) {
        if (amount > balance) throw new IllegalStateException("Insufficient funds");
        this.balance -= amount;
    }

    public double getBalance() {
        return balance;  // read-only access
    }
}
```

**Why it matters in LLD:** When you define your entities, every attribute should be private by default. Ask yourself: what is the only valid way to change this value? Make that a method with validation.

**The signal you're violating it:** You have public fields. Or getter+setter for every single field with zero validation — that is encapsulation in name only.

---

### 2.2 Abstraction

**What it is:** Exposing only *what* something does, hiding *how* it does it. Users of a class work with its interface (public methods), not its implementation.

**The core idea:** The caller should not need to know the internals. A driver uses a car's steering wheel and pedals — they don't need to understand the engine combustion cycle.

**Bad example:**
```java
class EmailSender {
    public void connectToSMTPServer(String host, int port) { ... }
    public void authenticateWithCredentials(String user, String pass) { ... }
    public void buildMIMEMessage(String to, String subject, String body) { ... }
    public void transmitMessage() { ... }
    public void closeConnection() { ... }
}

// The caller has to orchestrate all of this manually — they know too much
sender.connectToSMTPServer("smtp.gmail.com", 587);
sender.authenticateWithCredentials("user", "pass");
sender.buildMIMEMessage(to, subject, body);
sender.transmitMessage();
sender.closeConnection();
```

**Good example:**
```java
class EmailSender {
    public void send(String to, String subject, String body) {
        // All the complexity hidden inside
        connectToSMTPServer();
        authenticate();
        buildMessage(to, subject, body);
        transmit();
        closeConnection();
    }

    private void connectToSMTPServer() { ... }
    private void authenticate() { ... }
    // ...
}

// Caller only sees the abstraction
sender.send("user@example.com", "Hello", "Body text");
```

**Why it matters in LLD:** Abstraction is why interfaces and abstract classes exist. You define *what* your system can do (the contract), and hide *how* behind implementations. This lets you swap implementations without changing callers.

**The signal you're violating it:** Callers need to know the order of method calls. Internal helper methods are public. Changing the implementation requires changing the callers too.

---

### 2.3 Inheritance

**What it is:** A child class acquires the attributes and methods of a parent class. It represents an "is-a" relationship.

**The core idea:** Share common behaviour in a parent. Specialise in children. Don't repeat yourself.

**Example:**
```java
abstract class Vehicle {
    protected String licensePlate;
    protected VehicleType type;

    public String getLicensePlate() { return licensePlate; }
    public abstract int getFeePerHour();  // each vehicle type prices differently
}

class Car extends Vehicle {
    @Override
    public int getFeePerHour() { return 50; }
}

class Bike extends Vehicle {
    @Override
    public int getFeePerHour() { return 20; }
}
```

**When NOT to use inheritance:** When the relationship is "has-a", not "is-a". A `Car` *has an* `Engine` — don't make `Car extends Engine`. If you find yourself inheriting just to reuse one method, use composition instead.

**The rule:** If you cannot say "Child *is a* Parent" naturally in plain English, don't use inheritance.

**The signal you're misusing it:** You override a method just to throw an exception ("this subclass doesn't support this"). That's Liskov Substitution violation (see section 6).

---

### 2.4 Polymorphism

**What it is:** The ability for different types to be treated as the same type through a common interface, each responding to the same method call in its own way.

**Two types:**

**Compile-time polymorphism (Method Overloading)** — same method name, different parameters:
```java
class Calculator {
    public int add(int a, int b) { return a + b; }
    public double add(double a, double b) { return a + b; }
    public int add(int a, int b, int c) { return a + b + c; }
}
```

**Runtime polymorphism (Method Overriding)** — the more important one for LLD:
```java
List<Vehicle> vehicles = new ArrayList<>();
vehicles.add(new Car());
vehicles.add(new Bike());
vehicles.add(new Truck());

// Same method call, different behaviour per type — decided at runtime
for (Vehicle v : vehicles) {
    int fee = v.getFeePerHour();  // Car returns 50, Bike returns 20, Truck returns 100
}
```

**Why it matters in LLD:** Polymorphism lets you write code that works for all current *and future* types. If you add a new vehicle type tomorrow (`ElectricCar`), the loop above still works — you don't touch it. This directly enables the Open/Closed Principle.

**The signal you're missing it:** You have `if (vehicle instanceof Car)` or `switch (vehicle.getType())` blocks scattered through your code. That's a sign you should be using polymorphism instead.

---

## 3. SOLID — Overview

SOLID is an acronym for five design principles coined by Robert C. Martin. Each principle solves a specific class of design problem.

| Letter | Principle | One-line summary |
|---|---|---|
| **S** | Single Responsibility | A class should have only one reason to change |
| **O** | Open/Closed | Open for extension, closed for modification |
| **L** | Liskov Substitution | Subtypes must be substitutable for their base types |
| **I** | Interface Segregation | Don't force classes to implement methods they don't need |
| **D** | Dependency Inversion | Depend on abstractions, not on concrete implementations |

They are not independent rules — they work together. Violating one often leads to violating others.

---

## 4. S — Single Responsibility Principle

**Statement:** A class should have only one reason to change.

In other words: a class should do one thing and do it well.

**Bad example — a class with too many responsibilities:**
```java
class Invoice {
    private List<Item> items;
    private Customer customer;

    // Responsibility 1: business logic
    public double calculateTotal() { ... }
    public double applyDiscount() { ... }

    // Responsibility 2: persistence
    public void saveToDatabase() { ... }
    public Invoice loadFromDatabase(int id) { ... }

    // Responsibility 3: presentation
    public void printInvoice() { ... }
    public void sendByEmail() { ... }
}
```

If the database changes, you touch `Invoice`. If the email format changes, you touch `Invoice`. If the discount logic changes, you touch `Invoice`. Three reasons to change = three responsibilities = SRP violation.

**Good example — responsibilities split into separate classes:**
```java
class Invoice {
    private List<Item> items;
    private Customer customer;

    public double calculateTotal() { ... }
    public double applyDiscount() { ... }
}

class InvoiceRepository {
    public void save(Invoice invoice) { ... }
    public Invoice findById(int id) { ... }
}

class InvoicePrinter {
    public void print(Invoice invoice) { ... }
}

class InvoiceEmailer {
    public void send(Invoice invoice, String email) { ... }
}
```

Now each class has exactly one reason to change.

**How to find SRP violations:** Describe what a class does. If you use the word "and" more than once — it's violating SRP.

"The `Invoice` class calculates totals **and** saves to the database **and** sends emails." — three `and`s = three responsibilities.

**In LLD problems, SRP shows up as:** Splitting a `ParkingLotManager` god class into `ParkingLot` (domain), `TicketService` (ticket logic), `PaymentService` (payment logic), `FeeCalculator` (fee logic).

---

## 5. O — Open/Closed Principle

**Statement:** Software entities should be open for extension but closed for modification.

When requirements change, you should be able to add new behaviour by adding new code — not by changing existing, working code.

**Bad example — adding a new vehicle type breaks existing code:**
```java
class FeeCalculator {
    public double calculate(Vehicle vehicle, int hours) {
        if (vehicle.getType() == VehicleType.CAR) {
            return hours * 50;
        } else if (vehicle.getType() == VehicleType.BIKE) {
            return hours * 20;
        }
        // Add Truck? You must modify this method. Every change risks breaking Car and Bike logic.
        return 0;
    }
}
```

Every new vehicle type requires opening and modifying `FeeCalculator`. This is closed for extension and open for modification — exactly backwards.

**Good example — extend by adding, not modifying:**
```java
interface FeeStrategy {
    double calculate(int hours);
}

class CarFeeStrategy implements FeeStrategy {
    public double calculate(int hours) { return hours * 50; }
}

class BikeFeeStrategy implements FeeStrategy {
    public double calculate(int hours) { return hours * 20; }
}

class TruckFeeStrategy implements FeeStrategy {
    public double calculate(int hours) { return hours * 100; }
}

class FeeCalculator {
    public double calculate(FeeStrategy strategy, int hours) {
        return strategy.calculate(hours);  // never changes
    }
}
```

Adding `ElectricCarFeeStrategy` tomorrow requires zero changes to existing classes.

**The mechanism that enables OCP:** Abstraction. Interfaces and abstract classes let you define extension points. Polymorphism lets new types plug in without changing callers.

**In LLD problems, OCP shows up as:** Using the Strategy pattern for pricing, sorting, or discounts. Using the Factory pattern to create objects. Anywhere you'd otherwise write a switch/if-else on a type.

---

## 6. L — Liskov Substitution Principle

**Statement:** If S is a subtype of T, then objects of type T may be replaced with objects of type S without altering the correctness of the program.

In plain English: anywhere you use a parent class, a child class should work as a drop-in replacement without surprising the caller.

**Bad example — the classic Rectangle/Square problem:**
```java
class Rectangle {
    protected int width, height;

    public void setWidth(int w) { this.width = w; }
    public void setHeight(int h) { this.height = h; }
    public int area() { return width * height; }
}

class Square extends Rectangle {
    // A square has equal sides, so we override both setters
    @Override
    public void setWidth(int w) {
        this.width = w;
        this.height = w;  // force equal sides
    }

    @Override
    public void setHeight(int h) {
        this.height = h;
        this.width = h;  // force equal sides
    }
}

// This code works fine with Rectangle
Rectangle r = new Rectangle();
r.setWidth(5);
r.setHeight(3);
System.out.println(r.area());  // 15 — correct

// But breaks when you swap in a Square
Rectangle r = new Square();  // substituting subtype
r.setWidth(5);
r.setHeight(3);
System.out.println(r.area());  // 9 — wrong! setHeight changed width too
```

The Square *is-a* Rectangle geometrically, but not behaviourally in this model. LSP is violated.

**Another common violation — throwing exceptions in overrides:**
```java
class Bird {
    public void fly() { ... }
}

class Penguin extends Bird {
    @Override
    public void fly() {
        throw new UnsupportedOperationException("Penguins can't fly");
    }
}

// Code that works with Bird breaks when given a Penguin
Bird b = new Penguin();
b.fly();  // throws — caller didn't expect this
```

**Fix:** Restructure the hierarchy so that only birds that can fly extend `FlyingBird`. `Penguin` extends `Bird` directly without inheriting `fly()`.

```java
abstract class Bird { ... }
abstract class FlyingBird extends Bird {
    public abstract void fly();
}
class Eagle extends FlyingBird { public void fly() { ... } }
class Penguin extends Bird { ... }  // no fly() here
```

**The test for LSP:** Can you substitute every subclass wherever the parent is expected, without the caller needing to know which subtype it received?

**In LLD problems, LSP shows up as:** Designing your `Vehicle`, `Payment`, or `Notification` hierarchies. If any subclass throws `UnsupportedOperationException` on an inherited method, you're violating LSP.

---

## 7. I — Interface Segregation Principle

**Statement:** Clients should not be forced to depend on interfaces they do not use.

Prefer small, focused interfaces over large, fat ones. A class should not be forced to implement methods it has no use for.

**Bad example — a fat interface:**
```java
interface Worker {
    void work();
    void eat();
    void sleep();
}

class HumanWorker implements Worker {
    public void work() { ... }
    public void eat() { ... }
    public void sleep() { ... }
}

class RobotWorker implements Worker {
    public void work() { ... }
    public void eat() { throw new UnsupportedOperationException("Robots don't eat"); }
    public void sleep() { throw new UnsupportedOperationException("Robots don't sleep"); }
}
```

`RobotWorker` is forced to implement `eat()` and `sleep()` even though it has no use for them.

**Good example — segregated interfaces:**
```java
interface Workable {
    void work();
}

interface Feedable {
    void eat();
}

interface Restable {
    void sleep();
}

class HumanWorker implements Workable, Feedable, Restable {
    public void work() { ... }
    public void eat() { ... }
    public void sleep() { ... }
}

class RobotWorker implements Workable {
    public void work() { ... }
    // No forced stubs. Clean.
}
```

**In LLD problems, ISP shows up as:** Splitting a `Vehicle` interface into `Drivable`, `Flyable`, `Swimmable`. Splitting a notification interface into `Emailable`, `SMSable`, `Pushable` so a class only implements the channels it supports.

**The smell:** You implement an interface and some methods throw `UnsupportedOperationException` or are left as empty stubs. That is ISP violation.

---

## 8. D — Dependency Inversion Principle

**Statement:** High-level modules should not depend on low-level modules. Both should depend on abstractions. Abstractions should not depend on details. Details should depend on abstractions.

In plain English: program to interfaces, not implementations.

**Bad example — high-level class depends on a concrete low-level class:**
```java
class NotificationService {
    private EmailSender emailSender;  // concrete dependency

    public NotificationService() {
        this.emailSender = new EmailSender();  // hardwired — cannot swap
    }

    public void notify(String message) {
        emailSender.send(message);
    }
}
```

If you want to switch to SMS tomorrow, you must open `NotificationService` and change its internals. `NotificationService` (high-level) is tightly coupled to `EmailSender` (low-level).

**Good example — both depend on an abstraction:**
```java
interface NotificationChannel {
    void send(String message);
}

class EmailChannel implements NotificationChannel {
    public void send(String message) { /* email logic */ }
}

class SMSChannel implements NotificationChannel {
    public void send(String message) { /* SMS logic */ }
}

class PushChannel implements NotificationChannel {
    public void send(String message) { /* push notification logic */ }
}

class NotificationService {
    private NotificationChannel channel;  // depends on abstraction

    // Dependency injected from outside — not created internally
    public NotificationService(NotificationChannel channel) {
        this.channel = channel;
    }

    public void notify(String message) {
        channel.send(message);
    }
}

// At the call site, you decide the concrete type
NotificationService service = new NotificationService(new EmailChannel());
NotificationService service = new NotificationService(new SMSChannel());  // swap with zero changes inside
```

**Dependency Injection** is the technique DIP leads you to. Pass dependencies in through the constructor (constructor injection) rather than creating them inside the class.

**In LLD problems, DIP shows up as:** Any time you have a service that uses a repository, payment gateway, notification sender, or logger — inject the abstraction, don't instantiate the concrete class inside.

**The smell:** `new ConcreteClass()` deep inside a high-level class. Anywhere you see that, ask: should this be an injected abstraction?

---

## 9. How SOLID Shows Up in LLD Problems

This is a map of which principle to apply in which situation. Use it when designing any system.

| Situation | Principle to Apply |
|---|---|
| A class is getting too large / doing too many things | SRP — split into focused classes |
| Adding a new type (vehicle, payment method, shape) requires changing existing code | OCP — introduce an interface + new implementation |
| A subclass throws exceptions on inherited methods | LSP — restructure the hierarchy |
| An interface has methods irrelevant to some implementors | ISP — split the interface |
| A class creates its own dependencies with `new` | DIP — inject abstractions via constructor |
| `if instanceof` or `switch on type` blocks | OCP + Polymorphism — move behaviour into the type |
| Hard to write unit tests (need real DB, real email server) | DIP — inject mocks via interfaces |

### Parking Lot — SOLID in action

| Decision | Principle |
|---|---|
| `Vehicle` is abstract, `Car` and `Bike` extend it | Inheritance + Polymorphism |
| `FeeCalculator` depends on `FeeStrategy` interface, not `CarFeeStrategy` directly | OCP + DIP |
| `ParkingSlot`, `Ticket`, `PaymentService` are separate classes | SRP |
| `ParkingSlot` interface only has slot-related methods, not payment methods | ISP |
| `PaymentService` receives a `PaymentGateway` interface, not a concrete gateway | DIP |

---

## 10. OOP vs Procedural — When OOP Wins

Understanding why OOP exists helps you use it correctly.

**Procedural code** is a sequence of instructions. Data and logic are separate. Good for scripts, simple transformations, algorithmic problems.

**OOP** models the world as objects with state and behaviour. Data and logic live together. Good for systems with multiple interacting entities, complex state, and requirements that change.

OOP wins when:
- You have multiple entity types that share some behaviour but differ in others (polymorphism)
- State needs to be protected and validated (encapsulation)
- The system will grow — new types, new features (OCP via abstraction)
- You want to test components in isolation (DIP via injection)

OOP adds complexity. Don't reach for classes and inheritance for a 30-line script. The payoff comes in larger systems — exactly the kind you model in LLD.

---

## 11. Abstract Class vs Interface — When to Use Which

This is one of the most common questions in LLD. The answer comes from what you're trying to express.

| | Abstract Class | Interface |
|---|---|---|
| **Expresses** | "is-a" — a shared base type | "can-do" — a capability/contract |
| **Can have** | State (fields), concrete methods, constructors | Only method signatures (Java 8+: default methods) |
| **Inheritance** | Single inheritance only | A class can implement many interfaces |
| **Use when** | Subclasses share common implementation | Unrelated classes share a capability |

**Use an abstract class when:**
- Subclasses genuinely share both state and behaviour
- You want to provide a partial implementation that subclasses complete
- Example: `Vehicle` (all vehicles have `licensePlate`, `type`, and `getLicensePlate()`)

```java
abstract class Vehicle {
    protected String licensePlate;  // shared state
    protected VehicleType type;

    public String getLicensePlate() { return licensePlate; }  // shared behaviour

    public abstract int getFeePerHour();  // each subclass defines this differently
}
```

**Use an interface when:**
- You want to define a contract that unrelated classes can fulfil
- You want to support multiple "can-do" capabilities on one class
- Example: A `Drone` can fly and can deliver — `Flyable` and `Deliverable` are both interfaces

```java
interface Flyable {
    void fly();
    int getMaxAltitude();
}

interface Deliverable {
    void deliver(Package p);
    double getMaxPayload();
}

class Drone implements Flyable, Deliverable {
    public void fly() { ... }
    public int getMaxAltitude() { return 400; }
    public void deliver(Package p) { ... }
    public double getMaxPayload() { return 5.0; }
}
```

**The rule of thumb:**
- Abstract class = what you *are* (a Vehicle, a Shape, a User)
- Interface = what you *can do* (Flyable, Payable, Printable, Serializable)

---

## 12. Common Mistakes and Smells

### Mistake 1: Writing getters and setters for every field
Getters + setters with no logic = public fields in disguise. Ask: what is the *valid* way to change this state? Put that in a method.

### Mistake 2: Inheriting for code reuse when you should compose
If `B` extends `A` just to reuse one method, but `B` is not really an `A` — use composition. Create an instance of `A` inside `B` and call its methods.

### Mistake 3: One interface for everything
A `Vehicle` interface with `fly()`, `sail()`, `drive()`, `getFuelType()`, `getWingSpan()` is ISP violation. Split by capability.

### Mistake 4: `instanceof` checks in business logic
```java
if (payment instanceof CreditCardPayment) { ... }
else if (payment instanceof UPIPayment) { ... }
```
This is OCP violation. Move the behaviour into the `Payment` interface and let polymorphism handle it.

### Mistake 5: Creating dependencies inside a class with `new`
```java
class OrderService {
    private PaymentService ps = new PaymentService();  // tight coupling
}
```
Inject it. This makes the class testable and swappable.

### Mistake 6: God class
A class called `SystemManager` or `AppController` that coordinates everything. Split by responsibility.

### Mistake 7: Deep inheritance chains
`Vehicle → MotorVehicle → Car → ElectricCar → Tesla`. More than 2–3 levels usually signals overuse of inheritance. Favour composition.

---

## 13. Quick Reference Card

### OOP Pillars

```
Encapsulation   → Private fields, controlled access through methods
Abstraction     → Expose what, hide how. Interfaces and abstract classes.
Inheritance     → "is-a". Share behaviour in parent, specialise in child.
Polymorphism    → Same method, different behaviour per type at runtime.
```

### SOLID in one line each

```
S — One class, one job. One reason to change.
O — Add new types by adding code, not editing existing code.
L — Subtypes must honour the contract of the parent — no surprises.
I — Small focused interfaces. Don't force useless method implementations.
D — Depend on abstractions. Inject dependencies, don't hardwire them.
```

### The violation smells

```
God class (1000 lines, does everything)   → SRP
if-instanceof or switch-on-type blocks    → OCP + Polymorphism
Subclass throws UnsupportedOperation      → LSP
Empty stubs or forced irrelevant methods  → ISP
new ConcreteClass() inside high-level     → DIP
```

### Abstract class vs Interface

```
Abstract class → what you ARE  → "is-a" → Vehicle, Shape, User
Interface      → what you DO   → "can"  → Flyable, Payable, Sortable
```

---

## 14. Revision Checklist

Come back here after a week and test yourself — answer these without re-reading the file first.

- [ ] Explain Encapsulation. Why is a getter+setter with no logic still a violation?
- [ ] What is the difference between Abstraction and Encapsulation? (Common interview trick question — they are related but different.)
- [ ] Give a real-world example of Composition vs Inheritance and when to pick each.
- [ ] Explain Runtime Polymorphism. How does it enable OCP?
- [ ] Give an example of an SRP violation from a real LLD problem (Parking Lot, ATM, etc.).
- [ ] Explain OCP without using the word "extension". Give a concrete before/after.
- [ ] What is the Rectangle/Square problem and why does it violate LSP?
- [ ] Design a notification system (Email, SMS, Push) that satisfies all five SOLID principles. Sketch the classes and interfaces.
- [ ] When do you use an abstract class vs an interface? Give one example of each.
- [ ] What does Dependency Injection mean? Write a constructor-injected example.

---

## Key Takeaways

1. **OOP pillars are the vocabulary. SOLID is the grammar.** Know both before writing any design.
2. **Encapsulation is not just `private` fields.** It is controlled, validated access to state.
3. **Polymorphism replaces `if instanceof`.** Whenever you see type-checking in business logic, think polymorphism.
4. **OCP is enabled by abstraction.** Define interfaces first, then implementations.
5. **LSP is about honoring contracts.** If a subclass surprises the caller, the hierarchy is wrong.
6. **ISP keeps interfaces honest.** If implementing it requires empty stubs, split the interface.
7. **DIP makes things testable.** If you can't write a unit test without a real database or email server, you have a DIP violation.
8. **Abstract class = shared state + partial implementation. Interface = pure contract.**
9. **The smell words: God class, instanceof, UnsupportedOperation, new inside high-level.** Each one points to a specific principle violation.
