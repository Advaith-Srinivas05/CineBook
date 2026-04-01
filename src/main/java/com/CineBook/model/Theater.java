package com.CineBook.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "theaters", uniqueConstraints = {@jakarta.persistence.UniqueConstraint(columnNames = {"name"})})
public class Theater {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    private String location;

    @Column(name = "screen_count", nullable = false)
    private Integer screenCount = 1;

    @Column(name = "price", nullable = false)
    private Integer price = 250;

    @Column(name = "elite_price", nullable = false)
    private Integer elitePrice = 350;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getScreenCount() { return screenCount; }
    public void setScreenCount(Integer screenCount) { this.screenCount = screenCount; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public Integer getElitePrice() { return elitePrice; }
    public void setElitePrice(Integer elitePrice) { this.elitePrice = elitePrice; }
}
