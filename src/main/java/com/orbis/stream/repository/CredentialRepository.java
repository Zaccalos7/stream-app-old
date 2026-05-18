package com.orbis.stream.repository;

import com.orbis.stream.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface CredentialRepository extends JpaRepository<Credential, Integer> {

    Credential findByEmail(String email);
    Credential findByEmailAndPassword(String email, String password);
}
