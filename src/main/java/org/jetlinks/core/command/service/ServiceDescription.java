package org.jetlinks.core.command.service;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ServiceDescription {

    @Schema(title = "服务类型", description = "服务类型,如: parkingSystem,iotService")
    @NotBlank
    private String type;

    /**
     * @see CommandServiceProvider#getId()
     */
    @Schema(title = "提供商标识")
    private String provider;

    @Schema(title = "名称")
    @NotBlank
    private String name;

    @Schema(title = "说明")
    private String description;

    @Schema(title = "厂商")
    private String manufacturer;

    @Schema(title = "版本")
    private String version;

    @Schema(title = "其他信息")
    private Map<String, Object> others;
}
