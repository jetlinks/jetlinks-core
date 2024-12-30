package org.jetlinks.core.lang;

import org.jetlinks.core.utils.RecyclerUtils;
import org.jetlinks.core.utils.StringBuilderUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

abstract class AbstractSeparatedCharSequence implements SeparatedCharSequence {
    private int $hash;

    public abstract char separator();

    @Override
    public abstract int size();

    @Override
    public abstract CharSequence get(int index);

    @Override
    public SeparatedCharSequence replace(int index, CharSequence newChar) {
        return new ReplacedSeparatedCharSequence(this, index, newChar);
    }

    public SeparatedCharSequence replace(int i0, CharSequence r0, int i1, CharSequence r1) {
        return new ReplacedSeparatedCharSequence2(this, i0, r0, i1, r1);
    }

    public SeparatedCharSequence replace(int i0, CharSequence r0, int i1, CharSequence r1, int i2, CharSequence r2) {
        return new ReplacedSeparatedCharSequence3(this, i0, r0, i1, r1, i2, r2);
    }

    public AbstractSeparatedCharSequence append(CharSequence... csq) {
        if (csq == null || csq.length == 0) {
            return this;
        }
        if (csq.length == 1) {
            return append(csq[0]);
        }
        return new AppendSeparatedCharSequenceN(this, csq);
    }

    @Override
    public AbstractSeparatedCharSequence append(CharSequence csq) {
        if (csq instanceof SeparatedCharSequence) {
            return new AppendSeparatedCharSequenceX(this, (SeparatedCharSequence) csq);
        }
        // 以分隔符开头的字符序列,
        if (csq.length() > 0 && csq.charAt(0) == separator()) {
            csq = csq.subSequence(0, csq.length());
        }
        return new AppendSeparatedCharSequence(this, csq);
    }

    @Override
    public SeparatedCharSequence append(char c) {
        //拼接分割符
        if (c == separator()) {
            return new AppendSeparatedCharSequence(this, "");
        }
        return append(String.valueOf(c));
    }

    @Override
    public SeparatedCharSequence append(CharSequence csq, int start, int end) {
        return append(csq.subSequence(start, end));
    }

    @Override
    public SeparatedCharSequence range(int start, int end) {
        return new RangeSeparatedCharSequence(this, start, end);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbstractSeparatedCharSequence another = ((AbstractSeparatedCharSequence) obj);
        if (another.separator() != separator()) {
            return false;
        }
        int size = this.size();
        int anotherSize = another.size();
        if (size != anotherSize) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (!Objects.equals(get(i), another.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = $hash;
        if (h == 0) {
            h = this.getClass().hashCode();
            int size = size();
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    h = 31 * h + get(i).hashCode() + separator();
                }
            }
        }
        return $hash = h;
    }

    @Override
    public int compareTo(@Nonnull SeparatedCharSequence c) {
        if (this == c) {
            return 0;
        }
        if (c instanceof AbstractSeparatedCharSequence) {
            AbstractSeparatedCharSequence o = (AbstractSeparatedCharSequence) c;
            if (separator() != o.separator()) {
                return Character.compare(separator(), o.separator());
            }
        }
        int selfSize = size();
        int targetSize = c.size();
        if (selfSize != targetSize) {
            return Integer.compare(selfSize, targetSize);
        }
        int r = 0;
        for (int i = 0; i < selfSize; i++) {
            CharSequence selfElement = get(i);
            CharSequence targetElement = c.get(i);
            if (selfElement == null || targetElement == null) {
                r = Objects.equals(selfElement, targetElement) ? 1 : 0;
            } else if (selfElement instanceof String && targetElement instanceof String) {
                if (selfElement != targetElement) {
                    r = ((String) selfElement).compareTo((String) targetElement);
                }
            } else if (selfElement instanceof Comparable
                && selfElement.getClass() == targetElement.getClass()) {
                r = ((Comparable<CharSequence>) selfElement).compareTo(targetElement);
            } else {
                r = compare(selfElement, targetElement);
            }
            if (r != 0) {
                return r;
            }
        }
        return r;
    }


    private int compare(CharSequence a, CharSequence b) {
        int len1 = a.length();
        int len2 = b.length();
        int lim = Math.min(len1, len2);

        int k = 0;
        while (k < lim) {
            char c1 = a.charAt(k);
            char c2 = b.charAt(k);
            if (c1 != c2) {
                return Character.compare(c1,c2);
            }
            k++;
        }
        return Integer.compare(len1, len2);
    }

    @Override
    public int length() {
        int len = 0;
        for (int i = 0; i < size(); i++) {
            len += get(i).length();
            len++;
        }
        return len - 1;
    }

    @Override
    public char charAt(int index) {
        int size = size();
        int c = 0;
        for (int i = 0; i < size; i++) {
            CharSequence element = get(i);
            int len = element.length();
            int target = c + len;

            if (index < target) {
                return element.charAt(index - c);
            }
            if (index == target) {
                return separator();
            }
            c += len + 1;
        }
        throw new StringIndexOutOfBoundsException(index);
    }

    @Override
    @Nonnull
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    @Override
    @Nonnull
    public String toString() {

        return StringBuilderUtils.buildString(
            this,
            (self, sb) -> {
                char sp = self.separator();
                int index = 0;
                int size = self.size();
                for (int i = index; i < size; i++) {
                    if (i > index) {
                        sb.append(sp);
                    }
                    sb.append(self.get(i));
                }
            });
    }

    public AbstractSeparatedCharSequence intern() {
        return RecyclerUtils.intern(this);
    }
}
