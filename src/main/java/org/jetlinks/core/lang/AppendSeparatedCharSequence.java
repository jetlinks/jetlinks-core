package org.jetlinks.core.lang;


class AppendSeparatedCharSequence extends AbstractSeparatedCharSequence {

    final AbstractSeparatedCharSequence source;
    final CharSequence append;

    AppendSeparatedCharSequence(AbstractSeparatedCharSequence source,
                                CharSequence append) {
        this.source = source;
        this.append = append;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public SeparatedCharSequence internInner() {
        source.internInner();
        return this;
    }

    @Override
    public char separator() {
        return source.separator();
    }

    @Override
    public int size() {
        return source.size() + 1;
    }

    @Override
    public CharSequence get(int index) {
        if (index == source.size()) {
            return append;
        }
        return source.get(index);
    }

    @Override
    int appendHash(int hash) {
        int sourceHash = source.appendHash(hash);
        return 31 * sourceHash + append.hashCode() + separator();
    }

    @Override
    int contentLength() {
        return source.contentLength() + append.length() + 1;
    }

    @Override
    int appendTo(StringBuilder builder, int segmentIndex) {
        segmentIndex = source.appendTo(builder, segmentIndex);
        if (segmentIndex++ > 0) {
            builder.append(separator());
        }
        builder.append(append);
        return segmentIndex;
    }

}
