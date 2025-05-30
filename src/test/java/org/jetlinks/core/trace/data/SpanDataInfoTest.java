package org.jetlinks.core.trace.data;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class SpanDataInfoTest {

    @Test
    public void testBasicSpanToString() {
        // 创建基础span
        SpanDataInfo span = createSpanDataInfo(
            "test-app",
            "test-span",
            "trace123",
            "span123",
            "parent123",
            1000000000L, // 1秒开始
            1200000000L  // 1.2秒结束，耗时200ms
        );

        String result = span.toString();

        // 验证基本信息格式
        assertTrue("应包含app信息", result.contains("[test-app]"));
        assertTrue("应包含span名称", result.contains("test-span"));
        assertTrue("应包含耗时", result.contains("(200ms)"));
    }

    @Test
    public void testAttributesToString() {
        SpanDataInfo span = createSpanDataInfo(
            "test-app",
            "test-span",
            "trace123",
            "span123",
            "parent123",
            1000000000L,
            1100000000L
        );

        // 添加属性
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("key1", "value1");
        attributes.put("key2", "value2");
        span.setAttributes(attributes);

        String result = span.toString();
        // 验证attributes分支
        assertTrue("应包含Attributes标签", result.contains("├── Attributes:"));
        assertTrue("应包含key1属性", result.contains("key1: value1"));
        assertTrue("应包含key2属性", result.contains("key2: value2"));
        assertTrue("应使用正确的树状字符", result.contains("│   ├──") || result.contains("│   └──"));
    }

    @Test
    public void testMultiLineAttributesToString() {
        SpanDataInfo span = createSpanDataInfo(
            "test-app",
            "test-span",
            "trace123",
            "span123",
            "parent123",
            1000000000L,
            1100000000L
        );

        // 添加多行文本属性
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("multiline", "第一行\n第二行\n第三行");
        attributes.put("simple", "单行文本");
        span.setAttributes(attributes);

        String result = span.toString();

        // 验证多行文本处理
        assertTrue("应包含多行文本", result.contains("第一行"));
        assertTrue("应包含第二行", result.contains("第二行"));
        assertTrue("应包含第三行", result.contains("第三行"));

        // 验证缩进对齐（每行都应该有正确的缩进）
        String[] lines = result.split("\n");
        boolean foundMultilineStart = false;
        for (String line : lines) {
            if (line.contains("multiline: 第一行")) {
                foundMultilineStart = true;
            } else if (foundMultilineStart && line.contains("第二行")) {
                // 第二行应该有正确的缩进，不应该从行首开始
                assertFalse("第二行应该有缩进", line.startsWith("第二行"));
                break;
            }
        }
    }

    @Test
    public void testEventsToString() {
        SpanDataInfo span = createSpanDataInfo(
            "test-app",
            "test-span",
            "trace123",
            "span123",
            "parent123",
            1000000000L, // 开始时间
            1200000000L  // 结束时间
        );

        // 添加事件
        List<SpanEventDataInfo> events = new ArrayList<>();

        SpanEventDataInfo event1 = new SpanEventDataInfo();
        event1.setName("event1");
        event1.setTimeNanos(1050000000L); // 开始后50ms
        Map<String, Object> event1Attrs = new HashMap<>();
        event1Attrs.put("level", "info");
        event1.setAttributes(event1Attrs);

        SpanEventDataInfo event2 = new SpanEventDataInfo();
        event2.setName("event2");
        event2.setTimeNanos(1150000000L); // 开始后150ms

        events.add(event1);
        events.add(event2);
        span.setEvents(events);

        String result = span.toString();

        // 验证events分支
        assertTrue("应包含Events标签", result.contains("Events:"));
        assertTrue("应包含event1", result.contains("event1"));
        assertTrue("应包含event2", result.contains("event2"));
        assertTrue("应包含相对时间", result.contains("(at 50ms)"));
        assertTrue("应包含相对时间", result.contains("(at 150ms)"));
        assertTrue("应包含事件属性", result.contains("level=info"));
    }

    @Test
    public void testChildrenToString() {
        // 创建父span
        SpanDataInfo parent = createSpanDataInfo(
            "parent-app",
            "parent-span",
            "trace123",
            "parent123",
            null,
            1000000000L,
            1300000000L
        );

        // 创建子span
        SpanDataInfo child1 = createSpanDataInfo(
            "child-app",
            "child1-span",
            "trace123",
            "child1",
            "parent123",
            1050000000L,
            1150000000L
        );

        SpanDataInfo child2 = createSpanDataInfo(
            "child-app",
            "child2-span",
            "trace123",
            "child2",
            "parent123",
            1200000000L,
            1250000000L
        );

        List<SpanDataInfo> children = Arrays.asList(child1, child2);
        parent.setChildren(children);

        String result = parent.toString();

        // 验证层级结构
        assertTrue("应包含父span", result.contains("[parent-app] parent-span"));
        assertTrue("应包含子span1", result.contains("[child-app] child1-span"));
        assertTrue("应包含子span2", result.contains("[child-app] child2-span"));

        // 验证树状字符
        assertTrue("应使用├──或└──", result.contains("├──") || result.contains("└──"));

        // 修正缩进检查 - 子span应该以"├── "或"└── "开头（前面有4个空格）
        boolean foundCorrectIndent = false;
        String[] lines = result.split("\n");
        for (String line : lines) {
            if (line.contains("[child-app]")) {
                // 检查是否以正确的缩进开始
                if (line.startsWith("    ├──") || line.startsWith("    └──")) {
                    foundCorrectIndent = true;
                    break;
                }
            }
        }
        assertTrue("应有正确的缩进", foundCorrectIndent);
    }

    @Test
    public void testComplexTreeToString() {
        // 创建复杂的树状结构
        SpanDataInfo root = createSpanDataInfo(
            "root-app",
            "root-span",
            "trace123",
            "root",
            null,
            1000000000L,
            1500000000L
        );

        // 添加根节点的属性和事件
        Map<String, Object> rootAttrs = new HashMap<>();
        rootAttrs.put("version", "1.0");
        rootAttrs.put("config", "line1\nline2");
        root.setAttributes(rootAttrs);

        List<SpanEventDataInfo> rootEvents = new ArrayList<>();
        SpanEventDataInfo startEvent = new SpanEventDataInfo();
        startEvent.setName("start");
        startEvent.setTimeNanos(1010000000L);
        rootEvents.add(startEvent);
        root.setEvents(rootEvents);

        // 创建子节点
        SpanDataInfo child = createSpanDataInfo(
            "child-app",
            "child-span",
            "trace123",
            "child",
            "root",
            1100000000L,
            1400000000L
        );

        // 创建孙节点
        SpanDataInfo grandchild = createSpanDataInfo(
            "gc-app",
            "grandchild-span",
            "trace123",
            "grandchild",
            "child",
            1200000000L,
            1300000000L
        );

        child.setChildren(Arrays.asList(grandchild));
        root.setChildren(Arrays.asList(child));

        String result = root.toString();

        // 验证完整结构
        assertTrue("应包含根节点", result.contains("[root-app] root-span"));
        assertTrue("应包含子节点", result.contains("[child-app] child-span"));
        assertTrue("应包含孙节点", result.contains("[gc-app] grandchild-span"));
        assertTrue("应包含属性", result.contains("Attributes:"));
        assertTrue("应包含事件", result.contains("Events:"));
        assertTrue("应包含多行文本", result.contains("line1") && result.contains("line2"));

        // 验证三级缩进
        String[] lines = result.split("\n");
        boolean foundThirdLevel = false;
        for (String line : lines) {
            if (line.contains("[gc-app] grandchild-span")) {
                // 孙节点应该有两级缩进
                assertTrue("孙节点应该有正确缩进", line.startsWith("        "));
                foundThirdLevel = true;
                break;
            }
        }
        assertTrue("应该找到三级节点", foundThirdLevel);
    }

    @Test
    public void testNullAndEmptyToString() {
        // 测试空值和null值处理
        SpanDataInfo span = createSpanDataInfo(
            null,  // app为null
            null,  // name为null
            "trace123",
            "span123",
            "parent123",
            1000000000L,
            1100000000L
        );

        // 设置为null或空集合
        span.setAttributes(null);
        span.setEvents(null);
        span.setChildren(null);

        String result = span.toString();

        // 验证null值处理
        assertTrue("app为null时应显示unknown", result.contains("[unknown]"));
        assertTrue("name为null时应显示unknown", result.contains("unknown"));

        // 验证空集合不显示对应分支
        assertFalse("不应显示Attributes分支", result.contains("Attributes:"));
        assertFalse("不应显示Events分支", result.contains("Events:"));

        // 测试空集合
        span.setAttributes(new HashMap<>());
        span.setEvents(new ArrayList<>());
        span.setChildren(new ArrayList<>());

        result = span.toString();
        assertFalse("空attributes不应显示分支", result.contains("Attributes:"));
        assertFalse("空events不应显示分支", result.contains("Events:"));
    }

    @Test
    public void testToStringBuilder() {
        SpanDataInfo span = createSpanDataInfo(
            "test-app",
            "test-span",
            "trace123",
            "span123",
            "parent123",
            1000000000L,
            1100000000L
        );

        StringBuilder builder = new StringBuilder("前缀内容\n");
        span.toString(builder);
        String result = builder.toString();

        // 验证StringBuilder被正确使用
        assertTrue("应保留前缀内容", result.startsWith("前缀内容"));
        assertTrue("应包含span信息", result.contains("[test-app] test-span"));
    }

    @Test
    public void testMixedLanguageAttributesToString() {
        // 测试中英文混合的属性值
        SpanDataInfo span = createSpanDataInfo(
            "混合-app", 
            "mixed-span", 
            "trace123", 
            "span123", 
            "parent123",
            1000000000L,
            1100000000L
        );
        
        // 添加中英文混合的多行属性
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("message", "用户登录成功\nUser login successful\n用户ID: 12345\nLogin time: 2023-12-01");
        attributes.put("error_code", "AUTH_FAILED");
        attributes.put("详细信息", "This is a detailed\nmulti-line description\n包含中文的详细说明\nwith mixed languages");
        attributes.put("short", "简短信息");
        span.setAttributes(attributes);

        String result = span.toString();
        
        // 验证中英文混合内容
        assertTrue("应包含中文内容", result.contains("用户登录成功"));
        assertTrue("应包含英文内容", result.contains("User login successful"));
        assertTrue("应包含混合键名", result.contains("详细信息"));
        assertTrue("应包含英文键名", result.contains("error_code"));
        
        // 验证多行对齐（中英文混合）
        String[] lines = result.split("\n");
        boolean foundDetailStart = false;
        for (String line : lines) {
            if (line.contains("详细信息: This is a detailed")) {
                foundDetailStart = true;
            } else if (foundDetailStart && line.contains("multi-line description")) {
                // 应该有正确的缩进
                assertFalse("多行英文应该有缩进", line.startsWith("multi-line description"));
                assertTrue("应包含缩进字符", line.contains("│") || line.contains(" "));
                break;
            }
        }
    }

    @Test
    public void testDeepNestedChildrenToString() {
        // 测试多层嵌套的children结构
        SpanDataInfo rootSpan = createSpanDataInfo(
            "root-service", 
            "处理用户请求", 
            "trace-deep-123", 
            "root-span", 
            null,
            1000000000L,
            2000000000L // 1秒总耗时
        );
        
        // 添加根节点属性和事件
        Map<String, Object> rootAttrs = new HashMap<>();
        rootAttrs.put("request_id", "req-12345");
        rootAttrs.put("user_info", "用户: 张三\nUser ID: 12345\nRole: admin\nDepartment: IT部门");
        rootSpan.setAttributes(rootAttrs);
        
        List<SpanEventDataInfo> rootEvents = new ArrayList<>();
        SpanEventDataInfo requestStart = new SpanEventDataInfo();
        requestStart.setName("request_start");
        requestStart.setTimeNanos(1010000000L);
        Map<String, Object> startAttrs = new HashMap<>();
        startAttrs.put("method", "POST");
        startAttrs.put("url", "/api/user/login");
        requestStart.setAttributes(startAttrs);
        rootEvents.add(requestStart);
        rootSpan.setEvents(rootEvents);
        
        // 第二层：身份验证服务
        SpanDataInfo authSpan = createSpanDataInfo(
            "auth-service", 
            "用户身份验证", 
            "trace-deep-123", 
            "auth-span", 
            "root-span",
            1050000000L,
            1300000000L // 250ms
        );
        
        Map<String, Object> authAttrs = new HashMap<>();
        authAttrs.put("auth_type", "JWT");
        authAttrs.put("validation_rules", "密码强度检查\nPassword strength check\n用户状态验证\nUser status validation");
        authSpan.setAttributes(authAttrs);
        
        // 第三层：数据库查询
        SpanDataInfo dbSpan = createSpanDataInfo(
            "database-service", 
            "查询用户信息", 
            "trace-deep-123", 
            "db-span", 
            "auth-span",
            1100000000L,
            1200000000L // 100ms
        );
        
        Map<String, Object> dbAttrs = new HashMap<>();
        dbAttrs.put("sql", "SELECT * FROM users\nWHERE username = ?\nAND status = 'active'\nORDER BY last_login DESC");
        dbAttrs.put("table", "users");
        dbAttrs.put("rows_affected", 1);
        dbSpan.setAttributes(dbAttrs);
        
        List<SpanEventDataInfo> dbEvents = new ArrayList<>();
        SpanEventDataInfo queryEvent = new SpanEventDataInfo();
        queryEvent.setName("sql_execution");
        queryEvent.setTimeNanos(1150000000L);
        Map<String, Object> queryAttrs = new HashMap<>();
        queryAttrs.put("duration_ms", 50);
        queryAttrs.put("cache_hit", false);
        queryEvent.setAttributes(queryAttrs);
        dbEvents.add(queryEvent);
        dbSpan.setEvents(dbEvents);
        
        // 第四层：缓存操作
        SpanDataInfo cacheSpan = createSpanDataInfo(
            "cache-service", 
            "更新用户缓存", 
            "trace-deep-123", 
            "cache-span", 
            "db-span",
            1180000000L,
            1190000000L // 10ms
        );
        
        Map<String, Object> cacheAttrs = new HashMap<>();
        cacheAttrs.put("cache_key", "user:12345:profile");
        cacheAttrs.put("operation", "SET");
        cacheAttrs.put("ttl_seconds", 3600);
        cacheSpan.setAttributes(cacheAttrs);
        
        // 第三层的另一个分支：日志记录
        SpanDataInfo logSpan = createSpanDataInfo(
            "logging-service", 
            "记录登录日志", 
            "trace-deep-123", 
            "log-span", 
            "auth-span",
            1250000000L,
            1280000000L // 30ms
        );
        
        Map<String, Object> logAttrs = new HashMap<>();
        logAttrs.put("log_level", "INFO");
        logAttrs.put("message", "用户登录成功\nLogin successful for user\nIP: 192.168.1.100\nTimestamp: 2023-12-01 10:30:00");
        logSpan.setAttributes(logAttrs);
        
        // 构建树结构
        dbSpan.setChildren(Arrays.asList(cacheSpan));
        authSpan.setChildren(Arrays.asList(dbSpan, logSpan));
        rootSpan.setChildren(Arrays.asList(authSpan));

        String result = rootSpan.toString();
        
        // 验证多层级结构
        assertTrue("应包含根节点", result.contains("[root-service] 处理用户请求"));
        assertTrue("应包含第二层节点", result.contains("[auth-service] 用户身份验证"));
        assertTrue("应包含第三层节点", result.contains("[database-service] 查询用户信息"));
        assertTrue("应包含第四层节点", result.contains("[cache-service] 更新用户缓存"));
        assertTrue("应包含第三层分支节点", result.contains("[logging-service] 记录登录日志"));
        
        // 验证缩进层级（通过数空格来验证）
        String[] lines = result.split("\n");
        boolean foundLevel4 = false;
        boolean foundLevel3Branch = false;
        
        for (String line : lines) {
            if (line.contains("[cache-service] 更新用户缓存")) {
                // 第四层实际缩进是8个空格（每层4个空格）
                assertTrue("第四层应该有正确缩进", line.startsWith("        "));
                foundLevel4 = true;
            }
            if (line.contains("[logging-service] 记录登录日志")) {
                // 第三层分支实际缩进是8个空格
                assertTrue("第三层分支应该有正确缩进", line.startsWith("        "));
                foundLevel3Branch = true;
            }
        }
        
        assertTrue("应该找到第四层节点", foundLevel4);
        assertTrue("应该找到第三层分支节点", foundLevel3Branch);
        
        // 验证中英文混合的多行属性对齐
        assertTrue("应包含中文多行属性", result.contains("用户: 张三"));
        assertTrue("应包含英文多行属性", result.contains("User ID: 12345"));
        assertTrue("应包含SQL多行查询", result.contains("SELECT * FROM users"));
        assertTrue("应包含混合语言日志", result.contains("用户登录成功") && result.contains("Login successful"));
    }

    @Test
    public void testComplexTreeWithEventsToString() {
        // 测试包含大量事件的复杂树结构
        SpanDataInfo rootSpan = createSpanDataInfo(
            "api-gateway", 
            "API网关处理", 
            "trace-events-123", 
            "gateway-span", 
            null,
            1000000000L,
            1500000000L
        );
        
        // 添加多个事件到根节点
        List<SpanEventDataInfo> rootEvents = new ArrayList<>();
        
        SpanEventDataInfo event1 = new SpanEventDataInfo();
        event1.setName("request_received");
        event1.setTimeNanos(1005000000L);
        Map<String, Object> event1Attrs = new HashMap<>();
        event1Attrs.put("source_ip", "192.168.1.100");
        event1Attrs.put("user_agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        event1.setAttributes(event1Attrs);
        
        SpanEventDataInfo event2 = new SpanEventDataInfo();
        event2.setName("rate_limit_check");
        event2.setTimeNanos(1010000000L);
        Map<String, Object> event2Attrs = new HashMap<>();
        event2Attrs.put("limit", 1000);
        event2Attrs.put("current", 856);
        event2Attrs.put("result", "通过");
        event2.setAttributes(event2Attrs);
        
        SpanEventDataInfo event3 = new SpanEventDataInfo();
        event3.setName("response_sent");
        event3.setTimeNanos(1480000000L);
        Map<String, Object> event3Attrs = new HashMap<>();
        event3Attrs.put("status_code", 200);
        event3Attrs.put("response_size", 2048);
        event3.setAttributes(event3Attrs);
        
        rootEvents.add(event1);
        rootEvents.add(event2);
        rootEvents.add(event3);
        rootSpan.setEvents(rootEvents);
        
        // 添加子节点
        SpanDataInfo serviceSpan = createSpanDataInfo(
            "user-service", 
            "用户服务调用", 
            "trace-events-123", 
            "service-span", 
            "gateway-span",
            1020000000L,
            1450000000L
        );
        
        // 子节点也有事件
        List<SpanEventDataInfo> serviceEvents = new ArrayList<>();
        SpanEventDataInfo serviceEvent = new SpanEventDataInfo();
        serviceEvent.setName("business_logic_executed");
        serviceEvent.setTimeNanos(1200000000L);
        Map<String, Object> serviceEventAttrs = new HashMap<>();
        serviceEventAttrs.put("operation", "用户信息查询");
        serviceEventAttrs.put("cache_hit", true);
        serviceEvent.setAttributes(serviceEventAttrs);
        serviceEvents.add(serviceEvent);
        serviceSpan.setEvents(serviceEvents);
        
        rootSpan.setChildren(Arrays.asList(serviceSpan));

        String result = rootSpan.toString();
        
        // 验证事件信息
        assertTrue("应包含事件标签", result.contains("Events:"));
        assertTrue("应包含请求接收事件", result.contains("request_received"));
        assertTrue("应包含限流检查事件", result.contains("rate_limit_check"));
        assertTrue("应包含响应发送事件", result.contains("response_sent"));
        assertTrue("应包含业务逻辑事件", result.contains("business_logic_executed"));
        
        // 验证事件时间计算
        assertTrue("应包含正确的相对时间", result.contains("(at 5ms)")); // request_received
        assertTrue("应包含正确的相对时间", result.contains("(at 10ms)")); // rate_limit_check
        assertTrue("应包含正确的相对时间", result.contains("(at 480ms)")); // response_sent
        
        // 验证中文属性在事件中的显示
        assertTrue("应包含中文事件属性", result.contains("result=通过"));
        assertTrue("应包含中文操作描述", result.contains("operation=用户信息查询"));
    }

    /**
     * 创建测试用的SpanDataInfo对象
     */
    private SpanDataInfo createSpanDataInfo(String app, String name, String traceId,
                                          String spanId, String parentSpanId,
                                          long startNanos, long endNanos) {
        SpanDataInfo span = new SpanDataInfo();
        span.setApp(app);
        span.setName(name);
        span.setTraceId(traceId);
        span.setSpanId(spanId);
        span.setParentSpanId(parentSpanId);
        span.setStartWithNanos(startNanos);
        span.setEndWithNanos(endNanos);
        return span;
    }
}