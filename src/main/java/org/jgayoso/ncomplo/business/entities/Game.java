package org.jgayoso.ncomplo.business.entities;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import org.jgayoso.ncomplo.business.util.I18nUtils;

@Entity
@Table(name = "GAME")
public class Game implements DatedAndNamedEntity {

  @ElementCollection(fetch = FetchType.LAZY, targetClass = java.lang.String.class)
  @CollectionTable(name = "GAME_NAME_I18N", joinColumns = @JoinColumn(name = "GAME_ID"))
  @MapKeyColumn(name = "LANG", nullable = false, length = 20)
  @Column(name = "NAME", nullable = false, length = 200)
  private final Map<String, String> namesByLang = new LinkedHashMap<>();

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "COMPETITION_ID", nullable = false)
  private Competition competition;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ROUND_ID", nullable = false)
  private Round round;

  @Column(name = "GAME_ORDER", nullable = false)
  private Integer order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "DEFAULT_BET_TYPE_ID", nullable = false)
  private BetType defaultBetType;

  @Column(name = "NAME", nullable = false, length = 200)
  private String name;

  @Column(name = "DATE", nullable = true)
  private Date date;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "GAME_SIDE_A_ID", nullable = true)
  private GameSide gameSideA;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "GAME_SIDE_B_ID", nullable = true)
  private GameSide gameSideB;

  @Column(name = "SCORE_A", nullable = true)
  private Integer scoreA;

  @Column(name = "SCORE_B", nullable = true)
  private Integer scoreB;

  public Game() {
    super();
  }

  public Competition getCompetition() {
    return this.competition;
  }

  public void setCompetition(final Competition competition) {
    this.competition = competition;
  }

  public Round getRound() {
    return this.round;
  }

  public void setRound(final Round round) {
    this.round = round;
  }

  public Integer getOrder() {
    return this.order;
  }

  public void setOrder(final Integer order) {
    this.order = order;
  }

  public BetType getDefaultBetType() {
    return this.defaultBetType;
  }

  public void setDefaultBetType(final BetType defaultBetType) {
    this.defaultBetType = defaultBetType;
  }

  @Override
  public String getName() {
    return this.name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public Date getDate() {
    return this.date;
  }

  public void setDate(final Date date) {
    this.date = date;
  }

  public GameSide getGameSideA() {
    return this.gameSideA;
  }

  public void setGameSideA(final GameSide gameSideA) {
    this.gameSideA = gameSideA;
  }

  public GameSide getGameSideB() {
    return this.gameSideB;
  }

  public void setGameSideB(final GameSide gameSideB) {
    this.gameSideB = gameSideB;
  }

  public Integer getScoreA() {
    return this.scoreA;
  }

  public void setScoreA(final Integer scoreA) {
    this.scoreA = scoreA;
  }

  public Integer getScoreB() {
    return this.scoreB;
  }

  public void setScoreB(final Integer scoreB) {
    this.scoreB = scoreB;
  }

  public Integer getId() {
    return this.id;
  }

  @Override
  public Map<String, String> getNamesByLang() {
    return this.namesByLang;
  }

  @Override
  public String getName(final Locale locale) {
    return I18nUtils.getTextForLocale(locale, this.namesByLang, this.name);
  }

  @Override
  public String toString() {
    return "Game{"
        + "id="
        + id
        + ", round="
        + round
        + ", name='"
        + name
        + '\''
        + ", gameSideA="
        + gameSideA
        + ", gameSideB="
        + gameSideB
        + '}';
  }

  public boolean isDateDefined() {
    return (this.date != null);
  }

  public boolean isTeamsDefined() {
    return (this.gameSideA != null && this.gameSideB != null);
  }

  public boolean isScoreDefined() {
    return (this.scoreA != null && this.scoreB != null);
  }

  public static final class GameComparator implements Comparator<Game> {

    private final Locale locale;

    public GameComparator(final Locale locale) {
      super();
      this.locale = locale;
    }

    @Override
    public int compare(final Game o1, final Game o2) {

      final Integer o1Order = o1.getOrder();
      final Integer o2Order = o2.getOrder();

      final int orderComp = o1Order.compareTo(o2Order);
      if (orderComp != 0) {
        return orderComp;
      }

      final Date o1Date = o1.getDate();
      final Date o2Date = o2.getDate();

      if (o1Date != null) {
        if (o2Date == null) {
          return -1;
        }
        final int dateComp = o1Date.compareTo(o2Date);
        if (dateComp != 0) {
          return dateComp;
        }
      } else if (o2Date != null) {
        return 1;
      }

      final String o1Name = o1.getName(this.locale);
      final String o2Name = o2.getName(this.locale);
      if (o1Name == null) {
        return 1;
      }
      if (o2Name == null) {
        return -1;
      }
      return o1Name.compareTo(o2Name);
    }
  }

  public static final class GameOrderComparator implements Comparator<Game> {

    public GameOrderComparator() {
      super();
    }

    @Override
    public int compare(final Game o1, final Game o2) {

      final Integer o1Order = o1.getOrder();
      final Integer o2Order = o2.getOrder();

      final int orderComp = o1Order.compareTo(o2Order);
      if (orderComp != 0) {
        return orderComp;
      }

      final Date o1Date = o1.getDate();
      final Date o2Date = o2.getDate();

      if (o1Date != null) {
        if (o2Date == null) {
          return -1;
        }
        final int dateComp = o1Date.compareTo(o2Date);
        if (dateComp != 0) {
          return dateComp;
        }
      } else if (o2Date != null) {
        return 1;
      }
      return 0;
    }
  }
}
