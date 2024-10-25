package org.jetlinks.core.metadata.expand;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author gyl
 * @since 2.3
 */
public class LocaleResource extends HashMap<String, String> {
    private static final long serialVersionUID = 362498820763181265L;


    public LocaleResource(Map<String, String> data) {
        super(data);
    }

    public LocaleResource() {
    }

    public static LocaleResource of(Locale locale, String data) {
        LocaleResource resource = new LocaleResource();
        resource.addResource(locale, data);
        return resource;
    }

    public LocaleResource addResource(Locale locale, String data) {
        put(generateLocaleKey(locale), data);
        return this;
    }


    public static String generateLocaleKey(Locale locale) {
        if (StringUtils.hasText(locale.getCountry()) && !locale.getCountry().equals("")) {
            return locale.getLanguage() + "_" + locale.getCountry();
        }
        return locale.getLanguage();
    }

    @JsonIgnore
    public String getResource(Locale locale) {
        String key = LocaleResource.generateLocaleKey(locale);
        if (key.equals(locale.getLanguage())) {
            return this.get(key);
        }
        return this.getOrDefault(key, this.get(locale.getLanguage()));
    }


}
