package org.jgayoso.ncomplo.web.admin.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

import org.springframework.util.StringUtils;

public class LangBean implements Serializable {

  private static final long serialVersionUID = 5346733885599754478L;

  @NotNull private String lang;

  @NotNull private String value;

  public LangBean() {
    super();
  }

  public static List<LangBean> listFromMap(final Map<String, String> valuesByLang) {
    final List<LangBean> langBeans = new ArrayList<>();
    for (final Map.Entry<String, String> namesByLangEntry : valuesByLang.entrySet()) {
      final LangBean langBean = new LangBean();
      langBean.setLang(namesByLangEntry.getKey());
      langBean.setValue(namesByLangEntry.getValue());
      langBeans.add(langBean);
    }
    return langBeans;
  }

  public static Map<String, String> mapFromList(final List<LangBean> langBeans) {
    final Map<String, String> valuesByLang = new LinkedHashMap<>();
    for (final LangBean langBean : langBeans) {
      if (!StringUtils.isEmpty(langBean.getLang())) {
        valuesByLang.put(langBean.getLang(), langBean.getValue());
      }
    }
    return valuesByLang;
  }

  public String getLang() {
    return this.lang;
  }

  public void setLang(final String lang) {
    this.lang = lang;
  }

  public String getValue() {
    return this.value;
  }

  public void setValue(final String value) {
    this.value = value;
  }
}
