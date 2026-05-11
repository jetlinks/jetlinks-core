package org.jetlinks.core.metadata;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.metadata.expand.LocaleResource;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.metadata.types.UnknownType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

/**
 * @author gyl
 * @since 2.3
 */
public class PropertyMetadataTest {

    @Test
    public void test() {
        SimplePropertyMetadata metadata = SimplePropertyMetadata.of("test", "name", StringType.GLOBAL);
        LocaleResource resource = LocaleResource
            .of(Locale.ENGLISH, "en_name")
            .addResource(Locale.CHINESE, "zh_name");
        metadata.expand(MetadataConstants.Expand.LOCALE_RESOURCE_KEY, resource);


        Assert.assertEquals("en_name", MetadataConstants.Expand.getLocaleName(metadata, Locale.ENGLISH));
        Assert.assertEquals("zh_name", MetadataConstants.Expand.getLocaleName(metadata, Locale.CHINESE));
        Assert.assertEquals("name", MetadataConstants.Expand.getLocaleName(metadata, Locale.FRANCE));

        Assert.assertEquals("zh_name", MetadataConstants.Expand.getLocaleName(metadata, Locale.SIMPLIFIED_CHINESE));
        resource.addResource(Locale.SIMPLIFIED_CHINESE, "zh_CN_name");
        Assert.assertEquals("zh_CN_name", MetadataConstants.Expand.getLocaleName(metadata, Locale.SIMPLIFIED_CHINESE));
    }

    @Test
    public void testUnknownValueTypeFallback() {
        SimplePropertyMetadata metadata = new SimplePropertyMetadata();
        metadata.setId("test");
        metadata.setName("name");

        Assert.assertEquals(UnknownType.ID, metadata.getValueType().getId());

        JSONObject json = metadata.toJson();
        Assert.assertEquals(UnknownType.ID, json.getJSONObject("valueType").getString("type"));

        SimplePropertyMetadata another = new SimplePropertyMetadata();
        another.fromJson(json);
        Assert.assertEquals(UnknownType.ID, another.getValueType().getId());
    }

}
