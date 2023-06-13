package org.jetlinks.core.utils;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ResourceUtils;
import reactor.function.Function3;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.function.BiFunction;

@Slf4j
public class ClassUtils {

    public static <T, Loader extends ClassLoader> Optional<T> findImplClass(Class<T> superType,
                                                                            String location,
                                                                            Loader loader,
                                                                            Function3<Loader, String, InputStream, Class<?>> loadFunction) {
        boolean isJar = false;
        if (loader instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) loader).getURLs()) {
                isJar = ResourceUtils.isJarURL(url) || url.getFile().endsWith("jar")
                        || url.getFile().endsWith("zip");
            }
        }
        return findImplClass(superType, location, isJar, loader, loadFunction);
    }

    public static <T, Loader extends ClassLoader> Optional<T> findImplClass(Class<T> superType,
                                                                            String location,
                                                                            Loader loader,
                                                                            BiFunction<Loader, String, Class<?>> loadFunction) {
        return findImplClass(superType, location, loader, (_loader, classname, stream) -> loadFunction.apply(_loader, classname));
    }

    @SneakyThrows
    public static <T, Loader extends ClassLoader> Optional<T> findImplClass(Class<T> superType,
                                                                            String location,
                                                                            boolean jar,
                                                                            Loader loader,
                                                                            Function3<Loader, String, InputStream, Class<?>> loadFunction) {

        CachingMetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
        PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver(loader) {
            @Override
            protected boolean isJarResource(@NonNull Resource resource) {
                return jar;
            }
        };
        Resource[] classes = resourcePatternResolver.getResources(location);
        for (Resource aClass : classes) {
            MetadataReader reader = metadataReaderFactory.getMetadataReader(aClass);
            AnnotationMetadata annotationMetadata = reader.getAnnotationMetadata();
            if (annotationMetadata.hasAnnotation("java.lang.Deprecated")) {
                continue;
            }
            ClassMetadata classMetadata = reader.getClassMetadata();
            try (InputStream stream = aClass.getInputStream()) {
                Class<?> clazz = loadFunction.apply(loader, classMetadata.getClassName(), stream);
                if (superType.isAssignableFrom(clazz)) {
                    return Optional.of(
                            (T) clazz.getDeclaredConstructor().newInstance()
                    );
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                log.warn("load class [{}] error {}", classMetadata.getClassName(), e.getLocalizedMessage(), e);
            }
        }
        metadataReaderFactory.clearCache();

        return Optional.empty();
    }

    public static <T, Loader extends ClassLoader> Optional<T> findImplClass(Class<T> superType,
                                                                            String location,
                                                                            boolean jar,
                                                                            Loader loader,
                                                                            BiFunction<Loader, String, Class<?>> loadFunction) {
        return findImplClass(superType, location, jar, loader, (_loader, classname, stream) -> loadFunction.apply(_loader, classname));
    }

}
