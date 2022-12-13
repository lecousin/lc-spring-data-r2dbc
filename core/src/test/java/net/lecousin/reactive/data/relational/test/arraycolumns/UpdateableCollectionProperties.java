package net.lecousin.reactive.data.relational.test.arraycolumns;

import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.GeneratedValue;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;
import java.util.Set;

@Table
public class UpdateableCollectionProperties {

    @Id @GeneratedValue
    private Long id;

    @ColumnDefinition(updatable = true)
    private List<String> strings1;

    @ColumnDefinition(updatable = true)
    private Set<String> strings2;

    @ColumnDefinition(updatable = false)
    private List<String> strings3;

    @ColumnDefinition(updatable = false)
    private Set<String> strings4;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<String> getStrings1() {
        return strings1;
    }

    public void setStrings1(List<String> strings1) {
        this.strings1 = strings1;
    }

    public Set<String> getStrings2() {
        return strings2;
    }

    public void setStrings2(Set<String> strings2) {
        this.strings2 = strings2;
    }

    public List<String> getStrings3() {
        return strings3;
    }

    public void setStrings3(List<String> strings3) {
        this.strings3 = strings3;
    }

    public Set<String> getStrings4() {
        return strings4;
    }

    public void setStrings4(Set<String> strings4) {
        this.strings4 = strings4;
    }
}
