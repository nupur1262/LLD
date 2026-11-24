# Search Autocomplete — LLD Interview Revision Notes

> Develop an autocomplete feature for search queries using a Trie structure.
> The system should suggest words as the user types.
>
> Example: Input: "pro" → Suggestions: "pro", "problem", "program", "progress", "project"

---

## 1. Functional Requirements

1. As the user types, the system generates top 3 completions.
2. Ranking is alphabetical by default → but should be pluggable (Strategy Pattern).
3. System should support adding new words dynamically.
4. System should support deletion of words.
5. Only prefix search is supported (not substring/fuzzy).
6. Search is case-insensitive → normalize all input to lowercase on insert and search.
7. If no match found, return an empty list.
8. If the prefix itself is a valid word, it SHOULD be returned in the list.

---

## 2. Non-Functional Requirements

1. Availability > Consistency → stale suggestions are acceptable, downtime is not.
2. Latency ≤ 100ms end-to-end (user typing in real time).
3. Up to 10M words in the dictionary → memory usage is a concern.
4. Concurrent system → multiple users searching and inserting simultaneously.
   → Solution: ReadWriteLock (many concurrent readers, exclusive writers).

---

## 3. Domain / Entities

| Class | Type | Responsibility |
|---|---|---|
| TrieNode | Model | Holds one character's data (children, isEndOfWord, frequency) |
| Trie | Core DS | Insert, delete, prefix search logic |
| TrieService | Service | Thread-safe wrapper over Trie using ReadWriteLock |
| RankingStrategy | Interface | Pluggable ranking contract |
| AlphabeticalRankingStrategy | Implementation | Sorts words alphabetically |
| RankingType | Enum | Tags which ranking algo was used (ALPHABETICAL, FREQUENCY) |
| SuggestionResult | Model | Response object with suggestions + metadata |
| AutoCompleteService | Facade | Orchestrates TrieService + RankingStrategy |
| WordRepository | Interface | Optional persistence abstraction |

---

## 4. Class Definitions

### TrieNode
```
Map<Character, TrieNode> children   // character → next node
boolean isEndOfWord                  // true if a word ends at this node
int frequency                        // how many times this word was inserted/searched
```
> KEY LESSON: isEndOfWord ≠ isLeaf.
> "pro" can be isEndOfWord=true AND have children (j→project, b→problem).
> isLeaf means no children. isEndOfWord means a word terminates here. They are different!

### Trie
```
insert(String word)                  // O(L) — L = word length
delete(String word) : boolean        // recursive backtracking
isPrefix(String s) : boolean         // O(P) — P = prefix length
prefixSearch(String s) : List<String>// O(P + N) — N = nodes visited in DFS
```

### SuggestionResult
```
List<String> suggestions
RankingType rankingStrategy
String prefix
long timeTakenMs
```

### RankingType (Enum)
```
ALPHABETICAL
FREQUENCY
```

### RankingStrategy (Interface)
```
List<String> rank(List<String> words, int limit)
```
> Note: Interface methods are implicitly public in Java. No need to write 'public'.

### AlphabeticalRankingStrategy
```
rank(words, limit):
  Collections.sort(words)
  return new ArrayList<>(words.subList(0, Math.min(limit, words.size())))
```
> KEY LESSON: subList() returns a live VIEW of the original list, not a copy.
> Wrap with new ArrayList<>() to avoid unintended side effects.

### TrieService
```
ReadWriteLock lock = new ReentrantReadWriteLock()
insert(word)       → writeLock
delete(word)       → writeLock
prefixSearch(s)    → readLock
isPrefix(s)        → readLock
```

### AutoCompleteService (Facade)
```
getSuggestions(prefix):
  1. normalize → toLowerCase()
  2. trieService.prefixSearch(prefix) → raw list
  3. rankingStrategy.rank(raw, limit) → ranked list
  4. return SuggestionResult(ranked, rankingType, prefix, timeTaken)

addWord(word)   → normalize + trieService.insert()
removeWord(word)→ normalize + trieService.delete()
```

---

## 5. Design Patterns Used

| Pattern | Where | Why |
|---|---|---|
| Strategy | RankingStrategy interface | Swap ranking algorithm at runtime without changing AutoCompleteService |
| Singleton | Trie | Only one Trie instance across the application |
| Facade | AutoCompleteService | Hides complexity of Trie + Locking + Ranking behind one clean method |

---

## 6. How a Trie Works

```
Insert: "pro", "project", "problem"

root
 └── p
      └── r
           └── o  ✅ isEndOfWord=true  ("pro")
                ├── j → e → c → t  ✅  ("project")
                └── b → l → e → m  ✅  ("problem")
```

- Each node stores a map of its children and an isEndOfWord flag.
- Shared prefix storage is the Trie's superpower.
- Insert: walk char by char, create node if missing, mark isEndOfWord at end.
- PrefixSearch: navigate to prefix node, then DFS to collect all complete words.
- Delete: recursive backtracking — unmark isEndOfWord, remove nodes only if they have no children and are not end of another word.

### Complexity
| Operation | Time | Space |
|---|---|---|
| Insert | O(L) | O(L) |
| Search/isPrefix | O(P) | O(1) |
| PrefixSearch | O(P + N) | O(N) |
| Delete | O(L) | O(L) call stack |

---

## 7. Concurrency Deep Dive

### Why plain reads are NOT safe without locks

1. Structural change mid-read: Thread A is DFS-ing when Thread B deletes a node → NullPointerException.
2. HashMap is not thread-safe: concurrent reads during a rehash can return corrupt data.
3. CPU cache visibility: without synchronization, Thread A may not see Thread B's latest write (Java Memory Model).

