package searchengine.utilities;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
public class WordFinderUtil {

    private final LemmaFinderUtil lemmaFinderUtil;

    public WordFinderUtil() throws IOException {
        lemmaFinderUtil = new LemmaFinderUtil();
    }

    // !!!!!ПРОВЕРИТЬ ГЛЮК СО СНИППЕТАМИ !!!!
        public String getSnippet(String fullContentPage, List<String> lemmas) throws IOException {
        String onlyTextPage = getTextFromFullContentPage(fullContentPage);
        System.out.println("+++++++++++++ ПРОВЕРКА onlyTextPage в getSnippet " + onlyTextPage);
        System.out.println("+++++++++++++ ПРОВЕРКА LEMMAS В getSnippet " + lemmas);
        String newText = onlyTextPage.toLowerCase(Locale.ROOT).replaceAll("([^а-я\\s])", "").trim();
        System.out.println("+++++++++++++ ПРОВЕРКА newText в getSnippet " + newText);
        String[] words = newText.split("\\s");

        List<Integer> indexInText = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            for (String lemma : lemmas) {
                if (isLemmaInText(lemma, word)) {
                    indexInText.add(onlyTextPage.toLowerCase().indexOf(word));
                }
            }
        }

        Collections.sort(indexInText);

        int bestStartIndex = 0;
        int bestEndIndex = 0;
        int maxLemmas = 0;

        for (int i = 0; i < indexInText.size(); i++) {
            int startIndex = indexInText.get(i);
            int endIndex = startIndex + 150;
            int nextSpaceIndex = onlyTextPage.indexOf(" ", endIndex);
            if (nextSpaceIndex != -1) {
                endIndex = nextSpaceIndex;
            }

            int currentLemmas = 0;
            for (String lemma : lemmas) {
                if (onlyTextPage.toLowerCase().substring(startIndex, endIndex).contains(lemma)) {
                    currentLemmas++;
                }
            }
            if (currentLemmas > maxLemmas) {
                maxLemmas = currentLemmas;
                bestStartIndex = startIndex;
                bestEndIndex = endIndex;
            }
        }

        return onlyTextPage.substring(bestStartIndex, bestEndIndex);
    }

    public String getTitleFromFullContentPage(String html) {
        return Jsoup.parse(html).title();
    }

    private String getTextFromFullContentPage(String html) {
        return Jsoup.parse(html).text();
    }

    public boolean isLemmaInText(String lemma, String word) {
        if (word.isBlank()) {
            return false;
        }
        List<String> wordBaseForms = lemmaFinderUtil.luceneMorphology.getMorphInfo(word);
        if (lemmaFinderUtil.anyWordBaseBelongToParticle(wordBaseForms)) {
            return false;
        }
        List<String> normalForms = lemmaFinderUtil.luceneMorphology.getNormalForms(word);
        if (normalForms.isEmpty()) {
            return false;
        }
        return normalForms.get(0).equals(lemma);
    }

    // !!! РЕАЛИЗОВАТЬ - проверка на язык ввода слова
    private String checkEnterWordLanguage(String word) {
        String russianAlphabet = "[а-яА-Я]+";
        String englishAlphabet = "[a-zA-z]+";
        if (word.matches(russianAlphabet)) {
            return "Russian";
        } else if (word.matches(englishAlphabet)) {
            return "Please enter a word in Russian language";
        } else {
            return "";
        }
    }
}
