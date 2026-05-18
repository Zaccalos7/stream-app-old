package com.orbis.stream.service;

import com.orbis.stream.component.LoggerMessageComponent;
import com.orbis.stream.dto.CredentialDto;
import com.orbis.stream.dto.UserDto;
import com.orbis.stream.exceptions.DuplicationEntityException;
import com.orbis.stream.record.RegisterRecord;
import com.orbis.stream.mapping.mapperDTO.CredentialMapper;
import com.orbis.stream.mapping.mapperDTO.UserMapper;
import com.orbis.stream.model.Credential;
import com.orbis.stream.model.User;
import com.orbis.stream.repository.CredentialRepository;
import com.orbis.stream.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserRepository userRepository;
    private final CredentialMapper credentialMapper;
    private final CredentialRepository credentialRepository;
    private final UserMapper userMapper;
    private final LoggerMessageComponent loggerMessageComponent;

    private static final Boolean HAS_VERIFIED_EMAIL = false;

    public void register(RegisterRecord registerRecord){
        makeRegister(registerRecord);
    }

    @Transactional()
    private void makeRegister(RegisterRecord registerRecord){
        String email = registerRecord.email();
        String nickName = registerRecord.nickName();

        isEmailAlreadyPresent(email);
        isNickNameAlreadyPresent(nickName);
        Credential credential = makeCredential(email, registerRecord.password());
        User user = makeUser(registerRecord.nickName(), credential);

        credentialRepository.save(credential);
        userRepository.save(user);
    }

    private void isEmailAlreadyPresent(String email) {
        Credential credential = credentialRepository.findByEmail(email);
        if(credential == null){
            return;
        }

        log.error(loggerMessageComponent.printMessage("email.is.already.present"));
        throw new DuplicationEntityException("email.is.already.present");

    }

    private void isNickNameAlreadyPresent(String nickName){
        Optional<User> user = userRepository.findByNickName(nickName);
        if(user.isEmpty()){
            return;
        }
        log.error(loggerMessageComponent.printMessage("nickname.is.already.present"));
        throw new DuplicationEntityException("nickname.is.already.present");
    }

    private Credential makeCredential(String email , String password){
        String passwordEncoded = cryptPassword(password);

        CredentialDto credentialDto = CredentialDto.builder()
                .email(email)
                .password(passwordEncoded)
                .hasVerifiedEmail(HAS_VERIFIED_EMAIL)
                .build();

        Credential credential = credentialMapper.toModel(credentialDto);
        return credential;
    }

    private String cryptPassword(String password){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String passwordEncoded = encoder.encode(password);
        return passwordEncoded;
    }

    private User makeUser(String nickName, Credential credential){
        UserDto userDto = UserDto.builder()
                .nickName(nickName)
                .build();
        User user = userMapper.toModel(userDto);
        user.setCredential(credential);
        return user;
    }

}
