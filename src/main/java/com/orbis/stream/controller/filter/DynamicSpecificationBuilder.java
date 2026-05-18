package com.orbis.stream.controller.filter;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DynamicSpecificationBuilder {

    private DynamicSpecificationBuilder() {}

    public static <T> Specification<T> buildSpecification(Map<String, String> filters) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((fieldName, fieldValue) -> {

                Class<?> fieldType = root.get(fieldName).getJavaType();
                Object typedValue = castToRequiredType(fieldType, fieldValue);

                Predicate predicate = criteriaBuilder.equal(
                        root.get(fieldName),
                        typedValue
                );

                predicates.add(predicate);
            });

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Object castToRequiredType(Class<?> fieldType, String value) {

        if (fieldType.equals(Integer.class)) {
            return Integer.valueOf(value);
        }

        if (fieldType.equals(Long.class)) {
            return Long.valueOf(value);
        }

        if (fieldType.equals(Boolean.class)) {
            return Boolean.valueOf(value);
        }

        if (fieldType.equals(Double.class)) {
            return Double.valueOf(value);
        }

        if (fieldType.equals(String.class)) {
            return value;
        }

        if (fieldType.isEnum()) {
            return Enum.valueOf(fieldType.asSubclass(Enum.class), value);
        }


        throw new IllegalArgumentException("Tipo non supportato: " + fieldType);
    }

}