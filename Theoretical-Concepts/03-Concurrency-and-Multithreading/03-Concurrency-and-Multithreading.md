# 03 — Concurrency and Multithreading

> **Purpose of this file:** Understand everything about concurrency that shows up in SDE-2 interviews — thread safety, synchronisation, locks, common patterns, and how to apply them in LLD problems. Every concept has a bad example, a good example, and a signal for when you need it.

---

## Table of Contents

1. [Why Concurrency Matters for SDE-2](#1-why-concurrency-matters-for-sde-2)
2. [Core Concepts — The Mental Model](#2-core-concepts--the-mental-model)
3. [Thread Basics](#3-thread-basics)
4. [Race Conditions and Data Races](#4-race-conditions-and-data-races)
5. [Synchronisation — The `synchronized` Keyword](#5-synchronisation--the-synchronized-keyword)
6. [Locks — `ReentrantLock` and Friends](#6-locks--reentrantlock-and-friends)
7. [Volatile](#7-volatile)
8. [Atomic Classes](#8-atomic-classes)
9. [The Java Memory Model (JMM) — Just Enough](#9-the-java-memory-model-jmm--just-enough)
10. [Deadlock, Livelock, and Starvation](#10-deadlock-livelock-and-starvation)
11. [Thread-Safe Collections](#11-thread-safe-collections)
12. [Executor Framework and Thread Pools](#12-executor-framework-and-thread-pools)
13. [Classic Concurrency Problems](#13-classic-concurrency-problems)
14. [Concurrency in LLD Problems](#14-concurrency-in-lld-problems)
15. [Design Patterns for Concurrency](#15-design-patterns-for-concurrency)
16. [Interview Question Bank](#16-interview-question-bank)
17. [Quick Reference Card](#17-quick-reference-card)
18. [Revision Checklist](#18-revision-checklist)

---

## 1. Why Concurrency Matters for SDE-2

At SDE-1 level, you write code that works. At SDE-2, you write code that works **under load, with multiple users hitting it simultaneously.**

Almost every LLD system you design has concurrency implications:

- Two users booking the last seat in a movie theatre at the same time
- Two ATM transactions on the same account simultaneously
- Multiple threads updating a shared cache
- Ride-sharing matching a driver to two riders at once

If you design the Parking Lot with no thread safety, two cars could get the same slot. If you design the bank account with no synchronisation, a deposit and withdrawal running simultaneously could corrupt the balance.

**What interviewers test at SDE-2:**
- Do you recognise when shared state is a problem?
- Do you know the tools: `synchronized`, `Lock`, `volatile`, `AtomicInteger`, concurrent collections?
- Can you reason about deadlocks and prevent them?
- Do you know the Executor framework?
- Can you apply thread safety to an LLD design without making it a bottleneck?

---

## 2. Core Concepts — The Mental Model

Before any code, understand this mental model. It explains everything else.

### Shared mutable state is the root of all concurrency problems

Concurrency problems happen when:
1. Multiple threads access the **same data** (shared)
2. At least one thread **writes** to it (mutable)
3. The access is **not coordinated** (no synchronisation)

Remove any one of these three and the problem disappears:
- No sharing → thread-local data, no problem
- No mutation → immutable objects, no problem
- No coordination issue → proper synchronisation, no problem

### The three things that can go wrong

| Problem | What it means |
|---|---|
| **Visibility** | Thread A writes a value. Thread B reads stale data because it cached the old value in its CPU register. |
| **Atomicity** | An operation looks like one step but is actually multiple steps. Another thread can sneak in between. |
| **Ordering** | The compiler or CPU reorders instructions for performance. Thread B sees operations in a different order than Thread A executed them. |

Every concurrency tool — `synchronized`, `volatile`, `AtomicInteger`, `Lock` — exists to fix one or more of these three problems.

---

## 3. Thread Basics

### Creating a thread — three ways

```java
// Way 1: Extend Thread
class MyThread extends Thread {
    public void run() {
        System.out.println("Running in: " + Thread.currentThread().getName());
    }
}
new MyThread().start();

// Way 2: Implement Runnable (preferred — keeps class hierarchy free)
Thread t = new Thread(() -> System.out.println("Lambda runnable"));
t.start();

// Way 3: Use ExecutorService (best for production — see section 12)
ExecutorService executor = Executors.newFixedThreadPool(4);
executor.submit(() -> System.out.println("Submitted to pool"));
```

**Always prefer Runnable over extending Thread.** Extending Thread couples your task to the threading mechanism. Runnable separates the task from how it runs.

### Thread lifecycle

```
NEW → RUNNABLE → RUNNING → BLOCKED/WAITING/TIMED_WAITING → TERMINATED
         ↑_____________________|  (becomes runnable again after unblocking)
```

- `start()` → NEW to RUNNABLE
- `run()` executing → RUNNING
- Waiting for a lock → BLOCKED
- Calling `wait()`, `join()`, `sleep()` → WAITING or TIMED_WAITING
- `run()` returns or exception → TERMINATED

### Key thread methods

| Method | What it does |
|---|---|
| `thread.start()` | Starts the thread — never call `run()` directly |
| `Thread.sleep(ms)` | Pause current thread for N milliseconds |
| `thread.join()` | Caller waits until `thread` finishes |
| `Thread.currentThread()` | Get reference to the currently executing thread |
| `thread.interrupt()` | Signal a thread to stop (polite request, not forced) |
| `Thread.yield()` | Hint to scheduler to give other threads a turn |

---

## 4. Race Conditions and Data Races

### What is a race condition?

A race condition is when the correctness of a program depends on the relative timing of operations in multiple threads — and that timing is not guaranteed.

**The classic example — incrementing a counter:**

```java
class Counter {
    private int count = 0;

    public void increment() {
        count++;  // THIS IS NOT ATOMIC
    }

    public int getCount() { return count; }
}
```

`count++` looks like one operation. It is actually three:
1. Read `count` from memory
2. Add 1
3. Write the result back

If two threads execute this simultaneously:

```
Thread A reads count = 5
Thread B reads count = 5      ← reads BEFORE A writes back
Thread A writes count = 6
Thread B writes count = 6     ← overwrites A's result — 7 was expected, 6 is the result
```

Run this 100,000 times with two threads and you get a final count well below 200,000. The lost updates are the race condition.

### How to detect a race condition

Ask: "If thread A is paused at any point in this method, can thread B see invalid state?"

If yes — you have a race condition.

---

## 5. Synchronisation — The `synchronized` Keyword

`synchronized` is Java's built-in lock mechanism. It fixes all three problems: visibility, atomicity, and ordering.

### How it works

Every Java object has an intrinsic lock (also called a monitor). `synchronized` acquires this lock before executing the block and releases it after. Only one thread can hold a lock at a time — all others wait.

### Synchronized method

```java
class Counter {
    private int count = 0;

    public synchronized void increment() {
        count++;  // now atomic — only one thread at a time
    }

    public synchronized int getCount() {
        return count;
    }
}
```

The lock here is `this` — the Counter instance.

### Synchronized block (preferred — locks for less time)

```java
class Counter {
    private int count = 0;
    private final Object lock = new Object();  // dedicated lock object

    public void increment() {
        // Only the critical section is locked — not the whole method
        synchronized (lock) {
            count++;
        }
        // other non-critical work here runs freely
    }
}
```

**Always prefer synchronized blocks over synchronized methods.** Locking the entire method holds the lock longer than necessary, reducing throughput.

### Synchronized on static method

```java
class Counter {
    private static int globalCount = 0;

    public static synchronized void incrementGlobal() {
        globalCount++;
    }
}
```

Static `synchronized` locks on the `Class` object, not an instance. All instances share this lock.

### What synchronized guarantees

1. **Mutual exclusion** — only one thread in the block at a time
2. **Visibility** — on exit, all writes are flushed to main memory. On entry, all reads are refreshed from main memory
3. **Happens-before** — everything before the lock release is visible to the next thread that acquires it

### What synchronized does NOT do

- It does not make an object's fields thread-safe on their own — you must synchronise every access (both read and write)
- It does not prevent deadlock — you can still deadlock with `synchronized`
- It does not give you try-with-timeout or interruptible locking — use `ReentrantLock` for those

---

## 6. Locks — `ReentrantLock` and Friends

`synchronized` is simple but limited. `java.util.concurrent.locks` gives you more control.

### ReentrantLock — the most common

```java
import java.util.concurrent.locks.ReentrantLock;

class Counter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();

    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();  // ALWAYS unlock in finally — never forget this
        }
    }
}
```

**Why `finally`?** If an exception is thrown inside the locked section, you must still release the lock. Without `finally`, the lock is never released and every subsequent thread waits forever.

### ReentrantLock advantages over synchronized

| Feature | `synchronized` | `ReentrantLock` |
|---|---|---|
| Try to acquire, give up if busy | No | `tryLock()` |
| Acquire with timeout | No | `tryLock(time, unit)` |
| Interruptible lock acquisition | No | `lockInterruptibly()` |
| Multiple condition variables | One (`wait/notify`) | Many (`newCondition()`) |
| Fairness (longest-waiting gets it) | No | `new ReentrantLock(true)` |
| See if lock is held | No | `isLocked()`, `isHeldByCurrentThread()` |

### tryLock — avoid blocking

```java
public void doWork() {
    if (lock.tryLock()) {
        try {
            // Got the lock — do work
        } finally {
            lock.unlock();
        }
    } else {
        // Could not acquire — do something else or retry later
    }
}
```

### ReentrantReadWriteLock — for read-heavy scenarios

When many threads read but writes are rare, a regular lock is wasteful — reads block each other for no reason.

```java
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Cache {
    private final Map<String, String> data = new HashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    public String get(String key) {
        readLock.lock();        // multiple threads can hold readLock simultaneously
        try {
            return data.get(key);
        } finally {
            readLock.unlock();
        }
    }

    public void put(String key, String value) {
        writeLock.lock();       // exclusive — no readers or writers while write is in progress
        try {
            data.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }
}
```

**Rules of ReadWriteLock:**
- Multiple threads can hold the read lock simultaneously (as long as no writer holds the write lock)
- The write lock is exclusive — one writer, no readers
- Use when reads outnumber writes significantly (caches, config stores, leaderboards)

---

## 7. Volatile

`volatile` fixes the **visibility** problem — nothing more, nothing less.

### The problem it solves

CPUs cache values in registers and L1/L2 cache for performance. Thread A writes a value, but Thread B's CPU hasn't refreshed its cache yet and reads stale data.

```java
class Stopper {
    private boolean stopped = false;  // NOT volatile — thread may see stale value

    public void stop() { stopped = true; }

    public void run() {
        while (!stopped) {  // this might loop forever — thread caches stopped=false
            // do work
        }
    }
}
```

### The fix

```java
class Stopper {
    private volatile boolean stopped = false;  // every read goes to main memory

    public void stop() { stopped = true; }

    public void run() {
        while (!stopped) {  // guaranteed to see the latest value
            // do work
        }
    }
}
```

### What volatile guarantees

- Every read of the variable goes to main memory (no CPU cache)
- Every write is immediately flushed to main memory
- Establishes a happens-before relationship

### What volatile does NOT guarantee

- **Atomicity.** `volatile int count; count++` is still not atomic. `volatile` fixes visibility, not the read-modify-write race. Use `AtomicInteger` for that.

### When to use volatile

- Single boolean flags (stop flags, initialisation flags)
- A variable written by one thread, read by many — and the operation is a single write (not read-modify-write)
- The `stopped` / `running` / `initialized` pattern

**Rule of thumb:** If the operation is a simple write (not `++` or `+=`), `volatile` may be enough. If it's read-modify-write, use `AtomicInteger` or `synchronized`.

---

## 8. Atomic Classes

`java.util.concurrent.atomic` — thread-safe wrappers for primitives and references using hardware-level compare-and-swap (CAS). No locks, no blocking. Fast.

### The most important ones

```java
AtomicInteger count = new AtomicInteger(0);

count.incrementAndGet();         // count++ atomically, returns new value
count.getAndIncrement();         // count++, returns old value
count.addAndGet(5);              // count += 5 atomically
count.compareAndSet(5, 10);      // if count == 5, set to 10. Returns true if successful.
count.get();                     // plain read
count.set(100);                  // plain write
```

```java
AtomicLong   totalRevenue = new AtomicLong(0);
AtomicBoolean initialized = new AtomicBoolean(false);

// AtomicReference — for any object
AtomicReference<String> current = new AtomicReference<>("initial");
current.compareAndSet("initial", "updated");
```

### compareAndSet — the key operation (CAS)

CAS is the foundation of non-blocking concurrency. It says: "Set the value to `newValue` **only if** the current value is `expectedValue`. If it's been changed by another thread, fail and let me retry."

```java
AtomicInteger counter = new AtomicInteger(0);

// Retry loop — the non-blocking increment pattern
int oldValue, newValue;
do {
    oldValue = counter.get();
    newValue = oldValue + 1;
} while (!counter.compareAndSet(oldValue, newValue));
// No lock. No blocking. Works correctly even under contention.
```

### When to use Atomic classes

| Situation | Use |
|---|---|
| Increment/decrement a counter | `AtomicInteger` / `AtomicLong` |
| Toggle a flag from one thread, read from many | `AtomicBoolean` |
| Safely update a shared object reference | `AtomicReference` |
| Need CAS semantics on a field | `AtomicReference` or `AtomicInteger` |

**Atomic classes vs synchronized:**
- Atomic classes are non-blocking — no thread ever waits. Better throughput under contention.
- `synchronized` is simpler for complex multi-step operations. Use it when you need to atomically do more than one thing.

---

## 9. The Java Memory Model (JMM) — Just Enough

You don't need to be a JMM expert at SDE-2, but you need the happens-before concept.

### Happens-before

If operation A *happens-before* operation B, then A's results are visible to B.

The JMM defines these happens-before guarantees:
- Everything before `lock.unlock()` happens-before everything after `lock.lock()` on the same lock
- A `volatile` write happens-before all subsequent reads of that variable
- `thread.start()` happens-before any action in the started thread
- All actions in a thread happen-before `thread.join()` returns
- Object constructor happens-before the object's `finalize()`

**Why this matters:** Without a happens-before relationship, there is no guarantee of visibility. A thread can see stale values. `synchronized` and `volatile` establish happens-before, which is why they fix visibility.

---

## 10. Deadlock, Livelock, and Starvation

### Deadlock

Two or more threads are each waiting for a lock held by the other. Neither can proceed. The system is stuck forever.

**Classic example:**
```java
Object lockA = new Object();
Object lockB = new Object();

// Thread 1
synchronized (lockA) {
    Thread.sleep(100);
    synchronized (lockB) {  // waits for lockB — held by Thread 2
        System.out.println("Thread 1 done");
    }
}

// Thread 2 (running simultaneously)
synchronized (lockB) {
    Thread.sleep(100);
    synchronized (lockA) {  // waits for lockA — held by Thread 1
        System.out.println("Thread 2 done");
    }
}
// Both threads wait forever — deadlock
```

### The four conditions for deadlock (Coffman conditions)

All four must be true simultaneously for deadlock to occur:

1. **Mutual exclusion** — at least one resource is non-shareable
2. **Hold and wait** — a thread holds one lock while waiting for another
3. **No preemption** — locks cannot be forcibly taken from a thread
4. **Circular wait** — Thread A waits for Thread B, Thread B waits for Thread A

### How to prevent deadlock

**Strategy 1: Consistent lock ordering**
Always acquire locks in the same order everywhere in the codebase.

```java
// Both threads acquire lockA before lockB — no circular wait possible
synchronized (lockA) {
    synchronized (lockB) {
        // safe
    }
}
```

**Strategy 2: tryLock with timeout**
```java
if (lockA.tryLock(500, TimeUnit.MILLISECONDS)) {
    try {
        if (lockB.tryLock(500, TimeUnit.MILLISECONDS)) {
            try {
                // do work
            } finally { lockB.unlock(); }
        }
    } finally { lockA.unlock(); }
} else {
    // could not acquire — back off and retry
}
```

**Strategy 3: Avoid nested locks**
Try not to hold one lock while acquiring another. Restructure the code to acquire and release locks one at a time.

**Strategy 4: Lock ordering by object ID**
When you need to lock two arbitrary objects and can't control order, sort by system identity hash code to impose a consistent order.

### Livelock

Threads are not blocked — they are actively running — but they keep responding to each other and making no progress. Like two people in a corridor stepping aside to let the other pass but always mirroring each other.

```
Thread A: backs off when Thread B is trying
Thread B: backs off when Thread A is trying
Both back off simultaneously → both retry → both back off again → infinite
```

**Prevention:** Add randomised backoff (retry after a random delay) instead of immediate retry.

### Starvation

A thread is perpetually denied access to a resource because other threads always get priority. It never gets CPU time.

**Prevention:** Use fair locks — `new ReentrantLock(true)`. Threads acquire in the order they requested. Or use a thread pool with bounded queue sizes.

---

## 11. Thread-Safe Collections

Never use `HashMap`, `ArrayList`, or `HashSet` from multiple threads without synchronisation. Use these instead:

### ConcurrentHashMap — your default thread-safe map

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("key", 1);
map.get("key");
map.computeIfAbsent("key", k -> expensiveOperation());

// Atomic conditional operations
map.putIfAbsent("key", 100);       // only puts if key not present
map.replace("key", 1, 2);         // only replaces if current value == 1
```

**How it works:** Internally divided into segments (buckets). Only the relevant segment is locked on write — other segments remain accessible. Much better throughput than `Hashtable` (which locks the entire map).

**ConcurrentHashMap vs Hashtable vs `Collections.synchronizedMap`:**
- `Hashtable` — locks the entire map on every operation. Legacy. Do not use.
- `Collections.synchronizedMap(new HashMap<>())` — wraps a HashMap, locks every method. Slightly better than Hashtable but still coarse.
- `ConcurrentHashMap` — segment-level locking, lock-free reads. Use this.

### CopyOnWriteArrayList — for read-heavy lists

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("item");        // creates a new copy of the array on every write
list.get(0);             // reads need no lock — work on the stable snapshot
```

**When to use:** When reads vastly outnumber writes, and iteration (reading) is the main operation. Observer pattern listener lists, event subscriber lists.

**When not to use:** Frequent writes are expensive — every write copies the entire array.

### BlockingQueue — the producer-consumer workhorse

```java
BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100);  // bounded

// Producer thread
queue.put(task);       // blocks if queue is full

// Consumer thread
Task task = queue.take();  // blocks if queue is empty
```

`BlockingQueue` implementations:
- `LinkedBlockingQueue` — linked list, optionally bounded, good general choice
- `ArrayBlockingQueue` — array-backed, always bounded, predictable memory
- `PriorityBlockingQueue` — ordered by priority, unbounded
- `SynchronousQueue` — no capacity — handoff between producer and consumer directly

### Other useful concurrent collections

| Collection | Use case |
|---|---|
| `ConcurrentLinkedQueue` | Lock-free non-blocking queue |
| `ConcurrentSkipListMap` | Sorted concurrent map |
| `LinkedBlockingDeque` | Double-ended blocking queue |

---

## 12. Executor Framework and Thread Pools

**Never create raw threads in production code.** `new Thread().start()` is uncontrolled — unbounded thread creation crashes the JVM under load. Use the Executor framework.

### Why thread pools?

- Thread creation is expensive (stack allocation, OS scheduling)
- Unbounded threads → OOM, context switch overhead
- Thread pools reuse threads and bound the max concurrency

### The three most common executors

```java
// Fixed pool — N threads, tasks queue up if all busy
ExecutorService pool = Executors.newFixedThreadPool(4);

// Cached pool — creates threads on demand, reuses idle ones, unbounded
ExecutorService pool = Executors.newCachedThreadPool();

// Single thread — one thread, tasks execute sequentially
ExecutorService pool = Executors.newSingleThreadExecutor();
```

### Submitting tasks

```java
ExecutorService pool = Executors.newFixedThreadPool(4);

// Submit a Runnable — fire and forget
pool.execute(() -> System.out.println("Task running"));

// Submit a Callable — get a result back
Future<Integer> future = pool.submit(() -> {
    Thread.sleep(1000);
    return 42;
});

int result = future.get();      // blocks until result is ready
int result = future.get(2, TimeUnit.SECONDS);  // wait max 2 seconds

// Always shut down the pool when done
pool.shutdown();                // stops accepting new tasks, finishes existing ones
pool.shutdownNow();             // interrupts running tasks (best-effort)
```

### Future and CompletableFuture

**Future** — represents a result that will be available later. Limitation: `get()` is blocking.

**CompletableFuture** — non-blocking, chainable. The modern way:

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> fetchData())            // runs in ForkJoinPool
    .thenApply(data -> process(data))          // transform result
    .thenApply(result -> format(result))       // chain another step
    .exceptionally(ex -> "default value");     // handle errors

// Run two things in parallel, combine results
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "World");
CompletableFuture<String> combined = f1.thenCombine(f2, (a, b) -> a + " " + b);

// Wait for all
CompletableFuture.allOf(f1, f2).join();

// Take whichever finishes first
CompletableFuture.anyOf(f1, f2).join();
```

### ScheduledExecutorService — for periodic tasks

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

// Run once after a delay
scheduler.schedule(() -> sendReminder(), 10, TimeUnit.MINUTES);

// Run repeatedly at fixed rate (regardless of task duration)
scheduler.scheduleAtFixedRate(() -> syncData(), 0, 5, TimeUnit.SECONDS);

// Run with fixed delay between end of one run and start of next
scheduler.scheduleWithFixedDelay(() -> pollQueue(), 0, 2, TimeUnit.SECONDS);
```

---

## 13. Classic Concurrency Problems

These are the standard textbook problems. Knowing them shows interviewers you understand the fundamentals.

### Producer-Consumer

Multiple producer threads add items to a shared buffer. Multiple consumer threads remove and process items.

```java
class ProducerConsumer {
    private final BlockingQueue<Integer> queue;

    public ProducerConsumer(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    class Producer implements Runnable {
        public void run() {
            for (int i = 0; i < 10; i++) {
                try {
                    queue.put(i);          // blocks if full
                    System.out.println("Produced: " + i);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    class Consumer implements Runnable {
        public void run() {
            while (true) {
                try {
                    int item = queue.take();   // blocks if empty
                    System.out.println("Consumed: " + item);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
```

**Key insight:** `BlockingQueue` eliminates the need for manual `wait/notify`. It handles the blocking contract internally.

### Reader-Writer Problem

Multiple readers can read simultaneously. A writer needs exclusive access.

```java
// Solved cleanly with ReentrantReadWriteLock — see section 6
// Read: multiple concurrent readers allowed
// Write: exclusive access, no concurrent readers or writers
```

### Dining Philosophers (Deadlock illustration)

5 philosophers sit around a table. Each needs 2 forks to eat. Each fork is shared between two neighbours.

**The deadlock:** Each philosopher picks up their left fork and waits for their right fork — which is held by their neighbour waiting for their left fork.

**Solutions:**
1. **Resource ordering** — philosopher 4 picks up right fork before left (breaks circular wait)
2. **Arbitrator** — a waiter grants permission to pick up both forks atomically
3. **tryLock with timeout** — if can't get second fork, put first fork down and retry

### Singleton with Double-Checked Locking

```java
public class Singleton {
    // volatile prevents reordering — crucial for correctness
    private static volatile Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {                    // first check — no lock (fast path)
            synchronized (Singleton.class) {
                if (instance == null) {            // second check — inside lock (safe)
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

**Why double-check?** First check avoids locking for every call once initialised (performance). Second check inside the lock prevents two threads that both passed the first check from creating two instances.

**Why volatile?** `instance = new Singleton()` is three steps: allocate memory, initialise fields, assign reference. Without `volatile`, the compiler can reorder step 3 before step 2 — another thread sees a non-null but incompletely initialised object.

**Simpler alternative — Initialization-on-Demand Holder:**
```java
public class Singleton {
    private Singleton() {}

    private static class Holder {
        private static final Singleton INSTANCE = new Singleton();
    }

    public static Singleton getInstance() {
        return Holder.INSTANCE;  // class loaded lazily, thread-safe by JVM class loading
    }
}
```
This is cleaner. The JVM guarantees class initialisation is thread-safe.

---

## 14. Concurrency in LLD Problems

### Movie Ticket Booking — seat lock problem

Two users book the last seat simultaneously.

```java
class SeatBookingService {
    private final Map<String, Seat> seats = new ConcurrentHashMap<>();
    private final ReentrantLock bookingLock = new ReentrantLock();

    public boolean bookSeat(String seatId, String userId) {
        bookingLock.lock();
        try {
            Seat seat = seats.get(seatId);
            if (seat == null || seat.isBooked()) {
                return false;  // already taken
            }
            seat.book(userId);
            return true;
        } finally {
            bookingLock.unlock();
        }
    }
}
```

**Better approach — per-seat locking (finer granularity):**
```java
class SeatBookingService {
    private final ConcurrentHashMap<String, ReentrantLock> seatLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Seat> seats = new ConcurrentHashMap<>();

    private ReentrantLock getLockForSeat(String seatId) {
        return seatLocks.computeIfAbsent(seatId, k -> new ReentrantLock());
    }

    public boolean bookSeat(String seatId, String userId) {
        ReentrantLock lock = getLockForSeat(seatId);
        lock.lock();
        try {
            Seat seat = seats.get(seatId);
            if (seat.isBooked()) return false;
            seat.book(userId);
            return true;
        } finally {
            lock.unlock();
        }
    }
}
```

Per-seat locking means booking seat A doesn't block someone booking seat B. Much better throughput.

### Parking Lot — slot assignment

```java
class ParkingLot {
    private final ConcurrentHashMap<String, ParkingSlot> slots;
    private final AtomicInteger availableCount = new AtomicInteger(0);

    public ParkingSlot assignSlot(VehicleType type) {
        for (ParkingSlot slot : slots.values()) {
            if (slot.tryAssign(type)) {       // atomic CAS inside tryAssign
                availableCount.decrementAndGet();
                return slot;
            }
        }
        return null;  // full
    }
}

class ParkingSlot {
    private final AtomicBoolean isOccupied = new AtomicBoolean(false);
    private volatile Vehicle parkedVehicle;

    public boolean tryAssign(VehicleType type) {
        if (type != this.slotType) return false;
        return isOccupied.compareAndSet(false, true);  // atomic — only one thread succeeds
    }

    public void release() {
        parkedVehicle = null;
        isOccupied.set(false);
    }
}
```

### Bank Account — balance updates

```java
class BankAccount {
    private final String accountId;
    private double balance;
    private final ReentrantLock lock = new ReentrantLock();

    public boolean transfer(BankAccount target, double amount) {
        // Always lock accounts in consistent order to prevent deadlock
        BankAccount first  = this.accountId.compareTo(target.accountId) < 0 ? this : target;
        BankAccount second = first == this ? target : this;

        first.lock.lock();
        try {
            second.lock.lock();
            try {
                if (this.balance < amount) return false;
                this.balance -= amount;
                target.balance += amount;
                return true;
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }
}
```

**The key pattern here:** Lock ordering by account ID to prevent deadlock when two transfers between the same accounts run simultaneously.

### Rate Limiter — token bucket

```java
class TokenBucketRateLimiter {
    private final int maxTokens;
    private final int refillPerSecond;
    private AtomicInteger tokens;
    private volatile long lastRefillTimestamp;
    private final ReentrantLock refillLock = new ReentrantLock();

    public TokenBucketRateLimiter(int maxTokens, int refillPerSecond) {
        this.maxTokens = maxTokens;
        this.refillPerSecond = refillPerSecond;
        this.tokens = new AtomicInteger(maxTokens);
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    public boolean allowRequest() {
        refill();
        return tokens.getAndDecrement() > 0;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTimestamp;
        if (elapsed > 1000) {
            refillLock.lock();
            try {
                // Check again inside lock — another thread may have refilled already
                elapsed = now - lastRefillTimestamp;
                if (elapsed > 1000) {
                    int toAdd = (int)(elapsed / 1000) * refillPerSecond;
                    tokens.set(Math.min(maxTokens, tokens.get() + toAdd));
                    lastRefillTimestamp = now;
                }
            } finally {
                refillLock.unlock();
            }
        }
    }
}
```

---

## 15. Design Patterns for Concurrency

### Thread-Safe Singleton — covered in section 13

### Producer-Consumer with BlockingQueue — covered in section 13

### Thread-Local Storage

Each thread has its own copy of a variable. No sharing, no synchronisation needed.

```java
ThreadLocal<SimpleDateFormat> dateFormatter = ThreadLocal.withInitial(
    () -> new SimpleDateFormat("yyyy-MM-dd")
);

// Each thread gets its own SimpleDateFormat — no sharing, no race condition
String formatted = dateFormatter.get().format(new Date());

// Clean up when thread is done (critical for thread pools — threads are reused)
dateFormatter.remove();
```

Use cases: per-request context in web servers (user ID, transaction ID), database connections per thread, non-thread-safe formatters.

### Immutable Objects

If an object never changes after construction, it is inherently thread-safe. No synchronisation needed.

```java
final class Money {           // final — cannot be subclassed
    private final double amount;   // final — set once in constructor
    private final String currency;

    public Money(double amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public Money add(Money other) {
        return new Money(this.amount + other.amount, this.currency);  // returns new object
    }

    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
}
```

Rules for immutability:
- All fields `final`
- No setters
- Class declared `final` (prevents mutable subclasses)
- If a field is a mutable object (like a List), make a defensive copy in constructor and return copy in getter

### Object Pool Pattern (thread-safe)

```java
class ConnectionPool {
    private final BlockingQueue<Connection> pool;

    public ConnectionPool(int size) {
        pool = new ArrayBlockingQueue<>(size);
        for (int i = 0; i < size; i++) {
            pool.offer(new Connection());
        }
    }

    public Connection acquire() throws InterruptedException {
        return pool.take();   // blocks until one is available
    }

    public void release(Connection conn) {
        pool.offer(conn);     // returns connection to pool
    }
}
```

---

## 16. Interview Question Bank

These are the most commonly asked concurrency questions at SDE-2 level:

**Conceptual questions:**
- What is the difference between a process and a thread?
- What is a race condition? Give an example.
- What does `volatile` guarantee? What doesn't it guarantee?
- What is the difference between `synchronized` and `ReentrantLock`?
- Explain deadlock. How do you prevent it?
- What is the difference between `wait()` and `sleep()`?
- What is `happens-before`?
- What is the difference between `ConcurrentHashMap` and `Hashtable`?
- What is a thread pool and why should you use one?

**Difference questions (very common):**
- `synchronized method` vs `synchronized block`?
- `ReentrantLock` vs `synchronized`?
- `volatile` vs `AtomicInteger`?
- `execute()` vs `submit()` on an ExecutorService?
- `Future` vs `CompletableFuture`?
- Deadlock vs Livelock vs Starvation?
- `CopyOnWriteArrayList` vs `Collections.synchronizedList`?

**Design questions:**
- Design a thread-safe Singleton
- Design a producer-consumer system
- Design a thread-safe LRU Cache
- Design a rate limiter
- How would you make the Parking Lot thread-safe?
- How would you handle concurrent seat booking in a movie theatre?

### wait() vs sleep() — the most common trick question

| | `wait()` | `sleep()` |
|---|---|---|
| Defined in | `Object` | `Thread` |
| Releases lock? | **Yes** | **No** |
| Called from | Inside `synchronized` block | Anywhere |
| Woken up by | `notify()` / `notifyAll()` | Timer expiry or interrupt |
| Use case | Thread coordination / signalling | Pause for a duration |

**`wait()` releases the lock** — this is the key difference. A thread calling `wait()` says "I'm done with the lock for now, notify me when something changes." `sleep()` holds the lock and just pauses.

### notify() vs notifyAll()

- `notify()` — wakes up one arbitrary waiting thread
- `notifyAll()` — wakes up all waiting threads (they all contend for the lock; only one wins)

Prefer `notifyAll()` unless you are certain all waiting threads are waiting for the same condition. `notify()` can cause missed signals — the wrong thread wakes up, takes the lock, and the thread that should have woken remains sleeping.

---

## 17. Quick Reference Card

### The three problems and their fixes

```
Visibility   → volatile, synchronized, Lock
Atomicity    → synchronized, Lock, AtomicInteger/Long/Reference
Ordering     → volatile, synchronized, Lock (all establish happens-before)
```

### Choose your tool

```
Simple counter increment          → AtomicInteger
Boolean flag (written once)       → volatile boolean
Complex multi-step critical section → synchronized or ReentrantLock
Need tryLock / timeout            → ReentrantLock
Read-heavy, rare writes           → ReentrantReadWriteLock
Thread-safe map                   → ConcurrentHashMap
Thread-safe list (read-heavy)     → CopyOnWriteArrayList
Producer-consumer                 → BlockingQueue (LinkedBlockingQueue / ArrayBlockingQueue)
Per-thread state, no sharing      → ThreadLocal
Never-changes-after-construction  → Immutable object (no sync needed)
Run tasks asynchronously          → ExecutorService + Future / CompletableFuture
```

### Deadlock prevention rules

```
1. Always acquire locks in the same order
2. Use tryLock with timeout
3. Avoid holding a lock while acquiring another
4. If you must lock two objects, order by ID
```

### wait() vs sleep() one-liner

```
wait()  → releases lock, must be in synchronized, woken by notify()
sleep() → holds lock, can be anywhere, woken by timer
```

### Thread-safe Singleton — two ways

```
1. Double-checked locking with volatile
2. Initialization-on-demand Holder (simpler, preferred)
```

---

## 18. Revision Checklist

Test yourself — answer these without re-reading the file first.

- [ ] What are the three things that can go wrong in concurrent programs? Name and explain each.
- [ ] Write a thread-safe counter using (a) `synchronized`, (b) `ReentrantLock`, (c) `AtomicInteger`.
- [ ] What does `volatile` guarantee? Why is `volatile int count; count++` still not safe?
- [ ] What is double-checked locking? Why is `volatile` necessary for it?
- [ ] Give the four conditions for deadlock. How do you prevent it in a bank transfer between two accounts?
- [ ] What is the difference between `wait()` and `sleep()`? Which one releases the lock?
- [ ] When would you use `ReentrantReadWriteLock` instead of `ReentrantLock`?
- [ ] What is `ConcurrentHashMap` and how is it different from `Collections.synchronizedMap`?
- [ ] Write a producer-consumer using `BlockingQueue`.
- [ ] What is `compareAndSet`? When would you use it?
- [ ] How do you make the Parking Lot slot assignment thread-safe? Sketch the key classes.
- [ ] What is `CompletableFuture`? How does it differ from `Future`?
- [ ] When should you use `ThreadLocal`? Give a real use case.
- [ ] What is starvation? How do you prevent it?
- [ ] What does `notify()` do vs `notifyAll()`? When would you prefer one over the other?

---

## Key Takeaways

1. **Shared mutable state = concurrency problem.** Fix by removing sharing, removing mutation, or adding synchronisation.
2. **`volatile` fixes visibility only.** For read-modify-write operations, use `AtomicInteger` or `synchronized`.
3. **Always unlock in `finally`.** Every `lock.lock()` must have `lock.unlock()` in a `finally` block.
4. **Per-object locking beats one global lock.** Lock at the finest granularity that is still correct.
5. **Consistent lock ordering prevents deadlock.** When locking two objects, always do it in the same order everywhere.
6. **`wait()` releases the lock. `sleep()` does not.** This is the most common trick question.
7. **Use `BlockingQueue` for producer-consumer.** It handles the wait/notify contract internally.
8. **Never create raw threads in production.** Use `ExecutorService`.
9. **`ConcurrentHashMap` over `Hashtable` always.** Segment-level locking, dramatically better throughput.
10. **Immutable objects are the ultimate thread safety.** If state never changes, there's nothing to synchronise.
