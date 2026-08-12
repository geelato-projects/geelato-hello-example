package cn.geelato.sample.orm.entity;

import cn.geelato.core.meta.model.entity.IdEntity;
import cn.geelato.lang.meta.Col;
import cn.geelato.lang.meta.Entity;
import cn.geelato.lang.meta.Title;

import java.util.Date;

/**
 * 高级演示实体：示例商品。
 * <p>除业务字段外，刻意带上一组审计/租户字段（createAt/creator/tenantCode/updateAt/updater），
 * 用来演示「默认字段填充 SPI」自动补齐、「查询过滤注入 SPI」按租户隔离。</p>
 */
@Entity(name = "SampleProduct", table = "sample_adv_product")
@Title(title = "示例商品")
public class SampleProductEntity extends IdEntity {

    @Col(name = "name", dataType = "VARCHAR", charMaxlength = 128)
    @Title(title = "名称")
    private String name;

    @Col(name = "price", dataType = "DECIMAL", numericPrecision = 12, numericScale = 2)
    @Title(title = "价格")
    private Double price;

    @Col(name = "stock", dataType = "INT")
    @Title(title = "库存")
    private Integer stock;

    @Col(name = "category_id", dataType = "VARCHAR", charMaxlength = 64)
    @Title(title = "分类ID")
    private String categoryId;

    @Col(name = "status", dataType = "INT")
    @Title(title = "状态")
    private Integer status;

    @Col(name = "create_at", dataType = "TIMESTAMP")
    @Title(title = "创建时间")
    private Date createAt;

    @Col(name = "creator", dataType = "VARCHAR", charMaxlength = 64)
    @Title(title = "创建人")
    private String creator;

    @Col(name = "tenant_code", dataType = "VARCHAR", charMaxlength = 64)
    @Title(title = "租户编码")
    private String tenantCode;

    @Col(name = "update_at", dataType = "TIMESTAMP")
    @Title(title = "更新时间")
    private Date updateAt;

    @Col(name = "updater", dataType = "VARCHAR", charMaxlength = 64)
    @Title(title = "更新人")
    private String updater;
}
