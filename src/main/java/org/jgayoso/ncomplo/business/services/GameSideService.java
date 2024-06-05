package org.jgayoso.ncomplo.business.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jgayoso.ncomplo.business.entities.Competition;
import org.jgayoso.ncomplo.business.entities.GameSide;
import org.jgayoso.ncomplo.business.entities.repositories.CompetitionRepository;
import org.jgayoso.ncomplo.business.entities.repositories.GameSideRepository;
import org.jgayoso.ncomplo.business.util.ExcelProcessor;
import org.jgayoso.ncomplo.business.util.I18nNamedEntityComparator;
import org.jgayoso.ncomplo.business.util.IterableUtils;
import org.jgayoso.ncomplo.exceptions.CompetitionParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameSideService {

  private static final Log logger = LogFactory.getLog(GameSideService.class);

  @Autowired private CompetitionRepository competitionRepository;

  @Autowired private GameSideRepository gameSideRepository;

  public GameSideService() {
    super();
  }

  @Transactional
  public GameSide find(final Integer id) {
    return this.gameSideRepository.findById(id).orElse(null);
  }

  @Transactional
  public List<GameSide> findAll(final Integer competitionId, final Locale locale) {
    final List<GameSide> gameSides = IterableUtils.toList(this.gameSideRepository.findByCompetitionId(competitionId));
    Collections.sort(gameSides, new I18nNamedEntityComparator(locale));
    return gameSides;
  }

  @Transactional
  public GameSide save(
      final Integer id,
      final Integer competitionId,
      final String name,
      final Map<String, String> namesByLang,
      final String code) {

    final Competition competition = this.competitionRepository.findById(competitionId).orElse(null);

    final GameSide gameSide = (id == null ? new GameSide() : this.gameSideRepository.findById(id).orElse(null));

    gameSide.setCompetition(competition);
    gameSide.setName(name);
    gameSide.getNamesByLang().clear();
    gameSide.getNamesByLang().putAll(namesByLang);
    gameSide.setCode(code);

    if (id == null) {
      competition.getGameSides().add(gameSide);
      return this.gameSideRepository.save(gameSide);
    }
    return gameSide;
  }

  @Transactional
  public void deleteAll(final Integer competitionId) {

    Competition competition = this.competitionRepository.findById(competitionId).orElse(null);

    for (GameSide gameSide : new HashSet<>(competition.getGameSides())) {
      competition.getGameSides().remove(gameSide);
    }
  }

  @Transactional
  public void delete(final Integer gameSideId) {

    final GameSide gameSide = this.gameSideRepository.findById(gameSideId).orElse(null);
    final Competition competition = gameSide.getCompetition();

    competition.getGameSides().remove(gameSide);
  }

  @Transactional
  public void processFile(Integer competitionId, String login, File competitionFile) throws CompetitionParserException {
    Competition competition = this.competitionRepository.findById(competitionId).orElse(null);
    if (competition == null) {
      logger.error("Not possible to processFile, competition not found");
      return;
    }

    logger.info("User " + login + " generating teams for competition " + competitionId);

    try (FileInputStream fis = new FileInputStream(competitionFile);
        XSSFWorkbook book = new XSSFWorkbook(fis)) {
      Map<String, GameSide> gamesSidesByName = ExcelProcessor.processCompetitionGameSides(competition, book, this);
      logger.debug("Created " + gamesSidesByName.size() + " teams");
    } catch (IOException e) {
      logger.error("Not possible to processFile, IOException", e);
    }
  }
}
