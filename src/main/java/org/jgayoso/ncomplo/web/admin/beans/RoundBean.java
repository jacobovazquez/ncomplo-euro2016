package org.jgayoso.ncomplo.web.admin.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public class RoundBean implements Serializable {

  private static final long serialVersionUID = 3347795783622341493L;
  @NotNull private final List<LangBean> namesByLang = new ArrayList<>();
  @NotNull private Integer id;

  @NotNull
  @Length(min = 3, max = 200)
  private String name;

  @NotNull private Integer order;

  public RoundBean() {
    super();
  }

  public Integer getId() {
    return this.id;
  }

  public void setId(final Integer id) {
    this.id = id;
  }

  public String getName() {
    return this.name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public List<LangBean> getNamesByLang() {
    return this.namesByLang;
  }

  public Integer getOrder() {
    return this.order;
  }

  public void setOrder(final Integer order) {
    this.order = order;
  }
}
