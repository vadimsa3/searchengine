package searchengine.utilities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.model.SiteModel;
import searchengine.model.StatusSiteIndex;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;

@Service
public class SiteModelUtil {

    @Autowired
    private SiteRepository siteRepository;

    public SiteModel createNewSiteModel(Site site, StatusSiteIndex statusSiteIndex) {
        SiteModel siteModel = new SiteModel();
        siteModel.setStatusSiteIndex(statusSiteIndex);
        siteModel.setStatusTime(LocalDateTime.now());
        siteModel.setLastError(null);
        siteModel.setUrl(site.getUrl());
        siteModel.setName(site.getName());
        siteRepository.save(siteModel);
        return siteModel;
    }

    public void updateSiteModel(SiteModel siteModel, StatusSiteIndex statusSiteIndex,
                                LocalDateTime timeStatus, String lastError) {
        siteModel.setStatusSiteIndex(statusSiteIndex);
        siteModel.setStatusTime(timeStatus);
        siteModel.setLastError(lastError);
        siteRepository.save(siteModel);
    }

    public void updateStatusSiteModelToFailed(SiteModel siteModel, StatusSiteIndex statusSiteIndex,
                                              LocalDateTime timeStatus, String errorMessage) {
        siteModel.setStatusSiteIndex(statusSiteIndex);
        siteModel.setStatusTime(timeStatus);
        siteModel.setLastError(errorMessage);
        siteRepository.save(siteModel);
    }
}
