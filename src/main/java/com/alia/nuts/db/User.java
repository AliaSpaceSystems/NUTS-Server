package com.alia.nuts.db;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "\"user\"")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

//    @OneToMany(cascade = CascadeType.REMOVE,
//            mappedBy = "user",
//            orphanRemoval = true)
//    private Set<OrderTable> orderTables = new HashSet<>();

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

//    public Set<OrderTable> getOrderTables() {
//        return orderTables;
//    }
//
//    public void setOrderTables(Set<OrderTable> orderTables) {
//        this.orderTables = orderTables;
//    }
}
