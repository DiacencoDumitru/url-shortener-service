package com.diacencodumitru.url_shortener.repository;

import com.diacencodumitru.url_shortener.entities.UrlEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

// This interface extends the MongoRepository interface, which provides basic CRUD operations for the UrlEntity class.
// UrlEntity is the type of the entity to be managed, and String is the type of the ID field of the entity.
public interface UrlRepository extends MongoRepository<UrlEntity, String> {

}
