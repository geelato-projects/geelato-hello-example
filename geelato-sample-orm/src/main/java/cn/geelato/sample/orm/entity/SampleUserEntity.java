package cn.geelato.sample.orm.entity;

import cn.geelato.core.meta.model.entity.IdEntity;
import cn.geelato.lang.meta.Col;
import cn.geelato.lang.meta.Entity;
import cn.geelato.lang.meta.Title;

/**
 * 基础 CRUD 示例实体：示例用户。
 * 继承 {@link IdEntity} 复用主键 id；仅做最基础的 CRUD 演示，不含租户/审计字段
 * （因此默认字段填充 SPI 与查询过滤注入 SPI 都会按“实体感知”跳过它）。
 */
@Entity(name = "SampleUser", table = "sample_user")
@Title(title = "示例用户", description = "基础 CRUD 演示用实体")
public class SampleUserEntity extends IdEntity {

    @Col(name = "name", dataType = "VARCHAR", charMaxlength = 128)
    @Title(title = "姓名")
    private String name;

    @Col(name = "email", dataType = "VARCHAR", charMaxlength = 128)
    @Title(title = "邮箱")
    private String email;

    @Col(name = "status", dataType = "INT")
    @Title(title = "状态")
    private Integer status;
}
