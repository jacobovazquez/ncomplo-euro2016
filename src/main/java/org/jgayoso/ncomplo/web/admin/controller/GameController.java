package org.jgayoso.ncomplo.web.admin.controller;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;

import org.jgayoso.ncomplo.business.entities.Game;
import org.jgayoso.ncomplo.business.services.BetTypeService;
import org.jgayoso.ncomplo.business.services.CompetitionService;
import org.jgayoso.ncomplo.business.services.GameService;
import org.jgayoso.ncomplo.business.services.GameSideService;
import org.jgayoso.ncomplo.business.services.RoundService;
import org.jgayoso.ncomplo.business.util.I18nUtils;
import org.jgayoso.ncomplo.exceptions.CompetitionParserException;
import org.jgayoso.ncomplo.web.admin.beans.GameBean;
import org.jgayoso.ncomplo.web.admin.beans.LangBean;
import org.jgayoso.ncomplo.web.admin.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

@Controller
@RequestMapping("/admin/competition/{competitionId}/game")
public class GameController {

  private static final String VIEW_BASE = "admin/competition/game/";
  private static final SimpleDateFormat timeDateFormat = new SimpleDateFormat("HH:mm");

  @Autowired private CompetitionService competitionService;

  @Autowired private GameService gameService;

  @Autowired private RoundService roundService;

  @Autowired private GameSideService gameSideService;

  @Autowired private BetTypeService betTypeService;

  public GameController() {
    super();
  }

  @InitBinder
  public void initDateBinder(final WebDataBinder dataBinder) {
    final SimpleDateFormat sdf = new SimpleDateFormat(I18nUtils.ISO_DATE_FORMAT);
    sdf.setLenient(false);
    dataBinder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, false));
  }

  @RequestMapping("/list")
  public String list(
      @PathVariable("competitionId") final Integer competitionId,
      final HttpServletRequest request,
      final ModelMap model) {

    final List<Game> games = this.gameService.findAll(competitionId, RequestContextUtils.getLocale(request));

    model.addAttribute("allGames", games);
    model.addAttribute("competition", this.competitionService.find(competitionId));

    return VIEW_BASE + "list";
  }

  @RequestMapping("/manage")
  public String manage(
      @RequestParam(value = "id", required = false) final Integer id,
      @PathVariable("competitionId") final Integer competitionId,
      final ModelMap model,
      final HttpServletRequest request) {

    final GameBean gameBean = new GameBean();

    if (id != null) {
      final Game game = this.gameService.find(id);
      gameBean.setId(game.getId());
      gameBean.setName(game.getName());
      gameBean.getNamesByLang().clear();
      gameBean.getNamesByLang().addAll(LangBean.listFromMap(game.getNamesByLang()));
      gameBean.setDate(game.getDate());
      final String time = timeDateFormat.format(game.getDate());
      gameBean.setTime(time);
      gameBean.setDefaultBetTypeId(game.getDefaultBetType().getId());
      gameBean.setRoundId(game.getRound().getId());
      gameBean.setOrder(game.getOrder());
      gameBean.setGameSideAId(game.getGameSideA() == null ? null : game.getGameSideA().getId());
      gameBean.setGameSideBId(game.getGameSideB() == null ? null : game.getGameSideB().getId());
      gameBean.setScoreA(game.getScoreA());
      gameBean.setScoreB(game.getScoreB());
    }

    model.addAttribute("game", gameBean);
    model.addAttribute("competition", this.competitionService.find(competitionId));

    final Locale locale = RequestContextUtils.getLocale(request);
    model.addAttribute("allRounds", this.roundService.findAll(competitionId));
    model.addAttribute("allBetTypes", this.betTypeService.findAllOrderByName(competitionId, locale));
    model.addAttribute("allGameSides", this.gameSideService.findAll(competitionId, locale));

    return VIEW_BASE + "manage";
  }

  @RequestMapping("/save")
  public String save(
      final GameBean gameBean,
      final BindingResult bindingResult,
      @PathVariable("competitionId") final Integer competitionId) {

    if (StringUtils.hasText(gameBean.getTime()) && gameBean.getDate() != null) {
      final LocalTime localTime = LocalTime.parse(gameBean.getTime(), DateTimeFormatter.ofPattern("HH:mm"));
      final int hour = localTime.get(ChronoField.CLOCK_HOUR_OF_DAY);
      final int minute = localTime.get(ChronoField.MINUTE_OF_HOUR);

      Calendar cal = Calendar.getInstance();
      cal.setTime(gameBean.getDate());
      cal.set(Calendar.HOUR_OF_DAY, hour);
      cal.set(Calendar.MINUTE, minute);
      gameBean.setDate(cal.getTime());
    }

    this.gameService.save(
        gameBean.getId(),
        competitionId,
        gameBean.getDate(),
        gameBean.getName(),
        LangBean.mapFromList(gameBean.getNamesByLang()),
        gameBean.getDefaultBetTypeId(),
        gameBean.getRoundId(),
        gameBean.getOrder(),
        gameBean.getGameSideAId(),
        gameBean.getGameSideBId(),
        gameBean.getScoreA(),
        gameBean.getScoreB());

    return "redirect:list";
  }

  @RequestMapping("/deleteAll")
  public String deleteAll(@RequestParam(value = "competitionId") final Integer competitionId) {

    this.gameService.deleteAll(competitionId);
    return "redirect:list";
  }

  @RequestMapping("/delete")
  public String delete(@RequestParam(value = "id") final Integer id) {

    this.gameService.delete(id);
    return "redirect:list";
  }

  @RequestMapping(method = RequestMethod.POST, value = "/upload")
  public String uploadGames(
      @RequestParam("file") final MultipartFile file,
      @RequestParam(value = "id") final Integer id,
      final HttpServletRequest request,
      final RedirectAttributes redirectAttributes) {

    final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof AnonymousAuthenticationToken) {
      return "error";
    }
    final String login = auth.getName();

    File competitionFile = null;
    try {
      competitionFile = FileUtils.convert("games", file, login);
      if (!competitionFile.exists() || competitionFile.length() == 0) {
        redirectAttributes.addFlashAttribute("error", "Empty file");
        return "redirect:list";
      }
      this.gameService.processFile(id, login, competitionFile);
    } catch (final IOException e) {
      redirectAttributes.addFlashAttribute("error", "Error processing games file");
    } catch (CompetitionParserException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      return "redirect:list";
    } finally {
      // delete file
      if (competitionFile != null) {
        competitionFile.delete();
      }
    }
    redirectAttributes.addFlashAttribute("message", "Game sides processed successfully");
    return "redirect:list";
  }
}
