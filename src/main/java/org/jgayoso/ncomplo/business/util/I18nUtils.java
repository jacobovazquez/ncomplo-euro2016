package org.jgayoso.ncomplo.business.util;

import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.springframework.util.StringUtils;

public final class I18nUtils {

  public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";

  private I18nUtils() {
    super();
  }

  public static String getTextForLocale(
      final Locale locale, final Map<String, String> i18nTexts, final String defaultText) {

    Validate.notNull(i18nTexts, "The map of i18n-ized texts cannot be null");
    Validate.notNull(defaultText, "Default text cannot be null");
    Validate.notNull(locale, "Locale cannot be null");

    String selector = locale.toString();
    String text = i18nTexts.get(selector);
    if (!StringUtils.isEmpty(text)) {
      return text;
    }
    if (locale.getCountry() != null) {
      selector = locale.getLanguage() + "_" + locale.getCountry();
      text = i18nTexts.get(selector);
      if (!StringUtils.isEmpty(text)) {
        return text;
      }
    }
    selector = locale.getLanguage();
    text = i18nTexts.get(selector);
    if (!StringUtils.isEmpty(text)) {
      return text;
    }

    return defaultText;
  }
}
