package com.heldertheking.search_engine.website;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebpageRepository extends JpaRepository<Webpage, Long> {
    Optional<Webpage> findByUrl(String url);
}
