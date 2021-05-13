package org.jetlinks.core.metadata.types;

import com.alibaba.fastjson.JSON;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ArrayTypeTest {

    @Test
    public void test() {
        ArrayType type = new ArrayType();
        type.elementType(new ObjectType());

        List<Object> data = type.convert(JSON.parseArray(" [{\n" +
                                                                 "    \"Person\": {\n" +
                                                                 "      \"Type\": 1,\n" +
                                                                 "      \"Code\": \"10086\",\n" +
                                                                 "      \"CredentialNo\":\"339005xxxxxxxxxxxx\",\n" +
                                                                 "      \"GroupName\": \"默认权限组\",\n" +
                                                                 "      \"Name\": \"张三\",\n" +
                                                                 "      \"Sex\": \"male\",\n" +
                                                                 "      \"Birthday\": \"1980-01-01\",\n" +
                                                                 "      \"Images\":[\"base64Data\"],\n" +
                                                                 "      \"Cards\": [{\n" +
                                                                 "        \"ID\": \"abcd\",\n" +
                                                                 "        \"Type\": 1,\n" +
                                                                 "        \"Validity\": [\n" +
                                                                 "          \"2018-10-1\",\n" +
                                                                 "          \"2019-10-1\"\n" +
                                                                 "        ],\n" +
                                                                 "        \"ValidityTime\": [\n" +
                                                                 "          \"12:30:00\",\n" +
                                                                 "          \"23:59:59\"\n" +
                                                                 "        ]\n" +
                                                                 "      }]\n" +
                                                                 "    }\n" +
                                                                 "  }]"));
        assertEquals(data.get(0), data.get(0));
    }
}