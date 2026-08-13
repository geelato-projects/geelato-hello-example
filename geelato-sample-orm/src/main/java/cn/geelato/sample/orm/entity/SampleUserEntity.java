package cn.geelato.sample.orm.entity;

import cn.geelato.core.meta.model.entity.IdEntity;
import cn.geelato.lang.meta.Col;
import cn.geelato.lang.meta.Entity;
import cn.geelato.lang.meta.Title;

/**
 * 基础 CRUD 示例实体：示例用户。
 * 继承 {@link IdEntity} 复用主键 id；仅做最基础的 CRUD 演示，不含租户/审计字段
 * （因此默认字段填充 SPI 与查询过滤注入 SPI 都会按“实体感知”跳过它）。
 * <p>带 getter/setter，可直接用于 {@code MetaFactory.save/query(entity)} 等对象化重载
 * （对象读取走 {@code PropertyUtils.getProperty(...)}，需要 JavaBean 访问器）。</p>
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
