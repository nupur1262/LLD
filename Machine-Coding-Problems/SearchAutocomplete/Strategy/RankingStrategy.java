package SearchAutocomplete.Strategy;

import java.util.List;

public interface RankingStrategy
{
    public List<String> rank(List<String> words, int limit);
}
