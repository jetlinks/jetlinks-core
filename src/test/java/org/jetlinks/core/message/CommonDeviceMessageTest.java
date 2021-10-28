package org.jetlinks.core.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.hswebframework.web.bean.Copier;
import org.hswebframework.web.bean.FastBeanCopier;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class CommonDeviceMessageTest {


    @Test
    public void test() {
        JSON.toJSON(new CommonDeviceMessage());

        long time = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            JSON.toJSON(new CommonDeviceMessage());
        }

        System.out.println(System.currentTimeMillis() - time);

        FastBeanCopier.copy(new CommonDeviceMessage(), JSONObject::new);

       Copier copier= FastBeanCopier.getCopier(new CommonDeviceMessage(),new JSONObject(),true);

        time = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            FastBeanCopier.copy(new CommonDeviceMessage(), JSONObject::new);

//            copier.copy(new CommonDeviceMessage(),new JSONObject());
        }

        System.out.println(System.currentTimeMillis() - time);
    }
}