### Three options compared

| Approach | Safe? | Concurrent Reads? | Notes |
|---|---|---|---|
| synchronized on writes only | ❌ No | — | Reads still unsafe (see above) |
| synchronized on ALL methods | ✅ Yes | ❌ No | Simple but one thread at a time always |
| ReadWriteLock | ✅ Yes | ✅ Yes | Best for read-heavy systems |

### How ReadWriteLock works internally

Maintains two counters:
- activeReaders (can be many)
- activeWriters (0 or 1 only)

```
readLock().lock()  → allowed only if activeWriters == 0
writeLock().lock() → allowed only if activeReaders == 0 AND activeWriters == 0
```

Result: Many readers enter simultaneously. Writers wait for all readers to finish.
Readers block only when a writer is active.

### Fairness Policy

```java
new ReentrantReadWriteLock()       // Non-fair (default)
new ReentrantReadWriteLock(true)   // Fair
```

| | Non-Fair | Fair |
|---|---|---|
| Order | No guarantee | Strict FIFO |
| Throughput | Higher | Lower |
| Writer starvation | Possible | Impossible |

> For autocomplete: non-fair is fine (reads dominate).
> If write latency matters, use fair mode or make it configurable.

### Why always use finally for unlock

```java
lock.writeLock().lock();
try {
    trie.insert(word);  // if this throws an exception...
} finally {
    lock.writeLock().unlock();  // ...this STILL runs. Without finally → deadlock forever.
}
```

---

## 8. Delete — The Trickiest Part

Delete uses recursive backtracking:
- Travel all the way down to the end of the word.
- On the way BACK UP, decide whether to remove each node.
- Only remove a node if: it has NO children AND is NOT the end of another word.

```
Example: delete("project") while "pro" exists
  → Unmark isEndOfWord at 't'
  → Remove 't', 'c', 'e', 'j' nodes (all empty, not end of any other word)
  → STOP at 'o' because isEndOfWord=true ("pro" still exists)
```

The recursive helper returns true to mean "caller, you can safely remove me."

---

## 9. Interview Questions to Prepare

**Q: What is the difference between isLeaf and isEndOfWord?**
A: isLeaf means a node has no children. isEndOfWord means a valid word ends at that node.
   "pro" can be isEndOfWord=true while having children (project, problem). Using isLeaf breaks delete and prefix search.

**Q: Why ReadWriteLock over synchronized?**
A: Autocomplete is read-heavy. ReadWriteLock allows concurrent reads (users typing simultaneously)
   while blocking only for writes. synchronized forces all threads to take turns even for reads,
   killing the 100ms latency NFR at scale.

**Q: What happens if two write threads and two read threads are waiting?**
A: Depends on fairness mode. Non-fair (default): no guaranteed order, readers may barge in,
   writers risk starvation. Fair mode: strict FIFO, no starvation, lower throughput.

**Q: Your Trie uses too much memory with 10M words. How do you optimize?**
A: Several approaches:
   1. Compressed Trie (Radix Tree) — merge single-child chains into one node.
   2. Store children as array[26] instead of HashMap for fixed alphabet (faster, more cache-friendly).
   3. Limit Trie depth (cap word length).
   4. Offload cold words to disk, keep hot prefixes in memory (LRU cache).

**Q: How would you scale this to 10M concurrent users?**
A: 1. Horizontal scaling — multiple stateless AutoCompleteService instances behind a load balancer.
   2. Shared Trie — use a distributed cache (Redis) or replicate Trie across nodes.
   3. Prefix-based sharding — route "a*" to server 1, "b*" to server 2, etc.
   4. Result caching — cache getSuggestions("pro") result for N seconds (most prefixes repeat).
   5. Eventual consistency is acceptable (Availability > Consistency NFR).

**Q: How would you support ranking by frequency?**
A: Add a FrequencyRankingStrategy that sorts by node.frequency descending.
   Since RankingStrategy is an interface, no other class changes.
   This is the Strategy Pattern paying off.

**Q: Why is subList() dangerous?**
A: subList() returns a live view of the original list. If the original list is modified later,
   the view changes too. Always wrap with new ArrayList<>(subList(...)) to get an independent copy.

**Q: Why use toLowerCase() in AutoCompleteService and not in Trie?**
A: Single Responsibility. Trie is a pure data structure — it shouldn't know about business rules
   like case normalization. AutoCompleteService owns that rule and normalizes before delegating.

---

## 10. Project Structure

```
SearchAutocomplete/
├── Main.java
├── Core/
│   ├── TrieNode.java
│   ├── Trie.java
│   ├── TrieService.java          ← Thread-safe wrapper (ReadWriteLock)
│   ├── RankingType.java          ← Enum: ALPHABETICAL, FREQUENCY
│   └── SuggestionResult.java     ← Response model
├── Strategy/
│   ├── RankingStrategy.java      ← Interface
│   └── AlphabeticalRankingStrategy.java
└── Service/
    └── AutoCompleteService.java  ← Facade (orchestrator)
```

---

## 11. Sample Output

```
Prefix    : pro
Suggestions: [pro, problem, program]
Strategy  : Alphabetical
Time (ms) : 1

Prefix    : java
Suggestions: [java]
Strategy  : Alphabetical
Time (ms) : 0

Prefix    : xyz
Suggestions: []
Strategy  : Alphabetical
Time (ms) : 0

Prefix    : prog
Suggestions: [program, progress]
Strategy  : Alphabetical
Time (ms) : 0
```