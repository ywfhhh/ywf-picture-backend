package com.ywf.ywfpicturebackend.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ywf.ywfpicturebackend.domain.user.entity.User;
import com.ywf.ywfpicturebackend.domain.user.repository.UserRepository;
import com.ywf.ywfpicturebackend.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
}

