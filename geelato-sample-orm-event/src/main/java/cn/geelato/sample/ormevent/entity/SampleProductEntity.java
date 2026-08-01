package cn.geelato.sample.ormevent.entity;

import cn.geelato.core.meta.model.entity.IdEntity;
import cn.geelato.lang.meta.Col;
import cn.geelato.lang.meta.Entity;
import cn.geelato.lang.meta.Id;
import cn.geelato.lang.meta.Title;

/**
 * 示例商品实体。
 *
 * <p>继承 {@link IdEntity}（含 @Id 主键字段），以便走 {@code Dao.save(entity)} 路径，
 * 使事务感知 after 回调完整生效。
 * 用于演示 ORM 事件：保存前校验价格、保存后审计、查询统计等。
 */
@Entity(name = "SampleProduct", table = "sample_product")
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}
