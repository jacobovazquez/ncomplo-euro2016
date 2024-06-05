package org.jgayoso.ncomplo.business.services;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jgayoso.ncomplo.business.entities.BetType;
import org.jgayoso.ncomplo.business.entities.Competition;
import org.jgayoso.ncomplo.business.entities.repositories.BetTypeRepository;
import org.jgayoso.ncomplo.business.entities.repositories.CompetitionRepository;
import org.jgayoso.ncomplo.business.util.I18nNamedEntityComparator;
import org.jgayoso.ncomplo.business.util.IterableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BetTypeService {

  @Autowired private CompetitionRepository competitionRepository;

  @Autowired private BetTypeRepository betTypeRepository;

  @Value("${ncomplo.bettype.defaultGroupsSpec}")
  private String defaultGroupsSpec;

  @Value("${ncomplo.bettype.defaultRoundOf16Spec}")
  private String defaultRoundOf16Spec;

  @Value("${ncomplo.bettype.defaultQuarterFinalsSpec}")
  private String defaultQuarterFinalsSpec;

  @Value("${ncomplo.bettype.defaultSemiFinalsSpec}")
  private String defaultSemiFinalsSpec;

  @Value("${ncomplo.bettype.defaultFinalSpec}")
  private String defaultFinalSpec;

  public BetTypeService() {
    super();
  }

  @Transactional
  public BetType find(final Integer id) {
    return this.betTypeRepository.findById(id).orElse(null);
  }

  public BetType findByName(final Integer competitionId, String name) {
    return this.betTypeRepository.findByCompetitionIdAndName(competitionId, name);
  }

  @Transactional
  public List<BetType> findAllOrderByName(final Integer competitionId, final Locale locale) {
    final List<BetType> betTypes = IterableUtils.toList(this.betTypeRepository.findByCompetitionId(competitionId));
    Collections.sort(betTypes, new I18nNamedEntityComparator(locale));
    return betTypes;
  }

  @Transactional
  public BetType save(
      final Integer id,
      final Integer competitionId,
      final String name,
      final Map<String, String> namesByLang,
      final String spec,
      final boolean sidesMatter,
      final boolean scoreMatter,
      final boolean resultMatter) {

    final Competition competition = this.competitionRepository.findById(competitionId).orElse(null);

    final BetType betType = (id == null ? new BetType() : this.betTypeRepository.findById(id).orElse(null));

    betType.setCompetition(competition);
    betType.setName(name);
    betType.getNamesByLang().clear();
    betType.getNamesByLang().putAll(namesByLang);
    betType.setSpec(spec);
    betType.setSidesMatter(sidesMatter);
    betType.setScoreMatter(scoreMatter);
    betType.setResultMatter(resultMatter);

    if (id == null) {
      competition.getBetTypes().add(betType);
      return this.betTypeRepository.save(betType);
    }
    return betType;
  }

  @Transactional
  public void delete(final Integer betTypeId) {

    final BetType betType = this.betTypeRepository.findById(betTypeId).orElse(null);
    final Competition competition = betType.getCompetition();

    competition.getBetTypes().remove(betType);
  }

  @Transactional
  public void createDefaults(Integer competitionId) {

    final Competition competition = this.competitionRepository.findById(competitionId).orElse(null);
    BetType groupsBetType = new BetType();
    groupsBetType.setCompetition(competition);
    groupsBetType.setName(
        competition.getCompetitionParserProperties() != null
            ? competition.getCompetitionParserProperties().getGroupsName()
            : "Groups");
    groupsBetType.setResultMatter(true);
    groupsBetType.setScoreMatter(true);
    groupsBetType.setSidesMatter(false);
    groupsBetType.setSpec(defaultGroupsSpec);
    competition.getBetTypes().add(groupsBetType);
    this.betTypeRepository.save(groupsBetType);

    BetType roundOf16BetType = new BetType();
    roundOf16BetType.setCompetition(competition);
    roundOf16BetType.setName(
        competition.getCompetitionParserProperties() != null
            ? competition.getCompetitionParserProperties().getRoundOf16Name()
            : "Round Of 16");
    roundOf16BetType.setResultMatter(false);
    roundOf16BetType.setScoreMatter(false);
    roundOf16BetType.setSidesMatter(true);
    roundOf16BetType.setSpec(defaultRoundOf16Spec);
    competition.getBetTypes().add(roundOf16BetType);
    this.betTypeRepository.save(roundOf16BetType);

    BetType quarterFinalsBetType = new BetType();
    quarterFinalsBetType.setCompetition(competition);
    quarterFinalsBetType.setName(
        competition.getCompetitionParserProperties() != null
            ? competition.getCompetitionParserProperties().getQuarterFinalsName()
            : "Quarter Finals");
    quarterFinalsBetType.setResultMatter(false);
    quarterFinalsBetType.setScoreMatter(false);
    quarterFinalsBetType.setSidesMatter(true);
    quarterFinalsBetType.setSpec(defaultQuarterFinalsSpec);
    competition.getBetTypes().add(quarterFinalsBetType);
    this.betTypeRepository.save(quarterFinalsBetType);

    BetType semiFinalsBetType = new BetType();
    semiFinalsBetType.setCompetition(competition);
    semiFinalsBetType.setName(
        competition.getCompetitionParserProperties() != null
            ? competition.getCompetitionParserProperties().getSemiFinalsName()
            : "SemiFinals");
    semiFinalsBetType.setResultMatter(false);
    semiFinalsBetType.setScoreMatter(false);
    semiFinalsBetType.setSidesMatter(true);
    semiFinalsBetType.setSpec(defaultSemiFinalsSpec);
    competition.getBetTypes().add(semiFinalsBetType);
    this.betTypeRepository.save(semiFinalsBetType);

    BetType finalBetType = new BetType();
    finalBetType.setCompetition(competition);
    finalBetType.setName(
        competition.getCompetitionParserProperties() != null
            ? competition.getCompetitionParserProperties().getFinalName()
            : "Final");
    finalBetType.setResultMatter(true);
    finalBetType.setScoreMatter(false);
    finalBetType.setSidesMatter(true);
    finalBetType.setSpec(defaultFinalSpec);
    competition.getBetTypes().add(finalBetType);
    this.betTypeRepository.save(finalBetType);
  }
}
