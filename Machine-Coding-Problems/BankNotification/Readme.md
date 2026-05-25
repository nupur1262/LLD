# Banking Notification System — Low Level Design

A scalable, extensible notification system for a banking application, designed using industry-standard design patterns.

---

## Table of Contents

- [Problem Statement](#problem-statement)
- [Requirements](#requirements)
- [Design Patterns Used](#design-patterns-used)
- [Architecture Overview](#architecture-overview)
- [Class Design](#class-design)
- [End-to-End Flow](#end-to-end-flow)
- [Future Scope](#future-scope)

---

## Problem Statement

Design a Notification System for a banking application that:
- Sends notifications to customers via multiple channels (Email, SMS, Push)
- Supports different notification types (OTP, Transactional, Promotional)
- Handles priority and retry logic
- Is extensible for future channels like WhatsApp

---

## Requirements

### Functional Requirements
1. Bank has different customers to whom it sends notifications
2. Multiple notification channels: Email, SMS, Push Notifications
3. Multiple notification types: OTP, Transaction Alerts, Promotional Messages
4. Each notification has a priority level and retry logic

### Non-Functional Requirements
1. System should be extensible (open for extension, closed for modification)
2. Low coupling between components
3. Fault tolerant with retry and fallback mechanisms

---

## Design Patterns Used

### 1. Observer Pattern
- **Why:** To decouple the event source (TransactionService) from the notification delivery system
- **Subject:** `TransactionService` — publishes events when a transaction occurs
- **Observer:** `NotificationService` — subscribes to transaction events and triggers notification delivery
- **Benefit:** Adding new event sources in the future doesn't require modifying existing observers

### 2. Strategy Pattern
- **Why:** Multiple delivery channels (SMS, Email, Push) with potentially more in the future
- **Interface:** `Channel` — defines a common `sendNotification()` contract
- **Concrete Strategies:** `SMSChannel`, `EmailChannel`, `PushChannel`
- **Benefit:** Adding WhatsApp simply means creating a new `WhatsAppChannel` class — no existing code changes needed

### 3. Factory Pattern
- **Why:** Centralizes the creation of different `Notification` objects with correct priority and metadata
- **Class:** `NotificationFactory`
- **Benefit:** Notification creation logic is in one place; adding a new type only requires updating the factory

---

## Architecture Overview

```
TransactionService  ──(event)──►  NotificationService
                                        │
                                        ▼
                                 NotificationFactory
                                 (creates Notification object)
                                        │
                                        ▼
                              PriorityBlockingQueue
                              [OTP=1, Transaction=2, Promotional=3]
                                        │
                                        ▼
                               Channel Strategy
                          (SMSChannel / EmailChannel / PushChannel)
                                        │
                              ┌─────────┴─────────┐
                           Success              Failure
                                                  │
                                                  ▼
                                           RetryService
                                     (exponential backoff, max 3)
                                                  │
                                        ┌─────────┴─────────┐
                                    Retry OK          Max Retries Exhausted
                                                            │
                                              ┌─────────────┼─────────────┐
                                              ▼             ▼             ▼
                                       Switch Channel  Dead Letter    Alert
                                       (fallback)        Queue       Ops Team
```

---

## Class Design

### Entities

```
Bank
- id: String
- name: String
- users: List<User>

User
- id: String
- name: String
- email: String
- phone: String
- deviceToken: String

Notification
- id: String
- type: NotificationType
- priority: Integer
- userId: String
- body: String
- status: NotificationStatus

NotificationType (Enum)
- OTP
- TRANSACTION
- PROMOTIONAL

NotificationStatus (Enum)
- PENDING
- SENT
- FAILED
```

### Interfaces

```
Channel (Interface)
+ sendNotification(notification: Notification): void
```

### Concrete Channel Implementations

```
EmailChannel implements Channel
+ sendNotification(notification: Notification): void

SMSChannel implements Channel
+ sendNotification(notification: Notification): void

PushChannel implements Channel
+ sendNotification(notification: Notification): void
```

### Services

```
TransactionService (Subject)
- observers: List<NotificationService>
+ subscribe(observer: NotificationService): void
+ unsubscribe(observer: NotificationService): void
+ notifyObservers(event: TransactionEvent): void
+ processTransaction(transaction: Transaction): void

NotificationService (Observer)
- factory: NotificationFactory
- queue: PriorityBlockingQueue<Notification>
- retryService: RetryService
+ update(event: TransactionEvent): void
+ processQueue(): void
+ resolveChannel(notification: Notification): Channel

NotificationFactory
+ createNotification(type: NotificationType, userId: String, body: String): Notification

RetryService
- maxRetries: int  (default: 3)
- retryCount: int
- backoffMultiplier: int  (default: 2)
+ retry(notification: Notification, channel: Channel): void
+ exponentialBackoff(retryCount: int): void
+ fallback(notification: Notification): void
- switchChannel(notification: Notification): void
- sendToDeadLetterQueue(notification: Notification): void
- alertOpsTeam(notification: Notification): void
```

---

## End-to-End Flow

1. A transaction occurs → `TransactionService.processTransaction()` is called
2. `TransactionService` publishes event → `NotificationService.update()` is triggered
3. `NotificationService` calls `NotificationFactory.createNotification()` to create a `Notification` object with correct priority
4. `Notification` is placed in `PriorityBlockingQueue` (OTP=1, Transaction=2, Promotional=3)
5. Worker thread picks up highest priority notification from the queue
6. `NotificationService.resolveChannel()` determines the right delivery channel (SMS/Email/Push)
7. `channel.sendNotification(notification)` is called
8. **On success:** Notification status updated to `SENT`
9. **On failure:** `RetryService.retry()` is called
    - Retries up to 3 times with exponential backoff (2s → 4s → 8s)
    - On max retries exhausted → `fallback()` is triggered:
        - Switch to alternate channel
        - Log to Dead Letter Queue
        - Alert Ops Team
    - Notification status updated to `FAILED`

---

## Future Scope

- **WhatsApp Channel:** Simply implement the `Channel` interface → `WhatsAppChannel` — no existing code changes needed
- **New Notification Types:** Add to `NotificationType` enum and update `NotificationFactory`
- **Analytics & Reporting:** Track delivery rates, failure rates per channel
- **Rate Limiting:** Prevent notification spam per user
- **Template Engine:** Dynamic message templates per notification type

---

## Work In Progress

- [ ] UML Class Diagram
- [ ] UML Sequence Diagram
- [ ] Java / Python Implementation
- [ ] Unit Tests

---

*Designed as part of a Low Level Design (LLD) interview exercise.*