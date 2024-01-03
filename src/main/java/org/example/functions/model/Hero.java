package org.example.functions.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Getter
@Setter
@ToString
public class Hero {
    private int id;
    private String name;

    /**
     * Used to prevent duplicate data being added.
     * Does a comparison but ignores id field.
     * @param obj
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Hero hero = (Hero) obj;
        return Objects.equals(name, hero.name);
    }

    /**
     * Add additional fields separated by comma to the hash method if you want to compare more.
     * Don't add the id field to the hash computation.
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
