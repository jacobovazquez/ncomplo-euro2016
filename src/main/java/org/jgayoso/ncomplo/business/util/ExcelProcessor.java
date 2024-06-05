package org.jgayoso.ncomplo.business.util;

import com.neovisionaries.i18n.CountryCode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jgayoso.ncomplo.business.entities.BetType;
import org.jgayoso.ncomplo.business.entities.Competition;
import org.jgayoso.ncomplo.business.entities.CompetitionParserProperties;
import org.jgayoso.ncomplo.business.entities.Game;
import org.jgayoso.ncomplo.business.entities.GameSide;
import org.jgayoso.ncomplo.business.entities.Round;
import org.jgayoso.ncomplo.business.services.BetService;
import org.jgayoso.ncomplo.business.services.GameService;
import org.jgayoso.ncomplo.business.services.GameSideService;
import org.jgayoso.ncomplo.business.views.BetView;
import org.jgayoso.ncomplo.exceptions.CompetitionParserException;
import org.springframework.util.StringUtils;

public class ExcelProcessor {

  private static final Log logger = LogFactory.getLog(ExcelProcessor.class);

  public static int processGroupGamesBets(
      Integer leagueId,
      String login,
      Competition competition,
      XSSFSheet sheet,
      Map<Integer, Game> gamesByOrder,
      Map<Integer, Integer> betIdsByGameId,
      Map<Integer, BetView> betsByGameId,
      BetService betService) {

    int matchNumber = 1;
    CompetitionParserProperties parserProperties = competition.getCompetitionParserProperties();
    for (int rowIndex = parserProperties.getGroupGamesStartIndex();
        rowIndex < parserProperties.getGroupGamesStartIndex() + parserProperties.getGroupGamesNumber();
        rowIndex++) {

      final BetView betView =
          ExcelProcessor.processGroupsGameBet(
              parserProperties,
              sheet,
              rowIndex,
              matchNumber,
              parserProperties.getGroupGamesColumnName(),
              gamesByOrder,
              betsByGameId);
      // If betId is not null, update the current bet instance
      final Integer betId = betIdsByGameId.get(betView.getGameId());

      betService.save(
          betId,
          leagueId,
          login,
          betView.getGameId(),
          betView.getGameSideAId(),
          betView.getGameSideBId(),
          betView.getScoreA(),
          betView.getScoreB());
      matchNumber++;
    }
    return matchNumber;
  }

  public static BetView processGroupsGameBet(
      CompetitionParserProperties parserProperties,
      final XSSFSheet sheet,
      final int rowIndex,
      final int matchNumber,
      final String columnName,
      final Map<Integer, Game> gamesByOrder,
      final Map<Integer, BetView> betsByGameId) {

    final CellReference cellReference = new CellReference(columnName + rowIndex);
    final Row row = sheet.getRow(cellReference.getRow());
    final Cell homeResultCell = row.getCell(cellReference.getCol() + parserProperties.getGroupGamesHomeIndex() + 1);
    final Cell awayResultCell = row.getCell(cellReference.getCol() + parserProperties.getGroupGamesHomeIndex() + 2);

    final int homeResult = Double.valueOf(homeResultCell.getNumericCellValue()).intValue();
    final int awayResult = Double.valueOf(awayResultCell.getNumericCellValue()).intValue();

    final Game game = gamesByOrder.get(matchNumber);
    final BetView betView = betsByGameId.get(game.getId());
    betView.setScoreA(homeResult);
    betView.setScoreB(awayResult);
    return betView;
  }

  public static List<BetView> processPlayOffGamesBets(
      Integer leagueId,
      String login,
      final XSSFSheet sheet,
      int matchNumber,
      final String columnName,
      final int startIndex,
      final int numberOfGames,
      final int jumpSize,
      final Map<Integer, Game> gamesByOrder,
      final Map<Integer, Integer> betIdsByGameId,
      final Map<Integer, BetView> betsByGameId,
      final Map<String, GameSide> gameSidesByName,
      BetService betService) {

    final List<BetView> betViews = new ArrayList<>();
    for (int i = 0; i < numberOfGames; i++) {
      final BetView betView =
          processPlayOffGameBet(
              sheet, startIndex + (i * jumpSize), matchNumber, columnName, gamesByOrder, betsByGameId, gameSidesByName);
      // If betId is not null, update the current bet instance
      final Integer betId = betIdsByGameId.get(betView.getGameId());
      betService.save(
          betId,
          leagueId,
          login,
          betView.getGameId(),
          betView.getGameSideAId(),
          betView.getGameSideBId(),
          betView.getScoreA(),
          betView.getScoreB());

      betViews.add(betView);
      matchNumber++;
    }

    return betViews;
  }

