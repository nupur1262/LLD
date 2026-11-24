package SearchAutocomplete.Core;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TrieService
{
    private final Trie trie = new Trie();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    public void insert(String word)
    {
        lock.writeLock().lock();
        try
        {
            trie.insert(word);
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public List<String> prefixSearch(String prefix)
    {
        lock.readLock().lock();
        try
        {
            return trie.prefixSearch(prefix);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    public boolean delete(String word)
    {
        lock.writeLock().lock();
        try{
            return trie.delete(word);
        }
        finally {
            lock.writeLock().unlock();
        }
    }
    public boolean isPrefix(String word)
    {
        lock.readLock().lock();
        try
        {
            return trie.isPrefix(word);
        }
        finally {
            lock.readLock().unlock();
        }
    }

}