package org.jetlinks.core.utils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ResourceUtils;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.function.BiFunction;

@Slf4j
public class ClassUtils {

    public static <T, Loader extends ClassLoader> Optional<T> findImplClass(Class<T> superType,
                                                                            String location,
                                                                            Loader loader,
                                                                            BiFunction<Loader, String, Class<?>> loadFunction) {
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
                                                                            boolean jar,
                                                                            Loader loader,
                                                                            BiFunction<Loader, String, Class<?>> loadFunction) {
        try {

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
                try {
                    Class<?> clazz = loadFunction.apply(loader, classMetadata.getClassName());
                    if (superType.isAssignableFrom(clazz)) {
                        return Optional.of(
                                (T) clazz.getDeclaredConstructor().newInstance()
                        );
                    }
                } catch (Throwable ignore) {

                }
            }
            metadataReaderFactory.clearCache();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        return Optional.empty();
    }

}
