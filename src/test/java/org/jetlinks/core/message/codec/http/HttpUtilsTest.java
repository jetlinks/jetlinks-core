package org.jetlinks.core.message.codec.http;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class HttpUtilsTest {


    @Test
    public void testDecode(){

        String str="https://www.baidu.com/?a=b&b=c&c=a=b&d=a&d=c";
        Map<String,String> params = HttpUtils.parseEncodedUrlParams(str);

        Assert.assertEquals(params.get("a"),"b");
        Assert.assertEquals(params.get("b"),"c");
        Assert.assertEquals(params.get("c"),"a=b");
        Assert.assertEquals(params.get("d"),"a,c");
    }

    @Test
    public void testDecodeEncoded(){

        String str="a=%e6%b5%8b%e8%af%95&b+c=a%26b";

        Map<String,String> params = HttpUtils.parseEncodedUrlParams(str);

        Assert.assertEquals(params.get("a"),"测试");
        Assert.assertEquals(params.get("b c"),"a&b");

        Assert.assertEquals(HttpUtils.createEncodedUrlParams(params).toLowerCase(),str.toLowerCase());

    }


    @Test
    public void testPath(){
        Assert.assertEquals(HttpUtils.getUrlPath("http://www.baidu.com/test/a?a=b"),"/test/a");
        Assert.assertEquals(HttpUtils.getUrlPath("http://www.baidu.com"),"/");

        Assert.assertEquals(HttpUtils.getUrlPath("/test/a"),"/test/a");

        Assert.assertEquals(HttpUtils.getUrlPath("test/a"),"/test/a");
        Assert.assertEquals(HttpUtils.getUrlPath("test/a?a=b"),"/test/a");

    }

}