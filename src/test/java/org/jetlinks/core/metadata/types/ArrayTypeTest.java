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
        
        Integer[] intArr = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        String[] strArr = {"字符串1", "字符串2", "字符串3", "字符串4"};
        double[] doubleArr = {1.2, 1.2, 3.3, 34.2, 323.1, 434.4};
        
        ArrayType type1 = new ArrayType();
        type1.elementType(new IntType());
        List<Object> intConvert = type1.convert(intArr);
        
        ArrayType type2 = new ArrayType();
        type2.elementType(new StringType());
        List<Object> strConvert = type2.convert(strArr);
        
        ArrayType type3 = new ArrayType();
        type3.elementType(new DoubleType());
        List<Object> doubleConvert = type3.convert(doubleArr);
        
        assertEquals(intArr[0], intConvert.get(0));
        assertEquals(strArr[0], strConvert.get(0));
        assertEquals(doubleArr[0], doubleConvert.get(0));
    }
}