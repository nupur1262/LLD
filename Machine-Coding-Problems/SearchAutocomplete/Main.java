package SearchAutocomplete;

import SearchAutocomplete.Core.RankingType;
import SearchAutocomplete.Core.SuggestionResult;
import SearchAutocomplete.Core.TrieService;
import SearchAutocomplete.Service.AutoCompleteService;
import SearchAutocomplete.Strategy.AlphabeticalRankingStrategy;

public class Main
{
    public static void main(String[] args)
    {
        AutoCompleteService service = new AutoCompleteService(new TrieService(),new AlphabeticalRankingStrategy(), RankingType.Alphabetical, 3);

        service.addWord("project");
        service.addWord("problem");
        service.addWord("progress");
        service.addWord("pro");
        service.addWord("program");
        service.addWord("java");



        SuggestionResult result1 = service.getSuggestions("pro");
        System.out.println("Prefix    : " + result1.prefix);
        System.out.println("Suggestions: " + result1.suggestions);
        System.out.println("Strategy  : " + result1.rankingStrategy);
        System.out.println("Time (ms) : " + result1.timeTakenMs);
        System.out.println("---");

        SuggestionResult result2 = service.getSuggestions("java");
        System.out.println("Prefix    : " + result2.prefix);
        System.out.println("Suggestions: " + result2.suggestions);
        System.out.println("Strategy  : " + result2.rankingStrategy);
        System.out.println("Time (ms) : " + result2.timeTakenMs);
        System.out.println("---");


        SuggestionResult result3 = service.getSuggestions("xyz");
        System.out.println("Prefix    : " + result3.prefix);
        System.out.println("Suggestions: " + result3.suggestions);
        System.out.println("Strategy  : " + result3.rankingStrategy);
        System.out.println("Time (ms) : " + result3.timeTakenMs);
        System.out.println("---");

        SuggestionResult result4 = service.getSuggestions("PROG");
        System.out.println("Prefix    : " + result4.prefix);
        System.out.println("Suggestions: " + result4.suggestions);
        System.out.println("Strategy  : " + result4.rankingStrategy);
        System.out.println("Time (ms) : " + result4.timeTakenMs);
        System.out.println("---");
    }
}