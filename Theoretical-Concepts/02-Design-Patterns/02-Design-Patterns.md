# 02 — Design Patterns

> **Purpose of this file:** Understand the most important design patterns for LLD interviews — what problem each one solves, when to use it, when NOT to use it, and how it shows up in real LLD problems like Parking Lot, Ride Sharing, and Notification Systems. Patterns are grouped by category. Each has a bad example, a good example, and a "when you need it" signal.

---

## Table of Contents

1. [Why Design Patterns Exist](#1-why-design-patterns-exist)
2. [The Three Categories](#2-the-three-categories)
3. [Creational Patterns](#3-creational-patterns)
   - [Singleton](#31-singleton)
   - [Factory Method](#32-factory-method)
   - [Abstract Factory](#33-abstract-factory)
   - [Builder](#34-builder)
   - [Prototype](#35-prototype)
4. [Structural Patterns](#4-structural-patterns)
   - [Adapter](#41-adapter)
   - [Decorator](#42-decorator)
   - [Facade](#43-facade)
   - [Composite](#44-composite)
   - [Proxy](#45-proxy)
   - [Bridge](#46-bridge)
5. [Behavioural Patterns](#5-behavioural-patterns)
   - [Strategy](#51-strategy)
   - [Observer](#52-observer)
   - [Command](#53-command)
   - [State](#54-state)
   - [Template Method](#55-template-method)
   - [Chain of Responsibility](#56-chain-of-responsibility)
   - [Iterator](#57-iterator)
6. [Pattern Combinations in Real LLD Problems](#6-pattern-combinations-in-real-lld-problems)
7. [Pattern vs Pattern — When They Seem Similar](#7-pattern-vs-pattern--when-they-seem-similar)
8. [How to Spot Which Pattern to Use](#8-how-to-spot-which-pattern-to-use)
9. [Interview Question Bank](#9-interview-question-bank)
10. [Quick Reference Card](#10-quick-reference-card)
11. [Revision Checklist](#11-revision-checklist)

---

## 1. Why Design Patterns Exist

Design patterns are **reusable solutions to recurring design problems**. They are not code you copy — they are proven templates for structuring relationships between classes.

They exist because the same design problems appear over and over:
- How do I create objects without tightly coupling the caller to a specific class?
- How do I add new behaviour to an object without modifying its class?
- How do I notify many objects when something changes?
- How do I allow an algorithm to vary independently from the code that uses it?

Patterns answer these questions with battle-tested structures.

**Why they matter in interviews:**

At SDE-2 level, an interviewer doesn't just want to see that you can define classes. They want to see that you make *deliberate design decisions*. When you say "I'll use a Strategy pattern here because the fee calculation algorithm needs to vary by vehicle type", you demonstrate design thinking — not just coding ability.

**What interviewers are actually checking:**
- Do you recognise when a pattern applies?
- Can you justify *why* you chose it over alternatives?
- Does your design remain extensible when requirements change?

---

## 2. The Three Categories

| Category | Deals with | Patterns |
|---|---|---|
| **Creational** | How objects are created | Singleton, Factory Method, Abstract Factory, Builder, Prototype |
| **Structural** | How classes and objects are composed | Adapter, Decorator, Facade, Composite, Proxy, Bridge |
| **Behavioural** | How objects communicate and distribute responsibility | Strategy, Observer, Command, State, Template Method, Chain of Responsibility, Iterator |

**Interview priority — learn in this order:**

Tier 1 (very high frequency): Strategy, Observer, Factory Method, Singleton, Builder, Decorator  
Tier 2 (medium frequency): Command, State, Adapter, Facade, Proxy, Template Method, Composite  
Tier 3 (good to know): Abstract Factory, Bridge, Chain of Responsibility, Iterator, Prototype

---

## 3. Creational Patterns

### 3.1 Singleton

**Problem it solves:** You need exactly one instance of a class shared across the entire application — a shared resource like a config store, connection pool, or logger.

**The core idea:** Make the constructor private. Provide a static method that returns the single instance, creating it on the first call.

**Bad — no thread safety:**
```java
class Logger {
    private static Logger instance;

    private Logger() {}

    public static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();   // race condition — two threads can both enter here
        }
        return instance;
    }
}
```

**Good — Initialization-on-Demand Holder (simplest, cleanest):**
```java
class Logger {
    private Logger() {}

    private static class Holder {
        private static final Logger INSTANCE = new Logger();
        // JVM guarantees class loading is thread-safe — no synchronisation needed
    }

    public static Logger getInstance() {
        return Holder.INSTANCE;
    }

    public void log(String message) {
        System.out.println("[LOG] " + message);
    }
}
```

**Also good — Double-checked locking (when lazy init + thread safety is needed explicitly):**
```java
class ConfigManager {
    private static volatile ConfigManager instance;  // volatile is mandatory
    private Map<String, String> config;

    private ConfigManager() {
        config = loadConfig();  // expensive init
    }

    public static ConfigManager getInstance() {
        if (instance == null) {                      // fast path — no lock after init
            synchronized (ConfigManager.class) {
                if (instance == null) {              // safe — only one thread creates
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }
}
```

**When you need it:**
- Logging service
- Config / properties manager
- Thread pool manager
- Database connection pool
- Cache manager

**When NOT to use it:**
- When you might need more than one instance in the future (e.g. multi-tenant apps)
- When it makes testing hard (Singletons are notoriously difficult to mock)
- Don't reach for Singleton just because a class feels "global"

**The smell that tells you to use it:** "Only one of these should ever exist in the system."

---

### 3.2 Factory Method

**Problem it solves:** You need to create objects, but the exact type to create should be decided by subclasses or based on input — not hardwired into the caller.

**The core idea:** Define an interface for creating an object, but let subclasses decide which class to instantiate. The caller works with the interface, never the concrete type.

**Bad — caller hardwires the type:**
```java
class NotificationService {
    public void sendAlert(String type, String message) {
        if (type.equals("EMAIL")) {
            new EmailNotification().send(message);    // hardwired — OCP violation
        } else if (type.equals("SMS")) {
            new SMSNotification().send(message);
        } else if (type.equals("PUSH")) {
            new PushNotification().send(message);
        }
        // Adding WhatsApp? Open this method and modify it. OCP violation.
    }
}
```

**Good — Factory Method:**
```java
interface Notification {
    void send(String message);
}

class EmailNotification implements Notification {
    public void send(String message) { System.out.println("Email: " + message); }
}

class SMSNotification implements Notification {
    public void send(String message) { System.out.println("SMS: " + message); }
}

class PushNotification implements Notification {
    public void send(String message) { System.out.println("Push: " + message); }
}

// The factory — creation logic in one place
class NotificationFactory {
    public static Notification create(String type) {
        return switch (type) {
            case "EMAIL" -> new EmailNotification();
            case "SMS"   -> new SMSNotification();
            case "PUSH"  -> new PushNotification();
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }
}

// Caller — never touches concrete types
class NotificationService {
    public void sendAlert(String type, String message) {
        Notification notification = NotificationFactory.create(type);
        notification.send(message);
    }
}
```

**Adding WhatsApp:** Create `WhatsAppNotification implements Notification`. Add one case to the factory. Zero changes to `NotificationService`. OCP satisfied.

**When you need it:**
- Object creation logic is complex or should be centralised
- The caller should not depend on concrete types
- You need to swap implementations based on config or input
- Vehicle creation in Parking Lot, Payment method creation, Logger type selection

**The smell that tells you to use it:** "I'm using `new ConcreteClass()` in multiple places and I'd have to change all of them if I add a new type."

---

### 3.3 Abstract Factory

**Problem it solves:** You need to create **families of related objects** that must be used together, without specifying their concrete classes.

**The core idea:** An interface for creating multiple related products. Different factories produce different product families. The caller uses only the factory interface.

**Example — UI components for different OS themes:**
```java
// Product interfaces
interface Button { void render(); }
interface Checkbox { void render(); }

// Dark theme products
class DarkButton implements Button {
    public void render() { System.out.println("Dark button"); }
}
class DarkCheckbox implements Checkbox {
    public void render() { System.out.println("Dark checkbox"); }
}

// Light theme products
class LightButton implements Button {
    public void render() { System.out.println("Light button"); }
}
class LightCheckbox implements Checkbox {
    public void render() { System.out.println("Light checkbox"); }
}

// Abstract Factory — creates a consistent family
interface UIFactory {
    Button createButton();
    Checkbox createCheckbox();
}

class DarkThemeFactory implements UIFactory {
    public Button createButton() { return new DarkButton(); }
    public Checkbox createCheckbox() { return new DarkCheckbox(); }
}

class LightThemeFactory implements UIFactory {
    public Button createButton() { return new LightButton(); }
    public Checkbox createCheckbox() { return new LightCheckbox(); }
}

// Client — works with factory interface, never concrete types
class Application {
    private UIFactory factory;

    public Application(UIFactory factory) {
        this.factory = factory;
    }

    public void render() {
        Button btn = factory.createButton();
        Checkbox cb = factory.createCheckbox();
        btn.render();
        cb.render();
    }
}
```

**Factory Method vs Abstract Factory:**
- Factory Method: creates **one type** of object. One factory method.
- Abstract Factory: creates **a family of related objects**. Multiple factory methods grouped together.

**When you need it:**
- When your system needs to work with multiple families of related products
- When you want to ensure products from the same family are used together
- Cross-platform UI components, cloud provider resource factories (S3+EC2 vs GCS+GCE)

**The smell that tells you to use it:** "I need to create multiple related things that belong together and must be consistent."

---

### 3.4 Builder

**Problem it solves:** Constructing a complex object step by step, where the construction process should allow different representations.

**The core idea:** Separate the construction of a complex object from its representation. A Director orchestrates the steps. A Builder implements each step.

**Bad — telescoping constructor:**
```java
// Adding a new optional field? Add another constructor. Nightmare.
class Pizza {
    public Pizza(String size, String crust) { ... }
    public Pizza(String size, String crust, String sauce) { ... }
    public Pizza(String size, String crust, String sauce, boolean cheese) { ... }
    public Pizza(String size, String crust, String sauce, boolean cheese, List<String> toppings) { ... }
}
```

**Good — Builder:**
```java
class Pizza {
    private final String size;         // required
    private final String crust;        // required
    private final String sauce;        // optional
    private final boolean extraCheese; // optional
    private final List<String> toppings;

    private Pizza(Builder builder) {
        this.size = builder.size;
        this.crust = builder.crust;
        this.sauce = builder.sauce;
        this.extraCheese = builder.extraCheese;
        this.toppings = builder.toppings;
    }

    public static class Builder {
        private final String size;    // required — in constructor
        private final String crust;
        private String sauce = "tomato";
        private boolean extraCheese = false;
        private List<String> toppings = new ArrayList<>();

        public Builder(String size, String crust) {
            this.size = size;
            this.crust = crust;
        }

        public Builder sauce(String sauce) { this.sauce = sauce; return this; }
        public Builder extraCheese() { this.extraCheese = true; return this; }
        public Builder topping(String t) { this.toppings.add(t); return this; }

        public Pizza build() {
            return new Pizza(this);
        }
    }
}

// Usage — readable, no argument position confusion
Pizza pizza = new Pizza.Builder("LARGE", "THIN")
    .sauce("bbq")
    .extraCheese()
    .topping("mushrooms")
    .topping("olives")
    .build();
```

**When you need it:**
- Object has many optional parameters (4+ fields where most are optional)
- Object construction involves multiple steps
- You want immutable objects with a clean construction API
- `HttpRequest`, `DatabaseConfig`, `UserProfile`, `Booking` with many optional fields

**The smell that tells you to use it:** Constructor has more than 4 parameters, especially optional ones. You find yourself passing `null` for fields you don't need.

---

### 3.5 Prototype

**Problem it solves:** Create new objects by cloning an existing object — when creation from scratch is expensive and you have a ready-made instance to copy from.

**The core idea:** Implement `clone()` on your objects. Copy an existing instance instead of constructing a new one.

```java
interface Cloneable {
    Shape clone();
}

class Circle implements Cloneable {
    private int radius;
    private String colour;

    public Circle(int radius, String colour) {
        this.radius = radius;
        this.colour = colour;
    }

    // Copy constructor — explicit, clear
    public Circle(Circle source) {
        this.radius = source.radius;
        this.colour = source.colour;
    }

    public Circle clone() {
        return new Circle(this);
    }
}

// Usage
Circle original = new Circle(10, "red");
Circle copy = original.clone();
// Modify copy independently — original unchanged
```

**When you need it:**
- Object creation is expensive (loads config from DB, makes network calls)
- You need many similar objects with small variations
- Game development (clone template enemies), document templates

**When NOT to use it:** If your objects are simple and cheap to construct. Cloning adds complexity for no benefit.

---

## 4. Structural Patterns

### 4.1 Adapter

**Problem it solves:** You have an existing class with a useful interface, but it doesn't match the interface your client expects. You can't change either side.

**The core idea:** Wrap the incompatible class in an adapter that translates its interface into the expected one.

**Real-world analogy:** A power adapter lets your Indian plug work in a UK socket. Neither the plug nor the socket changes — the adapter translates.

**Example — integrating a third-party payment gateway:**
```java
// Your system expects this interface
interface PaymentProcessor {
    boolean processPayment(double amount, String currency);
}

// Third-party library you cannot modify
class StripeAPI {
    public StripeResponse charge(StripeRequest request) {
        // Stripe's own API — different method name, different params
        return new StripeResponse();
    }
}

// Adapter — wraps Stripe, exposes your interface
class StripeAdapter implements PaymentProcessor {
    private final StripeAPI stripe;

    public StripeAdapter(StripeAPI stripe) {
        this.stripe = stripe;
    }

    public boolean processPayment(double amount, String currency) {
        StripeRequest request = new StripeRequest(amount, currency);
        StripeResponse response = stripe.charge(request);
        return response.isSuccess();
    }
}

// Client — only knows PaymentProcessor interface
PaymentProcessor processor = new StripeAdapter(new StripeAPI());
processor.processPayment(100.0, "USD");
```

**When you need it:**
- Integrating a third-party library with your own interfaces
- Making an old class work in a new system
- Wrapping external APIs (payment gateways, SMS providers, map services)

**The smell that tells you to use it:** "I have a class that does exactly what I want, but its interface doesn't match what my code expects."

---

### 4.2 Decorator

**Problem it solves:** You want to add behaviour to an object at runtime, without modifying the class and without creating a subclass explosion.

**The core idea:** Wrap the object in a decorator that has the same interface. The decorator adds behaviour before or after delegating to the wrapped object. Decorators can be stacked.

**Bad — subclass explosion:**
```java
class Coffee { }
class CoffeeWithMilk extends Coffee { }
class CoffeeWithSugar extends Coffee { }
class CoffeeWithMilkAndSugar extends Coffee { }
class CoffeeWithVanillaAndMilkAndSugar extends Coffee { }
// 2^N combinations for N add-ons = unmaintainable
```

**Good — Decorator:**
```java
interface Coffee {
    String getDescription();
    double getCost();
}

class SimpleCoffee implements Coffee {
    public String getDescription() { return "Coffee"; }
    public double getCost() { return 30.0; }
}

abstract class CoffeeDecorator implements Coffee {
    protected final Coffee wrapped;
    public CoffeeDecorator(Coffee coffee) { this.wrapped = coffee; }
}

class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) { super(coffee); }
    public String getDescription() { return wrapped.getDescription() + ", Milk"; }
    public double getCost() { return wrapped.getCost() + 10.0; }
}

class SugarDecorator extends CoffeeDecorator {
    public SugarDecorator(Coffee coffee) { super(coffee); }
    public String getDescription() { return wrapped.getDescription() + ", Sugar"; }
    public double getCost() { return wrapped.getCost() + 5.0; }
}

class VanillaDecorator extends CoffeeDecorator {
    public VanillaDecorator(Coffee coffee) { super(coffee); }
    public String getDescription() { return wrapped.getDescription() + ", Vanilla"; }
    public double getCost() { return wrapped.getCost() + 20.0; }
}

// Stack decorators — any combination, any order
Coffee order = new VanillaDecorator(new MilkDecorator(new SugarDecorator(new SimpleCoffee())));
System.out.println(order.getDescription()); // Coffee, Sugar, Milk, Vanilla
System.out.println(order.getCost());        // 65.0
```

**Real-world example — Java I/O:**
```java
InputStream raw    = new FileInputStream("file.txt");
InputStream buffed = new BufferedInputStream(raw);      // adds buffering
InputStream zipped = new GZIPInputStream(buffed);       // adds decompression
// Stacked decorators — each wraps the previous
```

**When you need it:**
- Adding optional features to an object at runtime
- Avoiding subclass explosion for combinations of features
- Java I/O streams, middleware pipelines, HTTP request/response filters, logging wrappers

**Decorator vs Inheritance:** Inheritance adds behaviour at compile time to all instances. Decorator adds behaviour at runtime to specific instances. Decorator is more flexible.

**The smell that tells you to use it:** "I'm creating subclasses for every combination of features (WithMilk, WithSugar, WithMilkAndSugar)."

---

### 4.3 Facade

**Problem it solves:** A subsystem has many complex components. You want a simple, unified interface that hides that complexity from clients.

**The core idea:** Create a single class that coordinates the subsystem's components and exposes only the operations clients actually need.

**Real-world analogy:** A hotel concierge. You say "I need a taxi and a restaurant table." The concierge coordinates the taxi service, restaurant bookings, and timing. You don't call each service yourself.

**Example — home theatre system:**
```java
// Complex subsystem
class Amplifier { public void on() {...} public void setVolume(int v) {...} }
class DVDPlayer { public void on() {...} public void play(String movie) {...} }
class Projector { public void on() {...} public void setInput(String src) {...} }
class Lights { public void dim(int level) {...} }

// Facade — one method hides all the coordination
class HomeTheatreFacade {
    private Amplifier amp;
    private DVDPlayer dvd;
    private Projector projector;
    private Lights lights;

    public HomeTheatreFacade(Amplifier amp, DVDPlayer dvd, Projector projector, Lights lights) {
        this.amp = amp; this.dvd = dvd; this.projector = projector; this.lights = lights;
    }

    public void watchMovie(String movie) {
        lights.dim(10);
        projector.on();
        projector.setInput("DVD");
        amp.on();
        amp.setVolume(50);
        dvd.on();
        dvd.play(movie);
    }

    public void endMovie() {
        // reverse the sequence
    }
}

// Client — one call instead of eight
HomeTheatreFacade theatre = new HomeTheatreFacade(amp, dvd, projector, lights);
theatre.watchMovie("Inception");
```

**In LLD problems:**
- `BookingFacade` that coordinates `SeatService`, `PaymentService`, `NotificationService`, `TicketService`
- `OrderFacade` in e-commerce that coordinates inventory, payment, shipping, notification

**When you need it:**
- You have a complex subsystem with many interacting components
- You want to provide a simple entry point without restricting access to the full subsystem
- Onboarding new developers (they use the facade, learn internals later)

**The smell that tells you to use it:** "The client has to call 6 different services in the right order to complete one operation."

---

### 4.4 Composite

**Problem it solves:** You have a tree structure of objects and you want to treat individual objects and groups of objects uniformly.

**The core idea:** Both leaf objects and container objects implement the same interface. Containers hold children (which can be leaves or more containers). The client treats both the same way.

**Example — file system:**
```java
interface FileSystemItem {
    String getName();
    long getSize();
    void display(String indent);
}

class File implements FileSystemItem {
    private String name;
    private long size;

    public File(String name, long size) { this.name = name; this.size = size; }
    public String getName() { return name; }
    public long getSize() { return size; }
    public void display(String indent) { System.out.println(indent + name + " (" + size + " bytes)"); }
}

class Directory implements FileSystemItem {
    private String name;
    private List<FileSystemItem> children = new ArrayList<>();

    public Directory(String name) { this.name = name; }
    public void add(FileSystemItem item) { children.add(item); }

    public String getName() { return name; }
    public long getSize() {
        return children.stream().mapToLong(FileSystemItem::getSize).sum();  // recursive
    }
    public void display(String indent) {
        System.out.println(indent + name + "/");
        children.forEach(c -> c.display(indent + "  "));
    }
}

// Usage — uniform treatment
Directory root = new Directory("root");
Directory src = new Directory("src");
src.add(new File("Main.java", 2000));
src.add(new File("Utils.java", 1500));
root.add(src);
root.add(new File("README.md", 500));

root.display("");  // recursively displays entire tree
root.getSize();    // 4000 — recursive sum
```

**In LLD problems:**
- Discount system where a `CompositeDiscount` applies multiple discounts
- Organisation hierarchy where `Manager` contains `Employee` objects
- Menu items where a `SubMenu` contains `MenuItem` objects

**When you need it:**
- Tree structures — file systems, org charts, UI component hierarchies, menu systems
- You want to treat individual items and groups uniformly

---

### 4.5 Proxy

**Problem it solves:** You need to control access to an object — add lazy initialisation, access control, caching, logging, or remote access — without changing the object.

**The core idea:** A proxy implements the same interface as the real subject. Clients talk to the proxy, which controls access to the real object.

**Three main types:**

**Virtual Proxy — lazy initialisation:**
```java
interface Image {
    void display();
}

class RealImage implements Image {
    private String filename;

    public RealImage(String filename) {
        this.filename = filename;
        loadFromDisk();  // expensive — takes time
    }

    private void loadFromDisk() { System.out.println("Loading " + filename); }
    public void display() { System.out.println("Displaying " + filename); }
}

class ProxyImage implements Image {
    private String filename;
    private RealImage real;  // null until first display()

    public ProxyImage(String filename) { this.filename = filename; }

    public void display() {
        if (real == null) {
            real = new RealImage(filename);  // load only when first needed
        }
        real.display();
    }
}
```

**Protection Proxy — access control:**
```java
class AdminProxyService implements Service {
    private RealService real;
    private User currentUser;

    public AdminProxyService(RealService real, User user) {
        this.real = real;
        this.currentUser = user;
    }

    public void deleteRecord(int id) {
        if (!currentUser.hasRole("ADMIN")) {
            throw new SecurityException("Admin access required");
        }
        real.deleteRecord(id);
    }
}
```

**Caching Proxy:**
```java
class CachingWeatherService implements WeatherService {
    private WeatherService real;
    private Map<String, WeatherData> cache = new HashMap<>();

    public WeatherData getWeather(String city) {
        return cache.computeIfAbsent(city, real::getWeather);
    }
}
```

**When you need it:**
- Lazy loading of expensive resources
- Access control / authorisation checks
- Caching results of expensive operations
- Logging and auditing without modifying the real class
- Remote proxies (hiding network calls behind a local interface)

**The smell that tells you to use it:** "I want to add access control / caching / logging to this class but don't want to modify it."

---

### 4.6 Bridge

**Problem it solves:** You have two dimensions of variation — abstraction and implementation — and you want them to vary independently without a 2D class explosion.

**Example — shapes and rendering:**
```java
// Without Bridge: ShapeX × RendererY = N×M classes
// RedCircle, BlueCircle, RedSquare, BlueSquare...

// With Bridge: N + M classes
interface Renderer {
    void renderCircle(int radius);
    void renderSquare(int side);
}
class VectorRenderer implements Renderer { ... }
class RasterRenderer implements Renderer { ... }

abstract class Shape {
    protected Renderer renderer;  // the "bridge" — composition over inheritance
    public Shape(Renderer renderer) { this.renderer = renderer; }
    public abstract void draw();
}

class Circle extends Shape {
    private int radius;
    public Circle(int radius, Renderer renderer) { super(renderer); this.radius = radius; }
    public void draw() { renderer.renderCircle(radius); }
}

class Square extends Shape {
    private int side;
    public Square(int side, Renderer renderer) { super(renderer); this.side = side; }
    public void draw() { renderer.renderSquare(side); }
}

// Any shape × any renderer — independent variation
Shape c = new Circle(5, new VectorRenderer());
Shape s = new Square(3, new RasterRenderer());
```

**When you need it:** Two independent hierarchies that need to work together. Adding a new shape doesn't require new renderers. Adding a new renderer doesn't require new shapes.

---

## 5. Behavioural Patterns

### 5.1 Strategy

**Problem it solves:** You have multiple algorithms for doing the same thing and want to swap them at runtime without changing the class that uses them.

**The core idea:** Define a family of algorithms, encapsulate each one, and make them interchangeable. The class that uses the algorithm holds a reference to the strategy interface.

**Bad — algorithm hardwired with conditionals:**
```java
class FeeCalculator {
    public double calculate(String vehicleType, int hours) {
        if (vehicleType.equals("CAR")) return hours * 50;
        else if (vehicleType.equals("BIKE")) return hours * 20;
        else if (vehicleType.equals("TRUCK")) return hours * 100;
        // Adding electric vehicle? Open and modify this. OCP violation.
        return 0;
    }
}
```

**Good — Strategy:**
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

class DynamicSurgeFeeStrategy implements FeeStrategy {
    private double surgeMultiplier;
    public DynamicSurgeFeeStrategy(double multiplier) { this.surgeMultiplier = multiplier; }
    public double calculate(int hours) { return hours * 50 * surgeMultiplier; }
}

class FeeCalculator {
    private FeeStrategy strategy;

    public FeeCalculator(FeeStrategy strategy) { this.strategy = strategy; }

    public void setStrategy(FeeStrategy strategy) { this.strategy = strategy; }  // swap at runtime

    public double calculate(int hours) { return strategy.calculate(hours); }
}

// Usage
FeeCalculator calc = new FeeCalculator(new CarFeeStrategy());
calc.calculate(3);   // 150

calc.setStrategy(new DynamicSurgeFeeStrategy(1.5));
calc.calculate(3);   // 225 — surge pricing
```

**In LLD problems — everywhere:**
- Fee calculation by vehicle type (Parking Lot)
- Payment method (Credit Card, UPI, Wallet)
- Sorting strategy (sort rides by distance, rating, or price)
- Discount strategy (flat, percentage, BOGO)
- Split strategy in Splitwise (equal, exact, percentage)

**Strategy vs Template Method:**
- Strategy: the algorithm is entirely replaceable. Different classes, swapped via composition.
- Template Method: the skeleton stays fixed, subclasses fill in specific steps. Inheritance-based.

**The smell that tells you to use it:** "I have an if-else / switch block that selects a behaviour, and adding a new behaviour means editing this block."

---

### 5.2 Observer

**Problem it solves:** One object changes state and you need to notify a variable number of dependent objects — without tightly coupling the publisher to its subscribers.

**The core idea:** The Subject maintains a list of Observers. When state changes, it notifies all of them. Observers register and deregister dynamically. Subject doesn't know what the observers do with the notification.

**Example — stock price alert system:**
```java
interface Observer {
    void update(String stockSymbol, double price);
}

interface Subject {
    void subscribe(Observer observer);
    void unsubscribe(Observer observer);
    void notifyObservers();
}

class StockMarket implements Subject {
    private Map<String, Double> prices = new HashMap<>();
    private List<Observer> observers = new ArrayList<>();

    public void subscribe(Observer o) { observers.add(o); }
    public void unsubscribe(Observer o) { observers.remove(o); }

    public void updatePrice(String symbol, double price) {
        prices.put(symbol, price);
        notifyObservers(symbol, price);
    }

    private void notifyObservers(String symbol, double price) {
        for (Observer o : observers) {
            o.update(symbol, price);
        }
    }

    public void notifyObservers() {}  // interface compliance
}

class EmailAlertObserver implements Observer {
    private String email;
    public EmailAlertObserver(String email) { this.email = email; }
    public void update(String symbol, double price) {
        System.out.println("Email to " + email + ": " + symbol + " is now " + price);
    }
}

class MobileAlertObserver implements Observer {
    public void update(String symbol, double price) {
        System.out.println("Push notification: " + symbol + " → " + price);
    }
}

// Usage
StockMarket market = new StockMarket();
market.subscribe(new EmailAlertObserver("trader@gmail.com"));
market.subscribe(new MobileAlertObserver());

market.updatePrice("INFY", 1450.50);
// Both observers notified — market doesn't know or care what they do
```

**In LLD problems:**
- Notification system (user follows driver, gets updates)
- Event system in Ride Sharing (ride status updates push to rider and driver)
- Order status updates in Food Delivery
- Inventory alerts when stock goes below threshold
- Undo system — views observing model changes

**Observer vs Event Bus:**
- Observer: direct coupling — subject holds references to observers
- Event Bus: decoupled — publisher and subscriber don't know about each other, communicate via event types

**The smell that tells you to use it:** "When X happens, I need to trigger actions in Y, Z, and W — and the list might grow."

---

### 5.3 Command

**Problem it solves:** You want to encapsulate a request as an object — supporting undo/redo, request queuing, logging, and transactional operations.

**The core idea:** Every operation is wrapped in a Command object with an `execute()` method (and optionally `undo()`). The invoker holds commands and calls them. The receiver does the actual work.

**Example — text editor with undo:**
```java
interface Command {
    void execute();
    void undo();
}

class TextEditor {
    private StringBuilder text = new StringBuilder();

    public void insertText(String s) { text.append(s); }
    public void deleteText(int length) { text.delete(text.length() - length, text.length()); }
    public String getText() { return text.toString(); }
}

class InsertTextCommand implements Command {
    private TextEditor editor;
    private String text;

    public InsertTextCommand(TextEditor editor, String text) {
        this.editor = editor; this.text = text;
    }

    public void execute() { editor.insertText(text); }
    public void undo() { editor.deleteText(text.length()); }
}

// Invoker — manages command history
class CommandManager {
    private Deque<Command> history = new ArrayDeque<>();

    public void executeCommand(Command cmd) {
        cmd.execute();
        history.push(cmd);
    }

    public void undo() {
        if (!history.isEmpty()) {
            history.pop().undo();
        }
    }
}

// Usage
TextEditor editor = new TextEditor();
CommandManager manager = new CommandManager();

manager.executeCommand(new InsertTextCommand(editor, "Hello"));
manager.executeCommand(new InsertTextCommand(editor, " World"));
System.out.println(editor.getText());  // Hello World

manager.undo();
System.out.println(editor.getText());  // Hello
```

**In LLD problems:**
- Smart home — light on/off commands, queued and undoable
- Job queue — commands submitted and processed asynchronously
- Transaction management — execute, then rollback on failure
- Macro recording — sequence of commands replayed

**The smell that tells you to use it:** "I need undo/redo", "I need to queue requests for later", "I need to log every operation as a first-class object."

---

### 5.4 State

**Problem it solves:** An object's behaviour changes dramatically based on its internal state. You want to avoid massive if-else / switch blocks that check state.

**The core idea:** Represent each state as a separate class. The context delegates its behaviour to the current state object. Transitions are handled by the state classes themselves.

**Example — ATM machine:**
```java
interface ATMState {
    void insertCard(ATMContext atm);
    void enterPIN(ATMContext atm, String pin);
    void withdrawCash(ATMContext atm, double amount);
    void ejectCard(ATMContext atm);
}

class IdleState implements ATMState {
    public void insertCard(ATMContext atm) {
        System.out.println("Card inserted");
        atm.setState(new CardInsertedState());
    }
    public void enterPIN(ATMContext atm, String pin) { System.out.println("Insert card first"); }
    public void withdrawCash(ATMContext atm, double amount) { System.out.println("Insert card first"); }
    public void ejectCard(ATMContext atm) { System.out.println("No card to eject"); }
}

class CardInsertedState implements ATMState {
    public void insertCard(ATMContext atm) { System.out.println("Card already inserted"); }
    public void enterPIN(ATMContext atm, String pin) {
        if (pin.equals("1234")) {
            System.out.println("PIN correct");
            atm.setState(new AuthenticatedState());
        } else {
            System.out.println("Wrong PIN");
        }
    }
    public void withdrawCash(ATMContext atm, double amount) { System.out.println("Enter PIN first"); }
    public void ejectCard(ATMContext atm) {
        System.out.println("Card ejected");
        atm.setState(new IdleState());
    }
}

class AuthenticatedState implements ATMState {
    public void insertCard(ATMContext atm) { System.out.println("Card already in"); }
    public void enterPIN(ATMContext atm, String pin) { System.out.println("Already authenticated"); }
    public void withdrawCash(ATMContext atm, double amount) {
        System.out.println("Dispensing: " + amount);
        atm.setState(new IdleState());
    }
    public void ejectCard(ATMContext atm) {
        System.out.println("Card ejected");
        atm.setState(new IdleState());
    }
}

class ATMContext {
    private ATMState currentState = new IdleState();

    public void setState(ATMState state) { this.currentState = state; }

    public void insertCard() { currentState.insertCard(this); }
    public void enterPIN(String pin) { currentState.enterPIN(this, pin); }
    public void withdrawCash(double amount) { currentState.withdrawCash(this, amount); }
    public void ejectCard() { currentState.ejectCard(this); }
}
```

**In LLD problems:**
- ATM (Idle → CardInserted → Authenticated → Dispensing)
- Vending Machine (Idle → HasMoney → ItemSelected → Dispensing)
- Order (Placed → Confirmed → Shipped → Delivered → Cancelled)
- Traffic light (Red → Green → Yellow)
- Ride (Requested → DriverAssigned → Ongoing → Completed)

**State vs Strategy:**
- Strategy: algorithm varies but state doesn't drive behaviour
- State: the object's own state drives which behaviour runs, and states transition to each other

**The smell that tells you to use it:** "My class has a huge switch/if-else on a `status` or `state` field, and each branch does completely different things."

---

### 5.5 Template Method

**Problem it solves:** You have an algorithm with a fixed skeleton but some steps need to be customised by subclasses.

**The core idea:** The parent class defines the algorithm's skeleton in a template method. Specific steps are defined as abstract methods that subclasses override. The overall sequence never changes.

**Example — data export pipeline:**
```java
abstract class DataExporter {
    // Template method — fixed skeleton, final so subclasses cannot reorder steps
    public final void export(String destination) {
        List<Object> data = fetchData();       // step 1 — fixed
        List<Object> parsed = parseData(data); // step 2 — customisable
        validate(parsed);                      // step 3 — fixed
        writeToDestination(parsed, destination); // step 4 — customisable
        sendNotification(destination);         // step 5 — fixed
    }

    protected abstract List<Object> parseData(List<Object> raw);
    protected abstract void writeToDestination(List<Object> data, String dest);

    private List<Object> fetchData() { /* always the same */ return new ArrayList<>(); }
    private void validate(List<Object> data) { /* always the same */ }
    private void sendNotification(String dest) { System.out.println("Export complete: " + dest); }
}

class CSVExporter extends DataExporter {
    protected List<Object> parseData(List<Object> raw) { /* CSV parsing */ return raw; }
    protected void writeToDestination(List<Object> data, String dest) { /* write CSV */ }
}

class PDFExporter extends DataExporter {
    protected List<Object> parseData(List<Object> raw) { /* PDF formatting */ return raw; }
    protected void writeToDestination(List<Object> data, String dest) { /* write PDF */ }
}
```

**In LLD problems:**
- Order processing pipeline where steps are fixed but payment and notification differ
- Game loop where setup/teardown are fixed but each game implements its own moves
- Report generation where fetching and sending are fixed but formatting varies

**The smell that tells you to use it:** "Multiple classes do the same thing in the same order, but differ in a few specific steps."

---

### 5.6 Chain of Responsibility

**Problem it solves:** You have a request that might be handled by one of many handlers, but you don't want the sender to know which one will handle it. Handlers are chained — each either handles the request or passes it to the next.

**Example — customer support escalation:**
```java
abstract class SupportHandler {
    protected SupportHandler next;

    public SupportHandler setNext(SupportHandler next) {
        this.next = next;
        return next;  // allows chaining: h1.setNext(h2).setNext(h3)
    }

    public abstract void handle(SupportTicket ticket);
}

class Level1Support extends SupportHandler {
    public void handle(SupportTicket ticket) {
        if (ticket.getPriority() == Priority.LOW) {
            System.out.println("L1 handles: " + ticket.getIssue());
        } else if (next != null) {
            next.handle(ticket);
        }
    }
}

class Level2Support extends SupportHandler {
    public void handle(SupportTicket ticket) {
        if (ticket.getPriority() == Priority.MEDIUM) {
            System.out.println("L2 handles: " + ticket.getIssue());
        } else if (next != null) {
            next.handle(ticket);
        }
    }
}

class Level3Support extends SupportHandler {
    public void handle(SupportTicket ticket) {
        System.out.println("L3 (engineering) handles: " + ticket.getIssue());
    }
}

// Setup the chain
SupportHandler l1 = new Level1Support();
SupportHandler l2 = new Level2Support();
SupportHandler l3 = new Level3Support();
l1.setNext(l2).setNext(l3);

// All requests go through l1 — it routes them
l1.handle(new SupportTicket(Priority.HIGH, "System down"));
```

**In LLD problems:**
- Logger — Debug → Info → Warn → Error handlers
- HTTP middleware — authentication → rate limiting → logging → business logic
- Discount chain — check loyalty discount → check seasonal → check coupon
- ATM cash dispenser — try 500 note handler → 200 → 100 → 50

**The smell that tells you to use it:** "Multiple handlers could process a request. I want to try them in order and stop at the first one that handles it."

---

### 5.7 Iterator

**Problem it solves:** Provide a way to sequentially access elements of a collection without exposing its internal representation.

```java
interface Iterator<T> {
    boolean hasNext();
    T next();
}

class BookShelf {
    private List<Book> books = new ArrayList<>();
    public void addBook(Book b) { books.add(b); }

    public Iterator<Book> createIterator() {
        return new BookShelfIterator(books);
    }

    private class BookShelfIterator implements Iterator<Book> {
        private List<Book> books;
        private int index = 0;

        BookShelfIterator(List<Book> books) { this.books = books; }

        public boolean hasNext() { return index < books.size(); }
        public Book next() { return books.get(index++); }
    }
}
```

Java's `Iterable` and `Iterator` interfaces are built on this pattern. You use it every time you write a for-each loop on a collection.

---

## 6. Pattern Combinations in Real LLD Problems

Real systems use multiple patterns together. Here's how they combine:

### Parking Lot
| Decision | Pattern |
|---|---|
| Only one ParkingLot instance | Singleton |
| Create Car, Bike, Truck from type string | Factory Method |
| Fee calculation varies by vehicle type | Strategy |
| Notify subscribers when slot freed | Observer |
| ParkingLot → Floor → Slot hierarchy | Composite |

### Ride Sharing (Uber)
| Decision | Pattern |
|---|---|
| Match rider to driver | Strategy (matching algorithm) |
| Notify rider/driver of status changes | Observer |
| Surge pricing vs flat pricing | Strategy |
| Ride goes through states | State (Requested → Assigned → Ongoing → Completed) |
| Create different ride types (Pool, Premium) | Factory Method |

### Food Delivery
| Decision | Pattern |
|---|---|
| Order pipeline (validate → pay → dispatch) | Template Method |
| Notify restaurant, delivery agent, customer | Observer |
| Payment methods | Strategy |
| Order status transitions | State |
| Wrap delivery with premium packaging feature | Decorator |

### Notification System
| Decision | Pattern |
|---|---|
| Create Email/SMS/Push based on preference | Factory Method |
| Add retry, logging to any channel | Decorator |
| One interface for all channel types | Adapter (for third-party) |
| Wrap complex channel setup | Facade |

---

## 7. Pattern vs Pattern — When They Seem Similar

These pairs are commonly confused in interviews:

### Strategy vs State
| | Strategy | State |
|---|---|---|
| Purpose | Swap algorithms | Represent object states |
| Who changes behaviour | Client passes different strategy | Object transitions itself |
| States know each other? | No | Yes — transitions |
| Example | Fee calculation algorithm | ATM states |

### Factory Method vs Abstract Factory
| | Factory Method | Abstract Factory |
|---|---|---|
| Creates | One type of object | A family of related objects |
| Methods | One factory method | Multiple factory methods |
| Use when | One variable type | Multiple related variable types |

### Decorator vs Proxy
| | Decorator | Proxy |
|---|---|---|
| Purpose | Add behaviour | Control access |
| Wrapping | Client chooses wrapping | Proxy created transparently |
| Example | Coffee add-ons | Caching, auth checks |

### Observer vs Command
| | Observer | Command |
|---|---|---|
| Purpose | Broadcast state changes | Encapsulate a request |
| Directionality | One-to-many notification | One-to-one execution |
| Undo support | No | Yes |
| Example | Price alerts | Text editor operations |

### Adapter vs Facade
| | Adapter | Facade |
|---|---|---|
| Purpose | Interface translation | Simplification |
| Classes involved | Two incompatible interfaces | Complex subsystem |
| Example | Stripe wrapping | Home theatre control |

---

## 8. How to Spot Which Pattern to Use

Use this decision guide in interviews when you're not sure which pattern applies:

```
Need to control object creation?
  → Only one instance ever          → Singleton
  → Create based on type/input      → Factory Method
  → Create families of objects      → Abstract Factory
  → Complex construction, many optional params → Builder
  → Clone existing expensive object → Prototype

Need to compose objects?
  → Translate interface              → Adapter
  → Add behaviour at runtime        → Decorator
  → Simplify a complex subsystem    → Facade
  → Uniform treatment of tree items → Composite
  → Control access to an object     → Proxy
  → Two independent varying hierarchies → Bridge

Need to define object communication?
  → Swap algorithms at runtime      → Strategy
  → One-to-many event notification  → Observer
  → Encapsulate request with undo   → Command
  → Behaviour driven by own state   → State
  → Fixed algorithm, variable steps → Template Method
  → Pass request through chain      → Chain of Responsibility
```

---

## 9. Interview Question Bank

**Conceptual:**
- What is the difference between Strategy and State pattern?
- When would you use Decorator over Inheritance?
- What problem does the Observer pattern solve?
- Explain the difference between Factory Method and Abstract Factory.
- What is the difference between Adapter and Facade?
- When would you use Builder over a constructor?
- What is a Proxy and what are its three main uses?

**Design questions (most common at SDE-2):**
- Design a notification system that supports Email, SMS, and Push — which patterns apply?
- Design a parking lot — how does Strategy apply for fee calculation?
- Design an ATM — which pattern models the state transitions?
- Design a logging framework — which patterns would you use?
- Design a coffee shop billing system — how would you use Decorator?
- How would you design a plugin architecture? (Factory + Strategy)
- Design an undo/redo system for a text editor.

---

## 10. Quick Reference Card

### Creational — how objects are born

```
Singleton       → one instance, shared globally (Logger, Config, Pool)
Factory Method  → create one type by input (NotificationFactory)
Abstract Factory→ create a family of related objects (UI theme factories)
Builder         → complex object, many optional params (Pizza, HttpRequest)
Prototype       → clone expensive objects
```

### Structural — how objects are assembled

```
Adapter         → translate incompatible interfaces (Stripe wrapper)
Decorator       → add behaviour at runtime, stackable (Coffee add-ons, Java I/O)
Facade          → simplify a subsystem (HomeTheatre, BookingFacade)
Composite       → uniform tree treatment (FileSystem, OrgChart)
Proxy           → control access (lazy load, cache, auth)
Bridge          → two independent hierarchies (Shape × Renderer)
```

### Behavioural — how objects talk

```
Strategy        → swap algorithms (FeeCalculator, PaymentMethod, SplitStrategy)
Observer        → one-to-many notification (StockAlerts, OrderStatus)
Command         → encapsulate request, enable undo (TextEditor, JobQueue)
State           → behaviour driven by state (ATM, Vending, Order, Ride)
Template Method → fixed skeleton, variable steps (DataExporter, GameLoop)
Chain of Resp.  → pass request through handlers (Logger, Middleware, Discounts)
Iterator        → traverse without exposing internals (for-each)
```

### Pattern confusion quick resolve

```
Strategy vs State           → Strategy: client swaps. State: object transitions itself.
Factory vs Abstract Factory → Factory: one type. Abstract Factory: a family.
Decorator vs Proxy          → Decorator: add behaviour. Proxy: control access.
Adapter vs Facade           → Adapter: translate. Facade: simplify.
Observer vs Command         → Observer: broadcast. Command: encapsulate + undo.
```

---

## 11. Revision Checklist

Answer these without re-reading the file first:

- [ ] Name all five creational patterns and the problem each solves.
- [ ] Name all six structural patterns. Give a one-line use case for each.
- [ ] Name all seven behavioural patterns covered here.
- [ ] Explain Strategy with the Parking Lot fee calculation example.
- [ ] Draw the Observer pattern — Subject, Observer interface, and two concrete observers.
- [ ] Why would you use Builder over a constructor with 6 parameters?
- [ ] What is the difference between Composite and Decorator?
- [ ] Design the Notification system (Email, SMS, Push) using Factory Method. How do you add WhatsApp without modifying existing code?
- [ ] How does State pattern model ATM transitions? Name the states.
- [ ] What is the difference between Strategy and State? Give an example of each.
- [ ] Where would you use Chain of Responsibility in a food delivery system?
- [ ] Why is `volatile` necessary in double-checked locking Singleton?
- [ ] When does Decorator become better than Inheritance? Give the coffee example.
- [ ] What three things can a Proxy do? Give one LLD example for each.
- [ ] In a Ride Sharing system, list every pattern you would apply and where.

---

## Key Takeaways

1. **Patterns are design vocabulary.** They let you communicate intent clearly — "I'll use Strategy here" conveys more than "I'll use an if-else."
2. **Strategy enables OCP.** Add new algorithms by adding classes, not editing existing ones.
3. **Observer decouples publishers from subscribers.** The Subject never knows what its Observers do.
4. **State eliminates if-else on status fields.** Each state is a class. Transitions are explicit.
5. **Decorator beats subclass explosion.** N features = N decorators, not 2^N subclasses.
6. **Builder beats telescoping constructors.** More than 4 parameters, especially optional ones, need a Builder.
7. **Factory Method centralises object creation.** One place to change when you add a new type.
8. **Proxy adds a layer of indirection transparently.** The client doesn't know it's talking to a proxy.
9. **Real systems combine patterns.** Parking Lot uses Singleton + Factory + Strategy + Observer together.
10. **The smell is your signal.** If-else on type → Strategy. If-else on state → State. Complex construction → Builder. One-to-many event → Observer.
