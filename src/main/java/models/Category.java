package models;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity(name="categories")
public class Category implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @NotNull
    private String name;
    private String color;

    @LazyCollection(LazyCollectionOption.FALSE)
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(
            name="category_feed",
            joinColumns=@JoinColumn(name="cat_id", referencedColumnName="id"),
            inverseJoinColumns=@JoinColumn(name="feed_id", referencedColumnName="id"))
    private List<RSSFeed>  feeds = new ArrayList<>();

    @NotNull
    private Long owner_id;

    public Long getOwner_id() {
        return owner_id;
    }

    public void setOwner_id(Long owner_id) {
        this.owner_id = owner_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RSSFeed> getFeeds() {
        return feeds;
    }

    public String getColor() {
        return color;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setFeeds(List<RSSFeed> feeds) {
        this.feeds = feeds;
    }
}
