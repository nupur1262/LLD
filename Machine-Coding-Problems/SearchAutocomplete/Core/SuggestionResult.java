package SearchAutocomplete.Core;

import java.util.List;

public class SuggestionResult
{
    public List<String> suggestions;
    public RankingType rankingStrategy;
    public String prefix;
    public Long timeTakenMs;

    public SuggestionResult(List<String>suggestions, RankingType type, String prefix, long timeTakenMs)
    {
        this.prefix = prefix;
        this.suggestions = suggestions;
        this.rankingStrategy = type;
        this.timeTakenMs = timeTakenMs;
    }
}
