package org.jgayoso.ncomplo.business.services;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgayoso.ncomplo.business.entities.Competition;
import org.jgayoso.ncomplo.business.entities.CompetitionParserProperties;
import org.jgayoso.ncomplo.business.entities.repositories.CompetitionParserPropertiesRepository;
import org.jgayoso.ncomplo.business.entities.repositories.CompetitionRepository;
import org.jgayoso.ncomplo.business.util.I18nNamedEntityComparator;
import org.jgayoso.ncomplo.business.util.IterableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionService {

  private static final Log logger = LogFactory.getLog(CompetitionService.class);

  @Autowired private CompetitionRepository competitionRepository;
  @Autowired private CompetitionParserPropertiesRepository competitionParserPropertiesRepository;

  @Autowired private GameSideService gameSideService;

  @Autowired private GameService gameService;

  @Autowired private BetTypeService betTypeService;
  @Autowired private RoundService roundService;

  public CompetitionService() {
    super();
  }

  @Transactional
  public Competition find(final Integer id) {
    return this.competitionRepository.findById(id).orElse(null);
  }

  @Transactional
  public List<Competition> findAll(final Locale locale) {
    final List<Competition> competitions = IterableUtils.toList(this.competitionRepository.findAll());
    Collections.sort(competitions, new I18nNamedEntityComparator(locale));
    return competitions;
  }

  @Transactional
  public Competition save(
      final Integer id,
      final String name,
      final Map<String, String> namesByLang,
      final boolean active,
      final String updaterUri,
      final CompetitionParserProperties competitionParserProperties) {

    final Competition competition = (id == null ? new Competition() : this.competitionRepository.findById(id).orElse(null));

    competition.setName(name);
    competition.getNamesByLang().clear();
    competition.getNamesByLang().putAll(namesByLang);
    competition.setActive(active);
    competition.setUpdaterUri(updaterUri);

    if (competition.getCompetitionParserProperties() == null && competitionParserProperties != null) {
      competitionParserProperties.setCompetition(competition);
      competition.setCompetitionParserProperties(
          competitionParserPropertiesRepository.save(competitionParserProperties));
    } else if (competition.getCompetitionParserProperties() != null && competitionParserProperties != null) {
      // Update properties values
      competition.getCompetitionParserProperties().updateProperties(competitionParserProperties);
    }
    if (id == null) {
      return this.competitionRepository.save(competition);
    }
    return competition;
  }

  @Transactional
  public void delete(final Integer competitionId) {
    this.competitionRepository.deleteById(competitionId);
  }
}
