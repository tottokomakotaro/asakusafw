/**
 * Copyright 2011 Asakusa Framework Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asakusafw.testdriver.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * The normal-form of property names.
 * @since 0.2.0
 */
public class PropertyName implements Comparable<PropertyName> {

    private final List<String> words;

    /**
     * Creates a new instance.
     * @param words the words which consists this name
     * @throws IllegalArgumentException if some parameters were {@code null}
     */
    private PropertyName(List<String> words) {
        assert words != null;
        assert words.isEmpty() == false;
        this.words = words;
    }

    /**
     * Creates a new instance.
     * @param words the words which consists this name
     * @return the created instance
     * @throws IllegalArgumentException if some parameters were {@code null}
     */
    public static PropertyName newInstance(List<String> words) {
        if (words == null) {
            throw new IllegalArgumentException("words must not be null"); //$NON-NLS-1$
        }
        if (words.isEmpty()) {
            throw new IllegalArgumentException("words must not be empty"); //$NON-NLS-1$
        }
        List<String> work = new ArrayList<String>(words.size());
        for (String w : words) {
            work.add(normalize(w));
        }
        return new PropertyName(work);
    }

    /**
     * Creates a new instance.
     * @param words the words which consists this name
     * @return the created instance
     * @throws IllegalArgumentException if some parameters were {@code null}
     */
    public static PropertyName newInstance(String... words) {
        if (words == null) {
            throw new IllegalArgumentException("words must not be null"); //$NON-NLS-1$
        }
        return newInstance(Arrays.asList(words));
    }

    private static String normalize(String word) {
        if (word == null) {
            throw new IllegalArgumentException("word must not be null"); //$NON-NLS-1$
        }
        return word.toLowerCase();
    }

    /**
     * Returns the words which consists this property name.
     * @return the words in this property name
     */
    public List<String> getWords() {
        return words;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + words.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PropertyName other = (PropertyName) obj;
        if (!words.equals(other.words)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(PropertyName o) {
        Iterator<String> left = words.iterator();
        Iterator<String> right = o.words.iterator();
        while (true) {
            if (left.hasNext() == false) {
                if (right.hasNext() == false) {
                    break;
                }
                return -1;
            }
            if (right.hasNext() == false) {
                return +1;
            }
            int diff = left.next().compareTo(right.next());
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        Iterator<String> iter = words.iterator();
        assert iter.hasNext();
        buf.append(iter.next());
        while (iter.hasNext()) {
            buf.append('-');
            buf.append(iter.next());
        }
        return buf.toString();
    }
}