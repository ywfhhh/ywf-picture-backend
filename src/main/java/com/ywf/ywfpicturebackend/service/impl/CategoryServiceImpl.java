package com.ywf.ywfpicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ywf.ywfpicturebackend.mapper.CategoryMapper;
import com.ywf.ywfpicturebackend.model.entity.Category;
import com.ywf.ywfpicturebackend.service.CategoryService;
import org.springframework.stereotype.Service;

/**
* @author yiwenfeng
* @description 针对表【category(分类表)】的数据库操作Service实现
* @createDate 2025-04-27 01:50:56
*/
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
    implements CategoryService {

}




