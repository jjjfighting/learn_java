package com.studentms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.studentms.entity.Clazz;
import com.studentms.vo.ClazzVO;

import java.util.List;

/**
 * 班级业务层接口
 * <p>
 * 与学生模块相同的模板：继承 IService 拿通用 CRUD，只声明带业务规则的方法。
 */
public interface ClazzService extends IService<Clazz> {

    /** 班级列表（含在校学生数统计） */
    List<ClazzVO> listClazzes();

    /** 新增班级 */
    void addClazz(Clazz clazz);

    /** 修改班级（含存在性校验） */
    void updateClazz(Long id, Clazz clazz);

    /** 删除班级（班级下还有学生时禁止删除） */
    void removeClazz(Long id);

    /** 查询详情，不存在时抛 CLAZZ_NOT_FOUND */
    Clazz getClazz(Long id);
}
