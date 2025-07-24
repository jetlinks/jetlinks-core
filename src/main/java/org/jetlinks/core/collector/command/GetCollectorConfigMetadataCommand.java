package org.jetlinks.core.collector.command;

import org.jetlinks.core.command.Command;

/**
 * 获取采集器配置的元数据信息,用于动态渲染编辑,导入导出等场景
 *
 * @author zhouhao
 * @since 1.2.4
 * @see org.jetlinks.core.collector.DataCollectorProvider#execute(Command)
 */
public class GetCollectorConfigMetadataCommand
    extends GetConfigMetadataCommand<GetCollectorConfigMetadataCommand> {
}
