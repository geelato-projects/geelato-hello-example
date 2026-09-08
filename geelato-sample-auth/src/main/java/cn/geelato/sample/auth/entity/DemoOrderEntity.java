package cn.geelato.sample.auth.entity;

import cn.geelato.core.meta.model.entity.IdEntity;
import cn.geelato.lang.meta.Col;
import cn.geelato.lang.meta.Entity;
import cn.geelato.lang.meta.Title;

/**
 * 数据权限受控实体：示例订单。
 * <p>{@code @Entity name = "DemoOrder"} 必须与认证中心数据权限规则中的 {@code entity} 字段严格一致，
 * 数据权限注入器据此匹配。继承 {@link IdEntity} 复用主键 id。
 */
@Entity(name = "DemoOrder", table = "demo_order")
@Title(title = "示例订单", description = "数据权限受控实体（按 部门/事业部/创建人 过滤）")
public class DemoOrderEntity extends IdEntity {

    @Col(name = "title", dataType = "VARCHAR", charMaxlength = 128)
    @Title(title = "标题")
    private String title;

    @Col(name = "dept_id", dataType = "VARCHAR", charMaxlength = 64)
    @Title(title = "部门ID")
    private String deptId;

    @Col(name = "bu_id", dataType = "VARCHAR", charMaxlength = 64)
    @Title(title = "事业部ID")
    private String buId;

    @Col(name = "creator", dataType = "VARCHAR", charMaxlength = 64)
    @Title(title = "创建人")
    private String creator;

    @Col(name = "tenant_code", dataType = "VARCHAR", charMaxlength = 64)
    @Title(title = "租户编码")
    private String tenantCode;
}
