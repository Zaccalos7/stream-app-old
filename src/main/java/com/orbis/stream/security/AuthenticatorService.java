package com.orbis.stream.security;

import com.orbis.stream.record.LoginRecord;
import com.orbis.stream.model.Credential;
import com.orbis.stream.repository.CredentialRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.login.LoginException;

@RequiredArgsConstructor
@Service
public class AuthenticatorService {
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtilComponent jwtUtilComponent;

    public String login(@Valid LoginRecord loginRecord) throws LoginException {
        String email = loginRecord.email();
        String password = loginRecord.password();

        Credential credential = getEmailFromCredential(email);

        if (credential == null) {
            throw new LoginException("not.valid.credentials");
        }

        boolean match = passwordEncoder.matches(
                password,
                credential.getPassword()
        );

        if (!match) {
            throw new LoginException("not.valid.credentials");
        }

        String bearer = null;
        bearer = jwtUtilComponent.generateToken(email);
        return bearer;
    }


    @Transactional(readOnly = true)
    protected Credential getEmailFromCredential(String email){
        return credentialRepository.findByEmail(email);
    }
}
