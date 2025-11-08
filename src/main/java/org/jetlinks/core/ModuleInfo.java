package org.jetlinks.core;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ModuleInfo implements Module {
    private String id;
    private String name;
    private String description;
    private Map<String, Object> metadata;

    public static ModuleInfo of(Module module) {
        if (module instanceof ModuleInfo) {
            return (ModuleInfo) module;
        }
        ModuleInfo moduleInfo = new ModuleInfo();
        moduleInfo.id = module.getId();
        moduleInfo.name = module.getName();
        moduleInfo.description = module.getDescription();
        moduleInfo.metadata = module.getMetadata();
        return moduleInfo;
    }
}
