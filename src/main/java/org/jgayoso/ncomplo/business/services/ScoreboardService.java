package org.jgayoso.ncomplo.business.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jgayoso.ncomplo.business.entities.Bet;
import org.jgayoso.ncomplo.business.entities.League;
import org.jgayoso.ncomplo.business.entities.User;
import org.jgayoso.ncomplo.business.entities.repositories.BetRepository;
import org.jgayoso.ncomplo.business.entities.repositories.LeagueRepository;
import org.jgayoso.ncomplo.business.views.ScoreboardEntry;
import org.jgayoso.ncomplo.business.views.ScoreboardEntry.ScoreboardEntryComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreboardService {

  @Autowired private LeagueRepository leagueRepository;

  @Autowired private BetRepository betRepository;

  public ScoreboardService() {
    super();
  }

  @Transactional
  public List<ScoreboardEntry> computeScoreboard(final Integer leagueId, final Integer roundId, final Locale locale) {

    final List<ScoreboardEntry> scoreboard = new ArrayList<>();

    final League league = this.leagueRepository.findById(leagueId).orElse(null);
    final Set<User> participants = league.getParticipants();

    for (final User participant : participants) {

      int totalPoints = 0;

      final List<Bet> bets = this.betRepository.findByLeagueIdAndUserLogin(leagueId, participant.getLogin());

      for (final Bet bet : bets) {
        if (bet.getPoints() != null) {
          if (roundId != null) {
            if (!bet.getGame().getRound().getId().equals(roundId)) {
              continue;
            }
          }
          totalPoints += bet.getPoints().intValue();
        }
      }

      scoreboard.add(new ScoreboardEntry(participant, Integer.valueOf(totalPoints)));
    }

    Collections.sort(scoreboard, new ScoreboardEntryComparator(locale));

    int position = 0;
    int treated = 0;
    int lastPoints = -1;
    for (final ScoreboardEntry entry : scoreboard) {
      treated++;
      if (entry.getPoints().intValue() != lastPoints) {
        position = treated;
        lastPoints = entry.getPoints().intValue();
      }
      entry.setPosition(Integer.valueOf(position));
    }

    return scoreboard;
  }
}
