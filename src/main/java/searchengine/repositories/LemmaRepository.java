package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.LemmaModel;
import searchengine.model.SiteModel;

public interface LemmaRepository extends CrudRepository<LemmaModel, Integer> {
    LemmaModel findByLemma(String lemma);
    LemmaModel findByLemmaAndSiteId(String lemma, SiteModel siteModel);
}