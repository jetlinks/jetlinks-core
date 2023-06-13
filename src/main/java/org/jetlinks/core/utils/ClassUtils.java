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
import reactor.function.Consumer3;
import reactor.function.Function3;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
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
                                                                            Consumer3<Loader,String,InputStream> walker,
                                                                            Function3<Loader, String, InputStream, Class<?>> loadFunction) {
        return createScanner(loader, location, jar)
                .walkClass(walker)
                .findImplClass(superType, loadFunction);
    }


    @SneakyThrows
    public static <T, Loader extends ClassLoader> Optional<T> findImplClass(Class<T> superType,
                                                                            String location,
                                                                            boolean jar,
                                                                            Loader loader,
                                                                            Function3<Loader, String, InputStream, Class<?>> loadFunction) {
        return createScanner(loader, location, jar).findImplClass(superType, loadFunction);
    }

    public static <T, Loader extends ClassLoader> Optional<T> findImplClass(Class<T> superType,
                                                                            String location,
                                                                            boolean jar,
                                                                            Loader loader,
                                                                            BiFunction<Loader, String, Class<?>> loadFunction) {
        return findImplClass(superType, location, jar, loader, (_loader, classname, stream) -> loadFunction.apply(_loader, classname));
    }

    public static <Loader extends ClassLoader> Scanner<Loader> createScanner(Loader loader,
                                                                             String location,
                                                                             boolean jar) {
        return new Scanner<>(loader, location, jar);
    }


    public static class Scanner<Loader extends ClassLoader> {

        private final Loader loader;

        private final List<MetadataReader> classResources;

        @SneakyThrows
        public Scanner(Loader loader, String location, boolean jar) {
            this.loader = loader;
            CachingMetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
            PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver(loader) {
                @Override
                protected boolean isJarResource(@NonNull Resource resource) {
                    return jar;
                }
            };
            Resource[] classes = resourcePatternResolver.getResources(location);
            classResources = new ArrayList<>(classes.length);
            for (Resource aClass : classes) {
                classResources.add(metadataReaderFactory.getMetadataReader(aClass));
            }
            metadataReaderFactory.clearCache();

        }

        @SneakyThrows
        public Scanner<Loader> walkClass(Consumer3<Loader, String, InputStream> consumer) {
            for (MetadataReader reader : classResources) {
                try (InputStream stream = reader.getResource().getInputStream()) {
                    consumer.accept(loader, reader.getClassMetadata().getClassName(), stream);
                }
            }
            return this;
        }

        public <T> Optional<T> findImplClass(Class<T> superType,
                                             Function3<Loader, String, InputStream, Class<?>> loader) {
            for (MetadataReader reader : classResources) {
                AnnotationMetadata annotationMetadata = reader.getAnnotationMetadata();
                if (annotationMetadata.hasAnnotation("java.lang.Deprecated")) {
                    continue;
                }
                ClassMetadata classMetadata = reader.getClassMetadata();
                try (InputStream stream = reader.getResource().getInputStream()) {
                    Class<?> clazz = loader.apply(this.loader, classMetadata.getClassName(), stream);
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
            return Optional.empty();
        }

    }
}
