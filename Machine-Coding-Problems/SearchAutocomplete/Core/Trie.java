package SearchAutocomplete.Core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Trie
{
    private final TrieNode root;
    public Trie(){
        root = new TrieNode();
    }
    public void insert(String s)
    {
        TrieNode current = root;

        for(char ch : s.toCharArray())
        {
            if(!current.children.containsKey(ch))
            {
                current.children.put(ch,new TrieNode());
            }
            current = current.children.get(ch);
        }
        current.isEndOfWord = true;
        current.frequency += 1;
    }

    public boolean isPrefix(String s)
    {
        TrieNode current = root;

        for(char ch : s.toCharArray())
        {
            if(!current.children.containsKey(ch))
            {
                return false;
            }
            current = current.children.get(ch);
        }
        return true;
    }

    private void dfs(TrieNode node, String s, List<String>resultList)
    {
        if(node.isEndOfWord)
        {
            resultList.add(s);
        }
        for(Map.Entry<Character,TrieNode>entry : node.children.entrySet())
        {
            dfs(entry.getValue() , s + entry.getKey() , resultList);
        }
    }
    public List<String> prefixSearch(String s)
    {
        TrieNode current = root;

        List<String> resultList = new ArrayList<>();

        for(char ch: s.toCharArray())
        {
            if(!current.children.containsKey(ch))
            {
                return resultList;
            }
            current = current.children.get(ch);
        }
        dfs(current, s, resultList);
        return resultList;
    }

    private boolean deleteNode(String s, TrieNode root, int index)
    {
        if(index == s.length())
        {
            if(!root.isEndOfWord)
                return false;
            root.isEndOfWord = false;
            return root.children.isEmpty();
        }

        char ch = s.charAt(index);
        TrieNode nextNode  = root.children.get(ch);
        if(nextNode == null)
            return false;

        boolean shouldDeleteChildNode = deleteNode(s,nextNode,index+1);
        if(shouldDeleteChildNode)
        {
            root.children.remove(ch);
            return root.children.isEmpty() && !root.isEndOfWord;
        }
        return false;
    }
    public boolean delete(String s)
    {
        return deleteNode(s, root, 0);
    }
}
