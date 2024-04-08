package org.jetlinks.core.utils;

import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.things.ThingMetadata;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class TypeScriptUtils {


    public static StringBuilder declareClass(String name,
                                             String comment,
                                             List<PropertyMetadata> properties,
                                             StringBuilder main) {

        List<String> declares = new ArrayList<>();

        StringBuilder builder = new StringBuilder();
        declareClass(name, comment, properties, builder, declares);

        for (String declare : declares) {
            main.append(declare);
        }
        main.append(builder);
        return main;
    }


    public static void createMetadataDeclare(ThingMetadata metadata, StringBuilder main) {

        List<PropertyMetadata> properties = metadata.getProperties();
        List<EventMetadata> events = metadata.getEvents();

        List<String> declares = new ArrayList<>();

        StringBuilder builder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(properties)) {
            declareClass("PropertyMetadata", "物模型属性", properties, builder, declares);
        }


        for (String declare : declares) {
            main.append(declare);
        }
        main.append(builder);


    }

    public static void createMetadataVar(ThingMetadata metadata, StringBuilder main) {
        List<PropertyMetadata> properties = metadata.getProperties();
        List<String> declares = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        if (CollectionUtils.isNotEmpty(properties)) {
            declareVar(properties, builder, declares);
        }

        for (String declare : declares) {
            main.append(declare);
        }
        main.append(builder);

    }

    public static void declareVar(List<PropertyMetadata> properties,
                                  StringBuilder builder,
                                  List<String> declares) {


        for (PropertyMetadata property : properties) {
            String id = property.getId();
            if (id.contains("-")) {
                continue;
            }
            String comment = property.getName();
            if (property.getDescription() != null) {
                comment = comment + "\n" + property.getDescription();
            }
            appendComments(builder, property.getValueType(), comment);
            builder.append("declare var ")
                   .append(id)
                   .append(":")
                   .append(convertType(id, property.getValueType(), declares))
                   .append(";")
                   .append("\n");
        }
    }

    public static void declareClass(String name,
                                    String comment,
                                    List<PropertyMetadata> properties,
                                    StringBuilder builder,
                                    Collection<String> declares) {
        appendComments(builder, comment);
        builder.append("declare class ").append(name).append("{\n");
        for (PropertyMetadata property : properties) {
            String id = property.getId();
            if (id.contains("-")) {
                continue;
            }

            appendComments(builder, property.getValueType(), property.getName());
            builder.append("declare ");
            builder.append(id)
                   .append(":")
                   .append(convertType(name + "_" + id, property.getValueType(), declares))
                   .append(";")
                   .append("\n");
        }
        builder.append("\n}\n");
    }

    private static void declareClassAsMap(List<PropertyMetadata> properties,
                                          StringBuilder builder,
                                          Collection<String> declares) {
        builder.append("{\n");
        for (PropertyMetadata property : properties) {
            String id = property.getId();
            if (id.contains("-")) {
                continue;
            }

            appendComments(builder, property.getValueType(), property.getName());

            builder.append(id)
                   .append(":")
                   .append(convertType(id, property.getValueType(), declares))
                   .append(",")
                   .append("\n");
        }
        builder.append("\n}\n");
    }


    public static String convertType(String owner, DataType type, Collection<String> declares) {
        if (type == null) {
            return "object";
        }
        if (type instanceof StringType) {
            return "string";
        }
        if (type instanceof NumberType) {
            return type.getId();
        }
        if (type instanceof DateTimeType) {
            return "long";
        }
        if (type instanceof ArrayType) {
            return "java.util.List<" + convertType(owner + "_Element", ((ArrayType) type).getElementType(), declares) + ">";
        }
        if (type instanceof GeoType) {
            return "org.jetlinks.metadata.types.GeoPoint";
        }
        if (type instanceof EnumType) {
            return ((EnumType) type)
                .getElements()
                .stream()
                .map(EnumType.Element::getValue)
                .collect(Collectors.joining("'|'", "'", "'"));
        }
        if (type instanceof ObjectType) {
            // String name = owner + "_Properties";
            ObjectType objectType = ((ObjectType) type);
            StringBuilder builder = new StringBuilder();
//            declareClass(name, owner + "对象属性", objectType.getProperties(), builder, declares);
//            declares.add(builder.toString());
            declareClassAsMap(objectType.getProperties(), builder, declares);
            return builder.toString();
        }
        return "object";

    }

    private static void appendComments(StringBuilder builder, DataType type, String comments) {
        builder.append("/**\n");
        builder.append("* ").append(comments == null ? "" : comments.replace("\n", "\n* "))
               .append("\n*");
        if (type instanceof EnumType) {
            for (EnumType.Element element : ((EnumType) type).getElements()) {
                builder.append("\n* ")
                       .append(element.getValue())
                       .append(" :")
                       .append(element.getText())
                       .append("\n");
                builder.append("*");
            }
        }
        builder.append("*/\n");
    }

    private static void appendComments(StringBuilder builder, String comments) {
        builder.append("/**\n");
        builder.append("* ").append(comments==null?"": comments).append("\n");
        builder.append("*/\n");
    }

    public static String loadDeclare(String name) {
        StringBuilder builder = new StringBuilder();
        loadDeclare(name, builder, new LinkedHashSet<>());
        return builder.toString();
    }

    public static void loadDeclare(String name, StringBuilder builder) {
        loadDeclare(name, builder, new LinkedHashSet<>());
    }

    @SneakyThrows
    private static void loadDeclare(String name, StringBuilder builder, Set<String> imports) {
        if (imports.contains(name)) {
            return;
        }
        try (InputStream stream = new ClassPathResource("typescript/" + name + ".d.ts").getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.contains("@ts-ignore")) {
                    continue;
                }
                if (line.startsWith("import")) {
                    int index = line.lastIndexOf("from ");
                    if (index > 0) {
                        String module = line.substring(line.lastIndexOf("from ") + 5).trim()
                                            .replace("\"", "")
                                            .replace("'", "")
                                            .replace(";", "");
                        loadDeclare(module, builder, imports);
                        imports.add(module);
                    }
                } else {
                    builder.append(line).append("\n");
                }
            }
        }
    }


}
