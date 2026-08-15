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

    @Override
    int appendHash(int hash) {
        int h = source.appendHash(hash);
        char separator = separator();
        for (int i = ignoreFirst ? 1 : 0, size = append.size(); i < size; i++) {
            h = 31 * h + append.get(i).hashCode() + separator;
        }
        return h;
    }

    @Override
    int contentLength() {
        int length = source.contentLength();
        for (int i = ignoreFirst ? 1 : 0, size = append.size(); i < size; i++) {
            length += append.get(i).length() + 1;
        }
        return length;
    }

    @Override
    int appendTo(StringBuilder builder, int segmentIndex) {
        segmentIndex = source.appendTo(builder, segmentIndex);
        char separator = separator();
        for (int i = ignoreFirst ? 1 : 0, size = append.size(); i < size; i++) {
            if (segmentIndex++ > 0) {
                builder.append(separator);
            }
            builder.append(append.get(i));
        }
        return segmentIndex;
    }
}
