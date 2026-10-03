package com.pelia.mtgfinder.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pelia.mtgfinder.model.Card;

public interface CardRepository extends JpaRepository<Card, UUID> {

    @Query("select c from Card c join fetch c.oracle o where lower(o.name) like lower(concat('%', :name, '%'))")
    List<Card> findByNameContainingIgnoreCase(@Param("name") String name);

    @Query("select c from Card c join fetch c.oracle o where o.oracleId = :oracleId")
    List<Card> findByOracleId(@Param("oracleId") UUID oracleId);

    @Query("""
            select c from Card c where c.cardIdentityId = :cardIdentityId
            order by c.releasedAt desc nulls last, c.id
            """)
    List<Card> findByCardIdentityId(@Param("cardIdentityId") UUID cardIdentityId);

    List<Card> findByImportId(UUID importId);

    @Query("select c from Card c join fetch c.oracle o where c.importId = :importId and lower(o.name) like lower(concat('%', :name, '%'))")
    List<Card> findByImportIdAndNameContainingIgnoreCase(@Param("importId") UUID importId, @Param("name") String name);

    List<Card> findBySetCodeIgnoreCase(String setCode);

}
