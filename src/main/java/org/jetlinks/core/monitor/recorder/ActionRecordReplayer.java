package org.jetlinks.core.monitor.recorder;

/**
 * 支持直接回放已有操作记录的能力.
 *
 * @author zhouhao
 * @since 1.3.2
 */
public interface ActionRecordReplayer {

    /**
     * 回放已有的操作记录.
     *
     * @param record 操作记录
     */
    void replay(ActionRecord record);

    /**
     * 使用回放能力处理记录,如果 recorder 不支持直接回放,则退化为通过公开 API 进行兼容回放.
     *
     * @param recorder 记录器
     * @param record   操作记录
     */
    static void replay(ActionRecorder<?> recorder, ActionRecord record) {
        if (recorder == null || record == null) {
            return;
        }
        if (recorder instanceof ActionRecordReplayer replayer) {
            replayer.replay(record);
            return;
        }
        if (record.getTags() != null) {
            recorder.tags(record.getTags());
        }
        if (record.getAttributes() != null) {
            recorder.attributes(record.getAttributes());
        }
        if (record.isCancel()) {
            recorder.cancel();
            return;
        }
        if (record.isHasError()) {
            recorder.error(new RecordedActionException(record));
            return;
        }
        recorder.complete();
    }
}
