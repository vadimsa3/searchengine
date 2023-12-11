package searchengine.utilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaModel;

import java.io.IOException;
import java.util.*;

@Service
public class WordFinderUtil {

    private final LemmaFinderUtil lemmaFinderUtil;

    public WordFinderUtil() throws IOException {
        lemmaFinderUtil = new LemmaFinderUtil();
    }

    public String getSnippet(String fullContentPage, List<String> lemmas) {
        if (lemmas.isEmpty()) {
            return null;
        }
        String onlyTextFromPage = getTextFromFullContentPage(fullContentPage);
        Map<String, Integer> snippetsOnPage = new HashMap<>();
        for (String lemma : lemmas) {
            Set<Integer> indexesLemmas = getIndexesLemmaInText(onlyTextFromPage, lemma);
            for (int startIndex : indexesLemmas) {
                int endIndex = startIndex + 200;
                int nextSpaceIndex = onlyTextFromPage.indexOf(" ", endIndex);
                if (nextSpaceIndex != -1) {
                    endIndex = nextSpaceIndex;
                }
                String resultSnippet = onlyTextFromPage.substring(startIndex, endIndex)
                        .concat("...");
                int countSearchLemmasOnPage = 0;
                countSearchLemmasOnPage += StringUtils.countMatches(onlyTextFromPage, lemma);
                snippetsOnPage.put(resultSnippet, countSearchLemmasOnPage);
            }
        }
        return snippetsOnPage.isEmpty()
                ? null
                : getTextSnippetWithSelectLemma(snippetsOnPage.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElse(null).getKey(), lemmas);
    }

    public Set<Integer> getIndexesLemmaInText(String onlyTextPage, String lemma) {
        String newEditText = onlyTextPage.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", "").trim();
        String[] onlyWordsFromText = newEditText.split("\\s");
        Set<Integer> indexWordInText = new HashSet<>();
        for (String word : onlyWordsFromText) {
            if (isLemmaInText(lemma, word)) {
                indexWordInText.add(onlyTextPage.toLowerCase().indexOf(word));
            }
        }
        return indexWordInText;
    }

    public String getTitleFromFullContentPage(String html) {
        Document document = Jsoup.parse(html);
        return document.title();
    }

    public String getTextFromFullContentPage(String html) {
        Document document = Jsoup.parse(html);
        return document.text();
    }

    public boolean isLemmaInText(String lemma, String word) {
        if (word.isBlank()) {
            return false;
        }
        List<String> wordBaseForms = lemmaFinderUtil.luceneMorphology.getMorphInfo(word);
        if (lemmaFinderUtil.anyWordBaseFormBelongToParticle(wordBaseForms)) {
            return false;
        }
        List<String> normalWordForms = lemmaFinderUtil.luceneMorphology.getNormalForms(word);
        if (normalWordForms.isEmpty()) {
            return false;
        }
        return normalWordForms.get(0).equals(lemma);
    }

    public String getTextSnippetWithSelectLemma(String textSnippet, List<String> lemmas) {
        List<String> listTextSnippetWithLemmaSelect = new ArrayList<>();
        listTextSnippetWithLemmaSelect.add(textSnippet);
        lemmas.forEach(lemma -> {
            String textSnippetFromList = listTextSnippetWithLemmaSelect.get(0);
            String textSnippetWithLemmaSelect = textSnippetFromList.toLowerCase()
                    .replaceAll(lemma, "<b> ".concat(lemma).concat(" </b>"));
            listTextSnippetWithLemmaSelect.add(0, textSnippetWithLemmaSelect);
        });
        return listTextSnippetWithLemmaSelect.get(0);
    }
}