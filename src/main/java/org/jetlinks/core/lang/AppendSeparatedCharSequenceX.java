package org.jetlinks.core.lang;

class AppendSeparatedCharSequenceX extends AbstractSeparatedCharSequence {

    final AbstractSeparatedCharSequence source;
    final SeparatedCharSequence append;

    AppendSeparatedCharSequenceX(AbstractSeparatedCharSequence source,
                                 SeparatedCharSequence append) {
        this.source = source;
        this.append = append;
    }

    @Override
    protected char separator() {
        return source.separator();
    }

    @Override
    protected int size0() {
        return source.size() + append.size();
    }

    @Override
    protected CharSequence get0(int index) {
        int size = source.size();
        if (index >= size) {
            return append.get(index - size);
        }
        return source.get(index);
    }
}
