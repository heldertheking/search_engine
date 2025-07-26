package com.heldertheking.search_engine.website;

import com.heldertheking.search_engine.page.Page;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "webpage")
public class Webpage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;
    private String name;

    @OneToMany(mappedBy = "webpage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Page> pages = new ArrayList<>();
}

