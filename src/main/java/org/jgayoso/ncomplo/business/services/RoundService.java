package org.jgayoso.ncomplo.business.services;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jgayoso.ncomplo.business.entities.Competition;
import org.jgayoso.ncomplo.business.entities.Round;
import org.jgayoso.ncomplo.business.entities.repositories.CompetitionRepository;
import org.jgayoso.ncomplo.business.entities.repositories.RoundRepository;
import org.jgayoso.ncomplo.business.util.IterableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoundService {

  @Autowired private CompetitionRepository competitionRepository;

  @Autowired private RoundRepository roundRepository;

  public RoundService() {
    super();
  }

  @Transactional
  public Round find(final Integer id) {
    return this.roundRepository.findById(id).orElse(null);
  }

  @Transactional
  public List<Round> findAll(final Integer competitionId) {
    final List<Round> rounds = IterableUtils.toList(this.roundRepository.findByCompetitionId(competitionId));
    Collections.sort(rounds);
    return rounds;
  }

  @Transactional
  public Round findByCompetitionIdAndName(final Integer competitionId, final String name) {
    return this.roundRepository.findByCompetitionIdAndName(competitionId, name);
  }

  @Transactional
  public Round save(
      final Integer id,
      final Integer competitionId,
      final String name,
      final Map<String, String> namesByLang,
      final Integer order) {

    final Competition competition = this.competitionRepository.findById(competitionId).orElse(null);

    final Round round = (id == null ? new Round() : this.roundRepository.findById(id).orElse(null));

    round.setCompetition(competition);
    round.setName(name);
    round.getNamesByLang().clear();
    round.getNamesByLang().putAll(namesByLang);
    round.setOrder(order);

    if (id == null) {
      competition.getRounds().add(round);
      return this.roundRepository.save(round);
    }
    return round;
  }

  @Transactional
  public void delete(final Integer roundId) {

    final Round round = this.roundRepository.findById(roundId).orElse(null);
    final Competition competition = round.getCompetition();

    competition.getRounds().remove(round);
  }

  @Transactional
  public void createDefaults(Integer competitionId) {
    final Competition competition = this.competitionRepository.findById(competitionId).orElse(null);

    Round groupsRound = new Round();
    groupsRound.setCompetition(competition);
    groupsRound.setName(
        competition.getCompetitionParserProperties() != null
            ? competition.getCompetitionParserProperties().getGroupsName()
            : "Groups");
    groupsRound.setOrder(1);
    competition.getRounds().add(groupsRound);
    this.roundRepository.save(groupsRound);

    Round roundOf16Round = new Round();
    roundOf16Round.setCompetition(competition);
    roundOf16Round.setName(
        competition.getCompetitionParserProperties() != null
            ? competition.getCompetitionParserProperties().getRoundOf16Name()
            : "Round Of 16");
    roundOf16Round.setOrder(2);
    competition.getRounds().add(roundOf16Round);
    this.roundRepository.save(roundOf16Round);

    Round quarterFinalsRound = new Round();
    quarterFinalsRound.setCompetition(competition);
    quarterFinalsRound.setName(
        competition.getCompetitionParserProperties() != null
            ? competition.getCompetitionParserProperties().getQuarterFinalsName()
            : "Quarter Finals");
    quarterFinalsRound.setOrder(3);
    competition.getRounds().add(quarterFinalsRound);
    this.roundRepository.save(quarterFinalsRound);

    Round semiFinalsRound = new Round();
    semiFinalsRound.setCompetition(competition);
    semiFinalsRound.setName(
        competition.getCompetitionParserProperties() != null
            ? competition.getCompetitionParserProperties().getSemiFinalsName()
            : "SemiFinals");
    semiFinalsRound.setOrder(4);
    competition.getRounds().add(semiFinalsRound);
    this.roundRepository.save(semiFinalsRound);

    Round finalRound = new Round();
    finalRound.setCompetition(competition);
    finalRound.setName(
        competition.getCompetitionParserProperties() != null
            ? competition.getCompetitionParserProperties().getFinalName()
            : "Final");
    finalRound.setOrder(4);
    competition.getRounds().add(finalRound);
    this.roundRepository.save(finalRound);
  }
}
