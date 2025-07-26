package com.heldertheking.search_engine.page;

import com.heldertheking.search_engine.website.Webpage;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "page")
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1024, unique = true)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webpage_id")
    private Webpage webpage;

    @ManyToMany
    @JoinTable(
            name = "page_connections",
            joinColumns = @JoinColumn(name = "page_id"),
            inverseJoinColumns = @JoinColumn(name = "connected_page_id")
    )
    private Set<Page> connections = new HashSet<>();

    // Getters, Setters
}