  public static BetView processPlayOffGameBet(
      final XSSFSheet sheet,
      final int rowIndex,
      final int matchNumber,
      final String columnName,
      final Map<Integer, Game> gamesByOrder,
      final Map<Integer, BetView> betsByGameId,
      final Map<String, GameSide> gameSidesByName) {

    // rowIndex points to Date. Go to the next row
    final CellReference homeCellReference = new CellReference(columnName + (rowIndex + 1));

    final Row homeRow = sheet.getRow(homeCellReference.getRow());

    final Cell homeTeamCell = homeRow.getCell(homeCellReference.getCol() + 1);
    final Cell homeResultCell = homeRow.getCell(homeCellReference.getCol() + 2);
    final Cell extraTimeHomeResultCell = homeRow.getCell(homeCellReference.getCol() + 3);
    final Cell penaltiesHomeResultCell = homeRow.getCell(homeCellReference.getCol() + 4);
    final String homeTeamName = homeTeamCell.getStringCellValue();
    int homeResult = Double.valueOf(homeResultCell.getNumericCellValue()).intValue();
    final int extraTimeHomeResult = Double.valueOf(extraTimeHomeResultCell.getNumericCellValue()).intValue();
    final int penaltiesHomeResult = Double.valueOf(penaltiesHomeResultCell.getNumericCellValue()).intValue();

    final CellReference awayCellReference = new CellReference(columnName + (rowIndex + 2));
    final Row awayRow = sheet.getRow(awayCellReference.getRow());

    final Cell awayTeamCell = awayRow.getCell(awayCellReference.getCol() + 1);
    final Cell awayResultCell = awayRow.getCell(awayCellReference.getCol() + 2);
    final Cell extraTimeAwayResultCell = awayRow.getCell(homeCellReference.getCol() + 3);
    final Cell penaltiesAwayResultCell = awayRow.getCell(homeCellReference.getCol() + 4);
    final String awayTeamName = awayTeamCell.getStringCellValue();
    int awayResult = Double.valueOf(awayResultCell.getNumericCellValue()).intValue();
    final int extraTimeAwayResult = Double.valueOf(extraTimeAwayResultCell.getNumericCellValue()).intValue();
    final int penaltiesAwayResult = Double.valueOf(penaltiesAwayResultCell.getNumericCellValue()).intValue();

    final Integer gameSideAId =
        gameSidesByName.get(homeTeamName) == null ? null : gameSidesByName.get(homeTeamName).getId();
    final Integer gameSideBId =
        gameSidesByName.get(awayTeamName) == null ? null : gameSidesByName.get(awayTeamName).getId();

    final Game game = gamesByOrder.get(matchNumber);
    final BetView betView = betsByGameId.get(game.getId());

    final BetType betType = game.getDefaultBetType();
    betView.setGameSideAId(gameSideAId);
    betView.setGameSideBId(gameSideBId);

    if (homeResult == awayResult) {
      // We need to parse the extra time and maybe penalties results
      if (extraTimeHomeResult == extraTimeAwayResult) {
        // Penalties
        homeResult = penaltiesHomeResult;
        awayResult = penaltiesAwayResult;
      } else {
        homeResult = extraTimeHomeResult;
        awayResult = extraTimeAwayResult;
      }
    }

    betView.setScoreA(homeResult);
    betView.setScoreB(awayResult);

    if (betType != null) {
      if (betType.isSidesMatter() && (gameSideAId == null || gameSideBId == null)) {
        betView.setInvalidMessage("Game " + game.getName() + ": Missing Team");
      }
      if (homeResult == awayResult) {
        betView.setInvalidMessage("Game " + game.getName() + ": Draw not allowed");
      }
    }

    return betView;
  }

  public static Map<String, GameSide> processCompetitionGameSides(
      Competition competition, XSSFWorkbook book, GameSideService gameSideService) throws CompetitionParserException {

    CompetitionParserProperties properties = competition.getCompetitionParserProperties();
    if (properties == null) {
      throw new CompetitionParserException("Competition without parser properties");
    }

    if (StringUtils.isEmpty(properties.getTeamsSheetName())) {
      throw new CompetitionParserException("Missing game sides property teams sheet name");
    }

    final XSSFSheet teams = book.getSheet(properties.getTeamsSheetName());

    if (StringUtils.isEmpty(properties.getTeamsColumnName())) {
      throw new CompetitionParserException("Missing game sides property teams column name");
    }
    final String teamsColumnName = properties.getTeamsColumnName();

    if (properties.getTeamsStartIndex() == null) {
      throw new CompetitionParserException("Missing game sides property teams start index");
    }
    final int teamsStartIndex = properties.getTeamsStartIndex();

    if (properties.getTeamsNumber() == null) {
      throw new CompetitionParserException("Missing game sides property number of teams");
    }
    final int numberOfTeams = properties.getTeamsNumber();

    Map<String, GameSide> gamesByName = new HashMap<>(numberOfTeams);

    for (int i = 0; i < numberOfTeams; i++) {
      final CellReference cellReference = new CellReference(teamsColumnName + (teamsStartIndex + i));
      final Row row = teams.getRow(cellReference.getRow());
      final String teamName = row.getCell(cellReference.getCol()).getStringCellValue();
      String countryCode = teamName.substring(0, 5);
      List<CountryCode> codes = CountryCode.findByName(teamName);
      if (CollectionUtils.isNotEmpty(codes)) {
        countryCode = codes.get(0).getAlpha2().toLowerCase();
      }

      logger.debug("Creating team " + teamName);
      GameSide newGameSide = gameSideService.save(null, competition.getId(), teamName, new HashMap<>(), countryCode);
      gamesByName.put(teamName, newGameSide);
    }
    return gamesByName;
  }

