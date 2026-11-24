package SearchAutocomplete.Strategy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AlphabeticalRankingStrategy implements RankingStrategy{
    @Override
    public List<String> rank(List<String> words, int limit)
    {
        Collections.sort(words);
        return words.subList(0,Math.min(limit,words.size()));
    }
}
