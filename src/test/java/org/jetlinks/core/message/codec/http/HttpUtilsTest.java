package org.jetlinks.core.message.codec.http;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class HttpUtilsTest {


    @Test
    public void test(){

        Map<String,String> params = HttpUtils.parseEncodedUrlParams("a=b&b=c&c=a=b&d=a&d=c");

        Assert.assertEquals(params.get("a"),"b");
        Assert.assertEquals(params.get("b"),"c");
        Assert.assertEquals(params.get("c"),"a=b");
        Assert.assertEquals(params.get("d"),"a,c");
    }

    @Test
    public void testEncoded(){

        Map<String,String> params = HttpUtils.parseEncodedUrlParams("a=%e6%b5%8b%e8%af%95&b+c=a%26b");

        Assert.assertEquals(params.get("a"),"测试");
        Assert.assertEquals(params.get("b c"),"a&b");
    }

    @Test
    public void testPath(){
        Assert.assertEquals(HttpUtils.getUrlPath("http://www.baidu.com/test/a"),"/test/a");
        Assert.assertEquals(HttpUtils.getUrlPath("http://www.baidu.com"),"/");

        Assert.assertEquals(HttpUtils.getUrlPath("/test/a"),"/test/a");

        Assert.assertEquals(HttpUtils.getUrlPath("test/a"),"/test/a");

    }

}