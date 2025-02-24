package org.jetlinks.core.lang;

class ReplacedSeparatedCharSequence2 extends ReplacedSeparatedCharSequence {

    final int i2;
    final CharSequence r2;

    public ReplacedSeparatedCharSequence2(AbstractSeparatedCharSequence source,
                                          int i, CharSequence r,
                                          int i2, CharSequence r2) {
        super(source, i, r);
        this.i2 = i2;
        this.r2 = r2;
    }

    @Override
    public SeparatedCharSequence internInner() {
          super.internInner();
          if(r2 instanceof SeparatedCharSequence){
              ((SeparatedCharSequence) r2).internInner();
          }
          return this;
    }

    @Override
    public CharSequence get(int index) {
        if (index == this.i2) {
            return r2;
        }
        return super.get(index);
    }

    @Override
    public SeparatedCharSequence replace(int index, CharSequence newChar) {
        if (index == i) {
            return new ReplacedSeparatedCharSequence2(source, i, newChar, i2, r2);
        }
        if (index == i2) {
            return new ReplacedSeparatedCharSequence2(source, i, r, i2, newChar);
        }
        return new ReplacedSeparatedCharSequence3(source, i, r, i2, r2, index, newChar);
    }
}
