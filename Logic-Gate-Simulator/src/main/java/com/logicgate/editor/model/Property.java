package com.logicgate.editor.model;

import java.util.function.Consumer;

public class Property<T> {
    public enum Type {
        COLOR,
        BOOLEAN,
        INTEGER,
        STRING,
        CHOICE
    }

    private final String name;
    private T value;
    private final Type type;
    private final Consumer<T> onValueChange;
    private String[] options; // CHOICE 타입일 경우 사용

    public Property(String name, T value, Type type, Consumer<T> onValueChange) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.onValueChange = onValueChange;
    }

    public Property(String name, T value, Type type, String[] options, Consumer<T> onValueChange) {
        this(name, value, type, onValueChange);
        this.options = options;
    }

    public String getName() { return name; }
    public T getValue() { return value; }
    public Type getType() { return type; }
    public String[] getOptions() { return options; }

    public void setValue(T newValue) {
        this.value = newValue;
        if (onValueChange != null) {
            onValueChange.accept(newValue);
        }
    }
}
