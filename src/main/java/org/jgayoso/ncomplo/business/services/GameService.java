package org.jgayoso.ncomplo.business.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jgayoso.ncomplo.business.entities.BetType;
import org.jgayoso.ncomplo.business.entities.Competition;
import org.jgayoso.ncomplo.business.entities.CompetitionParserProperties;
import org.jgayoso.ncomplo.business.entities.Game;
import org.jgayoso.ncomplo.business.entities.Game.GameComparator;
import org.jgayoso.ncomplo.business.entities.GameSide;
import org.jgayoso.ncomplo.business.entities.League;
import org.jgayoso.ncomplo.business.entities.Round;
import org.jgayoso.ncomplo.business.entities.repositories.BetTypeRepository;
import org.jgayoso.ncomplo.business.entities.repositories.CompetitionRepository;
import org.jgayoso.ncomplo.business.entities.repositories.GameRepository;
import org.jgayoso.ncomplo.business.entities.repositories.GameSideRepository;
import org.jgayoso.ncomplo.business.entities.repositories.RoundRepository;
import org.jgayoso.ncomplo.business.util.ExcelProcessor;
import org.jgayoso.ncomplo.business.util.IterableUtils;
import org.jgayoso.ncomplo.exceptions.CompetitionParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class GameService {

  private static final Log logger = LogFactory.getLog(GameService.class);

  @Autowired private CompetitionRepository competitionRepository;

  @Autowired private GameRepository gameRepository;

  @Autowired private BetTypeRepository betTypeRepository;

  @Autowired private RoundRepository roundRepository;

  @Autowired private GameSideRepository gameSideRepository;

  @Autowired private GameSideService gameSideService;

  @Autowired private BetTypeService betTypeService;
  @Autowired private RoundService roundService;

  @Autowired private LeagueService leagueService;

  public GameService() {
    super();
  }

  @Transactional
  public Game find(final Integer id) {
    return this.gameRepository.findById(id).orElse(null);
  }

  @Transactional
  public List<Game> findAll(final Integer competitionId, final Locale locale) {
    final List<Game> rounds = IterableUtils.toList(this.gameRepository.findByCompetitionId(competitionId));
    Collections.sort(rounds, new GameComparator(locale));
    return rounds;
  }

  @Transactional
  public Game save(
      final Integer id,
      final Integer competitionId,
      final Date date,
      final String name,
      final Map<String, String> namesByLang,
      final Integer defaultBetTypeId,
      final Integer roundId,
      final Integer order,
      final Integer gameSideAId,
      final Integer gameSideBId,
      final Integer scoreA,
      final Integer scoreB) {

    final Competition competition = this.competitionRepository.findById(competitionId).orElse(null);

    final Game game = (id == null ? new Game() : this.gameRepository.findById(id).orElse(null));

    final GameSide gameSideA = (gameSideAId == null ? null : this.gameSideRepository.findById(gameSideAId).orElse(null));
    final GameSide gameSideB = (gameSideBId == null ? null : this.gameSideRepository.findById(gameSideBId).orElse(null));

    game.setCompetition(competition);
    game.setDate(date);
    game.setName(name);
    game.getNamesByLang().clear();
    game.getNamesByLang().putAll(namesByLang);
    game.setDefaultBetType(this.betTypeRepository.findById(defaultBetTypeId).orElse(null));
    game.setRound(this.roundRepository.findById(roundId).orElse(null));
    game.setOrder(order);
    game.setGameSideA(gameSideA);
    game.setGameSideB(gameSideB);
    game.setScoreA(scoreA);
    game.setScoreB(scoreB);

    if (id == null) {
      competition.getGames().add(game);
      return this.gameRepository.save(game);
    }

    return game;
  }

  @Transactional
  public void deleteAll(Integer competitionId) {
    final Competition competition = this.competitionRepository.findById(competitionId).orElse(null);
    for (Game game : new HashSet<>(competition.getGames())) {
      competition.getGames().remove(game);
    }
  }

  @Transactional
  public void delete(final Integer gameId) {

    final Game game = this.gameRepository.findById(gameId).orElse(null);
    final Competition competition = game.getCompetition();

    competition.getGames().remove(game);
  }

  public List<Game> findNextGames(final Integer leagueId) {
    final League league = this.leagueService.find(leagueId);
    final Date today = new Date();
    final Date todayMorning = DateUtils.truncate(today, Calendar.DATE);
    final Date todayEvening = DateUtils.addSeconds(DateUtils.addMinutes(DateUtils.addHours(todayMorning, 23), 59), 59);
    final List<Game> todayGames =
        this.gameRepository.findByCompetitionAndDateBetweenOrderByDate(
            league.getCompetition(), todayMorning, todayEvening);
    if (!CollectionUtils.isEmpty(todayGames)) {
      // Collections.sort(todayGames, new GameOrderComparator());
      return todayGames;
    }

    final Date nextWeek = DateUtils.addDays(todayMorning, 7);
    final List<Game> games =
        this.gameRepository.findByCompetitionAndDateBetweenOrderByDate(league.getCompetition(), todayMorning, nextWeek);
    if (CollectionUtils.isEmpty(games)) {
      return null;
    }
    final Date date = games.get(0).getDate();
    final List<Game> gamesToReturn = new ArrayList<>();
    for (final Game game : games) {
      if (DateUtils.isSameDay(date, game.getDate())) {
        gamesToReturn.add(game);
      } else {
        break;
      }
    }

    return gamesToReturn;
  }

  @Transactional
  public void processFile(Integer competitionId, String login, File competitionFile) throws CompetitionParserException {
    final Competition competition = this.competitionRepository.findById(competitionId).orElse(null);
    CompetitionParserProperties competitionParserProperties = competition.getCompetitionParserProperties();
    if (competitionParserProperties == null) {
      logger.error("Not possible to processFile, competition properties not found");
      throw new CompetitionParserException("Competition properties not found");
    }

    logger.info("User " + login + " generating games for competition " + competition.getName());

    try (FileInputStream fis = new FileInputStream(competitionFile);
        XSSFWorkbook book = new XSSFWorkbook(fis)) {
      FormulaEvaluator evaluator = book.getCreationHelper().createFormulaEvaluator();

      if (StringUtils.isEmpty(competitionParserProperties.getGamesSheetName())) {
        throw new CompetitionParserException("Missing games property games sheet name");
      }
      final XSSFSheet games = book.getSheet(competitionParserProperties.getGamesSheetName());
      if (games == null) {
        throw new CompetitionParserException("Games sheet not found");
      }

      Iterable<GameSide> gameSides = gameSideRepository.findAll();
      Map<String, GameSide> gameSideMap = new HashMap<>();
      gameSides.forEach(gameSide -> gameSideMap.put(gameSide.getName(), gameSide));

      BetType groupsBetType = betTypeService.findByName(competitionId, competitionParserProperties.getGroupsName());
      Round groupsRound =
          roundService.findByCompetitionIdAndName(competitionId, competitionParserProperties.getGroupsName());
      if (groupsBetType == null || groupsRound == null) {
        throw new CompetitionParserException("Groups BetType or Round are null");
      }
      int gameIndex =
          ExcelProcessor.processGroupGames(
              competition, games, evaluator, groupsBetType, groupsRound, gameSideMap, this);

      // Round of 16
      BetType roundOf16BetType =
          betTypeService.findByName(competitionId, competitionParserProperties.getRoundOf16Name());
      Round roundOf16Round =
          roundService.findByCompetitionIdAndName(competitionId, competitionParserProperties.getRoundOf16Name());
      if (roundOf16BetType == null || roundOf16Round == null) {
        throw new CompetitionParserException("Round of 16 BetType or Round are null");
      }
      gameIndex =
          ExcelProcessor.processPlayoffGames(
              competition,
              games,
              evaluator,
              roundOf16BetType,
              roundOf16Round,
              gameIndex,
              8,
              competitionParserProperties.getRoundOf16GamesJumpSize(),
              competitionParserProperties.getRoundOf16GamesColumnName(),
              competitionParserProperties.getRoundOf16GamesStartIndex(),
              this);

      // Quarter Finals
      BetType quarterFinalsBetType =
          betTypeService.findByName(competitionId, competitionParserProperties.getQuarterFinalsName());
      Round quarterFinalsRound =
          roundService.findByCompetitionIdAndName(competitionId, competitionParserProperties.getQuarterFinalsName());
      if (quarterFinalsBetType == null || quarterFinalsRound == null) {
        throw new CompetitionParserException("Quarter Finals BetType or Round are null");
      }
      gameIndex =
          ExcelProcessor.processPlayoffGames(
              competition,
              games,
              evaluator,
              quarterFinalsBetType,
              quarterFinalsRound,
              gameIndex,
              4,
              competitionParserProperties.getQuarteFinalsGamesJumpSize(),
              competitionParserProperties.getQuarteFinalsGamesColumnName(),
              competitionParserProperties.getQuarteFinalsGamesStartIndex(),
              this);

      // Semifinals
      BetType semifinalsBetType =
          betTypeService.findByName(competitionId, competitionParserProperties.getSemiFinalsName());
      Round semifinalsRound =
          roundService.findByCompetitionIdAndName(competitionId, competitionParserProperties.getSemiFinalsName());
      if (semifinalsBetType == null || semifinalsRound == null) {
        throw new CompetitionParserException("Semifinals BetType or Round are null");
      }
      gameIndex =
          ExcelProcessor.processPlayoffGames(
              competition,
              games,
              evaluator,
              semifinalsBetType,
              semifinalsRound,
              gameIndex,
              2,
              competitionParserProperties.getSemiFinalsGamesJumpSize(),
              competitionParserProperties.getSemiFinalsGamesColumnName(),
              competitionParserProperties.getSemiFinalsGamesStartIndex(),
              this);

      // Final
      BetType finalBetType = betTypeService.findByName(competitionId, competitionParserProperties.getFinalName());
      Round finalRound =
          roundService.findByCompetitionIdAndName(competitionId, competitionParserProperties.getFinalName());
      if (finalBetType == null || finalRound == null) {
        throw new CompetitionParserException("Final BetType or Round are null");
      }
      ExcelProcessor.processPlayoffGames(
          competition,
          games,
          evaluator,
          finalBetType,
          finalRound,
          gameIndex,
          1,
          1,
          competitionParserProperties.getFinalGamesColumnName(),
          competitionParserProperties.getFinalGamesStartIndex(),
          this);

    } catch (IOException e) {
      logger.error("Not possible to processFile, IOException", e);
    }
  }
}
