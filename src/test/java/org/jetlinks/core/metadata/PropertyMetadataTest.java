package org.jetlinks.core.metadata;

import org.jetlinks.core.metadata.types.StringType;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author gyl
 * @since 2.3
 */
public class PropertyMetadataTest {

    @Test
    public void test() {
        SimplePropertyMetadata metadata = SimplePropertyMetadata.of("test", "name", StringType.GLOBAL);
        Map<String, String> map = new HashMap<>();
        map.put(Locale.ENGLISH.getLanguage(), "en_name");
        map.put(Locale.CHINESE.getLanguage(), "zh_name");
        metadata.expand(MetadataConstants.Expand.LOCALE_NAME_KEY, map);


        Assert.assertEquals("en_name", MetadataConstants.Expand.getLocaleName(metadata, Locale.ENGLISH));
        Assert.assertEquals("zh_name", MetadataConstants.Expand.getLocaleName(metadata, Locale.CHINESE));
        Assert.assertEquals("name", MetadataConstants.Expand.getLocaleName(metadata, Locale.FRANCE));
    }

}
