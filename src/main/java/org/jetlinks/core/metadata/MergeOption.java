package org.jetlinks.core.metadata;

import java.util.EnumSet;

public enum MergeOption {
    ignoreExists,
    mergeExpands
    ;

    public static MergeOption[] DEFAULT_OPTIONS = new MergeOption[0];
    private static final EnumSet<MergeOption> sets=EnumSet.allOf(MergeOption.class);


    public static boolean has(MergeOption option, MergeOption... target){
        return EnumSet.of(option,target).contains(option);
    }

}
