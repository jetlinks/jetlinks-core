package org.jetlinks.core.annotation;

public interface AttrConstants {

    interface AttrKey {
        //选择器类型
        String SELECTOR = "selector";
        //是否多选
        String MULTI = "multi";
    }

    interface AttrValue {
        // 设备选择器
        String DEVICE_SELECTOR = "deviceSelector";

        // 产品选择器
        String PRODUCT_SELECTOR = "productSelector";

        // 用户选择器
        String USER_SELECTOR = "userSelector";

        // 组织选择器
        String ORGANIZATION_SELECTOR = "organizationSelector";
    }


}
