package SearchAutocomplete.Service;

import SearchAutocomplete.Core.RankingType;
import SearchAutocomplete.Core.SuggestionResult;
import SearchAutocomplete.Core.TrieService;
import SearchAutocomplete.Strategy.RankingStrategy;

import java.util.List;

public class AutoCompleteService
{
    private final TrieService trieService;
    private final RankingStrategy rankingStrategy;
    private final RankingType rankingType;
    private final int limit;

    public AutoCompleteService(TrieService trieService,
                               RankingStrategy rankingStrategy,
                               RankingType rankingType,
                               int limit) {
        this.limit = limit;
        this.trieService = trieService;
        this.rankingStrategy = rankingStrategy;
        this.rankingType = rankingType;
    }

    public SuggestionResult getSuggestions(String word)
    {
        long start = System.currentTimeMillis();
        word = word.toLowerCase();
        List<String> raw = trieService.prefixSearch(word);
        List<String> ranked = rankingStrategy.rank(raw, limit);
        long end = System.currentTimeMillis();
        return new SuggestionResult(ranked, rankingType , word, end-start);
    }


    public void addWord(String word)
    {
        word = word.toLowerCase();
        trieService.insert(word);
    }

    public void removeWord(String word)
    {
        word = word.toLowerCase();
        trieService.delete(word);
    }
}