  public static int processPlayoffGames(
      Competition competition,
      XSSFSheet sheet,
      FormulaEvaluator evaluator,
      BetType betType,
      Round round,
      int gameOrder,
      int numberOfGames,
      int jumpSize,
      String gamesColumnName,
      Integer gamesStartIndex,
      GameService gameService) {

    final CompetitionParserProperties competitionParserProperties = competition.getCompetitionParserProperties();
    List<Date> matchDates = new ArrayList<>(numberOfGames);
    for (int i = 0; i < numberOfGames; i++) {
      final CellReference cellReference = new CellReference(gamesColumnName + (gamesStartIndex + i * jumpSize));
      final Row row = sheet.getRow(cellReference.getRow());
      Date matchDate =
          getGameDate(
              cellReference,
              row,
              evaluator,
              competitionParserProperties.getPlayoffsGamesDateIndex(),
              competitionParserProperties.getPlayoffsGamesHourIndex(),
              new SimpleDateFormat(competitionParserProperties.getPlayoffsGamesDateFormat()));
      matchDates.add(matchDate);
    }

    Collections.sort(matchDates);
    int i = 0;
    for (Date finalDate : matchDates) {
      int gameIndex = i + 1;
      String gameName = numberOfGames == 1 ? round.getName() : (round.getName() + ' ' + gameIndex);
      logger.debug("Creating game " + gameName);
      gameService.save(
          null,
          competition.getId(),
          finalDate,
          gameName,
          new HashMap<>(),
          betType.getId(),
          round.getId(),
          gameOrder,
          null,
          null,
          null,
          null);
      i++;
      gameOrder++;
    }
    return gameOrder;
  }

  public static int processGroupGames(
      Competition competition,
      XSSFSheet sheet,
      FormulaEvaluator evaluator,
      BetType groupsBetType,
      Round groupsRound,
      Map<String, GameSide> gamesByName,
      GameService gameService) {

    final CompetitionParserProperties competitionParserProperties = competition.getCompetitionParserProperties();
    // Groups games
    int matchNumber = 1;
    for (int rowIndex = competitionParserProperties.getGroupGamesStartIndex();
        rowIndex
            < competitionParserProperties.getGroupGamesStartIndex() + competitionParserProperties.getGroupGamesNumber();
        rowIndex++) {

      final CellReference cellReference =
          new CellReference(competitionParserProperties.getGroupGamesColumnName() + rowIndex);
      final Row row = sheet.getRow(cellReference.getRow());
      Cell homeGameSideNameCell =
          row.getCell(cellReference.getCol() + competitionParserProperties.getGroupGamesHomeIndex());
      Cell awayGameSideNameCell =
          row.getCell(cellReference.getCol() + competitionParserProperties.getGroupGamesAwayIndex());

      evaluator.evaluateInCell(homeGameSideNameCell);
      evaluator.evaluateInCell(awayGameSideNameCell);
      String homeGameSideName = homeGameSideNameCell.getStringCellValue();
      String awayGameSideName = awayGameSideNameCell.getStringCellValue();

      GameSide home = gamesByName.get(homeGameSideName);
      GameSide away = gamesByName.get(awayGameSideName);

      Date finalDate =
          getGameDate(
              cellReference,
              row,
              evaluator,
              competitionParserProperties.getGroupsGamesDateIndex(),
              competitionParserProperties.getGroupsGamesHourIndex(),
              new SimpleDateFormat(competitionParserProperties.getGroupsGamesDateFormat()));

      String gameName = competitionParserProperties.getGroupsName() + ' ' + matchNumber;
      logger.debug("Creating game " + gameName);
      gameService.save(
          null,
          competition.getId(),
          finalDate,
          gameName,
          new HashMap<>(),
          groupsBetType.getId(),
          groupsRound.getId(),
          matchNumber,
          home.getId(),
          away.getId(),
          null,
          null);
      matchNumber++;
    }

    return matchNumber;
  }

  private static Date getGameDate(
      CellReference cellReference,
      Row row,
      FormulaEvaluator evaluator,
      Integer gamesDateIndex,
      Integer gamesHourIndex,
      DateFormat dateFormat) {
    Date date = new Date();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    try {
      Cell dateCell = row.getCell(cellReference.getCol() + gamesDateIndex);
      evaluator.evaluateInCell(dateCell);

      Cell hourCell = null;
      if (gamesHourIndex != null) {
        hourCell = row.getCell(cellReference.getCol() + gamesHourIndex);
        evaluator.evaluateInCell(hourCell);
      }
      date = dateFormat.parse(dateCell.getStringCellValue());
      calendar.setTime(date);

      if (hourCell != null) {
        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.setTime(hourCell.getDateCellValue());
        calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
      }

    } catch (Exception e) {
      logger.error("Error parsing game date");
    }
    // Get the final Date object
    return calendar.getTime();
  }
}
