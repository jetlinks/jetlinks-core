package org.jetlinks.core.lang;

class AppendSeparatedCharSequenceX extends AbstractSeparatedCharSequence {

    final AbstractSeparatedCharSequence source;
    final SeparatedCharSequence append;
    final boolean ignoreFirst;
    final int $size;
    AppendSeparatedCharSequenceX(AbstractSeparatedCharSequence source,
                                 SeparatedCharSequence append) {
        this.source = source;
        this.ignoreFirst = !append.isEmpty() && "".equals(String.valueOf(append.get(0)));
        this.append = append;
        this.$size = source.size() + append.size() + (ignoreFirst ? -1 : 0);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public SeparatedCharSequence internInner() {
        source.internInner();
        append.internInner();
        return this;
    }

    @Override
    public char separator() {
        return source.separator();
    }

    @Override
    public int size() {
        return $size;
    }

    @Override
    public CharSequence get(int index) {

        int size = source.size();
        if (index >= size) {
            if (ignoreFirst) {
                index++;
            }
            return append.get(index - size);
        }
        return source.get(index);
    }
}
