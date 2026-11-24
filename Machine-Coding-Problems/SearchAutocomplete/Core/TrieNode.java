package SearchAutocomplete.Core;

import java.util.HashMap;
import java.util.Map;

public class TrieNode
{
    Map<Character, TrieNode> children;
    boolean isEndOfWord;
    int frequency;

    public TrieNode()
    {
        children = HashMap.newHashMap(26);
        isEndOfWord = false;
        frequency = 0;
    }
}
