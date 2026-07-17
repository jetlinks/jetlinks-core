package org.jetlinks.core.metadata;

import org.jetlinks.core.ProtocolSupport;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * 固件元数据解析上下文。
 * <p>
 * 每个上下文只对应一次
 * {@link ProtocolSupport#parseFirmwareMetadata(FirmwareMetadataContext)} 调用的解析生命周期。
 * 协议实现不得缓存上下文、归档、条目或读取回调中获得的资源。输入流和归档资源只在对应
 * 回调执行期间有效，并由上下文实现负责关闭。
 * <p>
 * 读取操作是同步且可能阻塞的，阻塞调度边界由协议实现返回的 {@code Mono} 链负责。
 * 所有读取回调及其返回值均不可为 {@code null}。
 *
 * @since 1.3.2
 * @see ProtocolSupport
 */
public interface FirmwareMetadataContext {

    /**
     * 获取当前固件文件的位置。
     *
     * @return 文件位置，不可为 {@code null}
     */
    String getFileLocation();

    /**
     * 读取固件文件内容。输入流只在 {@code reader} 回调执行期间有效，回调结束后由上下文实现关闭。
     *
     * @param reader 内容读取回调，不可为 {@code null}
     * @param <T>    读取结果类型
     * @return 回调返回的读取结果，不可为 {@code null}
     * @throws IOException 读取或解析内容失败
     */
    <T> T readContent(ContentReader<T> reader) throws IOException;

    /**
     * 将固件文件作为归档读取。归档只在 {@code reader} 回调执行期间有效，回调结束后由上下文实现关闭。
     *
     * @param reader 归档读取回调，不可为 {@code null}
     * @param <T>    读取结果类型
     * @return 回调返回的读取结果；文件不是受支持的归档时返回 {@link Optional#empty()}，
     * 此时不会调用 {@code reader}
     * @throws IOException 已识别为归档但读取或解析失败
     */
    <T> Optional<T> readArchive(ArchiveReader<T> reader) throws IOException;

    /**
     * 文件内容读取回调。
     *
     * @param <T> 读取结果类型
     * @since 1.3.2
     */
    @FunctionalInterface
    interface ContentReader<T> {

        /**
         * 在输入流有效期内读取内容。
         *
         * @param stream 当前文件或归档条目的输入流，不可为 {@code null}，不得缓存
         * @return 读取结果，不可为 {@code null}
         * @throws IOException 读取或解析内容失败
         */
        T read(InputStream stream) throws IOException;
    }

    /**
     * 归档读取回调。
     *
     * @param <T> 读取结果类型
     * @since 1.3.2
     */
    @FunctionalInterface
    interface ArchiveReader<T> {

        /**
         * 在归档有效期内读取条目。
         *
         * @param archive 当前归档，不可为 {@code null}，不得缓存
         * @return 读取结果，不可为 {@code null}
         * @throws IOException 读取或解析归档失败
         */
        T read(Archive archive) throws IOException;
    }

    /**
     * 归档内容访问契约，仅在所属 {@link ArchiveReader} 回调执行期间有效。
     *
     * @since 1.3.2
     */
    interface Archive {

        /**
         * 获取归档中的全部条目。
         * 返回列表保持归档中的原始顺序和重复条目，并包含目录条目。
         *
         * @return 有序条目列表，不可为 {@code null}
         * @throws IOException 读取归档条目失败
         */
        List<? extends Entry> getEntries() throws IOException;

        /**
         * 读取指定条目的内容。输入流只在 {@code reader} 回调执行期间有效，回调结束后由上下文实现关闭。
         *
         * @param entry  当前归档通过 {@link #getEntries()} 返回的条目，不可为 {@code null}
         * @param reader 内容读取回调，不可为 {@code null}
         * @param <T>    读取结果类型
         * @return 回调返回的读取结果，不可为 {@code null}
         * @throws IOException 读取或解析条目失败
         */
        <T> T readContent(Entry entry, ContentReader<T> reader) throws IOException;

        /**
         * 将指定条目作为嵌套归档读取。嵌套归档只在 {@code reader} 回调执行期间有效，
         * 回调结束后由上下文实现关闭。
         *
         * @param entry  当前归档通过 {@link #getEntries()} 返回的条目，不可为 {@code null}
         * @param reader 归档读取回调，不可为 {@code null}
         * @param <T>    读取结果类型
         * @return 回调返回的读取结果；条目不是受支持的归档时返回 {@link Optional#empty()}，
         * 此时不会调用 {@code reader}
         * @throws IOException 已识别为归档但读取或解析失败
         */
        <T> Optional<T> readArchive(Entry entry, ArchiveReader<T> reader) throws IOException;
    }

    /**
     * 归档条目，只在所属 {@link ArchiveReader} 回调执行期间有效。
     *
     * @since 1.3.2
     */
    interface Entry {

        /**
         * @return 归档中的条目名称，不可为 {@code null}
         */
        String getName();

        /**
         * @return 是否为目录条目
         */
        boolean isDirectory();
    }
}
