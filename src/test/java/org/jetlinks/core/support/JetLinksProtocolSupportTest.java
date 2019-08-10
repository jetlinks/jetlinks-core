package org.jetlinks.core.support;

import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class JetLinksProtocolSupportTest {


    @Test
    public void testEmptyMetaParser() {
        JetLinksProtocolSupport protocolSupport = new JetLinksProtocolSupport();
        DeviceMetadata metadata = protocolSupport.getMetadataCodec()
                .decode("{}");

        Assert.assertNotNull(metadata);
        Assert.assertNotNull(metadata.getEvents());
        Assert.assertNotNull(metadata.getFunctions());
        Assert.assertNotNull(metadata.getProperties());
    }

    @Test
    public void testMetaPropertiesParser() {
        JetLinksProtocolSupport protocolSupport = new JetLinksProtocolSupport();
        DeviceMetadata metadata = protocolSupport.getMetadataCodec()
                .decode("{\"id\":\"device_0001\"" +
                        ",\"name\":\"测试设备\"" +
                        ",\"properties\":[" +
                        "{\"id\":\"name\",\"valueType\":{\"type\":\"string\"}}" +
                        "]}");

        Assert.assertNotNull(metadata);
        Assert.assertNotNull(metadata.getEvents());
        Assert.assertNotNull(metadata.getFunctions());
        Assert.assertNotNull(metadata.getProperties());
        Assert.assertNotNull(metadata.getProperty("name").orElse(null));
        Assert.assertTrue(metadata.getProperty("name").map(PropertyMetadata::getValueType).orElse(null) instanceof StringType);
    }

    @Test
    public void testMetaFunctionParser() {
        JetLinksProtocolSupport protocolSupport = new JetLinksProtocolSupport();
        DeviceMetadata metadata = protocolSupport.getMetadataCodec()
                .decode("{\n" +
                        "  \"id\": \"test-device\",\n" +
                        "  \"name\": \"测试设备\",\n" +
                        "  \"properties\": [\n" +
                        "    {\n" +
                        "      \"id\": \"name\",\n" +
                        "      \"name\": \"名称\",\n" +
                        "      \"valueType\": {\n" +
                        "        \"type\": \"string\"\n" +
                        "      }\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"id\": \"model\",\n" +
                        "      \"name\": \"型号\",\n" +
                        "      \"valueType\": {\n" +
                        "        \"type\": \"string\"\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"functions\": [\n" +
                        "    {\n" +
                        "      \"id\": \"playVoice\",\n" +
                        "      \"name\": \"播放声音\",\n" +
                        "      \"inputs\": [\n" +
                        "        {\n" +
                        "          \"id\": \"content\",\n" +
                        "          \"name\": \"内容\",\n" +
                        "          \"valueType\": {\n" +
                        "            \"type\": \"string\"\n" +
                        "          }\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"id\": \"times\",\n" +
                        "          \"name\": \"播放次数\",\n" +
                        "          \"valueType\": {\n" +
                        "            \"type\": \"int\"\n" +
                        "          }\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"id\": \"setColor\",\n" +
                        "      \"name\": \"灯光颜色\",\n" +
                        "      \"inputs\": [\n" +
                        "        {\n" +
                        "          \"id\": \"colorRgb\",\n" +
                        "          \"name\": \"颜色RGB值\",\n" +
                        "          \"valueType\": {\n" +
                        "            \"type\": \"string\"\n" +
                        "          }\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"events\": [\n" +
                        "    {\n" +
                        "      \"id\": \"temperature\",\n" +
                        "      \"name\": \"温度\",\n" +
                        "      \"parameters\": [\n" +
                        "        {\n" +
                        "          \"id\": \"temperature\",\n" +
                        "          \"valueType\": {\n" +
                        "            \"type\": \"int\"\n" +
                        "          }\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}");

        Assert.assertNotNull(metadata);
        Assert.assertNotNull(metadata.getEvents());
        Assert.assertNotNull(metadata.getFunctions());
        Assert.assertNotNull(metadata.getProperties());
        Assert.assertNotNull(metadata.getFunction("playVoice").orElse(null));
        Assert.assertTrue(metadata.getFunction("playVoice")
                .map(FunctionMetadata::getInputs)
                .flatMap(input -> input.stream().findFirst())
                .map(PropertyMetadata::getValueType)
                .orElse(null) instanceof StringType);
        String json = protocolSupport.getMetadataCodec().encode(metadata);
        System.out.println(json);
    }
}