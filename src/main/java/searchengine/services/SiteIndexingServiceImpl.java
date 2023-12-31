package searchengine.services;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteIndex;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utilities.*;

@Service
public class SiteIndexingServiceImpl implements SiteIndexingService {

    private static final Logger log = LoggerFactory.getLogger(SiteIndexingServiceImpl.class);

    private ForkJoinPool forkJoinPool;

    @Autowired
    private SitesList sitesList;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteModelUtil siteModelUtil;
    @Autowired
    private PageModelUtil pageModelUtil;
    @Autowired
    private LemmaModelUtil lemmaModelUtil;
    @Autowired
    private LemmaFinderUtil lemmaFinderUtil;

    @Getter
    private static String domainName;

    private static Set<String> visitedLinks = ConcurrentHashMap.newKeySet();
    private static final Queue<String> queueLinks = new ConcurrentLinkedQueue<>();
    private static final HashMap<Integer, String> lastError = new HashMap<>();
    private SiteModel siteModel;
    private Boolean isUnsuccessfulResult = null;
    private final Map<String, Boolean> indexingStatus;

    public SiteIndexingServiceImpl() {
        forkJoinPool = new ForkJoinPool();
        indexingStatus = new HashMap<>();
    }

    public boolean startIndexingSite() {
        sitesList.getSites().forEach((site) -> {
            indexingStatus.put(site.getUrl(), false);
            deleteOldDataByUrlSite(site.getUrl());
            siteModel = siteModelUtil.createNewSiteModel(site, StatusSiteIndex.INDEXING);
            log.info("Запущена индексация сайта: " + site.getUrl());
            isUnsuccessfulResult = startParsingSite(site.getUrl());
            if (isUnsuccessfulResult) {
                siteModelUtil.updateStatusSiteModelToFailed(siteModel, StatusSiteIndex.FAILED,
                        LocalDateTime.now(), "Ошибка индексации сайта");
                indexingStatus.put(site.getUrl(), false);
                log.error("Ошибка индексации сайта");
            } else {
                indexingStatus.put(site.getUrl(), true);
                log.info("Процесс индексации сайта завершен успешно: " + site.getUrl());
                log.info("Страниц на сайте - " + siteModel.getName() + " - " + countPagesBySiteId(siteModel));
            }
        });
        return isUnsuccessfulResult;
    }

    public boolean startParsingSite(String url) {
        String[] tmpArray = url.split("/");
        domainName = tmpArray[2];
        queueLinks.add(url);
        List<ParserSiteUtil> taskListLinkParsers = new ArrayList<>();
        for (int threads = 0; threads < Runtime.getRuntime().availableProcessors(); ++threads) {
            ParserSiteUtil parser = new ParserSiteUtil(queueLinks, visitedLinks, siteRepository,
                    pageRepository, siteModel, lastError, siteModelUtil, pageModelUtil, lemmaModelUtil, lemmaFinderUtil);
            taskListLinkParsers.add(parser);
        }
        if (!forkJoinPool.isShutdown()) {
            taskListLinkParsers.forEach(forkJoinPool::invoke);
            taskListLinkParsers.forEach(parserSiteUtil -> {
                if (parserSiteUtil.getStatus().equals("working")) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException interruptedException) {
                        throw new RuntimeException(interruptedException);
                    }
                }
            });
            return false;
        } else {
            log.warn("Ошибка индексации");
            return true;
        }
    }

    public Integer countPagesBySiteId(SiteModel siteModel) {
        return (pageRepository.findAllPagesBySiteId(siteModel).size());
    }

    public void deleteOldDataByUrlSite(String urlSite) {
        List<SiteModel> listModelsToDelete = siteRepository.findSiteModelsByUrl(urlSite);
        log.info("В репозитории уже находятся SiteModel с указанным URL - " + listModelsToDelete.size());
        if (!listModelsToDelete.isEmpty()) {
            for (SiteModel siteModelToDelete : listModelsToDelete) {
                siteRepository.delete(siteModelToDelete);
                log.info("Успешно удалены устаревшие данные по сайту - " + siteModelToDelete.getUrl());
            }
        }
    }

    @Override
    public boolean isIndexing() {
        List<SiteModel> listOfIndexedSites = new ArrayList<>();
        siteRepository.findAll().forEach(siteModel -> {
            if (siteModel.getStatusSiteIndex() == StatusSiteIndex.INDEXING) {
                listOfIndexedSites.add(siteModel);
            }
        });
        return !listOfIndexedSites.isEmpty();
    }

    @Override
    public boolean stopIndexingSite() {
        if (isIndexing()) {
            siteRepository.findAll().forEach(siteModel -> {
                if (siteModel.getStatusSiteIndex() == StatusSiteIndex.INDEXING) {
                    siteModelUtil.updateStatusSiteModelToFailed(siteModel, StatusSiteIndex.FAILED,
                            LocalDateTime.now(), "Индексация остановлена пользователем!");
                }
            });
            forkJoinPool.shutdownNow();
            queueLinks.clear();
            log.warn("Индексация остановлена пользователем!");
            return true;
        } else {
            log.info("Начните индексацию перед остановкой!");
        }
        return false;
    }
}