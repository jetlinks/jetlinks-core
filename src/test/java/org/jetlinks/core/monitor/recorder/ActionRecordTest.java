package org.jetlinks.core.monitor.recorder;

import org.jetlinks.core.utils.SerializeUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ActionRecordTest {

    @Test
    public void testCodecShouldKeepIds() throws Exception {
        ActionRecord record = new ActionRecord();
        record.setId("action-1");
        record.setParentId("parent-1");
        record.setAction("open-detail");
        record.setTags(Map.of("type", "marketplace"));
        record.setAttributes(Map.of("target", "skill-1"));
        record.setHasError(true);
        record.setValueCount(2);
        record.setCancel(false);
        record.setTraceId("trace-1");
        record.setSpanId("span-1");
        record.setTimestamp(100L);
        record.setUseNanos(200L);
        record.setErrorType("java.lang.IllegalStateException");
        record.setErrorDetail("stack");

        ActionRecord decoded = codec(record);

        assertNotNull(decoded);
        assertEquals(record.getId(), decoded.getId());
        assertEquals(record.getParentId(), decoded.getParentId());
        assertEquals(String.valueOf(record.getAction()), String.valueOf(decoded.getAction()));
        assertEquals(record.getTags(), decoded.getTags());
        assertEquals(record.getAttributes(), decoded.getAttributes());
        assertEquals(record.isHasError(), decoded.isHasError());
        assertEquals(record.getValueCount(), decoded.getValueCount());
        assertEquals(record.isCancel(), decoded.isCancel());
        assertEquals(record.getTraceId(), decoded.getTraceId());
        assertEquals(record.getSpanId(), decoded.getSpanId());
        assertEquals(record.getTimestamp(), decoded.getTimestamp());
        assertEquals(record.getUseNanos(), decoded.getUseNanos());
        assertEquals(record.getErrorType(), decoded.getErrorType());
        assertEquals(record.getErrorDetail(), decoded.getErrorDetail());
    }

    private ActionRecord codec(ActionRecord record) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ObjectOutputStream objOut = new ObjectOutputStream(output)) {
            SerializeUtils.writeObject(record, objOut);
        }
        try (ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            return (ActionRecord) SerializeUtils.readObject(objIn);
        }
    }
}
