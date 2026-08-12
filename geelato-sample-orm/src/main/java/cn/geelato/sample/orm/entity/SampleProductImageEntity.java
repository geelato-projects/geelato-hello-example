package cn.geelato.sample.orm.entity;

import cn.geelato.core.meta.model.entity.IdEntity;
import cn.geelato.lang.meta.Col;
import cn.geelato.lang.meta.Entity;
import cn.geelato.lang.meta.Title;

/**
 * 高级演示实体：示例商品图片。作为「父子嵌套保存」的子表，
 * 通过 productId 关联到 {@link SampleProductEntity}。
 */
@Entity(name = "SampleProductImage", table = "sample_adv_product_image")
@Title(title = "示例商品图片")
public class SampleProductImageEntity extends IdEntity {

    @Col(name = "product_id", dataType = "VARCHAR", charMaxlength = 64)
    @Title(title = "商品ID")
    private String productId;

    @Col(name = "url", dataType = "VARCHAR", charMaxlength = 256)
    @Title(title = "图片地址")
    private String url;
}
